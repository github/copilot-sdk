use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::collections::HashMap;

// ============================================================================
// Connection State
// ============================================================================

#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "lowercase")]
pub enum ConnectionState {
    Disconnected,
    Connecting,
    Connected,
    Error,
}

// ============================================================================
// Client Configuration
// ============================================================================

/// ClientOptions configures the Copilot client
#[derive(Debug, Clone, Default)]
pub struct ClientOptions {
    /// Path to the Copilot CLI executable (default: "copilot")
    pub cli_path: Option<String>,
    /// Working directory for the CLI process (default: inherit from current process)
    pub cwd: Option<String>,
    /// Port for TCP transport (default: 0 = random port)
    pub port: Option<u16>,
    /// Enable stdio transport instead of TCP (default: true)
    pub use_stdio: bool,
    /// URL of an existing Copilot CLI server to connect to over TCP
    /// Format: "host:port", "http://host:port", or just "port" (defaults to localhost)
    /// Examples: "localhost:8080", "http://127.0.0.1:9000", "8080"
    /// Mutually exclusive with cli_path, use_stdio
    pub cli_url: Option<String>,
    /// Log level for the CLI server
    pub log_level: Option<String>,
    /// Automatically starts the CLI server on first use (default: true)
    pub auto_start: Option<bool>,
    /// Automatically restarts the CLI server if it crashes (default: true)
    pub auto_restart: Option<bool>,
    /// Environment variables for the CLI process (default: inherits from current process)
    pub env: Option<Vec<(String, String)>>,
}

impl ClientOptions {
    pub fn new() -> Self {
        Self {
            cli_path: Some("copilot".to_string()),
            use_stdio: true,
            log_level: Some("info".to_string()),
            auto_start: Some(true),
            auto_restart: Some(true),
            ..Default::default()
        }
    }
}

// ============================================================================
// System Message Configuration
// ============================================================================

/// System message configuration for session creation
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SystemMessageConfig {
    /// Mode: "append" or "replace"
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<String>,
    /// Content: additional instructions (append) or complete system message (replace)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub content: Option<String>,
}

// ============================================================================
// Permission Types
// ============================================================================

/// Permission request from the server
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequest {
    pub kind: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
    #[serde(flatten)]
    pub extra: HashMap<String, Value>,
}

/// Result of a permission request
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PermissionRequestResult {
    pub kind: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub rules: Option<Vec<Value>>,
}

/// Context for permission invocation
#[derive(Debug, Clone)]
pub struct PermissionInvocation {
    pub session_id: String,
}

/// Handler for permission requests
pub type PermissionHandler = Box<
    dyn Fn(PermissionRequest, PermissionInvocation) -> Result<PermissionRequestResult, String>
        + Send
        + Sync,
>;

// ============================================================================
// MCP Server Configuration
// ============================================================================

/// Configuration for a local/stdio MCP server
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MCPLocalServerConfig {
    pub tools: Vec<String>,
    #[serde(rename = "type", skip_serializing_if = "Option::is_none")]
    pub server_type: Option<String>, // "local" or "stdio"
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<u32>,
    pub command: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub args: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub env: Option<HashMap<String, String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
}

/// Configuration for a remote MCP server (HTTP or SSE)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MCPRemoteServerConfig {
    pub tools: Vec<String>,
    #[serde(rename = "type")]
    pub server_type: String, // "http" or "sse"
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<u32>,
    pub url: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub headers: Option<HashMap<String, String>>,
}

/// MCP server configuration (can be local or remote)
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum MCPServerConfig {
    Local(MCPLocalServerConfig),
    Remote(MCPRemoteServerConfig),
    Raw(HashMap<String, Value>),
}

// ============================================================================
// Custom Agent Configuration
// ============================================================================

/// Configuration for a custom agent
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CustomAgentConfig {
    /// Unique name of the custom agent
    pub name: String,
    /// Display name for UI purposes
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_name: Option<String>,
    /// Description of what the agent does
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// List of tool names the agent can use (None for all tools)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tools: Option<Vec<String>>,
    /// Prompt content for the agent
    pub prompt: String,
    /// MCP servers specific to this agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_servers: Option<HashMap<String, MCPServerConfig>>,
    /// Whether the agent should be available for model inference
    #[serde(skip_serializing_if = "Option::is_none")]
    pub infer: Option<bool>,
}

// ============================================================================
// Infinite Session Configuration
// ============================================================================

/// Configuration for infinite sessions with automatic context compaction
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InfiniteSessionConfig {
    /// Controls whether infinite sessions are enabled (default: true)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enabled: Option<bool>,
    /// Context utilization (0.0-1.0) at which background compaction starts (default: 0.80)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub background_compaction_threshold: Option<f64>,
    /// Context utilization (0.0-1.0) at which the session blocks until compaction completes (default: 0.95)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub buffer_exhaustion_threshold: Option<f64>,
}

// ============================================================================
// Provider Configuration
// ============================================================================

