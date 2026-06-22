//! Protocol types shared between the SDK and the GitHub Copilot CLI.
//!
//! These types map directly to the JSON-RPC request/response payloads
//! defined by the GitHub Copilot CLI protocol. They are used for session
//! configuration, event handling, tool invocations, and model queries.

use std::collections::HashMap;
use std::path::{Path, PathBuf};
use std::sync::Arc;
use std::time::Duration;

use serde::{Deserialize, Serialize};
use serde_json::Value;

use crate::canvas::{CanvasDeclaration, CanvasHandler};
pub use crate::copilot_request_handler::{
    CopilotHttpRequest, CopilotHttpResponse, CopilotHttpResponseBody, CopilotRequestContext,
    CopilotRequestError, CopilotRequestHandler, CopilotRequestTransport, CopilotWebSocketHandler,
    CopilotWebSocketMessage, CopilotWebSocketResponse, ForwardingCopilotWebSocketHandler,
    ForwardingCopilotWebSocketHandlerBuilder, WebSocketTransform, forward_http,
};
use crate::generated::api_types::OpenCanvasInstance;
/// Context window tier for models that support tiered context windows.
pub use crate::generated::session_events::ContextTier;
use crate::generated::session_events::ReasoningSummary;
use crate::handler::{
    AutoModeSwitchHandler, ElicitationHandler, ExitPlanModeHandler, PermissionHandler,
    UserInputHandler,
};
use crate::hooks::SessionHooks;
pub use crate::session_fs::{
    DirEntry, DirEntryKind, FileInfo, FsError, SessionFsCapabilities, SessionFsConfig,
    SessionFsConventions, SessionFsProvider, SessionFsSqliteProvider, SessionFsSqliteQueryResult,
    SessionFsSqliteQueryType,
};
pub use crate::trace_context::{TraceContext, TraceContextProvider};
use crate::transforms::SystemMessageTransform;

/// Lifecycle state of a [`Client`](crate::Client) connection. Internal —
/// not part of the public API.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
#[allow(dead_code)]
#[non_exhaustive]
pub(crate) enum ConnectionState {
    /// No CLI process is attached or the process has exited cleanly.
    Disconnected,
    /// The client is starting up (spawning the CLI, negotiating protocol).
    Connecting,
    /// The client is connected and ready to handle RPC traffic.
    Connected,
    /// Startup failed or the connection encountered an unrecoverable error.
    Error,
}

/// Type of [`SessionLifecycleEvent`] received via [`Client::subscribe_lifecycle`](crate::Client::subscribe_lifecycle).
///
/// Values serialize as the dotted JSON strings the CLI sends (e.g.
/// `"session.created"`).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[non_exhaustive]
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

/// A `session.lifecycle` notification dispatched to subscribers obtained via
/// [`Client::subscribe_lifecycle`](crate::Client::subscribe_lifecycle).
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct SessionLifecycleEvent {
    /// The kind of lifecycle change this event represents.
    #[serde(rename = "type")]
    pub event_type: SessionLifecycleEventType,
    /// Identifier of the session this event refers to.
    #[serde(rename = "sessionId")]
    pub session_id: SessionId,
    /// Optional metadata describing the session at the time of the event.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub metadata: Option<SessionLifecycleEventMetadata>,
}

/// Opaque session identifier assigned by the CLI.
///
/// A newtype wrapper around `String` that provides type safety — prevents
/// accidentally passing a workspace ID or request ID where a session ID
/// is expected. Derefs to `str` for zero-friction borrowing.
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
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
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
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
///
/// A `Tool` may optionally carry a [`handler`](Self::handler): an
/// `Arc<dyn ToolHandler>` that implements the tool's runtime behavior.
/// When present, the SDK dispatches matching `external_tool.requested`
/// broadcasts to it automatically. When absent (`None`), the tool is
/// declaration-only — another connected client must service incoming
/// invocations.
#[derive(Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
#[non_exhaustive]
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
    /// Controls whether the tool may be deferred (loaded lazily via tool
    /// search) rather than always pre-loaded. When [`DeferMode::Auto`], the
    /// tool can be deferred and surfaced through tool search. When
    /// [`DeferMode::Never`], the tool is always pre-loaded. `None` lets the
    /// runtime decide.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub defer: Option<DeferMode>,
    /// Optional runtime implementation. When `Some`, the SDK dispatches
    /// matching `external_tool.requested` broadcasts to this handler.
    /// When `None`, the tool is declaration-only.
    ///
    /// Skipped during serialization — the handler is runtime behavior,
    /// not part of the wire representation.
    ///
    /// Crate-private to enforce builder semantics: external callers must
    /// install a handler through [`Tool::with_handler`] and inspect via
    /// [`Tool::handler`], so an already-attached handler cannot be
    /// silently overwritten by direct field assignment.
    #[serde(skip)]
    pub(crate) handler: Option<Arc<dyn crate::tool::ToolHandler>>,
}

#[inline]
fn is_false(b: &bool) -> bool {
    !*b
}

/// Controls whether a [`Tool`] may be deferred (loaded lazily via tool search)
/// rather than always pre-loaded.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum DeferMode {
    /// The tool can be deferred and surfaced through tool search.
    Auto,
    /// The tool is always pre-loaded.
    Never,
}

impl Tool {
    /// Construct a new [`Tool`] with the given name and otherwise default
    /// values. The struct is `#[non_exhaustive]`, so external callers
    /// cannot use struct-literal syntax — use this builder or
    /// [`Default::default`] plus mut-let.
    ///
    /// # Example
    ///
    /// ```
    /// # use github_copilot_sdk::types::Tool;
    /// # use serde_json::json;
    /// let tool = Tool::new("greet")
    ///     .with_description("Say hello to a user")
    ///     .with_parameters(json!({
    ///         "type": "object",
    ///         "properties": { "name": { "type": "string" } },
    ///         "required": ["name"]
    ///     }));
    /// # let _ = tool;
    /// ```
    pub fn new(name: impl Into<String>) -> Self {
        Self {
            name: name.into(),
            ..Default::default()
        }
    }

    /// Set the namespaced name for declarative filtering (e.g.
    /// `"playwright/navigate"` for MCP tools).
    pub fn with_namespaced_name(mut self, namespaced_name: impl Into<String>) -> Self {
        self.namespaced_name = Some(namespaced_name.into());
        self
    }

    /// Set the human-readable description of what the tool does.
    pub fn with_description(mut self, description: impl Into<String>) -> Self {
        self.description = description.into();
        self
    }

    /// Set optional instructions for how to use this tool effectively.
    pub fn with_instructions(mut self, instructions: impl Into<String>) -> Self {
        self.instructions = Some(instructions.into());
        self
    }

    /// Set the JSON Schema for the tool's input parameters.
    ///
    /// Accepts a JSON Schema as a `serde_json::Value`, typically built with
    /// `serde_json::json!({...})` or returned by `schema_for` (available
    /// with the `derive` feature). Tool parameter schemas are always
    /// top-level JSON objects (`{"type": "object", ...}`).
    ///
    /// # Panics
    ///
    /// Panics if `parameters` is not a JSON object. Use
    /// [`crate::tool::try_tool_parameters`] and assign to
    /// [`Tool::parameters`] directly when the schema comes from dynamic
    /// input and should produce a recoverable error instead.
    pub fn with_parameters(mut self, parameters: Value) -> Self {
        self.parameters = crate::tool::tool_parameters(parameters);
        self
    }

    /// Mark this tool as overriding a built-in tool of the same name.
    /// E.g. supplying a custom `grep` that the agent uses in place of the
    /// CLI's built-in implementation.
    pub fn with_overrides_built_in_tool(mut self, overrides: bool) -> Self {
        self.overrides_built_in_tool = overrides;
        self
    }

    /// When `true`, the CLI will not request permission before invoking
    /// this tool. Use with caution — the tool is responsible for any
    /// access control.
    pub fn with_skip_permission(mut self, skip: bool) -> Self {
        self.skip_permission = skip;
        self
    }

    /// Set the deferral mode controlling whether the tool may be loaded
    /// lazily via tool search ([`DeferMode::Auto`]) or always pre-loaded
    /// ([`DeferMode::Never`]).
    pub fn with_defer(mut self, defer: DeferMode) -> Self {
        self.defer = Some(defer);
        self
    }

    /// Attach a runtime implementation. The SDK will dispatch matching
    /// `external_tool.requested` broadcasts to `handler` for this tool's
    /// name. Without a handler the tool is declaration-only.
    pub fn with_handler(mut self, handler: Arc<dyn crate::tool::ToolHandler>) -> Self {
        self.handler = Some(handler);
        self
    }

    /// Returns the attached runtime handler, if any.
    ///
    /// Read-only inspection — to install or replace a handler, use
    /// [`Tool::with_handler`].
    pub fn handler(&self) -> Option<&Arc<dyn crate::tool::ToolHandler>> {
        self.handler.as_ref()
    }
}

impl std::fmt::Debug for Tool {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("Tool")
            .field("name", &self.name)
            .field("namespaced_name", &self.namespaced_name)
            .field("description", &self.description)
            .field("instructions", &self.instructions)
            .field("parameters", &self.parameters)
            .field("overrides_built_in_tool", &self.overrides_built_in_tool)
            .field("skip_permission", &self.skip_permission)
            .field("defer", &self.defer)
            .field(
                "handler",
                &self.handler.as_ref().map(|_| "<set>").unwrap_or("None"),
            )
            .finish()
    }
}

/// Context passed to a [`CommandHandler`] when a registered slash command
/// is executed by the user.
#[non_exhaustive]
#[derive(Debug, Clone)]
pub struct CommandContext {
    /// Session ID where the command was invoked.
    pub session_id: SessionId,
    /// The full command text (e.g. `"/deploy production"`).
    pub command: String,
    /// Command name without the leading `/` (e.g. `"deploy"`).
    pub command_name: String,
    /// Raw argument string after the command name (e.g. `"production"`).
    pub args: String,
}

/// Handler invoked when a registered slash command is executed.
///
/// Returning `Err(_)` causes the SDK to forward the error message back to
/// the CLI via `session.commands.handlePendingCommand` so the TUI can
/// surface it. Returning `Ok(())` reports success.
#[async_trait::async_trait]
pub trait CommandHandler: Send + Sync {
    /// Called when the user invokes the command this handler is registered for.
    async fn on_command(&self, ctx: CommandContext) -> Result<(), crate::Error>;
}

/// Definition of a slash command registered with the session.
///
/// When the CLI is running with a TUI, registered commands appear as
/// `/name` for the user to invoke. Only `name` and `description` are sent
/// over the wire — the handler is local to this SDK process.
#[non_exhaustive]
#[derive(Clone)]
pub struct CommandDefinition {
    /// Command name (without leading `/`).
    pub name: String,
    /// Human-readable description shown in command-completion UI.
    pub description: Option<String>,
    /// Handler invoked when the command is executed.
    pub handler: Arc<dyn CommandHandler>,
}

impl CommandDefinition {
    /// Construct a new command definition. Use [`with_description`](Self::with_description)
    /// to add a description.
    pub fn new(name: impl Into<String>, handler: Arc<dyn CommandHandler>) -> Self {
        Self {
            name: name.into(),
            description: None,
            handler,
        }
    }

    /// Set the human-readable description shown in the CLI's command-completion UI.
    pub fn with_description(mut self, description: impl Into<String>) -> Self {
        self.description = Some(description.into());
        self
    }
}

impl std::fmt::Debug for CommandDefinition {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("CommandDefinition")
            .field("name", &self.name)
            .field("description", &self.description)
            .field("handler", &"<set>")
            .finish()
    }
}

impl Serialize for CommandDefinition {
    fn serialize<S: serde::Serializer>(&self, serializer: S) -> Result<S::Ok, S::Error> {
        use serde::ser::SerializeStruct;
        let len = if self.description.is_some() { 2 } else { 1 };
        let mut state = serializer.serialize_struct("CommandDefinition", len)?;
        state.serialize_field("name", &self.name)?;
        if let Some(description) = &self.description {
            state.serialize_field("description", description)?;
        }
        state.end()
    }
}

/// Configures a custom agent (sub-agent) for the session.
///
/// Custom agents have their own prompt, tool allowlist, and optionally
/// their own MCP servers and skill set. The agent named in
/// [`SessionConfig::agent`] (or the runtime default) is the active one
/// when the session starts.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
#[non_exhaustive]
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
    pub mcp_servers: Option<HashMap<String, McpServerConfig>>,
    /// Whether the agent is available for model inference.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub infer: Option<bool>,
    /// Skill names to preload into this agent's context at startup.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub skills: Option<Vec<String>>,
    /// Model identifier for this agent (e.g. `"claude-haiku-4.5"`).
    ///
    /// When set, the runtime will attempt to use this model for the agent,
    /// falling back to the parent session model if unavailable.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
}

impl CustomAgentConfig {
    /// Construct a custom agent configuration with the required `name`
    /// and `prompt` fields populated.
    ///
    /// All other fields default to unset; use the `with_*` chain to
    /// customize them. Fields are also `pub` if direct assignment is
    /// preferred for `Option<T>` pass-through.
    pub fn new(name: impl Into<String>, prompt: impl Into<String>) -> Self {
        Self {
            name: name.into(),
            prompt: prompt.into(),
            ..Self::default()
        }
    }

    /// Set the display name shown in the CLI's agent-selection UI.
    pub fn with_display_name(mut self, display_name: impl Into<String>) -> Self {
        self.display_name = Some(display_name.into());
        self
    }

    /// Set the description of what the agent does.
    pub fn with_description(mut self, description: impl Into<String>) -> Self {
        self.description = Some(description.into());
        self
    }

    /// Restrict the agent to a specific tool allowlist. When unset, the
    /// agent inherits the parent session's tool set.
    pub fn with_tools<I, S>(mut self, tools: I) -> Self
    where
        I: IntoIterator<Item = S>,
        S: Into<String>,
    {
        self.tools = Some(tools.into_iter().map(Into::into).collect());
        self
    }

    /// Configure agent-specific MCP servers.
    pub fn with_mcp_servers(mut self, mcp_servers: HashMap<String, McpServerConfig>) -> Self {
        self.mcp_servers = Some(mcp_servers);
        self
    }

    /// Whether the agent participates in model inference.
    pub fn with_infer(mut self, infer: bool) -> Self {
        self.infer = Some(infer);
        self
    }

    /// Set the skills preloaded into the agent's context at startup.
    pub fn with_skills<I, S>(mut self, skills: I) -> Self
    where
        I: IntoIterator<Item = S>,
        S: Into<String>,
    {
        self.skills = Some(skills.into_iter().map(Into::into).collect());
        self
    }

    /// Set the model identifier for this agent.
    pub fn with_model(mut self, model: impl Into<String>) -> Self {
        self.model = Some(model.into());
        self
    }
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

/// Configuration for large tool output handling.
///
/// When a tool produces output exceeding [`max_size_bytes`](Self::max_size_bytes),
/// the SDK writes the full output to a file in [`output_directory`](Self::output_directory)
/// and returns a truncated preview to the model.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
#[non_exhaustive]
pub struct LargeToolOutputConfig {
    /// Whether large tool output handling is enabled. Defaults to `true` on the CLI.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub enabled: Option<bool>,
    /// Maximum tool output size in bytes before it is redirected to a file.
    /// Defaults to 50KB on the CLI.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub max_size_bytes: Option<u64>,
    /// Directory where large tool output files are written. Defaults to
    /// the OS temp directory on the CLI.
    #[serde(default, rename = "outputDir", skip_serializing_if = "Option::is_none")]
    pub output_directory: Option<PathBuf>,
}

impl LargeToolOutputConfig {
    /// Construct an empty [`LargeToolOutputConfig`]; all fields default to
    /// unset (the CLI applies its own defaults).
    pub fn new() -> Self {
        Self::default()
    }

    /// Toggle large tool output handling on or off.
    pub fn with_enabled(mut self, enabled: bool) -> Self {
        self.enabled = Some(enabled);
        self
    }

    /// Set the maximum tool output size in bytes before it is redirected to a file.
    pub fn with_max_size_bytes(mut self, max_size_bytes: u64) -> Self {
        self.max_size_bytes = Some(max_size_bytes);
        self
    }

    /// Set the directory where large tool output files are written.
    pub fn with_output_directory<P: Into<PathBuf>>(mut self, output_directory: P) -> Self {
        self.output_directory = Some(output_directory.into());
        self
    }
}

/// Configures infinite sessions: persistent workspaces with automatic
/// context-window compaction.
///
/// When enabled (default), sessions automatically manage context limits
/// through background compaction and persist state to a workspace
/// directory.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
#[non_exhaustive]
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

impl InfiniteSessionConfig {
    /// Construct an empty [`InfiniteSessionConfig`]; all fields default to
    /// unset (the CLI applies its own defaults).
    pub fn new() -> Self {
        Self::default()
    }

    /// Toggle infinite sessions on or off. Defaults to `true` on the CLI
    /// when unset.
    pub fn with_enabled(mut self, enabled: bool) -> Self {
        self.enabled = Some(enabled);
        self
    }

    /// Set the context utilization (0.0–1.0) at which background
    /// compaction starts.
    pub fn with_background_compaction_threshold(mut self, threshold: f64) -> Self {
        self.background_compaction_threshold = Some(threshold);
        self
    }

    /// Set the context utilization (0.0–1.0) at which the session blocks
    /// until compaction completes.
    pub fn with_buffer_exhaustion_threshold(mut self, threshold: f64) -> Self {
        self.buffer_exhaustion_threshold = Some(threshold);
        self
    }
}

/// Per-session configuration for the runtime memory feature.
///
/// Supplied via [`SessionConfig::with_memory`] /
/// [`ResumeSessionConfig::with_memory`]. When a session is created or resumed
/// without a memory configuration, the runtime applies its own default for the
/// memory feature.
///
/// The type is extensible: today it carries [`enabled`](Self::enabled), and
/// further tuning knobs can be added as optional fields without a breaking
/// change.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
#[non_exhaustive]
pub struct MemoryConfiguration {
    /// Whether the memory feature is enabled for this session.
    pub enabled: bool,
}

impl MemoryConfiguration {
    /// A configuration with the memory feature enabled.
    pub fn enabled() -> Self {
        Self { enabled: true }
    }

    /// A configuration with the memory feature disabled.
    pub fn disabled() -> Self {
        Self { enabled: false }
    }

    /// Set whether the memory feature is enabled.
    pub fn with_enabled(mut self, enabled: bool) -> Self {
        self.enabled = enabled;
        self
    }
}

/// GitHub repository metadata to associate with a cloud session.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
#[non_exhaustive]
pub struct CloudSessionRepository {
    /// Repository owner.
    pub owner: String,
    /// Repository name.
    pub name: String,
    /// Optional branch name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
}

impl CloudSessionRepository {
    /// Create repository metadata for a cloud session.
    pub fn new(owner: impl Into<String>, name: impl Into<String>) -> Self {
        Self {
            owner: owner.into(),
            name: name.into(),
            branch: None,
        }
    }

    /// Set the branch associated with the repository.
    pub fn with_branch(mut self, branch: impl Into<String>) -> Self {
        self.branch = Some(branch.into());
        self
    }
}

/// Options for creating a remote session in the cloud.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
#[non_exhaustive]
pub struct CloudSessionOptions {
    /// Optional GitHub repository metadata to associate with the cloud session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<CloudSessionRepository>,
}

impl CloudSessionOptions {
    /// Create cloud session options with repository metadata.
    pub fn with_repository(repository: CloudSessionRepository) -> Self {
        Self {
            repository: Some(repository),
        }
    }
}

/// Stable extension identity for session participants that provide canvases.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionInfo {
    /// Extension namespace/source, e.g. `"github-app"`.
    pub source: String,
    /// Stable provider name within the source namespace.
    pub name: String,
}

impl ExtensionInfo {
    /// Create stable extension identity metadata.
    pub fn new(source: impl Into<String>, name: impl Into<String>) -> Self {
        Self {
            source: source.into(),
            name: name.into(),
        }
    }
}

/// Configuration for a single MCP server.
///
/// MCP (Model Context Protocol) servers expose external tools to the
/// agent. Local servers run as a subprocess over stdio; remote servers
/// speak HTTP or Server-Sent Events.
///
/// Serialized as a JSON object with a `type` discriminator (`"stdio"` |
/// `"http"` | `"sse"`).
///
/// # Example
///
/// ```
/// # use github_copilot_sdk::types::{McpServerConfig, McpStdioServerConfig, McpHttpServerConfig};
/// # use std::collections::HashMap;
/// let mut servers = HashMap::new();
/// servers.insert(
///     "playwright".to_string(),
///     McpServerConfig::Stdio(McpStdioServerConfig {
///         tools: Some(vec!["*".to_string()]),
///         command: "npx".to_string(),
///         args: vec!["-y".to_string(), "@playwright/mcp".to_string()],
///         ..Default::default()
///     }),
/// );
/// servers.insert(
///     "weather".to_string(),
///     McpServerConfig::Http(McpHttpServerConfig {
///         tools: Some(vec!["forecast".to_string()]),
///         url: "https://example.com/mcp".to_string(),
///         ..Default::default()
///     }),
/// );
/// ```
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type", rename_all = "lowercase")]
#[non_exhaustive]
pub enum McpServerConfig {
    /// Local MCP server launched as a subprocess and addressed over stdio.
    /// On the wire this serializes as `{"type": "stdio", ...}`. The CLI
    /// also accepts `"local"` as an alias on input.
    #[serde(alias = "local")]
    Stdio(McpStdioServerConfig),
    /// Remote MCP server addressed over HTTP.
    Http(McpHttpServerConfig),
    /// Remote MCP server addressed over Server-Sent Events.
    Sse(McpHttpServerConfig),
}

