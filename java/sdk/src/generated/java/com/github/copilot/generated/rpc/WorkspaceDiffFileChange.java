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
    /** Path to the changed file, relative to the workspace root when the file lives under it. A file changed outside the workspace root keeps a `../`-relative path, or an absolute path when no relative path exists (for example a different Windows drive). */
    @JsonProperty("path") String path,
    /** Unified diff content for the file. Empty when the diff was truncated. */
    @JsonProperty("diff") String diff,
    /** Type of change represented by this file diff. */
    @JsonProperty("changeType") WorkspaceDiffFileChangeType changeType,
    /** Original file path for renamed files. */
    @JsonProperty("oldPath") String oldPath,
    /** Whether the diff content was omitted because it exceeded the per-file size limit. */
    @JsonProperty("isTruncated") Boolean isTruncated,
    /** Full text used for this patch, only when includeContents was requested for session mode. Omitted for binary, oversized or unavailable contents, and for fallback results. Read isFallback and isTruncated before treating an absent value as a missing file. */
    @JsonProperty("contents") WorkspaceDiffContents contents
) {

    /**
     * Creates a record with the components it had before later optional fields were added.
     *
     * @param path Path to the changed file, relative to the workspace root when the file lives under it. A file changed outside the workspace root keeps a `../`-relative path, or an absolute path when no relative path exists (for example a different Windows drive).
     * @param diff Unified diff content for the file. Empty when the diff was truncated.
     * @param changeType Type of change represented by this file diff.
     * @param oldPath Original file path for renamed files.
     * @param isTruncated Whether the diff content was omitted because it exceeded the per-file size limit.
     */
    public WorkspaceDiffFileChange(
        String path,
        String diff,
        WorkspaceDiffFileChangeType changeType,
        String oldPath,
        Boolean isTruncated
    ) {
        this(path, diff, changeType, oldPath, isTruncated, null);
    }
}
