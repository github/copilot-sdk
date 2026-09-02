/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "subagent.started". Sub-agent startup details including parent tool call and agent information
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
        @JsonProperty("agentDescription") String agentDescription,
        /** Model the sub-agent will run with, when known at start. */
        @JsonProperty("model") String model,
        /** Root id of the factory run that spawned this sub-agent, when it was spawned by one. */
        @JsonProperty("factoryRunId") String factoryRunId,
        /** Task-registry ID of the spawning sub-agent. Absent when the root session spawned this child. */
        @JsonProperty("parentId") String parentId,
        /** Whether this sub-agent can be resumed. Currently always false. */
        @JsonProperty("resumable") Boolean resumable,
        /** Type of the sub-agent selected at spawn time. */
        @JsonProperty("agentType") String agentType,
        /** Whether the sub-agent runs synchronously or in the background. */
        @JsonProperty("executionMode") String executionMode
    ) {
    }
}
