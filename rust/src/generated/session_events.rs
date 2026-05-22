//! Auto-generated from session-events.schema.json — do not edit manually.

use std::collections::HashMap;

use serde::{Deserialize, Serialize};

use crate::types::{RequestId, SessionId};

/// Identifies the kind of session event.
#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum SessionEventType {
    #[serde(rename = "session.start")]
    SessionStart,
    #[serde(rename = "session.resume")]
    SessionResume,
    #[serde(rename = "session.remote_steerable_changed")]
    SessionRemoteSteerableChanged,
    #[serde(rename = "session.error")]
    SessionError,
    #[serde(rename = "session.idle")]
    SessionIdle,
    #[serde(rename = "session.title_changed")]
    SessionTitleChanged,
    #[serde(rename = "session.schedule_created")]
    SessionScheduleCreated,
    #[serde(rename = "session.schedule_cancelled")]
    SessionScheduleCancelled,
    #[serde(rename = "session.info")]
    SessionInfo,
    #[serde(rename = "session.warning")]
    SessionWarning,
    #[serde(rename = "session.model_change")]
    SessionModelChange,
    #[serde(rename = "session.mode_changed")]
    SessionModeChanged,
    #[serde(rename = "session.plan_changed")]
    SessionPlanChanged,
    #[serde(rename = "session.workspace_file_changed")]
    SessionWorkspaceFileChanged,
    #[serde(rename = "session.handoff")]
    SessionHandoff,
    #[serde(rename = "session.truncation")]
    SessionTruncation,
    #[serde(rename = "session.snapshot_rewind")]
    SessionSnapshotRewind,
    #[serde(rename = "session.shutdown")]
    SessionShutdown,
    #[serde(rename = "session.context_changed")]
    SessionContextChanged,
    #[serde(rename = "session.usage_info")]
    SessionUsageInfo,
    #[serde(rename = "session.compaction_start")]
    SessionCompactionStart,
    #[serde(rename = "session.compaction_complete")]
    SessionCompactionComplete,
    #[serde(rename = "session.task_complete")]
    SessionTaskComplete,
    #[serde(rename = "user.message")]
    UserMessage,
    #[serde(rename = "pending_messages.modified")]
    PendingMessagesModified,
    #[serde(rename = "assistant.turn_start")]
    AssistantTurnStart,
    #[serde(rename = "assistant.intent")]
    AssistantIntent,
    #[serde(rename = "assistant.reasoning")]
    AssistantReasoning,
    #[serde(rename = "assistant.reasoning_delta")]
    AssistantReasoningDelta,
    #[serde(rename = "assistant.streaming_delta")]
    AssistantStreamingDelta,
    #[serde(rename = "assistant.message")]
    AssistantMessage,
    #[serde(rename = "assistant.message_start")]
    AssistantMessageStart,
    #[serde(rename = "assistant.message_delta")]
    AssistantMessageDelta,
    #[serde(rename = "assistant.turn_end")]
    AssistantTurnEnd,
    #[serde(rename = "assistant.usage")]
    AssistantUsage,
    #[serde(rename = "model.call_failure")]
    ModelCallFailure,
    #[serde(rename = "abort")]
    Abort,
    #[serde(rename = "tool.user_requested")]
    ToolUserRequested,
    #[serde(rename = "tool.execution_start")]
    ToolExecutionStart,
    #[serde(rename = "tool.execution_partial_result")]
    ToolExecutionPartialResult,
    #[serde(rename = "tool.execution_progress")]
    ToolExecutionProgress,
    #[serde(rename = "tool.execution_complete")]
    ToolExecutionComplete,
    #[serde(rename = "skill.invoked")]
    SkillInvoked,
    #[serde(rename = "subagent.started")]
    SubagentStarted,
    #[serde(rename = "subagent.completed")]
    SubagentCompleted,
    #[serde(rename = "subagent.failed")]
    SubagentFailed,
    #[serde(rename = "subagent.selected")]
    SubagentSelected,
    #[serde(rename = "subagent.deselected")]
    SubagentDeselected,
    #[serde(rename = "hook.start")]
    HookStart,
    #[serde(rename = "hook.end")]
    HookEnd,
    #[serde(rename = "system.message")]
    SystemMessage,
    #[serde(rename = "system.notification")]
    SystemNotification,
    #[serde(rename = "permission.requested")]
    PermissionRequested,
    #[serde(rename = "permission.completed")]
    PermissionCompleted,
    #[serde(rename = "user_input.requested")]
    UserInputRequested,
    #[serde(rename = "user_input.completed")]
    UserInputCompleted,
    #[serde(rename = "elicitation.requested")]
    ElicitationRequested,
    #[serde(rename = "elicitation.completed")]
    ElicitationCompleted,
    #[serde(rename = "sampling.requested")]
    SamplingRequested,
    #[serde(rename = "sampling.completed")]
    SamplingCompleted,
    #[serde(rename = "mcp.oauth_required")]
    McpOauthRequired,
    #[serde(rename = "mcp.oauth_completed")]
    McpOauthCompleted,
    #[serde(rename = "session.custom_notification")]
    SessionCustomNotification,
    #[serde(rename = "external_tool.requested")]
    ExternalToolRequested,
    #[serde(rename = "external_tool.completed")]
    ExternalToolCompleted,
    #[serde(rename = "command.queued")]
    CommandQueued,
    #[serde(rename = "command.execute")]
    CommandExecute,
    #[serde(rename = "command.completed")]
    CommandCompleted,
    #[serde(rename = "auto_mode_switch.requested")]
    AutoModeSwitchRequested,
    #[serde(rename = "auto_mode_switch.completed")]
    AutoModeSwitchCompleted,
    #[serde(rename = "commands.changed")]
    CommandsChanged,
    #[serde(rename = "capabilities.changed")]
    CapabilitiesChanged,
    #[serde(rename = "exit_plan_mode.requested")]
    ExitPlanModeRequested,
    #[serde(rename = "exit_plan_mode.completed")]
    ExitPlanModeCompleted,
    #[serde(rename = "session.tools_updated")]
    SessionToolsUpdated,
    #[serde(rename = "session.background_tasks_changed")]
    SessionBackgroundTasksChanged,
    #[serde(rename = "session.skills_loaded")]
    SessionSkillsLoaded,
    #[serde(rename = "session.custom_agents_updated")]
    SessionCustomAgentsUpdated,
    #[serde(rename = "session.mcp_servers_loaded")]
    SessionMcpServersLoaded,
    #[serde(rename = "session.mcp_server_status_changed")]
    SessionMcpServerStatusChanged,
    #[serde(rename = "session.extensions_loaded")]
    SessionExtensionsLoaded,
    /// Unknown event type for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Typed session event data, discriminated by the event `type` field.
///
/// Use with [`TypedSessionEvent`] for fully typed event handling.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type", content = "data")]
pub enum SessionEventData {
    #[serde(rename = "session.start")]
    SessionStart(SessionStartData),
    #[serde(rename = "session.resume")]
    SessionResume(SessionResumeData),
    #[serde(rename = "session.remote_steerable_changed")]
    SessionRemoteSteerableChanged(SessionRemoteSteerableChangedData),
    #[serde(rename = "session.error")]
    SessionError(SessionErrorData),
    #[serde(rename = "session.idle")]
    SessionIdle(SessionIdleData),
    #[serde(rename = "session.title_changed")]
    SessionTitleChanged(SessionTitleChangedData),
    #[serde(rename = "session.schedule_created")]
    SessionScheduleCreated(SessionScheduleCreatedData),
    #[serde(rename = "session.schedule_cancelled")]
    SessionScheduleCancelled(SessionScheduleCancelledData),
    #[serde(rename = "session.info")]
    SessionInfo(SessionInfoData),
    #[serde(rename = "session.warning")]
    SessionWarning(SessionWarningData),
    #[serde(rename = "session.model_change")]
    SessionModelChange(SessionModelChangeData),
    #[serde(rename = "session.mode_changed")]
    SessionModeChanged(SessionModeChangedData),
    #[serde(rename = "session.plan_changed")]
    SessionPlanChanged(SessionPlanChangedData),
    #[serde(rename = "session.workspace_file_changed")]
    SessionWorkspaceFileChanged(SessionWorkspaceFileChangedData),
    #[serde(rename = "session.handoff")]
    SessionHandoff(SessionHandoffData),
    #[serde(rename = "session.truncation")]
    SessionTruncation(SessionTruncationData),
    #[serde(rename = "session.snapshot_rewind")]
    SessionSnapshotRewind(SessionSnapshotRewindData),
    #[serde(rename = "session.shutdown")]
    SessionShutdown(SessionShutdownData),
    #[serde(rename = "session.context_changed")]
    SessionContextChanged(SessionContextChangedData),
    #[serde(rename = "session.usage_info")]
    SessionUsageInfo(SessionUsageInfoData),
    #[serde(rename = "session.compaction_start")]
    SessionCompactionStart(SessionCompactionStartData),
    #[serde(rename = "session.compaction_complete")]
    SessionCompactionComplete(SessionCompactionCompleteData),
    #[serde(rename = "session.task_complete")]
    SessionTaskComplete(SessionTaskCompleteData),
    #[serde(rename = "user.message")]
    UserMessage(UserMessageData),
    #[serde(rename = "pending_messages.modified")]
    PendingMessagesModified(PendingMessagesModifiedData),
    #[serde(rename = "assistant.turn_start")]
    AssistantTurnStart(AssistantTurnStartData),
    #[serde(rename = "assistant.intent")]
    AssistantIntent(AssistantIntentData),
    #[serde(rename = "assistant.reasoning")]
    AssistantReasoning(AssistantReasoningData),
    #[serde(rename = "assistant.reasoning_delta")]
    AssistantReasoningDelta(AssistantReasoningDeltaData),
    #[serde(rename = "assistant.streaming_delta")]
    AssistantStreamingDelta(AssistantStreamingDeltaData),
    #[serde(rename = "assistant.message")]
    AssistantMessage(AssistantMessageData),
    #[serde(rename = "assistant.message_start")]
    AssistantMessageStart(AssistantMessageStartData),
    #[serde(rename = "assistant.message_delta")]
    AssistantMessageDelta(AssistantMessageDeltaData),
    #[serde(rename = "assistant.turn_end")]
    AssistantTurnEnd(AssistantTurnEndData),
    #[serde(rename = "assistant.usage")]
    AssistantUsage(AssistantUsageData),
    #[serde(rename = "model.call_failure")]
    ModelCallFailure(ModelCallFailureData),
    #[serde(rename = "abort")]
    Abort(AbortData),
    #[serde(rename = "tool.user_requested")]
    ToolUserRequested(ToolUserRequestedData),
    #[serde(rename = "tool.execution_start")]
    ToolExecutionStart(ToolExecutionStartData),
    #[serde(rename = "tool.execution_partial_result")]
    ToolExecutionPartialResult(ToolExecutionPartialResultData),
    #[serde(rename = "tool.execution_progress")]
    ToolExecutionProgress(ToolExecutionProgressData),
    #[serde(rename = "tool.execution_complete")]
    ToolExecutionComplete(ToolExecutionCompleteData),
    #[serde(rename = "skill.invoked")]
    SkillInvoked(SkillInvokedData),
    #[serde(rename = "subagent.started")]
    SubagentStarted(SubagentStartedData),
    #[serde(rename = "subagent.completed")]
    SubagentCompleted(SubagentCompletedData),
    #[serde(rename = "subagent.failed")]
    SubagentFailed(SubagentFailedData),
    #[serde(rename = "subagent.selected")]
    SubagentSelected(SubagentSelectedData),
    #[serde(rename = "subagent.deselected")]
    SubagentDeselected(SubagentDeselectedData),
    #[serde(rename = "hook.start")]
    HookStart(HookStartData),
    #[serde(rename = "hook.end")]
    HookEnd(HookEndData),
    #[serde(rename = "system.message")]
    SystemMessage(SystemMessageData),
    #[serde(rename = "system.notification")]
    SystemNotification(SystemNotificationData),
    #[serde(rename = "permission.requested")]
    PermissionRequested(PermissionRequestedData),
    #[serde(rename = "permission.completed")]
    PermissionCompleted(PermissionCompletedData),
    #[serde(rename = "user_input.requested")]
    UserInputRequested(UserInputRequestedData),
    #[serde(rename = "user_input.completed")]
    UserInputCompleted(UserInputCompletedData),
    #[serde(rename = "elicitation.requested")]
    ElicitationRequested(ElicitationRequestedData),
    #[serde(rename = "elicitation.completed")]
    ElicitationCompleted(ElicitationCompletedData),
    #[serde(rename = "sampling.requested")]
    SamplingRequested(SamplingRequestedData),
    #[serde(rename = "sampling.completed")]
    SamplingCompleted(SamplingCompletedData),
    #[serde(rename = "mcp.oauth_required")]
    McpOauthRequired(McpOauthRequiredData),
    #[serde(rename = "mcp.oauth_completed")]
    McpOauthCompleted(McpOauthCompletedData),
    #[serde(rename = "session.custom_notification")]
    SessionCustomNotification(SessionCustomNotificationData),
    #[serde(rename = "external_tool.requested")]
    ExternalToolRequested(ExternalToolRequestedData),
    #[serde(rename = "external_tool.completed")]
    ExternalToolCompleted(ExternalToolCompletedData),
    #[serde(rename = "command.queued")]
    CommandQueued(CommandQueuedData),
    #[serde(rename = "command.execute")]
    CommandExecute(CommandExecuteData),
    #[serde(rename = "command.completed")]
    CommandCompleted(CommandCompletedData),
    #[serde(rename = "auto_mode_switch.requested")]
    AutoModeSwitchRequested(AutoModeSwitchRequestedData),
    #[serde(rename = "auto_mode_switch.completed")]
    AutoModeSwitchCompleted(AutoModeSwitchCompletedData),
    #[serde(rename = "commands.changed")]
    CommandsChanged(CommandsChangedData),
    #[serde(rename = "capabilities.changed")]
    CapabilitiesChanged(CapabilitiesChangedData),
    #[serde(rename = "exit_plan_mode.requested")]
    ExitPlanModeRequested(ExitPlanModeRequestedData),
    #[serde(rename = "exit_plan_mode.completed")]
    ExitPlanModeCompleted(ExitPlanModeCompletedData),
    #[serde(rename = "session.tools_updated")]
    SessionToolsUpdated(SessionToolsUpdatedData),
    #[serde(rename = "session.background_tasks_changed")]
    SessionBackgroundTasksChanged(SessionBackgroundTasksChangedData),
    #[serde(rename = "session.skills_loaded")]
    SessionSkillsLoaded(SessionSkillsLoadedData),
    #[serde(rename = "session.custom_agents_updated")]
    SessionCustomAgentsUpdated(SessionCustomAgentsUpdatedData),
    #[serde(rename = "session.mcp_servers_loaded")]
    SessionMcpServersLoaded(SessionMcpServersLoadedData),
    #[serde(rename = "session.mcp_server_status_changed")]
    SessionMcpServerStatusChanged(SessionMcpServerStatusChangedData),
    #[serde(rename = "session.extensions_loaded")]
    SessionExtensionsLoaded(SessionExtensionsLoadedData),
}

