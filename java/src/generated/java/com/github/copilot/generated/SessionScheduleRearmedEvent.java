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
 * Session event "session.schedule_rearmed". Self-paced schedule re-armed for its next run
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionScheduleRearmedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.schedule_rearmed"; }

    @JsonProperty("data")
    private SessionScheduleRearmedEventData data;

    public SessionScheduleRearmedEventData getData() { return data; }
    public void setData(SessionScheduleRearmedEventData data) { this.data = data; }

    /** Data payload for {@link SessionScheduleRearmedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionScheduleRearmedEventData(
        /** Id of the self-paced schedule that was re-armed */
        @JsonProperty("id") Long id,
        /** Absolute time (epoch milliseconds) the model armed the next run to fire */
        @JsonProperty("nextRunAt") Long nextRunAt
    ) {
    }
}
