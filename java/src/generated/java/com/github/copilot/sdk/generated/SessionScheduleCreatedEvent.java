/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * The {@code session.schedule_created} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionScheduleCreatedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.schedule_created"; }

    @JsonProperty("data")
    private SessionScheduleCreatedEventData data;

    public SessionScheduleCreatedEventData getData() { return data; }
    public void setData(SessionScheduleCreatedEventData data) { this.data = data; }

    /** Data payload for {@link SessionScheduleCreatedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionScheduleCreatedEventData(
        /** Sequential id assigned to the scheduled prompt within the session */
        @JsonProperty("id") Long id,
        /** Interval between ticks in milliseconds */
        @JsonProperty("intervalMs") Long intervalMs,
        /** Prompt text that gets enqueued on every tick */
        @JsonProperty("prompt") String prompt,
        /** Whether the schedule re-arms after each tick (`/every`) or fires once (`/after`) */
        @JsonProperty("recurring") Boolean recurring
    ) {
    }
}
