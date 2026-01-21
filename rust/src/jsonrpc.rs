//! JSON-RPC 2.0 client implementation.

use crate::error::{CopilotError, JsonRpcError, Result};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::collections::HashMap;
use std::sync::Arc;
use tokio::io::{AsyncBufReadExt, AsyncReadExt, AsyncWriteExt, BufReader};
use tokio::sync::{mpsc, oneshot, Mutex, RwLock};

/// JSON-RPC 2.0 request.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcRequest {
    pub jsonrpc: String,
    pub id: Value,
    pub method: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub params: Option<Value>,
}

/// JSON-RPC 2.0 response.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcResponse {
    pub jsonrpc: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub id: Option<Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<JsonRpcErrorResponse>,
}

/// JSON-RPC 2.0 error in response.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcErrorResponse {
    pub code: i32,
    pub message: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub data: Option<Value>,
}

/// JSON-RPC 2.0 notification.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcNotification {
    pub jsonrpc: String,
    pub method: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub params: Option<Value>,
}

/// Notification handler type.
pub type NotificationHandler = Arc<dyn Fn(String, Value) + Send + Sync>;

/// Request handler type - handles incoming requests from the server.
pub type RequestHandler =
    Arc<dyn Fn(Value) -> Result<Value> + Send + Sync>;

/// Internal message types for the write loop.
enum WriteMessage {
    Send(Vec<u8>),
    Stop,
}

/// JSON-RPC client for stdio/TCP transport with Content-Length framing.
pub struct JsonRpcClient {
    write_tx: mpsc::Sender<WriteMessage>,
    pending_requests: Arc<RwLock<HashMap<String, oneshot::Sender<JsonRpcResponse>>>>,
    notification_handler: Arc<RwLock<Option<NotificationHandler>>>,
    request_handlers: Arc<RwLock<HashMap<String, RequestHandler>>>,
    running: Arc<std::sync::atomic::AtomicBool>,
    response_write_tx: Arc<Mutex<Option<mpsc::Sender<WriteMessage>>>>,
}

impl JsonRpcClient {
    /// Create a new JSON-RPC client from async read/write streams.
    pub fn new<R, W>(reader: R, writer: W) -> Self
    where
        R: tokio::io::AsyncRead + Unpin + Send + 'static,
        W: tokio::io::AsyncWrite + Unpin + Send + 'static,
    {
        let (write_tx, write_rx) = mpsc::channel::<WriteMessage>(100);
        let pending_requests: Arc<RwLock<HashMap<String, oneshot::Sender<JsonRpcResponse>>>> =
            Arc::new(RwLock::new(HashMap::new()));
        let notification_handler: Arc<RwLock<Option<NotificationHandler>>> =
            Arc::new(RwLock::new(None));
        let request_handlers: Arc<RwLock<HashMap<String, RequestHandler>>> =
            Arc::new(RwLock::new(HashMap::new()));
        let running = Arc::new(std::sync::atomic::AtomicBool::new(true));

        let client = Self {
            write_tx: write_tx.clone(),
            pending_requests: pending_requests.clone(),
            notification_handler: notification_handler.clone(),
            request_handlers: request_handlers.clone(),
            running: running.clone(),
            response_write_tx: Arc::new(Mutex::new(Some(write_tx.clone()))),
        };

        // Spawn write loop
        let running_write = running.clone();
        tokio::spawn(async move {
            Self::write_loop(writer, write_rx, running_write).await;
        });

        // Spawn read loop
        let write_tx_for_read = write_tx;
        tokio::spawn(async move {
            Self::read_loop(
                reader,
                pending_requests,
                notification_handler,
                request_handlers,
                running,
                write_tx_for_read,
            )
            .await;
        });

        client
    }

    /// Write loop - sends messages to the writer.
    async fn write_loop<W>(
        mut writer: W,
        mut write_rx: mpsc::Receiver<WriteMessage>,
        running: Arc<std::sync::atomic::AtomicBool>,
    ) where
        W: tokio::io::AsyncWrite + Unpin,
    {
        while running.load(std::sync::atomic::Ordering::SeqCst) {
            match write_rx.recv().await {
                Some(WriteMessage::Send(data)) => {
                    // Write Content-Length header + message
                    let header = format!("Content-Length: {}\r\n\r\n", data.len());
                    if writer.write_all(header.as_bytes()).await.is_err() {
                        break;
                    }
                    if writer.write_all(&data).await.is_err() {
                        break;
                    }
                    if writer.flush().await.is_err() {
                        break;
                    }
                }
                Some(WriteMessage::Stop) | None => break,
            }
        }
    }

