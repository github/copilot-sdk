/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The {@code session.mcp_servers_loaded} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionMcpServersLoadedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.mcp_servers_loaded"; }

    @JsonProperty("data")
    private SessionMcpServersLoadedEventData data;

    public SessionMcpServersLoadedEventData getData() { return data; }
    public void setData(SessionMcpServersLoadedEventData data) { this.data = data; }

    /** Data payload for {@link SessionMcpServersLoadedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionMcpServersLoadedEventData(
        /** Array of MCP server status summaries */
        @JsonProperty("servers") List<McpServersLoadedServer> servers
    ) {
    }
}
