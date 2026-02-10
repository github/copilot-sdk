/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: subagent.completed
 *
 * @since 1.0.0
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
    public record SubagentCompletedData(@JsonProperty("toolCallId") String toolCallId,
            @JsonProperty("agentName") String agentName) {
    }
}
