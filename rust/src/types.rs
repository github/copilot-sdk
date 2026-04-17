use std::collections::HashMap;
use std::future::Future;
use std::path::{Path, PathBuf};

use async_trait::async_trait;
use serde::{Deserialize, Serialize};
use serde_json::Value;

// Re-export generated session event types so they're available from `types::*`.
pub use crate::generated::session_events::{SessionEvent, SessionEventData, SessionEventType};

/// Opaque session identifier assigned by the CLI.
///
/// A newtype wrapper around `String` that provides type safety — prevents
/// accidentally passing a workspace ID or request ID where a session ID
/// is expected. Derefs to `str` for zero-friction borrowing.
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[serde(transparent)]
pub struct SessionId(String);

impl SessionId {
    pub fn new(id: impl Into<String>) -> Self {
        Self(id.into())
    }

    pub fn into_inner(self) -> String {
        self.0
    }
}

impl std::ops::Deref for SessionId {
    type Target = str;

    fn deref(&self) -> &str {
        &self.0
    }
}

impl std::fmt::Display for SessionId {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(&self.0)
    }
}

impl From<String> for SessionId {
    fn from(s: String) -> Self {
        Self(s)
    }
}

impl From<&str> for SessionId {
    fn from(s: &str) -> Self {
        Self(s.to_owned())
    }
}

impl AsRef<str> for SessionId {
    fn as_ref(&self) -> &str {
        &self.0
    }
}

impl std::borrow::Borrow<str> for SessionId {
    fn borrow(&self) -> &str {
        &self.0
    }
}

impl From<SessionId> for String {
    fn from(id: SessionId) -> String {
        id.0
    }
}

impl PartialEq<str> for SessionId {
    fn eq(&self, other: &str) -> bool {
        self.0 == other
    }
}

impl PartialEq<String> for SessionId {
    fn eq(&self, other: &String) -> bool {
        &self.0 == other
    }
}

impl PartialEq<SessionId> for String {
    fn eq(&self, other: &SessionId) -> bool {
        self == &other.0
    }
}

impl PartialEq<&str> for SessionId {
    fn eq(&self, other: &&str) -> bool {
        self.0 == *other
    }
}

/// Opaque request identifier for pending CLI requests (permission, user-input, etc.).
///
/// A newtype wrapper around `String` that provides type safety — prevents
/// accidentally passing a session ID or workspace ID where a request ID
/// is expected. Derefs to `str` for zero-friction borrowing.
#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[serde(transparent)]
pub struct RequestId(String);

impl RequestId {
    pub fn new(id: impl Into<String>) -> Self {
        Self(id.into())
    }

    pub fn into_inner(self) -> String {
        self.0
    }
}

impl std::ops::Deref for RequestId {
    type Target = str;

    fn deref(&self) -> &str {
        &self.0
    }
}

impl std::fmt::Display for RequestId {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(&self.0)
    }
}

impl From<String> for RequestId {
    fn from(s: String) -> Self {
        Self(s)
    }
}

impl From<&str> for RequestId {
    fn from(s: &str) -> Self {
        Self(s.to_owned())
    }
}

impl AsRef<str> for RequestId {
    fn as_ref(&self) -> &str {
        &self.0
    }
}

impl std::borrow::Borrow<str> for RequestId {
    fn borrow(&self) -> &str {
        &self.0
    }
}

impl From<RequestId> for String {
    fn from(id: RequestId) -> String {
        id.0
    }
}

impl PartialEq<str> for RequestId {
    fn eq(&self, other: &str) -> bool {
        self.0 == other
    }
}

impl PartialEq<String> for RequestId {
    fn eq(&self, other: &String) -> bool {
        &self.0 == other
    }
}

impl PartialEq<RequestId> for String {
    fn eq(&self, other: &RequestId) -> bool {
        self == &other.0
    }
}

impl PartialEq<&str> for RequestId {
    fn eq(&self, other: &&str) -> bool {
        self.0 == *other
    }
}

/// A tool that the client exposes to the Copilot agent.
///
/// Tools are declared during session creation and invoked by the agent
/// via `tool.call` JSON-RPC requests.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Tool {
    /// Unique tool name (e.g. `"get_weather"`).
    pub name: String,
    /// Human-readable description shown to the agent.
    pub description: String,
    /// JSON Schema describing the tool's parameters. `None` means no parameters.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parameters: Option<Value>,
    /// When `true`, this tool replaces a built-in tool with the same name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub overrides_built_in_tool: Option<bool>,
}

