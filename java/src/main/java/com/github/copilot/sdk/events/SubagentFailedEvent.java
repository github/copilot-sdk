/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: subagent.failed
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SubagentFailedEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SubagentFailedData data;

    @Override
    public String getType() {
        return "subagent.failed";
    }

    public SubagentFailedData getData() {
        return data;
    }

    public void setData(SubagentFailedData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubagentFailedData {

        @JsonProperty("toolCallId")
        private String toolCallId;

        @JsonProperty("agentName")
        private String agentName;

        @JsonProperty("error")
        private String error;

        public String getToolCallId() {
            return toolCallId;
        }

        public void setToolCallId(String toolCallId) {
            this.toolCallId = toolCallId;
        }

        public String getAgentName() {
            return agentName;
        }

        public void setAgentName(String agentName) {
            this.agentName = agentName;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
