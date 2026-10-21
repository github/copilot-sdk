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
 * An argument accepted by an MCP prompt.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpPromptArgument(
    /** Name of the argument */
    @JsonProperty("name") String name,
    /** Description of the argument */
    @JsonProperty("description") String description,
    /** Whether the argument is required; omission is distinct from false */
    @JsonProperty("required") Boolean required,
    /** Argument-level metadata */
    @JsonProperty("_meta") Map<String, Object> meta,
    /** Server-provided non-standard argument fields */
    @JsonProperty("additionalProperties") Map<String, Object> additionalProperties
) {
}
