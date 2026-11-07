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
import javax.annotation.processing.Generated;

/**
 * Filesystem metadata for a path in the session workspace files directory.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionWorkspacesStatFileResult(
    /** Whether the path identifies a regular file */
    @JsonProperty("isFile") Boolean isFile,
    /** Whether the path identifies a directory */
    @JsonProperty("isDirectory") Boolean isDirectory,
    /** Size in bytes */
    @JsonProperty("size") Double size,
    /** Last modification time in Unix epoch milliseconds */
    @JsonProperty("mtimeMs") Double mtimeMs,
    /** Creation time in Unix epoch milliseconds */
    @JsonProperty("birthtimeMs") Double birthtimeMs
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionWorkspacesStatFileResult {}

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
        private Double size;
        private Double mtimeMs;
        private Double birthtimeMs;

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
        public Builder size(Double value) {
            this.size = value;
            return this;
        }

        /**
         * Sets the {@code mtimeMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mtimeMs(Double value) {
            this.mtimeMs = value;
            return this;
        }

        /**
         * Sets the {@code birthtimeMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder birthtimeMs(Double value) {
            this.birthtimeMs = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionWorkspacesStatFileResult build() {
            if (isFile == null) throw new IllegalStateException("Missing required property: isFile");
            if (isDirectory == null) throw new IllegalStateException("Missing required property: isDirectory");
            if (size == null) throw new IllegalStateException("Missing required property: size");
            if (mtimeMs == null) throw new IllegalStateException("Missing required property: mtimeMs");
            if (birthtimeMs == null) throw new IllegalStateException("Missing required property: birthtimeMs");
            return new SessionWorkspacesStatFileResult(isFile, isDirectory, size, mtimeMs, birthtimeMs);
        }
    }
}