/// Configuration for a local/stdio MCP server.
///
/// See [`McpServerConfig::Stdio`].
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpStdioServerConfig {
    /// Tools to expose from this server.
    ///
    /// - `None` (field omitted on the wire) — expose **all** tools.
    /// - `Some(vec![])` — expose **no** tools.
    /// - `Some(vec!["a", ...])` — expose only the listed tools.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub tools: Option<Vec<String>>,
    /// Optional timeout in milliseconds for tool calls to this server.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub timeout: Option<i64>,
    /// Subprocess executable.
    pub command: String,
    /// Arguments to pass to the subprocess.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub args: Vec<String>,
    /// Environment variables to set on the subprocess. Values are passed
    /// through literally to the child process.
    #[serde(default, skip_serializing_if = "HashMap::is_empty")]
    pub env: HashMap<String, String>,
    /// Working directory for the subprocess.
    #[serde(default, skip_serializing_if = "Option::is_none", rename = "cwd")]
    pub working_directory: Option<String>,
}

/// Configuration for a remote MCP server (HTTP or SSE).
///
/// See [`McpServerConfig::Http`] and [`McpServerConfig::Sse`].
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpHttpServerConfig {
    /// Tools to expose from this server.
    ///
    /// - `None` (field omitted on the wire) — expose **all** tools.
    /// - `Some(vec![])` — expose **no** tools.
    /// - `Some(vec!["a", ...])` — expose only the listed tools.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub tools: Option<Vec<String>>,
    /// Optional timeout in milliseconds for tool calls to this server.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub timeout: Option<i64>,
    /// Server URL.
    pub url: String,
    /// Optional HTTP headers to include on every request.
    #[serde(default, skip_serializing_if = "HashMap::is_empty")]
    pub headers: HashMap<String, String>,
}

/// Configures a custom inference provider (BYOK — Bring Your Own Key).
///
/// Routes session requests through an alternative model provider
/// (OpenAI-compatible, Azure, Anthropic, or local) instead of GitHub
/// Copilot's default routing.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
#[non_exhaustive]
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
    /// Well-known model ID used to look up agent config and default token
    /// limits. Also used as the wire model when [`wire_model`](Self::wire_model)
    /// is unset. Falls back to [`SessionConfig::model`](crate::SessionConfig::model).
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
    /// Model name sent to the provider API for inference. Use this when
    /// the provider's model name (e.g. an Azure deployment name or a
    /// custom fine-tune name) differs from
    /// [`model_id`](Self::model_id). Falls back to
    /// [`model_id`](Self::model_id), then to
    /// [`SessionConfig::model`](crate::SessionConfig::model).
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub wire_model: Option<String>,
    /// Overrides the resolved model's default max prompt tokens. The
    /// runtime triggers conversation compaction before sending a request
    /// when the prompt (system message, history, tool definitions, user
    /// message) would exceed this limit.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub max_prompt_tokens: Option<i64>,
    /// Overrides the resolved model's default max output tokens. When
    /// hit, the model stops generating and returns a truncated response.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub max_output_tokens: Option<i64>,
}

impl ProviderConfig {
    /// Construct a [`ProviderConfig`] with the required `base_url` set;
    /// all other fields default to unset.
    pub fn new(base_url: impl Into<String>) -> Self {
        Self {
            base_url: base_url.into(),
            ..Self::default()
        }
    }

    /// Set the provider type (`"openai"`, `"azure"`, or `"anthropic"`).
    pub fn with_provider_type(mut self, provider_type: impl Into<String>) -> Self {
        self.provider_type = Some(provider_type.into());
        self
    }

    /// Set the API format (`"completions"` or `"responses"`; openai/azure only).
    pub fn with_wire_api(mut self, wire_api: impl Into<String>) -> Self {
        self.wire_api = Some(wire_api.into());
        self
    }

    /// Set the API key. Optional for local providers like Ollama.
    pub fn with_api_key(mut self, api_key: impl Into<String>) -> Self {
        self.api_key = Some(api_key.into());
        self
    }

    /// Set the bearer token used to populate the `Authorization` header.
    /// Takes precedence over `api_key` when both are set.
    pub fn with_bearer_token(mut self, bearer_token: impl Into<String>) -> Self {
        self.bearer_token = Some(bearer_token.into());
        self
    }

    /// Set Azure-specific options.
    pub fn with_azure(mut self, azure: AzureProviderOptions) -> Self {
        self.azure = Some(azure);
        self
    }

    /// Set the custom HTTP headers attached to outbound provider requests.
    pub fn with_headers(mut self, headers: HashMap<String, String>) -> Self {
        self.headers = Some(headers);
        self
    }

    /// Set the well-known model ID used to look up agent config and default
    /// token limits. Falls back to the session's configured model when unset.
    pub fn with_model_id(mut self, model_id: impl Into<String>) -> Self {
        self.model_id = Some(model_id.into());
        self
    }

    /// Set the model name sent to the provider API for inference. Use this
    /// when the provider's model name (e.g. an Azure deployment name or a
    /// custom fine-tune name) differs from
    /// [`model_id`](Self::model_id).
    pub fn with_wire_model(mut self, wire_model: impl Into<String>) -> Self {
        self.wire_model = Some(wire_model.into());
        self
    }

    /// Override the resolved model's default max prompt tokens. The
    /// runtime triggers conversation compaction when the prompt would
    /// exceed this limit.
    pub fn with_max_prompt_tokens(mut self, max: i64) -> Self {
        self.max_prompt_tokens = Some(max);
        self
    }

    /// Override the resolved model's default max output tokens. When
    /// hit, the model stops generating and returns a truncated response.
    pub fn with_max_output_tokens(mut self, max: i64) -> Self {
        self.max_output_tokens = Some(max);
        self
    }
}

/// Azure-specific provider options.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureProviderOptions {
    /// Azure API version. Defaults to `"2024-10-21"`.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub api_version: Option<String>,
}

/// A named BYOK provider connection in the multi-provider registry.
///
/// **Experimental.** Multi-provider BYOK configuration is part of an
/// experimental surface and may change or be removed in a future release.
///
/// Unlike [`ProviderConfig`], which routes the whole session through a
/// single provider, named providers are additive: the session keeps its
/// default Copilot routing and exposes these providers' models alongside
/// it. Models are attached via [`ProviderModelConfig`], which references a
/// provider by [`name`](Self::name).
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
#[non_exhaustive]
pub struct NamedProviderConfig {
    /// Unique name used by [`ProviderModelConfig::provider`] to reference
    /// this connection.
    pub name: String,
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
    /// directly. Takes precedence over `api_key` when both are set.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub bearer_token: Option<String>,
    /// Azure-specific options.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub azure: Option<AzureProviderOptions>,
    /// Custom HTTP headers included in outbound provider requests.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub headers: Option<HashMap<String, String>>,
}

impl NamedProviderConfig {
    /// Construct a [`NamedProviderConfig`] with the required `name` and
    /// `base_url` set; all other fields default to unset.
    pub fn new(name: impl Into<String>, base_url: impl Into<String>) -> Self {
        Self {
            name: name.into(),
            base_url: base_url.into(),
            ..Self::default()
        }
    }

    /// Set the provider type (`"openai"`, `"azure"`, or `"anthropic"`).
    pub fn with_provider_type(mut self, provider_type: impl Into<String>) -> Self {
        self.provider_type = Some(provider_type.into());
        self
    }

    /// Set the API format (`"completions"` or `"responses"`; openai/azure only).
    pub fn with_wire_api(mut self, wire_api: impl Into<String>) -> Self {
        self.wire_api = Some(wire_api.into());
        self
    }

    /// Set the API key. Optional for local providers like Ollama.
    pub fn with_api_key(mut self, api_key: impl Into<String>) -> Self {
        self.api_key = Some(api_key.into());
        self
    }

    /// Set the bearer token used to populate the `Authorization` header.
    /// Takes precedence over `api_key` when both are set.
    pub fn with_bearer_token(mut self, bearer_token: impl Into<String>) -> Self {
        self.bearer_token = Some(bearer_token.into());
        self
    }

    /// Set Azure-specific options.
    pub fn with_azure(mut self, azure: AzureProviderOptions) -> Self {
        self.azure = Some(azure);
        self
    }

    /// Set the custom HTTP headers attached to outbound provider requests.
    pub fn with_headers(mut self, headers: HashMap<String, String>) -> Self {
        self.headers = Some(headers);
        self
    }
}

/// A BYOK model definition in the multi-provider registry.
///
/// **Experimental.** Multi-provider BYOK configuration is part of an
/// experimental surface and may change or be removed in a future release.
///
/// References a [`NamedProviderConfig`] by [`provider`](Self::provider) and
/// becomes selectable under the provider-qualified id `provider/id`.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
#[non_exhaustive]
pub struct ProviderModelConfig {
    /// Model identifier, unique within its provider. Combined with
    /// [`provider`](Self::provider) to form the selection id `provider/id`.
    pub id: String,
    /// Name of the [`NamedProviderConfig`] this model is served by.
    pub provider: String,
    /// Model name sent to the provider API for inference. Use when the
    /// provider's model name differs from [`id`](Self::id).
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub wire_model: Option<String>,
    /// Well-known model ID used to look up agent config and default token
    /// limits.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
    /// Human-readable display name.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Overrides the resolved model's default max prompt tokens.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub max_prompt_tokens: Option<i64>,
    /// Overrides the resolved model's default max context window tokens.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub max_context_window_tokens: Option<i64>,
    /// Overrides the resolved model's default max output tokens.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub max_output_tokens: Option<i64>,
    /// Per-property overrides for model capabilities, deep-merged over
    /// runtime defaults.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub capabilities: Option<crate::generated::api_types::ModelCapabilitiesOverride>,
}

impl ProviderModelConfig {
    /// Construct a [`ProviderModelConfig`] with the required `id` and
    /// `provider` set; all other fields default to unset.
    pub fn new(id: impl Into<String>, provider: impl Into<String>) -> Self {
        Self {
            id: id.into(),
            provider: provider.into(),
            ..Self::default()
        }
    }

    /// Set the model name sent to the provider API for inference.
    pub fn with_wire_model(mut self, wire_model: impl Into<String>) -> Self {
        self.wire_model = Some(wire_model.into());
        self
    }

    /// Set the well-known model ID used to look up agent config and default
    /// token limits.
    pub fn with_model_id(mut self, model_id: impl Into<String>) -> Self {
        self.model_id = Some(model_id.into());
        self
    }

    /// Set the human-readable display name.
    pub fn with_name(mut self, name: impl Into<String>) -> Self {
        self.name = Some(name.into());
        self
    }

    /// Override the resolved model's default max prompt tokens.
    pub fn with_max_prompt_tokens(mut self, max: i64) -> Self {
        self.max_prompt_tokens = Some(max);
        self
    }

    /// Override the resolved model's default max context window tokens.
    pub fn with_max_context_window_tokens(mut self, max: i64) -> Self {
        self.max_context_window_tokens = Some(max);
        self
    }

    /// Override the resolved model's default max output tokens.
    pub fn with_max_output_tokens(mut self, max: i64) -> Self {
        self.max_output_tokens = Some(max);
        self
    }

    /// Set per-property model capability overrides.
    pub fn with_capabilities(
        mut self,
        capabilities: crate::generated::api_types::ModelCapabilitiesOverride,
    ) -> Self {
        self.capabilities = Some(capabilities);
        self
    }
}

/// Configuration for creating a new session via the `session.create` RPC.
///
/// All fields are optional — the CLI applies sensible defaults.
///
/// # Construction
///
/// Two equivalent shapes are supported:
///
/// 1. **Chained builder** (preferred for compile-time-known values):
///
///    ```
///    # use github_copilot_sdk::types::SessionConfig;
///    let cfg = SessionConfig::default()
///        .with_client_name("my-app")
///        .with_streaming(true)
///        .with_enable_config_discovery(true);
///    ```
///
/// 2. **Direct field assignment** (preferred when forwarding `Option<T>`
///    from upstream code, since `with_<field>` setters take the inner
///    `T`, not `Option<T>`):
///
///    ```
///    # use github_copilot_sdk::types::SessionConfig;
///    # let upstream_model: Option<String> = None;
///    # let upstream_system_message: Option<github_copilot_sdk::types::SystemMessageConfig> = None;
///    let mut cfg = SessionConfig::default()
///        .with_client_name("my-app")
///        .with_streaming(true);
///    cfg.model = upstream_model;
///    cfg.system_message = upstream_system_message;
///    ```
///
///    Mixing the two is fine: chain the fields you know at compile time,
///    then assign the `Option<T>` pass-through fields directly. All
///    fields on this struct are `pub`. This pattern matches the
///    `http::request::Parts` / `hyper::Body::Builder` convention in the
///    wider Rust ecosystem.
///
/// # Field naming across SDKs
///
/// Rust field names are snake_case (`available_tools`, `system_message`);
/// the wire protocol uses camelCase (`availableTools`, `systemMessage`).
/// The mapping happens inside `SessionConfig::into_wire` (crate-private),
/// which builds a separate `SessionCreateWire` payload. This config
/// struct is no longer itself serializable — the trait-object handler
/// fields (e.g. [`permission_handler`](Self::permission_handler)) could
/// never round-trip through serde, so the only legitimate serialization
/// path is now `into_wire`. When porting code from the TypeScript, Go,
/// Python, or .NET SDKs — or reading the raw JSON-RPC traces — fields
/// appear as `availableTools`, `systemMessage`, etc.
#[derive(Clone)]
#[non_exhaustive]
pub struct SessionConfig {
    /// Custom session ID. When unset, the CLI generates one.
    pub session_id: Option<SessionId>,
    /// Model to use (e.g. `"gpt-4"`, `"claude-sonnet-4"`).
    pub model: Option<String>,
    /// Application name sent as `User-Agent` context.
    pub client_name: Option<String>,
    /// Reasoning effort level (e.g. `"low"`, `"medium"`, `"high"`).
    pub reasoning_effort: Option<String>,
    /// Reasoning summary mode for models that support configurable
    /// reasoning summaries. Use [`ReasoningSummary::None`] to suppress
    /// summary output regardless of whether reasoning is enabled.
    pub reasoning_summary: Option<ReasoningSummary>,
    /// Context window tier for models that support it. Use `"long_context"`
    /// to pin the session to the long-context tier.
    pub context_tier: Option<String>,
    /// Enable streaming token deltas via `assistant.message_delta` events.
    pub streaming: Option<bool>,
    /// Custom system message configuration.
    pub system_message: Option<SystemMessageConfig>,
    /// Client-defined tool declarations to expose to the agent.
    pub tools: Option<Vec<Tool>>,
    /// Canvas declarations this connection provides to the runtime.
    pub canvases: Option<Vec<CanvasDeclaration>>,
    /// Provider-side canvas lifecycle handler. The SDK routes inbound
    /// `canvas.open` / `canvas.close` / `canvas.action.invoke` requests to
    /// this handler. Use [`with_canvas_handler`](Self::with_canvas_handler)
    /// to install one.
    pub canvas_handler: Option<Arc<dyn CanvasHandler>>,
    /// Request canvas renderer tools for this connection.
    pub request_canvas_renderer: Option<bool>,
    /// Request extension tools and dispatch for this connection.
    pub request_extensions: Option<bool>,
    /// Optional override path to a `copilot-sdk/` folder to inject into
    /// extension subprocesses for this session. Invalid paths fall back
    /// to the bundled SDK; takes precedence over the host's default.
    pub extension_sdk_path: Option<String>,
    /// Stable extension identity for canvas/tool providers on this connection.
    pub extension_info: Option<ExtensionInfo>,
    /// Allowlist of built-in tool names the agent may use.
    pub available_tools: Option<Vec<String>>,
    /// Blocklist of built-in tool names the agent must not use.
    pub excluded_tools: Option<Vec<String>>,
    /// MCP server configurations passed through to the CLI.
    pub mcp_servers: Option<HashMap<String, McpServerConfig>>,
    /// Controls how MCP OAuth tokens are stored for this session.
    ///
    /// - `"persistent"` — tokens are stored in the OS keychain (shared across sessions).
    /// - `"in-memory"` — tokens are stored in memory and discarded when the session ends.
    ///
    /// Defaults to `"in-memory"` when the client is in [`crate::ClientMode::Empty`],
    /// applied automatically at session creation/resume time. `None` means no
    /// explicit value is set and the runtime default takes effect.
    pub mcp_oauth_token_storage: Option<String>,
    /// When true, the CLI runs config discovery (MCP config files, skills, plugins).
    pub enable_config_discovery: Option<bool>,
    /// When true, skips embedding retrieval for this session.
    pub skip_embedding_retrieval: Option<bool>,
    /// Controls how the embedding cache is stored for this session.
    /// `"persistent"` caches on disk; `"in-memory"` discards when session ends.
    pub embedding_cache_storage: Option<String>,
    /// Organization-level custom instructions to apply to this session.
    pub organization_custom_instructions: Option<String>,
    /// When true, enables on-demand instruction discovery for this session.
    pub enable_on_demand_instruction_discovery: Option<bool>,
    /// When true, enables file hooks for this session.
    pub enable_file_hooks: Option<bool>,
    /// When true, allows host Git operations for this session.
    pub enable_host_git_operations: Option<bool>,
    /// When true, enables the session store for this session.
    pub enable_session_store: Option<bool>,
    /// When true, enables skills for this session.
    pub enable_skills: Option<bool>,
    /// **Experimental.** This option is part of an experimental wire-protocol
    /// surface (SEP-1865) and may change or be removed in a future release.
    ///
    /// Enable MCP Apps (SEP-1865) UI passthrough on this session.
    ///
    /// When `true` **and** the runtime has MCP Apps enabled (via the
    /// `MCP_APPS` feature flag or `COPILOT_MCP_APPS=true` environment
    /// override), the runtime adds the `mcp-apps` capability to the
    /// session, which causes it to advertise the
    /// `extensions.io.modelcontextprotocol/ui` extension to MCP servers (so
    /// they expose `_meta.ui.resourceUri` on tools) and to expose the
    /// `session.rpc.mcp.apps.{listTools,callTool,readResource,setHostContext,
    /// getHostContext,diagnose}` JSON-RPC methods.
    ///
    /// If the runtime gate is off, the opt-in is silently dropped
    /// server-side (the runtime logs a warning); the session is created
    /// normally but the MCP Apps surface is unavailable. Inspect the
    /// runtime's `capabilities.ui.mcpApps` on the create/resume response to
    /// detect this.
    ///
    /// SDK consumers MUST set this to `true` only when they have an iframe
    /// renderer that can display `ui://` MCP App bundles. Setting it
    /// without a renderer will cause MCP servers to register UI-enabled
    /// tool variants the consumer cannot display.
    ///
    /// Defaults to `None` (treated as `false`).
    pub enable_mcp_apps: Option<bool>,
    /// Skill directory paths passed through to the GitHub Copilot CLI.
    pub skill_directories: Option<Vec<PathBuf>>,
    /// Additional directories to search for custom instruction files.
    /// Forwarded to the CLI; not the same as [`skill_directories`](Self::skill_directories).
    pub instruction_directories: Option<Vec<PathBuf>>,
    /// Open Plugin directory paths passed through to the CLI.
    pub plugin_directories: Option<Vec<PathBuf>>,
    /// Configuration for large tool output handling, forwarded to the CLI.
    pub large_output: Option<LargeToolOutputConfig>,
    /// Skill names to disable. Skills in this set will not be available
    /// even if found in skill directories.
    pub disabled_skills: Option<Vec<String>>,
    /// Enable session hooks. When `true`, the CLI sends `hooks.invoke`
    /// RPC requests at key lifecycle points (pre/post tool use, prompt
    /// submission, session start/end, errors).
    pub hooks: Option<bool>,
    /// Custom agents (sub-agents) configured for this session.
    pub custom_agents: Option<Vec<CustomAgentConfig>>,
    /// Configures the built-in default agent. Use `excluded_tools` to
    /// hide tools from the default agent while keeping them available
    /// to custom sub-agents that reference them in their `tools` list.
    pub default_agent: Option<DefaultAgentConfig>,
    /// Name of the custom agent to activate when the session starts.
    /// Must match the `name` of one of the agents in [`Self::custom_agents`].
    pub agent: Option<String>,
    /// Configures infinite sessions: persistent workspace + automatic
    /// context-window compaction. Enabled by default on the CLI.
    pub infinite_sessions: Option<InfiniteSessionConfig>,
    /// Custom model provider (BYOK). When set, the session routes
    /// requests through this provider instead of the default Copilot
    /// routing.
    pub provider: Option<ProviderConfig>,
    /// **Experimental.** This field is part of an experimental multi-provider
    /// BYOK surface and may change or be removed in a future release.
    ///
    /// Named BYOK provider connections. Additive to the default Copilot
    /// routing — unlike [`provider`](Self::provider), these do not switch
    /// the whole session to BYOK. Referenced by [`models`](Self::models).
    pub providers: Option<Vec<NamedProviderConfig>>,
    /// **Experimental.** This field is part of an experimental multi-provider
    /// BYOK surface and may change or be removed in a future release.
    ///
    /// BYOK model definitions, each referencing a [`providers`](Self::providers)
    /// entry by name. Selectable under the id `provider/id`.
    pub models: Option<Vec<ProviderModelConfig>>,
    /// Enables or disables internal session telemetry for this session.
    ///
    /// When `Some(false)`, disables session telemetry. When `None` or
    /// `Some(true)`, telemetry is enabled for GitHub-authenticated sessions.
    /// When a custom [`provider`](Self::provider) is configured, session
    /// telemetry is always disabled regardless of this setting. This is
    /// independent of [`ClientOptions::telemetry`](crate::ClientOptions::telemetry).
    pub enable_session_telemetry: Option<bool>,
    /// Per-property overrides for model capabilities, deep-merged over
    /// runtime defaults.
    pub model_capabilities: Option<crate::generated::api_types::ModelCapabilitiesOverride>,
    /// Per-session configuration for the runtime memory feature.
    pub memory: Option<MemoryConfiguration>,
    /// Override the default configuration directory location. When set,
    /// the session uses this directory for storing config and state.
    pub config_directory: Option<PathBuf>,
    /// Working directory for the session. Tool operations resolve
    /// relative paths against this directory.
    pub working_directory: Option<PathBuf>,
    /// Per-session GitHub token. Distinct from
    /// [`ClientOptions::github_token`](crate::ClientOptions::github_token),
    /// which authenticates the CLI process itself; this token determines
    /// the GitHub identity used for content exclusion, model routing, and
    /// quota checks for *this session*.
    pub github_token: Option<String>,
    /// Per-session remote behavior control:
    /// - `Off` — local only, no remote export (default)
    /// - `Export` — export session events to GitHub without
    ///   enabling remote steering
    /// - `On` — export to GitHub AND enable remote steering
    pub remote_session: Option<crate::generated::api_types::RemoteSessionMode>,
    /// Creates a remote session in the cloud instead of a local session.
    /// The optional repository is associated with the cloud session.
    pub cloud: Option<CloudSessionOptions>,
    /// Forward sub-agent streaming events to this connection. When false,
    /// only non-streaming sub-agent events and `subagent.*` lifecycle events
    /// are delivered. Defaults to true on the CLI.
    pub include_sub_agent_streaming_events: Option<bool>,
    /// Slash commands registered for this session. When the CLI has a TUI,
    /// each command appears as `/name` for the user to invoke and the
    /// associated [`CommandHandler`] is called when executed.
    pub commands: Option<Vec<CommandDefinition>>,
    /// Custom session filesystem provider for this session. Required when
    /// the [`Client`](crate::Client) was started with
    /// [`ClientOptions::session_fs`](crate::ClientOptions::session_fs) set.
    /// See [`SessionFsProvider`].
    pub session_fs_provider: Option<Arc<dyn SessionFsProvider>>,
    /// Optional permission-request handler. When `None`, the SDK sends
    /// `requestPermission: false` on the wire so the runtime does not
    /// emit `permission.requested` broadcasts to this client.
    pub permission_handler: Option<Arc<dyn PermissionHandler>>,
    /// Optional elicitation-request handler. When `None`,
    /// `requestElicitation: false` goes on the wire.
    pub elicitation_handler: Option<Arc<dyn ElicitationHandler>>,
    /// Optional user-input handler. When `None`,
    /// `requestUserInput: false` goes on the wire and the `ask_user`
    /// tool is disabled.
    pub user_input_handler: Option<Arc<dyn UserInputHandler>>,
    /// Optional exit-plan-mode handler. When `None`,
    /// `requestExitPlanMode: false` goes on the wire.
    pub exit_plan_mode_handler: Option<Arc<dyn ExitPlanModeHandler>>,
    /// Optional auto-mode-switch handler. When `None`,
    /// `requestAutoModeSwitch: false` goes on the wire.
    pub auto_mode_switch_handler: Option<Arc<dyn AutoModeSwitchHandler>>,
    /// Session lifecycle hook handler (pre/post tool use, session
    /// start/end, etc.). When set, the SDK auto-enables the wire-level
    /// `hooks` flag. Use [`with_hooks`](Self::with_hooks) to install one.
    pub hooks_handler: Option<Arc<dyn SessionHooks>>,
    /// Permission policy applied to the handler. Stored separately from
    /// `permission_handler` so the order of `with_permission_handler` and
    /// `approve_all_permissions` (and friends) is irrelevant.
    pub(crate) permission_policy: Option<crate::permission::Policy>,
    /// System-message transform. When set, the SDK injects the matching
    /// `action: "transform"` sections into the system message and routes
    /// `systemMessage.transform` RPC callbacks to it during the session.
    /// Use [`with_system_message_transform`](Self::with_system_message_transform) to install one.
    pub system_message_transform: Option<Arc<dyn SystemMessageTransform>>,
    /// Whether to skip loading custom-instruction sources for this session.
    /// Applied via `session.options.update` after create/resume. Defaults to
    /// `true` in [`crate::ClientMode::Empty`] when unset.
    pub skip_custom_instructions: Option<bool>,
    /// Whether to constrain custom agents to local-only execution. Applied
    /// via `session.options.update` after create/resume. Defaults to `true`
    /// in [`crate::ClientMode::Empty`] when unset.
    pub custom_agents_local_only: Option<bool>,
    /// Whether to include the `Co-authored-by` trailer in commit messages.
    /// Applied via `session.options.update` after create/resume. Defaults to
    /// `false` in [`crate::ClientMode::Empty`] when unset.
    pub coauthor_enabled: Option<bool>,
    /// Whether to expose the `manage_schedule` tool. Applied via
    /// `session.options.update` after create/resume. Defaults to `false` in
    /// [`crate::ClientMode::Empty`] when unset.
    pub manage_schedule_enabled: Option<bool>,
}

