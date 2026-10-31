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
 * A tool invocation request from the assistant
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AssistantMessageToolRequest(
    /** Unique identifier for this tool call */
    @JsonProperty("toolCallId") String toolCallId,
    /** Name of the tool being invoked */
    @JsonProperty("name") String name,
    /** Arguments to pass to the tool, format depends on the tool */
    @JsonProperty("arguments") Object arguments,
    /** Tool call type: "function" for standard tool calls, "custom" for grammar-based tool calls. Defaults to "function" when absent. */
    @JsonProperty("type") AssistantMessageToolRequestType type,
    /** Human-readable display title for the tool */
    @JsonProperty("toolTitle") String toolTitle,
    /** Configured key of the MCP server hosting this tool. Legacy events may contain the display name instead. */
    @JsonProperty("mcpServerName") String mcpServerName,
    /** Original tool name on the MCP server, when the tool is an MCP tool */
    @JsonProperty("mcpToolName") String mcpToolName,
    /** Resolved intention summary describing what this specific call does */
    @JsonProperty("intentionSummary") String intentionSummary,
    /** Hosted program that requested this client tool call */
    @JsonProperty("caller") AssistantMessageToolRequestCaller caller,
    /** Explicitly configured MCP server display name. */
    @JsonProperty("mcpServerDisplayName") String mcpServerDisplayName
) {

    /**
     * Creates a record with the components it had before later optional fields were added.
     *
     * @param toolCallId Unique identifier for this tool call
     * @param name Name of the tool being invoked
     * @param arguments Arguments to pass to the tool, format depends on the tool
     * @param type Tool call type: "function" for standard tool calls, "custom" for grammar-based tool calls. Defaults to "function" when absent.
     * @param toolTitle Human-readable display title for the tool
     * @param mcpServerName Configured key of the MCP server hosting this tool. Legacy events may contain the display name instead.
     * @param mcpToolName Original tool name on the MCP server, when the tool is an MCP tool
     * @param intentionSummary Resolved intention summary describing what this specific call does
     * @param caller Hosted program that requested this client tool call
     */
    public AssistantMessageToolRequest(
        String toolCallId,
        String name,
        Object arguments,
        AssistantMessageToolRequestType type,
        String toolTitle,
        String mcpServerName,
        String mcpToolName,
        String intentionSummary,
        AssistantMessageToolRequestCaller caller
    ) {
        this(toolCallId, name, arguments, type, toolTitle, mcpServerName, mcpToolName, intentionSummary, caller, null);
    }
}
