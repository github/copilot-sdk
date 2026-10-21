/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "session.compaction_complete". Conversation compaction results including success status, metrics, and optional error details
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionCompactionCompleteEvent extends SessionEvent {

    @Override
    public String getType() { return "session.compaction_complete"; }

    @JsonProperty("data")
    private SessionCompactionCompleteEventData data;

    public SessionCompactionCompleteEventData getData() { return data; }
    public void setData(SessionCompactionCompleteEventData data) { this.data = data; }

    /** Data payload for {@link SessionCompactionCompleteEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionCompactionCompleteEventData(
        /** Whether compaction completed successfully */
        @JsonProperty("success") Boolean success,
        /** Typed terminal outcome for this observed compaction completion. Unknown future values are preserved. */
        @JsonProperty("outcome") String outcome,
        /** Bounded reason for an expected non-application outcome such as cancellation or no applicable history. Unknown future values are preserved. */
        @JsonProperty("outcomeReason") String outcomeReason,
        /** Bounded reason for a typed failed outcome. Unknown future values are preserved. */
        @JsonProperty("failureReason") String failureReason,
        /** Identifier shared with the matching compaction start event */
        @JsonProperty("compactionAttemptId") String compactionAttemptId,
        /** Model identifier used for compaction when provider usage does not already supply it */
        @JsonProperty("model") String model,
        /** Model identifier at admission when routing later changed to the reported compaction model */
        @JsonProperty("initialModel") String initialModel,
        /** Error message if compaction failed */
        @JsonProperty("error") String error,
        /** Total tokens in conversation before compaction */
        @JsonProperty("preCompactionTokens") Long preCompactionTokens,
        /** Total tokens in conversation after compaction */
        @JsonProperty("postCompactionTokens") Long postCompactionTokens,
        /** Number of messages before compaction */
        @JsonProperty("preCompactionMessagesLength") Long preCompactionMessagesLength,
        /** Number of messages removed during compaction */
        @JsonProperty("messagesRemoved") Long messagesRemoved,
        /** Number of tokens removed during compaction */
        @JsonProperty("tokensRemoved") Long tokensRemoved,
        /** Open accounting-basis identifier for trustworthy paired pre/post token measurements. The current runtime emits local_full_context_estimate. */
        @JsonProperty("tokenAccountingBasis") String tokenAccountingBasis,
        /** User-supplied focus instructions provided to a manual `/compact` invocation. Omitted for automatic compaction and for manual compaction with no focus text. */
        @JsonProperty("customInstructions") String customInstructions,
        /** LLM-generated summary of the compacted conversation history */
        @JsonProperty("summaryContent") String summaryContent,
        /** Reasoning baseline on the replacement summary, preserved when replay skips the compacted history */
        @JsonProperty("responsesReasoning") ResponsesReasoning responsesReasoning,
        /** Authoritative active-workflow reminder appended to the compacted context */
        @JsonProperty("activeWorkflowSummary") String activeWorkflowSummary,
        /** Canonical model identifier used for model-specific behavior when replaying compaction */
        @JsonProperty("behaviorModelId") String behaviorModelId,
        /** Checkpoint snapshot number created for recovery */
        @JsonProperty("checkpointNumber") Long checkpointNumber,
        /** File path where the checkpoint was stored */
        @JsonProperty("checkpointPath") String checkpointPath,
        /** Token usage breakdown for the compaction LLM call (aligned with assistant.usage format) */
        @JsonProperty("compactionTokensUsed") CompactionCompleteCompactionTokensUsed compactionTokensUsed,
        /** GitHub request tracing ID (x-github-request-id header) for the compaction LLM call */
        @JsonProperty("requestId") String requestId,
        /** Copilot service request ID (x-copilot-service-request-id header) for the compaction LLM call */
        @JsonProperty("serviceRequestId") String serviceRequestId,
        /** Token count from system message(s) after compaction */
        @JsonProperty("systemTokens") Long systemTokens,
        /** Token count from non-system messages (user, assistant, tool) after compaction */
        @JsonProperty("conversationTokens") Long conversationTokens,
        /** Token count from tool definitions after compaction */
        @JsonProperty("toolDefinitionsTokens") Long toolDefinitionsTokens,
        /** For failed compaction only: the HTTP status code of the compaction LLM call failure, when it carried one. Absent for successful compaction and for failures without an HTTP status (e.g. an empty model response or a transport error). */
        @JsonProperty("statusCode") Long statusCode,
        /** Model context window token limit the compaction was targeting, when known */
        @JsonProperty("tokenLimit") Long tokenLimit,
        /** Destination prompt-token budget when it differs from the effective compaction token limit */
        @JsonProperty("targetPromptTokenLimit") Long targetPromptTokenLimit,
        /** Destination model for a model-switch compaction when it differs from the compacting model; for a HydraFusion turn, the routed root model */
        @JsonProperty("switchTargetModel") String switchTargetModel,
        /** What initiated this compaction, when known */
        @JsonProperty("trigger") CompactionTrigger trigger
    ) {
    }
}
