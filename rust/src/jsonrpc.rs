//! JSON-RPC 2.0 client implementation.
//!
//! This module provides a JSON-RPC 2.0 client implementation for communicating with the
//! Copilot CLI server using either stdio or TCP transport with Content-Length framing.
//!
//! The client handles:
//! - Sending requests and receiving responses
//! - Receiving and dispatching notifications
//! - Handling incoming requests from the server (for tool calls)
//!
//! # Protocol
//!
//! The client uses the Language Server Protocol (LSP) framing format:
//! - Messages are preceded by a `Content-Length: <size>\r\n\r\n` header
//! - Message bodies are JSON-encoded according to JSON-RPC 2.0
//!
//! # Security
//!
//! Messages larger than [`MAX_MESSAGE_SIZE`] (100 MB) are rejected to prevent
//! denial-of-service attacks via unbounded memory allocation.
//!
//! # Example
//!
//! ```ignore
//! use copilot_sdk::jsonrpc::JsonRpcClient;
//! use serde_json::json;
//!
//! // Create client from stdio streams
//! let client = JsonRpcClient::new(stdin, stdout);
//!
//! // Send a request
//! let result = client.request("ping", json!({"message": "hello"})).await?;
//! ```

use crate::error::{CopilotError, JsonRpcError, Result};

/// Maximum allowed message size (100 MB) to prevent DoS attacks via unbounded memory allocation.
///
/// Any incoming message with a `Content-Length` header exceeding this value will be rejected
/// with an I/O error.
pub const MAX_MESSAGE_SIZE: usize = 100 * 1024 * 1024;

use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::collections::HashMap;
use std::future::Future;
use std::pin::Pin;
use std::sync::Arc;
use tokio::io::{AsyncBufReadExt, AsyncReadExt, AsyncWriteExt, BufReader};
use tokio::sync::{mpsc, oneshot, RwLock};

/// JSON-RPC 2.0 request message.
///
/// Represents a request sent to the server that expects a response.
///
/// # JSON Format
///
/// ```json
/// {
///     "jsonrpc": "2.0",
///     "id": "unique-request-id",
///     "method": "method.name",
///     "params": { ... }
/// }
/// ```
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcRequest {
    /// JSON-RPC protocol version, always "2.0".
    pub jsonrpc: String,

    /// Unique identifier for this request. Used to match responses to requests.
    pub id: Value,

    /// The method name to invoke on the server.
    pub method: String,

    /// Optional parameters for the method call.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub params: Option<Value>,
}

/// JSON-RPC 2.0 response message.
///
/// Represents a response from the server to a request.
///
/// # JSON Format
///
/// Success response:
/// ```json
/// {
///     "jsonrpc": "2.0",
///     "id": "request-id",
///     "result": { ... }
/// }
/// ```
///
/// Error response:
/// ```json
/// {
///     "jsonrpc": "2.0",
///     "id": "request-id",
///     "error": { "code": -32600, "message": "..." }
/// }
/// ```
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcResponse {
    /// JSON-RPC protocol version, always "2.0".
    pub jsonrpc: String,

    /// Request ID this response corresponds to.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub id: Option<Value>,

    /// Result value on success.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<Value>,

    /// Error information on failure.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<JsonRpcErrorResponse>,
}

/// JSON-RPC 2.0 error object in a response.
///
/// Contains error information when a request fails.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcErrorResponse {
    /// Error code indicating the type of error.
    /// Standard codes are defined in [`crate::JsonRpcError`].
    pub code: i32,

    /// Human-readable error message.
    pub message: String,

    /// Optional additional error data.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub data: Option<Value>,
}

/// JSON-RPC 2.0 notification message.
///
/// Represents a one-way message that does not expect a response.
///
/// # JSON Format
///
/// ```json
/// {
///     "jsonrpc": "2.0",
///     "method": "notification.name",
///     "params": { ... }
/// }
/// ```
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcNotification {
    /// JSON-RPC protocol version, always "2.0".
    pub jsonrpc: String,

    /// The notification method name.
    pub method: String,

    /// Optional parameters for the notification.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub params: Option<Value>,
}

