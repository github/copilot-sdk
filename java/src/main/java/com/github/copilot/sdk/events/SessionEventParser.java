/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Parser for session events that handles polymorphic deserialization.
 * <p>
 * This class deserializes JSON event data into the appropriate
 * {@link AbstractSessionEvent} subclass based on the "type" field. It is used
 * internally by the SDK to convert server events to Java objects.
 *
 * <h2>Supported Event Types</h2>
 * <ul>
 * <li><strong>Session</strong>: session.start, session.resume, session.error,
 * session.idle, session.info, etc.</li>
 * <li><strong>Assistant</strong>: assistant.message, assistant.message_delta,
 * assistant.turn_start, assistant.turn_end, etc.</li>
 * <li><strong>Tool</strong>: tool.execution_start, tool.execution_complete,
 * etc.</li>
 * <li><strong>User</strong>: user.message, pending_messages.modified</li>
 * <li><strong>Subagent</strong>: subagent.started, subagent.completed,
 * etc.</li>
 * </ul>
 *
 * @see AbstractSessionEvent
 * @since 1.0.0
 */
public class SessionEventParser {

    private static final Logger LOG = Logger.getLogger(SessionEventParser.class.getName());
    private static final ObjectMapper MAPPER;
    private static final Map<String, Class<? extends AbstractSessionEvent>> TYPE_MAP = new HashMap<>();

    static {
        MAPPER = new ObjectMapper();
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        TYPE_MAP.put("session.start", SessionStartEvent.class);
        TYPE_MAP.put("session.resume", SessionResumeEvent.class);
        TYPE_MAP.put("session.error", SessionErrorEvent.class);
        TYPE_MAP.put("session.idle", SessionIdleEvent.class);
        TYPE_MAP.put("session.info", SessionInfoEvent.class);
        TYPE_MAP.put("session.model_change", SessionModelChangeEvent.class);
        TYPE_MAP.put("session.mode_changed", SessionModeChangedEvent.class);
        TYPE_MAP.put("session.plan_changed", SessionPlanChangedEvent.class);
        TYPE_MAP.put("session.workspace_file_changed", SessionWorkspaceFileChangedEvent.class);
        TYPE_MAP.put("session.handoff", SessionHandoffEvent.class);
        TYPE_MAP.put("session.truncation", SessionTruncationEvent.class);
        TYPE_MAP.put("session.snapshot_rewind", SessionSnapshotRewindEvent.class);
        TYPE_MAP.put("session.usage_info", SessionUsageInfoEvent.class);
        TYPE_MAP.put("session.compaction_start", SessionCompactionStartEvent.class);
        TYPE_MAP.put("session.compaction_complete", SessionCompactionCompleteEvent.class);
        TYPE_MAP.put("session.context_changed", SessionContextChangedEvent.class);
        TYPE_MAP.put("user.message", UserMessageEvent.class);
        TYPE_MAP.put("pending_messages.modified", PendingMessagesModifiedEvent.class);
        TYPE_MAP.put("assistant.turn_start", AssistantTurnStartEvent.class);
        TYPE_MAP.put("assistant.intent", AssistantIntentEvent.class);
        TYPE_MAP.put("assistant.reasoning", AssistantReasoningEvent.class);
        TYPE_MAP.put("assistant.reasoning_delta", AssistantReasoningDeltaEvent.class);
        TYPE_MAP.put("assistant.message", AssistantMessageEvent.class);
        TYPE_MAP.put("assistant.message_delta", AssistantMessageDeltaEvent.class);
        TYPE_MAP.put("assistant.turn_end", AssistantTurnEndEvent.class);
        TYPE_MAP.put("assistant.usage", AssistantUsageEvent.class);
        TYPE_MAP.put("abort", AbortEvent.class);
        TYPE_MAP.put("tool.user_requested", ToolUserRequestedEvent.class);
        TYPE_MAP.put("tool.execution_start", ToolExecutionStartEvent.class);
        TYPE_MAP.put("tool.execution_partial_result", ToolExecutionPartialResultEvent.class);
        TYPE_MAP.put("tool.execution_progress", ToolExecutionProgressEvent.class);
        TYPE_MAP.put("tool.execution_complete", ToolExecutionCompleteEvent.class);
        TYPE_MAP.put("subagent.started", SubagentStartedEvent.class);
        TYPE_MAP.put("subagent.completed", SubagentCompletedEvent.class);
        TYPE_MAP.put("subagent.failed", SubagentFailedEvent.class);
        TYPE_MAP.put("subagent.selected", SubagentSelectedEvent.class);
        TYPE_MAP.put("hook.start", HookStartEvent.class);
        TYPE_MAP.put("hook.end", HookEndEvent.class);
        TYPE_MAP.put("system.message", SystemMessageEvent.class);
        TYPE_MAP.put("session.shutdown", SessionShutdownEvent.class);
        TYPE_MAP.put("skill.invoked", SkillInvokedEvent.class);
    }

    /**
     * Parses a JsonNode into the appropriate SessionEvent subclass.
     *
     * @param node
     *            the JSON node representing an event
     * @return the parsed event, or {@code null} if parsing fails or type is unknown
     */
    public static AbstractSessionEvent parse(JsonNode node) {
        try {
            String type = node.has("type") ? node.get("type").asText() : null;

            if (type == null) {
                LOG.warning("Missing 'type' field in event");
                return null;
            }

            Class<? extends AbstractSessionEvent> eventClass = TYPE_MAP.get(type);
            if (eventClass == null) {
                LOG.fine("Unknown event type: " + type);
                return null;
            }

            return MAPPER.treeToValue(node, eventClass);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to parse session event", e);
            return null;
        }
    }
}
