//! Core type definitions for the Copilot SDK.

use serde::{Deserialize, Serialize};
use std::collections::HashMap;

use crate::error::Result;
use crate::generated::SessionEvent;

/// Connection state of the client.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum ConnectionState {
    /// Not connected to the CLI server.
    #[default]
    Disconnected,
    /// Currently connecting to the CLI server.
    Connecting,
    /// Connected to the CLI server.
    Connected,
    /// Connection error occurred.
    Error,
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
    /// Use stdio transport instead of TCP (default: true).
    pub use_stdio: Option<bool>,
    /// URL of an existing Copilot CLI server to connect to over TCP.
    /// Format: "host:port", "http://host:port", or just "port" (defaults to localhost).
    /// Mutually exclusive with cli_path, use_stdio.
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

impl ClientOptions {
    /// Create new client options with defaults.
    pub fn new() -> Self {
        Self::default()
    }

    /// Set the CLI path.
    pub fn cli_path(mut self, path: impl Into<String>) -> Self {
        self.cli_path = Some(path.into());
        self
    }

    /// Set the working directory.
    pub fn cwd(mut self, cwd: impl Into<String>) -> Self {
        self.cwd = Some(cwd.into());
        self
    }

    /// Set the port for TCP transport.
    pub fn port(mut self, port: u16) -> Self {
        self.port = Some(port);
        self
    }

    /// Set whether to use stdio transport.
    pub fn use_stdio(mut self, use_stdio: bool) -> Self {
        self.use_stdio = Some(use_stdio);
        self
    }

    /// Set the CLI URL for connecting to an existing server.
    pub fn cli_url(mut self, url: impl Into<String>) -> Self {
        self.cli_url = Some(url.into());
        self
    }

    /// Set the log level.
    pub fn log_level(mut self, level: impl Into<String>) -> Self {
        self.log_level = Some(level.into());
        self
    }

    /// Set auto-start behavior.
    pub fn auto_start(mut self, auto_start: bool) -> Self {
        self.auto_start = Some(auto_start);
        self
    }

    /// Set auto-restart behavior.
    pub fn auto_restart(mut self, auto_restart: bool) -> Self {
        self.auto_restart = Some(auto_restart);
        self
    }

    /// Set environment variables.
    pub fn env(mut self, env: Vec<(String, String)>) -> Self {
        self.env = Some(env);
        self
    }
}

/// System message configuration mode.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SystemMessageConfig {
    /// Mode: "append" (default) or "replace".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<String>,
    /// Content to append or replace with.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub content: Option<String>,
}

impl SystemMessageConfig {
    /// Create append mode configuration.
    pub fn append(content: impl Into<String>) -> Self {
        Self {
            mode: Some("append".to_string()),
            content: Some(content.into()),
        }
    }

    /// Create replace mode configuration.
    pub fn replace(content: impl Into<String>) -> Self {
        Self {
            mode: Some("replace".to_string()),
            content: Some(content.into()),
        }
    }
}

/// Permission request from the server.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequest {
    /// The kind of permission being requested.
    pub kind: String,
    /// The tool call ID associated with the permission request.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
    /// Additional fields vary by kind.
    #[serde(flatten)]
    pub extra: HashMap<String, serde_json::Value>,
}

/// Result of a permission request.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequestResult {
    /// The kind of result.
    pub kind: String,
    /// Optional rules for the permission.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub rules: Option<Vec<serde_json::Value>>,
}

impl PermissionRequestResult {
    /// Create a denied result.
    pub fn denied() -> Self {
        Self {
            kind: "denied-no-approval-rule-and-could-not-request-from-user".to_string(),
            rules: None,
        }
    }

    /// Create an approved result.
    pub fn approved() -> Self {
        Self {
            kind: "approved".to_string(),
            rules: None,
        }
    }
}

/// Context for a permission invocation.
#[derive(Debug, Clone)]
pub struct PermissionInvocation {
    /// The session ID.
    pub session_id: String,
}

/// Configuration for a local/stdio MCP server.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpLocalServerConfig {
    /// List of tool names.
    pub tools: Vec<String>,
    /// Server type: "local" or "stdio".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<String>,
    /// Timeout in milliseconds.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<i32>,
    /// Command to execute.
    pub command: String,
    /// Command arguments.
    pub args: Vec<String>,
    /// Environment variables.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub env: Option<HashMap<String, String>>,
    /// Working directory.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
}

