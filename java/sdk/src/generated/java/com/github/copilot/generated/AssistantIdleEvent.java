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
 * Session event "assistant.idle". Payload emitted whenever the main agent's processing loop goes idle, including while related background work (running agents or in-flight attached shell commands) is still pending and the session-level idle event is therefore deferred
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantIdleEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.idle"; }

    @JsonProperty("data")
    private AssistantIdleEventData data;

    public AssistantIdleEventData getData() { return data; }
    public void setData(AssistantIdleEventData data) { this.data = data; }

    /** Data payload for {@link AssistantIdleEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantIdleEventData(
        /** True when the preceding agentic loop was cancelled via abort signal */
        @JsonProperty("aborted") Boolean aborted
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AssistantIdleEventData {}

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
            private Boolean aborted;

            /**
             * Sets the {@code aborted} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder aborted(Boolean value) {
                this.aborted = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public AssistantIdleEventData build() {
                return new AssistantIdleEventData(aborted);
            }
        }
    }
}
