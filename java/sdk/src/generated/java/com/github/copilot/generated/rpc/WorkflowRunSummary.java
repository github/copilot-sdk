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
import javax.annotation.processing.Generated;

/**
 * Durable workflow run summary with read-time live overlays.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowRunSummary(
    /** Workflow run identifier. */
    @JsonProperty("runId") String runId,
    /** Registered workflow name. */
    @JsonProperty("workflowName") String workflowName,
    /** Human-readable workflow description. */
    @JsonProperty("description") String description,
    /** Current workflow run status. */
    @JsonProperty("status") WorkflowRunStatus status,
    /** Monotonic durable run revision. */
    @JsonProperty("revision") Long revision,
    /** Epoch milliseconds when the run was created. */
    @JsonProperty("createdAt") Long createdAt,
    /** Epoch milliseconds when execution first started, or null before start. */
    @JsonProperty("startedAt") Long startedAt,
    /** Epoch milliseconds when the durable run was last updated. */
    @JsonProperty("updatedAt") Long updatedAt,
    /** Epoch milliseconds when the run completed, or null while nonterminal. */
    @JsonProperty("completedAt") Long completedAt,
    /** Current phase identity, or null before any phase is entered. */
    @JsonProperty("currentPhase") WorkflowCurrentPhase currentPhase,
    /** Number of phases declared by the workflow. */
    @JsonProperty("declaredPhaseCount") Long declaredPhaseCount,
    /** Number of direct workflow agents currently live. */
    @JsonProperty("liveAgentCount") Long liveAgentCount,
    /** Total direct workflow agents spawned across all attempts. */
    @JsonProperty("totalSpawnedAgentCount") Long totalSpawnedAgentCount,
    /** Durable resource consumption. */
    @JsonProperty("consumed") WorkflowRunConsumed consumed,
    /** Resource ceilings declared by the workflow. */
    @JsonProperty("declaredLimits") WorkflowDeclaredLimits declaredLimits,
    /** Approved effective resource ceilings, or null until approved. */
    @JsonProperty("approved") WorkflowDeclaredLimits approved,
    /** Epoch milliseconds when this live-overlay snapshot was observed. */
    @JsonProperty("observedAt") Long observedAt,
    /** Epoch milliseconds when the current active segment started, or null while inactive. */
    @JsonProperty("activeSegmentStartedAt") Long activeSegmentStartedAt,
    /** Terminal run outcome, or null while nonterminal. */
    @JsonProperty("terminal") WorkflowRunTerminal terminal,
    /** Whether the durable run state currently passes runtime resume eligibility checks. */
    @JsonProperty("canResume") Boolean canResume
) {
}
