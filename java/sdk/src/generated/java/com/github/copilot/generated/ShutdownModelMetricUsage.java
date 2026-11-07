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
 * Token usage breakdown
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ShutdownModelMetricUsage(
    /** Total input tokens consumed across all requests to this model */
    @JsonProperty("inputTokens") Long inputTokens,
    /** Total output tokens produced across all requests to this model */
    @JsonProperty("outputTokens") Long outputTokens,
    /** Total tokens read from prompt cache across all requests */
    @JsonProperty("cacheReadTokens") Long cacheReadTokens,
    /** Total tokens written to prompt cache across all requests */
    @JsonProperty("cacheWriteTokens") Long cacheWriteTokens,
    /** Total reasoning tokens produced across all requests to this model */
    @JsonProperty("reasoningTokens") Long reasoningTokens
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ShutdownModelMetricUsage {}

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
        private Long inputTokens;
        private Long outputTokens;
        private Long cacheReadTokens;
        private Long cacheWriteTokens;
        private Long reasoningTokens;

        /**
         * Sets the {@code inputTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder inputTokens(Long value) {
            this.inputTokens = value;
            return this;
        }

        /**
         * Sets the {@code outputTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder outputTokens(Long value) {
            this.outputTokens = value;
            return this;
        }

        /**
         * Sets the {@code cacheReadTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cacheReadTokens(Long value) {
            this.cacheReadTokens = value;
            return this;
        }

        /**
         * Sets the {@code cacheWriteTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cacheWriteTokens(Long value) {
            this.cacheWriteTokens = value;
            return this;
        }

        /**
         * Sets the {@code reasoningTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
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
        public ShutdownModelMetricUsage build() {
            if (inputTokens == null) throw new IllegalStateException("Missing required property: inputTokens");
            if (outputTokens == null) throw new IllegalStateException("Missing required property: outputTokens");
            if (cacheReadTokens == null) throw new IllegalStateException("Missing required property: cacheReadTokens");
            if (cacheWriteTokens == null) throw new IllegalStateException("Missing required property: cacheWriteTokens");
            return new ShutdownModelMetricUsage(inputTokens, outputTokens, cacheReadTokens, cacheWriteTokens, reasoningTokens);
        }
    }
}
