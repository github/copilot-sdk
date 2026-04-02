/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

using System.Diagnostics;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace GitHub.Copilot.SDK;

/// <summary>
/// Provides the base class from which all session events derive.
/// </summary>
[DebuggerDisplay("{DebuggerDisplay,nq}")]
[JsonPolymorphic(
    TypeDiscriminatorPropertyName = "type",
    IgnoreUnrecognizedTypeDiscriminators = true)]
[JsonDerivedType(typeof(AbortEvent), "abort")]
[JsonDerivedType(typeof(AssistantIntentEvent), "assistant.intent")]
[JsonDerivedType(typeof(AssistantMessageEvent), "assistant.message")]
[JsonDerivedType(typeof(AssistantMessageDeltaEvent), "assistant.message_delta")]
[JsonDerivedType(typeof(AssistantReasoningEvent), "assistant.reasoning")]
[JsonDerivedType(typeof(AssistantReasoningDeltaEvent), "assistant.reasoning_delta")]
[JsonDerivedType(typeof(AssistantStreamingDeltaEvent), "assistant.streaming_delta")]
[JsonDerivedType(typeof(AssistantTurnEndEvent), "assistant.turn_end")]
[JsonDerivedType(typeof(AssistantTurnStartEvent), "assistant.turn_start")]
[JsonDerivedType(typeof(AssistantUsageEvent), "assistant.usage")]
[JsonDerivedType(typeof(CapabilitiesChangedEvent), "capabilities.changed")]
[JsonDerivedType(typeof(CommandCompletedEvent), "command.completed")]
[JsonDerivedType(typeof(CommandExecuteEvent), "command.execute")]
[JsonDerivedType(typeof(CommandQueuedEvent), "command.queued")]
[JsonDerivedType(typeof(CommandsChangedEvent), "commands.changed")]
[JsonDerivedType(typeof(ElicitationCompletedEvent), "elicitation.completed")]
[JsonDerivedType(typeof(ElicitationRequestedEvent), "elicitation.requested")]
[JsonDerivedType(typeof(ExitPlanModeCompletedEvent), "exit_plan_mode.completed")]
[JsonDerivedType(typeof(ExitPlanModeRequestedEvent), "exit_plan_mode.requested")]
[JsonDerivedType(typeof(ExternalToolCompletedEvent), "external_tool.completed")]
[JsonDerivedType(typeof(ExternalToolRequestedEvent), "external_tool.requested")]
[JsonDerivedType(typeof(HookEndEvent), "hook.end")]
[JsonDerivedType(typeof(HookStartEvent), "hook.start")]
[JsonDerivedType(typeof(McpOauthCompletedEvent), "mcp.oauth_completed")]
[JsonDerivedType(typeof(McpOauthRequiredEvent), "mcp.oauth_required")]
[JsonDerivedType(typeof(PendingMessagesModifiedEvent), "pending_messages.modified")]
[JsonDerivedType(typeof(PermissionCompletedEvent), "permission.completed")]
[JsonDerivedType(typeof(PermissionRequestedEvent), "permission.requested")]
[JsonDerivedType(typeof(SamplingCompletedEvent), "sampling.completed")]
[JsonDerivedType(typeof(SamplingRequestedEvent), "sampling.requested")]
[JsonDerivedType(typeof(SessionBackgroundTasksChangedEvent), "session.background_tasks_changed")]
[JsonDerivedType(typeof(SessionCompactionCompleteEvent), "session.compaction_complete")]
[JsonDerivedType(typeof(SessionCompactionStartEvent), "session.compaction_start")]
[JsonDerivedType(typeof(SessionContextChangedEvent), "session.context_changed")]
[JsonDerivedType(typeof(SessionCustomAgentsUpdatedEvent), "session.custom_agents_updated")]
[JsonDerivedType(typeof(SessionErrorEvent), "session.error")]
[JsonDerivedType(typeof(SessionExtensionsLoadedEvent), "session.extensions_loaded")]
[JsonDerivedType(typeof(SessionHandoffEvent), "session.handoff")]
[JsonDerivedType(typeof(SessionIdleEvent), "session.idle")]
[JsonDerivedType(typeof(SessionInfoEvent), "session.info")]
[JsonDerivedType(typeof(SessionMcpServerStatusChangedEvent), "session.mcp_server_status_changed")]
[JsonDerivedType(typeof(SessionMcpServersLoadedEvent), "session.mcp_servers_loaded")]
[JsonDerivedType(typeof(SessionModeChangedEvent), "session.mode_changed")]
[JsonDerivedType(typeof(SessionModelChangeEvent), "session.model_change")]
[JsonDerivedType(typeof(SessionPlanChangedEvent), "session.plan_changed")]
[JsonDerivedType(typeof(SessionRemoteSteerableChangedEvent), "session.remote_steerable_changed")]
[JsonDerivedType(typeof(SessionResumeEvent), "session.resume")]
[JsonDerivedType(typeof(SessionShutdownEvent), "session.shutdown")]
[JsonDerivedType(typeof(SessionSkillsLoadedEvent), "session.skills_loaded")]
[JsonDerivedType(typeof(SessionSnapshotRewindEvent), "session.snapshot_rewind")]
[JsonDerivedType(typeof(SessionStartEvent), "session.start")]
[JsonDerivedType(typeof(SessionTaskCompleteEvent), "session.task_complete")]
[JsonDerivedType(typeof(SessionTitleChangedEvent), "session.title_changed")]
[JsonDerivedType(typeof(SessionToolsUpdatedEvent), "session.tools_updated")]
[JsonDerivedType(typeof(SessionTruncationEvent), "session.truncation")]
[JsonDerivedType(typeof(SessionUsageInfoEvent), "session.usage_info")]
[JsonDerivedType(typeof(SessionWarningEvent), "session.warning")]
[JsonDerivedType(typeof(SessionWorkspaceFileChangedEvent), "session.workspace_file_changed")]
[JsonDerivedType(typeof(SkillInvokedEvent), "skill.invoked")]
[JsonDerivedType(typeof(SubagentCompletedEvent), "subagent.completed")]
[JsonDerivedType(typeof(SubagentDeselectedEvent), "subagent.deselected")]
[JsonDerivedType(typeof(SubagentFailedEvent), "subagent.failed")]
[JsonDerivedType(typeof(SubagentSelectedEvent), "subagent.selected")]
[JsonDerivedType(typeof(SubagentStartedEvent), "subagent.started")]
[JsonDerivedType(typeof(SystemMessageEvent), "system.message")]
[JsonDerivedType(typeof(SystemNotificationEvent), "system.notification")]
[JsonDerivedType(typeof(ToolExecutionCompleteEvent), "tool.execution_complete")]
[JsonDerivedType(typeof(ToolExecutionPartialResultEvent), "tool.execution_partial_result")]
[JsonDerivedType(typeof(ToolExecutionProgressEvent), "tool.execution_progress")]
[JsonDerivedType(typeof(ToolExecutionStartEvent), "tool.execution_start")]
[JsonDerivedType(typeof(ToolUserRequestedEvent), "tool.user_requested")]
[JsonDerivedType(typeof(UserInputCompletedEvent), "user_input.completed")]
[JsonDerivedType(typeof(UserInputRequestedEvent), "user_input.requested")]
[JsonDerivedType(typeof(UserMessageEvent), "user.message")]
public partial class SessionEvent
{
    /// <summary>Unique event identifier (UUID v4), generated when the event is emitted.</summary>
    [JsonPropertyName("id")]
    public Guid Id { get; set; }

    /// <summary>ISO 8601 timestamp when the event was created.</summary>
    [JsonPropertyName("timestamp")]
    public DateTimeOffset Timestamp { get; set; }

    /// <summary>ID of the chronologically preceding event in the session, forming a linked chain. Null for the first event.</summary>
    [JsonPropertyName("parentId")]
    public Guid? ParentId { get; set; }

    /// <summary>When true, the event is transient and not persisted to the session event log on disk.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("ephemeral")]
    public bool? Ephemeral { get; set; }

    /// <summary>
    /// The event type discriminator.
    /// </summary>
    [JsonIgnore]
    public virtual string Type => "unknown";

    /// <summary>Deserializes a JSON string into a <see cref="SessionEvent"/>.</summary>
    public static SessionEvent FromJson(string json) =>
        JsonSerializer.Deserialize(json, SessionEventsJsonContext.Default.SessionEvent)!;

    /// <summary>Serializes this event to a JSON string.</summary>
    public string ToJson() =>
        JsonSerializer.Serialize(this, SessionEventsJsonContext.Default.SessionEvent);

    [DebuggerBrowsable(DebuggerBrowsableState.Never)]
    private string DebuggerDisplay => ToJson();
}

/// <summary>Session initialization metadata including context and configuration.</summary>
/// <remarks>Represents the <c>session.start</c> event.</remarks>
public partial class SessionStartEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.start";

    /// <summary>The <c>session.start</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionStartData Data { get; set; }
}

/// <summary>Session resume metadata including current context and event count.</summary>
/// <remarks>Represents the <c>session.resume</c> event.</remarks>
public partial class SessionResumeEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.resume";

    /// <summary>The <c>session.resume</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionResumeData Data { get; set; }
}

/// <summary>Notifies Mission Control that the session's remote steering capability has changed.</summary>
/// <remarks>Represents the <c>session.remote_steerable_changed</c> event.</remarks>
public partial class SessionRemoteSteerableChangedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.remote_steerable_changed";

    /// <summary>The <c>session.remote_steerable_changed</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionRemoteSteerableChangedData Data { get; set; }
}

/// <summary>Error details for timeline display including message and optional diagnostic information.</summary>
/// <remarks>Represents the <c>session.error</c> event.</remarks>
public partial class SessionErrorEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.error";

    /// <summary>The <c>session.error</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionErrorData Data { get; set; }
}

/// <summary>Payload indicating the agent is idle; includes any background tasks still in flight.</summary>
/// <remarks>Represents the <c>session.idle</c> event.</remarks>
public partial class SessionIdleEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.idle";

    /// <summary>The <c>session.idle</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionIdleData Data { get; set; }
}

/// <summary>Session title change payload containing the new display title.</summary>
/// <remarks>Represents the <c>session.title_changed</c> event.</remarks>
public partial class SessionTitleChangedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.title_changed";

    /// <summary>The <c>session.title_changed</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionTitleChangedData Data { get; set; }
}

/// <summary>Informational message for timeline display with categorization.</summary>
/// <remarks>Represents the <c>session.info</c> event.</remarks>
public partial class SessionInfoEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.info";

    /// <summary>The <c>session.info</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionInfoData Data { get; set; }
}

/// <summary>Warning message for timeline display with categorization.</summary>
/// <remarks>Represents the <c>session.warning</c> event.</remarks>
public partial class SessionWarningEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.warning";

    /// <summary>The <c>session.warning</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionWarningData Data { get; set; }
}

/// <summary>Model change details including previous and new model identifiers.</summary>
/// <remarks>Represents the <c>session.model_change</c> event.</remarks>
public partial class SessionModelChangeEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.model_change";

    /// <summary>The <c>session.model_change</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionModelChangeData Data { get; set; }
}

/// <summary>Agent mode change details including previous and new modes.</summary>
/// <remarks>Represents the <c>session.mode_changed</c> event.</remarks>
public partial class SessionModeChangedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.mode_changed";

    /// <summary>The <c>session.mode_changed</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionModeChangedData Data { get; set; }
}

/// <summary>Plan file operation details indicating what changed.</summary>
/// <remarks>Represents the <c>session.plan_changed</c> event.</remarks>
public partial class SessionPlanChangedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.plan_changed";

    /// <summary>The <c>session.plan_changed</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionPlanChangedData Data { get; set; }
}

/// <summary>Workspace file change details including path and operation type.</summary>
/// <remarks>Represents the <c>session.workspace_file_changed</c> event.</remarks>
public partial class SessionWorkspaceFileChangedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.workspace_file_changed";

    /// <summary>The <c>session.workspace_file_changed</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionWorkspaceFileChangedData Data { get; set; }
}

/// <summary>Session handoff metadata including source, context, and repository information.</summary>
/// <remarks>Represents the <c>session.handoff</c> event.</remarks>
public partial class SessionHandoffEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.handoff";

    /// <summary>The <c>session.handoff</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionHandoffData Data { get; set; }
}

/// <summary>Conversation truncation statistics including token counts and removed content metrics.</summary>
/// <remarks>Represents the <c>session.truncation</c> event.</remarks>
public partial class SessionTruncationEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.truncation";

    /// <summary>The <c>session.truncation</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionTruncationData Data { get; set; }
}

/// <summary>Session rewind details including target event and count of removed events.</summary>
/// <remarks>Represents the <c>session.snapshot_rewind</c> event.</remarks>
public partial class SessionSnapshotRewindEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.snapshot_rewind";

    /// <summary>The <c>session.snapshot_rewind</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionSnapshotRewindData Data { get; set; }
}

/// <summary>Session termination metrics including usage statistics, code changes, and shutdown reason.</summary>
/// <remarks>Represents the <c>session.shutdown</c> event.</remarks>
public partial class SessionShutdownEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.shutdown";

    /// <summary>The <c>session.shutdown</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionShutdownData Data { get; set; }
}

/// <summary>Updated working directory and git context after the change.</summary>
/// <remarks>Represents the <c>session.context_changed</c> event.</remarks>
public partial class SessionContextChangedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.context_changed";

    /// <summary>The <c>session.context_changed</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionContextChangedData Data { get; set; }
}

/// <summary>Current context window usage statistics including token and message counts.</summary>
/// <remarks>Represents the <c>session.usage_info</c> event.</remarks>
public partial class SessionUsageInfoEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.usage_info";

    /// <summary>The <c>session.usage_info</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionUsageInfoData Data { get; set; }
}

/// <summary>Context window breakdown at the start of LLM-powered conversation compaction.</summary>
/// <remarks>Represents the <c>session.compaction_start</c> event.</remarks>
public partial class SessionCompactionStartEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.compaction_start";

    /// <summary>The <c>session.compaction_start</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionCompactionStartData Data { get; set; }
}

/// <summary>Conversation compaction results including success status, metrics, and optional error details.</summary>
/// <remarks>Represents the <c>session.compaction_complete</c> event.</remarks>
public partial class SessionCompactionCompleteEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.compaction_complete";

    /// <summary>The <c>session.compaction_complete</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionCompactionCompleteData Data { get; set; }
}

/// <summary>Task completion notification with summary from the agent.</summary>
/// <remarks>Represents the <c>session.task_complete</c> event.</remarks>
public partial class SessionTaskCompleteEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.task_complete";

    /// <summary>The <c>session.task_complete</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionTaskCompleteData Data { get; set; }
}

/// <summary>Represents the <c>user.message</c> event.</summary>
public partial class UserMessageEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "user.message";

    /// <summary>The <c>user.message</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required UserMessageData Data { get; set; }
}

/// <summary>Empty payload; the event signals that the pending message queue has changed.</summary>
/// <remarks>Represents the <c>pending_messages.modified</c> event.</remarks>
public partial class PendingMessagesModifiedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "pending_messages.modified";

    /// <summary>The <c>pending_messages.modified</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required PendingMessagesModifiedData Data { get; set; }
}

/// <summary>Turn initialization metadata including identifier and interaction tracking.</summary>
/// <remarks>Represents the <c>assistant.turn_start</c> event.</remarks>
public partial class AssistantTurnStartEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "assistant.turn_start";

    /// <summary>The <c>assistant.turn_start</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required AssistantTurnStartData Data { get; set; }
}

/// <summary>Agent intent description for current activity or plan.</summary>
/// <remarks>Represents the <c>assistant.intent</c> event.</remarks>
public partial class AssistantIntentEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "assistant.intent";

    /// <summary>The <c>assistant.intent</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required AssistantIntentData Data { get; set; }
}

/// <summary>Assistant reasoning content for timeline display with complete thinking text.</summary>
/// <remarks>Represents the <c>assistant.reasoning</c> event.</remarks>
public partial class AssistantReasoningEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "assistant.reasoning";

    /// <summary>The <c>assistant.reasoning</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required AssistantReasoningData Data { get; set; }
}

/// <summary>Streaming reasoning delta for incremental extended thinking updates.</summary>
/// <remarks>Represents the <c>assistant.reasoning_delta</c> event.</remarks>
public partial class AssistantReasoningDeltaEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "assistant.reasoning_delta";

    /// <summary>The <c>assistant.reasoning_delta</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required AssistantReasoningDeltaData Data { get; set; }
}

/// <summary>Streaming response progress with cumulative byte count.</summary>
/// <remarks>Represents the <c>assistant.streaming_delta</c> event.</remarks>
public partial class AssistantStreamingDeltaEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "assistant.streaming_delta";

    /// <summary>The <c>assistant.streaming_delta</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required AssistantStreamingDeltaData Data { get; set; }
}

/// <summary>Assistant response containing text content, optional tool requests, and interaction metadata.</summary>
/// <remarks>Represents the <c>assistant.message</c> event.</remarks>
public partial class AssistantMessageEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "assistant.message";

    /// <summary>The <c>assistant.message</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required AssistantMessageData Data { get; set; }
}

/// <summary>Streaming assistant message delta for incremental response updates.</summary>
/// <remarks>Represents the <c>assistant.message_delta</c> event.</remarks>
public partial class AssistantMessageDeltaEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "assistant.message_delta";

    /// <summary>The <c>assistant.message_delta</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required AssistantMessageDeltaData Data { get; set; }
}

/// <summary>Turn completion metadata including the turn identifier.</summary>
/// <remarks>Represents the <c>assistant.turn_end</c> event.</remarks>
public partial class AssistantTurnEndEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "assistant.turn_end";

    /// <summary>The <c>assistant.turn_end</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required AssistantTurnEndData Data { get; set; }
}

/// <summary>LLM API call usage metrics including tokens, costs, quotas, and billing information.</summary>
/// <remarks>Represents the <c>assistant.usage</c> event.</remarks>
public partial class AssistantUsageEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "assistant.usage";

    /// <summary>The <c>assistant.usage</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required AssistantUsageData Data { get; set; }
}

