/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Result for the {@code account.getQuota} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountGetQuotaResult(
    /** Quota snapshots keyed by type (e.g., chat, completions, premium_interactions) */
    @JsonProperty("quotaSnapshots") Map<String, AccountGetQuotaResultQuotaSnapshotsValue> quotaSnapshots
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AccountGetQuotaResultQuotaSnapshotsValue(
        /** Number of requests included in the entitlement */
        @JsonProperty("entitlementRequests") Double entitlementRequests,
        /** Number of requests used so far this period */
        @JsonProperty("usedRequests") Double usedRequests,
        /** Percentage of entitlement remaining */
        @JsonProperty("remainingPercentage") Double remainingPercentage,
        /** Number of overage requests made this period */
        @JsonProperty("overage") Double overage,
        /** Whether pay-per-request usage is allowed when quota is exhausted */
        @JsonProperty("overageAllowedWithExhaustedQuota") Boolean overageAllowedWithExhaustedQuota,
        /** Date when the quota resets (ISO 8601) */
        @JsonProperty("resetDate") String resetDate
    ) {
    }
}
