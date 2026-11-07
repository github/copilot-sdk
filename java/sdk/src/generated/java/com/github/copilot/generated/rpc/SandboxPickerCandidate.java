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
 * One file-picker suggestion offered up for the sandbox filesystem check.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SandboxPickerCandidate(
    /** Absolute path the suggestion would attach. */
    @JsonProperty("path") String path,
    /** Whether the suggestion is a directory. A directory the policy does not itself allow may still be needed as a waypoint to an allowed subtree. */
    @JsonProperty("isDirectory") Boolean isDirectory
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SandboxPickerCandidate {}

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
        private Boolean isDirectory;

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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SandboxPickerCandidate build() {
            if (path == null) throw new IllegalStateException("Missing required property: path");
            if (isDirectory == null) throw new IllegalStateException("Missing required property: isDirectory");
            return new SandboxPickerCandidate(path, isDirectory);
        }
    }
}
