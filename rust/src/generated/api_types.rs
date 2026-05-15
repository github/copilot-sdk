//! Auto-generated from api.schema.json — do not edit manually.

#![allow(clippy::large_enum_variant)]

use std::collections::HashMap;

use serde::{Deserialize, Serialize};

use super::session_events::ReasoningSummary;
use crate::types::{RequestId, SessionId};

/// JSON-RPC method name constants.
pub mod rpc_methods {
    /// `ping`
    pub const PING: &str = "ping";
    /// `connect`
    pub const CONNECT: &str = "connect";
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
    /// `sessions.connect`
    pub const SESSIONS_CONNECT: &str = "sessions.connect";
    /// `session.suspend`
    pub const SESSION_SUSPEND: &str = "session.suspend";
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
    /// `session.tasks.startAgent`
    pub const SESSION_TASKS_STARTAGENT: &str = "session.tasks.startAgent";
    /// `session.tasks.list`
    pub const SESSION_TASKS_LIST: &str = "session.tasks.list";
    /// `session.tasks.promoteToBackground`
    pub const SESSION_TASKS_PROMOTETOBACKGROUND: &str = "session.tasks.promoteToBackground";
    /// `session.tasks.cancel`
    pub const SESSION_TASKS_CANCEL: &str = "session.tasks.cancel";
    /// `session.tasks.remove`
    pub const SESSION_TASKS_REMOVE: &str = "session.tasks.remove";
    /// `session.tasks.sendMessage`
    pub const SESSION_TASKS_SENDMESSAGE: &str = "session.tasks.sendMessage";
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
    /// `session.commands.list`
    pub const SESSION_COMMANDS_LIST: &str = "session.commands.list";
    /// `session.commands.invoke`
    pub const SESSION_COMMANDS_INVOKE: &str = "session.commands.invoke";
    /// `session.commands.handlePendingCommand`
    pub const SESSION_COMMANDS_HANDLEPENDINGCOMMAND: &str = "session.commands.handlePendingCommand";
    /// `session.commands.respondToQueuedCommand`
    pub const SESSION_COMMANDS_RESPONDTOQUEUEDCOMMAND: &str =
        "session.commands.respondToQueuedCommand";
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
    /// `session.remote.enable`
    pub const SESSION_REMOTE_ENABLE: &str = "session.remote.enable";
    /// `session.remote.disable`
    pub const SESSION_REMOTE_DISABLE: &str = "session.remote.disable";
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

/// Optional GitHub token used to look up quota for a specific user instead of the global auth context.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountGetQuotaRequest {
    /// GitHub token for per-user quota lookup. When provided, resolves this token to determine the user's quota instead of using the global auth.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_hub_token: Option<String>,
}

/// Schema for the `AccountQuotaSnapshot` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// Quota usage snapshots for the resolved user, keyed by quota type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountGetQuotaResult {
    /// Quota snapshots keyed by type (e.g., chat, completions, premium_interactions)
    pub quota_snapshots: HashMap<String, AccountQuotaSnapshot>,
}

/// Schema for the `AgentInfo` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentInfo {
    /// Description of the agent's purpose
    pub description: String,
    /// Human-readable display name
    pub display_name: String,
    /// Unique identifier of the custom agent
    pub name: String,
    /// Absolute local file path of the agent definition. Only set for file-based agents loaded from disk; remote agents do not have a path.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
}

/// The currently selected custom agent, or null when using the default agent.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentGetCurrentResult {
    /// Currently selected custom agent, or null if using the default agent
    pub agent: AgentInfo,
}

/// Custom agents available to the session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentList {
    /// Available custom agents
    pub agents: Vec<AgentInfo>,
}

/// Custom agents available to the session after reloading definitions from disk.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentReloadResult {
    /// Reloaded custom agents
    pub agents: Vec<AgentInfo>,
}

/// Name of the custom agent to select for subsequent turns.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentSelectRequest {
    /// Name of the custom agent to select
    pub name: String,
}

/// The newly selected custom agent.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentSelectResult {
    /// The newly selected custom agent
    pub agent: AgentInfo,
}

/// Optional unstructured input hint
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SlashCommandInput {
    /// Optional completion hint for the input (e.g. 'directory' for filesystem path completion)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub completion: Option<SlashCommandInputCompletion>,
    /// Hint to display when command input has not been provided
    pub hint: String,
    /// When true, clients should pass the full text after the command name as a single argument rather than splitting on whitespace
    #[serde(skip_serializing_if = "Option::is_none")]
    pub preserve_multiline_input: Option<bool>,
    /// When true, the command requires non-empty input; clients should render the input hint as required
    #[serde(skip_serializing_if = "Option::is_none")]
    pub required: Option<bool>,
}

/// Schema for the `SlashCommandInfo` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SlashCommandInfo {
    /// Canonical aliases without leading slashes
    #[serde(default)]
    pub aliases: Vec<String>,
    /// Whether the command may run while an agent turn is active
    pub allow_during_agent_execution: bool,
    /// Human-readable command description
    pub description: String,
    /// Whether the command is experimental
    #[serde(skip_serializing_if = "Option::is_none")]
    pub experimental: Option<bool>,
    /// Optional unstructured input hint
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input: Option<SlashCommandInput>,
    /// Coarse command category for grouping and behavior: runtime built-in, skill-backed command, or SDK/client-owned command
    pub kind: SlashCommandKind,
    /// Canonical command name without a leading slash
    pub name: String,
}

/// Slash commands available in the session, after applying any include/exclude filters.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandList {
    /// Commands available in this session
    pub commands: Vec<SlashCommandInfo>,
}

/// Pending command request ID and an optional error if the client handler failed.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsHandlePendingCommandRequest {
    /// Error message if the command handler failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Request ID from the command invocation event
    pub request_id: RequestId,
}

/// Indicates whether the pending client-handled command was completed successfully.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsHandlePendingCommandResult {
    /// Whether the command was handled successfully
    pub success: bool,
}

/// Slash command name and optional raw input string to invoke.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsInvokeRequest {
    /// Raw input after the command name
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input: Option<String>,
    /// Command name. Leading slashes are stripped and the name is matched case-insensitively.
    pub name: String,
}

/// Optional filters controlling which command sources to include in the listing.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsListRequest {
    /// Include runtime built-in commands
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_builtins: Option<bool>,
    /// Include commands registered by protocol clients, including SDK clients and extensions
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_client_commands: Option<bool>,
    /// Include enabled user-invocable skills and commands
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_skills: Option<bool>,
}

/// Queued command request ID and the result indicating whether the client handled it.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsRespondToQueuedCommandRequest {
    /// Request ID from the queued command event
    pub request_id: RequestId,
    /// Result of the queued command execution
    pub result: serde_json::Value,
}

/// Indicates whether the queued-command response was accepted by the session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsRespondToQueuedCommandResult {
    /// Whether the response was accepted (false if the requestId was not found or already resolved)
    pub success: bool,
}

/// Repository associated with the connected remote session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectedRemoteSessionMetadataRepository {
    /// Branch associated with the remote session.
    pub branch: String,
    /// Repository name.
    pub name: String,
    /// Repository owner or organization login.
    pub owner: String,
}

/// Metadata for a connected remote session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectedRemoteSessionMetadata {
    /// Neutral SDK discriminator for the connected remote session kind.
    pub kind: ConnectedRemoteSessionMetadataKind,
    /// Last session update time as an ISO 8601 string.
    pub modified_time: String,
    /// Optional friendly session name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Pull request number associated with the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pull_request_number: Option<i64>,
    /// Repository associated with the connected remote session.
    pub repository: ConnectedRemoteSessionMetadataRepository,
    /// Original remote resource identifier.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resource_id: Option<String>,
    /// SDK session ID for the connected remote session.
    pub session_id: SessionId,
    /// Remote session staleness deadline as an ISO 8601 string.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub stale_at: Option<String>,
    /// Session start time as an ISO 8601 string.
    pub start_time: String,
    /// Remote session state returned by the backing service.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub state: Option<String>,
    /// Optional session summary.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
}

/// Remote session connection parameters.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectRemoteSessionParams {
    /// Session ID to connect to.
    pub session_id: SessionId,
}

/// Optional connection token presented by the SDK client during the handshake.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectRequest {
    /// Connection token; required when the server was started with COPILOT_CONNECTION_TOKEN
    #[serde(skip_serializing_if = "Option::is_none")]
    pub token: Option<String>,
}

/// Handshake result reporting the server's protocol version and package version on success.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectResult {
    /// Always true on success
    pub ok: bool,
    /// Server protocol version number
    pub protocol_version: i64,
    /// Server package version
    pub version: String,
}

/// The currently selected model for the session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CurrentModel {
    /// Currently active model identifier
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
}

/// Schema for the `DiscoveredMcpServer` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// Schema for the `Extension` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// Extensions discovered for the session, with their current status.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionList {
    /// Discovered extensions and their current status
    pub extensions: Vec<Extension>,
}

/// Source-qualified extension identifier to disable for the session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionsDisableRequest {
    /// Source-qualified extension ID to disable
    pub id: String,
}

/// Source-qualified extension identifier to enable for the session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionsEnableRequest {
    /// Source-qualified extension ID to enable
    pub id: String,
}

/// Expanded external tool result payload
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlm {
    /// Structured content blocks from the tool
    #[serde(default)]
    pub contents: Vec<serde_json::Value>,
    /// Optional error message for failed executions
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Execution outcome classification. Optional for back-compat; normalized to 'success' (or 'failure' when error is present) when missing or unrecognized.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result_type: Option<String>,
    /// Detailed log content for timeline display
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_log: Option<String>,
    /// Text result returned to the model
    pub text_result_for_llm: String,
    /// Optional tool-specific telemetry
    #[serde(default)]
    pub tool_telemetry: HashMap<String, serde_json::Value>,
}

