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
        /** W3C traceparent of this tool's active runtime execute_tool span. Available on live events when tool-context propagation is enabled; absent when the span is unavailable or on persisted history. This diagnostic context does not authorize execution. */
        @JsonProperty("traceparent") String traceparent,
        /** Optional W3C tracestate associated with traceparent. Omitted when no valid vendor state is available. */
        @JsonProperty("tracestate") String tracestate,
        /** Human-readable display title for the tool, when the selected tool descriptor has a non-empty title. */
        @JsonProperty("toolTitle") String toolTitle,
        /** Arguments passed to the tool */
        @JsonProperty("arguments") Object arguments,
        /** Shell-tool path hints derived from the command at start time for shell tools (bash/powershell/local_shell). Produced by the same shell-aware extractor as PermissionRequestShell.possiblePaths, so it is present even when the command is auto-approved and no permission request fires. Absent for non-shell tools. */
        @JsonProperty("shellToolInfo") ToolExecutionStartShellToolInfo shellToolInfo,
        /** Model identifier that generated this tool call */
        @JsonProperty("model") String model,
        /** Per-request treatment/eligibility signal returned by the Copilot API in the `X-GitHub-Copilot-Request-TE` response header for the associated model call; `false` when the header was absent or unparseable. */
        @JsonProperty("rte") Boolean rte,
        /** Name of the MCP server hosting this tool, when the tool is an MCP tool */
        @JsonProperty("mcpServerName") String mcpServerName,
        /** Preferred lookup name for the MCP server hosting this tool: the configured (namespaced) config-map key when the tool carries one, otherwise the display name from `mcpServerName`. Present when the tool is an MCP tool; this is the name unrestricted provenance telemetry hashes so it joins with `mcp_server_setup`, which keys off the configured name too. */
        @JsonProperty("mcpConfigServerName") String mcpConfigServerName,
        /** Original tool name on the MCP server, when the tool is an MCP tool */
        @JsonProperty("mcpToolName") String mcpToolName,
        /** Transport the MCP server hosting this tool is connected over, when the tool is an MCP tool and the server is configured */
        @JsonProperty("mcpTransport") McpServerTransport mcpTransport,
        /** Where the MCP server's configuration came from (`user`, `workspace`, `plugin`, or `builtin`), when the tool is an MCP tool and the server is configured */
        @JsonProperty("mcpConfigSource") McpServerSource mcpConfigSource,
        /** Identifier for the agent loop turn this tool was invoked in, matching the corresponding assistant.turn_start event */
        @JsonProperty("turnId") String turnId,
        /** When true, the tool output should be displayed expanded (verbatim) in the CLI timeline */
        @JsonProperty("displayVerbatim") Boolean displayVerbatim,
        /** Tool definition metadata, present for MCP tools with MCP Apps support */
        @JsonProperty("toolDescription") ToolExecutionStartToolDescription toolDescription,
        /** Tool call ID of the parent tool invocation when this event originates from a sub-agent */
        @JsonProperty("parentToolCallId") String parentToolCallId,
        /** Experimental HydraFusion attribution for this tool execution. */
        @JsonProperty("fusion") FusionAttribution fusion
    ) {
    }
}
