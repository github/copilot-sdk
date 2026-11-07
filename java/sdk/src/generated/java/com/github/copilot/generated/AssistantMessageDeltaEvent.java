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
 * Session event "assistant.message_delta". Streaming assistant message delta for incremental response updates
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantMessageDeltaEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.message_delta"; }

    @JsonProperty("data")
    private AssistantMessageDeltaEventData data;

    public AssistantMessageDeltaEventData getData() { return data; }
    public void setData(AssistantMessageDeltaEventData data) { this.data = data; }

    /** Data payload for {@link AssistantMessageDeltaEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantMessageDeltaEventData(
        /** Message ID this delta belongs to, matching the corresponding assistant.message event */
        @JsonProperty("messageId") String messageId,
        /** Incremental text chunk to append to the message content */
        @JsonProperty("deltaContent") String deltaContent,
        /** Tool call ID of the parent tool invocation when this event originates from a sub-agent */
        @Deprecated
        @JsonProperty("parentToolCallId") String parentToolCallId
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AssistantMessageDeltaEventData {}

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
            private String deltaContent;
            private String parentToolCallId;

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
             * Sets the {@code deltaContent} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder deltaContent(String value) {
                this.deltaContent = value;
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
            public AssistantMessageDeltaEventData build() {
                if (messageId == null) throw new IllegalStateException("Missing required property: messageId");
                if (deltaContent == null) throw new IllegalStateException("Missing required property: deltaContent");
                return new AssistantMessageDeltaEventData(messageId, deltaContent, parentToolCallId);
            }
        }
    }
}
