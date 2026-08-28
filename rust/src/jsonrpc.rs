use std::collections::{HashMap, VecDeque};
use std::future::Future;
use std::hash::{BuildHasher, Hash, Hasher};
use std::sync::Arc;
use std::sync::atomic::{AtomicU64, Ordering};
use std::time::Instant;

use parking_lot::{Mutex, RwLock};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use tokio::io::{AsyncBufReadExt, AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt, BufReader};
use tokio::sync::{broadcast, mpsc, oneshot};
use tokio::task::JoinHandle;
use tokio::time::Instant as TokioInstant;
use tracing::{Instrument, debug, error, warn};

use crate::{Error, ErrorKind, ProtocolErrorKind};

/// Callback invoked synchronously by the JSON-RPC read loop the instant a
/// successful response is parsed, before the response is delivered to the
/// awaiter and before the read loop dispatches the next message. Use this
/// when client-side state (for example, registering a server-assigned
/// session id with the router) must be visible to any subsequent
/// notification on the same connection.
///
/// If the callback returns an error, that error is delivered to the
/// awaiter in place of the response.
pub(crate) type InlineResponseCallback =
    Box<dyn FnOnce(&JsonRpcResponse) -> Result<(), Error> + Send + Sync>;

/// Internal pairing of the response delivery channel with an optional
/// inline callback that the read loop runs synchronously before delivery.
struct PendingRequest {
    sender: oneshot::Sender<JsonRpcResponse>,
    inline_callback: Option<InlineResponseCallback>,
}

/// A JSON-RPC 2.0 request message.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct JsonRpcRequest {
    /// Protocol version (always `"2.0"`).
    pub jsonrpc: String,
    /// Request ID for correlating responses.
    pub id: u64,
    /// RPC method name.
    pub method: String,
    /// Optional method parameters.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub params: Option<Value>,
}

/// A JSON-RPC 2.0 response message.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct JsonRpcResponse {
    /// Protocol version (always `"2.0"`).
    pub jsonrpc: String,
    /// Request ID this response correlates to.
    pub id: u64,
    /// Success payload (mutually exclusive with `error`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<Value>,
    /// Error payload (mutually exclusive with `result`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<JsonRpcError>,
}

/// A JSON-RPC 2.0 error object.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcError {
    /// Numeric error code.
    pub code: i32,
    /// Human-readable error description.
    pub message: String,
    /// Optional structured error data.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub data: Option<Value>,
}

/// Standard JSON-RPC 2.0 error codes.
pub mod error_codes {
    /// Method not found (-32601).
    pub const METHOD_NOT_FOUND: i32 = -32601;
    /// Invalid method parameters (-32602).
    pub const INVALID_PARAMS: i32 = -32602;
    /// Internal server error (-32603).
    #[allow(dead_code, reason = "standard JSON-RPC code, reserved for future use")]
    pub const INTERNAL_ERROR: i32 = -32603;
}

/// A JSON-RPC 2.0 notification (no `id`, no response expected).
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct JsonRpcNotification {
    /// Protocol version (always `"2.0"`).
    pub jsonrpc: String,
    /// Notification method name.
    pub method: String,
    /// Optional notification parameters.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub params: Option<Value>,
}

/// A parsed JSON-RPC 2.0 message — request, response, or notification.
#[derive(Debug, Clone, Serialize)]
pub enum JsonRpcMessage {
    /// An incoming or outgoing request.
    Request(JsonRpcRequest),
    /// A response to a previous request.
    Response(JsonRpcResponse),
    /// A fire-and-forget notification.
    Notification(JsonRpcNotification),
}

/// Custom deserializer that dispatches based on field presence instead of
/// `#[serde(untagged)]` which tries each variant sequentially (3× parse
/// attempts for Notification — the hot-path streaming variant).
///
/// Dispatch logic:
/// - has `id` + has `method` → Request
/// - has `id` + no `method` → Response
/// - no `id`                → Notification
impl<'de> Deserialize<'de> for JsonRpcMessage {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = Value::deserialize(deserializer)?;
        let obj = value
            .as_object()
            .ok_or_else(|| serde::de::Error::custom("expected a JSON object"))?;

        let has_id = obj.contains_key("id");
        let has_method = obj.contains_key("method");

        if has_id && has_method {
            JsonRpcRequest::deserialize(value)
                .map(JsonRpcMessage::Request)
                .map_err(serde::de::Error::custom)
        } else if has_id {
            JsonRpcResponse::deserialize(value)
                .map(JsonRpcMessage::Response)
                .map_err(serde::de::Error::custom)
        } else {
            JsonRpcNotification::deserialize(value)
                .map(JsonRpcMessage::Notification)
                .map_err(serde::de::Error::custom)
        }
    }
}

impl JsonRpcRequest {
    /// Create a new JSON-RPC request with the given ID, method, and params.
    pub fn new(id: u64, method: &str, params: Option<Value>) -> Self {
        Self {
            jsonrpc: "2.0".to_string(),
            id,
            method: method.to_string(),
            params,
        }
    }
}

impl JsonRpcResponse {
    /// Returns `true` if this response contains an error.
    #[allow(dead_code)]
    pub fn is_error(&self) -> bool {
        self.error.is_some()
    }
}

const CONTENT_LENGTH_HEADER: &str = "Content-Length: ";
/// Opt-in target for content-free reverse-RPC latency events.
///
/// `request_forward` measures parsed-request receipt to forwarding,
/// `request_schedule` forwarding to dispatch start (`since_receive_us` is
/// the combined interval), `response_encode` is `serde_json::to_vec`,
/// `writer_queue` is enqueue to dequeue, and `write_all` / `flush` measure
/// the corresponding `AsyncWrite` calls. `hook_callback` is emitted by the
/// hooks dispatcher around `SessionHooks::on_hook` only. Every request phase
/// includes its start offset from request receipt, and `request_complete`
/// records total elapsed time. Collection is enabled only when this target
/// has an active DEBUG subscriber when the client is constructed.
const REVERSE_RPC_TIMING_TARGET: &str = "github_copilot_sdk::reverse_rpc_timing";
const REVERSE_RPC_TIMING_CAPACITY: usize = 256;
type CorrelationHasher = std::collections::hash_map::RandomState;

/// Rewrites unpaired UTF-16 surrogate escapes to `\uFFFD`.
///
/// Returns `None` when the body contains no unpaired surrogate, so valid
/// frames do not incur a repair allocation.
fn repair_lone_surrogates(body: &[u8]) -> Option<Vec<u8>> {
    fn hex_escape_at(body: &[u8], index: usize) -> Option<u16> {
        let digits = body.get(index + 2..index + 6)?;
        let text = std::str::from_utf8(digits).ok()?;
        u16::from_str_radix(text, 16).ok()
    }

    let mut repaired = None;
    let mut in_string = false;
    let mut index = 0;

    while index < body.len() {
        let byte = body[index];

        if !in_string {
            in_string = byte == b'"';
            index += 1;
            continue;
        }

        match byte {
            b'"' => {
                in_string = false;
                index += 1;
            }
            // Consume non-Unicode escapes whole so an escaped backslash cannot
            // be mistaken for the start of a surrogate escape.
            b'\\' if body.get(index + 1) != Some(&b'u') => index += 2,
            b'\\' => {
                let Some(unit) = hex_escape_at(body, index) else {
                    index += 2;
                    continue;
                };

                let is_pair = (0xD800..0xDC00).contains(&unit)
                    && body.get(index + 6) == Some(&b'\\')
                    && body.get(index + 7) == Some(&b'u')
                    && hex_escape_at(body, index + 6)
                        .is_some_and(|low| (0xDC00..0xE000).contains(&low));

                if is_pair {
                    index += 12;
                    continue;
                }

                if (0xD800..0xE000).contains(&unit) {
                    let output = repaired.get_or_insert_with(|| body.to_vec());
                    output[index..index + 6].copy_from_slice(br"\ufffd");
                }
                index += 6;
            }
            _ => index += 1,
        }
    }

    repaired
}

/// One framed JSON-RPC message handed to the writer actor.
///
/// `frame` is the fully serialized bytes (header + body); the caller pays
/// the serde cost synchronously before enqueueing so the actor never sees a
/// `Result` from JSON encoding. `ack` resolves once the bytes have been
/// fully written and flushed (or the underlying I/O reports an error). If
/// the caller drops the `oneshot::Receiver`, the actor still completes the
/// frame — caller cancellation cannot desync the wire.
struct WriteCommand {
    frame: Vec<u8>,
    ack: oneshot::Sender<Result<(), std::io::Error>>,
    reverse_rpc: Option<ReverseRpcWriteTrace>,
    enqueued_at: Option<TokioInstant>,
}

struct ReverseRpcWriteTrace {
    trace: ReverseRpcTrace,
    completion_attempted: bool,
}

struct ReverseRpcResponseTrace {
    request_id: u64,
    trace: ReverseRpcTrace,
}

// Response helpers retain their existing signatures while the dispatch task
// carries the exact request generation that produced the response.
tokio::task_local! {
    static REVERSE_RPC_RESPONSE_TRACE: ReverseRpcResponseTrace;
}

impl ReverseRpcWriteTrace {
    fn new(trace: ReverseRpcTrace) -> Self {
        Self {
            trace,
            completion_attempted: false,
        }
    }

    fn record_complete(&mut self, completed_at: TokioInstant, succeeded: bool) {
        self.completion_attempted = true;
        let _ = self.trace.record_complete(completed_at, succeeded);
    }
}

impl Drop for ReverseRpcWriteTrace {
    fn drop(&mut self) {
        if !self.completion_attempted {
            let _ = self.trace.record_complete(TokioInstant::now(), false);
        }
    }
}

enum ReverseRpcTimingEvent {
    Phase {
        trace: ReverseRpcTrace,
        phase: &'static str,
        start_offset_us: u64,
        elapsed_us: u64,
        succeeded: bool,
    },
    Scheduled {
        trace: ReverseRpcTrace,
        start_offset_us: u64,
        elapsed_us: u64,
        since_receive_us: u64,
    },
    HookCallback {
        trace: ReverseRpcTrace,
        hook_type: &'static str,
        start_offset_us: u64,
        elapsed_us: u64,
    },
    Complete {
        trace: ReverseRpcTrace,
        required_phase_records: u64,
        elapsed_us: u64,
        succeeded: bool,
    },
}

#[derive(Clone)]
struct ReverseRpcTimingEmitter {
    phase_tx: mpsc::Sender<ReverseRpcTimingEvent>,
    terminal_tx: mpsc::Sender<ReverseRpcTimingEvent>,
    dropped_records: Arc<AtomicU64>,
}

impl ReverseRpcTimingEmitter {
    fn emit(&self, event: ReverseRpcTimingEvent) -> bool {
        // Timing is measurement-only: a full bounded queue drops one logical
        // record rather than delaying RPC work, including for terminal records.
        // `records_dropped` is the explicit signal that the trace is incomplete.
        let result = if matches!(&event, ReverseRpcTimingEvent::Complete { .. }) {
            self.terminal_tx.try_send(event)
        } else {
            self.phase_tx.try_send(event)
        };
        match result {
            Ok(()) => true,
            Err(mpsc::error::TrySendError::Full(_)) => {
                let _ = self.dropped_records.fetch_update(
                    Ordering::Relaxed,
                    Ordering::Relaxed,
                    |count| Some(count.saturating_add(1)),
                );
                false
            }
            Err(mpsc::error::TrySendError::Closed(_)) => false,
        }
    }
}

/// Internal, content-free timing context for one inbound JSON-RPC request.
///
/// The correlation key is a deterministic digest of the numeric wire ID,
/// RPC method, and optional session ID. The original session ID and params
/// are not retained.
#[derive(Clone)]
pub(crate) struct ReverseRpcTrace {
    inner: Arc<ReverseRpcTraceInner>,
}

