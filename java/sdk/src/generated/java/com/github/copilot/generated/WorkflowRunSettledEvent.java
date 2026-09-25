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
 * Session event "workflow.run_settled". Ephemeral signal that a workflow run reached a terminal status.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class WorkflowRunSettledEvent extends SessionEvent {

    @Override
    public String getType() { return "workflow.run_settled"; }

    @JsonProperty("data")
    private WorkflowRunSettledEventData data;

    public WorkflowRunSettledEventData getData() { return data; }
    public void setData(WorkflowRunSettledEventData data) { this.data = data; }

    /** Data payload for {@link WorkflowRunSettledEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record WorkflowRunSettledEventData(
        /** Identifier of the workflow run that settled. */
        @JsonProperty("runId") String runId,
        /** Terminal status the run committed. */
        @JsonProperty("status") WorkflowRunSettledStatus status,
        /** Subagents this run consumed against its limits. */
        @JsonProperty("consumedSubagents") Long consumedSubagents,
        /** AI credits this run consumed, in nano-AIU. */
        @JsonProperty("consumedNanoAiu") Long consumedNanoAiu,
        /** Active milliseconds accumulated across every attempt of this run. */
        @JsonProperty("elapsedMs") Long elapsedMs,
        /** Typed failure class recorded on the run, when it failed with one (e.g. `workflow_limit_reached`). */
        @JsonProperty("failureType") String failureType
    ) {
    }
}
