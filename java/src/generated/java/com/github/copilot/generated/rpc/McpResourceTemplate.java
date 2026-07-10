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
 * An MCP resource template descriptor (spec `ResourceTemplate`): an RFC 6570 URI template, name, and optional title, description, MIME type, icons, annotations, and metadata. Server-provided fields outside the standard descriptor shape are exposed under `additionalProperties`.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpResourceTemplate(
    /** An RFC 6570 URI template for constructing resource URIs */
    @JsonProperty("uriTemplate") String uriTemplate,
    /** The programmatic name of the resource template */
    @JsonProperty("name") String name,
    /** Optional human-readable display title */
    @JsonProperty("title") String title,
    /** Optional description of what this template is for */
    @JsonProperty("description") String description,
    /** MIME type for resources matching this template, if uniform */
    @JsonProperty("mimeType") String mimeType,
    /** Icons associated with resources matching this template */
    @JsonProperty("icons") List<McpResourceIcon> icons,
    /** Model/client annotations associated with this template */
    @JsonProperty("annotations") McpResourceAnnotations annotations,
    /** Resource-template-level metadata */
    @JsonProperty("_meta") Map<String, Object> meta,
    /** Server-provided non-standard descriptor fields preserved from the MCP response */
    @JsonProperty("additionalProperties") Map<String, Object> additionalProperties
) {
}
