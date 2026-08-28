use std::collections::HashMap;
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
/// hooks dispatcher around `SessionHooks::on_hook` only.
const REVERSE_RPC_TIMING_TARGET: &str = "github_copilot_sdk::reverse_rpc_timing";

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
    reverse_rpc: Option<ReverseRpcTrace>,
    enqueued_at: TokioInstant,
}

enum ReverseRpcTimingEvent {
    Phase {
        trace: ReverseRpcTrace,
        phase: &'static str,
        elapsed_us: u64,
        succeeded: bool,
    },
    Scheduled {
        trace: ReverseRpcTrace,
        elapsed_us: u64,
        since_receive_us: u64,
    },
    HookCallback {
        trace: ReverseRpcTrace,
        hook_type: String,
        elapsed_us: u64,
    },
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
    method: String,
    received_at: TokioInstant,
    forwarded_at: TokioInstant,
    timing_tx: mpsc::UnboundedSender<ReverseRpcTimingEvent>,
}

impl ReverseRpcTrace {
    fn new(
        request: &JsonRpcRequest,
        received_at: TokioInstant,
        forwarded_at: TokioInstant,
        timing_tx: mpsc::UnboundedSender<ReverseRpcTimingEvent>,
    ) -> Self {
        let session_id = request
            .params
            .as_ref()
            .and_then(|params| params.get("sessionId"))
            .and_then(Value::as_str);
        Self {
            inner: Arc::new(ReverseRpcTraceInner {
                correlation_key: Self::correlation_key(request.id, &request.method, session_id),
                method: request.method.clone(),
                received_at,
                forwarded_at,
                timing_tx,
            }),
        }
    }

    #[cfg(test)]
    pub(crate) fn for_test(
        request: &JsonRpcRequest,
        received_at: TokioInstant,
        forwarded_at: TokioInstant,
    ) -> Self {
        let (timing_tx, timing_rx) = mpsc::unbounded_channel();
        tokio::spawn(JsonRpcClient::timing_loop(timing_rx));
        Self::new(request, received_at, forwarded_at, timing_tx)
    }

    fn correlation_key(request_id: u64, method: &str, session_id: Option<&str>) -> String {
        let mut hash = 0xcbf29ce484222325_u64;
        for byte in session_id
            .unwrap_or("<global>")
            .bytes()
            .chain([0xff])
            .chain(method.bytes())
            .chain([0xfe])
            .chain(request_id.to_le_bytes())
        {
            hash ^= u64::from(byte);
            hash = hash.wrapping_mul(0x100000001b3);
        }
        format!("rrpc-{hash:016x}")
    }

    fn elapsed_us(duration: std::time::Duration) -> u64 {
        u64::try_from(duration.as_micros()).unwrap_or(u64::MAX)
    }

    fn record_forwarded(&self, succeeded: bool) {
        self.record_phase(
            "request_forward",
            self.inner
                .forwarded_at
                .duration_since(self.inner.received_at),
            succeeded,
        );
    }

    fn record_scheduled(&self, scheduled_at: TokioInstant) {
        let _ = self.inner.timing_tx.send(ReverseRpcTimingEvent::Scheduled {
            trace: self.clone(),
            elapsed_us: Self::elapsed_us(scheduled_at.duration_since(self.inner.forwarded_at)),
            since_receive_us: Self::elapsed_us(scheduled_at.duration_since(self.inner.received_at)),
        });
    }

    pub(crate) fn record_hook_callback(&self, hook_type: &str, elapsed: std::time::Duration) {
        let _ = self
            .inner
            .timing_tx
            .send(ReverseRpcTimingEvent::HookCallback {
                trace: self.clone(),
                hook_type: hook_type.to_string(),
                elapsed_us: Self::elapsed_us(elapsed),
            });
    }

    fn record_phase(&self, phase: &'static str, elapsed: std::time::Duration, succeeded: bool) {
        let _ = self.inner.timing_tx.send(ReverseRpcTimingEvent::Phase {
            trace: self.clone(),
            phase,
            elapsed_us: Self::elapsed_us(elapsed),
            succeeded,
        });
    }
}

pub(crate) struct ReverseRpcDispatchGuard {
    reverse_requests: Arc<RwLock<HashMap<u64, ReverseRpcTrace>>>,
    request_id: u64,
    trace: ReverseRpcTrace,
}

impl ReverseRpcDispatchGuard {
    pub(crate) fn trace(&self) -> &ReverseRpcTrace {
        &self.trace
    }
}

impl Drop for ReverseRpcDispatchGuard {
    fn drop(&mut self) {
        remove_reverse_request_if_same(&self.reverse_requests, self.request_id, &self.trace);
    }
}

