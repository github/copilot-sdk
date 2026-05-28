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
 * Tool definition metadata, present for MCP tools with MCP Apps support
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolExecutionCompleteToolDescription(
    /** Tool name */
    @JsonProperty("name") String name,
    /** Tool description */
    @JsonProperty("description") String description,
    /** MCP Apps metadata for UI resource association */
    @JsonProperty("_meta") ToolExecutionCompleteToolDescriptionMeta meta
) {
}
