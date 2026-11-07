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
 * Session event "session.info". Informational message for timeline display with categorization
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionInfoEvent extends SessionEvent {

    @Override
    public String getType() { return "session.info"; }

    @JsonProperty("data")
    private SessionInfoEventData data;

    public SessionInfoEventData getData() { return data; }
    public void setData(SessionInfoEventData data) { this.data = data; }

    /** Data payload for {@link SessionInfoEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionInfoEventData(
        /** Category of informational message (e.g., "notification", "timing", "context_window", "mcp", "snapshot", "configuration", "authentication", "model") */
        @JsonProperty("infoType") String infoType,
        /** Human-readable informational message for display in the timeline */
        @JsonProperty("message") String message,
        /** Optional URL associated with this message that the user can open in a browser */
        @JsonProperty("url") String url,
        /** Optional actionable tip displayed with this message */
        @JsonProperty("tip") String tip,
        /** Reasoning configuration consumed at this conversation boundary. Present on reasoning_effort updates; preserves the original request baseline and applied effort without creating a user message. */
        @JsonProperty("responsesReasoning") ResponsesReasoning responsesReasoning
    ) {

        /**
         * Creates a record with the components it had before later optional fields were added.
         * @deprecated Use named construction via {@link #builder()}.
         * @param infoType Category of informational message (e.g., "notification", "timing", "context_window", "mcp", "snapshot", "configuration", "authentication", "model")
         * @param message Human-readable informational message for display in the timeline
         * @param url Optional URL associated with this message that the user can open in a browser
         * @param tip Optional actionable tip displayed with this message
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionInfoEventData(
            String infoType,
            String message,
            String url,
            String tip
        ) {
            this(infoType, message, url, tip, null);
        }

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionInfoEventData {}

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
            private String infoType;
            private String message;
            private String url;
            private String tip;
            private ResponsesReasoning responsesReasoning;

            /**
             * Sets the {@code infoType} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder infoType(String value) {
                this.infoType = value;
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
             * Sets the {@code tip} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder tip(String value) {
                this.tip = value;
                return this;
            }

            /**
             * Sets the {@code responsesReasoning} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder responsesReasoning(ResponsesReasoning value) {
                this.responsesReasoning = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionInfoEventData build() {
                if (infoType == null) throw new IllegalStateException("Missing required property: infoType");
                if (message == null) throw new IllegalStateException("Missing required property: message");
                return new SessionInfoEventData(infoType, message, url, tip, responsesReasoning);
            }
        }
    }
}
