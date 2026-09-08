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
 * Current per-window credit limit and consumption for an autopilot objective.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AutopilotObjectiveCreditLimit(
    /** Configured AI-credit cap, when one is set. */
    @JsonProperty("credits") Double credits,
    /** Window consumption in fractional AI credits, for display. */
    @JsonProperty("creditsUsed") Double creditsUsed,
    /** Exact window consumption in non-negative integer nano-AIU, encoded as a decimal string. */
    @JsonProperty("creditsUsedNanoAiu") String creditsUsedNanoAiu
) {
}
