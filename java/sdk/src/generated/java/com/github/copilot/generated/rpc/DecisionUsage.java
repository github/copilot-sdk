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
 * Token usage the backend reported for one request.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DecisionUsage(
    /** Input tokens, including cached and cache-write tokens. */
    @JsonProperty("inputTokens") Long inputTokens,
    /** Input tokens served from the backend's prompt cache, when reported. */
    @JsonProperty("cachedInputTokens") Long cachedInputTokens,
    /** Input tokens written to the backend's prompt cache, when reported. */
    @JsonProperty("cacheWriteTokens") Long cacheWriteTokens,
    /** Output tokens, including reasoning tokens. */
    @JsonProperty("outputTokens") Long outputTokens,
    /** Reasoning tokens, when reported. */
    @JsonProperty("reasoningTokens") Long reasoningTokens
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public DecisionUsage {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private Long inputTokens;
        private Long cachedInputTokens;
        private Long cacheWriteTokens;
        private Long outputTokens;
        private Long reasoningTokens;

        /**
         * Sets the {@code inputTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder inputTokens(Long value) {
            this.inputTokens = value;
            return this;
        }

        /**
         * Sets the {@code cachedInputTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder cachedInputTokens(Long value) {
            this.cachedInputTokens = value;
            return this;
        }

        /**
         * Sets the {@code cacheWriteTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder cacheWriteTokens(Long value) {
            this.cacheWriteTokens = value;
            return this;
        }

        /**
         * Sets the {@code outputTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder outputTokens(Long value) {
            this.outputTokens = value;
            return this;
        }

        /**
         * Sets the {@code reasoningTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder reasoningTokens(Long value) {
            this.reasoningTokens = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public DecisionUsage build() {
            if (inputTokens == null) throw new IllegalStateException("Missing required property: inputTokens");
            if (outputTokens == null) throw new IllegalStateException("Missing required property: outputTokens");
            return new DecisionUsage(inputTokens, cachedInputTokens, cacheWriteTokens, outputTokens, reasoningTokens);
        }
    }
}
