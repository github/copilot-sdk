/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: assistant.message_delta
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AssistantMessageDeltaEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private AssistantMessageDeltaData data;

    @Override
    public String getType() {
        return "assistant.message_delta";
    }

    public AssistantMessageDeltaData getData() {
        return data;
    }

    public void setData(AssistantMessageDeltaData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AssistantMessageDeltaData(@JsonProperty("messageId") String messageId,
            @JsonProperty("deltaContent") String deltaContent,
            @JsonProperty("totalResponseSizeBytes") Double totalResponseSizeBytes,
            @JsonProperty("parentToolCallId") String parentToolCallId) {
    }
}
