use std::collections::HashMap;
use std::collections::hash_map::Entry;
use std::sync::Arc;
use std::sync::atomic::{AtomicU64, Ordering};
use std::time::Instant;

use parking_lot::{Mutex, RwLock};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use tokio::io::{AsyncBufReadExt, AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt, BufReader};
use tokio::sync::{broadcast, mpsc, oneshot};
use tokio::task::JoinHandle;
use tokio_util::sync::CancellationToken;
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

pub(crate) type RequestHandler = Arc<
    dyn Fn(Value) -> futures_util::future::BoxFuture<'static, Result<Value, Error>> + Send + Sync,
>;
type RequestHandlers = Arc<RwLock<HashMap<String, RequestHandler>>>;

fn remote_error_log_message<'a>(method: &str, message: &'a str) -> &'a str {
    // Listener negotiation carries a token that a remote error may echo.
    match method {
        "host.getConfiguration" | "host.ready" => "listener negotiation request rejected",
        _ => message,
    }
}

/// Internal pairing of the response delivery channel with an optional
/// inline callback that the read loop runs synchronously before delivery.
///
/// The channel carries the response, or the error that ends the request
/// without one (see [`JsonRpcMessage::UncorrelatedError`]). An uncorrelated
/// rejection takes the channel but leaves a request with an inline callback
/// registered, so a later successful response still runs the callback.
struct PendingRequest {
    sender: Option<oneshot::Sender<Result<JsonRpcResponse, Error>>>,
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
    /// Numeric error code. The CLI's codes are 64-bit, so the wire type is too.
    pub code: i64,
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
    /// Request cancelled by the peer's `$/cancelRequest` (-32800).
    pub const REQUEST_CANCELLED: i32 = -32800;
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

/// A parsed JSON-RPC 2.0 message — request, response, notification, or an
/// error the peer could not correlate with a request.
#[derive(Debug, Clone, Serialize)]
pub enum JsonRpcMessage {
    /// An incoming or outgoing request.
    Request(JsonRpcRequest),
    /// A response to a previous request.
    Response(JsonRpcResponse),
    /// A fire-and-forget notification.
    Notification(JsonRpcNotification),
    /// An error response whose `id` is `null`: the CLI rejected a frame it
    /// could not identify (for example, one its parser could not read), so the
    /// error names no request.
    UncorrelatedError(JsonRpcError),
}

/// Custom deserializer that dispatches based on field presence instead of
/// `#[serde(untagged)]` which tries each variant sequentially (3× parse
/// attempts for Notification — the hot-path streaming variant).
///
/// Dispatch logic:
/// - has `id` + has `method` → Request
/// - `id` is `null`, no `method` → UncorrelatedError (it must carry an `error`)
/// - has `id` + no `method` → Response
/// - no `id`                → Notification
impl<'de> Deserialize<'de> for JsonRpcMessage {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let mut value = Value::deserialize(deserializer)?;
        let obj = value
            .as_object_mut()
            .ok_or_else(|| serde::de::Error::custom("expected a JSON object"))?;

        let has_id = obj.contains_key("id");
        let has_method = obj.contains_key("method");

        // Preserve the owned payload instead of rebuilding its JSON containers
        // while serde validates the envelope. Optional null payloads remain None.
        let payload_key = if has_id && !has_method {
            "result"
        } else {
            "params"
        };
        let payload = obj.remove(payload_key).filter(|value| !value.is_null());

        if has_id && has_method {
            JsonRpcRequest::deserialize(value)
                .map(|mut request| {
                    request.params = payload;
                    JsonRpcMessage::Request(request)
                })
                .map_err(serde::de::Error::custom)
        } else if obj.get("id").is_some_and(Value::is_null) {
            if !obj.get("jsonrpc").is_some_and(Value::is_string) {
                return Err(serde::de::Error::custom(
                    "a response with a null id must name its jsonrpc version",
                ));
            }
            let error = obj.remove("error").ok_or_else(|| {
                serde::de::Error::custom("a response with a null id must carry an error")
            })?;
            JsonRpcError::deserialize(error)
                .map(JsonRpcMessage::UncorrelatedError)
                .map_err(serde::de::Error::custom)
        } else if has_id {
            JsonRpcResponse::deserialize(value)
                .map(|mut response| {
                    response.result = payload;
                    JsonRpcMessage::Response(response)
                })
                .map_err(serde::de::Error::custom)
        } else {
            JsonRpcNotification::deserialize(value)
                .map(|mut notification| {
                    notification.params = payload;
                    JsonRpcMessage::Notification(notification)
                })
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

    /// Whether this request nests arrays and objects deeper than the CLI
    /// reads: a frame deeper than [`PEER_MAX_NESTING`], counting the request
    /// object itself. The CLI could not identify such a frame, so it would
    /// answer with an error whose `id` is `null`.
    pub(crate) fn nests_too_deep_for_peer(&self) -> bool {
        self.params
            .as_ref()
            .is_some_and(|params| nests_deeper_than(params, PEER_MAX_NESTING - 1))
    }
}

impl JsonRpcResponse {
    /// Returns `true` if this response contains an error.
    #[allow(dead_code)]
    pub fn is_error(&self) -> bool {
        self.error.is_some()
    }

