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

/**
 * The {@code subagent.started} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SubagentStartedEvent extends SessionEvent {

    @Override
    public String getType() { return "subagent.started"; }

    @JsonProperty("data")
    private SubagentStartedEventData data;

    public SubagentStartedEventData getData() { return data; }
    public void setData(SubagentStartedEventData data) { this.data = data; }

    /** Data payload for {@link SubagentStartedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SubagentStartedEventData(
        /** Tool call ID of the parent tool invocation that spawned this sub-agent */
        @JsonProperty("toolCallId") String toolCallId,
        /** Internal name of the sub-agent */
        @JsonProperty("agentName") String agentName,
        /** Human-readable display name of the sub-agent */
        @JsonProperty("agentDisplayName") String agentDisplayName,
        /** Description of what the sub-agent does */
        @JsonProperty("agentDescription") String agentDescription
    ) {
    }
}
