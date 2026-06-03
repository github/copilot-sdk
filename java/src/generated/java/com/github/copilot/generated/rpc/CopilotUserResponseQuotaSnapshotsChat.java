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
 * Schema for the `CopilotUserResponseQuotaSnapshotsChat` type.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CopilotUserResponseQuotaSnapshotsChat(
    @JsonProperty("entitlement") Double entitlement,
    @JsonProperty("overage_count") Double overageCount,
    @JsonProperty("overage_permitted") Boolean overagePermitted,
    @JsonProperty("percent_remaining") Double percentRemaining,
    @JsonProperty("quota_id") String quotaId,
    @JsonProperty("quota_remaining") Double quotaRemaining,
    @JsonProperty("remaining") Double remaining,
    @JsonProperty("unlimited") Boolean unlimited,
    @JsonProperty("timestamp_utc") String timestampUtc,
    @JsonProperty("has_quota") Boolean hasQuota,
    @JsonProperty("quota_reset_at") Double quotaResetAt,
    @JsonProperty("token_based_billing") Boolean tokenBasedBilling
) {
}
