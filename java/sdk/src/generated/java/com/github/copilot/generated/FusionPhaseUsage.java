/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Aggregate concrete-model usage for one HydraFusion phase.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FusionPhaseUsage(
    /** Number of concrete model requests made by the phase. */
    @JsonProperty("requestCount") Long requestCount,
    /** Total input tokens consumed by the phase. */
    @JsonProperty("inputTokens") Long inputTokens,
    /** Total output tokens produced by the phase. */
    @JsonProperty("outputTokens") Long outputTokens,
    /** Total cached input tokens reported for the phase. */
    @JsonProperty("cachedTokens") Long cachedTokens,
    /** Total tokens written to prompt cache during the phase. */
    @JsonProperty("cacheWriteTokens") Long cacheWriteTokens,
    /** Total normalized AI-unit cost reported for the phase, in nano-AIU. */
    @JsonProperty("totalNanoAiu") Double totalNanoAiu
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public FusionPhaseUsage {}

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
        private Long requestCount;
        private Long inputTokens;
        private Long outputTokens;
        private Long cachedTokens;
        private Long cacheWriteTokens;
        private Double totalNanoAiu;

        /**
         * Sets the {@code requestCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder requestCount(Long value) {
            this.requestCount = value;
            return this;
        }

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
         * Sets the {@code cachedTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder cachedTokens(Long value) {
            this.cachedTokens = value;
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
         * Sets the {@code totalNanoAiu} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder totalNanoAiu(Double value) {
            this.totalNanoAiu = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public FusionPhaseUsage build() {
            if (requestCount == null) throw new IllegalStateException("Missing required property: requestCount");
            if (inputTokens == null) throw new IllegalStateException("Missing required property: inputTokens");
            if (outputTokens == null) throw new IllegalStateException("Missing required property: outputTokens");
            if (cachedTokens == null) throw new IllegalStateException("Missing required property: cachedTokens");
            if (totalNanoAiu == null) throw new IllegalStateException("Missing required property: totalNanoAiu");
            return new FusionPhaseUsage(requestCount, inputTokens, outputTokens, cachedTokens, cacheWriteTokens, totalNanoAiu);
        }
    }
}
