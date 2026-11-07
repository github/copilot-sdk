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
 * Session event "session.usage_info". Current context window usage statistics including token and message counts
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionUsageInfoEvent extends SessionEvent {

    @Override
    public String getType() { return "session.usage_info"; }

    @JsonProperty("data")
    private SessionUsageInfoEventData data;

    public SessionUsageInfoEventData getData() { return data; }
    public void setData(SessionUsageInfoEventData data) { this.data = data; }

    /** Data payload for {@link SessionUsageInfoEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionUsageInfoEventData(
        /** Maximum token count for the model's context window */
        @JsonProperty("tokenLimit") Long tokenLimit,
        /** Current number of tokens in the context window */
        @JsonProperty("currentTokens") Long currentTokens,
        /** Current number of messages in the conversation */
        @JsonProperty("messagesLength") Long messagesLength,
        /** Token count from system message(s) */
        @JsonProperty("systemTokens") Long systemTokens,
        /** Token count from non-system messages (user, assistant, tool) */
        @JsonProperty("conversationTokens") Long conversationTokens,
        /** Token count from tool definitions */
        @JsonProperty("toolDefinitionsTokens") Long toolDefinitionsTokens,
        /** Whether this is the first usage_info event emitted in this session */
        @JsonProperty("isInitial") Boolean isInitial
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionUsageInfoEventData {}

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
            private Long tokenLimit;
            private Long currentTokens;
            private Long messagesLength;
            private Long systemTokens;
            private Long conversationTokens;
            private Long toolDefinitionsTokens;
            private Boolean isInitial;

            /**
             * Sets the {@code tokenLimit} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder tokenLimit(Long value) {
                this.tokenLimit = value;
                return this;
            }

            /**
             * Sets the {@code currentTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder currentTokens(Long value) {
                this.currentTokens = value;
                return this;
            }

            /**
             * Sets the {@code messagesLength} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder messagesLength(Long value) {
                this.messagesLength = value;
                return this;
            }

            /**
             * Sets the {@code systemTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder systemTokens(Long value) {
                this.systemTokens = value;
                return this;
            }

            /**
             * Sets the {@code conversationTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder conversationTokens(Long value) {
                this.conversationTokens = value;
                return this;
            }

            /**
             * Sets the {@code toolDefinitionsTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolDefinitionsTokens(Long value) {
                this.toolDefinitionsTokens = value;
                return this;
            }

            /**
             * Sets the {@code isInitial} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder isInitial(Boolean value) {
                this.isInitial = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionUsageInfoEventData build() {
                if (tokenLimit == null) throw new IllegalStateException("Missing required property: tokenLimit");
                if (currentTokens == null) throw new IllegalStateException("Missing required property: currentTokens");
                if (messagesLength == null) throw new IllegalStateException("Missing required property: messagesLength");
                return new SessionUsageInfoEventData(tokenLimit, currentTokens, messagesLength, systemTokens, conversationTokens, toolDefinitionsTokens, isInitial);
            }
        }
    }
}
