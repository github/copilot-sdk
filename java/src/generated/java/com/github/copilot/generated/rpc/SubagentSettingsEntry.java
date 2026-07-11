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
 * Subagent model, reasoning effort, and context tier settings
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubagentSettingsEntry(
    /** Model override for matching subagents */
    @JsonProperty("model") String model,
    /** Reasoning effort override for matching subagents */
    @JsonProperty("effortLevel") String effortLevel,
    /** Context tier override for matching subagents */
    @JsonProperty("contextTier") SubagentSettingsEntryContextTier contextTier
) {
}