/// <summary>Turn abort information including the reason for termination.</summary>
/// <remarks>Represents the <c>abort</c> event.</remarks>
public partial class AbortEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "abort";

    /// <summary>The <c>abort</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required AbortData Data { get; set; }
}

/// <summary>User-initiated tool invocation request with tool name and arguments.</summary>
/// <remarks>Represents the <c>tool.user_requested</c> event.</remarks>
public partial class ToolUserRequestedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "tool.user_requested";

    /// <summary>The <c>tool.user_requested</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required ToolUserRequestedData Data { get; set; }
}

/// <summary>Tool execution startup details including MCP server information when applicable.</summary>
/// <remarks>Represents the <c>tool.execution_start</c> event.</remarks>
public partial class ToolExecutionStartEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "tool.execution_start";

    /// <summary>The <c>tool.execution_start</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required ToolExecutionStartData Data { get; set; }
}

/// <summary>Streaming tool execution output for incremental result display.</summary>
/// <remarks>Represents the <c>tool.execution_partial_result</c> event.</remarks>
public partial class ToolExecutionPartialResultEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "tool.execution_partial_result";

    /// <summary>The <c>tool.execution_partial_result</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required ToolExecutionPartialResultData Data { get; set; }
}

/// <summary>Tool execution progress notification with status message.</summary>
/// <remarks>Represents the <c>tool.execution_progress</c> event.</remarks>
public partial class ToolExecutionProgressEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "tool.execution_progress";

    /// <summary>The <c>tool.execution_progress</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required ToolExecutionProgressData Data { get; set; }
}

/// <summary>Tool execution completion results including success status, detailed output, and error information.</summary>
/// <remarks>Represents the <c>tool.execution_complete</c> event.</remarks>
public partial class ToolExecutionCompleteEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "tool.execution_complete";

    /// <summary>The <c>tool.execution_complete</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required ToolExecutionCompleteData Data { get; set; }
}

/// <summary>Skill invocation details including content, allowed tools, and plugin metadata.</summary>
/// <remarks>Represents the <c>skill.invoked</c> event.</remarks>
public partial class SkillInvokedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "skill.invoked";

    /// <summary>The <c>skill.invoked</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SkillInvokedData Data { get; set; }
}

/// <summary>Sub-agent startup details including parent tool call and agent information.</summary>
/// <remarks>Represents the <c>subagent.started</c> event.</remarks>
public partial class SubagentStartedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "subagent.started";

    /// <summary>The <c>subagent.started</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SubagentStartedData Data { get; set; }
}

/// <summary>Sub-agent completion details for successful execution.</summary>
/// <remarks>Represents the <c>subagent.completed</c> event.</remarks>
public partial class SubagentCompletedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "subagent.completed";

    /// <summary>The <c>subagent.completed</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SubagentCompletedData Data { get; set; }
}

/// <summary>Sub-agent failure details including error message and agent information.</summary>
/// <remarks>Represents the <c>subagent.failed</c> event.</remarks>
public partial class SubagentFailedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "subagent.failed";

    /// <summary>The <c>subagent.failed</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SubagentFailedData Data { get; set; }
}

/// <summary>Custom agent selection details including name and available tools.</summary>
/// <remarks>Represents the <c>subagent.selected</c> event.</remarks>
public partial class SubagentSelectedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "subagent.selected";

    /// <summary>The <c>subagent.selected</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SubagentSelectedData Data { get; set; }
}

/// <summary>Empty payload; the event signals that the custom agent was deselected, returning to the default agent.</summary>
/// <remarks>Represents the <c>subagent.deselected</c> event.</remarks>
public partial class SubagentDeselectedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "subagent.deselected";

    /// <summary>The <c>subagent.deselected</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SubagentDeselectedData Data { get; set; }
}

/// <summary>Hook invocation start details including type and input data.</summary>
/// <remarks>Represents the <c>hook.start</c> event.</remarks>
public partial class HookStartEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "hook.start";

    /// <summary>The <c>hook.start</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required HookStartData Data { get; set; }
}

/// <summary>Hook invocation completion details including output, success status, and error information.</summary>
/// <remarks>Represents the <c>hook.end</c> event.</remarks>
public partial class HookEndEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "hook.end";

    /// <summary>The <c>hook.end</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required HookEndData Data { get; set; }
}

/// <summary>System or developer message content with role and optional template metadata.</summary>
/// <remarks>Represents the <c>system.message</c> event.</remarks>
public partial class SystemMessageEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "system.message";

    /// <summary>The <c>system.message</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SystemMessageData Data { get; set; }
}

/// <summary>System-generated notification for runtime events like background task completion.</summary>
/// <remarks>Represents the <c>system.notification</c> event.</remarks>
public partial class SystemNotificationEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "system.notification";

    /// <summary>The <c>system.notification</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SystemNotificationData Data { get; set; }
}

/// <summary>Permission request notification requiring client approval with request details.</summary>
/// <remarks>Represents the <c>permission.requested</c> event.</remarks>
public partial class PermissionRequestedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "permission.requested";

    /// <summary>The <c>permission.requested</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required PermissionRequestedData Data { get; set; }
}

/// <summary>Permission request completion notification signaling UI dismissal.</summary>
/// <remarks>Represents the <c>permission.completed</c> event.</remarks>
public partial class PermissionCompletedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "permission.completed";

    /// <summary>The <c>permission.completed</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required PermissionCompletedData Data { get; set; }
}

/// <summary>User input request notification with question and optional predefined choices.</summary>
/// <remarks>Represents the <c>user_input.requested</c> event.</remarks>
public partial class UserInputRequestedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "user_input.requested";

    /// <summary>The <c>user_input.requested</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required UserInputRequestedData Data { get; set; }
}

/// <summary>User input request completion notification signaling UI dismissal.</summary>
/// <remarks>Represents the <c>user_input.completed</c> event.</remarks>
public partial class UserInputCompletedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "user_input.completed";

    /// <summary>The <c>user_input.completed</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required UserInputCompletedData Data { get; set; }
}

/// <summary>Elicitation request; may be form-based (structured input) or URL-based (browser redirect).</summary>
/// <remarks>Represents the <c>elicitation.requested</c> event.</remarks>
public partial class ElicitationRequestedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "elicitation.requested";

    /// <summary>The <c>elicitation.requested</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required ElicitationRequestedData Data { get; set; }
}

/// <summary>Elicitation request completion notification signaling UI dismissal.</summary>
/// <remarks>Represents the <c>elicitation.completed</c> event.</remarks>
public partial class ElicitationCompletedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "elicitation.completed";

    /// <summary>The <c>elicitation.completed</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required ElicitationCompletedData Data { get; set; }
}

/// <summary>Sampling request from an MCP server; contains the server name and a requestId for correlation.</summary>
/// <remarks>Represents the <c>sampling.requested</c> event.</remarks>
public partial class SamplingRequestedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "sampling.requested";

    /// <summary>The <c>sampling.requested</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SamplingRequestedData Data { get; set; }
}

/// <summary>Sampling request completion notification signaling UI dismissal.</summary>
/// <remarks>Represents the <c>sampling.completed</c> event.</remarks>
public partial class SamplingCompletedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "sampling.completed";

    /// <summary>The <c>sampling.completed</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SamplingCompletedData Data { get; set; }
}

/// <summary>OAuth authentication request for an MCP server.</summary>
/// <remarks>Represents the <c>mcp.oauth_required</c> event.</remarks>
public partial class McpOauthRequiredEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "mcp.oauth_required";

    /// <summary>The <c>mcp.oauth_required</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required McpOauthRequiredData Data { get; set; }
}

/// <summary>MCP OAuth request completion notification.</summary>
/// <remarks>Represents the <c>mcp.oauth_completed</c> event.</remarks>
public partial class McpOauthCompletedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "mcp.oauth_completed";

    /// <summary>The <c>mcp.oauth_completed</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required McpOauthCompletedData Data { get; set; }
}

/// <summary>External tool invocation request for client-side tool execution.</summary>
/// <remarks>Represents the <c>external_tool.requested</c> event.</remarks>
public partial class ExternalToolRequestedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "external_tool.requested";

    /// <summary>The <c>external_tool.requested</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required ExternalToolRequestedData Data { get; set; }
}

/// <summary>External tool completion notification signaling UI dismissal.</summary>
/// <remarks>Represents the <c>external_tool.completed</c> event.</remarks>
public partial class ExternalToolCompletedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "external_tool.completed";

    /// <summary>The <c>external_tool.completed</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required ExternalToolCompletedData Data { get; set; }
}

/// <summary>Queued slash command dispatch request for client execution.</summary>
/// <remarks>Represents the <c>command.queued</c> event.</remarks>
public partial class CommandQueuedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "command.queued";

    /// <summary>The <c>command.queued</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required CommandQueuedData Data { get; set; }
}

/// <summary>Registered command dispatch request routed to the owning client.</summary>
/// <remarks>Represents the <c>command.execute</c> event.</remarks>
public partial class CommandExecuteEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "command.execute";

    /// <summary>The <c>command.execute</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required CommandExecuteData Data { get; set; }
}

/// <summary>Queued command completion notification signaling UI dismissal.</summary>
/// <remarks>Represents the <c>command.completed</c> event.</remarks>
public partial class CommandCompletedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "command.completed";

    /// <summary>The <c>command.completed</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required CommandCompletedData Data { get; set; }
}

/// <summary>SDK command registration change notification.</summary>
/// <remarks>Represents the <c>commands.changed</c> event.</remarks>
public partial class CommandsChangedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "commands.changed";

    /// <summary>The <c>commands.changed</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required CommandsChangedData Data { get; set; }
}

/// <summary>Session capability change notification.</summary>
/// <remarks>Represents the <c>capabilities.changed</c> event.</remarks>
public partial class CapabilitiesChangedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "capabilities.changed";

    /// <summary>The <c>capabilities.changed</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required CapabilitiesChangedData Data { get; set; }
}

/// <summary>Plan approval request with plan content and available user actions.</summary>
/// <remarks>Represents the <c>exit_plan_mode.requested</c> event.</remarks>
public partial class ExitPlanModeRequestedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "exit_plan_mode.requested";

    /// <summary>The <c>exit_plan_mode.requested</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required ExitPlanModeRequestedData Data { get; set; }
}

/// <summary>Plan mode exit completion notification signaling UI dismissal.</summary>
/// <remarks>Represents the <c>exit_plan_mode.completed</c> event.</remarks>
public partial class ExitPlanModeCompletedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "exit_plan_mode.completed";

    /// <summary>The <c>exit_plan_mode.completed</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required ExitPlanModeCompletedData Data { get; set; }
}

/// <summary>Represents the <c>session.tools_updated</c> event.</summary>
public partial class SessionToolsUpdatedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.tools_updated";

    /// <summary>The <c>session.tools_updated</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionToolsUpdatedData Data { get; set; }
}

/// <summary>Represents the <c>session.background_tasks_changed</c> event.</summary>
public partial class SessionBackgroundTasksChangedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.background_tasks_changed";

    /// <summary>The <c>session.background_tasks_changed</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionBackgroundTasksChangedData Data { get; set; }
}

/// <summary>Represents the <c>session.skills_loaded</c> event.</summary>
public partial class SessionSkillsLoadedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.skills_loaded";

    /// <summary>The <c>session.skills_loaded</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionSkillsLoadedData Data { get; set; }
}

/// <summary>Represents the <c>session.custom_agents_updated</c> event.</summary>
public partial class SessionCustomAgentsUpdatedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.custom_agents_updated";

    /// <summary>The <c>session.custom_agents_updated</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionCustomAgentsUpdatedData Data { get; set; }
}

/// <summary>Represents the <c>session.mcp_servers_loaded</c> event.</summary>
public partial class SessionMcpServersLoadedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.mcp_servers_loaded";

    /// <summary>The <c>session.mcp_servers_loaded</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionMcpServersLoadedData Data { get; set; }
}

/// <summary>Represents the <c>session.mcp_server_status_changed</c> event.</summary>
public partial class SessionMcpServerStatusChangedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.mcp_server_status_changed";

    /// <summary>The <c>session.mcp_server_status_changed</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionMcpServerStatusChangedData Data { get; set; }
}

/// <summary>Represents the <c>session.extensions_loaded</c> event.</summary>
public partial class SessionExtensionsLoadedEvent : SessionEvent
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "session.extensions_loaded";

    /// <summary>The <c>session.extensions_loaded</c> event payload.</summary>
    [JsonPropertyName("data")]
    public required SessionExtensionsLoadedData Data { get; set; }
}

/// <summary>Session initialization metadata including context and configuration.</summary>
public partial class SessionStartData
{
    /// <summary>Unique identifier for the session.</summary>
    [JsonPropertyName("sessionId")]
    public required string SessionId { get; set; }

    /// <summary>Schema version number for the session event format.</summary>
    [JsonPropertyName("version")]
    public required double Version { get; set; }

    /// <summary>Identifier of the software producing the events (e.g., "copilot-agent").</summary>
    [JsonPropertyName("producer")]
    public required string Producer { get; set; }

    /// <summary>Version string of the Copilot application.</summary>
    [JsonPropertyName("copilotVersion")]
    public required string CopilotVersion { get; set; }

    /// <summary>ISO 8601 timestamp when the session was created.</summary>
    [JsonPropertyName("startTime")]
    public required DateTimeOffset StartTime { get; set; }

    /// <summary>Model selected at session creation time, if any.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("selectedModel")]
    public string? SelectedModel { get; set; }

    /// <summary>Reasoning effort level used for model calls, if applicable (e.g. "low", "medium", "high", "xhigh").</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("reasoningEffort")]
    public string? ReasoningEffort { get; set; }

    /// <summary>Working directory and git context at session start.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("context")]
    public SessionStartDataContext? Context { get; set; }

    /// <summary>Whether the session was already in use by another client at start time.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("alreadyInUse")]
    public bool? AlreadyInUse { get; set; }

    /// <summary>Whether this session supports remote steering via Mission Control.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("remoteSteerable")]
    public bool? RemoteSteerable { get; set; }
}

/// <summary>Session resume metadata including current context and event count.</summary>
public partial class SessionResumeData
{
    /// <summary>ISO 8601 timestamp when the session was resumed.</summary>
    [JsonPropertyName("resumeTime")]
    public required DateTimeOffset ResumeTime { get; set; }

    /// <summary>Total number of persisted events in the session at the time of resume.</summary>
    [JsonPropertyName("eventCount")]
    public required double EventCount { get; set; }

    /// <summary>Model currently selected at resume time.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("selectedModel")]
    public string? SelectedModel { get; set; }

    /// <summary>Reasoning effort level used for model calls, if applicable (e.g. "low", "medium", "high", "xhigh").</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("reasoningEffort")]
    public string? ReasoningEffort { get; set; }

    /// <summary>Updated working directory and git context at resume time.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("context")]
    public SessionResumeDataContext? Context { get; set; }

    /// <summary>Whether the session was already in use by another client at resume time.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("alreadyInUse")]
    public bool? AlreadyInUse { get; set; }

    /// <summary>Whether this session supports remote steering via Mission Control.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("remoteSteerable")]
    public bool? RemoteSteerable { get; set; }
}

/// <summary>Notifies Mission Control that the session's remote steering capability has changed.</summary>
public partial class SessionRemoteSteerableChangedData
{
    /// <summary>Whether this session now supports remote steering via Mission Control.</summary>
    [JsonPropertyName("remoteSteerable")]
    public required bool RemoteSteerable { get; set; }
}

/// <summary>Error details for timeline display including message and optional diagnostic information.</summary>
public partial class SessionErrorData
{
    /// <summary>Category of error (e.g., "authentication", "authorization", "quota", "rate_limit", "query").</summary>
    [JsonPropertyName("errorType")]
    public required string ErrorType { get; set; }

    /// <summary>Human-readable error message.</summary>
    [JsonPropertyName("message")]
    public required string Message { get; set; }

    /// <summary>Error stack trace, when available.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("stack")]
    public string? Stack { get; set; }

    /// <summary>HTTP status code from the upstream request, if applicable.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("statusCode")]
    public double? StatusCode { get; set; }

    /// <summary>GitHub request tracing ID (x-github-request-id header) for correlating with server-side logs.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("providerCallId")]
    public string? ProviderCallId { get; set; }

    /// <summary>Optional URL associated with this error that the user can open in a browser.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("url")]
    public string? Url { get; set; }
}

/// <summary>Payload indicating the agent is idle; includes any background tasks still in flight.</summary>
public partial class SessionIdleData
{
    /// <summary>Background tasks still running when the agent became idle.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("backgroundTasks")]
    public SessionIdleDataBackgroundTasks? BackgroundTasks { get; set; }

    /// <summary>True when the preceding agentic loop was cancelled via abort signal.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("aborted")]
    public bool? Aborted { get; set; }
}

/// <summary>Session title change payload containing the new display title.</summary>
public partial class SessionTitleChangedData
{
    /// <summary>The new display title for the session.</summary>
    [JsonPropertyName("title")]
    public required string Title { get; set; }
}

/// <summary>Informational message for timeline display with categorization.</summary>
public partial class SessionInfoData
{
    /// <summary>Category of informational message (e.g., "notification", "timing", "context_window", "mcp", "snapshot", "configuration", "authentication", "model").</summary>
    [JsonPropertyName("infoType")]
    public required string InfoType { get; set; }

    /// <summary>Human-readable informational message for display in the timeline.</summary>
    [JsonPropertyName("message")]
    public required string Message { get; set; }

    /// <summary>Optional URL associated with this message that the user can open in a browser.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("url")]
    public string? Url { get; set; }
}

/// <summary>Warning message for timeline display with categorization.</summary>
public partial class SessionWarningData
{
    /// <summary>Category of warning (e.g., "subscription", "policy", "mcp").</summary>
    [JsonPropertyName("warningType")]
    public required string WarningType { get; set; }

    /// <summary>Human-readable warning message for display in the timeline.</summary>
    [JsonPropertyName("message")]
    public required string Message { get; set; }

    /// <summary>Optional URL associated with this warning that the user can open in a browser.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("url")]
    public string? Url { get; set; }
}