struct ReverseRpcTraceInner {
    correlation_key: String,
    method: &'static str,
    received_at: TokioInstant,
    forwarded_at: std::sync::OnceLock<TokioInstant>,
    timing: ReverseRpcTimingEmitter,
    timing_state: Mutex<ReverseRpcTimingState>,
    emitted_phase_records: AtomicU64,
}

#[derive(Clone, Copy)]
struct PendingReverseRpcCompletion {
    required_phase_records: u64,
    elapsed_us: u64,
    succeeded: bool,
}

#[derive(Default)]
struct ReverseRpcTimingState {
    accepted_phase_records: u64,
    pending_completion: Option<PendingReverseRpcCompletion>,
    completion_recorded: bool,
    writer_owns_completion: bool,
}

impl ReverseRpcTrace {
    fn new(
        request: &JsonRpcRequest,
        generation: u64,
        received_at: TokioInstant,
        correlation_hasher: &CorrelationHasher,
        timing: ReverseRpcTimingEmitter,
    ) -> Self {
        let session_id = request
            .params
            .as_ref()
            .and_then(|params| params.get("sessionId"))
            .and_then(Value::as_str);
        Self {
            inner: Arc::new(ReverseRpcTraceInner {
                correlation_key: Self::correlation_key(
                    correlation_hasher,
                    request.id,
                    &request.method,
                    session_id,
                    generation,
                ),
                method: Self::timing_method(&request.method),
                received_at,
                forwarded_at: std::sync::OnceLock::new(),
                timing,
                timing_state: Mutex::new(ReverseRpcTimingState::default()),
                emitted_phase_records: AtomicU64::new(0),
            }),
        }
    }

    #[cfg(test)]
    pub(crate) fn for_test(
        request: &JsonRpcRequest,
        received_at: TokioInstant,
        forwarded_at: TokioInstant,
    ) -> Self {
        let (phase_tx, phase_rx) = mpsc::channel(REVERSE_RPC_TIMING_CAPACITY);
        let (terminal_tx, terminal_rx) = mpsc::channel(REVERSE_RPC_TIMING_CAPACITY);
        let dropped_records = Arc::new(AtomicU64::new(0));
        tokio::spawn(JsonRpcClient::timing_loop(
            phase_rx,
            terminal_rx,
            dropped_records.clone(),
        ));
        let trace = Self::new(
            request,
            0,
            received_at,
            &CorrelationHasher::new(),
            ReverseRpcTimingEmitter {
                phase_tx,
                terminal_tx,
                dropped_records,
            },
        );
        trace.mark_forwarding(forwarded_at);
        trace
    }

    fn correlation_key(
        correlation_hasher: &CorrelationHasher,
        request_id: u64,
        method: &str,
        session_id: Option<&str>,
        generation: u64,
    ) -> String {
        // A per-client keyed hash keeps the request-derived key stable for all
        // phases without making custom session IDs guessable from trace output.
        let mut hasher = correlation_hasher.build_hasher();
        session_id.unwrap_or("<global>").hash(&mut hasher);
        method.hash(&mut hasher);
        request_id.hash(&mut hasher);
        generation.hash(&mut hasher);
        format!("rrpc-{:016x}", hasher.finish())
    }

    fn timing_method(method: &str) -> &'static str {
        match method {
            "hooks.invoke" => "hooks.invoke",
            "userInput.request" => "userInput.request",
            "exitPlanMode.request" => "exitPlanMode.request",
            "autoModeSwitch.request" => "autoModeSwitch.request",
            "systemMessage.transform" => "systemMessage.transform",
            "gitHubToken.getToken" => "gitHubToken.getToken",
            "providerToken.getToken" => "providerToken.getToken",
            _ if method.starts_with("sessionFs.") => "sessionFs.*",
            _ if method.starts_with("canvas.") => "canvas.*",
            _ if method.starts_with("llmInference.") => "llmInference.*",
            _ => "unknown",
        }
    }

    fn timing_hook_type(hook_type: &str) -> &'static str {
        match hook_type {
            "preToolUse" => "preToolUse",
            "preMcpToolCall" => "preMcpToolCall",
            "postToolUse" => "postToolUse",
            "postToolUseFailure" => "postToolUseFailure",
            "userPromptSubmitted" => "userPromptSubmitted",
            "userPromptTransformed" => "userPromptTransformed",
            "sessionStart" => "sessionStart",
            "sessionEnd" => "sessionEnd",
            "errorOccurred" => "errorOccurred",
            "agentStop" => "agentStop",
            _ => "unknown",
        }
    }

    fn elapsed_us(duration: std::time::Duration) -> u64 {
        u64::try_from(duration.as_micros()).unwrap_or(u64::MAX)
    }

    fn start_offset_us(&self, started_at: TokioInstant) -> u64 {
        Self::elapsed_us(started_at.duration_since(self.inner.received_at))
    }

    fn mark_forwarding(&self, forwarded_at: TokioInstant) {
        self.inner
            .forwarded_at
            .set(forwarded_at)
            .expect("forwarding timestamp must be recorded exactly once");
    }

    fn forward<T>(&self, send: impl FnOnce() -> Result<(), T>) -> Result<(), T> {
        let forwarded_at = TokioInstant::now();
        self.mark_forwarding(forwarded_at);
        let mut state = self.inner.timing_state.lock();
        let result = send();
        self.emit_phase_with_state(
            &mut state,
            ReverseRpcTimingEvent::Phase {
                trace: self.clone(),
                phase: "request_forward",
                start_offset_us: 0,
                elapsed_us: Self::elapsed_us(forwarded_at.duration_since(self.inner.received_at)),
                succeeded: result.is_ok(),
            },
        );
        result
    }

    fn record_scheduled(&self, scheduled_at: TokioInstant) {
        let forwarded_at = self
            .inner
            .forwarded_at
            .get()
            .expect("forwarding timestamp must be set before scheduling");
        self.emit_phase(ReverseRpcTimingEvent::Scheduled {
            trace: self.clone(),
            start_offset_us: self.start_offset_us(*forwarded_at),
            elapsed_us: Self::elapsed_us(scheduled_at.duration_since(*forwarded_at)),
            since_receive_us: Self::elapsed_us(scheduled_at.duration_since(self.inner.received_at)),
        });
    }

    pub(crate) fn record_hook_callback(
        &self,
        hook_type: &str,
        started_at: TokioInstant,
        elapsed: std::time::Duration,
    ) {
        self.emit_phase(ReverseRpcTimingEvent::HookCallback {
            trace: self.clone(),
            hook_type: Self::timing_hook_type(hook_type),
            start_offset_us: self.start_offset_us(started_at),
            elapsed_us: Self::elapsed_us(elapsed),
        });
    }

    fn record_phase(
        &self,
        phase: &'static str,
        started_at: TokioInstant,
        elapsed: std::time::Duration,
        succeeded: bool,
    ) {
        self.emit_phase(ReverseRpcTimingEvent::Phase {
            trace: self.clone(),
            phase,
            start_offset_us: self.start_offset_us(started_at),
            elapsed_us: Self::elapsed_us(elapsed),
            succeeded,
        });
    }

    fn emit_phase(&self, event: ReverseRpcTimingEvent) {
        let mut state = self.inner.timing_state.lock();
        self.emit_phase_with_state(&mut state, event);
    }

    fn emit_phase_with_state(
        &self,
        state: &mut ReverseRpcTimingState,
        event: ReverseRpcTimingEvent,
    ) {
        if state.pending_completion.is_some() || state.completion_recorded {
            return;
        }
        if self.inner.timing.emit(event) {
            state.accepted_phase_records = state.accepted_phase_records.saturating_add(1);
        }
    }

    fn record_complete(&self, completed_at: TokioInstant, succeeded: bool) -> bool {
        let mut state = self.inner.timing_state.lock();
        self.record_complete_with_state(&mut state, completed_at, succeeded)
    }

    fn record_abandoned(&self, completed_at: TokioInstant) {
        let mut state = self.inner.timing_state.lock();
        if !state.writer_owns_completion {
            let _ = self.record_complete_with_state(&mut state, completed_at, false);
        }
    }

    fn record_complete_with_state(
        &self,
        state: &mut ReverseRpcTimingState,
        completed_at: TokioInstant,
        succeeded: bool,
    ) -> bool {
        if state.completion_recorded {
            return true;
        }
        if state.pending_completion.is_none() {
            state.pending_completion = Some(PendingReverseRpcCompletion {
                required_phase_records: state.accepted_phase_records,
                elapsed_us: Self::elapsed_us(completed_at.duration_since(self.inner.received_at)),
                succeeded,
            });
        }
        let completion = state
            .pending_completion
            .expect("pending completion must be initialized");
        if self.inner.timing.emit(ReverseRpcTimingEvent::Complete {
            trace: self.clone(),
            required_phase_records: completion.required_phase_records,
            elapsed_us: completion.elapsed_us,
            succeeded: completion.succeeded,
        }) {
            state.pending_completion = None;
            state.completion_recorded = true;
            true
        } else {
            false
        }
    }

    fn transfer_completion_to_writer(&self) {
        self.inner.timing_state.lock().writer_owns_completion = true;
    }
}

#[derive(Clone)]
struct ReverseRpcRegistry(Arc<ReverseRpcRegistryInner>);

struct ReverseRpcRegistryInner {
    traces: Mutex<Vec<ReverseRpcTrace>>,
    next_generation: AtomicU64,
}

impl ReverseRpcRegistry {
    fn new() -> Self {
        Self(Arc::new(ReverseRpcRegistryInner {
            traces: Mutex::new(Vec::new()),
            next_generation: AtomicU64::new(1),
        }))
    }

    fn next_generation(&self) -> u64 {
        self.0.next_generation.fetch_add(1, Ordering::Relaxed)
    }

    fn insert(&self, trace: ReverseRpcTrace) {
        self.0.traces.lock().push(trace);
    }

    fn remove(&self, trace: &ReverseRpcTrace) -> bool {
        let mut traces = self.0.traces.lock();
        let Some(index) = traces
            .iter()
            .position(|current| Arc::ptr_eq(&current.inner, &trace.inner))
        else {
            return false;
        };
        traces.swap_remove(index);
        true
    }

    fn abandon_all(&self) {
        let traces = std::mem::take(&mut *self.0.traces.lock());
        for trace in traces {
            trace.record_abandoned(TokioInstant::now());
        }
    }

    #[cfg(test)]
    fn contains(&self, trace: &ReverseRpcTrace) -> bool {
        self.0
            .traces
            .lock()
            .iter()
            .any(|current| Arc::ptr_eq(&current.inner, &trace.inner))
    }

    #[cfg(test)]
    fn is_empty(&self) -> bool {
        self.0.traces.lock().is_empty()
    }
}

pub(crate) struct ReverseRpcRequest {
    request: Option<JsonRpcRequest>,
    trace: Option<ReverseRpcTrace>,
    registry: Option<ReverseRpcRegistry>,
}

impl std::ops::Deref for ReverseRpcRequest {
    type Target = JsonRpcRequest;

    fn deref(&self) -> &Self::Target {
        self.request
            .as_ref()
            .expect("reverse RPC request must exist until dispatch")
    }
}

impl ReverseRpcRequest {
    fn new(
        request: JsonRpcRequest,
        trace: Option<ReverseRpcTrace>,
        registry: Option<ReverseRpcRegistry>,
    ) -> Self {
        Self {
            request: Some(request),
            trace,
            registry,
        }
    }

    pub(crate) fn into_dispatch(mut self) -> (JsonRpcRequest, Option<ReverseRpcDispatchGuard>) {
        let request = self
            .request
            .take()
            .expect("reverse RPC request must exist until dispatch");
        let guard = self.trace.take().map(|trace| {
            trace.record_scheduled(TokioInstant::now());
            ReverseRpcDispatchGuard {
                registry: self
                    .registry
                    .take()
                    .expect("timed reverse RPC request must have a registry"),
                request_id: request.id,
                trace,
            }
        });
        (request, guard)
    }
}

