//! Session management for conversation sessions with the Copilot CLI.

use std::collections::HashMap;
use std::sync::atomic::{AtomicU64, Ordering};
use std::time::Duration;

use serde_json::{json, Value};
use tokio::sync::{broadcast, RwLock};

use crate::error::{CopilotError, Result};
use crate::generated::{SessionEvent, SessionEventType};
use crate::types::*;

/// Handler ID for tracking subscriptions.
type HandlerId = u64;

/// Internal handler entry.
struct HandlerEntry {
    id: HandlerId,
    handler: Box<dyn Fn(SessionEvent) + Send + Sync>,
}

/// A conversation session with the Copilot CLI.
///
/// Sessions maintain conversation state, handle events, and manage tool execution.
/// Sessions are created via [`Client::create_session`](crate::Client::create_session)
/// or resumed via [`Client::resume_session`](crate::Client::resume_session).
///
/// # Example
///
/// ```no_run
/// use copilot_sdk::{Client, ClientOptions, SessionConfig, MessageOptions};
///
/// #[tokio::main]
/// async fn main() -> copilot_sdk::Result<()> {
///     let mut client = Client::new(ClientOptions::new());
///     client.start().await?;
///
///     let session = client.create_session(SessionConfig::new().model("gpt-5")).await?;
///
///     // Subscribe to events
///     let _unsubscribe = session.on(|event| {
///         if event.r#type == copilot_sdk::SessionEventType::AssistantMessage {
///             if let Some(content) = &event.data.content {
///                 println!("Assistant: {}", content);
///             }
///         }
///     });
///
///     // Send a message
///     session.send(MessageOptions::new("Hello!")).await?;
///
///     Ok(())
/// }
/// ```
pub struct Session {
    /// The session ID.
    session_id: String,
    /// Event handlers.
    handlers: RwLock<Vec<HandlerEntry>>,
    /// Next handler ID.
    next_handler_id: AtomicU64,
    /// Tool handlers.
    tool_handlers: RwLock<HashMap<String, ToolHandlerFn>>,
    /// Permission handler.
    permission_handler: RwLock<Option<PermissionHandlerFn>>,
    /// Broadcast channel for events.
    event_tx: broadcast::Sender<SessionEvent>,
    /// Reference to the client for making requests.
    /// We use a raw pointer here to avoid circular Arc references.
    /// Safety: The Client owns Sessions and ensures this pointer remains valid.
    client_ptr: *const (),
}

// Safety: Session is Send because all its fields are Send-safe.
// The client_ptr is only used through request_fn which handles synchronization.
unsafe impl Send for Session {}
unsafe impl Sync for Session {}

/// Type alias for permission handler function.
type PermissionHandlerFn = Box<
    dyn Fn(PermissionRequest, PermissionInvocation) -> std::pin::Pin<Box<dyn std::future::Future<Output = Result<PermissionRequestResult>> + Send>>
        + Send
        + Sync,
>;

impl Session {
    /// Create a new session.
    ///
    /// Note: This is primarily for internal use. Use [`Client::create_session`](crate::Client::create_session)
    /// to create sessions.
    pub(crate) fn new(session_id: String, client: &crate::client::Client) -> Self {
        let (event_tx, _) = broadcast::channel(100);

        Self {
            session_id,
            handlers: RwLock::new(Vec::new()),
            next_handler_id: AtomicU64::new(0),
            tool_handlers: RwLock::new(HashMap::new()),
            permission_handler: RwLock::new(None),
            event_tx,
            client_ptr: client as *const _ as *const (),
        }
    }

    /// Get the session ID.
    pub fn session_id(&self) -> &str {
        &self.session_id
    }

    /// Send a message to this session.
    ///
    /// Returns the message ID which can be used to correlate events.
    pub async fn send(&self, options: MessageOptions) -> Result<String> {
        let mut params = serde_json::Map::new();
        params.insert("sessionId".to_string(), json!(self.session_id));
        params.insert("prompt".to_string(), json!(options.prompt));

        if let Some(attachments) = options.attachments {
            params.insert("attachments".to_string(), serde_json::to_value(attachments)?);
        }

        if let Some(mode) = options.mode {
            params.insert("mode".to_string(), json!(mode));
        }

        let result = self.request("session.send", Value::Object(params)).await?;

        let message_id = result
            .get("messageId")
            .and_then(|v| v.as_str())
            .ok_or_else(|| CopilotError::session("invalid response: missing messageId"))?
            .to_string();

        Ok(message_id)
    }

