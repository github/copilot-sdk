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
 * How far the compaction's summarizer model calls had progressed when the compaction's outcome was decided. Stream statistics describe the latest model request; durationMs and attemptCount cover every request.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CompactionCompleteSummarizerProgress(
    /** Time from the start of the compaction to the decision of its outcome, in milliseconds */
    @JsonProperty("durationMs") Long durationMs,
    /** Number of summarizer model requests dispatched, including retries */
    @JsonProperty("attemptCount") Long attemptCount,
    /** Time since the latest request was dispatched, whether or not it had finished, in milliseconds. Absent when no request was dispatched. */
    @JsonProperty("attemptAgeMs") Long attemptAgeMs,
    /** Whether the latest request had already returned a response or an error. False when it was still in flight, such as when the compaction deadline cut it off; absent when no request was dispatched. */
    @JsonProperty("attemptSettled") Boolean attemptSettled,
    /** Time from dispatching the latest request to its first token-bearing stream event, in milliseconds. Absent when no stream event arrived. */
    @JsonProperty("timeToFirstTokenMs") Long timeToFirstTokenMs,
    /** Time from dispatching the latest request to its first observable text, reasoning, or tool-call output, in milliseconds. Absent when no output arrived. */
    @JsonProperty("timeToFirstOutputMs") Long timeToFirstOutputMs,
    /** Time from dispatching the latest request to its first reply text or tool-call output, in milliseconds. Reasoning does not count. Absent when no such output arrived. */
    @JsonProperty("timeToFirstAnswerOutputMs") Long timeToFirstAnswerOutputMs,
    /** Token-bearing stream events the latest request received, keepalive deltas included */
    @JsonProperty("streamEventCount") Long streamEventCount,
    /** Stream events of the latest request that carried observable output */
    @JsonProperty("outputEventCount") Long outputEventCount,
    /** Longest gap between two consecutive stream events of the latest request, in milliseconds. Excludes the still-open gap reported as idleMs; absent with fewer than two events. */
    @JsonProperty("maxStreamEventGapMs") Long maxStreamEventGapMs,
    /** Longest gap between two consecutive outputs of the latest request up to its first reply text or tool-call output, including the gap that ends there, in milliseconds. Excludes the still-open gap reported as sinceLastOutputMs; absent without such a gap. */
    @JsonProperty("maxOutputGapBeforeAnswerMs") Long maxOutputGapBeforeAnswerMs,
    /** Longest gap between two consecutive outputs of the latest request after its first reply text or tool-call output, in milliseconds. Excludes the still-open gap reported as sinceLastOutputMs; absent without such a gap. */
    @JsonProperty("maxOutputGapAfterAnswerMs") Long maxOutputGapAfterAnswerMs,
    /** Time since the latest request's last stream event, or since its dispatch if none arrived, in milliseconds. Absent when no request was dispatched. */
    @JsonProperty("idleMs") Long idleMs,
    /** Time since the latest request's last observable output, in milliseconds. Unlike idleMs, keepalive deltas do not reset it. Absent when no output arrived. */
    @JsonProperty("sinceLastOutputMs") Long sinceLastOutputMs,
    /** Characters of reply text the latest request streamed, counted as received, so it measures output even when the request was cut off before reporting usage. Tool-call arguments and reasoning are not counted. Absent when no request was dispatched. */
    @JsonProperty("answerChars") Long answerChars,
    /** Characters of reasoning text, summarized or raw, the latest request streamed, counted as received. Encrypted reasoning is not counted. Absent when no request was dispatched. */
    @JsonProperty("reasoningChars") Long reasoningChars,
    /** Sampled estimate of the input tokens the latest request sent, taken at dispatch, so it is known even when the request was cut off before reporting usage. Absent when no request was dispatched. */
    @JsonProperty("inputTokensEstimate") Long inputTokensEstimate,
    /** Whether historical attachments and reasoning were dropped from the compaction's input because the session's previous compaction ran out of time. */
    @JsonProperty("inputShrunk") Boolean inputShrunk
) {
}
