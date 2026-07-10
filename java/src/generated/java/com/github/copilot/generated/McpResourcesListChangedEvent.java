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
 * Session event "mcp.resources.list_changed". Payload of MCP `list_changed` notification events, emitted when an MCP server announces at runtime that one of its advertised lists changed.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpResourcesListChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "mcp.resources.list_changed"; }

    @JsonProperty("data")
    private McpResourcesListChangedEventData data;

    public McpResourcesListChangedEventData getData() { return data; }
    public void setData(McpResourcesListChangedEventData data) { this.data = data; }

    /** Data payload for {@link McpResourcesListChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record McpResourcesListChangedEventData(
        /** Name of the MCP server whose list changed */
        @JsonProperty("serverName") String serverName
    ) {
    }
}
