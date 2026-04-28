//! Protocol types shared between the SDK and the Copilot CLI.
//!
//! These types map directly to the JSON-RPC request/response payloads
//! defined by the Copilot CLI protocol. They are used for session
//! configuration, event handling, tool invocations, and model queries.

use std::collections::HashMap;
use std::path::{Path, PathBuf};
use std::sync::Arc;
use std::time::Duration;

use serde::{Deserialize, Serialize};
use serde_json::Value;

use crate::handler::SessionHandler;
use crate::hooks::SessionHooks;
use crate::transforms::SystemMessageTransform;

/// Lifecycle state of a [`Client`](crate::Client) connection to the CLI.
///
/// Mirrors Go's `ConnectionState` (`go/types.go:14`). The state advances
/// from `Connecting` → `Connected` during construction, transitions to
/// `Disconnected` after [`Client::stop`](crate::Client::stop) or
/// [`Client::force_stop`](crate::Client::force_stop), and lands in
/// `Errored` if startup fails or the underlying transport tears down
/// unexpectedly.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum ConnectionState {
    /// No CLI process is attached or the process has exited cleanly.
    Disconnected,
    /// The client is starting up (spawning the CLI, negotiating protocol).
    Connecting,
    /// The client is connected and ready to handle RPC traffic.
    Connected,
    /// Startup failed or the connection encountered an unrecoverable error.
    Errored,
}

/// Type of [`SessionLifecycleEvent`] received via [`Client::on`](crate::Client::on).
///
/// Mirrors Go's `SessionLifecycleEventType` (`go/types.go:961`). Values
/// serialize as the dotted JSON strings the CLI sends (e.g. `"session.created"`).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum SessionLifecycleEventType {
    /// A new session was created.
    #[serde(rename = "session.created")]
    Created,
    /// A session was deleted.
    #[serde(rename = "session.deleted")]
    Deleted,
    /// A session's metadata was updated (e.g. summary regenerated).
    #[serde(rename = "session.updated")]
    Updated,
    /// A session moved into the foreground.
    #[serde(rename = "session.foreground")]
    Foreground,
    /// A session moved into the background.
    #[serde(rename = "session.background")]
    Background,
}

/// Optional metadata attached to a [`SessionLifecycleEvent`].
///
/// Mirrors Go's `SessionLifecycleEventMetadata` (`go/types.go:977`).
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct SessionLifecycleEventMetadata {
    /// ISO-8601 timestamp the session was created.
    #[serde(rename = "startTime")]
    pub start_time: String,
    /// ISO-8601 timestamp the session was last modified.
    #[serde(rename = "modifiedTime")]
    pub modified_time: String,
    /// Optional generated summary of the session conversation so far.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
}

/// A `session.lifecycle` notification dispatched to subscribers registered via
/// [`Client::on`](crate::Client::on) and
/// [`Client::on_event_type`](crate::Client::on_event_type).
///
/// Mirrors Go's `SessionLifecycleEvent` (`go/types.go:970`).
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct SessionLifecycleEvent {
    /// The kind of lifecycle change this event represents.
    #[serde(rename = "type")]
    pub event_type: SessionLifecycleEventType,
    /// Identifier of the session this event refers to.
    #[serde(rename = "sessionId")]
    pub session_id: String,
    /// Optional metadata describing the session at the time of the event.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub metadata: Option<SessionLifecycleEventMetadata>,
}

/// Opaque session identifier assigned by the CLI.
///
/// A newtype wrapper around `String` that provides type safety — prevents
/// accidentally passing a workspace ID or request ID where a session ID
/// is expected. Derefs to `str` for zero-friction borrowing.
#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[serde(transparent)]
pub struct SessionId(String);

impl SessionId {
    /// Create a new session ID from any string-like value.
    pub fn new(id: impl Into<String>) -> Self {
        Self(id.into())
    }

    /// Borrow the inner string.
    pub fn as_str(&self) -> &str {
        &self.0
    }

