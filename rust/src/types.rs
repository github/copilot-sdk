//! Core type definitions for the Copilot SDK.
//!
//! This module contains all the configuration types, enums, and data structures
//! used throughout the SDK for client configuration, session management, and
//! message handling.
//!
//! # Main Types
//!
//! - [`ClientOptions`] - Configuration for creating a [`CopilotClient`](crate::CopilotClient)
//! - [`SessionConfig`] - Configuration for creating a new session
//! - [`MessageOptions`] - Options for sending messages to a session
//! - [`ConnectionState`] - Current connection state of the client

use serde::{Deserialize, Serialize};
use std::collections::HashMap;

/// Connection state of the client.
///
/// Represents the current state of the connection between the SDK and the
/// Copilot CLI server. Use [`CopilotClient::get_state()`](crate::CopilotClient::get_state)
/// to retrieve the current state.
///
/// # Example
///
/// ```ignore
/// use copilot_sdk::{CopilotClient, ConnectionState};
///
/// let client = CopilotClient::new(None)?;
/// assert_eq!(client.get_state(), ConnectionState::Disconnected);
///
/// client.start().await?;
/// assert_eq!(client.get_state(), ConnectionState::Connected);
/// ```
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ConnectionState {
    /// Client is not connected to the CLI server.
    ///
    /// This is the initial state before [`start()`](crate::CopilotClient::start) is called,
    /// or after [`stop()`](crate::CopilotClient::stop) completes.
    Disconnected,

    /// Client is in the process of connecting to the CLI server.
    ///
    /// This transient state occurs during [`start()`](crate::CopilotClient::start)
    /// while the connection is being established.
    Connecting,

    /// Client is connected and ready to use.
    ///
    /// The client can create sessions and send messages in this state.
    Connected,

    /// An error occurred with the connection.
    ///
    /// This state indicates the connection failed or was lost unexpectedly.
    /// Check logs for details and consider calling [`start()`](crate::CopilotClient::start)
    /// again to reconnect.
    Error,
}

impl std::fmt::Display for ConnectionState {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            ConnectionState::Disconnected => write!(f, "disconnected"),
            ConnectionState::Connecting => write!(f, "connecting"),
            ConnectionState::Connected => write!(f, "connected"),
            ConnectionState::Error => write!(f, "error"),
        }
    }
}

/// Options for configuring the CopilotClient.
#[derive(Debug, Clone, Default)]
pub struct ClientOptions {
    /// Path to the Copilot CLI executable (default: "copilot").
    pub cli_path: Option<String>,

    /// Working directory for the CLI process.
    pub cwd: Option<String>,

    /// Port for TCP transport (default: 0 = random port).
    pub port: Option<u16>,

    /// Enable stdio transport instead of TCP (default: true).
    pub use_stdio: Option<bool>,

    /// URL of an existing Copilot CLI server to connect to over TCP.
    /// Format: "host:port", "http://host:port", or just "port" (defaults to localhost).
    /// Mutually exclusive with cli_path and use_stdio.
    pub cli_url: Option<String>,

    /// Log level for the CLI server.
    pub log_level: Option<String>,

    /// Automatically start the CLI server on first use (default: true).
    pub auto_start: Option<bool>,

    /// Automatically restart the CLI server if it crashes (default: true).
    pub auto_restart: Option<bool>,

    /// Environment variables for the CLI process.
    pub env: Option<Vec<(String, String)>>,
}

/// System message configuration for session creation.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SystemMessageConfig {
    /// Mode: "append" or "replace".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<String>,

    /// Content for the system message.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub content: Option<String>,
}

/// Configuration for a local/stdio MCP server.
///
/// Local MCP servers are spawned as child processes and communicate via stdio.
/// This is the most common configuration for MCP servers running on the same machine.
///
/// # Example
///
/// ```ignore
/// use copilot_sdk::McpLocalServerConfig;
///
/// let config = McpLocalServerConfig {
///     tools: vec!["read_file".to_string(), "write_file".to_string()],
///     server_type: None,  // Defaults to "stdio"
///     timeout: Some(30000),
///     command: "npx".to_string(),
///     args: Some(vec!["-y".to_string(), "@modelcontextprotocol/server-filesystem".to_string()]),
///     env: None,
///     cwd: Some("/path/to/project".to_string()),
/// };
/// ```
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct McpLocalServerConfig {
    /// List of tool names this server provides.
    pub tools: Vec<String>,

    /// Server type (typically "stdio" for local servers).
    #[serde(rename = "type", skip_serializing_if = "Option::is_none")]
    pub server_type: Option<String>,

    /// Timeout in milliseconds for server operations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<i32>,

    /// Command to execute to start the server.
    pub command: String,

    /// Arguments to pass to the command.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub args: Option<Vec<String>>,

    /// Environment variables to set for the server process.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub env: Option<HashMap<String, String>>,

    /// Working directory for the server process.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
}

