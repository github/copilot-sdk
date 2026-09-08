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
import javax.annotation.processing.Generated;

/**
 * Shell-specific names and description lines used to materialize built-in shell tool descriptors.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolsShellDescriptorConfig(
    /** Stable shell type identifier. */
    @JsonProperty("shellType") String shellType,
    /** Human-readable shell name. */
    @JsonProperty("displayName") String displayName,
    /** Tool name used to start shell commands. */
    @JsonProperty("shellToolName") String shellToolName,
    /** Tool name used to read shell output. */
    @JsonProperty("readShellToolName") String readShellToolName,
    /** Tool name used to stop shell commands. */
    @JsonProperty("stopShellToolName") String stopShellToolName,
    /** Tool name used to list active shells. */
    @JsonProperty("listShellsToolName") String listShellsToolName,
    /** Additional model-facing shell description lines. */
    @JsonProperty("descriptionLines") List<String> descriptionLines
) {
}
