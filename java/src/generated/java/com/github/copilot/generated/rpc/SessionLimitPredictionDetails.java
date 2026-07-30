/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Explainable AI-credit session-limit prediction.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionLimitPredictionDetails(
    /** Client population used for the prediction. */
    @JsonProperty("clientType") SessionLimitPredictionClientType clientType,
    /** Model identifier used for lookup. */
    @JsonProperty("modelId") String modelId,
    /** Baseline fallback level used to create the prediction. */
    @JsonProperty("source") SessionLimitPredictionSource source,
    /** Key matched at the source level, such as a model id, family id, or `global`. */
    @JsonProperty("sourceKey") String sourceKey,
    /** Resolved model family when known. */
    @JsonProperty("family") String family,
    /** Ordered usage tiers and their AI-credit caps. */
    @JsonProperty("tiers") List<SessionLimitPredictionTierOption> tiers,
    /** Baseline data provenance. */
    @JsonProperty("baselineData") SessionLimitPredictionBaselineData baselineData,
    /** Tier chosen as the recommended cap. */
    @JsonProperty("recommendedTier") SessionLimitPredictionTier recommendedTier,
    /** Recommended maximum AI credits for this session. */
    @JsonProperty("recommendedCap") Double recommendedCap
) {
}