/// A session event with typed data payload.
///
/// The common event fields (id, timestamp, parentId, ephemeral, agentId)
/// are available directly. The event-specific data is in the `payload`
/// field as a [`SessionEventData`] enum.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TypedSessionEvent {
    /// Unique event identifier (UUID v4).
    pub id: String,
    /// ISO 8601 timestamp when the event was created.
    pub timestamp: String,
    /// ID of the preceding event in the chain.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parent_id: Option<String>,
    /// When true, the event is transient and not persisted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ephemeral: Option<bool>,
    /// Sub-agent instance identifier. Absent for events from the root /
    /// main agent and session-level events.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_id: Option<String>,
    /// The typed event payload (discriminated by event type).
    #[serde(flatten)]
    pub payload: SessionEventData,
}

/// Working directory and git context at session start
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkingDirectoryContext {
    /// Base commit of current git branch at session start time
    #[serde(skip_serializing_if = "Option::is_none")]
    pub base_commit: Option<String>,
    /// Current git branch name
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Current working directory path
    pub cwd: String,
    /// Root directory of the git repository, resolved via git rev-parse
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Head commit of current git branch at session start time
    #[serde(skip_serializing_if = "Option::is_none")]
    pub head_commit: Option<String>,
    /// Hosting platform type of the repository (github or ado)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host_type: Option<WorkingDirectoryContextHostType>,
    /// Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    /// Raw host string from the git remote URL (e.g. "github.com", "mycompany.ghe.com", "dev.azure.com")
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository_host: Option<String>,
}

/// Session event "session.start". Session initialization metadata including context and configuration
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionStartData {
    /// Whether the session was already in use by another client at start time
    #[serde(skip_serializing_if = "Option::is_none")]
    pub already_in_use: Option<bool>,
    /// Working directory and git context at session start
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context: Option<WorkingDirectoryContext>,
    /// Version string of the Copilot application
    pub copilot_version: String,
    /// When set, identifies a parent session whose context this session continues — e.g., a detached headless rem-agent run launched on the parent's interactive shutdown. Telemetry from this session is reported under the parent's session_id.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub detached_from_spawning_parent_session_id: Option<String>,
    /// Identifier of the software producing the events (e.g., "copilot-agent")
    pub producer: String,
    /// Reasoning effort level used for model calls, if applicable (e.g. "none", "low", "medium", "high", "xhigh", "max")
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    /// Reasoning summary mode used for model calls, if applicable (e.g. "none", "concise", "detailed")
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_summary: Option<ReasoningSummary>,
    /// Whether this session supports remote steering via GitHub
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_steerable: Option<bool>,
    /// Model selected at session creation time, if any
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selected_model: Option<String>,
    /// Unique identifier for the session
    pub session_id: SessionId,
    /// ISO 8601 timestamp when the session was created
    pub start_time: String,
    /// Schema version number for the session event format
    pub version: i64,
}

/// Session event "session.resume". Session resume metadata including current context and event count
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionResumeData {
    /// Whether the session was already in use by another client at resume time
    #[serde(skip_serializing_if = "Option::is_none")]
    pub already_in_use: Option<bool>,
    /// Updated working directory and git context at resume time
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context: Option<WorkingDirectoryContext>,
    /// When true, tool calls and permission requests left in flight by the previous session lifetime remain pending after resume and the agentic loop awaits their results. User sends are queued behind the pending work until all such requests reach a terminal state. When false (the default), any such tool calls and permission requests are immediately marked as interrupted on resume.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub continue_pending_work: Option<bool>,
    /// Total number of persisted events in the session at the time of resume
    pub event_count: i64,
    /// Reasoning effort level used for model calls, if applicable (e.g. "none", "low", "medium", "high", "xhigh", "max")
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    /// Reasoning summary mode used for model calls, if applicable (e.g. "none", "concise", "detailed")
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_summary: Option<ReasoningSummary>,
    /// Whether this session supports remote steering via GitHub
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_steerable: Option<bool>,
    /// ISO 8601 timestamp when the session was resumed
    pub resume_time: String,
    /// Model currently selected at resume time
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selected_model: Option<String>,
    /// True when this resume attached to a session that the runtime already had running in-memory (for example, an extension joining a session another client was actively driving). False (or omitted) for cold resumes — the runtime had to reconstitute the session from its persisted event log.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_was_active: Option<bool>,
}

/// Session event "session.remote_steerable_changed". Notifies that the session's remote steering capability has changed
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionRemoteSteerableChangedData {
    /// Whether this session now supports remote steering via GitHub
    pub remote_steerable: bool,
}

/// Session event "session.error". Error details for timeline display including message and optional diagnostic information
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionErrorData {
    /// Only set on `errorType: "rate_limit"`. When `true`, the runtime will follow this error with an `auto_mode_switch.requested` event (or silently switch if `continueOnAutoMode` is enabled). UI clients can use this flag to suppress duplicate rendering of the rate-limit error when they show their own auto-mode-switch prompt.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub eligible_for_auto_switch: Option<bool>,
    /// Fine-grained error code from the upstream provider, when available. For `errorType: "rate_limit"`, this is one of the `RateLimitErrorCode` values (e.g., `"user_weekly_rate_limited"`, `"user_global_rate_limited"`, `"rate_limited"`, `"user_model_rate_limited"`, `"integration_rate_limited"`). For `errorType: "quota"`, this is the CAPI quota error code (e.g., `"quota_exceeded"`, `"session_quota_exceeded"`, `"billing_not_configured"`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error_code: Option<String>,
    /// Category of error (e.g., "authentication", "authorization", "quota", "rate_limit", "context_limit", "query")
    pub error_type: String,
    /// Human-readable error message
    pub message: String,
    /// GitHub request tracing ID (x-github-request-id header) for correlating with server-side logs
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider_call_id: Option<String>,
    /// Error stack trace, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub stack: Option<String>,
    /// HTTP status code from the upstream request, if applicable
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status_code: Option<i32>,
    /// Optional URL associated with this error that the user can open in a browser
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Session event "session.idle". Payload indicating the session is idle with no background agents in flight
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionIdleData {
    /// True when the preceding agentic loop was cancelled via abort signal
    #[serde(skip_serializing_if = "Option::is_none")]
    pub aborted: Option<bool>,
}

/// Session event "session.title_changed". Session title change payload containing the new display title
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTitleChangedData {
    /// The new display title for the session
    pub title: String,
}

/// Session event "session.schedule_created". Scheduled prompt registered via /every or /after
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionScheduleCreatedData {
    /// Optional user-facing label shown in the timeline instead of the actual prompt (e.g. `/skill-name args` when the prompt is a skill invocation expansion)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_prompt: Option<String>,
    /// Sequential id assigned to the scheduled prompt within the session
    pub id: i64,
    /// Interval between ticks in milliseconds
    pub interval_ms: i64,
    /// Prompt text that gets enqueued on every tick
    pub prompt: String,
    /// Whether the schedule re-arms after each tick (`/every`) or fires once (`/after`)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub recurring: Option<bool>,
}

/// Session event "session.schedule_cancelled". Scheduled prompt cancelled from the schedule manager dialog
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionScheduleCancelledData {
    /// Id of the scheduled prompt that was cancelled
    pub id: i64,
}

/// Session event "session.info". Informational message for timeline display with categorization
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionInfoData {
    /// Category of informational message (e.g., "notification", "timing", "context_window", "mcp", "snapshot", "configuration", "authentication", "model")
    pub info_type: String,
    /// Human-readable informational message for display in the timeline
    pub message: String,
    /// Optional actionable tip displayed with this message
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tip: Option<String>,
    /// Optional URL associated with this message that the user can open in a browser
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Session event "session.warning". Warning message for timeline display with categorization
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWarningData {
    /// Human-readable warning message for display in the timeline
    pub message: String,
    /// Optional URL associated with this warning that the user can open in a browser
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
    /// Category of warning (e.g., "subscription", "policy", "mcp")
    pub warning_type: String,
}

/// Session event "session.model_change". Model change details including previous and new model identifiers
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelChangeData {
    /// Reason the change happened, when not user-initiated. Currently `"rate_limit_auto_switch"` for changes triggered by the auto-mode-switch rate-limit recovery path. UI clients can use this to render contextual copy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cause: Option<String>,
    /// Newly selected model identifier
    pub new_model: String,
    /// Model that was previously selected, if any
    #[serde(skip_serializing_if = "Option::is_none")]
    pub previous_model: Option<String>,
    /// Reasoning effort level before the model change, if applicable
    #[serde(skip_serializing_if = "Option::is_none")]
    pub previous_reasoning_effort: Option<String>,
    /// Reasoning summary mode before the model change, if applicable
    #[serde(skip_serializing_if = "Option::is_none")]
    pub previous_reasoning_summary: Option<ReasoningSummary>,
    /// Reasoning effort level after the model change, if applicable
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    /// Reasoning summary mode after the model change, if applicable
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_summary: Option<ReasoningSummary>,
}

/// Session event "session.mode_changed". Agent mode change details including previous and new modes
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModeChangedData {
    /// The session mode the agent is operating in
    pub new_mode: SessionMode,
    /// The session mode the agent is operating in
    pub previous_mode: SessionMode,
}

/// Session event "session.plan_changed". Plan file operation details indicating what changed
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPlanChangedData {
    /// The type of operation performed on the plan file
    pub operation: PlanChangedOperation,
}

/// Session event "session.workspace_file_changed". Workspace file change details including path and operation type
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspaceFileChangedData {
    /// Whether the file was newly created or updated
    pub operation: WorkspaceFileChangedOperation,
    /// Relative path within the session workspace files directory
    pub path: String,
}

/// Repository context for the handed-off session
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HandoffRepository {
    /// Git branch name, if applicable
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Repository name
    pub name: String,
    /// Repository owner (user or organization)
    pub owner: String,
}

/// Session event "session.handoff". Session handoff metadata including source, context, and repository information
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHandoffData {
    /// Additional context information for the handoff
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context: Option<String>,
    /// ISO 8601 timestamp when the handoff occurred
    pub handoff_time: String,
    /// GitHub host URL for the source session (e.g., https://github.com or https://tenant.ghe.com)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<String>,
    /// Session ID of the remote session being handed off
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_session_id: Option<SessionId>,
    /// Repository context for the handed-off session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<HandoffRepository>,
    /// Origin type of the session being handed off
    pub source_type: HandoffSourceType,
    /// Summary of the work done in the source session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
}

/// Session event "session.truncation". Conversation truncation statistics including token counts and removed content metrics
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTruncationData {
    /// Number of messages removed by truncation
    pub messages_removed_during_truncation: i64,
    /// Identifier of the component that performed truncation (e.g., "BasicTruncator")
    pub performed_by: String,
    /// Number of conversation messages after truncation
    pub post_truncation_messages_length: i64,
    /// Total tokens in conversation messages after truncation
    pub post_truncation_tokens_in_messages: i64,
    /// Number of conversation messages before truncation
    pub pre_truncation_messages_length: i64,
    /// Total tokens in conversation messages before truncation
    pub pre_truncation_tokens_in_messages: i64,
    /// Maximum token count for the model's context window
    pub token_limit: i64,
    /// Number of tokens removed by truncation
    pub tokens_removed_during_truncation: i64,
}

/// Session event "session.snapshot_rewind". Session rewind details including target event and count of removed events
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSnapshotRewindData {
    /// Number of events that were removed by the rewind
    pub events_removed: i64,
    /// Event ID that was rewound to; this event and all after it were removed
    pub up_to_event_id: String,
}

/// Aggregate code change metrics for the session
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShutdownCodeChanges {
    /// List of file paths that were modified during the session
    pub files_modified: Vec<String>,
    /// Total number of lines added during the session
    pub lines_added: i64,
    /// Total number of lines removed during the session
    pub lines_removed: i64,
}

/// Request count and cost metrics
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShutdownModelMetricRequests {
    /// Cumulative cost multiplier for requests to this model
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cost: Option<f64>,
    /// Total number of API requests made to this model
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub count: Option<i64>,
}

/// Schema for the `ShutdownModelMetricTokenDetail` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShutdownModelMetricTokenDetail {
    /// Accumulated token count for this token type
    pub token_count: i64,
}

/// Token usage breakdown
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShutdownModelMetricUsage {
    /// Total tokens read from prompt cache across all requests
    pub cache_read_tokens: i64,
    /// Total tokens written to prompt cache across all requests
    pub cache_write_tokens: i64,
    /// Total input tokens consumed across all requests to this model
    pub input_tokens: i64,
    /// Total output tokens produced across all requests to this model
    pub output_tokens: i64,
    /// Total reasoning tokens produced across all requests to this model
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_tokens: Option<i64>,
}

/// Schema for the `ShutdownModelMetric` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShutdownModelMetric {
    /// Request count and cost metrics
    pub requests: ShutdownModelMetricRequests,
    /// Token count details per type
    #[serde(default)]
    pub token_details: HashMap<String, ShutdownModelMetricTokenDetail>,
    /// Accumulated nano-AI units cost for this model
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub total_nano_aiu: Option<f64>,
    /// Token usage breakdown
    pub usage: ShutdownModelMetricUsage,
}