    /// Whether this response nests arrays and objects deeper than the CLI
    /// reads: a frame deeper than [`PEER_MAX_NESTING`], counting the response
    /// object itself, which the CLI would refuse with an error whose `id` is
    /// `null`, leaving its own request unanswered.
    pub(crate) fn nests_too_deep_for_peer(&self) -> bool {
        self.result
            .as_ref()
            .is_some_and(|result| nests_deeper_than(result, PEER_MAX_NESTING - 1))
            || self
                .error
                .as_ref()
                .and_then(|error| error.data.as_ref())
                .is_some_and(|data| nests_deeper_than(data, PEER_MAX_NESTING - 2))
    }

    /// The error response this client sends for the same request instead of a
    /// response [`nests_too_deep_for_peer`](Self::nests_too_deep_for_peer), so
    /// the CLI's request completes and the connection stays open.
    pub(crate) fn nesting_error(&self) -> Self {
        warn!(
            request_id = self.id,
            limit = PEER_MAX_NESTING,
            "a response nests deeper than the CLI reads; answering its request with an error"
        );
        Self {
            jsonrpc: self.jsonrpc.clone(),
            id: self.id,
            result: None,
            error: Some(JsonRpcError {
                code: error_codes::INTERNAL_ERROR.into(),
                message: format!(
                    "the response nests deeper than {PEER_MAX_NESTING} levels of arrays and objects, which the CLI cannot read"
                ),
                data: None,
            }),
        }
    }
}

/// The deepest nesting of arrays and objects a frame can carry for the CLI to
/// read it. The CLI parses every frame with `serde_json::from_slice`
/// (`IncomingMessage::from_slice`), whose default recursion limit of 128 fails
/// on entering the 128th nested array or object, so 127 levels read and 128
/// do not. The tests pin this to serde_json's behaviour.
pub(crate) const PEER_MAX_NESTING: usize = 127;

/// Whether `value` nests arrays and objects more than `levels` deep (a scalar
/// is 0 levels, an array or object one more than its deepest member). Stops at
/// the first branch past the limit, and allocates nothing.
pub(crate) fn nests_deeper_than(value: &Value, levels: usize) -> bool {
    match value {
        Value::Array(items) => {
            levels == 0 || items.iter().any(|item| nests_deeper_than(item, levels - 1))
        }
        Value::Object(members) => {
            levels == 0
                || members
                    .values()
                    .any(|member| nests_deeper_than(member, levels - 1))
        }
        _ => false,
    }
}

const CONTENT_LENGTH_HEADER: &str = "Content-Length: ";

/// The most characters of a peer's error message the SDK repeats in a log or
/// an error.
const PEER_MESSAGE_MAX_CHARS: usize = 200;

/// A peer's error message, safe to repeat: every span in double quotes or
/// backticks, where a decoder may quote the value it refused, becomes `"…"` or
/// `` `…` ``, and the result is cut to [`PEER_MESSAGE_MAX_CHARS`] characters,
/// followed by an ellipsis.
fn sanitized_peer_message(message: &str) -> String {
    let mut out = String::new();
    let mut quote = None;
    let mut escaped = false;
    for c in message.chars() {
        if let Some(open) = quote {
            if escaped {
                escaped = false;
            } else if c == '\\' {
                escaped = true;
            } else if c == open {
                quote = None;
            }
        } else if c == '"' || c == '`' {
            quote = Some(c);
            out.push(c);
            out.push('…');
            out.push(c);
        } else {
            out.push(c);
        }
    }
    if let Some((cut, _)) = out.char_indices().nth(PEER_MESSAGE_MAX_CHARS) {
        out.truncate(cut);
        out.push('…');
    }
    out
}

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
/// `frame` is the fully serialized frame (header + body, written as
/// [`EncodedFrame::as_bytes`]); the caller pays the serde cost synchronously
/// before enqueueing so the actor never sees a `Result` from JSON encoding.
/// When present, `ack` resolves once the bytes have been
/// fully written and flushed (or the underlying I/O reports an error). If
/// the caller drops the `oneshot::Receiver`, the actor still completes the
/// frame — caller cancellation cannot desync the wire.
struct WriteCommand {
    frame: EncodedFrame,
    ack: Option<oneshot::Sender<Result<(), std::io::Error>>>,
}

/// Room reserved in front of a frame's body for its header: the header's name,
/// the 20 digits of the longest `usize`, and the blank line.
const FRAME_PREFIX: usize = CONTENT_LENGTH_HEADER.len() + 20 + 4;

/// One JSON-RPC frame, header and body, in a single buffer: the header is
/// written into room reserved in front of the body and ends right where the
/// body starts.
pub(crate) struct EncodedFrame {
    buf: Vec<u8>,
    start: usize,
}

impl EncodedFrame {
    /// The frame as it goes on the wire: the header, then the body.
    pub(crate) fn as_bytes(&self) -> &[u8] {
        &self.buf[self.start..]
    }
}

/// Encodes one JSON-RPC frame, header and body, into a single buffer.
///
/// The body is serialized once, behind room reserved for the header, and the
/// header is then written into that room. A large message, such as a
/// `sessionFs.readFile` reply carrying a long session's `events.jsonl`, is held
/// once, escaped, instead of as a serialized body plus a framed copy of it. The
/// buffer grows by doubling, as `serde_json::to_vec`'s does. A frame can then
/// wait in the writer's queue, so spare capacity beyond 4 KiB or a quarter of
/// the frame, whichever is larger, is given back first.
pub(crate) fn encode_frame<T: serde::Serialize>(message: &T) -> Result<EncodedFrame, Error> {
    // `serde_json::to_vec` starts at 128 bytes; the body gets the same room here.
    let mut buf = Vec::with_capacity(FRAME_PREFIX + 128);
    buf.extend_from_slice(&[0; FRAME_PREFIX]);
    serde_json::to_writer(&mut buf, message)?;
    let digits = (buf.len() - FRAME_PREFIX).to_string();
    let start = FRAME_PREFIX - CONTENT_LENGTH_HEADER.len() - digits.len() - 4;
    let (name, rest) = buf[start..FRAME_PREFIX].split_at_mut(CONTENT_LENGTH_HEADER.len());
    let (length, blank_line) = rest.split_at_mut(digits.len());
    name.copy_from_slice(CONTENT_LENGTH_HEADER.as_bytes());
    length.copy_from_slice(digits.as_bytes());
    blank_line.copy_from_slice(b"\r\n\r\n");
    // Doubling can leave the buffer up to twice the frame's size: a long string
    // is written in one piece, and its closing quote then doubles the buffer.
    if buf.capacity() - buf.len() > (buf.len() / 4).max(4096) {
        buf.shrink_to_fit();
    }
    Ok(EncodedFrame { buf, start })
}

/// Inbound requests that honor `$/cancelRequest`.
///
/// The read loop registers each one synchronously before forwarding it. This
/// preserves request/cancellation ordering across the router's separate queues.
#[derive(Default)]
pub(crate) struct CancellableRequests {
    pending: Mutex<HashMap<u64, Arc<CancellationToken>>>,
}

impl CancellableRequests {
    fn honors_cancellation(method: &str) -> bool {
        use crate::generated::api_types::rpc_methods;

        matches!(
            method,
            crate::installation_confirmation::CONFIRM_METHOD
                | rpc_methods::SKILLPROVIDER_LIST
                | rpc_methods::SKILLPROVIDER_READ
        )
    }

