//! JSON-RPC 2.0 client implementation with Content-Length framing.

use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::collections::HashMap;
use std::sync::Arc;
use tokio::io::{AsyncBufReadExt, AsyncReadExt, AsyncWriteExt, BufReader};
use tokio::sync::{mpsc, oneshot, Mutex, RwLock};

use crate::error::{CopilotError, JsonRpcError, Result};

/// JSON-RPC 2.0 request.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcRequest {
    /// JSON-RPC version.
    pub jsonrpc: String,
    /// Request ID.
    pub id: Value,
    /// Method name.
    pub method: String,
    /// Parameters.
    #[serde(default)]
    pub params: Value,
}

/// JSON-RPC 2.0 response.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcResponse {
    /// JSON-RPC version.
    pub jsonrpc: String,
    /// Request ID.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub id: Option<Value>,
    /// Result (if successful).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<Value>,
    /// Error (if failed).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<JsonRpcErrorPayload>,
}

/// JSON-RPC error payload.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcErrorPayload {
    /// Error code.
    pub code: i32,
    /// Error message.
    pub message: String,
    /// Additional error data.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub data: Option<Value>,
}

/// JSON-RPC 2.0 notification (no ID).
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcNotification {
    /// JSON-RPC version.
    pub jsonrpc: String,
    /// Method name.
    pub method: String,
    /// Parameters.
    #[serde(default)]
    pub params: Value,
}

/// Handler for incoming notifications.
pub type NotificationHandler = Box<dyn Fn(String, Value) + Send + Sync>;

/// Handler for incoming server requests.
pub type RequestHandler = Box<
    dyn Fn(Value) -> std::pin::Pin<Box<dyn std::future::Future<Output = std::result::Result<Value, JsonRpcError>> + Send>>
        + Send
        + Sync,
>;

/// Pending request awaiting response.
struct PendingRequest {
    response_tx: oneshot::Sender<JsonRpcResponse>,
}

/// JSON-RPC client for communication with the Copilot CLI.
pub struct JsonRpcClient<R, W> {
    /// Reader (stdout from CLI).
    reader: Arc<Mutex<BufReader<R>>>,
    /// Writer (stdin to CLI).
    writer: Arc<Mutex<W>>,
    /// Pending requests awaiting responses.
    pending_requests: Arc<RwLock<HashMap<String, PendingRequest>>>,
    /// Notification handler.
    notification_handler: Arc<RwLock<Option<NotificationHandler>>>,
    /// Request handlers for server->client requests.
    request_handlers: Arc<RwLock<HashMap<String, RequestHandler>>>,
    /// Stop signal sender.
    stop_tx: Option<mpsc::Sender<()>>,
    /// Whether the client is running.
    running: Arc<std::sync::atomic::AtomicBool>,
}

