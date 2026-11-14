/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Authoritative usage reported by a separately metered runtime-owned tool operation. May be present on a failed tool when the service reported usage before local result processing failed. Missing token counts or billing mean unavailable, not zero.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolExecutionUsage(
    /** Runtime-generated identifier for this metered operation. Deduplicates repeated delivery or replay; not an upstream billing identifier. */
    @JsonProperty("requestId") String requestId,
    /** Requested model for the tool operation, independent of the conversation model. Not a provider-confirmed serving model. */
    @JsonProperty("model") String model,
    /** Provider responsible for the tool operation, captured independently of the conversation provider. */
    @JsonProperty("provider") ModelProviderRef provider,
    /** Runtime-owned accounting order, preserved across session checkpoints and replay. */
    @JsonProperty("accounting") UsageAccountingIdentity accounting,
    /** Total provider-reported input tokens across all modalities. */
    @JsonProperty("inputTokens") Long inputTokens,
    /** Total provider-reported output tokens across all modalities. */
    @JsonProperty("outputTokens") Long outputTokens,
    /** Provider-reported sum of input and output tokens. */
    @JsonProperty("totalTokens") Long totalTokens,
    /** Optional input modality breakdown; these counts are already included in inputTokens. */
    @JsonProperty("inputTokensDetails") ToolExecutionUsageTokenDetails inputTokensDetails,
    /** Optional output modality breakdown; these counts are already included in outputTokens. */
    @JsonProperty("outputTokensDetails") ToolExecutionUsageTokenDetails outputTokensDetails,
    /** Server-reported charge. Divide totalNanoAiu by 1,000,000,000 for AI credits; do not calculate customer cost from token prices. */
    @JsonProperty("copilotUsage") AssistantUsageCopilotUsage copilotUsage,
    /** Account quota snapshots, not per-operation cost deltas. */
    @JsonProperty("quotaSnapshots") Map<String, Object> quotaSnapshots
) {
}
