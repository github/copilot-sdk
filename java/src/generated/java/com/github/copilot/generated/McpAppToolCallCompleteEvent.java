/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Session event "mcp_app.tool_call_complete". MCP App view called a tool on a connected MCP server (SEP-1865)
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpAppToolCallCompleteEvent extends SessionEvent {

    @Override
    public String getType() { return "mcp_app.tool_call_complete"; }

    @JsonProperty("data")
    private McpAppToolCallCompleteEventData data;

    public McpAppToolCallCompleteEventData getData() { return data; }
    public void setData(McpAppToolCallCompleteEventData data) { this.data = data; }

    /** Data payload for {@link McpAppToolCallCompleteEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record McpAppToolCallCompleteEventData(
        /** Name of the MCP server hosting the tool */
        @JsonProperty("serverName") String serverName,
        /** MCP tool name that was invoked */
        @JsonProperty("toolName") String toolName,
        /** Arguments passed to the tool by the app view, if any */
        @JsonProperty("arguments") Map<String, Object> arguments,
        /** True when the call completed without throwing AND the MCP CallToolResult did not set isError */
        @JsonProperty("success") Boolean success,
        /** Wall-clock duration of the underlying tools/call in milliseconds */
        @JsonProperty("durationMs") Double durationMs,
        /** Standard MCP CallToolResult returned by the server. Present whether or not the call set isError. */
        @JsonProperty("result") Map<String, Object> result,
        /** Set when the underlying tools/call threw an error before returning a CallToolResult */
        @JsonProperty("error") McpAppToolCallCompleteError error,
        /** The tool's `_meta.ui` block at the time of the call, so consumers can decide whether to forward the result to the model without re-listing tools. */
        @JsonProperty("toolMeta") McpAppToolCallCompleteToolMeta toolMeta
    ) {
    }
}
