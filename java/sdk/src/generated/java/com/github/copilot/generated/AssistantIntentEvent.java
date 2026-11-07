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
 * Session event "assistant.intent". Agent intent description for current activity or plan
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantIntentEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.intent"; }

    @JsonProperty("data")
    private AssistantIntentEventData data;

    public AssistantIntentEventData getData() { return data; }
    public void setData(AssistantIntentEventData data) { this.data = data; }

    /** Data payload for {@link AssistantIntentEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantIntentEventData(
        /** Short description of what the agent is currently doing or planning to do */
        @JsonProperty("intent") String intent
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AssistantIntentEventData {}

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
            private String intent;

            /**
             * Sets the {@code intent} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder intent(String value) {
                this.intent = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public AssistantIntentEventData build() {
                if (intent == null) throw new IllegalStateException("Missing required property: intent");
                return new AssistantIntentEventData(intent);
            }
        }
    }
}
