/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: assistant.turn_start
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AssistantTurnStartEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private AssistantTurnStartData data;

    @Override
    public String getType() {
        return "assistant.turn_start";
    }

    public AssistantTurnStartData getData() {
        return data;
    }

    public void setData(AssistantTurnStartData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AssistantTurnStartData(@JsonProperty("turnId") String turnId) {
    }
}