fn remove_reverse_request_if_same(
    reverse_requests: &RwLock<HashMap<u64, ReverseRpcTrace>>,
    request_id: u64,
    trace: &ReverseRpcTrace,
) {
    let mut reverse_requests = reverse_requests.write();
    if reverse_requests
        .get(&request_id)
        .is_some_and(|current| Arc::ptr_eq(&current.inner, &trace.inner))
    {
        reverse_requests.remove(&request_id);
    }
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
    reverse_requests: Arc<RwLock<HashMap<u64, ReverseRpcTrace>>>,
    notification_tx: broadcast::Sender<JsonRpcNotification>,
    request_tx: mpsc::UnboundedSender<JsonRpcRequest>,
    read_task: Mutex<Option<JoinHandle<()>>>,
    write_task: Mutex<Option<JoinHandle<()>>>,
    timing_task: Mutex<Option<JoinHandle<()>>>,
}

impl JsonRpcClient {
    /// Create a new client from async read/write streams.
    ///
    /// Spawns two background tasks: a reader that dispatches incoming
    /// messages to pending request channels, the notification broadcast,
    /// or the request-forwarding channel; and a writer actor that owns the
    /// underlying `AsyncWrite` and serializes frames atomically.
    pub fn new(
        writer: impl AsyncWrite + Unpin + Send + 'static,
        reader: impl AsyncRead + Unpin + Send + 'static,
        notification_tx: broadcast::Sender<JsonRpcNotification>,
        request_tx: mpsc::UnboundedSender<JsonRpcRequest>,
    ) -> Self {
        Self::new_inner(writer, reader, notification_tx, request_tx, false)
    }

    pub(crate) fn new_with_reverse_rpc_timing(
        writer: impl AsyncWrite + Unpin + Send + 'static,
        reader: impl AsyncRead + Unpin + Send + 'static,
        notification_tx: broadcast::Sender<JsonRpcNotification>,
        request_tx: mpsc::UnboundedSender<JsonRpcRequest>,
    ) -> Self {
        Self::new_inner(writer, reader, notification_tx, request_tx, true)
    }

