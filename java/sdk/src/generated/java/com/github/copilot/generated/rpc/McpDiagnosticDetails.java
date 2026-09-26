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
 * MCP-specific detail for a source-discriminated diagnostic entry.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpDiagnosticDetails(
    /** Diagnostic record category. */
    @JsonProperty("kind") McpDiagnosticKind kind,
    /** Configured MCP server name. */
    @JsonProperty("serverName") String serverName,
    /** Fresh identifier for the MCP connection attempt, including failed starts. */
    @JsonProperty("connectionId") String connectionId,
    /** Protocol-frame direction when kind is protocol. */
    @JsonProperty("direction") McpDiagnosticDirection direction,
    /** Serialized diagnostic detail. Protocol and HTTP records use JSON when detail is present. */
    @JsonProperty("data") String data
) {
}
