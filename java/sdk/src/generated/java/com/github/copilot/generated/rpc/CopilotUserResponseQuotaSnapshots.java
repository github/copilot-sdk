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
 * Quota snapshot map from the raw Copilot user-response passthrough, with chat, completions, premium-interactions, and other entries.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CopilotUserResponseQuotaSnapshots(
    /** Chat quota snapshot from the raw Copilot user-response passthrough, with entitlement, overage, remaining quota, reset, and billing fields. */
    @JsonProperty("chat") CopilotUserResponseQuotaSnapshotsChat chat,
    /** Completions quota snapshot from the raw Copilot user-response passthrough, with entitlement, overage, remaining quota, reset, and billing fields. */
    @JsonProperty("completions") CopilotUserResponseQuotaSnapshotsCompletions completions,
    /** Premium-interactions quota snapshot from the raw Copilot user-response passthrough, with entitlement, overage, remaining quota, reset, and billing fields. */
    @JsonProperty("premium_interactions") CopilotUserResponseQuotaSnapshotsPremiumInteractions premiumInteractions
) {
}
