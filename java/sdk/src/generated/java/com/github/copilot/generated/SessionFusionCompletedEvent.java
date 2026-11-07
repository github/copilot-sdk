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
import javax.annotation.processing.Generated;

/**
 * Session event "session.fusion_completed". Experimental durable aggregate outcome of a HydraFusion turn.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionFusionCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.fusion_completed"; }

    @JsonProperty("data")
    private SessionFusionCompletedEventData data;

    public SessionFusionCompletedEventData getData() { return data; }
    public void setData(SessionFusionCompletedEventData data) { this.data = data; }

    /** Data payload for {@link SessionFusionCompletedEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionFusionCompletedEventData(
        /** Stable identifier for the completed HydraFusion turn. */
        @JsonProperty("fusionId") String fusionId,
        /** Idempotency identifier for the authoritative final commit. */
        @JsonProperty("commitId") String commitId,
        /** Identifier of the session turn associated with the completion. */
        @JsonProperty("turnId") String turnId,
        /** Synthetic HydraFusion model selected for the session. */
        @JsonProperty("syntheticModel") String syntheticModel,
        /** HydraFusion orchestration pattern executed for the turn. */
        @JsonProperty("pattern") FusionPattern pattern,
        /** Stable aggregate outcome of the HydraFusion turn. */
        @JsonProperty("outcome") String outcome,
        /** Phase whose output supplied the authoritative final content. */
        @JsonProperty("finalSourcePhaseId") String finalSourcePhaseId,
        /** Concrete model that supplied the authoritative final content. */
        @JsonProperty("finalSourceModel") String finalSourceModel,
        /** Concrete model recommended for eligible follow-up turns. */
        @JsonProperty("followUpModel") String followUpModel,
        /** Reason the turn used a degraded route, when applicable. */
        @JsonProperty("degradedReason") String degradedReason,
        /** Number of concrete phases attempted by the turn. */
        @JsonProperty("phaseCount") Long phaseCount,
        /** Total concrete model requests made across all phases. */
        @JsonProperty("requestCount") Long requestCount,
        /** Total input tokens consumed across all phases. */
        @JsonProperty("inputTokens") Long inputTokens,
        /** Total output tokens produced across all phases. */
        @JsonProperty("outputTokens") Long outputTokens,
        /** Total cached input tokens reported across all phases. */
        @JsonProperty("cachedTokens") Long cachedTokens,
        /** Total tokens written to prompt cache across all phases. */
        @JsonProperty("cacheWriteTokens") Long cacheWriteTokens,
        /** Total normalized AI-unit cost reported across all phases, in nano-AIU. */
        @JsonProperty("totalNanoAiu") Double totalNanoAiu,
        /** Total elapsed execution time for the HydraFusion turn in milliseconds. */
        @JsonProperty("durationMs") Double durationMs,
        /** True when the committed first pass ended with background work outstanding, so that work's completion notifications continue the conversation; absent otherwise. */
        @CopilotExperimental
        @JsonProperty("awaitingBackgroundWork") Boolean awaitingBackgroundWork
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionFusionCompletedEventData {}

        /**
         * Returns a named builder.
         * @return a new builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public static Builder builder() { return new Builder(); }

        /**
         * Named construction for this record.
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public static final class Builder {
            private Builder() {}
            private String fusionId;
            private String commitId;
            private String turnId;
            private String syntheticModel;
            private FusionPattern pattern;
            private String outcome;
            private String finalSourcePhaseId;
            private boolean $presence6;
            private String finalSourceModel;
            private boolean $presence7;
            private String followUpModel;
            private String degradedReason;
            private boolean $presence9;
            private Long phaseCount;
            private Long requestCount;
            private Long inputTokens;
            private Long outputTokens;
            private Long cachedTokens;
            private Long cacheWriteTokens;
            private Double totalNanoAiu;
            private Double durationMs;
            private Boolean awaitingBackgroundWork;

            /**
             * Sets the {@code fusionId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder fusionId(String value) {
                this.fusionId = value;
                return this;
            }

            /**
             * Sets the {@code commitId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder commitId(String value) {
                this.commitId = value;
                return this;
            }

            /**
             * Sets the {@code turnId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder turnId(String value) {
                this.turnId = value;
                return this;
            }

            /**
             * Sets the {@code syntheticModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder syntheticModel(String value) {
                this.syntheticModel = value;
                return this;
            }

            /**
             * Sets the {@code pattern} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder pattern(FusionPattern value) {
                this.pattern = value;
                return this;
            }

            /**
             * Sets the {@code outcome} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder outcome(String value) {
                this.outcome = value;
                return this;
            }

            /**
             * Sets the {@code finalSourcePhaseId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder finalSourcePhaseId(String value) {
                this.finalSourcePhaseId = value;
                this.$presence6 = true;
                return this;
            }

            /**
             * Sets the {@code finalSourceModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder finalSourceModel(String value) {
                this.finalSourceModel = value;
                this.$presence7 = true;
                return this;
            }

            /**
             * Sets the {@code followUpModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder followUpModel(String value) {
                this.followUpModel = value;
                return this;
            }

            /**
             * Sets the {@code degradedReason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder degradedReason(String value) {
                this.degradedReason = value;
                this.$presence9 = true;
                return this;
            }

            /**
             * Sets the {@code phaseCount} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder phaseCount(Long value) {
                this.phaseCount = value;
                return this;
            }

            /**
             * Sets the {@code requestCount} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder requestCount(Long value) {
                this.requestCount = value;
                return this;
            }

            /**
             * Sets the {@code inputTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
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
            @com.github.copilot.CopilotExperimental
            public Builder outputTokens(Long value) {
                this.outputTokens = value;
                return this;
            }

            /**
             * Sets the {@code cachedTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder cachedTokens(Long value) {
                this.cachedTokens = value;
                return this;
            }

            /**
             * Sets the {@code cacheWriteTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder cacheWriteTokens(Long value) {
                this.cacheWriteTokens = value;
                return this;
            }

            /**
             * Sets the {@code totalNanoAiu} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder totalNanoAiu(Double value) {
                this.totalNanoAiu = value;
                return this;
            }

            /**
             * Sets the {@code durationMs} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder durationMs(Double value) {
                this.durationMs = value;
                return this;
            }

            /**
             * Sets the {@code awaitingBackgroundWork} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder awaitingBackgroundWork(Boolean value) {
                this.awaitingBackgroundWork = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionFusionCompletedEventData build() {
                if (fusionId == null) throw new IllegalStateException("Missing required property: fusionId");
                if (commitId == null) throw new IllegalStateException("Missing required property: commitId");
                if (turnId == null) throw new IllegalStateException("Missing required property: turnId");
                if (syntheticModel == null) throw new IllegalStateException("Missing required property: syntheticModel");
                if (pattern == null) throw new IllegalStateException("Missing required property: pattern");
                if (outcome == null) throw new IllegalStateException("Missing required property: outcome");
                if (!$presence6) throw new IllegalStateException("Missing required property: finalSourcePhaseId");
                if (!$presence7) throw new IllegalStateException("Missing required property: finalSourceModel");
                if (followUpModel == null) throw new IllegalStateException("Missing required property: followUpModel");
                if (!$presence9) throw new IllegalStateException("Missing required property: degradedReason");
                if (phaseCount == null) throw new IllegalStateException("Missing required property: phaseCount");
                if (requestCount == null) throw new IllegalStateException("Missing required property: requestCount");
                if (inputTokens == null) throw new IllegalStateException("Missing required property: inputTokens");
                if (outputTokens == null) throw new IllegalStateException("Missing required property: outputTokens");
                if (cachedTokens == null) throw new IllegalStateException("Missing required property: cachedTokens");
                if (totalNanoAiu == null) throw new IllegalStateException("Missing required property: totalNanoAiu");
                if (durationMs == null) throw new IllegalStateException("Missing required property: durationMs");
                return new SessionFusionCompletedEventData(fusionId, commitId, turnId, syntheticModel, pattern, outcome, finalSourcePhaseId, finalSourceModel, followUpModel, degradedReason, phaseCount, requestCount, inputTokens, outputTokens, cachedTokens, cacheWriteTokens, totalNanoAiu, durationMs, awaitingBackgroundWork);
            }
        }
    }
}
