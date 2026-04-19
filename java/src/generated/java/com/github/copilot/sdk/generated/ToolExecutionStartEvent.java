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
 * The {@code tool.execution_start} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ToolExecutionStartEvent extends SessionEvent {

    @Override
    public String getType() { return "tool.execution_start"; }

    @JsonProperty("data")
    private ToolExecutionStartEventData data;

    public ToolExecutionStartEventData getData() { return data; }
    public void setData(ToolExecutionStartEventData data) { this.data = data; }

    /** Data payload for {@link ToolExecutionStartEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToolExecutionStartEventData(
        /** Unique identifier for this tool call */
        @JsonProperty("toolCallId") String toolCallId,
        /** Name of the tool being executed */
        @JsonProperty("toolName") String toolName,
        /** Arguments passed to the tool */
        @JsonProperty("arguments") Object arguments,
        /** Name of the MCP server hosting this tool, when the tool is an MCP tool */
        @JsonProperty("mcpServerName") String mcpServerName,
        /** Original tool name on the MCP server, when the tool is an MCP tool */
        @JsonProperty("mcpToolName") String mcpToolName,
        /** Tool call ID of the parent tool invocation when this event originates from a sub-agent */
        @JsonProperty("parentToolCallId") String parentToolCallId
    ) {
    }
}
