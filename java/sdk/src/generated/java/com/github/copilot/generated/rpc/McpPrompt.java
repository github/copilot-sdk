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
 * An MCP prompt descriptor. Server-provided non-standard fields are exposed under `additionalProperties`.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpPrompt(
    /** The programmatic name of the prompt */
    @JsonProperty("name") String name,
    /** Human-readable display title */
    @JsonProperty("title") String title,
    /** Description of what this prompt provides */
    @JsonProperty("description") String description,
    /** Arguments accepted by the prompt */
    @JsonProperty("arguments") List<McpPromptArgument> arguments,
    /** Icons associated with this prompt */
    @JsonProperty("icons") List<McpPromptIcon> icons,
    /** Prompt-level metadata */
    @JsonProperty("_meta") Map<String, Object> meta,
    /** Server-provided non-standard descriptor fields */
    @JsonProperty("additionalProperties") Map<String, Object> additionalProperties
) {
}