impl<R, W> JsonRpcClient<R, W>
where
    R: tokio::io::AsyncRead + Unpin + Send + 'static,
    W: tokio::io::AsyncWrite + Unpin + Send + 'static,
{
    /// Create a new JSON-RPC client.
    pub fn new(reader: R, writer: W) -> Self {
        Self {
            reader: Arc::new(Mutex::new(BufReader::new(reader))),
            writer: Arc::new(Mutex::new(writer)),
            pending_requests: Arc::new(RwLock::new(HashMap::new())),
            notification_handler: Arc::new(RwLock::new(None)),
            request_handlers: Arc::new(RwLock::new(HashMap::new())),
            stop_tx: None,
            running: Arc::new(std::sync::atomic::AtomicBool::new(false)),
        }
    }

    /// Start the client's read loop in a background task.
    pub fn start(&mut self) -> tokio::task::JoinHandle<()> {
        let (stop_tx, mut stop_rx) = mpsc::channel::<()>(1);
        self.stop_tx = Some(stop_tx);
        self.running.store(true, std::sync::atomic::Ordering::SeqCst);

        let reader = Arc::clone(&self.reader);
        let pending_requests = Arc::clone(&self.pending_requests);
        let notification_handler = Arc::clone(&self.notification_handler);
        let request_handlers = Arc::clone(&self.request_handlers);
        let writer = Arc::clone(&self.writer);
        let running = Arc::clone(&self.running);

        tokio::spawn(async move {
            loop {
                tokio::select! {
                    _ = stop_rx.recv() => {
                        break;
                    }
                    result = Self::read_message(&reader) => {
                        match result {
                            Ok(Some(message)) => {
                                Self::handle_message(
                                    message,
                                    &pending_requests,
                                    &notification_handler,
                                    &request_handlers,
                                    &writer,
                                ).await;
                            }
                            Ok(None) => {
                                // EOF - connection closed
                                break;
                            }
                            Err(e) => {
                                if running.load(std::sync::atomic::Ordering::SeqCst) {
                                    eprintln!("Error reading message: {}", e);
                                }
                                break;
                            }
                        }
                    }
                }
            }
            running.store(false, std::sync::atomic::Ordering::SeqCst);
        })
    }

    /// Stop the client.
    pub fn stop(&mut self) {
        self.running.store(false, std::sync::atomic::Ordering::SeqCst);
        if let Some(tx) = self.stop_tx.take() {
            let _ = tx.try_send(());
        }
    }

    /// Set the notification handler.
    pub async fn set_notification_handler(&self, handler: NotificationHandler) {
        let mut guard = self.notification_handler.write().await;
        *guard = Some(handler);
    }

    /// Set a request handler for server->client requests.
    pub async fn set_request_handler(&self, method: impl Into<String>, handler: RequestHandler) {
        let mut guard = self.request_handlers.write().await;
        guard.insert(method.into(), handler);
    }

    /// Send a request and wait for the response.
    pub async fn request(&self, method: impl Into<String>, params: Value) -> Result<Value> {
        let request_id = uuid::Uuid::new_v4().to_string();
        let (response_tx, response_rx) = oneshot::channel();

        // Register pending request
        {
            let mut pending = self.pending_requests.write().await;
            pending.insert(request_id.clone(), PendingRequest { response_tx });
        }

        // Send request
        let request = JsonRpcRequest {
            jsonrpc: "2.0".to_string(),
            id: Value::String(request_id.clone()),
            method: method.into(),
            params,
        };

        if let Err(e) = self.send_message(&request).await {
            // Remove pending request on send failure
            let mut pending = self.pending_requests.write().await;
            pending.remove(&request_id);
            return Err(e);
        }

        // Wait for response
        match response_rx.await {
            Ok(response) => {
                if let Some(error) = response.error {
                    Err(CopilotError::json_rpc(
                        error.code,
                        error.message,
                        error.data,
                    ))
                } else {
                    Ok(response.result.unwrap_or(Value::Null))
                }
            }
            Err(_) => Err(CopilotError::ClientStopped),
        }
    }

    /// Send a notification (no response expected).
    pub async fn notify(&self, method: impl Into<String>, params: Value) -> Result<()> {
        let notification = JsonRpcNotification {
            jsonrpc: "2.0".to_string(),
            method: method.into(),
            params,
        };
        self.send_message(&notification).await
    }

    /// Send a message with Content-Length framing.
    async fn send_message<T: Serialize>(&self, message: &T) -> Result<()> {
        let data = serde_json::to_vec(message)?;
        let header = format!("Content-Length: {}\r\n\r\n", data.len());

        let mut writer = self.writer.lock().await;
        writer.write_all(header.as_bytes()).await?;
        writer.write_all(&data).await?;
        writer.flush().await?;

        Ok(())
    }

    /// Read a message with Content-Length framing.
    async fn read_message(reader: &Arc<Mutex<BufReader<R>>>) -> Result<Option<Value>> {
        let mut reader = reader.lock().await;

        // Read headers until we get Content-Length
        let mut content_length: Option<usize> = None;

        loop {
            let mut line = String::new();
            let bytes_read = reader.read_line(&mut line).await?;

            if bytes_read == 0 {
                return Ok(None); // EOF
            }

            let trimmed = line.trim();
            if trimmed.is_empty() {
                break; // End of headers
            }

            if let Some(len_str) = trimmed.strip_prefix("Content-Length:") {
                if let Ok(len) = len_str.trim().parse::<usize>() {
                    content_length = Some(len);
                }
            }
        }

        let content_length = match content_length {
            Some(len) => len,
            None => return Ok(None),
        };

        // Read body
        let mut body = vec![0u8; content_length];
        reader.read_exact(&mut body).await?;

        let value: Value = serde_json::from_slice(&body)?;
        Ok(Some(value))
    }

    /// Handle an incoming message.
    async fn handle_message(
        message: Value,
        pending_requests: &Arc<RwLock<HashMap<String, PendingRequest>>>,
        notification_handler: &Arc<RwLock<Option<NotificationHandler>>>,
        request_handlers: &Arc<RwLock<HashMap<String, RequestHandler>>>,
        writer: &Arc<Mutex<W>>,
    ) {
        // Check if it's a request (has both id and method)
        if message.get("method").is_some() && message.get("id").is_some() {
            // Server->client request
            if let Ok(request) = serde_json::from_value::<JsonRpcRequest>(message) {
                Self::handle_request(request, request_handlers, writer).await;
            }
            return;
        }

        // Check if it's a response (has id but no method)
        if message.get("id").is_some() && message.get("method").is_none() {
            if let Ok(response) = serde_json::from_value::<JsonRpcResponse>(message) {
                Self::handle_response(response, pending_requests).await;
            }
            return;
        }

        // Check if it's a notification (has method but no id)
        if message.get("method").is_some() && message.get("id").is_none() {
            if let Ok(notification) = serde_json::from_value::<JsonRpcNotification>(message) {
                Self::handle_notification(notification, notification_handler).await;
            }
        }
    }

    /// Handle an incoming response.
    async fn handle_response(
        response: JsonRpcResponse,
        pending_requests: &Arc<RwLock<HashMap<String, PendingRequest>>>,
    ) {
        let id = match &response.id {
            Some(Value::String(s)) => s.clone(),
            _ => return,
        };

        let pending = {
            let mut guard = pending_requests.write().await;
            guard.remove(&id)
        };

        if let Some(pending) = pending {
            let _ = pending.response_tx.send(response);
        }
    }

    /// Handle an incoming notification.
    async fn handle_notification(
        notification: JsonRpcNotification,
        notification_handler: &Arc<RwLock<Option<NotificationHandler>>>,
    ) {
        let handler = notification_handler.read().await;
        if let Some(ref handler) = *handler {
            handler(notification.method, notification.params);
        }
    }

    /// Handle an incoming server->client request.
    async fn handle_request(
        request: JsonRpcRequest,
        request_handlers: &Arc<RwLock<HashMap<String, RequestHandler>>>,
        writer: &Arc<Mutex<W>>,
    ) {
        let handler = {
            let handlers = request_handlers.read().await;
            handlers.get(&request.method).map(|_| {
                // Mark that we found a handler
                request.method.clone()
            })
        };

        let response = if handler.is_some() {
            let handlers = request_handlers.read().await;
            if let Some(handler) = handlers.get(&request.method) {
                match handler(request.params).await {
                    Ok(result) => JsonRpcResponse {
                        jsonrpc: "2.0".to_string(),
                        id: Some(request.id),
                        result: Some(result),
                        error: None,
                    },
                    Err(err) => JsonRpcResponse {
                        jsonrpc: "2.0".to_string(),
                        id: Some(request.id),
                        result: None,
                        error: Some(JsonRpcErrorPayload {
                            code: err.code,
                            message: err.message,
                            data: err.data,
                        }),
                    },
                }
            } else {
                JsonRpcResponse {
                    jsonrpc: "2.0".to_string(),
                    id: Some(request.id),
                    result: None,
                    error: Some(JsonRpcErrorPayload {
                        code: JsonRpcError::METHOD_NOT_FOUND,
                        message: format!("Method not found: {}", request.method),
                        data: None,
                    }),
                }
            }
        } else {
            JsonRpcResponse {
                jsonrpc: "2.0".to_string(),
                id: Some(request.id),
                result: None,
                error: Some(JsonRpcErrorPayload {
                    code: JsonRpcError::METHOD_NOT_FOUND,
                    message: format!("Method not found: {}", request.method),
                    data: None,
                }),
            }
        };

        // Send response
        let data = match serde_json::to_vec(&response) {
            Ok(d) => d,
            Err(_) => return,
        };
        let header = format!("Content-Length: {}\r\n\r\n", data.len());

        let mut writer = writer.lock().await;
        let _ = writer.write_all(header.as_bytes()).await;
        let _ = writer.write_all(&data).await;
        let _ = writer.flush().await;
    }
}
