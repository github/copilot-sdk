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
 * Session quota snapshot, preserving the incumbent numeric units and reset metadata.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionQuotaSnapshot(
    /** Whether the entitlement is unlimited. */
    @JsonProperty("isUnlimitedEntitlement") Boolean isUnlimitedEntitlement,
    /** Entitled quantity in this quota's units. */
    @JsonProperty("entitlementRequests") Double entitlementRequests,
    /** Consumed quantity in this quota's units. */
    @JsonProperty("usedRequests") Double usedRequests,
    /** Whether usage is allowed after quota exhaustion. */
    @JsonProperty("usageAllowedWithExhaustedQuota") Boolean usageAllowedWithExhaustedQuota,
    /** Additional usage in this quota's units. */
    @JsonProperty("overage") Double overage,
    /** Whether additional usage is allowed after quota exhaustion. */
    @JsonProperty("overageAllowedWithExhaustedQuota") Boolean overageAllowedWithExhaustedQuota,
    /** Percentage of the entitlement remaining. */
    @JsonProperty("remainingPercentage") Double remainingPercentage,
    /** Quota reset time in milliseconds since the Unix epoch, when known. */
    @JsonProperty("resetDateEpochMs") Double resetDateEpochMs,
    /** Whether the reset time is estimated. */
    @JsonProperty("resetDateEstimated") Boolean resetDateEstimated,
    /** Whether the provider reports available quota. */
    @JsonProperty("hasQuota") Boolean hasQuota,
    /** Whether this quota uses token-based billing. */
    @JsonProperty("tokenBasedBilling") Boolean tokenBasedBilling,
    /** Additional-usage budget cap, when provided. */
    @JsonProperty("overageEntitlement") Double overageEntitlement
) {
}
