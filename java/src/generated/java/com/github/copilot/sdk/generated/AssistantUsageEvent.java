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
import java.util.List;
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
        @JsonProperty("quotaSnapshots") Map<String, AssistantUsageEventDataQuotaSnapshotsValue> quotaSnapshots,
        /** Per-request cost and usage data from the CAPI copilot_usage response field */
        @JsonProperty("copilotUsage") AssistantUsageEventDataCopilotUsage copilotUsage,
        /** Reasoning effort level used for model calls, if applicable (e.g. "low", "medium", "high", "xhigh") */
        @JsonProperty("reasoningEffort") String reasoningEffort
    ) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record AssistantUsageEventDataQuotaSnapshotsValue(
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

        /** Per-request cost and usage data from the CAPI copilot_usage response field */
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record AssistantUsageEventDataCopilotUsage(
            /** Itemized token usage breakdown */
            @JsonProperty("tokenDetails") List<AssistantUsageEventDataCopilotUsageTokenDetailsItem> tokenDetails,
            /** Total cost in nano-AIU (AI Units) for this request */
            @JsonProperty("totalNanoAiu") Double totalNanoAiu
        ) {

            /** Token usage detail for a single billing category */
            @JsonIgnoreProperties(ignoreUnknown = true)
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public record AssistantUsageEventDataCopilotUsageTokenDetailsItem(
                /** Number of tokens in this billing batch */
                @JsonProperty("batchSize") Double batchSize,
                /** Cost per batch of tokens */
                @JsonProperty("costPerBatch") Double costPerBatch,
                /** Total token count for this entry */
                @JsonProperty("tokenCount") Double tokenCount,
                /** Token category (e.g., "input", "output") */
                @JsonProperty("tokenType") String tokenType
            ) {
            }
        }
    }
}