/// Configuration for a remote MCP server (HTTP or SSE).
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpRemoteServerConfig {
    /// List of tool names.
    pub tools: Vec<String>,
    /// Server type: "http" or "sse".
    pub r#type: String,
    /// Timeout in milliseconds.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<i32>,
    /// Server URL.
    pub url: String,
    /// HTTP headers.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub headers: Option<HashMap<String, String>>,
}

/// MCP server configuration (can be local or remote).
pub type McpServerConfig = serde_json::Value;

/// Configuration for a custom agent.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CustomAgentConfig {
    /// Unique name of the custom agent.
    pub name: String,
    /// Display name for UI purposes.
    #[serde(skip_serializing_if = "Option::is_none")]
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
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_servers: Option<HashMap<String, McpServerConfig>>,
    /// Whether the agent should be available for model inference.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub infer: Option<bool>,
}

/// Configuration for creating a new session.
#[derive(Debug, Default)]
pub struct SessionConfig {
    /// Optional custom session ID.
    pub session_id: Option<String>,
    /// Model to use for this session.
    pub model: Option<String>,
    /// Override the default configuration directory location.
    pub config_dir: Option<String>,
    /// Tools to expose to the CLI.
    pub tools: Vec<Tool>,
    /// System message configuration.
    pub system_message: Option<SystemMessageConfig>,
    /// List of tool names to allow. Takes precedence over excluded_tools.
    pub available_tools: Option<Vec<String>>,
    /// List of tool names to disable.
    pub excluded_tools: Option<Vec<String>>,
    /// Enable streaming of assistant message and reasoning chunks.
    pub streaming: bool,
    /// Custom model provider configuration.
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

impl SessionConfig {
    /// Create a new session config.
    pub fn new() -> Self {
        Self::default()
    }

    /// Set the model.
    pub fn model(mut self, model: impl Into<String>) -> Self {
        self.model = Some(model.into());
        self
    }

    /// Add a tool.
    pub fn tool(mut self, tool: Tool) -> Self {
        self.tools.push(tool);
        self
    }

    /// Set tools.
    pub fn tools(mut self, tools: Vec<Tool>) -> Self {
        self.tools = tools;
        self
    }

    /// Enable streaming.
    pub fn streaming(mut self, streaming: bool) -> Self {
        self.streaming = streaming;
        self
    }

    /// Set system message.
    pub fn system_message(mut self, config: SystemMessageConfig) -> Self {
        self.system_message = Some(config);
        self
    }

    /// Set provider configuration.
    pub fn provider(mut self, provider: ProviderConfig) -> Self {
        self.provider = Some(provider);
        self
    }
}

/// Configuration for resuming a session.
#[derive(Debug, Default)]
pub struct ResumeSessionConfig {
    /// Tools to expose to the CLI.
    pub tools: Vec<Tool>,
    /// Custom model provider configuration.
    pub provider: Option<ProviderConfig>,
    /// Enable streaming.
    pub streaming: bool,
    /// MCP servers for the session.
    pub mcp_servers: Option<HashMap<String, McpServerConfig>>,
    /// Custom agents for the session.
    pub custom_agents: Option<Vec<CustomAgentConfig>>,
    /// Directories to load skills from.
    pub skill_directories: Option<Vec<String>>,
    /// Skill names to disable.
    pub disabled_skills: Option<Vec<String>>,
}

/// Tool definition.
pub struct Tool {
    /// Tool name.
    pub name: String,
    /// Tool description.
    pub description: String,
    /// JSON schema for tool parameters.
    pub parameters: Option<serde_json::Value>,
    /// Tool handler function (set via handler_fn for async support).
    pub(crate) handler: Option<ToolHandlerFn>,
}

impl std::fmt::Debug for Tool {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("Tool")
            .field("name", &self.name)
            .field("description", &self.description)
            .field("parameters", &self.parameters)
            .field("handler", &self.handler.as_ref().map(|_| "<handler>"))
            .finish()
    }
}

