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
import javax.annotation.processing.Generated;

/**
 * Session event "session.fusion_resolved". Experimental durable validated HydraFusion route and turn policy.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionFusionResolvedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.fusion_resolved"; }

    @JsonProperty("data")
    private SessionFusionResolvedEventData data;

    public SessionFusionResolvedEventData getData() { return data; }
    public void setData(SessionFusionResolvedEventData data) { this.data = data; }

    /** Data payload for {@link SessionFusionResolvedEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionFusionResolvedEventData(
        /** Stable identifier for the resolved HydraFusion turn. */
        @JsonProperty("fusionId") String fusionId,
        /** Identifier of the session turn associated with the route. */
        @JsonProperty("turnId") String turnId,
        /** Version of the validated HydraFusion event contract. */
        @JsonProperty("contractVersion") Long contractVersion,
        /** Synthetic HydraFusion model selected for the session. */
        @JsonProperty("syntheticModel") String syntheticModel,
        /** HydraFusion routing policy used to resolve the plan. */
        @JsonProperty("policy") String policy,
        /** Router implementation that supplied the plan. */
        @JsonProperty("routeSource") String routeSource,
        /** Version of the validated execution-plan format. */
        @JsonProperty("planVersion") String planVersion,
        /** Version of the local routing policy. */
        @JsonProperty("policyVersion") String policyVersion,
        /** Version of the executable model universe used for selection. */
        @JsonProperty("modelUniverseVersion") String modelUniverseVersion,
        /** Identifier of the local policy rule that matched. */
        @JsonProperty("ruleId") String ruleId,
        /** Identifier of the server policy rule whose workflow the plan's steps describe, when CAPI's sticky root routing replaced the workflow of `ruleId`'s rule to keep the previous turn's root model. Absent when no substitution occurred. */
        @CopilotExperimental
        @JsonProperty("selectedRuleId") String selectedRuleId,
        /** Zero-based index of the local policy rule that matched. */
        @JsonProperty("ruleIndex") Long ruleIndex,
        /** Human-readable name of the local policy rule that matched. */
        @JsonProperty("ruleName") String ruleName,
        /** Validated capability scores used to select the route. */
        @JsonProperty("scores") FusionScores scores,
        /** Validated orchestration pattern selected for the turn. */
        @JsonProperty("pattern") FusionPattern pattern,
        /** Short human-readable summary of the selected workflow, suitable for immediate client display after routing. May be absent in older durable events; omit the explanation or derive one from pattern and phasePlan. Display text, not a stable machine-readable value. */
        @CopilotExperimental
        @JsonProperty("hint") String hint,
        /** Presentation-neutral phase plan for clients that render workflow progress. */
        @CopilotExperimental
        @JsonProperty("phasePlan") List<FusionPhasePlanStep> phasePlan,
        /** Concrete model selected for the primary solver phase. */
        @JsonProperty("primaryModel") String primaryModel,
        /** Concrete model selected for Critique review, or the legacy Cascade judge/repair model when role-specific fields are absent. */
        @JsonProperty("secondaryModel") String secondaryModel,
        /** Concrete model selected for Cascade escalation-gate calls, when required. */
        @CopilotExperimental
        @JsonProperty("judgeModel") String judgeModel,
        /** Concrete model selected for Cascade repair, when required. */
        @CopilotExperimental
        @JsonProperty("repairModel") String repairModel,
        /** Planned Critique phase identities, including independent critics with repeated model IDs. May be absent in older events; consumers then use secondaryModel for the legacy critic. */
        @CopilotExperimental
        @JsonProperty("critics") List<FusionCritic> critics,
        /** Concrete model used when the planned primary model cannot execute. */
        @JsonProperty("fallbackModel") String fallbackModel,
        /** Concrete model recommended for eligible follow-up turns. */
        @JsonProperty("followUpModel") String followUpModel,
        /** Router recommendation controlling reuse or rerouting on later turns. */
        @JsonProperty("followUp") FusionFollowUpRecommendation followUp,
        /** Elapsed time in milliseconds required to resolve and validate the route. */
        @JsonProperty("routingLatencyMs") Double routingLatencyMs
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionFusionResolvedEventData {}

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
            private String turnId;
            private Long contractVersion;
            private String syntheticModel;
            private String policy;
            private String routeSource;
            private String planVersion;
            private String policyVersion;
            private String modelUniverseVersion;
            private String ruleId;
            private String selectedRuleId;
            private Long ruleIndex;
            private String ruleName;
            private FusionScores scores;
            private FusionPattern pattern;
            private String hint;
            private List<FusionPhasePlanStep> phasePlan;
            private String primaryModel;
            private String secondaryModel;
            private boolean $presence18;
            private String judgeModel;
            private String repairModel;
            private List<FusionCritic> critics;
            private String fallbackModel;
            private String followUpModel;
            private FusionFollowUpRecommendation followUp;
            private Double routingLatencyMs;

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
             * Sets the {@code contractVersion} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder contractVersion(Long value) {
                this.contractVersion = value;
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
             * Sets the {@code policy} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder policy(String value) {
                this.policy = value;
                return this;
            }

            /**
             * Sets the {@code routeSource} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder routeSource(String value) {
                this.routeSource = value;
                return this;
            }

            /**
             * Sets the {@code planVersion} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder planVersion(String value) {
                this.planVersion = value;
                return this;
            }

            /**
             * Sets the {@code policyVersion} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder policyVersion(String value) {
                this.policyVersion = value;
                return this;
            }

            /**
             * Sets the {@code modelUniverseVersion} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder modelUniverseVersion(String value) {
                this.modelUniverseVersion = value;
                return this;
            }

            /**
             * Sets the {@code ruleId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder ruleId(String value) {
                this.ruleId = value;
                return this;
            }

            /**
             * Sets the {@code selectedRuleId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder selectedRuleId(String value) {
                this.selectedRuleId = value;
                return this;
            }

            /**
             * Sets the {@code ruleIndex} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder ruleIndex(Long value) {
                this.ruleIndex = value;
                return this;
            }

            /**
             * Sets the {@code ruleName} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder ruleName(String value) {
                this.ruleName = value;
                return this;
            }

            /**
             * Sets the {@code scores} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder scores(FusionScores value) {
                this.scores = value;
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
             * Sets the {@code hint} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder hint(String value) {
                this.hint = value;
                return this;
            }

            /**
             * Sets the {@code phasePlan} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder phasePlan(List<FusionPhasePlanStep> value) {
                this.phasePlan = value;
                return this;
            }

            /**
             * Sets the {@code primaryModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder primaryModel(String value) {
                this.primaryModel = value;
                return this;
            }

            /**
             * Sets the {@code secondaryModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder secondaryModel(String value) {
                this.secondaryModel = value;
                this.$presence18 = true;
                return this;
            }

            /**
             * Sets the {@code judgeModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder judgeModel(String value) {
                this.judgeModel = value;
                return this;
            }

            /**
             * Sets the {@code repairModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder repairModel(String value) {
                this.repairModel = value;
                return this;
            }

            /**
             * Sets the {@code critics} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder critics(List<FusionCritic> value) {
                this.critics = value;
                return this;
            }

            /**
             * Sets the {@code fallbackModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder fallbackModel(String value) {
                this.fallbackModel = value;
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
             * Sets the {@code followUp} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder followUp(FusionFollowUpRecommendation value) {
                this.followUp = value;
                return this;
            }

            /**
             * Sets the {@code routingLatencyMs} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder routingLatencyMs(Double value) {
                this.routingLatencyMs = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionFusionResolvedEventData build() {
                if (fusionId == null) throw new IllegalStateException("Missing required property: fusionId");
                if (turnId == null) throw new IllegalStateException("Missing required property: turnId");
                if (contractVersion == null) throw new IllegalStateException("Missing required property: contractVersion");
                if (syntheticModel == null) throw new IllegalStateException("Missing required property: syntheticModel");
                if (policy == null) throw new IllegalStateException("Missing required property: policy");
                if (pattern == null) throw new IllegalStateException("Missing required property: pattern");
                if (primaryModel == null) throw new IllegalStateException("Missing required property: primaryModel");
                if (!$presence18) throw new IllegalStateException("Missing required property: secondaryModel");
                if (fallbackModel == null) throw new IllegalStateException("Missing required property: fallbackModel");
                if (followUpModel == null) throw new IllegalStateException("Missing required property: followUpModel");
                return new SessionFusionResolvedEventData(fusionId, turnId, contractVersion, syntheticModel, policy, routeSource, planVersion, policyVersion, modelUniverseVersion, ruleId, selectedRuleId, ruleIndex, ruleName, scores, pattern, hint, phasePlan, primaryModel, secondaryModel, judgeModel, repairModel, critics, fallbackModel, followUpModel, followUp, routingLatencyMs);
            }
        }
    }
}