    fn register(&self, id: u64) -> bool {
        let mut pending = self.pending.lock();
        match pending.entry(id) {
            Entry::Vacant(entry) => {
                entry.insert(Arc::new(CancellationToken::new()));
                true
            }
            Entry::Occupied(_) => false,
        }
    }

    fn cancel(&self, id: u64) {
        if let Some(token) = self.pending.lock().get(&id) {
            token.cancel();
        }
    }

    fn clear(&self) {
        self.pending.lock().clear();
    }

    /// Take ownership of a registered request's cancellation until the
    /// returned guard drops, or `None` if the connection already retired it.
    pub(crate) fn claim(self: &Arc<Self>, id: u64) -> Option<PendingCancellation> {
        let cancellation = self.pending.lock().get(&id)?.clone();
        Some(PendingCancellation {
            requests: self.clone(),
            id,
            cancellation,
        })
    }
}

pub(crate) struct PendingCancellation {
    requests: Arc<CancellableRequests>,
    id: u64,
    cancellation: Arc<CancellationToken>,
}

impl PendingCancellation {
    /// Cancelled when the runtime sends `$/cancelRequest` for this request.
    pub(crate) fn cancellation(&self) -> &CancellationToken {
        &self.cancellation
    }
}

impl Drop for PendingCancellation {
    fn drop(&mut self) {
        let mut pending = self.requests.pending.lock();
        if pending
            .get(&self.id)
            .is_some_and(|token| Arc::ptr_eq(token, &self.cancellation))
        {
            pending.remove(&self.id);
        }
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
    notification_tx: broadcast::Sender<JsonRpcNotification>,
    request_tx: mpsc::UnboundedSender<JsonRpcRequest>,
    request_handlers: RequestHandlers,
    connection_closed: CancellationToken,
    pub(crate) cancellable_requests: Arc<CancellableRequests>,
    read_task: Mutex<Option<JoinHandle<()>>>,
    write_task: Mutex<Option<JoinHandle<()>>>,
}

impl JsonRpcClient {
    /// Create a new client from async read/write streams.
    ///
    /// Spawns two background tasks: a reader that dispatches incoming
    /// messages to pending request channels, the notification broadcast,
    /// or the request-forwarding channel; and a writer actor that owns the
    /// underlying `AsyncWrite` and serializes frames atomically.
    #[cfg(any(test, feature = "test-support"))]
    pub fn new(
        writer: impl AsyncWrite + Unpin + Send + 'static,
        reader: impl AsyncRead + Unpin + Send + 'static,
        notification_tx: broadcast::Sender<JsonRpcNotification>,
        request_tx: mpsc::UnboundedSender<JsonRpcRequest>,
    ) -> Self {
        Self::new_with_host_notifications(writer, reader, notification_tx, request_tx, None)
    }

