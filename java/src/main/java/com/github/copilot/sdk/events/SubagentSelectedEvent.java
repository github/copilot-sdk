/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: subagent.selected
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SubagentSelectedEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SubagentSelectedData data;

    @Override
    public String getType() {
        return "subagent.selected";
    }

    public SubagentSelectedData getData() {
        return data;
    }

    public void setData(SubagentSelectedData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubagentSelectedData {

        @JsonProperty("agentName")
        private String agentName;

        @JsonProperty("agentDisplayName")
        private String agentDisplayName;

        @JsonProperty("tools")
        private String[] tools;

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

        public String[] getTools() {
            return tools;
        }

        public void setTools(String[] tools) {
            this.tools = tools;
        }
    }
}
