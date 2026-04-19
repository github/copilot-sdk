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
 * The {@code tool.user_requested} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ToolUserRequestedEvent extends SessionEvent {

    @Override
    public String getType() { return "tool.user_requested"; }

    @JsonProperty("data")
    private ToolUserRequestedEventData data;

    public ToolUserRequestedEventData getData() { return data; }
    public void setData(ToolUserRequestedEventData data) { this.data = data; }

    /** Data payload for {@link ToolUserRequestedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToolUserRequestedEventData(
        /** Unique identifier for this tool call */
        @JsonProperty("toolCallId") String toolCallId,
        /** Name of the tool the user wants to invoke */
        @JsonProperty("toolName") String toolName,
        /** Arguments for the tool invocation */
        @JsonProperty("arguments") Object arguments
    ) {
    }
}
