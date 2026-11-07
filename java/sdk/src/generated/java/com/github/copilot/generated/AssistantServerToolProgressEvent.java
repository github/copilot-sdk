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
 * Session event "assistant.server_tool_progress". Live progress signal for a provider-hosted server tool (e.g. hosted web search) while it runs, before the finalized serverTools envelope lands on the terminal assistant.message
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantServerToolProgressEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.server_tool_progress"; }

    @JsonProperty("data")
    private AssistantServerToolProgressEventData data;

    public AssistantServerToolProgressEventData getData() { return data; }
    public void setData(AssistantServerToolProgressEventData data) { this.data = data; }

    /** Data payload for {@link AssistantServerToolProgressEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantServerToolProgressEventData(
        /** Position of the hosted tool call in the response output. Stable across the call's lifecycle events (unlike the provider's per-event item id, which CAPI rotates), so the host keys the live in-progress row on it. */
        @JsonProperty("outputIndex") Long outputIndex,
        /** Kind of hosted server tool that is running. Only `web_search` is emitted today. */
        @JsonProperty("kind") String kind,
        /** Lifecycle status of the hosted call: `in_progress`, `searching`, or `completed`. */
        @JsonProperty("status") String status
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AssistantServerToolProgressEventData {}

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
            private Long outputIndex;
            private String kind;
            private String status;

            /**
             * Sets the {@code outputIndex} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder outputIndex(Long value) {
                this.outputIndex = value;
                return this;
            }

            /**
             * Sets the {@code kind} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder kind(String value) {
                this.kind = value;
                return this;
            }

            /**
             * Sets the {@code status} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder status(String value) {
                this.status = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public AssistantServerToolProgressEventData build() {
                if (outputIndex == null) throw new IllegalStateException("Missing required property: outputIndex");
                if (kind == null) throw new IllegalStateException("Missing required property: kind");
                if (status == null) throw new IllegalStateException("Missing required property: status");
                return new AssistantServerToolProgressEventData(outputIndex, kind, status);
            }
        }
    }
}