impl std::fmt::Debug for SessionConfig {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("SessionConfig")
            .field("session_id", &self.session_id)
            .field("model", &self.model)
            .field("client_name", &self.client_name)
            .field("reasoning_effort", &self.reasoning_effort)
            .field("reasoning_summary", &self.reasoning_summary)
            .field("context_tier", &self.context_tier)
            .field("streaming", &self.streaming)
            .field("system_message", &self.system_message)
            .field("tools", &self.tools)
            .field("canvases", &self.canvases)
            .field(
                "canvas_handler",
                &self.canvas_handler.as_ref().map(|_| "<set>"),
            )
            .field("request_canvas_renderer", &self.request_canvas_renderer)
            .field("request_extensions", &self.request_extensions)
            .field("extension_sdk_path", &self.extension_sdk_path)
            .field("extension_info", &self.extension_info)
            .field("available_tools", &self.available_tools)
            .field("excluded_tools", &self.excluded_tools)
            .field("mcp_servers", &self.mcp_servers)
            .field("mcp_oauth_token_storage", &self.mcp_oauth_token_storage)
            .field("embedding_cache_storage", &self.embedding_cache_storage)
            .field("enable_config_discovery", &self.enable_config_discovery)
            .field("skip_embedding_retrieval", &self.skip_embedding_retrieval)
            .field(
                "organization_custom_instructions",
                &self
                    .organization_custom_instructions
                    .as_ref()
                    .map(|_| "<redacted>"),
            )
            .field(
                "enable_on_demand_instruction_discovery",
                &self.enable_on_demand_instruction_discovery,
            )
            .field("enable_file_hooks", &self.enable_file_hooks)
            .field(
                "enable_host_git_operations",
                &self.enable_host_git_operations,
            )
            .field("enable_session_store", &self.enable_session_store)
            .field("enable_skills", &self.enable_skills)
            .field("enable_mcp_apps", &self.enable_mcp_apps)
            .field("skill_directories", &self.skill_directories)
            .field("instruction_directories", &self.instruction_directories)
            .field("plugin_directories", &self.plugin_directories)
            .field("large_output", &self.large_output)
            .field("disabled_skills", &self.disabled_skills)
            .field("hooks", &self.hooks)
            .field("custom_agents", &self.custom_agents)
            .field("default_agent", &self.default_agent)
            .field("agent", &self.agent)
            .field("infinite_sessions", &self.infinite_sessions)
            .field("provider", &self.provider)
            .field("enable_session_telemetry", &self.enable_session_telemetry)
            .field("model_capabilities", &self.model_capabilities)
            .field("memory", &self.memory)
            .field("config_directory", &self.config_directory)
            .field("working_directory", &self.working_directory)
            .field(
                "github_token",
                &self.github_token.as_ref().map(|_| "<redacted>"),
            )
            .field("remote_session", &self.remote_session)
            .field("cloud", &self.cloud)
            .field(
                "include_sub_agent_streaming_events",
                &self.include_sub_agent_streaming_events,
            )
            .field("commands", &self.commands)
            .field(
                "session_fs_provider",
                &self.session_fs_provider.as_ref().map(|_| "<set>"),
            )
            .field(
                "permission_handler",
                &self.permission_handler.as_ref().map(|_| "<set>"),
            )
            .field(
                "elicitation_handler",
                &self.elicitation_handler.as_ref().map(|_| "<set>"),
            )
            .field(
                "user_input_handler",
                &self.user_input_handler.as_ref().map(|_| "<set>"),
            )
            .field(
                "exit_plan_mode_handler",
                &self.exit_plan_mode_handler.as_ref().map(|_| "<set>"),
            )
            .field(
                "auto_mode_switch_handler",
                &self.auto_mode_switch_handler.as_ref().map(|_| "<set>"),
            )
            .field(
                "hooks_handler",
                &self.hooks_handler.as_ref().map(|_| "<set>"),
            )
            .field(
                "system_message_transform",
                &self.system_message_transform.as_ref().map(|_| "<set>"),
            )
            .finish()
    }
}

impl Default for SessionConfig {
    /// All wire-level "request" flags and handler fields start unset.
    /// Install a [`PermissionHandler`] via
    /// [`with_permission_handler`](Self::with_permission_handler) and
    /// the SDK derives `requestPermission: true` on the wire at
    /// [`Client::create_session`](crate::Client::create_session) time.
    fn default() -> Self {
        Self {
            session_id: None,
            model: None,
            client_name: None,
            reasoning_effort: None,
            reasoning_summary: None,
            context_tier: None,
            streaming: None,
            system_message: None,
            tools: None,
            canvases: None,
            canvas_handler: None,
            request_canvas_renderer: None,
            request_extensions: None,
            extension_sdk_path: None,
            extension_info: None,
            available_tools: None,
            excluded_tools: None,
            mcp_servers: None,
            mcp_oauth_token_storage: None,
            enable_config_discovery: None,
            skip_embedding_retrieval: None,
            organization_custom_instructions: None,
            enable_on_demand_instruction_discovery: None,
            enable_file_hooks: None,
            enable_host_git_operations: None,
            enable_session_store: None,
            enable_skills: None,
            embedding_cache_storage: None,
            enable_mcp_apps: None,
            skill_directories: None,
            instruction_directories: None,
            plugin_directories: None,
            large_output: None,
            disabled_skills: None,
            hooks: None,
            custom_agents: None,
            default_agent: None,
            agent: None,
            infinite_sessions: None,
            provider: None,
            providers: None,
            models: None,
            enable_session_telemetry: None,
            model_capabilities: None,
            memory: None,
            config_directory: None,
            working_directory: None,
            github_token: None,
            remote_session: None,
            cloud: None,
            include_sub_agent_streaming_events: None,
            commands: None,
            session_fs_provider: None,
            permission_handler: None,
            elicitation_handler: None,
            user_input_handler: None,
            exit_plan_mode_handler: None,
            auto_mode_switch_handler: None,
            hooks_handler: None,
            permission_policy: None,
            system_message_transform: None,
            skip_custom_instructions: None,
            custom_agents_local_only: None,
            coauthor_enabled: None,
            manage_schedule_enabled: None,
        }
    }
}

/// Runtime-only bundle drained out of a [`SessionConfig`] or
/// [`ResumeSessionConfig`] by [`SessionConfig::into_wire`] /
/// [`ResumeSessionConfig::into_wire`]. Holds the trait-object handlers,
/// session-fs provider, and slash commands so the wire payload struct
/// stays a pure data shape.
pub(crate) struct SessionConfigRuntime {
    pub permission_handler: Option<Arc<dyn PermissionHandler>>,
    pub permission_policy: Option<crate::permission::Policy>,
    pub elicitation_handler: Option<Arc<dyn ElicitationHandler>>,
    pub user_input_handler: Option<Arc<dyn UserInputHandler>>,
    pub exit_plan_mode_handler: Option<Arc<dyn ExitPlanModeHandler>>,
    pub auto_mode_switch_handler: Option<Arc<dyn AutoModeSwitchHandler>>,
    pub hooks_handler: Option<Arc<dyn SessionHooks>>,
    pub system_message_transform: Option<Arc<dyn SystemMessageTransform>>,
    pub tool_handlers: HashMap<String, Arc<dyn crate::tool::ToolHandler>>,
    pub canvas_handler: Option<Arc<dyn CanvasHandler>>,
    pub session_fs_provider: Option<Arc<dyn SessionFsProvider>>,
    pub commands: Option<Vec<CommandDefinition>>,
}

impl SessionConfig {
    /// Consume this config to produce the [`SessionCreateWire`] payload
    /// for `session.create` and a [`SessionConfigRuntime`] bundle holding
    /// the runtime-only fields (handlers, transforms, providers).
    ///
    /// Wire-format flags are derived from handler presence and the policy
    /// field; runtime fields are moved out into the returned runtime so
    /// the deep `Vec<Tool>` / `HashMap<String, Value>` clones the previous
    /// `&self`-based shape required are eliminated, and the order of
    /// reading-vs-moving is enforced at compile time.
    ///
    /// [`SessionCreateWire`]: crate::wire::SessionCreateWire
    pub(crate) fn into_wire(
        mut self,
        session_id: Option<SessionId>,
    ) -> Result<(crate::wire::SessionCreateWire, SessionConfigRuntime), crate::Error> {
        let permission_active =
            self.permission_handler.is_some() || self.permission_policy.is_some();
        let request_user_input = self.user_input_handler.is_some();
        let request_exit_plan_mode = self.exit_plan_mode_handler.is_some();
        let request_auto_mode_switch = self.auto_mode_switch_handler.is_some();
        let request_elicitation = self.elicitation_handler.is_some();
        let hooks_flag = self.hooks_handler.is_some();

        let mut tool_handlers: HashMap<String, Arc<dyn crate::tool::ToolHandler>> = HashMap::new();
        if let Some(tools) = self.tools.as_mut() {
            for tool in tools.iter_mut() {
                if let Some(handler) = tool.handler.take()
                    && tool_handlers.insert(tool.name.clone(), handler).is_some()
                {
                    return Err(crate::Error::with_message(
                        crate::ErrorKind::InvalidConfig,
                        format!("duplicate tool handler registered for name {:?}", tool.name),
                    ));
                }
            }
        }

        let wire_commands = self.commands.as_ref().map(|cmds| {
            cmds.iter()
                .map(|c| crate::wire::CommandWireDefinition {
                    name: c.name.clone(),
                    description: c.description.clone(),
                })
                .collect()
        });
        let wire_canvases = self.canvases.clone();
        let canvas_handler = self.canvas_handler.clone();

        let wire = crate::wire::SessionCreateWire {
            session_id,
            model: self.model,
            client_name: self.client_name,
            reasoning_effort: self.reasoning_effort,
            reasoning_summary: self.reasoning_summary,
            context_tier: self.context_tier,
            streaming: self.streaming,
            system_message: self.system_message,
            tools: self.tools,
            canvases: wire_canvases,
            request_canvas_renderer: self.request_canvas_renderer,
            request_extensions: self.request_extensions,
            extension_sdk_path: self.extension_sdk_path,
            extension_info: self.extension_info,
            available_tools: self.available_tools,
            excluded_tools: self.excluded_tools,
            tool_filter_precedence: "excluded",
            mcp_servers: self.mcp_servers,
            mcp_oauth_token_storage: self.mcp_oauth_token_storage,
            embedding_cache_storage: self.embedding_cache_storage,
            env_value_mode: "direct",
            enable_config_discovery: self.enable_config_discovery,
            skip_embedding_retrieval: self.skip_embedding_retrieval,
            organization_custom_instructions: self.organization_custom_instructions,
            enable_on_demand_instruction_discovery: self.enable_on_demand_instruction_discovery,
            enable_file_hooks: self.enable_file_hooks,
            enable_host_git_operations: self.enable_host_git_operations,
            enable_session_store: self.enable_session_store,
            enable_skills: self.enable_skills,
            request_user_input,
            request_permission: permission_active,
            request_exit_plan_mode,
            request_auto_mode_switch,
            request_elicitation,
            request_mcp_apps: self.enable_mcp_apps.unwrap_or(false),
            hooks: hooks_flag,
            skill_directories: self.skill_directories,
            instruction_directories: self.instruction_directories,
            plugin_directories: self.plugin_directories,
            large_output: self.large_output,
            disabled_skills: self.disabled_skills,
            custom_agents: self.custom_agents,
            default_agent: self.default_agent,
            agent: self.agent,
            infinite_sessions: self.infinite_sessions,
            provider: self.provider,
            providers: self.providers,
            models: self.models,
            enable_session_telemetry: self.enable_session_telemetry,
            model_capabilities: self.model_capabilities,
            memory: self.memory,
            config_dir: self.config_directory,
            working_directory: self.working_directory,
            github_token: self.github_token,
            remote_session: self.remote_session,
            cloud: self.cloud,
            include_sub_agent_streaming_events: self.include_sub_agent_streaming_events,
            commands: wire_commands,
        };

        let runtime = SessionConfigRuntime {
            permission_handler: self.permission_handler,
            permission_policy: self.permission_policy,
            elicitation_handler: self.elicitation_handler,
            user_input_handler: self.user_input_handler,
            exit_plan_mode_handler: self.exit_plan_mode_handler,
            auto_mode_switch_handler: self.auto_mode_switch_handler,
            hooks_handler: self.hooks_handler,
            system_message_transform: self.system_message_transform,
            tool_handlers,
            canvas_handler,
            session_fs_provider: self.session_fs_provider,
            commands: self.commands,
        };

        Ok((wire, runtime))
    }

    /// Install a [`PermissionHandler`] for this session. When omitted, the
    /// SDK sends `requestPermission: false` on the wire and the runtime
    /// short-circuits permission prompts for this client.
    pub fn with_permission_handler(mut self, handler: Arc<dyn PermissionHandler>) -> Self {
        self.permission_handler = Some(handler);
        self
    }

    /// Install an [`ElicitationHandler`]. When omitted, the SDK sends
    /// `requestElicitation: false` on the wire.
    pub fn with_elicitation_handler(mut self, handler: Arc<dyn ElicitationHandler>) -> Self {
        self.elicitation_handler = Some(handler);
        self
    }

    /// Install a [`UserInputHandler`]. Required for the `ask_user` tool
    /// to be enabled.
    pub fn with_user_input_handler(mut self, handler: Arc<dyn UserInputHandler>) -> Self {
        self.user_input_handler = Some(handler);
        self
    }

    /// Install an [`ExitPlanModeHandler`].
    pub fn with_exit_plan_mode_handler(mut self, handler: Arc<dyn ExitPlanModeHandler>) -> Self {
        self.exit_plan_mode_handler = Some(handler);
        self
    }

    /// Install an [`AutoModeSwitchHandler`].
    pub fn with_auto_mode_switch_handler(
        mut self,
        handler: Arc<dyn AutoModeSwitchHandler>,
    ) -> Self {
        self.auto_mode_switch_handler = Some(handler);
        self
    }

    /// Register slash commands for this session. Each command appears as
    /// `/name` in the CLI's TUI; the handler is invoked when the user
    /// executes the command. Replaces any commands previously set on this
    /// config. See [`CommandDefinition`].
    pub fn with_commands(mut self, commands: Vec<CommandDefinition>) -> Self {
        self.commands = Some(commands);
        self
    }