/// Azure-specific provider options
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureProviderOptions {
    /// Azure API version (default: "2024-10-21")
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api_version: Option<String>,
}

/// Configuration for a custom model provider
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderConfig {
    /// Provider type: "openai", "azure", or "anthropic" (default: "openai")
    #[serde(rename = "type", skip_serializing_if = "Option::is_none")]
    pub provider_type: Option<String>,
    /// API format (openai/azure only): "completions" or "responses" (default: "completions")
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wire_api: Option<String>,
    /// API endpoint URL
    pub base_url: String,
    /// API key (optional for local providers like Ollama)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api_key: Option<String>,
    /// Bearer token for authentication (takes precedence over api_key)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub bearer_token: Option<String>,
    /// Azure-specific options
    #[serde(skip_serializing_if = "Option::is_none")]
    pub azure: Option<AzureProviderOptions>,
}

// ============================================================================
// Tool Types
// ============================================================================

/// A tool that can be invoked by Copilot
#[derive(Clone)]
pub struct Tool {
    pub name: String,
    pub description: Option<String>,
    pub parameters: HashMap<String, Value>,
    pub handler: ToolHandler,
}

impl std::fmt::Debug for Tool {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("Tool")
            .field("name", &self.name)
            .field("description", &self.description)
            .field("parameters", &self.parameters)
            .field("handler", &"<function>")
            .finish()
    }
}

/// Context for a tool invocation
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolInvocation {
    pub session_id: String,
    pub tool_call_id: String,
    pub tool_name: String,
    pub arguments: Value,
}

/// Handler for tool invocations
pub type ToolHandler =
    std::sync::Arc<dyn Fn(ToolInvocation) -> Result<ToolResult, String> + Send + Sync>;

/// Binary result for tools
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolBinaryResult {
    pub data: String,
    pub mime_type: String,
    #[serde(rename = "type")]
    pub result_type: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
}

/// Result of a tool invocation
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolResult {
    pub text_result_for_llm: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub binary_results_for_llm: Option<Vec<ToolBinaryResult>>,
    pub result_type: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_log: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_telemetry: Option<HashMap<String, Value>>,
}

// ============================================================================
// Session Configuration
// ============================================================================

/// Configuration for creating a new session
#[derive(Default)]
pub struct SessionConfig {
    /// Optional custom session ID
    pub session_id: Option<String>,
    /// Model to use for this session
    pub model: Option<String>,
    /// Override the default configuration directory location
    pub config_dir: Option<String>,
    /// Caller-implemented tools to expose to the CLI
    pub tools: Vec<Tool>,
    /// System message customization
    pub system_message: Option<SystemMessageConfig>,
    /// List of tool names to allow (takes precedence over excluded_tools)
    pub available_tools: Option<Vec<String>>,
    /// List of tool names to disable
    pub excluded_tools: Option<Vec<String>>,
    /// Handler for permission requests
    pub on_permission_request: Option<PermissionHandler>,
    /// Enable streaming of assistant message and reasoning chunks
    pub streaming: bool,
    /// Custom model provider configuration (BYOK)
    pub provider: Option<ProviderConfig>,
    /// MCP servers for the session
    pub mcp_servers: Option<HashMap<String, MCPServerConfig>>,
    /// Custom agents for the session
    pub custom_agents: Option<Vec<CustomAgentConfig>>,
    /// Directories to load skills from
    pub skill_directories: Option<Vec<String>>,
    /// Skill names to disable
    pub disabled_skills: Option<Vec<String>>,
    /// Infinite sessions configuration
    pub infinite_sessions: Option<InfiniteSessionConfig>,
}

/// Configuration for resuming a session
#[derive(Default)]
pub struct ResumeSessionConfig {
    /// Caller-implemented tools to expose to the CLI
    pub tools: Vec<Tool>,
    /// Custom model provider configuration
    pub provider: Option<ProviderConfig>,
    /// Handler for permission requests
    pub on_permission_request: Option<PermissionHandler>,
    /// Enable streaming
    pub streaming: bool,
    /// MCP servers for the session
    pub mcp_servers: Option<HashMap<String, MCPServerConfig>>,
    /// Custom agents for the session
    pub custom_agents: Option<Vec<CustomAgentConfig>>,
    /// Directories to load skills from
    pub skill_directories: Option<Vec<String>>,
    /// Skill names to disable
    pub disabled_skills: Option<Vec<String>>,
}

// ============================================================================
// Message Types
// ============================================================================

/// Attachment type
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum AttachmentType {
    File,
    Directory,
}

/// Attachment for a message
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Attachment {
    pub display_name: String,
    pub path: String,
    #[serde(rename = "type")]
    pub attachment_type: AttachmentType,
}

/// Options for sending a message
#[derive(Debug, Clone)]
pub struct MessageOptions {
    /// The message prompt
    pub prompt: String,
    /// File or directory attachments
    pub attachments: Vec<Attachment>,
    /// Message delivery mode (default: "enqueue")
    pub mode: Option<String>,
}

