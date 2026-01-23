//! JSON-RPC 2.0 implementation for Copilot SDK

use crate::error::{Error, Result};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::collections::HashMap;
use std::sync::Arc;
use tokio::io::{AsyncBufReadExt, AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt, BufReader};
use tokio::sync::{mpsc, oneshot, Mutex};

/// JSON-RPC 2.0 error
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcError {
    pub code: i32,
    pub message: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub data: Option<Value>,
}

impl std::fmt::Display for JsonRpcError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "JSON-RPC Error {}: {}", self.code, self.message)
    }
}

/// JSON-RPC 2.0 request
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcRequest {
    pub jsonrpc: String,
    pub id: Value,
    pub method: String,
    pub params: HashMap<String, Value>,
}

/// JSON-RPC 2.0 response
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcResponse {
    pub jsonrpc: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub id: Option<Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<HashMap<String, Value>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<JsonRpcError>,
}

/// JSON-RPC 2.0 notification
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcNotification {
    pub jsonrpc: String,
    pub method: String,
    pub params: HashMap<String, Value>,
}

/// Handler for incoming notifications
pub type NotificationHandler = Arc<dyn Fn(String, HashMap<String, Value>) + Send + Sync>;

/// Handler for incoming requests
pub type RequestHandler =
    Arc<dyn Fn(HashMap<String, Value>) -> Result<HashMap<String, Value>> + Send + Sync>;

/// JSON-RPC client for bidirectional communication
pub struct JsonRpcClient {
    writer: Arc<Mutex<Box<dyn AsyncWrite + Unpin + Send>>>,
    pending_requests: Arc<Mutex<HashMap<String, oneshot::Sender<JsonRpcResponse>>>>,
    notification_handler: Arc<Mutex<Option<NotificationHandler>>>,
    request_handlers: Arc<Mutex<HashMap<String, RequestHandler>>>,
    shutdown_tx: Option<mpsc::Sender<()>>,
}

impl JsonRpcClient {
    /// Create a new JSON-RPC client with the given reader and writer
    pub fn new<R, W>(reader: R, writer: W) -> Self
    where
        R: AsyncRead + Unpin + Send + 'static,
        W: AsyncWrite + Unpin + Send + 'static,
    {
        let writer = Arc::new(Mutex::new(
            Box::new(writer) as Box<dyn AsyncWrite + Unpin + Send>
        ));
        let pending_requests = Arc::new(Mutex::new(HashMap::new()));
        let notification_handler = Arc::new(Mutex::new(None));
        let request_handlers = Arc::new(Mutex::new(HashMap::new()));

        let (shutdown_tx, shutdown_rx) = mpsc::channel(1);

        // Spawn reader task
        let pending_requests_clone = Arc::clone(&pending_requests);
        let notification_handler_clone = Arc::clone(&notification_handler);
        let request_handlers_clone = Arc::clone(&request_handlers);
        let writer_clone = Arc::clone(&writer);

        tokio::spawn(Self::read_loop(
            reader,
            pending_requests_clone,
            notification_handler_clone,
            request_handlers_clone,
            writer_clone,
            shutdown_rx,
        ));

        Self {
            writer,
            pending_requests,
            notification_handler,
            request_handlers,
            shutdown_tx: Some(shutdown_tx),
        }
    }

    /// Register a notification handler
    pub async fn set_notification_handler(&self, handler: NotificationHandler) {
        *self.notification_handler.lock().await = Some(handler);
    }

    /// Register a request handler for a specific method
    pub async fn register_request_handler(&self, method: String, handler: RequestHandler) {
        self.request_handlers.lock().await.insert(method, handler);
    }

    /// Send a JSON-RPC request and wait for response
    pub async fn request(
        &self,
        method: String,
        params: HashMap<String, Value>,
    ) -> Result<HashMap<String, Value>> {
        let id = uuid::Uuid::new_v4().to_string();
        let request = JsonRpcRequest {
            jsonrpc: "2.0".to_string(),
            id: Value::String(id.clone()),
            method,
            params,
        };

        let (tx, rx) = oneshot::channel();
        self.pending_requests.lock().await.insert(id, tx);

        self.send_message(&request).await?;

        let response = rx
            .await
            .map_err(|_| Error::ConnectionError("Request cancelled".to_string()))?;

        if let Some(error) = response.error {
            return Err(Error::JsonRpc(error.to_string()));
        }

        Ok(response.result.unwrap_or_default())
    }

    /// Send a JSON-RPC notification (no response expected)
    pub async fn notify(&self, method: String, params: HashMap<String, Value>) -> Result<()> {
        let notification = JsonRpcNotification {
            jsonrpc: "2.0".to_string(),
            method,
            params,
        };

        self.send_message(&notification).await
    }

    /// Send a message with Content-Length header framing
    async fn send_message<T: Serialize>(&self, message: &T) -> Result<()> {
        let json = serde_json::to_string(message)?;
        let content = format!("Content-Length: {}\r\n\r\n{}", json.len(), json);

        let mut writer = self.writer.lock().await;
        writer.write_all(content.as_bytes()).await?;
        writer.flush().await?;

        Ok(())
    }

