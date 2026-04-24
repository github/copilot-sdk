/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record InstructionsSources(
    /** Unique identifier for this source (used for toggling) */
    @JsonProperty("id") String id,
    /** Human-readable label */
    @JsonProperty("label") String label,
    /** File path relative to repo or absolute for home */
    @JsonProperty("sourcePath") String sourcePath,
    /** Raw content of the instruction file */
    @JsonProperty("content") String content,
    /** Category of instruction source — used for merge logic */
    @JsonProperty("type") InstructionsSourcesType type,
    /** Where this source lives — used for UI grouping */
    @JsonProperty("location") InstructionsSourcesLocation location,
    /** Glob pattern from frontmatter — when set, this instruction applies only to matching files */
    @JsonProperty("applyTo") String applyTo,
    /** Short description (body after frontmatter) for use in instruction tables */
    @JsonProperty("description") String description
) {
}