/// <summary>Model change details including previous and new model identifiers.</summary>
public partial class SessionModelChangeData
{
    /// <summary>Model that was previously selected, if any.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("previousModel")]
    public string? PreviousModel { get; set; }

    /// <summary>Newly selected model identifier.</summary>
    [JsonPropertyName("newModel")]
    public required string NewModel { get; set; }

    /// <summary>Reasoning effort level before the model change, if applicable.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("previousReasoningEffort")]
    public string? PreviousReasoningEffort { get; set; }

    /// <summary>Reasoning effort level after the model change, if applicable.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("reasoningEffort")]
    public string? ReasoningEffort { get; set; }
}

/// <summary>Agent mode change details including previous and new modes.</summary>
public partial class SessionModeChangedData
{
    /// <summary>Agent mode before the change (e.g., "interactive", "plan", "autopilot").</summary>
    [JsonPropertyName("previousMode")]
    public required string PreviousMode { get; set; }

    /// <summary>Agent mode after the change (e.g., "interactive", "plan", "autopilot").</summary>
    [JsonPropertyName("newMode")]
    public required string NewMode { get; set; }
}

/// <summary>Plan file operation details indicating what changed.</summary>
public partial class SessionPlanChangedData
{
    /// <summary>The type of operation performed on the plan file.</summary>
    [JsonPropertyName("operation")]
    public required SessionPlanChangedDataOperation Operation { get; set; }
}

/// <summary>Workspace file change details including path and operation type.</summary>
public partial class SessionWorkspaceFileChangedData
{
    /// <summary>Relative path within the session workspace files directory.</summary>
    [JsonPropertyName("path")]
    public required string Path { get; set; }

    /// <summary>Whether the file was newly created or updated.</summary>
    [JsonPropertyName("operation")]
    public required SessionWorkspaceFileChangedDataOperation Operation { get; set; }
}

/// <summary>Session handoff metadata including source, context, and repository information.</summary>
public partial class SessionHandoffData
{
    /// <summary>ISO 8601 timestamp when the handoff occurred.</summary>
    [JsonPropertyName("handoffTime")]
    public required DateTimeOffset HandoffTime { get; set; }

    /// <summary>Origin type of the session being handed off.</summary>
    [JsonPropertyName("sourceType")]
    public required SessionHandoffDataSourceType SourceType { get; set; }

    /// <summary>Repository context for the handed-off session.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("repository")]
    public SessionHandoffDataRepository? Repository { get; set; }

    /// <summary>Additional context information for the handoff.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("context")]
    public string? Context { get; set; }

    /// <summary>Summary of the work done in the source session.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("summary")]
    public string? Summary { get; set; }

    /// <summary>Session ID of the remote session being handed off.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("remoteSessionId")]
    public string? RemoteSessionId { get; set; }

    /// <summary>GitHub host URL for the source session (e.g., https://github.com or https://tenant.ghe.com).</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("host")]
    public string? Host { get; set; }
}

/// <summary>Conversation truncation statistics including token counts and removed content metrics.</summary>
public partial class SessionTruncationData
{
    /// <summary>Maximum token count for the model's context window.</summary>
    [JsonPropertyName("tokenLimit")]
    public required double TokenLimit { get; set; }

    /// <summary>Total tokens in conversation messages before truncation.</summary>
    [JsonPropertyName("preTruncationTokensInMessages")]
    public required double PreTruncationTokensInMessages { get; set; }

    /// <summary>Number of conversation messages before truncation.</summary>
    [JsonPropertyName("preTruncationMessagesLength")]
    public required double PreTruncationMessagesLength { get; set; }

    /// <summary>Total tokens in conversation messages after truncation.</summary>
    [JsonPropertyName("postTruncationTokensInMessages")]
    public required double PostTruncationTokensInMessages { get; set; }

    /// <summary>Number of conversation messages after truncation.</summary>
    [JsonPropertyName("postTruncationMessagesLength")]
    public required double PostTruncationMessagesLength { get; set; }

    /// <summary>Number of tokens removed by truncation.</summary>
    [JsonPropertyName("tokensRemovedDuringTruncation")]
    public required double TokensRemovedDuringTruncation { get; set; }

    /// <summary>Number of messages removed by truncation.</summary>
    [JsonPropertyName("messagesRemovedDuringTruncation")]
    public required double MessagesRemovedDuringTruncation { get; set; }

    /// <summary>Identifier of the component that performed truncation (e.g., "BasicTruncator").</summary>
    [JsonPropertyName("performedBy")]
    public required string PerformedBy { get; set; }
}

/// <summary>Session rewind details including target event and count of removed events.</summary>
public partial class SessionSnapshotRewindData
{
    /// <summary>Event ID that was rewound to; all events after this one were removed.</summary>
    [JsonPropertyName("upToEventId")]
    public required string UpToEventId { get; set; }

    /// <summary>Number of events that were removed by the rewind.</summary>
    [JsonPropertyName("eventsRemoved")]
    public required double EventsRemoved { get; set; }
}

/// <summary>Session termination metrics including usage statistics, code changes, and shutdown reason.</summary>
public partial class SessionShutdownData
{
    /// <summary>Whether the session ended normally ("routine") or due to a crash/fatal error ("error").</summary>
    [JsonPropertyName("shutdownType")]
    public required SessionShutdownDataShutdownType ShutdownType { get; set; }

    /// <summary>Error description when shutdownType is "error".</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("errorReason")]
    public string? ErrorReason { get; set; }

    /// <summary>Total number of premium API requests used during the session.</summary>
    [JsonPropertyName("totalPremiumRequests")]
    public required double TotalPremiumRequests { get; set; }

    /// <summary>Cumulative time spent in API calls during the session, in milliseconds.</summary>
    [JsonPropertyName("totalApiDurationMs")]
    public required double TotalApiDurationMs { get; set; }

    /// <summary>Unix timestamp (milliseconds) when the session started.</summary>
    [JsonPropertyName("sessionStartTime")]
    public required double SessionStartTime { get; set; }

    /// <summary>Aggregate code change metrics for the session.</summary>
    [JsonPropertyName("codeChanges")]
    public required SessionShutdownDataCodeChanges CodeChanges { get; set; }

    /// <summary>Per-model usage breakdown, keyed by model identifier.</summary>
    [JsonPropertyName("modelMetrics")]
    public required Dictionary<string, object> ModelMetrics { get; set; }

    /// <summary>Model that was selected at the time of shutdown.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("currentModel")]
    public string? CurrentModel { get; set; }

    /// <summary>Total tokens in context window at shutdown.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("currentTokens")]
    public double? CurrentTokens { get; set; }

    /// <summary>System message token count at shutdown.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("systemTokens")]
    public double? SystemTokens { get; set; }

    /// <summary>Non-system message token count at shutdown.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("conversationTokens")]
    public double? ConversationTokens { get; set; }

    /// <summary>Tool definitions token count at shutdown.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("toolDefinitionsTokens")]
    public double? ToolDefinitionsTokens { get; set; }
}

/// <summary>Updated working directory and git context after the change.</summary>
public partial class SessionContextChangedData
{
    /// <summary>Current working directory path.</summary>
    [JsonPropertyName("cwd")]
    public required string Cwd { get; set; }

    /// <summary>Root directory of the git repository, resolved via git rev-parse.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("gitRoot")]
    public string? GitRoot { get; set; }

    /// <summary>Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps).</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("repository")]
    public string? Repository { get; set; }

    /// <summary>Hosting platform type of the repository (github or ado).</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("hostType")]
    public SessionStartDataContextHostType? HostType { get; set; }

    /// <summary>Current git branch name.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("branch")]
    public string? Branch { get; set; }

    /// <summary>Head commit of current git branch at session start time.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("headCommit")]
    public string? HeadCommit { get; set; }

    /// <summary>Base commit of current git branch at session start time.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("baseCommit")]
    public string? BaseCommit { get; set; }
}

/// <summary>Current context window usage statistics including token and message counts.</summary>
public partial class SessionUsageInfoData
{
    /// <summary>Maximum token count for the model's context window.</summary>
    [JsonPropertyName("tokenLimit")]
    public required double TokenLimit { get; set; }

    /// <summary>Current number of tokens in the context window.</summary>
    [JsonPropertyName("currentTokens")]
    public required double CurrentTokens { get; set; }

    /// <summary>Current number of messages in the conversation.</summary>
    [JsonPropertyName("messagesLength")]
    public required double MessagesLength { get; set; }

    /// <summary>Token count from system message(s).</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("systemTokens")]
    public double? SystemTokens { get; set; }

    /// <summary>Token count from non-system messages (user, assistant, tool).</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("conversationTokens")]
    public double? ConversationTokens { get; set; }

    /// <summary>Token count from tool definitions.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("toolDefinitionsTokens")]
    public double? ToolDefinitionsTokens { get; set; }

    /// <summary>Whether this is the first usage_info event emitted in this session.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("isInitial")]
    public bool? IsInitial { get; set; }
}

/// <summary>Context window breakdown at the start of LLM-powered conversation compaction.</summary>
public partial class SessionCompactionStartData
{
    /// <summary>Token count from system message(s) at compaction start.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("systemTokens")]
    public double? SystemTokens { get; set; }

    /// <summary>Token count from non-system messages (user, assistant, tool) at compaction start.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("conversationTokens")]
    public double? ConversationTokens { get; set; }

    /// <summary>Token count from tool definitions at compaction start.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("toolDefinitionsTokens")]
    public double? ToolDefinitionsTokens { get; set; }
}

/// <summary>Conversation compaction results including success status, metrics, and optional error details.</summary>
public partial class SessionCompactionCompleteData
{
    /// <summary>Whether compaction completed successfully.</summary>
    [JsonPropertyName("success")]
    public required bool Success { get; set; }

    /// <summary>Error message if compaction failed.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("error")]
    public string? Error { get; set; }

    /// <summary>Total tokens in conversation before compaction.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("preCompactionTokens")]
    public double? PreCompactionTokens { get; set; }

    /// <summary>Total tokens in conversation after compaction.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("postCompactionTokens")]
    public double? PostCompactionTokens { get; set; }

    /// <summary>Number of messages before compaction.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("preCompactionMessagesLength")]
    public double? PreCompactionMessagesLength { get; set; }

    /// <summary>Number of messages removed during compaction.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("messagesRemoved")]
    public double? MessagesRemoved { get; set; }

    /// <summary>Number of tokens removed during compaction.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("tokensRemoved")]
    public double? TokensRemoved { get; set; }

    /// <summary>LLM-generated summary of the compacted conversation history.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("summaryContent")]
    public string? SummaryContent { get; set; }

    /// <summary>Checkpoint snapshot number created for recovery.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("checkpointNumber")]
    public double? CheckpointNumber { get; set; }

    /// <summary>File path where the checkpoint was stored.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("checkpointPath")]
    public string? CheckpointPath { get; set; }

    /// <summary>Token usage breakdown for the compaction LLM call.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("compactionTokensUsed")]
    public SessionCompactionCompleteDataCompactionTokensUsed? CompactionTokensUsed { get; set; }

    /// <summary>GitHub request tracing ID (x-github-request-id header) for the compaction LLM call.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("requestId")]
    public string? RequestId { get; set; }

    /// <summary>Token count from system message(s) after compaction.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("systemTokens")]
    public double? SystemTokens { get; set; }

    /// <summary>Token count from non-system messages (user, assistant, tool) after compaction.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("conversationTokens")]
    public double? ConversationTokens { get; set; }

    /// <summary>Token count from tool definitions after compaction.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("toolDefinitionsTokens")]
    public double? ToolDefinitionsTokens { get; set; }
}

/// <summary>Task completion notification with summary from the agent.</summary>
public partial class SessionTaskCompleteData
{
    /// <summary>Summary of the completed task, provided by the agent.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("summary")]
    public string? Summary { get; set; }

    /// <summary>Whether the tool call succeeded. False when validation failed (e.g., invalid arguments).</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("success")]
    public bool? Success { get; set; }
}

/// <summary>Event payload for <see cref="UserMessageEvent"/>.</summary>
public partial class UserMessageData
{
    /// <summary>The user's message text as displayed in the timeline.</summary>
    [JsonPropertyName("content")]
    public required string Content { get; set; }

    /// <summary>Transformed version of the message sent to the model, with XML wrapping, timestamps, and other augmentations for prompt caching.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("transformedContent")]
    public string? TransformedContent { get; set; }

    /// <summary>Files, selections, or GitHub references attached to the message.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("attachments")]
    public UserMessageDataAttachmentsItem[]? Attachments { get; set; }

    /// <summary>Origin of this message, used for timeline filtering (e.g., "skill-pdf" for skill-injected messages that should be hidden from the user).</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("source")]
    public string? Source { get; set; }

    /// <summary>The agent mode that was active when this message was sent.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("agentMode")]
    public UserMessageDataAgentMode? AgentMode { get; set; }

    /// <summary>CAPI interaction ID for correlating this user message with its turn.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("interactionId")]
    public string? InteractionId { get; set; }
}

/// <summary>Empty payload; the event signals that the pending message queue has changed.</summary>
public partial class PendingMessagesModifiedData
{
}

/// <summary>Turn initialization metadata including identifier and interaction tracking.</summary>
public partial class AssistantTurnStartData
{
    /// <summary>Identifier for this turn within the agentic loop, typically a stringified turn number.</summary>
    [JsonPropertyName("turnId")]
    public required string TurnId { get; set; }

    /// <summary>CAPI interaction ID for correlating this turn with upstream telemetry.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("interactionId")]
    public string? InteractionId { get; set; }
}

/// <summary>Agent intent description for current activity or plan.</summary>
public partial class AssistantIntentData
{
    /// <summary>Short description of what the agent is currently doing or planning to do.</summary>
    [JsonPropertyName("intent")]
    public required string Intent { get; set; }
}

/// <summary>Assistant reasoning content for timeline display with complete thinking text.</summary>
public partial class AssistantReasoningData
{
    /// <summary>Unique identifier for this reasoning block.</summary>
    [JsonPropertyName("reasoningId")]
    public required string ReasoningId { get; set; }

    /// <summary>The complete extended thinking text from the model.</summary>
    [JsonPropertyName("content")]
    public required string Content { get; set; }
}

/// <summary>Streaming reasoning delta for incremental extended thinking updates.</summary>
public partial class AssistantReasoningDeltaData
{
    /// <summary>Reasoning block ID this delta belongs to, matching the corresponding assistant.reasoning event.</summary>
    [JsonPropertyName("reasoningId")]
    public required string ReasoningId { get; set; }

    /// <summary>Incremental text chunk to append to the reasoning content.</summary>
    [JsonPropertyName("deltaContent")]
    public required string DeltaContent { get; set; }
}

/// <summary>Streaming response progress with cumulative byte count.</summary>
public partial class AssistantStreamingDeltaData
{
    /// <summary>Cumulative total bytes received from the streaming response so far.</summary>
    [JsonPropertyName("totalResponseSizeBytes")]
    public required double TotalResponseSizeBytes { get; set; }
}

/// <summary>Assistant response containing text content, optional tool requests, and interaction metadata.</summary>
public partial class AssistantMessageData
{
    /// <summary>Unique identifier for this assistant message.</summary>
    [JsonPropertyName("messageId")]
    public required string MessageId { get; set; }

    /// <summary>The assistant's text response content.</summary>
    [JsonPropertyName("content")]
    public required string Content { get; set; }

    /// <summary>Tool invocations requested by the assistant in this message.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("toolRequests")]
    public AssistantMessageDataToolRequestsItem[]? ToolRequests { get; set; }

    /// <summary>Opaque/encrypted extended thinking data from Anthropic models. Session-bound and stripped on resume.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("reasoningOpaque")]
    public string? ReasoningOpaque { get; set; }

    /// <summary>Readable reasoning text from the model's extended thinking.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("reasoningText")]
    public string? ReasoningText { get; set; }

    /// <summary>Encrypted reasoning content from OpenAI models. Session-bound and stripped on resume.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("encryptedContent")]
    public string? EncryptedContent { get; set; }

    /// <summary>Generation phase for phased-output models (e.g., thinking vs. response phases).</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("phase")]
    public string? Phase { get; set; }

    /// <summary>Actual output token count from the API response (completion_tokens), used for accurate token accounting.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("outputTokens")]
    public double? OutputTokens { get; set; }

    /// <summary>CAPI interaction ID for correlating this message with upstream telemetry.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("interactionId")]
    public string? InteractionId { get; set; }

    /// <summary>Tool call ID of the parent tool invocation when this event originates from a sub-agent.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("parentToolCallId")]
    public string? ParentToolCallId { get; set; }
}

/// <summary>Streaming assistant message delta for incremental response updates.</summary>
public partial class AssistantMessageDeltaData
{
    /// <summary>Message ID this delta belongs to, matching the corresponding assistant.message event.</summary>
    [JsonPropertyName("messageId")]
    public required string MessageId { get; set; }

    /// <summary>Incremental text chunk to append to the message content.</summary>
    [JsonPropertyName("deltaContent")]
    public required string DeltaContent { get; set; }

    /// <summary>Tool call ID of the parent tool invocation when this event originates from a sub-agent.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("parentToolCallId")]
    public string? ParentToolCallId { get; set; }
}

/// <summary>Turn completion metadata including the turn identifier.</summary>
public partial class AssistantTurnEndData
{
    /// <summary>Identifier of the turn that has ended, matching the corresponding assistant.turn_start event.</summary>
    [JsonPropertyName("turnId")]
    public required string TurnId { get; set; }
}

/// <summary>LLM API call usage metrics including tokens, costs, quotas, and billing information.</summary>
public partial class AssistantUsageData
{
    /// <summary>Model identifier used for this API call.</summary>
    [JsonPropertyName("model")]
    public required string Model { get; set; }

    /// <summary>Number of input tokens consumed.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("inputTokens")]
    public double? InputTokens { get; set; }

    /// <summary>Number of output tokens produced.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("outputTokens")]
    public double? OutputTokens { get; set; }

    /// <summary>Number of tokens read from prompt cache.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("cacheReadTokens")]
    public double? CacheReadTokens { get; set; }

    /// <summary>Number of tokens written to prompt cache.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("cacheWriteTokens")]
    public double? CacheWriteTokens { get; set; }