impl Tool {
    /// Create a new tool with a name and description.
    pub fn new(name: impl Into<String>, description: impl Into<String>) -> Self {
        Self {
            name: name.into(),
            description: description.into(),
            parameters: None,
            handler: None,
        }
    }

    /// Set the parameters schema.
    pub fn parameters(mut self, params: serde_json::Value) -> Self {
        self.parameters = Some(params);
        self
    }

    /// Set the handler function.
    pub fn handler<F>(mut self, handler: F) -> Self
    where
        F: Fn(ToolInvocation) -> std::pin::Pin<Box<dyn std::future::Future<Output = Result<ToolResult>> + Send>>
            + Send
            + Sync
            + 'static,
    {
        self.handler = Some(Box::new(handler));
        self
    }
}

/// Type alias for tool handler function.
pub(crate) type ToolHandlerFn = Box<
    dyn Fn(ToolInvocation) -> std::pin::Pin<Box<dyn std::future::Future<Output = Result<ToolResult>> + Send>>
        + Send
        + Sync,
>;

/// Tool invocation context.
#[derive(Debug, Clone)]
pub struct ToolInvocation {
    /// Session ID.
    pub session_id: String,
    /// Tool call ID.
    pub tool_call_id: String,
    /// Tool name.
    pub tool_name: String,
    /// Tool arguments.
    pub arguments: serde_json::Value,
}

impl ToolInvocation {
    /// Parse arguments into a typed struct.
    pub fn parse_arguments<T: serde::de::DeserializeOwned>(&self) -> Result<T> {
        Ok(serde_json::from_value(self.arguments.clone())?)
    }
}

/// Result of a tool invocation.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolResult {
    /// Text result for the LLM.
    pub text_result_for_llm: String,
    /// Binary results for the LLM.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub binary_results_for_llm: Option<Vec<ToolBinaryResult>>,
    /// Result type: "success" or "failure".
    pub result_type: String,
    /// Error message if result_type is "failure".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Session log message.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_log: Option<String>,
    /// Tool telemetry data.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_telemetry: Option<HashMap<String, serde_json::Value>>,
}

impl ToolResult {
    /// Create a successful result with text.
    pub fn success(text: impl Into<String>) -> Self {
        Self {
            text_result_for_llm: text.into(),
            binary_results_for_llm: None,
            result_type: "success".to_string(),
            error: None,
            session_log: None,
            tool_telemetry: None,
        }
    }

    /// Create a failure result.
    pub fn failure(error: impl Into<String>) -> Self {
        Self {
            text_result_for_llm: "Invoking this tool produced an error. Detailed information is not available.".to_string(),
            binary_results_for_llm: None,
            result_type: "failure".to_string(),
            error: Some(error.into()),
            session_log: None,
            tool_telemetry: None,
        }
    }

    /// Set session log.
    pub fn session_log(mut self, log: impl Into<String>) -> Self {
        self.session_log = Some(log.into());
        self
    }

    /// Set tool telemetry.
    pub fn telemetry(mut self, telemetry: HashMap<String, serde_json::Value>) -> Self {
        self.tool_telemetry = Some(telemetry);
        self
    }
}

/// Binary result from a tool.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolBinaryResult {
    /// Base64-encoded data.
    pub data: String,
    /// MIME type of the data.
    pub mime_type: String,
    /// Type of binary result.
    pub r#type: String,
    /// Description of the binary result.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
}

/// Custom model provider configuration.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderConfig {
    /// Provider type: "openai", "azure", or "anthropic".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<String>,
    /// API format: "completions" or "responses".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wire_api: Option<String>,
    /// API endpoint URL.
    pub base_url: String,
    /// API key.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api_key: Option<String>,
    /// Bearer token for authentication.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub bearer_token: Option<String>,
    /// Azure-specific options.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub azure: Option<AzureProviderOptions>,
}

/// Azure-specific provider options.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureProviderOptions {
    /// Azure API version.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api_version: Option<String>,
}

/// Message options for sending to a session.
#[derive(Debug, Clone, Default)]
pub struct MessageOptions {
    /// The message prompt.
    pub prompt: String,
    /// File or directory attachments.
    pub attachments: Option<Vec<Attachment>>,
    /// Message delivery mode (default: "enqueue").
    pub mode: Option<String>,
}

