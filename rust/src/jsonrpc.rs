use std::collections::HashMap;
use std::sync::Arc;
use std::sync::atomic::{AtomicU64, Ordering};

use serde::{Deserialize, Serialize};
use serde_json::Value;
use tokio::io::{AsyncBufReadExt, AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt, BufReader};
use tokio::sync::{Mutex, RwLock, broadcast, mpsc, oneshot};
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

/// Low-level JSON-RPC 2.0 client over Content-Length-framed streams.
pub struct JsonRpcClient {
    request_id: AtomicU64,
    writer: Arc<Mutex<Box<dyn AsyncWrite + Unpin + Send>>>,
    pending_requests: Arc<RwLock<HashMap<u64, oneshot::Sender<JsonRpcResponse>>>>,
    notification_tx: broadcast::Sender<JsonRpcNotification>,
    request_tx: mpsc::UnboundedSender<JsonRpcRequest>,
}

impl JsonRpcClient {
    /// Create a new client from async read/write streams.
    ///
    /// Spawns a background reader task that dispatches incoming messages to
    /// pending request channels, the notification broadcast, or the request
    /// forwarding channel.
    pub fn new(
        writer: impl AsyncWrite + Unpin + Send + 'static,
        reader: impl AsyncRead + Unpin + Send + 'static,
        notification_tx: broadcast::Sender<JsonRpcNotification>,
        request_tx: mpsc::UnboundedSender<JsonRpcRequest>,
    ) -> Self {
        let client = Self {
            request_id: AtomicU64::new(1),
            writer: Arc::new(Mutex::new(Box::new(writer))),
            pending_requests: Arc::new(RwLock::new(HashMap::new())),
            notification_tx,
            request_tx,
        };

        let pending_requests = client.pending_requests.clone();
        let notification_tx_clone = client.notification_tx.clone();
        let request_tx_clone = client.request_tx.clone();
        let span = tracing::error_span!("jsonrpc_read_loop");

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
            .instrument(span),
        );

        client
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
                        let mut pending = pending_requests.write().await;
                        if let Some(tx) = pending.remove(&id) {
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
        let mut pending = pending_requests.write().await;
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
    pub async fn send_request(
        &self,
        method: &str,
        params: Option<serde_json::Value>,
    ) -> Result<JsonRpcResponse, Error> {
        let id = self.request_id.fetch_add(1, Ordering::SeqCst);
        let request = JsonRpcRequest::new(id, method, params);

        let (tx, rx) = oneshot::channel();
        {
            let mut pending = self.pending_requests.write().await;
            pending.insert(id, tx);
        }

        if let Err(e) = self.write(&request).await {
            self.pending_requests.write().await.remove(&id);
            return Err(e);
        }

        let response = rx
            .await
            .map_err(|_| Error::Protocol(ProtocolError::RequestCancelled))?;
        Ok(response)
    }

    /// Write a Content-Length-framed JSON-RPC message to the transport.
    pub async fn write<T: serde::Serialize>(&self, message: &T) -> Result<(), Error> {
        let body = serde_json::to_vec(message)?;
        let header = format!("{}{}\r\n\r\n", CONTENT_LENGTH_HEADER, body.len());
        let mut writer = self.writer.lock().await;
        writer.write_all(header.as_bytes()).await?;
        writer.write_all(&body).await?;
        writer.flush().await?;
        Ok(())
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