    /// Consume the wrapper, returning the inner string.
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

impl PartialEq<&SessionId> for SessionId {
    fn eq(&self, other: &&SessionId) -> bool {
        self.0 == other.0
    }
}

impl PartialEq<SessionId> for &SessionId {
    fn eq(&self, other: &SessionId) -> bool {
        self.0 == other.0
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
    /// Create a new request ID from any string-like value.
    pub fn new(id: impl Into<String>) -> Self {
        Self(id.into())
    }

    /// Consume the wrapper, returning the inner string.
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
/// Sent to the CLI as part of [`SessionConfig::tools`] / [`ResumeSessionConfig::tools`]
/// at session creation/resume time. The Rust SDK hand-authors this struct
/// (rather than using the schema-generated form) so it can carry runtime
/// hints — `overrides_built_in_tool`, `skip_permission` — that don't appear
/// in the wire schema but are honored by the CLI.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Tool {
    /// Tool identifier (e.g., `"bash"`, `"grep"`, `"str_replace_editor"`).
    pub name: String,
    /// Optional namespaced name for declarative filtering (e.g., `"playwright/navigate"`
    /// for MCP tools).
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub namespaced_name: Option<String>,
    /// Description of what the tool does.
    #[serde(default)]
    pub description: String,
    /// Optional instructions for how to use this tool effectively.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub instructions: Option<String>,
    /// JSON Schema for the tool's input parameters.
    #[serde(default, skip_serializing_if = "HashMap::is_empty")]
    pub parameters: HashMap<String, Value>,
    /// When `true`, this tool replaces a built-in tool of the same name
    /// (e.g. supplying a custom `grep` that the agent uses in place of the
    /// CLI's built-in implementation).
    #[serde(default, skip_serializing_if = "is_false")]
    pub overrides_built_in_tool: bool,
    /// When `true`, the CLI does not request permission before invoking
    /// this tool. Use with caution — the tool is responsible for any
    /// access control.
    #[serde(default, skip_serializing_if = "is_false")]
    pub skip_permission: bool,
}

#[inline]
fn is_false(b: &bool) -> bool {
    !*b
}

/// Configures a custom agent (sub-agent) for the session.
///
/// Custom agents have their own prompt, tool allowlist, and optionally
/// their own MCP servers and skill set. The agent named in
/// [`SessionConfig::agent`] (or the runtime default) is the active one
/// when the session starts.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CustomAgentConfig {
    /// Unique name of the custom agent.
    pub name: String,
    /// Display name for UI purposes.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub display_name: Option<String>,
    /// Description of what the agent does.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// List of tool names the agent can use. `None` means all tools.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub tools: Option<Vec<String>>,
    /// Prompt content for the agent.
    pub prompt: String,
    /// MCP servers specific to this agent.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub mcp_servers: Option<HashMap<String, Value>>,
    /// Whether the agent is available for model inference.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub infer: Option<bool>,
    /// Skill names to preload into this agent's context at startup.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub skills: Option<Vec<String>>,
}

/// Configures the default (built-in) agent that handles turns when no
/// custom agent is selected.
///
/// Use [`Self::excluded_tools`] to hide tools from the default agent
/// while keeping them available to custom sub-agents that list them in
/// their [`CustomAgentConfig::tools`].
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DefaultAgentConfig {
    /// Tool names to exclude from the default agent.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub excluded_tools: Option<Vec<String>>,
}

/// Configures infinite sessions: persistent workspaces with automatic
/// context-window compaction.
///
/// When enabled (default), sessions automatically manage context limits
/// through background compaction and persist state to a workspace
/// directory.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InfiniteSessionConfig {
    /// Whether infinite sessions are enabled. Defaults to `true` on the CLI.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub enabled: Option<bool>,
    /// Context utilization (0.0–1.0) at which background compaction starts.
    /// Default: 0.80.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub background_compaction_threshold: Option<f64>,
    /// Context utilization (0.0–1.0) at which the session blocks until
    /// compaction completes. Default: 0.95.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub buffer_exhaustion_threshold: Option<f64>,
}

/// Configures a custom inference provider (BYOK — Bring Your Own Key).
///
/// Routes session requests through an alternative model provider
/// (OpenAI-compatible, Azure, Anthropic, or local) instead of GitHub
/// Copilot's default routing.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderConfig {
    /// Provider type: `"openai"`, `"azure"`, or `"anthropic"`. Defaults to
    /// `"openai"` on the CLI.
    #[serde(default, skip_serializing_if = "Option::is_none", rename = "type")]
    pub provider_type: Option<String>,
    /// API format (openai/azure only): `"completions"` or `"responses"`.
    /// Defaults to `"completions"`.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub wire_api: Option<String>,
    /// API endpoint URL.
    pub base_url: String,
    /// API key. Optional for local providers like Ollama.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub api_key: Option<String>,
    /// Bearer token for authentication. Sets the `Authorization` header
    /// directly. Use for services requiring bearer-token auth instead of
    /// API key. Takes precedence over `api_key` when both are set.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub bearer_token: Option<String>,
    /// Azure-specific options.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub azure: Option<AzureProviderOptions>,
    /// Custom HTTP headers included in outbound provider requests.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub headers: Option<HashMap<String, String>>,
}

/// Azure-specific provider options.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureProviderOptions {
    /// Azure API version. Defaults to `"2024-10-21"`.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub api_version: Option<String>,
}

/// Configuration for creating a new session via the `session.create` RPC.
///
/// All fields are optional — the CLI applies sensible defaults.
///
/// # Field naming across SDKs
///
/// Rust field names are snake_case (`available_tools`, `system_message`);
/// they round-trip to the camelCase wire protocol via `#[serde(rename_all =
/// "camelCase")]`. When porting code from the TypeScript, Go, Python, or
/// .NET SDKs — or reading the raw JSON-RPC traces — fields appear as
/// `availableTools`, `systemMessage`, etc.
#[derive(Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionConfig {
    /// Model to use (e.g. `"gpt-4"`, `"claude-sonnet-4"`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Application name sent as `User-Agent` context.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Reasoning effort level (e.g. `"low"`, `"medium"`, `"high"`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
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
    /// Custom agents (sub-agents) configured for this session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub custom_agents: Option<Vec<CustomAgentConfig>>,
    /// Configures the built-in default agent. Use `excluded_tools` to
    /// hide tools from the default agent while keeping them available
    /// to custom sub-agents that reference them in their `tools` list.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default_agent: Option<DefaultAgentConfig>,
    /// Name of the custom agent to activate when the session starts.
    /// Must match the `name` of one of the agents in [`Self::custom_agents`].
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent: Option<String>,
    /// Configures infinite sessions: persistent workspace + automatic
    /// context-window compaction. Enabled by default on the CLI.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub infinite_sessions: Option<InfiniteSessionConfig>,
    /// Custom model provider (BYOK). When set, the session routes
    /// requests through this provider instead of the default Copilot
    /// routing.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider: Option<ProviderConfig>,
    /// Session-level event handler. The default is
    /// [`DenyAllHandler`](crate::handler::DenyAllHandler) — permission
    /// requests are denied; other events are no-ops. Use
    /// [`with_handler`](Self::with_handler) to install a custom handler.
    #[serde(skip)]
    pub handler: Option<Arc<dyn SessionHandler>>,
    /// Session lifecycle hook handler (pre/post tool use, session
    /// start/end, etc.). When set, the SDK auto-enables the wire-level
    /// `hooks` flag. Use [`with_hooks`](Self::with_hooks) to install one.
    #[serde(skip)]
    pub hooks_handler: Option<Arc<dyn SessionHooks>>,
    /// System-message transform. When set, the SDK injects the matching
    /// `action: "transform"` sections into the system message and routes
    /// `systemMessage.transform` RPC callbacks to it during the session.
    /// Use [`with_transform`](Self::with_transform) to install one.
    #[serde(skip)]
    pub transform: Option<Arc<dyn SystemMessageTransform>>,
}

