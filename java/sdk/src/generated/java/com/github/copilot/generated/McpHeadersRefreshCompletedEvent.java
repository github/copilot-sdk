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
 * Session event "mcp.headers_refresh_completed". MCP headers refresh request completion notification
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpHeadersRefreshCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "mcp.headers_refresh_completed"; }

    @JsonProperty("data")
    private McpHeadersRefreshCompletedEventData data;

    public McpHeadersRefreshCompletedEventData getData() { return data; }
    public void setData(McpHeadersRefreshCompletedEventData data) { this.data = data; }

    /** Data payload for {@link McpHeadersRefreshCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record McpHeadersRefreshCompletedEventData(
        /** Request ID of the resolved headers refresh request */
        @JsonProperty("requestId") String requestId,
        /** How the pending MCP headers refresh request resolved. */
        @JsonProperty("outcome") McpHeadersRefreshCompletedOutcome outcome
    ) {
    }
}