// ============================================================================
// Response Types
// ============================================================================

/// Response from a ping request
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PingResponse {
    pub message: String,
    pub timestamp: i64,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub protocol_version: Option<i32>,
}

/// Response from session.create
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCreateResponse {
    pub session_id: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub workspace_path: Option<String>,
}

/// Response from session.send
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSendResponse {
    pub message_id: String,
}

/// Response from status.get
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GetStatusResponse {
    pub version: String,
    pub protocol_version: i32,
}

/// Response from auth.getStatus
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GetAuthStatusResponse {
    pub is_authenticated: bool,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_type: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub login: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status_message: Option<String>,
}

/// Vision-specific limits
#[derive(Debug, Clone, Deserialize)]
pub struct ModelVisionLimits {
    pub supported_media_types: Vec<String>,
    pub max_prompt_images: i32,
    pub max_prompt_image_size: i32,
}

/// Model limits
#[derive(Debug, Clone, Deserialize)]
pub struct ModelLimits {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_prompt_tokens: Option<i32>,
    pub max_context_window_tokens: i32,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub vision: Option<ModelVisionLimits>,
}

/// Model support flags
#[derive(Debug, Clone, Deserialize)]
pub struct ModelSupports {
    pub vision: bool,
}

/// Model capabilities
#[derive(Debug, Clone, Deserialize)]
pub struct ModelCapabilities {
    pub supports: ModelSupports,
    pub limits: ModelLimits,
}

/// Model policy
#[derive(Debug, Clone, Deserialize)]
pub struct ModelPolicy {
    pub state: String,
    pub terms: String,
}

/// Model billing information
#[derive(Debug, Clone, Deserialize)]
pub struct ModelBilling {
    pub multiplier: f64,
}

/// Information about a model
#[derive(Debug, Clone, Deserialize)]
pub struct ModelInfo {
    pub id: String,
    pub name: String,
    pub capabilities: ModelCapabilities,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy: Option<ModelPolicy>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub billing: Option<ModelBilling>,
}

/// Response from models.list
#[derive(Debug, Clone, Deserialize)]
pub struct GetModelsResponse {
    pub models: Vec<ModelInfo>,
}

/// Response from session.getMessages
#[derive(Debug, Clone, Deserialize)]
pub struct SessionGetMessagesResponse {
    pub events: Vec<SessionEvent>,
}

// ============================================================================
// Session Events (simplified for now)
// ============================================================================

/// Session event (simplified - will be expanded with generated types later)
#[derive(Debug, Clone, Deserialize)]
pub struct SessionEvent {
    #[serde(rename = "type")]
    pub event_type: String,
    pub id: String,
    #[serde(flatten)]
    pub data: Value,
}

/// Handler for session events
pub type SessionEventHandler = Box<dyn Fn(SessionEvent) + Send + Sync>;

// ============================================================================
// Logging and Events
// ============================================================================

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LogMessage {
    pub level: LogLevel,
    pub message: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub metadata: Option<HashMap<String, String>>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum LogLevel {
    Debug,
    Info,
    Warn,
    Error,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentConfig {
    pub agent_id: String,
    pub agent_name: String,
    pub version: String,
    #[serde(default)]
    pub capabilities: Value,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ProtocolEvent {
    pub event: String,
    #[serde(flatten)]
    pub payload: Value,
}

// ============================================================================
// Manual Debug Implementations for types with function pointers
// ============================================================================

impl std::fmt::Debug for SessionConfig {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("SessionConfig")
            .field("session_id", &self.session_id)
            .field("model", &self.model)
            .field("config_dir", &self.config_dir)
            .field("tools", &format!("[{} tools]", self.tools.len()))
            .field("system_message", &self.system_message)
            .field("available_tools", &self.available_tools)
            .field("excluded_tools", &self.excluded_tools)
            .field(
                "on_permission_request",
                &self.on_permission_request.as_ref().map(|_| "<handler>"),
            )
            .field("streaming", &self.streaming)
            .field("provider", &self.provider)
            .field("mcp_servers", &self.mcp_servers)
            .field("custom_agents", &self.custom_agents)
            .field("skill_directories", &self.skill_directories)
            .field("disabled_skills", &self.disabled_skills)
            .field("infinite_sessions", &self.infinite_sessions)
            .finish()
    }
}

impl std::fmt::Debug for ResumeSessionConfig {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("ResumeSessionConfig")
            .field("tools", &format!("[{} tools]", self.tools.len()))
            .field("provider", &self.provider)
            .field(
                "on_permission_request",
                &self.on_permission_request.as_ref().map(|_| "<handler>"),
            )
            .field("streaming", &self.streaming)
            .field("mcp_servers", &self.mcp_servers)
            .field("custom_agents", &self.custom_agents)
            .field("skill_directories", &self.skill_directories)
            .field("disabled_skills", &self.disabled_skills)
            .finish()
    }
}