/// Custom API provider configuration for BYOK (Bring Your Own Key).
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderConfig {
    /// Provider type: `"openai"`, `"azure"`, or `"anthropic"`. Defaults to `"openai"`.
    #[serde(rename = "type", skip_serializing_if = "Option::is_none")]
    pub provider_type: Option<String>,
    /// API endpoint URL.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub base_url: Option<String>,
    /// API key for authentication.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api_key: Option<String>,
    /// Bearer token for authentication. Takes precedence over `api_key`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub bearer_token: Option<String>,
    /// API wire format (`"completions"` or `"responses"`). OpenAI/Azure only.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wire_api: Option<String>,
    /// Azure-specific options.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub azure: Option<AzureProviderOptions>,
    /// Custom HTTP headers for outbound requests.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub headers: Option<std::collections::HashMap<String, String>>,
}

/// Azure-specific provider configuration.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureProviderOptions {
    /// Azure API version (e.g. `"2024-02-15-preview"`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api_version: Option<String>,
}

/// Deep-partial model capabilities override used by `session.create`,
/// `session.resume`, and `session.model.switchTo`.
pub type ModelCapabilitiesOverride = crate::generated::rpc::ModelSwitchToRequestModelCapabilities;
/// Feature-flag override section for [`ModelCapabilitiesOverride`].
pub type ModelCapabilitiesOverrideSupports =
    crate::generated::rpc::ModelSwitchToRequestModelCapabilitiesSupports;
/// Limits override section for [`ModelCapabilitiesOverride`].
pub type ModelCapabilitiesOverrideLimits =
    crate::generated::rpc::ModelSwitchToRequestModelCapabilitiesLimits;
/// Vision-specific limits override section for [`ModelCapabilitiesOverride`].
pub type ModelCapabilitiesOverrideLimitsVision =
    crate::generated::rpc::ModelSwitchToRequestModelCapabilitiesLimitsVision;

/// Path conventions used by a custom session filesystem provider.
pub type SessionFsConventions = crate::generated::rpc::SessionFsSetProviderRequestConventions;
/// Request payload for `sessionFs.readFile`.
pub type SessionFsReadFileRequest = crate::generated::rpc::SessionFsReadFileRequest;
/// Result payload for `sessionFs.readFile`.
pub type SessionFsReadFileResult = crate::generated::rpc::SessionFsReadFileResult;
/// Request payload for `sessionFs.writeFile`.
pub type SessionFsWriteFileRequest = crate::generated::rpc::SessionFsWriteFileRequest;
/// Request payload for `sessionFs.appendFile`.
pub type SessionFsAppendFileRequest = crate::generated::rpc::SessionFsAppendFileRequest;
/// Request payload for `sessionFs.exists`.
pub type SessionFsExistsRequest = crate::generated::rpc::SessionFsExistsRequest;
/// Result payload for `sessionFs.exists`.
pub type SessionFsExistsResult = crate::generated::rpc::SessionFsExistsResult;
/// Request payload for `sessionFs.stat`.
pub type SessionFsStatRequest = crate::generated::rpc::SessionFsStatRequest;
/// Result payload for `sessionFs.stat`.
pub type SessionFsStatResult = crate::generated::rpc::SessionFsStatResult;
/// Request payload for `sessionFs.mkdir`.
pub type SessionFsMkdirRequest = crate::generated::rpc::SessionFsMkdirRequest;
/// Request payload for `sessionFs.readdir`.
pub type SessionFsReaddirRequest = crate::generated::rpc::SessionFsReaddirRequest;
/// Result payload for `sessionFs.readdir`.
pub type SessionFsReaddirResult = crate::generated::rpc::SessionFsReaddirResult;
/// Directory entry for `sessionFs.readdirWithTypes`.
pub type SessionFsReaddirEntry = crate::generated::rpc::SessionFsReaddirWithTypesResultEntries;
/// Entry type for `sessionFs.readdirWithTypes`.
pub type SessionFsReaddirEntryType =
    crate::generated::rpc::SessionFsReaddirWithTypesResultEntriesType;
/// Request payload for `sessionFs.readdirWithTypes`.
pub type SessionFsReaddirWithTypesRequest = crate::generated::rpc::SessionFsReaddirWithTypesRequest;
/// Result payload for `sessionFs.readdirWithTypes`.
pub type SessionFsReaddirWithTypesResult = crate::generated::rpc::SessionFsReaddirWithTypesResult;
/// Request payload for `sessionFs.rm`.
pub type SessionFsRmRequest = crate::generated::rpc::SessionFsRmRequest;
/// Request payload for `sessionFs.rename`.
pub type SessionFsRenameRequest = crate::generated::rpc::SessionFsRenameRequest;

/// Connection-level configuration for a custom session filesystem provider.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsConfig {
    /// Initial working directory for sessions.
    pub initial_cwd: PathBuf,
    /// Path inside each session's virtual filesystem where the runtime stores
    /// session-scoped files (events, checkpoints, temp files, etc.).
    pub session_state_path: PathBuf,
    /// Path conventions used by this provider.
    pub conventions: SessionFsConventions,
}

