/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: assistant.reasoning
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AssistantReasoningEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private AssistantReasoningData data;

    @Override
    public String getType() {
        return "assistant.reasoning";
    }

    public AssistantReasoningData getData() {
        return data;
    }

    public void setData(AssistantReasoningData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AssistantReasoningData(@JsonProperty("reasoningId") String reasoningId,
            @JsonProperty("content") String content) {
    }
}