/// Audio content block with base64-encoded data
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmContentAudio {
    /// Base64-encoded audio data
    pub data: String,
    /// MIME type of the audio (e.g., audio/wav, audio/mpeg)
    pub mime_type: String,
    /// Content block type discriminator
    pub r#type: ExternalToolTextResultForLlmContentAudioType,
}

/// Image content block with base64-encoded data
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmContentImage {
    /// Base64-encoded image data
    pub data: String,
    /// MIME type of the image (e.g., image/png, image/jpeg)
    pub mime_type: String,
    /// Content block type discriminator
    pub r#type: ExternalToolTextResultForLlmContentImageType,
}

/// Embedded resource content block with inline text or binary data
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmContentResource {
    /// The embedded resource contents, either text or base64-encoded binary
    pub resource: serde_json::Value,
    /// Content block type discriminator
    pub r#type: ExternalToolTextResultForLlmContentResourceType,
}

/// Icon image for a resource
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmContentResourceLinkIcon {
    /// MIME type of the icon image
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mime_type: Option<String>,
    /// Available icon sizes (e.g., ['16x16', '32x32'])
    #[serde(default)]
    pub sizes: Vec<String>,
    /// URL or path to the icon image
    pub src: String,
    /// Theme variant this icon is intended for
    #[serde(skip_serializing_if = "Option::is_none")]
    pub theme: Option<ExternalToolTextResultForLlmContentResourceLinkIconTheme>,
}

/// Resource link content block referencing an external resource
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmContentResourceLink {
    /// Human-readable description of the resource
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Icons associated with this resource
    #[serde(default)]
    pub icons: Vec<ExternalToolTextResultForLlmContentResourceLinkIcon>,
    /// MIME type of the resource content
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mime_type: Option<String>,
    /// Resource name identifier
    pub name: String,
    /// Size of the resource in bytes
    #[serde(skip_serializing_if = "Option::is_none")]
    pub size: Option<f64>,
    /// Human-readable display title for the resource
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Content block type discriminator
    pub r#type: ExternalToolTextResultForLlmContentResourceLinkType,
    /// URI identifying the resource
    pub uri: String,
}

/// Terminal/shell output content block with optional exit code and working directory
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmContentTerminal {
    /// Working directory where the command was executed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Process exit code, if the command has completed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exit_code: Option<f64>,
    /// Terminal/shell output text
    pub text: String,
    /// Content block type discriminator
    pub r#type: ExternalToolTextResultForLlmContentTerminalType,
}

/// Plain text content block
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmContentText {
    /// The text content
    pub text: String,
    /// Content block type discriminator
    pub r#type: ExternalToolTextResultForLlmContentTextType,
}

/// Optional user prompt to combine with the fleet orchestration instructions.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FleetStartRequest {
    /// Optional user prompt to combine with fleet instructions
    #[serde(skip_serializing_if = "Option::is_none")]
    pub prompt: Option<String>,
}

/// Indicates whether fleet mode was successfully activated.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FleetStartResult {
    /// Whether fleet mode was successfully activated
    pub started: bool,
}

/// Pending external tool call request ID, with the tool result or an error describing why it failed.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HandlePendingToolCallRequest {
    /// Error message if the tool call failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Request ID of the pending tool call
    pub request_id: RequestId,
    /// Tool call result (string or expanded result object)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<serde_json::Value>,
}

/// Indicates whether the external tool call result was handled successfully.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HandlePendingToolCallResult {
    /// Whether the tool call result was handled successfully
    pub success: bool,
}

/// Post-compaction context window usage breakdown
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// Compaction outcome with the number of tokens and messages removed and the resulting context window breakdown.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// Identifier of the event to truncate to; this event and all later events are removed.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryTruncateRequest {
    /// Event ID to truncate to. This event and all events after it are removed from the session.
    pub event_id: String,
}

/// Number of events that were removed by the truncation.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryTruncateResult {
    /// Number of events that were removed
    pub events_removed: i64,
}

/// Schema for the `InstructionsSources` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// Instruction sources loaded for the session, in merge order.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstructionsGetSourcesResult {
    /// Instruction sources for the session
    pub sources: Vec<InstructionsSources>,
}

/// Message text, optional severity level, persistence flag, and optional follow-up URL.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// Identifier of the session event that was emitted for the log message.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LogResult {
    /// The unique identifier of the emitted session event
    pub event_id: String,
}

/// MCP server name and configuration to add to user configuration.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigAddRequest {
    /// MCP server configuration (local/stdio or remote/http)
    pub config: serde_json::Value,
    /// Unique name for the MCP server
    pub name: String,
}

/// MCP server names to disable for new sessions.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigDisableRequest {
    /// Names of MCP servers to disable. Each server is added to the persisted disabled list so new sessions skip it. Already-disabled names are ignored. Active sessions keep their current connections until they end.
    pub names: Vec<String>,
}

/// MCP server names to enable for new sessions.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigEnableRequest {
    /// Names of MCP servers to enable. Each server is removed from the persisted disabled list so new sessions spawn it. Unknown or already-enabled names are ignored.
    pub names: Vec<String>,
}

/// User-configured MCP servers, keyed by server name.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigList {
    /// All MCP servers from user config, keyed by name
    pub servers: HashMap<String, serde_json::Value>,
}

/// MCP server name to remove from user configuration.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigRemoveRequest {
    /// Name of the MCP server to remove
    pub name: String,
}

/// MCP server name and replacement configuration to write to user configuration.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigUpdateRequest {
    /// MCP server configuration (local/stdio or remote/http)
    pub config: serde_json::Value,
    /// Name of the MCP server to update
    pub name: String,
}

/// Name of the MCP server to disable for the session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpDisableRequest {
    /// Name of the MCP server to disable
    pub server_name: String,
}

/// Optional working directory used as context for MCP server discovery.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpDiscoverRequest {
    /// Working directory used as context for discovery (e.g., plugin resolution)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<String>,
}

/// MCP servers discovered from user, workspace, plugin, and built-in sources.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpDiscoverResult {
    /// MCP servers discovered from all sources
    pub servers: Vec<DiscoveredMcpServer>,
}

/// Name of the MCP server to enable for the session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpEnableRequest {
    /// Name of the MCP server to enable
    pub server_name: String,
}

/// Remote MCP server name and optional overrides controlling reauthentication, OAuth client display name, and the callback success-page copy.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// OAuth authorization URL the caller should open, or empty when cached tokens already authenticated the server.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthLoginResult {
    /// URL the caller should open in a browser to complete OAuth. Omitted when cached tokens were still valid and no browser interaction was needed — the server is already reconnected in that case. When present, the runtime starts the callback listener before returning and continues the flow in the background; completion is signaled via session.mcp_server_status_changed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub authorization_url: Option<String>,
}

/// Schema for the `McpServer` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// Remote MCP server configuration accessed over HTTP or SSE.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerConfigHttp {
    /// Content filtering mode to apply to all tools, or a map of tool name to content filtering mode.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub filter_mapping: Option<serde_json::Value>,
    /// HTTP headers to include in requests to the remote MCP server.
    #[serde(default)]
    pub headers: HashMap<String, String>,
    /// Whether this server is a built-in fallback used when the user has not configured their own server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_default_server: Option<bool>,
    /// OAuth client ID for a pre-registered remote MCP OAuth client.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub oauth_client_id: Option<String>,
    /// OAuth grant type to use when authenticating to the remote MCP server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub oauth_grant_type: Option<McpServerConfigHttpOauthGrantType>,
    /// Whether the configured OAuth client is public and does not require a client secret.
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
    /// URL of the remote MCP server endpoint.
    pub url: String,
}

/// Local MCP server configuration launched as a child process.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerConfigLocal {
    /// Command-line arguments passed to the local MCP server process.
    pub args: Vec<String>,
    /// Executable command used to start the local MCP server process.
    pub command: String,
    /// Working directory for the local MCP server process.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Environment variables to pass to the local MCP server process.
    #[serde(default)]
    pub env: HashMap<String, String>,
    /// Content filtering mode to apply to all tools, or a map of tool name to content filtering mode.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub filter_mapping: Option<serde_json::Value>,
    /// Whether this server is a built-in fallback used when the user has not configured their own server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_default_server: Option<bool>,
    /// Timeout in milliseconds for tool calls to this server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<i64>,
    /// Tools to include. Defaults to all tools if not specified.
    #[serde(default)]
    pub tools: Vec<String>,
    /// Local transport type. Defaults to "local".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<McpServerConfigLocalType>,
}

/// MCP servers configured for the session, with their connection status.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerList {
    /// Configured MCP servers
    pub servers: Vec<McpServer>,
}

/// Token-level pricing information for this model
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelBillingTokenPrices {
    /// Number of tokens per standard billing batch
    #[serde(skip_serializing_if = "Option::is_none")]
    pub batch_size: Option<i64>,
    /// Price per billing batch of cached tokens in nano-AIUs (1 nano-AIU = 0.000000001 AIU, 1 AIU = $0.01 USD)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_price: Option<i64>,
    /// Price per billing batch of input tokens in nano-AIUs (1 nano-AIU = 0.000000001 AIU, 1 AIU = $0.01 USD)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input_price: Option<i64>,
    /// Price per billing batch of output tokens in nano-AIUs (1 nano-AIU = 0.000000001 AIU, 1 AIU = $0.01 USD)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub output_price: Option<i64>,
}

