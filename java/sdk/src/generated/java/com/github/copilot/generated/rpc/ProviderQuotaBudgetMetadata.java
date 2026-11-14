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
 * Authoritative budget measurements and policy metadata, independent of provider.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProviderQuotaBudgetMetadata(
    /** Whether the budget has unlimited entitlement. */
    @JsonProperty("unlimited") Boolean unlimited,
    /** Exact budget entitlement, retaining incumbent fractional-unit compatibility. */
    @JsonProperty("entitlement") Double entitlement,
    /** Exact measured consumption in the budget's unit. */
    @JsonProperty("consumed") Double consumed,
    /** Service-reported remaining percentage for authoritative budget presentation. */
    @JsonProperty("remainingPercentage") Double remainingPercentage,
    /** Whether service policy allows continued usage after exhaustion. */
    @JsonProperty("usageAllowedWhenExhausted") Boolean usageAllowedWhenExhausted,
    /** Usage beyond entitlement, in the budget's unit. */
    @JsonProperty("overage") Double overage,
    /** Whether additional usage is allowed when the budget is exhausted. */
    @JsonProperty("overageAllowedWhenExhausted") Boolean overageAllowedWhenExhausted,
    /** Reset instant in epoch milliseconds when this budget actually defines a window. */
    @JsonProperty("resetAtEpochMs") Double resetAtEpochMs,
    /** Whether the reset instant is an estimate. */
    @JsonProperty("resetEstimated") Boolean resetEstimated,
    /** Whether this budget uses token-based billing. */
    @JsonProperty("tokenBasedBilling") Boolean tokenBasedBilling,
    /** Optional additional-usage budget cap. */
    @JsonProperty("overageLimit") Double overageLimit
) {
}
