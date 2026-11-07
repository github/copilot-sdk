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
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Session event "session.auto_mode_resolved". Auto Intent resolution: the concrete model an auto-mode session settled on, and why. Published when Auto settles on a model that differs from the one it last published in the current Auto selection, so a routing check that keeps the same model does not repeat it; selecting a model, switching Auto tiers, or returning to Auto starts a new selection. Lets SDK clients render the chosen model and the full reason it was picked. The core selection fields (chosenModel/reasoningBucket/categoryScores) are stable; the routing-analytics fields (predictedLabel/confidence/candidateModels) mirror the upstream intent service and may evolve, hence the event's experimental stability.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionAutoModeResolvedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.auto_mode_resolved"; }

    @JsonProperty("data")
    private SessionAutoModeResolvedEventData data;

    public SessionAutoModeResolvedEventData getData() { return data; }
    public void setData(SessionAutoModeResolvedEventData data) { this.data = data; }

    /** Data payload for {@link SessionAutoModeResolvedEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionAutoModeResolvedEventData(
        /** The concrete model the session will use after any intent refinement */
        @JsonProperty("chosenModel") String chosenModel,
        /** The effective Auto routing preference at publication, including managed defaults. Recorded so resume can distinguish an unchanged preference from a tier switch. Absent when no preference is known or in older session histories. */
        @JsonProperty("autoTier") AutoTier autoTier,
        /** Coarse request-difficulty bucket, for explaining why a model was chosen ("picked X because this looks like high-reasoning work") */
        @JsonProperty("reasoningBucket") AutoModeResolvedReasoningBucket reasoningBucket,
        /** Per-category classifier scores (0-1) behind the bucket: the granular HYDRA capability scores (reasoning, code_gen, debugging, tool_use), or the binary needs_reasoning/no_reasoning scores when HYDRA didn't run. Lets clients show a breakdown rather than just the bucket. */
        @JsonProperty("categoryScores") Map<String, Double> categoryScores,
        /** The predicted classifier label (e.g. `needs_reasoning`), when available */
        @JsonProperty("predictedLabel") String predictedLabel,
        /** Classifier confidence for the predicted label, when available */
        @JsonProperty("confidence") Double confidence,
        /** Ordered candidate model list the router returned, when not a fallback */
        @JsonProperty("candidateModels") List<String> candidateModels,
        /** The routing method the server applied, when Auto Intent ran */
        @JsonProperty("routingMethod") String routingMethod,
        /** Models offered to the router for this resolution */
        @JsonProperty("availableModels") List<String> availableModels,
        /** Whether the router fell back to the standard Auto selection */
        @JsonProperty("fallback") Boolean fallback,
        /** Server-provided reason for falling back, when available */
        @JsonProperty("fallbackReason") String fallbackReason,
        /** Short human-readable sentence from the routing service explaining why this model was chosen, for display alongside the model. Present only when the service supplied one: it is omitted for on-device selections, when the service did not provide an explanation, and when a replayed decision made no routing call. The text is display-only and drawn from a fixed catalogue; several distinct routing categories share identical wording, so it cannot be used to recover the category or keyed on programmatically. */
        @JsonProperty("selectionReason") String selectionReason,
        /** Whether a sticky model choice overrode the router result */
        @JsonProperty("stickyOverride") Boolean stickyOverride,
        /** Server-reported router processing time in milliseconds */
        @JsonProperty("routerLatencyMs") Double routerLatencyMs,
        /** End-to-end client wait time for the router request in milliseconds */
        @JsonProperty("endToEndLatencyMs") Double endToEndLatencyMs,
        /** The chosen model's score shortfall relative to the top candidate */
        @JsonProperty("chosenShortfall") Double chosenShortfall,
        /** Whether the routed prompt contained an image */
        @JsonProperty("hasImage") Boolean hasImage
    ) {

        /**
         * Retains the constructor published in Java SDK 1.0.28.
         * @deprecated Use named construction via {@link #builder()}.
         * @param chosenModel The concrete model the session will use after any intent refinement
         * @param reasoningBucket Coarse request-difficulty bucket, for explaining why a model was chosen ("picked X because this looks like high-reasoning work")
         * @param categoryScores Per-category classifier scores (0-1) behind the bucket: the granular HYDRA capability scores (reasoning, code_gen, debugging, tool_use), or the binary needs_reasoning/no_reasoning scores when HYDRA didn't run. Lets clients show a breakdown rather than just the bucket.
         * @param predictedLabel The predicted classifier label (e.g. `needs_reasoning`), when available
         * @param confidence Classifier confidence for the predicted label, when available
         * @param candidateModels Ordered candidate model list the router returned, when not a fallback
         * @param routingMethod The routing method the server applied, when Auto Intent ran
         * @param availableModels Models offered to the router for this resolution
         * @param fallback Whether the router fell back to the standard Auto selection
         * @param fallbackReason Server-provided reason for falling back, when available
         * @param selectionReason Short human-readable sentence from the routing service explaining why this model was chosen, for display alongside the model. Present only when the service supplied one: it is omitted for on-device selections, when the service did not provide an explanation, and when a replayed decision made no routing call. The text is display-only and drawn from a fixed catalogue; several distinct routing categories share identical wording, so it cannot be used to recover the category or keyed on programmatically.
         * @param stickyOverride Whether a sticky model choice overrode the router result
         * @param routerLatencyMs Server-reported router processing time in milliseconds
         * @param endToEndLatencyMs End-to-end client wait time for the router request in milliseconds
         * @param chosenShortfall The chosen model's score shortfall relative to the top candidate
         * @param hasImage Whether the routed prompt contained an image
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionAutoModeResolvedEventData(
            String chosenModel,
            AutoModeResolvedReasoningBucket reasoningBucket,
            Map<String, Double> categoryScores,
            String predictedLabel,
            Double confidence,
            List<String> candidateModels,
            String routingMethod,
            List<String> availableModels,
            Boolean fallback,
            String fallbackReason,
            String selectionReason,
            Boolean stickyOverride,
            Double routerLatencyMs,
            Double endToEndLatencyMs,
            Double chosenShortfall,
            Boolean hasImage
        ) {
            this(chosenModel, null, reasoningBucket, categoryScores, predictedLabel, confidence, candidateModels, routingMethod, availableModels, fallback, fallbackReason, selectionReason, stickyOverride, routerLatencyMs, endToEndLatencyMs, chosenShortfall, hasImage);
        }

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionAutoModeResolvedEventData {}

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
            private String chosenModel;
            private AutoTier autoTier;
            private AutoModeResolvedReasoningBucket reasoningBucket;
            private Map<String, Double> categoryScores;
            private String predictedLabel;
            private Double confidence;
            private List<String> candidateModels;
            private String routingMethod;
            private List<String> availableModels;
            private Boolean fallback;
            private String fallbackReason;
            private String selectionReason;
            private Boolean stickyOverride;
            private Double routerLatencyMs;
            private Double endToEndLatencyMs;
            private Double chosenShortfall;
            private Boolean hasImage;

            /**
             * Sets the {@code chosenModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder chosenModel(String value) {
                this.chosenModel = value;
                return this;
            }

            /**
             * Sets the {@code autoTier} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder autoTier(AutoTier value) {
                this.autoTier = value;
                return this;
            }

            /**
             * Sets the {@code reasoningBucket} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder reasoningBucket(AutoModeResolvedReasoningBucket value) {
                this.reasoningBucket = value;
                return this;
            }

            /**
             * Sets the {@code categoryScores} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder categoryScores(Map<String, Double> value) {
                this.categoryScores = value;
                return this;
            }

            /**
             * Sets the {@code predictedLabel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder predictedLabel(String value) {
                this.predictedLabel = value;
                return this;
            }

            /**
             * Sets the {@code confidence} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder confidence(Double value) {
                this.confidence = value;
                return this;
            }

            /**
             * Sets the {@code candidateModels} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder candidateModels(List<String> value) {
                this.candidateModels = value;
                return this;
            }

            /**
             * Sets the {@code routingMethod} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder routingMethod(String value) {
                this.routingMethod = value;
                return this;
            }

            /**
             * Sets the {@code availableModels} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder availableModels(List<String> value) {
                this.availableModels = value;
                return this;
            }

            /**
             * Sets the {@code fallback} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder fallback(Boolean value) {
                this.fallback = value;
                return this;
            }

            /**
             * Sets the {@code fallbackReason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder fallbackReason(String value) {
                this.fallbackReason = value;
                return this;
            }

            /**
             * Sets the {@code selectionReason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder selectionReason(String value) {
                this.selectionReason = value;
                return this;
            }

            /**
             * Sets the {@code stickyOverride} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder stickyOverride(Boolean value) {
                this.stickyOverride = value;
                return this;
            }

            /**
             * Sets the {@code routerLatencyMs} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder routerLatencyMs(Double value) {
                this.routerLatencyMs = value;
                return this;
            }

            /**
             * Sets the {@code endToEndLatencyMs} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder endToEndLatencyMs(Double value) {
                this.endToEndLatencyMs = value;
                return this;
            }

            /**
             * Sets the {@code chosenShortfall} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder chosenShortfall(Double value) {
                this.chosenShortfall = value;
                return this;
            }

            /**
             * Sets the {@code hasImage} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder hasImage(Boolean value) {
                this.hasImage = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionAutoModeResolvedEventData build() {
                if (chosenModel == null) throw new IllegalStateException("Missing required property: chosenModel");
                return new SessionAutoModeResolvedEventData(chosenModel, autoTier, reasoningBucket, categoryScores, predictedLabel, confidence, candidateModels, routingMethod, availableModels, fallback, fallbackReason, selectionReason, stickyOverride, routerLatencyMs, endToEndLatencyMs, chosenShortfall, hasImage);
            }
        }
    }
}