/// Billing information
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelBilling {
    /// Billing cost multiplier relative to the base rate
    #[serde(skip_serializing_if = "Option::is_none")]
    pub multiplier: Option<f64>,
    /// Token-level pricing information for this model
    #[serde(skip_serializing_if = "Option::is_none")]
    pub token_prices: Option<ModelBillingTokenPrices>,
}

/// Vision-specific limits
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelPolicy {
    /// Current policy state for this model
    pub state: String,
    /// Usage terms or conditions for this model
    #[serde(skip_serializing_if = "Option::is_none")]
    pub terms: Option<String>,
}

/// Schema for the `Model` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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
    /// Model capability category for grouping in the model picker
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_picker_category: Option<ModelPickerCategory>,
    /// Relative cost tier for token-based billing users
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_picker_price_category: Option<ModelPickerPriceCategory>,
    /// Display name
    pub name: String,
    /// Policy state (if applicable)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy: Option<ModelPolicy>,
    /// Supported reasoning effort levels (only present if model supports reasoning effort)
    #[serde(default)]
    pub supported_reasoning_efforts: Vec<String>,
}

/// Vision-specific limits
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesOverrideLimits {
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
    pub vision: Option<ModelCapabilitiesOverrideLimitsVision>,
}

/// Feature flags indicating what the model supports
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesOverrideSupports {
    /// Whether this model supports reasoning effort configuration
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<bool>,
    /// Whether this model supports vision/image input
    #[serde(skip_serializing_if = "Option::is_none")]
    pub vision: Option<bool>,
}

/// Override individual model capabilities resolved by the runtime
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesOverride {
    /// Token limits for prompts, outputs, and context window
    #[serde(skip_serializing_if = "Option::is_none")]
    pub limits: Option<ModelCapabilitiesOverrideLimits>,
    /// Feature flags indicating what the model supports
    #[serde(skip_serializing_if = "Option::is_none")]
    pub supports: Option<ModelCapabilitiesOverrideSupports>,
}

/// List of Copilot models available to the resolved user, including capabilities and billing metadata.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelList {
    /// List of available models with full metadata
    pub models: Vec<Model>,
}

/// Optional GitHub token used to list models for a specific user instead of the global auth context.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelsListRequest {
    /// GitHub token for per-user model listing. When provided, resolves this token to determine the user's Copilot plan and available models instead of using the global auth.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_hub_token: Option<String>,
}

/// Target model identifier and optional reasoning effort, summary, and capability overrides.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSwitchToRequest {
    /// Override individual model capabilities resolved by the runtime
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_capabilities: Option<ModelCapabilitiesOverride>,
    /// Model identifier to switch to
    pub model_id: String,
    /// Reasoning effort level to use for the model. "none" disables reasoning.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    /// Reasoning summary mode to request for supported model clients
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_summary: Option<ReasoningSummary>,
}

/// The model identifier active on the session after the switch.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSwitchToResult {
    /// Currently active model identifier after the switch
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
}

/// Agent interaction mode to apply to the session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModeSetRequest {
    /// The agent mode. Valid values: "interactive", "plan", "autopilot".
    pub mode: SessionMode,
}

/// The session's friendly name, or null when not yet set.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct NameGetResult {
    /// The session name (user-set or auto-generated), or null if not yet set
    pub name: Option<String>,
}

/// New friendly name to apply to the session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct NameSetRequest {
    /// New session name (1–100 characters, trimmed of leading/trailing whitespace)
    pub name: String,
}

/// Schema for the `PermissionDecisionApproveOnce` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveOnce {
    /// The permission request was approved for this one instance
    pub kind: PermissionDecisionApproveOnceKind,
}

/// Schema for the `PermissionDecisionApproveForSessionApprovalCommands` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalCommands {
    /// Command identifiers covered by this approval.
    pub command_identifiers: Vec<String>,
    /// Approval scoped to specific command identifiers.
    pub kind: PermissionDecisionApproveForSessionApprovalCommandsKind,
}

/// Schema for the `PermissionDecisionApproveForSessionApprovalRead` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalRead {
    /// Approval covering read-only filesystem operations.
    pub kind: PermissionDecisionApproveForSessionApprovalReadKind,
}

/// Schema for the `PermissionDecisionApproveForSessionApprovalWrite` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalWrite {
    /// Approval covering filesystem write operations.
    pub kind: PermissionDecisionApproveForSessionApprovalWriteKind,
}

/// Schema for the `PermissionDecisionApproveForSessionApprovalMcp` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalMcp {
    /// Approval covering an MCP tool.
    pub kind: PermissionDecisionApproveForSessionApprovalMcpKind,
    /// MCP server name.
    pub server_name: String,
    /// MCP tool name, or null to cover every tool on the server.
    pub tool_name: Option<String>,
}

/// Schema for the `PermissionDecisionApproveForSessionApprovalMcpSampling` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalMcpSampling {
    /// Approval covering MCP sampling requests for a server.
    pub kind: PermissionDecisionApproveForSessionApprovalMcpSamplingKind,
    /// MCP server name.
    pub server_name: String,
}

/// Schema for the `PermissionDecisionApproveForSessionApprovalMemory` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalMemory {
    /// Approval covering writes to long-term memory.
    pub kind: PermissionDecisionApproveForSessionApprovalMemoryKind,
}

/// Schema for the `PermissionDecisionApproveForSessionApprovalCustomTool` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalCustomTool {
    /// Approval covering a custom tool.
    pub kind: PermissionDecisionApproveForSessionApprovalCustomToolKind,
    /// Custom tool name.
    pub tool_name: String,
}

/// Schema for the `PermissionDecisionApproveForSessionApprovalExtensionManagement` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalExtensionManagement {
    /// Approval covering extension lifecycle operations such as enable, disable, or reload.
    pub kind: PermissionDecisionApproveForSessionApprovalExtensionManagementKind,
    /// Optional operation identifier; when omitted, the approval covers all extension management operations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub operation: Option<String>,
}

/// Schema for the `PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess {
    /// Extension name.
    pub extension_name: String,
    /// Approval covering an extension's request to access a permission-gated capability.
    pub kind: PermissionDecisionApproveForSessionApprovalExtensionPermissionAccessKind,
}

/// Schema for the `PermissionDecisionApproveForSession` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSession {
    /// The approval to add as a session-scoped rule
    #[serde(skip_serializing_if = "Option::is_none")]
    pub approval: Option<PermissionDecisionApproveForSessionApproval>,
    /// The URL domain to approve for this session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub domain: Option<String>,
    /// Approved and remembered for the rest of the session
    pub kind: PermissionDecisionApproveForSessionKind,
}

/// Schema for the `PermissionDecisionApproveForLocationApprovalCommands` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalCommands {
    /// Command identifiers covered by this approval.
    pub command_identifiers: Vec<String>,
    /// Approval scoped to specific command identifiers.
    pub kind: PermissionDecisionApproveForLocationApprovalCommandsKind,
}

/// Schema for the `PermissionDecisionApproveForLocationApprovalRead` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalRead {
    /// Approval covering read-only filesystem operations.
    pub kind: PermissionDecisionApproveForLocationApprovalReadKind,
}

/// Schema for the `PermissionDecisionApproveForLocationApprovalWrite` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalWrite {
    /// Approval covering filesystem write operations.
    pub kind: PermissionDecisionApproveForLocationApprovalWriteKind,
}

/// Schema for the `PermissionDecisionApproveForLocationApprovalMcp` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalMcp {
    /// Approval covering an MCP tool.
    pub kind: PermissionDecisionApproveForLocationApprovalMcpKind,
    /// MCP server name.
    pub server_name: String,
    /// MCP tool name, or null to cover every tool on the server.
    pub tool_name: Option<String>,
}

/// Schema for the `PermissionDecisionApproveForLocationApprovalMcpSampling` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalMcpSampling {
    /// Approval covering MCP sampling requests for a server.
    pub kind: PermissionDecisionApproveForLocationApprovalMcpSamplingKind,
    /// MCP server name.
    pub server_name: String,
}

/// Schema for the `PermissionDecisionApproveForLocationApprovalMemory` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalMemory {
    /// Approval covering writes to long-term memory.
    pub kind: PermissionDecisionApproveForLocationApprovalMemoryKind,
}

/// Schema for the `PermissionDecisionApproveForLocationApprovalCustomTool` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalCustomTool {
    /// Approval covering a custom tool.
    pub kind: PermissionDecisionApproveForLocationApprovalCustomToolKind,
    /// Custom tool name.
    pub tool_name: String,
}

/// Schema for the `PermissionDecisionApproveForLocationApprovalExtensionManagement` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalExtensionManagement {
    /// Approval covering extension lifecycle operations such as enable, disable, or reload.
    pub kind: PermissionDecisionApproveForLocationApprovalExtensionManagementKind,
    /// Optional operation identifier; when omitted, the approval covers all extension management operations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub operation: Option<String>,
}

/// Schema for the `PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess {
    /// Extension name.
    pub extension_name: String,
    /// Approval covering an extension's request to access a permission-gated capability.
    pub kind: PermissionDecisionApproveForLocationApprovalExtensionPermissionAccessKind,
}

/// Schema for the `PermissionDecisionApproveForLocation` type.
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

/// Schema for the `PermissionDecisionApprovePermanently` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApprovePermanently {
    /// The URL domain to approve permanently
    pub domain: String,
    /// Approved and persisted across sessions
    pub kind: PermissionDecisionApprovePermanentlyKind,
}

