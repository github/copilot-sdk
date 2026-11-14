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
import javax.annotation.processing.Generated;

/**
 * Non-secret host-managed HTTP MCP server configuration. The containing map key is the stable managed identity; credentials are supplied dynamically by the host.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedMcpServerConfig(
    /** Human-readable catalog display name. */
    @JsonProperty("displayName") String displayName,
    /** Hosted MCP streamable HTTP endpoint. */
    @JsonProperty("url") String url,
    /** Tools to include. Defaults to all tools when omitted. */
    @JsonProperty("tools") List<String> tools,
    /** Timeout in milliseconds for tool discovery and tool calls. */
    @JsonProperty("timeout") Long timeout,
    /** Maximum dynamic-header cache lifetime in milliseconds. */
    @JsonProperty("headersRefreshTtlMs") Long headersRefreshTtlMs
) {
}
