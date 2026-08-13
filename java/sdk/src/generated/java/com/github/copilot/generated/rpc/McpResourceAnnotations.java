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
 * Standard MCP resource annotations plus preserved non-standard annotation fields.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpResourceAnnotations(
    /** Intended audience roles for this resource */
    @JsonProperty("audience") List<String> audience,
    /** Priority hint for model/client use */
    @JsonProperty("priority") Double priority,
    /** Last-modified timestamp hint */
    @JsonProperty("lastModified") String lastModified,
    /** Server-provided non-standard annotation fields preserved from the MCP response */
    @JsonProperty("additionalProperties") Map<String, Object> additionalProperties
) {
}