/// Schema for the `PermissionDecisionReject` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionReject {
    /// Optional feedback from the user explaining the denial
    #[serde(skip_serializing_if = "Option::is_none")]
    pub feedback: Option<String>,
    /// Denied by the user during an interactive prompt
    pub kind: PermissionDecisionRejectKind,
}

/// Schema for the `PermissionDecisionUserNotAvailable` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionUserNotAvailable {
    /// Denied because user confirmation was unavailable
    pub kind: PermissionDecisionUserNotAvailableKind,
}

/// Pending permission request ID and the decision to apply (approve/reject and scope).
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionRequest {
    /// Request ID of the pending permission request
    pub request_id: RequestId,
    /// Decision to apply to a pending permission request.
    pub result: PermissionDecision,
}

/// Indicates whether the permission decision was applied; false when the request was already resolved.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequestResult {
    /// Whether the permission request was handled successfully
    pub success: bool,
}

/// No parameters; clears all session-scoped tool permission approvals.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsResetSessionApprovalsRequest {}

/// Indicates whether the operation succeeded.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsResetSessionApprovalsResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Whether to auto-approve all tool permission requests for the rest of the session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsSetApproveAllRequest {
    /// Whether to auto-approve all tool permission requests
    pub enabled: bool,
}

/// Indicates whether the operation succeeded.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsSetApproveAllResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Optional message to echo back to the caller.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PingRequest {
    /// Optional message to echo back
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
}

/// Server liveness response, including the echoed message, current timestamp, and protocol version.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PingResult {
    /// Echoed message (or default greeting)
    pub message: String,
    /// Server protocol version number
    pub protocol_version: i64,
    /// Server timestamp in milliseconds
    pub timestamp: i64,
}

/// Existence, contents, and resolved path of the session plan file.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PlanReadResult {
    /// The content of the plan file, or null if it does not exist
    pub content: Option<String>,
    /// Whether the plan file exists in the workspace
    pub exists: bool,
    /// Absolute file path of the plan file, or null if workspace is not enabled
    pub path: Option<String>,
}

/// Replacement contents to write to the session plan file.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PlanUpdateRequest {
    /// The new content for the plan file
    pub content: String,
}

/// Schema for the `Plugin` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// Plugins installed for the session, with their enabled state and version metadata.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginList {
    /// Installed plugins
    pub plugins: Vec<Plugin>,
}

/// Schema for the `QueuedCommandHandled` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueuedCommandHandled {
    /// The command was handled
    pub handled: bool,
    /// If true, stop processing remaining queued items
    #[serde(skip_serializing_if = "Option::is_none")]
    pub stop_processing_queue: Option<bool>,
}

/// Schema for the `QueuedCommandNotHandled` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueuedCommandNotHandled {
    /// The command was not handled
    pub handled: bool,
}

/// Optional remote session mode ("off", "export", or "on"); defaults to enabling both export and remote steering.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteEnableRequest {
    /// Per-session remote mode. "off" disables remote, "export" exports session events to GitHub without enabling remote steering, "on" enables both export and remote steering.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<RemoteSessionMode>,
}

/// GitHub URL for the session and a flag indicating whether remote steering is enabled.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteEnableResult {
    /// Whether remote steering is enabled
    pub remote_steerable: bool,
    /// GitHub frontend URL for this session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Remote session connection result.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteSessionConnectionResult {
    /// Metadata for a connected remote session.
    pub metadata: ConnectedRemoteSessionMetadata,
    /// SDK session ID for the connected remote session.
    pub session_id: SessionId,
}

/// Schema for the `ServerSkill` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// Skills discovered across global and project sources.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ServerSkillList {
    /// All discovered skills across all sources
    pub skills: Vec<ServerSkill>,
}

/// Authentication status and account metadata for the session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// File path, content to append, and optional mode for the client-provided session filesystem.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsError {
    /// Error classification
    pub code: SessionFsErrorCode,
    /// Free-form detail about the error, for logging/diagnostics
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
}

/// Path to test for existence in the client-provided session filesystem.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsExistsRequest {
    /// Path using SessionFs conventions
    pub path: String,
}

/// Indicates whether the requested path exists in the client-provided session filesystem.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsExistsResult {
    /// Whether the path exists
    pub exists: bool,
}

/// Directory path to create in the client-provided session filesystem, with options for recursive creation and POSIX mode.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// Directory path whose entries should be listed from the client-provided session filesystem.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirRequest {
    /// Path using SessionFs conventions
    pub path: String,
}

/// Names of entries in the requested directory, or a filesystem error if the read failed.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirResult {
    /// Entry names in the directory
    pub entries: Vec<String>,
    /// Describes a filesystem error.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<SessionFsError>,
}

/// Schema for the `SessionFsReaddirWithTypesEntry` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirWithTypesEntry {
    /// Entry name
    pub name: String,
    /// Entry type
    pub r#type: SessionFsReaddirWithTypesEntryType,
}

/// Directory path whose entries (with type information) should be listed from the client-provided session filesystem.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirWithTypesRequest {
    /// Path using SessionFs conventions
    pub path: String,
}

/// Entries in the requested directory paired with file/directory type information, or a filesystem error if the read failed.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirWithTypesResult {
    /// Directory entries with type information
    pub entries: Vec<SessionFsReaddirWithTypesEntry>,
    /// Describes a filesystem error.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<SessionFsError>,
}

/// Path of the file to read from the client-provided session filesystem.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReadFileRequest {
    /// Path using SessionFs conventions
    pub path: String,
}

/// File content as a UTF-8 string, or a filesystem error if the read failed.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReadFileResult {
    /// File content as UTF-8 string
    pub content: String,
    /// Describes a filesystem error.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<SessionFsError>,
}

/// Source and destination paths for renaming or moving an entry in the client-provided session filesystem.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsRenameRequest {
    /// Destination path using SessionFs conventions
    pub dest: String,
    /// Source path using SessionFs conventions
    pub src: String,
}

/// Path to remove from the client-provided session filesystem, with options for recursive removal and force.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// Initial working directory, session-state path layout, and path conventions used to register the calling SDK client as the session filesystem provider.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSetProviderRequest {
    /// Path conventions used by this filesystem
    pub conventions: SessionFsSetProviderConventions,
    /// Initial working directory for sessions
    pub initial_cwd: String,
    /// Path within each session's SessionFs where the runtime stores files for that session
    pub session_state_path: String,
}

/// Indicates whether the calling client was registered as the session filesystem provider.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSetProviderResult {
    /// Whether the provider was set successfully
    pub success: bool,
}

/// Path whose metadata should be returned from the client-provided session filesystem.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsStatRequest {
    /// Path using SessionFs conventions
    pub path: String,
}

/// Filesystem metadata for the requested path, or a filesystem error if the stat failed.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// File path, content to write, and optional mode for the client-provided session filesystem.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// Source session identifier to fork from, optional event-ID boundary, and optional friendly name for the new session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsForkRequest {
    /// Optional friendly name to assign to the forked session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Source session ID to fork from
    pub session_id: SessionId,
    /// Optional event ID boundary. When provided, the fork includes only events before this ID (exclusive). When omitted, all events are included.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub to_event_id: Option<String>,
}

/// Identifier and optional friendly name assigned to the newly forked session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsForkResult {
    /// Friendly name assigned to the forked session, if any.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// The new forked session's ID
    pub session_id: SessionId,
}

/// Shell command to run, with optional working directory and timeout in milliseconds.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// Identifier of the spawned process, used to correlate streamed output and exit notifications.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellExecResult {
    /// Unique identifier for tracking streamed output
    pub process_id: String,
}

/// Identifier of a process previously returned by "shell.exec" and the signal to send.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellKillRequest {
    /// Process identifier returned by shell.exec
    pub process_id: String,
    /// Signal to send (default: SIGTERM)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub signal: Option<ShellKillSignal>,
}

/// Indicates whether the signal was delivered; false if the process was unknown or already exited.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellKillResult {
    /// Whether the signal was sent successfully
    pub killed: bool,
}

/// Schema for the `Skill` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// Skills available to the session, with their enabled state.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillList {
    /// Available skills
    pub skills: Vec<Skill>,
}

/// Skill names to mark as disabled in global configuration, replacing any previous list.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsConfigSetDisabledSkillsRequest {
    /// List of skill names to disable
    pub disabled_skills: Vec<String>,
}

/// Name of the skill to disable for the session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsDisableRequest {
    /// Name of the skill to disable
    pub name: String,
}

/// Optional project paths and additional skill directories to include in discovery.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsDiscoverRequest {
    /// Optional list of project directory paths to scan for project-scoped skills
    #[serde(default)]
    pub project_paths: Vec<String>,
    /// Optional list of additional skill directory paths to include
    #[serde(default)]
    pub skill_directories: Vec<String>,
}

/// Name of the skill to enable for the session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsEnableRequest {
    /// Name of the skill to enable
    pub name: String,
}

/// Diagnostics from reloading skill definitions, with warnings and errors as separate lists.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsLoadDiagnostics {
    /// Errors emitted while loading skills (e.g. skills that failed to load entirely)
    pub errors: Vec<String>,
    /// Warnings emitted while loading skills (e.g. skills that loaded but had issues)
    pub warnings: Vec<String>,
}

/// Schema for the `SlashCommandAgentPromptResult` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SlashCommandAgentPromptResult {
    /// Prompt text to display to the user
    pub display_prompt: String,
    /// Agent prompt result discriminator
    pub kind: SlashCommandAgentPromptResultKind,
    /// Optional target session mode
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<SlashCommandAgentPromptMode>,
    /// Prompt to submit to the agent
    pub prompt: String,
    /// True when the invocation mutated user runtime settings; consumers caching settings should refresh
    #[serde(skip_serializing_if = "Option::is_none")]
    pub runtime_settings_changed: Option<bool>,
}

