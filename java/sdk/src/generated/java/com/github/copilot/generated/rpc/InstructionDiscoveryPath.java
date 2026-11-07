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
 * Canonical file or directory where custom instructions can be discovered or created, with location, kind, preference, and project path.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record InstructionDiscoveryPath(
    /** Absolute path of the file or directory (may not exist on disk yet) */
    @JsonProperty("path") String path,
    /** Which tier this target belongs to */
    @JsonProperty("location") InstructionDiscoveryPathLocation location,
    /** Whether the target is a single file or a directory of instruction files */
    @JsonProperty("kind") InstructionDiscoveryPathKind kind,
    /** Whether this is the canonical target to create new instructions in its tier. At most one entry per tier is preferred. */
    @JsonProperty("preferredForCreation") Boolean preferredForCreation,
    /** The input project path this target was derived from (only for repository targets) */
    @JsonProperty("projectPath") String projectPath
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public InstructionDiscoveryPath {}

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
        private InstructionDiscoveryPathLocation location;
        private InstructionDiscoveryPathKind kind;
        private Boolean preferredForCreation;
        private String projectPath;

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
         * Sets the {@code location} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder location(InstructionDiscoveryPathLocation value) {
            this.location = value;
            return this;
        }

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder kind(InstructionDiscoveryPathKind value) {
            this.kind = value;
            return this;
        }

        /**
         * Sets the {@code preferredForCreation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder preferredForCreation(Boolean value) {
            this.preferredForCreation = value;
            return this;
        }

        /**
         * Sets the {@code projectPath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder projectPath(String value) {
            this.projectPath = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public InstructionDiscoveryPath build() {
            if (path == null) throw new IllegalStateException("Missing required property: path");
            if (location == null) throw new IllegalStateException("Missing required property: location");
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            if (preferredForCreation == null) throw new IllegalStateException("Missing required property: preferredForCreation");
            return new InstructionDiscoveryPath(path, location, kind, preferredForCreation, projectPath);
        }
    }
}
