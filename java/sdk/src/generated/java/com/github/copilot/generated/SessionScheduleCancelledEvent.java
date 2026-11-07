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
 * Session event "session.schedule_cancelled". Scheduled prompt cancelled from the schedule manager dialog
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionScheduleCancelledEvent extends SessionEvent {

    @Override
    public String getType() { return "session.schedule_cancelled"; }

    @JsonProperty("data")
    private SessionScheduleCancelledEventData data;

    public SessionScheduleCancelledEventData getData() { return data; }
    public void setData(SessionScheduleCancelledEventData data) { this.data = data; }

    /** Data payload for {@link SessionScheduleCancelledEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionScheduleCancelledEventData(
        /** Id of the scheduled prompt that was cancelled */
        @JsonProperty("id") Long id
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionScheduleCancelledEventData {}

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
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionScheduleCancelledEventData build() {
                if (id == null) throw new IllegalStateException("Missing required property: id");
                return new SessionScheduleCancelledEventData(id);
            }
        }
    }
}
