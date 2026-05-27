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
 * A single changed file and its unified diff.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkspaceDiffFileChange(
    /** Path to the changed file, relative to the workspace root. */
    @JsonProperty("path") String path,
    /** Unified diff content for the file. Empty when the diff was truncated. */
    @JsonProperty("diff") String diff,
    /** Type of change represented by this file diff. */
    @JsonProperty("changeType") WorkspaceDiffFileChangeType changeType,
    /** Original file path for renamed files. */
    @JsonProperty("oldPath") String oldPath,
    /** Whether the diff content was omitted because it exceeded the per-file size limit. */
    @JsonProperty("isTruncated") Boolean isTruncated
) {
}
