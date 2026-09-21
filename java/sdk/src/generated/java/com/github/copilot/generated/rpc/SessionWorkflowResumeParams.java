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
 * Parameters for resuming a workflow run from its persisted identity.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionWorkflowResumeParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Workflow run identifier. */
    @JsonProperty("runId") String runId,
    /** Optional per-invocation resource ceiling overrides. */
    @JsonProperty("limits") WorkflowRunLimits limits,
    /** Whether to notify the originating session when the workflow completes. */
    @JsonProperty("notifyOnComplete") Boolean notifyOnComplete,
    /** Whether to emit workflow phase names to the session transcript. */
    @JsonProperty("logPhaseNames") Boolean logPhaseNames
) {
}
