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
 * Session event "auto_mode_switch.requested". Auto mode switch request notification requiring user approval
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AutoModeSwitchRequestedEvent extends SessionEvent {

    @Override
    public String getType() { return "auto_mode_switch.requested"; }

    @JsonProperty("data")
    private AutoModeSwitchRequestedEventData data;

    public AutoModeSwitchRequestedEventData getData() { return data; }
    public void setData(AutoModeSwitchRequestedEventData data) { this.data = data; }

    /** Data payload for {@link AutoModeSwitchRequestedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AutoModeSwitchRequestedEventData(
        /** Unique identifier for this request; used to respond via session.respondToAutoModeSwitch() */
        @JsonProperty("requestId") String requestId,
        /** The rate limit error code that triggered this request */
        @JsonProperty("errorCode") String errorCode,
        /** Seconds until the rate limit resets, when known. Lets clients render a humanized reset time alongside the prompt. */
        @JsonProperty("retryAfterSeconds") Long retryAfterSeconds
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AutoModeSwitchRequestedEventData {}

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
            private String errorCode;
            private Long retryAfterSeconds;

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
             * Sets the {@code errorCode} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder errorCode(String value) {
                this.errorCode = value;
                return this;
            }

            /**
             * Sets the {@code retryAfterSeconds} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder retryAfterSeconds(Long value) {
                this.retryAfterSeconds = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public AutoModeSwitchRequestedEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                return new AutoModeSwitchRequestedEventData(requestId, errorCode, retryAfterSeconds);
            }
        }
    }
}
