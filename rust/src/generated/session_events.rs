//! Generated session event types

use serde::{Deserialize, Serialize};
use serde_json::Value;

/// Session event type
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type", rename_all = "camelCase")]
pub enum SessionEvent {
    /// Assistant message event
    #[serde(rename = "assistant.message")]
    AssistantMessage {
        content: String,
        #[serde(flatten)]
        extra: serde_json::Map<String, Value>,
    },

    /// Message pending event (progress indicator)
    #[serde(rename = "message.pending")]
    MessagePending {
        #[serde(flatten)]
        data: serde_json::Map<String, Value>,
    },

    /// Tool call requested event
    #[serde(rename = "tool.call_requested")]
    ToolCallRequested {
        tool_name: String,
        tool_call_id: String,
        arguments: serde_json::Map<String, Value>,
    },

    /// Permission requested event
    #[serde(rename = "permission.requested")]
    PermissionRequested {
        kind: String,
        #[serde(flatten)]
        data: serde_json::Map<String, Value>,
    },

    /// Session state changed
    #[serde(rename = "session.state_changed")]
    SessionStateChanged {
        state: String,
        #[serde(flatten)]
        data: serde_json::Map<String, Value>,
    },

    /// Error event
    #[serde(rename = "error")]
    Error {
        message: String,
        #[serde(flatten)]
        data: serde_json::Map<String, Value>,
    },

    /// Unknown event type (forward compatibility)
    #[serde(untagged)]
    Unknown {
        #[serde(flatten)]
        data: serde_json::Map<String, Value>,
    },
}
