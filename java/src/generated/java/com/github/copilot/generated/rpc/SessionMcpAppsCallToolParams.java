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
 * MCP server, tool name, and arguments to invoke from an MCP App view.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMcpAppsCallToolParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** MCP server hosting the tool */
    @JsonProperty("serverName") String serverName,
    /** MCP tool name */
    @JsonProperty("toolName") String toolName,
    /** Tool arguments */
    @JsonProperty("arguments") Map<String, Object> arguments,
    /** **Required.** Server whose ui:// view issued the request. Per SEP-1865 ('callable by the app from this server only'), the call is rejected when this differs from `serverName`, and rejected outright when missing. */
    @JsonProperty("originServerName") String originServerName
) {
}
