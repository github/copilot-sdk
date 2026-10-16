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
 * One MCP server a reviewed plugin declares.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluginInstallationMcpServerReview(
    /** Server name as the plugin declares it. */
    @JsonProperty("name") String name,
    /** `remote` for an HTTP or SSE server, `local` for a command the plugin runs. */
    @JsonProperty("transport") PluginInstallationMcpTransport transport,
    /** Host of a remote server's URL. */
    @JsonProperty("host") String host
) {
}
