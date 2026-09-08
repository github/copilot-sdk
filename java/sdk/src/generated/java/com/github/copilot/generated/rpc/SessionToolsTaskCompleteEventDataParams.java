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
 * Task-completion tool arguments and final result used to build a label-safe session event payload.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionToolsTaskCompleteEventDataParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Arguments supplied to the completed task_complete tool call. */
    @JsonProperty("toolArgs") Object toolArgs,
    /** Final expanded result returned by the task_complete tool. */
    @JsonProperty("finalResult") ToolResultExpanded finalResult
) {
}