/// Schema for the `SlashCommandCompletedResult` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SlashCommandCompletedResult {
    /// Completed result discriminator
    pub kind: SlashCommandCompletedResultKind,
    /// Optional user-facing message describing the completed command
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// True when the invocation mutated user runtime settings; consumers caching settings should refresh
    #[serde(skip_serializing_if = "Option::is_none")]
    pub runtime_settings_changed: Option<bool>,
}

/// Schema for the `SlashCommandTextResult` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SlashCommandTextResult {
    /// Text result discriminator
    pub kind: SlashCommandTextResultKind,
    /// Whether text contains Markdown
    #[serde(skip_serializing_if = "Option::is_none")]
    pub markdown: Option<bool>,
    /// Whether ANSI sequences should be preserved
    #[serde(skip_serializing_if = "Option::is_none")]
    pub preserve_ansi: Option<bool>,
    /// True when the invocation mutated user runtime settings; consumers caching settings should refresh
    #[serde(skip_serializing_if = "Option::is_none")]
    pub runtime_settings_changed: Option<bool>,
    /// Text output for the client to render
    pub text: String,
}

/// Schema for the `TaskAgentInfo` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskAgentInfo {
    /// ISO 8601 timestamp when the current active period began
    #[serde(skip_serializing_if = "Option::is_none")]
    pub active_started_at: Option<String>,
    /// Accumulated active execution time in milliseconds
    #[serde(skip_serializing_if = "Option::is_none")]
    pub active_time_ms: Option<i64>,
    /// Type of agent running this task
    pub agent_type: String,
    /// Whether the task is currently in the original sync wait and can be moved to background mode. False once it is already backgrounded, idle, finished, or no longer has a promotable sync waiter.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub can_promote_to_background: Option<bool>,
    /// ISO 8601 timestamp when the task finished
    #[serde(skip_serializing_if = "Option::is_none")]
    pub completed_at: Option<String>,
    /// Short description of the task
    pub description: String,
    /// Error message when the task failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// How the agent is currently being managed by the runtime
    #[serde(skip_serializing_if = "Option::is_none")]
    pub execution_mode: Option<TaskAgentInfoExecutionMode>,
    /// Unique task identifier
    pub id: String,
    /// ISO 8601 timestamp when the agent entered idle state
    #[serde(skip_serializing_if = "Option::is_none")]
    pub idle_since: Option<String>,
    /// Most recent response text from the agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub latest_response: Option<String>,
    /// Model used for the task when specified
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Prompt passed to the agent
    pub prompt: String,
    /// Result text from the task when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<String>,
    /// ISO 8601 timestamp when the task was started
    pub started_at: String,
    /// Current lifecycle status of the task
    pub status: TaskAgentInfoStatus,
    /// Tool call ID associated with this agent task
    pub tool_call_id: String,
    /// Task kind
    pub r#type: TaskAgentInfoType,
}

/// Background tasks currently tracked by the session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskList {
    /// Currently tracked tasks
    pub tasks: Vec<serde_json::Value>,
}

/// Identifier of the background task to cancel.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksCancelRequest {
    /// Task identifier
    pub id: String,
}

/// Indicates whether the background task was successfully cancelled.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksCancelResult {
    /// Whether the task was successfully cancelled
    pub cancelled: bool,
}

/// Schema for the `TaskShellInfo` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskShellInfo {
    /// Whether the shell runs inside a managed PTY session or as an independent background process
    pub attachment_mode: TaskShellInfoAttachmentMode,
    /// Whether this shell task can be promoted to background mode
    #[serde(skip_serializing_if = "Option::is_none")]
    pub can_promote_to_background: Option<bool>,
    /// Command being executed
    pub command: String,
    /// ISO 8601 timestamp when the task finished
    #[serde(skip_serializing_if = "Option::is_none")]
    pub completed_at: Option<String>,
    /// Short description of the task
    pub description: String,
    /// Whether the shell command is currently sync-waited or background-managed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub execution_mode: Option<TaskShellInfoExecutionMode>,
    /// Unique task identifier
    pub id: String,
    /// Path to the detached shell log, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub log_path: Option<String>,
    /// Process ID when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pid: Option<i64>,
    /// ISO 8601 timestamp when the task was started
    pub started_at: String,
    /// Current lifecycle status of the task
    pub status: TaskShellInfoStatus,
    /// Task kind
    pub r#type: TaskShellInfoType,
}

/// Identifier of the task to promote to background mode.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksPromoteToBackgroundRequest {
    /// Task identifier
    pub id: String,
}

/// Indicates whether the task was successfully promoted to background mode.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksPromoteToBackgroundResult {
    /// Whether the task was successfully promoted to background mode
    pub promoted: bool,
}

/// Identifier of the completed or cancelled task to remove from tracking.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksRemoveRequest {
    /// Task identifier
    pub id: String,
}

/// Indicates whether the task was removed. False when the task does not exist or is still running/idle.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksRemoveResult {
    /// Whether the task was removed. Returns false if the task does not exist or is still running/idle (cancel it first).
    pub removed: bool,
}

/// Identifier of the target agent task, message content, and optional sender agent ID.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksSendMessageRequest {
    /// Agent ID of the sender, if sent on behalf of another agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub from_agent_id: Option<String>,
    /// Agent task identifier
    pub id: String,
    /// Message content to send to the agent
    pub message: String,
}

/// Indicates whether the message was delivered, with an error message when delivery failed.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksSendMessageResult {
    /// Error message if delivery failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Whether the message was successfully delivered or steered
    pub sent: bool,
}

/// Agent type, prompt, name, and optional description and model override for the new task.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksStartAgentRequest {
    /// Type of agent to start (e.g., 'explore', 'task', 'general-purpose')
    pub agent_type: String,
    /// Short description of the task
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Optional model override
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Short name for the agent, used to generate a human-readable ID
    pub name: String,
    /// Task prompt for the agent
    pub prompt: String,
}

/// Identifier assigned to the newly started background agent task.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksStartAgentResult {
    /// Generated agent ID for the background task
    pub agent_id: String,
}

/// Schema for the `Tool` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// Built-in tools available for the requested model, with their parameters and instructions.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolList {
    /// List of available built-in tools with metadata
    pub tools: Vec<Tool>,
}

/// Optional model identifier whose tool overrides should be applied to the listing.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsListRequest {
    /// Optional model ID — when provided, the returned tool list reflects model-specific overrides
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
}

/// Schema for the `UIElicitationArrayAnyOfFieldItemsAnyOf` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationArrayAnyOfFieldItemsAnyOf {
    /// Value submitted when this option is selected.
    pub r#const: String,
    /// Display label for this option.
    pub title: String,
}

/// Schema applied to each item in the array.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationArrayAnyOfFieldItems {
    /// Selectable options, each with a value and a display label.
    pub any_of: Vec<UIElicitationArrayAnyOfFieldItemsAnyOf>,
}

/// Multi-select string field where each option pairs a value with a display label.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationArrayAnyOfField {
    /// Default values selected when the form is first shown.
    #[serde(default)]
    pub default: Vec<String>,
    /// Help text describing the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Schema applied to each item in the array.
    pub items: UIElicitationArrayAnyOfFieldItems,
    /// Maximum number of items the user may select.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_items: Option<f64>,
    /// Minimum number of items the user must select.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub min_items: Option<f64>,
    /// Human-readable label for the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Type discriminator. Always "array".
    pub r#type: UIElicitationArrayAnyOfFieldType,
}

/// Schema applied to each item in the array.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationArrayEnumFieldItems {
    /// Allowed string values for each selected item.
    pub r#enum: Vec<String>,
    /// Type discriminator. Always "string".
    pub r#type: UIElicitationArrayEnumFieldItemsType,
}

/// Multi-select string field whose allowed values are defined inline.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationArrayEnumField {
    /// Default values selected when the form is first shown.
    #[serde(default)]
    pub default: Vec<String>,
    /// Help text describing the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Schema applied to each item in the array.
    pub items: UIElicitationArrayEnumFieldItems,
    /// Maximum number of items the user may select.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_items: Option<f64>,
    /// Minimum number of items the user must select.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub min_items: Option<f64>,
    /// Human-readable label for the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Type discriminator. Always "array".
    pub r#type: UIElicitationArrayEnumFieldType,
}

/// JSON Schema describing the form fields to present to the user
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// Prompt message and JSON schema describing the form fields to elicit from the user.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationRequest {
    /// Message describing what information is needed from the user
    pub message: String,
    /// JSON Schema describing the form fields to present to the user
    pub requested_schema: UIElicitationSchema,
}

/// The elicitation response (accept with form values, decline, or cancel)
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationResponse {
    /// The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
    pub action: UIElicitationResponseAction,
    /// The form values submitted by the user (present when action is 'accept')
    #[serde(default)]
    pub content: HashMap<String, serde_json::Value>,
}

/// Indicates whether the elicitation response was accepted; false if it was already resolved by another client.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationResult {
    /// Whether the response was accepted. False if the request was already resolved by another client.
    pub success: bool,
}

/// Boolean field rendered as a yes/no toggle.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationSchemaPropertyBoolean {
    /// Default value selected when the form is first shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<bool>,
    /// Help text describing the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Human-readable label for the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Type discriminator. Always "boolean".
    pub r#type: UIElicitationSchemaPropertyBooleanType,
}