    /// <summary>Model multiplier cost for billing purposes.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("cost")]
    public double? Cost { get; set; }

    /// <summary>Duration of the API call in milliseconds.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("duration")]
    public double? Duration { get; set; }

    /// <summary>Time to first token in milliseconds. Only available for streaming requests.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("ttftMs")]
    public double? TtftMs { get; set; }

    /// <summary>Average inter-token latency in milliseconds. Only available for streaming requests.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("interTokenLatencyMs")]
    public double? InterTokenLatencyMs { get; set; }

    /// <summary>What initiated this API call (e.g., "sub-agent", "mcp-sampling"); absent for user-initiated calls.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("initiator")]
    public string? Initiator { get; set; }

    /// <summary>Completion ID from the model provider (e.g., chatcmpl-abc123).</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("apiCallId")]
    public string? ApiCallId { get; set; }

    /// <summary>GitHub request tracing ID (x-github-request-id header) for server-side log correlation.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("providerCallId")]
    public string? ProviderCallId { get; set; }

    /// <summary>Parent tool call ID when this usage originates from a sub-agent.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("parentToolCallId")]
    public string? ParentToolCallId { get; set; }

    /// <summary>Per-quota resource usage snapshots, keyed by quota identifier.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("quotaSnapshots")]
    public Dictionary<string, object>? QuotaSnapshots { get; set; }

    /// <summary>Per-request cost and usage data from the CAPI copilot_usage response field.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("copilotUsage")]
    public AssistantUsageDataCopilotUsage? CopilotUsage { get; set; }

    /// <summary>Reasoning effort level used for model calls, if applicable (e.g. "low", "medium", "high", "xhigh").</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("reasoningEffort")]
    public string? ReasoningEffort { get; set; }
}

/// <summary>Turn abort information including the reason for termination.</summary>
public partial class AbortData
{
    /// <summary>Reason the current turn was aborted (e.g., "user initiated").</summary>
    [JsonPropertyName("reason")]
    public required string Reason { get; set; }
}

/// <summary>User-initiated tool invocation request with tool name and arguments.</summary>
public partial class ToolUserRequestedData
{
    /// <summary>Unique identifier for this tool call.</summary>
    [JsonPropertyName("toolCallId")]
    public required string ToolCallId { get; set; }

    /// <summary>Name of the tool the user wants to invoke.</summary>
    [JsonPropertyName("toolName")]
    public required string ToolName { get; set; }

    /// <summary>Arguments for the tool invocation.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("arguments")]
    public object? Arguments { get; set; }
}

/// <summary>Tool execution startup details including MCP server information when applicable.</summary>
public partial class ToolExecutionStartData
{
    /// <summary>Unique identifier for this tool call.</summary>
    [JsonPropertyName("toolCallId")]
    public required string ToolCallId { get; set; }

    /// <summary>Name of the tool being executed.</summary>
    [JsonPropertyName("toolName")]
    public required string ToolName { get; set; }

    /// <summary>Arguments passed to the tool.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("arguments")]
    public object? Arguments { get; set; }

    /// <summary>Name of the MCP server hosting this tool, when the tool is an MCP tool.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("mcpServerName")]
    public string? McpServerName { get; set; }

    /// <summary>Original tool name on the MCP server, when the tool is an MCP tool.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("mcpToolName")]
    public string? McpToolName { get; set; }

    /// <summary>Tool call ID of the parent tool invocation when this event originates from a sub-agent.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("parentToolCallId")]
    public string? ParentToolCallId { get; set; }
}

/// <summary>Streaming tool execution output for incremental result display.</summary>
public partial class ToolExecutionPartialResultData
{
    /// <summary>Tool call ID this partial result belongs to.</summary>
    [JsonPropertyName("toolCallId")]
    public required string ToolCallId { get; set; }

    /// <summary>Incremental output chunk from the running tool.</summary>
    [JsonPropertyName("partialOutput")]
    public required string PartialOutput { get; set; }
}

/// <summary>Tool execution progress notification with status message.</summary>
public partial class ToolExecutionProgressData
{
    /// <summary>Tool call ID this progress notification belongs to.</summary>
    [JsonPropertyName("toolCallId")]
    public required string ToolCallId { get; set; }

    /// <summary>Human-readable progress status message (e.g., from an MCP server).</summary>
    [JsonPropertyName("progressMessage")]
    public required string ProgressMessage { get; set; }
}

/// <summary>Tool execution completion results including success status, detailed output, and error information.</summary>
public partial class ToolExecutionCompleteData
{
    /// <summary>Unique identifier for the completed tool call.</summary>
    [JsonPropertyName("toolCallId")]
    public required string ToolCallId { get; set; }

    /// <summary>Whether the tool execution completed successfully.</summary>
    [JsonPropertyName("success")]
    public required bool Success { get; set; }

    /// <summary>Model identifier that generated this tool call.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("model")]
    public string? Model { get; set; }

    /// <summary>CAPI interaction ID for correlating this tool execution with upstream telemetry.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("interactionId")]
    public string? InteractionId { get; set; }

    /// <summary>Whether this tool call was explicitly requested by the user rather than the assistant.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("isUserRequested")]
    public bool? IsUserRequested { get; set; }

    /// <summary>Tool execution result on success.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("result")]
    public ToolExecutionCompleteDataResult? Result { get; set; }

    /// <summary>Error details when the tool execution failed.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("error")]
    public ToolExecutionCompleteDataError? Error { get; set; }

    /// <summary>Tool-specific telemetry data (e.g., CodeQL check counts, grep match counts).</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("toolTelemetry")]
    public Dictionary<string, object>? ToolTelemetry { get; set; }

    /// <summary>Tool call ID of the parent tool invocation when this event originates from a sub-agent.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("parentToolCallId")]
    public string? ParentToolCallId { get; set; }
}

/// <summary>Skill invocation details including content, allowed tools, and plugin metadata.</summary>
public partial class SkillInvokedData
{
    /// <summary>Name of the invoked skill.</summary>
    [JsonPropertyName("name")]
    public required string Name { get; set; }

    /// <summary>File path to the SKILL.md definition.</summary>
    [JsonPropertyName("path")]
    public required string Path { get; set; }

    /// <summary>Full content of the skill file, injected into the conversation for the model.</summary>
    [JsonPropertyName("content")]
    public required string Content { get; set; }

    /// <summary>Tool names that should be auto-approved when this skill is active.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("allowedTools")]
    public string[]? AllowedTools { get; set; }

    /// <summary>Name of the plugin this skill originated from, when applicable.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("pluginName")]
    public string? PluginName { get; set; }

    /// <summary>Version of the plugin this skill originated from, when applicable.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("pluginVersion")]
    public string? PluginVersion { get; set; }

    /// <summary>Description of the skill from its SKILL.md frontmatter.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("description")]
    public string? Description { get; set; }
}

/// <summary>Sub-agent startup details including parent tool call and agent information.</summary>
public partial class SubagentStartedData
{
    /// <summary>Tool call ID of the parent tool invocation that spawned this sub-agent.</summary>
    [JsonPropertyName("toolCallId")]
    public required string ToolCallId { get; set; }

    /// <summary>Internal name of the sub-agent.</summary>
    [JsonPropertyName("agentName")]
    public required string AgentName { get; set; }

    /// <summary>Human-readable display name of the sub-agent.</summary>
    [JsonPropertyName("agentDisplayName")]
    public required string AgentDisplayName { get; set; }

    /// <summary>Description of what the sub-agent does.</summary>
    [JsonPropertyName("agentDescription")]
    public required string AgentDescription { get; set; }
}

/// <summary>Sub-agent completion details for successful execution.</summary>
public partial class SubagentCompletedData
{
    /// <summary>Tool call ID of the parent tool invocation that spawned this sub-agent.</summary>
    [JsonPropertyName("toolCallId")]
    public required string ToolCallId { get; set; }

    /// <summary>Internal name of the sub-agent.</summary>
    [JsonPropertyName("agentName")]
    public required string AgentName { get; set; }

    /// <summary>Human-readable display name of the sub-agent.</summary>
    [JsonPropertyName("agentDisplayName")]
    public required string AgentDisplayName { get; set; }

    /// <summary>Model used by the sub-agent.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("model")]
    public string? Model { get; set; }

    /// <summary>Total number of tool calls made by the sub-agent.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("totalToolCalls")]
    public double? TotalToolCalls { get; set; }

    /// <summary>Total tokens (input + output) consumed by the sub-agent.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("totalTokens")]
    public double? TotalTokens { get; set; }

    /// <summary>Wall-clock duration of the sub-agent execution in milliseconds.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("durationMs")]
    public double? DurationMs { get; set; }
}

/// <summary>Sub-agent failure details including error message and agent information.</summary>
public partial class SubagentFailedData
{
    /// <summary>Tool call ID of the parent tool invocation that spawned this sub-agent.</summary>
    [JsonPropertyName("toolCallId")]
    public required string ToolCallId { get; set; }

    /// <summary>Internal name of the sub-agent.</summary>
    [JsonPropertyName("agentName")]
    public required string AgentName { get; set; }

    /// <summary>Human-readable display name of the sub-agent.</summary>
    [JsonPropertyName("agentDisplayName")]
    public required string AgentDisplayName { get; set; }

    /// <summary>Error message describing why the sub-agent failed.</summary>
    [JsonPropertyName("error")]
    public required string Error { get; set; }

    /// <summary>Model used by the sub-agent (if any model calls succeeded before failure).</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("model")]
    public string? Model { get; set; }

    /// <summary>Total number of tool calls made before the sub-agent failed.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("totalToolCalls")]
    public double? TotalToolCalls { get; set; }

    /// <summary>Total tokens (input + output) consumed before the sub-agent failed.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("totalTokens")]
    public double? TotalTokens { get; set; }

    /// <summary>Wall-clock duration of the sub-agent execution in milliseconds.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("durationMs")]
    public double? DurationMs { get; set; }
}

/// <summary>Custom agent selection details including name and available tools.</summary>
public partial class SubagentSelectedData
{
    /// <summary>Internal name of the selected custom agent.</summary>
    [JsonPropertyName("agentName")]
    public required string AgentName { get; set; }

    /// <summary>Human-readable display name of the selected custom agent.</summary>
    [JsonPropertyName("agentDisplayName")]
    public required string AgentDisplayName { get; set; }

    /// <summary>List of tool names available to this agent, or null for all tools.</summary>
    [JsonPropertyName("tools")]
    public string[]? Tools { get; set; }
}

/// <summary>Empty payload; the event signals that the custom agent was deselected, returning to the default agent.</summary>
public partial class SubagentDeselectedData
{
}

/// <summary>Hook invocation start details including type and input data.</summary>
public partial class HookStartData
{
    /// <summary>Unique identifier for this hook invocation.</summary>
    [JsonPropertyName("hookInvocationId")]
    public required string HookInvocationId { get; set; }

    /// <summary>Type of hook being invoked (e.g., "preToolUse", "postToolUse", "sessionStart").</summary>
    [JsonPropertyName("hookType")]
    public required string HookType { get; set; }

    /// <summary>Input data passed to the hook.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("input")]
    public object? Input { get; set; }
}

/// <summary>Hook invocation completion details including output, success status, and error information.</summary>
public partial class HookEndData
{
    /// <summary>Identifier matching the corresponding hook.start event.</summary>
    [JsonPropertyName("hookInvocationId")]
    public required string HookInvocationId { get; set; }

    /// <summary>Type of hook that was invoked (e.g., "preToolUse", "postToolUse", "sessionStart").</summary>
    [JsonPropertyName("hookType")]
    public required string HookType { get; set; }

    /// <summary>Output data produced by the hook.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("output")]
    public object? Output { get; set; }

    /// <summary>Whether the hook completed successfully.</summary>
    [JsonPropertyName("success")]
    public required bool Success { get; set; }

    /// <summary>Error details when the hook failed.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("error")]
    public HookEndDataError? Error { get; set; }
}

/// <summary>System or developer message content with role and optional template metadata.</summary>
public partial class SystemMessageData
{
    /// <summary>The system or developer prompt text.</summary>
    [JsonPropertyName("content")]
    public required string Content { get; set; }

    /// <summary>Message role: "system" for system prompts, "developer" for developer-injected instructions.</summary>
    [JsonPropertyName("role")]
    public required SystemMessageDataRole Role { get; set; }

    /// <summary>Optional name identifier for the message source.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("name")]
    public string? Name { get; set; }

    /// <summary>Metadata about the prompt template and its construction.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("metadata")]
    public SystemMessageDataMetadata? Metadata { get; set; }
}

/// <summary>System-generated notification for runtime events like background task completion.</summary>
public partial class SystemNotificationData
{
    /// <summary>The notification text, typically wrapped in &lt;system_notification&gt; XML tags.</summary>
    [JsonPropertyName("content")]
    public required string Content { get; set; }

    /// <summary>Structured metadata identifying what triggered this notification.</summary>
    [JsonPropertyName("kind")]
    public required SystemNotificationDataKind Kind { get; set; }
}

/// <summary>Permission request notification requiring client approval with request details.</summary>
public partial class PermissionRequestedData
{
    /// <summary>Unique identifier for this permission request; used to respond via session.respondToPermission().</summary>
    [JsonPropertyName("requestId")]
    public required string RequestId { get; set; }

    /// <summary>Details of the permission being requested.</summary>
    [JsonPropertyName("permissionRequest")]
    public required PermissionRequest PermissionRequest { get; set; }
}

/// <summary>Permission request completion notification signaling UI dismissal.</summary>
public partial class PermissionCompletedData
{
    /// <summary>Request ID of the resolved permission request; clients should dismiss any UI for this request.</summary>
    [JsonPropertyName("requestId")]
    public required string RequestId { get; set; }

    /// <summary>The result of the permission request.</summary>
    [JsonPropertyName("result")]
    public required PermissionCompletedDataResult Result { get; set; }
}

/// <summary>User input request notification with question and optional predefined choices.</summary>
public partial class UserInputRequestedData
{
    /// <summary>Unique identifier for this input request; used to respond via session.respondToUserInput().</summary>
    [JsonPropertyName("requestId")]
    public required string RequestId { get; set; }

    /// <summary>The question or prompt to present to the user.</summary>
    [JsonPropertyName("question")]
    public required string Question { get; set; }

    /// <summary>Predefined choices for the user to select from, if applicable.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("choices")]
    public string[]? Choices { get; set; }

    /// <summary>Whether the user can provide a free-form text response in addition to predefined choices.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("allowFreeform")]
    public bool? AllowFreeform { get; set; }

    /// <summary>The LLM-assigned tool call ID that triggered this request; used by remote UIs to correlate responses.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("toolCallId")]
    public string? ToolCallId { get; set; }
}

/// <summary>User input request completion notification signaling UI dismissal.</summary>
public partial class UserInputCompletedData
{
    /// <summary>Request ID of the resolved user input request; clients should dismiss any UI for this request.</summary>
    [JsonPropertyName("requestId")]
    public required string RequestId { get; set; }
}

/// <summary>Elicitation request; may be form-based (structured input) or URL-based (browser redirect).</summary>
public partial class ElicitationRequestedData
{
    /// <summary>Unique identifier for this elicitation request; used to respond via session.respondToElicitation().</summary>
    [JsonPropertyName("requestId")]
    public required string RequestId { get; set; }

    /// <summary>Tool call ID from the LLM completion; used to correlate with CompletionChunk.toolCall.id for remote UIs.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("toolCallId")]
    public string? ToolCallId { get; set; }

    /// <summary>The source that initiated the request (MCP server name, or absent for agent-initiated).</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("elicitationSource")]
    public string? ElicitationSource { get; set; }

    /// <summary>Message describing what information is needed from the user.</summary>
    [JsonPropertyName("message")]
    public required string Message { get; set; }

    /// <summary>Elicitation mode; "form" for structured input, "url" for browser-based. Defaults to "form" when absent.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("mode")]
    public ElicitationRequestedDataMode? Mode { get; set; }

    /// <summary>JSON Schema describing the form fields to present to the user (form mode only).</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("requestedSchema")]
    public ElicitationRequestedDataRequestedSchema? RequestedSchema { get; set; }

    /// <summary>URL to open in the user's browser (url mode only).</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("url")]
    public string? Url { get; set; }
}

/// <summary>Elicitation request completion notification signaling UI dismissal.</summary>
public partial class ElicitationCompletedData
{
    /// <summary>Request ID of the resolved elicitation request; clients should dismiss any UI for this request.</summary>
    [JsonPropertyName("requestId")]
    public required string RequestId { get; set; }
}

/// <summary>Sampling request from an MCP server; contains the server name and a requestId for correlation.</summary>
public partial class SamplingRequestedData
{
    /// <summary>Unique identifier for this sampling request; used to respond via session.respondToSampling().</summary>
    [JsonPropertyName("requestId")]
    public required string RequestId { get; set; }

    /// <summary>Name of the MCP server that initiated the sampling request.</summary>
    [JsonPropertyName("serverName")]
    public required string ServerName { get; set; }

    /// <summary>The JSON-RPC request ID from the MCP protocol.</summary>
    [JsonPropertyName("mcpRequestId")]
    public required object McpRequestId { get; set; }
}

/// <summary>Sampling request completion notification signaling UI dismissal.</summary>
public partial class SamplingCompletedData
{
    /// <summary>Request ID of the resolved sampling request; clients should dismiss any UI for this request.</summary>
    [JsonPropertyName("requestId")]
    public required string RequestId { get; set; }
}

/// <summary>OAuth authentication request for an MCP server.</summary>
public partial class McpOauthRequiredData
{
    /// <summary>Unique identifier for this OAuth request; used to respond via session.respondToMcpOAuth().</summary>
    [JsonPropertyName("requestId")]
    public required string RequestId { get; set; }

    /// <summary>Display name of the MCP server that requires OAuth.</summary>
    [JsonPropertyName("serverName")]
    public required string ServerName { get; set; }

    /// <summary>URL of the MCP server that requires OAuth.</summary>
    [JsonPropertyName("serverUrl")]
    public required string ServerUrl { get; set; }

