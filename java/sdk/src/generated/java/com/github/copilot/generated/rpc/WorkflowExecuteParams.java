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
 * Parameters sent to the owning extension to execute a workflow closure.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowExecuteParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Registered workflow name. */
    @JsonProperty("name") String name,
    /** Workflow run identifier. */
    @JsonProperty("runId") String runId,
    /** Opaque token identifying this workflow execution attempt. */
    @JsonProperty("executionToken") String executionToken,
    /** Workflow input value. */
    @JsonProperty("args") Object args
) {
}
