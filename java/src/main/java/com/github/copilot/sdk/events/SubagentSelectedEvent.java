/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: subagent.selected
 *
 * @since 1.0.0
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
    public record SubagentSelectedData(@JsonProperty("agentName") String agentName,
            @JsonProperty("agentDisplayName") String agentDisplayName, @JsonProperty("tools") String[] tools) {

        /** Returns a defensive copy of the tools array. */
        @Override
        public String[] tools() {
            return tools == null ? null : tools.clone();
        }
    }
}