    /// <summary>Static OAuth client configuration, if the server specifies one.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("staticClientConfig")]
    public McpOauthRequiredDataStaticClientConfig? StaticClientConfig { get; set; }
}

/// <summary>MCP OAuth request completion notification.</summary>
public partial class McpOauthCompletedData
{
    /// <summary>Request ID of the resolved OAuth request.</summary>
    [JsonPropertyName("requestId")]
    public required string RequestId { get; set; }
}

/// <summary>External tool invocation request for client-side tool execution.</summary>
public partial class ExternalToolRequestedData
{
    /// <summary>Unique identifier for this request; used to respond via session.respondToExternalTool().</summary>
    [JsonPropertyName("requestId")]
    public required string RequestId { get; set; }

    /// <summary>Session ID that this external tool request belongs to.</summary>
    [JsonPropertyName("sessionId")]
    public required string SessionId { get; set; }

    /// <summary>Tool call ID assigned to this external tool invocation.</summary>
    [JsonPropertyName("toolCallId")]
    public required string ToolCallId { get; set; }

    /// <summary>Name of the external tool to invoke.</summary>
    [JsonPropertyName("toolName")]
    public required string ToolName { get; set; }

    /// <summary>Arguments to pass to the external tool.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("arguments")]
    public object? Arguments { get; set; }

    /// <summary>W3C Trace Context traceparent header for the execute_tool span.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("traceparent")]
    public string? Traceparent { get; set; }

    /// <summary>W3C Trace Context tracestate header for the execute_tool span.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("tracestate")]
    public string? Tracestate { get; set; }
}

/// <summary>External tool completion notification signaling UI dismissal.</summary>
public partial class ExternalToolCompletedData
{
    /// <summary>Request ID of the resolved external tool request; clients should dismiss any UI for this request.</summary>
    [JsonPropertyName("requestId")]
    public required string RequestId { get; set; }
}

/// <summary>Queued slash command dispatch request for client execution.</summary>
public partial class CommandQueuedData
{
    /// <summary>Unique identifier for this request; used to respond via session.respondToQueuedCommand().</summary>
    [JsonPropertyName("requestId")]
    public required string RequestId { get; set; }

    /// <summary>The slash command text to be executed (e.g., /help, /clear).</summary>
    [JsonPropertyName("command")]
    public required string Command { get; set; }
}

/// <summary>Registered command dispatch request routed to the owning client.</summary>
public partial class CommandExecuteData
{
    /// <summary>Unique identifier; used to respond via session.commands.handlePendingCommand().</summary>
    [JsonPropertyName("requestId")]
    public required string RequestId { get; set; }

    /// <summary>The full command text (e.g., /deploy production).</summary>
    [JsonPropertyName("command")]
    public required string Command { get; set; }

    /// <summary>Command name without leading /.</summary>
    [JsonPropertyName("commandName")]
    public required string CommandName { get; set; }

    /// <summary>Raw argument string after the command name.</summary>
    [JsonPropertyName("args")]
    public required string Args { get; set; }
}

/// <summary>Queued command completion notification signaling UI dismissal.</summary>
public partial class CommandCompletedData
{
    /// <summary>Request ID of the resolved command request; clients should dismiss any UI for this request.</summary>
    [JsonPropertyName("requestId")]
    public required string RequestId { get; set; }
}

/// <summary>SDK command registration change notification.</summary>
public partial class CommandsChangedData
{
    /// <summary>Current list of registered SDK commands.</summary>
    [JsonPropertyName("commands")]
    public required CommandsChangedDataCommandsItem[] Commands { get; set; }
}

/// <summary>Session capability change notification.</summary>
public partial class CapabilitiesChangedData
{
    /// <summary>UI capability changes.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("ui")]
    public CapabilitiesChangedDataUi? Ui { get; set; }
}

/// <summary>Plan approval request with plan content and available user actions.</summary>
public partial class ExitPlanModeRequestedData
{
    /// <summary>Unique identifier for this request; used to respond via session.respondToExitPlanMode().</summary>
    [JsonPropertyName("requestId")]
    public required string RequestId { get; set; }

    /// <summary>Summary of the plan that was created.</summary>
    [JsonPropertyName("summary")]
    public required string Summary { get; set; }

    /// <summary>Full content of the plan file.</summary>
    [JsonPropertyName("planContent")]
    public required string PlanContent { get; set; }

    /// <summary>Available actions the user can take (e.g., approve, edit, reject).</summary>
    [JsonPropertyName("actions")]
    public required string[] Actions { get; set; }

    /// <summary>The recommended action for the user to take.</summary>
    [JsonPropertyName("recommendedAction")]
    public required string RecommendedAction { get; set; }
}

/// <summary>Plan mode exit completion notification signaling UI dismissal.</summary>
public partial class ExitPlanModeCompletedData
{
    /// <summary>Request ID of the resolved exit plan mode request; clients should dismiss any UI for this request.</summary>
    [JsonPropertyName("requestId")]
    public required string RequestId { get; set; }
}

/// <summary>Event payload for <see cref="SessionToolsUpdatedEvent"/>.</summary>
public partial class SessionToolsUpdatedData
{
    /// <summary>Gets or sets the <c>model</c> value.</summary>
    [JsonPropertyName("model")]
    public required string Model { get; set; }
}

/// <summary>Event payload for <see cref="SessionBackgroundTasksChangedEvent"/>.</summary>
public partial class SessionBackgroundTasksChangedData
{
}

/// <summary>Event payload for <see cref="SessionSkillsLoadedEvent"/>.</summary>
public partial class SessionSkillsLoadedData
{
    /// <summary>Array of resolved skill metadata.</summary>
    [JsonPropertyName("skills")]
    public required SessionSkillsLoadedDataSkillsItem[] Skills { get; set; }
}

/// <summary>Event payload for <see cref="SessionCustomAgentsUpdatedEvent"/>.</summary>
public partial class SessionCustomAgentsUpdatedData
{
    /// <summary>Array of loaded custom agent metadata.</summary>
    [JsonPropertyName("agents")]
    public required SessionCustomAgentsUpdatedDataAgentsItem[] Agents { get; set; }

    /// <summary>Non-fatal warnings from agent loading.</summary>
    [JsonPropertyName("warnings")]
    public required string[] Warnings { get; set; }

    /// <summary>Fatal errors from agent loading.</summary>
    [JsonPropertyName("errors")]
    public required string[] Errors { get; set; }
}

/// <summary>Event payload for <see cref="SessionMcpServersLoadedEvent"/>.</summary>
public partial class SessionMcpServersLoadedData
{
    /// <summary>Array of MCP server status summaries.</summary>
    [JsonPropertyName("servers")]
    public required SessionMcpServersLoadedDataServersItem[] Servers { get; set; }
}

/// <summary>Event payload for <see cref="SessionMcpServerStatusChangedEvent"/>.</summary>
public partial class SessionMcpServerStatusChangedData
{
    /// <summary>Name of the MCP server whose status changed.</summary>
    [JsonPropertyName("serverName")]
    public required string ServerName { get; set; }

    /// <summary>New connection status: connected, failed, needs-auth, pending, disabled, or not_configured.</summary>
    [JsonPropertyName("status")]
    public required SessionMcpServersLoadedDataServersItemStatus Status { get; set; }
}

/// <summary>Event payload for <see cref="SessionExtensionsLoadedEvent"/>.</summary>
public partial class SessionExtensionsLoadedData
{
    /// <summary>Array of discovered extensions and their status.</summary>
    [JsonPropertyName("extensions")]
    public required SessionExtensionsLoadedDataExtensionsItem[] Extensions { get; set; }
}

/// <summary>Working directory and git context at session start.</summary>
/// <remarks>Nested data type for <c>SessionStartDataContext</c>.</remarks>
public partial class SessionStartDataContext
{
    /// <summary>Current working directory path.</summary>
    [JsonPropertyName("cwd")]
    public required string Cwd { get; set; }

    /// <summary>Root directory of the git repository, resolved via git rev-parse.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("gitRoot")]
    public string? GitRoot { get; set; }

    /// <summary>Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps).</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("repository")]
    public string? Repository { get; set; }

    /// <summary>Hosting platform type of the repository (github or ado).</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("hostType")]
    public SessionStartDataContextHostType? HostType { get; set; }

    /// <summary>Current git branch name.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("branch")]
    public string? Branch { get; set; }

    /// <summary>Head commit of current git branch at session start time.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("headCommit")]
    public string? HeadCommit { get; set; }

    /// <summary>Base commit of current git branch at session start time.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("baseCommit")]
    public string? BaseCommit { get; set; }
}

/// <summary>Updated working directory and git context at resume time.</summary>
/// <remarks>Nested data type for <c>SessionResumeDataContext</c>.</remarks>
public partial class SessionResumeDataContext
{
    /// <summary>Current working directory path.</summary>
    [JsonPropertyName("cwd")]
    public required string Cwd { get; set; }

    /// <summary>Root directory of the git repository, resolved via git rev-parse.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("gitRoot")]
    public string? GitRoot { get; set; }

    /// <summary>Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps).</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("repository")]
    public string? Repository { get; set; }

    /// <summary>Hosting platform type of the repository (github or ado).</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("hostType")]
    public SessionStartDataContextHostType? HostType { get; set; }

    /// <summary>Current git branch name.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("branch")]
    public string? Branch { get; set; }

    /// <summary>Head commit of current git branch at session start time.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("headCommit")]
    public string? HeadCommit { get; set; }

    /// <summary>Base commit of current git branch at session start time.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("baseCommit")]
    public string? BaseCommit { get; set; }
}

/// <summary>A background agent task.</summary>
/// <remarks>Nested data type for <c>SessionIdleDataBackgroundTasksAgentsItem</c>.</remarks>
public partial class SessionIdleDataBackgroundTasksAgentsItem
{
    /// <summary>Unique identifier of the background agent.</summary>
    [JsonPropertyName("agentId")]
    public required string AgentId { get; set; }

    /// <summary>Type of the background agent.</summary>
    [JsonPropertyName("agentType")]
    public required string AgentType { get; set; }

    /// <summary>Human-readable description of the agent task.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("description")]
    public string? Description { get; set; }
}

/// <summary>A background shell command.</summary>
/// <remarks>Nested data type for <c>SessionIdleDataBackgroundTasksShellsItem</c>.</remarks>
public partial class SessionIdleDataBackgroundTasksShellsItem
{
    /// <summary>Unique identifier of the background shell.</summary>
    [JsonPropertyName("shellId")]
    public required string ShellId { get; set; }

    /// <summary>Human-readable description of the shell command.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("description")]
    public string? Description { get; set; }
}

/// <summary>Background tasks still running when the agent became idle.</summary>
/// <remarks>Nested data type for <c>SessionIdleDataBackgroundTasks</c>.</remarks>
public partial class SessionIdleDataBackgroundTasks
{
    /// <summary>Currently running background agents.</summary>
    [JsonPropertyName("agents")]
    public required SessionIdleDataBackgroundTasksAgentsItem[] Agents { get; set; }

    /// <summary>Currently running background shell commands.</summary>
    [JsonPropertyName("shells")]
    public required SessionIdleDataBackgroundTasksShellsItem[] Shells { get; set; }
}

/// <summary>Repository context for the handed-off session.</summary>
/// <remarks>Nested data type for <c>SessionHandoffDataRepository</c>.</remarks>
public partial class SessionHandoffDataRepository
{
    /// <summary>Repository owner (user or organization).</summary>
    [JsonPropertyName("owner")]
    public required string Owner { get; set; }

    /// <summary>Repository name.</summary>
    [JsonPropertyName("name")]
    public required string Name { get; set; }

    /// <summary>Git branch name, if applicable.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("branch")]
    public string? Branch { get; set; }
}

/// <summary>Aggregate code change metrics for the session.</summary>
/// <remarks>Nested data type for <c>SessionShutdownDataCodeChanges</c>.</remarks>
public partial class SessionShutdownDataCodeChanges
{
    /// <summary>Total number of lines added during the session.</summary>
    [JsonPropertyName("linesAdded")]
    public required double LinesAdded { get; set; }

    /// <summary>Total number of lines removed during the session.</summary>
    [JsonPropertyName("linesRemoved")]
    public required double LinesRemoved { get; set; }

    /// <summary>List of file paths that were modified during the session.</summary>
    [JsonPropertyName("filesModified")]
    public required string[] FilesModified { get; set; }
}

/// <summary>Token usage breakdown for the compaction LLM call.</summary>
/// <remarks>Nested data type for <c>SessionCompactionCompleteDataCompactionTokensUsed</c>.</remarks>
public partial class SessionCompactionCompleteDataCompactionTokensUsed
{
    /// <summary>Input tokens consumed by the compaction LLM call.</summary>
    [JsonPropertyName("input")]
    public required double Input { get; set; }

    /// <summary>Output tokens produced by the compaction LLM call.</summary>
    [JsonPropertyName("output")]
    public required double Output { get; set; }

    /// <summary>Cached input tokens reused in the compaction LLM call.</summary>
    [JsonPropertyName("cachedInput")]
    public required double CachedInput { get; set; }
}

/// <summary>Optional line range to scope the attachment to a specific section of the file.</summary>
/// <remarks>Nested data type for <c>UserMessageDataAttachmentsItemFileLineRange</c>.</remarks>
public partial class UserMessageDataAttachmentsItemFileLineRange
{
    /// <summary>Start line number (1-based).</summary>
    [JsonPropertyName("start")]
    public required double Start { get; set; }

    /// <summary>End line number (1-based, inclusive).</summary>
    [JsonPropertyName("end")]
    public required double End { get; set; }
}

/// <summary>File attachment.</summary>
/// <remarks>The <c>file</c> variant of <see cref="UserMessageDataAttachmentsItem"/>.</remarks>
public partial class UserMessageDataAttachmentsItemFile : UserMessageDataAttachmentsItem
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "file";

    /// <summary>Absolute file path.</summary>
    [JsonPropertyName("path")]
    public required string Path { get; set; }

    /// <summary>User-facing display name for the attachment.</summary>
    [JsonPropertyName("displayName")]
    public required string DisplayName { get; set; }

    /// <summary>Optional line range to scope the attachment to a specific section of the file.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("lineRange")]
    public UserMessageDataAttachmentsItemFileLineRange? LineRange { get; set; }
}

/// <summary>Directory attachment.</summary>
/// <remarks>The <c>directory</c> variant of <see cref="UserMessageDataAttachmentsItem"/>.</remarks>
public partial class UserMessageDataAttachmentsItemDirectory : UserMessageDataAttachmentsItem
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "directory";

    /// <summary>Absolute directory path.</summary>
    [JsonPropertyName("path")]
    public required string Path { get; set; }

    /// <summary>User-facing display name for the attachment.</summary>
    [JsonPropertyName("displayName")]
    public required string DisplayName { get; set; }
}

/// <summary>Start position of the selection.</summary>
/// <remarks>Nested data type for <c>UserMessageDataAttachmentsItemSelectionSelectionStart</c>.</remarks>
public partial class UserMessageDataAttachmentsItemSelectionSelectionStart
{
    /// <summary>Start line number (0-based).</summary>
    [JsonPropertyName("line")]
    public required double Line { get; set; }

    /// <summary>Start character offset within the line (0-based).</summary>
    [JsonPropertyName("character")]
    public required double Character { get; set; }
}

/// <summary>End position of the selection.</summary>
/// <remarks>Nested data type for <c>UserMessageDataAttachmentsItemSelectionSelectionEnd</c>.</remarks>
public partial class UserMessageDataAttachmentsItemSelectionSelectionEnd
{
    /// <summary>End line number (0-based).</summary>
    [JsonPropertyName("line")]
    public required double Line { get; set; }

    /// <summary>End character offset within the line (0-based).</summary>
    [JsonPropertyName("character")]
    public required double Character { get; set; }
}

/// <summary>Position range of the selection within the file.</summary>
/// <remarks>Nested data type for <c>UserMessageDataAttachmentsItemSelectionSelection</c>.</remarks>
public partial class UserMessageDataAttachmentsItemSelectionSelection
{
    /// <summary>Start position of the selection.</summary>
    [JsonPropertyName("start")]
    public required UserMessageDataAttachmentsItemSelectionSelectionStart Start { get; set; }

    /// <summary>End position of the selection.</summary>
    [JsonPropertyName("end")]
    public required UserMessageDataAttachmentsItemSelectionSelectionEnd End { get; set; }
}

/// <summary>Code selection attachment from an editor.</summary>
/// <remarks>The <c>selection</c> variant of <see cref="UserMessageDataAttachmentsItem"/>.</remarks>
public partial class UserMessageDataAttachmentsItemSelection : UserMessageDataAttachmentsItem
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "selection";

    /// <summary>Absolute path to the file containing the selection.</summary>
    [JsonPropertyName("filePath")]
    public required string FilePath { get; set; }

    /// <summary>User-facing display name for the selection.</summary>
    [JsonPropertyName("displayName")]
    public required string DisplayName { get; set; }

    /// <summary>The selected text content.</summary>
    [JsonPropertyName("text")]
    public required string Text { get; set; }

    /// <summary>Position range of the selection within the file.</summary>
    [JsonPropertyName("selection")]
    public required UserMessageDataAttachmentsItemSelectionSelection Selection { get; set; }
}

/// <summary>GitHub issue, pull request, or discussion reference.</summary>
/// <remarks>The <c>github_reference</c> variant of <see cref="UserMessageDataAttachmentsItem"/>.</remarks>
public partial class UserMessageDataAttachmentsItemGithubReference : UserMessageDataAttachmentsItem
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "github_reference";

    /// <summary>Issue, pull request, or discussion number.</summary>
    [JsonPropertyName("number")]
    public required double Number { get; set; }

    /// <summary>Title of the referenced item.</summary>
    [JsonPropertyName("title")]
    public required string Title { get; set; }

    /// <summary>Type of GitHub reference.</summary>
    [JsonPropertyName("referenceType")]
    public required UserMessageDataAttachmentsItemGithubReferenceReferenceType ReferenceType { get; set; }

    /// <summary>Current state of the referenced item (e.g., open, closed, merged).</summary>
    [JsonPropertyName("state")]
    public required string State { get; set; }

    /// <summary>URL to the referenced item on GitHub.</summary>
    [JsonPropertyName("url")]
    public required string Url { get; set; }
}

