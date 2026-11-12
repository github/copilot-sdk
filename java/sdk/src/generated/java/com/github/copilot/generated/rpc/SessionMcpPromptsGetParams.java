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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * MCP server, prompt name, and optional string-valued arguments.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMcpPromptsGetParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Name of the MCP server hosting the prompt */
    @JsonProperty("serverName") String serverName,
    /** The programmatic name of the prompt */
    @JsonProperty("promptName") String promptName,
    /** String-valued arguments to pass to the prompt */
    @JsonProperty("arguments") Map<String, String> arguments
) {
}
