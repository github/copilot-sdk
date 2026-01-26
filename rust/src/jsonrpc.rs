use anyhow::{Context, Result};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::collections::HashMap;
use std::io::{BufRead, BufReader, Write};
use std::sync::{
    Arc, Mutex,
    atomic::{AtomicBool, Ordering},
};
use tokio::sync::{mpsc, oneshot};
use uuid::Uuid;

// ============================================================================
// JSON-RPC Types
// ============================================================================

/// JSON-RPC error
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

impl std::error::Error for JsonRpcError {}

/// JSON-RPC request
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcRequest {
    pub jsonrpc: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub id: Option<Value>,
    pub method: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub params: Option<Value>,
}

/// JSON-RPC response
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcResponse {
    pub jsonrpc: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub id: Option<Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<JsonRpcError>,
}

/// JSON-RPC notification (request without ID)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcNotification {
    pub jsonrpc: String,
    pub method: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub params: Option<Value>,
}

// ============================================================================
// Handler Types
// ============================================================================

/// Handler for incoming notifications
pub type NotificationHandler = Arc<dyn Fn(String, Value) + Send + Sync>;

/// Handler for incoming requests from the server
pub type RequestHandler = Arc<dyn Fn(Value) -> Result<Value, JsonRpcError> + Send + Sync>;

// ============================================================================
// JSON-RPC Client
// ============================================================================

type ResponseSender = oneshot::Sender<Result<Value, JsonRpcError>>;

pub struct JsonRpcClient<W: Write + Send, R: BufRead + Send> {
    writer: Arc<Mutex<W>>,
    reader: Arc<Mutex<R>>,
    pending_requests: Arc<Mutex<HashMap<String, ResponseSender>>>,
    request_handlers: Arc<Mutex<HashMap<String, RequestHandler>>>,
    notification_handler: Arc<Mutex<Option<NotificationHandler>>>,
    running: Arc<AtomicBool>,
    stop_tx: mpsc::Sender<()>,
    stop_rx: Arc<Mutex<Option<mpsc::Receiver<()>>>>,
}

impl<W: Write + Send + 'static, R: BufRead + Send + 'static> JsonRpcClient<W, R> {
    /// Create a new JSON-RPC client
    pub fn new(writer: W, reader: R) -> Self {
        let (stop_tx, stop_rx) = mpsc::channel(1);

        Self {
            writer: Arc::new(Mutex::new(writer)),
            reader: Arc::new(Mutex::new(reader)),
            pending_requests: Arc::new(Mutex::new(HashMap::new())),
            request_handlers: Arc::new(Mutex::new(HashMap::new())),
            notification_handler: Arc::new(Mutex::new(None)),
            running: Arc::new(AtomicBool::new(false)),
            stop_tx,
            stop_rx: Arc::new(Mutex::new(Some(stop_rx))),
        }
    }

    /// Start the client and begin processing messages
    pub fn start(&self) {
        if self
            .running
            .compare_exchange(false, true, Ordering::SeqCst, Ordering::SeqCst)
            .is_err()
        {
            return; // Already running
        }

        let reader = Arc::clone(&self.reader);
        let writer = Arc::clone(&self.writer);
        let pending_requests = Arc::clone(&self.pending_requests);
        let request_handlers = Arc::clone(&self.request_handlers);
        let notification_handler = Arc::clone(&self.notification_handler);
        let running = Arc::clone(&self.running);
        let stop_rx = Arc::clone(&self.stop_rx);

        std::thread::spawn(move || {
            let mut stop_rx = stop_rx.lock().unwrap().take().unwrap();

            loop {
                // Check if we should stop
                if !running.load(Ordering::SeqCst) {
                    break;
                }

                // Try to receive stop signal (non-blocking)
                match stop_rx.try_recv() {
                    Ok(_) | Err(mpsc::error::TryRecvError::Disconnected) => break,
                    Err(mpsc::error::TryRecvError::Empty) => {}
                }

                // Read next message
                let message_result = {
                    let mut reader_guard = reader.lock().unwrap();
                    Self::read_message(&mut *reader_guard)
                };

                match message_result {
                    Ok(Some(msg)) => {
                        Self::handle_message(
                            msg,
                            &writer,
                            &pending_requests,
                            &request_handlers,
                            &notification_handler,
                        );
                    }
                    Ok(None) => continue,
                    Err(e) => {
                        if running.load(Ordering::SeqCst) {
                            eprintln!("Error reading message: {}", e);
                        }
                        break;
                    }
                }
            }

            running.store(false, Ordering::SeqCst);
        });
    }