impl std::fmt::Debug for SessionConfig {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("SessionConfig")
            .field("model", &self.model)
            .field("client_name", &self.client_name)
            .field("reasoning_effort", &self.reasoning_effort)
            .field("streaming", &self.streaming)
            .field("system_message", &self.system_message)
            .field("tools", &self.tools)
            .field("available_tools", &self.available_tools)
            .field("excluded_tools", &self.excluded_tools)
            .field("mcp_servers", &self.mcp_servers)
            .field("env_value_mode", &self.env_value_mode)
            .field("enable_config_discovery", &self.enable_config_discovery)
            .field("request_user_input", &self.request_user_input)
            .field("request_permission", &self.request_permission)
            .field("request_exit_plan_mode", &self.request_exit_plan_mode)
            .field("request_elicitation", &self.request_elicitation)
            .field("skill_directories", &self.skill_directories)
            .field("disabled_skills", &self.disabled_skills)
            .field("disabled_mcp_servers", &self.disabled_mcp_servers)
            .field("hooks", &self.hooks)
            .field("custom_agents", &self.custom_agents)
            .field("default_agent", &self.default_agent)
            .field("agent", &self.agent)
            .field("infinite_sessions", &self.infinite_sessions)
            .field("provider", &self.provider)
            .field("handler", &self.handler.as_ref().map(|_| "<set>"))
            .field(
                "hooks_handler",
                &self.hooks_handler.as_ref().map(|_| "<set>"),
            )
            .field("transform", &self.transform.as_ref().map(|_| "<set>"))
            .finish()
    }
}

impl SessionConfig {
    /// Install a custom [`SessionHandler`] for this session.
    pub fn with_handler(mut self, handler: Arc<dyn SessionHandler>) -> Self {
        self.handler = Some(handler);
        self
    }

    /// Install a [`SessionHooks`] handler. Automatically enables the
    /// wire-level `hooks` flag on session creation.
    pub fn with_hooks(mut self, hooks: Arc<dyn SessionHooks>) -> Self {
        self.hooks_handler = Some(hooks);
        self
    }

    /// Install a [`SystemMessageTransform`]. The SDK injects the matching
    /// `action: "transform"` sections into the system message and routes
    /// `systemMessage.transform` RPC callbacks to it during the session.
    pub fn with_transform(mut self, transform: Arc<dyn SystemMessageTransform>) -> Self {
        self.transform = Some(transform);
        self
    }

    /// Wrap the configured handler so every permission request is
    /// auto-approved. Forwards every non-permission event to the inner
    /// handler unchanged.
    ///
    /// If no handler has been installed via [`with_handler`](Self::with_handler),
    /// wraps a [`DenyAllHandler`](crate::handler::DenyAllHandler) — useful
    /// when you only care about permission policy and want the trait
    /// fallback responses for everything else.
    ///
    /// Order-independent: `with_handler(...).approve_all_permissions()` and
    /// `approve_all_permissions().with_handler(...)` are NOT equivalent —
    /// the second form discards the wrap because `with_handler` overwrites
    /// the handler field. Always call `approve_all_permissions` *after*
    /// `with_handler`.
    pub fn approve_all_permissions(mut self) -> Self {
        let inner = self
            .handler
            .take()
            .unwrap_or_else(|| Arc::new(crate::handler::DenyAllHandler));
        self.handler = Some(crate::permission::approve_all(inner));
        self
    }

    /// Wrap the configured handler so every permission request is
    /// auto-denied. See [`approve_all_permissions`](Self::approve_all_permissions)
    /// for ordering and default-handler semantics.
    pub fn deny_all_permissions(mut self) -> Self {
        let inner = self
            .handler
            .take()
            .unwrap_or_else(|| Arc::new(crate::handler::DenyAllHandler));
        self.handler = Some(crate::permission::deny_all(inner));
        self
    }

    /// Wrap the configured handler with a closure-based permission policy:
    /// `predicate` is called for each permission request; `true` approves,
    /// `false` denies. See
    /// [`approve_all_permissions`](Self::approve_all_permissions) for
    /// ordering and default-handler semantics.
    pub fn approve_permissions_if<F>(mut self, predicate: F) -> Self
    where
        F: Fn(&crate::types::PermissionRequestData) -> bool + Send + Sync + 'static,
    {
        let inner = self
            .handler
            .take()
            .unwrap_or_else(|| Arc::new(crate::handler::DenyAllHandler));
        self.handler = Some(crate::permission::approve_if(inner, predicate));
        self
    }
}