impl Drop for ReverseRpcRequest {
    fn drop(&mut self) {
        if let Some(trace) = self.trace.take()
            && self
                .registry
                .as_ref()
                .is_some_and(|registry| registry.remove(&trace))
        {
            trace.record_abandoned(TokioInstant::now());
        }
    }
}

pub(crate) struct ReverseRpcDispatchGuard {
    registry: ReverseRpcRegistry,
    request_id: u64,
    trace: ReverseRpcTrace,
}

impl ReverseRpcDispatchGuard {
    pub(crate) fn trace(&self) -> &ReverseRpcTrace {
        &self.trace
    }

    pub(crate) async fn scope<F: Future>(&self, future: F) -> F::Output {
        REVERSE_RPC_RESPONSE_TRACE
            .scope(
                ReverseRpcResponseTrace {
                    request_id: self.request_id,
                    trace: self.trace.clone(),
                },
                future,
            )
            .await
    }
}

impl Drop for ReverseRpcDispatchGuard {
    fn drop(&mut self) {
        if self.registry.remove(&self.trace) {
            self.trace.record_abandoned(TokioInstant::now());
        }
    }
}

#[derive(Clone)]
enum ReverseRequestSender {
    Public(mpsc::UnboundedSender<JsonRpcRequest>),
    Internal(mpsc::UnboundedSender<ReverseRpcRequest>),
}

/// Low-level JSON-RPC 2.0 client over Content-Length-framed streams.
///
/// # Cancel safety
///
/// All public methods (`write`, `send_request`) are **cancel-safe**: the
/// actual bytes hit the wire on a dedicated background actor task, so
/// dropping the caller's future after `await` returns `Pending` cannot
/// produce a partial frame on the wire. Frames either land atomically or
/// the underlying I/O fails. See `cancel-safety review` artifact for the
/// full RFD-400 reasoning.
pub struct JsonRpcClient {
    request_id: AtomicU64,
    /// Sender side of the writer actor's command queue. Public methods
    /// pre-serialize their frames and enqueue here; the background actor
    /// drains the queue and serializes writes onto the underlying
    /// `AsyncWrite`. Unbounded by design — RFD 400 explicitly permits this
    /// for cancel-safety, and JSON-RPC frames are small relative to the
    /// natural request/response back-pressure of the wire.
    write_tx: mpsc::UnboundedSender<WriteCommand>,
    pending_requests: Arc<RwLock<HashMap<u64, PendingRequest>>>,
    reverse_requests: Option<ReverseRpcRegistry>,
    notification_tx: broadcast::Sender<JsonRpcNotification>,
    request_tx: ReverseRequestSender,
    read_task: Mutex<Option<JoinHandle<()>>>,
    write_task: Mutex<Option<JoinHandle<()>>>,
    timing_task: Mutex<Option<std::thread::JoinHandle<()>>>,
}

