/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Usage for one recorded provider and model, without merging identical model IDs across providers.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsageMetricsProviderModelMetric(
    /** Provider identity and product label captured when the call was dispatched; null when unknown. */
    @JsonProperty("provider") ModelProviderRef provider,
    /** Model identity, or null for legacy aggregate-only usage. */
    @JsonProperty("modelId") String modelId,
    /** Model display name captured at call time, when known. */
    @JsonProperty("modelDisplayName") String modelDisplayName,
    /** Request, token, and cost totals for this provider/model. */
    @JsonProperty("metrics") UsageMetricsModelMetric metrics
) {
}
