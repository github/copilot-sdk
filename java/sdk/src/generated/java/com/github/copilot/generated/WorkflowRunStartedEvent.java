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
 * Session event "workflow.run_started". Ephemeral signal that a workflow run attempt began executing.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class WorkflowRunStartedEvent extends SessionEvent {

    @Override
    public String getType() { return "workflow.run_started"; }

    @JsonProperty("data")
    private WorkflowRunStartedEventData data;

    public WorkflowRunStartedEventData getData() { return data; }
    public void setData(WorkflowRunStartedEventData data) { this.data = data; }

    /** Data payload for {@link WorkflowRunStartedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record WorkflowRunStartedEventData(
        /** Identifier of the workflow run that started. */
        @JsonProperty("runId") String runId,
        /** Name of the workflow this run executes. Low cardinality by construction. */
        @JsonProperty("workflowName") String workflowName,
        /** Attempt number this start committed; a resumed run increments it. */
        @JsonProperty("attempt") Long attempt
    ) {
    }
}