/// Configuration for resuming an existing session via the `session.resume` RPC.
///
/// See [`SessionConfig`] for the note on snake_case vs. camelCase field naming.
#[derive(Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ResumeSessionConfig {
    /// ID of the session to resume.
    pub session_id: SessionId,
    /// Application name sent as User-Agent context.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Enable streaming token deltas.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub streaming: Option<bool>,
    /// Re-supply the system message so the agent retains workspace context
    /// across CLI process restarts.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub system_message: Option<SystemMessageConfig>,
    /// Client-defined tools to re-supply on resume.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tools: Option<Vec<Tool>>,
    /// Blocklist of built-in tool names.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub excluded_tools: Option<Vec<String>>,
    /// Re-supply MCP servers so they remain available after app restart.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_servers: Option<Value>,
    /// How the CLI interprets env values in MCP configs.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub env_value_mode: Option<String>,
    /// Enable config discovery on resume.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_config_discovery: Option<bool>,
    /// Enable the ask_user tool.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_user_input: Option<bool>,
    /// Enable permission request RPCs.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_permission: Option<bool>,
    /// Enable exit-plan-mode request RPCs.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_exit_plan_mode: Option<bool>,
    /// Advertise elicitation provider capability on resume.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_elicitation: Option<bool>,
    /// Skill directory paths passed through to the Copilot CLI on resume.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skill_directories: Option<Vec<PathBuf>>,
    /// Enable session hooks on resume.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub hooks: Option<bool>,
    /// Custom agents to re-supply on resume.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub custom_agents: Option<Vec<CustomAgentConfig>>,
    /// Configures the built-in default agent on resume.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default_agent: Option<DefaultAgentConfig>,
    /// Name of the custom agent to activate.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent: Option<String>,
    /// Re-supply infinite session configuration on resume.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub infinite_sessions: Option<InfiniteSessionConfig>,
    /// Re-supply BYOK provider configuration on resume.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider: Option<ProviderConfig>,
    /// Session-level event handler. See [`SessionConfig::handler`].
    #[serde(skip)]
    pub handler: Option<Arc<dyn SessionHandler>>,
    /// Session hook handler. See [`SessionConfig::hooks_handler`].
    #[serde(skip)]
    pub hooks_handler: Option<Arc<dyn SessionHooks>>,
    /// System-message transform. See [`SessionConfig::transform`].
    #[serde(skip)]
    pub transform: Option<Arc<dyn SystemMessageTransform>>,
}

impl std::fmt::Debug for ResumeSessionConfig {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("ResumeSessionConfig")
            .field("session_id", &self.session_id)
            .field("client_name", &self.client_name)
            .field("streaming", &self.streaming)
            .field("system_message", &self.system_message)
            .field("tools", &self.tools)
            .field("excluded_tools", &self.excluded_tools)
            .field("mcp_servers", &self.mcp_servers)
            .field("env_value_mode", &self.env_value_mode)
            .field("enable_config_discovery", &self.enable_config_discovery)
            .field("request_user_input", &self.request_user_input)
            .field("request_permission", &self.request_permission)
            .field("request_exit_plan_mode", &self.request_exit_plan_mode)
            .field("request_elicitation", &self.request_elicitation)
            .field("skill_directories", &self.skill_directories)
            .field("hooks", &self.hooks)
            .field("custom_agents", &self.custom_agents)
            .field("default_agent", &self.default_agent)
            .field("agent", &self.agent)
            .field("infinite_sessions", &self.infinite_sessions)
            .field("provider", &self.provider)
            .field("handler", &self.handler.as_ref().map(|_| "<set>"))
            .field(
                "hooks_handler",
                &self.hooks_handler.as_ref().map(|_| "<set>"),
            )
            .field("transform", &self.transform.as_ref().map(|_| "<set>"))
            .finish()
    }
}

impl ResumeSessionConfig {
    /// Construct a `ResumeSessionConfig` with the given session ID and all
    /// other fields left unset. Combine with `.with_*` builders or struct
    /// update syntax (`..ResumeSessionConfig::new(id)`) to populate the
    /// fields you need.
    pub fn new(session_id: SessionId) -> Self {
        Self {
            session_id,
            client_name: None,
            streaming: None,
            system_message: None,
            tools: None,
            excluded_tools: None,
            mcp_servers: None,
            env_value_mode: None,
            enable_config_discovery: None,
            request_user_input: None,
            request_permission: None,
            request_exit_plan_mode: None,
            request_elicitation: None,
            skill_directories: None,
            hooks: None,
            custom_agents: None,
            default_agent: None,
            agent: None,
            infinite_sessions: None,
            provider: None,
            handler: None,
            hooks_handler: None,
            transform: None,
        }
    }

    /// Install a custom [`SessionHandler`] for this session.
    pub fn with_handler(mut self, handler: Arc<dyn SessionHandler>) -> Self {
        self.handler = Some(handler);
        self
    }

    /// Install a [`SessionHooks`] handler. Automatically enables the
    /// wire-level `hooks` flag on session resumption.
    pub fn with_hooks(mut self, hooks: Arc<dyn SessionHooks>) -> Self {
        self.hooks_handler = Some(hooks);
        self
    }

    /// Install a [`SystemMessageTransform`].
    pub fn with_transform(mut self, transform: Arc<dyn SystemMessageTransform>) -> Self {
        self.transform = Some(transform);
        self
    }

    /// Wrap the configured handler so every permission request is
    /// auto-approved. See
    /// [`SessionConfig::approve_all_permissions`] for semantics.
    pub fn approve_all_permissions(mut self) -> Self {
        let inner = self
            .handler
            .take()
            .unwrap_or_else(|| Arc::new(crate::handler::DenyAllHandler));
        self.handler = Some(crate::permission::approve_all(inner));
        self
    }

