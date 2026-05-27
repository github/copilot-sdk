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
 * Session event "session.autopilot_objective_changed". Autopilot objective state file operation details indicating what changed
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionAutopilotObjectiveChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.autopilot_objective_changed"; }

    @JsonProperty("data")
    private SessionAutopilotObjectiveChangedEventData data;

    public SessionAutopilotObjectiveChangedEventData getData() { return data; }
    public void setData(SessionAutopilotObjectiveChangedEventData data) { this.data = data; }

    /** Data payload for {@link SessionAutopilotObjectiveChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionAutopilotObjectiveChangedEventData(
        /** The type of operation performed on the autopilot objective state file */
        @JsonProperty("operation") AutopilotObjectiveChangedOperation operation,
        /** Current autopilot objective id, if one exists */
        @JsonProperty("id") Long id,
        /** Current autopilot objective status, if one exists */
        @JsonProperty("status") AutopilotObjectiveChangedStatus status
    ) {
    }
}
