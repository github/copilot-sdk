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
 * Schema for the `ServerSkill` type.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServerSkill(
    /** Unique identifier for the skill */
    @JsonProperty("name") String name,
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
}
