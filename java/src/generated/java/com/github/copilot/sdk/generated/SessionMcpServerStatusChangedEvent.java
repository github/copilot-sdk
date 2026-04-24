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
 * The {@code session.mcp_server_status_changed} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionMcpServerStatusChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.mcp_server_status_changed"; }

    @JsonProperty("data")
    private SessionMcpServerStatusChangedEventData data;

    public SessionMcpServerStatusChangedEventData getData() { return data; }
    public void setData(SessionMcpServerStatusChangedEventData data) { this.data = data; }

    /** Data payload for {@link SessionMcpServerStatusChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionMcpServerStatusChangedEventData(
        /** Name of the MCP server whose status changed */
        @JsonProperty("serverName") String serverName,
        /** New connection status: connected, failed, needs-auth, pending, disabled, or not_configured */
        @JsonProperty("status") McpServerStatusChangedStatus status
    ) {
    }
}