    pub(crate) fn new_with_host_notifications(
        writer: impl AsyncWrite + Unpin + Send + 'static,
        reader: impl AsyncRead + Unpin + Send + 'static,
        notification_tx: broadcast::Sender<JsonRpcNotification>,
        request_tx: mpsc::UnboundedSender<JsonRpcRequest>,
        host_notifications: Option<mpsc::UnboundedSender<JsonRpcNotification>>,
    ) -> Self {
        let (write_tx, write_rx) = mpsc::unbounded_channel::<WriteCommand>();
        let connection_closed = CancellationToken::new();

        let writer_span = tracing::error_span!("jsonrpc_write_loop");
        let write_task = tokio::spawn(
            Self::write_loop(writer, write_rx, connection_closed.clone()).instrument(writer_span),
        );

        let client = Self {
            request_id: AtomicU64::new(1),
            write_tx,
            pending_requests: Arc::new(RwLock::new(HashMap::new())),
            notification_tx,
            request_tx,
            request_handlers: Arc::new(RwLock::new(HashMap::new())),
            connection_closed,
            cancellable_requests: Arc::new(CancellableRequests::default()),
            read_task: Mutex::new(None),
            write_task: Mutex::new(Some(write_task)),
        };

        let pending_requests = client.pending_requests.clone();
        let notification_tx_clone = client.notification_tx.clone();
        let request_tx_clone = client.request_tx.clone();
        let connection_closed = client.connection_closed.clone();
        let cancellable_requests = client.cancellable_requests.clone();
        let request_handlers = client.request_handlers.clone();
        let write_tx = client.write_tx.clone();
        let reader_span = tracing::error_span!("jsonrpc_read_loop");

        let read_task = tokio::spawn(
            async move {
                Self::read_loop(
                    reader,
                    pending_requests,
                    (notification_tx_clone, host_notifications),
                    request_tx_clone,
                    request_handlers,
                    write_tx,
                    (connection_closed, cancellable_requests),
                )
                .await;
            }
            .instrument(reader_span),
        );
        *client.read_task.lock() = Some(read_task);

        client
    }