    /// Install a [`SessionFsProvider`] backing the session's filesystem.
    /// Required when the [`Client`](crate::Client) was started with
    /// [`ClientOptions::session_fs`](crate::ClientOptions::session_fs).
    pub fn with_session_fs_provider(mut self, provider: Arc<dyn SessionFsProvider>) -> Self {
        self.session_fs_provider = Some(provider);
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
    pub fn with_system_message_transform(
        mut self,
        transform: Arc<dyn SystemMessageTransform>,
    ) -> Self {
        self.system_message_transform = Some(transform);
        self
    }

    /// Auto-approve every permission request on this session. Stored as a
    /// policy that's applied at
    /// [`Client::create_session`](crate::Client::create_session) time, so
    /// order with [`with_permission_handler`](Self::with_permission_handler)
    /// is irrelevant.
    pub fn approve_all_permissions(mut self) -> Self {
        self.permission_policy = Some(crate::permission::Policy::ApproveAll);
        self
    }

    /// Auto-deny every permission request on this session. See
    /// [`approve_all_permissions`](Self::approve_all_permissions).
    pub fn deny_all_permissions(mut self) -> Self {
        self.permission_policy = Some(crate::permission::Policy::DenyAll);
        self
    }

    /// Apply a closure-based permission policy: `predicate` returns `true`
    /// to approve, `false` to deny. See
    /// [`approve_all_permissions`](Self::approve_all_permissions) for
    /// ordering semantics.
    pub fn approve_permissions_if<F>(mut self, predicate: F) -> Self
    where
        F: Fn(&crate::types::PermissionRequestData) -> bool + Send + Sync + 'static,
    {
        self.permission_policy = Some(crate::permission::Policy::Predicate(Arc::new(predicate)));
        self
    }

    /// Set a custom session ID (when unset, the CLI generates one).
    pub fn with_session_id(mut self, id: impl Into<SessionId>) -> Self {
        self.session_id = Some(id.into());
        self
    }

    /// Set the model identifier (e.g. `"claude-sonnet-4"`).
    pub fn with_model(mut self, model: impl Into<String>) -> Self {
        self.model = Some(model.into());
        self
    }

    /// Set the application name sent as `User-Agent` context.
    pub fn with_client_name(mut self, name: impl Into<String>) -> Self {
        self.client_name = Some(name.into());
        self
    }

    /// Set the reasoning effort level (e.g. `"low"`, `"medium"`, `"high"`).
    pub fn with_reasoning_effort(mut self, effort: impl Into<String>) -> Self {
        self.reasoning_effort = Some(effort.into());
        self
    }

    /// Set [`reasoning_summary`](Self::reasoning_summary).
    pub fn with_reasoning_summary(mut self, summary: ReasoningSummary) -> Self {
        self.reasoning_summary = Some(summary);
        self
    }

    /// Set the context window tier (e.g. `"default"`, `"long_context"`).
    pub fn with_context_tier(mut self, tier: impl Into<String>) -> Self {
        self.context_tier = Some(tier.into());
        self
    }

    /// Enable streaming token deltas via `assistant.message_delta` events.
    pub fn with_streaming(mut self, streaming: bool) -> Self {
        self.streaming = Some(streaming);
        self
    }

    /// Set a custom system message configuration.
    pub fn with_system_message(mut self, system_message: SystemMessageConfig) -> Self {
        self.system_message = Some(system_message);
        self
    }

    /// Set the client-defined tools to expose to the agent.
    pub fn with_tools<I: IntoIterator<Item = Tool>>(mut self, tools: I) -> Self {
        self.tools = Some(tools.into_iter().collect());
        self
    }

    /// Set canvas declarations for this connection. The runtime advertises
    /// these to the agent; install a [`CanvasHandler`] via
    /// [`with_canvas_handler`](Self::with_canvas_handler) to receive the
    /// resulting provider callbacks.
    pub fn with_canvases<I: IntoIterator<Item = CanvasDeclaration>>(mut self, canvases: I) -> Self {
        self.canvases = Some(canvases.into_iter().collect());
        self
    }

    /// Install the provider-side [`CanvasHandler`] for this session.
    pub fn with_canvas_handler(mut self, handler: Arc<dyn CanvasHandler>) -> Self {
        self.canvas_handler = Some(handler);
        self
    }

    /// Request host canvas renderer tools for this connection.
    pub fn with_request_canvas_renderer(mut self, request: bool) -> Self {
        self.request_canvas_renderer = Some(request);
        self
    }

    /// Request extension tools and dispatch for this connection.
    pub fn with_request_extensions(mut self, request: bool) -> Self {
        self.request_extensions = Some(request);
        self
    }

    /// Override the bundled `@github/copilot-sdk` drop injected into extension
    /// subprocesses for this session. Invalid paths fall back to the bundled
    /// SDK silently.
    pub fn with_extension_sdk_path(mut self, path: impl Into<String>) -> Self {
        self.extension_sdk_path = Some(path.into());
        self
    }

    /// Set stable extension identity metadata for this connection.
    pub fn with_extension_info(mut self, extension_info: ExtensionInfo) -> Self {
        self.extension_info = Some(extension_info);
        self
    }

    /// Set the allowlist of built-in tool names the agent may use.
    pub fn with_available_tools<I, S>(mut self, tools: I) -> Self
    where
        I: IntoIterator<Item = S>,
        S: Into<String>,
    {
        self.available_tools = Some(tools.into_iter().map(Into::into).collect());
        self
    }

    /// Set the blocklist of built-in tool names the agent must not use.
    pub fn with_excluded_tools<I, S>(mut self, tools: I) -> Self
    where
        I: IntoIterator<Item = S>,
        S: Into<String>,
    {
        self.excluded_tools = Some(tools.into_iter().map(Into::into).collect());
        self
    }

    /// Set MCP server configurations passed through to the CLI.
    pub fn with_mcp_servers(mut self, servers: HashMap<String, McpServerConfig>) -> Self {
        self.mcp_servers = Some(servers);
        self
    }

    /// Set MCP OAuth token storage mode.
    ///
    /// - `"persistent"` — tokens stored in the OS keychain.
    /// - `"in-memory"` — tokens discarded when the session ends.
    ///
    /// Defaults to `"in-memory"` when the client is in [`crate::ClientMode::Empty`],
    /// applied automatically at session creation/resume time.
    pub fn with_mcp_oauth_token_storage(mut self, mode: impl Into<String>) -> Self {
        self.mcp_oauth_token_storage = Some(mode.into());
        self
    }

    /// Set embedding cache storage mode.
    pub fn with_embedding_cache_storage(
        mut self,
        embedding_cache_storage: impl Into<String>,
    ) -> Self {
        self.embedding_cache_storage = Some(embedding_cache_storage.into());
        self
    }

    /// Enable or disable CLI config discovery (MCP config files, skills, plugins).
    pub fn with_enable_config_discovery(mut self, enable: bool) -> Self {
        self.enable_config_discovery = Some(enable);
        self
    }

    /// Set [`Self::skip_embedding_retrieval`].
    pub fn with_skip_embedding_retrieval(mut self, value: bool) -> Self {
        self.skip_embedding_retrieval = Some(value);
        self
    }

    /// Set [`Self::organization_custom_instructions`].
    pub fn with_organization_custom_instructions(
        mut self,
        instructions: impl Into<String>,
    ) -> Self {
        self.organization_custom_instructions = Some(instructions.into());
        self
    }

    /// Set [`Self::enable_on_demand_instruction_discovery`].
    pub fn with_enable_on_demand_instruction_discovery(mut self, value: bool) -> Self {
        self.enable_on_demand_instruction_discovery = Some(value);
        self
    }

    /// Set [`Self::enable_file_hooks`].
    pub fn with_enable_file_hooks(mut self, value: bool) -> Self {
        self.enable_file_hooks = Some(value);
        self
    }

    /// Set [`Self::enable_host_git_operations`].
    pub fn with_enable_host_git_operations(mut self, value: bool) -> Self {
        self.enable_host_git_operations = Some(value);
        self
    }

    /// Set [`Self::enable_session_store`].
    pub fn with_enable_session_store(mut self, value: bool) -> Self {
        self.enable_session_store = Some(value);
        self
    }

    /// Set [`Self::enable_skills`].
    pub fn with_enable_skills(mut self, value: bool) -> Self {
        self.enable_skills = Some(value);
        self
    }

    /// **Experimental.** This method is part of an experimental wire-protocol
    /// surface (SEP-1865) and may change or be removed in a future release.
    ///
    /// Enable MCP Apps (SEP-1865) UI passthrough on this session. Defaults
    /// to `None` (treated as `false`). See [`SessionConfig::enable_mcp_apps`].
    pub fn with_enable_mcp_apps(mut self, enable: bool) -> Self {
        self.enable_mcp_apps = Some(enable);
        self
    }

    /// Set skill directory paths passed through to the CLI.
    pub fn with_skill_directories<I, P>(mut self, paths: I) -> Self
    where
        I: IntoIterator<Item = P>,
        P: Into<PathBuf>,
    {
        self.skill_directories = Some(paths.into_iter().map(Into::into).collect());
        self
    }

    /// Set additional directories to search for custom instruction files.
    /// Forwarded to the CLI on session create; not the same as
    /// [`with_skill_directories`](Self::with_skill_directories).
    pub fn with_instruction_directories<I, P>(mut self, paths: I) -> Self
    where
        I: IntoIterator<Item = P>,
        P: Into<PathBuf>,
    {
        self.instruction_directories = Some(paths.into_iter().map(Into::into).collect());
        self
    }

    /// Set Open Plugin directory paths passed through to the CLI on session create.
    pub fn with_plugin_directories<I, P>(mut self, paths: I) -> Self
    where
        I: IntoIterator<Item = P>,
        P: Into<PathBuf>,
    {
        self.plugin_directories = Some(paths.into_iter().map(Into::into).collect());
        self
    }

    /// Set the [`LargeToolOutputConfig`] forwarded to the CLI on session create.
    pub fn with_large_output(mut self, config: LargeToolOutputConfig) -> Self {
        self.large_output = Some(config);
        self
    }

    /// Set the names of skills to disable (overrides skill discovery).
    pub fn with_disabled_skills<I, S>(mut self, names: I) -> Self
    where
        I: IntoIterator<Item = S>,
        S: Into<String>,
    {
        self.disabled_skills = Some(names.into_iter().map(Into::into).collect());
        self
    }

    /// Set the custom agents (sub-agents) configured for this session.
    pub fn with_custom_agents<I: IntoIterator<Item = CustomAgentConfig>>(
        mut self,
        agents: I,
    ) -> Self {
        self.custom_agents = Some(agents.into_iter().collect());
        self
    }

    /// Configure the built-in default agent.
    pub fn with_default_agent(mut self, agent: DefaultAgentConfig) -> Self {
        self.default_agent = Some(agent);
        self
    }

    /// Activate a named custom agent on session start. Must match the
    /// `name` of one of the agents in [`Self::custom_agents`].
    pub fn with_agent(mut self, name: impl Into<String>) -> Self {
        self.agent = Some(name.into());
        self
    }

    /// Configure infinite sessions (persistent workspace + automatic
    /// context-window compaction).
    pub fn with_infinite_sessions(mut self, config: InfiniteSessionConfig) -> Self {
        self.infinite_sessions = Some(config);
        self
    }

    /// Configure a custom model provider (BYOK).
    pub fn with_provider(mut self, provider: ProviderConfig) -> Self {
        self.provider = Some(provider);
        self
    }

    /// **Experimental.** This method is part of an experimental multi-provider
    /// BYOK surface and may change or be removed in a future release.
    ///
    /// Set the named BYOK provider connections (additive multi-provider
    /// registry). Attach models referencing these with [`Self::with_models`].
    pub fn with_providers(mut self, providers: Vec<NamedProviderConfig>) -> Self {
        self.providers = Some(providers);
        self
    }

    /// **Experimental.** This method is part of an experimental multi-provider
    /// BYOK surface and may change or be removed in a future release.
    ///
    /// Set the BYOK model definitions, each referencing a named provider
    /// supplied via [`Self::with_providers`].
    pub fn with_models(mut self, models: Vec<ProviderModelConfig>) -> Self {
        self.models = Some(models);
        self
    }

    /// Enable or disable internal session telemetry.
    ///
    /// See [`Self::enable_session_telemetry`] for default and BYOK behavior.
    pub fn with_enable_session_telemetry(mut self, enable: bool) -> Self {
        self.enable_session_telemetry = Some(enable);
        self
    }

    /// Set per-property overrides for model capabilities.
    pub fn with_model_capabilities(
        mut self,
        capabilities: crate::generated::api_types::ModelCapabilitiesOverride,
    ) -> Self {
        self.model_capabilities = Some(capabilities);
        self
    }

    /// Configure the runtime memory feature for this session.
    pub fn with_memory(mut self, memory: MemoryConfiguration) -> Self {
        self.memory = Some(memory);
        self
    }

    /// Override the default configuration directory location.
    pub fn with_config_directory(mut self, dir: impl Into<PathBuf>) -> Self {
        self.config_directory = Some(dir.into());
        self
    }

    /// Set the per-session working directory. Tool operations resolve
    /// relative paths against this directory.
    pub fn with_working_directory(mut self, dir: impl Into<PathBuf>) -> Self {
        self.working_directory = Some(dir.into());
        self
    }

    /// Set the per-session GitHub token. Distinct from
    /// [`ClientOptions::github_token`](crate::ClientOptions::github_token);
    /// this token determines the GitHub identity used for content exclusion,
    /// model routing, and quota checks for this session only.
    pub fn with_github_token(mut self, token: impl Into<String>) -> Self {
        self.github_token = Some(token.into());
        self
    }

    /// Forward sub-agent streaming events to this connection. Defaults
    /// to true on the CLI when unset.
    pub fn with_include_sub_agent_streaming_events(mut self, include: bool) -> Self {
        self.include_sub_agent_streaming_events = Some(include);
        self
    }

    /// Set per-session remote behavior.
    pub fn with_remote_session(
        mut self,
        mode: crate::generated::api_types::RemoteSessionMode,
    ) -> Self {
        self.remote_session = Some(mode);
        self
    }

    /// Create a remote session in the cloud instead of a local session.
    pub fn with_cloud(mut self, cloud: CloudSessionOptions) -> Self {
        self.cloud = Some(cloud);
        self
    }

    /// Set [`Self::skip_custom_instructions`].
    pub fn with_skip_custom_instructions(mut self, value: bool) -> Self {
        self.skip_custom_instructions = Some(value);
        self
    }

    /// Set [`Self::custom_agents_local_only`].
    pub fn with_custom_agents_local_only(mut self, value: bool) -> Self {
        self.custom_agents_local_only = Some(value);
        self
    }

    /// Set [`Self::coauthor_enabled`].
    pub fn with_coauthor_enabled(mut self, value: bool) -> Self {
        self.coauthor_enabled = Some(value);
        self
    }

    /// Set [`Self::manage_schedule_enabled`].
    pub fn with_manage_schedule_enabled(mut self, value: bool) -> Self {
        self.manage_schedule_enabled = Some(value);
        self
    }
}

/// Configuration for resuming an existing session via the `session.resume` RPC.
///
/// See [`SessionConfig`] for the construction patterns (chained `with_*`
/// builder vs. direct field assignment for `Option<T>` pass-through) and
/// the note on snake_case vs. camelCase field naming. This config is not
/// itself serializable — call `ResumeSessionConfig::into_wire`
/// (crate-private) to produce the wire payload.
#[derive(Clone)]
#[non_exhaustive]
pub struct ResumeSessionConfig {
    /// ID of the session to resume.
    pub session_id: SessionId,
    /// Application name sent as User-Agent context.
    pub client_name: Option<String>,
    /// Desired reasoning effort to apply after resuming the session.
    pub reasoning_effort: Option<String>,
    /// Reasoning summary mode to apply after resuming the session. Use
    /// [`ReasoningSummary::None`] to suppress summary output regardless of
    /// whether reasoning is enabled.
    pub reasoning_summary: Option<ReasoningSummary>,
    /// Context window tier to apply after resuming the session. Use
    /// `"long_context"` to pin the session to the long-context tier.
    pub context_tier: Option<String>,
    /// Enable streaming token deltas.
    pub streaming: Option<bool>,
    /// Re-supply the system message so the agent retains workspace context
    /// across CLI process restarts.
    pub system_message: Option<SystemMessageConfig>,
    /// Client-defined tool declarations to re-supply on resume.
    pub tools: Option<Vec<Tool>>,
    /// Canvas declarations this connection provides to the runtime.
    pub canvases: Option<Vec<CanvasDeclaration>>,
    /// Provider-side canvas lifecycle handler. See
    /// [`SessionConfig::canvas_handler`].
    pub canvas_handler: Option<Arc<dyn CanvasHandler>>,
    /// Open canvas instances the caller knows were open before this resume.
    pub open_canvases: Option<Vec<OpenCanvasInstance>>,
    /// Request canvas renderer tools for this connection.
    pub request_canvas_renderer: Option<bool>,
    /// Request extension tools and dispatch for this connection.
    pub request_extensions: Option<bool>,
    /// Optional override path to a `copilot-sdk/` folder to inject into
    /// extension subprocesses for this session on resume. See
    /// `SessionConfig::extension_sdk_path`.
    pub extension_sdk_path: Option<String>,
    /// Stable extension identity for canvas/tool providers on this connection.
    pub extension_info: Option<ExtensionInfo>,
    /// Allowlist of tool names the agent may use.
    pub available_tools: Option<Vec<String>>,
    /// Blocklist of built-in tool names.
    pub excluded_tools: Option<Vec<String>>,
    /// Re-supply MCP servers so they remain available after app restart.
    pub mcp_servers: Option<HashMap<String, McpServerConfig>>,
    /// Controls how MCP OAuth tokens are stored for this session.
    /// See [`SessionConfig::mcp_oauth_token_storage`] for details.
    pub mcp_oauth_token_storage: Option<String>,
    /// Enable config discovery on resume.
    pub enable_config_discovery: Option<bool>,
    /// When true, skips embedding retrieval on resume.
    pub skip_embedding_retrieval: Option<bool>,
    /// Controls how the embedding cache is stored for this session.
    pub embedding_cache_storage: Option<String>,
    /// Organization-level custom instructions to apply on resume.
    pub organization_custom_instructions: Option<String>,
    /// When true, enables on-demand instruction discovery on resume.
    pub enable_on_demand_instruction_discovery: Option<bool>,
    /// When true, enables file hooks on resume.
    pub enable_file_hooks: Option<bool>,
    /// When true, allows host Git operations on resume.
    pub enable_host_git_operations: Option<bool>,
    /// When true, enables the session store on resume.
    pub enable_session_store: Option<bool>,
    /// When true, enables skills on resume.
    pub enable_skills: Option<bool>,
    /// **Experimental.** This option is part of an experimental wire-protocol
    /// surface (SEP-1865) and may change or be removed in a future release.
    ///
    /// Enable MCP Apps (SEP-1865) UI passthrough on resume. See
    /// [`SessionConfig::enable_mcp_apps`]. Defaults to `None` (treated as `false`).
    pub enable_mcp_apps: Option<bool>,
    /// Skill directory paths passed through to the GitHub Copilot CLI on resume.
    pub skill_directories: Option<Vec<PathBuf>>,
    /// Additional directories to search for custom instruction files on
    /// resume. Forwarded to the CLI; not the same as [`skill_directories`](Self::skill_directories).
    pub instruction_directories: Option<Vec<PathBuf>>,
    /// Open Plugin directory paths passed through to the CLI on resume.
    pub plugin_directories: Option<Vec<PathBuf>>,
    /// Configuration for large tool output handling, forwarded to the CLI on resume.
    pub large_output: Option<LargeToolOutputConfig>,
    /// Skill names to disable on resume.
    pub disabled_skills: Option<Vec<String>>,
    /// Enable session hooks on resume.
    pub hooks: Option<bool>,
    /// Custom agents to re-supply on resume.
    pub custom_agents: Option<Vec<CustomAgentConfig>>,
    /// Configures the built-in default agent on resume.
    pub default_agent: Option<DefaultAgentConfig>,
    /// Name of the custom agent to activate.
    pub agent: Option<String>,
    /// Re-supply infinite session configuration on resume.
    pub infinite_sessions: Option<InfiniteSessionConfig>,
    /// Re-supply BYOK provider configuration on resume.
    pub provider: Option<ProviderConfig>,
    /// **Experimental.** This field is part of an experimental multi-provider
    /// BYOK surface and may change or be removed in a future release.
    ///
    /// Re-supply named BYOK provider connections on resume. Additive to
    /// the default Copilot routing. Referenced by [`models`](Self::models).
    pub providers: Option<Vec<NamedProviderConfig>>,
    /// **Experimental.** This field is part of an experimental multi-provider
    /// BYOK surface and may change or be removed in a future release.
    ///
    /// Re-supply BYOK model definitions on resume, each referencing a
    /// [`providers`](Self::providers) entry by name.
    pub models: Option<Vec<ProviderModelConfig>>,
    /// Enables or disables internal session telemetry for this session.
    ///
    /// When `Some(false)`, disables session telemetry. When `None` or
    /// `Some(true)`, telemetry is enabled for GitHub-authenticated sessions.
    /// When a custom [`provider`](Self::provider) is configured, session
    /// telemetry is always disabled regardless of this setting. This is
    /// independent of [`ClientOptions::telemetry`](crate::ClientOptions::telemetry).
    pub enable_session_telemetry: Option<bool>,
    /// Per-property model capability overrides on resume.
    pub model_capabilities: Option<crate::generated::api_types::ModelCapabilitiesOverride>,
    /// Per-session configuration for the runtime memory feature on resume.
    pub memory: Option<MemoryConfiguration>,
    /// Override the default configuration directory location on resume.
    pub config_directory: Option<PathBuf>,
    /// Per-session working directory on resume.
    pub working_directory: Option<PathBuf>,
    /// Per-session GitHub token on resume. See
    /// [`SessionConfig::github_token`].
    pub github_token: Option<String>,
    /// Per-session remote behavior control on resume. See
    /// [`SessionConfig::remote_session`].
    pub remote_session: Option<crate::generated::api_types::RemoteSessionMode>,
    /// Forward sub-agent streaming events to this connection on resume.
    pub include_sub_agent_streaming_events: Option<bool>,
    /// Slash commands registered for this session on resume. See
    /// [`SessionConfig::commands`] — commands are not persisted server-side,
    /// so the resume payload re-supplies the registration.
    pub commands: Option<Vec<CommandDefinition>>,
    /// Custom session filesystem provider. Required on resume when the
    /// [`Client`](crate::Client) was started with
    /// [`ClientOptions::session_fs`](crate::ClientOptions::session_fs).
    /// See [`SessionConfig::session_fs_provider`].
    pub session_fs_provider: Option<Arc<dyn SessionFsProvider>>,
    /// Force-fail resume if the session does not exist on disk, instead of
    /// silently starting a new session. Wire field name stays `disableResume`.
    pub suppress_resume_event: Option<bool>,
    /// When `true`, instructs the runtime to continue any tool calls or
    /// permission requests that were pending when the previous connection
    /// was dropped. Use this together with [`Client::force_stop`] to hand
    /// off a session from one process to another without losing in-flight
    /// work.
    ///
    /// [`Client::force_stop`]: crate::Client::force_stop
    pub continue_pending_work: Option<bool>,
    /// Optional permission-request handler. See
    /// [`SessionConfig::permission_handler`].
    pub permission_handler: Option<Arc<dyn PermissionHandler>>,
    /// Optional elicitation handler. See
    /// [`SessionConfig::elicitation_handler`].
    pub elicitation_handler: Option<Arc<dyn ElicitationHandler>>,
    /// Optional user-input handler. See
    /// [`SessionConfig::user_input_handler`].
    pub user_input_handler: Option<Arc<dyn UserInputHandler>>,
    /// Optional exit-plan-mode handler. See
    /// [`SessionConfig::exit_plan_mode_handler`].
    pub exit_plan_mode_handler: Option<Arc<dyn ExitPlanModeHandler>>,
    /// Optional auto-mode-switch handler. See
    /// [`SessionConfig::auto_mode_switch_handler`].
    pub auto_mode_switch_handler: Option<Arc<dyn AutoModeSwitchHandler>>,
    /// Session hook handler. See [`SessionConfig::hooks_handler`].
    pub hooks_handler: Option<Arc<dyn SessionHooks>>,
    /// Permission policy. See `SessionConfig::permission_policy`.
    pub(crate) permission_policy: Option<crate::permission::Policy>,
    /// System-message transform. See [`SessionConfig::system_message_transform`].
    pub system_message_transform: Option<Arc<dyn SystemMessageTransform>>,
    /// See [`SessionConfig::skip_custom_instructions`].
    pub skip_custom_instructions: Option<bool>,
    /// See [`SessionConfig::custom_agents_local_only`].
    pub custom_agents_local_only: Option<bool>,
    /// See [`SessionConfig::coauthor_enabled`].
    pub coauthor_enabled: Option<bool>,
    /// See [`SessionConfig::manage_schedule_enabled`].
    pub manage_schedule_enabled: Option<bool>,
}

impl std::fmt::Debug for ResumeSessionConfig {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("ResumeSessionConfig")
            .field("session_id", &self.session_id)
            .field("client_name", &self.client_name)
            .field("reasoning_effort", &self.reasoning_effort)
            .field("reasoning_summary", &self.reasoning_summary)
            .field("context_tier", &self.context_tier)
            .field("streaming", &self.streaming)
            .field("system_message", &self.system_message)
            .field("tools", &self.tools)
            .field("canvases", &self.canvases)
            .field(
                "canvas_handler",
                &self.canvas_handler.as_ref().map(|_| "<set>"),
            )
            .field("open_canvases", &self.open_canvases)
            .field("request_canvas_renderer", &self.request_canvas_renderer)
            .field("request_extensions", &self.request_extensions)
            .field("extension_sdk_path", &self.extension_sdk_path)
            .field("extension_info", &self.extension_info)
            .field("available_tools", &self.available_tools)
            .field("excluded_tools", &self.excluded_tools)
            .field("mcp_servers", &self.mcp_servers)
            .field("mcp_oauth_token_storage", &self.mcp_oauth_token_storage)
            .field("embedding_cache_storage", &self.embedding_cache_storage)
            .field("enable_config_discovery", &self.enable_config_discovery)
            .field("skip_embedding_retrieval", &self.skip_embedding_retrieval)
            .field(
                "organization_custom_instructions",
                &self
                    .organization_custom_instructions
                    .as_ref()
                    .map(|_| "<redacted>"),
            )
            .field(
                "enable_on_demand_instruction_discovery",
                &self.enable_on_demand_instruction_discovery,
            )
            .field("enable_file_hooks", &self.enable_file_hooks)
            .field(
                "enable_host_git_operations",
                &self.enable_host_git_operations,
            )
            .field("enable_session_store", &self.enable_session_store)
            .field("enable_skills", &self.enable_skills)
            .field("enable_mcp_apps", &self.enable_mcp_apps)
            .field("skill_directories", &self.skill_directories)
            .field("instruction_directories", &self.instruction_directories)
            .field("plugin_directories", &self.plugin_directories)
            .field("large_output", &self.large_output)
            .field("disabled_skills", &self.disabled_skills)
            .field("hooks", &self.hooks)
            .field("custom_agents", &self.custom_agents)
            .field("default_agent", &self.default_agent)
            .field("agent", &self.agent)
            .field("infinite_sessions", &self.infinite_sessions)
            .field("provider", &self.provider)
            .field("enable_session_telemetry", &self.enable_session_telemetry)
            .field("model_capabilities", &self.model_capabilities)
            .field("memory", &self.memory)
            .field("config_directory", &self.config_directory)
            .field("working_directory", &self.working_directory)
            .field(
                "github_token",
                &self.github_token.as_ref().map(|_| "<redacted>"),
            )
            .field("remote_session", &self.remote_session)
            .field(
                "include_sub_agent_streaming_events",
                &self.include_sub_agent_streaming_events,
            )
            .field("commands", &self.commands)
            .field(
                "session_fs_provider",
                &self.session_fs_provider.as_ref().map(|_| "<set>"),
            )
            .field(
                "permission_handler",
                &self.permission_handler.as_ref().map(|_| "<set>"),
            )
            .field(
                "elicitation_handler",
                &self.elicitation_handler.as_ref().map(|_| "<set>"),
            )
            .field(
                "user_input_handler",
                &self.user_input_handler.as_ref().map(|_| "<set>"),
            )
            .field(
                "exit_plan_mode_handler",
                &self.exit_plan_mode_handler.as_ref().map(|_| "<set>"),
            )
            .field(
                "auto_mode_switch_handler",
                &self.auto_mode_switch_handler.as_ref().map(|_| "<set>"),
            )
            .field(
                "hooks_handler",
                &self.hooks_handler.as_ref().map(|_| "<set>"),
            )
            .field(
                "system_message_transform",
                &self.system_message_transform.as_ref().map(|_| "<set>"),
            )
            .field("suppress_resume_event", &self.suppress_resume_event)
            .field("continue_pending_work", &self.continue_pending_work)
            .finish()
    }
}

