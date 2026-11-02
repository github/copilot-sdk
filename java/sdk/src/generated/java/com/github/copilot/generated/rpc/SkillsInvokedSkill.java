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
 * Skill invocation record with name, path, content, allowed tools, and turn number.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillsInvokedSkill(
    /** Unique identifier for the skill */
    @JsonProperty("name") String name,
    /** Path to the SKILL.md file, or an empty string for an SDK-provided skill without a filesystem identity */
    @JsonProperty("path") String path,
    /** Full content of the skill file */
    @JsonProperty("content") String content,
    /** Tools that should be auto-approved when this skill is active, captured at invocation time */
    @JsonProperty("allowedTools") List<String> allowedTools,
    /** Whether model invocation was disabled when this skill was invoked */
    @JsonProperty("disableModelInvocation") Boolean disableModelInvocation,
    /** Turn number when the skill was invoked */
    @JsonProperty("invokedAtTurn") Long invokedAtTurn
) {
}