/// Schema for the `ShutdownTokenDetail` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShutdownTokenDetail {
    /// Accumulated token count for this token type
    pub token_count: i64,
}

/// Session event "session.shutdown". Session termination metrics including usage statistics, code changes, and shutdown reason
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionShutdownData {
    /// Aggregate code change metrics for the session
    pub code_changes: ShutdownCodeChanges,
    /// Non-system message token count at shutdown
    #[serde(skip_serializing_if = "Option::is_none")]
    pub conversation_tokens: Option<i64>,
    /// Model that was selected at the time of shutdown
    #[serde(skip_serializing_if = "Option::is_none")]
    pub current_model: Option<String>,
    /// Total tokens in context window at shutdown
    #[serde(skip_serializing_if = "Option::is_none")]
    pub current_tokens: Option<i64>,
    /// Error description when shutdownType is "error"
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error_reason: Option<String>,
    /// Per-model usage breakdown, keyed by model identifier
    pub model_metrics: HashMap<String, ShutdownModelMetric>,
    /// Unix timestamp (milliseconds) when the session started
    pub session_start_time: i64,
    /// Whether the session ended normally ("routine") or due to a crash/fatal error ("error")
    pub shutdown_type: ShutdownType,
    /// System message token count at shutdown
    #[serde(skip_serializing_if = "Option::is_none")]
    pub system_tokens: Option<i64>,
    /// Session-wide per-token-type accumulated token counts
    #[serde(default)]
    pub token_details: HashMap<String, ShutdownTokenDetail>,
    /// Tool definitions token count at shutdown
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_definitions_tokens: Option<i64>,
    /// Cumulative time spent in API calls during the session, in milliseconds
    pub total_api_duration_ms: i64,
    /// Session-wide accumulated nano-AI units cost
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub total_nano_aiu: Option<f64>,
    /// Total number of premium API requests used during the session
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) total_premium_requests: Option<f64>,
}

/// Session event "session.context_changed". Updated working directory and git context after the change
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionContextChangedData {
    /// Base commit of current git branch at session start time
    #[serde(skip_serializing_if = "Option::is_none")]
    pub base_commit: Option<String>,
    /// Current git branch name
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Current working directory path
    pub cwd: String,
    /// Root directory of the git repository, resolved via git rev-parse
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Head commit of current git branch at session start time
    #[serde(skip_serializing_if = "Option::is_none")]
    pub head_commit: Option<String>,
    /// Hosting platform type of the repository (github or ado)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host_type: Option<WorkingDirectoryContextHostType>,
    /// Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    /// Raw host string from the git remote URL (e.g. "github.com", "mycompany.ghe.com", "dev.azure.com")
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository_host: Option<String>,
}

/// Session event "session.usage_info". Current context window usage statistics including token and message counts
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUsageInfoData {
    /// Token count from non-system messages (user, assistant, tool)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub conversation_tokens: Option<i64>,
    /// Current number of tokens in the context window
    pub current_tokens: i64,
    /// Whether this is the first usage_info event emitted in this session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_initial: Option<bool>,
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

/// Session event "session.compaction_start". Context window breakdown at the start of LLM-powered conversation compaction
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCompactionStartData {
    /// Token count from non-system messages (user, assistant, tool) at compaction start
    #[serde(skip_serializing_if = "Option::is_none")]
    pub conversation_tokens: Option<i64>,
    /// Token count from system message(s) at compaction start
    #[serde(skip_serializing_if = "Option::is_none")]
    pub system_tokens: Option<i64>,
    /// Token count from tool definitions at compaction start
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_definitions_tokens: Option<i64>,
}

/// Token usage detail for a single billing category
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CompactionCompleteCompactionTokensUsedCopilotUsageTokenDetail {
    /// Number of tokens in this billing batch
    pub batch_size: i64,
    /// Cost per batch of tokens
    pub cost_per_batch: i64,
    /// Total token count for this entry
    pub token_count: i64,
    /// Token category (e.g., "input", "output")
    pub token_type: String,
}

/// Per-request cost and usage data from the CAPI copilot_usage response field
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct CompactionCompleteCompactionTokensUsedCopilotUsage {
    /// Itemized token usage breakdown
    pub token_details: Vec<CompactionCompleteCompactionTokensUsedCopilotUsageTokenDetail>,
    /// Total cost in nano-AI units for this request
    pub total_nano_aiu: f64,
}

/// Token usage breakdown for the compaction LLM call (aligned with assistant.usage format)
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CompactionCompleteCompactionTokensUsed {
    /// Cached input tokens reused in the compaction LLM call
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_read_tokens: Option<i64>,
    /// Tokens written to prompt cache in the compaction LLM call
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_write_tokens: Option<i64>,
    /// Per-request cost and usage data from the CAPI copilot_usage response field
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) copilot_usage: Option<CompactionCompleteCompactionTokensUsedCopilotUsage>,
    /// Duration of the compaction LLM call in milliseconds
    #[serde(skip_serializing_if = "Option::is_none")]
    pub duration: Option<i64>,
    /// Input tokens consumed by the compaction LLM call
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input_tokens: Option<i64>,
    /// Model identifier used for the compaction LLM call
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Output tokens produced by the compaction LLM call
    #[serde(skip_serializing_if = "Option::is_none")]
    pub output_tokens: Option<i64>,
}

/// Session event "session.compaction_complete". Conversation compaction results including success status, metrics, and optional error details
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCompactionCompleteData {
    /// Checkpoint snapshot number created for recovery
    #[serde(skip_serializing_if = "Option::is_none")]
    pub checkpoint_number: Option<i64>,
    /// File path where the checkpoint was stored
    #[serde(skip_serializing_if = "Option::is_none")]
    pub checkpoint_path: Option<String>,
    /// Token usage breakdown for the compaction LLM call (aligned with assistant.usage format)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub compaction_tokens_used: Option<CompactionCompleteCompactionTokensUsed>,
    /// Token count from non-system messages (user, assistant, tool) after compaction
    #[serde(skip_serializing_if = "Option::is_none")]
    pub conversation_tokens: Option<i64>,
    /// User-supplied focus instructions provided to a manual `/compact` invocation. Omitted for automatic compaction and for manual compaction with no focus text.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub custom_instructions: Option<String>,
    /// Error message if compaction failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Number of messages removed during compaction
    #[serde(skip_serializing_if = "Option::is_none")]
    pub messages_removed: Option<i64>,
    /// Total tokens in conversation after compaction
    #[serde(skip_serializing_if = "Option::is_none")]
    pub post_compaction_tokens: Option<i64>,
    /// Number of messages before compaction
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pre_compaction_messages_length: Option<i64>,
    /// Total tokens in conversation before compaction
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pre_compaction_tokens: Option<i64>,
    /// GitHub request tracing ID (x-github-request-id header) for the compaction LLM call
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_id: Option<RequestId>,
    /// Whether compaction completed successfully
    pub success: bool,
    /// LLM-generated summary of the compacted conversation history
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary_content: Option<String>,
    /// Token count from system message(s) after compaction
    #[serde(skip_serializing_if = "Option::is_none")]
    pub system_tokens: Option<i64>,
    /// Number of tokens removed during compaction
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tokens_removed: Option<i64>,
    /// Token count from tool definitions after compaction
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_definitions_tokens: Option<i64>,
}

/// Session event "session.task_complete". Task completion notification with summary from the agent
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTaskCompleteData {
    /// Whether the tool call succeeded. False when validation failed (e.g., invalid arguments)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub success: Option<bool>,
    /// Summary of the completed task, provided by the agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
}

/// Session event "user.message".
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserMessageData {
    /// The agent mode that was active when this message was sent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_mode: Option<UserMessageAgentMode>,
    /// Files, selections, or GitHub references attached to the message
    #[serde(default)]
    pub attachments: Vec<serde_json::Value>,
    /// The user's message text as displayed in the timeline
    pub content: String,
    /// CAPI interaction ID for correlating this user message with its turn
    #[serde(skip_serializing_if = "Option::is_none")]
    pub interaction_id: Option<String>,
    /// True when this user message was auto-injected by autopilot's continuation loop rather than typed by the user; used to distinguish autopilot-driven turns in telemetry.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_autopilot_continuation: Option<bool>,
    /// Path-backed native document attachments that stayed on the tagged_files path flow because native upload could not read them or would exceed the request size limit
    #[serde(default)]
    pub native_document_path_fallback_paths: Vec<String>,
    /// Parent agent task ID for background telemetry correlated to this user turn
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parent_agent_task_id: Option<String>,
    /// Origin of this message, used for timeline filtering (e.g., "skill-pdf" for skill-injected messages that should be hidden from the user)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<String>,
    /// Normalized document MIME types that were sent natively instead of through tagged_files XML
    #[serde(default)]
    pub supported_native_document_mime_types: Vec<String>,
    /// Transformed version of the message sent to the model, with XML wrapping, timestamps, and other augmentations for prompt caching
    #[serde(skip_serializing_if = "Option::is_none")]
    pub transformed_content: Option<String>,
}

/// Session event "pending_messages.modified". Empty payload; the event signals that the pending message queue has changed
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PendingMessagesModifiedData {}

/// Session event "assistant.turn_start". Turn initialization metadata including identifier and interaction tracking
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantTurnStartData {
    /// CAPI interaction ID for correlating this turn with upstream telemetry
    #[serde(skip_serializing_if = "Option::is_none")]
    pub interaction_id: Option<String>,
    /// Identifier for this turn within the agentic loop, typically a stringified turn number
    pub turn_id: String,
}

/// Session event "assistant.intent". Agent intent description for current activity or plan
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantIntentData {
    /// Short description of what the agent is currently doing or planning to do
    pub intent: String,
}

/// Session event "assistant.reasoning". Assistant reasoning content for timeline display with complete thinking text
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantReasoningData {
    /// The complete extended thinking text from the model
    pub content: String,
    /// Unique identifier for this reasoning block
    pub reasoning_id: String,
}

/// Session event "assistant.reasoning_delta". Streaming reasoning delta for incremental extended thinking updates
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantReasoningDeltaData {
    /// Incremental text chunk to append to the reasoning content
    pub delta_content: String,
    /// Reasoning block ID this delta belongs to, matching the corresponding assistant.reasoning event
    pub reasoning_id: String,
}

/// Session event "assistant.streaming_delta". Streaming response progress with cumulative byte count
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantStreamingDeltaData {
    /// Cumulative total bytes received from the streaming response so far
    pub total_response_size_bytes: i64,
}

/// A tool invocation request from the assistant
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantMessageToolRequest {
    /// Arguments to pass to the tool, format depends on the tool
    #[serde(skip_serializing_if = "Option::is_none")]
    pub arguments: Option<serde_json::Value>,
    /// Resolved intention summary describing what this specific call does
    #[serde(skip_serializing_if = "Option::is_none")]
    pub intention_summary: Option<String>,
    /// Name of the MCP server hosting this tool, when the tool is an MCP tool
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_server_name: Option<String>,
    /// Original tool name on the MCP server, when the tool is an MCP tool
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_tool_name: Option<String>,
    /// Name of the tool being invoked
    pub name: String,
    /// Unique identifier for this tool call
    pub tool_call_id: String,
    /// Human-readable display title for the tool
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_title: Option<String>,
    /// Tool call type: "function" for standard tool calls, "custom" for grammar-based tool calls. Defaults to "function" when absent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<AssistantMessageToolRequestType>,
}

/// Session event "assistant.message". Assistant response containing text content, optional tool requests, and interaction metadata
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantMessageData {
    /// Raw Anthropic content array with advisor blocks (server_tool_use, advisor_tool_result) for verbatim round-tripping
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(default)]
    pub anthropic_advisor_blocks: Vec<serde_json::Value>,
    /// Anthropic advisor model ID used for this response, for timeline display on replay
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub anthropic_advisor_model: Option<String>,
    /// The assistant's text response content
    pub content: String,
    /// Encrypted reasoning content from OpenAI models. Session-bound and stripped on resume.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub encrypted_content: Option<String>,
    /// CAPI interaction ID for correlating this message with upstream telemetry
    #[serde(skip_serializing_if = "Option::is_none")]
    pub interaction_id: Option<String>,
    /// Unique identifier for this assistant message
    pub message_id: String,
    /// Model that produced this assistant message, if known
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Actual output token count from the API response (completion_tokens), used for accurate token accounting
    #[serde(skip_serializing_if = "Option::is_none")]
    pub output_tokens: Option<i64>,
    /// Tool call ID of the parent tool invocation when this event originates from a sub-agent
    #[doc(hidden)]
    #[deprecated]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parent_tool_call_id: Option<String>,
    /// Generation phase for phased-output models (e.g., thinking vs. response phases)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub phase: Option<String>,
    /// Opaque/encrypted extended thinking data from Anthropic models. Session-bound and stripped on resume.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_opaque: Option<String>,
    /// Readable reasoning text from the model's extended thinking
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_text: Option<String>,
    /// GitHub request tracing ID (x-github-request-id header) for correlating with server-side logs
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_id: Option<RequestId>,
    /// Tool invocations requested by the assistant in this message
    #[serde(default)]
    pub tool_requests: Vec<AssistantMessageToolRequest>,
    /// Identifier for the agent loop turn that produced this message, matching the corresponding assistant.turn_start event
    #[serde(skip_serializing_if = "Option::is_none")]
    pub turn_id: Option<String>,
}