/// Per-session handler for custom session filesystem operations.
#[async_trait]
pub trait SessionFsHandler: Send + Sync + 'static {
    /// Read a UTF-8 text file.
    async fn read_file(
        &self,
        request: &SessionFsReadFileRequest,
    ) -> Result<SessionFsReadFileResult, crate::Error>;
    /// Write a UTF-8 text file, creating it if needed.
    async fn write_file(&self, request: &SessionFsWriteFileRequest) -> Result<(), crate::Error>;
    /// Append UTF-8 text to a file, creating it if needed.
    async fn append_file(&self, request: &SessionFsAppendFileRequest) -> Result<(), crate::Error>;
    /// Check whether a path exists.
    async fn exists(
        &self,
        request: &SessionFsExistsRequest,
    ) -> Result<SessionFsExistsResult, crate::Error>;
    /// Return metadata for a file or directory.
    async fn stat(
        &self,
        request: &SessionFsStatRequest,
    ) -> Result<SessionFsStatResult, crate::Error>;
    /// Create a directory.
    async fn mkdir(&self, request: &SessionFsMkdirRequest) -> Result<(), crate::Error>;
    /// List directory entry names.
    async fn readdir(
        &self,
        request: &SessionFsReaddirRequest,
    ) -> Result<SessionFsReaddirResult, crate::Error>;
    /// List directory entries with type information.
    async fn readdir_with_types(
        &self,
        request: &SessionFsReaddirWithTypesRequest,
    ) -> Result<SessionFsReaddirWithTypesResult, crate::Error>;
    /// Remove a file or directory.
    async fn rm(&self, request: &SessionFsRmRequest) -> Result<(), crate::Error>;
    /// Rename or move a file or directory.
    async fn rename(&self, request: &SessionFsRenameRequest) -> Result<(), crate::Error>;
}

/// Configuration for a custom sub-agent.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CustomAgentConfig {
    /// Unique agent name.
    pub name: String,
    /// Display name for UI purposes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_name: Option<String>,
    /// Description of what the agent does.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Tool names the agent can use. `None` allows all tools.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tools: Option<Vec<String>>,
    /// System prompt for the agent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub prompt: Option<String>,
    /// MCP servers specific to this agent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_servers: Option<Value>,
    /// Whether the agent should be available for model inference.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub infer: Option<bool>,
    /// Skill names to preload into this agent's context at startup.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skills: Option<Vec<String>>,
}

/// Configuration for infinite (long-running) sessions with automatic compaction.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InfiniteSessionConfig {
    /// Whether infinite sessions are enabled (default: true).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enabled: Option<bool>,
    /// Context utilization (0.0–1.0) at which background compaction starts. Default: 0.80.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub background_compaction_threshold: Option<f64>,
    /// Context utilization (0.0–1.0) at which the session blocks until compaction completes. Default: 0.95.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub buffer_exhaustion_threshold: Option<f64>,
}

/// Command definition for slash-commands.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandDefinition {
    /// The slash-command name (without leading `/`).
    pub name: String,
    /// Description shown in command completion.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
}