impl JsonRpcClient {
    /// Create a new client from async read/write streams.
    ///
    /// Spawns two background tasks: a reader that dispatches incoming
    /// messages to pending request channels, the notification broadcast,
    /// or the request-forwarding channel; and a writer actor that owns the
    /// underlying `AsyncWrite` and serializes frames atomically.
    #[cfg_attr(
        not(any(test, feature = "test-support")),
        expect(
            dead_code,
            reason = "low-level constructor is exported only with test-support"
        )
    )]
    pub fn new(
        writer: impl AsyncWrite + Unpin + Send + 'static,
        reader: impl AsyncRead + Unpin + Send + 'static,
        notification_tx: broadcast::Sender<JsonRpcNotification>,
        request_tx: mpsc::UnboundedSender<JsonRpcRequest>,
    ) -> Self {
        Self::new_inner(
            writer,
            reader,
            notification_tx,
            ReverseRequestSender::Public(request_tx),
            false,
        )
    }

    pub(crate) fn new_with_reverse_rpc_timing(
        writer: impl AsyncWrite + Unpin + Send + 'static,
        reader: impl AsyncRead + Unpin + Send + 'static,
        notification_tx: broadcast::Sender<JsonRpcNotification>,
        request_tx: mpsc::UnboundedSender<ReverseRpcRequest>,
    ) -> Self {
        let trace_reverse_rpc = tracing::enabled!(
            target: REVERSE_RPC_TIMING_TARGET,
            tracing::Level::DEBUG
        );
        Self::new_inner(
            writer,
            reader,
            notification_tx,
            ReverseRequestSender::Internal(request_tx),
            trace_reverse_rpc,
        )
    }

    fn new_inner(
        writer: impl AsyncWrite + Unpin + Send + 'static,
        reader: impl AsyncRead + Unpin + Send + 'static,
        notification_tx: broadcast::Sender<JsonRpcNotification>,
        request_tx: ReverseRequestSender,
        trace_reverse_rpc: bool,
    ) -> Self {
        let (write_tx, write_rx) = mpsc::unbounded_channel::<WriteCommand>();

        let writer_span = tracing::error_span!("jsonrpc_write_loop");
        let write_task = tokio::spawn(Self::write_loop(writer, write_rx).instrument(writer_span));
        let (timing, timing_task, correlation_hasher, reverse_requests) = if trace_reverse_rpc {
            let (phase_tx, phase_rx) =
                mpsc::channel::<ReverseRpcTimingEvent>(REVERSE_RPC_TIMING_CAPACITY);
            let (terminal_tx, terminal_rx) =
                mpsc::channel::<ReverseRpcTimingEvent>(REVERSE_RPC_TIMING_CAPACITY);
            let dropped_records = Arc::new(AtomicU64::new(0));
            (
                Some(ReverseRpcTimingEmitter {
                    phase_tx,
                    terminal_tx,
                    dropped_records: dropped_records.clone(),
                }),
                Some(Self::spawn_timing_thread(
                    phase_rx,
                    terminal_rx,
                    dropped_records,
                )),
                Some(CorrelationHasher::new()),
                Some(ReverseRpcRegistry::new()),
            )
        } else {
            (None, None, None, None)
        };

        let client = Self {
            request_id: AtomicU64::new(1),
            write_tx,
            pending_requests: Arc::new(RwLock::new(HashMap::new())),
            reverse_requests,
            notification_tx,
            request_tx,
            read_task: Mutex::new(None),
            write_task: Mutex::new(Some(write_task)),
            timing_task: Mutex::new(timing_task),
        };

        let pending_requests = client.pending_requests.clone();
        let reverse_requests = client.reverse_requests.clone();
        let notification_tx_clone = client.notification_tx.clone();
        let request_tx_clone = client.request_tx.clone();
        let reader_span = tracing::error_span!("jsonrpc_read_loop");

        let read_task = tokio::spawn(
            async move {
                Self::read_loop(
                    reader,
                    pending_requests,
                    reverse_requests,
                    notification_tx_clone,
                    request_tx_clone,
                    timing,
                    correlation_hasher,
                )
                .await;
            }
            .instrument(reader_span),
        );
        *client.read_task.lock() = Some(read_task);

        client
    }

    fn spawn_timing_thread(
        phase_rx: mpsc::Receiver<ReverseRpcTimingEvent>,
        terminal_rx: mpsc::Receiver<ReverseRpcTimingEvent>,
        dropped_records: Arc<AtomicU64>,
    ) -> std::thread::JoinHandle<()> {
        let dispatch = tracing::dispatcher::get_default(Clone::clone);
        std::thread::Builder::new()
            .name("copilot-reverse-rpc-timing".to_string())
            .spawn(move || {
                tracing::dispatcher::with_default(&dispatch, || {
                    tokio::runtime::Builder::new_current_thread()
                        .enable_all()
                        .build()
                        .expect("reverse RPC timing runtime should start")
                        .block_on(Self::timing_loop(phase_rx, terminal_rx, dropped_records));
                });
            })
            .expect("reverse RPC timing thread should start")
    }

    pub(crate) fn force_close(&self) {
        if let Some(task) = self.read_task.lock().take() {
            task.abort();
        }
        if let Some(task) = self.write_task.lock().take() {
            task.abort();
        }
        self.pending_requests.write().clear();
        if let Some(reverse_requests) = &self.reverse_requests {
            reverse_requests.abandon_all();
        }
        // Detach the timing task so it can drain the bounded queue. The
        // aborted read/write tasks drop the remaining senders, so it exits
        // once those final diagnostics are emitted.
        let _ = self.timing_task.lock().take();
    }

    async fn timing_loop(
        mut phase_rx: mpsc::Receiver<ReverseRpcTimingEvent>,
        mut terminal_rx: mpsc::Receiver<ReverseRpcTimingEvent>,
        dropped_records: Arc<AtomicU64>,
    ) {
        let mut phase_closed = false;
        let mut terminal_closed = false;
        let mut pending_terminals = VecDeque::new();
        while !phase_closed || !terminal_closed {
            Self::record_dropped_timing_records(&dropped_records);
            tokio::select! {
                biased;
                event = terminal_rx.recv(), if !terminal_closed => {
                    if let Some(event) = event {
                        Self::record_or_defer_timing_event(event, &mut pending_terminals);
                    } else {
                        terminal_closed = true;
                    }
                }
                event = phase_rx.recv(), if !phase_closed => {
                    if let Some(event) = event {
                        Self::record_or_defer_timing_event(event, &mut pending_terminals);
                    } else {
                        phase_closed = true;
                    }
                }
            }
            Self::record_dropped_timing_records(&dropped_records);
        }
        Self::record_dropped_timing_records(&dropped_records);
    }

    fn record_or_defer_timing_event(
        event: ReverseRpcTimingEvent,
        pending_terminals: &mut VecDeque<ReverseRpcTimingEvent>,
    ) {
        if let ReverseRpcTimingEvent::Complete {
            trace,
            required_phase_records,
            ..
        } = &event
            && trace.inner.emitted_phase_records.load(Ordering::Acquire) < *required_phase_records
        {
            pending_terminals.push_back(event);
            return;
        }

        let phase_trace = match &event {
            ReverseRpcTimingEvent::Complete { .. } => None,
            ReverseRpcTimingEvent::Phase { trace, .. }
            | ReverseRpcTimingEvent::Scheduled { trace, .. }
            | ReverseRpcTimingEvent::HookCallback { trace, .. } => Some(trace.clone()),
        };
        Self::record_timing_event(event);
        if let Some(trace) = phase_trace {
            trace
                .inner
                .emitted_phase_records
                .fetch_add(1, Ordering::Release);
        }

        let mut index = 0;
        while index < pending_terminals.len() {
            let ready = match &pending_terminals[index] {
                ReverseRpcTimingEvent::Complete {
                    trace,
                    required_phase_records,
                    ..
                } => {
                    trace.inner.emitted_phase_records.load(Ordering::Acquire)
                        >= *required_phase_records
                }
                _ => unreachable!("only terminal records are deferred"),
            };
            if ready {
                let terminal = pending_terminals
                    .remove(index)
                    .expect("pending terminal index should exist");
                Self::record_timing_event(terminal);
            } else {
                index += 1;
            }
        }
    }

    fn record_timing_event(event: ReverseRpcTimingEvent) {
        match event {
            ReverseRpcTimingEvent::Phase {
                trace,
                phase,
                start_offset_us,
                elapsed_us,
                succeeded,
            } => {
                debug!(
                    target: REVERSE_RPC_TIMING_TARGET,
                    parent: None,
                    correlation_key = %trace.inner.correlation_key,
                    rpc_method = %trace.inner.method,
                    phase,
                    start_offset_us,
                    elapsed_us,
                    status = if succeeded { "succeeded" } else { "failed" },
                    "reverse JSON-RPC timing"
                );
            }
            ReverseRpcTimingEvent::Scheduled {
                trace,
                start_offset_us,
                elapsed_us,
                since_receive_us,
            } => {
                debug!(
                    target: REVERSE_RPC_TIMING_TARGET,
                    parent: None,
                    correlation_key = %trace.inner.correlation_key,
                    rpc_method = %trace.inner.method,
                    phase = "request_schedule",
                    start_offset_us,
                    elapsed_us,
                    since_receive_us,
                    status = "succeeded",
                    "reverse JSON-RPC timing"
                );
            }
            ReverseRpcTimingEvent::HookCallback {
                trace,
                hook_type,
                start_offset_us,
                elapsed_us,
            } => {
                debug!(
                    target: REVERSE_RPC_TIMING_TARGET,
                    parent: None,
                    correlation_key = %trace.inner.correlation_key,
                    rpc_method = %trace.inner.method,
                    hook_type,
                    phase = "hook_callback",
                    start_offset_us,
                    elapsed_us,
                    status = "succeeded",
                    "reverse JSON-RPC timing"
                );
            }
            ReverseRpcTimingEvent::Complete {
                trace,
                required_phase_records: _,
                elapsed_us,
                succeeded,
            } => {
                debug!(
                    target: REVERSE_RPC_TIMING_TARGET,
                    parent: None,
                    correlation_key = %trace.inner.correlation_key,
                    rpc_method = %trace.inner.method,
                    phase = "request_complete",
                    start_offset_us = 0_u64,
                    elapsed_us,
                    status = if succeeded { "succeeded" } else { "failed" },
                    "reverse JSON-RPC timing"
                );
            }
        }
    }

    fn record_dropped_timing_records(dropped_records: &AtomicU64) {
        let dropped_records = dropped_records.swap(0, Ordering::Relaxed);
        if dropped_records > 0 {
            debug!(
                target: REVERSE_RPC_TIMING_TARGET,
                parent: None,
                phase = "records_dropped",
                dropped_records,
                status = "dropped",
                "reverse JSON-RPC timing records dropped"
            );
        }
    }

    /// Writer-actor task. Owns the `AsyncWrite`, drains the command queue,
    /// and writes each frame atomically (header + body + flush) before
    /// signaling the ack.
    ///
    /// Caller-side cancellation cannot interrupt a write in progress:
    /// dropping the ack `oneshot::Receiver` does not cancel the in-flight
    /// I/O. Once `WriteCommand` is enqueued the frame is committed to land
    /// on the wire (or surface an `io::Error` to the ack receiver if the
    /// transport is broken).
    ///
    /// Exits cleanly when all senders drop (channel closes), flushing any
    /// final buffered bytes.
    async fn write_loop(
        mut writer: impl AsyncWrite + Unpin + Send + 'static,
        mut rx: mpsc::UnboundedReceiver<WriteCommand>,
    ) {
        while let Some(WriteCommand {
            frame,
            ack,
            mut reverse_rpc,
            enqueued_at,
        }) = rx.recv().await
        {
            let queue_timing = enqueued_at.map(|enqueued_at| (enqueued_at, enqueued_at.elapsed()));
            let write_start = reverse_rpc.as_ref().map(|_| TokioInstant::now());
            let write_result = writer.write_all(&frame).await;
            let write_timing = write_start.map(|write_start| (write_start, write_start.elapsed()));
            let write_succeeded = write_result.is_ok();

            let (result, flush_timing) = match write_result {
                Ok(()) => {
                    let flush_start = reverse_rpc.as_ref().map(|_| TokioInstant::now());
                    let flush_result = writer.flush().await;
                    let flush_succeeded = flush_result.is_ok();
                    (
                        flush_result,
                        flush_start.map(|flush_start| {
                            (flush_start, flush_start.elapsed(), flush_succeeded)
                        }),
                    )
                }
                Err(error) => (Err(error), None),
            };
            let completed_at = reverse_rpc.as_ref().map(|_| TokioInstant::now());
            let succeeded = result.is_ok();

            // Caller may have dropped the ack receiver (e.g. their
            // `await` was cancelled); that's fine — we still completed
            // the write, which was the whole point.
            let _ = ack.send(result);

            if let Some(write_trace) = &mut reverse_rpc {
                let trace = &write_trace.trace;
                let (enqueued_at, queue_elapsed) =
                    queue_timing.expect("timed write must include its enqueue timestamp");
                let (write_start, write_elapsed) =
                    write_timing.expect("timed write must include its write timestamp");
                trace.record_phase("writer_queue", enqueued_at, queue_elapsed, true);
                trace.record_phase("write_all", write_start, write_elapsed, write_succeeded);
                if let Some((flush_start, flush_elapsed, flush_succeeded)) = flush_timing {
                    trace.record_phase("flush", flush_start, flush_elapsed, flush_succeeded);
                }
                write_trace.record_complete(
                    completed_at.expect("timed write must include its completion timestamp"),
                    succeeded,
                );
            }
        }
    }

    async fn read_loop(
        reader: impl AsyncRead + Unpin + Send,
        pending_requests: Arc<RwLock<HashMap<u64, PendingRequest>>>,
        reverse_requests: Option<ReverseRpcRegistry>,
        notification_tx: broadcast::Sender<JsonRpcNotification>,
        request_tx: ReverseRequestSender,
        timing: Option<ReverseRpcTimingEmitter>,
        correlation_hasher: Option<CorrelationHasher>,
    ) {
        let mut reader = BufReader::new(reader);

        loop {
            match Self::read_message(&mut reader).await {
                Ok(Some(message)) => match message {
                    JsonRpcMessage::Response(mut response) => {
                        let id = response.id;
                        let pending = pending_requests.write().remove(&id);
                        if let Some(PendingRequest {
                            sender,
                            inline_callback,
                        }) = pending
                        {
                            // Run the inline callback synchronously on the
                            // read loop so any state it mutates (e.g.
                            // registering a server-assigned session id with
                            // the router) is visible before the loop reads
                            // and dispatches the next message.
                            if let Some(cb) = inline_callback
                                && response.error.is_none()
                            {
                                let cb_outcome =
                                    std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
                                        cb(&response)
                                    }));
                                match cb_outcome {
                                    Ok(Ok(())) => {}
                                    Ok(Err(error)) => {
                                        response.result = None;
                                        response.error = Some(JsonRpcError {
                                            code: -32603,
                                            message: error.to_string(),
                                            data: None,
                                        });
                                    }
                                    Err(panic) => {
                                        let message = panic
                                            .downcast_ref::<&'static str>()
                                            .map(|s| (*s).to_string())
                                            .or_else(|| panic.downcast_ref::<String>().cloned())
                                            .unwrap_or_else(|| {
                                                "inline response callback panicked".to_string()
                                            });
                                        response.result = None;
                                        response.error = Some(JsonRpcError {
                                            code: -32603,
                                            message,
                                            data: None,
                                        });
                                    }
                                }
                            }
                            if sender.send(response).is_err() {
                                warn!(request_id = %id, "failed to send response for request");
                            }
                        } else {
                            warn!(request_id = %id, "received response for unknown request id");
                        }
                    }
                    JsonRpcMessage::Notification(notification) => {
                        let _ = notification_tx.send(notification);
                    }
                    JsonRpcMessage::Request(request) => {
                        let trace = if tracing::enabled!(
                            target: REVERSE_RPC_TIMING_TARGET,
                            tracing::Level::DEBUG
                        ) {
                            timing
                                .as_ref()
                                .zip(correlation_hasher.as_ref())
                                .zip(reverse_requests.as_ref())
                                .map(|((timing, correlation_hasher), registry)| {
                                    ReverseRpcTrace::new(
                                        &request,
                                        registry.next_generation(),
                                        TokioInstant::now(),
                                        correlation_hasher,
                                        timing.clone(),
                                    )
                                })
                        } else {
                            None
                        };
                        if let Some(trace) = &trace {
                            reverse_requests
                                .as_ref()
                                .expect("timed requests must have a registry")
                                .insert(trace.clone());
                        }
                        let forwarded = match &request_tx {
                            ReverseRequestSender::Public(request_tx) => {
                                request_tx.send(request).is_ok()
                            }
                            ReverseRequestSender::Internal(request_tx) => {
                                let request = ReverseRpcRequest::new(
                                    request,
                                    trace.clone(),
                                    reverse_requests.clone(),
                                );
                                let result = if let Some(trace) = &trace {
                                    trace.forward(|| request_tx.send(request))
                                } else {
                                    request_tx.send(request)
                                };
                                let forwarded = result.is_ok();
                                drop(result);
                                forwarded
                            }
                        };
                        if !forwarded {
                            warn!("failed to forward JSON-RPC request, channel closed");
                        }
                    }
                },
                Ok(None) => {
                    break;
                }
                Err(e) => {
                    error!(error = %e, "error reading from CLI");
                    break;
                }
            }
        }

        // Drain in-flight requests so callers observe cancellation
        // instead of hanging on a oneshot receiver.
        let mut pending = pending_requests.write();
        if !pending.is_empty() {
            warn!(
                count = pending.len(),
                "draining pending requests after read loop exit"
            );
            pending.clear();
        }
        if let Some(reverse_requests) = &reverse_requests {
            reverse_requests.abandon_all();
        }
    }

    async fn read_message(
        reader: &mut BufReader<impl AsyncRead + Unpin>,
    ) -> Result<Option<JsonRpcMessage>, Error> {
        let mut line = String::new();
        let mut content_length = None;

        loop {
            line.clear();
            if reader.read_line(&mut line).await? == 0 {
                return Ok(None);
            }

            let trimmed = line.trim();
            if trimmed.is_empty() {
                break;
            }

            if let Some(value) = trimmed.strip_prefix(CONTENT_LENGTH_HEADER) {
                content_length = Some(value.trim().parse::<usize>().map_err(|_| {
                    Error::from(ErrorKind::Protocol(
                        ProtocolErrorKind::InvalidContentLength(value.trim().to_string()),
                    ))
                })?);
            }
        }

        let Some(length) = content_length else {
            return Err(ErrorKind::Protocol(ProtocolErrorKind::MissingContentLength).into());
        };

        let mut body = vec![0u8; length];
        reader.read_exact(&mut body).await?;

        match serde_json::from_slice::<JsonRpcMessage>(&body) {
            Ok(message) => Ok(Some(message)),
            Err(error) => {
                // Dropping an undecodable frame could leave its pending
                // request waiting forever because this layer has no timeout.
                match repair_lone_surrogates(&body)
                    .and_then(|repaired| serde_json::from_slice::<JsonRpcMessage>(&repaired).ok())
                {
                    Some(message) => {
                        warn!(
                            error = %error,
                            length,
                            "recovered JSON-RPC frame containing unpaired UTF-16 surrogates"
                        );
                        Ok(Some(message))
                    }
                    None => Err(error.into()),
                }
            }
        }
    }

    /// Send a JSON-RPC request and wait for the matching response.
    ///
    /// # Cancel safety
    ///
    /// **Cancel-safe.** The frame is committed to the wire via the writer
    /// actor before this future yields; cancelling the await drops the
    /// response oneshot but does not desync the transport. The pending-
    /// requests map is cleaned up automatically (the `PendingGuard` drop
    /// removes the entry, and the read loop's response handling tolerates
    /// a missing entry).
    #[allow(dead_code, reason = "public API exported via crate::JsonRpcClient")]
    pub async fn send_request(
        &self,
        method: &str,
        params: Option<serde_json::Value>,
    ) -> Result<JsonRpcResponse, Error> {
        self.send_request_with_inline_callback(method, params, None)
            .await
    }

    /// Send a JSON-RPC request whose response is observed synchronously
    /// by the read loop *before* it is delivered to the awaiter.
    ///
    /// The optional `inline_callback` runs on the JSON-RPC read task the
    /// instant a successful response is parsed, and before the read loop
    /// dispatches the next message. This is the only way to perform
    /// client-side bookkeeping (for example, registering a server-
    /// assigned session id with the router) that must be visible to any
    /// notification or request that the server may emit on the same
    /// connection immediately after the response.
    ///
    /// If the callback returns an error or panics, that error is
    /// surfaced to the awaiter in place of the original response (the
    /// response payload is discarded and an internal-error JSON-RPC
    /// error is delivered instead). The error is never propagated back
    /// to the server and does not crash the read loop.
    pub(crate) async fn send_request_with_inline_callback(
        &self,
        method: &str,
        params: Option<serde_json::Value>,
        inline_callback: Option<InlineResponseCallback>,
    ) -> Result<JsonRpcResponse, Error> {
        let request_start = Instant::now();
        let id = self.request_id.fetch_add(1, Ordering::SeqCst);
        let request = JsonRpcRequest::new(id, method, params);

        let (tx, rx) = oneshot::channel();
        self.pending_requests.write().insert(
            id,
            PendingRequest {
                sender: tx,
                inline_callback,
            },
        );

        // RAII guard that removes the pending entry if this future is
        // dropped before the response arrives. Disarmed below before the
        // success return so the read loop owns the cleanup on the happy
        // path.
        let mut guard = PendingGuard {
            map: &self.pending_requests,
            id,
            armed: true,
        };

        // The PendingGuard's drop removes the entry on every error path
        // and on cancellation; disarmed below before the success return so
        // the read loop owns the cleanup on the happy path.
        if let Err(error) = self.write(&request).await {
            warn!(
                elapsed_ms = request_start.elapsed().as_millis(),
                method = %method,
                request_id = id,
                status = "failed",
                error = %error,
                "JsonRpcClient::send_request JSON-RPC request finished"
            );
            return Err(error);
        }

        let response = match rx.await {
            Ok(response) => response,
            Err(_) => {
                let error = ErrorKind::Protocol(ProtocolErrorKind::RequestCancelled).into();
                warn!(
                    elapsed_ms = request_start.elapsed().as_millis(),
                    method = %method,
                    request_id = id,
                    status = "failed",
                    error = %error,
                    "JsonRpcClient::send_request JSON-RPC request finished"
                );
                return Err(error);
            }
        };
        guard.disarm();
        if let Some(error) = &response.error {
            warn!(
                elapsed_ms = request_start.elapsed().as_millis(),
                method = %method,
                request_id = id,
                status = "failed",
                code = error.code,
                error = %error.message,
                "JsonRpcClient::send_request JSON-RPC request finished"
            );
        } else {
            debug!(
                elapsed_ms = request_start.elapsed().as_millis(),
                method = %method,
                request_id = id,
                status = "succeeded",
                "JsonRpcClient::send_request JSON-RPC request finished"
            );
        }
        Ok(response)
    }

    /// Write a Content-Length-framed JSON-RPC message to the transport.
    ///
    /// # Cancel safety
    ///
    /// **Cancel-safe.** Pre-serializes the body, enqueues it on the writer
    /// actor's command channel, and awaits an ack. Caller cancellation
    /// drops the ack receiver; the actor still completes the frame and
    /// flushes. A partial frame can never appear on the wire.
    pub async fn write<T: serde::Serialize>(&self, message: &T) -> Result<(), Error> {
        self.write_frame(message, None).await
    }

    pub(crate) async fn write_response(&self, response: &JsonRpcResponse) -> Result<(), Error> {
        let trace = REVERSE_RPC_RESPONSE_TRACE
            .try_with(|scoped| (scoped.request_id == response.id).then(|| scoped.trace.clone()))
            .ok()
            .flatten();
        let result = self.write_frame(response, trace.clone()).await;
        if let Some(trace) = &trace
            && let Some(reverse_requests) = &self.reverse_requests
        {
            let _ = reverse_requests.remove(trace);
        }
        result
    }

    async fn write_frame<T: serde::Serialize>(
        &self,
        message: &T,
        reverse_rpc: Option<ReverseRpcTrace>,
    ) -> Result<(), Error> {
        let encode_start = reverse_rpc.as_ref().map(|_| TokioInstant::now());
        let encoded = serde_json::to_vec(message);
        if let (Some(trace), Some(encode_start)) = (&reverse_rpc, encode_start) {
            trace.record_phase(
                "response_encode",
                encode_start,
                encode_start.elapsed(),
                encoded.is_ok(),
            );
            if encoded.is_err() {
                trace.record_complete(TokioInstant::now(), false);
            }
        }
        let body = encoded?;
        let mut frame = Vec::with_capacity(CONTENT_LENGTH_HEADER.len() + 16 + body.len() + 4);
        frame.extend_from_slice(CONTENT_LENGTH_HEADER.as_bytes());
        frame.extend_from_slice(body.len().to_string().as_bytes());
        frame.extend_from_slice(b"\r\n\r\n");
        frame.extend_from_slice(&body);

        let (ack_tx, ack_rx) = oneshot::channel();
        let enqueued_at = reverse_rpc.as_ref().map(|_| TokioInstant::now());
        if let Some(trace) = &reverse_rpc {
            trace.transfer_completion_to_writer();
        }
        if self
            .write_tx
            .send(WriteCommand {
                frame,
                ack: ack_tx,
                reverse_rpc: reverse_rpc.map(ReverseRpcWriteTrace::new),
                enqueued_at,
            })
            .is_err()
        {
            return Err(Error::from(std::io::Error::new(
                std::io::ErrorKind::BrokenPipe,
                "writer actor has shut down",
            )));
        }

        match ack_rx.await {
            Ok(Ok(())) => Ok(()),
            Ok(Err(e)) => Err(Error::from(e)),
            Err(_) => Err(Error::from(std::io::Error::new(
                std::io::ErrorKind::BrokenPipe,
                "writer actor dropped ack without responding",
            ))),
        }
    }
}

