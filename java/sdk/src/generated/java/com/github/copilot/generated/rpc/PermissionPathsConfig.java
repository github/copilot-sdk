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
 * If specified, replaces the session's path-permission policy. The runtime constructs the appropriate PathManager based on these inputs (rooted at the session's working directory). Omit to leave the current path policy unchanged.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PermissionPathsConfig(
    /** If true, the runtime allows access to all paths without prompting. Equivalent to constructing an UnrestrictedPathManager. */
    @JsonProperty("unrestricted") Boolean unrestricted,
    /** Additional directories to allow tool access to (in addition to the session's working directory). Conventional `.github/skills/` and `.github/agents/` definitions under them also join the session catalogs when their subsystem gates are enabled, so supplying a directory is a trust decision for configuration stored there. When `unrestricted` is true, these are still pre-populated on the UnrestrictedPathManager so they remain visible via getDirectories() (e.g. for @-mention completion). */
    @JsonProperty("additionalDirectories") List<String> additionalDirectories,
    /** Whether to include the system temp directory in the allowed list (defaults to true). Ignored when `unrestricted` is true. */
    @JsonProperty("includeTempDirectory") Boolean includeTempDirectory,
    /** Workspace root path (special-cased to be allowed even before the directory exists). Ignored when `unrestricted` is true. */
    @JsonProperty("workspacePath") String workspacePath
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public PermissionPathsConfig {}

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
        private Boolean unrestricted;
        private List<String> additionalDirectories;
        private Boolean includeTempDirectory;
        private String workspacePath;

        /**
         * Sets the {@code unrestricted} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder unrestricted(Boolean value) {
            this.unrestricted = value;
            return this;
        }

        /**
         * Sets the {@code additionalDirectories} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder additionalDirectories(List<String> value) {
            this.additionalDirectories = value;
            return this;
        }

        /**
         * Sets the {@code includeTempDirectory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder includeTempDirectory(Boolean value) {
            this.includeTempDirectory = value;
            return this;
        }

        /**
         * Sets the {@code workspacePath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder workspacePath(String value) {
            this.workspacePath = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public PermissionPathsConfig build() {
            return new PermissionPathsConfig(unrestricted, additionalDirectories, includeTempDirectory, workspacePath);
        }
    }
}
