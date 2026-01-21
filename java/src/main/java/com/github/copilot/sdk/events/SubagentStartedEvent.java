/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: subagent.started
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SubagentStartedEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SubagentStartedData data;

    @Override
    public String getType() {
        return "subagent.started";
    }

    public SubagentStartedData getData() {
        return data;
    }

    public void setData(SubagentStartedData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubagentStartedData {

        @JsonProperty("toolCallId")
        private String toolCallId;

        @JsonProperty("agentName")
        private String agentName;

        @JsonProperty("agentDisplayName")
        private String agentDisplayName;

        @JsonProperty("agentDescription")
        private String agentDescription;

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

        public String getAgentDisplayName() {
            return agentDisplayName;
        }

        public void setAgentDisplayName(String agentDisplayName) {
            this.agentDisplayName = agentDisplayName;
        }

        public String getAgentDescription() {
            return agentDescription;
        }

        public void setAgentDescription(String agentDescription) {
            this.agentDescription = agentDescription;
        }
    }
}