/// Session event "assistant.message_start". Streaming assistant message start metadata
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantMessageStartData {
    /// Message ID this start event belongs to, matching subsequent deltas and assistant.message
    pub message_id: String,
    /// Generation phase this message belongs to for phased-output models
    #[serde(skip_serializing_if = "Option::is_none")]
    pub phase: Option<String>,
}

/// Session event "assistant.message_delta". Streaming assistant message delta for incremental response updates
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantMessageDeltaData {
    /// Incremental text chunk to append to the message content
    pub delta_content: String,
    /// Message ID this delta belongs to, matching the corresponding assistant.message event
    pub message_id: String,
    /// Tool call ID of the parent tool invocation when this event originates from a sub-agent
    #[doc(hidden)]
    #[deprecated]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parent_tool_call_id: Option<String>,
}

/// Session event "assistant.turn_end". Turn completion metadata including the turn identifier
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantTurnEndData {
    /// Identifier of the turn that has ended, matching the corresponding assistant.turn_start event
    pub turn_id: String,
}

/// Token usage detail for a single billing category
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantUsageCopilotUsageTokenDetail {
    /// Number of tokens in this billing batch
    pub batch_size: i64,
    /// Cost per batch of tokens
    pub cost_per_batch: i64,
    /// Total token count for this entry
    pub token_count: i64,
    /// Token category (e.g., "input", "output")
    pub token_type: String,
}

/// Per-request cost and usage data from the CAPI copilot_usage response field
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AssistantUsageCopilotUsage {
    /// Itemized token usage breakdown
    pub token_details: Vec<AssistantUsageCopilotUsageTokenDetail>,
    /// Total cost in nano-AI units for this request
    pub total_nano_aiu: f64,
}

/// Schema for the `AssistantUsageQuotaSnapshot` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AssistantUsageQuotaSnapshot {
    /// Total requests allowed by the entitlement
    #[doc(hidden)]
    pub(crate) entitlement_requests: i64,
    /// Whether the user has an unlimited usage entitlement
    #[doc(hidden)]
    pub(crate) is_unlimited_entitlement: bool,
    /// Number of additional usage requests made this period
    #[doc(hidden)]
    pub(crate) overage: f64,
    /// Whether additional usage is allowed when quota is exhausted
    #[doc(hidden)]
    pub(crate) overage_allowed_with_exhausted_quota: bool,
    /// Percentage of quota remaining (0 to 100)
    #[doc(hidden)]
    pub(crate) remaining_percentage: f64,
    /// Date when the quota resets
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) reset_date: Option<String>,
    /// Whether usage is still permitted after quota exhaustion
    #[doc(hidden)]
    pub(crate) usage_allowed_with_exhausted_quota: bool,
    /// Number of requests already consumed
    #[doc(hidden)]
    pub(crate) used_requests: i64,
}

/// Session event "assistant.usage". LLM API call usage metrics including tokens, costs, quotas, and billing information
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AssistantUsageData {
    /// Completion ID from the model provider (e.g., chatcmpl-abc123)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api_call_id: Option<String>,
    /// API endpoint used for this model call, matching CAPI supported_endpoints vocabulary
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api_endpoint: Option<AssistantUsageApiEndpoint>,
    /// Number of tokens read from prompt cache
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_read_tokens: Option<i64>,
    /// Number of tokens written to prompt cache
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_write_tokens: Option<i64>,
    /// Per-request cost and usage data from the CAPI copilot_usage response field
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) copilot_usage: Option<AssistantUsageCopilotUsage>,
    /// Model multiplier cost for billing purposes
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cost: Option<f64>,
    /// Duration of the API call in milliseconds
    #[serde(skip_serializing_if = "Option::is_none")]
    pub duration: Option<i64>,
    /// What initiated this API call (e.g., "sub-agent", "mcp-sampling"); absent for user-initiated calls
    #[serde(skip_serializing_if = "Option::is_none")]
    pub initiator: Option<String>,
    /// Number of input tokens consumed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input_tokens: Option<i64>,
    /// Average inter-token latency in milliseconds. Only available for streaming requests
    #[serde(skip_serializing_if = "Option::is_none")]
    pub inter_token_latency_ms: Option<f64>,
    /// Model identifier used for this API call
    pub model: String,
    /// Number of output tokens produced
    #[serde(skip_serializing_if = "Option::is_none")]
    pub output_tokens: Option<i64>,
    /// Parent tool call ID when this usage originates from a sub-agent
    #[doc(hidden)]
    #[deprecated]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parent_tool_call_id: Option<String>,
    /// GitHub request tracing ID (x-github-request-id header) for server-side log correlation
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider_call_id: Option<String>,
    /// Per-quota resource usage snapshots, keyed by quota identifier
    #[doc(hidden)]
    #[serde(default)]
    pub(crate) quota_snapshots: HashMap<String, AssistantUsageQuotaSnapshot>,
    /// Reasoning effort level used for model calls, if applicable (e.g. "none", "low", "medium", "high", "xhigh", "max")
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    /// Number of output tokens used for reasoning (e.g., chain-of-thought)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_tokens: Option<i64>,
    /// Time to first token in milliseconds. Only available for streaming requests
    #[serde(skip_serializing_if = "Option::is_none")]
    pub time_to_first_token_ms: Option<i64>,
}

/// Session event "model.call_failure". Failed LLM API call metadata for telemetry
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCallFailureData {
    /// Completion ID from the model provider (e.g., chatcmpl-abc123)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api_call_id: Option<String>,
    /// Duration of the failed API call in milliseconds
    #[serde(skip_serializing_if = "Option::is_none")]
    pub duration_ms: Option<i64>,
    /// Raw provider/runtime error message for restricted telemetry
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error_message: Option<String>,
    /// What initiated this API call (e.g., "sub-agent", "mcp-sampling"); absent for user-initiated calls
    #[serde(skip_serializing_if = "Option::is_none")]
    pub initiator: Option<String>,
    /// Model identifier used for the failed API call
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// GitHub request tracing ID (x-github-request-id header) for server-side log correlation
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider_call_id: Option<String>,
    /// Where the failed model call originated
    pub source: ModelCallFailureSource,
    /// HTTP status code from the failed request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status_code: Option<i32>,
}

/// Session event "abort". Turn abort information including the reason for termination
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AbortData {
    /// Finite reason code describing why the current turn was aborted
    pub reason: AbortReason,
}

/// Session event "tool.user_requested". User-initiated tool invocation request with tool name and arguments
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolUserRequestedData {
    /// Arguments for the tool invocation
    #[serde(skip_serializing_if = "Option::is_none")]
    pub arguments: Option<serde_json::Value>,
    /// Unique identifier for this tool call
    pub tool_call_id: String,
    /// Name of the tool the user wants to invoke
    pub tool_name: String,
}

/// Session event "tool.execution_start". Tool execution startup details including MCP server information when applicable
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolExecutionStartData {
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
    #[doc(hidden)]
    #[deprecated]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parent_tool_call_id: Option<String>,
    /// Unique identifier for this tool call
    pub tool_call_id: String,
    /// Name of the tool being executed
    pub tool_name: String,
    /// Identifier for the agent loop turn this tool was invoked in, matching the corresponding assistant.turn_start event
    #[serde(skip_serializing_if = "Option::is_none")]
    pub turn_id: Option<String>,
}

/// Session event "tool.execution_partial_result". Streaming tool execution output for incremental result display
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolExecutionPartialResultData {
    /// Incremental output chunk from the running tool
    pub partial_output: String,
    /// Tool call ID this partial result belongs to
    pub tool_call_id: String,
}

/// Session event "tool.execution_progress". Tool execution progress notification with status message
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolExecutionProgressData {
    /// Human-readable progress status message (e.g., from an MCP server)
    pub progress_message: String,
    /// Tool call ID this progress notification belongs to
    pub tool_call_id: String,
}

/// Error details when the tool execution failed
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolExecutionCompleteError {
    /// Machine-readable error code
    #[serde(skip_serializing_if = "Option::is_none")]
    pub code: Option<String>,
    /// Human-readable error message
    pub message: String,
}

/// Plain text content block
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolExecutionCompleteContentText {
    /// The text content
    pub text: String,
    /// Content block type discriminator
    pub r#type: ToolExecutionCompleteContentTextType,
}

/// Terminal/shell output content block with optional exit code and working directory
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolExecutionCompleteContentTerminal {
    /// Working directory where the command was executed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Process exit code, if the command has completed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exit_code: Option<i64>,
    /// Terminal/shell output text
    pub text: String,
    /// Content block type discriminator
    pub r#type: ToolExecutionCompleteContentTerminalType,
}

/// Image content block with base64-encoded data
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolExecutionCompleteContentImage {
    /// Base64-encoded image data
    pub data: String,
    /// MIME type of the image (e.g., image/png, image/jpeg)
    pub mime_type: String,
    /// Content block type discriminator
    pub r#type: ToolExecutionCompleteContentImageType,
}

/// Audio content block with base64-encoded data
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolExecutionCompleteContentAudio {
    /// Base64-encoded audio data
    pub data: String,
    /// MIME type of the audio (e.g., audio/wav, audio/mpeg)
    pub mime_type: String,
    /// Content block type discriminator
    pub r#type: ToolExecutionCompleteContentAudioType,
}

/// Icon image for a resource
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolExecutionCompleteContentResourceLinkIcon {
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
    pub theme: Option<ToolExecutionCompleteContentResourceLinkIconTheme>,
}

/// Resource link content block referencing an external resource
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolExecutionCompleteContentResourceLink {
    /// Human-readable description of the resource
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Icons associated with this resource
    #[serde(default)]
    pub icons: Vec<ToolExecutionCompleteContentResourceLinkIcon>,
    /// MIME type of the resource content
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mime_type: Option<String>,
    /// Resource name identifier
    pub name: String,
    /// Size of the resource in bytes
    #[serde(skip_serializing_if = "Option::is_none")]
    pub size: Option<i64>,
    /// Human-readable display title for the resource
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Content block type discriminator
    pub r#type: ToolExecutionCompleteContentResourceLinkType,
    /// URI identifying the resource
    pub uri: String,
}

/// Schema for the `EmbeddedTextResourceContents` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EmbeddedTextResourceContents {
    /// MIME type of the text content
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mime_type: Option<String>,
    /// Text content of the resource
    pub text: String,
    /// URI identifying the resource
    pub uri: String,
}

/// Schema for the `EmbeddedBlobResourceContents` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EmbeddedBlobResourceContents {
    /// Base64-encoded binary content of the resource
    pub blob: String,
    /// MIME type of the blob content
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mime_type: Option<String>,
    /// URI identifying the resource
    pub uri: String,
}

/// Embedded resource content block with inline text or binary data
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolExecutionCompleteContentResource {
    /// The embedded resource contents, either text or base64-encoded binary
    pub resource: ToolExecutionCompleteContentResourceDetails,
    /// Content block type discriminator
    pub r#type: ToolExecutionCompleteContentResourceType,
}

/// Tool execution result on success
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolExecutionCompleteResult {
    /// Concise tool result text sent to the LLM for chat completion, potentially truncated for token efficiency
    pub content: String,
    /// Structured content blocks (text, images, audio, resources) returned by the tool in their native format
    #[serde(default)]
    pub contents: Vec<ToolExecutionCompleteContent>,
    /// Full detailed tool result for UI/timeline display, preserving complete content such as diffs. Falls back to content when absent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub detailed_content: Option<String>,
}

/// Session event "tool.execution_complete". Tool execution completion results including success status, detailed output, and error information
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolExecutionCompleteData {
    /// Error details when the tool execution failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<ToolExecutionCompleteError>,
    /// CAPI interaction ID for correlating this tool execution with upstream telemetry
    #[serde(skip_serializing_if = "Option::is_none")]
    pub interaction_id: Option<String>,
    /// Whether this tool call was explicitly requested by the user rather than the assistant
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_user_requested: Option<bool>,
    /// Model identifier that generated this tool call
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Tool call ID of the parent tool invocation when this event originates from a sub-agent
    #[doc(hidden)]
    #[deprecated]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parent_tool_call_id: Option<String>,
    /// Tool execution result on success
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<ToolExecutionCompleteResult>,
    /// Whether this tool execution ran inside a sandbox container
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandboxed: Option<bool>,
    /// Whether the tool execution completed successfully
    pub success: bool,
    /// Unique identifier for the completed tool call
    pub tool_call_id: String,
    /// Tool-specific telemetry data (e.g., CodeQL check counts, grep match counts)
    #[serde(default)]
    pub tool_telemetry: HashMap<String, serde_json::Value>,
    /// Identifier for the agent loop turn this tool was invoked in, matching the corresponding assistant.turn_start event
    #[serde(skip_serializing_if = "Option::is_none")]
    pub turn_id: Option<String>,
}