    pub(crate) fn force_close(&self) {
        self.connection_closed.cancel();
        self.cancellable_requests.clear();
        let handlers = std::mem::take(&mut *self.request_handlers.write());
        drop(handlers);
        if let Some(task) = self.read_task.lock().take() {
            task.abort();
        }
        self.close_writer();
        self.pending_requests.write().clear();
    }

    /// Release stdin while continuing to drain the owned child's final stdout.
    pub(crate) fn close_writer(&self) {
        if let Some(task) = self.write_task.lock().take() {
            task.abort();
        }
    }

    pub(crate) fn connection_closed_token(&self) -> CancellationToken {
        self.connection_closed.child_token()
    }

    pub(crate) fn register_request_handler(
        &self,
        method: &str,
        handler: RequestHandler,
    ) -> Result<(), Error> {
        let mut handlers = self.request_handlers.write();
        if method.is_empty() || self.connection_closed.is_cancelled() {
            return Err(Error::with_message(
                ErrorKind::InvalidConfig,
                "Request handlers require a nonempty method and an open connection",
            ));
        }
        if handlers.contains_key(method) {
            return Err(Error::with_message(
                ErrorKind::InvalidConfig,
                format!("A request handler is already registered for {method}"),
            ));
        }
        handlers.insert(method.to_owned(), handler);
        Ok(())
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
        connection_closed: CancellationToken,
    ) {
        while let Some(WriteCommand { frame, ack }) = rx.recv().await {
            let result = async {
                writer.write_all(frame.as_bytes()).await?;
                writer.flush().await?;
                Ok::<_, std::io::Error>(())
            }
            .await;
            // Caller may have dropped the ack receiver (e.g. their
            // `await` was cancelled); that's fine — we still completed
            // the write, which was the whole point.
            match ack {
                Some(ack) => {
                    let _ = ack.send(result);
                }
                None => {
                    if let Err(error) = result {
                        error!(%error, "router rejection write failed; closing connection");
                        connection_closed.cancel();
                        break;
                    }
                }
            }
        }
    }

