/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Filesystem metadata for the requested path, or a filesystem error if the stat failed.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFsStatResult(
    /** Whether the path is a file */
    @JsonProperty("isFile") Boolean isFile,
    /** Whether the path is a directory */
    @JsonProperty("isDirectory") Boolean isDirectory,
    /** File size in bytes */
    @JsonProperty("size") Long size,
    /** ISO 8601 timestamp of last modification */
    @JsonProperty("mtime") OffsetDateTime mtime,
    /** ISO 8601 timestamp of creation */
    @JsonProperty("birthtime") OffsetDateTime birthtime,
    /** Describes a filesystem error. */
    @JsonProperty("error") SessionFsError error
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionFsStatResult {}

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
        private Boolean isFile;
        private Boolean isDirectory;
        private Long size;
        private OffsetDateTime mtime;
        private OffsetDateTime birthtime;
        private SessionFsError error;

        /**
         * Sets the {@code isFile} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isFile(Boolean value) {
            this.isFile = value;
            return this;
        }

        /**
         * Sets the {@code isDirectory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isDirectory(Boolean value) {
            this.isDirectory = value;
            return this;
        }

        /**
         * Sets the {@code size} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder size(Long value) {
            this.size = value;
            return this;
        }

        /**
         * Sets the {@code mtime} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mtime(OffsetDateTime value) {
            this.mtime = value;
            return this;
        }

        /**
         * Sets the {@code birthtime} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder birthtime(OffsetDateTime value) {
            this.birthtime = value;
            return this;
        }

        /**
         * Sets the {@code error} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder error(SessionFsError value) {
            this.error = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionFsStatResult build() {
            if (isFile == null) throw new IllegalStateException("Missing required property: isFile");
            if (isDirectory == null) throw new IllegalStateException("Missing required property: isDirectory");
            if (size == null) throw new IllegalStateException("Missing required property: size");
            if (mtime == null) throw new IllegalStateException("Missing required property: mtime");
            if (birthtime == null) throw new IllegalStateException("Missing required property: birthtime");
            return new SessionFsStatResult(isFile, isDirectory, size, mtime, birthtime, error);
        }
    }
}
