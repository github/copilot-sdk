/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AssistantUsageQuotaSnapshot(
    /** Whether the user has an unlimited usage entitlement */
    @JsonProperty("isUnlimitedEntitlement") Boolean isUnlimitedEntitlement,
    /** Total requests allowed by the entitlement */
    @JsonProperty("entitlementRequests") Double entitlementRequests,
    /** Number of requests already consumed */
    @JsonProperty("usedRequests") Double usedRequests,
    /** Whether usage is still permitted after quota exhaustion */
    @JsonProperty("usageAllowedWithExhaustedQuota") Boolean usageAllowedWithExhaustedQuota,
    /** Number of requests over the entitlement limit */
    @JsonProperty("overage") Double overage,
    /** Whether overage is allowed when quota is exhausted */
    @JsonProperty("overageAllowedWithExhaustedQuota") Boolean overageAllowedWithExhaustedQuota,
    /** Percentage of quota remaining (0.0 to 1.0) */
    @JsonProperty("remainingPercentage") Double remainingPercentage,
    /** Date when the quota resets */
    @JsonProperty("resetDate") OffsetDateTime resetDate
) {
}
