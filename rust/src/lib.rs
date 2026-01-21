//! Rust SDK for programmatic access to the GitHub Copilot CLI.
//!
//! This crate provides a Rust interface for interacting with the Copilot CLI server,
//! creating and managing conversation sessions, and integrating custom tools.
//!
//! # Quick Start
//!
//! ```ignore
//! use copilot_sdk::{CopilotClient, SessionConfig, MessageOptions};
//!
//! #[tokio::main]
//! async fn main() -> Result<(), Box<dyn std::error::Error>> {
//!     // Create a client (spawns CLI server automatically)
//!     let client = CopilotClient::new(None);
//!     client.start().await?;
//!
//!     // Create a session
//!     let session = client.create_session(Some(SessionConfig {
//!         model: Some("gpt-4".to_string()),
//!         ..Default::default()
//!     })).await?;
//!
//!     // Subscribe to events
//!     let _unsubscribe = session.on(std::sync::Arc::new(|event| {
//!         if event.event_type == copilot_sdk::SessionEventType::AssistantMessage {
//!             if let Some(content) = &event.data.content {
//!                 println!("Assistant: {}", content);
//!             }
//!         }
//!     }));
//!
//!     // Send a message
//!     session.send(MessageOptions {
//!         prompt: "Hello, Copilot!".to_string(),
//!         ..Default::default()
//!     }).await?;
//!
//!     // Clean up
//!     client.stop().await;
//!
//!     Ok(())
//! }
//! ```
//!
//! # Custom Tools
//!
//! You can define custom tools that the assistant can invoke:
//!
//! ```ignore
//! use copilot_sdk::{define_tool, SessionConfig};
//! use schemars::JsonSchema;
//! use serde::Deserialize;
//!
//! #[derive(Deserialize, JsonSchema)]
//! struct GetWeatherParams {
//!     city: String,
//! }
//!
//! let tool = define_tool::<GetWeatherParams, _, _, _>(
//!     "get_weather",
//!     "Get weather for a city",
//!     |params, _inv| async move {
//!         Ok(format!("Weather in {}: 22 degrees", params.city))
//!     },
//! );
//!
//! let session = client.create_session(Some(SessionConfig {
//!     tools: vec![tool],
//!     ..Default::default()
//! })).await?;
//! ```

#![warn(missing_docs)]
#![warn(rustdoc::missing_crate_level_docs)]

pub mod client;
pub mod error;
pub mod generated;
pub mod jsonrpc;
pub mod session;
pub mod tool;
pub mod types;

// Re-export main types at crate root
pub use client::CopilotClient;
pub use error::{CopilotError, JsonRpcError, Result};
pub use generated::{SessionEvent, SessionEventData, SessionEventType};
pub use session::{CopilotSession, SessionEventHandler, UnsubscribeFn};
pub use tool::{
    define_tool, IntoToolResult, Tool, ToolBinaryResult, ToolBuilder, ToolHandler, ToolInvocation,
    ToolResult,
};
pub use types::{
    Attachment, AttachmentType, AzureProviderOptions, ClientOptions, ConnectionState,
    CustomAgentConfig, McpLocalServerConfig, McpRemoteServerConfig, McpServerConfig,
    MessageOptions, PermissionInvocation, PermissionRequest, PermissionRequestResult, PingResponse,
    ProviderConfig, ResumeSessionConfig, SessionConfig, SessionMetadata, SystemMessageConfig,
    get_sdk_protocol_version, SDK_PROTOCOL_VERSION,
};

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_sdk_protocol_version() {
        assert_eq!(get_sdk_protocol_version(), SDK_PROTOCOL_VERSION);
        assert_eq!(SDK_PROTOCOL_VERSION, 1);
    }

    #[test]
    fn test_connection_state_display() {
        assert_eq!(ConnectionState::Disconnected.to_string(), "disconnected");
        assert_eq!(ConnectionState::Connecting.to_string(), "connecting");
        assert_eq!(ConnectionState::Connected.to_string(), "connected");
        assert_eq!(ConnectionState::Error.to_string(), "error");
    }

    #[test]
    fn test_session_event_type_display() {
        assert_eq!(
            SessionEventType::AssistantMessage.to_string(),
            "assistant.message"
        );
        assert_eq!(SessionEventType::SessionIdle.to_string(), "session.idle");
        assert_eq!(
            SessionEventType::ToolExecutionStart.to_string(),
            "tool.execution_start"
        );
    }
}
