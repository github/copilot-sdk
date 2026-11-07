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
import javax.annotation.processing.Generated;

/**
 * Re-tokenize the session's existing messages against `modelId` and return the token totals. Useful for hosts that want an initial estimate of context usage on session resume, before the next agent turn fires `session.context_info_changed` events. Returns zeros for an empty session.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMetadataRecomputeContextTokensResult(
    /** Sum of tokens across chat-context and system-context messages currently held by the session. */
    @JsonProperty("totalTokens") Long totalTokens,
    /** Tokens contributed by user/assistant/tool messages (excludes system/developer prompts). */
    @JsonProperty("messagesTokenCount") Long messagesTokenCount,
    /** Tokens contributed by system/developer prompt snapshots. */
    @JsonProperty("systemTokenCount") Long systemTokenCount
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionMetadataRecomputeContextTokensResult {}

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
        private Long messagesTokenCount;
        private Long systemTokenCount;

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
         * Sets the {@code messagesTokenCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder messagesTokenCount(Long value) {
            this.messagesTokenCount = value;
            return this;
        }

        /**
         * Sets the {@code systemTokenCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder systemTokenCount(Long value) {
            this.systemTokenCount = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionMetadataRecomputeContextTokensResult build() {
            if (totalTokens == null) throw new IllegalStateException("Missing required property: totalTokens");
            if (messagesTokenCount == null) throw new IllegalStateException("Missing required property: messagesTokenCount");
            if (systemTokenCount == null) throw new IllegalStateException("Missing required property: systemTokenCount");
            return new SessionMetadataRecomputeContextTokensResult(totalTokens, messagesTokenCount, systemTokenCount);
        }
    }
}
