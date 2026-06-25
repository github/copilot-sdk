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
 * Session event "tool.execution_start". Tool execution startup details including MCP server information when applicable
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
        /** Model identifier that generated this tool call */
        @JsonProperty("model") String model,
        /** Name of the MCP server hosting this tool, when the tool is an MCP tool */
        @JsonProperty("mcpServerName") String mcpServerName,
        /** Original tool name on the MCP server, when the tool is an MCP tool */
        @JsonProperty("mcpToolName") String mcpToolName,
        /** Identifier for the agent loop turn this tool was invoked in, matching the corresponding assistant.turn_start event */
        @JsonProperty("turnId") String turnId,
        /** When true, the tool output should be displayed expanded (verbatim) in the CLI timeline */
        @JsonProperty("displayVerbatim") Boolean displayVerbatim,
        /** Tool definition metadata, present for MCP tools with MCP Apps support */
        @JsonProperty("toolDescription") ToolExecutionStartToolDescription toolDescription,
        /** Tool call ID of the parent tool invocation when this event originates from a sub-agent */
        @JsonProperty("parentToolCallId") String parentToolCallId
    ) {
    }
}
