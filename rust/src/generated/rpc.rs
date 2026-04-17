// AUTO-GENERATED FILE — DO NOT EDIT
// Generated from: api.schema.json
//
// Run `cd scripts/codegen && npm run generate:rust` to regenerate.

#![allow(clippy::derivable_impls)]
#![allow(deprecated)]
use serde::{Deserialize, Serialize};

/// Server transport type: stdio, http, sse, or memory (local configs are normalized to stdio)
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum McpDiscoverResultServersType {
    #[serde(rename = "stdio")]
    Stdio,
    #[serde(rename = "http")]
    Http,
    #[serde(rename = "sse")]
    Sse,
    #[serde(rename = "memory")]
    Memory,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Configuration source
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum McpDiscoverResultServersSource {
    #[serde(rename = "user")]
    User,
    #[serde(rename = "workspace")]
    Workspace,
    #[serde(rename = "plugin")]
    Plugin,
    #[serde(rename = "builtin")]
    Builtin,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Path conventions used by this filesystem
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum SessionFsSetProviderRequestConventions {
    #[serde(rename = "windows")]
    Windows,
    #[serde(rename = "posix")]
    Posix,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The agent mode. Valid values: "interactive", "plan", "autopilot".
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum SessionMode {
    #[serde(rename = "interactive")]
    Interactive,
    #[serde(rename = "plan")]
    Plan,
    #[serde(rename = "autopilot")]
    Autopilot,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The agent mode. Valid values: "interactive", "plan", "autopilot".
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum ModeSetRequestMode {
    #[serde(rename = "interactive")]
    Interactive,
    #[serde(rename = "plan")]
    Plan,
    #[serde(rename = "autopilot")]
    Autopilot,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum WorkspacesGetWorkspaceResultWorkspaceHostType {
    #[serde(rename = "github")]
    Github,
    #[serde(rename = "ado")]
    Ado,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel {
    #[serde(rename = "local")]
    Local,
    #[serde(rename = "user")]
    User,
    #[serde(rename = "repo_and_user")]
    RepoAndUser,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Connection status: connected, failed, needs-auth, pending, disabled, or not_configured
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum McpServerListServersStatus {
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
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Configuration source: user, workspace, plugin, or builtin
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum McpServerListServersSource {
    #[serde(rename = "user")]
    User,
    #[serde(rename = "workspace")]
    Workspace,
    #[serde(rename = "plugin")]
    Plugin,
    #[serde(rename = "builtin")]
    Builtin,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/)
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum ExtensionListExtensionsSource {
    #[serde(rename = "project")]
    Project,
    #[serde(rename = "user")]
    User,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Current status: running, disabled, failed, or starting
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum ExtensionListExtensionsStatus {
    #[serde(rename = "running")]
    Running,
    #[serde(rename = "disabled")]
    Disabled,
    #[serde(rename = "failed")]
    Failed,
    #[serde(rename = "starting")]
    Starting,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum UIElicitationResponseAction {
    #[serde(rename = "accept")]
    Accept,
    #[serde(rename = "decline")]
    Decline,
    #[serde(rename = "cancel")]
    Cancel,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum UIHandlePendingElicitationRequestResultAction {
    #[serde(rename = "accept")]
    Accept,
    #[serde(rename = "decline")]
    Decline,
    #[serde(rename = "cancel")]
    Cancel,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum LogRequestLevel {
    #[serde(rename = "info")]
    Info,
    #[serde(rename = "warning")]
    Warning,
    #[serde(rename = "error")]
    Error,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Signal to send (default: SIGTERM)
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum ShellKillRequestSignal {
    SIGTERM,
    SIGKILL,
    SIGINT,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Entry type
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum SessionFsReaddirWithTypesResultEntriesType {
    #[serde(rename = "file")]
    File,
    #[serde(rename = "directory")]
    Directory,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PingResult {
    /// Echoed message (or default greeting)
    pub message: String,
    /// Server timestamp in milliseconds
    pub timestamp: i64,
    /// Server protocol version number
    pub protocol_version: i64,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PingRequest {
    /// Optional message to echo back
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
}

/// Feature flags indicating what the model supports
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelListModelsCapabilitiesSupports {
    /// Whether this model supports vision/image input
    #[serde(skip_serializing_if = "Option::is_none")]
    pub vision: Option<bool>,
    /// Whether this model supports reasoning effort configuration
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<bool>,
}

/// Vision-specific limits
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelListModelsCapabilitiesLimitsVision {
    /// MIME types the model accepts
    pub supported_media_types: Vec<String>,
    /// Maximum number of images per prompt
    pub max_prompt_images: i64,
    /// Maximum image size in bytes
    pub max_prompt_image_size: i64,
}

/// Token limits for prompts, outputs, and context window
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelListModelsCapabilitiesLimits {
    /// Maximum number of prompt/input tokens
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_prompt_tokens: Option<i64>,
    /// Maximum number of output/completion tokens
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_output_tokens: Option<i64>,
    /// Maximum total context window size in tokens
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_context_window_tokens: Option<i64>,
    /// Vision-specific limits
    #[serde(skip_serializing_if = "Option::is_none")]
    pub vision: Option<ModelListModelsCapabilitiesLimitsVision>,
}

/// Model capabilities and limits
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelListModelsCapabilities {
    /// Feature flags indicating what the model supports
    #[serde(skip_serializing_if = "Option::is_none")]
    pub supports: Option<ModelListModelsCapabilitiesSupports>,
    /// Token limits for prompts, outputs, and context window
    #[serde(skip_serializing_if = "Option::is_none")]
    pub limits: Option<ModelListModelsCapabilitiesLimits>,
}

/// Policy state (if applicable)
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelListModelsPolicy {
    /// Current policy state for this model
    pub state: String,
    /// Usage terms or conditions for this model
    pub terms: String,
}

/// Billing information
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelListModelsBilling {
    /// Billing cost multiplier relative to the base rate
    pub multiplier: f64,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelListModels {
    /// Model identifier (e.g., "claude-sonnet-4.5")
    pub id: String,
    /// Display name
    pub name: String,
    /// Model capabilities and limits
    pub capabilities: ModelListModelsCapabilities,
    /// Policy state (if applicable)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy: Option<ModelListModelsPolicy>,
    /// Billing information
    #[serde(skip_serializing_if = "Option::is_none")]
    pub billing: Option<ModelListModelsBilling>,
    /// Supported reasoning effort levels (only present if model supports reasoning effort)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub supported_reasoning_efforts: Option<Vec<String>>,
    /// Default reasoning effort level (only present if model supports reasoning effort)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default_reasoning_effort: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelList {
    /// List of available models with full metadata
    pub models: Vec<ModelListModels>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolListTools {
    /// Tool identifier (e.g., "bash", "grep", "str_replace_editor")
    pub name: String,
    /// Optional namespaced name for declarative filtering (e.g., "playwright/navigate" for MCP tools)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub namespaced_name: Option<String>,
    /// Description of what the tool does
    pub description: String,
    /// JSON Schema for the tool's input parameters
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parameters: Option<std::collections::HashMap<String, serde_json::Value>>,
    /// Optional instructions for how to use this tool effectively
    #[serde(skip_serializing_if = "Option::is_none")]
    pub instructions: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolList {
    /// List of available built-in tools with metadata
    pub tools: Vec<ToolListTools>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsListRequest {
    /// Optional model ID — when provided, the returned tool list reflects model-specific overrides
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountGetQuotaResultQuotaSnapshots {
    /// Number of requests included in the entitlement
    pub entitlement_requests: i64,
    /// Number of requests used so far this period
    pub used_requests: i64,
    /// Percentage of entitlement remaining
    pub remaining_percentage: f64,
    /// Number of overage requests made this period
    pub overage: i64,
    /// Whether pay-per-request usage is allowed when quota is exhausted
    pub overage_allowed_with_exhausted_quota: bool,
    /// Date when the quota resets (ISO 8601)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reset_date: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountGetQuotaResult {
    /// Quota snapshots keyed by type (e.g., chat, completions, premium_interactions)
    pub quota_snapshots: std::collections::HashMap<String, AccountGetQuotaResultQuotaSnapshots>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigList {
    /// All MCP servers from user config, keyed by name
    pub servers: std::collections::HashMap<String, serde_json::Value>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigAddRequest {
    /// Unique name for the MCP server
    pub name: String,
    /// MCP server configuration (local/stdio or remote/http)
    pub config: serde_json::Value,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigUpdateRequest {
    /// Name of the MCP server to update
    pub name: String,
    /// MCP server configuration (local/stdio or remote/http)
    pub config: serde_json::Value,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigRemoveRequest {
    /// Name of the MCP server to remove
    pub name: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpDiscoverResultServers {
    /// Server name (config key)
    pub name: String,
    /// Server transport type: stdio, http, sse, or memory (local configs are normalized to stdio)
    #[serde(rename = "type")]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<McpDiscoverResultServersType>,
    /// Configuration source
    pub source: McpDiscoverResultServersSource,
    /// Whether the server is enabled (not in the disabled list)
    pub enabled: bool,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpDiscoverResult {
    /// MCP servers discovered from all sources
    pub servers: Vec<McpDiscoverResultServers>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpDiscoverRequest {
    /// Working directory used as context for discovery (e.g., plugin resolution)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsConfigSetDisabledSkillsRequest {
    /// List of skill names to disable
    pub disabled_skills: Vec<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ServerSkillListSkills {
    /// Unique identifier for the skill
    pub name: String,
    /// Description of what the skill does
    pub description: String,
    /// Source location type (e.g., project, personal-copilot, plugin, builtin)
    pub source: String,
    /// Whether the skill can be invoked by the user as a slash command
    pub user_invocable: bool,
    /// Whether the skill is currently enabled (based on global config)
    pub enabled: bool,
    /// Absolute path to the skill file
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// The project path this skill belongs to (only for project/inherited skills)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_path: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ServerSkillList {
    /// All discovered skills across all sources
    pub skills: Vec<ServerSkillListSkills>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsDiscoverRequest {
    /// Optional list of project directory paths to scan for project-scoped skills
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_paths: Option<Vec<String>>,
    /// Optional list of additional skill directory paths to include
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skill_directories: Option<Vec<String>>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSetProviderResult {
    /// Whether the provider was set successfully
    pub success: bool,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSetProviderRequest {
    /// Initial working directory for sessions
    pub initial_cwd: String,
    /// Path within each session's SessionFs where the runtime stores files for that session
    pub session_state_path: String,
    /// Path conventions used by this filesystem
    pub conventions: SessionFsSetProviderRequestConventions,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsForkResult {
    /// The new forked session's ID
    pub session_id: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsForkRequest {
    /// Source session ID to fork from
    pub session_id: String,
    /// Optional event ID boundary. When provided, the fork includes only events before this ID (exclusive). When omitted, all events are included.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub to_event_id: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CurrentModel {
    /// Currently active model identifier
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSwitchToResult {
    /// Currently active model identifier after the switch
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
}

/// Feature flags indicating what the model supports
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSwitchToRequestModelCapabilitiesSupports {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub vision: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<bool>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSwitchToRequestModelCapabilitiesLimitsVision {
    /// MIME types the model accepts
    #[serde(skip_serializing_if = "Option::is_none")]
    pub supported_media_types: Option<Vec<String>>,
    /// Maximum number of images per prompt
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_prompt_images: Option<i64>,
    /// Maximum image size in bytes
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_prompt_image_size: Option<i64>,
}

/// Token limits for prompts, outputs, and context window
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSwitchToRequestModelCapabilitiesLimits {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_prompt_tokens: Option<i64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_output_tokens: Option<i64>,
    /// Maximum total context window size in tokens
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_context_window_tokens: Option<i64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub vision: Option<ModelSwitchToRequestModelCapabilitiesLimitsVision>,
}

/// Override individual model capabilities resolved by the runtime
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSwitchToRequestModelCapabilities {
    /// Feature flags indicating what the model supports
    #[serde(skip_serializing_if = "Option::is_none")]
    pub supports: Option<ModelSwitchToRequestModelCapabilitiesSupports>,
    /// Token limits for prompts, outputs, and context window
    #[serde(skip_serializing_if = "Option::is_none")]
    pub limits: Option<ModelSwitchToRequestModelCapabilitiesLimits>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSwitchToRequest {
    /// Model identifier to switch to
    pub model_id: String,
    /// Reasoning effort level to use for the model
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    /// Override individual model capabilities resolved by the runtime
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_capabilities: Option<ModelSwitchToRequestModelCapabilities>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModeSetRequest {
    /// The agent mode. Valid values: "interactive", "plan", "autopilot".
    pub mode: ModeSetRequestMode,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct NameGetResult {
    /// The session name, falling back to the auto-generated summary, or null if neither exists
    pub name: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct NameSetRequest {
    /// New session name (1–100 characters, trimmed of leading/trailing whitespace)
    pub name: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PlanReadResult {
    /// Whether the plan file exists in the workspace
    pub exists: bool,
    /// The content of the plan file, or null if it does not exist
    pub content: String,
    /// Absolute file path of the plan file, or null if workspace is not enabled
    pub path: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PlanUpdateRequest {
    /// The new content for the plan file
    pub content: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesGetWorkspaceResultWorkspace {
    pub id: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host_type: Option<WorkspacesGetWorkspaceResultWorkspaceHostType>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary_count: Option<i64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub created_at: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub updated_at: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mc_task_id: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mc_session_id: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mc_last_event_id: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_sync_level: Option<WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pr_create_sync_dismissed: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub chronicle_sync_dismissed: Option<bool>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesGetWorkspaceResult {
    /// Current workspace metadata, or null if not available
    pub workspace: WorkspacesGetWorkspaceResultWorkspace,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesListFilesResult {
    /// Relative file paths in the workspace files directory
    pub files: Vec<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesReadFileResult {
    /// File content as a UTF-8 string
    pub content: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesReadFileRequest {
    /// Relative path within the workspace files directory
    pub path: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesCreateFileRequest {
    /// Relative path within the workspace files directory
    pub path: String,
    /// File content to write as a UTF-8 string
    pub content: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FleetStartResult {
    /// Whether fleet mode was successfully activated
    pub started: bool,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FleetStartRequest {
    /// Optional user prompt to combine with fleet instructions
    #[serde(skip_serializing_if = "Option::is_none")]
    pub prompt: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentListAgents {
    /// Unique identifier of the custom agent
    pub name: String,
    /// Human-readable display name
    pub display_name: String,
    /// Description of the agent's purpose
    pub description: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentList {
    /// Available custom agents
    pub agents: Vec<AgentListAgents>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentGetCurrentResultAgent {
    /// Unique identifier of the custom agent
    pub name: String,
    /// Human-readable display name
    pub display_name: String,
    /// Description of the agent's purpose
    pub description: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentGetCurrentResult {
    /// Currently selected custom agent, or null if using the default agent
    pub agent: AgentGetCurrentResultAgent,
}

/// The newly selected custom agent
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentSelectResultAgent {
    /// Unique identifier of the custom agent
    pub name: String,
    /// Human-readable display name
    pub display_name: String,
    /// Description of the agent's purpose
    pub description: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentSelectResult {
    /// The newly selected custom agent
    pub agent: AgentSelectResultAgent,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentSelectRequest {
    /// Name of the custom agent to select
    pub name: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentReloadResultAgents {
    /// Unique identifier of the custom agent
    pub name: String,
    /// Human-readable display name
    pub display_name: String,
    /// Description of the agent's purpose
    pub description: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentReloadResult {
    /// Reloaded custom agents
    pub agents: Vec<AgentReloadResultAgents>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillListSkills {
    /// Unique identifier for the skill
    pub name: String,
    /// Description of what the skill does
    pub description: String,
    /// Source location type (e.g., project, personal, plugin)
    pub source: String,
    /// Whether the skill can be invoked by the user as a slash command
    pub user_invocable: bool,
    /// Whether the skill is currently enabled
    pub enabled: bool,
    /// Absolute path to the skill file
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillList {
    /// Available skills
    pub skills: Vec<SkillListSkills>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsEnableRequest {
    /// Name of the skill to enable
    pub name: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsDisableRequest {
    /// Name of the skill to disable
    pub name: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerListServers {
    /// Server name (config key)
    pub name: String,
    /// Connection status: connected, failed, needs-auth, pending, disabled, or not_configured
    pub status: McpServerListServersStatus,
    /// Configuration source: user, workspace, plugin, or builtin
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<McpServerListServersSource>,
    /// Error message if the server failed to connect
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerList {
    /// Configured MCP servers
    pub servers: Vec<McpServerListServers>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpEnableRequest {
    /// Name of the MCP server to enable
    pub server_name: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpDisableRequest {
    /// Name of the MCP server to disable
    pub server_name: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginListPlugins {
    /// Plugin name
    pub name: String,
    /// Marketplace the plugin came from
    pub marketplace: String,
    /// Installed version
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
    /// Whether the plugin is currently enabled
    pub enabled: bool,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginList {
    /// Installed plugins
    pub plugins: Vec<PluginListPlugins>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionListExtensions {
    /// Source-qualified ID (e.g., 'project:my-ext', 'user:auth-helper')
    pub id: String,
    /// Extension name (directory name)
    pub name: String,
    /// Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/)
    pub source: ExtensionListExtensionsSource,
    /// Current status: running, disabled, failed, or starting
    pub status: ExtensionListExtensionsStatus,
    /// Process ID if the extension is running
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pid: Option<i64>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionList {
    /// Discovered extensions and their current status
    pub extensions: Vec<ExtensionListExtensions>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionsEnableRequest {
    /// Source-qualified extension ID to enable
    pub id: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionsDisableRequest {
    /// Source-qualified extension ID to disable
    pub id: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HandleToolCallResult {
    /// Whether the tool call result was handled successfully
    pub success: bool,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsHandlePendingToolCallRequest {
    /// Request ID of the pending tool call
    pub request_id: String,
    /// Tool call result (string or expanded result object)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<serde_json::Value>,
    /// Error message if the tool call failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsHandlePendingCommandResult {
    /// Whether the command was handled successfully
    pub success: bool,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsHandlePendingCommandRequest {
    /// Request ID from the command invocation event
    pub request_id: String,
    /// Error message if the command handler failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
}

/// The elicitation response (accept with form values, decline, or cancel)
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationResponse {
    /// The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
    pub action: UIElicitationResponseAction,
    /// The form values submitted by the user (present when action is 'accept')
    #[serde(skip_serializing_if = "Option::is_none")]
    pub content: Option<std::collections::HashMap<String, serde_json::Value>>,
}

/// JSON Schema describing the form fields to present to the user
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationRequestRequestedSchema {
    /// Schema type indicator (always 'object')
    #[serde(rename = "type")]
    pub r#type: String,
    /// Form field definitions, keyed by field name
    pub properties: std::collections::HashMap<String, serde_json::Value>,
    /// List of required field names
    #[serde(skip_serializing_if = "Option::is_none")]
    pub required: Option<Vec<String>>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationRequest {
    /// Message describing what information is needed from the user
    pub message: String,
    /// JSON Schema describing the form fields to present to the user
    pub requested_schema: UIElicitationRequestRequestedSchema,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationResult {
    /// Whether the response was accepted. False if the request was already resolved by another client.
    pub success: bool,
}

/// The elicitation response (accept with form values, decline, or cancel)
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIHandlePendingElicitationRequestResult {
    /// The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
    pub action: UIHandlePendingElicitationRequestResultAction,
    /// The form values submitted by the user (present when action is 'accept')
    #[serde(skip_serializing_if = "Option::is_none")]
    pub content: Option<std::collections::HashMap<String, serde_json::Value>>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIHandlePendingElicitationRequest {
    /// The unique request ID from the elicitation.requested event
    pub request_id: String,
    /// The elicitation response (accept with form values, decline, or cancel)
    pub result: UIHandlePendingElicitationRequestResult,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequestResult {
    /// Whether the permission request was handled successfully
    pub success: bool,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionRequest {
    /// Request ID of the pending permission request
    pub request_id: String,
    pub result: serde_json::Value,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LogResult {
    /// The unique identifier of the emitted session event
    pub event_id: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LogRequest {
    /// Human-readable message
    pub message: String,
    /// Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub level: Option<LogRequestLevel>,
    /// When true, the message is transient and not persisted to the session event log on disk
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ephemeral: Option<bool>,
    /// Optional URL the user can open in their browser for more details
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellExecResult {
    /// Unique identifier for tracking streamed output
    pub process_id: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellExecRequest {
    /// Shell command to execute
    pub command: String,
    /// Working directory (defaults to session working directory)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Timeout in milliseconds (default: 30000)
    #[serde(
        default,
        with = "crate::duration_serde::millis_opt",
        skip_serializing_if = "Option::is_none"
    )]
    pub timeout: Option<std::time::Duration>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellKillResult {
    /// Whether the signal was sent successfully
    pub killed: bool,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellKillRequest {
    /// Process identifier returned by shell.exec
    pub process_id: String,
    /// Signal to send (default: SIGTERM)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub signal: Option<ShellKillRequestSignal>,
}

/// Post-compaction context window usage breakdown
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryCompactResultContextWindow {
    /// Maximum token count for the model's context window
    pub token_limit: i64,
    /// Current total tokens in the context window (system + conversation + tool definitions)
    pub current_tokens: i64,
    /// Current number of messages in the conversation
    pub messages_length: i64,
    /// Token count from system message(s)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub system_tokens: Option<i64>,
    /// Token count from non-system messages (user, assistant, tool)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub conversation_tokens: Option<i64>,
    /// Token count from tool definitions
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_definitions_tokens: Option<i64>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryCompactResult {
    /// Whether compaction completed successfully
    pub success: bool,
    /// Number of tokens freed by compaction
    pub tokens_removed: i64,
    /// Number of messages removed during compaction
    pub messages_removed: i64,
    /// Post-compaction context window usage breakdown
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_window: Option<HistoryCompactResultContextWindow>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryTruncateResult {
    /// Number of events that were removed
    pub events_removed: i64,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryTruncateRequest {
    /// Event ID to truncate to. This event and all events after it are removed from the session.
    pub event_id: String,
}

/// Aggregated code change metrics
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UsageGetMetricsResultCodeChanges {
    /// Total lines of code added
    pub lines_added: i64,
    /// Total lines of code removed
    pub lines_removed: i64,
    /// Number of distinct files modified
    pub files_modified_count: i64,
}

/// Request count and cost metrics for this model
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UsageGetMetricsResultModelMetricsRequests {
    /// Number of API requests made with this model
    pub count: i64,
    /// User-initiated premium request cost (with multiplier applied)
    pub cost: f64,
}

/// Token usage metrics for this model
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UsageGetMetricsResultModelMetricsUsage {
    /// Total input tokens consumed
    pub input_tokens: i64,
    /// Total output tokens produced
    pub output_tokens: i64,
    /// Total tokens read from prompt cache
    pub cache_read_tokens: i64,
    /// Total tokens written to prompt cache
    pub cache_write_tokens: i64,
    /// Total output tokens used for reasoning
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_tokens: Option<i64>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UsageGetMetricsResultModelMetrics {
    /// Request count and cost metrics for this model
    pub requests: UsageGetMetricsResultModelMetricsRequests,
    /// Token usage metrics for this model
    pub usage: UsageGetMetricsResultModelMetricsUsage,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UsageGetMetricsResult {
    /// Total user-initiated premium request cost across all models (may be fractional due to multipliers)
    pub total_premium_request_cost: f64,
    /// Raw count of user-initiated API requests
    pub total_user_requests: i64,
    /// Total time spent in model API calls (milliseconds)
    #[serde(with = "crate::duration_serde::millis_f64")]
    pub total_api_duration_ms: std::time::Duration,
    /// Session start timestamp (epoch milliseconds)
    pub session_start_time: i64,
    /// Aggregated code change metrics
    pub code_changes: UsageGetMetricsResultCodeChanges,
    /// Per-model token and request metrics, keyed by model identifier
    pub model_metrics: std::collections::HashMap<String, UsageGetMetricsResultModelMetrics>,
    /// Currently active model identifier
    #[serde(skip_serializing_if = "Option::is_none")]
    pub current_model: Option<String>,
    /// Input tokens from the most recent main-agent API call
    pub last_call_input_tokens: i64,
    /// Output tokens from the most recent main-agent API call
    pub last_call_output_tokens: i64,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReadFileResult {
    /// File content as UTF-8 string
    pub content: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReadFileRequest {
    /// Target session identifier
    pub session_id: String,
    /// Path using SessionFs conventions
    pub path: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsWriteFileRequest {
    /// Target session identifier
    pub session_id: String,
    /// Path using SessionFs conventions
    pub path: String,
    /// Content to write
    pub content: String,
    /// Optional POSIX-style mode for newly created files
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<i64>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsAppendFileRequest {
    /// Target session identifier
    pub session_id: String,
    /// Path using SessionFs conventions
    pub path: String,
    /// Content to append
    pub content: String,
    /// Optional POSIX-style mode for newly created files
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<i64>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsExistsResult {
    /// Whether the path exists
    pub exists: bool,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsExistsRequest {
    /// Target session identifier
    pub session_id: String,
    /// Path using SessionFs conventions
    pub path: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsStatResult {
    /// Whether the path is a file
    pub is_file: bool,
    /// Whether the path is a directory
    pub is_directory: bool,
    /// File size in bytes
    pub size: i64,
    /// ISO 8601 timestamp of last modification
    pub mtime: String,
    /// ISO 8601 timestamp of creation
    pub birthtime: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsStatRequest {
    /// Target session identifier
    pub session_id: String,
    /// Path using SessionFs conventions
    pub path: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsMkdirRequest {
    /// Target session identifier
    pub session_id: String,
    /// Path using SessionFs conventions
    pub path: String,
    /// Create parent directories as needed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub recursive: Option<bool>,
    /// Optional POSIX-style mode for newly created directories
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<i64>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirResult {
    /// Entry names in the directory
    pub entries: Vec<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirRequest {
    /// Target session identifier
    pub session_id: String,
    /// Path using SessionFs conventions
    pub path: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirWithTypesResultEntries {
    /// Entry name
    pub name: String,
    /// Entry type
    #[serde(rename = "type")]
    pub r#type: SessionFsReaddirWithTypesResultEntriesType,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirWithTypesResult {
    /// Directory entries with type information
    pub entries: Vec<SessionFsReaddirWithTypesResultEntries>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirWithTypesRequest {
    /// Target session identifier
    pub session_id: String,
    /// Path using SessionFs conventions
    pub path: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsRmRequest {
    /// Target session identifier
    pub session_id: String,
    /// Path using SessionFs conventions
    pub path: String,
    /// Remove directories and their contents recursively
    #[serde(skip_serializing_if = "Option::is_none")]
    pub recursive: Option<bool>,
    /// Ignore errors if the path does not exist
    #[serde(skip_serializing_if = "Option::is_none")]
    pub force: Option<bool>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsRenameRequest {
    /// Target session identifier
    pub session_id: String,
    /// Source path using SessionFs conventions
    pub src: String,
    /// Destination path using SessionFs conventions
    pub dest: String,
}

/// RPC method name constants.
pub mod methods {
    /// ping
    pub const PING: &str = "ping";
    /// models.list
    pub const MODELS_LIST: &str = "models.list";
    /// tools.list
    pub const TOOLS_LIST: &str = "tools.list";
    /// account.getQuota
    pub const ACCOUNT_GET_QUOTA: &str = "account.getQuota";
    /// mcp.config.list
    pub const MCP_CONFIG_LIST: &str = "mcp.config.list";
    /// mcp.config.add
    pub const MCP_CONFIG_ADD: &str = "mcp.config.add";
    /// mcp.config.update
    pub const MCP_CONFIG_UPDATE: &str = "mcp.config.update";
    /// mcp.config.remove
    pub const MCP_CONFIG_REMOVE: &str = "mcp.config.remove";
    /// mcp.discover
    pub const MCP_DISCOVER: &str = "mcp.discover";
    /// skills.config.setDisabledSkills
    pub const SKILLS_CONFIG_SET_DISABLED_SKILLS: &str = "skills.config.setDisabledSkills";
    /// skills.discover
    pub const SKILLS_DISCOVER: &str = "skills.discover";
    /// sessionFs.setProvider
    pub const SESSION_FS_SET_PROVIDER: &str = "sessionFs.setProvider";
    /// (Experimental) sessions.fork
    pub const SESSIONS_FORK: &str = "sessions.fork";
    /// session.model.getCurrent
    pub const SESSION_MODEL_GET_CURRENT: &str = "session.model.getCurrent";
    /// session.model.switchTo
    pub const SESSION_MODEL_SWITCH_TO: &str = "session.model.switchTo";
    /// session.mode.get
    pub const SESSION_MODE_GET: &str = "session.mode.get";
    /// session.mode.set
    pub const SESSION_MODE_SET: &str = "session.mode.set";
    /// session.name.get
    pub const SESSION_NAME_GET: &str = "session.name.get";
    /// session.name.set
    pub const SESSION_NAME_SET: &str = "session.name.set";
    /// session.plan.read
    pub const SESSION_PLAN_READ: &str = "session.plan.read";
    /// session.plan.update
    pub const SESSION_PLAN_UPDATE: &str = "session.plan.update";
    /// session.plan.delete
    pub const SESSION_PLAN_DELETE: &str = "session.plan.delete";
    /// session.workspaces.getWorkspace
    pub const SESSION_WORKSPACES_GET_WORKSPACE: &str = "session.workspaces.getWorkspace";
    /// session.workspaces.listFiles
    pub const SESSION_WORKSPACES_LIST_FILES: &str = "session.workspaces.listFiles";
    /// session.workspaces.readFile
    pub const SESSION_WORKSPACES_READ_FILE: &str = "session.workspaces.readFile";
    /// session.workspaces.createFile
    pub const SESSION_WORKSPACES_CREATE_FILE: &str = "session.workspaces.createFile";
    /// (Experimental) session.fleet.start
    pub const SESSION_FLEET_START: &str = "session.fleet.start";
    /// (Experimental) session.agent.list
    pub const SESSION_AGENT_LIST: &str = "session.agent.list";
    /// (Experimental) session.agent.getCurrent
    pub const SESSION_AGENT_GET_CURRENT: &str = "session.agent.getCurrent";
    /// (Experimental) session.agent.select
    pub const SESSION_AGENT_SELECT: &str = "session.agent.select";
    /// (Experimental) session.agent.deselect
    pub const SESSION_AGENT_DESELECT: &str = "session.agent.deselect";
    /// (Experimental) session.agent.reload
    pub const SESSION_AGENT_RELOAD: &str = "session.agent.reload";
    /// (Experimental) session.skills.list
    pub const SESSION_SKILLS_LIST: &str = "session.skills.list";
    /// (Experimental) session.skills.enable
    pub const SESSION_SKILLS_ENABLE: &str = "session.skills.enable";
    /// (Experimental) session.skills.disable
    pub const SESSION_SKILLS_DISABLE: &str = "session.skills.disable";
    /// (Experimental) session.skills.reload
    pub const SESSION_SKILLS_RELOAD: &str = "session.skills.reload";
    /// (Experimental) session.mcp.list
    pub const SESSION_MCP_LIST: &str = "session.mcp.list";
    /// (Experimental) session.mcp.enable
    pub const SESSION_MCP_ENABLE: &str = "session.mcp.enable";
    /// (Experimental) session.mcp.disable
    pub const SESSION_MCP_DISABLE: &str = "session.mcp.disable";
    /// (Experimental) session.mcp.reload
    pub const SESSION_MCP_RELOAD: &str = "session.mcp.reload";
    /// (Experimental) session.plugins.list
    pub const SESSION_PLUGINS_LIST: &str = "session.plugins.list";
    /// (Experimental) session.extensions.list
    pub const SESSION_EXTENSIONS_LIST: &str = "session.extensions.list";
    /// (Experimental) session.extensions.enable
    pub const SESSION_EXTENSIONS_ENABLE: &str = "session.extensions.enable";
    /// (Experimental) session.extensions.disable
    pub const SESSION_EXTENSIONS_DISABLE: &str = "session.extensions.disable";
    /// (Experimental) session.extensions.reload
    pub const SESSION_EXTENSIONS_RELOAD: &str = "session.extensions.reload";
    /// session.tools.handlePendingToolCall
    pub const SESSION_TOOLS_HANDLE_PENDING_TOOL_CALL: &str = "session.tools.handlePendingToolCall";
    /// session.commands.handlePendingCommand
    pub const SESSION_COMMANDS_HANDLE_PENDING_COMMAND: &str =
        "session.commands.handlePendingCommand";
    /// session.ui.elicitation
    pub const SESSION_UI_ELICITATION: &str = "session.ui.elicitation";
    /// session.ui.handlePendingElicitation
    pub const SESSION_UI_HANDLE_PENDING_ELICITATION: &str = "session.ui.handlePendingElicitation";
    /// session.permissions.handlePendingPermissionRequest
    pub const SESSION_PERMISSIONS_HANDLE_PENDING_PERMISSION_REQUEST: &str =
        "session.permissions.handlePendingPermissionRequest";
    /// session.log
    pub const SESSION_LOG: &str = "session.log";
    /// session.shell.exec
    pub const SESSION_SHELL_EXEC: &str = "session.shell.exec";
    /// session.shell.kill
    pub const SESSION_SHELL_KILL: &str = "session.shell.kill";
    /// (Experimental) session.history.compact
    pub const SESSION_HISTORY_COMPACT: &str = "session.history.compact";
    /// (Experimental) session.history.truncate
    pub const SESSION_HISTORY_TRUNCATE: &str = "session.history.truncate";
    /// (Experimental) session.usage.getMetrics
    pub const SESSION_USAGE_GET_METRICS: &str = "session.usage.getMetrics";
    /// sessionFs.readFile
    pub const SESSION_FS_READ_FILE: &str = "sessionFs.readFile";
    /// sessionFs.writeFile
    pub const SESSION_FS_WRITE_FILE: &str = "sessionFs.writeFile";
    /// sessionFs.appendFile
    pub const SESSION_FS_APPEND_FILE: &str = "sessionFs.appendFile";
    /// sessionFs.exists
    pub const SESSION_FS_EXISTS: &str = "sessionFs.exists";
    /// sessionFs.stat
    pub const SESSION_FS_STAT: &str = "sessionFs.stat";
    /// sessionFs.mkdir
    pub const SESSION_FS_MKDIR: &str = "sessionFs.mkdir";
    /// sessionFs.readdir
    pub const SESSION_FS_READDIR: &str = "sessionFs.readdir";
    /// sessionFs.readdirWithTypes
    pub const SESSION_FS_READDIR_WITH_TYPES: &str = "sessionFs.readdirWithTypes";
    /// sessionFs.rm
    pub const SESSION_FS_RM: &str = "sessionFs.rm";
    /// sessionFs.rename
    pub const SESSION_FS_RENAME: &str = "sessionFs.rename";
}
