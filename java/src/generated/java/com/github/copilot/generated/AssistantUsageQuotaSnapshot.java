/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Schema for the `AssistantUsageQuotaSnapshot` type.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AssistantUsageQuotaSnapshot(
    /** Whether the user has an unlimited usage entitlement */
    @JsonProperty("isUnlimitedEntitlement") Boolean isUnlimitedEntitlement,
    /** Total requests allowed by the entitlement */
    @JsonProperty("entitlementRequests") Long entitlementRequests,
    /** Number of requests already consumed */
    @JsonProperty("usedRequests") Long usedRequests,
    /** Whether usage is still permitted after quota exhaustion */
    @JsonProperty("usageAllowedWithExhaustedQuota") Boolean usageAllowedWithExhaustedQuota,
    /** Number of additional usage requests made this period */
    @JsonProperty("overage") Double overage,
    /** Whether additional usage is allowed when quota is exhausted */
    @JsonProperty("overageAllowedWithExhaustedQuota") Boolean overageAllowedWithExhaustedQuota,
    /** Percentage of quota remaining (0 to 100) */
    @JsonProperty("remainingPercentage") Double remainingPercentage,
    /** Date when the quota resets */
    @JsonProperty("resetDate") OffsetDateTime resetDate,
    /** Whether the user currently has quota available for use */
    @JsonProperty("hasQuota") Boolean hasQuota,
    /** Whether this snapshot uses token-based billing (AI-credits allocation) */
    @JsonProperty("tokenBasedBilling") Boolean tokenBasedBilling,
    /** Pay-as-you-go additional-usage budget cap in AI credits (1 credit = $0.01); present only when CAPI emits a finite value */
    @JsonProperty("overageEntitlement") Double overageEntitlement
) {
}
