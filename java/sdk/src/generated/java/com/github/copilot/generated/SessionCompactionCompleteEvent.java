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
        /** Reasoning effort level the compaction summarizer request used, if applicable (e.g. "none", "low", "medium", "high", "xhigh", "max") */
        @JsonProperty("reasoningEffort") String reasoningEffort,
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
        /** Client-generated request ID (X-Request-Id header) the runtime sent with the latest compaction LLM request. Present even when the request failed before any response arrived. */
        @JsonProperty("clientRequestId") String clientRequestId,
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
        @JsonProperty("trigger") CompactionTrigger trigger,
        /** How the compaction ran relative to the conversation: "background" (alongside the turn), "turn_blocking" (the turn waited for it), "pre_prompt" (a non-interactive run waited before recording its prompt), or "direct" (an explicit compaction request). Open set; readers must tolerate other values. */
        @JsonProperty("execution") String execution,
        /** How far the summarizer model calls had progressed when the outcome was decided, so a stalled summarizer can be told from a slow one */
        @JsonProperty("summarizerProgress") CompactionCompleteSummarizerProgress summarizerProgress,
        /** How long the compaction attempt took end to end, how long it blocked the user, and the configuration it ran under */
        @JsonProperty("attemptMetrics") CompactionCompleteAttemptMetrics attemptMetrics,
        /** Evidence about the summarizer's latest model call for a failed compaction, so a failure its reason alone does not explain can be diagnosed */
        @JsonProperty("failureDiagnostics") CompactionCompleteFailureDiagnostics failureDiagnostics
    ) {

        /**
         * Retains the constructor published in Java SDK 1.0.28.
         * @deprecated Use named construction via {@link #builder()}.
         * @param success Whether compaction completed successfully
         * @param outcome Typed terminal outcome for this observed compaction completion. Unknown future values are preserved.
         * @param outcomeReason Bounded reason for an expected non-application outcome such as cancellation or no applicable history. Unknown future values are preserved.
         * @param failureReason Bounded reason for a typed failed outcome. Unknown future values are preserved.
         * @param compactionAttemptId Identifier shared with the matching compaction start event
         * @param model Model identifier used for compaction when provider usage does not already supply it
         * @param initialModel Model identifier at admission when routing later changed to the reported compaction model
         * @param error Error message if compaction failed
         * @param preCompactionTokens Total tokens in conversation before compaction
         * @param postCompactionTokens Total tokens in conversation after compaction
         * @param preCompactionMessagesLength Number of messages before compaction
         * @param messagesRemoved Number of messages removed during compaction
         * @param tokensRemoved Number of tokens removed during compaction
         * @param tokenAccountingBasis Open accounting-basis identifier for trustworthy paired pre/post token measurements. The current runtime emits local_full_context_estimate.
         * @param customInstructions User-supplied focus instructions provided to a manual `/compact` invocation. Omitted for automatic compaction and for manual compaction with no focus text.
         * @param summaryContent LLM-generated summary of the compacted conversation history
         * @param responsesReasoning Reasoning baseline on the replacement summary, preserved when replay skips the compacted history
         * @param activeWorkflowSummary Authoritative active-workflow reminder appended to the compacted context
         * @param behaviorModelId Canonical model identifier used for model-specific behavior when replaying compaction
         * @param checkpointNumber Checkpoint snapshot number created for recovery
         * @param checkpointPath File path where the checkpoint was stored
         * @param compactionTokensUsed Token usage breakdown for the compaction LLM call (aligned with assistant.usage format)
         * @param requestId GitHub request tracing ID (x-github-request-id header) for the compaction LLM call
         * @param serviceRequestId Copilot service request ID (x-copilot-service-request-id header) for the compaction LLM call
         * @param systemTokens Token count from system message(s) after compaction
         * @param conversationTokens Token count from non-system messages (user, assistant, tool) after compaction
         * @param toolDefinitionsTokens Token count from tool definitions after compaction
         * @param statusCode For failed compaction only: the HTTP status code of the compaction LLM call failure, when it carried one. Absent for successful compaction and for failures without an HTTP status (e.g. an empty model response or a transport error).
         * @param tokenLimit Model context window token limit the compaction was targeting, when known
         * @param targetPromptTokenLimit Destination prompt-token budget when it differs from the effective compaction token limit
         * @param switchTargetModel Destination model for a model-switch compaction when it differs from the compacting model; for a HydraFusion turn, the routed root model
         * @param trigger What initiated this compaction, when known
         * @param summarizerProgress How far the summarizer model calls had progressed when the outcome was decided, so a stalled summarizer can be told from a slow one
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionCompactionCompleteEventData(
            Boolean success,
            String outcome,
            String outcomeReason,
            String failureReason,
            String compactionAttemptId,
            String model,
            String initialModel,
            String error,
            Long preCompactionTokens,
            Long postCompactionTokens,
            Long preCompactionMessagesLength,
            Long messagesRemoved,
            Long tokensRemoved,
            String tokenAccountingBasis,
            String customInstructions,
            String summaryContent,
            ResponsesReasoning responsesReasoning,
            String activeWorkflowSummary,
            String behaviorModelId,
            Long checkpointNumber,
            String checkpointPath,
            CompactionCompleteCompactionTokensUsed compactionTokensUsed,
            String requestId,
            String serviceRequestId,
            Long systemTokens,
            Long conversationTokens,
            Long toolDefinitionsTokens,
            Long statusCode,
            Long tokenLimit,
            Long targetPromptTokenLimit,
            String switchTargetModel,
            CompactionTrigger trigger,
            CompactionCompleteSummarizerProgress summarizerProgress
        ) {
            this(success, outcome, outcomeReason, failureReason, compactionAttemptId, model, initialModel, null, error, preCompactionTokens, postCompactionTokens, preCompactionMessagesLength, messagesRemoved, tokensRemoved, tokenAccountingBasis, customInstructions, summaryContent, responsesReasoning, activeWorkflowSummary, behaviorModelId, checkpointNumber, checkpointPath, compactionTokensUsed, requestId, serviceRequestId, null, systemTokens, conversationTokens, toolDefinitionsTokens, statusCode, tokenLimit, targetPromptTokenLimit, switchTargetModel, trigger, null, summarizerProgress, null, null);
        }

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionCompactionCompleteEventData {}

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
            private Boolean success;
            private String outcome;
            private String outcomeReason;
            private String failureReason;
            private String compactionAttemptId;
            private String model;
            private String initialModel;
            private String reasoningEffort;
            private String error;
            private Long preCompactionTokens;
            private Long postCompactionTokens;
            private Long preCompactionMessagesLength;
            private Long messagesRemoved;
            private Long tokensRemoved;
            private String tokenAccountingBasis;
            private String customInstructions;
            private String summaryContent;
            private ResponsesReasoning responsesReasoning;
            private String activeWorkflowSummary;
            private String behaviorModelId;
            private Long checkpointNumber;
            private String checkpointPath;
            private CompactionCompleteCompactionTokensUsed compactionTokensUsed;
            private String requestId;
            private String serviceRequestId;
            private String clientRequestId;
            private Long systemTokens;
            private Long conversationTokens;
            private Long toolDefinitionsTokens;
            private Long statusCode;
            private Long tokenLimit;
            private Long targetPromptTokenLimit;
            private String switchTargetModel;
            private CompactionTrigger trigger;
            private String execution;
            private CompactionCompleteSummarizerProgress summarizerProgress;
            private CompactionCompleteAttemptMetrics attemptMetrics;
            private CompactionCompleteFailureDiagnostics failureDiagnostics;

            /**
             * Sets the {@code success} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder success(Boolean value) {
                this.success = value;
                return this;
            }

            /**
             * Sets the {@code outcome} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder outcome(String value) {
                this.outcome = value;
                return this;
            }

            /**
             * Sets the {@code outcomeReason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder outcomeReason(String value) {
                this.outcomeReason = value;
                return this;
            }

            /**
             * Sets the {@code failureReason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder failureReason(String value) {
                this.failureReason = value;
                return this;
            }

            /**
             * Sets the {@code compactionAttemptId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder compactionAttemptId(String value) {
                this.compactionAttemptId = value;
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
             * Sets the {@code initialModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder initialModel(String value) {
                this.initialModel = value;
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
             * Sets the {@code error} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder error(String value) {
                this.error = value;
                return this;
            }

            /**
             * Sets the {@code preCompactionTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder preCompactionTokens(Long value) {
                this.preCompactionTokens = value;
                return this;
            }

            /**
             * Sets the {@code postCompactionTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder postCompactionTokens(Long value) {
                this.postCompactionTokens = value;
                return this;
            }

            /**
             * Sets the {@code preCompactionMessagesLength} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder preCompactionMessagesLength(Long value) {
                this.preCompactionMessagesLength = value;
                return this;
            }

            /**
             * Sets the {@code messagesRemoved} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder messagesRemoved(Long value) {
                this.messagesRemoved = value;
                return this;
            }

            /**
             * Sets the {@code tokensRemoved} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder tokensRemoved(Long value) {
                this.tokensRemoved = value;
                return this;
            }

            /**
             * Sets the {@code tokenAccountingBasis} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder tokenAccountingBasis(String value) {
                this.tokenAccountingBasis = value;
                return this;
            }

            /**
             * Sets the {@code customInstructions} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder customInstructions(String value) {
                this.customInstructions = value;
                return this;
            }

            /**
             * Sets the {@code summaryContent} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder summaryContent(String value) {
                this.summaryContent = value;
                return this;
            }

            /**
             * Sets the {@code responsesReasoning} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder responsesReasoning(ResponsesReasoning value) {
                this.responsesReasoning = value;
                return this;
            }

            /**
             * Sets the {@code activeWorkflowSummary} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder activeWorkflowSummary(String value) {
                this.activeWorkflowSummary = value;
                return this;
            }

            /**
             * Sets the {@code behaviorModelId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder behaviorModelId(String value) {
                this.behaviorModelId = value;
                return this;
            }

            /**
             * Sets the {@code checkpointNumber} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder checkpointNumber(Long value) {
                this.checkpointNumber = value;
                return this;
            }

            /**
             * Sets the {@code checkpointPath} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder checkpointPath(String value) {
                this.checkpointPath = value;
                return this;
            }

            /**
             * Sets the {@code compactionTokensUsed} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder compactionTokensUsed(CompactionCompleteCompactionTokensUsed value) {
                this.compactionTokensUsed = value;
                return this;
            }

            /**
             * Sets the {@code requestId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestId(String value) {
                this.requestId = value;
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
             * Sets the {@code clientRequestId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder clientRequestId(String value) {
                this.clientRequestId = value;
                return this;
            }

            /**
             * Sets the {@code systemTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder systemTokens(Long value) {
                this.systemTokens = value;
                return this;
            }

            /**
             * Sets the {@code conversationTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder conversationTokens(Long value) {
                this.conversationTokens = value;
                return this;
            }

            /**
             * Sets the {@code toolDefinitionsTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolDefinitionsTokens(Long value) {
                this.toolDefinitionsTokens = value;
                return this;
            }

            /**
             * Sets the {@code statusCode} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder statusCode(Long value) {
                this.statusCode = value;
                return this;
            }

            /**
             * Sets the {@code tokenLimit} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder tokenLimit(Long value) {
                this.tokenLimit = value;
                return this;
            }

            /**
             * Sets the {@code targetPromptTokenLimit} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder targetPromptTokenLimit(Long value) {
                this.targetPromptTokenLimit = value;
                return this;
            }

            /**
             * Sets the {@code switchTargetModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder switchTargetModel(String value) {
                this.switchTargetModel = value;
                return this;
            }

            /**
             * Sets the {@code trigger} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder trigger(CompactionTrigger value) {
                this.trigger = value;
                return this;
            }

            /**
             * Sets the {@code execution} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder execution(String value) {
                this.execution = value;
                return this;
            }

            /**
             * Sets the {@code summarizerProgress} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder summarizerProgress(CompactionCompleteSummarizerProgress value) {
                this.summarizerProgress = value;
                return this;
            }

            /**
             * Sets the {@code attemptMetrics} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder attemptMetrics(CompactionCompleteAttemptMetrics value) {
                this.attemptMetrics = value;
                return this;
            }

            /**
             * Sets the {@code failureDiagnostics} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder failureDiagnostics(CompactionCompleteFailureDiagnostics value) {
                this.failureDiagnostics = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionCompactionCompleteEventData build() {
                if (success == null) throw new IllegalStateException("Missing required property: success");
                return new SessionCompactionCompleteEventData(success, outcome, outcomeReason, failureReason, compactionAttemptId, model, initialModel, reasoningEffort, error, preCompactionTokens, postCompactionTokens, preCompactionMessagesLength, messagesRemoved, tokensRemoved, tokenAccountingBasis, customInstructions, summaryContent, responsesReasoning, activeWorkflowSummary, behaviorModelId, checkpointNumber, checkpointPath, compactionTokensUsed, requestId, serviceRequestId, clientRequestId, systemTokens, conversationTokens, toolDefinitionsTokens, statusCode, tokenLimit, targetPromptTokenLimit, switchTargetModel, trigger, execution, summarizerProgress, attemptMetrics, failureDiagnostics);
            }
        }
    }
}