impl MessageOptions {
    /// Create new message options with a prompt.
    pub fn new(prompt: impl Into<String>) -> Self {
        Self {
            prompt: prompt.into(),
            attachments: None,
            mode: None,
        }
    }

    /// Add attachments.
    pub fn attachments(mut self, attachments: Vec<Attachment>) -> Self {
        self.attachments = Some(attachments);
        self
    }

    /// Set the delivery mode.
    pub fn mode(mut self, mode: impl Into<String>) -> Self {
        self.mode = Some(mode.into());
        self
    }
}

/// File or directory attachment.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Attachment {
    /// Attachment type: "file" or "directory".
    pub r#type: AttachmentType,
    /// Path to the file or directory.
    pub path: String,
    /// Display name for the attachment.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_name: Option<String>,
}

impl Attachment {
    /// Create a file attachment.
    pub fn file(path: impl Into<String>) -> Self {
        Self {
            r#type: AttachmentType::File,
            path: path.into(),
            display_name: None,
        }
    }

    /// Create a directory attachment.
    pub fn directory(path: impl Into<String>) -> Self {
        Self {
            r#type: AttachmentType::Directory,
            path: path.into(),
            display_name: None,
        }
    }
}

/// Attachment type.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum AttachmentType {
    /// File attachment.
    File,
    /// Directory attachment.
    Directory,
}

/// Session event handler function type.
pub type SessionEventHandler = Box<dyn Fn(SessionEvent) + Send + Sync>;

/// Response from a ping request.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PingResponse {
    /// Echo of the sent message.
    pub message: String,
    /// Server timestamp.
    pub timestamp: i64,
    /// Protocol version.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub protocol_version: Option<i32>,
}

/// Response from session.create.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCreateResponse {
    /// Created session ID.
    pub session_id: String,
}

/// Response from session.send.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSendResponse {
    /// Message ID.
    pub message_id: String,
}

/// Response from status.get.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GetStatusResponse {
    /// CLI version.
    pub version: String,
    /// Protocol version.
    pub protocol_version: i32,
}

/// Response from auth.getStatus.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GetAuthStatusResponse {
    /// Whether the user is authenticated.
    pub is_authenticated: bool,
    /// Authentication type.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_type: Option<String>,
    /// Host.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<String>,
    /// Login username.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub login: Option<String>,
    /// Status message.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status_message: Option<String>,
}

/// Model vision limits.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct ModelVisionLimits {
    /// Supported media types.
    pub supported_media_types: Vec<String>,
    /// Maximum number of images in a prompt.
    pub max_prompt_images: i32,
    /// Maximum image size.
    pub max_prompt_image_size: i32,
}

/// Model limits.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct ModelLimits {
    /// Maximum prompt tokens.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_prompt_tokens: Option<i32>,
    /// Maximum context window tokens.
    pub max_context_window_tokens: i32,
    /// Vision limits.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub vision: Option<ModelVisionLimits>,
}

/// Model support flags.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct ModelSupports {
    /// Whether the model supports vision.
    pub vision: bool,
}

/// Model capabilities.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct ModelCapabilities {
    /// Support flags.
    pub supports: ModelSupports,
    /// Limits.
    pub limits: ModelLimits,
}

/// Model policy.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct ModelPolicy {
    /// Policy state.
    pub state: String,
    /// Policy terms.
    pub terms: String,
}

/// Model billing information.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct ModelBilling {
    /// Billing multiplier.
    pub multiplier: f64,
}

/// Information about an available model.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct ModelInfo {
    /// Model ID.
    pub id: String,
    /// Model name.
    pub name: String,
    /// Model capabilities.
    pub capabilities: ModelCapabilities,
    /// Model policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy: Option<ModelPolicy>,
    /// Billing information.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub billing: Option<ModelBilling>,
}

/// Response from models.list.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GetModelsResponse {
    /// Available models.
    pub models: Vec<ModelInfo>,
}

/// Session list item.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionListItem {
    /// Session ID.
    pub session_id: String,
}

/// Response from session.list.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ListSessionsResponse {
    /// List of sessions.
    pub sessions: Vec<SessionListItem>,
}
