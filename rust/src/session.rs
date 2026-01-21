//! CopilotSession implementation for managing conversation sessions.

use crate::error::{CopilotError, Result};
use crate::generated::{SessionEvent, SessionEventType};
use crate::jsonrpc::JsonRpcClient;
use crate::tool::{Tool, ToolHandler, ToolInvocation, ToolResult};
use crate::types::MessageOptions;
use serde_json::json;
use std::collections::HashMap;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::Arc;
use std::time::Duration;
use tokio::sync::{mpsc, RwLock};

/// Callback type for session events.
pub type SessionEventHandler = Arc<dyn Fn(SessionEvent) + Send + Sync>;

/// Unsubscribe function returned by `on()`.
pub type UnsubscribeFn = Box<dyn FnOnce() + Send>;

struct EventHandler {
    id: u64,
    handler: SessionEventHandler,
}

/// A session for conversing with the Copilot CLI.
///
/// Sessions maintain conversation state, handle events, and manage tool execution.
/// Sessions are created via [`CopilotClient::create_session`] or resumed via
/// [`CopilotClient::resume_session`].
///
/// # Example
///
/// ```ignore
/// use copilot_sdk::{CopilotClient, MessageOptions};
///
/// #[tokio::main]
/// async fn main() -> Result<(), Box<dyn std::error::Error>> {
///     let client = CopilotClient::new(None);
///     let session = client.create_session(None).await?;
///
///     // Subscribe to events
///     let _unsubscribe = session.on(|event| {
///         println!("Event: {:?}", event.event_type);
///     });
///
///     // Send a message
///     let message_id = session.send(MessageOptions {
///         prompt: "Hello!".to_string(),
///         ..Default::default()
///     }).await?;
///
///     Ok(())
/// }
/// ```
pub struct CopilotSession {
    session_id: String,
    rpc_client: Arc<JsonRpcClient>,
    handlers: Arc<RwLock<Vec<EventHandler>>>,
    next_handler_id: AtomicU64,
    tool_handlers: RwLock<HashMap<String, ToolHandler>>,
}

impl CopilotSession {
    /// Create a new session wrapper.
    ///
    /// Note: This is primarily for internal use. Use `CopilotClient::create_session`
    /// to create sessions with proper initialization.
    pub(crate) fn new(session_id: String, rpc_client: Arc<JsonRpcClient>) -> Self {
        Self {
            session_id,
            rpc_client,
            handlers: Arc::new(RwLock::new(Vec::new())),
            next_handler_id: AtomicU64::new(0),
            tool_handlers: RwLock::new(HashMap::new()),
        }
    }

    /// Get the session ID.
    pub fn session_id(&self) -> &str {
        &self.session_id
    }

    /// Send a message to this session.
    ///
    /// The message is processed asynchronously. Subscribe to events via [`on`]
    /// to receive streaming responses and other session events.
    ///
    /// Returns the message ID of the response.
    pub async fn send(&self, options: MessageOptions) -> Result<String> {
        let mut params = json!({
            "sessionId": self.session_id,
            "prompt": options.prompt,
        });

        if let Some(ref attachments) = options.attachments {
            params["attachments"] = json!(attachments);
        }
        if let Some(ref mode) = options.mode {
            params["mode"] = json!(mode);
        }

        let result = self.rpc_client.request("session.send", params).await?;

        let message_id = result
            .get("messageId")
            .and_then(|v| v.as_str())
            .ok_or_else(|| CopilotError::InvalidResponse("missing messageId".to_string()))?
            .to_string();

        Ok(message_id)
    }

    /// Send a message and wait for the session to become idle.
    ///
    /// This is a convenience method that combines [`send`] with waiting for
    /// the `session.idle` event.
    ///
    /// Events are still delivered to handlers registered via [`on`] while waiting.
    ///
    /// Returns the final assistant message event, or None if none was received.
    pub async fn send_and_wait(
        &self,
        options: MessageOptions,
        timeout: Option<Duration>,
    ) -> Result<Option<SessionEvent>> {
        let timeout = timeout.unwrap_or(Duration::from_secs(60));

        let (idle_tx, mut idle_rx) = mpsc::channel::<()>(1);
        let (error_tx, mut error_rx) = mpsc::channel::<String>(1);
        let last_assistant_message: Arc<RwLock<Option<SessionEvent>>> = Arc::new(RwLock::new(None));

        let last_msg = last_assistant_message.clone();
        let idle_tx_clone = idle_tx.clone();
        let error_tx_clone = error_tx.clone();

        let unsubscribe = self.on(Arc::new(move |event: SessionEvent| {
            let last_msg = last_msg.clone();
            let idle_tx = idle_tx_clone.clone();
            let error_tx = error_tx_clone.clone();

            tokio::spawn(async move {
                match event.event_type {
                    SessionEventType::AssistantMessage => {
                        let mut last = last_msg.write().await;
                        *last = Some(event);
                    }
                    SessionEventType::SessionIdle => {
                        let _ = idle_tx.send(()).await;
                    }
                    SessionEventType::SessionError => {
                        let msg = event
                            .data
                            .message
                            .clone()
                            .unwrap_or_else(|| "session error".to_string());
                        let _ = error_tx.send(msg).await;
                    }
                    _ => {}
                }
            });
        }));

        // Send the message
        self.send(options).await?;

        // Wait for idle, error, or timeout
        let result = tokio::select! {
            _ = idle_rx.recv() => {
                let last = last_assistant_message.read().await;
                Ok(last.clone())
            }
            Some(err) = error_rx.recv() => {
                Err(CopilotError::Session(format!("session error: {}", err)))
            }
            _ = tokio::time::sleep(timeout) => {
                Err(CopilotError::Timeout)
            }
        };

        // Unsubscribe
        unsubscribe();

        result
    }