/// Session event "skill.invoked". Skill invocation details including content, allowed tools, and plugin metadata
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInvokedData {
    /// Tool names that should be auto-approved when this skill is active
    #[serde(default)]
    pub allowed_tools: Vec<String>,
    /// Full content of the skill file, injected into the conversation for the model
    pub content: String,
    /// Description of the skill from its SKILL.md frontmatter
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Name of the invoked skill
    pub name: String,
    /// File path to the SKILL.md definition
    pub path: String,
    /// Name of the plugin this skill originated from, when applicable
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plugin_name: Option<String>,
    /// Version of the plugin this skill originated from, when applicable
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plugin_version: Option<String>,
}

/// Session event "subagent.started". Sub-agent startup details including parent tool call and agent information
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SubagentStartedData {
    /// Description of what the sub-agent does
    pub agent_description: String,
    /// Human-readable display name of the sub-agent
    pub agent_display_name: String,
    /// Internal name of the sub-agent
    pub agent_name: String,
    /// Model the sub-agent will run with, when known at start. Surfaced in the timeline for auto-selected sub-agents (e.g. rubber-duck).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Tool call ID of the parent tool invocation that spawned this sub-agent
    pub tool_call_id: String,
}

/// Session event "subagent.completed". Sub-agent completion details for successful execution
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SubagentCompletedData {
    /// Human-readable display name of the sub-agent
    pub agent_display_name: String,
    /// Internal name of the sub-agent
    pub agent_name: String,
    /// Wall-clock duration of the sub-agent execution in milliseconds
    #[serde(skip_serializing_if = "Option::is_none")]
    pub duration_ms: Option<i64>,
    /// Model used by the sub-agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Tool call ID of the parent tool invocation that spawned this sub-agent
    pub tool_call_id: String,
    /// Total tokens (input + output) consumed by the sub-agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub total_tokens: Option<i64>,
    /// Total number of tool calls made by the sub-agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub total_tool_calls: Option<i64>,
}

/// Session event "subagent.failed". Sub-agent failure details including error message and agent information
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SubagentFailedData {
    /// Human-readable display name of the sub-agent
    pub agent_display_name: String,
    /// Internal name of the sub-agent
    pub agent_name: String,
    /// Wall-clock duration of the sub-agent execution in milliseconds
    #[serde(skip_serializing_if = "Option::is_none")]
    pub duration_ms: Option<i64>,
    /// Error message describing why the sub-agent failed
    pub error: String,
    /// Model used by the sub-agent (if any model calls succeeded before failure)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Tool call ID of the parent tool invocation that spawned this sub-agent
    pub tool_call_id: String,
    /// Total tokens (input + output) consumed before the sub-agent failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub total_tokens: Option<i64>,
    /// Total number of tool calls made before the sub-agent failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub total_tool_calls: Option<i64>,
}

/// Session event "subagent.selected". Custom agent selection details including name and available tools
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SubagentSelectedData {
    /// Human-readable display name of the selected custom agent
    pub agent_display_name: String,
    /// Internal name of the selected custom agent
    pub agent_name: String,
    /// List of tool names available to this agent, or null for all tools
    pub tools: Vec<String>,
}

/// Session event "subagent.deselected". Empty payload; the event signals that the custom agent was deselected, returning to the default agent
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SubagentDeselectedData {}

/// Session event "hook.start". Hook invocation start details including type and input data
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
pub struct HookEndError {
    /// Human-readable error message
    pub message: String,
    /// Error stack trace, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub stack: Option<String>,
}

/// Session event "hook.end". Hook invocation completion details including output, success status, and error information
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HookEndData {
    /// Error details when the hook failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<HookEndError>,
    /// Identifier matching the corresponding hook.start event
    pub hook_invocation_id: String,
    /// Type of hook that was invoked (e.g., "preToolUse", "postToolUse", "sessionStart")
    pub hook_type: String,
    /// Output data produced by the hook
    #[serde(skip_serializing_if = "Option::is_none")]
    pub output: Option<serde_json::Value>,
    /// Whether the hook completed successfully
    pub success: bool,
}

/// Metadata about the prompt template and its construction
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SystemMessageMetadata {
    /// Version identifier of the prompt template used
    #[serde(skip_serializing_if = "Option::is_none")]
    pub prompt_version: Option<String>,
    /// Template variables used when constructing the prompt
    #[serde(default)]
    pub variables: HashMap<String, serde_json::Value>,
}

/// Session event "system.message". System/developer instruction content with role and optional template metadata
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SystemMessageData {
    /// The system or developer prompt text sent as model input
    pub content: String,
    /// Metadata about the prompt template and its construction
    #[serde(skip_serializing_if = "Option::is_none")]
    pub metadata: Option<SystemMessageMetadata>,
    /// Optional name identifier for the message source
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Message role: "system" for system prompts, "developer" for developer-injected instructions
    pub role: SystemMessageRole,
}

/// Session event "system.notification". System-generated notification for runtime events like background task completion
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SystemNotificationData {
    /// The notification text, typically wrapped in <system_notification> XML tags
    pub content: String,
    /// Structured metadata identifying what triggered this notification
    pub kind: serde_json::Value,
}

/// Schema for the `PermissionRequestShellCommand` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequestShellCommand {
    /// Command identifier (e.g., executable name)
    pub identifier: String,
    /// Whether this command is read-only (no side effects)
    pub read_only: bool,
}

/// Schema for the `PermissionRequestShellPossibleUrl` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequestShellPossibleUrl {
    /// URL that may be accessed by the command
    pub url: String,
}

/// Shell command permission request
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequestShell {
    /// Whether the UI can offer session-wide approval for this command pattern
    pub can_offer_session_approval: bool,
    /// Parsed command identifiers found in the command text
    pub commands: Vec<PermissionRequestShellCommand>,
    /// The complete shell command text to be executed
    pub full_command_text: String,
    /// Whether the command includes a file write redirection (e.g., > or >>)
    pub has_write_file_redirection: bool,
    /// Human-readable description of what the command intends to do
    pub intention: String,
    /// Permission kind discriminator
    pub kind: PermissionRequestShellKind,
    /// File paths that may be read or written by the command
    pub possible_paths: Vec<String>,
    /// URLs that may be accessed by the command
    pub possible_urls: Vec<PermissionRequestShellPossibleUrl>,
    /// Tool call ID that triggered this permission request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
    /// Optional warning message about risks of running this command
    #[serde(skip_serializing_if = "Option::is_none")]
    pub warning: Option<String>,
}

/// File write permission request
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequestWrite {
    /// Whether the UI can offer session-wide approval for file write operations
    pub can_offer_session_approval: bool,
    /// Unified diff showing the proposed changes
    pub diff: String,
    /// Path of the file being written to
    pub file_name: String,
    /// Human-readable description of the intended file change
    pub intention: String,
    /// Permission kind discriminator
    pub kind: PermissionRequestWriteKind,
    /// Complete new file contents for newly created files
    #[serde(skip_serializing_if = "Option::is_none")]
    pub new_file_contents: Option<String>,
    /// Tool call ID that triggered this permission request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
}

/// File or directory read permission request
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequestRead {
    /// Human-readable description of why the file is being read
    pub intention: String,
    /// Permission kind discriminator
    pub kind: PermissionRequestReadKind,
    /// Path of the file or directory being read
    pub path: String,
    /// Tool call ID that triggered this permission request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
}

/// MCP tool invocation permission request
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequestMcp {
    /// Arguments to pass to the MCP tool
    #[serde(skip_serializing_if = "Option::is_none")]
    pub args: Option<serde_json::Value>,
    /// Permission kind discriminator
    pub kind: PermissionRequestMcpKind,
    /// Whether this MCP tool is read-only (no side effects)
    pub read_only: bool,
    /// Name of the MCP server providing the tool
    pub server_name: String,
    /// Tool call ID that triggered this permission request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
    /// Internal name of the MCP tool
    pub tool_name: String,
    /// Human-readable title of the MCP tool
    pub tool_title: String,
}

/// URL access permission request
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequestUrl {
    /// Human-readable description of why the URL is being accessed
    pub intention: String,
    /// Permission kind discriminator
    pub kind: PermissionRequestUrlKind,
    /// Tool call ID that triggered this permission request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
    /// URL to be fetched
    pub url: String,
}

/// Memory operation permission request
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequestMemory {
    /// Whether this is a store or vote memory operation
    #[serde(skip_serializing_if = "Option::is_none")]
    pub action: Option<PermissionRequestMemoryAction>,
    /// Source references for the stored fact (store only)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub citations: Option<String>,
    /// Vote direction (vote only)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub direction: Option<PermissionRequestMemoryDirection>,
    /// The fact being stored or voted on
    pub fact: String,
    /// Permission kind discriminator
    pub kind: PermissionRequestMemoryKind,
    /// Reason for the vote (vote only)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// Topic or subject of the memory (store only)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub subject: Option<String>,
    /// Tool call ID that triggered this permission request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
}

/// Custom tool invocation permission request
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequestCustomTool {
    /// Arguments to pass to the custom tool
    #[serde(skip_serializing_if = "Option::is_none")]
    pub args: Option<serde_json::Value>,
    /// Permission kind discriminator
    pub kind: PermissionRequestCustomToolKind,
    /// Tool call ID that triggered this permission request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
    /// Description of what the custom tool does
    pub tool_description: String,
    /// Name of the custom tool
    pub tool_name: String,
}

/// Hook confirmation permission request
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequestHook {
    /// Optional message from the hook explaining why confirmation is needed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub hook_message: Option<String>,
    /// Permission kind discriminator
    pub kind: PermissionRequestHookKind,
    /// Arguments of the tool call being gated
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_args: Option<serde_json::Value>,
    /// Tool call ID that triggered this permission request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
    /// Name of the tool the hook is gating
    pub tool_name: String,
}

/// Extension management permission request
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequestExtensionManagement {
    /// Name of the extension being managed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub extension_name: Option<String>,
    /// Permission kind discriminator
    pub kind: PermissionRequestExtensionManagementKind,
    /// The extension management operation (scaffold, reload)
    pub operation: String,
    /// Tool call ID that triggered this permission request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
}

/// Extension permission access request
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequestExtensionPermissionAccess {
    /// Capabilities the extension is requesting
    pub capabilities: Vec<String>,
    /// Name of the extension requesting permission access
    pub extension_name: String,
    /// Permission kind discriminator
    pub kind: PermissionRequestExtensionPermissionAccessKind,
    /// Tool call ID that triggered this permission request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
}

/// Shell command permission prompt
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPromptRequestCommands {
    /// Whether the UI can offer session-wide approval for this command pattern
    pub can_offer_session_approval: bool,
    /// Command identifiers covered by this approval prompt
    pub command_identifiers: Vec<String>,
    /// The complete shell command text to be executed
    pub full_command_text: String,
    /// Human-readable description of what the command intends to do
    pub intention: String,
    /// Prompt kind discriminator
    pub kind: PermissionPromptRequestCommandsKind,
    /// Tool call ID that triggered this permission request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
    /// Optional warning message about risks of running this command
    #[serde(skip_serializing_if = "Option::is_none")]
    pub warning: Option<String>,
}

/// File write permission prompt
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPromptRequestWrite {
    /// Whether the UI can offer session-wide approval for file write operations
    pub can_offer_session_approval: bool,
    /// Unified diff showing the proposed changes
    pub diff: String,
    /// Path of the file being written to
    pub file_name: String,
    /// Human-readable description of the intended file change
    pub intention: String,
    /// Prompt kind discriminator
    pub kind: PermissionPromptRequestWriteKind,
    /// Complete new file contents for newly created files
    #[serde(skip_serializing_if = "Option::is_none")]
    pub new_file_contents: Option<String>,
    /// Tool call ID that triggered this permission request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
}

/// File read permission prompt
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPromptRequestRead {
    /// Human-readable description of why the file is being read
    pub intention: String,
    /// Prompt kind discriminator
    pub kind: PermissionPromptRequestReadKind,
    /// Path of the file or directory being read
    pub path: String,
    /// Tool call ID that triggered this permission request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
}

/// MCP tool invocation permission prompt
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPromptRequestMcp {
    /// Arguments to pass to the MCP tool
    #[serde(skip_serializing_if = "Option::is_none")]
    pub args: Option<serde_json::Value>,
    /// Prompt kind discriminator
    pub kind: PermissionPromptRequestMcpKind,
    /// Name of the MCP server providing the tool
    pub server_name: String,
    /// Tool call ID that triggered this permission request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
    /// Internal name of the MCP tool
    pub tool_name: String,
    /// Human-readable title of the MCP tool
    pub tool_title: String,
}

/// URL access permission prompt
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPromptRequestUrl {
    /// Human-readable description of why the URL is being accessed
    pub intention: String,
    /// Prompt kind discriminator
    pub kind: PermissionPromptRequestUrlKind,
    /// Tool call ID that triggered this permission request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
    /// URL to be fetched
    pub url: String,
}

/// Memory operation permission prompt
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPromptRequestMemory {
    /// Whether this is a store or vote memory operation
    #[serde(skip_serializing_if = "Option::is_none")]
    pub action: Option<PermissionRequestMemoryAction>,
    /// Source references for the stored fact (store only)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub citations: Option<String>,
    /// Vote direction (vote only)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub direction: Option<PermissionRequestMemoryDirection>,
    /// The fact being stored or voted on
    pub fact: String,
    /// Prompt kind discriminator
    pub kind: PermissionPromptRequestMemoryKind,
    /// Reason for the vote (vote only)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// Topic or subject of the memory (store only)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub subject: Option<String>,
    /// Tool call ID that triggered this permission request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
}

