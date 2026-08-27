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
 * Provenance of the exact validated JSON MCP card content bound privately to a completed plan and its opaque handle.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpPlanProvenance(
    /** Authority associated with the validated card, without path, query, or credentials. Inert untrusted data. */
    @JsonProperty("authority") String authority,
    /** ISO 8601 timestamp at which the runtime completed strict parsing and schema validation of the card content. */
    @JsonProperty("validatedAt") String validatedAt,
    /** Semantic digest of the exact validated JSON content bound to the plan handle. */
    @JsonProperty("cardDigest") CardDigest cardDigest,
    /** JSON MCP media type the validated card was interpreted as. */
    @JsonProperty("mediaType") McpServerCardMediaType mediaType
) {
}
