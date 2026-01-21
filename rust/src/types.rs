//! Core type definitions for the Copilot SDK.

use serde::{Deserialize, Serialize};
use std::collections::HashMap;

/// Connection state of the client.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ConnectionState {
    Disconnected,
    Connecting,
    Connected,
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
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct McpLocalServerConfig {
    pub tools: Vec<String>,
    #[serde(rename = "type", skip_serializing_if = "Option::is_none")]
    pub server_type: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<i32>,
    pub command: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub args: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub env: Option<HashMap<String, String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
}

/// Configuration for a remote MCP server (HTTP or SSE).
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct McpRemoteServerConfig {
    pub tools: Vec<String>,
    #[serde(rename = "type")]
    pub server_type: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<i32>,
    pub url: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub headers: Option<HashMap<String, String>>,
}

/// MCP server configuration (can be local or remote).
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum McpServerConfig {
    Local(McpLocalServerConfig),
    Remote(McpRemoteServerConfig),
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
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "lowercase")]
pub enum AttachmentType {
    File,
    Directory,
}

/// Response from a ping request.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PingResponse {
    pub message: String,
    pub timestamp: i64,
    #[serde(rename = "protocolVersion")]
    pub protocol_version: Option<i32>,
}

/// Permission request from the server.
#[derive(Debug, Clone)]
pub struct PermissionRequest {
    pub kind: String,
    pub tool_call_id: Option<String>,
    pub extra: serde_json::Value,
}

/// Result of a permission request.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PermissionRequestResult {
    pub kind: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub rules: Option<Vec<serde_json::Value>>,
}

/// Context for a permission request.
#[derive(Debug, Clone)]
pub struct PermissionInvocation {
    pub session_id: String,
}

/// Session metadata returned by list_sessions.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SessionMetadata {
    #[serde(rename = "sessionId")]
    pub session_id: String,
}

/// SDK protocol version constant.
pub const SDK_PROTOCOL_VERSION: i32 = 1;

/// Returns the SDK protocol version.
pub fn get_sdk_protocol_version() -> i32 {
    SDK_PROTOCOL_VERSION
}
