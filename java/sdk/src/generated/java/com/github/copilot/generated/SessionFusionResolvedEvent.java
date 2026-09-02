/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "session.fusion_resolved". Experimental durable validated HydraFusion route and turn policy.
 * @since 1.0.0
 */
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
        /** Zero-based index of the local policy rule that matched. */
        @JsonProperty("ruleIndex") Long ruleIndex,
        /** Human-readable name of the local policy rule that matched. */
        @JsonProperty("ruleName") String ruleName,
        /** Validated capability scores used to select the route. */
        @JsonProperty("scores") FusionScores scores,
        /** Validated orchestration pattern selected for the turn. */
        @JsonProperty("pattern") FusionPattern pattern,
        /** Presentation-neutral phase plan for clients that render workflow progress. */
        @JsonProperty("phasePlan") List<FusionPhasePlanStep> phasePlan,
        /** Concrete model selected for the primary solver phase. */
        @JsonProperty("primaryModel") String primaryModel,
        /** Concrete model selected for the review or judge phase, when required. */
        @JsonProperty("secondaryModel") String secondaryModel,
        /** Concrete model used when the planned primary model cannot execute. */
        @JsonProperty("fallbackModel") String fallbackModel,
        /** Concrete model recommended for eligible follow-up turns. */
        @JsonProperty("followUpModel") String followUpModel,
        /** Router recommendation controlling reuse or rerouting on later turns. */
        @JsonProperty("followUp") FusionFollowUpRecommendation followUp,
        /** Elapsed time in milliseconds required to resolve and validate the route. */
        @JsonProperty("routingLatencyMs") Double routingLatencyMs
    ) {
    }
}
