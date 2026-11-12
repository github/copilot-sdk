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
 * End-to-end timing of a compaction attempt and the configuration it ran under, measured from the start of the compaction to its terminal event.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CompactionCompleteAttemptMetrics(
    /** Time from the start of the compaction to this event, in milliseconds, whatever the outcome */
    @JsonProperty("durationMs") Long durationMs,
    /** Time a turn, or a prompt-mode run's prompt, spent blocked waiting for this compaction, in milliseconds. Absent for compactions that block nothing. */
    @JsonProperty("blockingWaitMs") Long blockingWaitMs,
    /** Time from the decision of the compaction's outcome to this event, in milliseconds. Absent when no outcome was decided. */
    @JsonProperty("applyDelayMs") Long applyDelayMs,
    /** Estimated time the machine spent suspended or asleep during the compaction, in milliseconds */
    @JsonProperty("suspendedMs") Long suspendedMs,
    /** Reasoning effort the summarizer requests were sent with */
    @JsonProperty("summarizerReasoningEffort") String summarizerReasoningEffort,
    /** Reasoning effort the summarizer would have used before any compaction-specific reduction */
    @JsonProperty("requestedReasoningEffort") String requestedReasoningEffort,
    /** Deadline policy bounding the summarizer, such as "fixed" or "progress_aware" */
    @JsonProperty("deadlinePolicy") String deadlinePolicy,
    /** The deadline limit that ended the compaction, in milliseconds. Absent unless a deadline fired. */
    @JsonProperty("deadlineLimitMs") Long deadlineLimitMs,
    /** How long after its limit the deadline was observed to fire, in milliseconds. Absent unless a deadline fired. */
    @JsonProperty("deadlineOvershootMs") Long deadlineOvershootMs,
    /** Context utilization threshold, as a fraction of the token limit, whose crossing started this compaction. Absent for compactions not started by a threshold. */
    @JsonProperty("triggerThreshold") Double triggerThreshold,
    /** Output token limit of the latest summarizer request. Absent when no request was sent. */
    @JsonProperty("maxOutputTokens") Double maxOutputTokens,
    /** Summarizer requests that failed with a provider or transport error. Absent when none failed. */
    @JsonProperty("failedRequestCount") Long failedRequestCount,
    /** Sum of the sampled input token estimates of the summarizer requests that failed, whose usage the provider never reported. Absent when none failed. */
    @JsonProperty("failedRequestInputTokensEstimate") Long failedRequestInputTokensEstimate,
    /** Summarizer requests cancelled while in flight, one per model call cancellation reported for this compaction. Not counted in failedRequestCount. Absent when none was cancelled. */
    @JsonProperty("cancelledRequestCount") Long cancelledRequestCount,
    /** Total time from a failed summarizer request settling to the next one starting, which includes the retry backoff, in milliseconds. Absent when no request was retried. */
    @JsonProperty("retryWaitMs") Long retryWaitMs,
    /** Retry-After of the latest failed summarizer request, in milliseconds. Absent when that request sent none. */
    @JsonProperty("lastRetryAfterMs") Long lastRetryAfterMs,
    /** Historical attachments removed from the summarizer input when it was shrunk. Absent unless the input was shrunk. */
    @JsonProperty("shrinkRemovedAttachments") Long shrinkRemovedAttachments,
    /** Historical reasoning items stripped from the summarizer input when it was shrunk. Absent unless the input was shrunk. */
    @JsonProperty("shrinkStrippedReasoning") Long shrinkStrippedReasoning,
    /** Step whose failure ended the compaction before or after its summarizer request: "session_read", "auth_hydration", "model_resolution", "context_breakdown", "token_limit", "start_publication", "pre_compact_hook", "output_hydration", "auth_missing", "agent_context_missing", "summarizer_setup", "summary_parse", "plan_read", "history_write" or "background_apply". Absent when the summarizer request itself failed, or nothing failed. */
    @JsonProperty("failureOperation") String failureOperation
) {
}
