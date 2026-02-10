/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: subagent.started
 *
 * @since 1.0.0
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
    public record SubagentStartedData(@JsonProperty("toolCallId") String toolCallId,
            @JsonProperty("agentName") String agentName, @JsonProperty("agentDisplayName") String agentDisplayName,
            @JsonProperty("agentDescription") String agentDescription) {
    }
}
