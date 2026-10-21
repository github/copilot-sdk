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
 * An MCP prompt icon with standard size hints and preserved non-standard fields.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpPromptIcon(
    /** Icon URI */
    @JsonProperty("src") String src,
    /** Icon MIME type, when known */
    @JsonProperty("mimeType") String mimeType,
    /** Icon sizes, such as `48x48` or `any` */
    @JsonProperty("sizes") List<String> sizes,
    /** Theme hint for this icon */
    @JsonProperty("theme") String theme,
    /** Server-provided non-standard icon fields */
    @JsonProperty("additionalProperties") Map<String, Object> additionalProperties
) {
}
