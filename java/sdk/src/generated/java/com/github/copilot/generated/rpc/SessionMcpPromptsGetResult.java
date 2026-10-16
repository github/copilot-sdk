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
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Prompt messages returned by the MCP server without sending them to the model.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMcpPromptsGetResult(
    /** Description of the prompt */
    @JsonProperty("description") String description,
    /** Ordered prompt messages */
    @JsonProperty("messages") List<McpPromptMessage> messages,
    /** MCP result metadata */
    @JsonProperty("_meta") Map<String, Object> meta,
    /** Server-provided non-standard result fields */
    @JsonProperty("additionalProperties") Map<String, Object> additionalProperties
) {
}
