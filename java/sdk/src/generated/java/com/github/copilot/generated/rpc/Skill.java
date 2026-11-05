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
 * Skill metadata available to a session, with name, description, source, enabled/invocable state, path, plugin, and argument hint.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Skill(
    /** Opaque identity of the plugin owning this skill. Matches plugins.list().plugins[].id on the same runtime host. Compare the complete value without parsing it. Omitted for non-plugin skills. */
    @JsonProperty("pluginId") String pluginId,
    /** Unique identifier for the skill */
    @JsonProperty("name") String name,
    /** Original frontmatter name for an MCP skill, for display only. Use name for identity and enable/disable operations. */
    @JsonProperty("declaredName") String declaredName,
    /** Canonical slash command name used to invoke the skill, without the leading '/' */
    @JsonProperty("commandName") String commandName,
    /** Description of what the skill does */
    @JsonProperty("description") String description,
    /** Source location type (e.g., project, personal-copilot, plugin, builtin) */
    @JsonProperty("source") SkillSource source,
    /** MCP server and skill resource address when source is 'mcp'. Contains no activation grants or internal verification metadata. */
    @JsonProperty("mcpOrigin") McpSkillOrigin mcpOrigin,
    /** Whether the skill can be invoked by the user as a slash command */
    @JsonProperty("userInvocable") Boolean userInvocable,
    /** Whether the skill is currently enabled */
    @JsonProperty("enabled") Boolean enabled,
    /** Absolute path to the skill file */
    @JsonProperty("path") String path,
    /** Name of the plugin that provides the skill, when source is 'plugin' */
    @JsonProperty("pluginName") String pluginName,
    /** Optional freeform hint describing the skill's expected arguments, from the `argument-hint` frontmatter field */
    @JsonProperty("argumentHint") String argumentHint
) {

    /**
     * Creates a record without plugin ownership or content-root metadata.
     *
     * @param name Unique identifier for the skill
     * @param declaredName Original frontmatter name for an MCP skill, for display only. Use name for identity and enable/disable operations.
     * @param commandName Canonical slash command name used to invoke the skill, without the leading '/'
     * @param description Description of what the skill does
     * @param source Source location type (e.g., project, personal-copilot, plugin, builtin)
     * @param mcpOrigin MCP server and skill resource address when source is 'mcp'. Contains no activation grants or internal verification metadata.
     * @param userInvocable Whether the skill can be invoked by the user as a slash command
     * @param enabled Whether the skill is currently enabled
     * @param path Absolute path to the skill file
     * @param pluginName Name of the plugin that provides the skill, when source is 'plugin'
     * @param argumentHint Optional freeform hint describing the skill's expected arguments, from the `argument-hint` frontmatter field
     */
    public Skill(
        String name,
        String declaredName,
        String commandName,
        String description,
        SkillSource source,
        McpSkillOrigin mcpOrigin,
        Boolean userInvocable,
        Boolean enabled,
        String path,
        String pluginName,
        String argumentHint
    ) {
        this(null, name, declaredName, commandName, description, source, mcpOrigin, userInvocable, enabled, path, pluginName, argumentHint);
    }
}
