use std::collections::HashMap;
use std::sync::Arc;
use std::sync::atomic::{AtomicU64, Ordering};

use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use serde_json::Value;
use tokio::io::{AsyncBufReadExt, AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt, BufReader};
use tokio::sync::{broadcast, mpsc, oneshot};
use tracing::{Instrument, error, warn};

use crate::{Error, ProtocolError};

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
    pending_requests: Arc<RwLock<HashMap<u64, oneshot::Sender<JsonRpcResponse>>>>,
    notification_tx: broadcast::Sender<JsonRpcNotification>,
    request_tx: mpsc::UnboundedSender<JsonRpcRequest>,
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
        let (write_tx, write_rx) = mpsc::unbounded_channel::<WriteCommand>();

        let writer_span = tracing::error_span!("jsonrpc_write_loop");
        tokio::spawn(Self::write_loop(writer, write_rx).instrument(writer_span));

        let client = Self {
            request_id: AtomicU64::new(1),
            write_tx,
            pending_requests: Arc::new(RwLock::new(HashMap::new())),
            notification_tx,
            request_tx,
        };

        let pending_requests = client.pending_requests.clone();
        let notification_tx_clone = client.notification_tx.clone();
        let request_tx_clone = client.request_tx.clone();
        let reader_span = tracing::error_span!("jsonrpc_read_loop");

        tokio::spawn(
            async move {
                Self::read_loop(
                    reader,
                    pending_requests,
                    notification_tx_clone,
                    request_tx_clone,
                )
                .await;
            }
            .instrument(reader_span),
        );

        client
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
        while let Some(WriteCommand { frame, ack }) = rx.recv().await {
            let result = async {
                writer.write_all(&frame).await?;
                writer.flush().await?;
                Ok::<_, std::io::Error>(())
            }
            .await;

            // Caller may have dropped the ack receiver (e.g. their
            // `await` was cancelled); that's fine — we still completed
            // the write, which was the whole point.
            let _ = ack.send(result);
        }
    }

    async fn read_loop(
        reader: impl AsyncRead + Unpin + Send,
        pending_requests: Arc<RwLock<HashMap<u64, oneshot::Sender<JsonRpcResponse>>>>,
        notification_tx: broadcast::Sender<JsonRpcNotification>,
        request_tx: mpsc::UnboundedSender<JsonRpcRequest>,
    ) {
        let mut reader = BufReader::new(reader);

        loop {
            match Self::read_message(&mut reader).await {
                Ok(Some(message)) => match message {
                    JsonRpcMessage::Response(response) => {
                        let id = response.id;
                        let tx = pending_requests.write().remove(&id);
                        if let Some(tx) = tx {
                            if tx.send(response).is_err() {
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
                        if request_tx.send(request).is_err() {
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
                    Error::Protocol(ProtocolError::InvalidContentLength(
                        value.trim().to_string(),
                    ))
                })?);
            }
        }

        let Some(length) = content_length else {
            return Err(Error::Protocol(ProtocolError::MissingContentLength));
        };

        let mut body = vec![0u8; length];
        reader.read_exact(&mut body).await?;

        let message: JsonRpcMessage = serde_json::from_slice(&body)?;
        Ok(Some(message))
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
    pub async fn send_request(
        &self,
        method: &str,
        params: Option<serde_json::Value>,
    ) -> Result<JsonRpcResponse, Error> {
        let id = self.request_id.fetch_add(1, Ordering::SeqCst);
        let request = JsonRpcRequest::new(id, method, params);

        let (tx, rx) = oneshot::channel();
        self.pending_requests.write().insert(id, tx);

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
        self.write(&request).await?;

        let response = rx
            .await
            .map_err(|_| Error::Protocol(ProtocolError::RequestCancelled))?;
        guard.disarm();
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
        let body = serde_json::to_vec(message)?;
        let mut frame = Vec::with_capacity(CONTENT_LENGTH_HEADER.len() + 16 + body.len() + 4);
        frame.extend_from_slice(CONTENT_LENGTH_HEADER.as_bytes());
        frame.extend_from_slice(body.len().to_string().as_bytes());
        frame.extend_from_slice(b"\r\n\r\n");
        frame.extend_from_slice(&body);

        let (ack_tx, ack_rx) = oneshot::channel();
        self.write_tx
            .send(WriteCommand { frame, ack: ack_tx })
            .map_err(|_| {
                Error::Io(std::io::Error::new(
                    std::io::ErrorKind::BrokenPipe,
                    "writer actor has shut down",
                ))
            })?;

        match ack_rx.await {
            Ok(Ok(())) => Ok(()),
            Ok(Err(e)) => Err(Error::Io(e)),
            Err(_) => Err(Error::Io(std::io::Error::new(
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
    map: &'a RwLock<HashMap<u64, oneshot::Sender<JsonRpcResponse>>>,
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
    use super::*;

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
}