/// Configuration for creating a new session via the `session.create` RPC.
///
/// All fields are optional — the CLI applies sensible defaults.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionConfig {
    /// Optional custom session ID. If omitted, the CLI generates one.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
    /// Model to use (e.g. `"gpt-4"`, `"claude-sonnet-4"`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Application name sent as `User-Agent` context.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Reasoning effort level (e.g. `"low"`, `"medium"`, `"high"`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    /// Per-property overrides for model capabilities, deep-merged over runtime defaults.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_capabilities: Option<ModelCapabilitiesOverride>,
    /// Override the default configuration directory location.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config_dir: Option<PathBuf>,
    /// Working directory for the session. Tool operations are resolved relative to it.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<PathBuf>,
    /// Enable streaming token deltas via `assistant.message_delta` events.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub streaming: Option<bool>,
    /// Custom system message configuration.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub system_message: Option<SystemMessageConfig>,
    /// Client-defined tools to expose to the agent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tools: Option<Vec<Tool>>,
    /// Allowlist of built-in tool names the agent may use.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub available_tools: Option<Vec<String>>,
    /// Blocklist of built-in tool names the agent must not use.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub excluded_tools: Option<Vec<String>>,
    /// MCP server configurations passed through to the CLI.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_servers: Option<Value>,
    /// How the CLI interprets env values in MCP server configs.
    /// `"direct"` = literal values; `"indirect"` = env var names to look up.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub env_value_mode: Option<String>,
    /// When true, the CLI runs config discovery (MCP config files, skills, plugins).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_config_discovery: Option<bool>,
    /// Enable the `ask_user` tool for interactive user input.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_user_input: Option<bool>,
    /// Enable `permission.request` JSON-RPC calls from the CLI.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_permission: Option<bool>,
    /// Enable `exitPlanMode.request` JSON-RPC calls for plan approval.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_exit_plan_mode: Option<bool>,
    /// Advertise elicitation provider capability. When true, the CLI sends
    /// `elicitation.requested` events that the handler can respond to.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_elicitation: Option<bool>,
    /// Skill directory paths passed through to the Copilot CLI.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skill_directories: Option<Vec<PathBuf>>,
    /// Skill names to disable. Skills in this set will not be available
    /// even if found in skill directories.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disabled_skills: Option<Vec<String>>,
    /// MCP server names to disable. Servers in this set will not be
    /// started or connected.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disabled_mcp_servers: Option<Vec<String>>,
    /// Enable session hooks. When `true`, the CLI sends `hooks.invoke`
    /// RPC requests at key lifecycle points (pre/post tool use, prompt
    /// submission, session start/end, errors).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub hooks: Option<bool>,
    /// Custom API provider configuration (BYOK).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider: Option<ProviderConfig>,
    /// Custom sub-agent configurations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub custom_agents: Option<Vec<CustomAgentConfig>>,
    /// Name of the custom agent to activate when the session starts.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent: Option<String>,
    /// Infinite session (automatic compaction) configuration.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub infinite_sessions: Option<InfiniteSessionConfig>,
    /// Slash-command definitions registered for this session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub commands: Option<Vec<CommandDefinition>>,
}

/// Configuration for resuming an existing session via the `session.resume` RPC.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ResumeSessionConfig {
    /// ID of the session to resume.
    pub session_id: SessionId,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Optional model override for the resumed session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Reasoning effort level for the active model, if supported.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    /// Per-property overrides for model capabilities, deep-merged over runtime defaults.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_capabilities: Option<ModelCapabilitiesOverride>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub streaming: Option<bool>,
    /// Re-supply the system message so the agent retains workspace context
    /// across CLI process restarts.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub system_message: Option<SystemMessageConfig>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tools: Option<Vec<Tool>>,
    /// Allowlist of built-in tool names the agent may use.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub available_tools: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub excluded_tools: Option<Vec<String>>,
    /// Custom API provider configuration (BYOK).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider: Option<ProviderConfig>,
    /// Working directory for the resumed session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<PathBuf>,
    /// Override the default configuration directory location.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config_dir: Option<PathBuf>,
    /// Re-supply MCP servers so they remain available after app restart.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_servers: Option<Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub env_value_mode: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_config_discovery: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_user_input: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_permission: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_exit_plan_mode: Option<bool>,
    /// Advertise elicitation provider capability on resume.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_elicitation: Option<bool>,
    /// Skill directory paths passed through to the Copilot CLI on resume.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skill_directories: Option<Vec<PathBuf>>,
    /// Skill names to disable for the resumed session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disabled_skills: Option<Vec<String>>,
    /// Enable session hooks on resume.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub hooks: Option<bool>,
    /// Custom sub-agent configurations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub custom_agents: Option<Vec<CustomAgentConfig>>,
    /// Name of the custom agent to activate when the resumed session starts.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent: Option<String>,
    /// Infinite session (automatic compaction) configuration.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub infinite_sessions: Option<InfiniteSessionConfig>,
    /// Slash-command definitions registered for this session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub commands: Option<Vec<CommandDefinition>>,
    /// When true, skip emitting the `session.resume` event.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disable_resume: Option<bool>,
}

/// Controls how the system message is constructed.
///
/// Use `mode: "append"` (default) to add content after the built-in system
/// message, `"replace"` to substitute it entirely, or `"customize"` for
/// section-level overrides.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SystemMessageConfig {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub content: Option<String>,
    /// Section-level overrides (used with `mode: "customize"`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sections: Option<HashMap<String, SectionOverride>>,
}

/// An override operation for a single system prompt section.
///
/// Used within [`SystemMessageConfig::sections`] when `mode` is `"customize"`.
/// The `action` field determines the operation: `"replace"`, `"remove"`,
/// `"append"`, `"prepend"`, or `"transform"`.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SectionOverride {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub action: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub content: Option<String>,
}

/// Response from `session.create`.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CreateSessionResult {
    pub session_id: SessionId,
    /// Workspace directory for the session (infinite sessions).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub workspace_path: Option<PathBuf>,
    /// Remote session URL, if the session is running remotely.
    #[serde(default, alias = "remote_url")]
    pub remote_url: Option<String>,
    /// Capabilities negotiated with the CLI for this session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub capabilities: Option<SessionCapabilities>,
}

