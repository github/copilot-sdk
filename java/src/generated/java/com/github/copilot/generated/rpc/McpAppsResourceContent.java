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
 * Deprecated/obsolete MCP Apps alias for `McpResourceContent`; use `session.mcp.resources.read` instead.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpAppsResourceContent(
    /** The resource URI */
    @JsonProperty("uri") String uri,
    /** MIME type of the content */
    @JsonProperty("mimeType") String mimeType,
    /** Text content (e.g. HTML) */
    @JsonProperty("text") String text,
    /** Base64-encoded binary content */
    @JsonProperty("blob") String blob,
    /** Resource-level metadata */
    @JsonProperty("_meta") Map<String, Object> meta
) {
}
