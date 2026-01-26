/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: tool.execution_start
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
    public static class ToolExecutionStartData {

        @JsonProperty("toolCallId")
        private String toolCallId;

        @JsonProperty("toolName")
        private String toolName;

        @JsonProperty("arguments")
        private Object arguments;

        @JsonProperty("mcpServerName")
        private String mcpServerName;

        @JsonProperty("mcpToolName")
        private String mcpToolName;

        @JsonProperty("parentToolCallId")
        private String parentToolCallId;

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

        public String getMcpServerName() {
            return mcpServerName;
        }

        public void setMcpServerName(String mcpServerName) {
            this.mcpServerName = mcpServerName;
        }

        public String getMcpToolName() {
            return mcpToolName;
        }

        public void setMcpToolName(String mcpToolName) {
            this.mcpToolName = mcpToolName;
        }

        public String getParentToolCallId() {
            return parentToolCallId;
        }

        public void setParentToolCallId(String parentToolCallId) {
            this.parentToolCallId = parentToolCallId;
        }
    }
}