    /// Wrap the configured handler so every permission request is
    /// auto-denied. See
    /// [`SessionConfig::deny_all_permissions`] for semantics.
    pub fn deny_all_permissions(mut self) -> Self {
        let inner = self
            .handler
            .take()
            .unwrap_or_else(|| Arc::new(crate::handler::DenyAllHandler));
        self.handler = Some(crate::permission::deny_all(inner));
        self
    }

    /// Wrap the configured handler with a predicate-based permission policy.
    /// See [`SessionConfig::approve_permissions_if`] for semantics.
    pub fn approve_permissions_if<F>(mut self, predicate: F) -> Self
    where
        F: Fn(&crate::types::PermissionRequestData) -> bool + Send + Sync + 'static,
    {
        let inner = self
            .handler
            .take()
            .unwrap_or_else(|| Arc::new(crate::handler::DenyAllHandler));
        self.handler = Some(crate::permission::approve_if(inner, predicate));
        self
    }
}

/// Controls how the system message is constructed.
///
/// Use `mode: "append"` (default) to add content after the built-in system
/// message, `"replace"` to substitute it entirely, or `"customize"` for
/// section-level overrides.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SystemMessageConfig {
    /// How content is applied: `"append"` (default), `"replace"`, or `"customize"`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<String>,
    /// Content string to append or replace.
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
    /// Override action: `"replace"`, `"remove"`, `"append"`, `"prepend"`, or `"transform"`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub action: Option<String>,
    /// Content for the override operation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub content: Option<String>,
}

/// Response from `session.create`.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CreateSessionResult {
    /// The CLI-assigned session ID.
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

/// Parameters for the `session.send` RPC — sends a user message to the agent.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MessageOptions {
    /// Target session.
    pub session_id: SessionId,
    /// User message text.
    pub prompt: String,
    /// Session mode (e.g. `"interactive"`, `"plan"`, `"autopilot"`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<String>,
    /// File attachments to include with the message.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attachments: Option<Vec<Attachment>>,
}

/// Parameters for the `session.sendTelemetry` RPC.
#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTelemetryEvent {
    /// Telemetry event kind (for example, `"session_shutdown"`).
    pub kind: String,
    /// Non-restricted string properties to include with the telemetry event.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub properties: Option<HashMap<String, String>>,
    /// Restricted string properties that may contain sensitive data.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub restricted_properties: Option<HashMap<String, String>>,
    /// Numeric metrics to include with the telemetry event.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub metrics: Option<HashMap<String, f64>>,
}

/// Severity level for [`Session::log`](crate::session::Session::log) messages.
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum LogLevel {
    /// Informational message (default).
    #[default]
    Info,
    /// Warning message.
    Warning,
    /// Error message.
    Error,
}

/// Options for [`Session::log`](crate::session::Session::log).
///
/// Pass `None` to `log` for defaults (info level, persisted to the session
/// event log on disk).
#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LogOptions {
    /// Log severity. `None` lets the server pick (defaults to `info`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub level: Option<LogLevel>,
    /// When `Some(true)`, the message is transient and not persisted to the
    /// session event log on disk. `None` lets the server pick.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ephemeral: Option<bool>,
}

impl LogOptions {
    /// Set [`level`](Self::level).
    pub fn with_level(mut self, level: LogLevel) -> Self {
        self.level = Some(level);
        self
    }

    /// Set [`ephemeral`](Self::ephemeral).
    pub fn with_ephemeral(mut self, ephemeral: bool) -> Self {
        self.ephemeral = Some(ephemeral);
        self
    }
}

/// Options for [`Session::set_model`](crate::session::Session::set_model).
///
/// Pass `None` to `set_model` to switch model without any overrides.
#[derive(Debug, Clone, Default)]
pub struct SetModelOptions {
    /// Reasoning effort for the new model (e.g. `"low"`, `"medium"`,
    /// `"high"`, `"xhigh"`).
    pub reasoning_effort: Option<String>,
    /// Override individual model capabilities resolved by the runtime. Only
    /// fields set on the override are applied; the rest fall back to the
    /// runtime-resolved values for the model.
    pub model_capabilities: Option<crate::generated::api_types::ModelCapabilitiesOverride>,
}

impl SetModelOptions {
    /// Set [`reasoning_effort`](Self::reasoning_effort).
    pub fn with_reasoning_effort(mut self, effort: impl Into<String>) -> Self {
        self.reasoning_effort = Some(effort.into());
        self
    }

    /// Set [`model_capabilities`](Self::model_capabilities).
    pub fn with_model_capabilities(
        mut self,
        caps: crate::generated::api_types::ModelCapabilitiesOverride,
    ) -> Self {
        self.model_capabilities = Some(caps);
        self
    }
}

/// Response from the top-level `ping` RPC.
///
/// Mirrors Go's `PingResponse`. The `protocol_version` field is the most
/// commonly-inspected piece — see [`Client::verify_protocol_version`].
///
/// [`Client::verify_protocol_version`]: crate::Client::verify_protocol_version
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PingResponse {
    /// The message echoed back by the CLI.
    #[serde(default)]
    pub message: String,
    /// Server-side timestamp (Unix epoch milliseconds).
    #[serde(default)]
    pub timestamp: i64,
    /// The protocol version negotiated by the CLI, if reported.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub protocol_version: Option<u32>,
}