    /// Read loop - reads messages from the reader and dispatches them.
    async fn read_loop<R>(
        reader: R,
        pending_requests: Arc<RwLock<HashMap<String, oneshot::Sender<JsonRpcResponse>>>>,
        notification_handler: Arc<RwLock<Option<NotificationHandler>>>,
        request_handlers: Arc<RwLock<HashMap<String, RequestHandler>>>,
        running: Arc<std::sync::atomic::AtomicBool>,
        write_tx: mpsc::Sender<WriteMessage>,
    ) where
        R: tokio::io::AsyncRead + Unpin,
    {
        let mut reader = BufReader::new(reader);

        while running.load(std::sync::atomic::Ordering::SeqCst) {
            // Read Content-Length header
            let content_length = match Self::read_content_length(&mut reader).await {
                Ok(Some(len)) => len,
                Ok(None) => continue,
                Err(_) => break,
            };

            if content_length == 0 {
                continue;
            }

            // Read message body
            let mut body = vec![0u8; content_length];
            if reader.read_exact(&mut body).await.is_err() {
                break;
            }

            // Parse message
            let message: Value = match serde_json::from_slice(&body) {
                Ok(v) => v,
                Err(_) => continue,
            };

            // Determine message type
            let has_id = message.get("id").is_some();
            let has_method = message.get("method").is_some();

            if has_id && has_method {
                // Request from server
                Self::handle_server_request(
                    message,
                    request_handlers.clone(),
                    write_tx.clone(),
                )
                .await;
            } else if has_id {
                // Response to our request
                Self::handle_response(message, pending_requests.clone()).await;
            } else if has_method {
                // Notification
                Self::handle_notification(message, notification_handler.clone()).await;
            }
        }
    }

    /// Read Content-Length header.
    async fn read_content_length<R>(reader: &mut BufReader<R>) -> std::io::Result<Option<usize>>
    where
        R: tokio::io::AsyncRead + Unpin,
    {
        let mut content_length = 0usize;

        loop {
            let mut line = String::new();
            let bytes_read = reader.read_line(&mut line).await?;
            if bytes_read == 0 {
                return Err(std::io::Error::new(
                    std::io::ErrorKind::UnexpectedEof,
                    "EOF",
                ));
            }

            // Check for blank line (end of headers)
            if line == "\r\n" || line == "\n" {
                break;
            }

            // Parse Content-Length
            if let Some(len_str) = line.strip_prefix("Content-Length: ") {
                if let Ok(len) = len_str.trim().parse::<usize>() {
                    content_length = len;
                }
            }
        }

        Ok(Some(content_length))
    }

    /// Handle a response to our request.
    async fn handle_response(
        message: Value,
        pending_requests: Arc<RwLock<HashMap<String, oneshot::Sender<JsonRpcResponse>>>>,
    ) {
        let response: JsonRpcResponse = match serde_json::from_value(message) {
            Ok(r) => r,
            Err(_) => return,
        };

        let id = match &response.id {
            Some(Value::String(s)) => s.clone(),
            _ => return,
        };

        let sender = {
            let mut pending = pending_requests.write().await;
            pending.remove(&id)
        };

        if let Some(sender) = sender {
            let _ = sender.send(response);
        }
    }

    /// Handle a notification from the server.
    async fn handle_notification(
        message: Value,
        notification_handler: Arc<RwLock<Option<NotificationHandler>>>,
    ) {
        let notification: JsonRpcNotification = match serde_json::from_value(message) {
            Ok(n) => n,
            Err(_) => return,
        };

        let handler = notification_handler.read().await;
        if let Some(handler) = handler.as_ref() {
            let params = notification.params.unwrap_or(Value::Null);
            handler(notification.method, params);
        }
    }

