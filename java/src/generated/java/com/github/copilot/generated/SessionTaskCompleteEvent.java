/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "session.task_complete". Task completion notification with summary from the agent
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionTaskCompleteEvent extends SessionEvent {

    @Override
    public String getType() { return "session.task_complete"; }

    @JsonProperty("data")
    private SessionTaskCompleteEventData data;

    public SessionTaskCompleteEventData getData() { return data; }
    public void setData(SessionTaskCompleteEventData data) { this.data = data; }

    /** Data payload for {@link SessionTaskCompleteEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionTaskCompleteEventData(
        /** Summary of the completed task, provided by the agent */
        @JsonProperty("summary") String summary,
        /** Whether the task was accepted as complete. False when validation failed or completion was rejected or blocked by the reviewer */
        @JsonProperty("success") Boolean success,
        /** Semantic completion decision. Absent on legacy events and invalid tool calls */
        @JsonProperty("outcome") TaskCompletionOutcome outcome,
        /** Label-safe runtime rationale for the completion decision (e.g. a cancellation or pause/resume downgrade), when one applies. Reviewer-authored rationale is intentionally omitted here because this event has no IFC label channel; the reviewer's findings remain available through its own labeled sub-agent events */
        @JsonProperty("reason") String reason,
        /** Active autopilot objective ID evaluated by the completion reviewer */
        @JsonProperty("objectiveId") Long objectiveId
    ) {
    }
}
