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
import java.time.OffsetDateTime;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Accumulated session usage metrics, including premium request cost, token counts, model breakdown, and code-change totals.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionUsageGetMetricsResult(
    /** Total user-initiated premium request cost across all models (may be fractional due to multipliers) */
    @JsonProperty("totalPremiumRequestCost") Double totalPremiumRequestCost,
    /** Raw count of user-initiated API requests */
    @JsonProperty("totalUserRequests") Long totalUserRequests,
    /** Session-wide accumulated nano-AI units cost */
    @JsonProperty("totalNanoAiu") Double totalNanoAiu,
    /** Session-wide per-token-type accumulated token counts */
    @JsonProperty("tokenDetails") Map<String, UsageMetricsTokenDetail> tokenDetails,
    /** Total time spent in model API calls (milliseconds) */
    @JsonProperty("totalApiDurationMs") Long totalApiDurationMs,
    /** ISO 8601 timestamp when the session started */
    @JsonProperty("sessionStartTime") OffsetDateTime sessionStartTime,
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
