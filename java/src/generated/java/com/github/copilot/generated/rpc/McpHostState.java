/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Host-level state, omitted when no MCP host is initialized.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpHostState(
    /** Whether third-party MCP servers are policy-enabled for this session. */
    @JsonProperty("mcp3pEnabled") Boolean mcp3pEnabled,
    /** Configured servers that are explicitly disabled. */
    @JsonProperty("disabledServers") List<String> disabledServers,
    /** Configured servers filtered out by enterprise allowlist policy. */
    @JsonProperty("filteredServers") List<String> filteredServers,
    /** Names of currently-connected MCP clients. */
    @JsonProperty("clients") List<String> clients,
    /** Names of servers with in-flight connection attempts. */
    @JsonProperty("pendingConnections") List<String> pendingConnections,
    /** Map of server name to recorded connection failure. */
    @JsonProperty("failedServers") Map<String, McpServerFailureInfo> failedServers,
    /** Map of server name to recorded pending-auth state. */
    @JsonProperty("needsAuthServers") Map<String, McpServerNeedsAuthInfo> needsAuthServers
) {
}
