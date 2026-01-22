/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event fired when a tool execution reports progress.
 * <p>
 * This event provides progress updates during tool execution.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ToolExecutionProgressEvent extends AbstractSessionEvent {

    public static final String TYPE = "tool.execution_progress";

    @JsonProperty("data")
    private ToolExecutionProgressData data;

    @Override
    public String getType() {
        return TYPE;
    }

    public ToolExecutionProgressData getData() {
        return data;
    }

    public ToolExecutionProgressEvent setData(ToolExecutionProgressData data) {
        this.data = data;
        return this;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ToolExecutionProgressData {

        @JsonProperty("toolCallId")
        private String toolCallId;

        @JsonProperty("progressMessage")
        private String progressMessage;

        public String getToolCallId() {
            return toolCallId;
        }

        public ToolExecutionProgressData setToolCallId(String toolCallId) {
            this.toolCallId = toolCallId;
            return this;
        }

        public String getProgressMessage() {
            return progressMessage;
        }

        public ToolExecutionProgressData setProgressMessage(String progressMessage) {
            this.progressMessage = progressMessage;
            return this;
        }
    }
}
