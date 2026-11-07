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
 * Session event "elicitation.requested". Elicitation request; may be form-based (structured input) or URL-based (browser redirect)
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ElicitationRequestedEvent extends SessionEvent {

    @Override
    public String getType() { return "elicitation.requested"; }

    @JsonProperty("data")
    private ElicitationRequestedEventData data;

    public ElicitationRequestedEventData getData() { return data; }
    public void setData(ElicitationRequestedEventData data) { this.data = data; }

    /** Data payload for {@link ElicitationRequestedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ElicitationRequestedEventData(
        /** Unique identifier for this elicitation request; used to respond via session.respondToElicitation() */
        @JsonProperty("requestId") String requestId,
        /** Tool call ID from the LLM completion; used to correlate with CompletionChunk.toolCall.id for remote UIs */
        @JsonProperty("toolCallId") String toolCallId,
        /** The source that initiated the request (MCP server name, or absent for agent-initiated) */
        @JsonProperty("elicitationSource") String elicitationSource,
        /** Display label for a Connector-owned MCP server that initiated the request, from the trusted Connector catalog; absent for other sources. `elicitationSource` stays the identity. */
        @JsonProperty("elicitationSourceDisplayName") String elicitationSourceDisplayName,
        /** Message describing what information is needed from the user */
        @JsonProperty("message") String message,
        /** Elicitation mode; "form" for structured input, "url" for browser-based. Defaults to "form" when absent. */
        @JsonProperty("mode") ElicitationRequestedMode mode,
        /** JSON Schema describing the form fields to present to the user (form mode only) */
        @JsonProperty("requestedSchema") ElicitationRequestedSchema requestedSchema,
        /** URL to open in the user's browser (url mode only) */
        @JsonProperty("url") String url
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public ElicitationRequestedEventData {}

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
            private String requestId;
            private String toolCallId;
            private String elicitationSource;
            private String elicitationSourceDisplayName;
            private String message;
            private ElicitationRequestedMode mode;
            private ElicitationRequestedSchema requestedSchema;
            private String url;

            /**
             * Sets the {@code requestId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestId(String value) {
                this.requestId = value;
                return this;
            }

            /**
             * Sets the {@code toolCallId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolCallId(String value) {
                this.toolCallId = value;
                return this;
            }

            /**
             * Sets the {@code elicitationSource} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder elicitationSource(String value) {
                this.elicitationSource = value;
                return this;
            }

            /**
             * Sets the {@code elicitationSourceDisplayName} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder elicitationSourceDisplayName(String value) {
                this.elicitationSourceDisplayName = value;
                return this;
            }

            /**
             * Sets the {@code message} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder message(String value) {
                this.message = value;
                return this;
            }

            /**
             * Sets the {@code mode} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder mode(ElicitationRequestedMode value) {
                this.mode = value;
                return this;
            }

            /**
             * Sets the {@code requestedSchema} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestedSchema(ElicitationRequestedSchema value) {
                this.requestedSchema = value;
                return this;
            }

            /**
             * Sets the {@code url} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder url(String value) {
                this.url = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public ElicitationRequestedEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                if (message == null) throw new IllegalStateException("Missing required property: message");
                return new ElicitationRequestedEventData(requestId, toolCallId, elicitationSource, elicitationSourceDisplayName, message, mode, requestedSchema, url);
            }
        }
    }
}
