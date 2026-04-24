/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.annotation.processing.Generated;

/**
 * Base class for all generated session events.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true, defaultImpl = UnknownSessionEvent.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SessionStartEvent.class, name = "session.start"),
    @JsonSubTypes.Type(value = SessionResumeEvent.class, name = "session.resume"),
    @JsonSubTypes.Type(value = SessionRemoteSteerableChangedEvent.class, name = "session.remote_steerable_changed"),
    @JsonSubTypes.Type(value = SessionErrorEvent.class, name = "session.error"),
    @JsonSubTypes.Type(value = SessionIdleEvent.class, name = "session.idle"),
    @JsonSubTypes.Type(value = SessionTitleChangedEvent.class, name = "session.title_changed"),
    @JsonSubTypes.Type(value = SessionInfoEvent.class, name = "session.info"),
    @JsonSubTypes.Type(value = SessionWarningEvent.class, name = "session.warning"),
    @JsonSubTypes.Type(value = SessionModelChangeEvent.class, name = "session.model_change"),
    @JsonSubTypes.Type(value = SessionModeChangedEvent.class, name = "session.mode_changed"),
    @JsonSubTypes.Type(value = SessionPlanChangedEvent.class, name = "session.plan_changed"),
    @JsonSubTypes.Type(value = SessionWorkspaceFileChangedEvent.class, name = "session.workspace_file_changed"),
    @JsonSubTypes.Type(value = SessionHandoffEvent.class, name = "session.handoff"),
    @JsonSubTypes.Type(value = SessionTruncationEvent.class, name = "session.truncation"),
    @JsonSubTypes.Type(value = SessionSnapshotRewindEvent.class, name = "session.snapshot_rewind"),
    @JsonSubTypes.Type(value = SessionShutdownEvent.class, name = "session.shutdown"),
    @JsonSubTypes.Type(value = SessionContextChangedEvent.class, name = "session.context_changed"),
    @JsonSubTypes.Type(value = SessionUsageInfoEvent.class, name = "session.usage_info"),
    @JsonSubTypes.Type(value = SessionCompactionStartEvent.class, name = "session.compaction_start"),
    @JsonSubTypes.Type(value = SessionCompactionCompleteEvent.class, name = "session.compaction_complete"),
    @JsonSubTypes.Type(value = SessionTaskCompleteEvent.class, name = "session.task_complete"),
    @JsonSubTypes.Type(value = UserMessageEvent.class, name = "user.message"),
    @JsonSubTypes.Type(value = PendingMessagesModifiedEvent.class, name = "pending_messages.modified"),
    @JsonSubTypes.Type(value = AssistantTurnStartEvent.class, name = "assistant.turn_start"),
    @JsonSubTypes.Type(value = AssistantIntentEvent.class, name = "assistant.intent"),
    @JsonSubTypes.Type(value = AssistantReasoningEvent.class, name = "assistant.reasoning"),
    @JsonSubTypes.Type(value = AssistantReasoningDeltaEvent.class, name = "assistant.reasoning_delta"),
    @JsonSubTypes.Type(value = AssistantStreamingDeltaEvent.class, name = "assistant.streaming_delta"),
    @JsonSubTypes.Type(value = AssistantMessageEvent.class, name = "assistant.message"),
    @JsonSubTypes.Type(value = AssistantMessageDeltaEvent.class, name = "assistant.message_delta"),
    @JsonSubTypes.Type(value = AssistantTurnEndEvent.class, name = "assistant.turn_end"),
    @JsonSubTypes.Type(value = AssistantUsageEvent.class, name = "assistant.usage"),
    @JsonSubTypes.Type(value = AbortEvent.class, name = "abort"),
    @JsonSubTypes.Type(value = ToolUserRequestedEvent.class, name = "tool.user_requested"),
    @JsonSubTypes.Type(value = ToolExecutionStartEvent.class, name = "tool.execution_start"),
    @JsonSubTypes.Type(value = ToolExecutionPartialResultEvent.class, name = "tool.execution_partial_result"),
    @JsonSubTypes.Type(value = ToolExecutionProgressEvent.class, name = "tool.execution_progress"),
    @JsonSubTypes.Type(value = ToolExecutionCompleteEvent.class, name = "tool.execution_complete"),
    @JsonSubTypes.Type(value = SkillInvokedEvent.class, name = "skill.invoked"),
    @JsonSubTypes.Type(value = SubagentStartedEvent.class, name = "subagent.started"),
    @JsonSubTypes.Type(value = SubagentCompletedEvent.class, name = "subagent.completed"),
    @JsonSubTypes.Type(value = SubagentFailedEvent.class, name = "subagent.failed"),
    @JsonSubTypes.Type(value = SubagentSelectedEvent.class, name = "subagent.selected"),
    @JsonSubTypes.Type(value = SubagentDeselectedEvent.class, name = "subagent.deselected"),
    @JsonSubTypes.Type(value = HookStartEvent.class, name = "hook.start"),
    @JsonSubTypes.Type(value = HookEndEvent.class, name = "hook.end"),
    @JsonSubTypes.Type(value = SystemMessageEvent.class, name = "system.message"),
    @JsonSubTypes.Type(value = SystemNotificationEvent.class, name = "system.notification"),
    @JsonSubTypes.Type(value = PermissionRequestedEvent.class, name = "permission.requested"),
    @JsonSubTypes.Type(value = PermissionCompletedEvent.class, name = "permission.completed"),
    @JsonSubTypes.Type(value = UserInputRequestedEvent.class, name = "user_input.requested"),
    @JsonSubTypes.Type(value = UserInputCompletedEvent.class, name = "user_input.completed"),
    @JsonSubTypes.Type(value = ElicitationRequestedEvent.class, name = "elicitation.requested"),
    @JsonSubTypes.Type(value = ElicitationCompletedEvent.class, name = "elicitation.completed"),
    @JsonSubTypes.Type(value = SamplingRequestedEvent.class, name = "sampling.requested"),
    @JsonSubTypes.Type(value = SamplingCompletedEvent.class, name = "sampling.completed"),
    @JsonSubTypes.Type(value = McpOauthRequiredEvent.class, name = "mcp.oauth_required"),
    @JsonSubTypes.Type(value = McpOauthCompletedEvent.class, name = "mcp.oauth_completed"),
    @JsonSubTypes.Type(value = ExternalToolRequestedEvent.class, name = "external_tool.requested"),
    @JsonSubTypes.Type(value = ExternalToolCompletedEvent.class, name = "external_tool.completed"),
    @JsonSubTypes.Type(value = CommandQueuedEvent.class, name = "command.queued"),
    @JsonSubTypes.Type(value = CommandExecuteEvent.class, name = "command.execute"),
    @JsonSubTypes.Type(value = CommandCompletedEvent.class, name = "command.completed"),
    @JsonSubTypes.Type(value = AutoModeSwitchRequestedEvent.class, name = "auto_mode_switch.requested"),
    @JsonSubTypes.Type(value = AutoModeSwitchCompletedEvent.class, name = "auto_mode_switch.completed"),
    @JsonSubTypes.Type(value = CommandsChangedEvent.class, name = "commands.changed"),
    @JsonSubTypes.Type(value = CapabilitiesChangedEvent.class, name = "capabilities.changed"),
    @JsonSubTypes.Type(value = ExitPlanModeRequestedEvent.class, name = "exit_plan_mode.requested"),
    @JsonSubTypes.Type(value = ExitPlanModeCompletedEvent.class, name = "exit_plan_mode.completed"),
    @JsonSubTypes.Type(value = SessionToolsUpdatedEvent.class, name = "session.tools_updated"),
    @JsonSubTypes.Type(value = SessionBackgroundTasksChangedEvent.class, name = "session.background_tasks_changed"),
    @JsonSubTypes.Type(value = SessionSkillsLoadedEvent.class, name = "session.skills_loaded"),
    @JsonSubTypes.Type(value = SessionCustomAgentsUpdatedEvent.class, name = "session.custom_agents_updated"),
    @JsonSubTypes.Type(value = SessionMcpServersLoadedEvent.class, name = "session.mcp_servers_loaded"),
    @JsonSubTypes.Type(value = SessionMcpServerStatusChangedEvent.class, name = "session.mcp_server_status_changed"),
    @JsonSubTypes.Type(value = SessionExtensionsLoadedEvent.class, name = "session.extensions_loaded")
})
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract sealed class SessionEvent permits
        SessionStartEvent,
        SessionResumeEvent,
        SessionRemoteSteerableChangedEvent,
        SessionErrorEvent,
        SessionIdleEvent,
        SessionTitleChangedEvent,
        SessionInfoEvent,
        SessionWarningEvent,
        SessionModelChangeEvent,
        SessionModeChangedEvent,
        SessionPlanChangedEvent,
        SessionWorkspaceFileChangedEvent,
        SessionHandoffEvent,
        SessionTruncationEvent,
        SessionSnapshotRewindEvent,
        SessionShutdownEvent,
        SessionContextChangedEvent,
        SessionUsageInfoEvent,
        SessionCompactionStartEvent,
        SessionCompactionCompleteEvent,
        SessionTaskCompleteEvent,
        UserMessageEvent,
        PendingMessagesModifiedEvent,
        AssistantTurnStartEvent,
        AssistantIntentEvent,
        AssistantReasoningEvent,
        AssistantReasoningDeltaEvent,
        AssistantStreamingDeltaEvent,
        AssistantMessageEvent,
        AssistantMessageDeltaEvent,
        AssistantTurnEndEvent,
        AssistantUsageEvent,
        AbortEvent,
        ToolUserRequestedEvent,
        ToolExecutionStartEvent,
        ToolExecutionPartialResultEvent,
        ToolExecutionProgressEvent,
        ToolExecutionCompleteEvent,
        SkillInvokedEvent,
        SubagentStartedEvent,
        SubagentCompletedEvent,
        SubagentFailedEvent,
        SubagentSelectedEvent,
        SubagentDeselectedEvent,
        HookStartEvent,
        HookEndEvent,
        SystemMessageEvent,
        SystemNotificationEvent,
        PermissionRequestedEvent,
        PermissionCompletedEvent,
        UserInputRequestedEvent,
        UserInputCompletedEvent,
        ElicitationRequestedEvent,
        ElicitationCompletedEvent,
        SamplingRequestedEvent,
        SamplingCompletedEvent,
        McpOauthRequiredEvent,
        McpOauthCompletedEvent,
        ExternalToolRequestedEvent,
        ExternalToolCompletedEvent,
        CommandQueuedEvent,
        CommandExecuteEvent,
        CommandCompletedEvent,
        AutoModeSwitchRequestedEvent,
        AutoModeSwitchCompletedEvent,
        CommandsChangedEvent,
        CapabilitiesChangedEvent,
        ExitPlanModeRequestedEvent,
        ExitPlanModeCompletedEvent,
        SessionToolsUpdatedEvent,
        SessionBackgroundTasksChangedEvent,
        SessionSkillsLoadedEvent,
        SessionCustomAgentsUpdatedEvent,
        SessionMcpServersLoadedEvent,
        SessionMcpServerStatusChangedEvent,
        SessionExtensionsLoadedEvent,
        UnknownSessionEvent {

    /** Unique event identifier (UUID v4), generated when the event is emitted. */
    @JsonProperty("id")
    private UUID id;

    /** ISO 8601 timestamp when the event was created. */
    @JsonProperty("timestamp")
    private OffsetDateTime timestamp;

    /** ID of the chronologically preceding event in the session. Null for the first event. */
    @JsonProperty("parentId")
    private UUID parentId;

    /** When true, the event is transient and not persisted to the session event log on disk. */
    @JsonProperty("ephemeral")
    private Boolean ephemeral;

    /**
     * Returns the event-type discriminator string (e.g., {@code "session.idle"}).
     *
     * @return the event type
     */
    public abstract String getType();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }

    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }

    public Boolean getEphemeral() { return ephemeral; }
    public void setEphemeral(Boolean ephemeral) { this.ephemeral = ephemeral; }
}
