/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Result for the {@code session.usage.getMetrics} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionUsageGetMetricsResult(
    /** Total user-initiated premium request cost across all models (may be fractional due to multipliers) */
    @JsonProperty("totalPremiumRequestCost") Double totalPremiumRequestCost,
    /** Raw count of user-initiated API requests */
    @JsonProperty("totalUserRequests") Long totalUserRequests,
    /** Total time spent in model API calls (milliseconds) */
    @JsonProperty("totalApiDurationMs") Double totalApiDurationMs,
    /** Session start timestamp (epoch milliseconds) */
    @JsonProperty("sessionStartTime") Long sessionStartTime,
    /** Aggregated code change metrics */
    @JsonProperty("codeChanges") SessionUsageGetMetricsResultCodeChanges codeChanges,
    /** Per-model token and request metrics, keyed by model identifier */
    @JsonProperty("modelMetrics") Map<String, SessionUsageGetMetricsResultModelMetricsValue> modelMetrics,
    /** Currently active model identifier */
    @JsonProperty("currentModel") String currentModel,
    /** Input tokens from the most recent main-agent API call */
    @JsonProperty("lastCallInputTokens") Long lastCallInputTokens,
    /** Output tokens from the most recent main-agent API call */
    @JsonProperty("lastCallOutputTokens") Long lastCallOutputTokens
) {

    /** Aggregated code change metrics */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionUsageGetMetricsResultCodeChanges(
        /** Total lines of code added */
        @JsonProperty("linesAdded") Long linesAdded,
        /** Total lines of code removed */
        @JsonProperty("linesRemoved") Long linesRemoved,
        /** Number of distinct files modified */
        @JsonProperty("filesModifiedCount") Long filesModifiedCount
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionUsageGetMetricsResultModelMetricsValue(
        /** Request count and cost metrics for this model */
        @JsonProperty("requests") SessionUsageGetMetricsResultModelMetricsValueRequests requests,
        /** Token usage metrics for this model */
        @JsonProperty("usage") SessionUsageGetMetricsResultModelMetricsValueUsage usage
    ) {

        /** Request count and cost metrics for this model */
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record SessionUsageGetMetricsResultModelMetricsValueRequests(
            /** Number of API requests made with this model */
            @JsonProperty("count") Long count,
            /** User-initiated premium request cost (with multiplier applied) */
            @JsonProperty("cost") Double cost
        ) {
        }

        /** Token usage metrics for this model */
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record SessionUsageGetMetricsResultModelMetricsValueUsage(
            /** Total input tokens consumed */
            @JsonProperty("inputTokens") Long inputTokens,
            /** Total output tokens produced */
            @JsonProperty("outputTokens") Long outputTokens,
            /** Total tokens read from prompt cache */
            @JsonProperty("cacheReadTokens") Long cacheReadTokens,
            /** Total tokens written to prompt cache */
            @JsonProperty("cacheWriteTokens") Long cacheWriteTokens,
            /** Total output tokens used for reasoning */
            @JsonProperty("reasoningTokens") Long reasoningTokens
        ) {
        }
    }
}
