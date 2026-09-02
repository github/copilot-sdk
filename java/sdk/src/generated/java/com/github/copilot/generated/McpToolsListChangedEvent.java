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
 * Session event "mcp.tools.list_changed". Payload identifying the MCP server associated with a list change.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpToolsListChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "mcp.tools.list_changed"; }

    @JsonProperty("data")
    private McpToolsListChangedEventData data;

    public McpToolsListChangedEventData getData() { return data; }
    public void setData(McpToolsListChangedEventData data) { this.data = data; }

    /** Data payload for {@link McpToolsListChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record McpToolsListChangedEventData(
        /** Name of the MCP server whose list changed */
        @JsonProperty("serverName") String serverName
    ) {
    }
}
