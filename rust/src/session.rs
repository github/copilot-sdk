use crate::jsonrpc::JsonRpcClient;
use crate::types::{MessageOptions, SessionEvent, SessionEventHandler, SessionGetMessagesResponse};
use anyhow::{Context, Result, anyhow};
use serde_json::json;
use std::io::{BufRead, Write};
use std::sync::{Arc, Mutex};

// Type alias for the boxed RPC client type (matches client.rs)
type BoxedRpcClient = Arc<JsonRpcClient<Box<dyn Write + Send>, Box<dyn BufRead + Send>>>;

/// Represents a conversation session with the Copilot CLI
pub struct Session {
    pub id: String,
    workspace_path: Option<String>,
    rpc_client: BoxedRpcClient,
    event_handlers: Arc<Mutex<Vec<SessionEventHandler>>>,
}

impl Session {
    /// Create a new session
    pub fn new(id: String, rpc_client: BoxedRpcClient, workspace_path: Option<String>) -> Self {
        Self {
            id,
            workspace_path,
            rpc_client,
            event_handlers: Arc::new(Mutex::new(Vec::new())),
        }
    }

    /// Get the workspace path for infinite sessions
    pub fn workspace_path(&self) -> Option<&str> {
        self.workspace_path.as_deref()
    }

    /// Send a message to this session
    pub async fn send(&self, options: MessageOptions) -> Result<String> {
        let mut params = json!({
            "sessionId": self.id,
            "message": {
                "prompt": options.prompt
            }
        });

        if !options.attachments.is_empty() {
            params["message"]["attachments"] = serde_json::to_value(&options.attachments)?;
        }

        if let Some(ref mode) = options.mode {
            params["message"]["mode"] = json!(mode);
        }

        let result = self
            .rpc_client
            .request("session.send", params)
            .await
            .map_err(|e| anyhow!("Failed to send message: {}", e))?;

        let response: serde_json::Value = result;
        let message_id = response
            .get("messageId")
            .and_then(|v| v.as_str())
            .ok_or_else(|| anyhow!("Response missing messageId"))?;

        Ok(message_id.to_string())
    }

    /// Subscribe to session events
    pub fn on(&self, handler: SessionEventHandler) -> impl FnOnce() {
        let mut handlers = self.event_handlers.lock().unwrap();
        let index = handlers.len();
        handlers.push(handler);

        // Return unsubscribe function
        let event_handlers = Arc::clone(&self.event_handlers);
        move || {
            let mut handlers = event_handlers.lock().unwrap();
            if index < handlers.len() {
                let _ = handlers.remove(index);
            }
        }
    }

    /// Get messages from the session
    pub async fn get_messages(&self) -> Result<Vec<SessionEvent>> {
        let params = json!({
            "sessionId": self.id
        });

        let result = self
            .rpc_client
            .request("session.getMessages", params)
            .await
            .map_err(|e| anyhow!("Failed to get messages: {}", e))?;

        let response: SessionGetMessagesResponse =
            serde_json::from_value(result).context("Failed to parse get messages response")?;

        Ok(response.events)
    }

    /// Destroy the session
    pub async fn destroy(&self) -> Result<()> {
        let params = json!({
            "sessionId": self.id
        });

        self.rpc_client
            .request("session.destroy", params)
            .await
            .map_err(|e| anyhow!("Failed to destroy session: {}", e))?;

        Ok(())
    }

    /// Abort the current operation
    pub async fn abort(&self) -> Result<()> {
        let params = json!({
            "sessionId": self.id
        });

        self.rpc_client
            .request("session.abort", params)
            .await
            .map_err(|e| anyhow!("Failed to abort session: {}", e))?;

        Ok(())
    }

    /// Dispatch an event to all registered handlers
    pub fn dispatch_event(&self, event: SessionEvent) {
        let handlers = self.event_handlers.lock().unwrap();
        for handler in handlers.iter() {
            // Call handler - catch panics to prevent crashing the dispatcher
            let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
                handler(event.clone());
            }));
            if let Err(e) = result {
                eprintln!("Error in session event handler: {:?}", e);
            }
        }
    }
}
