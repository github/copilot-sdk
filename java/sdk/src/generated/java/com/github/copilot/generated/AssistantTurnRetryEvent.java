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
 * Session event "assistant.turn_retry". Metadata for an additional model inference attempt within an existing assistant turn
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantTurnRetryEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.turn_retry"; }

    @JsonProperty("data")
    private AssistantTurnRetryEventData data;

    public AssistantTurnRetryEventData getData() { return data; }
    public void setData(AssistantTurnRetryEventData data) { this.data = data; }

    /** Data payload for {@link AssistantTurnRetryEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantTurnRetryEventData(
        /** Identifier of the turn whose model inference is being retried */
        @JsonProperty("turnId") String turnId,
        /** Model identifier used for this retry, when known */
        @JsonProperty("model") String model,
        /** Provider or runtime classification that caused the retry, when known */
        @JsonProperty("reason") String reason
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AssistantTurnRetryEventData {}

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
            private String turnId;
            private String model;
            private String reason;

            /**
             * Sets the {@code turnId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder turnId(String value) {
                this.turnId = value;
                return this;
            }

            /**
             * Sets the {@code model} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder model(String value) {
                this.model = value;
                return this;
            }

            /**
             * Sets the {@code reason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reason(String value) {
                this.reason = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public AssistantTurnRetryEventData build() {
                if (turnId == null) throw new IllegalStateException("Missing required property: turnId");
                return new AssistantTurnRetryEventData(turnId, model, reason);
            }
        }
    }
}
