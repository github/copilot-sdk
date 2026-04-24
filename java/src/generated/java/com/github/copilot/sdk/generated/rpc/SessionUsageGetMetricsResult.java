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
    @JsonProperty("codeChanges") UsageMetricsCodeChanges codeChanges,
    /** Per-model token and request metrics, keyed by model identifier */
    @JsonProperty("modelMetrics") Map<String, UsageMetricsModelMetric> modelMetrics,
    /** Currently active model identifier */
    @JsonProperty("currentModel") String currentModel,
    /** Input tokens from the most recent main-agent API call */
    @JsonProperty("lastCallInputTokens") Long lastCallInputTokens,
    /** Output tokens from the most recent main-agent API call */
    @JsonProperty("lastCallOutputTokens") Long lastCallOutputTokens
) {
}
