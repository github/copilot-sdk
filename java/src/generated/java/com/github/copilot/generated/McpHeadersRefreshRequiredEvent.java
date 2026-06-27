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
 * Session event "mcp.headers_refresh_required". Dynamic headers refresh request for a remote MCP server
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpHeadersRefreshRequiredEvent extends SessionEvent {

    @Override
    public String getType() { return "mcp.headers_refresh_required"; }

    @JsonProperty("data")
    private McpHeadersRefreshRequiredEventData data;

    public McpHeadersRefreshRequiredEventData getData() { return data; }
    public void setData(McpHeadersRefreshRequiredEventData data) { this.data = data; }

    /** Data payload for {@link McpHeadersRefreshRequiredEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record McpHeadersRefreshRequiredEventData(
        /** Unique identifier for this headers refresh request; used to respond via session.mcp.headers.handlePendingHeadersRefreshRequest() */
        @JsonProperty("requestId") String requestId,
        /** Display name of the remote MCP server requesting headers */
        @JsonProperty("serverName") String serverName,
        /** URL of the remote MCP server requesting headers */
        @JsonProperty("serverUrl") String serverUrl,
        /** Why dynamic headers are being requested. */
        @JsonProperty("reason") McpHeadersRefreshRequiredReason reason
    ) {
    }
}