    /// Subscribe to events from this session.
    ///
    /// Events include assistant messages, tool executions, errors, and session state
    /// changes. Multiple handlers can be registered and will all receive events.
    ///
    /// Returns a function that can be called to unsubscribe the handler.
    pub fn on(&self, handler: SessionEventHandler) -> impl FnOnce() + Send {
        let id = self.next_handler_id.fetch_add(1, Ordering::SeqCst);

        // We need to handle this synchronously to return immediately
        let handlers = self.handlers.clone();
        let rt = tokio::runtime::Handle::current();
        rt.block_on(async {
            let mut h = handlers.write().await;
            h.push(EventHandler { id, handler });
        });

        // Return unsubscribe closure
        let handlers = self.handlers.clone();
        move || {
            let rt = tokio::runtime::Handle::try_current();
            match rt {
                Ok(handle) => {
                    handle.block_on(async {
                        let mut h = handlers.write().await;
                        h.retain(|h| h.id != id);
                    });
                }
                Err(_) => {
                    // We're not in a tokio context, spawn a new runtime
                    let rt = tokio::runtime::Runtime::new().unwrap();
                    rt.block_on(async {
                        let mut h = handlers.write().await;
                        h.retain(|h| h.id != id);
                    });
                }
            }
        }
    }

    /// Get all events and messages from this session's history.
    pub async fn get_messages(&self) -> Result<Vec<SessionEvent>> {
        let params = json!({
            "sessionId": self.session_id,
        });

        let result = self.rpc_client.request("session.getMessages", params).await?;

        let events_raw = result
            .get("events")
            .and_then(|v| v.as_array())
            .ok_or_else(|| CopilotError::InvalidResponse("missing events".to_string()))?;

        let events: Vec<SessionEvent> = events_raw
            .iter()
            .filter_map(|v| serde_json::from_value(v.clone()).ok())
            .collect();

        Ok(events)
    }

    /// Abort the currently processing message in this session.
    pub async fn abort(&self) -> Result<()> {
        let params = json!({
            "sessionId": self.session_id,
        });

        self.rpc_client.request("session.abort", params).await?;
        Ok(())
    }

    /// Destroy this session and release all associated resources.
    ///
    /// After calling this method, the session can no longer be used.
    pub async fn destroy(&self) -> Result<()> {
        let params = json!({
            "sessionId": self.session_id,
        });

        self.rpc_client.request("session.destroy", params).await?;

        // Clear handlers
        {
            let mut handlers = self.handlers.write().await;
            handlers.clear();
        }

        // Clear tool handlers
        {
            let mut tool_handlers = self.tool_handlers.write().await;
            tool_handlers.clear();
        }

        Ok(())
    }

    /// Register tools for this session.
    pub(crate) async fn register_tools(&self, tools: Vec<Tool>) {
        let mut handlers = self.tool_handlers.write().await;
        handlers.clear();
        for tool in tools {
            if !tool.name.is_empty() {
                handlers.insert(tool.name.clone(), tool.handler.clone());
            }
        }
    }

    /// Get a tool handler by name.
    pub(crate) async fn get_tool_handler(&self, name: &str) -> Option<ToolHandler> {
        let handlers = self.tool_handlers.read().await;
        handlers.get(name).cloned()
    }

    /// Execute a tool.
    pub(crate) async fn execute_tool(
        &self,
        tool_name: &str,
        invocation: ToolInvocation,
    ) -> Result<ToolResult> {
        let handler = self.get_tool_handler(tool_name).await;

        match handler {
            Some(handler) => {
                // Execute the tool handler
                match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
                    let rt = tokio::runtime::Handle::current();
                    rt.block_on(handler(invocation))
                })) {
                    Ok(result) => result,
                    Err(_) => Ok(ToolResult::failure(format!(
                        "tool panic: {}",
                        tool_name
                    ))),
                }
            }
            None => Ok(ToolResult::unsupported(tool_name)),
        }
    }

    /// Dispatch an event to all registered handlers.
    pub(crate) async fn dispatch_event(&self, event: SessionEvent) {
        let handlers: Vec<SessionEventHandler> = {
            let h = self.handlers.read().await;
            h.iter().map(|h| h.handler.clone()).collect()
        };

        for handler in handlers {
            // Don't let panics crash the dispatcher
            let event_clone = event.clone();
            let _ = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
                handler(event_clone);
            }));
        }
    }
}