    async fn read_loop(
        reader: impl AsyncRead + Unpin + Send,
        pending_requests: Arc<RwLock<HashMap<u64, PendingRequest>>>,
        notifications: (
            broadcast::Sender<JsonRpcNotification>,
            Option<mpsc::UnboundedSender<JsonRpcNotification>>,
        ),
        request_tx: mpsc::UnboundedSender<JsonRpcRequest>,
        request_handlers: RequestHandlers,
        write_tx: mpsc::UnboundedSender<WriteCommand>,
        connection: (CancellationToken, Arc<CancellableRequests>),
    ) {
        let mut reader = BufReader::new(reader);
        let (connection_closed, cancellable_requests) = connection;

        loop {
            let message = tokio::select! {
                biased;
                _ = connection_closed.cancelled() => break,
                message = Self::read_message(&mut reader) => message,
            };
            match message {
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
                            match sender {
                                Some(sender) => {
                                    if sender.send(Ok(response)).is_err() {
                                        warn!(request_id = %id, "failed to send response for request");
                                    }
                                }
                                // An uncorrelated rejection already failed the
                                // caller; the callback above still applied an
                                // accepted request.
                                None => {
                                    debug!(request_id = %id, "late response to a request already failed by an uncorrelated rejection");
                                }
                            }
                        } else {
                            warn!(request_id = %id, "received response for unknown request id");
                        }
                    }
                    JsonRpcMessage::UncorrelatedError(error) => {
                        Self::fail_pending_requests(&pending_requests, &error);
                    }
                    JsonRpcMessage::Notification(notification) => {
                        if notification.method == "$/cancelRequest" {
                            if let Some(id) = notification
                                .params
                                .as_ref()
                                .and_then(|params| params.get("id"))
                                .and_then(Value::as_u64)
                            {
                                cancellable_requests.cancel(id);
                            } else {
                                warn!("invalid numeric request cancellation");
                            }
                        }
                        if matches!(
                            notification.method.as_str(),
                            "host.exited" | "host.sessionReleased"
                        ) && let Some(hosts) = &notifications.1
                        {
                            let _ = hosts.send(notification.clone());
                        }
                        let _ = notifications.0.send(notification);
                    }
                    JsonRpcMessage::Request(request) => {
                        if CancellableRequests::honors_cancellation(&request.method)
                            && !cancellable_requests.register(request.id)
                        {
                            warn!(method = %request.method, "duplicate pending cancellable request ID");
                            break;
                        }
                        let handler = request_handlers.read().get(&request.method).cloned();
                        if let Some(handler) = handler {
                            let write_tx = write_tx.clone();
                            let closed = connection_closed.clone();
                            // Internal handlers may register request state before
                            // the reader dispatches a following notification.
                            let response = handler(request.params.unwrap_or(Value::Null));
                            tokio::spawn(async move {
                                let result = tokio::select! {
                                    biased;
                                    _ = closed.cancelled() => return,
                                    result = response => result,
                                };
                                let (result, error) = match result {
                                    Ok(value) => (Some(value), None),
                                    Err(error) => (
                                        None,
                                        Some(JsonRpcError {
                                            code: error_codes::INTERNAL_ERROR.into(),
                                            message: error.to_string(),
                                            data: None,
                                        }),
                                    ),
                                };
                                let mut response = JsonRpcResponse {
                                    jsonrpc: "2.0".into(),
                                    id: request.id,
                                    result,
                                    error,
                                };
                                if response.nests_too_deep_for_peer() {
                                    response = response.nesting_error();
                                }
                                if let Err(error) = Self::write_message(&write_tx, &response).await
                                {
                                    warn!(%error, "failed to send connection request response");
                                }
                            });
                        } else if request_tx.send(request).is_err() {
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
        connection_closed.cancel();
        cancellable_requests.clear();
        // A handler may own the last Client clone, whose drop closes the RPC.
        // Release the registry lock before dropping those captured values.
        let handlers = std::mem::take(&mut *request_handlers.write());
        drop(handlers);

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
    }

    /// Ends every pending request with the CLI's rejection of a frame it could
    /// not identify, and keeps the connection.
    ///
    /// A `null`-id error names no request, and nothing else in it says which
    /// pending request, if any, the CLI rejected: the rejected frame may also
    /// have been a notification or a response. Leaving the pending requests in
    /// place would keep the rejected one's caller waiting forever, because this
    /// layer has no timeout. So every pending request fails with
    /// [`ProtocolErrorKind::UncorrelatedRejection`], which carries the CLI's
    /// code and its sanitized message. The cost is that a request the CLI is
    /// still handling fails too. The connection stays open for the requests
    /// that follow.
    ///
    /// A request with an inline callback stays registered without its caller:
    /// the callback keeps this client's state in step with the CLI's
    /// (`Session::set_tools` installs the handlers the CLI accepted), so a
    /// later successful response still runs it. Any other request is removed,
    /// and its later response is dropped as answering an unknown request id.
    fn fail_pending_requests(
        pending_requests: &RwLock<HashMap<u64, PendingRequest>>,
        error: &JsonRpcError,
    ) {
        let message = sanitized_peer_message(&error.message);
        let senders: Vec<oneshot::Sender<Result<JsonRpcResponse, Error>>> = {
            let mut pending = pending_requests.write();
            let senders = pending
                .values_mut()
                .filter_map(|request| request.sender.take())
                .collect();
            pending.retain(|_, request| request.inline_callback.is_some());
            senders
        };
        warn!(
            code = error.code,
            peer_message = %message,
            failed_requests = senders.len(),
            "the CLI rejected a frame it could not identify; failing every pending request"
        );
        for sender in senders {
            let rejection = ProtocolErrorKind::UncorrelatedRejection {
                code: error.code,
                message: message.clone(),
            };
            let _ = sender.send(Err(rejection.into()));
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
    pub(crate) fn send_request_with_inline_callback(
        &self,
        method: &str,
        params: Option<serde_json::Value>,
        inline_callback: Option<InlineResponseCallback>,
    ) -> impl std::future::Future<Output = Result<JsonRpcResponse, Error>> + Send + 'static {
        let id = self.request_id.fetch_add(1, Ordering::SeqCst);
        Self::send_owned_request(
            self.pending_requests.clone(),
            self.write_tx.clone(),
            JsonRpcRequest::new(id, method, params),
            inline_callback,
        )
    }

    // Own the request bookkeeping, not the connection. A pending response may
    // outlive its caller without preventing the last Client from closing I/O.
    async fn send_owned_request(
        pending_requests: Arc<RwLock<HashMap<u64, PendingRequest>>>,
        write_tx: mpsc::UnboundedSender<WriteCommand>,
        request: JsonRpcRequest,
        inline_callback: Option<InlineResponseCallback>,
    ) -> Result<JsonRpcResponse, Error> {
        let request_start = Instant::now();
        let id = request.id;
        let method = request.method.as_str();
        // A frame the CLI cannot read draws a null-id rejection, which fails
        // every pending request on the connection, so fail only this one.
        if request.nests_too_deep_for_peer() {
            let error = ErrorKind::Protocol(ProtocolErrorKind::RequestTooDeep {
                method: method.to_string(),
                limit: PEER_MAX_NESTING,
            })
            .into();
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
        let (tx, rx) = oneshot::channel();
        pending_requests.write().insert(
            id,
            PendingRequest {
                sender: Some(tx),
                inline_callback,
            },
        );

        // RAII guard that removes the pending entry if this future is
        // dropped before the response arrives. Disarmed below before the
        // success return so the read loop owns the cleanup on the happy
        // path.
        let mut guard = PendingGuard {
            map: &pending_requests,
            id,
            armed: true,
        };

        // The PendingGuard's drop removes the entry on every error path
        // and on cancellation; disarmed below before the success return so
        // the read loop owns the cleanup on the happy path.
        if let Err(error) = Self::write_message(&write_tx, &request).await {
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
            Ok(Ok(response)) => response,
            Ok(Err(error)) => {
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
                error = %remote_error_log_message(method, &error.message),
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
        Self::write_message(&self.write_tx, message).await
    }

    /// Commit a router rejection to the existing writer actor without retaining
    /// a delivery waiter. The actor reports write failure and closes the connection.
    pub(crate) fn enqueue_response(&self, response: &JsonRpcResponse) -> Result<(), Error> {
        Self::enqueue_message(&self.write_tx, response, None)
    }

    async fn write_message<T: serde::Serialize>(
        write_tx: &mpsc::UnboundedSender<WriteCommand>,
        message: &T,
    ) -> Result<(), Error> {
        let frame = encode_frame(message)?;
        Self::send_frame(write_tx, frame).await
    }

    /// Send a frame already encoded with [`encode_frame`] and wait for the
    /// writer actor's acknowledgement, so a caller can drop what it encoded
    /// from before the frame is written.
    pub(crate) async fn write_frame(&self, frame: EncodedFrame) -> Result<(), Error> {
        Self::send_frame(&self.write_tx, frame).await
    }

    async fn send_frame(
        write_tx: &mpsc::UnboundedSender<WriteCommand>,
        frame: EncodedFrame,
    ) -> Result<(), Error> {
        let (ack_tx, ack_rx) = oneshot::channel();
        Self::enqueue_frame(write_tx, frame, Some(ack_tx))?;
        match ack_rx.await {
            Ok(Ok(())) => Ok(()),
            Ok(Err(e)) => Err(Error::from(e)),
            Err(_) => Err(Error::from(std::io::Error::new(
                std::io::ErrorKind::BrokenPipe,
                "writer actor dropped ack without responding",
            ))),
        }
    }

    fn enqueue_message<T: serde::Serialize>(
        write_tx: &mpsc::UnboundedSender<WriteCommand>,
        message: &T,
        ack: Option<oneshot::Sender<Result<(), std::io::Error>>>,
    ) -> Result<(), Error> {
        let frame = encode_frame(message)?;
        Self::enqueue_frame(write_tx, frame, ack)
    }

    /// Hand an encoded frame to the writer actor. When present, `ack` resolves
    /// once the frame is flushed.
    fn enqueue_frame(
        write_tx: &mpsc::UnboundedSender<WriteCommand>,
        frame: EncodedFrame,
        ack: Option<oneshot::Sender<Result<(), std::io::Error>>>,
    ) -> Result<(), Error> {
        write_tx.send(WriteCommand { frame, ack }).map_err(|_| {
            Error::from(std::io::Error::new(
                std::io::ErrorKind::BrokenPipe,
                "writer actor has shut down",
            ))
        })?;

        Ok(())
    }
}

impl Drop for JsonRpcClient {
    fn drop(&mut self) {
        self.force_close();
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
            let mut map = self.map.write();
            // An entry an uncorrelated rejection kept for its inline callback
            // (no caller left) stays until its response or the connection's end.
            if map
                .get(&self.id)
                .is_some_and(|request| request.sender.is_some())
            {
                map.remove(&self.id);
            }
        }
    }
}

#[cfg(test)]
mod tests;
