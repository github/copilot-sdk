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
 * The {@code subagent.completed} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SubagentCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "subagent.completed"; }

    @JsonProperty("data")
    private SubagentCompletedEventData data;

    public SubagentCompletedEventData getData() { return data; }
    public void setData(SubagentCompletedEventData data) { this.data = data; }

    /** Data payload for {@link SubagentCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SubagentCompletedEventData(
        /** Tool call ID of the parent tool invocation that spawned this sub-agent */
        @JsonProperty("toolCallId") String toolCallId,
        /** Internal name of the sub-agent */
        @JsonProperty("agentName") String agentName,
        /** Human-readable display name of the sub-agent */
        @JsonProperty("agentDisplayName") String agentDisplayName,
        /** Model used by the sub-agent */
        @JsonProperty("model") String model,
        /** Total number of tool calls made by the sub-agent */
        @JsonProperty("totalToolCalls") Double totalToolCalls,
        /** Total tokens (input + output) consumed by the sub-agent */
        @JsonProperty("totalTokens") Double totalTokens,
        /** Wall-clock duration of the sub-agent execution in milliseconds */
        @JsonProperty("durationMs") Double durationMs
    ) {
    }
}
