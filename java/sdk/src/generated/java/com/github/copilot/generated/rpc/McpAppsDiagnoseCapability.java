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
 * Capability negotiation snapshot
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpAppsDiagnoseCapability(
    /** Whether the session has the `mcp-apps` capability */
    @JsonProperty("sessionHasMcpApps") Boolean sessionHasMcpApps,
    /** Whether the MCP_APPS feature flag (or COPILOT_MCP_APPS env override) is on */
    @JsonProperty("featureFlagEnabled") Boolean featureFlagEnabled,
    /** Whether the runtime advertises `extensions.io.modelcontextprotocol/ui` to MCP servers */
    @JsonProperty("advertised") Boolean advertised
) {
}
