/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The {@code subagent.selected} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SubagentSelectedEvent extends SessionEvent {

    @Override
    public String getType() { return "subagent.selected"; }

    @JsonProperty("data")
    private SubagentSelectedEventData data;

    public SubagentSelectedEventData getData() { return data; }
    public void setData(SubagentSelectedEventData data) { this.data = data; }

    /** Data payload for {@link SubagentSelectedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SubagentSelectedEventData(
        /** Internal name of the selected custom agent */
        @JsonProperty("agentName") String agentName,
        /** Human-readable display name of the selected custom agent */
        @JsonProperty("agentDisplayName") String agentDisplayName,
        /** List of tool names available to this agent, or null for all tools */
        @JsonProperty("tools") List<String> tools
    ) {
    }
}