/// Custom tool invocation permission prompt
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPromptRequestCustomTool {
    /// Arguments to pass to the custom tool
    #[serde(skip_serializing_if = "Option::is_none")]
    pub args: Option<serde_json::Value>,
    /// Prompt kind discriminator
    pub kind: PermissionPromptRequestCustomToolKind,
    /// Tool call ID that triggered this permission request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
    /// Description of what the custom tool does
    pub tool_description: String,
    /// Name of the custom tool
    pub tool_name: String,
}

/// Path access permission prompt
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPromptRequestPath {
    /// Underlying permission kind that needs path approval
    pub access_kind: PermissionPromptRequestPathAccessKind,
    /// Prompt kind discriminator
    pub kind: PermissionPromptRequestPathKind,
    /// File paths that require explicit approval
    pub paths: Vec<String>,
    /// Tool call ID that triggered this permission request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
}

/// Hook confirmation permission prompt
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPromptRequestHook {
    /// Optional message from the hook explaining why confirmation is needed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub hook_message: Option<String>,
    /// Prompt kind discriminator
    pub kind: PermissionPromptRequestHookKind,
    /// Arguments of the tool call being gated
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_args: Option<serde_json::Value>,
    /// Tool call ID that triggered this permission request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
    /// Name of the tool the hook is gating
    pub tool_name: String,
}

/// Extension management permission prompt
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPromptRequestExtensionManagement {
    /// Name of the extension being managed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub extension_name: Option<String>,
    /// Prompt kind discriminator
    pub kind: PermissionPromptRequestExtensionManagementKind,
    /// The extension management operation (scaffold, reload)
    pub operation: String,
    /// Tool call ID that triggered this permission request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
}

/// Extension permission access prompt
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPromptRequestExtensionPermissionAccess {
    /// Capabilities the extension is requesting
    pub capabilities: Vec<String>,
    /// Name of the extension requesting permission access
    pub extension_name: String,
    /// Prompt kind discriminator
    pub kind: PermissionPromptRequestExtensionPermissionAccessKind,
    /// Tool call ID that triggered this permission request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
}

/// Session event "permission.requested". Permission request notification requiring client approval with request details
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequestedData {
    /// Details of the permission being requested
    pub permission_request: PermissionRequest,
    /// Derived user-facing permission prompt details for UI consumers
    #[serde(skip_serializing_if = "Option::is_none")]
    pub prompt_request: Option<PermissionPromptRequest>,
    /// Unique identifier for this permission request; used to respond via session.respondToPermission()
    pub request_id: RequestId,
    /// When true, this permission was already resolved by a permissionRequest hook and requires no client action
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resolved_by_hook: Option<bool>,
}

/// Schema for the `PermissionApproved` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionApproved {
    /// The permission request was approved
    pub kind: PermissionApprovedKind,
}

/// Schema for the `UserToolSessionApprovalCommands` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserToolSessionApprovalCommands {
    /// Command identifiers approved by the user
    pub command_identifiers: Vec<String>,
    /// Command approval kind
    pub kind: UserToolSessionApprovalCommandsKind,
}

/// Schema for the `UserToolSessionApprovalRead` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserToolSessionApprovalRead {
    /// Read approval kind
    pub kind: UserToolSessionApprovalReadKind,
}

/// Schema for the `UserToolSessionApprovalWrite` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserToolSessionApprovalWrite {
    /// Write approval kind
    pub kind: UserToolSessionApprovalWriteKind,
}

/// Schema for the `UserToolSessionApprovalMcp` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserToolSessionApprovalMcp {
    /// MCP tool approval kind
    pub kind: UserToolSessionApprovalMcpKind,
    /// MCP server name
    pub server_name: String,
    /// Optional MCP tool name, or null for all tools on the server
    pub tool_name: Option<String>,
}

/// Schema for the `UserToolSessionApprovalMemory` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserToolSessionApprovalMemory {
    /// Memory approval kind
    pub kind: UserToolSessionApprovalMemoryKind,
}

/// Schema for the `UserToolSessionApprovalCustomTool` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserToolSessionApprovalCustomTool {
    /// Custom tool approval kind
    pub kind: UserToolSessionApprovalCustomToolKind,
    /// Custom tool name
    pub tool_name: String,
}

/// Schema for the `UserToolSessionApprovalExtensionManagement` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserToolSessionApprovalExtensionManagement {
    /// Extension management approval kind
    pub kind: UserToolSessionApprovalExtensionManagementKind,
    /// Optional operation identifier
    #[serde(skip_serializing_if = "Option::is_none")]
    pub operation: Option<String>,
}

/// Schema for the `UserToolSessionApprovalExtensionPermissionAccess` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserToolSessionApprovalExtensionPermissionAccess {
    /// Extension name
    pub extension_name: String,
    /// Extension permission access approval kind
    pub kind: UserToolSessionApprovalExtensionPermissionAccessKind,
}

/// Schema for the `PermissionApprovedForSession` type.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionApprovedForSession {
    /// The approval to add as a session-scoped rule
    pub approval: UserToolSessionApproval,
    /// Approved and remembered for the rest of the session
    pub kind: PermissionApprovedForSessionKind,
}

/// Schema for the `PermissionApprovedForLocation` type.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionApprovedForLocation {
    /// The approval to persist for this location
    pub approval: UserToolSessionApproval,
    /// Approved and persisted for this project location
    pub kind: PermissionApprovedForLocationKind,
    /// The location key (git root or cwd) to persist the approval to
    pub location_key: String,
}

/// Schema for the `PermissionCancelled` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionCancelled {
    /// The permission request was cancelled before a response was used
    pub kind: PermissionCancelledKind,
    /// Optional explanation of why the request was cancelled
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
}

/// Schema for the `PermissionRule` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRule {
    /// Argument value matched against the request, or null when the rule kind has no argument (e.g. 'read', 'write', 'memory').
    pub argument: Option<String>,
    /// The rule kind, such as Shell or GitHubMCP
    pub kind: String,
}

/// Schema for the `PermissionDeniedByRules` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDeniedByRules {
    /// Denied because approval rules explicitly blocked it
    pub kind: PermissionDeniedByRulesKind,
    /// Rules that denied the request
    pub rules: Vec<PermissionRule>,
}

/// Schema for the `PermissionDeniedNoApprovalRuleAndCouldNotRequestFromUser` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDeniedNoApprovalRuleAndCouldNotRequestFromUser {
    /// Denied because no approval rule matched and user confirmation was unavailable
    pub kind: PermissionDeniedNoApprovalRuleAndCouldNotRequestFromUserKind,
}

/// Schema for the `PermissionDeniedInteractivelyByUser` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDeniedInteractivelyByUser {
    /// Optional feedback from the user explaining the denial
    #[serde(skip_serializing_if = "Option::is_none")]
    pub feedback: Option<String>,
    /// Whether to force-reject the current agent turn
    #[serde(skip_serializing_if = "Option::is_none")]
    pub force_reject: Option<bool>,
    /// Denied by the user during an interactive prompt
    pub kind: PermissionDeniedInteractivelyByUserKind,
}

/// Schema for the `PermissionDeniedByContentExclusionPolicy` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDeniedByContentExclusionPolicy {
    /// Denied by the organization's content exclusion policy
    pub kind: PermissionDeniedByContentExclusionPolicyKind,
    /// Human-readable explanation of why the path was excluded
    pub message: String,
    /// File path that triggered the exclusion
    pub path: String,
}

/// Schema for the `PermissionDeniedByPermissionRequestHook` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDeniedByPermissionRequestHook {
    /// Whether to interrupt the current agent turn
    #[serde(skip_serializing_if = "Option::is_none")]
    pub interrupt: Option<bool>,
    /// Denied by a permission request hook registered by an extension or plugin
    pub kind: PermissionDeniedByPermissionRequestHookKind,
    /// Optional message from the hook explaining the denial
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
}

/// Session event "permission.completed". Permission request completion notification signaling UI dismissal
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionCompletedData {
    /// Request ID of the resolved permission request; clients should dismiss any UI for this request
    pub request_id: RequestId,
    /// The result of the permission request
    pub result: PermissionResult,
    /// Optional tool call ID associated with this permission prompt; clients may use it to correlate UI created from tool-scoped prompts
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
}

/// Session event "user_input.requested". User input request notification with question and optional predefined choices
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserInputRequestedData {
    /// Whether the user can provide a free-form text response in addition to predefined choices
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allow_freeform: Option<bool>,
    /// Predefined choices for the user to select from, if applicable
    #[serde(default)]
    pub choices: Vec<String>,
    /// The question or prompt to present to the user
    pub question: String,
    /// Unique identifier for this input request; used to respond via session.respondToUserInput()
    pub request_id: RequestId,
    /// The LLM-assigned tool call ID that triggered this request; used by remote UIs to correlate responses
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
}

/// Session event "user_input.completed". User input request completion with the user's response
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserInputCompletedData {
    /// The user's answer to the input request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub answer: Option<String>,
    /// Request ID of the resolved user input request; clients should dismiss any UI for this request
    pub request_id: RequestId,
    /// Whether the answer was typed as free-form text rather than selected from choices
    #[serde(skip_serializing_if = "Option::is_none")]
    pub was_freeform: Option<bool>,
}

/// JSON Schema describing the form fields to present to the user (form mode only)
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ElicitationRequestedSchema {
    /// Form field definitions, keyed by field name
    pub properties: HashMap<String, serde_json::Value>,
    /// List of required field names
    #[serde(default)]
    pub required: Vec<String>,
    /// Schema type indicator (always 'object')
    pub r#type: ElicitationRequestedSchemaType,
}

/// Session event "elicitation.requested". Elicitation request; may be form-based (structured input) or URL-based (browser redirect)
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ElicitationRequestedData {
    /// The source that initiated the request (MCP server name, or absent for agent-initiated)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub elicitation_source: Option<String>,
    /// Message describing what information is needed from the user
    pub message: String,
    /// Elicitation mode; "form" for structured input, "url" for browser-based. Defaults to "form" when absent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<ElicitationRequestedMode>,
    /// JSON Schema describing the form fields to present to the user (form mode only)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub requested_schema: Option<ElicitationRequestedSchema>,
    /// Unique identifier for this elicitation request; used to respond via session.respondToElicitation()
    pub request_id: RequestId,
    /// Tool call ID from the LLM completion; used to correlate with CompletionChunk.toolCall.id for remote UIs
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
    /// URL to open in the user's browser (url mode only)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Session event "elicitation.completed". Elicitation request completion with the user's response
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ElicitationCompletedData {
    /// The user action: "accept" (submitted form), "decline" (explicitly refused), or "cancel" (dismissed)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub action: Option<ElicitationCompletedAction>,
    /// The submitted form data when action is 'accept'; keys match the requested schema fields
    #[serde(default)]
    pub content: HashMap<String, serde_json::Value>,
    /// Request ID of the resolved elicitation request; clients should dismiss any UI for this request
    pub request_id: RequestId,
}

/// Session event "sampling.requested". Sampling request from an MCP server; contains the server name and a requestId for correlation
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SamplingRequestedData {
    /// The JSON-RPC request ID from the MCP protocol
    pub mcp_request_id: serde_json::Value,
    /// Unique identifier for this sampling request; used to respond via session.respondToSampling()
    pub request_id: RequestId,
    /// Name of the MCP server that initiated the sampling request
    pub server_name: String,
}

/// Session event "sampling.completed". Sampling request completion notification signaling UI dismissal
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SamplingCompletedData {
    /// Request ID of the resolved sampling request; clients should dismiss any UI for this request
    pub request_id: RequestId,
}

/// Static OAuth client configuration, if the server specifies one
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthRequiredStaticClientConfig {
    /// OAuth client ID for the server
    pub client_id: String,
    /// Optional non-default OAuth grant type. When set to 'client_credentials', the OAuth flow runs headlessly using the client_id + keychain-stored secret (no browser, no callback server).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub grant_type: Option<McpOauthRequiredStaticClientConfigGrantType>,
    /// Whether this is a public OAuth client
    #[serde(skip_serializing_if = "Option::is_none")]
    pub public_client: Option<bool>,
}

/// Session event "mcp.oauth_required". OAuth authentication request for an MCP server
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthRequiredData {
    /// Unique identifier for this OAuth request; used to respond via session.respondToMcpOAuth()
    pub request_id: RequestId,
    /// Display name of the MCP server that requires OAuth
    pub server_name: String,
    /// URL of the MCP server that requires OAuth
    pub server_url: String,
    /// Static OAuth client configuration, if the server specifies one
    #[serde(skip_serializing_if = "Option::is_none")]
    pub static_client_config: Option<McpOauthRequiredStaticClientConfig>,
}

/// Session event "mcp.oauth_completed". MCP OAuth request completion notification
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthCompletedData {
    /// Request ID of the resolved OAuth request
    pub request_id: RequestId,
}

/// Session event "session.custom_notification". Opaque custom notification data. Consumers may branch on source and name, but payload semantics are source-defined.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCustomNotificationData {
    /// Source-defined custom notification name
    pub name: String,
    /// Source-defined JSON payload for the custom notification
    pub payload: serde_json::Value,
    /// Namespace for the custom notification producer
    pub source: String,
    /// Optional source-defined string identifiers describing the payload subject
    #[serde(default)]
    pub subject: HashMap<String, String>,
    /// Optional source-defined payload schema version
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<i64>,
}

