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
 * Aggregate concrete-model usage for one HydraFusion phase.
 *
 * @since 1.0.0
 */
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
}