/// <summary>Blob attachment with inline base64-encoded data.</summary>
/// <remarks>The <c>blob</c> variant of <see cref="UserMessageDataAttachmentsItem"/>.</remarks>
public partial class UserMessageDataAttachmentsItemBlob : UserMessageDataAttachmentsItem
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "blob";

    /// <summary>Base64-encoded content.</summary>
    [JsonPropertyName("data")]
    public required string Data { get; set; }

    /// <summary>MIME type of the inline data.</summary>
    [JsonPropertyName("mimeType")]
    public required string MimeType { get; set; }

    /// <summary>User-facing display name for the attachment.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("displayName")]
    public string? DisplayName { get; set; }
}

/// <summary>A user message attachment — a file, directory, code selection, blob, or GitHub reference.</summary>
/// <remarks>Polymorphic base type discriminated by <c>type</c>.</remarks>
[JsonPolymorphic(
    TypeDiscriminatorPropertyName = "type",
    UnknownDerivedTypeHandling = JsonUnknownDerivedTypeHandling.FallBackToBaseType)]
[JsonDerivedType(typeof(UserMessageDataAttachmentsItemFile), "file")]
[JsonDerivedType(typeof(UserMessageDataAttachmentsItemDirectory), "directory")]
[JsonDerivedType(typeof(UserMessageDataAttachmentsItemSelection), "selection")]
[JsonDerivedType(typeof(UserMessageDataAttachmentsItemGithubReference), "github_reference")]
[JsonDerivedType(typeof(UserMessageDataAttachmentsItemBlob), "blob")]
public partial class UserMessageDataAttachmentsItem
{
    /// <summary>The type discriminator.</summary>
    [JsonPropertyName("type")]
    public virtual string Type { get; set; } = string.Empty;
}


/// <summary>A tool invocation request from the assistant.</summary>
/// <remarks>Nested data type for <c>AssistantMessageDataToolRequestsItem</c>.</remarks>
public partial class AssistantMessageDataToolRequestsItem
{
    /// <summary>Unique identifier for this tool call.</summary>
    [JsonPropertyName("toolCallId")]
    public required string ToolCallId { get; set; }

    /// <summary>Name of the tool being invoked.</summary>
    [JsonPropertyName("name")]
    public required string Name { get; set; }

    /// <summary>Arguments to pass to the tool, format depends on the tool.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("arguments")]
    public object? Arguments { get; set; }

    /// <summary>Tool call type: "function" for standard tool calls, "custom" for grammar-based tool calls. Defaults to "function" when absent.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("type")]
    public AssistantMessageDataToolRequestsItemType? Type { get; set; }

    /// <summary>Human-readable display title for the tool.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("toolTitle")]
    public string? ToolTitle { get; set; }

    /// <summary>Resolved intention summary describing what this specific call does.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("intentionSummary")]
    public string? IntentionSummary { get; set; }
}

/// <summary>Token usage detail for a single billing category.</summary>
/// <remarks>Nested data type for <c>AssistantUsageDataCopilotUsageTokenDetailsItem</c>.</remarks>
public partial class AssistantUsageDataCopilotUsageTokenDetailsItem
{
    /// <summary>Number of tokens in this billing batch.</summary>
    [JsonPropertyName("batchSize")]
    public required double BatchSize { get; set; }

    /// <summary>Cost per batch of tokens.</summary>
    [JsonPropertyName("costPerBatch")]
    public required double CostPerBatch { get; set; }

    /// <summary>Total token count for this entry.</summary>
    [JsonPropertyName("tokenCount")]
    public required double TokenCount { get; set; }

    /// <summary>Token category (e.g., "input", "output").</summary>
    [JsonPropertyName("tokenType")]
    public required string TokenType { get; set; }
}

/// <summary>Per-request cost and usage data from the CAPI copilot_usage response field.</summary>
/// <remarks>Nested data type for <c>AssistantUsageDataCopilotUsage</c>.</remarks>
public partial class AssistantUsageDataCopilotUsage
{
    /// <summary>Itemized token usage breakdown.</summary>
    [JsonPropertyName("tokenDetails")]
    public required AssistantUsageDataCopilotUsageTokenDetailsItem[] TokenDetails { get; set; }

    /// <summary>Total cost in nano-AIU (AI Units) for this request.</summary>
    [JsonPropertyName("totalNanoAiu")]
    public required double TotalNanoAiu { get; set; }
}

/// <summary>Plain text content block.</summary>
/// <remarks>The <c>text</c> variant of <see cref="ToolExecutionCompleteDataResultContentsItem"/>.</remarks>
public partial class ToolExecutionCompleteDataResultContentsItemText : ToolExecutionCompleteDataResultContentsItem
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "text";

    /// <summary>The text content.</summary>
    [JsonPropertyName("text")]
    public required string Text { get; set; }
}

/// <summary>Terminal/shell output content block with optional exit code and working directory.</summary>
/// <remarks>The <c>terminal</c> variant of <see cref="ToolExecutionCompleteDataResultContentsItem"/>.</remarks>
public partial class ToolExecutionCompleteDataResultContentsItemTerminal : ToolExecutionCompleteDataResultContentsItem
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "terminal";

    /// <summary>Terminal/shell output text.</summary>
    [JsonPropertyName("text")]
    public required string Text { get; set; }

    /// <summary>Process exit code, if the command has completed.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("exitCode")]
    public double? ExitCode { get; set; }

    /// <summary>Working directory where the command was executed.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("cwd")]
    public string? Cwd { get; set; }
}

/// <summary>Image content block with base64-encoded data.</summary>
/// <remarks>The <c>image</c> variant of <see cref="ToolExecutionCompleteDataResultContentsItem"/>.</remarks>
public partial class ToolExecutionCompleteDataResultContentsItemImage : ToolExecutionCompleteDataResultContentsItem
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "image";

    /// <summary>Base64-encoded image data.</summary>
    [JsonPropertyName("data")]
    public required string Data { get; set; }

    /// <summary>MIME type of the image (e.g., image/png, image/jpeg).</summary>
    [JsonPropertyName("mimeType")]
    public required string MimeType { get; set; }
}

/// <summary>Audio content block with base64-encoded data.</summary>
/// <remarks>The <c>audio</c> variant of <see cref="ToolExecutionCompleteDataResultContentsItem"/>.</remarks>
public partial class ToolExecutionCompleteDataResultContentsItemAudio : ToolExecutionCompleteDataResultContentsItem
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "audio";

    /// <summary>Base64-encoded audio data.</summary>
    [JsonPropertyName("data")]
    public required string Data { get; set; }

    /// <summary>MIME type of the audio (e.g., audio/wav, audio/mpeg).</summary>
    [JsonPropertyName("mimeType")]
    public required string MimeType { get; set; }
}

/// <summary>Icon image for a resource.</summary>
/// <remarks>Nested data type for <c>ToolExecutionCompleteDataResultContentsItemResourceLinkIconsItem</c>.</remarks>
public partial class ToolExecutionCompleteDataResultContentsItemResourceLinkIconsItem
{
    /// <summary>URL or path to the icon image.</summary>
    [JsonPropertyName("src")]
    public required string Src { get; set; }

    /// <summary>MIME type of the icon image.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("mimeType")]
    public string? MimeType { get; set; }

    /// <summary>Available icon sizes (e.g., ['16x16', '32x32']).</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("sizes")]
    public string[]? Sizes { get; set; }

    /// <summary>Theme variant this icon is intended for.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("theme")]
    public ToolExecutionCompleteDataResultContentsItemResourceLinkIconsItemTheme? Theme { get; set; }
}

/// <summary>Resource link content block referencing an external resource.</summary>
/// <remarks>The <c>resource_link</c> variant of <see cref="ToolExecutionCompleteDataResultContentsItem"/>.</remarks>
public partial class ToolExecutionCompleteDataResultContentsItemResourceLink : ToolExecutionCompleteDataResultContentsItem
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "resource_link";

    /// <summary>Icons associated with this resource.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("icons")]
    public ToolExecutionCompleteDataResultContentsItemResourceLinkIconsItem[]? Icons { get; set; }

    /// <summary>Resource name identifier.</summary>
    [JsonPropertyName("name")]
    public required string Name { get; set; }

    /// <summary>Human-readable display title for the resource.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("title")]
    public string? Title { get; set; }

    /// <summary>URI identifying the resource.</summary>
    [JsonPropertyName("uri")]
    public required string Uri { get; set; }

    /// <summary>Human-readable description of the resource.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("description")]
    public string? Description { get; set; }

    /// <summary>MIME type of the resource content.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("mimeType")]
    public string? MimeType { get; set; }

    /// <summary>Size of the resource in bytes.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("size")]
    public double? Size { get; set; }
}

/// <summary>Embedded resource content block with inline text or binary data.</summary>
/// <remarks>The <c>resource</c> variant of <see cref="ToolExecutionCompleteDataResultContentsItem"/>.</remarks>
public partial class ToolExecutionCompleteDataResultContentsItemResource : ToolExecutionCompleteDataResultContentsItem
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "resource";

    /// <summary>The embedded resource contents, either text or base64-encoded binary.</summary>
    [JsonPropertyName("resource")]
    public required object Resource { get; set; }
}

/// <summary>A content block within a tool result, which may be text, terminal output, image, audio, or a resource.</summary>
/// <remarks>Polymorphic base type discriminated by <c>type</c>.</remarks>
[JsonPolymorphic(
    TypeDiscriminatorPropertyName = "type",
    UnknownDerivedTypeHandling = JsonUnknownDerivedTypeHandling.FallBackToBaseType)]
[JsonDerivedType(typeof(ToolExecutionCompleteDataResultContentsItemText), "text")]
[JsonDerivedType(typeof(ToolExecutionCompleteDataResultContentsItemTerminal), "terminal")]
[JsonDerivedType(typeof(ToolExecutionCompleteDataResultContentsItemImage), "image")]
[JsonDerivedType(typeof(ToolExecutionCompleteDataResultContentsItemAudio), "audio")]
[JsonDerivedType(typeof(ToolExecutionCompleteDataResultContentsItemResourceLink), "resource_link")]
[JsonDerivedType(typeof(ToolExecutionCompleteDataResultContentsItemResource), "resource")]
public partial class ToolExecutionCompleteDataResultContentsItem
{
    /// <summary>The type discriminator.</summary>
    [JsonPropertyName("type")]
    public virtual string Type { get; set; } = string.Empty;
}


/// <summary>Tool execution result on success.</summary>
/// <remarks>Nested data type for <c>ToolExecutionCompleteDataResult</c>.</remarks>
public partial class ToolExecutionCompleteDataResult
{
    /// <summary>Concise tool result text sent to the LLM for chat completion, potentially truncated for token efficiency.</summary>
    [JsonPropertyName("content")]
    public required string Content { get; set; }

    /// <summary>Full detailed tool result for UI/timeline display, preserving complete content such as diffs. Falls back to content when absent.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("detailedContent")]
    public string? DetailedContent { get; set; }

    /// <summary>Structured content blocks (text, images, audio, resources) returned by the tool in their native format.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("contents")]
    public ToolExecutionCompleteDataResultContentsItem[]? Contents { get; set; }
}

/// <summary>Error details when the tool execution failed.</summary>
/// <remarks>Nested data type for <c>ToolExecutionCompleteDataError</c>.</remarks>
public partial class ToolExecutionCompleteDataError
{
    /// <summary>Human-readable error message.</summary>
    [JsonPropertyName("message")]
    public required string Message { get; set; }

    /// <summary>Machine-readable error code.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("code")]
    public string? Code { get; set; }
}

/// <summary>Error details when the hook failed.</summary>
/// <remarks>Nested data type for <c>HookEndDataError</c>.</remarks>
public partial class HookEndDataError
{
    /// <summary>Human-readable error message.</summary>
    [JsonPropertyName("message")]
    public required string Message { get; set; }

    /// <summary>Error stack trace, when available.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("stack")]
    public string? Stack { get; set; }
}

/// <summary>Metadata about the prompt template and its construction.</summary>
/// <remarks>Nested data type for <c>SystemMessageDataMetadata</c>.</remarks>
public partial class SystemMessageDataMetadata
{
    /// <summary>Version identifier of the prompt template used.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("promptVersion")]
    public string? PromptVersion { get; set; }

    /// <summary>Template variables used when constructing the prompt.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("variables")]
    public Dictionary<string, object>? Variables { get; set; }
}

/// <summary>The <c>agent_completed</c> variant of <see cref="SystemNotificationDataKind"/>.</summary>
public partial class SystemNotificationDataKindAgentCompleted : SystemNotificationDataKind
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "agent_completed";

    /// <summary>Unique identifier of the background agent.</summary>
    [JsonPropertyName("agentId")]
    public required string AgentId { get; set; }

    /// <summary>Type of the agent (e.g., explore, task, general-purpose).</summary>
    [JsonPropertyName("agentType")]
    public required string AgentType { get; set; }

    /// <summary>Whether the agent completed successfully or failed.</summary>
    [JsonPropertyName("status")]
    public required SystemNotificationDataKindAgentCompletedStatus Status { get; set; }

    /// <summary>Human-readable description of the agent task.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("description")]
    public string? Description { get; set; }

    /// <summary>The full prompt given to the background agent.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("prompt")]
    public string? Prompt { get; set; }
}

/// <summary>The <c>agent_idle</c> variant of <see cref="SystemNotificationDataKind"/>.</summary>
public partial class SystemNotificationDataKindAgentIdle : SystemNotificationDataKind
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "agent_idle";

    /// <summary>Unique identifier of the background agent.</summary>
    [JsonPropertyName("agentId")]
    public required string AgentId { get; set; }

    /// <summary>Type of the agent (e.g., explore, task, general-purpose).</summary>
    [JsonPropertyName("agentType")]
    public required string AgentType { get; set; }

    /// <summary>Human-readable description of the agent task.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("description")]
    public string? Description { get; set; }
}

/// <summary>The <c>shell_completed</c> variant of <see cref="SystemNotificationDataKind"/>.</summary>
public partial class SystemNotificationDataKindShellCompleted : SystemNotificationDataKind
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "shell_completed";

    /// <summary>Unique identifier of the shell session.</summary>
    [JsonPropertyName("shellId")]
    public required string ShellId { get; set; }

    /// <summary>Exit code of the shell command, if available.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("exitCode")]
    public double? ExitCode { get; set; }

    /// <summary>Human-readable description of the command.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("description")]
    public string? Description { get; set; }
}

/// <summary>The <c>shell_detached_completed</c> variant of <see cref="SystemNotificationDataKind"/>.</summary>
public partial class SystemNotificationDataKindShellDetachedCompleted : SystemNotificationDataKind
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "shell_detached_completed";

    /// <summary>Unique identifier of the detached shell session.</summary>
    [JsonPropertyName("shellId")]
    public required string ShellId { get; set; }

    /// <summary>Human-readable description of the command.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("description")]
    public string? Description { get; set; }
}

/// <summary>Structured metadata identifying what triggered this notification.</summary>
/// <remarks>Polymorphic base type discriminated by <c>type</c>.</remarks>
[JsonPolymorphic(
    TypeDiscriminatorPropertyName = "type",
    UnknownDerivedTypeHandling = JsonUnknownDerivedTypeHandling.FallBackToBaseType)]
[JsonDerivedType(typeof(SystemNotificationDataKindAgentCompleted), "agent_completed")]
[JsonDerivedType(typeof(SystemNotificationDataKindAgentIdle), "agent_idle")]
[JsonDerivedType(typeof(SystemNotificationDataKindShellCompleted), "shell_completed")]
[JsonDerivedType(typeof(SystemNotificationDataKindShellDetachedCompleted), "shell_detached_completed")]
public partial class SystemNotificationDataKind
{
    /// <summary>The type discriminator.</summary>
    [JsonPropertyName("type")]
    public virtual string Type { get; set; } = string.Empty;
}


/// <summary>Nested data type for <c>PermissionRequestShellCommandsItem</c>.</summary>
public partial class PermissionRequestShellCommandsItem
{
    /// <summary>Command identifier (e.g., executable name).</summary>
    [JsonPropertyName("identifier")]
    public required string Identifier { get; set; }

    /// <summary>Whether this command is read-only (no side effects).</summary>
    [JsonPropertyName("readOnly")]
    public required bool ReadOnly { get; set; }
}

/// <summary>Nested data type for <c>PermissionRequestShellPossibleUrlsItem</c>.</summary>
public partial class PermissionRequestShellPossibleUrlsItem
{
    /// <summary>URL that may be accessed by the command.</summary>
    [JsonPropertyName("url")]
    public required string Url { get; set; }
}

/// <summary>Shell command permission request.</summary>
/// <remarks>The <c>shell</c> variant of <see cref="PermissionRequest"/>.</remarks>
public partial class PermissionRequestShell : PermissionRequest
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "shell";

    /// <summary>Tool call ID that triggered this permission request.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("toolCallId")]
    public string? ToolCallId { get; set; }

    /// <summary>The complete shell command text to be executed.</summary>
    [JsonPropertyName("fullCommandText")]
    public required string FullCommandText { get; set; }

    /// <summary>Human-readable description of what the command intends to do.</summary>
    [JsonPropertyName("intention")]
    public required string Intention { get; set; }

    /// <summary>Parsed command identifiers found in the command text.</summary>
    [JsonPropertyName("commands")]
    public required PermissionRequestShellCommandsItem[] Commands { get; set; }

    /// <summary>File paths that may be read or written by the command.</summary>
    [JsonPropertyName("possiblePaths")]
    public required string[] PossiblePaths { get; set; }

    /// <summary>URLs that may be accessed by the command.</summary>
    [JsonPropertyName("possibleUrls")]
    public required PermissionRequestShellPossibleUrlsItem[] PossibleUrls { get; set; }

    /// <summary>Whether the command includes a file write redirection (e.g., &gt; or &gt;&gt;).</summary>
    [JsonPropertyName("hasWriteFileRedirection")]
    public required bool HasWriteFileRedirection { get; set; }

    /// <summary>Whether the UI can offer session-wide approval for this command pattern.</summary>
    [JsonPropertyName("canOfferSessionApproval")]
    public required bool CanOfferSessionApproval { get; set; }

    /// <summary>Optional warning message about risks of running this command.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("warning")]
    public string? Warning { get; set; }
}