    /// Stop the client
    pub fn stop(&self) {
        self.running.store(false, Ordering::SeqCst);
        let _ = self.stop_tx.try_send(());
    }

    /// Set handler for incoming notifications
    pub fn set_notification_handler(&self, handler: NotificationHandler) {
        let mut guard = self.notification_handler.lock().unwrap();
        *guard = Some(handler);
    }

    /// Set handler for incoming server requests
    pub fn set_request_handler(&self, method: String, handler: RequestHandler) {
        let mut guard = self.request_handlers.lock().unwrap();
        guard.insert(method, handler);
    }

    /// Remove handler for a specific method
    pub fn remove_request_handler(&self, method: &str) {
        let mut guard = self.request_handlers.lock().unwrap();
        guard.remove(method);
    }

    /// Send a request and wait for response
    pub async fn request(&self, method: &str, params: Value) -> Result<Value, JsonRpcError> {
        let request_id = Uuid::new_v4().to_string();
        let (tx, rx) = oneshot::channel();

        // Register pending request
        {
            let mut pending = self.pending_requests.lock().unwrap();
            pending.insert(request_id.clone(), tx);
        }

        // Send request
        let request = JsonRpcRequest {
            jsonrpc: "2.0".to_string(),
            id: Some(Value::String(request_id.clone())),
            method: method.to_string(),
            params: Some(params),
        };

        if let Err(e) = self.send_message(&request) {
            // Clean up on error
            let mut pending = self.pending_requests.lock().unwrap();
            pending.remove(&request_id);
            return Err(JsonRpcError {
                code: -32000,
                message: format!("Failed to send request: {}", e),
                data: None,
            });
        }

        // Wait for response
        match rx.await {
            Ok(result) => result,
            Err(_) => Err(JsonRpcError {
                code: -32000,
                message: "Response channel closed".to_string(),
                data: None,
            }),
        }
    }

    /// Send a notification (no response expected)
    pub fn notify(&self, method: &str, params: Value) -> Result<()> {
        let notification = JsonRpcNotification {
            jsonrpc: "2.0".to_string(),
            method: method.to_string(),
            params: Some(params),
        };

        self.send_message(&notification)
    }

    /// Send a message (internal helper)
    fn send_message<T: Serialize>(&self, message: &T) -> Result<()> {
        let data = serde_json::to_vec(message)?;
        let header = format!("Content-Length: {}\r\n\r\n", data.len());

        let mut writer = self.writer.lock().unwrap();
        writer
            .write_all(header.as_bytes())
            .context("Failed to write header")?;
        writer.write_all(&data).context("Failed to write message")?;
        writer.flush().context("Failed to flush writer")?;

        Ok(())
    }

    /// Read a single message from the reader
    fn read_message(reader: &mut R) -> Result<Option<Value>> {
        // Read headers
        let mut content_length = 0;
        loop {
            let mut line = String::new();
            let bytes_read = reader.read_line(&mut line)?;
            if bytes_read == 0 {
                return Ok(None); // EOF
            }

            let line = line.trim();
            if line.is_empty() {
                break; // End of headers
            }

            if let Some(value) = line.strip_prefix("Content-Length:") {
                content_length = value.trim().parse()?;
            }
        }

        if content_length == 0 {
            return Ok(None);
        }

        // Read body
        let mut body = vec![0u8; content_length];
        reader.read_exact(&mut body)?;

        let value: Value = serde_json::from_slice(&body)?;
        Ok(Some(value))
    }

