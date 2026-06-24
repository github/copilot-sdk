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
 * Session event "session.schedule_created". Scheduled prompt registered via /every or /after
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
        /** Interval between ticks in milliseconds (relative-interval schedules) */
        @JsonProperty("intervalMs") Long intervalMs,
        /** 5-field cron expression for a recurring calendar schedule, evaluated in `tz` */
        @JsonProperty("cron") String cron,
        /** IANA timezone the `cron` expression is evaluated in */
        @JsonProperty("tz") String tz,
        /** Absolute fire time (epoch milliseconds) for a one-shot calendar schedule */
        @JsonProperty("at") Long at,
        /** Prompt text that gets enqueued on every tick */
        @JsonProperty("prompt") String prompt,
        /** Whether the schedule re-arms after each tick (`/every`) or fires once (`/after`) */
        @JsonProperty("recurring") Boolean recurring,
        /** Optional user-facing label shown in the timeline instead of the actual prompt (e.g. `/skill-name args` when the prompt is a skill invocation expansion) */
        @JsonProperty("displayPrompt") String displayPrompt
    ) {
    }
}
