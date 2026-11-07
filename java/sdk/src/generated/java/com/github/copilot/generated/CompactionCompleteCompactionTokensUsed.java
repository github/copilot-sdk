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
 * Token usage breakdown for the compaction LLM call (aligned with assistant.usage format)
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CompactionCompleteCompactionTokensUsed(
    /** Number of model requests represented by this aggregated compaction usage */
    @JsonProperty("requestCount") Long requestCount,
    /** Whether the compaction's credit amount was reported for all, some, or none of its calls. */
    @JsonProperty("aiCreditsStatus") AiCreditsStatus aiCreditsStatus,
    /** Provider captured for the compaction call; absent when attribution is unknown. */
    @JsonProperty("provider") ModelProviderRef provider,
    /** Model display name captured for the compaction call. */
    @JsonProperty("modelDisplayName") String modelDisplayName,
    /** Input tokens consumed by the compaction LLM call */
    @JsonProperty("inputTokens") Long inputTokens,
    /** Output tokens produced by the compaction LLM call */
    @JsonProperty("outputTokens") Long outputTokens,
    /** Cached input tokens reused in the compaction LLM call */
    @JsonProperty("cacheReadTokens") Long cacheReadTokens,
    /** Tokens written to prompt cache in the compaction LLM call */
    @JsonProperty("cacheWriteTokens") Long cacheWriteTokens,
    /** Per-request cost and usage data from the CAPI copilot_usage response field */
    @JsonProperty("copilotUsage") CompactionCompleteCompactionTokensUsedCopilotUsage copilotUsage,
    /** Duration of the compaction LLM call in milliseconds */
    @JsonProperty("duration") Long duration,
    /** Model identifier used for the compaction LLM call */
    @JsonProperty("model") String model
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CompactionCompleteCompactionTokensUsed {}

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
        private Long requestCount;
        private AiCreditsStatus aiCreditsStatus;
        private ModelProviderRef provider;
        private String modelDisplayName;
        private Long inputTokens;
        private Long outputTokens;
        private Long cacheReadTokens;
        private Long cacheWriteTokens;
        private CompactionCompleteCompactionTokensUsedCopilotUsage copilotUsage;
        private Long duration;
        private String model;

        /**
         * Sets the {@code requestCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder requestCount(Long value) {
            this.requestCount = value;
            return this;
        }

        /**
         * Sets the {@code aiCreditsStatus} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder aiCreditsStatus(AiCreditsStatus value) {
            this.aiCreditsStatus = value;
            return this;
        }

        /**
         * Sets the {@code provider} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder provider(ModelProviderRef value) {
            this.provider = value;
            return this;
        }

        /**
         * Sets the {@code modelDisplayName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modelDisplayName(String value) {
            this.modelDisplayName = value;
            return this;
        }

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
         * Sets the {@code copilotUsage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder copilotUsage(CompactionCompleteCompactionTokensUsedCopilotUsage value) {
            this.copilotUsage = value;
            return this;
        }

        /**
         * Sets the {@code duration} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder duration(Long value) {
            this.duration = value;
            return this;
        }

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder model(String value) {
            this.model = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CompactionCompleteCompactionTokensUsed build() {
            return new CompactionCompleteCompactionTokensUsed(requestCount, aiCreditsStatus, provider, modelDisplayName, inputTokens, outputTokens, cacheReadTokens, cacheWriteTokens, copilotUsage, duration, model);
        }
    }
}