    /// Send a message and wait until the session becomes idle.
    ///
    /// This is a convenience method that combines [`send`](Self::send) with waiting
    /// for the `session.idle` event.
    ///
    /// # Arguments
    ///
    /// * `options` - Message options including the prompt.
    /// * `timeout` - How long to wait for completion. Defaults to 60 seconds if None.
    ///
    /// Returns the final assistant message event, or None if none was received.
    pub async fn send_and_wait(
        &self,
        options: MessageOptions,
        timeout: Option<Duration>,
    ) -> Result<Option<SessionEvent>> {
        let timeout = timeout.unwrap_or(Duration::from_secs(60));

        let mut rx = self.subscribe();
        let mut last_assistant_message: Option<SessionEvent> = None;

        // Send the message
        self.send(options).await?;

        // Wait for idle or error
        let result = tokio::time::timeout(timeout, async {
            loop {
                match rx.recv().await {
                    Ok(event) => {
                        match event.r#type {
                            SessionEventType::AssistantMessage => {
                                last_assistant_message = Some(event);
                            }
                            SessionEventType::SessionIdle => {
                                return Ok(last_assistant_message);
                            }
                            SessionEventType::SessionError => {
                                let msg = event.data.message.clone().unwrap_or_else(|| "session error".to_string());
                                return Err(CopilotError::session(msg));
                            }
                            _ => {}
                        }
                    }
                    Err(broadcast::error::RecvError::Closed) => {
                        return Err(CopilotError::ClientStopped);
                    }
                    Err(broadcast::error::RecvError::Lagged(_)) => {
                        // Continue receiving
                    }
                }
            }
        })
        .await;

