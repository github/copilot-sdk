//! Session management for Copilot SDK

use crate::error::{Error, Result};
use crate::generated::SessionEvent;
use crate::jsonrpc::JsonRpcClient;
use crate::tools::{Tool, ToolHandler, ToolInvocation, ToolResult};
use crate::types::{
    MCPServerConfig, PermissionInvocation, PermissionRequest, PermissionRequestResult,
    SystemMessage,
};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::{mpsc, Mutex};

/// Configuration for creating a session
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct SessionConfig {
    /// Model to use (e.g., "gpt-4o", "claude-sonnet-4")
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,

    /// System message configuration
    #[serde(skip_serializing_if = "Option::is_none")]
    pub system_message: Option<SystemMessage>,

    /// MCP servers to use
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_servers: Option<HashMap<String, MCPServerConfig>>,

    /// Working directory for the session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,

    /// Custom metadata
    #[serde(skip_serializing_if = "Option::is_none")]
    pub metadata: Option<HashMap<String, Value>>,
}

/// Options for sending a message
#[derive(Debug, Clone, Default)]
pub struct MessageOptions {
    /// The prompt to send
    pub prompt: String,

    /// Additional context or parameters
    pub context: Option<HashMap<String, Value>>,
}

/// Event callback type
pub type EventCallback = Arc<dyn Fn(SessionEvent) + Send + Sync>;

/// Permission handler callback type
pub type PermissionHandler = Arc<
    dyn Fn(PermissionRequest, PermissionInvocation) -> Result<PermissionRequestResult>
        + Send
        + Sync,
>;

/// A Copilot session for interactive conversations
pub struct Session {
    id: String,
    client: Arc<JsonRpcClient>,
    event_callbacks: Arc<Mutex<Vec<EventCallback>>>,
    tool_handlers: Arc<Mutex<HashMap<String, Arc<dyn ToolHandler>>>>,
    permission_handler: Arc<Mutex<Option<PermissionHandler>>>,
    event_rx: Arc<Mutex<mpsc::UnboundedReceiver<SessionEvent>>>,
}

impl Session {
    /// Create a new session
    pub(crate) fn new(
        id: String,
        client: Arc<JsonRpcClient>,
        event_rx: mpsc::UnboundedReceiver<SessionEvent>,
    ) -> Self {
        Self {
            id,
            client,
            event_callbacks: Arc::new(Mutex::new(Vec::new())),
            tool_handlers: Arc::new(Mutex::new(HashMap::new())),
            permission_handler: Arc::new(Mutex::new(None)),
            event_rx: Arc::new(Mutex::new(event_rx)),
        }
    }

    /// Get the session ID
    pub fn id(&self) -> &str {
        &self.id
    }

    /// Register an event callback
    pub async fn on_event(&self, callback: EventCallback) {
        self.event_callbacks.lock().await.push(callback);
    }

    /// Register a tool handler
    pub async fn register_tool(&self, tool: Tool, handler: Arc<dyn ToolHandler>) -> Result<()> {
        let tool_name = tool.name.clone();

        // Register tool with server
        let mut params = HashMap::new();
        params.insert("sessionId".to_string(), Value::String(self.id.clone()));
        params.insert("tool".to_string(), serde_json::to_value(&tool)?);

        self.client
            .request("session/registerTool".to_string(), params)
            .await?;

        // Store handler locally
        self.tool_handlers.lock().await.insert(tool_name, handler);

        Ok(())
    }

    /// Set permission handler
    pub async fn set_permission_handler(&self, handler: PermissionHandler) {
        *self.permission_handler.lock().await = Some(handler);
    }

    /// Send a message and don't wait for completion
    pub async fn send(&self, prompt: impl Into<String>) -> Result<()> {
        let prompt = prompt.into();
        let mut params = HashMap::new();
        params.insert("sessionId".to_string(), Value::String(self.id.clone()));
        params.insert("prompt".to_string(), Value::String(prompt));

        self.client
            .notify("session/send".to_string(), params)
            .await?;

        Ok(())
    }

    /// Send a message and wait for the response
    pub async fn send_and_wait(&self, prompt: impl Into<String>) -> Result<String> {
        let prompt = prompt.into();
        let mut params = HashMap::new();
        params.insert("sessionId".to_string(), Value::String(self.id.clone()));
        params.insert("prompt".to_string(), Value::String(prompt));

        let result = self
            .client
            .request("session/sendAndWait".to_string(), params)
            .await?;

        // Extract content from response
        let content = result
            .get("content")
            .and_then(|v| v.as_str())
            .unwrap_or("")
            .to_string();

        Ok(content)
    }

    /// Handle an incoming tool call
    pub(crate) async fn handle_tool_call(
        &self,
        tool_name: String,
        tool_call_id: String,
        arguments: HashMap<String, Value>,
    ) -> Result<()> {
        let handlers = self.tool_handlers.lock().await;
        let handler = handlers.get(&tool_name).ok_or_else(|| {
            Error::ToolError(format!("No handler registered for tool: {}", tool_name))
        })?;

        let invocation = ToolInvocation {
            session_id: self.id.clone(),
            tool_call_id: tool_call_id.clone(),
        };

        // Execute tool handler
        let handler = Arc::clone(handler);
        drop(handlers); // Release lock before async operation

        let result = handler.handle(arguments, invocation).await;

        // Send result back
        self.send_tool_result(&tool_call_id, result).await?;

        Ok(())
    }

    /// Send tool result back to the server
    async fn send_tool_result(&self, tool_call_id: &str, result: Result<ToolResult>) -> Result<()> {
        let mut params = HashMap::new();
        params.insert("sessionId".to_string(), Value::String(self.id.clone()));
        params.insert(
            "toolCallId".to_string(),
            Value::String(tool_call_id.to_string()),
        );

        match result {
            Ok(tool_result) => {
                params.insert("result".to_string(), serde_json::to_value(tool_result)?);
            }
            Err(e) => {
                let error_result = ToolResult::text(format!("Tool execution failed: {}", e));
                params.insert("result".to_string(), serde_json::to_value(error_result)?);
            }
        }

        self.client
            .notify("session/toolResult".to_string(), params)
            .await?;

        Ok(())
    }

    /// Handle permission request
    pub(crate) async fn handle_permission_request(
        &self,
        request: PermissionRequest,
    ) -> Result<PermissionRequestResult> {
        let handler = self.permission_handler.lock().await;

        if let Some(h) = handler.as_ref() {
            let invocation = PermissionInvocation {
                session_id: self.id.clone(),
            };
            h(request, invocation)
        } else {
            // Default: allow all
            Ok(PermissionRequestResult {
                kind: "allow".to_string(),
                rules: None,
            })
        }
    }

    /// Emit an event to all registered callbacks
    pub(crate) async fn emit_event(&self, event: SessionEvent) {
        let callbacks = self.event_callbacks.lock().await;
        for callback in callbacks.iter() {
            callback(event.clone());
        }
    }

    /// Start event processing loop
    pub async fn start_event_loop(self: Arc<Self>) {
        let session = Arc::clone(&self);
        tokio::spawn(async move {
            let mut rx = session.event_rx.lock().await;
            while let Some(event) = rx.recv().await {
                session.emit_event(event).await;
            }
        });
    }
}
