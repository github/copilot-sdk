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
 * Deprecated/obsolete MCP Apps alias for `McpResourcesReadRequest`; use `session.mcp.resources.read` instead.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@Deprecated
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMcpAppsReadResourceParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Name of the MCP server hosting the resource */
    @JsonProperty("serverName") String serverName,
    /** Resource URI */
    @JsonProperty("uri") String uri
) {
}
