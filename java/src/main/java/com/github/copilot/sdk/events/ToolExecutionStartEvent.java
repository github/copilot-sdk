/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: tool.execution_start
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ToolExecutionStartEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private ToolExecutionStartData data;

    @Override
    public String getType() {
        return "tool.execution_start";
    }

    public ToolExecutionStartData getData() {
        return data;
    }

    public void setData(ToolExecutionStartData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ToolExecutionStartData(@JsonProperty("toolCallId") String toolCallId,
            @JsonProperty("toolName") String toolName, @JsonProperty("arguments") Object arguments,
            @JsonProperty("mcpServerName") String mcpServerName, @JsonProperty("mcpToolName") String mcpToolName,
            @JsonProperty("parentToolCallId") String parentToolCallId) {
    }
}
