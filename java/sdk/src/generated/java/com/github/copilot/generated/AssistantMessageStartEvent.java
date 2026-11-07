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
 * Session event "assistant.message_start". Streaming assistant message start metadata
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantMessageStartEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.message_start"; }

    @JsonProperty("data")
    private AssistantMessageStartEventData data;

    public AssistantMessageStartEventData getData() { return data; }
    public void setData(AssistantMessageStartEventData data) { this.data = data; }

    /** Data payload for {@link AssistantMessageStartEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantMessageStartEventData(
        /** Message ID this start event belongs to, matching subsequent deltas and assistant.message */
        @JsonProperty("messageId") String messageId,
        /** Generation phase this message belongs to for phased-output models */
        @JsonProperty("phase") String phase
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AssistantMessageStartEventData {}

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
            private String messageId;
            private String phase;

            /**
             * Sets the {@code messageId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder messageId(String value) {
                this.messageId = value;
                return this;
            }

            /**
             * Sets the {@code phase} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder phase(String value) {
                this.phase = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public AssistantMessageStartEventData build() {
                if (messageId == null) throw new IllegalStateException("Missing required property: messageId");
                return new AssistantMessageStartEventData(messageId, phase);
            }
        }
    }
}
