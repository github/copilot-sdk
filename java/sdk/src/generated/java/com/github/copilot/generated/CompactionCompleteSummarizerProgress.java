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

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CompactionCompleteSummarizerProgress {}

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
        private Long durationMs;
        private Long attemptCount;
        private Long attemptAgeMs;
        private Boolean attemptSettled;
        private Long timeToFirstTokenMs;
        private Long timeToFirstOutputMs;
        private Long timeToFirstAnswerOutputMs;
        private Long streamEventCount;
        private Long outputEventCount;
        private Long maxStreamEventGapMs;
        private Long maxOutputGapBeforeAnswerMs;
        private Long maxOutputGapAfterAnswerMs;
        private Long idleMs;
        private Long sinceLastOutputMs;
        private Long answerChars;
        private Long reasoningChars;
        private Long inputTokensEstimate;
        private Boolean inputShrunk;

        /**
         * Sets the {@code durationMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder durationMs(Long value) {
            this.durationMs = value;
            return this;
        }

        /**
         * Sets the {@code attemptCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder attemptCount(Long value) {
            this.attemptCount = value;
            return this;
        }

        /**
         * Sets the {@code attemptAgeMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder attemptAgeMs(Long value) {
            this.attemptAgeMs = value;
            return this;
        }

        /**
         * Sets the {@code attemptSettled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder attemptSettled(Boolean value) {
            this.attemptSettled = value;
            return this;
        }

        /**
         * Sets the {@code timeToFirstTokenMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder timeToFirstTokenMs(Long value) {
            this.timeToFirstTokenMs = value;
            return this;
        }

        /**
         * Sets the {@code timeToFirstOutputMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder timeToFirstOutputMs(Long value) {
            this.timeToFirstOutputMs = value;
            return this;
        }

        /**
         * Sets the {@code timeToFirstAnswerOutputMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder timeToFirstAnswerOutputMs(Long value) {
            this.timeToFirstAnswerOutputMs = value;
            return this;
        }

        /**
         * Sets the {@code streamEventCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder streamEventCount(Long value) {
            this.streamEventCount = value;
            return this;
        }

        /**
         * Sets the {@code outputEventCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder outputEventCount(Long value) {
            this.outputEventCount = value;
            return this;
        }

        /**
         * Sets the {@code maxStreamEventGapMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder maxStreamEventGapMs(Long value) {
            this.maxStreamEventGapMs = value;
            return this;
        }

        /**
         * Sets the {@code maxOutputGapBeforeAnswerMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder maxOutputGapBeforeAnswerMs(Long value) {
            this.maxOutputGapBeforeAnswerMs = value;
            return this;
        }

        /**
         * Sets the {@code maxOutputGapAfterAnswerMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder maxOutputGapAfterAnswerMs(Long value) {
            this.maxOutputGapAfterAnswerMs = value;
            return this;
        }

        /**
         * Sets the {@code idleMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder idleMs(Long value) {
            this.idleMs = value;
            return this;
        }

        /**
         * Sets the {@code sinceLastOutputMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sinceLastOutputMs(Long value) {
            this.sinceLastOutputMs = value;
            return this;
        }

        /**
         * Sets the {@code answerChars} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder answerChars(Long value) {
            this.answerChars = value;
            return this;
        }

        /**
         * Sets the {@code reasoningChars} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reasoningChars(Long value) {
            this.reasoningChars = value;
            return this;
        }

        /**
         * Sets the {@code inputTokensEstimate} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder inputTokensEstimate(Long value) {
            this.inputTokensEstimate = value;
            return this;
        }

        /**
         * Sets the {@code inputShrunk} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder inputShrunk(Boolean value) {
            this.inputShrunk = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CompactionCompleteSummarizerProgress build() {
            if (durationMs == null) throw new IllegalStateException("Missing required property: durationMs");
            if (attemptCount == null) throw new IllegalStateException("Missing required property: attemptCount");
            if (streamEventCount == null) throw new IllegalStateException("Missing required property: streamEventCount");
            if (outputEventCount == null) throw new IllegalStateException("Missing required property: outputEventCount");
            return new CompactionCompleteSummarizerProgress(durationMs, attemptCount, attemptAgeMs, attemptSettled, timeToFirstTokenMs, timeToFirstOutputMs, timeToFirstAnswerOutputMs, streamEventCount, outputEventCount, maxStreamEventGapMs, maxOutputGapBeforeAnswerMs, maxOutputGapAfterAnswerMs, idleMs, sinceLastOutputMs, answerChars, reasoningChars, inputTokensEstimate, inputShrunk);
        }
    }

    /**
     * Retains the constructor published in Java SDK 1.0.28.
     * @deprecated Use named construction via {@link #builder()}.
     * @param durationMs Time from the start of the compaction to the decision of its outcome, in milliseconds
     * @param attemptCount Number of summarizer model requests dispatched, including retries
     * @param attemptAgeMs Time since the latest request was dispatched, whether or not it had finished, in milliseconds. Absent when no request was dispatched.
     * @param attemptSettled Whether the latest request had already returned a response or an error. False when it was still in flight, such as when the compaction deadline cut it off; absent when no request was dispatched.
     * @param timeToFirstTokenMs Time from dispatching the latest request to its first token-bearing stream event, in milliseconds. Absent when no stream event arrived.
     * @param timeToFirstOutputMs Time from dispatching the latest request to its first observable text, reasoning, or tool-call output, in milliseconds. Absent when no output arrived.
     * @param timeToFirstAnswerOutputMs Time from dispatching the latest request to its first reply text or tool-call output, in milliseconds. Reasoning does not count. Absent when no such output arrived.
     * @param streamEventCount Token-bearing stream events the latest request received, keepalive deltas included
     * @param outputEventCount Stream events of the latest request that carried observable output
     * @param maxStreamEventGapMs Longest gap between two consecutive stream events of the latest request, in milliseconds. Excludes the still-open gap reported as idleMs; absent with fewer than two events.
     * @param maxOutputGapBeforeAnswerMs Longest gap between two consecutive outputs of the latest request up to its first reply text or tool-call output, including the gap that ends there, in milliseconds. Excludes the still-open gap reported as sinceLastOutputMs; absent without such a gap.
     * @param maxOutputGapAfterAnswerMs Longest gap between two consecutive outputs of the latest request after its first reply text or tool-call output, in milliseconds. Excludes the still-open gap reported as sinceLastOutputMs; absent without such a gap.
     * @param idleMs Time since the latest request's last stream event, or since its dispatch if none arrived, in milliseconds. Absent when no request was dispatched.
     * @param sinceLastOutputMs Time since the latest request's last observable output, in milliseconds. Unlike idleMs, keepalive deltas do not reset it. Absent when no output arrived.
     * @param inputShrunk Whether historical attachments and reasoning were dropped from the compaction's input because the session's previous compaction ran out of time.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CompactionCompleteSummarizerProgress(
        Long durationMs,
        Long attemptCount,
        Long attemptAgeMs,
        Boolean attemptSettled,
        Long timeToFirstTokenMs,
        Long timeToFirstOutputMs,
        Long timeToFirstAnswerOutputMs,
        Long streamEventCount,
        Long outputEventCount,
        Long maxStreamEventGapMs,
        Long maxOutputGapBeforeAnswerMs,
        Long maxOutputGapAfterAnswerMs,
        Long idleMs,
        Long sinceLastOutputMs,
        Boolean inputShrunk
    ) {
        this(durationMs, attemptCount, attemptAgeMs, attemptSettled, timeToFirstTokenMs, timeToFirstOutputMs, timeToFirstAnswerOutputMs, streamEventCount, outputEventCount, maxStreamEventGapMs, maxOutputGapBeforeAnswerMs, maxOutputGapAfterAnswerMs, idleMs, sinceLastOutputMs, null, null, null, inputShrunk);
    }
}
