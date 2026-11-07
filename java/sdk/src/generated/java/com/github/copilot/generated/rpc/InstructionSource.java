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
 * Loaded instruction source for a session, including path, content, category, location, applicability, and optional description.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record InstructionSource(
    /** Opaque identity of the plugin owning this instruction source. Matches plugins.list().plugins[].id on the same runtime host. Compare the complete value without parsing it. Omitted for non-plugin sources. */
    @JsonProperty("pluginId") String pluginId,
    /** Unique identifier for this source (used for toggling) */
    @JsonProperty("id") String id,
    /** Human-readable label */
    @JsonProperty("label") String label,
    /** File path relative to repo or absolute for home */
    @JsonProperty("sourcePath") String sourcePath,
    /** Raw content of the instruction file */
    @JsonProperty("content") String content,
    /** Category of instruction source — used for merge logic */
    @JsonProperty("type") InstructionSourceType type,
    /** Where this source lives — used for UI grouping */
    @JsonProperty("location") InstructionSourceLocation location,
    /** Glob pattern(s) from frontmatter — when set, this instruction applies only to matching files */
    @JsonProperty("applyTo") List<String> applyTo,
    /** Short description (body after frontmatter) for use in instruction tables */
    @JsonProperty("description") String description,
    /** When true, this source starts disabled and must be toggled on by the user */
    @JsonProperty("defaultDisabled") Boolean defaultDisabled,
    /** The project path this source was discovered from. Only set by sessionless discovery for repository, working-directory, and project-scoped plugin sources, where it disambiguates sources across multiple workspace roots. The session-scoped getSources leaves it unset. */
    @JsonProperty("projectPath") String projectPath
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public InstructionSource {}

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
        private String pluginId;
        private String id;
        private String label;
        private String sourcePath;
        private String content;
        private InstructionSourceType type;
        private InstructionSourceLocation location;
        private List<String> applyTo;
        private String description;
        private Boolean defaultDisabled;
        private String projectPath;

        /**
         * Sets the {@code pluginId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder pluginId(String value) {
            this.pluginId = value;
            return this;
        }

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder id(String value) {
            this.id = value;
            return this;
        }

        /**
         * Sets the {@code label} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder label(String value) {
            this.label = value;
            return this;
        }

        /**
         * Sets the {@code sourcePath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sourcePath(String value) {
            this.sourcePath = value;
            return this;
        }

        /**
         * Sets the {@code content} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder content(String value) {
            this.content = value;
            return this;
        }

        /**
         * Sets the {@code type} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder type(InstructionSourceType value) {
            this.type = value;
            return this;
        }

        /**
         * Sets the {@code location} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder location(InstructionSourceLocation value) {
            this.location = value;
            return this;
        }

        /**
         * Sets the {@code applyTo} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder applyTo(List<String> value) {
            this.applyTo = value;
            return this;
        }

        /**
         * Sets the {@code description} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder description(String value) {
            this.description = value;
            return this;
        }

        /**
         * Sets the {@code defaultDisabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder defaultDisabled(Boolean value) {
            this.defaultDisabled = value;
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
        public InstructionSource build() {
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (label == null) throw new IllegalStateException("Missing required property: label");
            if (sourcePath == null) throw new IllegalStateException("Missing required property: sourcePath");
            if (content == null) throw new IllegalStateException("Missing required property: content");
            if (type == null) throw new IllegalStateException("Missing required property: type");
            if (location == null) throw new IllegalStateException("Missing required property: location");
            return new InstructionSource(pluginId, id, label, sourcePath, content, type, location, applyTo, description, defaultDisabled, projectPath);
        }
    }

    /**
     * Creates a record without plugin ownership or content-root metadata.
     * @deprecated Use named construction via {@link #builder()}.
     * @param id Unique identifier for this source (used for toggling)
     * @param label Human-readable label
     * @param sourcePath File path relative to repo or absolute for home
     * @param content Raw content of the instruction file
     * @param type Category of instruction source — used for merge logic
     * @param location Where this source lives — used for UI grouping
     * @param applyTo Glob pattern(s) from frontmatter — when set, this instruction applies only to matching files
     * @param description Short description (body after frontmatter) for use in instruction tables
     * @param defaultDisabled When true, this source starts disabled and must be toggled on by the user
     * @param projectPath The project path this source was discovered from. Only set by sessionless discovery for repository, working-directory, and project-scoped plugin sources, where it disambiguates sources across multiple workspace roots. The session-scoped getSources leaves it unset.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public InstructionSource(
        String id,
        String label,
        String sourcePath,
        String content,
        InstructionSourceType type,
        InstructionSourceLocation location,
        List<String> applyTo,
        String description,
        Boolean defaultDisabled,
        String projectPath
    ) {
        this(null, id, label, sourcePath, content, type, location, applyTo, description, defaultDisabled, projectPath);
    }
}
