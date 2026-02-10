/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: assistant.intent
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AssistantIntentEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private AssistantIntentData data;

    @Override
    public String getType() {
        return "assistant.intent";
    }

    public AssistantIntentData getData() {
        return data;
    }

    public void setData(AssistantIntentData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AssistantIntentData(@JsonProperty("intent") String intent) {
    }
}
