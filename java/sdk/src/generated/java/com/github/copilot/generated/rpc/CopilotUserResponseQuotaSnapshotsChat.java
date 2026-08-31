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
 * Chat quota snapshot from the raw Copilot user-response passthrough, with entitlement, overage, remaining quota, reset, and billing fields.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CopilotUserResponseQuotaSnapshotsChat(
    /** Number of requests/units included in the entitlement for this period; `-1` denotes an unlimited entitlement. */
    @JsonProperty("entitlement") Double entitlement,
    /** Count of additional pay-per-request usage consumed this period beyond the entitlement. */
    @JsonProperty("overage_count") Double overageCount,
    /** Whether usage may continue at pay-per-request rates once the entitlement is exhausted. */
    @JsonProperty("overage_permitted") Boolean overagePermitted,
    /** Percentage of the entitlement remaining at the snapshot timestamp. */
    @JsonProperty("percent_remaining") Double percentRemaining,
    /** Identifier of the quota bucket this snapshot describes. */
    @JsonProperty("quota_id") String quotaId,
    /** Amount of quota remaining at the snapshot timestamp. */
    @JsonProperty("quota_remaining") Double quotaRemaining,
    /** Remaining entitlement/quota amount at the snapshot timestamp. */
    @JsonProperty("remaining") Double remaining,
    /** Whether the entitlement for this category is unlimited. */
    @JsonProperty("unlimited") Boolean unlimited,
    /** UTC timestamp when this snapshot was captured. */
    @JsonProperty("timestamp_utc") String timestampUtc,
    /** Whether the user currently has quota available; when `false` and not unlimited, further requests are blocked until the quota resets. */
    @JsonProperty("has_quota") Boolean hasQuota,
    /** Unix epoch time, in seconds, when this quota next resets. */
    @JsonProperty("quota_reset_at") Double quotaResetAt,
    /** Whether this category uses usage-based (token/AI-credit) billing rather than a fixed premium-request count. */
    @JsonProperty("token_based_billing") Boolean tokenBasedBilling
) {
}
