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
 * Subagent settings to apply to the current session
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionToolsUpdateSubagentSettingsParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Subagent settings to apply, or null to clear the live session override */
    @JsonProperty("subagents") SessionToolsUpdateSubagentSettingsParamsSubagents subagents
) {

    /** Configured per-agent subagent overrides */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionToolsUpdateSubagentSettingsParamsSubagents(
        /** Per-agent settings keyed by subagent agent_type */
        @JsonProperty("agents") Map<String, SubagentSettingsEntry> agents,
        /** Names of subagents the user has turned off; they cannot be dispatched */
        @JsonProperty("disabledSubagents") List<String> disabledSubagents
    ) {
    }
}
