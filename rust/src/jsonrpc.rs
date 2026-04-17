use std::collections::HashMap;
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use std::sync::Arc;

use serde::{Deserialize, Serialize};
use serde_json::Value;
use tokio::io::{AsyncBufReadExt, AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt, BufReader};
use tokio::sync::{broadcast, mpsc, oneshot, Mutex};
use tracing::{error, warn, Instrument};

use crate::{Error, ProtocolError};

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct JsonRpcRequest {
    pub jsonrpc: String,
    pub id: u64,
    pub method: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub params: Option<Value>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct JsonRpcResponse {
    pub jsonrpc: String,
    pub id: u64,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<JsonRpcError>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcError {
    pub code: i32,
    pub message: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub data: Option<Value>,
}

/// Standard JSON-RPC 2.0 error codes.
pub mod error_codes {
    pub const METHOD_NOT_FOUND: i32 = -32601;
    pub const INVALID_PARAMS: i32 = -32602;
    pub const INTERNAL_ERROR: i32 = -32603;
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct JsonRpcNotification {
    pub jsonrpc: String,
    pub method: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub params: Option<Value>,
}

#[derive(Debug, Clone, Serialize)]
pub enum JsonRpcMessage {
    Request(JsonRpcRequest),
    Response(JsonRpcResponse),
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
    pub fn is_error(&self) -> bool {
        self.error.is_some()
    }
}

const CONTENT_LENGTH_HEADER: &str = "Content-Length: ";

pub struct JsonRpcClient {
    request_id: AtomicU64,
    writer: Arc<Mutex<Box<dyn AsyncWrite + Unpin + Send>>>,
    pending_requests: Arc<parking_lot::Mutex<HashMap<u64, oneshot::Sender<JsonRpcResponse>>>>,
    /// Set once the read loop has exited; subsequent `send_request` calls
    /// fail fast instead of hanging forever waiting for a response that will
    /// never arrive.
    closed: Arc<AtomicBool>,
    notification_tx: broadcast::Sender<JsonRpcNotification>,
    request_tx: mpsc::UnboundedSender<JsonRpcRequest>,
}

impl JsonRpcClient {
    pub fn new(
        writer: impl AsyncWrite + Unpin + Send + 'static,
        reader: impl AsyncRead + Unpin + Send + 'static,
        notification_tx: broadcast::Sender<JsonRpcNotification>,
        request_tx: mpsc::UnboundedSender<JsonRpcRequest>,
    ) -> Self {
        let client = Self {
            request_id: AtomicU64::new(1),
            writer: Arc::new(Mutex::new(Box::new(writer))),
            pending_requests: Arc::new(parking_lot::Mutex::new(HashMap::new())),
            closed: Arc::new(AtomicBool::new(false)),
            notification_tx,
            request_tx,
        };

        let pending_requests = client.pending_requests.clone();
        let closed = client.closed.clone();
        let notification_tx_clone = client.notification_tx.clone();
        let request_tx_clone = client.request_tx.clone();
        let span = tracing::error_span!("jsonrpc_read_loop");

        tokio::spawn(
            async move {
                Self::read_loop(
                    reader,
                    pending_requests,
                    closed,
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
        pending_requests: Arc<parking_lot::Mutex<HashMap<u64, oneshot::Sender<JsonRpcResponse>>>>,
        closed: Arc<AtomicBool>,
        notification_tx: broadcast::Sender<JsonRpcNotification>,
        request_tx: mpsc::UnboundedSender<JsonRpcRequest>,
    ) {
        let mut reader = BufReader::new(reader);

        loop {
            match Self::read_message(&mut reader).await {
                Ok(Some(message)) => match message {
                    JsonRpcMessage::Response(response) => {
                        let id = response.id;
                        let tx = pending_requests.lock().remove(&id);
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

        // Mark closed (release) so any concurrent `send_request` will see
        // it after re-checking, then drain in-flight requests so callers
        // observe cancellation instead of hanging on a oneshot receiver.
        closed.store(true, Ordering::Release);
        let drained: HashMap<u64, oneshot::Sender<JsonRpcResponse>> =
            std::mem::take(&mut *pending_requests.lock());
        if !drained.is_empty() {
            warn!(
                count = drained.len(),
                "draining pending requests after read loop exit"
            );
            // Senders dropped here; receivers will observe `RequestCancelled`.
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

    pub async fn send_request(
        &self,
        method: &str,
        params: Option<serde_json::Value>,
    ) -> Result<JsonRpcResponse, Error> {
        let id = self.request_id.fetch_add(1, Ordering::SeqCst);
        let request = JsonRpcRequest::new(id, method, params);

        let (tx, rx) = oneshot::channel();
        {
            let mut pending = self.pending_requests.lock();
            // Re-check after acquiring the lock: if the read loop has already
            // marked us closed (and drained), inserting now would hang.
            if self.closed.load(Ordering::Acquire) {
                return Err(Error::Protocol(ProtocolError::RequestCancelled));
            }
            pending.insert(id, tx);
        }

        if let Err(e) = self.write(&request).await {
            self.pending_requests.lock().remove(&id);
            return Err(e);
        }

        let response = rx
            .await
            .map_err(|_| Error::Protocol(ProtocolError::RequestCancelled))?;
        Ok(response)
    }

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
