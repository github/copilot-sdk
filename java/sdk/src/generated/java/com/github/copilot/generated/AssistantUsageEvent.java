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
import java.time.OffsetDateTime;
import java.util.List;
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
        /** Availability of this call's reported AI-credit amount. Missing billing is unavailable, not a zero charge. */
        @JsonProperty("aiCreditsStatus") AiCreditsStatus aiCreditsStatus,
        /** Model identifier used for this API call */
        @JsonProperty("model") String model,
        /** Provider identity and product label captured by the actual call's dispatch path. */
        @JsonProperty("provider") ModelProviderRef provider,
        /** Model display name captured for this call, not derived from the current selection. */
        @JsonProperty("modelDisplayName") String modelDisplayName,
        /** Runtime-owned identity shared with the durable accounting receipt. */
        @JsonProperty("accounting") UsageAccountingIdentity accounting,
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
        /** Number of prior thinking blocks the provider dropped while transforming the request */
        @JsonProperty("thinkingDroppedBlocks") Long thinkingDroppedBlocks,
        /** Recognized provider-reported reasons for dropped thinking blocks, in response order */
        @JsonProperty("thinkingDroppedReasons") List<String> thinkingDroppedReasons,
        /** Model multiplier cost for billing purposes */
        @CopilotExperimental
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
        /** Where the bring-your-own-key model runs and who manages it: "local_managed" (on the device, managed by Copilot), "local_user" (on the device, managed by the user), or "remote_user" (off the device, managed by the user). Absent for Copilot-served models. */
        @JsonProperty("byokKind") String byokKind,
        /** Fixed-set provider family serving the bring-your-own-key model (for example "openai", "anthropic", "azure_openai", "ollama", "llama_cpp", or "other"). Never the caller-supplied provider name. Absent for Copilot-served models. */
        @JsonProperty("modelProvider") String modelProvider,
        /** Fixed-set configured provider type of the bring-your-own-key model: "openai", "azure", "anthropic", "local_model" (a user-configured local model), "winml" (a Copilot-managed Windows ML model), or "other". Never the caller-supplied provider name. Absent for Copilot-served models. */
        @JsonProperty("providerType") String providerType,
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
        /** Whether this dispatched request selected the managed inference gateway. Absent for older producers or when no request was dispatched; not proof of gateway receipt. */
        @JsonProperty("inferenceGateway") Boolean inferenceGateway,
        /** Serialized (uncompressed) byte length of the request body, before any Content-Encoding. A content-free size signal. */
        @JsonProperty("requestBodyBytes") Long requestBodyBytes,
        /** Byte length of the request body actually sent on the wire for the attempt that produced this outcome, after any Content-Encoding. Equals requestBodyBytes when the body was not compressed. */
        @JsonProperty("requestBodyWireBytes") Long requestBodyWireBytes,
        /** Content-Encoding applied to the request body of the attempt that produced this outcome; identity when uncompressed */
        @JsonProperty("requestBodyEncoding") ModelCallRequestBodyEncoding requestBodyEncoding,
        /** Uncompressed fallback that produced this outcome; absent when the body was sent with the requested encoding */
        @JsonProperty("requestBodyCompressionFallback") ModelCallRequestBodyCompressionFallback requestBodyCompressionFallback,
        /** Content-Encoding the request asked for before the uncompressed fallback; present only with requestBodyCompressionFallback */
        @JsonProperty("requestBodyCompressionFallbackFrom") ModelCallRequestBodyEncoding requestBodyCompressionFallbackFrom,
        /** HTTP status that rejected the compressed body before the uncompressed resend; present only when requestBodyCompressionFallback is retried */
        @JsonProperty("requestBodyCompressionRejectedStatus") Long requestBodyCompressionRejectedStatus,
        /** Why the call was carried by the HTTP fallback of a WebSocket-capable dispatcher; absent when no fallback occurred */
        @JsonProperty("websocketFallbackReason") ModelCallWebSocketFallbackReason websocketFallbackReason,
        /** Milliseconds spent on the WebSocket attempt before falling back to HTTP */
        @JsonProperty("websocketFallbackAfterMs") Long websocketFallbackAfterMs,
        /** Normalized cause of the WebSocket failure that triggered the HTTP fallback; absent when no fallback occurred or the cause is unknown */
        @JsonProperty("websocketFallbackErrorKind") ModelCallWebSocketFallbackErrorKind websocketFallbackErrorKind,
        /** HTTP status of the WebSocket upgrade rejection or WebSocket API error that triggered the HTTP fallback */
        @JsonProperty("websocketFallbackStatusCode") Long websocketFallbackStatusCode,
        /** WebSocket close code received before the HTTP fallback */
        @JsonProperty("websocketFallbackCloseCode") Long websocketFallbackCloseCode,
        /** Whether a WebSocket reconnect was attempted before falling back to HTTP */
        @JsonProperty("websocketFallbackReconnectAttempted") Boolean websocketFallbackReconnectAttempted,
        /** Whether the HTTP fallback started during this model call, rather than being inherited from an earlier call */
        @JsonProperty("websocketFallbackStartedThisCall") Boolean websocketFallbackStartedThisCall,
        /** Completion ID from the model provider (e.g., chatcmpl-abc123) */
        @JsonProperty("apiCallId") String apiCallId,
        /** GitHub request tracing ID (x-github-request-id header) for server-side log correlation */
        @JsonProperty("providerCallId") String providerCallId,
        /** Copilot service request ID (x-copilot-service-request-id header) for CAPI log correlation */
        @JsonProperty("serviceRequestId") String serviceRequestId,
        /** Internal correlation identifier for the qualifying instruction-update delivery consumed by this model request. */
        @JsonProperty("instructionUpdateDeliveryId") String instructionUpdateDeliveryId,
        /** Event identifier of the latest canonical instruction update included in this model request. */
        @JsonProperty("instructionUpdateLatestEventId") String instructionUpdateLatestEventId,
        /** Per-request treatment/eligibility signal returned by the Copilot API in the `X-GitHub-Copilot-Request-TE` response header for the associated model call; `false` when the header was absent or unparseable. */
        @JsonProperty("rte") Boolean rte,
        /** API endpoint used for this model call, matching CAPI supported_endpoints vocabulary */
        @JsonProperty("apiEndpoint") AssistantUsageApiEndpoint apiEndpoint,
        /** Parent tool call ID when this usage originates from a sub-agent */
        @Deprecated
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
        /** Free-form custom tool calls in the request history before request shaping, counted apart from named function calls */
        @JsonProperty("customToolCallCount") Long customToolCallCount,
        /** Tool results in the request history before request shaping that answer no earlier unanswered tool call */
        @JsonProperty("unmatchedToolResultCount") Long unmatchedToolResultCount,
        /** Tool calls in the request history before request shaping that no later tool result answers */
        @JsonProperty("orphanToolCallCount") Long orphanToolCallCount,
        /** How the prompt-cache frontier was determined for this call */
        @JsonProperty("frontierSource") String frontierSource,
        /** Effective prompt-cache lifetime in seconds for this call */
        @JsonProperty("cacheTtlSeconds") Long cacheTtlSeconds,
        /** Request-scoped prompt-cache policy lifetime in seconds; the shortest lifetime for mixed known policies. Does not establish or refresh cache state. Omitted when the policy is unknown. */
        @JsonProperty("cachePolicyTtlSeconds") Long cachePolicyTtlSeconds,
        /** Why the adaptive Anthropic prompt-cache policy chose this call's TTL: warmup, l1_first, l1_lapsed, l1_hot, g_guard, or ineligible. Omitted outside the adaptive TTL experiment. */
        @JsonProperty("promptCacheTtlReason") String promptCacheTtlReason,
        /** Bucket of AI credits the session had billed before this call, for the adaptive Anthropic prompt-cache policy. Omitted outside the adaptive TTL experiment. */
        @JsonProperty("promptCacheCreditsBucket") String promptCacheCreditsBucket,
        /** Whether the provider reported prompt-cache usage details for this call */
        @JsonProperty("cacheDetailsReported") Boolean cacheDetailsReported,
        /** Number of tool calls returned by the model */
        @JsonProperty("numToolCalls") Long numToolCalls,
        /** Tool-call counts keyed by tool name */
        @JsonProperty("toolCounts") Map<String, Long> toolCounts,
        /** Finish reason reported by the model for this API call (e.g. "stop", "length", "tool_calls", "content_filter"). Normalized to OpenAI vocabulary; for Anthropic models a "refusal" stop reason maps to "content_filter". */
        @JsonProperty("finishReason") String finishReason,
        /** Whether the model response was blocked or truncated by content filtering (finish_reason === 'content_filter'). For Anthropic models this corresponds to a 'refusal' stop reason. */
        @JsonProperty("contentFilterTriggered") Boolean contentFilterTriggered,
        /** Experimental HydraFusion attribution for this concrete model call's usage. */
        @CopilotExperimental
        @JsonProperty("fusion") FusionAttribution fusion,
        /** Model identifier returned by the provider, absent when the response does not identify it. */
        @JsonProperty("responseModel") String responseModel
    ) {

        /**
         * Creates a record with the components it had before later optional fields were added.
         * @deprecated Use named construction via {@link #builder()}.
         * @param aiCreditsStatus Availability of this call's reported AI-credit amount. Missing billing is unavailable, not a zero charge.
         * @param model Model identifier used for this API call
         * @param provider Provider identity and product label captured by the actual call's dispatch path.
         * @param modelDisplayName Model display name captured for this call, not derived from the current selection.
         * @param accounting Runtime-owned identity shared with the durable accounting receipt.
         * @param inputTokens Number of input tokens consumed
         * @param outputTokens Number of output tokens produced
         * @param cacheReadTokens Number of tokens read from prompt cache
         * @param cacheWriteTokens Number of tokens written to prompt cache
         * @param cacheExpiresAt Updated prompt-cache expiration for this model call. Present only when the call establishes or refreshes known cache state.
         * @param reasoningTokens Number of output tokens used for reasoning (e.g., chain-of-thought)
         * @param thinkingDroppedBlocks Number of prior thinking blocks the provider dropped while transforming the request
         * @param thinkingDroppedReasons Recognized provider-reported reasons for dropped thinking blocks, in response order
         * @param cost Model multiplier cost for billing purposes
         * @param duration Duration of the API call in milliseconds
         * @param timeToFirstTokenMs Time to first token in milliseconds. Only available for streaming requests
         * @param outputTtftMs Time to first observable model output in milliseconds. Includes text, reasoning, and tool-call output; only available for streaming requests that produce observable output.
         * @param interTokenLatencyMs Average inter-token latency in milliseconds. Only available for streaming requests
         * @param initiator What initiated this API call (e.g., "sub-agent", "mcp-sampling"); absent for user-initiated calls
         * @param interactionType Coarse classification of the interaction that produced this call, mirroring the session's per-request agent context (e.g. `conversation-agent`, `conversation-subagent`, `conversation-sampling`, `conversation-background`, `conversation-compaction`, `conversation-user`). Non-billing; lets consumers attribute a model call to a call class (e.g. sub-agent/sidekick) independently of the billing initiator. Absent when the runtime did not classify the request.
         * @param isByok Whether this model call used a bring-your-own-key provider
         * @param byokKind Where the bring-your-own-key model runs and who manages it: "local_managed" (on the device, managed by Copilot), "local_user" (on the device, managed by the user), or "remote_user" (off the device, managed by the user). Absent for Copilot-served models.
         * @param modelProvider Fixed-set provider family serving the bring-your-own-key model (for example "openai", "anthropic", "azure_openai", "ollama", "llama_cpp", or "other"). Never the caller-supplied provider name. Absent for Copilot-served models.
         * @param providerType Fixed-set configured provider type of the bring-your-own-key model: "openai", "azure", "anthropic", "local_model" (a user-configured local model), "winml" (a Copilot-managed Windows ML model), or "other". Never the caller-supplied provider name. Absent for Copilot-served models.
         * @param isAuto Whether Auto mode was selected for this model call
         * @param maxPromptTokens Effective maximum prompt-token limit used for this model call
         * @param maxOutputTokens Requested maximum output tokens used for this model call
         * @param acceptedPredictionTokens Number of accepted speculative prediction tokens
         * @param rejectedPredictionTokens Number of rejected speculative prediction tokens
         * @param transport Transport used for this model call (http or websocket)
         * @param requestBodyBytes Serialized (uncompressed) byte length of the request body, before any Content-Encoding. A content-free size signal.
         * @param requestBodyWireBytes Byte length of the request body actually sent on the wire for the attempt that produced this outcome, after any Content-Encoding. Equals requestBodyBytes when the body was not compressed.
         * @param requestBodyEncoding Content-Encoding applied to the request body of the attempt that produced this outcome; identity when uncompressed
         * @param requestBodyCompressionFallback Uncompressed fallback that produced this outcome; absent when the body was sent with the requested encoding
         * @param requestBodyCompressionFallbackFrom Content-Encoding the request asked for before the uncompressed fallback; present only with requestBodyCompressionFallback
         * @param requestBodyCompressionRejectedStatus HTTP status that rejected the compressed body before the uncompressed resend; present only when requestBodyCompressionFallback is retried
         * @param websocketFallbackReason Why the call was carried by the HTTP fallback of a WebSocket-capable dispatcher; absent when no fallback occurred
         * @param websocketFallbackAfterMs Milliseconds spent on the WebSocket attempt before falling back to HTTP
         * @param websocketFallbackErrorKind Normalized cause of the WebSocket failure that triggered the HTTP fallback; absent when no fallback occurred or the cause is unknown
         * @param websocketFallbackStatusCode HTTP status of the WebSocket upgrade rejection or WebSocket API error that triggered the HTTP fallback
         * @param websocketFallbackCloseCode WebSocket close code received before the HTTP fallback
         * @param websocketFallbackReconnectAttempted Whether a WebSocket reconnect was attempted before falling back to HTTP
         * @param websocketFallbackStartedThisCall Whether the HTTP fallback started during this model call, rather than being inherited from an earlier call
         * @param apiCallId Completion ID from the model provider (e.g., chatcmpl-abc123)
         * @param providerCallId GitHub request tracing ID (x-github-request-id header) for server-side log correlation
         * @param serviceRequestId Copilot service request ID (x-copilot-service-request-id header) for CAPI log correlation
         * @param rte Per-request treatment/eligibility signal returned by the Copilot API in the `X-GitHub-Copilot-Request-TE` response header for the associated model call; `false` when the header was absent or unparseable.
         * @param apiEndpoint API endpoint used for this model call, matching CAPI supported_endpoints vocabulary
         * @param parentToolCallId Parent tool call ID when this usage originates from a sub-agent
         * @param quotaSnapshots Per-quota resource usage snapshots, keyed by quota identifier
         * @param copilotUsage Per-request cost and usage data from the CAPI copilot_usage response field
         * @param reasoningEffort Reasoning effort level used for model calls, if applicable (e.g. "none", "low", "medium", "high", "xhigh", "max")
         * @param reasoningSummary Reasoning summary mode used for this model call, if applicable
         * @param availableToolCount Number of tools available to the model for this call
         * @param toolTokenCount Number of tokens used by tool definitions for this call
         * @param customToolCallCount Free-form custom tool calls in the request history before request shaping, counted apart from named function calls
         * @param unmatchedToolResultCount Tool results in the request history before request shaping that answer no earlier unanswered tool call
         * @param orphanToolCallCount Tool calls in the request history before request shaping that no later tool result answers
         * @param frontierSource How the prompt-cache frontier was determined for this call
         * @param cacheTtlSeconds Effective prompt-cache lifetime in seconds for this call
         * @param cachePolicyTtlSeconds Request-scoped prompt-cache policy lifetime in seconds; the shortest lifetime for mixed known policies. Does not establish or refresh cache state. Omitted when the policy is unknown.
         * @param promptCacheTtlReason Why the adaptive Anthropic prompt-cache policy chose this call's TTL: warmup, l1_first, l1_lapsed, l1_hot, g_guard, or ineligible. Omitted outside the adaptive TTL experiment.
         * @param promptCacheCreditsBucket Bucket of AI credits the session had billed before this call, for the adaptive Anthropic prompt-cache policy. Omitted outside the adaptive TTL experiment.
         * @param cacheDetailsReported Whether the provider reported prompt-cache usage details for this call
         * @param numToolCalls Number of tool calls returned by the model
         * @param toolCounts Tool-call counts keyed by tool name
         * @param finishReason Finish reason reported by the model for this API call (e.g. "stop", "length", "tool_calls", "content_filter"). Normalized to OpenAI vocabulary; for Anthropic models a "refusal" stop reason maps to "content_filter".
         * @param contentFilterTriggered Whether the model response was blocked or truncated by content filtering (finish_reason === 'content_filter'). For Anthropic models this corresponds to a 'refusal' stop reason.
         * @param fusion Experimental HydraFusion attribution for this concrete model call's usage.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AssistantUsageEventData(
            AiCreditsStatus aiCreditsStatus,
            String model,
            ModelProviderRef provider,
            String modelDisplayName,
            UsageAccountingIdentity accounting,
            Long inputTokens,
            Long outputTokens,
            Long cacheReadTokens,
            Long cacheWriteTokens,
            OffsetDateTime cacheExpiresAt,
            Long reasoningTokens,
            Long thinkingDroppedBlocks,
            List<String> thinkingDroppedReasons,
            Double cost,
            Long duration,
            Double timeToFirstTokenMs,
            Double outputTtftMs,
            Double interTokenLatencyMs,
            String initiator,
            String interactionType,
            Boolean isByok,
            String byokKind,
            String modelProvider,
            String providerType,
            Boolean isAuto,
            Long maxPromptTokens,
            Long maxOutputTokens,
            Long acceptedPredictionTokens,
            Long rejectedPredictionTokens,
            AssistantUsageTransport transport,
            Long requestBodyBytes,
            Long requestBodyWireBytes,
            ModelCallRequestBodyEncoding requestBodyEncoding,
            ModelCallRequestBodyCompressionFallback requestBodyCompressionFallback,
            ModelCallRequestBodyEncoding requestBodyCompressionFallbackFrom,
            Long requestBodyCompressionRejectedStatus,
            ModelCallWebSocketFallbackReason websocketFallbackReason,
            Long websocketFallbackAfterMs,
            ModelCallWebSocketFallbackErrorKind websocketFallbackErrorKind,
            Long websocketFallbackStatusCode,
            Long websocketFallbackCloseCode,
            Boolean websocketFallbackReconnectAttempted,
            Boolean websocketFallbackStartedThisCall,
            String apiCallId,
            String providerCallId,
            String serviceRequestId,
            Boolean rte,
            AssistantUsageApiEndpoint apiEndpoint,
            String parentToolCallId,
            Map<String, AssistantUsageQuotaSnapshot> quotaSnapshots,
            AssistantUsageCopilotUsage copilotUsage,
            String reasoningEffort,
            ReasoningSummary reasoningSummary,
            Long availableToolCount,
            Long toolTokenCount,
            Long customToolCallCount,
            Long unmatchedToolResultCount,
            Long orphanToolCallCount,
            String frontierSource,
            Long cacheTtlSeconds,
            Long cachePolicyTtlSeconds,
            String promptCacheTtlReason,
            String promptCacheCreditsBucket,
            Boolean cacheDetailsReported,
            Long numToolCalls,
            Map<String, Long> toolCounts,
            String finishReason,
            Boolean contentFilterTriggered,
            FusionAttribution fusion
        ) {
            this(aiCreditsStatus, model, provider, modelDisplayName, accounting, inputTokens, outputTokens, cacheReadTokens, cacheWriteTokens, cacheExpiresAt, reasoningTokens, thinkingDroppedBlocks, thinkingDroppedReasons, cost, duration, timeToFirstTokenMs, outputTtftMs, interTokenLatencyMs, initiator, interactionType, isByok, byokKind, modelProvider, providerType, isAuto, maxPromptTokens, maxOutputTokens, acceptedPredictionTokens, rejectedPredictionTokens, transport, null, requestBodyBytes, requestBodyWireBytes, requestBodyEncoding, requestBodyCompressionFallback, requestBodyCompressionFallbackFrom, requestBodyCompressionRejectedStatus, websocketFallbackReason, websocketFallbackAfterMs, websocketFallbackErrorKind, websocketFallbackStatusCode, websocketFallbackCloseCode, websocketFallbackReconnectAttempted, websocketFallbackStartedThisCall, apiCallId, providerCallId, serviceRequestId, null, null, rte, apiEndpoint, parentToolCallId, quotaSnapshots, copilotUsage, reasoningEffort, reasoningSummary, availableToolCount, toolTokenCount, customToolCallCount, unmatchedToolResultCount, orphanToolCallCount, frontierSource, cacheTtlSeconds, cachePolicyTtlSeconds, promptCacheTtlReason, promptCacheCreditsBucket, cacheDetailsReported, numToolCalls, toolCounts, finishReason, contentFilterTriggered, fusion, null);
        }

        /**
         * Retains the constructor published in Java SDK 1.0.28.
         * @deprecated Use named construction via {@link #builder()}.
         * @param aiCreditsStatus Availability of this call's reported AI-credit amount. Missing billing is unavailable, not a zero charge.
         * @param model Model identifier used for this API call
         * @param provider Provider identity and product label captured by the actual call's dispatch path.
         * @param modelDisplayName Model display name captured for this call, not derived from the current selection.
         * @param accounting Runtime-owned identity shared with the durable accounting receipt.
         * @param inputTokens Number of input tokens consumed
         * @param outputTokens Number of output tokens produced
         * @param cacheReadTokens Number of tokens read from prompt cache
         * @param cacheWriteTokens Number of tokens written to prompt cache
         * @param cacheExpiresAt Updated prompt-cache expiration for this model call. Present only when the call establishes or refreshes known cache state.
         * @param reasoningTokens Number of output tokens used for reasoning (e.g., chain-of-thought)
         * @param thinkingDroppedBlocks Number of prior thinking blocks the provider dropped while transforming the request
         * @param thinkingDroppedReasons Recognized provider-reported reasons for dropped thinking blocks, in response order
         * @param cost Model multiplier cost for billing purposes
         * @param duration Duration of the API call in milliseconds
         * @param timeToFirstTokenMs Time to first token in milliseconds. Only available for streaming requests
         * @param outputTtftMs Time to first observable model output in milliseconds. Includes text, reasoning, and tool-call output; only available for streaming requests that produce observable output.
         * @param interTokenLatencyMs Average inter-token latency in milliseconds. Only available for streaming requests
         * @param initiator What initiated this API call (e.g., "sub-agent", "mcp-sampling"); absent for user-initiated calls
         * @param interactionType Coarse classification of the interaction that produced this call, mirroring the session's per-request agent context (e.g. `conversation-agent`, `conversation-subagent`, `conversation-sampling`, `conversation-background`, `conversation-compaction`, `conversation-user`). Non-billing; lets consumers attribute a model call to a call class (e.g. sub-agent/sidekick) independently of the billing initiator. Absent when the runtime did not classify the request.
         * @param isByok Whether this model call used a bring-your-own-key provider
         * @param byokKind Where the bring-your-own-key model runs and who manages it: "local_managed" (on the device, managed by Copilot), "local_user" (on the device, managed by the user), or "remote_user" (off the device, managed by the user). Absent for Copilot-served models.
         * @param modelProvider Fixed-set provider family serving the bring-your-own-key model (for example "openai", "anthropic", "azure_openai", "ollama", "llama_cpp", or "other"). Never the caller-supplied provider name. Absent for Copilot-served models.
         * @param providerType Fixed-set configured provider type of the bring-your-own-key model: "openai", "azure", "anthropic", "local_model" (a user-configured local model), "winml" (a Copilot-managed Windows ML model), or "other". Never the caller-supplied provider name. Absent for Copilot-served models.
         * @param isAuto Whether Auto mode was selected for this model call
         * @param maxPromptTokens Effective maximum prompt-token limit used for this model call
         * @param maxOutputTokens Requested maximum output tokens used for this model call
         * @param acceptedPredictionTokens Number of accepted speculative prediction tokens
         * @param rejectedPredictionTokens Number of rejected speculative prediction tokens
         * @param transport Transport used for this model call (http or websocket)
         * @param requestBodyBytes Serialized (uncompressed) byte length of the request body, before any Content-Encoding. A content-free size signal.
         * @param requestBodyWireBytes Byte length of the request body actually sent on the wire for the attempt that produced this outcome, after any Content-Encoding. Equals requestBodyBytes when the body was not compressed.
         * @param requestBodyEncoding Content-Encoding applied to the request body of the attempt that produced this outcome; identity when uncompressed
         * @param requestBodyCompressionFallback Uncompressed fallback that produced this outcome; absent when the body was sent with the requested encoding
         * @param requestBodyCompressionFallbackFrom Content-Encoding the request asked for before the uncompressed fallback; present only with requestBodyCompressionFallback
         * @param requestBodyCompressionRejectedStatus HTTP status that rejected the compressed body before the uncompressed resend; present only when requestBodyCompressionFallback is retried
         * @param websocketFallbackReason Why the call was carried by the HTTP fallback of a WebSocket-capable dispatcher; absent when no fallback occurred
         * @param websocketFallbackAfterMs Milliseconds spent on the WebSocket attempt before falling back to HTTP
         * @param websocketFallbackErrorKind Normalized cause of the WebSocket failure that triggered the HTTP fallback; absent when no fallback occurred or the cause is unknown
         * @param websocketFallbackStatusCode HTTP status of the WebSocket upgrade rejection or WebSocket API error that triggered the HTTP fallback
         * @param websocketFallbackCloseCode WebSocket close code received before the HTTP fallback
         * @param websocketFallbackReconnectAttempted Whether a WebSocket reconnect was attempted before falling back to HTTP
         * @param websocketFallbackStartedThisCall Whether the HTTP fallback started during this model call, rather than being inherited from an earlier call
         * @param apiCallId Completion ID from the model provider (e.g., chatcmpl-abc123)
         * @param providerCallId GitHub request tracing ID (x-github-request-id header) for server-side log correlation
         * @param serviceRequestId Copilot service request ID (x-copilot-service-request-id header) for CAPI log correlation
         * @param rte Per-request treatment/eligibility signal returned by the Copilot API in the `X-GitHub-Copilot-Request-TE` response header for the associated model call; `false` when the header was absent or unparseable.
         * @param apiEndpoint API endpoint used for this model call, matching CAPI supported_endpoints vocabulary
         * @param parentToolCallId Parent tool call ID when this usage originates from a sub-agent
         * @param quotaSnapshots Per-quota resource usage snapshots, keyed by quota identifier
         * @param copilotUsage Per-request cost and usage data from the CAPI copilot_usage response field
         * @param reasoningEffort Reasoning effort level used for model calls, if applicable (e.g. "none", "low", "medium", "high", "xhigh", "max")
         * @param reasoningSummary Reasoning summary mode used for this model call, if applicable
         * @param availableToolCount Number of tools available to the model for this call
         * @param toolTokenCount Number of tokens used by tool definitions for this call
         * @param customToolCallCount Free-form custom tool calls in the request history before request shaping, counted apart from named function calls
         * @param unmatchedToolResultCount Tool results in the request history before request shaping that answer no earlier unanswered tool call
         * @param orphanToolCallCount Tool calls in the request history before request shaping that no later tool result answers
         * @param frontierSource How the prompt-cache frontier was determined for this call
         * @param cacheTtlSeconds Effective prompt-cache lifetime in seconds for this call
         * @param cachePolicyTtlSeconds Request-scoped prompt-cache policy lifetime in seconds; the shortest lifetime for mixed known policies. Does not establish or refresh cache state. Omitted when the policy is unknown.
         * @param cacheDetailsReported Whether the provider reported prompt-cache usage details for this call
         * @param numToolCalls Number of tool calls returned by the model
         * @param toolCounts Tool-call counts keyed by tool name
         * @param finishReason Finish reason reported by the model for this API call (e.g. "stop", "length", "tool_calls", "content_filter"). Normalized to OpenAI vocabulary; for Anthropic models a "refusal" stop reason maps to "content_filter".
         * @param contentFilterTriggered Whether the model response was blocked or truncated by content filtering (finish_reason === 'content_filter'). For Anthropic models this corresponds to a 'refusal' stop reason.
         * @param fusion Experimental HydraFusion attribution for this concrete model call's usage.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AssistantUsageEventData(
            AiCreditsStatus aiCreditsStatus,
            String model,
            ModelProviderRef provider,
            String modelDisplayName,
            UsageAccountingIdentity accounting,
            Long inputTokens,
            Long outputTokens,
            Long cacheReadTokens,
            Long cacheWriteTokens,
            OffsetDateTime cacheExpiresAt,
            Long reasoningTokens,
            Long thinkingDroppedBlocks,
            List<String> thinkingDroppedReasons,
            Double cost,
            Long duration,
            Double timeToFirstTokenMs,
            Double outputTtftMs,
            Double interTokenLatencyMs,
            String initiator,
            String interactionType,
            Boolean isByok,
            String byokKind,
            String modelProvider,
            String providerType,
            Boolean isAuto,
            Long maxPromptTokens,
            Long maxOutputTokens,
            Long acceptedPredictionTokens,
            Long rejectedPredictionTokens,
            AssistantUsageTransport transport,
            Long requestBodyBytes,
            Long requestBodyWireBytes,
            ModelCallRequestBodyEncoding requestBodyEncoding,
            ModelCallRequestBodyCompressionFallback requestBodyCompressionFallback,
            ModelCallRequestBodyEncoding requestBodyCompressionFallbackFrom,
            Long requestBodyCompressionRejectedStatus,
            ModelCallWebSocketFallbackReason websocketFallbackReason,
            Long websocketFallbackAfterMs,
            ModelCallWebSocketFallbackErrorKind websocketFallbackErrorKind,
            Long websocketFallbackStatusCode,
            Long websocketFallbackCloseCode,
            Boolean websocketFallbackReconnectAttempted,
            Boolean websocketFallbackStartedThisCall,
            String apiCallId,
            String providerCallId,
            String serviceRequestId,
            Boolean rte,
            AssistantUsageApiEndpoint apiEndpoint,
            String parentToolCallId,
            Map<String, AssistantUsageQuotaSnapshot> quotaSnapshots,
            AssistantUsageCopilotUsage copilotUsage,
            String reasoningEffort,
            ReasoningSummary reasoningSummary,
            Long availableToolCount,
            Long toolTokenCount,
            Long customToolCallCount,
            Long unmatchedToolResultCount,
            Long orphanToolCallCount,
            String frontierSource,
            Long cacheTtlSeconds,
            Long cachePolicyTtlSeconds,
            Boolean cacheDetailsReported,
            Long numToolCalls,
            Map<String, Long> toolCounts,
            String finishReason,
            Boolean contentFilterTriggered,
            FusionAttribution fusion
        ) {
            this(aiCreditsStatus, model, provider, modelDisplayName, accounting, inputTokens, outputTokens, cacheReadTokens, cacheWriteTokens, cacheExpiresAt, reasoningTokens, thinkingDroppedBlocks, thinkingDroppedReasons, cost, duration, timeToFirstTokenMs, outputTtftMs, interTokenLatencyMs, initiator, interactionType, isByok, byokKind, modelProvider, providerType, isAuto, maxPromptTokens, maxOutputTokens, acceptedPredictionTokens, rejectedPredictionTokens, transport, null, requestBodyBytes, requestBodyWireBytes, requestBodyEncoding, requestBodyCompressionFallback, requestBodyCompressionFallbackFrom, requestBodyCompressionRejectedStatus, websocketFallbackReason, websocketFallbackAfterMs, websocketFallbackErrorKind, websocketFallbackStatusCode, websocketFallbackCloseCode, websocketFallbackReconnectAttempted, websocketFallbackStartedThisCall, apiCallId, providerCallId, serviceRequestId, null, null, rte, apiEndpoint, parentToolCallId, quotaSnapshots, copilotUsage, reasoningEffort, reasoningSummary, availableToolCount, toolTokenCount, customToolCallCount, unmatchedToolResultCount, orphanToolCallCount, frontierSource, cacheTtlSeconds, cachePolicyTtlSeconds, null, null, cacheDetailsReported, numToolCalls, toolCounts, finishReason, contentFilterTriggered, fusion, null);
        }

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public AssistantUsageEventData {}

        /**
         * Returns a named builder.
         * @return a new builder
         * @since 2.0.0
         */
        public static Builder builder() { return new Builder(); }

        /**
         * Named construction for this record.
         * @since 2.0.0
         */
        public static final class Builder {
            private Builder() {}
            private AiCreditsStatus aiCreditsStatus;
            private String model;
            private ModelProviderRef provider;
            private String modelDisplayName;
            private UsageAccountingIdentity accounting;
            private Long inputTokens;
            private Long outputTokens;
            private Long cacheReadTokens;
            private Long cacheWriteTokens;
            private OffsetDateTime cacheExpiresAt;
            private Long reasoningTokens;
            private Long thinkingDroppedBlocks;
            private List<String> thinkingDroppedReasons;
            private Double cost;
            private Long duration;
            private Double timeToFirstTokenMs;
            private Double outputTtftMs;
            private Double interTokenLatencyMs;
            private String initiator;
            private String interactionType;
            private Boolean isByok;
            private String byokKind;
            private String modelProvider;
            private String providerType;
            private Boolean isAuto;
            private Long maxPromptTokens;
            private Long maxOutputTokens;
            private Long acceptedPredictionTokens;
            private Long rejectedPredictionTokens;
            private AssistantUsageTransport transport;
            private Boolean inferenceGateway;
            private Long requestBodyBytes;
            private Long requestBodyWireBytes;
            private ModelCallRequestBodyEncoding requestBodyEncoding;
            private ModelCallRequestBodyCompressionFallback requestBodyCompressionFallback;
            private ModelCallRequestBodyEncoding requestBodyCompressionFallbackFrom;
            private Long requestBodyCompressionRejectedStatus;
            private ModelCallWebSocketFallbackReason websocketFallbackReason;
            private Long websocketFallbackAfterMs;
            private ModelCallWebSocketFallbackErrorKind websocketFallbackErrorKind;
            private Long websocketFallbackStatusCode;
            private Long websocketFallbackCloseCode;
            private Boolean websocketFallbackReconnectAttempted;
            private Boolean websocketFallbackStartedThisCall;
            private String apiCallId;
            private String providerCallId;
            private String serviceRequestId;
            private String instructionUpdateDeliveryId;
            private String instructionUpdateLatestEventId;
            private Boolean rte;
            private AssistantUsageApiEndpoint apiEndpoint;
            private String parentToolCallId;
            private Map<String, AssistantUsageQuotaSnapshot> quotaSnapshots;
            private AssistantUsageCopilotUsage copilotUsage;
            private String reasoningEffort;
            private ReasoningSummary reasoningSummary;
            private Long availableToolCount;
            private Long toolTokenCount;
            private Long customToolCallCount;
            private Long unmatchedToolResultCount;
            private Long orphanToolCallCount;
            private String frontierSource;
            private Long cacheTtlSeconds;
            private Long cachePolicyTtlSeconds;
            private String promptCacheTtlReason;
            private String promptCacheCreditsBucket;
            private Boolean cacheDetailsReported;
            private Long numToolCalls;
            private Map<String, Long> toolCounts;
            private String finishReason;
            private Boolean contentFilterTriggered;
            private FusionAttribution fusion;
            private String responseModel;

            /**
             * Sets the {@code aiCreditsStatus} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder aiCreditsStatus(AiCreditsStatus value) {
                this.aiCreditsStatus = value;
                return this;
            }

            /**
             * Sets the {@code model} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder model(String value) {
                this.model = value;
                return this;
            }

            /**
             * Sets the {@code provider} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder provider(ModelProviderRef value) {
                this.provider = value;
                return this;
            }

            /**
             * Sets the {@code modelDisplayName} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder modelDisplayName(String value) {
                this.modelDisplayName = value;
                return this;
            }

            /**
             * Sets the {@code accounting} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder accounting(UsageAccountingIdentity value) {
                this.accounting = value;
                return this;
            }

            /**
             * Sets the {@code inputTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder inputTokens(Long value) {
                this.inputTokens = value;
                return this;
            }

            /**
             * Sets the {@code outputTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder outputTokens(Long value) {
                this.outputTokens = value;
                return this;
            }

            /**
             * Sets the {@code cacheReadTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder cacheReadTokens(Long value) {
                this.cacheReadTokens = value;
                return this;
            }

            /**
             * Sets the {@code cacheWriteTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder cacheWriteTokens(Long value) {
                this.cacheWriteTokens = value;
                return this;
            }

            /**
             * Sets the {@code cacheExpiresAt} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder cacheExpiresAt(OffsetDateTime value) {
                this.cacheExpiresAt = value;
                return this;
            }

            /**
             * Sets the {@code reasoningTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reasoningTokens(Long value) {
                this.reasoningTokens = value;
                return this;
            }

            /**
             * Sets the {@code thinkingDroppedBlocks} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder thinkingDroppedBlocks(Long value) {
                this.thinkingDroppedBlocks = value;
                return this;
            }

            /**
             * Sets the {@code thinkingDroppedReasons} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder thinkingDroppedReasons(List<String> value) {
                this.thinkingDroppedReasons = value;
                return this;
            }

            /**
             * Sets the {@code cost} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder cost(Double value) {
                this.cost = value;
                return this;
            }

            /**
             * Sets the {@code duration} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder duration(Long value) {
                this.duration = value;
                return this;
            }

            /**
             * Sets the {@code timeToFirstTokenMs} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder timeToFirstTokenMs(Double value) {
                this.timeToFirstTokenMs = value;
                return this;
            }

            /**
             * Sets the {@code outputTtftMs} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder outputTtftMs(Double value) {
                this.outputTtftMs = value;
                return this;
            }

            /**
             * Sets the {@code interTokenLatencyMs} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder interTokenLatencyMs(Double value) {
                this.interTokenLatencyMs = value;
                return this;
            }

            /**
             * Sets the {@code initiator} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder initiator(String value) {
                this.initiator = value;
                return this;
            }

            /**
             * Sets the {@code interactionType} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder interactionType(String value) {
                this.interactionType = value;
                return this;
            }

            /**
             * Sets the {@code isByok} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder isByok(Boolean value) {
                this.isByok = value;
                return this;
            }

            /**
             * Sets the {@code byokKind} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder byokKind(String value) {
                this.byokKind = value;
                return this;
            }

            /**
             * Sets the {@code modelProvider} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder modelProvider(String value) {
                this.modelProvider = value;
                return this;
            }

            /**
             * Sets the {@code providerType} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder providerType(String value) {
                this.providerType = value;
                return this;
            }

            /**
             * Sets the {@code isAuto} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder isAuto(Boolean value) {
                this.isAuto = value;
                return this;
            }

            /**
             * Sets the {@code maxPromptTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder maxPromptTokens(Long value) {
                this.maxPromptTokens = value;
                return this;
            }

            /**
             * Sets the {@code maxOutputTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder maxOutputTokens(Long value) {
                this.maxOutputTokens = value;
                return this;
            }

            /**
             * Sets the {@code acceptedPredictionTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder acceptedPredictionTokens(Long value) {
                this.acceptedPredictionTokens = value;
                return this;
            }

            /**
             * Sets the {@code rejectedPredictionTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder rejectedPredictionTokens(Long value) {
                this.rejectedPredictionTokens = value;
                return this;
            }

            /**
             * Sets the {@code transport} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder transport(AssistantUsageTransport value) {
                this.transport = value;
                return this;
            }

            /**
             * Sets the {@code inferenceGateway} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder inferenceGateway(Boolean value) {
                this.inferenceGateway = value;
                return this;
            }

            /**
             * Sets the {@code requestBodyBytes} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestBodyBytes(Long value) {
                this.requestBodyBytes = value;
                return this;
            }

            /**
             * Sets the {@code requestBodyWireBytes} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestBodyWireBytes(Long value) {
                this.requestBodyWireBytes = value;
                return this;
            }

            /**
             * Sets the {@code requestBodyEncoding} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestBodyEncoding(ModelCallRequestBodyEncoding value) {
                this.requestBodyEncoding = value;
                return this;
            }

            /**
             * Sets the {@code requestBodyCompressionFallback} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestBodyCompressionFallback(ModelCallRequestBodyCompressionFallback value) {
                this.requestBodyCompressionFallback = value;
                return this;
            }

            /**
             * Sets the {@code requestBodyCompressionFallbackFrom} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestBodyCompressionFallbackFrom(ModelCallRequestBodyEncoding value) {
                this.requestBodyCompressionFallbackFrom = value;
                return this;
            }

            /**
             * Sets the {@code requestBodyCompressionRejectedStatus} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestBodyCompressionRejectedStatus(Long value) {
                this.requestBodyCompressionRejectedStatus = value;
                return this;
            }

            /**
             * Sets the {@code websocketFallbackReason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder websocketFallbackReason(ModelCallWebSocketFallbackReason value) {
                this.websocketFallbackReason = value;
                return this;
            }

            /**
             * Sets the {@code websocketFallbackAfterMs} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder websocketFallbackAfterMs(Long value) {
                this.websocketFallbackAfterMs = value;
                return this;
            }

            /**
             * Sets the {@code websocketFallbackErrorKind} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder websocketFallbackErrorKind(ModelCallWebSocketFallbackErrorKind value) {
                this.websocketFallbackErrorKind = value;
                return this;
            }

            /**
             * Sets the {@code websocketFallbackStatusCode} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder websocketFallbackStatusCode(Long value) {
                this.websocketFallbackStatusCode = value;
                return this;
            }

            /**
             * Sets the {@code websocketFallbackCloseCode} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder websocketFallbackCloseCode(Long value) {
                this.websocketFallbackCloseCode = value;
                return this;
            }

            /**
             * Sets the {@code websocketFallbackReconnectAttempted} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder websocketFallbackReconnectAttempted(Boolean value) {
                this.websocketFallbackReconnectAttempted = value;
                return this;
            }

            /**
             * Sets the {@code websocketFallbackStartedThisCall} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder websocketFallbackStartedThisCall(Boolean value) {
                this.websocketFallbackStartedThisCall = value;
                return this;
            }

            /**
             * Sets the {@code apiCallId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder apiCallId(String value) {
                this.apiCallId = value;
                return this;
            }

            /**
             * Sets the {@code providerCallId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder providerCallId(String value) {
                this.providerCallId = value;
                return this;
            }

            /**
             * Sets the {@code serviceRequestId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder serviceRequestId(String value) {
                this.serviceRequestId = value;
                return this;
            }

            /**
             * Sets the {@code instructionUpdateDeliveryId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder instructionUpdateDeliveryId(String value) {
                this.instructionUpdateDeliveryId = value;
                return this;
            }

            /**
             * Sets the {@code instructionUpdateLatestEventId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder instructionUpdateLatestEventId(String value) {
                this.instructionUpdateLatestEventId = value;
                return this;
            }

            /**
             * Sets the {@code rte} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder rte(Boolean value) {
                this.rte = value;
                return this;
            }

            /**
             * Sets the {@code apiEndpoint} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder apiEndpoint(AssistantUsageApiEndpoint value) {
                this.apiEndpoint = value;
                return this;
            }

            /**
             * Sets the {@code parentToolCallId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder parentToolCallId(String value) {
                this.parentToolCallId = value;
                return this;
            }

            /**
             * Sets the {@code quotaSnapshots} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder quotaSnapshots(Map<String, AssistantUsageQuotaSnapshot> value) {
                this.quotaSnapshots = value;
                return this;
            }

            /**
             * Sets the {@code copilotUsage} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder copilotUsage(AssistantUsageCopilotUsage value) {
                this.copilotUsage = value;
                return this;
            }

            /**
             * Sets the {@code reasoningEffort} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reasoningEffort(String value) {
                this.reasoningEffort = value;
                return this;
            }

            /**
             * Sets the {@code reasoningSummary} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reasoningSummary(ReasoningSummary value) {
                this.reasoningSummary = value;
                return this;
            }

            /**
             * Sets the {@code availableToolCount} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder availableToolCount(Long value) {
                this.availableToolCount = value;
                return this;
            }

            /**
             * Sets the {@code toolTokenCount} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolTokenCount(Long value) {
                this.toolTokenCount = value;
                return this;
            }

            /**
             * Sets the {@code customToolCallCount} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder customToolCallCount(Long value) {
                this.customToolCallCount = value;
                return this;
            }

            /**
             * Sets the {@code unmatchedToolResultCount} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder unmatchedToolResultCount(Long value) {
                this.unmatchedToolResultCount = value;
                return this;
            }

            /**
             * Sets the {@code orphanToolCallCount} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder orphanToolCallCount(Long value) {
                this.orphanToolCallCount = value;
                return this;
            }

            /**
             * Sets the {@code frontierSource} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder frontierSource(String value) {
                this.frontierSource = value;
                return this;
            }

            /**
             * Sets the {@code cacheTtlSeconds} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder cacheTtlSeconds(Long value) {
                this.cacheTtlSeconds = value;
                return this;
            }

            /**
             * Sets the {@code cachePolicyTtlSeconds} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder cachePolicyTtlSeconds(Long value) {
                this.cachePolicyTtlSeconds = value;
                return this;
            }

            /**
             * Sets the {@code promptCacheTtlReason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder promptCacheTtlReason(String value) {
                this.promptCacheTtlReason = value;
                return this;
            }

            /**
             * Sets the {@code promptCacheCreditsBucket} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder promptCacheCreditsBucket(String value) {
                this.promptCacheCreditsBucket = value;
                return this;
            }

            /**
             * Sets the {@code cacheDetailsReported} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder cacheDetailsReported(Boolean value) {
                this.cacheDetailsReported = value;
                return this;
            }

            /**
             * Sets the {@code numToolCalls} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder numToolCalls(Long value) {
                this.numToolCalls = value;
                return this;
            }

            /**
             * Sets the {@code toolCounts} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolCounts(Map<String, Long> value) {
                this.toolCounts = value;
                return this;
            }

            /**
             * Sets the {@code finishReason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder finishReason(String value) {
                this.finishReason = value;
                return this;
            }

            /**
             * Sets the {@code contentFilterTriggered} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder contentFilterTriggered(Boolean value) {
                this.contentFilterTriggered = value;
                return this;
            }

            /**
             * Sets the {@code fusion} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder fusion(FusionAttribution value) {
                this.fusion = value;
                return this;
            }

            /**
             * Sets the {@code responseModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder responseModel(String value) {
                this.responseModel = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public AssistantUsageEventData build() {
                if (model == null) throw new IllegalStateException("Missing required property: model");
                return new AssistantUsageEventData(aiCreditsStatus, model, provider, modelDisplayName, accounting, inputTokens, outputTokens, cacheReadTokens, cacheWriteTokens, cacheExpiresAt, reasoningTokens, thinkingDroppedBlocks, thinkingDroppedReasons, cost, duration, timeToFirstTokenMs, outputTtftMs, interTokenLatencyMs, initiator, interactionType, isByok, byokKind, modelProvider, providerType, isAuto, maxPromptTokens, maxOutputTokens, acceptedPredictionTokens, rejectedPredictionTokens, transport, inferenceGateway, requestBodyBytes, requestBodyWireBytes, requestBodyEncoding, requestBodyCompressionFallback, requestBodyCompressionFallbackFrom, requestBodyCompressionRejectedStatus, websocketFallbackReason, websocketFallbackAfterMs, websocketFallbackErrorKind, websocketFallbackStatusCode, websocketFallbackCloseCode, websocketFallbackReconnectAttempted, websocketFallbackStartedThisCall, apiCallId, providerCallId, serviceRequestId, instructionUpdateDeliveryId, instructionUpdateLatestEventId, rte, apiEndpoint, parentToolCallId, quotaSnapshots, copilotUsage, reasoningEffort, reasoningSummary, availableToolCount, toolTokenCount, customToolCallCount, unmatchedToolResultCount, orphanToolCallCount, frontierSource, cacheTtlSeconds, cachePolicyTtlSeconds, promptCacheTtlReason, promptCacheCreditsBucket, cacheDetailsReported, numToolCalls, toolCounts, finishReason, contentFilterTriggered, fusion, responseModel);
            }
        }
    }
}
