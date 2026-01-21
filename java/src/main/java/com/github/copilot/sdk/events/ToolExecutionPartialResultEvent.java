/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: tool.execution_partial_result
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ToolExecutionPartialResultEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private ToolExecutionPartialResultData data;

    @Override
    public String getType() {
        return "tool.execution_partial_result";
    }

    public ToolExecutionPartialResultData getData() {
        return data;
    }

    public void setData(ToolExecutionPartialResultData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ToolExecutionPartialResultData {

        @JsonProperty("toolCallId")
        private String toolCallId;

        @JsonProperty("partialOutput")
        private String partialOutput;

        public String getToolCallId() {
            return toolCallId;
        }

        public void setToolCallId(String toolCallId) {
            this.toolCallId = toolCallId;
        }

        public String getPartialOutput() {
            return partialOutput;
        }

        public void setPartialOutput(String partialOutput) {
            this.partialOutput = partialOutput;
        }
    }
}
