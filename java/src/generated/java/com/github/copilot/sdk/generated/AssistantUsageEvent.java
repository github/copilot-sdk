/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * The {@code assistant.usage} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantUsageEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.usage"; }

    @JsonProperty("data")
    private AssistantUsageEventData data;

    public AssistantUsageEventData getData() { return data; }
    public void setData(AssistantUsageEventData data) { this.data = data; }

    /** Data payload for {@link AssistantUsageEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantUsageEventData(
        /** Model identifier used for this API call */
        @JsonProperty("model") String model,
        /** Number of input tokens consumed */
        @JsonProperty("inputTokens") Double inputTokens,
        /** Number of output tokens produced */
        @JsonProperty("outputTokens") Double outputTokens,
        /** Number of tokens read from prompt cache */
        @JsonProperty("cacheReadTokens") Double cacheReadTokens,
        /** Number of tokens written to prompt cache */
        @JsonProperty("cacheWriteTokens") Double cacheWriteTokens,
        /** Number of output tokens used for reasoning (e.g., chain-of-thought) */
        @JsonProperty("reasoningTokens") Double reasoningTokens,
        /** Model multiplier cost for billing purposes */
        @JsonProperty("cost") Double cost,
        /** Duration of the API call in milliseconds */
        @JsonProperty("duration") Double duration,
        /** Time to first token in milliseconds. Only available for streaming requests */
        @JsonProperty("ttftMs") Double ttftMs,
        /** Average inter-token latency in milliseconds. Only available for streaming requests */
        @JsonProperty("interTokenLatencyMs") Double interTokenLatencyMs,
        /** What initiated this API call (e.g., "sub-agent", "mcp-sampling"); absent for user-initiated calls */
        @JsonProperty("initiator") String initiator,
        /** Completion ID from the model provider (e.g., chatcmpl-abc123) */
        @JsonProperty("apiCallId") String apiCallId,
        /** GitHub request tracing ID (x-github-request-id header) for server-side log correlation */
        @JsonProperty("providerCallId") String providerCallId,
        /** Parent tool call ID when this usage originates from a sub-agent */
        @JsonProperty("parentToolCallId") String parentToolCallId,
        /** Per-quota resource usage snapshots, keyed by quota identifier */
        @JsonProperty("quotaSnapshots") Map<String, AssistantUsageQuotaSnapshot> quotaSnapshots,
        /** Per-request cost and usage data from the CAPI copilot_usage response field */
        @JsonProperty("copilotUsage") AssistantUsageCopilotUsage copilotUsage,
        /** Reasoning effort level used for model calls, if applicable (e.g. "low", "medium", "high", "xhigh") */
        @JsonProperty("reasoningEffort") String reasoningEffort
    ) {
    }
}
