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
    /** Name of the MCP server hosting this tool, when the tool is an MCP tool */
    @JsonProperty("mcpServerName") String mcpServerName,
    /** Resolved intention summary describing what this specific call does */
    @JsonProperty("intentionSummary") String intentionSummary
) {
}