    /// Handle an incoming message
    fn handle_message(
        msg: Value,
        writer: &Arc<Mutex<W>>,
        pending_requests: &Arc<Mutex<HashMap<String, ResponseSender>>>,
        request_handlers: &Arc<Mutex<HashMap<String, RequestHandler>>>,
        notification_handler: &Arc<Mutex<Option<NotificationHandler>>>,
    ) {
        // Try to parse as response first
        if let Ok(response) = serde_json::from_value::<JsonRpcResponse>(msg.clone()) {
            if let Some(id) = &response.id {
                let id_str = match id {
                    Value::String(s) => s.clone(),
                    Value::Number(n) => n.to_string(),
                    _ => return,
                };

                let mut pending = pending_requests.lock().unwrap();
                if let Some(tx) = pending.remove(&id_str) {
                    let result = if let Some(error) = response.error {
                        Err(error)
                    } else {
                        Ok(response.result.unwrap_or(Value::Null))
                    };
                    let _ = tx.send(result);
                }
            }
            return;
        }

        // Try to parse as request
        if let Ok(request) = serde_json::from_value::<JsonRpcRequest>(msg.clone())
            && request.id.is_some()
        {
            Self::handle_request(request, writer, request_handlers);
            return;
        }

        // Try to parse as notification
        if let Ok(notification) = serde_json::from_value::<JsonRpcNotification>(msg) {
            Self::handle_notification(notification, notification_handler);
        }
    }

    /// Handle an incoming request from the server
    fn handle_request(
        request: JsonRpcRequest,
        writer: &Arc<Mutex<W>>,
        request_handlers: &Arc<Mutex<HashMap<String, RequestHandler>>>,
    ) {
        let handler = {
            let handlers = request_handlers.lock().unwrap();
            handlers.get(&request.method).cloned()
        };

        let response = if let Some(handler) = handler {
            let result = handler(request.params.unwrap_or(Value::Null));
            match result {
                Ok(value) => JsonRpcResponse {
                    jsonrpc: "2.0".to_string(),
                    id: request.id.clone(),
                    result: Some(value),
                    error: None,
                },
                Err(error) => JsonRpcResponse {
                    jsonrpc: "2.0".to_string(),
                    id: request.id.clone(),
                    result: None,
                    error: Some(error),
                },
            }
        } else {
            JsonRpcResponse {
                jsonrpc: "2.0".to_string(),
                id: request.id.clone(),
                result: None,
                error: Some(JsonRpcError {
                    code: -32601,
                    message: format!("Method not found: {}", request.method),
                    data: None,
                }),
            }
        };

        // Send response
        let data = match serde_json::to_vec(&response) {
            Ok(d) => d,
            Err(e) => {
                eprintln!("Failed to serialize response: {}", e);
                return;
            }
        };

        let header = format!("Content-Length: {}\r\n\r\n", data.len());
        let mut writer = writer.lock().unwrap();
        if let Err(e) = writer.write_all(header.as_bytes()) {
            eprintln!("Failed to write response header: {}", e);
            return;
        }
        if let Err(e) = writer.write_all(&data) {
            eprintln!("Failed to write response body: {}", e);
            return;
        }
        let _ = writer.flush();
    }

    /// Handle an incoming notification
    fn handle_notification(
        notification: JsonRpcNotification,
        notification_handler: &Arc<Mutex<Option<NotificationHandler>>>,
    ) {
        let handler = notification_handler.lock().unwrap().clone();
        if let Some(handler) = handler {
            handler(
                notification.method,
                notification.params.unwrap_or(Value::Null),
            );
        }
    }
}

// ============================================================================
// Convenience type for stdio transport
// ============================================================================

pub type StdioJsonRpcClient = JsonRpcClient<std::io::Stdout, BufReader<std::io::Stdin>>;

impl StdioJsonRpcClient {
    pub fn new_stdio() -> Self {
        Self::new(std::io::stdout(), BufReader::new(std::io::stdin()))
    }
}
