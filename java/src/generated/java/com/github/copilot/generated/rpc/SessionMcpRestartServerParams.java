/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Server name and opaque configuration for an individual MCP server restart.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMcpRestartServerParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Name of the MCP server to restart */
    @JsonProperty("serverName") String serverName,
    /** Opaque server configuration (MCPServerConfig). Marked internal: an in-process runtime shape supplied only by in-process CLI callers. */
    @JsonProperty("config") Object config
) {
}
