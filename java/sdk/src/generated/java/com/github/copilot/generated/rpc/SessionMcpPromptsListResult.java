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
 * One page of prompts advertised by the named MCP server.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMcpPromptsListResult(
    /** Prompts advertised by the server */
    @JsonProperty("prompts") List<McpPrompt> prompts,
    /** Opaque cursor for the next page, if the server has more prompts */
    @JsonProperty("nextCursor") String nextCursor,
    /** MCP result metadata */
    @JsonProperty("_meta") Map<String, Object> meta,
    /** Server-provided non-standard result fields */
    @JsonProperty("additionalProperties") Map<String, Object> additionalProperties
) {
}
