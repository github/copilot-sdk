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
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public WorkspaceDiffFileChange {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private String path;
        private String diff;
        private WorkspaceDiffFileChangeType changeType;
        private String oldPath;
        private Boolean isTruncated;
        private WorkspaceDiffContents contents;

        /**
         * Sets the {@code path} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder path(String value) {
            this.path = value;
            return this;
        }

        /**
         * Sets the {@code diff} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder diff(String value) {
            this.diff = value;
            return this;
        }

        /**
         * Sets the {@code changeType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder changeType(WorkspaceDiffFileChangeType value) {
            this.changeType = value;
            return this;
        }

        /**
         * Sets the {@code oldPath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder oldPath(String value) {
            this.oldPath = value;
            return this;
        }

        /**
         * Sets the {@code isTruncated} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isTruncated(Boolean value) {
            this.isTruncated = value;
            return this;
        }

        /**
         * Sets the {@code contents} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder contents(WorkspaceDiffContents value) {
            this.contents = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public WorkspaceDiffFileChange build() {
            if (path == null) throw new IllegalStateException("Missing required property: path");
            if (diff == null) throw new IllegalStateException("Missing required property: diff");
            if (changeType == null) throw new IllegalStateException("Missing required property: changeType");
            return new WorkspaceDiffFileChange(path, diff, changeType, oldPath, isTruncated, contents);
        }
    }

    /**
     * Creates a record with the components it had before later optional fields were added.
     * @deprecated Use named construction via {@link #builder()}.
     * @param path Path to the changed file, relative to the workspace root when the file lives under it. A file changed outside the workspace root keeps a `../`-relative path, or an absolute path when no relative path exists (for example a different Windows drive).
     * @param diff Unified diff content for the file. Empty when the diff was truncated.
     * @param changeType Type of change represented by this file diff.
     * @param oldPath Original file path for renamed files.
     * @param isTruncated Whether the diff content was omitted because it exceeded the per-file size limit.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
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