/// Numeric field accepting either a number or an integer.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationSchemaPropertyNumber {
    /// Default value populated in the input when the form is first shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<f64>,
    /// Help text describing the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Maximum allowed value (inclusive).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub maximum: Option<f64>,
    /// Minimum allowed value (inclusive).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub minimum: Option<f64>,
    /// Human-readable label for the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Numeric type accepted by the field.
    pub r#type: UIElicitationSchemaPropertyNumberType,
}

/// Free-text string field with optional length and format constraints.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationSchemaPropertyString {
    /// Default value populated in the input when the form is first shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<String>,
    /// Help text describing the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Optional format hint that constrains the accepted input.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub format: Option<UIElicitationSchemaPropertyStringFormat>,
    /// Maximum number of characters allowed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_length: Option<f64>,
    /// Minimum number of characters required.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub min_length: Option<f64>,
    /// Human-readable label for the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Type discriminator. Always "string".
    pub r#type: UIElicitationSchemaPropertyStringType,
}

/// Single-select string field whose allowed values are defined inline.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationStringEnumField {
    /// Default value selected when the form is first shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<String>,
    /// Help text describing the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Allowed string values.
    pub r#enum: Vec<String>,
    /// Optional display labels for each enum value, in the same order as `enum`.
    #[serde(default)]
    pub enum_names: Vec<String>,
    /// Human-readable label for the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Type discriminator. Always "string".
    pub r#type: UIElicitationStringEnumFieldType,
}

/// Schema for the `UIElicitationStringOneOfFieldOneOf` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationStringOneOfFieldOneOf {
    /// Value submitted when this option is selected.
    pub r#const: String,
    /// Display label for this option.
    pub title: String,
}

/// Single-select string field where each option pairs a value with a display label.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationStringOneOfField {
    /// Default value selected when the form is first shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<String>,
    /// Help text describing the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Selectable options, each with a value and a display label.
    pub one_of: Vec<UIElicitationStringOneOfFieldOneOf>,
    /// Human-readable label for the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Type discriminator. Always "string".
    pub r#type: UIElicitationStringOneOfFieldType,
}

/// Pending elicitation request ID and the user's response (accept/decline/cancel + form values).
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIHandlePendingElicitationRequest {
    /// The unique request ID from the elicitation.requested event
    pub request_id: RequestId,
    /// The elicitation response (accept with form values, decline, or cancel)
    pub result: UIElicitationResponse,
}

/// Aggregated code change metrics
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UsageMetricsModelMetricRequests {
    /// User-initiated premium request cost (with multiplier applied)
    pub cost: f64,
    /// Number of API requests made with this model
    pub count: i64,
}

/// Schema for the `UsageMetricsModelMetricTokenDetail` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UsageMetricsModelMetricTokenDetail {
    /// Accumulated token count for this token type
    pub token_count: i64,
}

/// Token usage metrics for this model
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// Schema for the `UsageMetricsModelMetric` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UsageMetricsModelMetric {
    /// Request count and cost metrics for this model
    pub requests: UsageMetricsModelMetricRequests,
    /// Token count details per type
    #[serde(default)]
    pub token_details: HashMap<String, UsageMetricsModelMetricTokenDetail>,
    /// Accumulated nano-AI units cost for this model
    #[serde(skip_serializing_if = "Option::is_none")]
    pub total_nano_aiu: Option<i64>,
    /// Token usage metrics for this model
    pub usage: UsageMetricsModelMetricUsage,
}

/// Schema for the `UsageMetricsTokenDetail` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UsageMetricsTokenDetail {
    /// Accumulated token count for this token type
    pub token_count: i64,
}

/// Accumulated session usage metrics, including premium request cost, token counts, model breakdown, and code-change totals.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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
    /// Session-wide per-token-type accumulated token counts
    #[serde(default)]
    pub token_details: HashMap<String, UsageMetricsTokenDetail>,
    /// Total time spent in model API calls (milliseconds)
    pub total_api_duration_ms: f64,
    /// Session-wide accumulated nano-AI units cost
    #[serde(skip_serializing_if = "Option::is_none")]
    pub total_nano_aiu: Option<i64>,
    /// Total user-initiated premium request cost across all models (may be fractional due to multipliers)
    pub total_premium_request_cost: f64,
    /// Raw count of user-initiated API requests
    pub total_user_requests: i64,
}

/// Relative path and UTF-8 content for the workspace file to create or overwrite.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesCreateFileRequest {
    /// File content to write as a UTF-8 string
    pub content: String,
    /// Relative path within the workspace files directory
    pub path: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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
    #[serde(rename = "summary_count", skip_serializing_if = "Option::is_none")]
    pub summary_count: Option<i64>,
    #[serde(rename = "updated_at", skip_serializing_if = "Option::is_none")]
    pub updated_at: Option<String>,
    #[serde(rename = "user_named", skip_serializing_if = "Option::is_none")]
    pub user_named: Option<bool>,
}

/// Current workspace metadata for the session, or null when not available.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesGetWorkspaceResult {
    /// Current workspace metadata, or null if not available
    pub workspace: Option<WorkspacesGetWorkspaceResultWorkspace>,
}

/// Relative paths of files stored in the session workspace files directory.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesListFilesResult {
    /// Relative file paths in the workspace files directory
    pub files: Vec<String>,
}

/// Relative path of the workspace file to read.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesReadFileRequest {
    /// Relative path within the workspace files directory
    pub path: String,
}

/// Contents of the requested workspace file as a UTF-8 string.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesReadFileResult {
    /// File content as a UTF-8 string
    pub content: String,
}

/// List of Copilot models available to the resolved user, including capabilities and billing metadata.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelsListResult {
    /// List of available models with full metadata
    pub models: Vec<Model>,
}

/// Built-in tools available for the requested model, with their parameters and instructions.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsListResult {
    /// List of available built-in tools with metadata
    pub tools: Vec<Tool>,
}

/// User-configured MCP servers, keyed by server name.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigListResult {
    /// All MCP servers from user config, keyed by name
    pub servers: HashMap<String, serde_json::Value>,
}

/// Skills discovered across global and project sources.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsDiscoverResult {
    /// All discovered skills across all sources
    pub skills: Vec<ServerSkill>,
}

/// Remote session connection result.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsConnectResult {
    /// Metadata for a connected remote session.
    pub metadata: ConnectedRemoteSessionMetadata,
    /// SDK session ID for the connected remote session.
    pub session_id: SessionId,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSuspendParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAuthGetStatusParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Authentication status and account metadata for the session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelGetCurrentParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// The currently selected model for the session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelGetCurrentResult {
    /// Currently active model identifier
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
}

/// The model identifier active on the session after the switch.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelSwitchToResult {
    /// Currently active model identifier after the switch
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModeGetParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionNameGetParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// The session's friendly name, or null when not yet set.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionNameGetResult {
    /// The session name (user-set or auto-generated), or null if not yet set
    pub name: Option<String>,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPlanReadParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Existence, contents, and resolved path of the session plan file.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPlanReadResult {
    /// The content of the plan file, or null if it does not exist
    pub content: Option<String>,
    /// Whether the plan file exists in the workspace
    pub exists: bool,
    /// Absolute file path of the plan file, or null if workspace is not enabled
    pub path: Option<String>,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPlanDeleteParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesGetWorkspaceParams {
    /// Target session identifier
    pub session_id: SessionId,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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
    #[serde(rename = "summary_count", skip_serializing_if = "Option::is_none")]
    pub summary_count: Option<i64>,
    #[serde(rename = "updated_at", skip_serializing_if = "Option::is_none")]
    pub updated_at: Option<String>,
    #[serde(rename = "user_named", skip_serializing_if = "Option::is_none")]
    pub user_named: Option<bool>,
}

/// Current workspace metadata for the session, or null when not available.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesGetWorkspaceResult {
    /// Current workspace metadata, or null if not available
    pub workspace: Option<SessionWorkspacesGetWorkspaceResultWorkspace>,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesListFilesParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Relative paths of files stored in the session workspace files directory.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesListFilesResult {
    /// Relative file paths in the workspace files directory
    pub files: Vec<String>,
}

/// Contents of the requested workspace file as a UTF-8 string.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesReadFileResult {
    /// File content as a UTF-8 string
    pub content: String,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionInstructionsGetSourcesParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Instruction sources loaded for the session, in merge order.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionInstructionsGetSourcesResult {
    /// Instruction sources for the session
    pub sources: Vec<InstructionsSources>,
}

/// Indicates whether fleet mode was successfully activated.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFleetStartResult {
    /// Whether fleet mode was successfully activated
    pub started: bool,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Custom agents available to the session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentListResult {
    /// Available custom agents
    pub agents: Vec<AgentInfo>,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentGetCurrentParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// The currently selected custom agent, or null when using the default agent.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentGetCurrentResult {
    /// Currently selected custom agent, or null if using the default agent
    pub agent: AgentInfo,
}

/// The newly selected custom agent.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentSelectResult {
    /// The newly selected custom agent
    pub agent: AgentInfo,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentDeselectParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentReloadParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Custom agents available to the session after reloading definitions from disk.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentReloadResult {
    /// Reloaded custom agents
    pub agents: Vec<AgentInfo>,
}

/// Identifier assigned to the newly started background agent task.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksStartAgentResult {
    /// Generated agent ID for the background task
    pub agent_id: String,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Background tasks currently tracked by the session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksListResult {
    /// Currently tracked tasks
    pub tasks: Vec<serde_json::Value>,
}

/// Indicates whether the task was successfully promoted to background mode.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksPromoteToBackgroundResult {
    /// Whether the task was successfully promoted to background mode
    pub promoted: bool,
}