/// Notification handler function type.
///
/// Called when the server sends a notification. Receives the method name
/// and parameters.
///
/// # Arguments
///
/// - First argument: Method name (e.g., `"session.event"`)
/// - Second argument: Parameters as a JSON value
pub type NotificationHandler = Arc<dyn Fn(String, Value) + Send + Sync>;

/// Request handler function type for incoming server requests.
///
/// Called when the server sends a request (e.g., tool calls).
/// Returns a future that resolves to the response value or an error.
///
/// # Arguments
///
/// The handler receives the request parameters as a JSON value.
///
/// # Returns
///
/// A pinned boxed future that resolves to `Result<Value>`.
pub type RequestHandler = Arc<
    dyn Fn(Value) -> Pin<Box<dyn Future<Output = Result<Value>> + Send>> + Send + Sync
>;

/// Internal message types for the write loop.
enum WriteMessage {
    /// Send the given bytes to the writer.
    Send(Vec<u8>),
    /// Stop the write loop.
    Stop,
}

/// Disconnect handler function type.
///
/// Called when the connection is lost (read loop exits due to EOF or error).
pub type DisconnectHandler = Arc<dyn Fn() + Send + Sync>;

/// JSON-RPC client for stdio/TCP transport with Content-Length framing.
///
/// This client handles bidirectional JSON-RPC 2.0 communication over async streams.
/// It supports:
///
/// - Sending requests and receiving responses
/// - Sending notifications (one-way messages)
/// - Receiving notifications from the server
/// - Handling incoming requests from the server (e.g., tool calls)
///
/// # Thread Safety
///
/// The client is `Send + Sync` and can be safely shared across tasks.
///
/// # Example
///
/// ```ignore
/// use copilot_sdk::jsonrpc::JsonRpcClient;
/// use serde_json::json;
///
/// let client = JsonRpcClient::new(reader, writer);
///
/// // Set up notification handler
/// client.set_notification_handler(Arc::new(|method, params| {
///     println!("Notification: {} {:?}", method, params);
/// })).await;
///
/// // Send a request
/// let result = client.request("session.create", json!({
///     "model": "gpt-5"
/// })).await?;
/// ```
pub struct JsonRpcClient {
    write_tx: mpsc::Sender<WriteMessage>,
    pending_requests: Arc<RwLock<HashMap<String, oneshot::Sender<JsonRpcResponse>>>>,
    notification_handler: Arc<RwLock<Option<NotificationHandler>>>,
    request_handlers: Arc<RwLock<HashMap<String, RequestHandler>>>,
    running: Arc<std::sync::atomic::AtomicBool>,
    on_disconnect: Arc<RwLock<Option<DisconnectHandler>>>,
}

impl JsonRpcClient {
    /// Create a new JSON-RPC client from async read/write streams.
    ///
    /// This spawns two background tasks:
    /// - A write loop that sends outgoing messages
    /// - A read loop that receives and dispatches incoming messages
    ///
    /// # Arguments
    ///
    /// * `reader` - Async reader for incoming messages
    /// * `writer` - Async writer for outgoing messages
    ///
    /// # Example
    ///
    /// ```ignore
    /// // From stdio
    /// let client = JsonRpcClient::new(tokio::io::stdin(), tokio::io::stdout());
    ///
    /// // From TCP
    /// let (reader, writer) = stream.into_split();
    /// let client = JsonRpcClient::new(reader, writer);
    /// ```
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
        let on_disconnect: Arc<RwLock<Option<DisconnectHandler>>> = Arc::new(RwLock::new(None));

