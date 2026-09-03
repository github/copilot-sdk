/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Durable factory run summary with read-time live overlays.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FactoryRunSummary(
    /** Factory run identifier. */
    @JsonProperty("runId") String runId,
    /** Registered factory name. */
    @JsonProperty("factoryName") String factoryName,
    /** Human-readable factory description. */
    @JsonProperty("description") String description,
    /** Current factory run status. */
    @JsonProperty("status") FactoryRunStatus status,
    /** Monotonic durable run revision. */
    @JsonProperty("revision") Long revision,
    /** Epoch milliseconds when the run was created. */
    @JsonProperty("createdAt") Long createdAt,
    /** Epoch milliseconds when execution first started, or null before start. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("startedAt") Long startedAt,
    /** Epoch milliseconds when the durable run was last updated. */
    @JsonProperty("updatedAt") Long updatedAt,
    /** Epoch milliseconds when the run completed, or null while nonterminal. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("completedAt") Long completedAt,
    /** Current phase identity, or null before any phase is entered. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("currentPhase") FactoryCurrentPhase currentPhase,
    /** Number of phases declared by the factory. */
    @JsonProperty("declaredPhaseCount") Long declaredPhaseCount,
    /** Number of direct factory agents currently live. */
    @JsonProperty("liveAgentCount") Long liveAgentCount,
    /** Total direct factory agents spawned across all attempts. */
    @JsonProperty("totalSpawnedAgentCount") Long totalSpawnedAgentCount,
    /** Durable resource consumption. */
    @JsonProperty("consumed") FactoryRunConsumed consumed,
    /** Resource ceilings declared by the factory. */
    @JsonProperty("declaredLimits") FactoryDeclaredLimits declaredLimits,
    /** Approved effective resource ceilings, or null until approved. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("approved") FactoryDeclaredLimits approved,
    /** Epoch milliseconds when this live-overlay snapshot was observed. */
    @JsonProperty("observedAt") Long observedAt,
    /** Epoch milliseconds when the current active segment started, or null while inactive. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("activeSegmentStartedAt") Long activeSegmentStartedAt,
    /** Terminal run outcome, or null while nonterminal. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("terminal") FactoryRunTerminal terminal
) {
}