/// RAII guard that removes a pending-request entry from the map if the
/// owning future is dropped before the response arrives. Disarmed on the
/// happy path so the read loop's response handling owns the cleanup.
struct PendingGuard<'a> {
    map: &'a RwLock<HashMap<u64, PendingRequest>>,
    id: u64,
    armed: bool,
}

impl PendingGuard<'_> {
    fn disarm(&mut self) {
        self.armed = false;
    }
}

impl Drop for PendingGuard<'_> {
    fn drop(&mut self) {
        if self.armed {
            self.map.write().remove(&self.id);
        }
    }
}

#[cfg(test)]
mod tests {
    use std::collections::VecDeque;
    use std::future::Future;
    use std::io::{self, Write};
    use std::pin::Pin;
    use std::sync::Arc;
    use std::task::{Context, Poll};
    use std::time::Duration;

    use parking_lot::Mutex;
    use tokio::io::{AsyncWrite, AsyncWriteExt};
    use tokio::time::Sleep;
    use tracing_subscriber::Layer;
    use tracing_subscriber::fmt::MakeWriter;
    use tracing_subscriber::layer::SubscriberExt;

    use super::*;

    #[derive(Clone, Default)]
    struct TraceBuffer(Arc<Mutex<Vec<u8>>>);

    impl TraceBuffer {
        fn text(&self) -> String {
            String::from_utf8(self.0.lock().clone()).unwrap()
        }
    }

    impl Write for TraceBuffer {
        fn write(&mut self, buf: &[u8]) -> std::io::Result<usize> {
            self.0.lock().extend_from_slice(buf);
            Ok(buf.len())
        }

        fn flush(&mut self) -> std::io::Result<()> {
            Ok(())
        }
    }

    impl<'a> MakeWriter<'a> for TraceBuffer {
        type Writer = Self;

        fn make_writer(&'a self) -> Self::Writer {
            self.clone()
        }
    }

    struct DelayedWriter {
        write_delays: VecDeque<Duration>,
        flush_delays: VecDeque<Duration>,
        write_sleep: Option<Pin<Box<Sleep>>>,
        flush_sleep: Option<Pin<Box<Sleep>>>,
        started_tx: mpsc::UnboundedSender<&'static str>,
    }

    impl DelayedWriter {
        fn new(
            write_delays: [Duration; 2],
            flush_delays: [Duration; 2],
        ) -> (Self, mpsc::UnboundedReceiver<&'static str>) {
            let (started_tx, started_rx) = mpsc::unbounded_channel();
            (
                Self {
                    write_delays: write_delays.into(),
                    flush_delays: flush_delays.into(),
                    write_sleep: None,
                    flush_sleep: None,
                    started_tx,
                },
                started_rx,
            )
        }

        fn poll_delay(
            operation: &'static str,
            delay: &mut VecDeque<Duration>,
            sleep: &mut Option<Pin<Box<Sleep>>>,
            started_tx: &mpsc::UnboundedSender<&'static str>,
            cx: &mut Context<'_>,
        ) -> Poll<()> {
            if sleep.is_none() {
                let duration = delay.pop_front().unwrap_or_default();
                let _ = started_tx.send(operation);
                if duration.is_zero() {
                    return Poll::Ready(());
                }
                *sleep = Some(Box::pin(tokio::time::sleep(duration)));
            }

            match sleep
                .as_mut()
                .expect("delay sleep must exist")
                .as_mut()
                .poll(cx)
            {
                Poll::Ready(()) => {
                    *sleep = None;
                    Poll::Ready(())
                }
                Poll::Pending => Poll::Pending,
            }
        }
    }

    impl AsyncWrite for DelayedWriter {
        fn poll_write(
            mut self: Pin<&mut Self>,
            cx: &mut Context<'_>,
            buf: &[u8],
        ) -> Poll<std::io::Result<usize>> {
            let Self {
                write_delays,
                write_sleep,
                started_tx,
                ..
            } = self.as_mut().get_mut();
            match Self::poll_delay("write", write_delays, write_sleep, started_tx, cx) {
                Poll::Ready(()) => Poll::Ready(Ok(buf.len())),
                Poll::Pending => Poll::Pending,
            }
        }

        fn poll_flush(mut self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<std::io::Result<()>> {
            let Self {
                flush_delays,
                flush_sleep,
                started_tx,
                ..
            } = self.as_mut().get_mut();
            match Self::poll_delay("flush", flush_delays, flush_sleep, started_tx, cx) {
                Poll::Ready(()) => Poll::Ready(Ok(())),
                Poll::Pending => Poll::Pending,
            }
        }

        fn poll_shutdown(self: Pin<&mut Self>, _cx: &mut Context<'_>) -> Poll<std::io::Result<()>> {
            Poll::Ready(Ok(()))
        }
    }

    #[derive(Clone)]
    struct BlockingTraceWriter {
        entered_tx: std::sync::mpsc::Sender<()>,
        release: Arc<(std::sync::Mutex<bool>, std::sync::Condvar)>,
    }

    impl Write for BlockingTraceWriter {
        fn write(&mut self, buf: &[u8]) -> io::Result<usize> {
            let _ = self.entered_tx.send(());
            let (released, condvar) = &*self.release;
            let mut released = released.lock().unwrap();
            while !*released {
                released = condvar.wait(released).unwrap();
            }
            Ok(buf.len())
        }

        fn flush(&mut self) -> io::Result<()> {
            Ok(())
        }
    }

    impl<'a> MakeWriter<'a> for BlockingTraceWriter {
        type Writer = Self;

        fn make_writer(&'a self) -> Self::Writer {
            self.clone()
        }
    }

    fn trace_subscriber(buffer: TraceBuffer) -> impl tracing::Subscriber {
        tracing_subscriber::registry().with(
            tracing_subscriber::fmt::layer()
                .with_writer(buffer)
                .with_ansi(false)
                .without_time()
                .with_filter(tracing_subscriber::filter::filter_fn(|metadata| {
                    metadata.target() == REVERSE_RPC_TIMING_TARGET
                })),
        )
    }

