/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillsLoadedSkill(
    /** Unique identifier for the skill */
    @JsonProperty("name") String name,
    /** Description of what the skill does */
    @JsonProperty("description") String description,
    /** Source location type of the skill (e.g., project, personal, plugin) */
    @JsonProperty("source") String source,
    /** Whether the skill can be invoked by the user as a slash command */
    @JsonProperty("userInvocable") Boolean userInvocable,
    /** Whether the skill is currently enabled */
    @JsonProperty("enabled") Boolean enabled,
    /** Absolute path to the skill file, if available */
    @JsonProperty("path") String path
) {
}