/// Options for sending a user message to the agent via [`Session::send_message`]
/// or [`Session::send_and_wait`].
///
/// Use [`MessageOptions::new`] for the common case of a plain text message.
///
/// # Example
///
/// ```no_run
/// # use copilot::types::MessageOptions;
/// let opts = MessageOptions::new("What is the capital of France?");
/// ```
#[derive(Debug, Clone, Default)]
pub struct MessageOptions {
    /// User message text.
    pub prompt: String,
    /// Delivery mode (e.g. `"enqueue"`, `"immediate"`).
    pub mode: Option<String>,
    /// File attachments to include with the message.
    pub attachments: Option<Vec<Attachment>>,
    /// Custom HTTP headers forwarded with the request.
    pub request_headers: Option<HashMap<String, String>>,
}

impl MessageOptions {
    /// Create message options with just a prompt.
    pub fn new(prompt: impl Into<String>) -> Self {
        Self {
            prompt: prompt.into(),
            ..Default::default()
        }
    }

    /// Set the delivery mode.
    pub fn with_mode(mut self, mode: impl Into<String>) -> Self {
        self.mode = Some(mode.into());
        self
    }

    /// Set attachments.
    pub fn with_attachments(mut self, attachments: Vec<Attachment>) -> Self {
        self.attachments = Some(attachments);
        self
    }

    /// Set custom request headers.
    pub fn with_request_headers(mut self, headers: HashMap<String, String>) -> Self {
        self.request_headers = Some(headers);
        self
    }
}

/// Result of [`Session::send_and_wait`] — the message ID assigned by the CLI
/// and the last `assistant.message` event (if any) captured during the turn.
#[derive(Debug)]
pub struct SendAndWaitResult {
    /// Message ID assigned by the CLI when the message was accepted.
    pub message_id: String,
    /// The last `assistant.message` session event, if one was emitted.
    pub event: Option<SessionEvent>,
}

/// Options for [`Session::log`].
#[derive(Debug, Clone, Default)]
pub struct LogOptions {
    /// Log level: `"info"`, `"warning"`, or `"error"`. Default: `"info"`.
    pub level: Option<LogLevel>,
    /// When `true`, the log entry is ephemeral (not persisted to history).
    pub ephemeral: Option<bool>,
}

/// Log severity level for [`Session::log`].
#[non_exhaustive]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum LogLevel {
    Info,
    Warning,
    Error,
}

impl LogLevel {
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::Info => "info",
            Self::Warning => "warning",
            Self::Error => "error",
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentLineRange {
    pub start: u32,
    pub end: u32,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentSelectionPosition {
    pub line: u32,
    pub character: u32,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentSelectionRange {
    pub start: AttachmentSelectionPosition,
    pub end: AttachmentSelectionPosition,
}

#[non_exhaustive]
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum GitHubReferenceType {
    Issue,
    Pr,
    Discussion,
}

/// An attachment included with a user message.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(
    tag = "type",
    rename_all = "camelCase",
    rename_all_fields = "camelCase"
)]
pub enum Attachment {
    File {
        path: PathBuf,
        #[serde(skip_serializing_if = "Option::is_none")]
        display_name: Option<String>,
        #[serde(skip_serializing_if = "Option::is_none")]
        line_range: Option<AttachmentLineRange>,
    },
    Directory {
        path: PathBuf,
        #[serde(skip_serializing_if = "Option::is_none")]
        display_name: Option<String>,
    },
    Selection {
        file_path: PathBuf,
        text: String,
        #[serde(skip_serializing_if = "Option::is_none")]
        display_name: Option<String>,
        selection: AttachmentSelectionRange,
    },
    Blob {
        data: String,
        mime_type: String,
        #[serde(skip_serializing_if = "Option::is_none")]
        display_name: Option<String>,
    },
    #[serde(rename = "github_reference")]
    GitHubReference {
        number: u64,
        title: String,
        reference_type: GitHubReferenceType,
        state: String,
        url: String,
    },
}

impl Attachment {
    pub fn display_name(&self) -> Option<&str> {
        match self {
            Self::File { display_name, .. }
            | Self::Directory { display_name, .. }
            | Self::Selection { display_name, .. }
            | Self::Blob { display_name, .. } => display_name.as_deref(),
            Self::GitHubReference { .. } => None,
        }
    }

    pub fn label(&self) -> Option<String> {
        if let Some(display_name) = self
            .display_name()
            .map(str::trim)
            .filter(|name| !name.is_empty())
        {
            return Some(display_name.to_string());
        }

        match self {
            Self::GitHubReference { number, title, .. } => Some(if title.trim().is_empty() {
                format!("#{}", number)
            } else {
                title.trim().to_string()
            }),
            _ => self.derived_display_name(),
        }
    }

