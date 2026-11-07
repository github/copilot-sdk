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
 * Session event "session.truncation". Conversation truncation statistics including token counts and removed content metrics
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionTruncationEvent extends SessionEvent {

    @Override
    public String getType() { return "session.truncation"; }

    @JsonProperty("data")
    private SessionTruncationEventData data;

    public SessionTruncationEventData getData() { return data; }
    public void setData(SessionTruncationEventData data) { this.data = data; }

    /** Data payload for {@link SessionTruncationEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionTruncationEventData(
        /** Maximum token count for the model's context window */
        @JsonProperty("tokenLimit") Long tokenLimit,
        /** Total tokens in conversation messages before truncation */
        @JsonProperty("preTruncationTokensInMessages") Long preTruncationTokensInMessages,
        /** Number of conversation messages before truncation */
        @JsonProperty("preTruncationMessagesLength") Long preTruncationMessagesLength,
        /** Total tokens in conversation messages after truncation */
        @JsonProperty("postTruncationTokensInMessages") Long postTruncationTokensInMessages,
        /** Number of conversation messages after truncation */
        @JsonProperty("postTruncationMessagesLength") Long postTruncationMessagesLength,
        /** Number of tokens removed by truncation */
        @JsonProperty("tokensRemovedDuringTruncation") Long tokensRemovedDuringTruncation,
        /** Number of messages removed by truncation */
        @JsonProperty("messagesRemovedDuringTruncation") Long messagesRemovedDuringTruncation,
        /** Identifier of the component that performed truncation (e.g., "BasicTruncator") */
        @JsonProperty("performedBy") String performedBy,
        /** Identifier of the automatic compaction applied just before this truncation, when one was */
        @JsonProperty("compactionAttemptId") String compactionAttemptId
    ) {

        /**
         * Retains the constructor published in Java SDK 1.0.28.
         * @deprecated Use named construction via {@link #builder()}.
         * @param tokenLimit Maximum token count for the model's context window
         * @param preTruncationTokensInMessages Total tokens in conversation messages before truncation
         * @param preTruncationMessagesLength Number of conversation messages before truncation
         * @param postTruncationTokensInMessages Total tokens in conversation messages after truncation
         * @param postTruncationMessagesLength Number of conversation messages after truncation
         * @param tokensRemovedDuringTruncation Number of tokens removed by truncation
         * @param messagesRemovedDuringTruncation Number of messages removed by truncation
         * @param performedBy Identifier of the component that performed truncation (e.g., "BasicTruncator")
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionTruncationEventData(
            Long tokenLimit,
            Long preTruncationTokensInMessages,
            Long preTruncationMessagesLength,
            Long postTruncationTokensInMessages,
            Long postTruncationMessagesLength,
            Long tokensRemovedDuringTruncation,
            Long messagesRemovedDuringTruncation,
            String performedBy
        ) {
            this(tokenLimit, preTruncationTokensInMessages, preTruncationMessagesLength, postTruncationTokensInMessages, postTruncationMessagesLength, tokensRemovedDuringTruncation, messagesRemovedDuringTruncation, performedBy, null);
        }

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionTruncationEventData {}

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
            private Long preTruncationTokensInMessages;
            private Long preTruncationMessagesLength;
            private Long postTruncationTokensInMessages;
            private Long postTruncationMessagesLength;
            private Long tokensRemovedDuringTruncation;
            private Long messagesRemovedDuringTruncation;
            private String performedBy;
            private String compactionAttemptId;

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
             * Sets the {@code preTruncationTokensInMessages} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder preTruncationTokensInMessages(Long value) {
                this.preTruncationTokensInMessages = value;
                return this;
            }

            /**
             * Sets the {@code preTruncationMessagesLength} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder preTruncationMessagesLength(Long value) {
                this.preTruncationMessagesLength = value;
                return this;
            }

            /**
             * Sets the {@code postTruncationTokensInMessages} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder postTruncationTokensInMessages(Long value) {
                this.postTruncationTokensInMessages = value;
                return this;
            }

            /**
             * Sets the {@code postTruncationMessagesLength} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder postTruncationMessagesLength(Long value) {
                this.postTruncationMessagesLength = value;
                return this;
            }

            /**
             * Sets the {@code tokensRemovedDuringTruncation} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder tokensRemovedDuringTruncation(Long value) {
                this.tokensRemovedDuringTruncation = value;
                return this;
            }

            /**
             * Sets the {@code messagesRemovedDuringTruncation} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder messagesRemovedDuringTruncation(Long value) {
                this.messagesRemovedDuringTruncation = value;
                return this;
            }

            /**
             * Sets the {@code performedBy} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder performedBy(String value) {
                this.performedBy = value;
                return this;
            }

            /**
             * Sets the {@code compactionAttemptId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder compactionAttemptId(String value) {
                this.compactionAttemptId = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionTruncationEventData build() {
                if (tokenLimit == null) throw new IllegalStateException("Missing required property: tokenLimit");
                if (preTruncationTokensInMessages == null) throw new IllegalStateException("Missing required property: preTruncationTokensInMessages");
                if (preTruncationMessagesLength == null) throw new IllegalStateException("Missing required property: preTruncationMessagesLength");
                if (postTruncationTokensInMessages == null) throw new IllegalStateException("Missing required property: postTruncationTokensInMessages");
                if (postTruncationMessagesLength == null) throw new IllegalStateException("Missing required property: postTruncationMessagesLength");
                if (tokensRemovedDuringTruncation == null) throw new IllegalStateException("Missing required property: tokensRemovedDuringTruncation");
                if (messagesRemovedDuringTruncation == null) throw new IllegalStateException("Missing required property: messagesRemovedDuringTruncation");
                if (performedBy == null) throw new IllegalStateException("Missing required property: performedBy");
                return new SessionTruncationEventData(tokenLimit, preTruncationTokensInMessages, preTruncationMessagesLength, postTruncationTokensInMessages, postTruncationMessagesLength, tokensRemovedDuringTruncation, messagesRemovedDuringTruncation, performedBy, compactionAttemptId);
            }
        }
    }
}
