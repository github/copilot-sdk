/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: tool.user_requested
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ToolUserRequestedEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private ToolUserRequestedData data;

    @Override
    public String getType() {
        return "tool.user_requested";
    }

    public ToolUserRequestedData getData() {
        return data;
    }

    public void setData(ToolUserRequestedData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ToolUserRequestedData(@JsonProperty("toolCallId") String toolCallId,
            @JsonProperty("toolName") String toolName, @JsonProperty("arguments") Object arguments) {
    }
}