    fn timing_channel(
        capacity: usize,
    ) -> (
        ReverseRpcTimingEmitter,
        mpsc::Receiver<ReverseRpcTimingEvent>,
        mpsc::Receiver<ReverseRpcTimingEvent>,
        Arc<AtomicU64>,
    ) {
        let (phase_tx, phase_rx) = mpsc::channel(capacity);
        let (terminal_tx, terminal_rx) = mpsc::channel(capacity);
        let dropped_records = Arc::new(AtomicU64::new(0));
        (
            ReverseRpcTimingEmitter {
                phase_tx,
                terminal_tx,
                dropped_records: dropped_records.clone(),
            },
            phase_rx,
            terminal_rx,
            dropped_records,
        )
    }

    async fn wait_for_trace(buffer: &TraceBuffer, needle: &str) {
        for _ in 0..20 {
            if buffer.text().contains(needle) {
                return;
            }
            tokio::task::yield_now().await;
        }
        panic!("timing trace did not contain {needle:?}: {}", buffer.text());
    }

    fn frame(message: &impl Serialize) -> Vec<u8> {
        let body = serde_json::to_vec(message).unwrap();
        format!("Content-Length: {}\r\n\r\n", body.len())
            .into_bytes()
            .into_iter()
            .chain(body)
            .collect()
    }

    #[test]
    fn deserialize_notification() {
        let json = r#"{"jsonrpc":"2.0","method":"session.event","params":{"id":"e1"}}"#;
        let msg: JsonRpcMessage = serde_json::from_str(json).unwrap();
        assert!(matches!(msg, JsonRpcMessage::Notification(n) if n.method == "session.event"));
    }

    #[test]
    fn deserialize_request() {
        let json =
            r#"{"jsonrpc":"2.0","id":5,"method":"permission.request","params":{"kind":"shell"}}"#;
        let msg: JsonRpcMessage = serde_json::from_str(json).unwrap();
        assert!(
            matches!(msg, JsonRpcMessage::Request(r) if r.id == 5 && r.method == "permission.request")
        );
    }

    #[test]
    fn deserialize_response_with_result() {
        let json = r#"{"jsonrpc":"2.0","id":3,"result":{"ok":true}}"#;
        let msg: JsonRpcMessage = serde_json::from_str(json).unwrap();
        assert!(matches!(msg, JsonRpcMessage::Response(r) if r.id == 3 && !r.is_error()));
    }

    #[test]
    fn deserialize_error_response() {
        let json =
            r#"{"jsonrpc":"2.0","id":7,"error":{"code":-32600,"message":"Invalid Request"}}"#;
        let msg: JsonRpcMessage = serde_json::from_str(json).unwrap();
        match msg {
            JsonRpcMessage::Response(r) => {
                assert!(r.is_error());
                let err = r.error.unwrap();
                assert_eq!(err.code, -32600);
                assert_eq!(err.message, "Invalid Request");
            }
            other => panic!("expected Response, got {other:?}"),
        }
    }

