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
 * Server-side skill metadata, including name, description, source, enabled/invocable state, path, project path, and argument hint.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServerSkill(
    /** Opaque identity of the plugin owning this skill. Matches plugins.list().plugins[].id on the same runtime host. Compare the complete value without parsing it. Omitted for non-plugin skills. */
    @JsonProperty("pluginId") String pluginId,
    /** Unique identifier for the skill */
    @JsonProperty("name") String name,
    /** Canonical slash command name used to invoke the skill, without the leading '/' */
    @JsonProperty("commandName") String commandName,
    /** Description of what the skill does */
    @JsonProperty("description") String description,
    /** Source location type (e.g., project, personal-copilot, plugin, builtin) */
    @JsonProperty("source") SkillSource source,
    /** Whether the skill can be invoked by the user as a slash command */
    @JsonProperty("userInvocable") Boolean userInvocable,
    /** Whether the skill is currently enabled (based on global config) */
    @JsonProperty("enabled") Boolean enabled,
    /** Absolute path to the skill file */
    @JsonProperty("path") String path,
    /** The project path this skill belongs to (only for project/inherited skills) */
    @JsonProperty("projectPath") String projectPath,
    /** Optional freeform hint describing the skill's expected arguments, from the `argument-hint` frontmatter field */
    @JsonProperty("argumentHint") String argumentHint
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ServerSkill {}

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
        private String name;
        private String commandName;
        private String description;
        private SkillSource source;
        private Boolean userInvocable;
        private Boolean enabled;
        private String path;
        private String projectPath;
        private String argumentHint;

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
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code commandName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder commandName(String value) {
            this.commandName = value;
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
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(SkillSource value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code userInvocable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder userInvocable(Boolean value) {
            this.userInvocable = value;
            return this;
        }

        /**
         * Sets the {@code enabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enabled(Boolean value) {
            this.enabled = value;
            return this;
        }

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
         * Sets the {@code argumentHint} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder argumentHint(String value) {
            this.argumentHint = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ServerSkill build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (description == null) throw new IllegalStateException("Missing required property: description");
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (userInvocable == null) throw new IllegalStateException("Missing required property: userInvocable");
            if (enabled == null) throw new IllegalStateException("Missing required property: enabled");
            return new ServerSkill(pluginId, name, commandName, description, source, userInvocable, enabled, path, projectPath, argumentHint);
        }
    }

    /**
     * Creates a record without plugin ownership or content-root metadata.
     * @deprecated Use named construction via {@link #builder()}.
     * @param name Unique identifier for the skill
     * @param commandName Canonical slash command name used to invoke the skill, without the leading '/'
     * @param description Description of what the skill does
     * @param source Source location type (e.g., project, personal-copilot, plugin, builtin)
     * @param userInvocable Whether the skill can be invoked by the user as a slash command
     * @param enabled Whether the skill is currently enabled (based on global config)
     * @param path Absolute path to the skill file
     * @param projectPath The project path this skill belongs to (only for project/inherited skills)
     * @param argumentHint Optional freeform hint describing the skill's expected arguments, from the `argument-hint` frontmatter field
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ServerSkill(
        String name,
        String commandName,
        String description,
        SkillSource source,
        Boolean userInvocable,
        Boolean enabled,
        String path,
        String projectPath,
        String argumentHint
    ) {
        this(null, name, commandName, description, source, userInvocable, enabled, path, projectPath, argumentHint);
    }
}