/// Indicates whether the background task was successfully cancelled.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksCancelResult {
    /// Whether the task was successfully cancelled
    pub cancelled: bool,
}

/// Indicates whether the task was removed. False when the task does not exist or is still running/idle.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksRemoveResult {
    /// Whether the task was removed. Returns false if the task does not exist or is still running/idle (cancel it first).
    pub removed: bool,
}

/// Indicates whether the message was delivered, with an error message when delivery failed.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksSendMessageResult {
    /// Error message if delivery failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Whether the message was successfully delivered or steered
    pub sent: bool,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Skills available to the session, with their enabled state.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsListResult {
    /// Available skills
    pub skills: Vec<Skill>,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsReloadParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Diagnostics from reloading skill definitions, with warnings and errors as separate lists.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsReloadResult {
    /// Errors emitted while loading skills (e.g. skills that failed to load entirely)
    pub errors: Vec<String>,
    /// Warnings emitted while loading skills (e.g. skills that loaded but had issues)
    pub warnings: Vec<String>,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// MCP servers configured for the session, with their connection status.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpListResult {
    /// Configured MCP servers
    pub servers: Vec<McpServer>,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpReloadParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// OAuth authorization URL the caller should open, or empty when cached tokens already authenticated the server.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpOauthLoginResult {
    /// URL the caller should open in a browser to complete OAuth. Omitted when cached tokens were still valid and no browser interaction was needed — the server is already reconnected in that case. When present, the runtime starts the callback listener before returning and continues the flow in the background; completion is signaled via session.mcp_server_status_changed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub authorization_url: Option<String>,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPluginsListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Plugins installed for the session, with their enabled state and version metadata.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPluginsListResult {
    /// Installed plugins
    pub plugins: Vec<Plugin>,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionExtensionsListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Extensions discovered for the session, with their current status.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionExtensionsListResult {
    /// Discovered extensions and their current status
    pub extensions: Vec<Extension>,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionExtensionsReloadParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Indicates whether the external tool call result was handled successfully.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionToolsHandlePendingToolCallResult {
    /// Whether the tool call result was handled successfully
    pub success: bool,
}

/// Slash commands available in the session, after applying any include/exclude filters.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCommandsListResult {
    /// Commands available in this session
    pub commands: Vec<SlashCommandInfo>,
}

/// Indicates whether the pending client-handled command was completed successfully.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCommandsHandlePendingCommandResult {
    /// Whether the command was handled successfully
    pub success: bool,
}

/// Indicates whether the queued-command response was accepted by the session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCommandsRespondToQueuedCommandResult {
    /// Whether the response was accepted (false if the requestId was not found or already resolved)
    pub success: bool,
}

/// The elicitation response (accept with form values, decline, or cancel)
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiElicitationResult {
    /// The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
    pub action: UIElicitationResponseAction,
    /// The form values submitted by the user (present when action is 'accept')
    #[serde(default)]
    pub content: HashMap<String, serde_json::Value>,
}

/// Indicates whether the elicitation response was accepted; false if it was already resolved by another client.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiHandlePendingElicitationResult {
    /// Whether the response was accepted. False if the request was already resolved by another client.
    pub success: bool,
}

/// Indicates whether the permission decision was applied; false when the request was already resolved.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsHandlePendingPermissionRequestResult {
    /// Whether the permission request was handled successfully
    pub success: bool,
}

/// Indicates whether the operation succeeded.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsSetApproveAllResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the operation succeeded.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsResetSessionApprovalsResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Identifier of the session event that was emitted for the log message.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionLogResult {
    /// The unique identifier of the emitted session event
    pub event_id: String,
}

/// Identifier of the spawned process, used to correlate streamed output and exit notifications.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionShellExecResult {
    /// Unique identifier for tracking streamed output
    pub process_id: String,
}

/// Indicates whether the signal was delivered; false if the process was unknown or already exited.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionShellKillResult {
    /// Whether the signal was sent successfully
    pub killed: bool,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistoryCompactParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Compaction outcome with the number of tokens and messages removed and the resulting context window breakdown.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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

/// Number of events that were removed by the truncation.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistoryTruncateResult {
    /// Number of events that were removed
    pub events_removed: i64,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUsageGetMetricsParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Accumulated session usage metrics, including premium request cost, token counts, model breakdown, and code-change totals.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
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
    /// Session-wide per-token-type accumulated token counts
    #[serde(default)]
    pub token_details: HashMap<String, UsageMetricsTokenDetail>,
    /// Total time spent in model API calls (milliseconds)
    pub total_api_duration_ms: f64,
    /// Session-wide accumulated nano-AI units cost
    #[serde(skip_serializing_if = "Option::is_none")]
    pub total_nano_aiu: Option<i64>,
    /// Total user-initiated premium request cost across all models (may be fractional due to multipliers)
    pub total_premium_request_cost: f64,
    /// Raw count of user-initiated API requests
    pub total_user_requests: i64,
}

/// GitHub URL for the session and a flag indicating whether remote steering is enabled.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionRemoteEnableResult {
    /// Whether remote steering is enabled
    pub remote_steerable: bool,
    /// GitHub frontend URL for this session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Identifies the target session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionRemoteDisableParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Authentication type
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
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
    #[default]
    #[serde(other)]
    Unknown,
}

/// Optional completion hint for the input (e.g. 'directory' for filesystem path completion)
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandInputCompletion {
    #[serde(rename = "directory")]
    Directory,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Coarse command category for grouping and behavior: runtime built-in, skill-backed command, or SDK/client-owned command
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandKind {
    #[serde(rename = "builtin")]
    Builtin,
    #[serde(rename = "skill")]
    Skill,
    #[serde(rename = "client")]
    Client,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Neutral SDK discriminator for the connected remote session kind.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ConnectedRemoteSessionMetadataKind {
    #[serde(rename = "remote-session")]
    RemoteSession,
    #[serde(rename = "coding-agent")]
    CodingAgent,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Configuration source
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
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
    #[default]
    #[serde(other)]
    Unknown,
}

/// Server transport type: stdio, http, sse, or memory (local configs are normalized to stdio)
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
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
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/)
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExtensionSource {
    #[serde(rename = "project")]
    Project,
    #[serde(rename = "user")]
    User,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Current status: running, disabled, failed, or starting
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
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
    #[default]
    #[serde(other)]
    Unknown,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmContentAudioType {
    #[serde(rename = "audio")]
    #[default]
    Audio,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmContentImageType {
    #[serde(rename = "image")]
    #[default]
    Image,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmContentResourceType {
    #[serde(rename = "resource")]
    #[default]
    Resource,
}

/// Theme variant this icon is intended for
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmContentResourceLinkIconTheme {
    #[serde(rename = "light")]
    Light,
    #[serde(rename = "dark")]
    Dark,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmContentResourceLinkType {
    #[serde(rename = "resource_link")]
    #[default]
    ResourceLink,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmContentTerminalType {
    #[serde(rename = "terminal")]
    #[default]
    Terminal,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmContentTextType {
    #[serde(rename = "text")]
    #[default]
    Text,
}

/// Allowed values for the `FilterMappingString` enumeration.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum FilterMappingString {
    #[serde(rename = "none")]
    None,
    #[serde(rename = "markdown")]
    Markdown,
    #[serde(rename = "hidden_characters")]
    HiddenCharacters,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Allowed values for the `FilterMappingValue` enumeration.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum FilterMappingValue {
    #[serde(rename = "none")]
    None,
    #[serde(rename = "markdown")]
    Markdown,
    #[serde(rename = "hidden_characters")]
    HiddenCharacters,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Where this source lives — used for UI grouping
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstructionsSourcesLocation {
    #[serde(rename = "user")]
    User,
    #[serde(rename = "repository")]
    Repository,
    #[serde(rename = "working-directory")]
    WorkingDirectory,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Category of instruction source — used for merge logic
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
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
    #[default]
    #[serde(other)]
    Unknown,
}

/// Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionLogLevel {
    #[serde(rename = "info")]
    Info,
    #[serde(rename = "warning")]
    Warning,
    #[serde(rename = "error")]
    Error,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Configuration source: user, workspace, plugin, or builtin
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
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
    #[default]
    #[serde(other)]
    Unknown,
}

/// Connection status: connected, failed, needs-auth, pending, disabled, or not_configured
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
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
    #[default]
    #[serde(other)]
    Unknown,
}

/// OAuth grant type to use when authenticating to the remote MCP server.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpServerConfigHttpOauthGrantType {
    #[serde(rename = "authorization_code")]
    AuthorizationCode,
    #[serde(rename = "client_credentials")]
    ClientCredentials,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Remote transport type. Defaults to "http" when omitted.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpServerConfigHttpType {
    #[serde(rename = "http")]
    Http,
    #[serde(rename = "sse")]
    Sse,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Local transport type. Defaults to "local".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpServerConfigLocalType {
    #[serde(rename = "local")]
    Local,
    #[serde(rename = "stdio")]
    Stdio,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Model capability category for grouping in the model picker
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ModelPickerCategory {
    #[serde(rename = "lightweight")]
    Lightweight,
    #[serde(rename = "versatile")]
    Versatile,
    #[serde(rename = "powerful")]
    Powerful,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Relative cost tier for token-based billing users
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ModelPickerPriceCategory {
    #[serde(rename = "low")]
    Low,
    #[serde(rename = "medium")]
    Medium,
    #[serde(rename = "high")]
    High,
    #[serde(rename = "very_high")]
    VeryHigh,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The agent mode. Valid values: "interactive", "plan", "autopilot".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionMode {
    #[serde(rename = "interactive")]
    Interactive,
    #[serde(rename = "plan")]
    Plan,
    #[serde(rename = "autopilot")]
    Autopilot,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The permission request was approved for this one instance
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveOnceKind {
    #[serde(rename = "approve-once")]
    #[default]
    ApproveOnce,
}

/// Approval scoped to specific command identifiers.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalCommandsKind {
    #[serde(rename = "commands")]
    #[default]
    Commands,
}

/// Approval covering read-only filesystem operations.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalReadKind {
    #[serde(rename = "read")]
    #[default]
    Read,
}

/// Approval covering filesystem write operations.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalWriteKind {
    #[serde(rename = "write")]
    #[default]
    Write,
}

/// Approval covering an MCP tool.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalMcpKind {
    #[serde(rename = "mcp")]
    #[default]
    Mcp,
}

/// Approval covering MCP sampling requests for a server.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalMcpSamplingKind {
    #[serde(rename = "mcp-sampling")]
    #[default]
    McpSampling,
}

/// Approval covering writes to long-term memory.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalMemoryKind {
    #[serde(rename = "memory")]
    #[default]
    Memory,
}

/// Approval covering a custom tool.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalCustomToolKind {
    #[serde(rename = "custom-tool")]
    #[default]
    CustomTool,
}

/// Approval covering extension lifecycle operations such as enable, disable, or reload.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalExtensionManagementKind {
    #[serde(rename = "extension-management")]
    #[default]
    ExtensionManagement,
}

/// Approval covering an extension's request to access a permission-gated capability.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalExtensionPermissionAccessKind {
    #[serde(rename = "extension-permission-access")]
    #[default]
    ExtensionPermissionAccess,
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
    ExtensionManagement(PermissionDecisionApproveForSessionApprovalExtensionManagement),
    ExtensionPermissionAccess(PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess),
}

/// Approved and remembered for the rest of the session
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionKind {
    #[serde(rename = "approve-for-session")]
    #[default]
    ApproveForSession,
}

/// Approval scoped to specific command identifiers.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalCommandsKind {
    #[serde(rename = "commands")]
    #[default]
    Commands,
}

/// Approval covering read-only filesystem operations.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalReadKind {
    #[serde(rename = "read")]
    #[default]
    Read,
}

