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
 * Session event "session.mcp_server_needs_reconnect". Payload of `session.mcp_server_needs_reconnect` identifying an MCP server whose connection must be re-established.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionMcpServerNeedsReconnectEvent extends SessionEvent {

    @Override
    public String getType() { return "session.mcp_server_needs_reconnect"; }

    @JsonProperty("data")
    private SessionMcpServerNeedsReconnectEventData data;

    public SessionMcpServerNeedsReconnectEventData getData() { return data; }
    public void setData(SessionMcpServerNeedsReconnectEventData data) { this.data = data; }

    /** Data payload for {@link SessionMcpServerNeedsReconnectEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionMcpServerNeedsReconnectEventData(
        /** Name of the MCP server that needs to reconnect */
        @JsonProperty("serverName") String serverName
    ) {
    }
}