/// Parameters for the top-level `sendTelemetry` RPC.
#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ServerTelemetryEvent {
    /// Telemetry event kind (for example, `"app.launched"`).
    pub kind: String,
    /// SDK client name. Non-allowlisted values are hashed in telemetry.
    pub client_name: String,
    /// Non-restricted string properties to include with the telemetry event.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub properties: Option<HashMap<String, String>>,
    /// Restricted string properties that may contain sensitive data.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub restricted_properties: Option<HashMap<String, String>>,
    /// Numeric metrics to include with the telemetry event.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub metrics: Option<HashMap<String, f64>>,
}

/// Line range for file attachments.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentLineRange {
    /// First line (1-based).
    pub start: u32,
    /// Last line (inclusive).
    pub end: u32,
}

/// Cursor position within a file selection.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentSelectionPosition {
    /// Line number (0-based).
    pub line: u32,
    /// Character offset (0-based).
    pub character: u32,
}

/// Range of selected text within a file.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentSelectionRange {
    /// Start position.
    pub start: AttachmentSelectionPosition,
    /// End position.
    pub end: AttachmentSelectionPosition,
}

/// Type of GitHub reference attachment.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum GitHubReferenceType {
    /// GitHub issue.
    Issue,
    /// GitHub pull request.
    Pr,
    /// GitHub discussion.
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
    /// A file path, optionally with a line range.
    File {
        /// Absolute path to the file.
        path: PathBuf,
        /// Label shown in the UI.
        #[serde(skip_serializing_if = "Option::is_none")]
        display_name: Option<String>,
        /// Optional line range to focus on.
        #[serde(skip_serializing_if = "Option::is_none")]
        line_range: Option<AttachmentLineRange>,
    },
    /// A directory path.
    Directory {
        /// Absolute path to the directory.
        path: PathBuf,
        /// Label shown in the UI.
        #[serde(skip_serializing_if = "Option::is_none")]
        display_name: Option<String>,
    },
    /// A text selection within a file.
    Selection {
        /// Path to the file containing the selection.
        file_path: PathBuf,
        /// The selected text content.
        text: String,
        /// Label shown in the UI.
        #[serde(skip_serializing_if = "Option::is_none")]
        display_name: Option<String>,
        /// Character range of the selection.
        selection: AttachmentSelectionRange,
    },
    /// Raw binary data (e.g. an image).
    Blob {
        /// Base64-encoded data.
        data: String,
        /// MIME type of the data.
        mime_type: String,
        /// Label shown in the UI.
        #[serde(skip_serializing_if = "Option::is_none")]
        display_name: Option<String>,
    },
    /// A reference to a GitHub issue, PR, or discussion.
    #[serde(rename = "github_reference")]
    GitHubReference {
        /// Issue/PR/discussion number.
        number: u64,
        /// Title of the referenced item.
        title: String,
        /// Kind of reference.
        reference_type: GitHubReferenceType,
        /// Current state (e.g. "open", "closed").
        state: String,
        /// URL to the referenced item.
        url: String,
    },
}

impl Attachment {
    /// Returns the display name, if set.
    pub fn display_name(&self) -> Option<&str> {
        match self {
            Self::File { display_name, .. }
            | Self::Directory { display_name, .. }
            | Self::Selection { display_name, .. }
            | Self::Blob { display_name, .. } => display_name.as_deref(),
            Self::GitHubReference { .. } => None,
        }
    }

    /// Returns a human-readable label, deriving one from the path if needed.
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

/// Options for sending a user message to the agent.
///
/// Used by both [`Session::send_message`](crate::session::Session::send_message) and
/// [`Session::send_and_wait`](crate::session::Session::send_and_wait); the
/// `wait_timeout` field is honored only by `send_and_wait` and is ignored by
/// `send_message`.
///
/// `SendOptions` is `#[non_exhaustive]` and constructed via [`SendOptions::new`]
/// plus the `with_*` chain so future fields can land without breaking callers.
/// For the trivial case, both `&str` and `String` implement `Into<SendOptions>`,
/// so:
///
/// ```no_run
/// # use copilot::session::Session;
/// # async fn run(session: Session) -> Result<(), copilot::Error> {
/// session.send_message("hello").await?;
/// # Ok(()) }
/// ```
///
/// is equivalent to:
///
/// ```no_run
/// # use copilot::session::Session;
/// # use copilot::types::SendOptions;
/// # async fn run(session: Session) -> Result<(), copilot::Error> {
/// session.send_message(SendOptions::new("hello")).await?;
/// # Ok(()) }
/// ```
#[derive(Debug, Clone)]
#[non_exhaustive]
pub struct SendOptions {
    /// The user prompt to send.
    pub prompt: String,
    /// Optional permission mode for this turn (e.g. `"agent"`, `"autopilot"`).
    pub mode: Option<String>,
    /// Optional attachments to include with the message.
    pub attachments: Option<Vec<Attachment>>,
    /// Maximum time to wait for the session to go idle. Honored only by
    /// `send_and_wait`. Defaults to 60 seconds when unset.
    pub wait_timeout: Option<Duration>,
}

impl SendOptions {
    /// Build a new `SendOptions` with just a prompt.
    pub fn new(prompt: impl Into<String>) -> Self {
        Self {
            prompt: prompt.into(),
            mode: None,
            attachments: None,
            wait_timeout: None,
        }
    }

    /// Set the permission mode (e.g. `"agent"`, `"autopilot"`).
    pub fn with_mode(mut self, mode: impl Into<String>) -> Self {
        self.mode = Some(mode.into());
        self
    }