impl ResumeSessionConfig {
    /// Consume this config to produce the [`SessionResumeWire`] payload
    /// for `session.resume` and a [`SessionConfigRuntime`] bundle holding
    /// the runtime-only fields (handlers, transforms, providers).
    ///
    /// See [`SessionConfig::into_wire`] for the design rationale.
    ///
    /// [`SessionResumeWire`]: crate::wire::SessionResumeWire
    pub(crate) fn into_wire(
        mut self,
    ) -> Result<(crate::wire::SessionResumeWire, SessionConfigRuntime), crate::Error> {
        let permission_active =
            self.permission_handler.is_some() || self.permission_policy.is_some();
        let request_user_input = self.user_input_handler.is_some();
        let request_exit_plan_mode = self.exit_plan_mode_handler.is_some();
        let request_auto_mode_switch = self.auto_mode_switch_handler.is_some();
        let request_elicitation = self.elicitation_handler.is_some();
        let hooks_flag = self.hooks_handler.is_some();

        let mut tool_handlers: HashMap<String, Arc<dyn crate::tool::ToolHandler>> = HashMap::new();
        if let Some(tools) = self.tools.as_mut() {
            for tool in tools.iter_mut() {
                if let Some(handler) = tool.handler.take()
                    && tool_handlers.insert(tool.name.clone(), handler).is_some()
                {
                    return Err(crate::Error::with_message(
                        crate::ErrorKind::InvalidConfig,
                        format!("duplicate tool handler registered for name {:?}", tool.name),
                    ));
                }
            }
        }

        let wire_commands = self.commands.as_ref().map(|cmds| {
            cmds.iter()
                .map(|c| crate::wire::CommandWireDefinition {
                    name: c.name.clone(),
                    description: c.description.clone(),
                })
                .collect()
        });
        let wire_canvases = self.canvases.clone();
        let canvas_handler = self.canvas_handler.clone();

        let wire = crate::wire::SessionResumeWire {
            session_id: self.session_id,
            client_name: self.client_name,
            reasoning_effort: self.reasoning_effort,
            reasoning_summary: self.reasoning_summary,
            context_tier: self.context_tier,
            streaming: self.streaming,
            system_message: self.system_message,
            tools: self.tools,
            canvases: wire_canvases,
            open_canvases: self.open_canvases,
            request_canvas_renderer: self.request_canvas_renderer,
            request_extensions: self.request_extensions,
            extension_sdk_path: self.extension_sdk_path,
            extension_info: self.extension_info,
            available_tools: self.available_tools,
            excluded_tools: self.excluded_tools,
            tool_filter_precedence: "excluded",
            mcp_servers: self.mcp_servers,
            mcp_oauth_token_storage: self.mcp_oauth_token_storage,
            embedding_cache_storage: self.embedding_cache_storage,
            env_value_mode: "direct",
            enable_config_discovery: self.enable_config_discovery,
            skip_embedding_retrieval: self.skip_embedding_retrieval,
            organization_custom_instructions: self.organization_custom_instructions,
            enable_on_demand_instruction_discovery: self.enable_on_demand_instruction_discovery,
            enable_file_hooks: self.enable_file_hooks,
            enable_host_git_operations: self.enable_host_git_operations,
            enable_session_store: self.enable_session_store,
            enable_skills: self.enable_skills,
            request_user_input,
            request_permission: permission_active,
            request_exit_plan_mode,
            request_auto_mode_switch,
            request_elicitation,
            request_mcp_apps: self.enable_mcp_apps.unwrap_or(false),
            hooks: hooks_flag,
            skill_directories: self.skill_directories,
            instruction_directories: self.instruction_directories,
            plugin_directories: self.plugin_directories,
            large_output: self.large_output,
            disabled_skills: self.disabled_skills,
            custom_agents: self.custom_agents,
            default_agent: self.default_agent,
            agent: self.agent,
            infinite_sessions: self.infinite_sessions,
            provider: self.provider,
            providers: self.providers,
            models: self.models,
            enable_session_telemetry: self.enable_session_telemetry,
            model_capabilities: self.model_capabilities,
            memory: self.memory,
            config_dir: self.config_directory,
            working_directory: self.working_directory,
            github_token: self.github_token,
            remote_session: self.remote_session,
            include_sub_agent_streaming_events: self.include_sub_agent_streaming_events,
            commands: wire_commands,
            suppress_resume_event: self.suppress_resume_event,
            continue_pending_work: self.continue_pending_work,
        };

        let runtime = SessionConfigRuntime {
            permission_handler: self.permission_handler,
            permission_policy: self.permission_policy,
            elicitation_handler: self.elicitation_handler,
            user_input_handler: self.user_input_handler,
            exit_plan_mode_handler: self.exit_plan_mode_handler,
            auto_mode_switch_handler: self.auto_mode_switch_handler,
            hooks_handler: self.hooks_handler,
            system_message_transform: self.system_message_transform,
            tool_handlers,
            canvas_handler,
            session_fs_provider: self.session_fs_provider,
            commands: self.commands,
        };

        Ok((wire, runtime))
    }

    /// Construct a `ResumeSessionConfig` with the given session ID and all
    /// other fields left unset. Combine with `.with_*` builders or struct
    /// update syntax (`..ResumeSessionConfig::new(id)`) to populate the
    /// fields you need.
    pub fn new(session_id: SessionId) -> Self {
        Self {
            session_id,
            client_name: None,
            reasoning_effort: None,
            reasoning_summary: None,
            context_tier: None,
            streaming: None,
            system_message: None,
            tools: None,
            canvases: None,
            canvas_handler: None,
            open_canvases: None,
            request_canvas_renderer: None,
            request_extensions: None,
            extension_sdk_path: None,
            extension_info: None,
            available_tools: None,
            excluded_tools: None,
            mcp_servers: None,
            mcp_oauth_token_storage: None,
            enable_config_discovery: None,
            skip_embedding_retrieval: None,
            organization_custom_instructions: None,
            enable_on_demand_instruction_discovery: None,
            enable_file_hooks: None,
            enable_host_git_operations: None,
            enable_session_store: None,
            enable_skills: None,
            embedding_cache_storage: None,
            enable_mcp_apps: None,
            skill_directories: None,
            instruction_directories: None,
            plugin_directories: None,
            large_output: None,
            disabled_skills: None,
            hooks: None,
            custom_agents: None,
            default_agent: None,
            agent: None,
            infinite_sessions: None,
            provider: None,
            providers: None,
            models: None,
            enable_session_telemetry: None,
            model_capabilities: None,
            memory: None,
            config_directory: None,
            working_directory: None,
            github_token: None,
            remote_session: None,
            include_sub_agent_streaming_events: None,
            commands: None,
            session_fs_provider: None,
            suppress_resume_event: None,
            continue_pending_work: None,
            permission_handler: None,
            elicitation_handler: None,
            user_input_handler: None,
            exit_plan_mode_handler: None,
            auto_mode_switch_handler: None,
            hooks_handler: None,
            permission_policy: None,
            system_message_transform: None,
            skip_custom_instructions: None,
            custom_agents_local_only: None,
            coauthor_enabled: None,
            manage_schedule_enabled: None,
        }
    }

    /// Install a [`PermissionHandler`] for the resumed session.
    pub fn with_permission_handler(mut self, handler: Arc<dyn PermissionHandler>) -> Self {
        self.permission_handler = Some(handler);
        self
    }

    /// Install an [`ElicitationHandler`] for the resumed session.
    pub fn with_elicitation_handler(mut self, handler: Arc<dyn ElicitationHandler>) -> Self {
        self.elicitation_handler = Some(handler);
        self
    }

    /// Install a [`UserInputHandler`] for the resumed session.
    pub fn with_user_input_handler(mut self, handler: Arc<dyn UserInputHandler>) -> Self {
        self.user_input_handler = Some(handler);
        self
    }

    /// Install an [`ExitPlanModeHandler`] for the resumed session.
    pub fn with_exit_plan_mode_handler(mut self, handler: Arc<dyn ExitPlanModeHandler>) -> Self {
        self.exit_plan_mode_handler = Some(handler);
        self
    }

    /// Install an [`AutoModeSwitchHandler`] for the resumed session.
    pub fn with_auto_mode_switch_handler(
        mut self,
        handler: Arc<dyn AutoModeSwitchHandler>,
    ) -> Self {
        self.auto_mode_switch_handler = Some(handler);
        self
    }

    /// Install a [`SessionHooks`] handler. Automatically enables the
    /// wire-level `hooks` flag on session resumption.
    pub fn with_hooks(mut self, hooks: Arc<dyn SessionHooks>) -> Self {
        self.hooks_handler = Some(hooks);
        self
    }

    /// Install a [`SystemMessageTransform`].
    pub fn with_system_message_transform(
        mut self,
        transform: Arc<dyn SystemMessageTransform>,
    ) -> Self {
        self.system_message_transform = Some(transform);
        self
    }

    /// Register slash commands for the resumed session. See
    /// [`SessionConfig::with_commands`] — commands are not persisted
    /// server-side, so the resume payload re-supplies the registration.
    pub fn with_commands(mut self, commands: Vec<CommandDefinition>) -> Self {
        self.commands = Some(commands);
        self
    }

    /// Install a [`SessionFsProvider`] backing the resumed session's
    /// filesystem. See [`SessionConfig::with_session_fs_provider`].
    pub fn with_session_fs_provider(mut self, provider: Arc<dyn SessionFsProvider>) -> Self {
        self.session_fs_provider = Some(provider);
        self
    }

    /// Auto-approve every permission request on the resumed session. See
    /// [`SessionConfig::approve_all_permissions`].
    pub fn approve_all_permissions(mut self) -> Self {
        self.permission_policy = Some(crate::permission::Policy::ApproveAll);
        self
    }

    /// Auto-deny every permission request on the resumed session. See
    /// [`SessionConfig::deny_all_permissions`].
    pub fn deny_all_permissions(mut self) -> Self {
        self.permission_policy = Some(crate::permission::Policy::DenyAll);
        self
    }

    /// Apply a closure-based permission policy on the resumed session.
    /// See [`SessionConfig::approve_permissions_if`].
    pub fn approve_permissions_if<F>(mut self, predicate: F) -> Self
    where
        F: Fn(&crate::types::PermissionRequestData) -> bool + Send + Sync + 'static,
    {
        self.permission_policy = Some(crate::permission::Policy::Predicate(Arc::new(predicate)));
        self
    }

    /// Set the application name sent as `User-Agent` context.
    pub fn with_client_name(mut self, name: impl Into<String>) -> Self {
        self.client_name = Some(name.into());
        self
    }

    /// Set the reasoning effort to apply on resume.
    pub fn with_reasoning_effort(mut self, effort: impl Into<String>) -> Self {
        self.reasoning_effort = Some(effort.into());
        self
    }

    /// Set [`reasoning_summary`](Self::reasoning_summary).
    pub fn with_reasoning_summary(mut self, summary: ReasoningSummary) -> Self {
        self.reasoning_summary = Some(summary);
        self
    }

    /// Set the context window tier to apply on resume (e.g. `"default"`,
    /// `"long_context"`).
    pub fn with_context_tier(mut self, tier: impl Into<String>) -> Self {
        self.context_tier = Some(tier.into());
        self
    }

    /// Enable streaming token deltas via `assistant.message_delta` events.
    pub fn with_streaming(mut self, streaming: bool) -> Self {
        self.streaming = Some(streaming);
        self
    }

    /// Re-supply the system message so the agent retains workspace context
    /// across CLI process restarts.
    pub fn with_system_message(mut self, system_message: SystemMessageConfig) -> Self {
        self.system_message = Some(system_message);
        self
    }

    /// Re-supply client-defined tools on resume.
    pub fn with_tools<I: IntoIterator<Item = Tool>>(mut self, tools: I) -> Self {
        self.tools = Some(tools.into_iter().collect());
        self
    }

    /// Re-supply canvas declarations on resume.
    pub fn with_canvases<I: IntoIterator<Item = CanvasDeclaration>>(mut self, canvases: I) -> Self {
        self.canvases = Some(canvases.into_iter().collect());
        self
    }

    /// Install the provider-side [`CanvasHandler`] for the resumed session.
    pub fn with_canvas_handler(mut self, handler: Arc<dyn CanvasHandler>) -> Self {
        self.canvas_handler = Some(handler);
        self
    }

    /// Seed open canvas instances that were visible before resuming.
    pub fn with_open_canvases<I: IntoIterator<Item = OpenCanvasInstance>>(
        mut self,
        open_canvases: I,
    ) -> Self {
        self.open_canvases = Some(open_canvases.into_iter().collect());
        self
    }

    /// Request host canvas renderer tools for this connection on resume.
    pub fn with_request_canvas_renderer(mut self, request: bool) -> Self {
        self.request_canvas_renderer = Some(request);
        self
    }

    /// Request extension tools and dispatch for this connection on resume.
    pub fn with_request_extensions(mut self, request: bool) -> Self {
        self.request_extensions = Some(request);
        self
    }

    /// Override the bundled `@github/copilot-sdk` drop injected into extension
    /// subprocesses for this resumed session. Invalid paths fall back to the
    /// bundled SDK silently.
    pub fn with_extension_sdk_path(mut self, path: impl Into<String>) -> Self {
        self.extension_sdk_path = Some(path.into());
        self
    }

    /// Set stable extension identity metadata for this connection on resume.
    pub fn with_extension_info(mut self, extension_info: ExtensionInfo) -> Self {
        self.extension_info = Some(extension_info);
        self
    }

    /// Set the allowlist of tool names the agent may use.
    pub fn with_available_tools<I, S>(mut self, tools: I) -> Self
    where
        I: IntoIterator<Item = S>,
        S: Into<String>,
    {
        self.available_tools = Some(tools.into_iter().map(Into::into).collect());
        self
    }

    /// Set the blocklist of built-in tool names the agent must not use.
    pub fn with_excluded_tools<I, S>(mut self, tools: I) -> Self
    where
        I: IntoIterator<Item = S>,
        S: Into<String>,
    {
        self.excluded_tools = Some(tools.into_iter().map(Into::into).collect());
        self
    }

    /// Re-supply MCP server configurations on resume.
    pub fn with_mcp_servers(mut self, servers: HashMap<String, McpServerConfig>) -> Self {
        self.mcp_servers = Some(servers);
        self
    }

    /// Set MCP OAuth token storage mode on resume.
    /// See [`SessionConfig::with_mcp_oauth_token_storage`] for details.
    pub fn with_mcp_oauth_token_storage(mut self, mode: impl Into<String>) -> Self {
        self.mcp_oauth_token_storage = Some(mode.into());
        self
    }

    /// Set embedding cache storage mode on resume.
    pub fn with_embedding_cache_storage(
        mut self,
        embedding_cache_storage: impl Into<String>,
    ) -> Self {
        self.embedding_cache_storage = Some(embedding_cache_storage.into());
        self
    }

    /// Enable or disable CLI config discovery on resume.
    pub fn with_enable_config_discovery(mut self, enable: bool) -> Self {
        self.enable_config_discovery = Some(enable);
        self
    }

    /// Set [`Self::skip_embedding_retrieval`].
    pub fn with_skip_embedding_retrieval(mut self, value: bool) -> Self {
        self.skip_embedding_retrieval = Some(value);
        self
    }

    /// Set [`Self::organization_custom_instructions`].
    pub fn with_organization_custom_instructions(
        mut self,
        instructions: impl Into<String>,
    ) -> Self {
        self.organization_custom_instructions = Some(instructions.into());
        self
    }

    /// Set [`Self::enable_on_demand_instruction_discovery`].
    pub fn with_enable_on_demand_instruction_discovery(mut self, value: bool) -> Self {
        self.enable_on_demand_instruction_discovery = Some(value);
        self
    }

    /// Set [`Self::enable_file_hooks`].
    pub fn with_enable_file_hooks(mut self, value: bool) -> Self {
        self.enable_file_hooks = Some(value);
        self
    }

    /// Set [`Self::enable_host_git_operations`].
    pub fn with_enable_host_git_operations(mut self, value: bool) -> Self {
        self.enable_host_git_operations = Some(value);
        self
    }

    /// Set [`Self::enable_session_store`].
    pub fn with_enable_session_store(mut self, value: bool) -> Self {
        self.enable_session_store = Some(value);
        self
    }

    /// Set [`Self::enable_skills`].
    pub fn with_enable_skills(mut self, value: bool) -> Self {
        self.enable_skills = Some(value);
        self
    }

    /// **Experimental.** This method is part of an experimental wire-protocol
    /// surface (SEP-1865) and may change or be removed in a future release.
    ///
    /// Enable MCP Apps (SEP-1865) UI passthrough on resume. Defaults to
    /// `None` (treated as `false`). See [`SessionConfig::enable_mcp_apps`].
    pub fn with_enable_mcp_apps(mut self, enable: bool) -> Self {
        self.enable_mcp_apps = Some(enable);
        self
    }

    /// Set skill directory paths passed through to the CLI on resume.
    pub fn with_skill_directories<I, P>(mut self, paths: I) -> Self
    where
        I: IntoIterator<Item = P>,
        P: Into<PathBuf>,
    {
        self.skill_directories = Some(paths.into_iter().map(Into::into).collect());
        self
    }

    /// Set additional directories to search for custom instruction files
    /// on resume. Forwarded to the CLI; not the same as
    /// [`with_skill_directories`](Self::with_skill_directories).
    pub fn with_instruction_directories<I, P>(mut self, paths: I) -> Self
    where
        I: IntoIterator<Item = P>,
        P: Into<PathBuf>,
    {
        self.instruction_directories = Some(paths.into_iter().map(Into::into).collect());
        self
    }

    /// Set Open Plugin directory paths passed through to the CLI on resume.
    pub fn with_plugin_directories<I, P>(mut self, paths: I) -> Self
    where
        I: IntoIterator<Item = P>,
        P: Into<PathBuf>,
    {
        self.plugin_directories = Some(paths.into_iter().map(Into::into).collect());
        self
    }

    /// Set the [`LargeToolOutputConfig`] forwarded to the CLI on resume.
    pub fn with_large_output(mut self, config: LargeToolOutputConfig) -> Self {
        self.large_output = Some(config);
        self
    }

    /// Set the names of skills to disable on resume.
    pub fn with_disabled_skills<I, S>(mut self, names: I) -> Self
    where
        I: IntoIterator<Item = S>,
        S: Into<String>,
    {
        self.disabled_skills = Some(names.into_iter().map(Into::into).collect());
        self
    }

    /// Re-supply custom agents on resume.
    pub fn with_custom_agents<I: IntoIterator<Item = CustomAgentConfig>>(
        mut self,
        agents: I,
    ) -> Self {
        self.custom_agents = Some(agents.into_iter().collect());
        self
    }

