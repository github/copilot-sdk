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
 * Scheduled prompt entry with ID, timing (`intervalMs`, `cron`, or `at`), prompt text, recurrence, and next run time.
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

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ScheduleEntry {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private Long id;
        private Long intervalMs;
        private String cron;
        private String tz;
        private Long at;
        private String prompt;
        private Boolean recurring;
        private Boolean selfPaced;
        private String displayPrompt;
        private OffsetDateTime nextRunAt;

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder id(Long value) {
            this.id = value;
            return this;
        }

        /**
         * Sets the {@code intervalMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder intervalMs(Long value) {
            this.intervalMs = value;
            return this;
        }

        /**
         * Sets the {@code cron} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cron(String value) {
            this.cron = value;
            return this;
        }

        /**
         * Sets the {@code tz} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder tz(String value) {
            this.tz = value;
            return this;
        }

        /**
         * Sets the {@code at} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder at(Long value) {
            this.at = value;
            return this;
        }

        /**
         * Sets the {@code prompt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder prompt(String value) {
            this.prompt = value;
            return this;
        }

        /**
         * Sets the {@code recurring} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder recurring(Boolean value) {
            this.recurring = value;
            return this;
        }

        /**
         * Sets the {@code selfPaced} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder selfPaced(Boolean value) {
            this.selfPaced = value;
            return this;
        }

        /**
         * Sets the {@code displayPrompt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder displayPrompt(String value) {
            this.displayPrompt = value;
            return this;
        }

        /**
         * Sets the {@code nextRunAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder nextRunAt(OffsetDateTime value) {
            this.nextRunAt = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ScheduleEntry build() {
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (prompt == null) throw new IllegalStateException("Missing required property: prompt");
            if (recurring == null) throw new IllegalStateException("Missing required property: recurring");
            if (nextRunAt == null) throw new IllegalStateException("Missing required property: nextRunAt");
            return new ScheduleEntry(id, intervalMs, cron, tz, at, prompt, recurring, selfPaced, displayPrompt, nextRunAt);
        }
    }
}
