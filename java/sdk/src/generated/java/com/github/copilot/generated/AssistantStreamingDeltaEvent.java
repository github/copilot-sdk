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
 * Session event "assistant.streaming_delta". Streaming response progress with cumulative byte count
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantStreamingDeltaEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.streaming_delta"; }

    @JsonProperty("data")
    private AssistantStreamingDeltaEventData data;

    public AssistantStreamingDeltaEventData getData() { return data; }
    public void setData(AssistantStreamingDeltaEventData data) { this.data = data; }

    /** Data payload for {@link AssistantStreamingDeltaEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantStreamingDeltaEventData(
        /** Cumulative total bytes received from the streaming response so far */
        @JsonProperty("totalResponseSizeBytes") Long totalResponseSizeBytes
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AssistantStreamingDeltaEventData {}

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
            private Long totalResponseSizeBytes;

            /**
             * Sets the {@code totalResponseSizeBytes} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder totalResponseSizeBytes(Long value) {
                this.totalResponseSizeBytes = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public AssistantStreamingDeltaEventData build() {
                if (totalResponseSizeBytes == null) throw new IllegalStateException("Missing required property: totalResponseSizeBytes");
                return new AssistantStreamingDeltaEventData(totalResponseSizeBytes);
            }
        }
    }
}