    fn new_inner(
        writer: impl AsyncWrite + Unpin + Send + 'static,
        reader: impl AsyncRead + Unpin + Send + 'static,
        notification_tx: broadcast::Sender<JsonRpcNotification>,
        request_tx: mpsc::UnboundedSender<JsonRpcRequest>,
        trace_reverse_rpc: bool,
    ) -> Self {
        let (write_tx, write_rx) = mpsc::unbounded_channel::<WriteCommand>();

        let writer_span = tracing::error_span!("jsonrpc_write_loop");
        let write_task = tokio::spawn(Self::write_loop(writer, write_rx).instrument(writer_span));
        let (timing_tx, timing_task) = if trace_reverse_rpc {
            let (timing_tx, timing_rx) = mpsc::unbounded_channel::<ReverseRpcTimingEvent>();
            (
                Some(timing_tx),
                Some(tokio::spawn(Self::timing_loop(timing_rx))),
            )
        } else {
            (None, None)
        };

        let client = Self {
            request_id: AtomicU64::new(1),
            write_tx,
            pending_requests: Arc::new(RwLock::new(HashMap::new())),
            reverse_requests: Arc::new(RwLock::new(HashMap::new())),
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
                    timing_tx,
                )
                .await;
            }
            .instrument(reader_span),
        );
        *client.read_task.lock() = Some(read_task);

        client
    }

    pub(crate) fn force_close(&self) {
        if let Some(task) = self.read_task.lock().take() {
            task.abort();
        }
        if let Some(task) = self.write_task.lock().take() {
            task.abort();
        }
        if let Some(task) = self.timing_task.lock().take() {
            task.abort();
        }
        self.pending_requests.write().clear();
        self.reverse_requests.write().clear();
    }

    async fn timing_loop(mut rx: mpsc::UnboundedReceiver<ReverseRpcTimingEvent>) {
        while let Some(event) = rx.recv().await {
            match event {
                ReverseRpcTimingEvent::Phase {
                    trace,
                    phase,
                    elapsed_us,
                    succeeded,
                } => {
                    debug!(
                        target: REVERSE_RPC_TIMING_TARGET,
                        parent: None,
                        correlation_key = %trace.inner.correlation_key,
                        rpc_method = %trace.inner.method,
                        phase,
                        elapsed_us,
                        status = if succeeded { "succeeded" } else { "failed" },
                        "reverse JSON-RPC timing"
                    );
                }
                ReverseRpcTimingEvent::Scheduled {
                    trace,
                    elapsed_us,
                    since_receive_us,
                } => {
                    debug!(
                        target: REVERSE_RPC_TIMING_TARGET,
                        parent: None,
                        correlation_key = %trace.inner.correlation_key,
                        rpc_method = %trace.inner.method,
                        phase = "request_schedule",
                        elapsed_us,
                        since_receive_us,
                        status = "succeeded",
                        "reverse JSON-RPC timing"
                    );
                }
                ReverseRpcTimingEvent::HookCallback {
                    trace,
                    hook_type,
                    elapsed_us,
                } => {
                    debug!(
                        target: REVERSE_RPC_TIMING_TARGET,
                        parent: None,
                        correlation_key = %trace.inner.correlation_key,
                        rpc_method = %trace.inner.method,
                        hook_type,
                        phase = "hook_callback",
                        elapsed_us,
                        status = "succeeded",
                        "reverse JSON-RPC timing"
                    );
                }
            }
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
            reverse_rpc,
            enqueued_at,
        }) = rx.recv().await
        {
            let queue_elapsed = enqueued_at.elapsed();

            let write_start = TokioInstant::now();
            let write_result = writer.write_all(&frame).await;
            let write_elapsed = write_start.elapsed();
            let write_succeeded = write_result.is_ok();

            let (result, flush_timing) = match write_result {
                Ok(()) => {
                    let flush_start = TokioInstant::now();
                    let flush_result = writer.flush().await;
                    let flush_elapsed = flush_start.elapsed();
                    let flush_succeeded = flush_result.is_ok();
                    (flush_result, Some((flush_elapsed, flush_succeeded)))
                }
                Err(error) => (Err(error), None),
            };

            // Caller may have dropped the ack receiver (e.g. their
            // `await` was cancelled); that's fine — we still completed
            // the write, which was the whole point.
            let _ = ack.send(result);

            if let Some(trace) = &reverse_rpc {
                trace.record_phase("writer_queue", queue_elapsed, true);
                trace.record_phase("write_all", write_elapsed, write_succeeded);
                if let Some((flush_elapsed, flush_succeeded)) = flush_timing {
                    trace.record_phase("flush", flush_elapsed, flush_succeeded);
                }
            }
        }
    }

    async fn read_loop(
        reader: impl AsyncRead + Unpin + Send,
        pending_requests: Arc<RwLock<HashMap<u64, PendingRequest>>>,
        reverse_requests: Arc<RwLock<HashMap<u64, ReverseRpcTrace>>>,
        notification_tx: broadcast::Sender<JsonRpcNotification>,
        request_tx: mpsc::UnboundedSender<JsonRpcRequest>,
        timing_tx: Option<mpsc::UnboundedSender<ReverseRpcTimingEvent>>,
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
                        let request_id = request.id;
                        let trace = timing_tx.as_ref().map(|timing_tx| {
                            ReverseRpcTrace::new(
                                &request,
                                TokioInstant::now(),
                                TokioInstant::now(),
                                timing_tx.clone(),
                            )
                        });
                        if let Some(trace) = &trace {
                            reverse_requests.write().insert(request_id, trace.clone());
                        }
                        let forwarded = request_tx.send(request).is_ok();
                        if let Some(trace) = &trace {
                            trace.record_forwarded(forwarded);
                        }
                        if !forwarded {
                            reverse_requests.write().remove(&request_id);
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
        reverse_requests.write().clear();
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
        let trace = self.reverse_requests.read().get(&response.id).cloned();
        let result = self.write_frame(response, trace.clone()).await;
        if let Some(trace) = &trace {
            remove_reverse_request_if_same(&self.reverse_requests, response.id, trace);
        }
        result
    }

    async fn write_frame<T: serde::Serialize>(
        &self,
        message: &T,
        reverse_rpc: Option<ReverseRpcTrace>,
    ) -> Result<(), Error> {
        let encode_start = TokioInstant::now();
        let encoded = serde_json::to_vec(message);
        if let Some(trace) = &reverse_rpc {
            trace.record_phase("response_encode", encode_start.elapsed(), encoded.is_ok());
        }
        let body = encoded?;
        let mut frame = Vec::with_capacity(CONTENT_LENGTH_HEADER.len() + 16 + body.len() + 4);
        frame.extend_from_slice(CONTENT_LENGTH_HEADER.as_bytes());
        frame.extend_from_slice(body.len().to_string().as_bytes());
        frame.extend_from_slice(b"\r\n\r\n");
        frame.extend_from_slice(&body);

        let (ack_tx, ack_rx) = oneshot::channel();
        let enqueued_at = TokioInstant::now();
        self.write_tx
            .send(WriteCommand {
                frame,
                ack: ack_tx,
                reverse_rpc,
                enqueued_at,
            })
            .map_err(|_| {
                Error::from(std::io::Error::new(
                    std::io::ErrorKind::BrokenPipe,
                    "writer actor has shut down",
                ))
            })?;

        match ack_rx.await {
            Ok(Ok(())) => Ok(()),
            Ok(Err(e)) => Err(Error::from(e)),
            Err(_) => Err(Error::from(std::io::Error::new(
                std::io::ErrorKind::BrokenPipe,
                "writer actor dropped ack without responding",
            ))),
        }
    }

    pub(crate) fn trace_reverse_request_scheduled(
        &self,
        request_id: u64,
    ) -> Option<ReverseRpcDispatchGuard> {
        let trace = self.reverse_requests.read().get(&request_id).cloned();
        if let Some(trace) = &trace {
            trace.record_scheduled(TokioInstant::now());
        }
        trace.map(|trace| ReverseRpcDispatchGuard {
            reverse_requests: self.reverse_requests.clone(),
            request_id,
            trace,
        })
    }

    pub(crate) fn abandon_reverse_request(&self, request_id: u64) {
        self.reverse_requests.write().remove(&request_id);
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
    use std::io::Write;
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
        let same = ReverseRpcTrace::correlation_key(
            request.id,
            &request.method,
            Some("private-session-id"),
        );
        let repeated = ReverseRpcTrace::correlation_key(
            request.id,
            &request.method,
            Some("private-session-id"),
        );
        let different_session =
            ReverseRpcTrace::correlation_key(request.id, &request.method, Some("other-session"));

        assert_eq!(same, repeated);
        assert_ne!(same, different_session);
        assert!(same.starts_with("rrpc-"));
        assert!(!same.contains("private-session-id"));
    }

    #[test]
    fn reverse_request_guard_only_removes_its_own_generation() {
        let (timing_tx, _timing_rx) = mpsc::unbounded_channel();
        let request = JsonRpcRequest::new(17, "hooks.invoke", None);
        let now = TokioInstant::now();
        let first = ReverseRpcTrace::new(&request, now, now, timing_tx.clone());
        let second = ReverseRpcTrace::new(&request, now, now, timing_tx);
        let reverse_requests = Arc::new(RwLock::new(HashMap::new()));
        reverse_requests.write().insert(request.id, first.clone());
        let guard = ReverseRpcDispatchGuard {
            reverse_requests: reverse_requests.clone(),
            request_id: request.id,
            trace: first,
        };

        reverse_requests.write().insert(request.id, second.clone());
        drop(guard);

        let retained = reverse_requests
            .read()
            .get(&request.id)
            .cloned()
            .expect("new request generation should remain tracked");
        assert!(Arc::ptr_eq(&retained.inner, &second.inner));
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
        assert!(client.reverse_requests.read().is_empty());
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
            "hooks.invoke",
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

        let trace = client
            .trace_reverse_request_scheduled(forwarded.id)
            .expect("reverse request timing should be tracked");
        trace
            .trace()
            .record_hook_callback("userPromptSubmitted", Duration::from_millis(3));
        client
            .write_response(&JsonRpcResponse {
                jsonrpc: "2.0".to_string(),
                id: forwarded.id,
                result: Some(serde_json::json!({ "output": SENTINEL })),
                error: None,
            })
            .await
            .unwrap();

        wait_for_trace(&trace_buffer, "phase=\"flush\"").await;
        let output = trace_buffer.text();
        assert!(output.contains("github_copilot_sdk::reverse_rpc_timing"));
        assert!(output.contains("phase=\"request_forward\""));
        assert!(output.contains("phase=\"request_schedule\""));
        assert!(output.contains("elapsed_us=13000"));
        assert!(output.contains("phase=\"hook_callback\""));
        assert!(output.contains("phase=\"response_encode\""));
        assert!(output.contains("phase=\"writer_queue\""));
        assert!(output.contains("phase=\"write_all\""));
        assert!(output.contains("phase=\"flush\""));
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
                enqueued_at: TokioInstant::now(),
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
                reverse_rpc: Some(trace),
                enqueued_at: TokioInstant::now(),
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

        wait_for_trace(&trace_buffer, "phase=\"flush\"").await;
        let output = trace_buffer.text();
        assert!(output.contains("phase=\"writer_queue\" elapsed_us=15000"));
        assert!(output.contains("phase=\"write_all\" elapsed_us=7000"));
        assert!(output.contains("phase=\"flush\" elapsed_us=11000"));

        client.force_close();
    }
}
