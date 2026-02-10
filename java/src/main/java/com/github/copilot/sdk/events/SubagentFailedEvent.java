/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: subagent.failed
 *
 * @since 1.0.0
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
    public record SubagentFailedData(@JsonProperty("toolCallId") String toolCallId,
            @JsonProperty("agentName") String agentName, @JsonProperty("error") String error) {
    }
}
