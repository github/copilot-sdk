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
 * Internal parameters for invoking a registered workflow from a tool.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionWorkflowRunFromToolParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Registered workflow name. */
    @JsonProperty("name") String name,
    /** Workflow input value. */
    @JsonProperty("args") Object args,
    /** Tool-originated workflow invocation options. */
    @JsonProperty("options") WorkflowToolRunOptions options,
    /** Opaque identifier of the originating tool call. */
    @JsonProperty("toolCallId") String toolCallId
) {
}
