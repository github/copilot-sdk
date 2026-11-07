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
 * Session event "session_limits_exhausted.completed". Session limit exhaustion prompt completion notification.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionLimitsExhaustedCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "session_limits_exhausted.completed"; }

    @JsonProperty("data")
    private SessionLimitsExhaustedCompletedEventData data;

    public SessionLimitsExhaustedCompletedEventData getData() { return data; }
    public void setData(SessionLimitsExhaustedCompletedEventData data) { this.data = data; }

    /** Data payload for {@link SessionLimitsExhaustedCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionLimitsExhaustedCompletedEventData(
        /** Request ID of the resolved request; clients should dismiss any UI for this request. */
        @JsonProperty("requestId") String requestId,
        /** The user's selected session-limit action. */
        @JsonProperty("response") SessionLimitsExhaustedResponse response
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionLimitsExhaustedCompletedEventData {}

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
            private String requestId;
            private SessionLimitsExhaustedResponse response;

            /**
             * Sets the {@code requestId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestId(String value) {
                this.requestId = value;
                return this;
            }

            /**
             * Sets the {@code response} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder response(SessionLimitsExhaustedResponse value) {
                this.response = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionLimitsExhaustedCompletedEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                if (response == null) throw new IllegalStateException("Missing required property: response");
                return new SessionLimitsExhaustedCompletedEventData(requestId, response);
            }
        }
    }
}
