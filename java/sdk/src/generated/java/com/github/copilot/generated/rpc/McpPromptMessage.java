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
 * An MCP prompt message with opaque JSON content preserved without flattening or content-type filtering.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpPromptMessage(
    /** The role of the message sender */
    @JsonProperty("role") McpPromptRole role,
    /** The original MCP content block, including nested metadata and unfamiliar content types */
    @JsonProperty("content") Object content,
    /** Message-level metadata */
    @JsonProperty("_meta") Map<String, Object> meta,
    /** Server-provided non-standard message fields */
    @JsonProperty("additionalProperties") Map<String, Object> additionalProperties
) {
}