    /// Attach files / selections / blobs to the message.
    pub fn with_attachments(mut self, attachments: Vec<Attachment>) -> Self {
        self.attachments = Some(attachments);
        self
    }

    /// Override the default 60-second wait timeout for `send_and_wait`.
    pub fn with_wait_timeout(mut self, timeout: Duration) -> Self {
        self.wait_timeout = Some(timeout);
        self
    }
}

impl From<&str> for SendOptions {
    fn from(prompt: &str) -> Self {
        Self::new(prompt)
    }
}

impl From<String> for SendOptions {
    fn from(prompt: String) -> Self {
        Self::new(prompt)
    }
}

impl From<&String> for SendOptions {
    fn from(prompt: &String) -> Self {
        Self::new(prompt.clone())
    }
}

/// Wrapper for session event notifications received from the CLI.
///
/// The CLI sends these as JSON-RPC notifications on the `session.event` method.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionEventNotification {
    /// The session this event belongs to.
    pub session_id: SessionId,
    /// The event payload.
    pub event: SessionEvent,
}

/// A single event in a session's timeline.
///
/// Events form a linked chain via `parent_id`. The `event_type` string
/// identifies the kind (e.g. `"assistant.message_delta"`, `"session.idle"`,
/// `"tool.execution_start"`). Event-specific payload is in `data` as
/// untyped JSON.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionEvent {
    /// Unique event ID (UUID v4).
    pub id: String,
    /// ISO 8601 timestamp.
    pub timestamp: String,
    /// ID of the preceding event in the chain.
    pub parent_id: Option<String>,
    /// Transient events that are not persisted to disk.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ephemeral: Option<bool>,
    /// Debug timestamp: when the CLI received this event (ms since epoch).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub debug_cli_received_at_ms: Option<i64>,
    /// Debug timestamp: when the event was forwarded over WebSocket.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub debug_ws_forwarded_at_ms: Option<i64>,
    /// Event type string (e.g. `"assistant.message"`, `"session.idle"`).
    #[serde(rename = "type")]
    pub event_type: String,
    /// Event-specific data. Structure depends on `event_type`.
    pub data: Value,
}

impl SessionEvent {
    /// Parse the string `event_type` into a typed [`SessionEventType`](crate::generated::SessionEventType) enum.
    ///
    /// Returns `SessionEventType::Unknown` for unrecognized event types,
    /// ensuring forward compatibility with newer CLI versions.
    pub fn parsed_type(&self) -> crate::generated::SessionEventType {
        use serde::de::IntoDeserializer;
        let deserializer: serde::de::value::StrDeserializer<'_, serde::de::value::Error> =
            self.event_type.as_str().into_deserializer();
        crate::generated::SessionEventType::deserialize(deserializer)
            .unwrap_or(crate::generated::SessionEventType::Unknown)
    }

    /// Deserialize the event `data` field into a typed struct.
    ///
    /// Returns `None` if deserialization fails (e.g. unknown event type
    /// or schema mismatch). Prefer typed data accessors for specific
    /// event types where you need strongly-typed field access.
    pub fn typed_data<T: serde::de::DeserializeOwned>(&self) -> Option<T> {
        serde_json::from_value(self.data.clone()).ok()
    }

    /// `model_call` errors are transient — the CLI agent loop continues
    /// after them and may succeed on the next turn. These should not be
    /// treated as session-ending errors.
    pub fn is_transient_error(&self) -> bool {
        self.event_type == "session.error"
            && self.data.get("errorType").and_then(|v| v.as_str()) == Some("model_call")
    }
}

/// A request from the CLI to invoke a client-defined tool.
///
/// Received as a JSON-RPC request on the `tool.call` method. The client
/// must respond with a [`ToolResultResponse`].
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolInvocation {
    /// Session that owns this tool call.
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
    /// Optional log message for the session timeline.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_log: Option<String>,
    /// Error message, if the tool failed.
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
    /// The tool result payload.
    pub result: ToolResult,
}

/// Metadata for a persisted session, returned by `session.list`.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadata {
    /// The session's unique identifier.
    pub session_id: SessionId,
    /// ISO 8601 timestamp when the session was created.
    pub start_time: String,
    /// ISO 8601 timestamp of the last modification.
    pub modified_time: String,
    /// Agent-generated session summary.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
    /// Whether the session is running remotely.
    pub is_remote: bool,
}

/// Response from `session.list`.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ListSessionsResponse {
    /// The list of session metadata entries.
    pub sessions: Vec<SessionMetadata>,
}

/// Response from `session.getMetadata`.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GetSessionMetadataResponse {
    /// The session metadata, or `None` if the session was not found.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session: Option<SessionMetadata>,
}

/// Response from `session.getLastId`.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GetLastSessionIdResponse {
    /// The most recently updated session ID, or `None` if no sessions exist.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
}

/// Response from `session.getForeground`.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GetForegroundSessionResponse {
    /// The current foreground session ID, or `None` if no foreground session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
}

/// Response from `session.getMessages`.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GetMessagesResponse {
    /// Timeline events for the session.
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
#[derive(Debug, Clone, Copy)]
pub enum InputFormat {
    /// Email address.
    Email,
    /// URI.
    Uri,
    /// Calendar date.
    Date,
    /// Date and time.
    DateTime,
}

impl InputFormat {
    /// Returns the JSON Schema format string for this variant.
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::Email => "email",
            Self::Uri => "uri",
            Self::Date => "date",
            Self::DateTime => "date-time",
        }
    }
}