    /// Configure the built-in default agent on resume.
    pub fn with_default_agent(mut self, agent: DefaultAgentConfig) -> Self {
        self.default_agent = Some(agent);
        self
    }

    /// Activate a named custom agent on resume.
    pub fn with_agent(mut self, name: impl Into<String>) -> Self {
        self.agent = Some(name.into());
        self
    }

    /// Re-supply infinite session configuration on resume.
    pub fn with_infinite_sessions(mut self, config: InfiniteSessionConfig) -> Self {
        self.infinite_sessions = Some(config);
        self
    }

    /// Re-supply BYOK provider configuration on resume.
    pub fn with_provider(mut self, provider: ProviderConfig) -> Self {
        self.provider = Some(provider);
        self
    }

    /// **Experimental.** This method is part of an experimental multi-provider
    /// BYOK surface and may change or be removed in a future release.
    ///
    /// Re-supply the named BYOK provider connections on resume. Attach
    /// models referencing these with [`Self::with_models`].
    pub fn with_providers(mut self, providers: Vec<NamedProviderConfig>) -> Self {
        self.providers = Some(providers);
        self
    }

    /// **Experimental.** This method is part of an experimental multi-provider
    /// BYOK surface and may change or be removed in a future release.
    ///
    /// Re-supply the BYOK model definitions on resume, each referencing a
    /// named provider supplied via [`Self::with_providers`].
    pub fn with_models(mut self, models: Vec<ProviderModelConfig>) -> Self {
        self.models = Some(models);
        self
    }

    /// Enable or disable internal session telemetry on resume.
    ///
    /// See [`Self::enable_session_telemetry`] for default and BYOK behavior.
    pub fn with_enable_session_telemetry(mut self, enable: bool) -> Self {
        self.enable_session_telemetry = Some(enable);
        self
    }

    /// Set per-property model capability overrides on resume.
    pub fn with_model_capabilities(
        mut self,
        capabilities: crate::generated::api_types::ModelCapabilitiesOverride,
    ) -> Self {
        self.model_capabilities = Some(capabilities);
        self
    }

    /// Configure the runtime memory feature for the resumed session.
    pub fn with_memory(mut self, memory: MemoryConfiguration) -> Self {
        self.memory = Some(memory);
        self
    }

    /// Override the default configuration directory location on resume.
    pub fn with_config_directory(mut self, dir: impl Into<PathBuf>) -> Self {
        self.config_directory = Some(dir.into());
        self
    }

    /// Set the per-session working directory on resume.
    pub fn with_working_directory(mut self, dir: impl Into<PathBuf>) -> Self {
        self.working_directory = Some(dir.into());
        self
    }

    /// Set the per-session GitHub token on resume. See
    /// [`SessionConfig::github_token`] for distinction from the
    /// client-level token.
    pub fn with_github_token(mut self, token: impl Into<String>) -> Self {
        self.github_token = Some(token.into());
        self
    }

    /// Forward sub-agent streaming events to this connection on resume.
    pub fn with_include_sub_agent_streaming_events(mut self, include: bool) -> Self {
        self.include_sub_agent_streaming_events = Some(include);
        self
    }

    /// Set per-session remote behavior on resume.
    pub fn with_remote_session(
        mut self,
        mode: crate::generated::api_types::RemoteSessionMode,
    ) -> Self {
        self.remote_session = Some(mode);
        self
    }

    /// Force-fail resume if the session does not exist on disk, instead
    /// of silently starting a new session.
    pub fn with_suppress_resume_event(mut self, suppress: bool) -> Self {
        self.suppress_resume_event = Some(suppress);
        self
    }

    /// When `true`, instructs the runtime to continue any tool calls or
    /// permission requests that were pending when the previous connection
    /// was dropped. Use this together with
    /// [`Client::force_stop`](crate::Client::force_stop) to hand off a
    /// session from one process to another without losing in-flight work.
    pub fn with_continue_pending_work(mut self, continue_pending: bool) -> Self {
        self.continue_pending_work = Some(continue_pending);
        self
    }

    /// Set [`Self::skip_custom_instructions`].
    pub fn with_skip_custom_instructions(mut self, value: bool) -> Self {
        self.skip_custom_instructions = Some(value);
        self
    }

    /// Set [`Self::custom_agents_local_only`].
    pub fn with_custom_agents_local_only(mut self, value: bool) -> Self {
        self.custom_agents_local_only = Some(value);
        self
    }

    /// Set [`Self::coauthor_enabled`].
    pub fn with_coauthor_enabled(mut self, value: bool) -> Self {
        self.coauthor_enabled = Some(value);
        self
    }

