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
 * Session event "session.mcp_server_removed". Payload of `session.mcp_server_removed` identifying an MCP server the graph no longer runs.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionMcpServerRemovedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.mcp_server_removed"; }

    @JsonProperty("data")
    private SessionMcpServerRemovedEventData data;

    public SessionMcpServerRemovedEventData getData() { return data; }
    public void setData(SessionMcpServerRemovedEventData data) { this.data = data; }

    /** Data payload for {@link SessionMcpServerRemovedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionMcpServerRemovedEventData(
        /** Name of the MCP server that was removed from the graph */
        @JsonProperty("serverName") String serverName
    ) {
    }
}