    /// Ensure `display_name` is populated when the variant supports one.
    pub fn ensure_display_name(&mut self) {
        if self
            .display_name()
            .map(str::trim)
            .is_some_and(|name| !name.is_empty())
        {
            return;
        }

        let Some(derived_display_name) = self.derived_display_name() else {
            return;
        };

        match self {
            Self::File { display_name, .. }
            | Self::Directory { display_name, .. }
            | Self::Selection { display_name, .. }
            | Self::Blob { display_name, .. } => *display_name = Some(derived_display_name),
            Self::GitHubReference { .. } => {}
        }
    }

    fn derived_display_name(&self) -> Option<String> {
        match self {
            Self::File { path, .. } | Self::Directory { path, .. } => {
                Some(attachment_name_from_path(path))
            }
            Self::Selection { file_path, .. } => Some(attachment_name_from_path(file_path)),
            Self::Blob { .. } => Some("attachment".to_string()),
            Self::GitHubReference { .. } => None,
        }
    }
}

fn attachment_name_from_path(path: &Path) -> String {
    path.file_name()
        .map(|name| name.to_string_lossy().into_owned())
        .filter(|name| !name.is_empty())
        .unwrap_or_else(|| {
            let full = path.to_string_lossy();
            if full.is_empty() {
                "attachment".to_string()
            } else {
                full.into_owned()
            }
        })
}

/// Normalize a list of attachments so every entry has a `display_name`.
pub fn ensure_attachment_display_names(attachments: &mut [Attachment]) {
    for attachment in attachments {
        attachment.ensure_display_name();
    }
}

/// Wrapper for session event notifications received from the CLI.
///
/// The CLI sends these as JSON-RPC notifications on the `session.event` method.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionEventNotification {
    pub session_id: SessionId,
    pub event: SessionEvent,
}

impl SessionEvent {
    /// `model_call` errors are transient — the CLI agent loop continues
    /// after them and may succeed on the next turn. These should not be
    /// treated as session-ending errors.
    pub fn is_transient_error(&self) -> bool {
        matches!(&self.event_type, SessionEventType::SessionError)
            && matches!(
                &self.data,
                SessionEventData::SessionError(d) if d.error_type == "model_call"
            )
    }
}

/// A request from the CLI to invoke a client-defined tool.
///
/// Received as a JSON-RPC request on the `tool.call` method. The client
/// must respond with a [`ToolResultResponse`].
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolInvocation {
    pub session_id: SessionId,
    /// Unique ID for this tool call, used to correlate the response.
    pub tool_call_id: String,
    /// Name of the tool being invoked.
    pub tool_name: String,
    /// Tool arguments as JSON.
    pub arguments: Value,
}

/// Expanded tool result with metadata for the LLM and session log.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolResultExpanded {
    /// Result text sent back to the LLM.
    pub text_result_for_llm: String,
    /// `"success"` or `"failure"`.
    pub result_type: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_log: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
}

/// Result of a tool invocation — either a plain text string or an expanded result.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum ToolResult {
    /// Simple text result passed directly to the LLM.
    Text(String),
    /// Structured result with metadata.
    Expanded(ToolResultExpanded),
}

/// JSON-RPC response wrapper for a tool result, sent back to the CLI.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolResultResponse {
    pub result: ToolResult,
}

/// Metadata for a persisted session, returned by `session.list`.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadata {
    pub session_id: SessionId,
    pub start_time: String,
    pub modified_time: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
    pub is_remote: bool,
}

/// Response from `session.list`.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ListSessionsResponse {
    pub sessions: Vec<SessionMetadata>,
}

/// Response from `session.getMessages`.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GetMessagesResponse {
    pub events: Vec<SessionEvent>,
}

/// Result of an elicitation (interactive UI form) request.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ElicitationResult {
    /// User's action: `"accept"`, `"decline"`, or `"cancel"`.
    pub action: String,
    /// Form data submitted by the user (present when action is `"accept"`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub content: Option<Value>,
}

/// Elicitation display mode.
///
/// New modes may be added by the CLI in future protocol versions; the
/// `Unknown` variant keeps deserialization from failing on unrecognised
/// values so the SDK can still surface the request to callers.
#[non_exhaustive]
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub enum ElicitationMode {
    /// Structured form input rendered by the host.
    Form,
    /// Browser redirect to a URL.
    Url,
    /// A mode not yet known to this SDK version.
    #[serde(other)]
    Unknown,
}