    /// Set [`Self::manage_schedule_enabled`].
    pub fn with_manage_schedule_enabled(mut self, value: bool) -> Self {
        self.manage_schedule_enabled = Some(value);
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
#[non_exhaustive]
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

impl SystemMessageConfig {
    /// Construct an empty [`SystemMessageConfig`]; all fields default to
    /// unset.
    pub fn new() -> Self {
        Self::default()
    }

    /// Set the application mode: `"append"` (default), `"replace"`, or
    /// `"customize"`.
    pub fn with_mode(mut self, mode: impl Into<String>) -> Self {
        self.mode = Some(mode.into());
        self
    }

    /// Set the system message content (used by `"append"` and `"replace"`
    /// modes).
    pub fn with_content(mut self, content: impl Into<String>) -> Self {
        self.content = Some(content.into());
        self
    }

    /// Set the section-level overrides (used with `mode: "customize"`).
    pub fn with_sections(mut self, sections: HashMap<String, SectionOverride>) -> Self {
        self.sections = Some(sections);
        self
    }
}

/// An override operation for a single system message section.
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

/// Response from `session.resume`.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ResumeSessionResult {
    /// The CLI-assigned session ID. Older runtimes may omit this on resume.
    #[serde(default)]
    pub session_id: Option<SessionId>,
    /// Workspace directory for the session (infinite sessions).
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub workspace_path: Option<PathBuf>,
    /// Remote session URL, if the session is running remotely.
    #[serde(default, alias = "remote_url")]
    pub remote_url: Option<String>,
    /// Capabilities negotiated with the CLI for this session.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub capabilities: Option<SessionCapabilities>,
    /// Canvas instances already open when the session was resumed.
    #[serde(
        default,
        alias = "openCanvasInstances",
        skip_serializing_if = "Option::is_none"
    )]
    pub open_canvases: Option<Vec<OpenCanvasInstance>>,
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
    /// Reasoning summary mode for the new model. Use
    /// [`ReasoningSummary::None`] to suppress summary output regardless of
    /// whether reasoning is enabled.
    pub reasoning_summary: Option<ReasoningSummary>,
    /// Explicit context window tier for the new model. Leave unset to use
    /// normal model behavior with no explicit tier.
    pub context_tier: Option<ContextTier>,
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

    /// Set [`reasoning_summary`](Self::reasoning_summary).
    pub fn with_reasoning_summary(mut self, summary: ReasoningSummary) -> Self {
        self.reasoning_summary = Some(summary);
        self
    }

    /// Set [`context_tier`](Self::context_tier).
    pub fn with_context_tier(mut self, tier: ContextTier) -> Self {
        self.context_tier = Some(tier);
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
/// The `protocol_version` field is the most commonly-inspected piece —
/// see [`Client::verify_protocol_version`].
///
/// [`Client::verify_protocol_version`]: crate::Client::verify_protocol_version
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PingResponse {
    /// The message echoed back by the CLI.
    #[serde(default)]
    pub message: String,
    /// ISO 8601 timestamp when the ping was processed.
    #[serde(default)]
    pub timestamp: String,
    /// The protocol version negotiated by the CLI, if reported.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub protocol_version: Option<u32>,
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
#[non_exhaustive]
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
#[non_exhaustive]
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

/// Message delivery mode for [`MessageOptions::mode`].
///
/// Controls how a prompt is delivered relative to in-flight session work.
/// Wire values: `"enqueue"` and `"immediate"`.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
#[non_exhaustive]
pub enum DeliveryMode {
    /// Queue the prompt behind any in-flight work (default).
    Enqueue,
    /// Interrupt the session and run the prompt immediately.
    Immediate,
}

/// The UI mode the agent is in for a given turn, used by
/// [`MessageOptions::agent_mode`].
///
/// Wire values: `"interactive"`, `"plan"`, `"autopilot"`, `"shell"`.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
#[non_exhaustive]
pub enum AgentMode {
    /// The agent is responding interactively to the user.
    Interactive,
    /// The agent is preparing a plan before making changes.
    Plan,
    /// The agent is working autonomously toward task completion.
    Autopilot,
    /// The agent is in shell-focused UI mode.
    Shell,
}

/// Options for sending a user message to the agent.
///
/// Used by both [`Session::send`](crate::session::Session::send) and
/// [`Session::send_and_wait`](crate::session::Session::send_and_wait); the
/// `wait_timeout` field is honored only by `send_and_wait` and is ignored by
/// `send`.
///
/// `MessageOptions` is `#[non_exhaustive]` and constructed via [`MessageOptions::new`]
/// plus the `with_*` chain so future fields can land without breaking callers.
/// For the trivial case, both `&str` and `String` implement `Into<MessageOptions>`,
/// so:
///
/// ```no_run
/// # use github_copilot_sdk::session::Session;
/// # async fn run(session: Session) -> Result<(), github_copilot_sdk::Error> {
/// session.send("hello").await?;
/// # Ok(()) }
/// ```
///
/// is equivalent to:
///
/// ```no_run
/// # use github_copilot_sdk::session::Session;
/// # use github_copilot_sdk::types::MessageOptions;
/// # async fn run(session: Session) -> Result<(), github_copilot_sdk::Error> {
/// session.send(MessageOptions::new("hello")).await?;
/// # Ok(()) }
/// ```
#[derive(Debug, Clone)]
#[non_exhaustive]
pub struct MessageOptions {
    /// The user prompt to send.
    pub prompt: String,
    /// Optional message delivery mode for this turn.
    ///
    /// Controls whether the prompt is queued behind in-flight work
    /// ([`DeliveryMode::Enqueue`], default) or interrupts the session and
    /// runs immediately ([`DeliveryMode::Immediate`]).
    pub mode: Option<DeliveryMode>,
    /// Optional UI mode the agent was in when this message was sent
    /// (for example [`AgentMode::Plan`] or [`AgentMode::Autopilot`]).
    /// Defaults to the session's current mode when `None`.
    pub agent_mode: Option<AgentMode>,
    /// Optional attachments to include with the message.
    pub attachments: Option<Vec<Attachment>>,
    /// Maximum time to wait for the session to go idle. Honored only by
    /// `send_and_wait`. Defaults to 60 seconds when unset.
    pub wait_timeout: Option<Duration>,
    /// Custom HTTP headers to include in outbound model requests for this
    /// turn. When `None` or empty, no `requestHeaders` field is sent on
    /// the wire.
    pub request_headers: Option<HashMap<String, String>>,
    /// W3C Trace Context `traceparent` header for this turn.
    ///
    /// Per-turn override that takes precedence over
    /// [`ClientOptions::on_get_trace_context`](crate::ClientOptions::on_get_trace_context).
    /// When `None`, the SDK falls back to the provider (if configured)
    /// before omitting the field.
    pub traceparent: Option<String>,
    /// W3C Trace Context `tracestate` header for this turn.
    ///
    /// Per-turn override paired with [`traceparent`](Self::traceparent).
    pub tracestate: Option<String>,
    /// If provided, this is shown in the timeline instead of `prompt`.
    pub display_prompt: Option<String>,
}

impl MessageOptions {
    /// Build a new `MessageOptions` with just a prompt.
    pub fn new(prompt: impl Into<String>) -> Self {
        Self {
            prompt: prompt.into(),
            mode: None,
            agent_mode: None,
            attachments: None,
            wait_timeout: None,
            request_headers: None,
            traceparent: None,
            tracestate: None,
            display_prompt: None,
        }
    }

    /// Set the message delivery mode for this turn.
    ///
    /// Pass [`DeliveryMode::Immediate`] to interrupt the session and run
    /// the prompt now; the default ([`DeliveryMode::Enqueue`]) queues the
    /// prompt behind in-flight work.
    pub fn with_mode(mut self, mode: DeliveryMode) -> Self {
        self.mode = Some(mode);
        self
    }

    /// Set the per-message agent UI mode for this turn.
    ///
    /// When `None`, the session's current mode is used.
    pub fn with_agent_mode(mut self, agent_mode: AgentMode) -> Self {
        self.agent_mode = Some(agent_mode);
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

    /// Set custom HTTP headers for outbound model requests for this turn.
    pub fn with_request_headers(mut self, headers: HashMap<String, String>) -> Self {
        self.request_headers = Some(headers);
        self
    }

    /// Set both `traceparent` and `tracestate` from a [`TraceContext`].
    /// Either field may remain `None` if the [`TraceContext`] has no value
    /// for it. Use [`with_traceparent`](Self::with_traceparent) or
    /// [`with_tracestate`](Self::with_tracestate) to set them individually.
    pub fn with_trace_context(mut self, ctx: TraceContext) -> Self {
        self.traceparent = ctx.traceparent;
        self.tracestate = ctx.tracestate;
        self
    }

    /// Set the W3C `traceparent` header for this turn.
    pub fn with_traceparent(mut self, traceparent: impl Into<String>) -> Self {
        self.traceparent = Some(traceparent.into());
        self
    }

    /// Set the W3C `tracestate` header for this turn.
    pub fn with_tracestate(mut self, tracestate: impl Into<String>) -> Self {
        self.tracestate = Some(tracestate.into());
        self
    }

    /// Set the display prompt shown in the timeline instead of `prompt`.
    pub fn with_display_prompt(mut self, display_prompt: impl Into<String>) -> Self {
        self.display_prompt = Some(display_prompt.into());
        self
    }
}

impl From<&str> for MessageOptions {
    fn from(prompt: &str) -> Self {
        Self::new(prompt)
    }
}

impl From<String> for MessageOptions {
    fn from(prompt: String) -> Self {
        Self::new(prompt)
    }
}

impl From<&String> for MessageOptions {
    fn from(prompt: &String) -> Self {
        Self::new(prompt.clone())
    }
}

/// Response from [`Client::get_status`](crate::Client::get_status).
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
#[non_exhaustive]
pub struct GetStatusResponse {
    /// Package version (e.g. `"1.0.0"`).
    pub version: String,
    /// Protocol version for SDK compatibility.
    pub protocol_version: u32,
}

/// Response from [`Client::get_auth_status`](crate::Client::get_auth_status).
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
#[non_exhaustive]
pub struct GetAuthStatusResponse {
    /// Whether the user is authenticated.
    pub is_authenticated: bool,
    /// Authentication type (e.g. `"user"`, `"env"`, `"gh-cli"`, `"hmac"`,
    /// `"api-key"`, `"token"`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_type: Option<String>,
    /// GitHub host URL.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<String>,
    /// User login name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub login: Option<String>,
    /// Human-readable status message.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status_message: Option<String>,
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
    /// Sub-agent instance identifier. Absent for events emitted by the
    /// root/main agent and for session-level events.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_id: Option<String>,
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
    /// Parse the string `event_type` into a typed [`SessionEventType`](crate::session_events::SessionEventType) enum.
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
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
#[non_exhaustive]
pub struct ToolInvocation {
    /// Session that owns this tool call.
    pub session_id: SessionId,
    /// Unique ID for this tool call, used to correlate the response.
    pub tool_call_id: String,
    /// Name of the tool being invoked.
    pub tool_name: String,
    /// Tool arguments as JSON.
    pub arguments: Value,
    /// W3C Trace Context `traceparent` header propagated from the CLI's
    /// `execute_tool` span. Pass through to OpenTelemetry-aware code so
    /// child spans created inside the handler are parented to the CLI
    /// span. `None` when the CLI has no trace context for this call.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub traceparent: Option<String>,
    /// W3C Trace Context `tracestate` paired with
    /// [`traceparent`](Self::traceparent).
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub tracestate: Option<String>,
}

impl ToolInvocation {
    /// Deserialize this invocation's [`arguments`](Self::arguments) into a
    /// strongly-typed parameter struct.
    ///
    /// Idiomatic way to extract typed parameters when implementing
    /// [`ToolHandler`](crate::tool::ToolHandler) directly. Equivalent to
    /// `serde_json::from_value(invocation.arguments.clone())` with the SDK's
    /// error type.
    ///
    /// # Example
    ///
    /// ```rust,no_run
    /// # use github_copilot_sdk::{Error, types::ToolInvocation, ToolResult};
    /// # use serde::Deserialize;
    /// # #[derive(Deserialize)] struct MyParams { city: String }
    /// # async fn example(inv: ToolInvocation) -> Result<ToolResult, Error> {
    /// let params: MyParams = inv.params()?;
    /// // …use `inv.session_id` / `inv.tool_call_id` alongside `params`…
    /// # let _ = params; Ok(ToolResult::Text(String::new()))
    /// # }
    /// ```
    pub fn params<P: serde::de::DeserializeOwned>(&self) -> Result<P, crate::Error> {
        serde_json::from_value(self.arguments.clone()).map_err(crate::Error::from)
    }

    /// Returns the propagated [`TraceContext`] for this invocation, or
    /// [`TraceContext::default()`] when the CLI sent no headers.
    pub fn trace_context(&self) -> TraceContext {
        TraceContext {
            traceparent: self.traceparent.clone(),
            tracestate: self.tracestate.clone(),
        }
    }
}

/// Binary content returned by a tool.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolBinaryResult {
    /// Base64-encoded binary data.
    pub data: String,
    /// MIME type for the binary data.
    pub mime_type: String,
    /// Type identifier for the binary result.
    pub r#type: String,
    /// Optional description shown alongside the binary result.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
}

/// Expanded tool result with metadata for the LLM and session log.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolResultExpanded {
    /// Result text sent back to the LLM.
    pub text_result_for_llm: String,
    /// `"success"` or `"failure"`.
    pub result_type: String,
    /// Binary payloads sent back to the LLM.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub binary_results_for_llm: Option<Vec<ToolBinaryResult>>,
    /// Optional log message for the session timeline.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_log: Option<String>,
    /// Error message, if the tool failed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Tool-specific telemetry emitted with the result.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub tool_telemetry: Option<HashMap<String, Value>>,
}

/// Result of a tool invocation — either a plain text string or an expanded result.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
#[non_exhaustive]
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

/// Filter options for [`Client::list_sessions`](crate::Client::list_sessions).
///
/// All fields are optional; unset fields don't constrain the result.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionListFilter {
    /// Filter by exact `cwd` match.
    #[serde(default, skip_serializing_if = "Option::is_none", rename = "cwd")]
    pub working_directory: Option<String>,
    /// Filter by git root path.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Filter by repository in `owner/repo` form.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    /// Filter by git branch name.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
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
#[non_exhaustive]
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
/// Received via `elicitation.requested` session event when the session has
/// an [`ElicitationHandler`] installed.
/// The provider should render a form or dialog and return an
/// [`ElicitationResult`].
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
    /// **Experimental.** This field is part of an experimental wire-protocol
    /// surface (SEP-1865) and may change or be removed in a future release.
    ///
    /// Whether the runtime has accepted the session's MCP Apps (SEP-1865)
    /// opt-in. `Some(true)` when the consumer set
    /// [`SessionConfig::enable_mcp_apps`] / [`ResumeSessionConfig::enable_mcp_apps`]
    /// to `true` on create/resume **and** the runtime's `MCP_APPS` feature
    /// flag (or `COPILOT_MCP_APPS=true` env override) is on. Otherwise
    /// absent or `Some(false)`, indicating the runtime silently dropped the
    /// opt-in.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_apps: Option<bool>,
    /// Host-specific canvas capabilities.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub canvases: Option<bool>,
}

/// Options for the [`SessionUi::input`](crate::session::SessionUi::input) convenience method.
#[derive(Debug, Clone, Default)]
pub struct UiInputOptions<'a> {
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
#[non_exhaustive]
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
/// [`crate::rpc`]; they live here so the crate-root
/// `pub use types::*` surfaces them alongside hand-written SDK types.
pub use crate::generated::api_types::{
    Model, ModelBilling, ModelBillingTokenPrices, ModelBillingTokenPricesLongContext,
    ModelCapabilities, ModelCapabilitiesLimits, ModelCapabilitiesLimitsVision,
    ModelCapabilitiesSupports, ModelList, ModelPolicy, PermissionDecision,
    PermissionDecisionApproveOnce, PermissionDecisionReject, PermissionDecisionUserNotAvailable,
};

/// Permission categories the CLI may request approval for.
///
/// Wire values are the lower-kebab strings the CLI sends as the `kind`
/// discriminator on a permission request. Marked `#[non_exhaustive]`
/// because the CLI may add new kinds; matches must include a `_` arm.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[serde(rename_all = "kebab-case")]
#[non_exhaustive]
pub enum PermissionRequestKind {
    /// Run a shell command.
    Shell,
    /// Write to a file.
    Write,
    /// Read a file.
    Read,
    /// Open a URL.
    Url,
    /// Invoke an MCP server tool.
    Mcp,
    /// Invoke a client-defined custom tool.
    CustomTool,
    /// Update agent memory.
    Memory,
    /// Run a hook callback.
    Hook,
    /// Unrecognized kind. The original wire string is available in
    /// [`PermissionRequestData::extra`] under the `kind` key.
    #[serde(other)]
    Unknown,
}

/// Data sent by the CLI for permission-related events.
///
/// Used for both the `permission.request` RPC call (which expects a response)
/// and `permission.requested` notifications (fire-and-forget). Contains the
/// full params object.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequestData {
    /// The permission category being requested. `None` means the CLI did
    /// not include a `kind` field. Use this to branch on common cases
    /// (shell, write, etc.) without parsing [`extra`](Self::extra).
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub kind: Option<PermissionRequestKind>,
    /// The originating tool-call ID, if this permission request is tied
    /// to a specific tool invocation.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
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
        AgentMode, Attachment, AttachmentLineRange, AttachmentSelectionPosition,
        AttachmentSelectionRange, AzureProviderOptions, ConnectionState, CustomAgentConfig,
        DeliveryMode, ExtensionInfo, GitHubReferenceType, InfiniteSessionConfig,
        LargeToolOutputConfig, MemoryConfiguration, NamedProviderConfig, ProviderConfig,
        ProviderModelConfig, ReasoningSummary, ResumeSessionConfig, SessionConfig, SessionEvent,
        SessionId, SystemMessageConfig, Tool, ToolBinaryResult, ToolResult, ToolResultExpanded,
        ToolResultResponse, ensure_attachment_display_names,
    };
    use crate::generated::session_events::TypedSessionEvent;

    #[test]
    fn tool_builder_composes() {
        let tool = Tool::new("greet")
            .with_description("Say hello")
            .with_namespaced_name("hello/greet")
            .with_instructions("Pass the user's name")
            .with_parameters(json!({
                "type": "object",
                "properties": { "name": { "type": "string" } },
                "required": ["name"]
            }))
            .with_overrides_built_in_tool(true)
            .with_skip_permission(true);
        assert_eq!(tool.name, "greet");
        assert_eq!(tool.description, "Say hello");
        assert_eq!(tool.namespaced_name.as_deref(), Some("hello/greet"));
        assert_eq!(tool.instructions.as_deref(), Some("Pass the user's name"));
        assert_eq!(tool.parameters.get("type").unwrap(), &json!("object"));
        assert!(tool.overrides_built_in_tool);
        assert!(tool.skip_permission);
    }

    #[test]
    fn tool_defer_serialization() {
        let tool = Tool::new("lookup").with_defer(super::DeferMode::Auto);
        assert_eq!(tool.defer, Some(super::DeferMode::Auto));
        let value = serde_json::to_value(&tool).unwrap();
        assert_eq!(value.get("defer").unwrap(), &json!("auto"));

        let plain = Tool::new("plain");
        let value = serde_json::to_value(&plain).unwrap();
        assert!(value.get("defer").is_none());
    }

    #[test]
    fn custom_agent_config_builder_with_model() {
        let agent = CustomAgentConfig::new("my-agent", "You are helpful.")
            .with_model("claude-haiku-4.5")
            .with_display_name("My Agent");
        assert_eq!(agent.name, "my-agent");
        assert_eq!(agent.model.as_deref(), Some("claude-haiku-4.5"));
        assert_eq!(agent.display_name.as_deref(), Some("My Agent"));
    }

    #[test]
    fn custom_agent_config_serializes_model() {
        let agent = CustomAgentConfig::new("model-agent", "prompt").with_model("claude-haiku-4.5");
        let wire = serde_json::to_value(&agent).unwrap();
        assert_eq!(wire["model"], "claude-haiku-4.5");
        assert_eq!(wire["name"], "model-agent");
    }

    #[test]
    fn custom_agent_config_omits_model_when_none() {
        let agent = CustomAgentConfig::new("no-model-agent", "prompt");
        let wire = serde_json::to_value(&agent).unwrap();
        assert!(wire.get("model").is_none());
    }

    #[test]
    #[should_panic(expected = "tool parameter schema must be a JSON object")]
    fn tool_with_parameters_panics_on_non_object_value() {
        let _ = Tool::new("noop").with_parameters(json!(null));
    }

    #[test]
    fn tool_result_expanded_serializes_binary_results_for_llm() {
        let response = ToolResultResponse {
            result: ToolResult::Expanded(ToolResultExpanded {
                text_result_for_llm: "rendered chart".to_string(),
                result_type: "success".to_string(),
                binary_results_for_llm: Some(vec![ToolBinaryResult {
                    data: "aW1n".to_string(),
                    mime_type: "image/png".to_string(),
                    r#type: "image".to_string(),
                    description: Some("chart preview".to_string()),
                }]),
                session_log: None,
                error: None,
                tool_telemetry: None,
            }),
        };

        let wire = serde_json::to_value(&response).unwrap();

        assert_eq!(
            wire,
            json!({
                "result": {
                    "textResultForLlm": "rendered chart",
                    "resultType": "success",
                    "binaryResultsForLlm": [
                        {
                            "data": "aW1n",
                            "mimeType": "image/png",
                            "type": "image",
                            "description": "chart preview"
                        }
                    ]
                }
            })
        );
    }

    #[test]
    fn tool_result_expanded_omits_binary_results_for_llm_when_none() {
        let response = ToolResultResponse {
            result: ToolResult::Expanded(ToolResultExpanded {
                text_result_for_llm: "ok".to_string(),
                result_type: "success".to_string(),
                binary_results_for_llm: None,
                session_log: None,
                error: None,
                tool_telemetry: None,
            }),
        };

        let wire = serde_json::to_value(&response).unwrap();

        assert_eq!(wire["result"]["textResultForLlm"], "ok");
        assert!(wire["result"].get("binaryResultsForLlm").is_none());
    }

    #[test]
    fn session_config_default_wire_flags_off_without_handlers() {
        let cfg = SessionConfig::default();
        assert_eq!(cfg.mcp_oauth_token_storage, None);
        // Wire flags are derived from handler presence at create_session
        // time, not stored on the config. With no handlers installed, every
        // request_* flag should serialize as false.
        let (wire, _runtime) = cfg
            .into_wire(Some(SessionId::from("default-flags")))
            .expect("default config has no duplicate handlers");
        assert!(!wire.request_user_input);
        assert!(!wire.request_permission);
        assert!(!wire.request_elicitation);
        assert!(!wire.request_exit_plan_mode);
        assert!(!wire.request_auto_mode_switch);
        assert!(!wire.hooks);
        assert!(!wire.request_mcp_apps);
    }

    #[test]
    fn resume_session_config_new_wire_flags_off_without_handlers() {
        let cfg = ResumeSessionConfig::new(SessionId::from("resume-flags"));
        assert_eq!(cfg.mcp_oauth_token_storage, None);
        let (wire, _runtime) = cfg
            .into_wire()
            .expect("default resume config has no duplicate handlers");
        assert!(!wire.request_user_input);
        assert!(!wire.request_permission);
        assert!(!wire.request_elicitation);
        assert!(!wire.request_exit_plan_mode);
        assert!(!wire.request_auto_mode_switch);
        assert!(!wire.hooks);
        assert!(!wire.request_mcp_apps);
    }

    #[test]
    fn session_config_enable_mcp_apps_sets_wire_flag_and_serializes() {
        let cfg = SessionConfig::default().with_enable_mcp_apps(true);
        assert_eq!(cfg.enable_mcp_apps, Some(true));

        let (wire, _runtime) = cfg
            .into_wire(Some(SessionId::from("enable-mcp-apps")))
            .expect("enable_mcp_apps config has no duplicate handlers");
        assert!(wire.request_mcp_apps);

        let json = serde_json::to_value(&wire).unwrap();
        assert_eq!(json["requestMcpApps"], serde_json::Value::Bool(true));
    }

    #[test]
    fn resume_session_config_enable_mcp_apps_sets_wire_flag_and_serializes() {
        let cfg = ResumeSessionConfig::new(SessionId::from("resume-enable-mcp-apps"))
            .with_enable_mcp_apps(true);
        assert_eq!(cfg.enable_mcp_apps, Some(true));

        let (wire, _runtime) = cfg
            .into_wire()
            .expect("resume enable_mcp_apps config has no duplicate handlers");
        assert!(wire.request_mcp_apps);

        let json = serde_json::to_value(&wire).unwrap();
        assert_eq!(json["requestMcpApps"], serde_json::Value::Bool(true));
    }

    #[test]
    fn memory_configuration_constructors_and_serde() {
        assert!(MemoryConfiguration::enabled().enabled);
        assert!(!MemoryConfiguration::disabled().enabled);
        assert!(MemoryConfiguration::disabled().with_enabled(true).enabled);

        let json = serde_json::to_value(MemoryConfiguration::enabled()).unwrap();
        assert_eq!(json, serde_json::json!({ "enabled": true }));
    }

    #[test]
    fn session_config_with_memory_serializes() {
        let (wire, _runtime) = SessionConfig::default()
            .with_memory(MemoryConfiguration::enabled())
            .into_wire(Some(SessionId::from("memory-on")))
            .expect("no duplicate handlers");
        let json = serde_json::to_value(&wire).unwrap();
        assert_eq!(json["memory"], serde_json::json!({ "enabled": true }));

        let (wire_off, _) = SessionConfig::default()
            .with_memory(MemoryConfiguration::disabled())
            .into_wire(Some(SessionId::from("memory-off")))
            .expect("no duplicate handlers");
        let json_off = serde_json::to_value(&wire_off).unwrap();
        assert_eq!(json_off["memory"], serde_json::json!({ "enabled": false }));

        // Unset memory is omitted on the wire.
        let (empty_wire, _) = SessionConfig::default()
            .into_wire(Some(SessionId::from("memory-unset")))
            .expect("no duplicate handlers");
        let empty_json = serde_json::to_value(&empty_wire).unwrap();
        assert!(empty_json.get("memory").is_none());
    }

    #[test]
    fn resume_session_config_with_memory_serializes() {
        let (wire, _runtime) = ResumeSessionConfig::new(SessionId::from("resume-memory-on"))
            .with_memory(MemoryConfiguration::enabled())
            .into_wire()
            .expect("no duplicate handlers");
        let json = serde_json::to_value(&wire).unwrap();
        assert_eq!(json["memory"], serde_json::json!({ "enabled": true }));

        // Unset memory is omitted on the wire.
        let (empty_wire, _) = ResumeSessionConfig::new(SessionId::from("resume-memory-unset"))
            .into_wire()
            .expect("no duplicate handlers");
        let empty_json = serde_json::to_value(&empty_wire).unwrap();
        assert!(empty_json.get("memory").is_none());
    }

    #[test]
    #[allow(clippy::field_reassign_with_default)]
    fn session_config_into_wire_serializes_bucket_b_fields() {
        use std::path::PathBuf;

        use super::{CloudSessionOptions, CloudSessionRepository};

        let mut cfg = SessionConfig::default();
        cfg.config_directory = Some(PathBuf::from("/tmp/cfg"));
        cfg.working_directory = Some(PathBuf::from("/tmp/work"));
        cfg.github_token = Some("ghs_secret".to_string());
        cfg.include_sub_agent_streaming_events = Some(false);
        cfg.enable_session_telemetry = Some(false);
        cfg.reasoning_summary = Some(ReasoningSummary::Concise);
        cfg.remote_session = Some(crate::generated::api_types::RemoteSessionMode::Export);
        cfg.enable_on_demand_instruction_discovery = Some(false);
        cfg.cloud = Some(CloudSessionOptions::with_repository(
            CloudSessionRepository::new("github", "copilot-sdk").with_branch("main"),
        ));

        let (wire, _runtime) = cfg
            .into_wire(Some(SessionId::from("custom-id")))
            .expect("no duplicate handlers");
        let wire_json = serde_json::to_value(&wire).unwrap();
        assert_eq!(wire_json["sessionId"], "custom-id");
        assert_eq!(wire_json["configDir"], "/tmp/cfg");
        assert_eq!(wire_json["workingDirectory"], "/tmp/work");
        assert_eq!(wire_json["gitHubToken"], "ghs_secret");
        assert_eq!(wire_json["includeSubAgentStreamingEvents"], false);
        assert_eq!(wire_json["enableSessionTelemetry"], false);
        assert_eq!(wire_json["reasoningSummary"], "concise");
        assert_eq!(wire_json["remoteSession"], "export");
        assert_eq!(wire_json["enableOnDemandInstructionDiscovery"], false);
        assert_eq!(wire_json["cloud"]["repository"]["owner"], "github");
        assert_eq!(wire_json["cloud"]["repository"]["name"], "copilot-sdk");
        assert_eq!(wire_json["cloud"]["repository"]["branch"], "main");

        // Unset fields are omitted on the wire.
        let (empty_wire, _) = SessionConfig::default()
            .into_wire(Some(SessionId::from("empty")))
            .expect("default has no duplicate handlers");
        let empty_json = serde_json::to_value(&empty_wire).unwrap();
        assert!(empty_json.get("gitHubToken").is_none());
        assert!(empty_json.get("enableSessionTelemetry").is_none());
        assert!(empty_json.get("reasoningSummary").is_none());
        assert!(empty_json.get("remoteSession").is_none());
        assert!(
            empty_json
                .get("enableOnDemandInstructionDiscovery")
                .is_none()
        );
        assert!(empty_json.get("cloud").is_none());
    }

    #[test]
    fn session_config_into_wire_serializes_named_providers_and_models() {
        let cfg = SessionConfig::default()
            .with_providers(vec![
                NamedProviderConfig::new("my-openai", "https://api.example.com/v1")
                    .with_provider_type("openai")
                    .with_wire_api("responses")
                    .with_api_key("sk-test"),
            ])
            .with_models(vec![
                ProviderModelConfig::new("gpt-x", "my-openai")
                    .with_wire_model("gpt-x-2025")
                    .with_max_output_tokens(2048),
            ]);

        let (wire, _) = cfg
            .into_wire(Some(SessionId::from("sess-providers")))
            .expect("no duplicate handlers");
        let wire_json = serde_json::to_value(&wire).unwrap();
        assert_eq!(wire_json["providers"][0]["name"], "my-openai");
        assert_eq!(
            wire_json["providers"][0]["baseUrl"],
            "https://api.example.com/v1"
        );
        assert_eq!(wire_json["providers"][0]["type"], "openai");
        assert_eq!(wire_json["providers"][0]["wireApi"], "responses");
        assert_eq!(wire_json["providers"][0]["apiKey"], "sk-test");
        assert_eq!(wire_json["models"][0]["id"], "gpt-x");
        assert_eq!(wire_json["models"][0]["provider"], "my-openai");
        assert_eq!(wire_json["models"][0]["wireModel"], "gpt-x-2025");
        assert_eq!(wire_json["models"][0]["maxOutputTokens"], 2048);

        let (empty_wire, _) = SessionConfig::default()
            .into_wire(Some(SessionId::from("empty")))
            .expect("default has no duplicate handlers");
        let empty_json = serde_json::to_value(&empty_wire).unwrap();
        assert!(empty_json.get("providers").is_none());
        assert!(empty_json.get("models").is_none());
    }

    #[test]
    fn resume_config_into_wire_serializes_named_providers_and_models() {
        let cfg = ResumeSessionConfig::new(SessionId::from("sess-resume"))
            .with_providers(vec![
                NamedProviderConfig::new("my-azure", "https://example.openai.azure.com")
                    .with_provider_type("azure")
                    .with_azure(AzureProviderOptions {
                        api_version: Some("2024-10-21".to_string()),
                    }),
            ])
            .with_models(vec![
                ProviderModelConfig::new("deploy-1", "my-azure").with_model_id("gpt-4o"),
            ]);

        let (wire, _) = cfg.into_wire().expect("no duplicate handlers");
        let wire_json = serde_json::to_value(&wire).unwrap();
        assert_eq!(wire_json["providers"][0]["name"], "my-azure");
        assert_eq!(wire_json["providers"][0]["type"], "azure");
        assert_eq!(
            wire_json["providers"][0]["azure"]["apiVersion"],
            "2024-10-21"
        );
        assert_eq!(wire_json["models"][0]["id"], "deploy-1");
        assert_eq!(wire_json["models"][0]["provider"], "my-azure");
        assert_eq!(wire_json["models"][0]["modelId"], "gpt-4o");

        let (empty_wire, _) = ResumeSessionConfig::new(SessionId::from("empty"))
            .into_wire()
            .expect("default has no duplicate handlers");
        let empty_json = serde_json::to_value(&empty_wire).unwrap();
        assert!(empty_json.get("providers").is_none());
        assert!(empty_json.get("models").is_none());
    }

    #[test]
    fn session_config_into_wire_serializes_plugin_directories_and_large_output() {
        use std::path::PathBuf;

        let cfg = SessionConfig {
            plugin_directories: Some(vec![PathBuf::from("/tmp/plugins")]),
            large_output: Some(
                LargeToolOutputConfig::new()
                    .with_enabled(true)
                    .with_max_size_bytes(1024)
                    .with_output_directory(PathBuf::from("/tmp/large-output")),
            ),
            ..Default::default()
        };

        let (wire, _) = cfg
            .into_wire(Some(SessionId::from("sess-1")))
            .expect("no duplicate handlers");
        let wire_json = serde_json::to_value(&wire).unwrap();
        assert_eq!(wire_json["pluginDirectories"][0], "/tmp/plugins");
        assert_eq!(wire_json["largeOutput"]["enabled"], true);
        assert_eq!(wire_json["largeOutput"]["maxSizeBytes"], 1024);
        assert_eq!(wire_json["largeOutput"]["outputDir"], "/tmp/large-output");

        let (empty_wire, _) = SessionConfig::default()
            .into_wire(Some(SessionId::from("empty")))
            .expect("default has no duplicate handlers");
        let empty_json = serde_json::to_value(&empty_wire).unwrap();
        assert!(empty_json.get("pluginDirectories").is_none());
        assert!(empty_json.get("largeOutput").is_none());
    }

    #[test]
    fn resume_session_config_into_wire_serializes_bucket_b_fields() {
        use std::path::PathBuf;

        let mut cfg = ResumeSessionConfig::new(SessionId::from("sess-1"));
        cfg.working_directory = Some(PathBuf::from("/tmp/work"));
        cfg.config_directory = Some(PathBuf::from("/tmp/cfg"));
        cfg.github_token = Some("ghs_secret".to_string());
        cfg.include_sub_agent_streaming_events = Some(true);
        cfg.enable_session_telemetry = Some(false);
        cfg.reasoning_summary = Some(ReasoningSummary::Detailed);
        cfg.remote_session = Some(crate::generated::api_types::RemoteSessionMode::On);
        cfg.enable_on_demand_instruction_discovery = Some(false);

        let (wire, _) = cfg.into_wire().expect("no duplicate handlers");
        let wire_json = serde_json::to_value(&wire).unwrap();
        assert_eq!(wire_json["sessionId"], "sess-1");
        assert_eq!(wire_json["workingDirectory"], "/tmp/work");
        assert_eq!(wire_json["configDir"], "/tmp/cfg");
        assert_eq!(wire_json["gitHubToken"], "ghs_secret");
        assert_eq!(wire_json["includeSubAgentStreamingEvents"], true);
        assert_eq!(wire_json["enableSessionTelemetry"], false);
        assert_eq!(wire_json["reasoningSummary"], "detailed");
        assert_eq!(wire_json["remoteSession"], "on");
        assert_eq!(wire_json["enableOnDemandInstructionDiscovery"], false);

        // Unset remote_session is omitted on the wire.
        let (empty_wire, _) = ResumeSessionConfig::new(SessionId::from("sess-2"))
            .into_wire()
            .expect("default resume has no duplicate handlers");
        let empty_json = serde_json::to_value(&empty_wire).unwrap();
        assert!(empty_json.get("reasoningSummary").is_none());
        assert!(empty_json.get("remoteSession").is_none());
        assert!(
            empty_json
                .get("enableOnDemandInstructionDiscovery")
                .is_none()
        );
    }

    #[test]
    fn resume_session_config_into_wire_serializes_plugin_directories_and_large_output() {
        use std::path::PathBuf;

        let mut cfg = ResumeSessionConfig::new(SessionId::from("sess-1"));
        cfg.plugin_directories = Some(vec![PathBuf::from("/tmp/plugins-r")]);
        cfg.large_output = Some(
            LargeToolOutputConfig::new()
                .with_enabled(false)
                .with_max_size_bytes(2048)
                .with_output_directory(PathBuf::from("/tmp/large-output-r")),
        );

        let (wire, _) = cfg.into_wire().expect("no duplicate handlers");
        let wire_json = serde_json::to_value(&wire).unwrap();
        assert_eq!(wire_json["pluginDirectories"][0], "/tmp/plugins-r");
        assert_eq!(wire_json["largeOutput"]["enabled"], false);
        assert_eq!(wire_json["largeOutput"]["maxSizeBytes"], 2048);
        assert_eq!(wire_json["largeOutput"]["outputDir"], "/tmp/large-output-r");

        let (empty_wire, _) = ResumeSessionConfig::new(SessionId::from("sess-2"))
            .into_wire()
            .expect("default resume has no duplicate handlers");
        let empty_json = serde_json::to_value(&empty_wire).unwrap();
        assert!(empty_json.get("pluginDirectories").is_none());
        assert!(empty_json.get("largeOutput").is_none());
    }

    #[test]
    fn session_config_builder_composes() {
        use std::collections::HashMap;

        let cfg = SessionConfig::default()
            .with_session_id(SessionId::from("sess-1"))
            .with_model("claude-sonnet-4")
            .with_client_name("test-app")
            .with_reasoning_effort("medium")
            .with_reasoning_summary(ReasoningSummary::Concise)
            .with_context_tier("long_context")
            .with_streaming(true)
            .with_tools([Tool::new("greet")])
            .with_available_tools(["bash", "view"])
            .with_excluded_tools(["dangerous"])
            .with_mcp_servers(HashMap::new())
            .with_mcp_oauth_token_storage("persistent")
            .with_enable_config_discovery(true)
            .with_enable_on_demand_instruction_discovery(true)
            .with_skill_directories([PathBuf::from("/tmp/skills")])
            .with_disabled_skills(["broken-skill"])
            .with_agent("researcher")
            .with_config_directory(PathBuf::from("/tmp/config"))
            .with_working_directory(PathBuf::from("/tmp/work"))
            .with_github_token("ghp_test")
            .with_enable_session_telemetry(false)
            .with_include_sub_agent_streaming_events(false)
            .with_extension_info(ExtensionInfo::new("github-app", "counter"));

        assert_eq!(cfg.session_id.as_ref().map(|s| s.as_str()), Some("sess-1"));
        assert_eq!(cfg.model.as_deref(), Some("claude-sonnet-4"));
        assert_eq!(cfg.client_name.as_deref(), Some("test-app"));
        assert_eq!(cfg.reasoning_effort.as_deref(), Some("medium"));
        assert_eq!(cfg.reasoning_summary, Some(ReasoningSummary::Concise));
        assert_eq!(cfg.context_tier.as_deref(), Some("long_context"));
        assert_eq!(cfg.streaming, Some(true));
        assert_eq!(cfg.tools.as_ref().map(|t| t.len()), Some(1));
        assert_eq!(
            cfg.available_tools.as_deref(),
            Some(&["bash".to_string(), "view".to_string()][..])
        );
        assert_eq!(
            cfg.excluded_tools.as_deref(),
            Some(&["dangerous".to_string()][..])
        );
        assert!(cfg.mcp_servers.is_some());
        assert_eq!(cfg.mcp_oauth_token_storage.as_deref(), Some("persistent"));
        assert_eq!(cfg.enable_config_discovery, Some(true));
        assert_eq!(cfg.enable_on_demand_instruction_discovery, Some(true));
        assert_eq!(
            cfg.skill_directories.as_deref(),
            Some(&[PathBuf::from("/tmp/skills")][..])
        );
        assert_eq!(
            cfg.disabled_skills.as_deref(),
            Some(&["broken-skill".to_string()][..])
        );
        assert_eq!(cfg.agent.as_deref(), Some("researcher"));
        assert_eq!(cfg.config_directory, Some(PathBuf::from("/tmp/config")));
        assert_eq!(cfg.working_directory, Some(PathBuf::from("/tmp/work")));
        assert_eq!(cfg.github_token.as_deref(), Some("ghp_test"));
        assert_eq!(cfg.enable_session_telemetry, Some(false));
        assert_eq!(cfg.include_sub_agent_streaming_events, Some(false));
        assert_eq!(
            cfg.extension_info,
            Some(ExtensionInfo::new("github-app", "counter"))
        );
    }

    #[test]
    fn resume_session_config_builder_composes() {
        use std::collections::HashMap;

        let cfg = ResumeSessionConfig::new(SessionId::from("sess-2"))
            .with_client_name("test-app")
            .with_reasoning_summary(ReasoningSummary::None)
            .with_context_tier("default")
            .with_streaming(true)
            .with_tools([Tool::new("greet")])
            .with_available_tools(["bash", "view"])
            .with_excluded_tools(["dangerous"])
            .with_mcp_servers(HashMap::new())
            .with_mcp_oauth_token_storage("persistent")
            .with_enable_config_discovery(true)
            .with_enable_on_demand_instruction_discovery(false)
            .with_skill_directories([PathBuf::from("/tmp/skills")])
            .with_disabled_skills(["broken-skill"])
            .with_agent("researcher")
            .with_config_directory(PathBuf::from("/tmp/config"))
            .with_working_directory(PathBuf::from("/tmp/work"))
            .with_github_token("ghp_test")
            .with_enable_session_telemetry(false)
            .with_include_sub_agent_streaming_events(true)
            .with_suppress_resume_event(true)
            .with_continue_pending_work(true)
            .with_extension_info(ExtensionInfo::new("github-app", "counter"));

        assert_eq!(cfg.session_id.as_str(), "sess-2");
        assert_eq!(cfg.client_name.as_deref(), Some("test-app"));
        assert_eq!(cfg.reasoning_summary, Some(ReasoningSummary::None));
        assert_eq!(cfg.context_tier.as_deref(), Some("default"));
        assert_eq!(cfg.streaming, Some(true));
        assert_eq!(cfg.tools.as_ref().map(|t| t.len()), Some(1));
        assert_eq!(
            cfg.available_tools.as_deref(),
            Some(&["bash".to_string(), "view".to_string()][..])
        );
        assert_eq!(
            cfg.excluded_tools.as_deref(),
            Some(&["dangerous".to_string()][..])
        );
        assert!(cfg.mcp_servers.is_some());
        assert_eq!(cfg.mcp_oauth_token_storage.as_deref(), Some("persistent"));
        assert_eq!(cfg.enable_config_discovery, Some(true));
        assert_eq!(cfg.enable_on_demand_instruction_discovery, Some(false));
        assert_eq!(
            cfg.skill_directories.as_deref(),
            Some(&[PathBuf::from("/tmp/skills")][..])
        );
        assert_eq!(
            cfg.disabled_skills.as_deref(),
            Some(&["broken-skill".to_string()][..])
        );
        assert_eq!(cfg.agent.as_deref(), Some("researcher"));
        assert_eq!(cfg.config_directory, Some(PathBuf::from("/tmp/config")));
        assert_eq!(cfg.working_directory, Some(PathBuf::from("/tmp/work")));
        assert_eq!(cfg.github_token.as_deref(), Some("ghp_test"));
        assert_eq!(cfg.enable_session_telemetry, Some(false));
        assert_eq!(cfg.include_sub_agent_streaming_events, Some(true));
        assert_eq!(cfg.suppress_resume_event, Some(true));
        assert_eq!(cfg.continue_pending_work, Some(true));
        assert_eq!(
            cfg.extension_info,
            Some(ExtensionInfo::new("github-app", "counter"))
        );
    }

    /// `continue_pending_work` must serialize to wire as `continuePendingWork`
    /// — the runtime keys off this exact field name to opt into the
    /// pending-work-handoff pattern.
    #[test]
    fn resume_session_config_serializes_continue_pending_work_to_camel_case() {
        let cfg =
            ResumeSessionConfig::new(SessionId::from("sess-1")).with_continue_pending_work(true);
        let (wire, _) = cfg.into_wire().expect("no duplicate handlers");
        let json = serde_json::to_value(&wire).unwrap();
        assert_eq!(json["continuePendingWork"], true);

        // Unset case — skip_serializing_if must omit the field.
        let (wire, _) = ResumeSessionConfig::new(SessionId::from("sess-2"))
            .into_wire()
            .expect("no duplicate handlers");
        let json = serde_json::to_value(&wire).unwrap();
        assert!(json.get("continuePendingWork").is_none());
    }

    /// The Rust field is `suppress_resume_event`, but the wire field stays
    /// `disableResume` to preserve compatibility with the runtime and other
    /// SDKs.
    #[test]
    fn resume_session_config_serializes_suppress_resume_event_to_disable_resume_on_wire() {
        let cfg =
            ResumeSessionConfig::new(SessionId::from("sess-1")).with_suppress_resume_event(true);
        let (wire, _) = cfg.into_wire().expect("no duplicate handlers");
        let json = serde_json::to_value(&wire).unwrap();
        assert_eq!(json["disableResume"], true);
        assert!(json.get("suppressResumeEvent").is_none());
    }

    /// `instruction_directories` must serialize to wire as
    /// `instructionDirectories` on `SessionConfig`.
    #[test]
    fn session_config_serializes_instruction_directories_to_camel_case() {
        let cfg =
            SessionConfig::default().with_instruction_directories([PathBuf::from("/tmp/instr")]);
        let (wire, _) = cfg
            .into_wire(Some(SessionId::from("instr-on")))
            .expect("no duplicate handlers");
        let json = serde_json::to_value(&wire).unwrap();
        assert_eq!(
            json["instructionDirectories"],
            serde_json::json!(["/tmp/instr"])
        );

        // Unset case — skip_serializing_if must omit the field.
        let (wire, _) = SessionConfig::default()
            .into_wire(Some(SessionId::from("instr-off")))
            .expect("no duplicate handlers");
        let json = serde_json::to_value(&wire).unwrap();
        assert!(json.get("instructionDirectories").is_none());
    }

    /// Same check on the resume path. Forwarded to the CLI on
    /// `session.resume`.
    #[test]
    fn resume_session_config_serializes_instruction_directories_to_camel_case() {
        let cfg = ResumeSessionConfig::new(SessionId::from("sess-1"))
            .with_instruction_directories([PathBuf::from("/tmp/instr")]);
        let (wire, _) = cfg.into_wire().expect("no duplicate handlers");
        let json = serde_json::to_value(&wire).unwrap();
        assert_eq!(
            json["instructionDirectories"],
            serde_json::json!(["/tmp/instr"])
        );

        let (wire, _) = ResumeSessionConfig::new(SessionId::from("sess-2"))
            .into_wire()
            .expect("no duplicate handlers");
        let json = serde_json::to_value(&wire).unwrap();
        assert!(json.get("instructionDirectories").is_none());
    }

    #[test]
    fn custom_agent_config_builder_composes() {
        use std::collections::HashMap;

        let cfg = CustomAgentConfig::new("researcher", "You are a research assistant.")
            .with_display_name("Research Assistant")
            .with_description("Investigates technical questions.")
            .with_tools(["bash", "view"])
            .with_mcp_servers(HashMap::new())
            .with_infer(true)
            .with_skills(["rust-coding-skill"]);

        assert_eq!(cfg.name, "researcher");
        assert_eq!(cfg.prompt, "You are a research assistant.");
        assert_eq!(cfg.display_name.as_deref(), Some("Research Assistant"));
        assert_eq!(
            cfg.description.as_deref(),
            Some("Investigates technical questions.")
        );
        assert_eq!(
            cfg.tools.as_deref(),
            Some(&["bash".to_string(), "view".to_string()][..])
        );
        assert!(cfg.mcp_servers.is_some());
        assert_eq!(cfg.infer, Some(true));
        assert_eq!(
            cfg.skills.as_deref(),
            Some(&["rust-coding-skill".to_string()][..])
        );
    }

    #[test]
    fn infinite_session_config_builder_composes() {
        let cfg = InfiniteSessionConfig::new()
            .with_enabled(true)
            .with_background_compaction_threshold(0.75)
            .with_buffer_exhaustion_threshold(0.92);

        assert_eq!(cfg.enabled, Some(true));
        assert_eq!(cfg.background_compaction_threshold, Some(0.75));
        assert_eq!(cfg.buffer_exhaustion_threshold, Some(0.92));
    }

    #[test]
    fn provider_config_builder_composes() {
        use std::collections::HashMap;

        let mut headers = HashMap::new();
        headers.insert("X-Custom".to_string(), "value".to_string());

        let cfg = ProviderConfig::new("https://api.example.com")
            .with_provider_type("openai")
            .with_wire_api("completions")
            .with_api_key("sk-test")
            .with_bearer_token("bearer-test")
            .with_headers(headers)
            .with_model_id("gpt-4")
            .with_wire_model("azure-gpt-4-deployment")
            .with_max_prompt_tokens(8192)
            .with_max_output_tokens(2048);

        assert_eq!(cfg.base_url, "https://api.example.com");
        assert_eq!(cfg.provider_type.as_deref(), Some("openai"));
        assert_eq!(cfg.wire_api.as_deref(), Some("completions"));
        assert_eq!(cfg.api_key.as_deref(), Some("sk-test"));
        assert_eq!(cfg.bearer_token.as_deref(), Some("bearer-test"));
        assert_eq!(
            cfg.headers
                .as_ref()
                .and_then(|h| h.get("X-Custom"))
                .map(String::as_str),
            Some("value"),
        );
        assert_eq!(cfg.model_id.as_deref(), Some("gpt-4"));
        assert_eq!(cfg.wire_model.as_deref(), Some("azure-gpt-4-deployment"));
        assert_eq!(cfg.max_prompt_tokens, Some(8192));
        assert_eq!(cfg.max_output_tokens, Some(2048));

        // Wire-shape: camelCase, skip_serializing_if when unset.
        let wire = serde_json::to_value(&cfg).unwrap();
        assert_eq!(wire["modelId"], "gpt-4");
        assert_eq!(wire["wireModel"], "azure-gpt-4-deployment");
        assert_eq!(wire["maxPromptTokens"], 8192);
        assert_eq!(wire["maxOutputTokens"], 2048);

        let unset = ProviderConfig::new("https://api.example.com");
        let wire_unset = serde_json::to_value(&unset).unwrap();
        assert!(wire_unset.get("modelId").is_none());
        assert!(wire_unset.get("wireModel").is_none());
        assert!(wire_unset.get("maxPromptTokens").is_none());
        assert!(wire_unset.get("maxOutputTokens").is_none());
    }

    #[test]
    fn system_message_config_builder_composes() {
        use std::collections::HashMap;

        let cfg = SystemMessageConfig::new()
            .with_mode("replace")
            .with_content("Custom system message.")
            .with_sections(HashMap::new());

        assert_eq!(cfg.mode.as_deref(), Some("replace"));
        assert_eq!(cfg.content.as_deref(), Some("Custom system message."));
        assert!(cfg.sections.is_some());
    }

    #[test]
    fn delivery_mode_serializes_to_kebab_case_strings() {
        assert_eq!(
            serde_json::to_string(&DeliveryMode::Enqueue).unwrap(),
            "\"enqueue\""
        );
        assert_eq!(
            serde_json::to_string(&DeliveryMode::Immediate).unwrap(),
            "\"immediate\""
        );
        let parsed: DeliveryMode = serde_json::from_str("\"immediate\"").unwrap();
        assert_eq!(parsed, DeliveryMode::Immediate);
    }

    #[test]
    fn agent_mode_serializes_to_kebab_case_strings() {
        assert_eq!(
            serde_json::to_string(&AgentMode::Interactive).unwrap(),
            "\"interactive\""
        );
        assert_eq!(serde_json::to_string(&AgentMode::Plan).unwrap(), "\"plan\"");
        assert_eq!(
            serde_json::to_string(&AgentMode::Autopilot).unwrap(),
            "\"autopilot\""
        );
        assert_eq!(
            serde_json::to_string(&AgentMode::Shell).unwrap(),
            "\"shell\""
        );
        let parsed: AgentMode = serde_json::from_str("\"plan\"").unwrap();
        assert_eq!(parsed, AgentMode::Plan);
    }

    #[test]
    fn connection_state_distinguishes_variants() {
        // ConnectionState is now an internal type; verify we can construct
        // and compare the variants used by the lifecycle code paths.
        assert_ne!(ConnectionState::Connected, ConnectionState::Disconnected);
    }

    /// `agentId` is the sub-agent attribution field added in copilot-sdk
    /// commit f8cf846 ("Derive session event envelopes from schema").
    /// Every other SDK (Node, Python, Go, .NET) carries it on the event
    /// envelope; Rust must too or sub-agent events lose attribution at
    /// the deserialization boundary. Cross-SDK parity test.
    #[test]
    fn session_event_round_trips_agent_id_on_envelope() {
        let wire = json!({
            "id": "evt-1",
            "timestamp": "2026-04-30T12:00:00Z",
            "parentId": null,
            "agentId": "sub-agent-42",
            "type": "assistant.message",
            "data": { "message": "hi" }
        });

        let event: SessionEvent = serde_json::from_value(wire.clone()).unwrap();
        assert_eq!(event.agent_id.as_deref(), Some("sub-agent-42"));

        // Round-trip preserves the field on the wire.
        let roundtripped = serde_json::to_value(&event).unwrap();
        assert_eq!(roundtripped["agentId"], "sub-agent-42");

        // Absent agentId remains absent (skip_serializing_if).
        let main_agent_event: SessionEvent = serde_json::from_value(json!({
            "id": "evt-2",
            "timestamp": "2026-04-30T12:00:01Z",
            "parentId": null,
            "type": "session.idle",
            "data": {}
        }))
        .unwrap();
        assert!(main_agent_event.agent_id.is_none());
        let roundtripped = serde_json::to_value(&main_agent_event).unwrap();
        assert!(roundtripped.get("agentId").is_none());
    }

    /// Same parity for the typed event envelope produced by the codegen.
    #[test]
    fn typed_session_event_round_trips_agent_id_on_envelope() {
        let wire = json!({
            "id": "evt-1",
            "timestamp": "2026-04-30T12:00:00Z",
            "parentId": null,
            "agentId": "sub-agent-42",
            "type": "session.idle",
            "data": {}
        });

        let event: TypedSessionEvent = serde_json::from_value(wire).unwrap();
        assert_eq!(event.agent_id.as_deref(), Some("sub-agent-42"));

        let roundtripped = serde_json::to_value(&event).unwrap();
        assert_eq!(roundtripped["agentId"], "sub-agent-42");
    }

    #[test]
    fn connection_state_variants_compile() {
        // Defensive smoke test: all variants must be constructable from
        // within the crate. (The enum was demoted from pub to pub(crate)
        // in Phase D; this test guards against accidental removal.)
        let _ = ConnectionState::Disconnected;
        let _ = ConnectionState::Connecting;
        let _ = ConnectionState::Connected;
        let _ = ConnectionState::Error;
    }

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
                "url": "https://github.com/example/repo/issues/42"
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
                && url == "https://github.com/example/repo/issues/42"
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

    use crate::handler::{ApproveAllHandler, PermissionHandler, PermissionResult};
    use crate::permission;
    use crate::types::{
        PermissionDecision, PermissionRequestData, RequestId, ResumeSessionConfig, SessionConfig,
        SessionId,
    };

    fn data() -> PermissionRequestData {
        PermissionRequestData {
            extra: serde_json::json!({"tool": "shell"}),
            ..Default::default()
        }
    }

    /// Apply the same policy-resolution logic that `Client::create_session`
    /// uses, so tests exercise the effective handler.
    fn resolve_create(mut cfg: SessionConfig) -> Option<Arc<dyn PermissionHandler>> {
        permission::resolve_handler(cfg.permission_handler.take(), cfg.permission_policy.take())
    }

    fn resolve_resume(mut cfg: ResumeSessionConfig) -> Option<Arc<dyn PermissionHandler>> {
        permission::resolve_handler(cfg.permission_handler.take(), cfg.permission_policy.take())
    }

    async fn dispatch(handler: &Arc<dyn PermissionHandler>) -> PermissionResult {
        handler
            .handle(SessionId::from("s1"), RequestId::new("1"), data())
            .await
    }

    #[tokio::test]
    async fn approve_all_with_handler_present_approves() {
        let cfg = SessionConfig::default()
            .with_permission_handler(Arc::new(ApproveAllHandler))
            .approve_all_permissions();
        let h = resolve_create(cfg).expect("policy + handler yields handler");
        assert!(matches!(
            dispatch(&h).await,
            PermissionResult::Decision(PermissionDecision::ApproveOnce(_))
        ));
    }

    #[tokio::test]
    async fn approve_all_standalone_produces_handler() {
        let cfg = SessionConfig::default().approve_all_permissions();
        let h = resolve_create(cfg).expect("policy alone yields handler");
        assert!(matches!(
            dispatch(&h).await,
            PermissionResult::Decision(PermissionDecision::ApproveOnce(_))
        ));
    }

    /// Phase I: order between with_permission_handler and the policy
    /// builder must not matter.
    #[tokio::test]
    async fn approve_all_is_order_independent() {
        let a = SessionConfig::default()
            .with_permission_handler(Arc::new(ApproveAllHandler))
            .approve_all_permissions();
        let b = SessionConfig::default()
            .approve_all_permissions()
            .with_permission_handler(Arc::new(ApproveAllHandler));
        let ha = resolve_create(a).unwrap();
        let hb = resolve_create(b).unwrap();
        assert!(matches!(
            dispatch(&ha).await,
            PermissionResult::Decision(PermissionDecision::ApproveOnce(_))
        ));
        assert!(matches!(
            dispatch(&hb).await,
            PermissionResult::Decision(PermissionDecision::ApproveOnce(_))
        ));
    }

    #[tokio::test]
    async fn deny_all_is_order_independent() {
        let a = SessionConfig::default()
            .with_permission_handler(Arc::new(ApproveAllHandler))
            .deny_all_permissions();
        let b = SessionConfig::default()
            .deny_all_permissions()
            .with_permission_handler(Arc::new(ApproveAllHandler));
        let ha = resolve_create(a).unwrap();
        let hb = resolve_create(b).unwrap();
        assert!(matches!(
            dispatch(&ha).await,
            PermissionResult::Decision(PermissionDecision::Reject(_))
        ));
        assert!(matches!(
            dispatch(&hb).await,
            PermissionResult::Decision(PermissionDecision::Reject(_))
        ));
    }

    #[tokio::test]
    async fn approve_permissions_if_consults_predicate() {
        let cfg = SessionConfig::default().approve_permissions_if(|d| {
            d.extra.get("tool").and_then(|v| v.as_str()) != Some("shell")
        });
        let h = resolve_create(cfg).unwrap();
        assert!(matches!(
            dispatch(&h).await,
            PermissionResult::Decision(PermissionDecision::Reject(_))
        ));
    }

    #[tokio::test]
    async fn approve_permissions_if_is_order_independent() {
        let predicate = |d: &PermissionRequestData| {
            d.extra.get("tool").and_then(|v| v.as_str()) != Some("shell")
        };
        let a = SessionConfig::default()
            .with_permission_handler(Arc::new(ApproveAllHandler))
            .approve_permissions_if(predicate);
        let b = SessionConfig::default()
            .approve_permissions_if(predicate)
            .with_permission_handler(Arc::new(ApproveAllHandler));
        let ha = resolve_create(a).unwrap();
        let hb = resolve_create(b).unwrap();
        assert!(matches!(
            dispatch(&ha).await,
            PermissionResult::Decision(PermissionDecision::Reject(_))
        ));
        assert!(matches!(
            dispatch(&hb).await,
            PermissionResult::Decision(PermissionDecision::Reject(_))
        ));
    }

    #[tokio::test]
    async fn resume_session_config_approve_all_works() {
        let cfg = ResumeSessionConfig::new(SessionId::from("s1"))
            .with_permission_handler(Arc::new(ApproveAllHandler))
            .approve_all_permissions();
        let h = resolve_resume(cfg).unwrap();
        assert!(matches!(
            dispatch(&h).await,
            PermissionResult::Decision(PermissionDecision::ApproveOnce(_))
        ));
    }

    #[tokio::test]
    async fn resume_session_config_approve_all_is_order_independent() {
        let a = ResumeSessionConfig::new(SessionId::from("s1"))
            .with_permission_handler(Arc::new(ApproveAllHandler))
            .approve_all_permissions();
        let b = ResumeSessionConfig::new(SessionId::from("s1"))
            .approve_all_permissions()
            .with_permission_handler(Arc::new(ApproveAllHandler));
        let ha = resolve_resume(a).unwrap();
        let hb = resolve_resume(b).unwrap();
        assert!(matches!(
            dispatch(&ha).await,
            PermissionResult::Decision(PermissionDecision::ApproveOnce(_))
        ));
        assert!(matches!(
            dispatch(&hb).await,
            PermissionResult::Decision(PermissionDecision::ApproveOnce(_))
        ));
    }
}
