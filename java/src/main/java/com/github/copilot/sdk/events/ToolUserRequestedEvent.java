/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: tool.user_requested
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
    public static class ToolUserRequestedData {

        @JsonProperty("toolCallId")
        private String toolCallId;

        @JsonProperty("toolName")
        private String toolName;

        @JsonProperty("arguments")
        private Object arguments;

        public String getToolCallId() {
            return toolCallId;
        }

        public void setToolCallId(String toolCallId) {
            this.toolCallId = toolCallId;
        }

        public String getToolName() {
            return toolName;
        }

        public void setToolName(String toolName) {
            this.toolName = toolName;
        }

        public Object getArguments() {
            return arguments;
        }

        public void setArguments(Object arguments) {
            this.arguments = arguments;
        }
    }
}
