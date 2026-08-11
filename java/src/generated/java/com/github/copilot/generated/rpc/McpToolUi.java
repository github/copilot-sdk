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
 * Normalized MCP Apps discovery metadata from a tool's `_meta.ui` block.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpToolUi(
    /** URI of the tool's MCP App resource, typically a `ui://` resource identifier. Use `session.mcp.resources.read` to fetch its HTML and resource metadata. */
    @JsonProperty("resourceUri") String resourceUri,
    /** Tool visibility advertised by the server. When absent, MCP Apps defaults apply. */
    @JsonProperty("visibility") List<McpToolUiVisibility> visibility
) {
}