/// Approval covering filesystem write operations.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalWriteKind {
    #[serde(rename = "write")]
    #[default]
    Write,
}

/// Approval covering an MCP tool.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalMcpKind {
    #[serde(rename = "mcp")]
    #[default]
    Mcp,
}

/// Approval covering MCP sampling requests for a server.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalMcpSamplingKind {
    #[serde(rename = "mcp-sampling")]
    #[default]
    McpSampling,
}

/// Approval covering writes to long-term memory.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalMemoryKind {
    #[serde(rename = "memory")]
    #[default]
    Memory,
}

/// Approval covering a custom tool.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalCustomToolKind {
    #[serde(rename = "custom-tool")]
    #[default]
    CustomTool,
}

/// Approval covering extension lifecycle operations such as enable, disable, or reload.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalExtensionManagementKind {
    #[serde(rename = "extension-management")]
    #[default]
    ExtensionManagement,
}

/// Approval covering an extension's request to access a permission-gated capability.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalExtensionPermissionAccessKind {
    #[serde(rename = "extension-permission-access")]
    #[default]
    ExtensionPermissionAccess,
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
    ExtensionManagement(PermissionDecisionApproveForLocationApprovalExtensionManagement),
    ExtensionPermissionAccess(
        PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess,
    ),
}

/// Approved and persisted for this project location
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationKind {
    #[serde(rename = "approve-for-location")]
    #[default]
    ApproveForLocation,
}

/// Approved and persisted across sessions
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApprovePermanentlyKind {
    #[serde(rename = "approve-permanently")]
    #[default]
    ApprovePermanently,
}

/// Denied by the user during an interactive prompt
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionRejectKind {
    #[serde(rename = "reject")]
    #[default]
    Reject,
}

/// Denied because user confirmation was unavailable
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionUserNotAvailableKind {
    #[serde(rename = "user-not-available")]
    #[default]
    UserNotAvailable,
}

/// Decision to apply to a pending permission request.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum PermissionDecision {
    ApproveOnce(PermissionDecisionApproveOnce),
    ApproveForSession(PermissionDecisionApproveForSession),
    ApproveForLocation(PermissionDecisionApproveForLocation),
    ApprovePermanently(PermissionDecisionApprovePermanently),
    Reject(PermissionDecisionReject),
    UserNotAvailable(PermissionDecisionUserNotAvailable),
}

/// Per-session remote mode. "off" disables remote, "export" exports session events to GitHub without enabling remote steering, "on" enables both export and remote steering.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum RemoteSessionMode {
    #[serde(rename = "off")]
    Off,
    #[serde(rename = "export")]
    Export,
    #[serde(rename = "on")]
    On,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Error classification
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionFsErrorCode {
    ENOENT,
    UNKNOWN,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Entry type
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionFsReaddirWithTypesEntryType {
    #[serde(rename = "file")]
    File,
    #[serde(rename = "directory")]
    Directory,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Path conventions used by this filesystem
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionFsSetProviderConventions {
    #[serde(rename = "windows")]
    Windows,
    #[serde(rename = "posix")]
    Posix,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Signal to send (default: SIGTERM)
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ShellKillSignal {
    SIGTERM,
    SIGKILL,
    SIGINT,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Optional target session mode
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandAgentPromptMode {
    #[serde(rename = "interactive")]
    Interactive,
    #[serde(rename = "plan")]
    Plan,
    #[serde(rename = "autopilot")]
    Autopilot,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Agent prompt result discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandAgentPromptResultKind {
    #[serde(rename = "agent-prompt")]
    #[default]
    AgentPrompt,
}

/// Completed result discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandCompletedResultKind {
    #[serde(rename = "completed")]
    #[default]
    Completed,
}

/// Text result discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandTextResultKind {
    #[serde(rename = "text")]
    #[default]
    Text,
}

/// Result of invoking the slash command (text output, prompt to send to the agent, or completion).
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum SlashCommandInvocationResult {
    Text(SlashCommandTextResult),
    AgentPrompt(SlashCommandAgentPromptResult),
    Completed(SlashCommandCompletedResult),
}

/// How the agent is currently being managed by the runtime
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskAgentInfoExecutionMode {
    #[serde(rename = "sync")]
    Sync,
    #[serde(rename = "background")]
    Background,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Current lifecycle status of the task
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskAgentInfoStatus {
    #[serde(rename = "running")]
    Running,
    #[serde(rename = "idle")]
    Idle,
    #[serde(rename = "completed")]
    Completed,
    #[serde(rename = "failed")]
    Failed,
    #[serde(rename = "cancelled")]
    Cancelled,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Task kind
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskAgentInfoType {
    #[serde(rename = "agent")]
    #[default]
    Agent,
}

/// Whether the shell runs inside a managed PTY session or as an independent background process
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskShellInfoAttachmentMode {
    #[serde(rename = "attached")]
    Attached,
    #[serde(rename = "detached")]
    Detached,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Whether the shell command is currently sync-waited or background-managed
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskShellInfoExecutionMode {
    #[serde(rename = "sync")]
    Sync,
    #[serde(rename = "background")]
    Background,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Current lifecycle status of the task
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskShellInfoStatus {
    #[serde(rename = "running")]
    Running,
    #[serde(rename = "idle")]
    Idle,
    #[serde(rename = "completed")]
    Completed,
    #[serde(rename = "failed")]
    Failed,
    #[serde(rename = "cancelled")]
    Cancelled,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Task kind
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskShellInfoType {
    #[serde(rename = "shell")]
    #[default]
    Shell,
}

/// Type discriminator. Always "array".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationArrayAnyOfFieldType {
    #[serde(rename = "array")]
    #[default]
    Array,
}

/// Type discriminator. Always "string".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationArrayEnumFieldItemsType {
    #[serde(rename = "string")]
    #[default]
    String,
}

/// Type discriminator. Always "array".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationArrayEnumFieldType {
    #[serde(rename = "array")]
    #[default]
    Array,
}

/// Schema type indicator (always 'object')
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationSchemaType {
    #[serde(rename = "object")]
    #[default]
    Object,
}

/// The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationResponseAction {
    #[serde(rename = "accept")]
    Accept,
    #[serde(rename = "decline")]
    Decline,
    #[serde(rename = "cancel")]
    Cancel,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Type discriminator. Always "boolean".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationSchemaPropertyBooleanType {
    #[serde(rename = "boolean")]
    #[default]
    Boolean,
}

/// Numeric type accepted by the field.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationSchemaPropertyNumberType {
    #[serde(rename = "number")]
    Number,
    #[serde(rename = "integer")]
    Integer,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Optional format hint that constrains the accepted input.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
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
    #[default]
    #[serde(other)]
    Unknown,
}

/// Type discriminator. Always "string".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationSchemaPropertyStringType {
    #[serde(rename = "string")]
    #[default]
    String,
}

/// Type discriminator. Always "string".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationStringEnumFieldType {
    #[serde(rename = "string")]
    #[default]
    String,
}

/// Type discriminator. Always "string".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationStringOneOfFieldType {
    #[serde(rename = "string")]
    #[default]
    String,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum WorkspacesGetWorkspaceResultWorkspaceHostType {
    #[serde(rename = "github")]
    Github,
    #[serde(rename = "ado")]
    Ado,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionWorkspacesGetWorkspaceResultWorkspaceHostType {
    #[serde(rename = "github")]
    Github,
    #[serde(rename = "ado")]
    Ado,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}
