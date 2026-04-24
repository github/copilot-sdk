/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Tool(
    /** Tool identifier (e.g., "bash", "grep", "str_replace_editor") */
    @JsonProperty("name") String name,
    /** Optional namespaced name for declarative filtering (e.g., "playwright/navigate" for MCP tools) */
    @JsonProperty("namespacedName") String namespacedName,
    /** Description of what the tool does */
    @JsonProperty("description") String description,
    /** JSON Schema for the tool's input parameters */
    @JsonProperty("parameters") Map<String, Object> parameters,
    /** Optional instructions for how to use this tool effectively */
    @JsonProperty("instructions") String instructions
) {
}
