/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Full factory run observability detail.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFactoryGetRunDetailResult(
    @JsonProperty("runId") String runId,
    @JsonProperty("factoryName") String factoryName,
    @JsonProperty("description") String description,
    @JsonProperty("status") FactoryRunStatus status,
    @JsonProperty("revision") Long revision,
    @JsonProperty("createdAt") Long createdAt,
    @JsonProperty("startedAt") Long startedAt,
    @JsonProperty("updatedAt") Long updatedAt,
    @JsonProperty("completedAt") Long completedAt,
    @JsonProperty("currentPhase") FactoryCurrentPhase currentPhase,
    @JsonProperty("declaredPhaseCount") Long declaredPhaseCount,
    @JsonProperty("liveAgentCount") Long liveAgentCount,
    @JsonProperty("totalSpawnedAgentCount") Long totalSpawnedAgentCount,
    @JsonProperty("consumed") FactoryRunConsumed consumed,
    @JsonProperty("declaredLimits") FactoryDeclaredLimits declaredLimits,
    @JsonProperty("approved") FactoryDeclaredLimits approved,
    @JsonProperty("observedAt") Long observedAt,
    @JsonProperty("activeSegmentStartedAt") Long activeSegmentStartedAt,
    @JsonProperty("terminal") FactoryRunTerminal terminal,
    @JsonProperty("phases") List<FactoryPhaseObservation> phases,
    @JsonProperty("agents") List<FactoryAgentSummary> agents,
    @JsonProperty("progress") FactoryProgressPage progress
) {
}