/// An incoming elicitation request from the CLI (provider side).
///
/// Received via `elicitation.requested` session event when the session was
/// created with `request_elicitation: true`. The provider should render a
/// form or dialog and return an [`ElicitationResult`].
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ElicitationRequest {
    /// Message describing what information is needed from the user.
    pub message: String,
    /// JSON Schema describing the form fields to present.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub requested_schema: Option<Value>,
    /// Elicitation display mode.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<ElicitationMode>,
    /// The source that initiated the request (e.g. MCP server name).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub elicitation_source: Option<String>,
    /// URL to open in the user's browser (url mode only).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Session-level capabilities reported by the CLI after session creation.
///
/// Capabilities indicate which features the CLI host supports for this session.
/// Updated at runtime via `capabilities.changed` events.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCapabilities {
    /// UI capabilities (elicitation support, etc.).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ui: Option<UiCapabilities>,
}

/// UI-specific capabilities for a session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UiCapabilities {
    /// Whether the host supports interactive elicitation dialogs.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub elicitation: Option<bool>,
}

/// Options for the [`Session::input`](crate::session::Session::input) convenience method.
#[derive(Debug, Clone, Default)]
pub struct InputOptions<'a> {
    /// Title label for the input field.
    pub title: Option<&'a str>,
    /// Descriptive text shown below the field.
    pub description: Option<&'a str>,
    /// Minimum character length.
    pub min_length: Option<u64>,
    /// Maximum character length.
    pub max_length: Option<u64>,
    /// Semantic format hint.
    pub format: Option<InputFormat>,
    /// Default value pre-populated in the field.
    pub default: Option<&'a str>,
}

/// Semantic format hints for text input fields.
#[non_exhaustive]
#[derive(Debug, Clone, Copy)]
pub enum InputFormat {
    Email,
    Uri,
    Date,
    DateTime,
}

impl InputFormat {
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::Email => "email",
            Self::Uri => "uri",
            Self::Date => "date",
            Self::DateTime => "date-time",
        }
    }
}

/// Model capabilities (vision support, context limits, etc.).
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilities {
    #[serde(default)]
    pub supports: ModelSupports,
    #[serde(default)]
    pub limits: ModelLimits,
}

/// Feature flags for a model.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSupports {
    #[serde(default)]
    pub vision: bool,
    #[serde(default)]
    pub reasoning_effort: bool,
}

/// Vision-specific limits for a model.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelVisionLimits {
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub supported_media_types: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_prompt_images: Option<u64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_prompt_image_size: Option<u64>,
}

/// Token limits for a model.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelLimits {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_prompt_tokens: Option<u64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_context_window_tokens: Option<u64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub vision: Option<ModelVisionLimits>,
}

/// Organization policy for a model.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelPolicy {
    #[serde(default)]
    pub state: String,
    #[serde(default)]
    pub terms: String,
}

/// Billing multiplier for a model.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelBilling {
    #[serde(default)]
    pub multiplier: f64,
}

/// A model available via the CLI, returned by `models.list`.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelInfo {
    pub id: String,
    pub name: String,
    #[serde(default)]
    pub capabilities: ModelCapabilities,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy: Option<ModelPolicy>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub billing: Option<ModelBilling>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub supported_reasoning_efforts: Option<Vec<String>>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub default_reasoning_effort: Option<String>,
}

/// Response from `models.list`.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelsListResponse {
    pub models: Vec<ModelInfo>,
}

/// Optional parameters for [`Session::set_model`](crate::session::Session::set_model_with_options).
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SetModelOptions {
    /// Reasoning effort level (e.g. `"low"`, `"medium"`, `"high"`, `"xhigh"`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    /// Per-property overrides for model capabilities, deep-merged over runtime defaults.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_capabilities: Option<ModelCapabilitiesOverride>,
}

impl SetModelOptions {
    /// Create an empty options bag.
    pub fn new() -> Self {
        Self::default()
    }

    /// Set the reasoning effort for the target model.
    pub fn with_reasoning_effort(mut self, reasoning_effort: impl Into<String>) -> Self {
        self.reasoning_effort = Some(reasoning_effort.into());
        self
    }

    /// Override specific model capability properties.
    pub fn with_model_capabilities(
        mut self,
        model_capabilities: ModelCapabilitiesOverride,
    ) -> Self {
        self.model_capabilities = Some(model_capabilities);
        self
    }
}

/// Optional client-wide handler used by [`Client::list_models`](crate::Client::list_models).
#[async_trait]
pub trait ListModelsHandler: Send + Sync + 'static {
    /// Return the list of available models. When configured on the client, this
    /// replaces the `models.list` RPC entirely and uses the same client-side cache.
    async fn list_models(&self) -> Result<Vec<ModelInfo>, crate::Error>;
}