/// <summary>File write permission request.</summary>
/// <remarks>The <c>write</c> variant of <see cref="PermissionRequest"/>.</remarks>
public partial class PermissionRequestWrite : PermissionRequest
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "write";

    /// <summary>Tool call ID that triggered this permission request.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("toolCallId")]
    public string? ToolCallId { get; set; }

    /// <summary>Human-readable description of the intended file change.</summary>
    [JsonPropertyName("intention")]
    public required string Intention { get; set; }

    /// <summary>Path of the file being written to.</summary>
    [JsonPropertyName("fileName")]
    public required string FileName { get; set; }

    /// <summary>Unified diff showing the proposed changes.</summary>
    [JsonPropertyName("diff")]
    public required string Diff { get; set; }

    /// <summary>Complete new file contents for newly created files.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("newFileContents")]
    public string? NewFileContents { get; set; }
}

/// <summary>File or directory read permission request.</summary>
/// <remarks>The <c>read</c> variant of <see cref="PermissionRequest"/>.</remarks>
public partial class PermissionRequestRead : PermissionRequest
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "read";

    /// <summary>Tool call ID that triggered this permission request.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("toolCallId")]
    public string? ToolCallId { get; set; }

    /// <summary>Human-readable description of why the file is being read.</summary>
    [JsonPropertyName("intention")]
    public required string Intention { get; set; }

    /// <summary>Path of the file or directory being read.</summary>
    [JsonPropertyName("path")]
    public required string Path { get; set; }
}

/// <summary>MCP tool invocation permission request.</summary>
/// <remarks>The <c>mcp</c> variant of <see cref="PermissionRequest"/>.</remarks>
public partial class PermissionRequestMcp : PermissionRequest
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "mcp";

    /// <summary>Tool call ID that triggered this permission request.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("toolCallId")]
    public string? ToolCallId { get; set; }

    /// <summary>Name of the MCP server providing the tool.</summary>
    [JsonPropertyName("serverName")]
    public required string ServerName { get; set; }

    /// <summary>Internal name of the MCP tool.</summary>
    [JsonPropertyName("toolName")]
    public required string ToolName { get; set; }

    /// <summary>Human-readable title of the MCP tool.</summary>
    [JsonPropertyName("toolTitle")]
    public required string ToolTitle { get; set; }

    /// <summary>Arguments to pass to the MCP tool.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("args")]
    public object? Args { get; set; }

    /// <summary>Whether this MCP tool is read-only (no side effects).</summary>
    [JsonPropertyName("readOnly")]
    public required bool ReadOnly { get; set; }
}

/// <summary>URL access permission request.</summary>
/// <remarks>The <c>url</c> variant of <see cref="PermissionRequest"/>.</remarks>
public partial class PermissionRequestUrl : PermissionRequest
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "url";

    /// <summary>Tool call ID that triggered this permission request.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("toolCallId")]
    public string? ToolCallId { get; set; }

    /// <summary>Human-readable description of why the URL is being accessed.</summary>
    [JsonPropertyName("intention")]
    public required string Intention { get; set; }

    /// <summary>URL to be fetched.</summary>
    [JsonPropertyName("url")]
    public required string Url { get; set; }
}

/// <summary>Memory storage permission request.</summary>
/// <remarks>The <c>memory</c> variant of <see cref="PermissionRequest"/>.</remarks>
public partial class PermissionRequestMemory : PermissionRequest
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "memory";

    /// <summary>Tool call ID that triggered this permission request.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("toolCallId")]
    public string? ToolCallId { get; set; }

    /// <summary>Topic or subject of the memory being stored.</summary>
    [JsonPropertyName("subject")]
    public required string Subject { get; set; }

    /// <summary>The fact or convention being stored.</summary>
    [JsonPropertyName("fact")]
    public required string Fact { get; set; }

    /// <summary>Source references for the stored fact.</summary>
    [JsonPropertyName("citations")]
    public required string Citations { get; set; }
}

/// <summary>Custom tool invocation permission request.</summary>
/// <remarks>The <c>custom-tool</c> variant of <see cref="PermissionRequest"/>.</remarks>
public partial class PermissionRequestCustomTool : PermissionRequest
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "custom-tool";

    /// <summary>Tool call ID that triggered this permission request.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("toolCallId")]
    public string? ToolCallId { get; set; }

    /// <summary>Name of the custom tool.</summary>
    [JsonPropertyName("toolName")]
    public required string ToolName { get; set; }

    /// <summary>Description of what the custom tool does.</summary>
    [JsonPropertyName("toolDescription")]
    public required string ToolDescription { get; set; }

    /// <summary>Arguments to pass to the custom tool.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("args")]
    public object? Args { get; set; }
}

/// <summary>Hook confirmation permission request.</summary>
/// <remarks>The <c>hook</c> variant of <see cref="PermissionRequest"/>.</remarks>
public partial class PermissionRequestHook : PermissionRequest
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "hook";

    /// <summary>Tool call ID that triggered this permission request.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("toolCallId")]
    public string? ToolCallId { get; set; }

    /// <summary>Name of the tool the hook is gating.</summary>
    [JsonPropertyName("toolName")]
    public required string ToolName { get; set; }

    /// <summary>Arguments of the tool call being gated.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("toolArgs")]
    public object? ToolArgs { get; set; }

    /// <summary>Optional message from the hook explaining why confirmation is needed.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("hookMessage")]
    public string? HookMessage { get; set; }
}

/// <summary>Details of the permission being requested.</summary>
/// <remarks>Polymorphic base type discriminated by <c>kind</c>.</remarks>
[JsonPolymorphic(
    TypeDiscriminatorPropertyName = "kind",
    UnknownDerivedTypeHandling = JsonUnknownDerivedTypeHandling.FallBackToBaseType)]
[JsonDerivedType(typeof(PermissionRequestShell), "shell")]
[JsonDerivedType(typeof(PermissionRequestWrite), "write")]
[JsonDerivedType(typeof(PermissionRequestRead), "read")]
[JsonDerivedType(typeof(PermissionRequestMcp), "mcp")]
[JsonDerivedType(typeof(PermissionRequestUrl), "url")]
[JsonDerivedType(typeof(PermissionRequestMemory), "memory")]
[JsonDerivedType(typeof(PermissionRequestCustomTool), "custom-tool")]
[JsonDerivedType(typeof(PermissionRequestHook), "hook")]
public partial class PermissionRequest
{
    /// <summary>The type discriminator.</summary>
    [JsonPropertyName("kind")]
    public virtual string Kind { get; set; } = string.Empty;
}


/// <summary>The result of the permission request.</summary>
/// <remarks>Nested data type for <c>PermissionCompletedDataResult</c>.</remarks>
public partial class PermissionCompletedDataResult
{
    /// <summary>The outcome of the permission request.</summary>
    [JsonPropertyName("kind")]
    public required PermissionCompletedDataResultKind Kind { get; set; }
}

/// <summary>JSON Schema describing the form fields to present to the user (form mode only).</summary>
/// <remarks>Nested data type for <c>ElicitationRequestedDataRequestedSchema</c>.</remarks>
public partial class ElicitationRequestedDataRequestedSchema
{
    /// <summary>Schema type indicator (always 'object').</summary>
    [JsonPropertyName("type")]
    public required string Type { get; set; }

    /// <summary>Form field definitions, keyed by field name.</summary>
    [JsonPropertyName("properties")]
    public required Dictionary<string, object> Properties { get; set; }

    /// <summary>List of required field names.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("required")]
    public string[]? Required { get; set; }
}

/// <summary>Static OAuth client configuration, if the server specifies one.</summary>
/// <remarks>Nested data type for <c>McpOauthRequiredDataStaticClientConfig</c>.</remarks>
public partial class McpOauthRequiredDataStaticClientConfig
{
    /// <summary>OAuth client ID for the server.</summary>
    [JsonPropertyName("clientId")]
    public required string ClientId { get; set; }

    /// <summary>Whether this is a public OAuth client.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("publicClient")]
    public bool? PublicClient { get; set; }
}

/// <summary>Nested data type for <c>CommandsChangedDataCommandsItem</c>.</summary>
public partial class CommandsChangedDataCommandsItem
{
    /// <summary>Gets or sets the <c>name</c> value.</summary>
    [JsonPropertyName("name")]
    public required string Name { get; set; }

    /// <summary>Gets or sets the <c>description</c> value.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("description")]
    public string? Description { get; set; }
}

/// <summary>UI capability changes.</summary>
/// <remarks>Nested data type for <c>CapabilitiesChangedDataUi</c>.</remarks>
public partial class CapabilitiesChangedDataUi
{
    /// <summary>Whether elicitation is now supported.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("elicitation")]
    public bool? Elicitation { get; set; }
}

/// <summary>Nested data type for <c>SessionSkillsLoadedDataSkillsItem</c>.</summary>
public partial class SessionSkillsLoadedDataSkillsItem
{
    /// <summary>Unique identifier for the skill.</summary>
    [JsonPropertyName("name")]
    public required string Name { get; set; }

    /// <summary>Description of what the skill does.</summary>
    [JsonPropertyName("description")]
    public required string Description { get; set; }

    /// <summary>Source location type of the skill (e.g., project, personal, plugin).</summary>
    [JsonPropertyName("source")]
    public required string Source { get; set; }

    /// <summary>Whether the skill can be invoked by the user as a slash command.</summary>
    [JsonPropertyName("userInvocable")]
    public required bool UserInvocable { get; set; }

    /// <summary>Whether the skill is currently enabled.</summary>
    [JsonPropertyName("enabled")]
    public required bool Enabled { get; set; }

    /// <summary>Absolute path to the skill file, if available.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("path")]
    public string? Path { get; set; }
}

/// <summary>Nested data type for <c>SessionCustomAgentsUpdatedDataAgentsItem</c>.</summary>
public partial class SessionCustomAgentsUpdatedDataAgentsItem
{
    /// <summary>Unique identifier for the agent.</summary>
    [JsonPropertyName("id")]
    public required string Id { get; set; }

    /// <summary>Internal name of the agent.</summary>
    [JsonPropertyName("name")]
    public required string Name { get; set; }

    /// <summary>Human-readable display name.</summary>
    [JsonPropertyName("displayName")]
    public required string DisplayName { get; set; }

    /// <summary>Description of what the agent does.</summary>
    [JsonPropertyName("description")]
    public required string Description { get; set; }

    /// <summary>Source location: user, project, inherited, remote, or plugin.</summary>
    [JsonPropertyName("source")]
    public required string Source { get; set; }

    /// <summary>List of tool names available to this agent.</summary>
    [JsonPropertyName("tools")]
    public required string[] Tools { get; set; }

    /// <summary>Whether the agent can be selected by the user.</summary>
    [JsonPropertyName("userInvocable")]
    public required bool UserInvocable { get; set; }

    /// <summary>Model override for this agent, if set.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("model")]
    public string? Model { get; set; }
}

/// <summary>Nested data type for <c>SessionMcpServersLoadedDataServersItem</c>.</summary>
public partial class SessionMcpServersLoadedDataServersItem
{
    /// <summary>Server name (config key).</summary>
    [JsonPropertyName("name")]
    public required string Name { get; set; }

    /// <summary>Connection status: connected, failed, needs-auth, pending, disabled, or not_configured.</summary>
    [JsonPropertyName("status")]
    public required SessionMcpServersLoadedDataServersItemStatus Status { get; set; }

    /// <summary>Configuration source: user, workspace, plugin, or builtin.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("source")]
    public string? Source { get; set; }

    /// <summary>Error message if the server failed to connect.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("error")]
    public string? Error { get; set; }
}

/// <summary>Nested data type for <c>SessionExtensionsLoadedDataExtensionsItem</c>.</summary>
public partial class SessionExtensionsLoadedDataExtensionsItem
{
    /// <summary>Source-qualified extension ID (e.g., 'project:my-ext', 'user:auth-helper').</summary>
    [JsonPropertyName("id")]
    public required string Id { get; set; }

    /// <summary>Extension name (directory name).</summary>
    [JsonPropertyName("name")]
    public required string Name { get; set; }

    /// <summary>Discovery source.</summary>
    [JsonPropertyName("source")]
    public required SessionExtensionsLoadedDataExtensionsItemSource Source { get; set; }

    /// <summary>Current status: running, disabled, failed, or starting.</summary>
    [JsonPropertyName("status")]
    public required SessionExtensionsLoadedDataExtensionsItemStatus Status { get; set; }
}

/// <summary>Hosting platform type of the repository (github or ado).</summary>
[JsonConverter(typeof(JsonStringEnumConverter<SessionStartDataContextHostType>))]
public enum SessionStartDataContextHostType
{
    /// <summary>The <c>github</c> variant.</summary>
    [JsonStringEnumMemberName("github")]
    Github,
    /// <summary>The <c>ado</c> variant.</summary>
    [JsonStringEnumMemberName("ado")]
    Ado,
}

/// <summary>The type of operation performed on the plan file.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<SessionPlanChangedDataOperation>))]
public enum SessionPlanChangedDataOperation
{
    /// <summary>The <c>create</c> variant.</summary>
    [JsonStringEnumMemberName("create")]
    Create,
    /// <summary>The <c>update</c> variant.</summary>
    [JsonStringEnumMemberName("update")]
    Update,
    /// <summary>The <c>delete</c> variant.</summary>
    [JsonStringEnumMemberName("delete")]
    Delete,
}

/// <summary>Whether the file was newly created or updated.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<SessionWorkspaceFileChangedDataOperation>))]
public enum SessionWorkspaceFileChangedDataOperation
{
    /// <summary>The <c>create</c> variant.</summary>
    [JsonStringEnumMemberName("create")]
    Create,
    /// <summary>The <c>update</c> variant.</summary>
    [JsonStringEnumMemberName("update")]
    Update,
}

/// <summary>Origin type of the session being handed off.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<SessionHandoffDataSourceType>))]
public enum SessionHandoffDataSourceType
{
    /// <summary>The <c>remote</c> variant.</summary>
    [JsonStringEnumMemberName("remote")]
    Remote,
    /// <summary>The <c>local</c> variant.</summary>
    [JsonStringEnumMemberName("local")]
    Local,
}

/// <summary>Whether the session ended normally ("routine") or due to a crash/fatal error ("error").</summary>
[JsonConverter(typeof(JsonStringEnumConverter<SessionShutdownDataShutdownType>))]
public enum SessionShutdownDataShutdownType
{
    /// <summary>The <c>routine</c> variant.</summary>
    [JsonStringEnumMemberName("routine")]
    Routine,
    /// <summary>The <c>error</c> variant.</summary>
    [JsonStringEnumMemberName("error")]
    Error,
}

/// <summary>Type of GitHub reference.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<UserMessageDataAttachmentsItemGithubReferenceReferenceType>))]
public enum UserMessageDataAttachmentsItemGithubReferenceReferenceType
{
    /// <summary>The <c>issue</c> variant.</summary>
    [JsonStringEnumMemberName("issue")]
    Issue,
    /// <summary>The <c>pr</c> variant.</summary>
    [JsonStringEnumMemberName("pr")]
    Pr,
    /// <summary>The <c>discussion</c> variant.</summary>
    [JsonStringEnumMemberName("discussion")]
    Discussion,
}

/// <summary>The agent mode that was active when this message was sent.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<UserMessageDataAgentMode>))]
public enum UserMessageDataAgentMode
{
    /// <summary>The <c>interactive</c> variant.</summary>
    [JsonStringEnumMemberName("interactive")]
    Interactive,
    /// <summary>The <c>plan</c> variant.</summary>
    [JsonStringEnumMemberName("plan")]
    Plan,
    /// <summary>The <c>autopilot</c> variant.</summary>
    [JsonStringEnumMemberName("autopilot")]
    Autopilot,
    /// <summary>The <c>shell</c> variant.</summary>
    [JsonStringEnumMemberName("shell")]
    Shell,
}

/// <summary>Tool call type: "function" for standard tool calls, "custom" for grammar-based tool calls. Defaults to "function" when absent.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<AssistantMessageDataToolRequestsItemType>))]
public enum AssistantMessageDataToolRequestsItemType
{
    /// <summary>The <c>function</c> variant.</summary>
    [JsonStringEnumMemberName("function")]
    Function,
    /// <summary>The <c>custom</c> variant.</summary>
    [JsonStringEnumMemberName("custom")]
    Custom,
}

/// <summary>Theme variant this icon is intended for.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<ToolExecutionCompleteDataResultContentsItemResourceLinkIconsItemTheme>))]
public enum ToolExecutionCompleteDataResultContentsItemResourceLinkIconsItemTheme
{
    /// <summary>The <c>light</c> variant.</summary>
    [JsonStringEnumMemberName("light")]
    Light,
    /// <summary>The <c>dark</c> variant.</summary>
    [JsonStringEnumMemberName("dark")]
    Dark,
}

/// <summary>Message role: "system" for system prompts, "developer" for developer-injected instructions.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<SystemMessageDataRole>))]
public enum SystemMessageDataRole
{
    /// <summary>The <c>system</c> variant.</summary>
    [JsonStringEnumMemberName("system")]
    System,
    /// <summary>The <c>developer</c> variant.</summary>
    [JsonStringEnumMemberName("developer")]
    Developer,
}

/// <summary>Whether the agent completed successfully or failed.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<SystemNotificationDataKindAgentCompletedStatus>))]
public enum SystemNotificationDataKindAgentCompletedStatus
{
    /// <summary>The <c>completed</c> variant.</summary>
    [JsonStringEnumMemberName("completed")]
    Completed,
    /// <summary>The <c>failed</c> variant.</summary>
    [JsonStringEnumMemberName("failed")]
    Failed,
}

