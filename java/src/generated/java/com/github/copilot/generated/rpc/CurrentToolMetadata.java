/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Lightweight metadata for a currently initialized session tool
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CurrentToolMetadata(
    /** Model-facing tool name */
    @JsonProperty("name") String name,
    /** Optional MCP/config namespaced tool name */
    @JsonProperty("namespacedName") String namespacedName,
    /** MCP server name for MCP-backed tools */
    @JsonProperty("mcpServerName") String mcpServerName,
    /** Raw MCP tool name for MCP-backed tools */
    @JsonProperty("mcpToolName") String mcpToolName,
    /** Tool description */
    @JsonProperty("description") String description,
    /** JSON Schema for tool input */
    @JsonProperty("input_schema") Map<String, Object> inputSchema,
    /** Whether the tool is loaded on demand via tool search */
    @JsonProperty("deferLoading") Boolean deferLoading
) {
}