/// Configuration for a remote MCP server (HTTP or SSE).
///
/// Remote MCP servers communicate over HTTP or Server-Sent Events (SSE).
/// Use this for MCP servers hosted on remote machines or as web services.
///
/// # Example
///
/// ```ignore
/// use copilot_sdk::McpRemoteServerConfig;
/// use std::collections::HashMap;
///
/// let config = McpRemoteServerConfig {
///     tools: vec!["search".to_string()],
///     server_type: "sse".to_string(),
///     timeout: Some(60000),
///     url: "https://mcp.example.com/sse".to_string(),
///     headers: Some(HashMap::from([
///         ("Authorization".to_string(), "Bearer token".to_string()),
///     ])),
/// };
/// ```
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct McpRemoteServerConfig {
    /// List of tool names this server provides.
    pub tools: Vec<String>,

    /// Server type: "http" or "sse".
    #[serde(rename = "type")]
    pub server_type: String,

    /// Timeout in milliseconds for server operations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<i32>,

    /// URL of the remote MCP server.
    pub url: String,

    /// HTTP headers to include in requests to the server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub headers: Option<HashMap<String, String>>,
}

/// MCP server configuration (can be local or remote).
///
/// This enum represents different types of MCP server configurations.
/// Use [`McpLocalServerConfig`] for locally-spawned servers or
/// [`McpRemoteServerConfig`] for remote HTTP/SSE servers.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum McpServerConfig {
    /// Local MCP server spawned as a child process.
    Local(McpLocalServerConfig),
    /// Remote MCP server accessed via HTTP or SSE.
    Remote(McpRemoteServerConfig),
    /// Raw JSON configuration for advanced use cases.
    Raw(serde_json::Value),
}

/// Configuration for a custom agent.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CustomAgentConfig {
    /// Unique name of the custom agent.
    pub name: String,

    /// Display name for UI purposes.
    #[serde(rename = "displayName", skip_serializing_if = "Option::is_none")]
    pub display_name: Option<String>,

    /// Description of what the agent does.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,

    /// List of tool names the agent can use (None for all tools).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tools: Option<Vec<String>>,

    /// Prompt content for the agent.
    pub prompt: String,

    /// MCP servers specific to this agent.
    #[serde(rename = "mcpServers", skip_serializing_if = "Option::is_none")]
    pub mcp_servers: Option<HashMap<String, McpServerConfig>>,

    /// Whether the agent should be available for model inference.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub infer: Option<bool>,
}

/// Azure-specific provider configuration.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AzureProviderOptions {
    /// Azure API version (default: "2024-10-21").
    #[serde(rename = "apiVersion", skip_serializing_if = "Option::is_none")]
    pub api_version: Option<String>,
}

/// Configuration for a custom model provider.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ProviderConfig {
    /// Provider type: "openai", "azure", or "anthropic" (default: "openai").
    #[serde(rename = "type", skip_serializing_if = "Option::is_none")]
    pub provider_type: Option<String>,

    /// API format (openai/azure only): "completions" or "responses" (default: "completions").
    #[serde(rename = "wireApi", skip_serializing_if = "Option::is_none")]
    pub wire_api: Option<String>,

    /// API endpoint URL.
    #[serde(rename = "baseUrl")]
    pub base_url: String,

    /// API key. Optional for local providers like Ollama.
    #[serde(rename = "apiKey", skip_serializing_if = "Option::is_none")]
    pub api_key: Option<String>,

    /// Bearer token for authentication.
    #[serde(rename = "bearerToken", skip_serializing_if = "Option::is_none")]
    pub bearer_token: Option<String>,

    /// Azure-specific options.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub azure: Option<AzureProviderOptions>,
}

/// Configuration for creating a new session.
#[derive(Debug, Clone, Default)]
pub struct SessionConfig {
    /// Optional custom session ID.
    pub session_id: Option<String>,

    /// Model to use for this session.
    pub model: Option<String>,

    /// Override the default configuration directory location.
    pub config_dir: Option<String>,

    /// Caller-implemented tools to expose to the CLI.
    pub tools: Vec<crate::tool::Tool>,

    /// System message customization.
    pub system_message: Option<SystemMessageConfig>,

    /// List of tool names to allow. When specified, only these tools will be available.
    pub available_tools: Option<Vec<String>>,

    /// List of tool names to disable. All other tools remain available.
    pub excluded_tools: Option<Vec<String>>,

    /// Enable streaming of assistant message and reasoning chunks.
    pub streaming: Option<bool>,