#[async_trait]
impl<F, Fut> ListModelsHandler for F
where
    F: Fn() -> Fut + Send + Sync + 'static,
    Fut: Future<Output = Result<Vec<ModelInfo>, crate::Error>> + Send + 'static,
{
    async fn list_models(&self) -> Result<Vec<ModelInfo>, crate::Error> {
        (self)().await
    }
}

#[cfg(test)]
mod tests {
    use std::path::PathBuf;

    use serde_json::json;

    use super::{
        ensure_attachment_display_names, Attachment, AttachmentLineRange,
        AttachmentSelectionPosition, AttachmentSelectionRange, GitHubReferenceType,
    };

    #[test]
    fn deserializes_runtime_attachment_variants() {
        let attachments: Vec<Attachment> = serde_json::from_value(json!([
            {
                "type": "file",
                "path": "/tmp/file.rs",
                "displayName": "file.rs",
                "lineRange": { "start": 7, "end": 12 }
            },
            {
                "type": "directory",
                "path": "/tmp/project",
                "displayName": "project"
            },
            {
                "type": "selection",
                "filePath": "/tmp/lib.rs",
                "displayName": "lib.rs",
                "text": "fn main() {}",
                "selection": {
                    "start": { "line": 1, "character": 2 },
                    "end": { "line": 3, "character": 4 }
                }
            },
            {
                "type": "blob",
                "data": "Zm9v",
                "mimeType": "image/png",
                "displayName": "image.png"
            },
            {
                "type": "github_reference",
                "number": 42,
                "title": "Fix rendering",
                "referenceType": "issue",
                "state": "open",
                "url": "https://github.com/octocat/hello-world/issues/42"
            }
        ]))
        .expect("attachments should deserialize");

        assert_eq!(attachments.len(), 5);
        assert!(matches!(
            &attachments[0],
            Attachment::File {
                path,
                display_name,
                line_range: Some(AttachmentLineRange { start: 7, end: 12 }),
            } if path == &PathBuf::from("/tmp/file.rs") && display_name.as_deref() == Some("file.rs")
        ));
        assert!(matches!(
            &attachments[1],
            Attachment::Directory { path, display_name }
                if path == &PathBuf::from("/tmp/project") && display_name.as_deref() == Some("project")
        ));
        assert!(matches!(
            &attachments[2],
            Attachment::Selection {
                file_path,
                display_name,
                selection:
                    AttachmentSelectionRange {
                        start: AttachmentSelectionPosition { line: 1, character: 2 },
                        end: AttachmentSelectionPosition { line: 3, character: 4 },
                    },
                ..
            } if file_path == &PathBuf::from("/tmp/lib.rs") && display_name.as_deref() == Some("lib.rs")
        ));
        assert!(matches!(
            &attachments[3],
            Attachment::Blob {
                data,
                mime_type,
                display_name,
            } if data == "Zm9v" && mime_type == "image/png" && display_name.as_deref() == Some("image.png")
        ));
        assert!(matches!(
            &attachments[4],
            Attachment::GitHubReference {
                number: 42,
                title,
                reference_type: GitHubReferenceType::Issue,
                state,
                url,
            } if title == "Fix rendering"
                && state == "open"
                && url == "https://github.com/octocat/hello-world/issues/42"
        ));
    }

    #[test]
    fn ensures_display_names_for_variants_that_support_them() {
        let mut attachments = vec![
            Attachment::File {
                path: PathBuf::from("/tmp/file.rs"),
                display_name: None,
                line_range: None,
            },
            Attachment::Selection {
                file_path: PathBuf::from("/tmp/src/lib.rs"),
                display_name: None,
                text: "fn main() {}".to_string(),
                selection: AttachmentSelectionRange {
                    start: AttachmentSelectionPosition {
                        line: 0,
                        character: 0,
                    },
                    end: AttachmentSelectionPosition {
                        line: 0,
                        character: 10,
                    },
                },
            },
            Attachment::Blob {
                data: "Zm9v".to_string(),
                mime_type: "image/png".to_string(),
                display_name: None,
            },
            Attachment::GitHubReference {
                number: 7,
                title: "Track regressions".to_string(),
                reference_type: GitHubReferenceType::Issue,
                state: "open".to_string(),
                url: "https://example.com/issues/7".to_string(),
            },
        ];

        ensure_attachment_display_names(&mut attachments);

        assert_eq!(attachments[0].display_name(), Some("file.rs"));
        assert_eq!(attachments[1].display_name(), Some("lib.rs"));
        assert_eq!(attachments[2].display_name(), Some("attachment"));
        assert_eq!(attachments[3].display_name(), None);
        assert_eq!(
            attachments[3].label(),
            Some("Track regressions".to_string())
        );
    }
}
