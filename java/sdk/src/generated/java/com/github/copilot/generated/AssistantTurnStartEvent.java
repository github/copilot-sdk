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
 * Session event "assistant.turn_start". Turn initialization metadata including identifier and interaction tracking
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantTurnStartEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.turn_start"; }

    @JsonProperty("data")
    private AssistantTurnStartEventData data;

    public AssistantTurnStartEventData getData() { return data; }
    public void setData(AssistantTurnStartEventData data) { this.data = data; }

    /** Data payload for {@link AssistantTurnStartEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantTurnStartEventData(
        /** Identifier for this turn within the agentic loop, typically a stringified turn number */
        @JsonProperty("turnId") String turnId,
        /** Model identifier used for this turn, when known */
        @JsonProperty("model") String model,
        /** CAPI interaction ID for correlating this turn with upstream telemetry */
        @JsonProperty("interactionId") String interactionId,
        /** Parent task tool call ID when this turn belongs to a sub-agent */
        @JsonProperty("parentToolCallId") String parentToolCallId,
        /** Agent mode whose tools this turn's run was offered. Absent on historical events and sub-agent turns. */
        @JsonProperty("agentMode") SessionMode agentMode
    ) {

        /**
         * Retains the constructor published in Java SDK 1.0.28.
         * @deprecated Use named construction via {@link #builder()}.
         * @param turnId Identifier for this turn within the agentic loop, typically a stringified turn number
         * @param model Model identifier used for this turn, when known
         * @param interactionId CAPI interaction ID for correlating this turn with upstream telemetry
         * @param parentToolCallId Parent task tool call ID when this turn belongs to a sub-agent
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AssistantTurnStartEventData(
            String turnId,
            String model,
            String interactionId,
            String parentToolCallId
        ) {
            this(turnId, model, interactionId, parentToolCallId, null);
        }

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AssistantTurnStartEventData {}

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
            private String interactionId;
            private String parentToolCallId;
            private SessionMode agentMode;

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
             * Sets the {@code interactionId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder interactionId(String value) {
                this.interactionId = value;
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
             * Sets the {@code agentMode} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder agentMode(SessionMode value) {
                this.agentMode = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public AssistantTurnStartEventData build() {
                if (turnId == null) throw new IllegalStateException("Missing required property: turnId");
                return new AssistantTurnStartEventData(turnId, model, interactionId, parentToolCallId, agentMode);
            }
        }
    }
}
