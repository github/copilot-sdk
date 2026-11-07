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
 * Session event "assistant.turn_end". Turn completion metadata including the turn identifier
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantTurnEndEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.turn_end"; }

    @JsonProperty("data")
    private AssistantTurnEndEventData data;

    public AssistantTurnEndEventData getData() { return data; }
    public void setData(AssistantTurnEndEventData data) { this.data = data; }

    /** Data payload for {@link AssistantTurnEndEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantTurnEndEventData(
        /** Identifier of the turn that has ended, matching the corresponding assistant.turn_start event */
        @JsonProperty("turnId") String turnId,
        /** Model identifier used for this turn, when known */
        @JsonProperty("model") String model,
        /** Parent task tool call ID when this turn belongs to a sub-agent */
        @JsonProperty("parentToolCallId") String parentToolCallId
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AssistantTurnEndEventData {}

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
            private String parentToolCallId;

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
             * Sets the {@code parentToolCallId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder parentToolCallId(String value) {
                this.parentToolCallId = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public AssistantTurnEndEventData build() {
                if (turnId == null) throw new IllegalStateException("Missing required property: turnId");
                return new AssistantTurnEndEventData(turnId, model, parentToolCallId);
            }
        }
    }
}