/// Session event "external_tool.requested". External tool invocation request for client-side tool execution
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolRequestedData {
    /// Arguments to pass to the external tool
    #[serde(skip_serializing_if = "Option::is_none")]
    pub arguments: Option<serde_json::Value>,
    /// Unique identifier for this request; used to respond via session.respondToExternalTool()
    pub request_id: RequestId,
    /// Session ID that this external tool request belongs to
    pub session_id: SessionId,
    /// Tool call ID assigned to this external tool invocation
    pub tool_call_id: String,
    /// Name of the external tool to invoke
    pub tool_name: String,
    /// W3C Trace Context traceparent header for the execute_tool span
    #[serde(skip_serializing_if = "Option::is_none")]
    pub traceparent: Option<String>,
    /// W3C Trace Context tracestate header for the execute_tool span
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tracestate: Option<String>,
}

/// Session event "external_tool.completed". External tool completion notification signaling UI dismissal
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolCompletedData {
    /// Request ID of the resolved external tool request; clients should dismiss any UI for this request
    pub request_id: RequestId,
}

/// Session event "command.queued". Queued slash command dispatch request for client execution
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandQueuedData {
    /// The slash command text to be executed (e.g., /help, /clear)
    pub command: String,
    /// Unique identifier for this request; used to respond via session.respondToQueuedCommand()
    pub request_id: RequestId,
}

/// Session event "command.execute". Registered command dispatch request routed to the owning client
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandExecuteData {
    /// Raw argument string after the command name
    pub args: String,
    /// The full command text (e.g., /deploy production)
    pub command: String,
    /// Command name without leading /
    pub command_name: String,
    /// Unique identifier; used to respond via session.commands.handlePendingCommand()
    pub request_id: RequestId,
}

/// Session event "command.completed". Queued command completion notification signaling UI dismissal
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandCompletedData {
    /// Request ID of the resolved command request; clients should dismiss any UI for this request
    pub request_id: RequestId,
}

/// Session event "auto_mode_switch.requested". Auto mode switch request notification requiring user approval
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AutoModeSwitchRequestedData {
    /// The rate limit error code that triggered this request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error_code: Option<String>,
    /// Unique identifier for this request; used to respond via session.respondToAutoModeSwitch()
    pub request_id: RequestId,
    /// Seconds until the rate limit resets, when known. Lets clients render a humanized reset time alongside the prompt.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub retry_after_seconds: Option<i64>,
}

/// Session event "auto_mode_switch.completed". Auto mode switch completion notification
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AutoModeSwitchCompletedData {
    /// Request ID of the resolved request; clients should dismiss any UI for this request
    pub request_id: RequestId,
    /// The user's auto-mode-switch choice
    pub response: AutoModeSwitchResponse,
}

/// Schema for the `CommandsChangedCommand` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsChangedCommand {
    /// Optional human-readable command description.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Slash command name without the leading slash.
    pub name: String,
}

/// Session event "commands.changed". SDK command registration change notification
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsChangedData {
    /// Current list of registered SDK commands
    pub commands: Vec<CommandsChangedCommand>,
}

/// UI capability changes
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CapabilitiesChangedUI {
    /// Whether elicitation is now supported
    #[serde(skip_serializing_if = "Option::is_none")]
    pub elicitation: Option<bool>,
}

/// Session event "capabilities.changed". Session capability change notification
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CapabilitiesChangedData {
    /// UI capability changes
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ui: Option<CapabilitiesChangedUI>,
}

/// Session event "exit_plan_mode.requested". Plan approval request with plan content and available user actions
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExitPlanModeRequestedData {
    /// Available actions the user can take
    pub actions: Vec<ExitPlanModeAction>,
    /// Full content of the plan file
    pub plan_content: String,
    /// Recommended action to preselect for the user
    pub recommended_action: ExitPlanModeAction,
    /// Unique identifier for this request; used to respond via session.respondToExitPlanMode()
    pub request_id: RequestId,
    /// Summary of the plan that was created
    pub summary: String,
}

/// Session event "exit_plan_mode.completed". Plan mode exit completion with the user's approval decision and optional feedback
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExitPlanModeCompletedData {
    /// Whether the plan was approved by the user
    #[serde(skip_serializing_if = "Option::is_none")]
    pub approved: Option<bool>,
    /// Whether edits should be auto-approved without confirmation
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auto_approve_edits: Option<bool>,
    /// Free-form feedback from the user if they requested changes to the plan
    #[serde(skip_serializing_if = "Option::is_none")]
    pub feedback: Option<String>,
    /// Request ID of the resolved exit plan mode request; clients should dismiss any UI for this request
    pub request_id: RequestId,
    /// Action selected by the user
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selected_action: Option<ExitPlanModeAction>,
}

/// Session event "session.tools_updated".
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionToolsUpdatedData {
    /// Identifier of the model the resolved tools apply to.
    pub model: String,
}

/// Session event "session.background_tasks_changed".
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionBackgroundTasksChangedData {}

/// Schema for the `SkillsLoadedSkill` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsLoadedSkill {
    /// Description of what the skill does
    pub description: String,
    /// Whether the skill is currently enabled
    pub enabled: bool,
    /// Unique identifier for the skill
    pub name: String,
    /// Absolute path to the skill file, if available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// Source location type (e.g., project, personal-copilot, plugin, builtin)
    pub source: SkillSource,
    /// Whether the skill can be invoked by the user as a slash command
    pub user_invocable: bool,
}

/// Session event "session.skills_loaded".
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsLoadedData {
    /// Array of resolved skill metadata
    pub skills: Vec<SkillsLoadedSkill>,
}

/// Schema for the `CustomAgentsUpdatedAgent` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CustomAgentsUpdatedAgent {
    /// Description of what the agent does
    pub description: String,
    /// Human-readable display name
    pub display_name: String,
    /// Unique identifier for the agent
    pub id: String,
    /// Model override for this agent, if set
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Internal name of the agent
    pub name: String,
    /// Source location: user, project, inherited, remote, or plugin
    pub source: String,
    /// List of tool names available to this agent, or null when all tools are available
    pub tools: Vec<String>,
    /// Whether the agent can be selected by the user
    pub user_invocable: bool,
}

/// Session event "session.custom_agents_updated".
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCustomAgentsUpdatedData {
    /// Array of loaded custom agent metadata
    pub agents: Vec<CustomAgentsUpdatedAgent>,
    /// Fatal errors from agent loading
    pub errors: Vec<String>,
    /// Non-fatal warnings from agent loading
    pub warnings: Vec<String>,
}

/// Schema for the `McpServersLoadedServer` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServersLoadedServer {
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

/// Session event "session.mcp_servers_loaded".
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpServersLoadedData {
    /// Array of MCP server status summaries
    pub servers: Vec<McpServersLoadedServer>,
}

/// Session event "session.mcp_server_status_changed".
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpServerStatusChangedData {
    /// Name of the MCP server whose status changed
    pub server_name: String,
    /// Connection status: connected, failed, needs-auth, pending, disabled, or not_configured
    pub status: McpServerStatus,
}

/// Schema for the `ExtensionsLoadedExtension` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionsLoadedExtension {
    /// Source-qualified extension ID (e.g., 'project:my-ext', 'user:auth-helper')
    pub id: String,
    /// Extension name (directory name)
    pub name: String,
    /// Discovery source
    pub source: ExtensionsLoadedExtensionSource,
    /// Current status: running, disabled, failed, or starting
    pub status: ExtensionsLoadedExtensionStatus,
}

/// Session event "session.extensions_loaded".
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionExtensionsLoadedData {
    /// Array of discovered extensions and their status
    pub extensions: Vec<ExtensionsLoadedExtension>,
}

