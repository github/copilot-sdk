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
 * Schema for the `InstructionSource` type.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record InstructionSource(
    /** Unique identifier for this source (used for toggling) */
    @JsonProperty("id") String id,
    /** Human-readable label */
    @JsonProperty("label") String label,
    /** File path relative to repo or absolute for home */
    @JsonProperty("sourcePath") String sourcePath,
    /** Raw content of the instruction file */
    @JsonProperty("content") String content,
    /** Category of instruction source — used for merge logic */
    @JsonProperty("type") InstructionSourceType type,
    /** Where this source lives — used for UI grouping */
    @JsonProperty("location") InstructionSourceLocation location,
    /** Glob pattern(s) from frontmatter — when set, this instruction applies only to matching files */
    @JsonProperty("applyTo") List<String> applyTo,
    /** Short description (body after frontmatter) for use in instruction tables */
    @JsonProperty("description") String description,
    /** When true, this source starts disabled and must be toggled on by the user */
    @JsonProperty("defaultDisabled") Boolean defaultDisabled,
    /** The project path this source was discovered from. Only set by sessionless discovery for repository/working-directory sources, where it disambiguates same-named files (e.g. .github/copilot-instructions.md) across multiple workspace roots. The session-scoped getSources leaves it unset. */
    @JsonProperty("projectPath") String projectPath
) {
}
