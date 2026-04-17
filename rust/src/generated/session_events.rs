// AUTO-GENERATED FILE — DO NOT EDIT
// Generated from: session-events.schema.json
//
// Run `cd scripts/codegen && npm run generate:rust` to regenerate.

#![allow(deprecated)]
use serde::{Deserialize, Serialize};

/// Hosting platform type of the repository (github or ado)
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum SessionStartDataContextHostType {
    #[serde(rename = "github")]
    Github,
    #[serde(rename = "ado")]
    Ado,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Hosting platform type of the repository (github or ado)
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum SessionResumeDataContextHostType {
    #[serde(rename = "github")]
    Github,
    #[serde(rename = "ado")]
    Ado,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The type of operation performed on the plan file
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum SessionPlanChangedDataOperation {
    #[serde(rename = "create")]
    Create,
    #[serde(rename = "update")]
    Update,
    #[serde(rename = "delete")]
    Delete,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Whether the file was newly created or updated
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum SessionWorkspaceFileChangedDataOperation {
    #[serde(rename = "create")]
    Create,
    #[serde(rename = "update")]
    Update,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Origin type of the session being handed off
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum SessionHandoffDataSourceType {
    #[serde(rename = "remote")]
    Remote,
    #[serde(rename = "local")]
    Local,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Whether the session ended normally ("routine") or due to a crash/fatal error ("error")
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum SessionShutdownDataShutdownType {
    #[serde(rename = "routine")]
    Routine,
    #[serde(rename = "error")]
    Error,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Hosting platform type of the repository (github or ado)
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum SessionContextChangedDataHostType {
    #[serde(rename = "github")]
    Github,
    #[serde(rename = "ado")]
    Ado,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The agent mode that was active when this message was sent
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum UserMessageDataAgentMode {
    #[serde(rename = "interactive")]
    Interactive,
    #[serde(rename = "plan")]
    Plan,
    #[serde(rename = "autopilot")]
    Autopilot,
    #[serde(rename = "shell")]
    Shell,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Tool call type: "function" for standard tool calls, "custom" for grammar-based tool calls. Defaults to "function" when absent.
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum AssistantMessageDataToolRequestsType {
    #[serde(rename = "function")]
    Function,
    #[serde(rename = "custom")]
    Custom,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Message role: "system" for system prompts, "developer" for developer-injected instructions
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum SystemMessageDataRole {
    #[serde(rename = "system")]
    System,
    #[serde(rename = "developer")]
    Developer,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The outcome of the permission request
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum PermissionCompletedDataResultKind {
    #[serde(rename = "approved")]
    Approved,
    #[serde(rename = "denied-by-rules")]
    DeniedByRules,
    #[serde(rename = "denied-no-approval-rule-and-could-not-request-from-user")]
    DeniedNoApprovalRuleAndCouldNotRequestFromUser,
    #[serde(rename = "denied-interactively-by-user")]
    DeniedInteractivelyByUser,
    #[serde(rename = "denied-by-content-exclusion-policy")]
    DeniedByContentExclusionPolicy,
    #[serde(rename = "denied-by-permission-request-hook")]
    DeniedByPermissionRequestHook,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Elicitation mode; "form" for structured input, "url" for browser-based. Defaults to "form" when absent.
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum ElicitationRequestedDataMode {
    #[serde(rename = "form")]
    Form,
    #[serde(rename = "url")]
    Url,
    /// Unknown variant not yet covered by the SDK.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The user action: "accept" (submitted form), "decline" (explicitly refused), or "cancel" (dismissed)
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum ElicitationCompletedDataAction {
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

/// Connection status: connected, failed, needs-auth, pending, disabled, or not_configured
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum SessionMcpServersLoadedDataServersStatus {
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

/// New connection status: connected, failed, needs-auth, pending, disabled, or not_configured
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum SessionMcpServerStatusChangedDataStatus {
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

/// Discovery source
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum SessionExtensionsLoadedDataExtensionsSource {
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
pub enum SessionExtensionsLoadedDataExtensionsStatus {
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

/// All known session event type strings.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum SessionEventType {
    /// Session initialization metadata including context and configuration
    SessionStart,
    /// Session resume metadata including current context and event count
    SessionResume,
    /// Notifies Mission Control that the session's remote steering capability has changed
    SessionRemoteSteerableChanged,
    /// Error details for timeline display including message and optional diagnostic information
    SessionError,
    /// Payload indicating the session is idle with no background agents in flight
    SessionIdle,
    /// Session title change payload containing the new display title
    SessionTitleChanged,
    /// Informational message for timeline display with categorization
    SessionInfo,
    /// Warning message for timeline display with categorization
    SessionWarning,
    /// Model change details including previous and new model identifiers
    SessionModelChange,
    /// Agent mode change details including previous and new modes
    SessionModeChanged,
    /// Plan file operation details indicating what changed
    SessionPlanChanged,
    /// Workspace file change details including path and operation type
    SessionWorkspaceFileChanged,
    /// Session handoff metadata including source, context, and repository information
    SessionHandoff,
    /// Conversation truncation statistics including token counts and removed content metrics
    SessionTruncation,
    /// Session rewind details including target event and count of removed events
    SessionSnapshotRewind,
    /// Session termination metrics including usage statistics, code changes, and shutdown reason
    SessionShutdown,
    /// Updated working directory and git context after the change
    SessionContextChanged,
    /// Current context window usage statistics including token and message counts
    SessionUsageInfo,
    /// Context window breakdown at the start of LLM-powered conversation compaction
    SessionCompactionStart,
    /// Conversation compaction results including success status, metrics, and optional error details
    SessionCompactionComplete,
    /// Task completion notification with summary from the agent
    SessionTaskComplete,
    UserMessage,
    /// Empty payload; the event signals that the pending message queue has changed
    PendingMessagesModified,
    /// Turn initialization metadata including identifier and interaction tracking
    AssistantTurnStart,
    /// Agent intent description for current activity or plan
    AssistantIntent,
    /// Assistant reasoning content for timeline display with complete thinking text
    AssistantReasoning,
    /// Streaming reasoning delta for incremental extended thinking updates
    AssistantReasoningDelta,
    /// Streaming response progress with cumulative byte count
    AssistantStreamingDelta,
    /// Assistant response containing text content, optional tool requests, and interaction metadata
    AssistantMessage,
    /// Streaming assistant message delta for incremental response updates
    AssistantMessageDelta,
    /// Turn completion metadata including the turn identifier
    AssistantTurnEnd,
    /// LLM API call usage metrics including tokens, costs, quotas, and billing information
    AssistantUsage,
    /// Turn abort information including the reason for termination
    Abort,
    /// User-initiated tool invocation request with tool name and arguments
    ToolUserRequested,
    /// Tool execution startup details including MCP server information when applicable
    ToolExecutionStart,
    /// Streaming tool execution output for incremental result display
    ToolExecutionPartialResult,
    /// Tool execution progress notification with status message
    ToolExecutionProgress,
    /// Tool execution completion results including success status, detailed output, and error information
    ToolExecutionComplete,
    /// Skill invocation details including content, allowed tools, and plugin metadata
    SkillInvoked,
    /// Sub-agent startup details including parent tool call and agent information
    SubagentStarted,
    /// Sub-agent completion details for successful execution
    SubagentCompleted,
    /// Sub-agent failure details including error message and agent information
    SubagentFailed,
    /// Custom agent selection details including name and available tools
    SubagentSelected,
    /// Empty payload; the event signals that the custom agent was deselected, returning to the default agent
    SubagentDeselected,
    /// Hook invocation start details including type and input data
    HookStart,
    /// Hook invocation completion details including output, success status, and error information
    HookEnd,
    /// System or developer message content with role and optional template metadata
    SystemMessage,
    /// System-generated notification for runtime events like background task completion
    SystemNotification,
    /// Permission request notification requiring client approval with request details
    PermissionRequested,
    /// Permission request completion notification signaling UI dismissal
    PermissionCompleted,
    /// User input request notification with question and optional predefined choices
    UserInputRequested,
    /// User input request completion with the user's response
    UserInputCompleted,
    /// Elicitation request; may be form-based (structured input) or URL-based (browser redirect)
    ElicitationRequested,
    /// Elicitation request completion with the user's response
    ElicitationCompleted,
    /// Sampling request from an MCP server; contains the server name and a requestId for correlation
    SamplingRequested,
    /// Sampling request completion notification signaling UI dismissal
    SamplingCompleted,
    /// OAuth authentication request for an MCP server
    McpOauthRequired,
    /// MCP OAuth request completion notification
    McpOauthCompleted,
    /// External tool invocation request for client-side tool execution
    ExternalToolRequested,
    /// External tool completion notification signaling UI dismissal
    ExternalToolCompleted,
    /// Queued slash command dispatch request for client execution
    CommandQueued,
    /// Registered command dispatch request routed to the owning client
    CommandExecute,
    /// Queued command completion notification signaling UI dismissal
    CommandCompleted,
    /// SDK command registration change notification
    CommandsChanged,
    /// Session capability change notification
    CapabilitiesChanged,
    /// Plan approval request with plan content and available user actions
    ExitPlanModeRequested,
    /// Plan mode exit completion with the user's approval decision and optional feedback
    ExitPlanModeCompleted,
    SessionToolsUpdated,
    SessionBackgroundTasksChanged,
    SessionSkillsLoaded,
    SessionCustomAgentsUpdated,
    SessionMcpServersLoaded,
    SessionMcpServerStatusChanged,
    SessionExtensionsLoaded,
    /// Unknown event type not yet covered by the SDK.
    /// The original type string is preserved for round-tripping.
    Unknown(String),
}

impl serde::Serialize for SessionEventType {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
        serializer.serialize_str(self.as_str())
    }
}

impl<'de> serde::Deserialize<'de> for SessionEventType {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let s = String::deserialize(deserializer)?;
        Ok(Self::parse_type(&s))
    }
}

impl SessionEventType {
    /// Returns the wire-format string for this event type.
    pub fn as_str(&self) -> &str {
        match self {
            Self::SessionStart => "session.start",
            Self::SessionResume => "session.resume",
            Self::SessionRemoteSteerableChanged => "session.remote_steerable_changed",
            Self::SessionError => "session.error",
            Self::SessionIdle => "session.idle",
            Self::SessionTitleChanged => "session.title_changed",
            Self::SessionInfo => "session.info",
            Self::SessionWarning => "session.warning",
            Self::SessionModelChange => "session.model_change",
            Self::SessionModeChanged => "session.mode_changed",
            Self::SessionPlanChanged => "session.plan_changed",
            Self::SessionWorkspaceFileChanged => "session.workspace_file_changed",
            Self::SessionHandoff => "session.handoff",
            Self::SessionTruncation => "session.truncation",
            Self::SessionSnapshotRewind => "session.snapshot_rewind",
            Self::SessionShutdown => "session.shutdown",
            Self::SessionContextChanged => "session.context_changed",
            Self::SessionUsageInfo => "session.usage_info",
            Self::SessionCompactionStart => "session.compaction_start",
            Self::SessionCompactionComplete => "session.compaction_complete",
            Self::SessionTaskComplete => "session.task_complete",
            Self::UserMessage => "user.message",
            Self::PendingMessagesModified => "pending_messages.modified",
            Self::AssistantTurnStart => "assistant.turn_start",
            Self::AssistantIntent => "assistant.intent",
            Self::AssistantReasoning => "assistant.reasoning",
            Self::AssistantReasoningDelta => "assistant.reasoning_delta",
            Self::AssistantStreamingDelta => "assistant.streaming_delta",
            Self::AssistantMessage => "assistant.message",
            Self::AssistantMessageDelta => "assistant.message_delta",
            Self::AssistantTurnEnd => "assistant.turn_end",
            Self::AssistantUsage => "assistant.usage",
            Self::Abort => "abort",
            Self::ToolUserRequested => "tool.user_requested",
            Self::ToolExecutionStart => "tool.execution_start",
            Self::ToolExecutionPartialResult => "tool.execution_partial_result",
            Self::ToolExecutionProgress => "tool.execution_progress",
            Self::ToolExecutionComplete => "tool.execution_complete",
            Self::SkillInvoked => "skill.invoked",
            Self::SubagentStarted => "subagent.started",
            Self::SubagentCompleted => "subagent.completed",
            Self::SubagentFailed => "subagent.failed",
            Self::SubagentSelected => "subagent.selected",
            Self::SubagentDeselected => "subagent.deselected",
            Self::HookStart => "hook.start",
            Self::HookEnd => "hook.end",
            Self::SystemMessage => "system.message",
            Self::SystemNotification => "system.notification",
            Self::PermissionRequested => "permission.requested",
            Self::PermissionCompleted => "permission.completed",
            Self::UserInputRequested => "user_input.requested",
            Self::UserInputCompleted => "user_input.completed",
            Self::ElicitationRequested => "elicitation.requested",
            Self::ElicitationCompleted => "elicitation.completed",
            Self::SamplingRequested => "sampling.requested",
            Self::SamplingCompleted => "sampling.completed",
            Self::McpOauthRequired => "mcp.oauth_required",
            Self::McpOauthCompleted => "mcp.oauth_completed",
            Self::ExternalToolRequested => "external_tool.requested",
            Self::ExternalToolCompleted => "external_tool.completed",
            Self::CommandQueued => "command.queued",
            Self::CommandExecute => "command.execute",
            Self::CommandCompleted => "command.completed",
            Self::CommandsChanged => "commands.changed",
            Self::CapabilitiesChanged => "capabilities.changed",
            Self::ExitPlanModeRequested => "exit_plan_mode.requested",
            Self::ExitPlanModeCompleted => "exit_plan_mode.completed",
            Self::SessionToolsUpdated => "session.tools_updated",
            Self::SessionBackgroundTasksChanged => "session.background_tasks_changed",
            Self::SessionSkillsLoaded => "session.skills_loaded",
            Self::SessionCustomAgentsUpdated => "session.custom_agents_updated",
            Self::SessionMcpServersLoaded => "session.mcp_servers_loaded",
            Self::SessionMcpServerStatusChanged => "session.mcp_server_status_changed",
            Self::SessionExtensionsLoaded => "session.extensions_loaded",
            Self::Unknown(s) => s.as_str(),
        }
    }

    /// Parses a wire-format string into a typed event type.
    pub fn parse_type(s: &str) -> Self {
        match s {
            "session.start" => Self::SessionStart,
            "session.resume" => Self::SessionResume,
            "session.remote_steerable_changed" => Self::SessionRemoteSteerableChanged,
            "session.error" => Self::SessionError,
            "session.idle" => Self::SessionIdle,
            "session.title_changed" => Self::SessionTitleChanged,
            "session.info" => Self::SessionInfo,
            "session.warning" => Self::SessionWarning,
            "session.model_change" => Self::SessionModelChange,
            "session.mode_changed" => Self::SessionModeChanged,
            "session.plan_changed" => Self::SessionPlanChanged,
            "session.workspace_file_changed" => Self::SessionWorkspaceFileChanged,
            "session.handoff" => Self::SessionHandoff,
            "session.truncation" => Self::SessionTruncation,
            "session.snapshot_rewind" => Self::SessionSnapshotRewind,
            "session.shutdown" => Self::SessionShutdown,
            "session.context_changed" => Self::SessionContextChanged,
            "session.usage_info" => Self::SessionUsageInfo,
            "session.compaction_start" => Self::SessionCompactionStart,
            "session.compaction_complete" => Self::SessionCompactionComplete,
            "session.task_complete" => Self::SessionTaskComplete,
            "user.message" => Self::UserMessage,
            "pending_messages.modified" => Self::PendingMessagesModified,
            "assistant.turn_start" => Self::AssistantTurnStart,
            "assistant.intent" => Self::AssistantIntent,
            "assistant.reasoning" => Self::AssistantReasoning,
            "assistant.reasoning_delta" => Self::AssistantReasoningDelta,
            "assistant.streaming_delta" => Self::AssistantStreamingDelta,
            "assistant.message" => Self::AssistantMessage,
            "assistant.message_delta" => Self::AssistantMessageDelta,
            "assistant.turn_end" => Self::AssistantTurnEnd,
            "assistant.usage" => Self::AssistantUsage,
            "abort" => Self::Abort,
            "tool.user_requested" => Self::ToolUserRequested,
            "tool.execution_start" => Self::ToolExecutionStart,
            "tool.execution_partial_result" => Self::ToolExecutionPartialResult,
            "tool.execution_progress" => Self::ToolExecutionProgress,
            "tool.execution_complete" => Self::ToolExecutionComplete,
            "skill.invoked" => Self::SkillInvoked,
            "subagent.started" => Self::SubagentStarted,
            "subagent.completed" => Self::SubagentCompleted,
            "subagent.failed" => Self::SubagentFailed,
            "subagent.selected" => Self::SubagentSelected,
            "subagent.deselected" => Self::SubagentDeselected,
            "hook.start" => Self::HookStart,
            "hook.end" => Self::HookEnd,
            "system.message" => Self::SystemMessage,
            "system.notification" => Self::SystemNotification,
            "permission.requested" => Self::PermissionRequested,
            "permission.completed" => Self::PermissionCompleted,
            "user_input.requested" => Self::UserInputRequested,
            "user_input.completed" => Self::UserInputCompleted,
            "elicitation.requested" => Self::ElicitationRequested,
            "elicitation.completed" => Self::ElicitationCompleted,
            "sampling.requested" => Self::SamplingRequested,
            "sampling.completed" => Self::SamplingCompleted,
            "mcp.oauth_required" => Self::McpOauthRequired,
            "mcp.oauth_completed" => Self::McpOauthCompleted,
            "external_tool.requested" => Self::ExternalToolRequested,
            "external_tool.completed" => Self::ExternalToolCompleted,
            "command.queued" => Self::CommandQueued,
            "command.execute" => Self::CommandExecute,
            "command.completed" => Self::CommandCompleted,
            "commands.changed" => Self::CommandsChanged,
            "capabilities.changed" => Self::CapabilitiesChanged,
            "exit_plan_mode.requested" => Self::ExitPlanModeRequested,
            "exit_plan_mode.completed" => Self::ExitPlanModeCompleted,
            "session.tools_updated" => Self::SessionToolsUpdated,
            "session.background_tasks_changed" => Self::SessionBackgroundTasksChanged,
            "session.skills_loaded" => Self::SessionSkillsLoaded,
            "session.custom_agents_updated" => Self::SessionCustomAgentsUpdated,
            "session.mcp_servers_loaded" => Self::SessionMcpServersLoaded,
            "session.mcp_server_status_changed" => Self::SessionMcpServerStatusChanged,
            "session.extensions_loaded" => Self::SessionExtensionsLoaded,
            other => Self::Unknown(other.to_owned()),
        }
    }
}

impl std::str::FromStr for SessionEventType {
    type Err = std::convert::Infallible;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        Ok(Self::parse_type(s))
    }
}

impl std::fmt::Display for SessionEventType {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(self.as_str())
    }
}

/// Working directory and git context at session start
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionStartDataContext {
    /// Current working directory path
    pub cwd: String,
    /// Root directory of the git repository, resolved via git rev-parse
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    /// Hosting platform type of the repository (github or ado)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host_type: Option<SessionStartDataContextHostType>,
    /// Current git branch name
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Head commit of current git branch at session start time
    #[serde(skip_serializing_if = "Option::is_none")]
    pub head_commit: Option<String>,
    /// Base commit of current git branch at session start time
    #[serde(skip_serializing_if = "Option::is_none")]
    pub base_commit: Option<String>,
}

/// Session initialization metadata including context and configuration
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionStartData {
    /// Unique identifier for the session
    pub session_id: String,
    /// Schema version number for the session event format
    pub version: f64,
    /// Identifier of the software producing the events (e.g., "copilot-agent")
    pub producer: String,
    /// Version string of the Copilot application
    pub copilot_version: String,
    /// ISO 8601 timestamp when the session was created
    pub start_time: String,
    /// Model selected at session creation time, if any
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selected_model: Option<String>,
    /// Reasoning effort level used for model calls, if applicable (e.g. "low", "medium", "high", "xhigh")
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    /// Working directory and git context at session start
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context: Option<SessionStartDataContext>,
    /// Whether the session was already in use by another client at start time
    #[serde(skip_serializing_if = "Option::is_none")]
    pub already_in_use: Option<bool>,
    /// Whether this session supports remote steering via Mission Control
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_steerable: Option<bool>,
}

/// Updated working directory and git context at resume time
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionResumeDataContext {
    /// Current working directory path
    pub cwd: String,
    /// Root directory of the git repository, resolved via git rev-parse
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    /// Hosting platform type of the repository (github or ado)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host_type: Option<SessionResumeDataContextHostType>,
    /// Current git branch name
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Head commit of current git branch at session start time
    #[serde(skip_serializing_if = "Option::is_none")]
    pub head_commit: Option<String>,
    /// Base commit of current git branch at session start time
    #[serde(skip_serializing_if = "Option::is_none")]
    pub base_commit: Option<String>,
}

/// Session resume metadata including current context and event count
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionResumeData {
    /// ISO 8601 timestamp when the session was resumed
    pub resume_time: String,
    /// Total number of persisted events in the session at the time of resume
    pub event_count: f64,
    /// Model currently selected at resume time
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selected_model: Option<String>,
    /// Reasoning effort level used for model calls, if applicable (e.g. "low", "medium", "high", "xhigh")
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    /// Updated working directory and git context at resume time
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context: Option<SessionResumeDataContext>,
    /// Whether the session was already in use by another client at resume time
    #[serde(skip_serializing_if = "Option::is_none")]
    pub already_in_use: Option<bool>,
    /// Whether this session supports remote steering via Mission Control
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_steerable: Option<bool>,
}

/// Notifies Mission Control that the session's remote steering capability has changed
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionRemoteSteerableChangedData {
    /// Whether this session now supports remote steering via Mission Control
    pub remote_steerable: bool,
}

/// Error details for timeline display including message and optional diagnostic information
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionErrorData {
    /// Category of error (e.g., "authentication", "authorization", "quota", "rate_limit", "context_limit", "query")
    pub error_type: String,
    /// Human-readable error message
    pub message: String,
    /// Error stack trace, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub stack: Option<String>,
    /// HTTP status code from the upstream request, if applicable
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status_code: Option<i64>,
    /// GitHub request tracing ID (x-github-request-id header) for correlating with server-side logs
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider_call_id: Option<String>,
    /// Optional URL associated with this error that the user can open in a browser
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Payload indicating the session is idle with no background agents in flight
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionIdleData {
    /// True when the preceding agentic loop was cancelled via abort signal
    #[serde(skip_serializing_if = "Option::is_none")]
    pub aborted: Option<bool>,
}

/// Session title change payload containing the new display title
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTitleChangedData {
    /// The new display title for the session
    pub title: String,
}

/// Informational message for timeline display with categorization
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionInfoData {
    /// Category of informational message (e.g., "notification", "timing", "context_window", "mcp", "snapshot", "configuration", "authentication", "model")
    pub info_type: String,
    /// Human-readable informational message for display in the timeline
    pub message: String,
    /// Optional URL associated with this message that the user can open in a browser
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Warning message for timeline display with categorization
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWarningData {
    /// Category of warning (e.g., "subscription", "policy", "mcp")
    pub warning_type: String,
    /// Human-readable warning message for display in the timeline
    pub message: String,
    /// Optional URL associated with this warning that the user can open in a browser
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Model change details including previous and new model identifiers
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelChangeData {
    /// Model that was previously selected, if any
    #[serde(skip_serializing_if = "Option::is_none")]
    pub previous_model: Option<String>,
    /// Newly selected model identifier
    pub new_model: String,
    /// Reasoning effort level before the model change, if applicable
    #[serde(skip_serializing_if = "Option::is_none")]
    pub previous_reasoning_effort: Option<String>,
    /// Reasoning effort level after the model change, if applicable
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
}

/// Agent mode change details including previous and new modes
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModeChangedData {
    /// Agent mode before the change (e.g., "interactive", "plan", "autopilot")
    pub previous_mode: String,
    /// Agent mode after the change (e.g., "interactive", "plan", "autopilot")
    pub new_mode: String,
}

/// Plan file operation details indicating what changed
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPlanChangedData {
    /// The type of operation performed on the plan file
    pub operation: SessionPlanChangedDataOperation,
}

/// Workspace file change details including path and operation type
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspaceFileChangedData {
    /// Relative path within the session workspace files directory
    pub path: String,
    /// Whether the file was newly created or updated
    pub operation: SessionWorkspaceFileChangedDataOperation,
}

/// Repository context for the handed-off session
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHandoffDataRepository {
    /// Repository owner (user or organization)
    pub owner: String,
    /// Repository name
    pub name: String,
    /// Git branch name, if applicable
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
}

/// Session handoff metadata including source, context, and repository information
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHandoffData {
    /// ISO 8601 timestamp when the handoff occurred
    pub handoff_time: String,
    /// Origin type of the session being handed off
    pub source_type: SessionHandoffDataSourceType,
    /// Repository context for the handed-off session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<SessionHandoffDataRepository>,
    /// Additional context information for the handoff
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context: Option<String>,
    /// Summary of the work done in the source session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
    /// Session ID of the remote session being handed off
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_session_id: Option<String>,
    /// GitHub host URL for the source session (e.g., https://github.com or https://tenant.ghe.com)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<String>,
}

/// Conversation truncation statistics including token counts and removed content metrics
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTruncationData {
    /// Maximum token count for the model's context window
    pub token_limit: f64,
    /// Total tokens in conversation messages before truncation
    pub pre_truncation_tokens_in_messages: f64,
    /// Number of conversation messages before truncation
    pub pre_truncation_messages_length: f64,
    /// Total tokens in conversation messages after truncation
    pub post_truncation_tokens_in_messages: f64,
    /// Number of conversation messages after truncation
    pub post_truncation_messages_length: f64,
    /// Number of tokens removed by truncation
    pub tokens_removed_during_truncation: f64,
    /// Number of messages removed by truncation
    pub messages_removed_during_truncation: f64,
    /// Identifier of the component that performed truncation (e.g., "BasicTruncator")
    pub performed_by: String,
}

/// Session rewind details including target event and count of removed events
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSnapshotRewindData {
    /// Event ID that was rewound to; this event and all after it were removed
    pub up_to_event_id: String,
    /// Number of events that were removed by the rewind
    pub events_removed: f64,
}

/// Aggregate code change metrics for the session
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionShutdownDataCodeChanges {
    /// Total number of lines added during the session
    pub lines_added: f64,
    /// Total number of lines removed during the session
    pub lines_removed: f64,
    /// List of file paths that were modified during the session
    pub files_modified: Vec<String>,
}

/// Request count and cost metrics
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionShutdownDataModelMetricsRequests {
    /// Total number of API requests made to this model
    pub count: f64,
    /// Cumulative cost multiplier for requests to this model
    pub cost: f64,
}

/// Token usage breakdown
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionShutdownDataModelMetricsUsage {
    /// Total input tokens consumed across all requests to this model
    pub input_tokens: f64,
    /// Total output tokens produced across all requests to this model
    pub output_tokens: f64,
    /// Total tokens read from prompt cache across all requests
    pub cache_read_tokens: f64,
    /// Total tokens written to prompt cache across all requests
    pub cache_write_tokens: f64,
    /// Total reasoning tokens produced across all requests to this model
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_tokens: Option<f64>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionShutdownDataModelMetrics {
    /// Request count and cost metrics
    pub requests: SessionShutdownDataModelMetricsRequests,
    /// Token usage breakdown
    pub usage: SessionShutdownDataModelMetricsUsage,
}

/// Session termination metrics including usage statistics, code changes, and shutdown reason
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionShutdownData {
    /// Whether the session ended normally ("routine") or due to a crash/fatal error ("error")
    pub shutdown_type: SessionShutdownDataShutdownType,
    /// Error description when shutdownType is "error"
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error_reason: Option<String>,
    /// Total number of premium API requests used during the session
    pub total_premium_requests: f64,
    /// Cumulative time spent in API calls during the session, in milliseconds
    pub total_api_duration_ms: f64,
    /// Unix timestamp (milliseconds) when the session started
    pub session_start_time: f64,
    /// Aggregate code change metrics for the session
    pub code_changes: SessionShutdownDataCodeChanges,
    /// Per-model usage breakdown, keyed by model identifier
    pub model_metrics: std::collections::HashMap<String, SessionShutdownDataModelMetrics>,
    /// Model that was selected at the time of shutdown
    #[serde(skip_serializing_if = "Option::is_none")]
    pub current_model: Option<String>,
    /// Total tokens in context window at shutdown
    #[serde(skip_serializing_if = "Option::is_none")]
    pub current_tokens: Option<f64>,
    /// System message token count at shutdown
    #[serde(skip_serializing_if = "Option::is_none")]
    pub system_tokens: Option<f64>,
    /// Non-system message token count at shutdown
    #[serde(skip_serializing_if = "Option::is_none")]
    pub conversation_tokens: Option<f64>,
    /// Tool definitions token count at shutdown
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_definitions_tokens: Option<f64>,
}

/// Updated working directory and git context after the change
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionContextChangedData {
    /// Current working directory path
    pub cwd: String,
    /// Root directory of the git repository, resolved via git rev-parse
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    /// Hosting platform type of the repository (github or ado)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host_type: Option<SessionContextChangedDataHostType>,
    /// Current git branch name
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Head commit of current git branch at session start time
    #[serde(skip_serializing_if = "Option::is_none")]
    pub head_commit: Option<String>,
    /// Base commit of current git branch at session start time
    #[serde(skip_serializing_if = "Option::is_none")]
    pub base_commit: Option<String>,
}

/// Current context window usage statistics including token and message counts
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUsageInfoData {
    /// Maximum token count for the model's context window
    pub token_limit: f64,
    /// Current number of tokens in the context window
    pub current_tokens: f64,
    /// Current number of messages in the conversation
    pub messages_length: f64,
    /// Token count from system message(s)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub system_tokens: Option<f64>,
    /// Token count from non-system messages (user, assistant, tool)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub conversation_tokens: Option<f64>,
    /// Token count from tool definitions
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_definitions_tokens: Option<f64>,
    /// Whether this is the first usage_info event emitted in this session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_initial: Option<bool>,
}

/// Context window breakdown at the start of LLM-powered conversation compaction
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCompactionStartData {
    /// Token count from system message(s) at compaction start
    #[serde(skip_serializing_if = "Option::is_none")]
    pub system_tokens: Option<f64>,
    /// Token count from non-system messages (user, assistant, tool) at compaction start
    #[serde(skip_serializing_if = "Option::is_none")]
    pub conversation_tokens: Option<f64>,
    /// Token count from tool definitions at compaction start
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_definitions_tokens: Option<f64>,
}

/// Token usage breakdown for the compaction LLM call
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCompactionCompleteDataCompactionTokensUsed {
    /// Input tokens consumed by the compaction LLM call
    pub input: f64,
    /// Output tokens produced by the compaction LLM call
    pub output: f64,
    /// Cached input tokens reused in the compaction LLM call
    pub cached_input: f64,
}

/// Conversation compaction results including success status, metrics, and optional error details
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCompactionCompleteData {
    /// Whether compaction completed successfully
    pub success: bool,
    /// Error message if compaction failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Total tokens in conversation before compaction
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pre_compaction_tokens: Option<f64>,
    /// Total tokens in conversation after compaction
    #[serde(skip_serializing_if = "Option::is_none")]
    pub post_compaction_tokens: Option<f64>,
    /// Number of messages before compaction
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pre_compaction_messages_length: Option<f64>,
    /// Number of messages removed during compaction
    #[serde(skip_serializing_if = "Option::is_none")]
    pub messages_removed: Option<f64>,
    /// Number of tokens removed during compaction
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tokens_removed: Option<f64>,
    /// LLM-generated summary of the compacted conversation history
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary_content: Option<String>,
    /// Checkpoint snapshot number created for recovery
    #[serde(skip_serializing_if = "Option::is_none")]
    pub checkpoint_number: Option<f64>,
    /// File path where the checkpoint was stored
    #[serde(skip_serializing_if = "Option::is_none")]
    pub checkpoint_path: Option<String>,
    /// Token usage breakdown for the compaction LLM call
    #[serde(skip_serializing_if = "Option::is_none")]
    pub compaction_tokens_used: Option<SessionCompactionCompleteDataCompactionTokensUsed>,
    /// GitHub request tracing ID (x-github-request-id header) for the compaction LLM call
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_id: Option<String>,
    /// Token count from system message(s) after compaction
    #[serde(skip_serializing_if = "Option::is_none")]
    pub system_tokens: Option<f64>,
    /// Token count from non-system messages (user, assistant, tool) after compaction
    #[serde(skip_serializing_if = "Option::is_none")]
    pub conversation_tokens: Option<f64>,
    /// Token count from tool definitions after compaction
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_definitions_tokens: Option<f64>,
}

/// Task completion notification with summary from the agent
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTaskCompleteData {
    /// Summary of the completed task, provided by the agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
    /// Whether the tool call succeeded. False when validation failed (e.g., invalid arguments)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub success: Option<bool>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserMessageData {
    /// The user's message text as displayed in the timeline
    pub content: String,
    /// Transformed version of the message sent to the model, with XML wrapping, timestamps, and other augmentations for prompt caching
    #[serde(skip_serializing_if = "Option::is_none")]
    pub transformed_content: Option<String>,
    /// Files, selections, or GitHub references attached to the message
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attachments: Option<Vec<serde_json::Value>>,
    /// Origin of this message, used for timeline filtering (e.g., "skill-pdf" for skill-injected messages that should be hidden from the user)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<String>,
    /// The agent mode that was active when this message was sent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_mode: Option<UserMessageDataAgentMode>,
    /// CAPI interaction ID for correlating this user message with its turn
    #[serde(skip_serializing_if = "Option::is_none")]
    pub interaction_id: Option<String>,
}

/// Empty payload; the event signals that the pending message queue has changed
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PendingMessagesModifiedData {}

/// Turn initialization metadata including identifier and interaction tracking
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantTurnStartData {
    /// Identifier for this turn within the agentic loop, typically a stringified turn number
    pub turn_id: String,
    /// CAPI interaction ID for correlating this turn with upstream telemetry
    #[serde(skip_serializing_if = "Option::is_none")]
    pub interaction_id: Option<String>,
}

/// Agent intent description for current activity or plan
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantIntentData {
    /// Short description of what the agent is currently doing or planning to do
    pub intent: String,
}

/// Assistant reasoning content for timeline display with complete thinking text
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantReasoningData {
    /// Unique identifier for this reasoning block
    pub reasoning_id: String,
    /// The complete extended thinking text from the model
    pub content: String,
}

/// Streaming reasoning delta for incremental extended thinking updates
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantReasoningDeltaData {
    /// Reasoning block ID this delta belongs to, matching the corresponding assistant.reasoning event
    pub reasoning_id: String,
    /// Incremental text chunk to append to the reasoning content
    pub delta_content: String,
}

/// Streaming response progress with cumulative byte count
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantStreamingDeltaData {
    /// Cumulative total bytes received from the streaming response so far
    pub total_response_size_bytes: f64,
}

/// A tool invocation request from the assistant
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantMessageDataToolRequests {
    /// Unique identifier for this tool call
    pub tool_call_id: String,
    /// Name of the tool being invoked
    pub name: String,
    /// Arguments to pass to the tool, format depends on the tool
    #[serde(skip_serializing_if = "Option::is_none")]
    pub arguments: Option<serde_json::Value>,
    /// Tool call type: "function" for standard tool calls, "custom" for grammar-based tool calls. Defaults to "function" when absent.
    #[serde(rename = "type")]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<AssistantMessageDataToolRequestsType>,
    /// Human-readable display title for the tool
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_title: Option<String>,
    /// Name of the MCP server hosting this tool, when the tool is an MCP tool
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_server_name: Option<String>,
    /// Resolved intention summary describing what this specific call does
    #[serde(skip_serializing_if = "Option::is_none")]
    pub intention_summary: Option<String>,
}

/// Assistant response containing text content, optional tool requests, and interaction metadata
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantMessageData {
    /// Unique identifier for this assistant message
    pub message_id: String,
    /// The assistant's text response content
    pub content: String,
    /// Tool invocations requested by the assistant in this message
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_requests: Option<Vec<AssistantMessageDataToolRequests>>,
    /// Opaque/encrypted extended thinking data from Anthropic models. Session-bound and stripped on resume.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_opaque: Option<String>,
    /// Readable reasoning text from the model's extended thinking
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_text: Option<String>,
    /// Encrypted reasoning content from OpenAI models. Session-bound and stripped on resume.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub encrypted_content: Option<String>,
    /// Generation phase for phased-output models (e.g., thinking vs. response phases)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub phase: Option<String>,
    /// Actual output token count from the API response (completion_tokens), used for accurate token accounting
    #[serde(skip_serializing_if = "Option::is_none")]
    pub output_tokens: Option<f64>,
    /// CAPI interaction ID for correlating this message with upstream telemetry
    #[serde(skip_serializing_if = "Option::is_none")]
    pub interaction_id: Option<String>,
    /// GitHub request tracing ID (x-github-request-id header) for correlating with server-side logs
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_id: Option<String>,
    /// Tool call ID of the parent tool invocation when this event originates from a sub-agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parent_tool_call_id: Option<String>,
}

/// Streaming assistant message delta for incremental response updates
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantMessageDeltaData {
    /// Message ID this delta belongs to, matching the corresponding assistant.message event
    pub message_id: String,
    /// Incremental text chunk to append to the message content
    pub delta_content: String,
    /// Tool call ID of the parent tool invocation when this event originates from a sub-agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parent_tool_call_id: Option<String>,
}

/// Turn completion metadata including the turn identifier
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantTurnEndData {
    /// Identifier of the turn that has ended, matching the corresponding assistant.turn_start event
    pub turn_id: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantUsageDataQuotaSnapshots {
    /// Whether the user has an unlimited usage entitlement
    pub is_unlimited_entitlement: bool,
    /// Total requests allowed by the entitlement
    pub entitlement_requests: f64,
    /// Number of requests already consumed
    pub used_requests: f64,
    /// Whether usage is still permitted after quota exhaustion
    pub usage_allowed_with_exhausted_quota: bool,
    /// Number of requests over the entitlement limit
    pub overage: f64,
    /// Whether overage is allowed when quota is exhausted
    pub overage_allowed_with_exhausted_quota: bool,
    /// Percentage of quota remaining (0.0 to 1.0)
    pub remaining_percentage: f64,
    /// Date when the quota resets
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reset_date: Option<String>,
}

/// Token usage detail for a single billing category
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantUsageDataCopilotUsageTokenDetails {
    /// Number of tokens in this billing batch
    pub batch_size: f64,
    /// Cost per batch of tokens
    pub cost_per_batch: f64,
    /// Total token count for this entry
    pub token_count: f64,
    /// Token category (e.g., "input", "output")
    pub token_type: String,
}

/// Per-request cost and usage data from the CAPI copilot_usage response field
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantUsageDataCopilotUsage {
    /// Itemized token usage breakdown
    pub token_details: Vec<AssistantUsageDataCopilotUsageTokenDetails>,
    /// Total cost in nano-AIU (AI Units) for this request
    pub total_nano_aiu: f64,
}

/// LLM API call usage metrics including tokens, costs, quotas, and billing information
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantUsageData {
    /// Model identifier used for this API call
    pub model: String,
    /// Number of input tokens consumed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input_tokens: Option<f64>,
    /// Number of output tokens produced
    #[serde(skip_serializing_if = "Option::is_none")]
    pub output_tokens: Option<f64>,
    /// Number of tokens read from prompt cache
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_read_tokens: Option<f64>,
    /// Number of tokens written to prompt cache
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_write_tokens: Option<f64>,
    /// Number of output tokens used for reasoning (e.g., chain-of-thought)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_tokens: Option<f64>,
    /// Model multiplier cost for billing purposes
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cost: Option<f64>,
    /// Duration of the API call in milliseconds
    #[serde(skip_serializing_if = "Option::is_none")]
    pub duration: Option<f64>,
    /// Time to first token in milliseconds. Only available for streaming requests
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ttft_ms: Option<f64>,
    /// Average inter-token latency in milliseconds. Only available for streaming requests
    #[serde(skip_serializing_if = "Option::is_none")]
    pub inter_token_latency_ms: Option<f64>,
    /// What initiated this API call (e.g., "sub-agent", "mcp-sampling"); absent for user-initiated calls
    #[serde(skip_serializing_if = "Option::is_none")]
    pub initiator: Option<String>,
    /// Completion ID from the model provider (e.g., chatcmpl-abc123)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api_call_id: Option<String>,
    /// GitHub request tracing ID (x-github-request-id header) for server-side log correlation
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider_call_id: Option<String>,
    /// Parent tool call ID when this usage originates from a sub-agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parent_tool_call_id: Option<String>,
    /// Per-quota resource usage snapshots, keyed by quota identifier
    #[serde(skip_serializing_if = "Option::is_none")]
    pub quota_snapshots:
        Option<std::collections::HashMap<String, AssistantUsageDataQuotaSnapshots>>,
    /// Per-request cost and usage data from the CAPI copilot_usage response field
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_usage: Option<AssistantUsageDataCopilotUsage>,
    /// Reasoning effort level used for model calls, if applicable (e.g. "low", "medium", "high", "xhigh")
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
}

/// Turn abort information including the reason for termination
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AbortData {
    /// Reason the current turn was aborted (e.g., "user initiated")
    pub reason: String,
}

/// User-initiated tool invocation request with tool name and arguments
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolUserRequestedData {
    /// Unique identifier for this tool call
    pub tool_call_id: String,
    /// Name of the tool the user wants to invoke
    pub tool_name: String,
    /// Arguments for the tool invocation
    #[serde(skip_serializing_if = "Option::is_none")]
    pub arguments: Option<serde_json::Value>,
}

/// Tool execution startup details including MCP server information when applicable
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolExecutionStartData {
    /// Unique identifier for this tool call
    pub tool_call_id: String,
    /// Name of the tool being executed
    pub tool_name: String,
    /// Arguments passed to the tool
    #[serde(skip_serializing_if = "Option::is_none")]
    pub arguments: Option<serde_json::Value>,
    /// Name of the MCP server hosting this tool, when the tool is an MCP tool
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_server_name: Option<String>,
    /// Original tool name on the MCP server, when the tool is an MCP tool
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_tool_name: Option<String>,
    /// Tool call ID of the parent tool invocation when this event originates from a sub-agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parent_tool_call_id: Option<String>,
}

/// Streaming tool execution output for incremental result display
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolExecutionPartialResultData {
    /// Tool call ID this partial result belongs to
    pub tool_call_id: String,
    /// Incremental output chunk from the running tool
    pub partial_output: String,
}

/// Tool execution progress notification with status message
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolExecutionProgressData {
    /// Tool call ID this progress notification belongs to
    pub tool_call_id: String,
    /// Human-readable progress status message (e.g., from an MCP server)
    pub progress_message: String,
}

/// Tool execution result on success
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolExecutionCompleteDataResult {
    /// Concise tool result text sent to the LLM for chat completion, potentially truncated for token efficiency
    pub content: String,
    /// Full detailed tool result for UI/timeline display, preserving complete content such as diffs. Falls back to content when absent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub detailed_content: Option<String>,
    /// Structured content blocks (text, images, audio, resources) returned by the tool in their native format
    #[serde(skip_serializing_if = "Option::is_none")]
    pub contents: Option<Vec<serde_json::Value>>,
}

/// Error details when the tool execution failed
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolExecutionCompleteDataError {
    /// Human-readable error message
    pub message: String,
    /// Machine-readable error code
    #[serde(skip_serializing_if = "Option::is_none")]
    pub code: Option<String>,
}

/// Tool execution completion results including success status, detailed output, and error information
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolExecutionCompleteData {
    /// Unique identifier for the completed tool call
    pub tool_call_id: String,
    /// Whether the tool execution completed successfully
    pub success: bool,
    /// Model identifier that generated this tool call
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// CAPI interaction ID for correlating this tool execution with upstream telemetry
    #[serde(skip_serializing_if = "Option::is_none")]
    pub interaction_id: Option<String>,
    /// Whether this tool call was explicitly requested by the user rather than the assistant
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_user_requested: Option<bool>,
    /// Tool execution result on success
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<ToolExecutionCompleteDataResult>,
    /// Error details when the tool execution failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<ToolExecutionCompleteDataError>,
    /// Tool-specific telemetry data (e.g., CodeQL check counts, grep match counts)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_telemetry: Option<std::collections::HashMap<String, serde_json::Value>>,
    /// Tool call ID of the parent tool invocation when this event originates from a sub-agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parent_tool_call_id: Option<String>,
}

/// Skill invocation details including content, allowed tools, and plugin metadata
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInvokedData {
    /// Name of the invoked skill
    pub name: String,
    /// File path to the SKILL.md definition
    pub path: String,
    /// Full content of the skill file, injected into the conversation for the model
    pub content: String,
    /// Tool names that should be auto-approved when this skill is active
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allowed_tools: Option<Vec<String>>,
    /// Name of the plugin this skill originated from, when applicable
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plugin_name: Option<String>,
    /// Version of the plugin this skill originated from, when applicable
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plugin_version: Option<String>,
    /// Description of the skill from its SKILL.md frontmatter
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
}

/// Sub-agent startup details including parent tool call and agent information
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SubagentStartedData {
    /// Tool call ID of the parent tool invocation that spawned this sub-agent
    pub tool_call_id: String,
    /// Internal name of the sub-agent
    pub agent_name: String,
    /// Human-readable display name of the sub-agent
    pub agent_display_name: String,
    /// Description of what the sub-agent does
    pub agent_description: String,
}

/// Sub-agent completion details for successful execution
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SubagentCompletedData {
    /// Tool call ID of the parent tool invocation that spawned this sub-agent
    pub tool_call_id: String,
    /// Internal name of the sub-agent
    pub agent_name: String,
    /// Human-readable display name of the sub-agent
    pub agent_display_name: String,
    /// Model used by the sub-agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Total number of tool calls made by the sub-agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub total_tool_calls: Option<f64>,
    /// Total tokens (input + output) consumed by the sub-agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub total_tokens: Option<f64>,
    /// Wall-clock duration of the sub-agent execution in milliseconds
    #[serde(skip_serializing_if = "Option::is_none")]
    pub duration_ms: Option<f64>,
}

/// Sub-agent failure details including error message and agent information
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SubagentFailedData {
    /// Tool call ID of the parent tool invocation that spawned this sub-agent
    pub tool_call_id: String,
    /// Internal name of the sub-agent
    pub agent_name: String,
    /// Human-readable display name of the sub-agent
    pub agent_display_name: String,
    /// Error message describing why the sub-agent failed
    pub error: String,
    /// Model used by the sub-agent (if any model calls succeeded before failure)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Total number of tool calls made before the sub-agent failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub total_tool_calls: Option<f64>,
    /// Total tokens (input + output) consumed before the sub-agent failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub total_tokens: Option<f64>,
    /// Wall-clock duration of the sub-agent execution in milliseconds
    #[serde(skip_serializing_if = "Option::is_none")]
    pub duration_ms: Option<f64>,
}

/// Custom agent selection details including name and available tools
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SubagentSelectedData {
    /// Internal name of the selected custom agent
    pub agent_name: String,
    /// Human-readable display name of the selected custom agent
    pub agent_display_name: String,
    /// List of tool names available to this agent, or null for all tools
    pub tools: Vec<String>,
}

/// Empty payload; the event signals that the custom agent was deselected, returning to the default agent
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SubagentDeselectedData {}

/// Hook invocation start details including type and input data
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HookStartData {
    /// Unique identifier for this hook invocation
    pub hook_invocation_id: String,
    /// Type of hook being invoked (e.g., "preToolUse", "postToolUse", "sessionStart")
    pub hook_type: String,
    /// Input data passed to the hook
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input: Option<serde_json::Value>,
}

/// Error details when the hook failed
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HookEndDataError {
    /// Human-readable error message
    pub message: String,
    /// Error stack trace, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub stack: Option<String>,
}

/// Hook invocation completion details including output, success status, and error information
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HookEndData {
    /// Identifier matching the corresponding hook.start event
    pub hook_invocation_id: String,
    /// Type of hook that was invoked (e.g., "preToolUse", "postToolUse", "sessionStart")
    pub hook_type: String,
    /// Output data produced by the hook
    #[serde(skip_serializing_if = "Option::is_none")]
    pub output: Option<serde_json::Value>,
    /// Whether the hook completed successfully
    pub success: bool,
    /// Error details when the hook failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<HookEndDataError>,
}

/// Metadata about the prompt template and its construction
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SystemMessageDataMetadata {
    /// Version identifier of the prompt template used
    #[serde(skip_serializing_if = "Option::is_none")]
    pub prompt_version: Option<String>,
    /// Template variables used when constructing the prompt
    #[serde(skip_serializing_if = "Option::is_none")]
    pub variables: Option<std::collections::HashMap<String, serde_json::Value>>,
}

/// System or developer message content with role and optional template metadata
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SystemMessageData {
    /// The system or developer prompt text
    pub content: String,
    /// Message role: "system" for system prompts, "developer" for developer-injected instructions
    pub role: SystemMessageDataRole,
    /// Optional name identifier for the message source
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Metadata about the prompt template and its construction
    #[serde(skip_serializing_if = "Option::is_none")]
    pub metadata: Option<SystemMessageDataMetadata>,
}

/// System-generated notification for runtime events like background task completion
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SystemNotificationData {
    /// The notification text, typically wrapped in <system_notification> XML tags
    pub content: String,
    /// Structured metadata identifying what triggered this notification
    pub kind: serde_json::Value,
}

/// Permission request notification requiring client approval with request details
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequestedData {
    /// Unique identifier for this permission request; used to respond via session.respondToPermission()
    pub request_id: String,
    /// Details of the permission being requested
    pub permission_request: serde_json::Value,
    /// When true, this permission was already resolved by a permissionRequest hook and requires no client action
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resolved_by_hook: Option<bool>,
}

/// The result of the permission request
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionCompletedDataResult {
    /// The outcome of the permission request
    pub kind: PermissionCompletedDataResultKind,
}

/// Permission request completion notification signaling UI dismissal
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionCompletedData {
    /// Request ID of the resolved permission request; clients should dismiss any UI for this request
    pub request_id: String,
    /// The result of the permission request
    pub result: PermissionCompletedDataResult,
}

/// User input request notification with question and optional predefined choices
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserInputRequestedData {
    /// Unique identifier for this input request; used to respond via session.respondToUserInput()
    pub request_id: String,
    /// The question or prompt to present to the user
    pub question: String,
    /// Predefined choices for the user to select from, if applicable
    #[serde(skip_serializing_if = "Option::is_none")]
    pub choices: Option<Vec<String>>,
    /// Whether the user can provide a free-form text response in addition to predefined choices
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allow_freeform: Option<bool>,
    /// The LLM-assigned tool call ID that triggered this request; used by remote UIs to correlate responses
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
}

/// User input request completion with the user's response
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserInputCompletedData {
    /// Request ID of the resolved user input request; clients should dismiss any UI for this request
    pub request_id: String,
    /// The user's answer to the input request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub answer: Option<String>,
    /// Whether the answer was typed as free-form text rather than selected from choices
    #[serde(skip_serializing_if = "Option::is_none")]
    pub was_freeform: Option<bool>,
}

/// JSON Schema describing the form fields to present to the user (form mode only)
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ElicitationRequestedDataRequestedSchema {
    /// Schema type indicator (always 'object')
    #[serde(rename = "type")]
    pub r#type: String,
    /// Form field definitions, keyed by field name
    pub properties: std::collections::HashMap<String, serde_json::Value>,
    /// List of required field names
    #[serde(skip_serializing_if = "Option::is_none")]
    pub required: Option<Vec<String>>,
}

/// Elicitation request; may be form-based (structured input) or URL-based (browser redirect)
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ElicitationRequestedData {
    /// Unique identifier for this elicitation request; used to respond via session.respondToElicitation()
    pub request_id: String,
    /// Tool call ID from the LLM completion; used to correlate with CompletionChunk.toolCall.id for remote UIs
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
    /// The source that initiated the request (MCP server name, or absent for agent-initiated)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub elicitation_source: Option<String>,
    /// Message describing what information is needed from the user
    pub message: String,
    /// Elicitation mode; "form" for structured input, "url" for browser-based. Defaults to "form" when absent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<ElicitationRequestedDataMode>,
    /// JSON Schema describing the form fields to present to the user (form mode only)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub requested_schema: Option<ElicitationRequestedDataRequestedSchema>,
    /// URL to open in the user's browser (url mode only)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Elicitation request completion with the user's response
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ElicitationCompletedData {
    /// Request ID of the resolved elicitation request; clients should dismiss any UI for this request
    pub request_id: String,
    /// The user action: "accept" (submitted form), "decline" (explicitly refused), or "cancel" (dismissed)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub action: Option<ElicitationCompletedDataAction>,
    /// The submitted form data when action is 'accept'; keys match the requested schema fields
    #[serde(skip_serializing_if = "Option::is_none")]
    pub content: Option<std::collections::HashMap<String, serde_json::Value>>,
}

/// Sampling request from an MCP server; contains the server name and a requestId for correlation
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SamplingRequestedData {
    /// Unique identifier for this sampling request; used to respond via session.respondToSampling()
    pub request_id: String,
    /// Name of the MCP server that initiated the sampling request
    pub server_name: String,
    /// The JSON-RPC request ID from the MCP protocol
    pub mcp_request_id: serde_json::Value,
}

/// Sampling request completion notification signaling UI dismissal
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SamplingCompletedData {
    /// Request ID of the resolved sampling request; clients should dismiss any UI for this request
    pub request_id: String,
}

/// Static OAuth client configuration, if the server specifies one
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthRequiredDataStaticClientConfig {
    /// OAuth client ID for the server
    pub client_id: String,
    /// Whether this is a public OAuth client
    #[serde(skip_serializing_if = "Option::is_none")]
    pub public_client: Option<bool>,
}

/// OAuth authentication request for an MCP server
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthRequiredData {
    /// Unique identifier for this OAuth request; used to respond via session.respondToMcpOAuth()
    pub request_id: String,
    /// Display name of the MCP server that requires OAuth
    pub server_name: String,
    /// URL of the MCP server that requires OAuth
    pub server_url: String,
    /// Static OAuth client configuration, if the server specifies one
    #[serde(skip_serializing_if = "Option::is_none")]
    pub static_client_config: Option<McpOauthRequiredDataStaticClientConfig>,
}

/// MCP OAuth request completion notification
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthCompletedData {
    /// Request ID of the resolved OAuth request
    pub request_id: String,
}

/// External tool invocation request for client-side tool execution
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolRequestedData {
    /// Unique identifier for this request; used to respond via session.respondToExternalTool()
    pub request_id: String,
    /// Session ID that this external tool request belongs to
    pub session_id: String,
    /// Tool call ID assigned to this external tool invocation
    pub tool_call_id: String,
    /// Name of the external tool to invoke
    pub tool_name: String,
    /// Arguments to pass to the external tool
    #[serde(skip_serializing_if = "Option::is_none")]
    pub arguments: Option<serde_json::Value>,
    /// W3C Trace Context traceparent header for the execute_tool span
    #[serde(skip_serializing_if = "Option::is_none")]
    pub traceparent: Option<String>,
    /// W3C Trace Context tracestate header for the execute_tool span
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tracestate: Option<String>,
}

/// External tool completion notification signaling UI dismissal
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolCompletedData {
    /// Request ID of the resolved external tool request; clients should dismiss any UI for this request
    pub request_id: String,
}

/// Queued slash command dispatch request for client execution
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandQueuedData {
    /// Unique identifier for this request; used to respond via session.respondToQueuedCommand()
    pub request_id: String,
    /// The slash command text to be executed (e.g., /help, /clear)
    pub command: String,
}

/// Registered command dispatch request routed to the owning client
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandExecuteData {
    /// Unique identifier; used to respond via session.commands.handlePendingCommand()
    pub request_id: String,
    /// The full command text (e.g., /deploy production)
    pub command: String,
    /// Command name without leading /
    pub command_name: String,
    /// Raw argument string after the command name
    pub args: String,
}

/// Queued command completion notification signaling UI dismissal
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandCompletedData {
    /// Request ID of the resolved command request; clients should dismiss any UI for this request
    pub request_id: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsChangedDataCommands {
    pub name: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
}

/// SDK command registration change notification
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsChangedData {
    /// Current list of registered SDK commands
    pub commands: Vec<CommandsChangedDataCommands>,
}

/// UI capability changes
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CapabilitiesChangedDataUi {
    /// Whether elicitation is now supported
    #[serde(skip_serializing_if = "Option::is_none")]
    pub elicitation: Option<bool>,
}

/// Session capability change notification
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CapabilitiesChangedData {
    /// UI capability changes
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ui: Option<CapabilitiesChangedDataUi>,
}

/// Plan approval request with plan content and available user actions
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExitPlanModeRequestedData {
    /// Unique identifier for this request; used to respond via session.respondToExitPlanMode()
    pub request_id: String,
    /// Summary of the plan that was created
    pub summary: String,
    /// Full content of the plan file
    pub plan_content: String,
    /// Available actions the user can take (e.g., approve, edit, reject)
    pub actions: Vec<String>,
    /// The recommended action for the user to take
    pub recommended_action: String,
}

/// Plan mode exit completion with the user's approval decision and optional feedback
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExitPlanModeCompletedData {
    /// Request ID of the resolved exit plan mode request; clients should dismiss any UI for this request
    pub request_id: String,
    /// Whether the plan was approved by the user
    #[serde(skip_serializing_if = "Option::is_none")]
    pub approved: Option<bool>,
    /// Which action the user selected (e.g. 'autopilot', 'interactive', 'exit_only')
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selected_action: Option<String>,
    /// Whether edits should be auto-approved without confirmation
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auto_approve_edits: Option<bool>,
    /// Free-form feedback from the user if they requested changes to the plan
    #[serde(skip_serializing_if = "Option::is_none")]
    pub feedback: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionToolsUpdatedData {
    pub model: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionBackgroundTasksChangedData {}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsLoadedDataSkills {
    /// Unique identifier for the skill
    pub name: String,
    /// Description of what the skill does
    pub description: String,
    /// Source location type of the skill (e.g., project, personal, plugin)
    pub source: String,
    /// Whether the skill can be invoked by the user as a slash command
    pub user_invocable: bool,
    /// Whether the skill is currently enabled
    pub enabled: bool,
    /// Absolute path to the skill file, if available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsLoadedData {
    /// Array of resolved skill metadata
    pub skills: Vec<SessionSkillsLoadedDataSkills>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCustomAgentsUpdatedDataAgents {
    /// Unique identifier for the agent
    pub id: String,
    /// Internal name of the agent
    pub name: String,
    /// Human-readable display name
    pub display_name: String,
    /// Description of what the agent does
    pub description: String,
    /// Source location: user, project, inherited, remote, or plugin
    pub source: String,
    /// List of tool names available to this agent
    pub tools: Vec<String>,
    /// Whether the agent can be selected by the user
    pub user_invocable: bool,
    /// Model override for this agent, if set
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCustomAgentsUpdatedData {
    /// Array of loaded custom agent metadata
    pub agents: Vec<SessionCustomAgentsUpdatedDataAgents>,
    /// Non-fatal warnings from agent loading
    pub warnings: Vec<String>,
    /// Fatal errors from agent loading
    pub errors: Vec<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpServersLoadedDataServers {
    /// Server name (config key)
    pub name: String,
    /// Connection status: connected, failed, needs-auth, pending, disabled, or not_configured
    pub status: SessionMcpServersLoadedDataServersStatus,
    /// Configuration source: user, workspace, plugin, or builtin
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<String>,
    /// Error message if the server failed to connect
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpServersLoadedData {
    /// Array of MCP server status summaries
    pub servers: Vec<SessionMcpServersLoadedDataServers>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpServerStatusChangedData {
    /// Name of the MCP server whose status changed
    pub server_name: String,
    /// New connection status: connected, failed, needs-auth, pending, disabled, or not_configured
    pub status: SessionMcpServerStatusChangedDataStatus,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionExtensionsLoadedDataExtensions {
    /// Source-qualified extension ID (e.g., 'project:my-ext', 'user:auth-helper')
    pub id: String,
    /// Extension name (directory name)
    pub name: String,
    /// Discovery source
    pub source: SessionExtensionsLoadedDataExtensionsSource,
    /// Current status: running, disabled, failed, or starting
    pub status: SessionExtensionsLoadedDataExtensionsStatus,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionExtensionsLoadedData {
    /// Array of discovered extensions and their status
    pub extensions: Vec<SessionExtensionsLoadedDataExtensions>,
}

/// Typed session event data payload.
///
/// Each variant corresponds to a [`SessionEventType`] and carries the
/// typed data struct for that event. Unknown or new event types are
/// captured as raw JSON in the `Unknown` variant.
#[derive(Debug, Clone)]
pub enum SessionEventData {
    /// Session initialization metadata including context and configuration
    SessionStart(SessionStartData),
    /// Session resume metadata including current context and event count
    SessionResume(SessionResumeData),
    /// Notifies Mission Control that the session's remote steering capability has changed
    SessionRemoteSteerableChanged(SessionRemoteSteerableChangedData),
    /// Error details for timeline display including message and optional diagnostic information
    SessionError(SessionErrorData),
    /// Payload indicating the session is idle with no background agents in flight
    SessionIdle(SessionIdleData),
    /// Session title change payload containing the new display title
    SessionTitleChanged(SessionTitleChangedData),
    /// Informational message for timeline display with categorization
    SessionInfo(SessionInfoData),
    /// Warning message for timeline display with categorization
    SessionWarning(SessionWarningData),
    /// Model change details including previous and new model identifiers
    SessionModelChange(SessionModelChangeData),
    /// Agent mode change details including previous and new modes
    SessionModeChanged(SessionModeChangedData),
    /// Plan file operation details indicating what changed
    SessionPlanChanged(SessionPlanChangedData),
    /// Workspace file change details including path and operation type
    SessionWorkspaceFileChanged(SessionWorkspaceFileChangedData),
    /// Session handoff metadata including source, context, and repository information
    SessionHandoff(SessionHandoffData),
    /// Conversation truncation statistics including token counts and removed content metrics
    SessionTruncation(SessionTruncationData),
    /// Session rewind details including target event and count of removed events
    SessionSnapshotRewind(SessionSnapshotRewindData),
    /// Session termination metrics including usage statistics, code changes, and shutdown reason
    SessionShutdown(SessionShutdownData),
    /// Updated working directory and git context after the change
    SessionContextChanged(SessionContextChangedData),
    /// Current context window usage statistics including token and message counts
    SessionUsageInfo(SessionUsageInfoData),
    /// Context window breakdown at the start of LLM-powered conversation compaction
    SessionCompactionStart(SessionCompactionStartData),
    /// Conversation compaction results including success status, metrics, and optional error details
    SessionCompactionComplete(SessionCompactionCompleteData),
    /// Task completion notification with summary from the agent
    SessionTaskComplete(SessionTaskCompleteData),
    UserMessage(UserMessageData),
    /// Empty payload; the event signals that the pending message queue has changed
    PendingMessagesModified(PendingMessagesModifiedData),
    /// Turn initialization metadata including identifier and interaction tracking
    AssistantTurnStart(AssistantTurnStartData),
    /// Agent intent description for current activity or plan
    AssistantIntent(AssistantIntentData),
    /// Assistant reasoning content for timeline display with complete thinking text
    AssistantReasoning(AssistantReasoningData),
    /// Streaming reasoning delta for incremental extended thinking updates
    AssistantReasoningDelta(AssistantReasoningDeltaData),
    /// Streaming response progress with cumulative byte count
    AssistantStreamingDelta(AssistantStreamingDeltaData),
    /// Assistant response containing text content, optional tool requests, and interaction metadata
    AssistantMessage(AssistantMessageData),
    /// Streaming assistant message delta for incremental response updates
    AssistantMessageDelta(AssistantMessageDeltaData),
    /// Turn completion metadata including the turn identifier
    AssistantTurnEnd(AssistantTurnEndData),
    /// LLM API call usage metrics including tokens, costs, quotas, and billing information
    AssistantUsage(AssistantUsageData),
    /// Turn abort information including the reason for termination
    Abort(AbortData),
    /// User-initiated tool invocation request with tool name and arguments
    ToolUserRequested(ToolUserRequestedData),
    /// Tool execution startup details including MCP server information when applicable
    ToolExecutionStart(ToolExecutionStartData),
    /// Streaming tool execution output for incremental result display
    ToolExecutionPartialResult(ToolExecutionPartialResultData),
    /// Tool execution progress notification with status message
    ToolExecutionProgress(ToolExecutionProgressData),
    /// Tool execution completion results including success status, detailed output, and error information
    ToolExecutionComplete(ToolExecutionCompleteData),
    /// Skill invocation details including content, allowed tools, and plugin metadata
    SkillInvoked(SkillInvokedData),
    /// Sub-agent startup details including parent tool call and agent information
    SubagentStarted(SubagentStartedData),
    /// Sub-agent completion details for successful execution
    SubagentCompleted(SubagentCompletedData),
    /// Sub-agent failure details including error message and agent information
    SubagentFailed(SubagentFailedData),
    /// Custom agent selection details including name and available tools
    SubagentSelected(SubagentSelectedData),
    /// Empty payload; the event signals that the custom agent was deselected, returning to the default agent
    SubagentDeselected(SubagentDeselectedData),
    /// Hook invocation start details including type and input data
    HookStart(HookStartData),
    /// Hook invocation completion details including output, success status, and error information
    HookEnd(HookEndData),
    /// System or developer message content with role and optional template metadata
    SystemMessage(SystemMessageData),
    /// System-generated notification for runtime events like background task completion
    SystemNotification(SystemNotificationData),
    /// Permission request notification requiring client approval with request details
    PermissionRequested(PermissionRequestedData),
    /// Permission request completion notification signaling UI dismissal
    PermissionCompleted(PermissionCompletedData),
    /// User input request notification with question and optional predefined choices
    UserInputRequested(UserInputRequestedData),
    /// User input request completion with the user's response
    UserInputCompleted(UserInputCompletedData),
    /// Elicitation request; may be form-based (structured input) or URL-based (browser redirect)
    ElicitationRequested(ElicitationRequestedData),
    /// Elicitation request completion with the user's response
    ElicitationCompleted(ElicitationCompletedData),
    /// Sampling request from an MCP server; contains the server name and a requestId for correlation
    SamplingRequested(SamplingRequestedData),
    /// Sampling request completion notification signaling UI dismissal
    SamplingCompleted(SamplingCompletedData),
    /// OAuth authentication request for an MCP server
    McpOauthRequired(McpOauthRequiredData),
    /// MCP OAuth request completion notification
    McpOauthCompleted(McpOauthCompletedData),
    /// External tool invocation request for client-side tool execution
    ExternalToolRequested(ExternalToolRequestedData),
    /// External tool completion notification signaling UI dismissal
    ExternalToolCompleted(ExternalToolCompletedData),
    /// Queued slash command dispatch request for client execution
    CommandQueued(CommandQueuedData),
    /// Registered command dispatch request routed to the owning client
    CommandExecute(CommandExecuteData),
    /// Queued command completion notification signaling UI dismissal
    CommandCompleted(CommandCompletedData),
    /// SDK command registration change notification
    CommandsChanged(CommandsChangedData),
    /// Session capability change notification
    CapabilitiesChanged(CapabilitiesChangedData),
    /// Plan approval request with plan content and available user actions
    ExitPlanModeRequested(ExitPlanModeRequestedData),
    /// Plan mode exit completion with the user's approval decision and optional feedback
    ExitPlanModeCompleted(ExitPlanModeCompletedData),
    SessionToolsUpdated(SessionToolsUpdatedData),
    SessionBackgroundTasksChanged(SessionBackgroundTasksChangedData),
    SessionSkillsLoaded(SessionSkillsLoadedData),
    SessionCustomAgentsUpdated(SessionCustomAgentsUpdatedData),
    SessionMcpServersLoaded(SessionMcpServersLoadedData),
    SessionMcpServerStatusChanged(SessionMcpServerStatusChangedData),
    SessionExtensionsLoaded(SessionExtensionsLoadedData),
    /// Unknown event type — data is preserved as raw JSON.
    Unknown(serde_json::Value),
}

impl serde::Serialize for SessionEventData {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
        match self {
            Self::SessionStart(d) => d.serialize(serializer),
            Self::SessionResume(d) => d.serialize(serializer),
            Self::SessionRemoteSteerableChanged(d) => d.serialize(serializer),
            Self::SessionError(d) => d.serialize(serializer),
            Self::SessionIdle(d) => d.serialize(serializer),
            Self::SessionTitleChanged(d) => d.serialize(serializer),
            Self::SessionInfo(d) => d.serialize(serializer),
            Self::SessionWarning(d) => d.serialize(serializer),
            Self::SessionModelChange(d) => d.serialize(serializer),
            Self::SessionModeChanged(d) => d.serialize(serializer),
            Self::SessionPlanChanged(d) => d.serialize(serializer),
            Self::SessionWorkspaceFileChanged(d) => d.serialize(serializer),
            Self::SessionHandoff(d) => d.serialize(serializer),
            Self::SessionTruncation(d) => d.serialize(serializer),
            Self::SessionSnapshotRewind(d) => d.serialize(serializer),
            Self::SessionShutdown(d) => d.serialize(serializer),
            Self::SessionContextChanged(d) => d.serialize(serializer),
            Self::SessionUsageInfo(d) => d.serialize(serializer),
            Self::SessionCompactionStart(d) => d.serialize(serializer),
            Self::SessionCompactionComplete(d) => d.serialize(serializer),
            Self::SessionTaskComplete(d) => d.serialize(serializer),
            Self::UserMessage(d) => d.serialize(serializer),
            Self::PendingMessagesModified(d) => d.serialize(serializer),
            Self::AssistantTurnStart(d) => d.serialize(serializer),
            Self::AssistantIntent(d) => d.serialize(serializer),
            Self::AssistantReasoning(d) => d.serialize(serializer),
            Self::AssistantReasoningDelta(d) => d.serialize(serializer),
            Self::AssistantStreamingDelta(d) => d.serialize(serializer),
            Self::AssistantMessage(d) => d.serialize(serializer),
            Self::AssistantMessageDelta(d) => d.serialize(serializer),
            Self::AssistantTurnEnd(d) => d.serialize(serializer),
            Self::AssistantUsage(d) => d.serialize(serializer),
            Self::Abort(d) => d.serialize(serializer),
            Self::ToolUserRequested(d) => d.serialize(serializer),
            Self::ToolExecutionStart(d) => d.serialize(serializer),
            Self::ToolExecutionPartialResult(d) => d.serialize(serializer),
            Self::ToolExecutionProgress(d) => d.serialize(serializer),
            Self::ToolExecutionComplete(d) => d.serialize(serializer),
            Self::SkillInvoked(d) => d.serialize(serializer),
            Self::SubagentStarted(d) => d.serialize(serializer),
            Self::SubagentCompleted(d) => d.serialize(serializer),
            Self::SubagentFailed(d) => d.serialize(serializer),
            Self::SubagentSelected(d) => d.serialize(serializer),
            Self::SubagentDeselected(d) => d.serialize(serializer),
            Self::HookStart(d) => d.serialize(serializer),
            Self::HookEnd(d) => d.serialize(serializer),
            Self::SystemMessage(d) => d.serialize(serializer),
            Self::SystemNotification(d) => d.serialize(serializer),
            Self::PermissionRequested(d) => d.serialize(serializer),
            Self::PermissionCompleted(d) => d.serialize(serializer),
            Self::UserInputRequested(d) => d.serialize(serializer),
            Self::UserInputCompleted(d) => d.serialize(serializer),
            Self::ElicitationRequested(d) => d.serialize(serializer),
            Self::ElicitationCompleted(d) => d.serialize(serializer),
            Self::SamplingRequested(d) => d.serialize(serializer),
            Self::SamplingCompleted(d) => d.serialize(serializer),
            Self::McpOauthRequired(d) => d.serialize(serializer),
            Self::McpOauthCompleted(d) => d.serialize(serializer),
            Self::ExternalToolRequested(d) => d.serialize(serializer),
            Self::ExternalToolCompleted(d) => d.serialize(serializer),
            Self::CommandQueued(d) => d.serialize(serializer),
            Self::CommandExecute(d) => d.serialize(serializer),
            Self::CommandCompleted(d) => d.serialize(serializer),
            Self::CommandsChanged(d) => d.serialize(serializer),
            Self::CapabilitiesChanged(d) => d.serialize(serializer),
            Self::ExitPlanModeRequested(d) => d.serialize(serializer),
            Self::ExitPlanModeCompleted(d) => d.serialize(serializer),
            Self::SessionToolsUpdated(d) => d.serialize(serializer),
            Self::SessionBackgroundTasksChanged(d) => d.serialize(serializer),
            Self::SessionSkillsLoaded(d) => d.serialize(serializer),
            Self::SessionCustomAgentsUpdated(d) => d.serialize(serializer),
            Self::SessionMcpServersLoaded(d) => d.serialize(serializer),
            Self::SessionMcpServerStatusChanged(d) => d.serialize(serializer),
            Self::SessionExtensionsLoaded(d) => d.serialize(serializer),
            Self::Unknown(v) => v.serialize(serializer),
        }
    }
}

impl std::fmt::Display for SessionEventData {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match serde_json::to_string(self) {
            Ok(s) => f.write_str(&s),
            Err(_) => f.write_str("{}"),
        }
    }
}

/// A single event in a session's timeline.
///
/// Events form a linked chain via `parent_id`. The `event_type` field
/// is a typed enum that identifies the kind of event, and `data` carries
/// the corresponding typed payload.
#[derive(Debug, Clone, serde::Serialize)]
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
    /// Event type discriminator.
    #[serde(rename = "type")]
    pub event_type: SessionEventType,
    /// Typed event-specific data payload.
    pub data: SessionEventData,
}

impl<'de> serde::Deserialize<'de> for SessionEvent {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        #[derive(serde::Deserialize)]
        #[serde(rename_all = "camelCase")]
        struct RawEvent {
            id: String,
            timestamp: String,
            parent_id: Option<String>,
            #[serde(default)]
            ephemeral: Option<bool>,
            #[serde(rename = "type")]
            event_type: SessionEventType,
            #[serde(default)]
            data: serde_json::Value,
        }

        let raw = RawEvent::deserialize(deserializer)?;
        let data = match &raw.event_type {
            SessionEventType::SessionStart => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionStart)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionResume => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionResume)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionRemoteSteerableChanged => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionRemoteSteerableChanged)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionError => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionError)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionIdle => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionIdle)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionTitleChanged => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionTitleChanged)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionInfo => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionInfo)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionWarning => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionWarning)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionModelChange => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionModelChange)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionModeChanged => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionModeChanged)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionPlanChanged => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionPlanChanged)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionWorkspaceFileChanged => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionWorkspaceFileChanged)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionHandoff => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionHandoff)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionTruncation => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionTruncation)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionSnapshotRewind => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionSnapshotRewind)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionShutdown => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionShutdown)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionContextChanged => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionContextChanged)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionUsageInfo => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionUsageInfo)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionCompactionStart => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionCompactionStart)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionCompactionComplete => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionCompactionComplete)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionTaskComplete => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionTaskComplete)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::UserMessage => serde_json::from_value(raw.data)
                .map(SessionEventData::UserMessage)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::PendingMessagesModified => serde_json::from_value(raw.data)
                .map(SessionEventData::PendingMessagesModified)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::AssistantTurnStart => serde_json::from_value(raw.data)
                .map(SessionEventData::AssistantTurnStart)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::AssistantIntent => serde_json::from_value(raw.data)
                .map(SessionEventData::AssistantIntent)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::AssistantReasoning => serde_json::from_value(raw.data)
                .map(SessionEventData::AssistantReasoning)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::AssistantReasoningDelta => serde_json::from_value(raw.data)
                .map(SessionEventData::AssistantReasoningDelta)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::AssistantStreamingDelta => serde_json::from_value(raw.data)
                .map(SessionEventData::AssistantStreamingDelta)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::AssistantMessage => serde_json::from_value(raw.data)
                .map(SessionEventData::AssistantMessage)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::AssistantMessageDelta => serde_json::from_value(raw.data)
                .map(SessionEventData::AssistantMessageDelta)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::AssistantTurnEnd => serde_json::from_value(raw.data)
                .map(SessionEventData::AssistantTurnEnd)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::AssistantUsage => serde_json::from_value(raw.data)
                .map(SessionEventData::AssistantUsage)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::Abort => serde_json::from_value(raw.data)
                .map(SessionEventData::Abort)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::ToolUserRequested => serde_json::from_value(raw.data)
                .map(SessionEventData::ToolUserRequested)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::ToolExecutionStart => serde_json::from_value(raw.data)
                .map(SessionEventData::ToolExecutionStart)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::ToolExecutionPartialResult => serde_json::from_value(raw.data)
                .map(SessionEventData::ToolExecutionPartialResult)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::ToolExecutionProgress => serde_json::from_value(raw.data)
                .map(SessionEventData::ToolExecutionProgress)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::ToolExecutionComplete => serde_json::from_value(raw.data)
                .map(SessionEventData::ToolExecutionComplete)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SkillInvoked => serde_json::from_value(raw.data)
                .map(SessionEventData::SkillInvoked)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SubagentStarted => serde_json::from_value(raw.data)
                .map(SessionEventData::SubagentStarted)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SubagentCompleted => serde_json::from_value(raw.data)
                .map(SessionEventData::SubagentCompleted)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SubagentFailed => serde_json::from_value(raw.data)
                .map(SessionEventData::SubagentFailed)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SubagentSelected => serde_json::from_value(raw.data)
                .map(SessionEventData::SubagentSelected)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SubagentDeselected => serde_json::from_value(raw.data)
                .map(SessionEventData::SubagentDeselected)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::HookStart => serde_json::from_value(raw.data)
                .map(SessionEventData::HookStart)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::HookEnd => serde_json::from_value(raw.data)
                .map(SessionEventData::HookEnd)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SystemMessage => serde_json::from_value(raw.data)
                .map(SessionEventData::SystemMessage)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SystemNotification => serde_json::from_value(raw.data)
                .map(SessionEventData::SystemNotification)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::PermissionRequested => serde_json::from_value(raw.data)
                .map(SessionEventData::PermissionRequested)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::PermissionCompleted => serde_json::from_value(raw.data)
                .map(SessionEventData::PermissionCompleted)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::UserInputRequested => serde_json::from_value(raw.data)
                .map(SessionEventData::UserInputRequested)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::UserInputCompleted => serde_json::from_value(raw.data)
                .map(SessionEventData::UserInputCompleted)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::ElicitationRequested => serde_json::from_value(raw.data)
                .map(SessionEventData::ElicitationRequested)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::ElicitationCompleted => serde_json::from_value(raw.data)
                .map(SessionEventData::ElicitationCompleted)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SamplingRequested => serde_json::from_value(raw.data)
                .map(SessionEventData::SamplingRequested)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SamplingCompleted => serde_json::from_value(raw.data)
                .map(SessionEventData::SamplingCompleted)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::McpOauthRequired => serde_json::from_value(raw.data)
                .map(SessionEventData::McpOauthRequired)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::McpOauthCompleted => serde_json::from_value(raw.data)
                .map(SessionEventData::McpOauthCompleted)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::ExternalToolRequested => serde_json::from_value(raw.data)
                .map(SessionEventData::ExternalToolRequested)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::ExternalToolCompleted => serde_json::from_value(raw.data)
                .map(SessionEventData::ExternalToolCompleted)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::CommandQueued => serde_json::from_value(raw.data)
                .map(SessionEventData::CommandQueued)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::CommandExecute => serde_json::from_value(raw.data)
                .map(SessionEventData::CommandExecute)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::CommandCompleted => serde_json::from_value(raw.data)
                .map(SessionEventData::CommandCompleted)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::CommandsChanged => serde_json::from_value(raw.data)
                .map(SessionEventData::CommandsChanged)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::CapabilitiesChanged => serde_json::from_value(raw.data)
                .map(SessionEventData::CapabilitiesChanged)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::ExitPlanModeRequested => serde_json::from_value(raw.data)
                .map(SessionEventData::ExitPlanModeRequested)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::ExitPlanModeCompleted => serde_json::from_value(raw.data)
                .map(SessionEventData::ExitPlanModeCompleted)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionToolsUpdated => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionToolsUpdated)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionBackgroundTasksChanged => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionBackgroundTasksChanged)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionSkillsLoaded => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionSkillsLoaded)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionCustomAgentsUpdated => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionCustomAgentsUpdated)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionMcpServersLoaded => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionMcpServersLoaded)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionMcpServerStatusChanged => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionMcpServerStatusChanged)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::SessionExtensionsLoaded => serde_json::from_value(raw.data)
                .map(SessionEventData::SessionExtensionsLoaded)
                .map_err(serde::de::Error::custom)?,
            SessionEventType::Unknown(_) => SessionEventData::Unknown(raw.data),
        };

        Ok(SessionEvent {
            id: raw.id,
            timestamp: raw.timestamp,
            parent_id: raw.parent_id,
            ephemeral: raw.ephemeral,
            event_type: raw.event_type,
            data,
        })
    }
}
