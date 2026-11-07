/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Session event "ui.ephemeral_query". Ordered output and terminal state for a transient query that does not modify conversation history.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class UiEphemeralQueryEvent extends SessionEvent {

    @Override
    public String getType() { return "ui.ephemeral_query"; }

    @JsonProperty("data")
    private UiEphemeralQueryEventData data;

    public UiEphemeralQueryEventData getData() { return data; }
    public void setData(UiEphemeralQueryEventData data) { this.data = data; }

    /** Data payload for {@link UiEphemeralQueryEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UiEphemeralQueryEventData(
        /** Runtime-minted query identifier. */
        @JsonProperty("requestId") String requestId,
        /** Current query lifecycle phase. */
        @JsonProperty("phase") UIEphemeralQueryPhase phase,
        /** Ordered text delta, present for the `chunk` phase. */
        @JsonProperty("chunk") String chunk,
        /** Full response text, present for the `completed` phase. */
        @JsonProperty("answer") String answer,
        /** Model or transport failure message, present for the `failed` phase. */
        @JsonProperty("error") String error
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public UiEphemeralQueryEventData {}

        /**
         * Returns a named builder.
         * @return a new builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public static Builder builder() { return new Builder(); }

        /**
         * Named construction for this record.
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public static final class Builder {
            private Builder() {}
            private String requestId;
            private UIEphemeralQueryPhase phase;
            private String chunk;
            private String answer;
            private String error;

            /**
             * Sets the {@code requestId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder requestId(String value) {
                this.requestId = value;
                return this;
            }

            /**
             * Sets the {@code phase} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder phase(UIEphemeralQueryPhase value) {
                this.phase = value;
                return this;
            }

            /**
             * Sets the {@code chunk} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder chunk(String value) {
                this.chunk = value;
                return this;
            }

            /**
             * Sets the {@code answer} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder answer(String value) {
                this.answer = value;
                return this;
            }

            /**
             * Sets the {@code error} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder error(String value) {
                this.error = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public UiEphemeralQueryEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                if (phase == null) throw new IllegalStateException("Missing required property: phase");
                return new UiEphemeralQueryEventData(requestId, phase, chunk, answer, error);
            }
        }
    }
}