    /// Read loop that processes incoming messages
    async fn read_loop<R>(
        reader: R,
        pending_requests: Arc<Mutex<HashMap<String, oneshot::Sender<JsonRpcResponse>>>>,
        notification_handler: Arc<Mutex<Option<NotificationHandler>>>,
        request_handlers: Arc<Mutex<HashMap<String, RequestHandler>>>,
        writer: Arc<Mutex<Box<dyn AsyncWrite + Unpin + Send>>>,
        mut shutdown_rx: mpsc::Receiver<()>,
    ) where
        R: AsyncRead + Unpin,
    {
        let mut reader = BufReader::new(reader);
        let mut headers = Vec::new();

        loop {
            tokio::select! {
                _ = shutdown_rx.recv() => {
                    break;
                }
                result = Self::read_message(&mut reader, &mut headers) => {
                    match result {
                        Ok(Some(message)) => {
                            Self::handle_message(
                                message,
                                &pending_requests,
                                &notification_handler,
                                &request_handlers,
                                &writer,
                            )
                            .await;
                        }
                        Ok(None) => break, // EOF
                        Err(e) => {
                            eprintln!("Error reading message: {}", e);
                            break;
                        }
                    }
                }
            }
        }
    }

    /// Read a single message with Content-Length header
    async fn read_message<R>(
        reader: &mut BufReader<R>,
        headers: &mut Vec<u8>,
    ) -> Result<Option<Value>>
    where
        R: AsyncRead + Unpin,
    {
        headers.clear();

        // Read headers
        let mut content_length = 0;
        loop {
            let n = reader.read_until(b'\n', headers).await?;
            if n == 0 {
                return Ok(None); // EOF
            }

            let line = std::str::from_utf8(headers)
                .map_err(|e| Error::Other(format!("Invalid UTF-8 in headers: {}", e)))?;

            if line.trim().is_empty() {
                break; // End of headers
            }

            if line.starts_with("Content-Length:") {
                content_length = line
                    .trim_start_matches("Content-Length:")
                    .trim()
                    .parse()
                    .map_err(|e| Error::Other(format!("Invalid Content-Length: {}", e)))?;
            }

            headers.clear();
        }

        if content_length == 0 {
            return Err(Error::Other("Missing Content-Length header".to_string()));
        }

        // Read body
        let mut body = vec![0u8; content_length];
        reader.read_exact(&mut body).await?;

        let message: Value = serde_json::from_slice(&body)?;
        Ok(Some(message))
    }

    /// Handle an incoming message
    async fn handle_message(
        message: Value,
        pending_requests: &Arc<Mutex<HashMap<String, oneshot::Sender<JsonRpcResponse>>>>,
        notification_handler: &Arc<Mutex<Option<NotificationHandler>>>,
        request_handlers: &Arc<Mutex<HashMap<String, RequestHandler>>>,
        writer: &Arc<Mutex<Box<dyn AsyncWrite + Unpin + Send>>>,
    ) {
        // Check if it's a response
        if message.get("result").is_some() || message.get("error").is_some() {
            if let Ok(response) = serde_json::from_value::<JsonRpcResponse>(message) {
                if let Some(id) = response.id.as_ref().and_then(|v| v.as_str()) {
                    if let Some(tx) = pending_requests.lock().await.remove(id) {
                        let _ = tx.send(response);
                    }
                }
            }
        }
        // Check if it's a request
        else if message.get("id").is_some() {
            if let Ok(request) = serde_json::from_value::<JsonRpcRequest>(message) {
                let handlers = request_handlers.lock().await;
                if let Some(handler) = handlers.get(&request.method) {
                    let result = handler(request.params);
                    let response = match result {
                        Ok(res) => JsonRpcResponse {
                            jsonrpc: "2.0".to_string(),
                            id: Some(request.id),
                            result: Some(res),
                            error: None,
                        },
                        Err(e) => JsonRpcResponse {
                            jsonrpc: "2.0".to_string(),
                            id: Some(request.id),
                            result: None,
                            error: Some(JsonRpcError {
                                code: -32603,
                                message: e.to_string(),
                                data: None,
                            }),
                        },
                    };

                    // Send response
                    let json = serde_json::to_string(&response).unwrap();
                    let content = format!("Content-Length: {}\r\n\r\n{}", json.len(), json);
                    let mut w = writer.lock().await;
                    let _ = w.write_all(content.as_bytes()).await;
                    let _ = w.flush().await;
                }
            }
        }
        // It's a notification
        else if let Ok(notification) = serde_json::from_value::<JsonRpcNotification>(message) {
            if let Some(handler) = notification_handler.lock().await.as_ref() {
                handler(notification.method, notification.params);
            }
        }
    }

    /// Shutdown the client
    pub async fn shutdown(&mut self) {
        if let Some(tx) = self.shutdown_tx.take() {
            let _ = tx.send(()).await;
        }
    }
}