        let client = Self {
            write_tx: write_tx.clone(),
            pending_requests: pending_requests.clone(),
            notification_handler: notification_handler.clone(),
            request_handlers: request_handlers.clone(),
            running: running.clone(),
            on_disconnect: on_disconnect.clone(),
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
                on_disconnect,
            )
            .await;
        });

        client
    }

    /// Write loop - sends messages to the writer with Content-Length framing.
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
        on_disconnect: Arc<RwLock<Option<DisconnectHandler>>>,
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

            // Determine message type and dispatch
            let has_id = message.get("id").is_some();
            let has_method = message.get("method").is_some();

            if has_id && has_method {
                // Request from server (e.g., tool.call)
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
                // Notification from server
                Self::handle_notification(message, notification_handler.clone()).await;
            }
        }

        // Invoke disconnect callback when read loop exits
        if let Some(callback) = on_disconnect.read().await.as_ref() {
            callback();
        }
    }

    /// Read the Content-Length header from the stream.
    ///
    /// Returns the content length, or an error if the message is too large
    /// or the connection is closed.
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
                    if len > MAX_MESSAGE_SIZE {
                        return Err(std::io::Error::new(
                            std::io::ErrorKind::InvalidData,
                            format!("Message size {} exceeds maximum {}", len, MAX_MESSAGE_SIZE),
                        ));
                    }
                    content_length = len;
                }
            }
        }

        Ok(Some(content_length))
    }

    /// Handle a response to one of our requests.
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

    /// Handle a request from the server (e.g., tool.call).
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
            match handler(params).await {
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

    /// Set the notification handler for incoming server notifications.
    ///
    /// Only one handler can be active at a time. Setting a new handler
    /// replaces the previous one.
    ///
    /// # Arguments
    ///
    /// * `handler` - Function called for each notification
    pub async fn set_notification_handler(&self, handler: NotificationHandler) {
        let mut h = self.notification_handler.write().await;
        *h = Some(handler);
    }

    /// Set the disconnect handler called when the connection is lost.
    ///
    /// The callback is invoked when the read loop exits due to EOF or error.
    /// Only one handler can be active at a time. Setting a new handler
    /// replaces the previous one.
    ///
    /// # Arguments
    ///
    /// * `handler` - Function called when disconnected
    pub async fn set_on_disconnect(&self, handler: DisconnectHandler) {
        let mut h = self.on_disconnect.write().await;
        *h = Some(handler);
    }

    /// Set a request handler for a specific method.
    ///
    /// Used to handle incoming requests from the server, such as tool calls.
    ///
    /// # Arguments
    ///
    /// * `method` - The method name to handle (e.g., `"tool.call"`)
    /// * `handler` - Async function to handle the request
    pub async fn set_request_handler(&self, method: &str, handler: RequestHandler) {
        let mut handlers = self.request_handlers.write().await;
        handlers.insert(method.to_string(), handler);
    }

    /// Remove a request handler for a specific method.
    ///
    /// # Arguments
    ///
    /// * `method` - The method name to stop handling
    pub async fn remove_request_handler(&self, method: &str) {
        let mut handlers = self.request_handlers.write().await;
        handlers.remove(method);
    }

    /// Send a JSON-RPC request and wait for the response.
    ///
    /// This method sends a request to the server and blocks until a response
    /// is received or the connection is closed.
    ///
    /// # Arguments
    ///
    /// * `method` - The method name to call
    /// * `params` - Parameters for the method call
    ///
    /// # Returns
    ///
    /// The result value from the server, or an error if the request failed.
    ///
    /// # Errors
    ///
    /// - [`CopilotError::JsonRpc`] - Server returned an error
    /// - [`CopilotError::ClientStopped`] - Connection was closed
    /// - [`CopilotError::Serialization`] - Failed to serialize request
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
    ///
    /// Notifications are one-way messages that don't expect a response
    /// from the server.
    ///
    /// # Arguments
    ///
    /// * `method` - The notification method name
    /// * `params` - Parameters for the notification
    ///
    /// # Errors
    ///
    /// - [`CopilotError::ClientStopped`] - Connection was closed
    /// - [`CopilotError::Serialization`] - Failed to serialize notification
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

    /// Stop the client and close the connection.
    ///
    /// This signals the read and write loops to terminate and closes
    /// the underlying connection.
    pub async fn stop(&self) {
        self.running
            .store(false, std::sync::atomic::Ordering::SeqCst);
        let _ = self.write_tx.send(WriteMessage::Stop).await;
    }
}
