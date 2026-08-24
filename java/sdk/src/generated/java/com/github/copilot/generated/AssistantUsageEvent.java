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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Session event "assistant.usage". LLM API call usage metrics including tokens, costs, quotas, and billing information
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
        @JsonProperty("inputTokens") Long inputTokens,
        /** Number of output tokens produced */
        @JsonProperty("outputTokens") Long outputTokens,
        /** Number of tokens read from prompt cache */
        @JsonProperty("cacheReadTokens") Long cacheReadTokens,
        /** Number of tokens written to prompt cache */
        @JsonProperty("cacheWriteTokens") Long cacheWriteTokens,
        /** Updated prompt-cache expiration for this model call. Present only when the call establishes or refreshes known cache state. */
        @JsonProperty("cacheExpiresAt") OffsetDateTime cacheExpiresAt,
        /** Number of output tokens used for reasoning (e.g., chain-of-thought) */
        @JsonProperty("reasoningTokens") Long reasoningTokens,
        /** Model multiplier cost for billing purposes */
        @JsonProperty("cost") Double cost,
        /** Duration of the API call in milliseconds */
        @JsonProperty("duration") Long duration,
        /** Time to first token in milliseconds. Only available for streaming requests */
        @JsonProperty("timeToFirstTokenMs") Double timeToFirstTokenMs,
        /** Time to first observable model output in milliseconds. Includes text, reasoning, and tool-call output; only available for streaming requests that produce observable output. */
        @JsonProperty("outputTtftMs") Double outputTtftMs,
        /** Average inter-token latency in milliseconds. Only available for streaming requests */
        @JsonProperty("interTokenLatencyMs") Double interTokenLatencyMs,
        /** What initiated this API call (e.g., "sub-agent", "mcp-sampling"); absent for user-initiated calls */
        @JsonProperty("initiator") String initiator,
        /** Coarse classification of the interaction that produced this call, mirroring the session's per-request agent context (e.g. `conversation-agent`, `conversation-subagent`, `conversation-sampling`, `conversation-background`, `conversation-compaction`, `conversation-user`). Non-billing; lets consumers attribute a model call to a call class (e.g. sub-agent/sidekick) independently of the billing initiator. Absent when the runtime did not classify the request. */
        @JsonProperty("interactionType") String interactionType,
        /** Whether this model call used a bring-your-own-key provider */
        @JsonProperty("isByok") Boolean isByok,
        /** Whether Auto mode was selected for this model call */
        @JsonProperty("isAuto") Boolean isAuto,
        /** Effective maximum prompt-token limit used for this model call */
        @JsonProperty("maxPromptTokens") Long maxPromptTokens,
        /** Requested maximum output tokens used for this model call */
        @JsonProperty("maxOutputTokens") Long maxOutputTokens,
        /** Number of accepted speculative prediction tokens */
        @JsonProperty("acceptedPredictionTokens") Long acceptedPredictionTokens,
        /** Number of rejected speculative prediction tokens */
        @JsonProperty("rejectedPredictionTokens") Long rejectedPredictionTokens,
        /** Transport used for this model call (http or websocket) */
        @JsonProperty("transport") AssistantUsageTransport transport,
        /** Completion ID from the model provider (e.g., chatcmpl-abc123) */
        @JsonProperty("apiCallId") String apiCallId,
        /** GitHub request tracing ID (x-github-request-id header) for server-side log correlation */
        @JsonProperty("providerCallId") String providerCallId,
        /** Copilot service request ID (x-copilot-service-request-id header) for CAPI log correlation */
        @JsonProperty("serviceRequestId") String serviceRequestId,
        /** Per-request treatment/eligibility signal returned by the Copilot API in the `X-GitHub-Copilot-Request-TE` response header for the associated model call; `false` when the header was absent or unparseable. */
        @JsonProperty("rte") Boolean rte,
        /** API endpoint used for this model call, matching CAPI supported_endpoints vocabulary */
        @JsonProperty("apiEndpoint") AssistantUsageApiEndpoint apiEndpoint,
        /** Parent tool call ID when this usage originates from a sub-agent */
        @JsonProperty("parentToolCallId") String parentToolCallId,
        /** Per-quota resource usage snapshots, keyed by quota identifier */
        @JsonProperty("quotaSnapshots") Map<String, AssistantUsageQuotaSnapshot> quotaSnapshots,
        /** Per-request cost and usage data from the CAPI copilot_usage response field */
        @JsonProperty("copilotUsage") AssistantUsageCopilotUsage copilotUsage,
        /** Reasoning effort level used for model calls, if applicable (e.g. "none", "low", "medium", "high", "xhigh", "max") */
        @JsonProperty("reasoningEffort") String reasoningEffort,
        /** Reasoning summary mode used for this model call, if applicable */
        @JsonProperty("reasoningSummary") ReasoningSummary reasoningSummary,
        /** Number of tools available to the model for this call */
        @JsonProperty("availableToolCount") Long availableToolCount,
        /** Number of tokens used by tool definitions for this call */
        @JsonProperty("toolTokenCount") Long toolTokenCount,
        /** How the prompt-cache frontier was determined for this call */
        @JsonProperty("frontierSource") String frontierSource,
        /** Effective prompt-cache lifetime in seconds for this call */
        @JsonProperty("cacheTtlSeconds") Long cacheTtlSeconds,
        /** Whether the provider reported prompt-cache usage details for this call */
        @JsonProperty("cacheDetailsReported") Boolean cacheDetailsReported,
        /** Number of tool calls returned by the model */
        @JsonProperty("numToolCalls") Long numToolCalls,
        /** Tool-call counts keyed by tool name */
        @JsonProperty("toolCounts") Map<String, Long> toolCounts,
        /** Finish reason reported by the model for this API call (e.g. "stop", "length", "tool_calls", "content_filter"). Normalized to OpenAI vocabulary; for Anthropic models a "refusal" stop reason maps to "content_filter". */
        @JsonProperty("finishReason") String finishReason,
        /** Whether the model response was blocked or truncated by content filtering (finish_reason === 'content_filter'). For Anthropic models this corresponds to a 'refusal' stop reason. */
        @JsonProperty("contentFilterTriggered") Boolean contentFilterTriggered
    ) {
    }
}