    /// Custom model provider (BYOK).
    pub provider: Option<ProviderConfig>,

    /// MCP servers for the session.
    pub mcp_servers: Option<HashMap<String, McpServerConfig>>,

    /// Custom agents for the session.
    pub custom_agents: Option<Vec<CustomAgentConfig>>,

    /// Directories to load skills from.
    pub skill_directories: Option<Vec<String>>,

    /// Skill names to disable.
    pub disabled_skills: Option<Vec<String>>,
}

/// Configuration for resuming an existing session.
#[derive(Debug, Clone, Default)]
pub struct ResumeSessionConfig {
    /// Caller-implemented tools to expose to the CLI.
    pub tools: Vec<crate::tool::Tool>,

    /// Custom model provider.
    pub provider: Option<ProviderConfig>,

    /// Enable streaming of assistant message and reasoning chunks.
    pub streaming: Option<bool>,

    /// MCP servers for the session.
    pub mcp_servers: Option<HashMap<String, McpServerConfig>>,

    /// Custom agents for the session.
    pub custom_agents: Option<Vec<CustomAgentConfig>>,

    /// Directories to load skills from.
    pub skill_directories: Option<Vec<String>>,

    /// Skill names to disable.
    pub disabled_skills: Option<Vec<String>>,
}

/// Options for sending a message.
#[derive(Debug, Clone, Default)]
pub struct MessageOptions {
    /// The message to send.
    pub prompt: String,

    /// File or directory attachments.
    pub attachments: Option<Vec<Attachment>>,

    /// Message delivery mode (default: "enqueue").
    pub mode: Option<String>,
}

/// File or directory attachment.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Attachment {
    /// Display name for the attachment.
    #[serde(rename = "displayName")]
    pub display_name: String,

    /// Path to the file or directory.
    pub path: String,

    /// Type: "file" or "directory".
    #[serde(rename = "type")]
    pub attachment_type: AttachmentType,
}

/// Attachment type.
///
/// Specifies whether an [`Attachment`] refers to a single file or a directory.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "lowercase")]
pub enum AttachmentType {
    /// A single file attachment.
    File,
    /// A directory attachment (contents may be recursively included).
    Directory,
}

/// Response from a ping request.
///
/// Returned by [`CopilotClient::ping()`](crate::CopilotClient::ping) to verify
/// connectivity and protocol compatibility with the CLI server.
///
/// # Example
///
/// ```ignore
/// let response = client.ping(Some("hello")).await?;
/// println!("Server responded: {}", response.message);
/// println!("Protocol version: {:?}", response.protocol_version);
/// ```
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PingResponse {
    /// Echo of the ping message, or default response if none was sent.
    pub message: String,

    /// Unix timestamp (milliseconds) when the server processed the ping.
    pub timestamp: i64,

    /// Protocol version reported by the server.
    ///
    /// Used to verify SDK and server compatibility. If `None`, the server
    /// may be an older version that doesn't report protocol versions.
    #[serde(rename = "protocolVersion")]
    pub protocol_version: Option<i32>,
}

/// Permission request from the server.
///
/// Sent by the CLI server when an operation requires user permission.
/// The SDK can be configured with a permission handler to respond to these requests.
#[derive(Debug, Clone)]
pub struct PermissionRequest {
    /// Type of permission being requested (e.g., "file_write", "shell_execute").
    pub kind: String,

    /// ID of the tool call that triggered this permission request.
    pub tool_call_id: Option<String>,

    /// Additional context about the permission request as JSON.
    pub extra: serde_json::Value,
}

/// Result of a permission request.
///
/// Returned by permission handlers to grant or deny permission requests.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PermissionRequestResult {
    /// Type of permission (should match the request's kind).
    pub kind: String,

    /// Permission rules to apply. If `None`, permission is denied.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub rules: Option<Vec<serde_json::Value>>,
}

/// Context for a permission request.
///
/// Provides additional context to permission handlers about where the
/// permission request originated.
#[derive(Debug, Clone)]
pub struct PermissionInvocation {
    /// ID of the session that triggered the permission request.
    pub session_id: String,
}

/// Session metadata returned by list_sessions.
///
/// Contains basic information about an existing session. Use
/// [`CopilotClient::list_sessions()`](crate::CopilotClient::list_sessions)
/// to retrieve all available sessions.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SessionMetadata {
    /// Unique identifier for the session.
    #[serde(rename = "sessionId")]
    pub session_id: String,
}

/// SDK protocol version constant.
pub const SDK_PROTOCOL_VERSION: i32 = 1;

/// Returns the SDK protocol version.
pub fn get_sdk_protocol_version() -> i32 {
    SDK_PROTOCOL_VERSION
}
