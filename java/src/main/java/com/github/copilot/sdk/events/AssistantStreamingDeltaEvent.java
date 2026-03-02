/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: assistant.streaming_delta
 *
 * @since 1.0.11
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AssistantStreamingDeltaEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private AssistantStreamingDeltaData data;

    @Override
    public String getType() {
        return "assistant.streaming_delta";
    }

    public AssistantStreamingDeltaData getData() {
        return data;
    }

    public void setData(AssistantStreamingDeltaData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AssistantStreamingDeltaData(@JsonProperty("totalResponseSizeBytes") double totalResponseSizeBytes) {
    }
}
