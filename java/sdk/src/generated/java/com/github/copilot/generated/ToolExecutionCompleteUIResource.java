/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * MCP Apps UI resource content for rendering in a sandboxed iframe
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolExecutionCompleteUIResource(
    /** The ui:// URI of the resource */
    @JsonProperty("uri") String uri,
    /** MIME type of the content */
    @JsonProperty("mimeType") String mimeType,
    /** HTML content as a string */
    @JsonProperty("text") String text,
    /** Base64-encoded HTML content */
    @JsonProperty("blob") String blob,
    /** Resource-level UI metadata (CSP, permissions, visual preferences) */
    @JsonProperty("_meta") ToolExecutionCompleteUIResourceMeta meta
) {
}