    #[test]
    fn deserialize_rejects_non_object() {
        let result = serde_json::from_str::<JsonRpcMessage>(r#""not an object""#);
        assert!(result.is_err());
    }

    #[test]
    fn request_new_sets_version() {
        let req = JsonRpcRequest::new(42, "test.method", None);
        assert_eq!(req.jsonrpc, "2.0");
        assert_eq!(req.id, 42);
        assert_eq!(req.method, "test.method");
        assert!(req.params.is_none());
    }

    #[test]
    fn request_serializes_camel_case() {
        let req = JsonRpcRequest::new(1, "ping", Some(serde_json::json!({})));
        let json = serde_json::to_string(&req).unwrap();
        assert!(json.contains(r#""jsonrpc":"2.0""#));
        assert!(json.contains(r#""id":1"#));
        assert!(json.contains(r#""method":"ping""#));
    }

    #[test]
    fn notification_without_params_omits_field() {
        let n = JsonRpcNotification {
            jsonrpc: "2.0".into(),
            method: "ping".into(),
            params: None,
        };
        let json = serde_json::to_string(&n).unwrap();
        assert!(!json.contains("params"));
    }

    #[test]
    fn response_without_error_omits_field() {
        let r = JsonRpcResponse {
            jsonrpc: "2.0".into(),
            id: 1,
            result: Some(serde_json::json!(true)),
            error: None,
        };
        let json = serde_json::to_string(&r).unwrap();
        assert!(!json.contains("error"));
    }

    #[test]
    fn reverse_request_correlation_is_stable_and_opaque() {
        let request = JsonRpcRequest::new(
            17,
            "hooks.invoke",
            Some(serde_json::json!({ "sessionId": "private-session-id" })),
        );
        let correlation_hasher = CorrelationHasher::new();
        let same = ReverseRpcTrace::correlation_key(
            &correlation_hasher,
            request.id,
            &request.method,
            Some("private-session-id"),
            7,
        );
        let repeated = ReverseRpcTrace::correlation_key(
            &correlation_hasher,
            request.id,
            &request.method,
            Some("private-session-id"),
            7,
        );
        let different_session = ReverseRpcTrace::correlation_key(
            &correlation_hasher,
            request.id,
            &request.method,
            Some("other-session"),
            7,
        );
        let different_generation = ReverseRpcTrace::correlation_key(
            &correlation_hasher,
            request.id,
            &request.method,
            Some("private-session-id"),
            8,
        );

        assert_eq!(same, repeated);
        assert_ne!(same, different_session);
        assert_ne!(same, different_generation);
        assert!(same.starts_with("rrpc-"));
        assert!(!same.contains("private-session-id"));
    }

    #[test]
    fn reverse_request_timing_labels_all_supported_hook_types() {
        for hook_type in [
            "preToolUse",
            "preMcpToolCall",
            "postToolUse",
            "postToolUseFailure",
            "userPromptSubmitted",
            "userPromptTransformed",
            "sessionStart",
            "sessionEnd",
            "errorOccurred",
            "agentStop",
        ] {
            assert_eq!(ReverseRpcTrace::timing_hook_type(hook_type), hook_type);
        }
        assert_eq!(
            ReverseRpcTrace::timing_hook_type("PRIVATE_SENTINEL_DO_NOT_TRACE"),
            "unknown"
        );
    }

    #[test]
    fn reverse_request_guard_only_removes_its_own_generation() {
        let (timing, _phase_rx, _terminal_rx, _dropped_records) = timing_channel(1);
        let request = JsonRpcRequest::new(17, "hooks.invoke", None);
        let now = TokioInstant::now();
        let correlation_hasher = CorrelationHasher::new();
        let first = ReverseRpcTrace::new(&request, 1, now, &correlation_hasher, timing.clone());
        let second = ReverseRpcTrace::new(&request, 2, now, &correlation_hasher, timing);
        let registry = ReverseRpcRegistry::new();
        registry.insert(first.clone());
        registry.insert(second.clone());
        let guard = ReverseRpcDispatchGuard {
            registry: registry.clone(),
            request_id: request.id,
            trace: first,
        };

        drop(guard);

        assert!(registry.contains(&second));
    }

    #[tokio::test(start_paused = true)]
    async fn reverse_request_timing_uses_the_forwarding_boundary() {
        let trace_buffer = TraceBuffer::default();
        let _subscriber = tracing::subscriber::set_default(trace_subscriber(trace_buffer.clone()));
        let (timing, phase_rx, terminal_rx, dropped_records) =
            timing_channel(REVERSE_RPC_TIMING_CAPACITY);
        tokio::spawn(JsonRpcClient::timing_loop(
            phase_rx,
            terminal_rx,
            dropped_records,
        ));
        let request = JsonRpcRequest::new(
            29,
            "hooks.invoke",
            Some(serde_json::json!({ "sessionId": "session" })),
        );
        let received_at = TokioInstant::now();
        let trace =
            ReverseRpcTrace::new(&request, 1, received_at, &CorrelationHasher::new(), timing);

        tokio::time::advance(Duration::from_millis(5)).await;
        trace.forward(|| Ok::<(), ()>(())).unwrap();
        tokio::time::advance(Duration::from_millis(7)).await;
        trace.record_scheduled(TokioInstant::now());

        wait_for_trace(&trace_buffer, "phase=\"request_schedule\"").await;
        let output = trace_buffer.text();
        let forward = output
            .lines()
            .find(|line| line.contains("phase=\"request_forward\""))
            .expect("request_forward timing should be emitted");
        let schedule = output
            .lines()
            .find(|line| line.contains("phase=\"request_schedule\""))
            .expect("request_schedule timing should be emitted");
        assert!(forward.contains("elapsed_us=5000"));
        assert!(forward.contains("start_offset_us=0"));
        assert!(schedule.contains("elapsed_us=7000"));
        assert!(schedule.contains("start_offset_us=5000"));
        assert!(schedule.contains("since_receive_us=12000"));
    }

    #[tokio::test]
    async fn reverse_request_forwarding_is_recorded_before_dispatch_can_start() {
        let trace_buffer = TraceBuffer::default();
        let _subscriber = tracing::subscriber::set_default(trace_subscriber(trace_buffer.clone()));
        let (timing, phase_rx, terminal_rx, dropped_records) =
            timing_channel(REVERSE_RPC_TIMING_CAPACITY);
        tokio::spawn(JsonRpcClient::timing_loop(
            phase_rx,
            terminal_rx,
            dropped_records,
        ));
        let request = JsonRpcRequest::new(31, "hooks.invoke", None);
        let now = TokioInstant::now();
        let trace = ReverseRpcTrace::new(&request, 1, now, &CorrelationHasher::new(), timing);
        let registry = ReverseRpcRegistry::new();
        registry.insert(trace.clone());
        let forwarded =
            ReverseRpcRequest::new(request, Some(trace.clone()), Some(registry.clone()));
        let (request_tx, request_rx) = std::sync::mpsc::channel::<ReverseRpcRequest>();
        let (received_tx, received_rx) = std::sync::mpsc::channel();
        let receiver = std::thread::spawn(move || {
            let request = request_rx.recv().unwrap();
            received_tx.send(()).unwrap();
            let (_request, guard) = request.into_dispatch();
            drop(guard);
        });

        trace
            .forward(|| {
                request_tx.send(forwarded).map_err(|_| ())?;
                received_rx.recv().map_err(|_| ())?;
                Ok::<(), ()>(())
            })
            .unwrap();
        receiver.join().unwrap();

        wait_for_trace(&trace_buffer, "phase=\"request_complete\"").await;
        let output = trace_buffer.text();
        let forward = output.find("phase=\"request_forward\"").unwrap();
        let schedule = output.find("phase=\"request_schedule\"").unwrap();
        let complete = output.find("phase=\"request_complete\"").unwrap();
        assert!(forward < schedule);
        assert!(schedule < complete);
        assert!(registry.is_empty());
    }

    #[tokio::test]
    async fn closed_forward_channel_records_failed_forward_before_abandonment() {
        let trace_buffer = TraceBuffer::default();
        let _subscriber = tracing::subscriber::set_default(trace_subscriber(trace_buffer.clone()));
        let (mut server, reader) = tokio::io::duplex(4096);
        let (notification_tx, _) = broadcast::channel(1);
        let (request_tx, request_rx) = mpsc::unbounded_channel();
        drop(request_rx);
        let client = JsonRpcClient::new_with_reverse_rpc_timing(
            tokio::io::sink(),
            reader,
            notification_tx,
            request_tx,
        );
        let request = JsonRpcRequest::new(32, "hooks.invoke", None);

        server.write_all(&frame(&request)).await.unwrap();

        wait_for_trace(&trace_buffer, "phase=\"request_complete\"").await;
        let output = trace_buffer.text();
        let forward = output
            .lines()
            .find(|line| line.contains("phase=\"request_forward\""))
            .expect("failed forwarding phase should be emitted");
        assert!(forward.contains("status=\"failed\""));
        assert!(
            output.find("phase=\"request_forward\"").unwrap()
                < output.find("phase=\"request_complete\"").unwrap()
        );

        client.force_close();
    }

    #[tokio::test]
    async fn public_client_does_not_retain_reverse_request_timing_state() {
        let (mut server, reader) = tokio::io::duplex(4096);
        let (notification_tx, _) = broadcast::channel(1);
        let (request_tx, mut request_rx) = mpsc::unbounded_channel();
        let client = JsonRpcClient::new(tokio::io::sink(), reader, notification_tx, request_tx);
        let request = JsonRpcRequest::new(23, "consumer.request", None);

        server.write_all(&frame(&request)).await.unwrap();
        let forwarded = request_rx.recv().await.unwrap();

        assert_eq!(forwarded.id, request.id);
        assert!(client.reverse_requests.is_none());
        assert!(client.timing_task.lock().is_none());
        client.force_close();
    }

    #[tokio::test]
    async fn disabled_timing_target_does_not_allocate_reverse_request_state() {
        let _subscriber =
            tracing::subscriber::set_default(tracing::subscriber::NoSubscriber::default());
        let (mut server, reader) = tokio::io::duplex(4096);
        let (notification_tx, _) = broadcast::channel(1);
        let (request_tx, mut request_rx) = mpsc::unbounded_channel();
        let client = JsonRpcClient::new_with_reverse_rpc_timing(
            tokio::io::sink(),
            reader,
            notification_tx,
            request_tx,
        );
        let request = JsonRpcRequest::new(
            29,
            "hooks.invoke",
            Some(serde_json::json!({ "sessionId": "session" })),
        );

        server.write_all(&frame(&request)).await.unwrap();
        let forwarded = request_rx.recv().await.unwrap();

        assert_eq!(forwarded.id, request.id);
        assert!(forwarded.trace.is_none());
        assert!(client.reverse_requests.is_none());
        assert!(client.timing_task.lock().is_none());
        client.force_close();
    }

    #[tokio::test]
    async fn saturated_timing_queue_drops_records_and_reports_the_count() {
        let trace_buffer = TraceBuffer::default();
        let _subscriber = tracing::subscriber::set_default(trace_subscriber(trace_buffer.clone()));
        let (timing, phase_rx, terminal_rx, dropped_records) = timing_channel(1);
        let request = JsonRpcRequest::new(37, "hooks.invoke", None);
        let now = TokioInstant::now();
        let trace = ReverseRpcTrace::new(&request, 1, now, &CorrelationHasher::new(), timing);
        trace.mark_forwarding(now);

        trace.record_phase("first", now, Duration::ZERO, true);
        trace.record_phase("second", now, Duration::ZERO, true);
        trace.record_complete(now, true);
        assert_eq!(
            trace.inner.timing.dropped_records.load(Ordering::Relaxed),
            1
        );
        drop(trace);
        tokio::spawn(JsonRpcClient::timing_loop(
            phase_rx,
            terminal_rx,
            dropped_records,
        ));

        wait_for_trace(&trace_buffer, "phase=\"request_complete\"").await;
        let output = trace_buffer.text();
        let dropped = output
            .lines()
            .find(|line| line.contains("phase=\"records_dropped\""))
            .expect("saturation diagnostic should be emitted");
        assert!(dropped.contains("dropped_records=1"));
        assert!(output.contains("phase=\"first\""));
        assert!(!output.contains("phase=\"second\""));
        assert!(output.contains("phase=\"request_complete\""));
        assert!(
            output.find("phase=\"first\"").unwrap()
                < output.find("phase=\"request_complete\"").unwrap()
        );
    }

    #[tokio::test]
    async fn saturated_terminal_queue_counts_one_writer_completion_drop_once() {
        let trace_buffer = TraceBuffer::default();
        let _subscriber = tracing::subscriber::set_default(trace_subscriber(trace_buffer.clone()));
        let (timing, phase_rx, terminal_rx, dropped_records) = timing_channel(1);
        let now = TokioInstant::now();
        let filler_request = JsonRpcRequest::new(38, "hooks.invoke", None);
        let filler = ReverseRpcTrace::new(
            &filler_request,
            1,
            now,
            &CorrelationHasher::new(),
            timing.clone(),
        );
        filler.mark_forwarding(now);
        assert!(filler.record_complete(now, true));

        let writer_request = JsonRpcRequest::new(39, "userInput.request", None);
        let writer =
            ReverseRpcTrace::new(&writer_request, 2, now, &CorrelationHasher::new(), timing);
        writer.mark_forwarding(now);
        let mut writer_trace = ReverseRpcWriteTrace::new(writer);
        writer_trace.record_complete(now, true);
        drop(writer_trace);

        assert_eq!(
            filler.inner.timing.dropped_records.load(Ordering::Relaxed),
            1
        );
        drop(filler);
        tokio::spawn(JsonRpcClient::timing_loop(
            phase_rx,
            terminal_rx,
            dropped_records,
        ));

        wait_for_trace(&trace_buffer, "phase=\"records_dropped\"").await;
        let output = trace_buffer.text();
        let dropped = output
            .lines()
            .find(|line| line.contains("phase=\"records_dropped\""))
            .expect("terminal saturation diagnostic should be emitted");
        assert!(dropped.contains("dropped_records=1"));
        assert!(output.contains("rpc_method=hooks.invoke"));
        assert!(!output.lines().any(|line| {
            line.contains("rpc_method=userInput.request")
                && line.contains("phase=\"request_complete\"")
        }));
    }

    #[tokio::test]
    async fn saturated_terminal_queue_counts_closed_writer_fallback_once() {
        let trace_buffer = TraceBuffer::default();
        let _subscriber = tracing::subscriber::set_default(trace_subscriber(trace_buffer.clone()));
        let (timing, phase_rx, terminal_rx, dropped_records) = timing_channel(1);
        let now = TokioInstant::now();
        let filler_request = JsonRpcRequest::new(40, "hooks.invoke", None);
        let filler = ReverseRpcTrace::new(
            &filler_request,
            1,
            now,
            &CorrelationHasher::new(),
            timing.clone(),
        );
        filler.mark_forwarding(now);
        assert!(filler.record_complete(now, true));

        let (notification_tx, _) = broadcast::channel(1);
        let (request_tx, _request_rx) = mpsc::unbounded_channel();
        let client = JsonRpcClient::new(
            tokio::io::sink(),
            tokio::io::empty(),
            notification_tx,
            request_tx,
        );
        let write_task = client
            .write_task
            .lock()
            .take()
            .expect("writer task should be running");
        write_task.abort();
        let _ = write_task.await;

        let writer_request = JsonRpcRequest::new(41, "userInput.request", None);
        let writer =
            ReverseRpcTrace::new(&writer_request, 2, now, &CorrelationHasher::new(), timing);
        writer.mark_forwarding(now);
        let error = client
            .write_frame(
                &JsonRpcResponse {
                    jsonrpc: "2.0".to_string(),
                    id: writer_request.id,
                    result: Some(serde_json::json!({})),
                    error: None,
                },
                Some(writer),
            )
            .await
            .unwrap_err();
        assert!(matches!(error.kind(), ErrorKind::Io));
        assert_eq!(
            filler.inner.timing.dropped_records.load(Ordering::Relaxed),
            1
        );
        drop(filler);
        tokio::spawn(JsonRpcClient::timing_loop(
            phase_rx,
            terminal_rx,
            dropped_records,
        ));

        wait_for_trace(&trace_buffer, "phase=\"records_dropped\"").await;
        let output = trace_buffer.text();
        let dropped = output
            .lines()
            .find(|line| line.contains("phase=\"records_dropped\""))
            .expect("closed-writer saturation diagnostic should be emitted");
        assert!(dropped.contains("dropped_records=1"));
        assert!(output.contains("rpc_method=hooks.invoke"));
        assert!(!output.lines().any(|line| {
            line.contains("rpc_method=userInput.request")
                && line.contains("phase=\"request_complete\"")
        }));

        client.force_close();
    }

    #[tokio::test(start_paused = true)]
    async fn reverse_request_timing_tracks_gated_scheduling_without_content() {
        const SENTINEL: &str = "PRIVATE_SENTINEL_DO_NOT_TRACE";

        let trace_buffer = TraceBuffer::default();
        let _subscriber = tracing::subscriber::set_default(trace_subscriber(trace_buffer.clone()));
        let (mut server, reader) = tokio::io::duplex(4096);
        let (notification_tx, _) = broadcast::channel(1);
        let (request_tx, mut request_rx) = mpsc::unbounded_channel();
        let client = JsonRpcClient::new_with_reverse_rpc_timing(
            tokio::io::sink(),
            reader,
            notification_tx,
            request_tx,
        );
        let request = JsonRpcRequest::new(
            41,
            SENTINEL,
            Some(serde_json::json!({
                "sessionId": SENTINEL,
                "hookType": "userPromptSubmitted",
                "input": {
                    "prompt": SENTINEL,
                    "cwd": SENTINEL,
                    "toolArgs": { "secret": SENTINEL }
                }
            })),
        );

        server.write_all(&frame(&request)).await.unwrap();
        let forwarded = request_rx.recv().await.unwrap();
        tokio::time::advance(Duration::from_millis(13)).await;

        let (forwarded, trace) = forwarded.into_dispatch();
        let trace = trace.expect("reverse request timing should be tracked");
        let callback_start = TokioInstant::now();
        tokio::time::advance(Duration::from_millis(3)).await;
        trace
            .trace()
            .record_hook_callback(SENTINEL, callback_start, callback_start.elapsed());
        trace
            .scope(client.write_response(&JsonRpcResponse {
                jsonrpc: "2.0".to_string(),
                id: forwarded.id,
                result: Some(serde_json::json!({ "output": SENTINEL })),
                error: None,
            }))
            .await
            .unwrap();

        wait_for_trace(&trace_buffer, "phase=\"request_complete\"").await;
        let output = trace_buffer.text();
        assert!(output.contains("github_copilot_sdk::reverse_rpc_timing"));
        assert!(output.contains("rpc_method=unknown"));
        assert!(output.contains("hook_type=\"unknown\""));
        assert!(output.contains("phase=\"request_forward\""));
        assert!(output.contains("phase=\"request_schedule\""));
        assert!(output.contains("elapsed_us=13000"));
        assert!(output.contains("phase=\"hook_callback\""));
        assert!(output.contains("phase=\"response_encode\""));
        assert!(output.contains("phase=\"writer_queue\""));
        assert!(output.contains("phase=\"write_all\""));
        assert!(output.contains("phase=\"flush\""));
        assert!(output.contains("phase=\"request_complete\""));
        assert!(output.contains("start_offset_us="));
        assert!(output.contains("correlation_key=rrpc-"));
        assert!(!output.contains(SENTINEL));

        client.force_close();
    }

    #[tokio::test(start_paused = true)]
    async fn reverse_response_timing_distinguishes_writer_queue_write_and_flush() {
        let trace_buffer = TraceBuffer::default();
        let _subscriber = tracing::subscriber::set_default(trace_subscriber(trace_buffer.clone()));
        let (writer, mut started_rx) = DelayedWriter::new(
            [Duration::from_millis(20), Duration::from_millis(7)],
            [Duration::ZERO, Duration::from_millis(11)],
        );
        let (notification_tx, _) = broadcast::channel(1);
        let (request_tx, _request_rx) = mpsc::unbounded_channel();
        let client = JsonRpcClient::new(writer, tokio::io::empty(), notification_tx, request_tx);

        let (first_ack_tx, first_ack_rx) = oneshot::channel();
        client
            .write_tx
            .send(WriteCommand {
                frame: frame(&serde_json::json!({})),
                ack: first_ack_tx,
                reverse_rpc: None,
                enqueued_at: None,
            })
            .unwrap();
        assert_eq!(started_rx.recv().await, Some("write"));
        tokio::time::advance(Duration::from_millis(5)).await;

        let request = JsonRpcRequest::new(
            7,
            "hooks.invoke",
            Some(serde_json::json!({ "sessionId": "session" })),
        );
        let now = TokioInstant::now();
        let trace = ReverseRpcTrace::for_test(&request, now, now);
        let (second_ack_tx, second_ack_rx) = oneshot::channel();
        client
            .write_tx
            .send(WriteCommand {
                frame: frame(&JsonRpcResponse {
                    jsonrpc: "2.0".to_string(),
                    id: 7,
                    result: Some(serde_json::json!({})),
                    error: None,
                }),
                ack: second_ack_tx,
                reverse_rpc: Some(ReverseRpcWriteTrace::new(trace)),
                enqueued_at: Some(TokioInstant::now()),
            })
            .unwrap();

        tokio::time::advance(Duration::from_millis(15)).await;
        assert_eq!(started_rx.recv().await, Some("flush"));
        assert_eq!(started_rx.recv().await, Some("write"));
        tokio::time::advance(Duration::from_millis(7)).await;
        assert_eq!(started_rx.recv().await, Some("flush"));
        tokio::time::advance(Duration::from_millis(11)).await;

        first_ack_rx.await.unwrap().unwrap();
        second_ack_rx.await.unwrap().unwrap();

        wait_for_trace(&trace_buffer, "phase=\"request_complete\"").await;
        let output = trace_buffer.text();
        assert!(output.contains("phase=\"writer_queue\" start_offset_us=0 elapsed_us=15000"));
        assert!(output.contains("phase=\"write_all\" start_offset_us=15000 elapsed_us=7000"));
        assert!(output.contains("phase=\"flush\" start_offset_us=22000 elapsed_us=11000"));
        assert!(output.contains("phase=\"request_complete\" start_offset_us=0 elapsed_us=33000"));
        let writer_queue = output.find("phase=\"writer_queue\"").unwrap();
        let write_all = output.find("phase=\"write_all\"").unwrap();
        let flush = output.find("phase=\"flush\"").unwrap();
        let complete = output.find("phase=\"request_complete\"").unwrap();
        assert!(writer_queue < write_all);
        assert!(write_all < flush);
        assert!(flush < complete);

        client.force_close();
    }

    #[tokio::test(start_paused = true)]
    async fn cancelled_response_keeps_the_writer_terminal_outcome() {
        let trace_buffer = TraceBuffer::default();
        let _subscriber = tracing::subscriber::set_default(trace_subscriber(trace_buffer.clone()));
        let (writer, mut started_rx) = DelayedWriter::new(
            [Duration::from_millis(10), Duration::ZERO],
            [Duration::ZERO, Duration::ZERO],
        );
        let (notification_tx, _) = broadcast::channel(1);
        let (request_tx, _request_rx) = mpsc::unbounded_channel();
        let (_server_guard, reader) = tokio::io::duplex(64);
        let client = Arc::new(JsonRpcClient::new(
            writer,
            reader,
            notification_tx,
            request_tx,
        ));
        let request = JsonRpcRequest::new(53, "hooks.invoke", None);
        let now = TokioInstant::now();
        let trace = ReverseRpcTrace::for_test(&request, now, now);
        let registry = ReverseRpcRegistry::new();
        registry.insert(trace.clone());
        let dispatch_guard = ReverseRpcDispatchGuard {
            registry: registry.clone(),
            request_id: request.id,
            trace,
        };
        let response_task = tokio::spawn({
            let client = client.clone();
            async move {
                dispatch_guard
                    .scope(client.write_response(&JsonRpcResponse {
                        jsonrpc: "2.0".to_string(),
                        id: request.id,
                        result: Some(serde_json::json!({})),
                        error: None,
                    }))
                    .await
            }
        });

        assert_eq!(started_rx.recv().await, Some("write"));
        response_task.abort();
        let _ = response_task.await;
        tokio::time::advance(Duration::from_millis(10)).await;
        assert_eq!(started_rx.recv().await, Some("flush"));

        wait_for_trace(&trace_buffer, "phase=\"request_complete\"").await;
        let complete = trace_buffer
            .text()
            .lines()
            .filter(|line| line.contains("phase=\"request_complete\""))
            .map(str::to_owned)
            .collect::<Vec<_>>();
        assert_eq!(complete.len(), 1);
        assert!(complete[0].contains("status=\"succeeded\""));
        assert!(registry.is_empty());

        client.force_close();
    }

    #[tokio::test]
    async fn response_timing_uses_the_exact_dispatch_generation_when_ids_are_reused() {
        let trace_buffer = TraceBuffer::default();
        let _subscriber = tracing::subscriber::set_default(trace_subscriber(trace_buffer.clone()));
        let (notification_tx, _) = broadcast::channel(1);
        let (request_tx, mut request_rx) = mpsc::unbounded_channel();
        let (mut server, reader) = tokio::io::duplex(4096);
        let client = JsonRpcClient::new_with_reverse_rpc_timing(
            tokio::io::sink(),
            reader,
            notification_tx,
            request_tx,
        );
        let params = Some(serde_json::json!({ "sessionId": "same-session" }));
        let first_request = JsonRpcRequest::new(61, "hooks.invoke", params.clone());
        let second_request = JsonRpcRequest::new(61, "hooks.invoke", params);
        server.write_all(&frame(&first_request)).await.unwrap();
        server.write_all(&frame(&second_request)).await.unwrap();
        let first_forwarded = request_rx.recv().await.unwrap();
        let second_forwarded = request_rx.recv().await.unwrap();

        // Acquire the newer dispatch first to prove scheduling order cannot
        // change which receipt-generation each forwarded request carries.
        let (second_forwarded, second_guard) = second_forwarded.into_dispatch();
        let second_guard = second_guard.expect("second request should carry timing");
        let second_trace = second_guard.trace.clone();
        let second_correlation = second_trace.inner.correlation_key.clone();
        let (first_forwarded, first_guard) = first_forwarded.into_dispatch();
        let first_guard = first_guard.expect("first request should carry timing");
        let first_correlation = first_guard.trace.inner.correlation_key.clone();
        assert_ne!(first_correlation, second_correlation);

        first_guard
            .scope(client.write_response(&JsonRpcResponse {
                jsonrpc: "2.0".to_string(),
                id: first_forwarded.id,
                result: Some(serde_json::json!({})),
                error: None,
            }))
            .await
            .unwrap();
        wait_for_trace(
            &trace_buffer,
            &format!(
                "correlation_key={first_correlation} rpc_method=hooks.invoke phase=\"request_complete\""
            ),
        )
        .await;
        assert!(
            client
                .reverse_requests
                .as_ref()
                .is_some_and(|registry| registry.contains(&second_trace))
        );

        second_guard
            .scope(client.write_response(&JsonRpcResponse {
                jsonrpc: "2.0".to_string(),
                id: second_forwarded.id,
                result: Some(serde_json::json!({})),
                error: None,
            }))
            .await
            .unwrap();
        wait_for_trace(
            &trace_buffer,
            &format!(
                "correlation_key={second_correlation} rpc_method=hooks.invoke phase=\"request_complete\""
            ),
        )
        .await;

        let output = trace_buffer.text();
        assert_eq!(
            output
                .lines()
                .filter(|line| {
                    line.contains(&format!("correlation_key={first_correlation}"))
                        && line.contains("phase=\"request_complete\"")
                })
                .count(),
            1
        );
        assert_eq!(
            output
                .lines()
                .filter(|line| {
                    line.contains(&format!("correlation_key={second_correlation}"))
                        && line.contains("phase=\"request_complete\"")
                })
                .count(),
            1
        );
        assert!(
            client
                .reverse_requests
                .as_ref()
                .is_some_and(ReverseRpcRegistry::is_empty)
        );

        client.force_close();
    }

    #[tokio::test(start_paused = true)]
    async fn force_close_emits_one_failed_terminal_record() {
        let trace_buffer = TraceBuffer::default();
        let _subscriber = tracing::subscriber::set_default(trace_subscriber(trace_buffer.clone()));
        let (writer, mut started_rx) = DelayedWriter::new(
            [Duration::from_secs(60), Duration::ZERO],
            [Duration::ZERO, Duration::ZERO],
        );
        let (mut server, reader) = tokio::io::duplex(4096);
        let (notification_tx, _) = broadcast::channel(1);
        let (request_tx, mut request_rx) = mpsc::unbounded_channel();
        let client = Arc::new(JsonRpcClient::new_with_reverse_rpc_timing(
            writer,
            reader,
            notification_tx,
            request_tx,
        ));
        let request = JsonRpcRequest::new(59, "hooks.invoke", None);
        server.write_all(&frame(&request)).await.unwrap();
        let forwarded = request_rx.recv().await.unwrap();
        let (forwarded, dispatch_guard) = forwarded.into_dispatch();
        let dispatch_guard =
            dispatch_guard.expect("enabled timing target should track the request");
        let response_task = tokio::spawn({
            let client = client.clone();
            async move {
                dispatch_guard
                    .scope(client.write_response(&JsonRpcResponse {
                        jsonrpc: "2.0".to_string(),
                        id: forwarded.id,
                        result: Some(serde_json::json!({})),
                        error: None,
                    }))
                    .await
            }
        });

        assert_eq!(started_rx.recv().await, Some("write"));
        client.force_close();
        assert!(response_task.await.unwrap().is_err());

        wait_for_trace(&trace_buffer, "phase=\"request_complete\"").await;
        let complete = trace_buffer
            .text()
            .lines()
            .filter(|line| line.contains("phase=\"request_complete\""))
            .map(str::to_owned)
            .collect::<Vec<_>>();
        assert_eq!(complete.len(), 1);
        assert!(complete[0].contains("status=\"failed\""));
    }

    #[tokio::test]
    async fn slow_timing_subscriber_does_not_delay_response_ack() {
        let (entered_tx, entered_rx) = std::sync::mpsc::channel();
        let release = Arc::new((std::sync::Mutex::new(false), std::sync::Condvar::new()));
        let blocking_writer = BlockingTraceWriter {
            entered_tx,
            release: release.clone(),
        };
        let (timing, phase_rx, terminal_rx, dropped_records) =
            timing_channel(REVERSE_RPC_TIMING_CAPACITY);
        let subscriber = tracing_subscriber::registry().with(
            tracing_subscriber::fmt::layer()
                .with_writer(blocking_writer)
                .with_ansi(false)
                .without_time()
                .with_filter(tracing_subscriber::filter::filter_fn(|metadata| {
                    metadata.target() == REVERSE_RPC_TIMING_TARGET
                })),
        );
        let timing_thread = tracing::subscriber::with_default(subscriber, || {
            JsonRpcClient::spawn_timing_thread(phase_rx, terminal_rx, dropped_records)
        });
        let (notification_tx, _) = broadcast::channel(1);
        let (request_tx, _request_rx) = mpsc::unbounded_channel();
        let client = JsonRpcClient::new(
            tokio::io::sink(),
            tokio::io::empty(),
            notification_tx,
            request_tx,
        );
        let request = JsonRpcRequest::new(53, "hooks.invoke", None);
        let now = TokioInstant::now();
        let trace = ReverseRpcTrace::new(&request, 1, now, &CorrelationHasher::new(), timing);
        trace.mark_forwarding(now);

        tokio::time::timeout(
            Duration::from_secs(1),
            client.write_frame(
                &JsonRpcResponse {
                    jsonrpc: "2.0".to_string(),
                    id: request.id,
                    result: Some(serde_json::json!({})),
                    error: None,
                },
                Some(trace),
            ),
        )
        .await
        .expect("response acknowledgement should not wait for trace formatting")
        .unwrap();
        entered_rx
            .recv_timeout(Duration::from_secs(1))
            .expect("timing subscriber should be blocked after acknowledgement");

        let (released, condvar) = &*release;
        *released.lock().unwrap() = true;
        condvar.notify_all();
        client.force_close();
        timing_thread.join().unwrap();
    }
}
