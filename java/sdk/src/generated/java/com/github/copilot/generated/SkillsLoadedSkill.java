/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * A single resolved skill in `session.skills_loaded`, including source, invocability, enabled state, path, and argument hint.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillsLoadedSkill(
    /** Unique identifier for the skill */
    @JsonProperty("name") String name,
    /** Original frontmatter name for an MCP skill, for display only. Use name for identity and enable/disable operations. */
    @JsonProperty("declaredName") String declaredName,
    /** Canonical slash command name used to invoke the skill, without the leading '/' */
    @JsonProperty("commandName") String commandName,
    /** Description of what the skill does */
    @JsonProperty("description") String description,
    /** Source location type (e.g., project, personal-copilot, plugin, builtin, remote, sdk) */
    @JsonProperty("source") SkillSource source,
    /** MCP server and skill resource address when source is 'mcp'. Contains no activation grants or internal verification metadata. */
    @JsonProperty("mcpOrigin") McpSkillOrigin mcpOrigin,
    /** Whether the skill can be invoked by the user as a slash command */
    @JsonProperty("userInvocable") Boolean userInvocable,
    /** Whether the skill is currently enabled */
    @JsonProperty("enabled") Boolean enabled,
    /** Absolute path to the skill file, if available */
    @JsonProperty("path") String path,
    /** Optional freeform hint describing the skill's expected arguments, from the `argument-hint` frontmatter field */
    @JsonProperty("argumentHint") String argumentHint
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SkillsLoadedSkill {}

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
        private String name;
        private String declaredName;
        private String commandName;
        private String description;
        private SkillSource source;
        private McpSkillOrigin mcpOrigin;
        private Boolean userInvocable;
        private Boolean enabled;
        private String path;
        private String argumentHint;

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
         * Sets the {@code declaredName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder declaredName(String value) {
            this.declaredName = value;
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
         * Sets the {@code mcpOrigin} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mcpOrigin(McpSkillOrigin value) {
            this.mcpOrigin = value;
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
        public SkillsLoadedSkill build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (description == null) throw new IllegalStateException("Missing required property: description");
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (userInvocable == null) throw new IllegalStateException("Missing required property: userInvocable");
            if (enabled == null) throw new IllegalStateException("Missing required property: enabled");
            return new SkillsLoadedSkill(name, declaredName, commandName, description, source, mcpOrigin, userInvocable, enabled, path, argumentHint);
        }
    }

    /**
     * Retains the constructor published in Java SDK 1.0.28.
     * @deprecated Use named construction via {@link #builder()}.
     * @param name Unique identifier for the skill
     * @param commandName Canonical slash command name used to invoke the skill, without the leading '/'
     * @param description Description of what the skill does
     * @param source Source location type (e.g., project, personal-copilot, plugin, builtin, remote, sdk)
     * @param userInvocable Whether the skill can be invoked by the user as a slash command
     * @param enabled Whether the skill is currently enabled
     * @param path Absolute path to the skill file, if available
     * @param argumentHint Optional freeform hint describing the skill's expected arguments, from the `argument-hint` frontmatter field
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SkillsLoadedSkill(
        String name,
        String commandName,
        String description,
        SkillSource source,
        Boolean userInvocable,
        Boolean enabled,
        String path,
        String argumentHint
    ) {
        this(name, null, commandName, description, source, null, userInvocable, enabled, path, argumentHint);
    }
}
