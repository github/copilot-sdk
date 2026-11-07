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
 * Session event "session_limits_exhausted.requested". Session limit exhaustion notification requiring user action.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionLimitsExhaustedRequestedEvent extends SessionEvent {

    @Override
    public String getType() { return "session_limits_exhausted.requested"; }

    @JsonProperty("data")
    private SessionLimitsExhaustedRequestedEventData data;

    public SessionLimitsExhaustedRequestedEventData getData() { return data; }
    public void setData(SessionLimitsExhaustedRequestedEventData data) { this.data = data; }

    /** Data payload for {@link SessionLimitsExhaustedRequestedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionLimitsExhaustedRequestedEventData(
        /** Unique identifier for this request; used to respond via session.ui.handlePendingSessionLimitsExhausted(). */
        @JsonProperty("requestId") String requestId,
        /** AI Credits already consumed in the current accounting window. */
        @JsonProperty("usedAiCredits") Double usedAiCredits,
        /** Configured max AI Credits for the current accounting window. */
        @JsonProperty("maxAiCredits") Double maxAiCredits
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionLimitsExhaustedRequestedEventData {}

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
            private Double usedAiCredits;
            private Double maxAiCredits;

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
             * Sets the {@code usedAiCredits} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder usedAiCredits(Double value) {
                this.usedAiCredits = value;
                return this;
            }

            /**
             * Sets the {@code maxAiCredits} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder maxAiCredits(Double value) {
                this.maxAiCredits = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionLimitsExhaustedRequestedEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                if (usedAiCredits == null) throw new IllegalStateException("Missing required property: usedAiCredits");
                if (maxAiCredits == null) throw new IllegalStateException("Missing required property: maxAiCredits");
                return new SessionLimitsExhaustedRequestedEventData(requestId, usedAiCredits, maxAiCredits);
            }
        }
    }
}
