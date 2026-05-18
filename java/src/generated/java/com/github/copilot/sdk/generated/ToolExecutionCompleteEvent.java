/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Session event "tool.execution_complete". Tool execution completion results including success status, detailed output, and error information
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ToolExecutionCompleteEvent extends SessionEvent {

    @Override
    public String getType() { return "tool.execution_complete"; }

    @JsonProperty("data")
    private ToolExecutionCompleteEventData data;

    public ToolExecutionCompleteEventData getData() { return data; }
    public void setData(ToolExecutionCompleteEventData data) { this.data = data; }

    /** Data payload for {@link ToolExecutionCompleteEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToolExecutionCompleteEventData(
        /** Unique identifier for the completed tool call */
        @JsonProperty("toolCallId") String toolCallId,
        /** Whether the tool execution completed successfully */
        @JsonProperty("success") Boolean success,
        /** Model identifier that generated this tool call */
        @JsonProperty("model") String model,
        /** CAPI interaction ID for correlating this tool execution with upstream telemetry */
        @JsonProperty("interactionId") String interactionId,
        /** Whether this tool call was explicitly requested by the user rather than the assistant */
        @JsonProperty("isUserRequested") Boolean isUserRequested,
        /** Tool execution result on success */
        @JsonProperty("result") ToolExecutionCompleteResult result,
        /** Error details when the tool execution failed */
        @JsonProperty("error") ToolExecutionCompleteError error,
        /** Tool-specific telemetry data (e.g., CodeQL check counts, grep match counts) */
        @JsonProperty("toolTelemetry") Map<String, Object> toolTelemetry,
        /** Identifier for the agent loop turn this tool was invoked in, matching the corresponding assistant.turn_start event */
        @JsonProperty("turnId") String turnId,
        /** Tool call ID of the parent tool invocation when this event originates from a sub-agent */
        @JsonProperty("parentToolCallId") String parentToolCallId
    ) {
    }
}