    /// Handle a request from the server.
    async fn handle_server_request(
        message: Value,
        request_handlers: Arc<RwLock<HashMap<String, RequestHandler>>>,
        write_tx: mpsc::Sender<WriteMessage>,
    ) {
        let request: JsonRpcRequest = match serde_json::from_value(message) {
            Ok(r) => r,
            Err(_) => return,
        };

        let handlers = request_handlers.read().await;
        let handler = handlers.get(&request.method).cloned();
        drop(handlers);

        let response = if let Some(handler) = handler {
            let params = request.params.unwrap_or(Value::Null);
            match handler(params) {
                Ok(result) => JsonRpcResponse {
                    jsonrpc: "2.0".to_string(),
                    id: Some(request.id),
                    result: Some(result),
                    error: None,
                },
                Err(e) => {
                    let (code, message) = match e {
                        CopilotError::JsonRpc { code, message, .. } => (code, message),
                        _ => (JsonRpcError::INTERNAL_ERROR, e.to_string()),
                    };
                    JsonRpcResponse {
                        jsonrpc: "2.0".to_string(),
                        id: Some(request.id),
                        result: None,
                        error: Some(JsonRpcErrorResponse {
                            code,
                            message,
                            data: None,
                        }),
                    }
                }
            }
        } else {
            JsonRpcResponse {
                jsonrpc: "2.0".to_string(),
                id: Some(request.id),
                result: None,
                error: Some(JsonRpcErrorResponse {
                    code: JsonRpcError::METHOD_NOT_FOUND,
                    message: format!("Method not found: {}", request.method),
                    data: None,
                }),
            }
        };

        // Send response
        if let Ok(data) = serde_json::to_vec(&response) {
            let _ = write_tx.send(WriteMessage::Send(data)).await;
        }
    }

    /// Set the notification handler.
    pub async fn set_notification_handler(&self, handler: NotificationHandler) {
        let mut h = self.notification_handler.write().await;
        *h = Some(handler);
    }

    /// Set a request handler for a specific method.
    pub async fn set_request_handler(&self, method: &str, handler: RequestHandler) {
        let mut handlers = self.request_handlers.write().await;
        handlers.insert(method.to_string(), handler);
    }

    /// Remove a request handler.
    pub async fn remove_request_handler(&self, method: &str) {
        let mut handlers = self.request_handlers.write().await;
        handlers.remove(method);
    }

    /// Send a JSON-RPC request and wait for the response.
    pub async fn request(&self, method: &str, params: Value) -> Result<Value> {
        let request_id = uuid::Uuid::new_v4().to_string();

        // Create response channel
        let (tx, rx) = oneshot::channel();
        {
            let mut pending = self.pending_requests.write().await;
            pending.insert(request_id.clone(), tx);
        }

        // Build request
        let request = JsonRpcRequest {
            jsonrpc: "2.0".to_string(),
            id: Value::String(request_id.clone()),
            method: method.to_string(),
            params: Some(params),
        };

        // Send request
        let data = serde_json::to_vec(&request)?;
        self.write_tx
            .send(WriteMessage::Send(data))
            .await
            .map_err(|_| CopilotError::ClientStopped)?;

        // Wait for response
        let response = rx.await.map_err(|_| CopilotError::ClientStopped)?;

        // Clean up pending request
        {
            let mut pending = self.pending_requests.write().await;
            pending.remove(&request_id);
        }

        // Handle response
        if let Some(error) = response.error {
            return Err(CopilotError::JsonRpc {
                code: error.code,
                message: error.message,
                data: error.data,
            });
        }

        Ok(response.result.unwrap_or(Value::Null))
    }

    /// Send a JSON-RPC notification (no response expected).
    pub async fn notify(&self, method: &str, params: Value) -> Result<()> {
        let notification = JsonRpcNotification {
            jsonrpc: "2.0".to_string(),
            method: method.to_string(),
            params: Some(params),
        };

        let data = serde_json::to_vec(&notification)?;
        self.write_tx
            .send(WriteMessage::Send(data))
            .await
            .map_err(|_| CopilotError::ClientStopped)?;

        Ok(())
    }

    /// Stop the client.
    pub async fn stop(&self) {
        self.running
            .store(false, std::sync::atomic::Ordering::SeqCst);
        let _ = self.write_tx.send(WriteMessage::Stop).await;
    }
}
