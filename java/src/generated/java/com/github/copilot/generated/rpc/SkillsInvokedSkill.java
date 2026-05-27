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
 * Schema for the `SkillsInvokedSkill` type.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillsInvokedSkill(
    /** Unique identifier for the skill */
    @JsonProperty("name") String name,
    /** Path to the SKILL.md file */
    @JsonProperty("path") String path,
    /** Full content of the skill file */
    @JsonProperty("content") String content,
    /** Tools that should be auto-approved when this skill is active, captured at invocation time */
    @JsonProperty("allowedTools") List<String> allowedTools,
    /** Turn number when the skill was invoked */
    @JsonProperty("invokedAtTurn") Long invokedAtTurn
) {
}
