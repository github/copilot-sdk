/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Schema for the `ScheduleEntry` type.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScheduleEntry(
    /** Sequential id assigned by the runtime within the session. Stable across resumes (rebuilt from the event log). */
    @JsonProperty("id") Long id,
    /** Interval between scheduled ticks, in milliseconds (relative-interval schedules). */
    @JsonProperty("intervalMs") Long intervalMs,
    /** 5-field cron expression for a recurring calendar schedule, evaluated in `tz`. */
    @JsonProperty("cron") String cron,
    /** IANA timezone the `cron` expression is evaluated in. */
    @JsonProperty("tz") String tz,
    /** Absolute fire time (epoch milliseconds) for a one-shot calendar schedule. */
    @JsonProperty("at") Long at,
    /** Prompt text that gets enqueued on every tick. */
    @JsonProperty("prompt") String prompt,
    /** Whether the schedule re-arms after each tick (`/every`) or fires once (`/after`). */
    @JsonProperty("recurring") Boolean recurring,
    /** True for a self-paced (`dynamic`) schedule: no fixed cadence; the model arms each next run via the `manage_schedule` `wakeup` action. `nextRunAt` is model-controlled. */
    @JsonProperty("selfPaced") Boolean selfPaced,
    /** Display-only label for the prompt as shown in the UI (e.g. `/skill-name` for a skill-invocation schedule). The actual enqueued prompt is `prompt`. */
    @JsonProperty("displayPrompt") String displayPrompt,
    /** ISO 8601 timestamp when the next tick is scheduled to fire. */
    @JsonProperty("nextRunAt") OffsetDateTime nextRunAt
) {
}
