/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Schema for the `McpAppToolCallCompleteToolMetaUI` type.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpAppToolCallCompleteToolMetaUI(
    /** `ui://` URI declared by the tool's `_meta.ui.resourceUri` */
    @JsonProperty("resourceUri") String resourceUri,
    /** Tool visibility per SEP-1865 (typically a subset of `["model","app"]`) */
    @JsonProperty("visibility") List<String> visibility
) {
}
