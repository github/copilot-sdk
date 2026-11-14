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
 * Where and when an MCP server catalog reference was observed. Discovery provenance deliberately carries no content digest because search does not establish the exact validated content a later plan will bind.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogMcpServerCandidateProvenance(
    /** Host of the catalog authority that advertised the reference, without path, query, or credentials. Inert untrusted data. */
    @JsonProperty("authority") String authority,
    /** ISO 8601 timestamp at which the runtime observed the catalog reference. This is not a retrieval or validation timestamp. */
    @JsonProperty("observedAt") String observedAt,
    /** JSON MCP media type advertised for the referenced card. */
    @JsonProperty("mediaType") McpServerCardMediaType mediaType
) {
}
