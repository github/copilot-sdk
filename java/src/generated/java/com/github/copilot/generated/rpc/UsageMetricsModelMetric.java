/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Per-model usage metrics, including request counts/costs, token usage, nano-AI units, and per-token-type details.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsageMetricsModelMetric(
    /** Request count and cost metrics for this model */
    @JsonProperty("requests") UsageMetricsModelMetricRequests requests,
    /** Token usage metrics for this model */
    @JsonProperty("usage") UsageMetricsModelMetricUsage usage,
    /** Latest known prompt-cache expiration for this model. A timestamp in the past indicates that the observed cache has expired. */
    @JsonProperty("cacheExpiresAt") OffsetDateTime cacheExpiresAt,
    /** Accumulated nano-AI units cost for this model */
    @JsonProperty("totalNanoAiu") Double totalNanoAiu,
    /** Token count details per type */
    @JsonProperty("tokenDetails") Map<String, UsageMetricsModelMetricTokenDetail> tokenDetails
) {
}
