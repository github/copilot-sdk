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
 * Schema for the `SlashCommandInfo` type.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SlashCommandInfo(
    /** Canonical command name without a leading slash */
    @JsonProperty("name") String name,
    /** Canonical aliases without leading slashes */
    @JsonProperty("aliases") List<String> aliases,
    /** Human-readable command description */
    @JsonProperty("description") String description,
    /** Coarse command category for grouping and behavior: runtime built-in, skill-backed command, or SDK/client-owned command */
    @JsonProperty("kind") SlashCommandKind kind,
    /** Optional unstructured input hint */
    @JsonProperty("input") SlashCommandInput input,
    /** Whether the command may run while an agent turn is active */
    @JsonProperty("allowDuringAgentExecution") Boolean allowDuringAgentExecution,
    /** Whether the command is experimental */
    @JsonProperty("experimental") Boolean experimental
) {
}
