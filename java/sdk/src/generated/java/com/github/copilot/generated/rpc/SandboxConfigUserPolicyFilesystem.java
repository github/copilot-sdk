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
 * Filesystem rules to merge into the base policy.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SandboxConfigUserPolicyFilesystem(
    /** Paths granted read/write access. */
    @JsonProperty("readwritePaths") List<String> readwritePaths,
    /** Paths granted read-only access. */
    @JsonProperty("readonlyPaths") List<String> readonlyPaths,
    /** Paths explicitly denied. */
    @JsonProperty("deniedPaths") List<String> deniedPaths,
    /** Paths granted enumerate-only access: sandboxed commands can list these directories without reading the files in them. Honored only on Windows hosts with the `filesystem_enumeration` capability; ignored elsewhere. An entry at or under a read-only, read-write, or denied path is ignored, so it never narrows a grant or reopens a denial. */
    @JsonProperty("enumeratePaths") List<String> enumeratePaths
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SandboxConfigUserPolicyFilesystem {}

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
        private List<String> readwritePaths;
        private List<String> readonlyPaths;
        private List<String> deniedPaths;
        private List<String> enumeratePaths;

        /**
         * Sets the {@code readwritePaths} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder readwritePaths(List<String> value) {
            this.readwritePaths = value;
            return this;
        }

        /**
         * Sets the {@code readonlyPaths} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder readonlyPaths(List<String> value) {
            this.readonlyPaths = value;
            return this;
        }

        /**
         * Sets the {@code deniedPaths} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder deniedPaths(List<String> value) {
            this.deniedPaths = value;
            return this;
        }

        /**
         * Sets the {@code enumeratePaths} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enumeratePaths(List<String> value) {
            this.enumeratePaths = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SandboxConfigUserPolicyFilesystem build() {
            return new SandboxConfigUserPolicyFilesystem(readwritePaths, readonlyPaths, deniedPaths, enumeratePaths);
        }
    }

    /**
     * Creates filesystem rules without enumerate-only paths.
     * @deprecated Use named construction via {@link #builder()}.
     * @param readwritePaths Paths granted read/write access.
     * @param readonlyPaths Paths granted read-only access.
     * @param deniedPaths Paths explicitly denied.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SandboxConfigUserPolicyFilesystem(
        List<String> readwritePaths,
        List<String> readonlyPaths,
        List<String> deniedPaths
    ) {
        this(readwritePaths, readonlyPaths, deniedPaths, (List<String>) null);
    }

    /**
     * Retains the 1.0.28 constructor; clearPolicyOnExit is ignored, as it is by the runtime.
     * @deprecated Use named construction via {@link #builder()}.
     * @param readwritePaths Paths granted read/write access.
     * @param readonlyPaths Paths granted read-only access.
     * @param deniedPaths Paths explicitly denied.
     * @param clearPolicyOnExit clearPolicyOnExit
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SandboxConfigUserPolicyFilesystem(
        List<String> readwritePaths,
        List<String> readonlyPaths,
        List<String> deniedPaths,
        java.lang.Boolean clearPolicyOnExit
    ) {
        this(readwritePaths, readonlyPaths, deniedPaths, (List<String>) null);
    }
}