/// Re-exports of generated protocol types that are part of the SDK's
/// public API surface. The canonical definitions live in
/// [`crate::generated::api_types`]; they live here so the crate-root
/// `pub use types::*` surfaces them alongside hand-written SDK types.
pub use crate::generated::api_types::{
    Model, ModelBilling, ModelCapabilities, ModelCapabilitiesLimits, ModelCapabilitiesLimitsVision,
    ModelCapabilitiesSupports, ModelList, ModelPolicy,
};

/// Data sent by the CLI for permission-related events.
///
/// Used for both the `permission.request` RPC call (which expects a response)
/// and `permission.requested` notifications (fire-and-forget). Contains the
/// full params object. Note that `requestId` is also available as a separate
/// field on `HandlerEvent::PermissionRequest`.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequestData {
    /// The full permission request params from the CLI. The shape varies by
    /// permission type and CLI version, so we preserve it as `Value`.
    #[serde(flatten)]
    pub extra: Value,
}

/// Data sent by the CLI with an `exitPlanMode.request` RPC call.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExitPlanModeData {
    /// Markdown summary of the plan presented to the user.
    #[serde(default)]
    pub summary: String,
    /// Full plan content (e.g. the plan.md body), if available.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub plan_content: Option<String>,
    /// Allowed exit actions (e.g. "interactive", "autopilot", "autopilot_fleet").
    #[serde(default)]
    pub actions: Vec<String>,
    /// Which action the CLI recommends, defaults to "autopilot".
    #[serde(default = "default_recommended_action")]
    pub recommended_action: String,
}

fn default_recommended_action() -> String {
    "autopilot".to_string()
}

impl Default for ExitPlanModeData {
    fn default() -> Self {
        Self {
            summary: String::new(),
            plan_content: None,
            actions: Vec::new(),
            recommended_action: default_recommended_action(),
        }
    }
}

#[cfg(test)]
mod tests {
    use std::path::PathBuf;

    use serde_json::json;

    use super::{
        Attachment, AttachmentLineRange, AttachmentSelectionPosition, AttachmentSelectionRange,
        GitHubReferenceType, ensure_attachment_display_names,
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
                "url": "https://github.com/github/github-app/issues/42"
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
                && url == "https://github.com/github/github-app/issues/42"
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

#[cfg(test)]
mod permission_builder_tests {
    use std::sync::Arc;

    use crate::handler::{
        ApproveAllHandler, HandlerEvent, HandlerResponse, PermissionResult, SessionHandler,
    };
    use crate::types::{
        PermissionRequestData, RequestId, ResumeSessionConfig, SessionConfig, SessionId,
    };

    fn permission_event() -> HandlerEvent {
        HandlerEvent::PermissionRequest {
            session_id: SessionId::from("s1"),
            request_id: RequestId::new("1"),
            data: PermissionRequestData {
                extra: serde_json::json!({"tool": "shell"}),
            },
        }
    }

    async fn dispatch(handler: &Arc<dyn SessionHandler>) -> HandlerResponse {
        handler.on_event(permission_event()).await
    }

    #[tokio::test]
    async fn session_config_approve_all_wraps_existing_handler() {
        let cfg = SessionConfig::default()
            .with_handler(Arc::new(ApproveAllHandler))
            .approve_all_permissions();
        let handler = cfg.handler.expect("handler should be set");
        match dispatch(&handler).await {
            HandlerResponse::Permission(PermissionResult::Approved) => {}
            other => panic!("expected Approved, got {other:?}"),
        }
    }

    #[tokio::test]
    async fn session_config_approve_all_defaults_to_deny_inner() {
        // Without with_handler, the wrap defaults to DenyAllHandler. The
        // approve-all wrap intercepts permission events, so they're still
        // approved -- the inner handler is consulted only for other events.
        let cfg = SessionConfig::default().approve_all_permissions();
        let handler = cfg.handler.expect("handler should be set");
        match dispatch(&handler).await {
            HandlerResponse::Permission(PermissionResult::Approved) => {}
            other => panic!("expected Approved, got {other:?}"),
        }
    }

    #[tokio::test]
    async fn session_config_deny_all_denies() {
        let cfg = SessionConfig::default()
            .with_handler(Arc::new(ApproveAllHandler))
            .deny_all_permissions();
        let handler = cfg.handler.expect("handler should be set");
        match dispatch(&handler).await {
            HandlerResponse::Permission(PermissionResult::Denied) => {}
            other => panic!("expected Denied, got {other:?}"),
        }
    }

    #[tokio::test]
    async fn session_config_approve_permissions_if_consults_predicate() {
        let cfg = SessionConfig::default()
            .with_handler(Arc::new(ApproveAllHandler))
            .approve_permissions_if(|data| {
                data.extra.get("tool").and_then(|v| v.as_str()) != Some("shell")
            });
        let handler = cfg.handler.expect("handler should be set");
        match dispatch(&handler).await {
            HandlerResponse::Permission(PermissionResult::Denied) => {}
            other => panic!("expected Denied for shell, got {other:?}"),
        }
    }

    #[tokio::test]
    async fn resume_session_config_approve_all_wraps_existing_handler() {
        let cfg = ResumeSessionConfig::new(SessionId::from("s1"))
            .with_handler(Arc::new(ApproveAllHandler))
            .approve_all_permissions();
        let handler = cfg.handler.expect("handler should be set");
        match dispatch(&handler).await {
            HandlerResponse::Permission(PermissionResult::Approved) => {}
            other => panic!("expected Approved, got {other:?}"),
        }
    }
}
