/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: assistant.turn_end
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AssistantTurnEndEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private AssistantTurnEndData data;

    @Override
    public String getType() {
        return "assistant.turn_end";
    }

    public AssistantTurnEndData getData() {
        return data;
    }

    public void setData(AssistantTurnEndData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AssistantTurnEndData {

        @JsonProperty("turnId")
        private String turnId;

        public String getTurnId() {
            return turnId;
        }

        public void setTurnId(String turnId) {
            this.turnId = turnId;
        }
    }
}
