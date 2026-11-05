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
     * Creates a record without plugin ownership or content-root metadata.
     *
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
