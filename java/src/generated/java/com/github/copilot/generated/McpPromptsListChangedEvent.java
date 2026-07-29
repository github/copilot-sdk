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
 * Session event "mcp.prompts.list_changed". Payload identifying the MCP server associated with a list change.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpPromptsListChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "mcp.prompts.list_changed"; }

    @JsonProperty("data")
    private McpPromptsListChangedEventData data;

    public McpPromptsListChangedEventData getData() { return data; }
    public void setData(McpPromptsListChangedEventData data) { this.data = data; }

    /** Data payload for {@link McpPromptsListChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record McpPromptsListChangedEventData(
        /** Name of the MCP server whose list changed */
        @JsonProperty("serverName") String serverName
    ) {
    }
}
