/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: subagent.completed
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SubagentCompletedEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SubagentCompletedData data;

    @Override
    public String getType() {
        return "subagent.completed";
    }

    public SubagentCompletedData getData() {
        return data;
    }

    public void setData(SubagentCompletedData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubagentCompletedData {

        @JsonProperty("toolCallId")
        private String toolCallId;

        @JsonProperty("agentName")
        private String agentName;

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
    }
}
