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
 * An MCP resource descriptor (spec `Resource`): URI, name, and optional title, description, MIME type, size, icons, annotations, and metadata. Server-provided fields outside the standard descriptor shape are exposed under `additionalProperties`.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpResource(
    /** The resource URI (e.g. ui://... or file:///...) */
    @JsonProperty("uri") String uri,
    /** The programmatic name of the resource */
    @JsonProperty("name") String name,
    /** Optional human-readable display title */
    @JsonProperty("title") String title,
    /** Optional description of what this resource represents */
    @JsonProperty("description") String description,
    /** MIME type of the resource, if known */
    @JsonProperty("mimeType") String mimeType,
    /** Resource size in bytes, when known */
    @JsonProperty("size") Long size,
    /** Icons associated with this resource */
    @JsonProperty("icons") List<McpResourceIcon> icons,
    /** Model/client annotations associated with this resource */
    @JsonProperty("annotations") McpResourceAnnotations annotations,
    /** Resource-level metadata */
    @JsonProperty("_meta") Map<String, Object> meta,
    /** Server-provided non-standard descriptor fields preserved from the MCP response */
    @JsonProperty("additionalProperties") Map<String, Object> additionalProperties
) {
}
