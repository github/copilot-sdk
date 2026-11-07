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
 * One reviewed Skill file.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillInstallationFileReview(
    /** Relative file path within the Skill root. */
    @JsonProperty("path") String path,
    /** Exact reviewed file size in bytes. */
    @JsonProperty("sizeBytes") Long sizeBytes,
    /** Declared media type for the file. */
    @JsonProperty("mediaType") String mediaType,
    /** Whether the file is installed with executable permissions. */
    @JsonProperty("executable") Boolean executable,
    /** SHA-256 digest of the exact file bytes. */
    @JsonProperty("digest") String digest
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SkillInstallationFileReview {}

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
        private Long sizeBytes;
        private String mediaType;
        private Boolean executable;
        private String digest;

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
         * Sets the {@code sizeBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sizeBytes(Long value) {
            this.sizeBytes = value;
            return this;
        }

        /**
         * Sets the {@code mediaType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mediaType(String value) {
            this.mediaType = value;
            return this;
        }

        /**
         * Sets the {@code executable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder executable(Boolean value) {
            this.executable = value;
            return this;
        }

        /**
         * Sets the {@code digest} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder digest(String value) {
            this.digest = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SkillInstallationFileReview build() {
            if (path == null) throw new IllegalStateException("Missing required property: path");
            if (sizeBytes == null) throw new IllegalStateException("Missing required property: sizeBytes");
            if (mediaType == null) throw new IllegalStateException("Missing required property: mediaType");
            if (executable == null) throw new IllegalStateException("Missing required property: executable");
            if (digest == null) throw new IllegalStateException("Missing required property: digest");
            return new SkillInstallationFileReview(path, sizeBytes, mediaType, executable, digest);
        }
    }
}
