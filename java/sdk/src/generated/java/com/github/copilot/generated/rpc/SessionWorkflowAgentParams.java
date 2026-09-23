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
 * Parameters for one workflow-scoped subagent call.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionWorkflowAgentParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Workflow run identifier that owns the subagent. */
    @JsonProperty("workflowRunId") String workflowRunId,
    /** Opaque token identifying the current workflow execution attempt. */
    @JsonProperty("executionToken") String executionToken,
    /** Prompt to send to the subagent. */
    @JsonProperty("prompt") String prompt,
    /** Subagent execution options. */
    @JsonProperty("opts") WorkflowAgentOptions opts
) {
}
