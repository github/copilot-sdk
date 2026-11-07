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
        /** True for a self-paced (`dynamic`) schedule: no fixed cadence; the model arms each next run via the `manage_schedule` `wakeup` action. `nextRunAt` is model-controlled rather than auto-computed. */
        @JsonProperty("selfPaced") Boolean selfPaced,
        /** Optional user-facing label shown in the timeline instead of the actual prompt (e.g. `/skill-name args` when the prompt is a skill invocation expansion) */
        @JsonProperty("displayPrompt") String displayPrompt,
        /** Who created the schedule (`user` or `model`). Persisted so a resumed session keeps gating non-user schedules from firing skills that opted out of model invocation. Absent on entries created before this field existed; a missing origin fails closed (treated the same as a non-user origin), so such a schedule may not resolve a `disable-model-invocation` skill. */
        @JsonProperty("origin") ScheduleOrigin origin
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionScheduleCreatedEventData {}

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
            private ScheduleOrigin origin;

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
             * Sets the {@code origin} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder origin(ScheduleOrigin value) {
                this.origin = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionScheduleCreatedEventData build() {
                if (id == null) throw new IllegalStateException("Missing required property: id");
                if (prompt == null) throw new IllegalStateException("Missing required property: prompt");
                return new SessionScheduleCreatedEventData(id, intervalMs, cron, tz, at, prompt, recurring, selfPaced, displayPrompt, origin);
            }
        }
    }
}