/// <summary>The outcome of the permission request.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<PermissionCompletedDataResultKind>))]
public enum PermissionCompletedDataResultKind
{
    /// <summary>The <c>approved</c> variant.</summary>
    [JsonStringEnumMemberName("approved")]
    Approved,
    /// <summary>The <c>denied-by-rules</c> variant.</summary>
    [JsonStringEnumMemberName("denied-by-rules")]
    DeniedByRules,
    /// <summary>The <c>denied-no-approval-rule-and-could-not-request-from-user</c> variant.</summary>
    [JsonStringEnumMemberName("denied-no-approval-rule-and-could-not-request-from-user")]
    DeniedNoApprovalRuleAndCouldNotRequestFromUser,
    /// <summary>The <c>denied-interactively-by-user</c> variant.</summary>
    [JsonStringEnumMemberName("denied-interactively-by-user")]
    DeniedInteractivelyByUser,
    /// <summary>The <c>denied-by-content-exclusion-policy</c> variant.</summary>
    [JsonStringEnumMemberName("denied-by-content-exclusion-policy")]
    DeniedByContentExclusionPolicy,
}

/// <summary>Elicitation mode; "form" for structured input, "url" for browser-based. Defaults to "form" when absent.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<ElicitationRequestedDataMode>))]
public enum ElicitationRequestedDataMode
{
    /// <summary>The <c>form</c> variant.</summary>
    [JsonStringEnumMemberName("form")]
    Form,
    /// <summary>The <c>url</c> variant.</summary>
    [JsonStringEnumMemberName("url")]
    Url,
}

/// <summary>Connection status: connected, failed, needs-auth, pending, disabled, or not_configured.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<SessionMcpServersLoadedDataServersItemStatus>))]
public enum SessionMcpServersLoadedDataServersItemStatus
{
    /// <summary>The <c>connected</c> variant.</summary>
    [JsonStringEnumMemberName("connected")]
    Connected,
    /// <summary>The <c>failed</c> variant.</summary>
    [JsonStringEnumMemberName("failed")]
    Failed,
    /// <summary>The <c>needs-auth</c> variant.</summary>
    [JsonStringEnumMemberName("needs-auth")]
    NeedsAuth,
    /// <summary>The <c>pending</c> variant.</summary>
    [JsonStringEnumMemberName("pending")]
    Pending,
    /// <summary>The <c>disabled</c> variant.</summary>
    [JsonStringEnumMemberName("disabled")]
    Disabled,
    /// <summary>The <c>not_configured</c> variant.</summary>
    [JsonStringEnumMemberName("not_configured")]
    NotConfigured,
}

/// <summary>Discovery source.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<SessionExtensionsLoadedDataExtensionsItemSource>))]
public enum SessionExtensionsLoadedDataExtensionsItemSource
{
    /// <summary>The <c>project</c> variant.</summary>
    [JsonStringEnumMemberName("project")]
    Project,
    /// <summary>The <c>user</c> variant.</summary>
    [JsonStringEnumMemberName("user")]
    User,
}

/// <summary>Current status: running, disabled, failed, or starting.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<SessionExtensionsLoadedDataExtensionsItemStatus>))]
public enum SessionExtensionsLoadedDataExtensionsItemStatus
{
    /// <summary>The <c>running</c> variant.</summary>
    [JsonStringEnumMemberName("running")]
    Running,
    /// <summary>The <c>disabled</c> variant.</summary>
    [JsonStringEnumMemberName("disabled")]
    Disabled,
    /// <summary>The <c>failed</c> variant.</summary>
    [JsonStringEnumMemberName("failed")]
    Failed,
    /// <summary>The <c>starting</c> variant.</summary>
    [JsonStringEnumMemberName("starting")]
    Starting,
}

[JsonSourceGenerationOptions(
    JsonSerializerDefaults.Web,
    AllowOutOfOrderMetadataProperties = true,
    NumberHandling = JsonNumberHandling.AllowReadingFromString,
    DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull)]
[JsonSerializable(typeof(AbortData))]
[JsonSerializable(typeof(AbortEvent))]
[JsonSerializable(typeof(AssistantIntentData))]
[JsonSerializable(typeof(AssistantIntentEvent))]
[JsonSerializable(typeof(AssistantMessageData))]
[JsonSerializable(typeof(AssistantMessageDataToolRequestsItem))]
[JsonSerializable(typeof(AssistantMessageDeltaData))]
[JsonSerializable(typeof(AssistantMessageDeltaEvent))]
[JsonSerializable(typeof(AssistantMessageEvent))]
[JsonSerializable(typeof(AssistantReasoningData))]
[JsonSerializable(typeof(AssistantReasoningDeltaData))]
[JsonSerializable(typeof(AssistantReasoningDeltaEvent))]
[JsonSerializable(typeof(AssistantReasoningEvent))]
[JsonSerializable(typeof(AssistantStreamingDeltaData))]
[JsonSerializable(typeof(AssistantStreamingDeltaEvent))]
[JsonSerializable(typeof(AssistantTurnEndData))]
[JsonSerializable(typeof(AssistantTurnEndEvent))]
[JsonSerializable(typeof(AssistantTurnStartData))]
[JsonSerializable(typeof(AssistantTurnStartEvent))]
[JsonSerializable(typeof(AssistantUsageData))]
[JsonSerializable(typeof(AssistantUsageDataCopilotUsage))]
[JsonSerializable(typeof(AssistantUsageDataCopilotUsageTokenDetailsItem))]
[JsonSerializable(typeof(AssistantUsageEvent))]
[JsonSerializable(typeof(CapabilitiesChangedData))]
[JsonSerializable(typeof(CapabilitiesChangedDataUi))]
[JsonSerializable(typeof(CapabilitiesChangedEvent))]
[JsonSerializable(typeof(CommandCompletedData))]
[JsonSerializable(typeof(CommandCompletedEvent))]
[JsonSerializable(typeof(CommandExecuteData))]
[JsonSerializable(typeof(CommandExecuteEvent))]
[JsonSerializable(typeof(CommandQueuedData))]
[JsonSerializable(typeof(CommandQueuedEvent))]
[JsonSerializable(typeof(CommandsChangedData))]
[JsonSerializable(typeof(CommandsChangedDataCommandsItem))]
[JsonSerializable(typeof(CommandsChangedEvent))]
[JsonSerializable(typeof(ElicitationCompletedData))]
[JsonSerializable(typeof(ElicitationCompletedEvent))]
[JsonSerializable(typeof(ElicitationRequestedData))]
[JsonSerializable(typeof(ElicitationRequestedDataRequestedSchema))]
[JsonSerializable(typeof(ElicitationRequestedEvent))]
[JsonSerializable(typeof(ExitPlanModeCompletedData))]
[JsonSerializable(typeof(ExitPlanModeCompletedEvent))]
[JsonSerializable(typeof(ExitPlanModeRequestedData))]
[JsonSerializable(typeof(ExitPlanModeRequestedEvent))]
[JsonSerializable(typeof(ExternalToolCompletedData))]
[JsonSerializable(typeof(ExternalToolCompletedEvent))]
[JsonSerializable(typeof(ExternalToolRequestedData))]
[JsonSerializable(typeof(ExternalToolRequestedEvent))]
[JsonSerializable(typeof(HookEndData))]
[JsonSerializable(typeof(HookEndDataError))]
[JsonSerializable(typeof(HookEndEvent))]
[JsonSerializable(typeof(HookStartData))]
[JsonSerializable(typeof(HookStartEvent))]
[JsonSerializable(typeof(McpOauthCompletedData))]
[JsonSerializable(typeof(McpOauthCompletedEvent))]
[JsonSerializable(typeof(McpOauthRequiredData))]
[JsonSerializable(typeof(McpOauthRequiredDataStaticClientConfig))]
[JsonSerializable(typeof(McpOauthRequiredEvent))]
[JsonSerializable(typeof(PendingMessagesModifiedData))]
[JsonSerializable(typeof(PendingMessagesModifiedEvent))]
[JsonSerializable(typeof(PermissionCompletedData))]
[JsonSerializable(typeof(PermissionCompletedDataResult))]
[JsonSerializable(typeof(PermissionCompletedEvent))]
[JsonSerializable(typeof(PermissionRequest))]
[JsonSerializable(typeof(PermissionRequestCustomTool))]
[JsonSerializable(typeof(PermissionRequestHook))]
[JsonSerializable(typeof(PermissionRequestMcp))]
[JsonSerializable(typeof(PermissionRequestMemory))]
[JsonSerializable(typeof(PermissionRequestRead))]
[JsonSerializable(typeof(PermissionRequestShell))]
[JsonSerializable(typeof(PermissionRequestShellCommandsItem))]
[JsonSerializable(typeof(PermissionRequestShellPossibleUrlsItem))]
[JsonSerializable(typeof(PermissionRequestUrl))]
[JsonSerializable(typeof(PermissionRequestWrite))]
[JsonSerializable(typeof(PermissionRequestedData))]
[JsonSerializable(typeof(PermissionRequestedEvent))]
[JsonSerializable(typeof(SamplingCompletedData))]
[JsonSerializable(typeof(SamplingCompletedEvent))]
[JsonSerializable(typeof(SamplingRequestedData))]
[JsonSerializable(typeof(SamplingRequestedEvent))]
[JsonSerializable(typeof(SessionBackgroundTasksChangedData))]
[JsonSerializable(typeof(SessionBackgroundTasksChangedEvent))]
[JsonSerializable(typeof(SessionCompactionCompleteData))]
[JsonSerializable(typeof(SessionCompactionCompleteDataCompactionTokensUsed))]
[JsonSerializable(typeof(SessionCompactionCompleteEvent))]
[JsonSerializable(typeof(SessionCompactionStartData))]
[JsonSerializable(typeof(SessionCompactionStartEvent))]
[JsonSerializable(typeof(SessionContextChangedData))]
[JsonSerializable(typeof(SessionContextChangedEvent))]
[JsonSerializable(typeof(SessionCustomAgentsUpdatedData))]
[JsonSerializable(typeof(SessionCustomAgentsUpdatedDataAgentsItem))]
[JsonSerializable(typeof(SessionCustomAgentsUpdatedEvent))]
[JsonSerializable(typeof(SessionErrorData))]
[JsonSerializable(typeof(SessionErrorEvent))]
[JsonSerializable(typeof(SessionEvent))]
[JsonSerializable(typeof(SessionExtensionsLoadedData))]
[JsonSerializable(typeof(SessionExtensionsLoadedDataExtensionsItem))]
[JsonSerializable(typeof(SessionExtensionsLoadedEvent))]
[JsonSerializable(typeof(SessionHandoffData))]
[JsonSerializable(typeof(SessionHandoffDataRepository))]
[JsonSerializable(typeof(SessionHandoffEvent))]
[JsonSerializable(typeof(SessionIdleData))]
[JsonSerializable(typeof(SessionIdleDataBackgroundTasks))]
[JsonSerializable(typeof(SessionIdleDataBackgroundTasksAgentsItem))]
[JsonSerializable(typeof(SessionIdleDataBackgroundTasksShellsItem))]
[JsonSerializable(typeof(SessionIdleEvent))]
[JsonSerializable(typeof(SessionInfoData))]
[JsonSerializable(typeof(SessionInfoEvent))]
[JsonSerializable(typeof(SessionMcpServerStatusChangedData))]
[JsonSerializable(typeof(SessionMcpServerStatusChangedEvent))]
[JsonSerializable(typeof(SessionMcpServersLoadedData))]
[JsonSerializable(typeof(SessionMcpServersLoadedDataServersItem))]
[JsonSerializable(typeof(SessionMcpServersLoadedEvent))]
[JsonSerializable(typeof(SessionModeChangedData))]
[JsonSerializable(typeof(SessionModeChangedEvent))]
[JsonSerializable(typeof(SessionModelChangeData))]
[JsonSerializable(typeof(SessionModelChangeEvent))]
[JsonSerializable(typeof(SessionPlanChangedData))]
[JsonSerializable(typeof(SessionPlanChangedEvent))]
[JsonSerializable(typeof(SessionRemoteSteerableChangedData))]
[JsonSerializable(typeof(SessionRemoteSteerableChangedEvent))]
[JsonSerializable(typeof(SessionResumeData))]
[JsonSerializable(typeof(SessionResumeDataContext))]
[JsonSerializable(typeof(SessionResumeEvent))]
[JsonSerializable(typeof(SessionShutdownData))]
[JsonSerializable(typeof(SessionShutdownDataCodeChanges))]
[JsonSerializable(typeof(SessionShutdownEvent))]
[JsonSerializable(typeof(SessionSkillsLoadedData))]
[JsonSerializable(typeof(SessionSkillsLoadedDataSkillsItem))]
[JsonSerializable(typeof(SessionSkillsLoadedEvent))]
[JsonSerializable(typeof(SessionSnapshotRewindData))]
[JsonSerializable(typeof(SessionSnapshotRewindEvent))]
[JsonSerializable(typeof(SessionStartData))]
[JsonSerializable(typeof(SessionStartDataContext))]
[JsonSerializable(typeof(SessionStartEvent))]
[JsonSerializable(typeof(SessionTaskCompleteData))]
[JsonSerializable(typeof(SessionTaskCompleteEvent))]
[JsonSerializable(typeof(SessionTitleChangedData))]
[JsonSerializable(typeof(SessionTitleChangedEvent))]
[JsonSerializable(typeof(SessionToolsUpdatedData))]
[JsonSerializable(typeof(SessionToolsUpdatedEvent))]
[JsonSerializable(typeof(SessionTruncationData))]
[JsonSerializable(typeof(SessionTruncationEvent))]
[JsonSerializable(typeof(SessionUsageInfoData))]
[JsonSerializable(typeof(SessionUsageInfoEvent))]
[JsonSerializable(typeof(SessionWarningData))]
[JsonSerializable(typeof(SessionWarningEvent))]
[JsonSerializable(typeof(SessionWorkspaceFileChangedData))]
[JsonSerializable(typeof(SessionWorkspaceFileChangedEvent))]
[JsonSerializable(typeof(SkillInvokedData))]
[JsonSerializable(typeof(SkillInvokedEvent))]
[JsonSerializable(typeof(SubagentCompletedData))]
[JsonSerializable(typeof(SubagentCompletedEvent))]
[JsonSerializable(typeof(SubagentDeselectedData))]
[JsonSerializable(typeof(SubagentDeselectedEvent))]
[JsonSerializable(typeof(SubagentFailedData))]
[JsonSerializable(typeof(SubagentFailedEvent))]
[JsonSerializable(typeof(SubagentSelectedData))]
[JsonSerializable(typeof(SubagentSelectedEvent))]
[JsonSerializable(typeof(SubagentStartedData))]
[JsonSerializable(typeof(SubagentStartedEvent))]
[JsonSerializable(typeof(SystemMessageData))]
[JsonSerializable(typeof(SystemMessageDataMetadata))]
[JsonSerializable(typeof(SystemMessageEvent))]
[JsonSerializable(typeof(SystemNotificationData))]
[JsonSerializable(typeof(SystemNotificationDataKind))]
[JsonSerializable(typeof(SystemNotificationDataKindAgentCompleted))]
[JsonSerializable(typeof(SystemNotificationDataKindAgentIdle))]
[JsonSerializable(typeof(SystemNotificationDataKindShellCompleted))]
[JsonSerializable(typeof(SystemNotificationDataKindShellDetachedCompleted))]
[JsonSerializable(typeof(SystemNotificationEvent))]
[JsonSerializable(typeof(ToolExecutionCompleteData))]
[JsonSerializable(typeof(ToolExecutionCompleteDataError))]
[JsonSerializable(typeof(ToolExecutionCompleteDataResult))]
[JsonSerializable(typeof(ToolExecutionCompleteDataResultContentsItem))]
[JsonSerializable(typeof(ToolExecutionCompleteDataResultContentsItemAudio))]
[JsonSerializable(typeof(ToolExecutionCompleteDataResultContentsItemImage))]
[JsonSerializable(typeof(ToolExecutionCompleteDataResultContentsItemResource))]
[JsonSerializable(typeof(ToolExecutionCompleteDataResultContentsItemResourceLink))]
[JsonSerializable(typeof(ToolExecutionCompleteDataResultContentsItemResourceLinkIconsItem))]
[JsonSerializable(typeof(ToolExecutionCompleteDataResultContentsItemTerminal))]
[JsonSerializable(typeof(ToolExecutionCompleteDataResultContentsItemText))]
[JsonSerializable(typeof(ToolExecutionCompleteEvent))]
[JsonSerializable(typeof(ToolExecutionPartialResultData))]
[JsonSerializable(typeof(ToolExecutionPartialResultEvent))]
[JsonSerializable(typeof(ToolExecutionProgressData))]
[JsonSerializable(typeof(ToolExecutionProgressEvent))]
[JsonSerializable(typeof(ToolExecutionStartData))]
[JsonSerializable(typeof(ToolExecutionStartEvent))]
[JsonSerializable(typeof(ToolUserRequestedData))]
[JsonSerializable(typeof(ToolUserRequestedEvent))]
[JsonSerializable(typeof(UserInputCompletedData))]
[JsonSerializable(typeof(UserInputCompletedEvent))]
[JsonSerializable(typeof(UserInputRequestedData))]
[JsonSerializable(typeof(UserInputRequestedEvent))]
[JsonSerializable(typeof(UserMessageData))]
[JsonSerializable(typeof(UserMessageDataAttachmentsItem))]
[JsonSerializable(typeof(UserMessageDataAttachmentsItemBlob))]
[JsonSerializable(typeof(UserMessageDataAttachmentsItemDirectory))]
[JsonSerializable(typeof(UserMessageDataAttachmentsItemFile))]
[JsonSerializable(typeof(UserMessageDataAttachmentsItemFileLineRange))]
[JsonSerializable(typeof(UserMessageDataAttachmentsItemGithubReference))]
[JsonSerializable(typeof(UserMessageDataAttachmentsItemSelection))]
[JsonSerializable(typeof(UserMessageDataAttachmentsItemSelectionSelection))]
[JsonSerializable(typeof(UserMessageDataAttachmentsItemSelectionSelectionEnd))]
[JsonSerializable(typeof(UserMessageDataAttachmentsItemSelectionSelectionStart))]
[JsonSerializable(typeof(UserMessageEvent))]
[JsonSerializable(typeof(JsonElement))]
internal partial class SessionEventsJsonContext : JsonSerializerContext;