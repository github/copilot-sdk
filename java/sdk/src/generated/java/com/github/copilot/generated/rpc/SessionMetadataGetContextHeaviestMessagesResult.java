/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The heaviest individual messages in the session's context window, most-expensive first.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMetadataGetContextHeaviestMessagesResult(
    /** Total token count of the current context window, so callers can compute each message's share without a second call. */
    @JsonProperty("totalTokens") Long totalTokens,
    /** Heaviest messages, most-expensive first. */
    @JsonProperty("messages") List<ContextHeaviestMessage> messages
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionMetadataGetContextHeaviestMessagesResult {}

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
        private Long totalTokens;
        private List<ContextHeaviestMessage> messages;

        /**
         * Sets the {@code totalTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder totalTokens(Long value) {
            this.totalTokens = value;
            return this;
        }

        /**
         * Sets the {@code messages} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder messages(List<ContextHeaviestMessage> value) {
            this.messages = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionMetadataGetContextHeaviestMessagesResult build() {
            if (totalTokens == null) throw new IllegalStateException("Missing required property: totalTokens");
            if (messages == null) throw new IllegalStateException("Missing required property: messages");
            return new SessionMetadataGetContextHeaviestMessagesResult(totalTokens, messages);
        }
    }
}