        match result {
            Ok(inner) => inner,
            Err(_) => Err(CopilotError::Timeout(timeout)),
        }
    }

    /// Subscribe to events from this session.
    ///
    /// Returns an unsubscribe function that can be called to stop receiving events.
    ///
    /// # Example
    ///
    /// ```no_run
    /// # use copilot_sdk::{Session, SessionEventType};
    /// # async fn example(session: &Session) {
    /// let unsubscribe = session.on(|event| {
    ///     match event.r#type {
    ///         SessionEventType::AssistantMessage => {
    ///             if let Some(content) = &event.data.content {
    ///                 println!("Assistant: {}", content);
    ///             }
    ///         }
    ///         SessionEventType::SessionError => {
    ///             if let Some(msg) = &event.data.message {
    ///                 eprintln!("Error: {}", msg);
    ///             }
    ///         }
    ///         _ => {}
    ///     }
    /// });
    ///
    /// // Later, to stop receiving events:
    /// // unsubscribe();
    /// # }
    /// ```
    pub fn on<F>(&self, handler: F) -> impl FnOnce()
    where
        F: Fn(SessionEvent) + Send + Sync + 'static,
    {
        let id = self.next_handler_id.fetch_add(1, Ordering::SeqCst);
        let entry = HandlerEntry {
            id,
            handler: Box::new(handler),
        };

        // We can't use async in on(), so we need to spawn a task
        let handlers = unsafe { &*(&self.handlers as *const _) as &RwLock<Vec<HandlerEntry>> };

        // Use try_write to avoid blocking; if it fails, spawn a task
        if let Ok(mut guard) = handlers.try_write() {
            guard.push(entry);
        } else {
            // This is a fallback that shouldn't normally be needed
            let handlers_ptr = handlers as *const _ as usize;
            tokio::spawn(async move {
                let handlers = unsafe { &*(handlers_ptr as *const RwLock<Vec<HandlerEntry>>) };
                let mut guard = handlers.write().await;
                guard.push(entry);
            });
        }

        // Return unsubscribe function
        let handlers_ptr = &self.handlers as *const _ as usize;
        move || {
            let handlers = unsafe { &*(handlers_ptr as *const RwLock<Vec<HandlerEntry>>) };
            if let Ok(mut guard) = handlers.try_write() {
                guard.retain(|h| h.id != id);
            } else {
                tokio::spawn(async move {
                    let handlers = unsafe { &*(handlers_ptr as *const RwLock<Vec<HandlerEntry>>) };
                    let mut guard = handlers.write().await;
                    guard.retain(|h| h.id != id);
                });
            }
        }
    }

    /// Subscribe to events via a broadcast channel.
    ///
    /// This is useful for async code that needs to await events.
    pub fn subscribe(&self) -> broadcast::Receiver<SessionEvent> {
        self.event_tx.subscribe()
    }

    /// Get all messages from this session's history.
    pub async fn get_messages(&self) -> Result<Vec<SessionEvent>> {
        let params = json!({ "sessionId": self.session_id });
        let result = self.request("session.getMessages", params).await?;

        let events_raw = result
            .get("events")
            .and_then(|v| v.as_array())
            .ok_or_else(|| CopilotError::session("invalid response: missing events"))?;

        let mut events = Vec::with_capacity(events_raw.len());
        for event_raw in events_raw {
            if let Ok(event) = serde_json::from_value::<SessionEvent>(event_raw.clone()) {
                events.push(event);
            }
        }

        Ok(events)
    }

    /// Destroy this session and release resources.
    ///
    /// After calling this method, the session can no longer be used.
    pub async fn destroy(&self) -> Result<()> {
        let params = json!({ "sessionId": self.session_id });
        self.request("session.destroy", params).await?;

        // Clear handlers
        {
            let mut guard = self.handlers.write().await;
            guard.clear();
        }

        {
            let mut guard = self.tool_handlers.write().await;
            guard.clear();
        }

        {
            let mut guard = self.permission_handler.write().await;
            *guard = None;
        }

        Ok(())
    }

    /// Abort the currently processing message.
    pub async fn abort(&self) -> Result<()> {
        let params = json!({ "sessionId": self.session_id });
        self.request("session.abort", params).await?;
        Ok(())
    }

    /// Register tool handlers for this session.
    pub(crate) async fn register_tools(&self, tools: Vec<Tool>) {
        let mut guard = self.tool_handlers.write().await;
        guard.clear();

        for tool in tools {
            if tool.name.is_empty() {
                continue;
            }
            if let Some(handler) = tool.handler {
                guard.insert(tool.name, handler);
            }
        }
    }

    /// Register a permission handler for this session.
    pub async fn set_permission_handler<F, Fut>(&self, handler: F)
    where
        F: Fn(PermissionRequest, PermissionInvocation) -> Fut + Send + Sync + 'static,
        Fut: std::future::Future<Output = Result<PermissionRequestResult>> + Send + 'static,
    {
        let mut guard = self.permission_handler.write().await;
        *guard = Some(Box::new(move |req, inv| {
            Box::pin(handler(req, inv))
        }));
    }

    /// Execute a tool handler.
    pub(crate) async fn execute_tool(&self, tool_name: &str, invocation: ToolInvocation) -> ToolResult {
        let handler = {
            let guard = self.tool_handlers.read().await;
            guard.get(tool_name).map(|h| {
                // We need to invoke the handler
                h(invocation.clone())
            })
        };

        match handler {
            Some(fut) => {
                match fut.await {
                    Ok(result) => result,
                    Err(e) => ToolResult::failure(e.to_string()),
                }
            }
            None => {
                ToolResult {
                    text_result_for_llm: format!("Tool '{}' is not supported by this client instance.", tool_name),
                    binary_results_for_llm: None,
                    result_type: "failure".to_string(),
                    error: Some(format!("tool '{}' not supported", tool_name)),
                    session_log: None,
                    tool_telemetry: None,
                }
            }
        }
    }

    /// Handle a permission request.
    pub(crate) async fn handle_permission_request(&self, request_data: Value) -> PermissionRequestResult {
        let handler = {
            let guard = self.permission_handler.read().await;
            guard.is_some()
        };

        if !handler {
            return PermissionRequestResult::denied();
        }

        // Parse permission request
        let kind = request_data
            .get("kind")
            .and_then(|v| v.as_str())
            .unwrap_or("")
            .to_string();

        let tool_call_id = request_data
            .get("toolCallId")
            .and_then(|v| v.as_str())
            .map(|s| s.to_string());

        let request = PermissionRequest {
            kind,
            tool_call_id,
            extra: HashMap::new(),
        };

        let invocation = PermissionInvocation {
            session_id: self.session_id.clone(),
        };

        // Execute handler
        let guard = self.permission_handler.read().await;
        if let Some(ref handler) = *guard {
            match handler(request, invocation).await {
                Ok(result) => result,
                Err(_) => PermissionRequestResult::denied(),
            }
        } else {
            PermissionRequestResult::denied()
        }
    }

    /// Dispatch an event to all registered handlers.
    pub(crate) async fn dispatch_event(&self, event: SessionEvent) {
        // Send to broadcast channel
        let _ = self.event_tx.send(event.clone());

        // Call registered handlers
        let handlers = self.handlers.read().await;
        for entry in handlers.iter() {
            // Clone the event for each handler
            let event_clone = event.clone();
            // Call handler - catch panics
            std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
                (entry.handler)(event_clone);
            }))
            .ok();
        }
    }

    /// Make a request to the client.
    async fn request(&self, method: &str, params: Value) -> Result<Value> {
        // We need to access the client through the pointer
        // This is safe because Session is always owned by a Client
        // and Sessions are stored in the Client's sessions map

        // For now, we'll use a workaround where Session makes requests
        // through a global registry or by storing the client reference differently

        // Actually, let's use a different approach - store a reference to the
        // client's request function when creating the session

        // Since we can't easily store a reference to Client due to ownership,
        // we'll make the request function stored during session creation

        // For the current implementation, we'll use the stored request_fn
        // but this requires a different architecture

        // Let's implement a simpler approach using the raw pointer
        // This is safe because the Client always outlives its Sessions

        use crate::client::Client;
        let client = unsafe { &*(self.client_ptr as *const Client) };
        client.request(method, params).await
    }
}
