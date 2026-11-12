/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Configured per-agent subagent overrides
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubagentSettingsConfig(
    /** Per-agent settings keyed by subagent agent_type */
    @JsonProperty("agents") Map<String, SubagentSettingsEntry> agents,
    /** Configured opt-in for subagent context management tools; applied from persisted settings when the session starts */
    @JsonProperty("contextManagementTools") Boolean contextManagementTools,
    /** Names of subagents the user has turned off; they cannot be dispatched */
    @JsonProperty("disabledSubagents") List<String> disabledSubagents,
    /** Maximum number of subagents that can run concurrently; applies to usage-based billing users only */
    @JsonProperty("maxConcurrency") Long maxConcurrency,
    /** Maximum subagent nesting depth; applies to usage-based billing users only */
    @JsonProperty("maxDepth") Long maxDepth
) {
}
