//! Auto-generated from api.schema.json — do not edit manually.

#![allow(clippy::large_enum_variant)]

use std::collections::HashMap;

use serde::{Deserialize, Serialize};

/// JSON-RPC method name constants.
pub mod rpc_methods {
    /// `ping`
    pub const PING: &str = "ping";
    /// `models.list`
    pub const MODELS_LIST: &str = "models.list";
    /// `tools.list`
    pub const TOOLS_LIST: &str = "tools.list";
    /// `account.getQuota`
    pub const ACCOUNT_GETQUOTA: &str = "account.getQuota";
    /// `mcp.config.list`
    pub const MCP_CONFIG_LIST: &str = "mcp.config.list";
    /// `mcp.config.add`
    pub const MCP_CONFIG_ADD: &str = "mcp.config.add";
    /// `mcp.config.update`
    pub const MCP_CONFIG_UPDATE: &str = "mcp.config.update";
    /// `mcp.config.remove`
    pub const MCP_CONFIG_REMOVE: &str = "mcp.config.remove";
    /// `mcp.config.enable`
    pub const MCP_CONFIG_ENABLE: &str = "mcp.config.enable";
    /// `mcp.config.disable`
    pub const MCP_CONFIG_DISABLE: &str = "mcp.config.disable";
    /// `mcp.discover`
    pub const MCP_DISCOVER: &str = "mcp.discover";
    /// `skills.config.setDisabledSkills`
    pub const SKILLS_CONFIG_SETDISABLEDSKILLS: &str = "skills.config.setDisabledSkills";
    /// `skills.discover`
    pub const SKILLS_DISCOVER: &str = "skills.discover";
    /// `sessionFs.setProvider`
    pub const SESSIONFS_SETPROVIDER: &str = "sessionFs.setProvider";
    /// `sessions.fork`
    pub const SESSIONS_FORK: &str = "sessions.fork";
    /// `session.auth.getStatus`
    pub const SESSION_AUTH_GETSTATUS: &str = "session.auth.getStatus";
    /// `session.model.getCurrent`
    pub const SESSION_MODEL_GETCURRENT: &str = "session.model.getCurrent";
    /// `session.model.switchTo`
    pub const SESSION_MODEL_SWITCHTO: &str = "session.model.switchTo";
    /// `session.mode.get`
    pub const SESSION_MODE_GET: &str = "session.mode.get";
    /// `session.mode.set`
    pub const SESSION_MODE_SET: &str = "session.mode.set";
    /// `session.name.get`
    pub const SESSION_NAME_GET: &str = "session.name.get";
    /// `session.name.set`
    pub const SESSION_NAME_SET: &str = "session.name.set";
    /// `session.plan.read`
    pub const SESSION_PLAN_READ: &str = "session.plan.read";
    /// `session.plan.update`
    pub const SESSION_PLAN_UPDATE: &str = "session.plan.update";
    /// `session.plan.delete`
    pub const SESSION_PLAN_DELETE: &str = "session.plan.delete";
    /// `session.workspaces.getWorkspace`
    pub const SESSION_WORKSPACES_GETWORKSPACE: &str = "session.workspaces.getWorkspace";
    /// `session.workspaces.listFiles`
    pub const SESSION_WORKSPACES_LISTFILES: &str = "session.workspaces.listFiles";
    /// `session.workspaces.readFile`
    pub const SESSION_WORKSPACES_READFILE: &str = "session.workspaces.readFile";
    /// `session.workspaces.createFile`
    pub const SESSION_WORKSPACES_CREATEFILE: &str = "session.workspaces.createFile";
    /// `session.instructions.getSources`
    pub const SESSION_INSTRUCTIONS_GETSOURCES: &str = "session.instructions.getSources";
    /// `session.fleet.start`
    pub const SESSION_FLEET_START: &str = "session.fleet.start";
    /// `session.agent.list`
    pub const SESSION_AGENT_LIST: &str = "session.agent.list";
    /// `session.agent.getCurrent`
    pub const SESSION_AGENT_GETCURRENT: &str = "session.agent.getCurrent";
    /// `session.agent.select`
    pub const SESSION_AGENT_SELECT: &str = "session.agent.select";
    /// `session.agent.deselect`
    pub const SESSION_AGENT_DESELECT: &str = "session.agent.deselect";
    /// `session.agent.reload`
    pub const SESSION_AGENT_RELOAD: &str = "session.agent.reload";
    /// `session.skills.list`
    pub const SESSION_SKILLS_LIST: &str = "session.skills.list";
    /// `session.skills.enable`
    pub const SESSION_SKILLS_ENABLE: &str = "session.skills.enable";
    /// `session.skills.disable`
    pub const SESSION_SKILLS_DISABLE: &str = "session.skills.disable";
    /// `session.skills.reload`
    pub const SESSION_SKILLS_RELOAD: &str = "session.skills.reload";
    /// `session.mcp.list`
    pub const SESSION_MCP_LIST: &str = "session.mcp.list";
    /// `session.mcp.enable`
    pub const SESSION_MCP_ENABLE: &str = "session.mcp.enable";
    /// `session.mcp.disable`
    pub const SESSION_MCP_DISABLE: &str = "session.mcp.disable";
    /// `session.mcp.reload`
    pub const SESSION_MCP_RELOAD: &str = "session.mcp.reload";
    /// `session.mcp.oauth.login`
    pub const SESSION_MCP_OAUTH_LOGIN: &str = "session.mcp.oauth.login";
    /// `session.plugins.list`
    pub const SESSION_PLUGINS_LIST: &str = "session.plugins.list";
    /// `session.extensions.list`
    pub const SESSION_EXTENSIONS_LIST: &str = "session.extensions.list";
    /// `session.extensions.enable`
    pub const SESSION_EXTENSIONS_ENABLE: &str = "session.extensions.enable";
    /// `session.extensions.disable`
    pub const SESSION_EXTENSIONS_DISABLE: &str = "session.extensions.disable";
    /// `session.extensions.reload`
    pub const SESSION_EXTENSIONS_RELOAD: &str = "session.extensions.reload";
    /// `session.tools.handlePendingToolCall`
    pub const SESSION_TOOLS_HANDLEPENDINGTOOLCALL: &str = "session.tools.handlePendingToolCall";
    /// `session.commands.handlePendingCommand`
    pub const SESSION_COMMANDS_HANDLEPENDINGCOMMAND: &str = "session.commands.handlePendingCommand";
    /// `session.ui.elicitation`
    pub const SESSION_UI_ELICITATION: &str = "session.ui.elicitation";
    /// `session.ui.handlePendingElicitation`
    pub const SESSION_UI_HANDLEPENDINGELICITATION: &str = "session.ui.handlePendingElicitation";
    /// `session.permissions.handlePendingPermissionRequest`
    pub const SESSION_PERMISSIONS_HANDLEPENDINGPERMISSIONREQUEST: &str =
        "session.permissions.handlePendingPermissionRequest";
    /// `session.permissions.setApproveAll`
    pub const SESSION_PERMISSIONS_SETAPPROVEALL: &str = "session.permissions.setApproveAll";
    /// `session.permissions.resetSessionApprovals`
    pub const SESSION_PERMISSIONS_RESETSESSIONAPPROVALS: &str =
        "session.permissions.resetSessionApprovals";
    /// `session.log`
    pub const SESSION_LOG: &str = "session.log";
    /// `session.shell.exec`
    pub const SESSION_SHELL_EXEC: &str = "session.shell.exec";
    /// `session.shell.kill`
    pub const SESSION_SHELL_KILL: &str = "session.shell.kill";
    /// `session.history.compact`
    pub const SESSION_HISTORY_COMPACT: &str = "session.history.compact";
    /// `session.history.truncate`
    pub const SESSION_HISTORY_TRUNCATE: &str = "session.history.truncate";
    /// `session.usage.getMetrics`
    pub const SESSION_USAGE_GETMETRICS: &str = "session.usage.getMetrics";
    /// `sessionFs.readFile`
    pub const SESSIONFS_READFILE: &str = "sessionFs.readFile";
    /// `sessionFs.writeFile`
    pub const SESSIONFS_WRITEFILE: &str = "sessionFs.writeFile";
    /// `sessionFs.appendFile`
    pub const SESSIONFS_APPENDFILE: &str = "sessionFs.appendFile";
    /// `sessionFs.exists`
    pub const SESSIONFS_EXISTS: &str = "sessionFs.exists";
    /// `sessionFs.stat`
    pub const SESSIONFS_STAT: &str = "sessionFs.stat";
    /// `sessionFs.mkdir`
    pub const SESSIONFS_MKDIR: &str = "sessionFs.mkdir";
    /// `sessionFs.readdir`
    pub const SESSIONFS_READDIR: &str = "sessionFs.readdir";
    /// `sessionFs.readdirWithTypes`
    pub const SESSIONFS_READDIRWITHTYPES: &str = "sessionFs.readdirWithTypes";
    /// `sessionFs.rm`
    pub const SESSIONFS_RM: &str = "sessionFs.rm";
    /// `sessionFs.rename`
    pub const SESSIONFS_RENAME: &str = "sessionFs.rename";
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountGetQuotaRequest {
    /// GitHub token for per-user quota lookup. When provided, resolves this token to determine the user's quota instead of using the global auth.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_hub_token: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountQuotaSnapshot {
    /// Number of requests included in the entitlement
    pub entitlement_requests: i64,
    /// Whether the user has an unlimited usage entitlement
    pub is_unlimited_entitlement: bool,
    /// Number of overage requests made this period
    pub overage: f64,
    /// Whether overage is allowed when quota is exhausted
    pub overage_allowed_with_exhausted_quota: bool,
    /// Percentage of entitlement remaining
    pub remaining_percentage: f64,
    /// Date when the quota resets (ISO 8601 string)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reset_date: Option<String>,
    /// Whether usage is still permitted after quota exhaustion
    pub usage_allowed_with_exhausted_quota: bool,
    /// Number of requests used so far this period
    pub used_requests: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountGetQuotaResult {
    /// Quota snapshots keyed by type (e.g., chat, completions, premium_interactions)
    pub quota_snapshots: HashMap<String, AccountQuotaSnapshot>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentInfo {
    /// Description of the agent's purpose
    pub description: String,
    /// Human-readable display name
    pub display_name: String,
    /// Unique identifier of the custom agent
    pub name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentGetCurrentResult {
    /// Currently selected custom agent, or null if using the default agent
    pub agent: AgentInfo,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentList {
    /// Available custom agents
    pub agents: Vec<AgentInfo>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentReloadResult {
    /// Reloaded custom agents
    pub agents: Vec<AgentInfo>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentSelectRequest {
    /// Name of the custom agent to select
    pub name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentSelectResult {
    /// The newly selected custom agent
    pub agent: AgentInfo,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsHandlePendingCommandRequest {
    /// Error message if the command handler failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Request ID from the command invocation event
    pub request_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsHandlePendingCommandResult {
    /// Whether the command was handled successfully
    pub success: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CurrentModel {
    /// Currently active model identifier
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DiscoveredMcpServer {
    /// Whether the server is enabled (not in the disabled list)
    pub enabled: bool,
    /// Server name (config key)
    pub name: String,
    /// Configuration source
    pub source: DiscoveredMcpServerSource,
    /// Server transport type: stdio, http, sse, or memory (local configs are normalized to stdio)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<DiscoveredMcpServerType>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Extension {
    /// Source-qualified ID (e.g., 'project:my-ext', 'user:auth-helper')
    pub id: String,
    /// Extension name (directory name)
    pub name: String,
    /// Process ID if the extension is running
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pid: Option<i64>,
    /// Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/)
    pub source: ExtensionSource,
    /// Current status: running, disabled, failed, or starting
    pub status: ExtensionStatus,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionList {
    /// Discovered extensions and their current status
    pub extensions: Vec<Extension>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionsDisableRequest {
    /// Source-qualified extension ID to disable
    pub id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionsEnableRequest {
    /// Source-qualified extension ID to enable
    pub id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FleetStartRequest {
    /// Optional user prompt to combine with fleet instructions
    #[serde(skip_serializing_if = "Option::is_none")]
    pub prompt: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FleetStartResult {
    /// Whether fleet mode was successfully activated
    pub started: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HandleToolCallResult {
    /// Whether the tool call result was handled successfully
    pub success: bool,
}

/// Post-compaction context window usage breakdown
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryCompactContextWindow {
    /// Token count from non-system messages (user, assistant, tool)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub conversation_tokens: Option<i64>,
    /// Current total tokens in the context window (system + conversation + tool definitions)
    pub current_tokens: i64,
    /// Current number of messages in the conversation
    pub messages_length: i64,
    /// Token count from system message(s)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub system_tokens: Option<i64>,
    /// Maximum token count for the model's context window
    pub token_limit: i64,
    /// Token count from tool definitions
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_definitions_tokens: Option<i64>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryCompactResult {
    /// Post-compaction context window usage breakdown
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_window: Option<HistoryCompactContextWindow>,
    /// Number of messages removed during compaction
    pub messages_removed: i64,
    /// Whether compaction completed successfully
    pub success: bool,
    /// Number of tokens freed by compaction
    pub tokens_removed: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryTruncateRequest {
    /// Event ID to truncate to. This event and all events after it are removed from the session.
    pub event_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryTruncateResult {
    /// Number of events that were removed
    pub events_removed: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstructionsSources {
    /// Glob pattern from frontmatter — when set, this instruction applies only to matching files
    #[serde(skip_serializing_if = "Option::is_none")]
    pub apply_to: Option<String>,
    /// Raw content of the instruction file
    pub content: String,
    /// Short description (body after frontmatter) for use in instruction tables
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Unique identifier for this source (used for toggling)
    pub id: String,
    /// Human-readable label
    pub label: String,
    /// Where this source lives — used for UI grouping
    pub location: InstructionsSourcesLocation,
    /// File path relative to repo or absolute for home
    pub source_path: String,
    /// Category of instruction source — used for merge logic
    pub r#type: InstructionsSourcesType,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstructionsGetSourcesResult {
    /// Instruction sources for the session
    pub sources: Vec<InstructionsSources>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LogRequest {
    /// When true, the message is transient and not persisted to the session event log on disk
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ephemeral: Option<bool>,
    /// Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub level: Option<SessionLogLevel>,
    /// Human-readable message
    pub message: String,
    /// Optional URL the user can open in their browser for more details
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LogResult {
    /// The unique identifier of the emitted session event
    pub event_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigAddRequest {
    /// MCP server configuration (local/stdio or remote/http)
    pub config: serde_json::Value,
    /// Unique name for the MCP server
    pub name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigDisableRequest {
    /// Names of MCP servers to disable. Each server is added to the persisted disabled list so new sessions skip it. Already-disabled names are ignored. Active sessions keep their current connections until they end.
    pub names: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigEnableRequest {
    /// Names of MCP servers to enable. Each server is removed from the persisted disabled list so new sessions spawn it. Unknown or already-enabled names are ignored.
    pub names: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigList {
    /// All MCP servers from user config, keyed by name
    pub servers: HashMap<String, serde_json::Value>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigRemoveRequest {
    /// Name of the MCP server to remove
    pub name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigUpdateRequest {
    /// MCP server configuration (local/stdio or remote/http)
    pub config: serde_json::Value,
    /// Name of the MCP server to update
    pub name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpDisableRequest {
    /// Name of the MCP server to disable
    pub server_name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpDiscoverRequest {
    /// Working directory used as context for discovery (e.g., plugin resolution)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpDiscoverResult {
    /// MCP servers discovered from all sources
    pub servers: Vec<DiscoveredMcpServer>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpEnableRequest {
    /// Name of the MCP server to enable
    pub server_name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthLoginRequest {
    /// Optional override for the body text shown on the OAuth loopback callback success page. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass surface-specific copy telling the user where to return.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub callback_success_message: Option<String>,
    /// Optional override for the OAuth client display name shown on the consent screen. Applies to newly registered dynamic clients only — existing registrations keep the name they were created with. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass their own surface-specific label so the consent screen matches the product the user sees.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// When true, clears any cached OAuth token for the server and runs a full new authorization. Use when the user explicitly wants to switch accounts or believes their session is stuck.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub force_reauth: Option<bool>,
    /// Name of the remote MCP server to authenticate
    pub server_name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthLoginResult {
    /// URL the caller should open in a browser to complete OAuth. Omitted when cached tokens were still valid and no browser interaction was needed — the server is already reconnected in that case. When present, the runtime starts the callback listener before returning and continues the flow in the background; completion is signaled via session.mcp_server_status_changed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub authorization_url: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServer {
    /// Error message if the server failed to connect
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Server name (config key)
    pub name: String,
    /// Configuration source: user, workspace, plugin, or builtin
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<McpServerSource>,
    /// Connection status: connected, failed, needs-auth, pending, disabled, or not_configured
    pub status: McpServerStatus,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerConfigHttp {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub filter_mapping: Option<serde_json::Value>,
    #[serde(default)]
    pub headers: HashMap<String, String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_default_server: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub oauth_client_id: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub oauth_public_client: Option<bool>,
    /// Timeout in milliseconds for tool calls to this server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<i64>,
    /// Tools to include. Defaults to all tools if not specified.
    #[serde(default)]
    pub tools: Vec<String>,
    /// Remote transport type. Defaults to "http" when omitted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<McpServerConfigHttpType>,
    pub url: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerConfigLocal {
    pub args: Vec<String>,
    pub command: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    #[serde(default)]
    pub env: HashMap<String, String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub filter_mapping: Option<serde_json::Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_default_server: Option<bool>,
    /// Timeout in milliseconds for tool calls to this server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<i64>,
    /// Tools to include. Defaults to all tools if not specified.
    #[serde(default)]
    pub tools: Vec<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<McpServerConfigLocalType>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerList {
    /// Configured MCP servers
    pub servers: Vec<McpServer>,
}

/// Billing information
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelBilling {
    /// Billing cost multiplier relative to the base rate
    pub multiplier: f64,
}

/// Vision-specific limits
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesLimitsVision {
    /// Maximum image size in bytes
    #[serde(rename = "max_prompt_image_size")]
    pub max_prompt_image_size: i64,
    /// Maximum number of images per prompt
    #[serde(rename = "max_prompt_images")]
    pub max_prompt_images: i64,
    /// MIME types the model accepts
    #[serde(rename = "supported_media_types")]
    pub supported_media_types: Vec<String>,
}

/// Token limits for prompts, outputs, and context window
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesLimits {
    /// Maximum total context window size in tokens
    #[serde(
        rename = "max_context_window_tokens",
        skip_serializing_if = "Option::is_none"
    )]
    pub max_context_window_tokens: Option<i64>,
    /// Maximum number of output/completion tokens
    #[serde(rename = "max_output_tokens", skip_serializing_if = "Option::is_none")]
    pub max_output_tokens: Option<i64>,
    /// Maximum number of prompt/input tokens
    #[serde(rename = "max_prompt_tokens", skip_serializing_if = "Option::is_none")]
    pub max_prompt_tokens: Option<i64>,
    /// Vision-specific limits
    #[serde(skip_serializing_if = "Option::is_none")]
    pub vision: Option<ModelCapabilitiesLimitsVision>,
}

/// Feature flags indicating what the model supports
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesSupports {
    /// Whether this model supports reasoning effort configuration
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<bool>,
    /// Whether this model supports vision/image input
    #[serde(skip_serializing_if = "Option::is_none")]
    pub vision: Option<bool>,
}

/// Model capabilities and limits
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilities {
    /// Token limits for prompts, outputs, and context window
    #[serde(skip_serializing_if = "Option::is_none")]
    pub limits: Option<ModelCapabilitiesLimits>,
    /// Feature flags indicating what the model supports
    #[serde(skip_serializing_if = "Option::is_none")]
    pub supports: Option<ModelCapabilitiesSupports>,
}

/// Policy state (if applicable)
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelPolicy {
    /// Current policy state for this model
    pub state: String,
    /// Usage terms or conditions for this model
    #[serde(skip_serializing_if = "Option::is_none")]
    pub terms: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Model {
    /// Billing information
    #[serde(skip_serializing_if = "Option::is_none")]
    pub billing: Option<ModelBilling>,
    /// Model capabilities and limits
    pub capabilities: ModelCapabilities,
    /// Default reasoning effort level (only present if model supports reasoning effort)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default_reasoning_effort: Option<String>,
    /// Model identifier (e.g., "claude-sonnet-4.5")
    pub id: String,
    /// Display name
    pub name: String,
    /// Policy state (if applicable)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy: Option<ModelPolicy>,
    /// Supported reasoning effort levels (only present if model supports reasoning effort)
    #[serde(default)]
    pub supported_reasoning_efforts: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesOverrideLimitsVision {
    /// Maximum image size in bytes
    #[serde(
        rename = "max_prompt_image_size",
        skip_serializing_if = "Option::is_none"
    )]
    pub max_prompt_image_size: Option<i64>,
    /// Maximum number of images per prompt
    #[serde(rename = "max_prompt_images", skip_serializing_if = "Option::is_none")]
    pub max_prompt_images: Option<i64>,
    /// MIME types the model accepts
    #[serde(rename = "supported_media_types", default)]
    pub supported_media_types: Vec<String>,
}

/// Token limits for prompts, outputs, and context window
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesOverrideLimits {
    /// Maximum total context window size in tokens
    #[serde(
        rename = "max_context_window_tokens",
        skip_serializing_if = "Option::is_none"
    )]
    pub max_context_window_tokens: Option<i64>,
    #[serde(rename = "max_output_tokens", skip_serializing_if = "Option::is_none")]
    pub max_output_tokens: Option<i64>,
    #[serde(rename = "max_prompt_tokens", skip_serializing_if = "Option::is_none")]
    pub max_prompt_tokens: Option<i64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub vision: Option<ModelCapabilitiesOverrideLimitsVision>,
}

/// Feature flags indicating what the model supports
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesOverrideSupports {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub vision: Option<bool>,
}

/// Override individual model capabilities resolved by the runtime
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesOverride {
    /// Token limits for prompts, outputs, and context window
    #[serde(skip_serializing_if = "Option::is_none")]
    pub limits: Option<ModelCapabilitiesOverrideLimits>,
    /// Feature flags indicating what the model supports
    #[serde(skip_serializing_if = "Option::is_none")]
    pub supports: Option<ModelCapabilitiesOverrideSupports>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelList {
    /// List of available models with full metadata
    pub models: Vec<Model>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelsListRequest {
    /// GitHub token for per-user model listing. When provided, resolves this token to determine the user's Copilot plan and available models instead of using the global auth.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_hub_token: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSwitchToRequest {
    /// Override individual model capabilities resolved by the runtime
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_capabilities: Option<ModelCapabilitiesOverride>,
    /// Model identifier to switch to
    pub model_id: String,
    /// Reasoning effort level to use for the model
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSwitchToResult {
    /// Currently active model identifier after the switch
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModeSetRequest {
    /// The agent mode. Valid values: "interactive", "plan", "autopilot".
    pub mode: SessionMode,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct NameGetResult {
    /// The session name, falling back to the auto-generated summary, or null if neither exists
    pub name: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct NameSetRequest {
    /// New session name (1–100 characters, trimmed of leading/trailing whitespace)
    pub name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalCommands {
    pub command_identifiers: Vec<String>,
    pub kind: PermissionDecisionApproveForLocationApprovalCommandsKind,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalRead {
    pub kind: PermissionDecisionApproveForLocationApprovalReadKind,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalWrite {
    pub kind: PermissionDecisionApproveForLocationApprovalWriteKind,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalMcp {
    pub kind: PermissionDecisionApproveForLocationApprovalMcpKind,
    pub server_name: String,
    pub tool_name: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalMcpSampling {
    pub kind: PermissionDecisionApproveForLocationApprovalMcpSamplingKind,
    pub server_name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalMemory {
    pub kind: PermissionDecisionApproveForLocationApprovalMemoryKind,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalCustomTool {
    pub kind: PermissionDecisionApproveForLocationApprovalCustomToolKind,
    pub tool_name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocation {
    /// The approval to persist for this location
    pub approval: PermissionDecisionApproveForLocationApproval,
    /// Approved and persisted for this project location
    pub kind: PermissionDecisionApproveForLocationKind,
    /// The location key (git root or cwd) to persist the approval to
    pub location_key: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalCommands {
    pub command_identifiers: Vec<String>,
    pub kind: PermissionDecisionApproveForSessionApprovalCommandsKind,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalRead {
    pub kind: PermissionDecisionApproveForSessionApprovalReadKind,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalWrite {
    pub kind: PermissionDecisionApproveForSessionApprovalWriteKind,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalMcp {
    pub kind: PermissionDecisionApproveForSessionApprovalMcpKind,
    pub server_name: String,
    pub tool_name: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalMcpSampling {
    pub kind: PermissionDecisionApproveForSessionApprovalMcpSamplingKind,
    pub server_name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalMemory {
    pub kind: PermissionDecisionApproveForSessionApprovalMemoryKind,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalCustomTool {
    pub kind: PermissionDecisionApproveForSessionApprovalCustomToolKind,
    pub tool_name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSession {
    /// The approval to add as a session-scoped rule
    pub approval: PermissionDecisionApproveForSessionApproval,
    /// Approved and remembered for the rest of the session
    pub kind: PermissionDecisionApproveForSessionKind,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveOnce {
    /// The permission request was approved for this one instance
    pub kind: PermissionDecisionApproveOnceKind,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionReject {
    /// Optional feedback from the user explaining the denial
    #[serde(skip_serializing_if = "Option::is_none")]
    pub feedback: Option<String>,
    /// Denied by the user during an interactive prompt
    pub kind: PermissionDecisionRejectKind,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionUserNotAvailable {
    /// Denied because user confirmation was unavailable
    pub kind: PermissionDecisionUserNotAvailableKind,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionRequest {
    /// Request ID of the pending permission request
    pub request_id: String,
    pub result: PermissionDecision,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequestResult {
    /// Whether the permission request was handled successfully
    pub success: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsResetSessionApprovalsRequest {}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsResetSessionApprovalsResult {
    /// Whether the operation succeeded
    pub success: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsSetApproveAllRequest {
    /// Whether to auto-approve all tool permission requests
    pub enabled: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsSetApproveAllResult {
    /// Whether the operation succeeded
    pub success: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PingRequest {
    /// Optional message to echo back
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PingResult {
    /// Echoed message (or default greeting)
    pub message: String,
    /// Server protocol version number
    pub protocol_version: i64,
    /// Server timestamp in milliseconds
    pub timestamp: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PlanReadResult {
    /// The content of the plan file, or null if it does not exist
    pub content: Option<String>,
    /// Whether the plan file exists in the workspace
    pub exists: bool,
    /// Absolute file path of the plan file, or null if workspace is not enabled
    pub path: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PlanUpdateRequest {
    /// The new content for the plan file
    pub content: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Plugin {
    /// Whether the plugin is currently enabled
    pub enabled: bool,
    /// Marketplace the plugin came from
    pub marketplace: String,
    /// Plugin name
    pub name: String,
    /// Installed version
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginList {
    /// Installed plugins
    pub plugins: Vec<Plugin>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ServerSkill {
    /// Description of what the skill does
    pub description: String,
    /// Whether the skill is currently enabled (based on global config)
    pub enabled: bool,
    /// Unique identifier for the skill
    pub name: String,
    /// Absolute path to the skill file
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// The project path this skill belongs to (only for project/inherited skills)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_path: Option<String>,
    /// Source location type (e.g., project, personal-copilot, plugin, builtin)
    pub source: String,
    /// Whether the skill can be invoked by the user as a slash command
    pub user_invocable: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ServerSkillList {
    /// All discovered skills across all sources
    pub skills: Vec<ServerSkill>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAuthStatus {
    /// Authentication type
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_type: Option<AuthInfoType>,
    /// Copilot plan tier (e.g., individual_pro, business)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_plan: Option<String>,
    /// Authentication host URL
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<String>,
    /// Whether the session has resolved authentication
    pub is_authenticated: bool,
    /// Authenticated login/username, if available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub login: Option<String>,
    /// Human-readable authentication status description
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status_message: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsAppendFileRequest {
    /// Content to append
    pub content: String,
    /// Optional POSIX-style mode for newly created files
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<i64>,
    /// Path using SessionFs conventions
    pub path: String,
}

/// Describes a filesystem error.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsError {
    /// Error classification
    pub code: SessionFsErrorCode,
    /// Free-form detail about the error, for logging/diagnostics
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsExistsRequest {
    /// Path using SessionFs conventions
    pub path: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsExistsResult {
    /// Whether the path exists
    pub exists: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsMkdirRequest {
    /// Optional POSIX-style mode for newly created directories
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<i64>,
    /// Path using SessionFs conventions
    pub path: String,
    /// Create parent directories as needed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub recursive: Option<bool>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirRequest {
    /// Path using SessionFs conventions
    pub path: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirResult {
    /// Entry names in the directory
    pub entries: Vec<String>,
    /// Describes a filesystem error.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<SessionFsError>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirWithTypesEntry {
    /// Entry name
    pub name: String,
    /// Entry type
    pub r#type: SessionFsReaddirWithTypesEntryType,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirWithTypesRequest {
    /// Path using SessionFs conventions
    pub path: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirWithTypesResult {
    /// Directory entries with type information
    pub entries: Vec<SessionFsReaddirWithTypesEntry>,
    /// Describes a filesystem error.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<SessionFsError>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReadFileRequest {
    /// Path using SessionFs conventions
    pub path: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReadFileResult {
    /// File content as UTF-8 string
    pub content: String,
    /// Describes a filesystem error.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<SessionFsError>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsRenameRequest {
    /// Destination path using SessionFs conventions
    pub dest: String,
    /// Source path using SessionFs conventions
    pub src: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsRmRequest {
    /// Ignore errors if the path does not exist
    #[serde(skip_serializing_if = "Option::is_none")]
    pub force: Option<bool>,
    /// Path using SessionFs conventions
    pub path: String,
    /// Remove directories and their contents recursively
    #[serde(skip_serializing_if = "Option::is_none")]
    pub recursive: Option<bool>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSetProviderRequest {
    /// Path conventions used by this filesystem
    pub conventions: SessionFsSetProviderConventions,
    /// Initial working directory for sessions
    pub initial_cwd: String,
    /// Path within each session's SessionFs where the runtime stores files for that session
    pub session_state_path: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSetProviderResult {
    /// Whether the provider was set successfully
    pub success: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsStatRequest {
    /// Path using SessionFs conventions
    pub path: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsStatResult {
    /// ISO 8601 timestamp of creation
    pub birthtime: String,
    /// Describes a filesystem error.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<SessionFsError>,
    /// Whether the path is a directory
    pub is_directory: bool,
    /// Whether the path is a file
    pub is_file: bool,
    /// ISO 8601 timestamp of last modification
    pub mtime: String,
    /// File size in bytes
    pub size: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsWriteFileRequest {
    /// Content to write
    pub content: String,
    /// Optional POSIX-style mode for newly created files
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<i64>,
    /// Path using SessionFs conventions
    pub path: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsForkRequest {
    /// Source session ID to fork from
    pub session_id: String,
    /// Optional event ID boundary. When provided, the fork includes only events before this ID (exclusive). When omitted, all events are included.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub to_event_id: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsForkResult {
    /// The new forked session's ID
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellExecRequest {
    /// Shell command to execute
    pub command: String,
    /// Working directory (defaults to session working directory)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Timeout in milliseconds (default: 30000)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<i64>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellExecResult {
    /// Unique identifier for tracking streamed output
    pub process_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellKillRequest {
    /// Process identifier returned by shell.exec
    pub process_id: String,
    /// Signal to send (default: SIGTERM)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub signal: Option<ShellKillSignal>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellKillResult {
    /// Whether the signal was sent successfully
    pub killed: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Skill {
    /// Description of what the skill does
    pub description: String,
    /// Whether the skill is currently enabled
    pub enabled: bool,
    /// Unique identifier for the skill
    pub name: String,
    /// Absolute path to the skill file
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// Source location type (e.g., project, personal, plugin)
    pub source: String,
    /// Whether the skill can be invoked by the user as a slash command
    pub user_invocable: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillList {
    /// Available skills
    pub skills: Vec<Skill>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsConfigSetDisabledSkillsRequest {
    /// List of skill names to disable
    pub disabled_skills: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsDisableRequest {
    /// Name of the skill to disable
    pub name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsDiscoverRequest {
    /// Optional list of project directory paths to scan for project-scoped skills
    #[serde(default)]
    pub project_paths: Vec<String>,
    /// Optional list of additional skill directory paths to include
    #[serde(default)]
    pub skill_directories: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsEnableRequest {
    /// Name of the skill to enable
    pub name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Tool {
    /// Description of what the tool does
    pub description: String,
    /// Optional instructions for how to use this tool effectively
    #[serde(skip_serializing_if = "Option::is_none")]
    pub instructions: Option<String>,
    /// Tool identifier (e.g., "bash", "grep", "str_replace_editor")
    pub name: String,
    /// Optional namespaced name for declarative filtering (e.g., "playwright/navigate" for MCP tools)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub namespaced_name: Option<String>,
    /// JSON Schema for the tool's input parameters
    #[serde(default)]
    pub parameters: HashMap<String, serde_json::Value>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolCallResult {
    /// Error message if the tool call failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Type of the tool result
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result_type: Option<String>,
    /// Text result to send back to the LLM
    pub text_result_for_llm: String,
    /// Telemetry data from tool execution
    #[serde(default)]
    pub tool_telemetry: HashMap<String, serde_json::Value>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolList {
    /// List of available built-in tools with metadata
    pub tools: Vec<Tool>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsHandlePendingToolCallRequest {
    /// Error message if the tool call failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Request ID of the pending tool call
    pub request_id: String,
    /// Tool call result (string or expanded result object)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<serde_json::Value>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsListRequest {
    /// Optional model ID — when provided, the returned tool list reflects model-specific overrides
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationArrayAnyOfFieldItemsAnyOf {
    pub r#const: String,
    pub title: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationArrayAnyOfFieldItems {
    pub any_of: Vec<UIElicitationArrayAnyOfFieldItemsAnyOf>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationArrayAnyOfField {
    #[serde(default)]
    pub default: Vec<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    pub items: UIElicitationArrayAnyOfFieldItems,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_items: Option<f64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub min_items: Option<f64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    pub r#type: UIElicitationArrayAnyOfFieldType,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationArrayEnumFieldItems {
    pub r#enum: Vec<String>,
    pub r#type: UIElicitationArrayEnumFieldItemsType,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationArrayEnumField {
    #[serde(default)]
    pub default: Vec<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    pub items: UIElicitationArrayEnumFieldItems,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_items: Option<f64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub min_items: Option<f64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    pub r#type: UIElicitationArrayEnumFieldType,
}

/// JSON Schema describing the form fields to present to the user
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationSchema {
    /// Form field definitions, keyed by field name
    pub properties: HashMap<String, serde_json::Value>,
    /// List of required field names
    #[serde(default)]
    pub required: Vec<String>,
    /// Schema type indicator (always 'object')
    pub r#type: UIElicitationSchemaType,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationRequest {
    /// Message describing what information is needed from the user
    pub message: String,
    /// JSON Schema describing the form fields to present to the user
    pub requested_schema: UIElicitationSchema,
}

/// The elicitation response (accept with form values, decline, or cancel)
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationResponse {
    /// The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
    pub action: UIElicitationResponseAction,
    /// The form values submitted by the user (present when action is 'accept')
    #[serde(default)]
    pub content: HashMap<String, serde_json::Value>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationResult {
    /// Whether the response was accepted. False if the request was already resolved by another client.
    pub success: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationSchemaPropertyBoolean {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    pub r#type: UIElicitationSchemaPropertyBooleanType,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationSchemaPropertyNumber {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<f64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub maximum: Option<f64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub minimum: Option<f64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    pub r#type: UIElicitationSchemaPropertyNumberType,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationSchemaPropertyString {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub format: Option<UIElicitationSchemaPropertyStringFormat>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_length: Option<f64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub min_length: Option<f64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    pub r#type: UIElicitationSchemaPropertyStringType,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationStringEnumField {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    pub r#enum: Vec<String>,
    #[serde(default)]
    pub enum_names: Vec<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    pub r#type: UIElicitationStringEnumFieldType,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationStringOneOfFieldOneOf {
    pub r#const: String,
    pub title: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationStringOneOfField {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    pub one_of: Vec<UIElicitationStringOneOfFieldOneOf>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    pub r#type: UIElicitationStringOneOfFieldType,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIHandlePendingElicitationRequest {
    /// The unique request ID from the elicitation.requested event
    pub request_id: String,
    /// The elicitation response (accept with form values, decline, or cancel)
    pub result: UIElicitationResponse,
}

/// Aggregated code change metrics
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UsageMetricsCodeChanges {
    /// Number of distinct files modified
    pub files_modified_count: i64,
    /// Total lines of code added
    pub lines_added: i64,
    /// Total lines of code removed
    pub lines_removed: i64,
}

/// Request count and cost metrics for this model
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UsageMetricsModelMetricRequests {
    /// User-initiated premium request cost (with multiplier applied)
    pub cost: f64,
    /// Number of API requests made with this model
    pub count: i64,
}

/// Token usage metrics for this model
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UsageMetricsModelMetricUsage {
    /// Total tokens read from prompt cache
    pub cache_read_tokens: i64,
    /// Total tokens written to prompt cache
    pub cache_write_tokens: i64,
    /// Total input tokens consumed
    pub input_tokens: i64,
    /// Total output tokens produced
    pub output_tokens: i64,
    /// Total output tokens used for reasoning
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_tokens: Option<i64>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UsageMetricsModelMetric {
    /// Request count and cost metrics for this model
    pub requests: UsageMetricsModelMetricRequests,
    /// Token usage metrics for this model
    pub usage: UsageMetricsModelMetricUsage,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UsageGetMetricsResult {
    /// Aggregated code change metrics
    pub code_changes: UsageMetricsCodeChanges,
    /// Currently active model identifier
    #[serde(skip_serializing_if = "Option::is_none")]
    pub current_model: Option<String>,
    /// Input tokens from the most recent main-agent API call
    pub last_call_input_tokens: i64,
    /// Output tokens from the most recent main-agent API call
    pub last_call_output_tokens: i64,
    /// Per-model token and request metrics, keyed by model identifier
    pub model_metrics: HashMap<String, UsageMetricsModelMetric>,
    /// Session start timestamp (epoch milliseconds)
    pub session_start_time: i64,
    /// Total time spent in model API calls (milliseconds)
    pub total_api_duration_ms: f64,
    /// Total user-initiated premium request cost across all models (may be fractional due to multipliers)
    pub total_premium_request_cost: f64,
    /// Raw count of user-initiated API requests
    pub total_user_requests: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesCreateFileRequest {
    /// File content to write as a UTF-8 string
    pub content: String,
    /// Relative path within the workspace files directory
    pub path: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesGetWorkspaceResultWorkspace {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    #[serde(
        rename = "chronicle_sync_dismissed",
        skip_serializing_if = "Option::is_none"
    )]
    pub chronicle_sync_dismissed: Option<bool>,
    #[serde(rename = "created_at", skip_serializing_if = "Option::is_none")]
    pub created_at: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    #[serde(rename = "git_root", skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    #[serde(rename = "host_type", skip_serializing_if = "Option::is_none")]
    pub host_type: Option<WorkspacesGetWorkspaceResultWorkspaceHostType>,
    pub id: String,
    #[serde(rename = "mc_last_event_id", skip_serializing_if = "Option::is_none")]
    pub mc_last_event_id: Option<String>,
    #[serde(rename = "mc_session_id", skip_serializing_if = "Option::is_none")]
    pub mc_session_id: Option<String>,
    #[serde(rename = "mc_task_id", skip_serializing_if = "Option::is_none")]
    pub mc_task_id: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    #[serde(rename = "remote_steerable", skip_serializing_if = "Option::is_none")]
    pub remote_steerable: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    #[serde(rename = "session_sync_level", skip_serializing_if = "Option::is_none")]
    pub session_sync_level: Option<WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
    #[serde(rename = "summary_count", skip_serializing_if = "Option::is_none")]
    pub summary_count: Option<i64>,
    #[serde(rename = "updated_at", skip_serializing_if = "Option::is_none")]
    pub updated_at: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesGetWorkspaceResult {
    /// Current workspace metadata, or null if not available
    pub workspace: Option<WorkspacesGetWorkspaceResultWorkspace>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesListFilesResult {
    /// Relative file paths in the workspace files directory
    pub files: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesReadFileRequest {
    /// Relative path within the workspace files directory
    pub path: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesReadFileResult {
    /// File content as a UTF-8 string
    pub content: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelsListResult {
    /// List of available models with full metadata
    pub models: Vec<Model>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsListResult {
    /// List of available built-in tools with metadata
    pub tools: Vec<Tool>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigListResult {
    /// All MCP servers from user config, keyed by name
    pub servers: HashMap<String, serde_json::Value>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsDiscoverResult {
    /// All discovered skills across all sources
    pub skills: Vec<ServerSkill>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAuthGetStatusParams {
    /// Target session identifier
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAuthGetStatusResult {
    /// Authentication type
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_type: Option<AuthInfoType>,
    /// Copilot plan tier (e.g., individual_pro, business)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_plan: Option<String>,
    /// Authentication host URL
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<String>,
    /// Whether the session has resolved authentication
    pub is_authenticated: bool,
    /// Authenticated login/username, if available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub login: Option<String>,
    /// Human-readable authentication status description
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status_message: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelGetCurrentParams {
    /// Target session identifier
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelGetCurrentResult {
    /// Currently active model identifier
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelSwitchToResult {
    /// Currently active model identifier after the switch
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModeGetParams {
    /// Target session identifier
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionNameGetParams {
    /// Target session identifier
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionNameGetResult {
    /// The session name, falling back to the auto-generated summary, or null if neither exists
    pub name: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPlanReadParams {
    /// Target session identifier
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPlanReadResult {
    /// The content of the plan file, or null if it does not exist
    pub content: Option<String>,
    /// Whether the plan file exists in the workspace
    pub exists: bool,
    /// Absolute file path of the plan file, or null if workspace is not enabled
    pub path: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPlanDeleteParams {
    /// Target session identifier
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesGetWorkspaceParams {
    /// Target session identifier
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesGetWorkspaceResultWorkspace {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    #[serde(
        rename = "chronicle_sync_dismissed",
        skip_serializing_if = "Option::is_none"
    )]
    pub chronicle_sync_dismissed: Option<bool>,
    #[serde(rename = "created_at", skip_serializing_if = "Option::is_none")]
    pub created_at: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    #[serde(rename = "git_root", skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    #[serde(rename = "host_type", skip_serializing_if = "Option::is_none")]
    pub host_type: Option<SessionWorkspacesGetWorkspaceResultWorkspaceHostType>,
    pub id: String,
    #[serde(rename = "mc_last_event_id", skip_serializing_if = "Option::is_none")]
    pub mc_last_event_id: Option<String>,
    #[serde(rename = "mc_session_id", skip_serializing_if = "Option::is_none")]
    pub mc_session_id: Option<String>,
    #[serde(rename = "mc_task_id", skip_serializing_if = "Option::is_none")]
    pub mc_task_id: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    #[serde(rename = "remote_steerable", skip_serializing_if = "Option::is_none")]
    pub remote_steerable: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    #[serde(rename = "session_sync_level", skip_serializing_if = "Option::is_none")]
    pub session_sync_level: Option<SessionWorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
    #[serde(rename = "summary_count", skip_serializing_if = "Option::is_none")]
    pub summary_count: Option<i64>,
    #[serde(rename = "updated_at", skip_serializing_if = "Option::is_none")]
    pub updated_at: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesGetWorkspaceResult {
    /// Current workspace metadata, or null if not available
    pub workspace: Option<SessionWorkspacesGetWorkspaceResultWorkspace>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesListFilesParams {
    /// Target session identifier
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesListFilesResult {
    /// Relative file paths in the workspace files directory
    pub files: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesReadFileResult {
    /// File content as a UTF-8 string
    pub content: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionInstructionsGetSourcesParams {
    /// Target session identifier
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionInstructionsGetSourcesResult {
    /// Instruction sources for the session
    pub sources: Vec<InstructionsSources>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFleetStartResult {
    /// Whether fleet mode was successfully activated
    pub started: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentListParams {
    /// Target session identifier
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentListResult {
    /// Available custom agents
    pub agents: Vec<AgentInfo>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentGetCurrentParams {
    /// Target session identifier
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentGetCurrentResult {
    /// Currently selected custom agent, or null if using the default agent
    pub agent: AgentInfo,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentSelectResult {
    /// The newly selected custom agent
    pub agent: AgentInfo,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentDeselectParams {
    /// Target session identifier
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentReloadParams {
    /// Target session identifier
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentReloadResult {
    /// Reloaded custom agents
    pub agents: Vec<AgentInfo>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsListParams {
    /// Target session identifier
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsListResult {
    /// Available skills
    pub skills: Vec<Skill>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsReloadParams {
    /// Target session identifier
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpListParams {
    /// Target session identifier
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpListResult {
    /// Configured MCP servers
    pub servers: Vec<McpServer>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpReloadParams {
    /// Target session identifier
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpOauthLoginResult {
    /// URL the caller should open in a browser to complete OAuth. Omitted when cached tokens were still valid and no browser interaction was needed — the server is already reconnected in that case. When present, the runtime starts the callback listener before returning and continues the flow in the background; completion is signaled via session.mcp_server_status_changed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub authorization_url: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPluginsListParams {
    /// Target session identifier
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPluginsListResult {
    /// Installed plugins
    pub plugins: Vec<Plugin>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionExtensionsListParams {
    /// Target session identifier
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionExtensionsListResult {
    /// Discovered extensions and their current status
    pub extensions: Vec<Extension>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionExtensionsReloadParams {
    /// Target session identifier
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionToolsHandlePendingToolCallResult {
    /// Whether the tool call result was handled successfully
    pub success: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCommandsHandlePendingCommandResult {
    /// Whether the command was handled successfully
    pub success: bool,
}

/// The elicitation response (accept with form values, decline, or cancel)
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiElicitationResult {
    /// The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
    pub action: UIElicitationResponseAction,
    /// The form values submitted by the user (present when action is 'accept')
    #[serde(default)]
    pub content: HashMap<String, serde_json::Value>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiHandlePendingElicitationResult {
    /// Whether the response was accepted. False if the request was already resolved by another client.
    pub success: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsHandlePendingPermissionRequestResult {
    /// Whether the permission request was handled successfully
    pub success: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsSetApproveAllResult {
    /// Whether the operation succeeded
    pub success: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsResetSessionApprovalsResult {
    /// Whether the operation succeeded
    pub success: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionLogResult {
    /// The unique identifier of the emitted session event
    pub event_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionShellExecResult {
    /// Unique identifier for tracking streamed output
    pub process_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionShellKillResult {
    /// Whether the signal was sent successfully
    pub killed: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistoryCompactParams {
    /// Target session identifier
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistoryCompactResult {
    /// Post-compaction context window usage breakdown
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_window: Option<HistoryCompactContextWindow>,
    /// Number of messages removed during compaction
    pub messages_removed: i64,
    /// Whether compaction completed successfully
    pub success: bool,
    /// Number of tokens freed by compaction
    pub tokens_removed: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistoryTruncateResult {
    /// Number of events that were removed
    pub events_removed: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUsageGetMetricsParams {
    /// Target session identifier
    pub session_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUsageGetMetricsResult {
    /// Aggregated code change metrics
    pub code_changes: UsageMetricsCodeChanges,
    /// Currently active model identifier
    #[serde(skip_serializing_if = "Option::is_none")]
    pub current_model: Option<String>,
    /// Input tokens from the most recent main-agent API call
    pub last_call_input_tokens: i64,
    /// Output tokens from the most recent main-agent API call
    pub last_call_output_tokens: i64,
    /// Per-model token and request metrics, keyed by model identifier
    pub model_metrics: HashMap<String, UsageMetricsModelMetric>,
    /// Session start timestamp (epoch milliseconds)
    pub session_start_time: i64,
    /// Total time spent in model API calls (milliseconds)
    pub total_api_duration_ms: f64,
    /// Total user-initiated premium request cost across all models (may be fractional due to multipliers)
    pub total_premium_request_cost: f64,
    /// Raw count of user-initiated API requests
    pub total_user_requests: i64,
}

/// Authentication type
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthInfoType {
    #[serde(rename = "hmac")]
    Hmac,
    #[serde(rename = "env")]
    Env,
    #[serde(rename = "user")]
    User,
    #[serde(rename = "gh-cli")]
    GhCli,
    #[serde(rename = "api-key")]
    ApiKey,
    #[serde(rename = "token")]
    Token,
    #[serde(rename = "copilot-api-token")]
    CopilotApiToken,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

/// Configuration source
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum DiscoveredMcpServerSource {
    #[serde(rename = "user")]
    User,
    #[serde(rename = "workspace")]
    Workspace,
    #[serde(rename = "plugin")]
    Plugin,
    #[serde(rename = "builtin")]
    Builtin,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

/// Server transport type: stdio, http, sse, or memory (local configs are normalized to stdio)
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum DiscoveredMcpServerType {
    #[serde(rename = "stdio")]
    Stdio,
    #[serde(rename = "http")]
    Http,
    #[serde(rename = "sse")]
    Sse,
    #[serde(rename = "memory")]
    Memory,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

/// Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/)
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExtensionSource {
    #[serde(rename = "project")]
    Project,
    #[serde(rename = "user")]
    User,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

/// Current status: running, disabled, failed, or starting
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExtensionStatus {
    #[serde(rename = "running")]
    Running,
    #[serde(rename = "disabled")]
    Disabled,
    #[serde(rename = "failed")]
    Failed,
    #[serde(rename = "starting")]
    Starting,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum FilterMappingString {
    #[serde(rename = "none")]
    None,
    #[serde(rename = "markdown")]
    Markdown,
    #[serde(rename = "hidden_characters")]
    HiddenCharacters,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum FilterMappingValue {
    #[serde(rename = "none")]
    None,
    #[serde(rename = "markdown")]
    Markdown,
    #[serde(rename = "hidden_characters")]
    HiddenCharacters,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

/// Where this source lives — used for UI grouping
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstructionsSourcesLocation {
    #[serde(rename = "user")]
    User,
    #[serde(rename = "repository")]
    Repository,
    #[serde(rename = "working-directory")]
    WorkingDirectory,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

/// Category of instruction source — used for merge logic
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstructionsSourcesType {
    #[serde(rename = "home")]
    Home,
    #[serde(rename = "repo")]
    Repo,
    #[serde(rename = "model")]
    Model,
    #[serde(rename = "vscode")]
    Vscode,
    #[serde(rename = "nested-agents")]
    NestedAgents,
    #[serde(rename = "child-instructions")]
    ChildInstructions,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

/// Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionLogLevel {
    #[serde(rename = "info")]
    Info,
    #[serde(rename = "warning")]
    Warning,
    #[serde(rename = "error")]
    Error,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

/// Configuration source: user, workspace, plugin, or builtin
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpServerSource {
    #[serde(rename = "user")]
    User,
    #[serde(rename = "workspace")]
    Workspace,
    #[serde(rename = "plugin")]
    Plugin,
    #[serde(rename = "builtin")]
    Builtin,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

/// Connection status: connected, failed, needs-auth, pending, disabled, or not_configured
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpServerStatus {
    #[serde(rename = "connected")]
    Connected,
    #[serde(rename = "failed")]
    Failed,
    #[serde(rename = "needs-auth")]
    NeedsAuth,
    #[serde(rename = "pending")]
    Pending,
    #[serde(rename = "disabled")]
    Disabled,
    #[serde(rename = "not_configured")]
    NotConfigured,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

/// Remote transport type. Defaults to "http" when omitted.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpServerConfigHttpType {
    #[serde(rename = "http")]
    Http,
    #[serde(rename = "sse")]
    Sse,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpServerConfigLocalType {
    #[serde(rename = "local")]
    Local,
    #[serde(rename = "stdio")]
    Stdio,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

/// The agent mode. Valid values: "interactive", "plan", "autopilot".
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionMode {
    #[serde(rename = "interactive")]
    Interactive,
    #[serde(rename = "plan")]
    Plan,
    #[serde(rename = "autopilot")]
    Autopilot,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalCommandsKind {
    #[serde(rename = "commands")]
    Commands,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalReadKind {
    #[serde(rename = "read")]
    Read,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalWriteKind {
    #[serde(rename = "write")]
    Write,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalMcpKind {
    #[serde(rename = "mcp")]
    Mcp,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalMcpSamplingKind {
    #[serde(rename = "mcp-sampling")]
    McpSampling,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalMemoryKind {
    #[serde(rename = "memory")]
    Memory,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalCustomToolKind {
    #[serde(rename = "custom-tool")]
    CustomTool,
}

/// The approval to persist for this location
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum PermissionDecisionApproveForLocationApproval {
    Commands(PermissionDecisionApproveForLocationApprovalCommands),
    Read(PermissionDecisionApproveForLocationApprovalRead),
    Write(PermissionDecisionApproveForLocationApprovalWrite),
    Mcp(PermissionDecisionApproveForLocationApprovalMcp),
    McpSampling(PermissionDecisionApproveForLocationApprovalMcpSampling),
    Memory(PermissionDecisionApproveForLocationApprovalMemory),
    CustomTool(PermissionDecisionApproveForLocationApprovalCustomTool),
}

/// Approved and persisted for this project location
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationKind {
    #[serde(rename = "approve-for-location")]
    ApproveForLocation,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalCommandsKind {
    #[serde(rename = "commands")]
    Commands,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalReadKind {
    #[serde(rename = "read")]
    Read,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalWriteKind {
    #[serde(rename = "write")]
    Write,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalMcpKind {
    #[serde(rename = "mcp")]
    Mcp,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalMcpSamplingKind {
    #[serde(rename = "mcp-sampling")]
    McpSampling,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalMemoryKind {
    #[serde(rename = "memory")]
    Memory,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalCustomToolKind {
    #[serde(rename = "custom-tool")]
    CustomTool,
}

/// The approval to add as a session-scoped rule
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum PermissionDecisionApproveForSessionApproval {
    Commands(PermissionDecisionApproveForSessionApprovalCommands),
    Read(PermissionDecisionApproveForSessionApprovalRead),
    Write(PermissionDecisionApproveForSessionApprovalWrite),
    Mcp(PermissionDecisionApproveForSessionApprovalMcp),
    McpSampling(PermissionDecisionApproveForSessionApprovalMcpSampling),
    Memory(PermissionDecisionApproveForSessionApprovalMemory),
    CustomTool(PermissionDecisionApproveForSessionApprovalCustomTool),
}

/// Approved and remembered for the rest of the session
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionKind {
    #[serde(rename = "approve-for-session")]
    ApproveForSession,
}

/// The permission request was approved for this one instance
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveOnceKind {
    #[serde(rename = "approve-once")]
    ApproveOnce,
}

/// Denied by the user during an interactive prompt
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionRejectKind {
    #[serde(rename = "reject")]
    Reject,
}

/// Denied because user confirmation was unavailable
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionUserNotAvailableKind {
    #[serde(rename = "user-not-available")]
    UserNotAvailable,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum PermissionDecision {
    ApproveOnce(PermissionDecisionApproveOnce),
    ApproveForSession(PermissionDecisionApproveForSession),
    ApproveForLocation(PermissionDecisionApproveForLocation),
    Reject(PermissionDecisionReject),
    UserNotAvailable(PermissionDecisionUserNotAvailable),
}

/// Error classification
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionFsErrorCode {
    ENOENT,
    UNKNOWN,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

/// Entry type
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionFsReaddirWithTypesEntryType {
    #[serde(rename = "file")]
    File,
    #[serde(rename = "directory")]
    Directory,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

/// Path conventions used by this filesystem
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionFsSetProviderConventions {
    #[serde(rename = "windows")]
    Windows,
    #[serde(rename = "posix")]
    Posix,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

/// Signal to send (default: SIGTERM)
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum ShellKillSignal {
    SIGTERM,
    SIGKILL,
    SIGINT,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationArrayAnyOfFieldType {
    #[serde(rename = "array")]
    Array,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationArrayEnumFieldItemsType {
    #[serde(rename = "string")]
    String,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationArrayEnumFieldType {
    #[serde(rename = "array")]
    Array,
}

/// Schema type indicator (always 'object')
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationSchemaType {
    #[serde(rename = "object")]
    Object,
}

/// The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationResponseAction {
    #[serde(rename = "accept")]
    Accept,
    #[serde(rename = "decline")]
    Decline,
    #[serde(rename = "cancel")]
    Cancel,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationSchemaPropertyBooleanType {
    #[serde(rename = "boolean")]
    Boolean,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationSchemaPropertyNumberType {
    #[serde(rename = "number")]
    Number,
    #[serde(rename = "integer")]
    Integer,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationSchemaPropertyStringFormat {
    #[serde(rename = "email")]
    Email,
    #[serde(rename = "uri")]
    Uri,
    #[serde(rename = "date")]
    Date,
    #[serde(rename = "date-time")]
    DateTime,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationSchemaPropertyStringType {
    #[serde(rename = "string")]
    String,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationStringEnumFieldType {
    #[serde(rename = "string")]
    String,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationStringOneOfFieldType {
    #[serde(rename = "string")]
    String,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum WorkspacesGetWorkspaceResultWorkspaceHostType {
    #[serde(rename = "github")]
    Github,
    #[serde(rename = "ado")]
    Ado,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel {
    #[serde(rename = "local")]
    Local,
    #[serde(rename = "user")]
    User,
    #[serde(rename = "repo_and_user")]
    RepoAndUser,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionWorkspacesGetWorkspaceResultWorkspaceHostType {
    #[serde(rename = "github")]
    Github,
    #[serde(rename = "ado")]
    Ado,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionWorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel {
    #[serde(rename = "local")]
    Local,
    #[serde(rename = "user")]
    User,
    #[serde(rename = "repo_and_user")]
    RepoAndUser,
    /// Unknown variant for forward compatibility.
    #[serde(other)]
    Unknown,
}
