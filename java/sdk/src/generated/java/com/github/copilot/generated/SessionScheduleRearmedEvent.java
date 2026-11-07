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

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionScheduleRearmedEventData {}

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
            private Long nextRunAt;

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
             * Sets the {@code nextRunAt} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder nextRunAt(Long value) {
                this.nextRunAt = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionScheduleRearmedEventData build() {
                if (id == null) throw new IllegalStateException("Missing required property: id");
                if (nextRunAt == null) throw new IllegalStateException("Missing required property: nextRunAt");
                return new SessionScheduleRearmedEventData(id, nextRunAt);
            }
        }
    }
}
