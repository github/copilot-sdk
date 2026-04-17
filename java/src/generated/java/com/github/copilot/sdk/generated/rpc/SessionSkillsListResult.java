/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Result for the {@code session.skills.list} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionSkillsListResult(
    /** Available skills */
    @JsonProperty("skills") List<SessionSkillsListResultSkillsItem> skills
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionSkillsListResultSkillsItem(
        /** Unique identifier for the skill */
        @JsonProperty("name") String name,
        /** Description of what the skill does */
        @JsonProperty("description") String description,
        /** Source location type (e.g., project, personal, plugin) */
        @JsonProperty("source") String source,
        /** Whether the skill can be invoked by the user as a slash command */
        @JsonProperty("userInvocable") Boolean userInvocable,
        /** Whether the skill is currently enabled */
        @JsonProperty("enabled") Boolean enabled,
        /** Absolute path to the skill file */
        @JsonProperty("path") String path
    ) {
    }
}
