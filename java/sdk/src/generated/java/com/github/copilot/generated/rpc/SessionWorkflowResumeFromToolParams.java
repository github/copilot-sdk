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
 * Internal parameters for resuming a workflow run from a tool.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionWorkflowResumeFromToolParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Workflow run identifier. */
    @JsonProperty("runId") String runId,
    /** Optional per-invocation resource ceiling overrides. */
    @JsonProperty("limits") WorkflowRunLimits limits,
    /** Opaque identifier of the originating tool call. */
    @JsonProperty("toolCallId") String toolCallId
) {
}
