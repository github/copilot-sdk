/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Base class for all session events in the Copilot SDK.
 * <p>
 * Session events represent all activities that occur during a Copilot
 * conversation, including messages from the user and assistant, tool
 * executions, and session state changes. Events are delivered to handlers
 * registered via
 * {@link com.github.copilot.sdk.CopilotSession#on(java.util.function.Consumer)}.
 *
 * <h2>Event Categories</h2>
 * <ul>
 * <li><strong>Session events</strong>: {@link SessionStartEvent},
 * {@link SessionResumeEvent}, {@link SessionErrorEvent},
 * {@link SessionIdleEvent}, etc.</li>
 * <li><strong>Assistant events</strong>: {@link AssistantMessageEvent},
 * {@link AssistantMessageDeltaEvent}, {@link AssistantTurnStartEvent},
 * etc.</li>
 * <li><strong>Tool events</strong>: {@link ToolExecutionStartEvent},
 * {@link ToolExecutionCompleteEvent}, etc.</li>
 * <li><strong>User events</strong>: {@link UserMessageEvent}</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * session.on(event -> {
 * 	if (event instanceof AssistantMessageEvent msg) {
 * 		System.out.println("Assistant: " + msg.getData().content());
 * 	} else if (event instanceof SessionIdleEvent) {
 * 		System.out.println("Session is idle");
 * 	}
 * });
 * }</pre>
 *
 * @see com.github.copilot.sdk.CopilotSession#on(java.util.function.Consumer)
 * @see SessionEventParser
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract sealed class AbstractSessionEvent permits
        // Session events
        SessionStartEvent, SessionResumeEvent, SessionErrorEvent, SessionIdleEvent, SessionInfoEvent,
        SessionModelChangeEvent, SessionHandoffEvent, SessionTruncationEvent, SessionSnapshotRewindEvent,
        SessionUsageInfoEvent, SessionCompactionStartEvent, SessionCompactionCompleteEvent, SessionShutdownEvent,
        // Assistant events
        AssistantTurnStartEvent, AssistantIntentEvent, AssistantReasoningEvent, AssistantReasoningDeltaEvent,
        AssistantMessageEvent, AssistantMessageDeltaEvent, AssistantTurnEndEvent, AssistantUsageEvent, AbortEvent,
        // Tool events
        ToolUserRequestedEvent, ToolExecutionStartEvent, ToolExecutionPartialResultEvent, ToolExecutionProgressEvent,
        ToolExecutionCompleteEvent,
        // User events
        UserMessageEvent, PendingMessagesModifiedEvent,
        // Skill events
        SkillInvokedEvent,
        // Other events
        SubagentStartedEvent, SubagentCompletedEvent, SubagentFailedEvent, SubagentSelectedEvent, HookStartEvent,
        HookEndEvent, SystemMessageEvent {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("timestamp")
    private OffsetDateTime timestamp;

    @JsonProperty("parentId")
    private UUID parentId;

    @JsonProperty("ephemeral")
    private Boolean ephemeral;

    /**
     * Gets the event type discriminator string.
     * <p>
     * This corresponds to the event type in the JSON protocol (e.g.,
     * "assistant.message", "session.idle").
     *
     * @return the event type string
     */
    public abstract String getType();

    /**
     * Gets the unique identifier for this event.
     *
     * @return the event UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the event identifier.
     *
     * @param id
     *            the event UUID
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the timestamp when this event occurred.
     *
     * @return the event timestamp
     */
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the event timestamp.
     *
     * @param timestamp
     *            the event timestamp
     */
    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the parent event ID, if this event is a child of another.
     *
     * @return the parent event UUID, or {@code null}
     */
    public UUID getParentId() {
        return parentId;
    }

    /**
     * Sets the parent event ID.
     *
     * @param parentId
     *            the parent event UUID
     */
    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    /**
     * Returns whether this is an ephemeral event.
     * <p>
     * Ephemeral events are not persisted in session history.
     *
     * @return {@code true} if ephemeral, {@code false} otherwise
     */
    public Boolean getEphemeral() {
        return ephemeral;
    }

    /**
     * Sets whether this is an ephemeral event.
     *
     * @param ephemeral
     *            {@code true} if ephemeral
     */
    public void setEphemeral(Boolean ephemeral) {
        this.ephemeral = ephemeral;
    }
}