/// Hosting platform type of the repository (github or ado)
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum WorkingDirectoryContextHostType {
    /// Repository is hosted on GitHub.
    #[serde(rename = "github")]
    Github,
    /// Repository is hosted on Azure DevOps.
    #[serde(rename = "ado")]
    Ado,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Reasoning summary mode used for model calls, if applicable (e.g. "none", "concise", "detailed")
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ReasoningSummary {
    /// Do not request reasoning summaries from the model.
    #[serde(rename = "none")]
    None,
    /// Request a concise summary of the model's reasoning.
    #[serde(rename = "concise")]
    Concise,
    /// Request a detailed summary of the model's reasoning.
    #[serde(rename = "detailed")]
    Detailed,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The session mode the agent is operating in
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionMode {
    /// The agent is responding interactively to the user.
    #[serde(rename = "interactive")]
    Interactive,
    /// The agent is preparing a plan before making changes.
    #[serde(rename = "plan")]
    Plan,
    /// The agent is working autonomously toward task completion.
    #[serde(rename = "autopilot")]
    Autopilot,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The type of operation performed on the plan file
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PlanChangedOperation {
    /// The plan file was created.
    #[serde(rename = "create")]
    Create,
    /// The plan file was updated.
    #[serde(rename = "update")]
    Update,
    /// The plan file was deleted.
    #[serde(rename = "delete")]
    Delete,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Whether the file was newly created or updated
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum WorkspaceFileChangedOperation {
    /// The workspace file was created.
    #[serde(rename = "create")]
    Create,
    /// The workspace file was updated.
    #[serde(rename = "update")]
    Update,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Origin type of the session being handed off
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum HandoffSourceType {
    /// The handoff originated from a remote session.
    #[serde(rename = "remote")]
    Remote,
    /// The handoff originated from a local session.
    #[serde(rename = "local")]
    Local,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Whether the session ended normally ("routine") or due to a crash/fatal error ("error")
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ShutdownType {
    /// The session ended normally.
    #[serde(rename = "routine")]
    Routine,
    /// The session ended because of a crash or fatal error.
    #[serde(rename = "error")]
    Error,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The agent mode that was active when this message was sent
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UserMessageAgentMode {
    /// The agent is responding interactively to the user.
    #[serde(rename = "interactive")]
    Interactive,
    /// The agent is preparing a plan before making changes.
    #[serde(rename = "plan")]
    Plan,
    /// The agent is working autonomously toward task completion.
    #[serde(rename = "autopilot")]
    Autopilot,
    /// The agent is in shell-focused UI mode.
    #[serde(rename = "shell")]
    Shell,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Tool call type: "function" for standard tool calls, "custom" for grammar-based tool calls. Defaults to "function" when absent.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AssistantMessageToolRequestType {
    /// Standard function-style tool call.
    #[serde(rename = "function")]
    Function,
    /// Custom grammar-based tool call.
    #[serde(rename = "custom")]
    Custom,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// API endpoint used for this model call, matching CAPI supported_endpoints vocabulary
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AssistantUsageApiEndpoint {
    /// Chat Completions API endpoint.
    #[serde(rename = "/chat/completions")]
    ChatCompletions,
    /// Anthropic Messages API endpoint.
    #[serde(rename = "/v1/messages")]
    V1Messages,
    /// Responses API endpoint.
    #[serde(rename = "/responses")]
    Responses,
    /// WebSocket Responses API endpoint.
    #[serde(rename = "ws:/responses")]
    WsResponses,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Where the failed model call originated
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ModelCallFailureSource {
    /// Model call from the top-level agent.
    #[serde(rename = "top_level")]
    TopLevel,
    /// Model call from a sub-agent.
    #[serde(rename = "subagent")]
    Subagent,
    /// Model call from MCP sampling.
    #[serde(rename = "mcp_sampling")]
    McpSampling,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Finite reason code describing why the current turn was aborted
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AbortReason {
    /// The local user requested the abort, for example by pressing Ctrl+C in the CLI.
    #[serde(rename = "user_initiated")]
    UserInitiated,
    /// A remote command requested the abort.
    #[serde(rename = "remote_command")]
    RemoteCommand,
    /// An MCP server delivered a user.abort notification.
    #[serde(rename = "user_abort")]
    UserAbort,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ToolExecutionCompleteContentTextType {
    #[serde(rename = "text")]
    #[default]
    Text,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ToolExecutionCompleteContentTerminalType {
    #[serde(rename = "terminal")]
    #[default]
    Terminal,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ToolExecutionCompleteContentImageType {
    #[serde(rename = "image")]
    #[default]
    Image,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ToolExecutionCompleteContentAudioType {
    #[serde(rename = "audio")]
    #[default]
    Audio,
}

/// Theme variant this icon is intended for
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ToolExecutionCompleteContentResourceLinkIconTheme {
    /// Icon intended for light themes.
    #[serde(rename = "light")]
    Light,
    /// Icon intended for dark themes.
    #[serde(rename = "dark")]
    Dark,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ToolExecutionCompleteContentResourceLinkType {
    #[serde(rename = "resource_link")]
    #[default]
    ResourceLink,
}

/// The embedded resource contents, either text or base64-encoded binary
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum ToolExecutionCompleteContentResourceDetails {
    EmbeddedTextResourceContents(EmbeddedTextResourceContents),
    EmbeddedBlobResourceContents(EmbeddedBlobResourceContents),
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ToolExecutionCompleteContentResourceType {
    #[serde(rename = "resource")]
    #[default]
    Resource,
}

/// A content block within a tool result, which may be text, terminal output, image, audio, or a resource
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum ToolExecutionCompleteContent {
    Text(ToolExecutionCompleteContentText),
    Terminal(ToolExecutionCompleteContentTerminal),
    Image(ToolExecutionCompleteContentImage),
    Audio(ToolExecutionCompleteContentAudio),
    ResourceLink(ToolExecutionCompleteContentResourceLink),
    Resource(ToolExecutionCompleteContentResource),
}

/// Message role: "system" for system prompts, "developer" for developer-injected instructions
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SystemMessageRole {
    /// System prompt message.
    #[serde(rename = "system")]
    System,
    /// Developer instruction message.
    #[serde(rename = "developer")]
    Developer,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Permission kind discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionRequestShellKind {
    #[serde(rename = "shell")]
    #[default]
    Shell,
}

/// Permission kind discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionRequestWriteKind {
    #[serde(rename = "write")]
    #[default]
    Write,
}

/// Permission kind discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionRequestReadKind {
    #[serde(rename = "read")]
    #[default]
    Read,
}

/// Permission kind discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionRequestMcpKind {
    #[serde(rename = "mcp")]
    #[default]
    Mcp,
}

/// Permission kind discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionRequestUrlKind {
    #[serde(rename = "url")]
    #[default]
    Url,
}

/// Whether this is a store or vote memory operation
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionRequestMemoryAction {
    /// Store a new memory.
    #[serde(rename = "store")]
    Store,
    /// Vote on an existing memory.
    #[serde(rename = "vote")]
    Vote,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Vote direction (vote only)
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionRequestMemoryDirection {
    /// Vote that the memory is useful or accurate.
    #[serde(rename = "upvote")]
    Upvote,
    /// Vote that the memory is incorrect or outdated.
    #[serde(rename = "downvote")]
    Downvote,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Permission kind discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionRequestMemoryKind {
    #[serde(rename = "memory")]
    #[default]
    Memory,
}

/// Permission kind discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionRequestCustomToolKind {
    #[serde(rename = "custom-tool")]
    #[default]
    CustomTool,
}

/// Permission kind discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionRequestHookKind {
    #[serde(rename = "hook")]
    #[default]
    Hook,
}

/// Permission kind discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionRequestExtensionManagementKind {
    #[serde(rename = "extension-management")]
    #[default]
    ExtensionManagement,
}

/// Permission kind discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionRequestExtensionPermissionAccessKind {
    #[serde(rename = "extension-permission-access")]
    #[default]
    ExtensionPermissionAccess,
}

/// Details of the permission being requested
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum PermissionRequest {
    Shell(PermissionRequestShell),
    Write(PermissionRequestWrite),
    Read(PermissionRequestRead),
    Mcp(PermissionRequestMcp),
    Url(PermissionRequestUrl),
    Memory(PermissionRequestMemory),
    CustomTool(PermissionRequestCustomTool),
    Hook(PermissionRequestHook),
    ExtensionManagement(PermissionRequestExtensionManagement),
    ExtensionPermissionAccess(PermissionRequestExtensionPermissionAccess),
}

/// Prompt kind discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionPromptRequestCommandsKind {
    #[serde(rename = "commands")]
    #[default]
    Commands,
}

/// Prompt kind discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionPromptRequestWriteKind {
    #[serde(rename = "write")]
    #[default]
    Write,
}

/// Prompt kind discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionPromptRequestReadKind {
    #[serde(rename = "read")]
    #[default]
    Read,
}

/// Prompt kind discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionPromptRequestMcpKind {
    #[serde(rename = "mcp")]
    #[default]
    Mcp,
}

/// Prompt kind discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionPromptRequestUrlKind {
    #[serde(rename = "url")]
    #[default]
    Url,
}

/// Prompt kind discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionPromptRequestMemoryKind {
    #[serde(rename = "memory")]
    #[default]
    Memory,
}

/// Prompt kind discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionPromptRequestCustomToolKind {
    #[serde(rename = "custom-tool")]
    #[default]
    CustomTool,
}

/// Underlying permission kind that needs path approval
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionPromptRequestPathAccessKind {
    /// Read access to a filesystem path.
    #[serde(rename = "read")]
    Read,
    /// Shell command access involving a filesystem path.
    #[serde(rename = "shell")]
    Shell,
    /// Write access to a filesystem path.
    #[serde(rename = "write")]
    Write,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Prompt kind discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionPromptRequestPathKind {
    #[serde(rename = "path")]
    #[default]
    Path,
}

/// Prompt kind discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionPromptRequestHookKind {
    #[serde(rename = "hook")]
    #[default]
    Hook,
}

/// Prompt kind discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionPromptRequestExtensionManagementKind {
    #[serde(rename = "extension-management")]
    #[default]
    ExtensionManagement,
}

/// Prompt kind discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionPromptRequestExtensionPermissionAccessKind {
    #[serde(rename = "extension-permission-access")]
    #[default]
    ExtensionPermissionAccess,
}

/// Derived user-facing permission prompt details for UI consumers
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum PermissionPromptRequest {
    Commands(PermissionPromptRequestCommands),
    Write(PermissionPromptRequestWrite),
    Read(PermissionPromptRequestRead),
    Mcp(PermissionPromptRequestMcp),
    Url(PermissionPromptRequestUrl),
    Memory(PermissionPromptRequestMemory),
    CustomTool(PermissionPromptRequestCustomTool),
    Path(PermissionPromptRequestPath),
    Hook(PermissionPromptRequestHook),
    ExtensionManagement(PermissionPromptRequestExtensionManagement),
    ExtensionPermissionAccess(PermissionPromptRequestExtensionPermissionAccess),
}

/// The permission request was approved
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionApprovedKind {
    #[serde(rename = "approved")]
    #[default]
    Approved,
}

/// Command approval kind
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UserToolSessionApprovalCommandsKind {
    #[serde(rename = "commands")]
    #[default]
    Commands,
}

/// Read approval kind
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UserToolSessionApprovalReadKind {
    #[serde(rename = "read")]
    #[default]
    Read,
}

/// Write approval kind
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UserToolSessionApprovalWriteKind {
    #[serde(rename = "write")]
    #[default]
    Write,
}

/// MCP tool approval kind
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UserToolSessionApprovalMcpKind {
    #[serde(rename = "mcp")]
    #[default]
    Mcp,
}

/// Memory approval kind
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UserToolSessionApprovalMemoryKind {
    #[serde(rename = "memory")]
    #[default]
    Memory,
}

/// Custom tool approval kind
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UserToolSessionApprovalCustomToolKind {
    #[serde(rename = "custom-tool")]
    #[default]
    CustomTool,
}

/// Extension management approval kind
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UserToolSessionApprovalExtensionManagementKind {
    #[serde(rename = "extension-management")]
    #[default]
    ExtensionManagement,
}

/// Extension permission access approval kind
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UserToolSessionApprovalExtensionPermissionAccessKind {
    #[serde(rename = "extension-permission-access")]
    #[default]
    ExtensionPermissionAccess,
}

/// The approval to add as a session-scoped rule
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum UserToolSessionApproval {
    Commands(UserToolSessionApprovalCommands),
    Read(UserToolSessionApprovalRead),
    Write(UserToolSessionApprovalWrite),
    Mcp(UserToolSessionApprovalMcp),
    Memory(UserToolSessionApprovalMemory),
    CustomTool(UserToolSessionApprovalCustomTool),
    ExtensionManagement(UserToolSessionApprovalExtensionManagement),
    ExtensionPermissionAccess(UserToolSessionApprovalExtensionPermissionAccess),
}

/// Approved and remembered for the rest of the session
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionApprovedForSessionKind {
    #[serde(rename = "approved-for-session")]
    #[default]
    ApprovedForSession,
}

/// Approved and persisted for this project location
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionApprovedForLocationKind {
    #[serde(rename = "approved-for-location")]
    #[default]
    ApprovedForLocation,
}

/// The permission request was cancelled before a response was used
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionCancelledKind {
    #[serde(rename = "cancelled")]
    #[default]
    Cancelled,
}

/// Denied because approval rules explicitly blocked it
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDeniedByRulesKind {
    #[serde(rename = "denied-by-rules")]
    #[default]
    DeniedByRules,
}

/// Denied because no approval rule matched and user confirmation was unavailable
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDeniedNoApprovalRuleAndCouldNotRequestFromUserKind {
    #[serde(rename = "denied-no-approval-rule-and-could-not-request-from-user")]
    #[default]
    DeniedNoApprovalRuleAndCouldNotRequestFromUser,
}

/// Denied by the user during an interactive prompt
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDeniedInteractivelyByUserKind {
    #[serde(rename = "denied-interactively-by-user")]
    #[default]
    DeniedInteractivelyByUser,
}

/// Denied by the organization's content exclusion policy
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDeniedByContentExclusionPolicyKind {
    #[serde(rename = "denied-by-content-exclusion-policy")]
    #[default]
    DeniedByContentExclusionPolicy,
}

/// Denied by a permission request hook registered by an extension or plugin
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDeniedByPermissionRequestHookKind {
    #[serde(rename = "denied-by-permission-request-hook")]
    #[default]
    DeniedByPermissionRequestHook,
}

/// The result of the permission request
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum PermissionResult {
    Approved(PermissionApproved),
    ApprovedForSession(PermissionApprovedForSession),
    ApprovedForLocation(PermissionApprovedForLocation),
    Cancelled(PermissionCancelled),
    DeniedByRules(PermissionDeniedByRules),
    DeniedNoApprovalRuleAndCouldNotRequestFromUser(
        PermissionDeniedNoApprovalRuleAndCouldNotRequestFromUser,
    ),
    DeniedInteractivelyByUser(PermissionDeniedInteractivelyByUser),
    DeniedByContentExclusionPolicy(PermissionDeniedByContentExclusionPolicy),
    DeniedByPermissionRequestHook(PermissionDeniedByPermissionRequestHook),
}

/// Elicitation mode; "form" for structured input, "url" for browser-based. Defaults to "form" when absent.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ElicitationRequestedMode {
    /// Structured form-based elicitation.
    #[serde(rename = "form")]
    Form,
    /// Browser URL-based elicitation.
    #[serde(rename = "url")]
    Url,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Schema type indicator (always 'object')
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ElicitationRequestedSchemaType {
    #[serde(rename = "object")]
    #[default]
    Object,
}

/// The user action: "accept" (submitted form), "decline" (explicitly refused), or "cancel" (dismissed)
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ElicitationCompletedAction {
    /// The user submitted the requested form.
    #[serde(rename = "accept")]
    Accept,
    /// The user explicitly declined the request.
    #[serde(rename = "decline")]
    Decline,
    /// The user dismissed the request.
    #[serde(rename = "cancel")]
    Cancel,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Optional non-default OAuth grant type. When set to 'client_credentials', the OAuth flow runs headlessly using the client_id + keychain-stored secret (no browser, no callback server).
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpOauthRequiredStaticClientConfigGrantType {
    #[serde(rename = "client_credentials")]
    #[default]
    ClientCredentials,
}

/// The user's auto-mode-switch choice
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AutoModeSwitchResponse {
    /// Switch models for this request.
    #[serde(rename = "yes")]
    Yes,
    /// Switch models now and keep using the replacement automatically.
    #[serde(rename = "yes_always")]
    YesAlways,
    /// Do not switch models.
    #[serde(rename = "no")]
    No,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Exit plan mode action
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExitPlanModeAction {
    /// Exit plan mode without starting implementation.
    #[serde(rename = "exit_only")]
    ExitOnly,
    /// Exit plan mode and continue in interactive mode.
    #[serde(rename = "interactive")]
    Interactive,
    /// Exit plan mode and continue autonomously.
    #[serde(rename = "autopilot")]
    Autopilot,
    /// Exit plan mode and continue with parallel autonomous workers.
    #[serde(rename = "autopilot_fleet")]
    AutopilotFleet,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Source location type (e.g., project, personal-copilot, plugin, builtin)
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillSource {
    /// Skill defined in the current project's skill directories.
    #[serde(rename = "project")]
    Project,
    /// Skill discovered from a parent directory in the current workspace tree.
    #[serde(rename = "inherited")]
    Inherited,
    /// Skill defined in the user's Copilot skill directory.
    #[serde(rename = "personal-copilot")]
    PersonalCopilot,
    /// Skill defined in the user's personal agents skill directory.
    #[serde(rename = "personal-agents")]
    PersonalAgents,
    /// Skill provided by an installed plugin.
    #[serde(rename = "plugin")]
    Plugin,
    /// Skill loaded from a configured custom skill directory.
    #[serde(rename = "custom")]
    Custom,
    /// Skill bundled with the runtime.
    #[serde(rename = "builtin")]
    Builtin,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Configuration source: user, workspace, plugin, or builtin
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpServerSource {
    /// Server configured in the user's global MCP configuration.
    #[serde(rename = "user")]
    User,
    /// Server configured by the current workspace.
    #[serde(rename = "workspace")]
    Workspace,
    /// Server contributed by an installed plugin.
    #[serde(rename = "plugin")]
    Plugin,
    /// Server bundled with the runtime.
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
    /// The server is connected and available.
    #[serde(rename = "connected")]
    Connected,
    /// The server failed to connect or initialize.
    #[serde(rename = "failed")]
    Failed,
    /// The server requires authentication before it can connect.
    #[serde(rename = "needs-auth")]
    NeedsAuth,
    /// The server connection is still being established.
    #[serde(rename = "pending")]
    Pending,
    /// The server is configured but disabled.
    #[serde(rename = "disabled")]
    Disabled,
    /// The server is not configured for this session.
    #[serde(rename = "not_configured")]
    NotConfigured,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discovery source
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExtensionsLoadedExtensionSource {
    /// Extension discovered from the current project.
    #[serde(rename = "project")]
    Project,
    /// Extension discovered from the user's extension directory.
    #[serde(rename = "user")]
    User,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Current status: running, disabled, failed, or starting
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExtensionsLoadedExtensionStatus {
    /// The extension process is running.
    #[serde(rename = "running")]
    Running,
    /// The extension is installed but disabled.
    #[serde(rename = "disabled")]
    Disabled,
    /// The extension failed to start or crashed.
    #[serde(rename = "failed")]
    Failed,
    /// The extension process is starting.
    #[serde(rename = "starting")]
    Starting,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}
