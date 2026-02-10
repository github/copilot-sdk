/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: assistant.reasoning_delta
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AssistantReasoningDeltaEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private AssistantReasoningDeltaData data;

    @Override
    public String getType() {
        return "assistant.reasoning_delta";
    }

    public AssistantReasoningDeltaData getData() {
        return data;
    }

    public void setData(AssistantReasoningDeltaData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AssistantReasoningDeltaData(@JsonProperty("reasoningId") String reasoningId,
            @JsonProperty("deltaContent") String deltaContent) {
    }
}
