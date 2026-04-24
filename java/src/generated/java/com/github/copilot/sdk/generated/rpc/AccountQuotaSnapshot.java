/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountQuotaSnapshot(
    /** Whether the user has an unlimited usage entitlement */
    @JsonProperty("isUnlimitedEntitlement") Boolean isUnlimitedEntitlement,
    /** Number of requests included in the entitlement */
    @JsonProperty("entitlementRequests") Long entitlementRequests,
    /** Number of requests used so far this period */
    @JsonProperty("usedRequests") Long usedRequests,
    /** Whether usage is still permitted after quota exhaustion */
    @JsonProperty("usageAllowedWithExhaustedQuota") Boolean usageAllowedWithExhaustedQuota,
    /** Percentage of entitlement remaining */
    @JsonProperty("remainingPercentage") Double remainingPercentage,
    /** Number of overage requests made this period */
    @JsonProperty("overage") Double overage,
    /** Whether overage is allowed when quota is exhausted */
    @JsonProperty("overageAllowedWithExhaustedQuota") Boolean overageAllowedWithExhaustedQuota,
    /** Date when the quota resets (ISO 8601 string) */
    @JsonProperty("resetDate") String resetDate
) {
}
