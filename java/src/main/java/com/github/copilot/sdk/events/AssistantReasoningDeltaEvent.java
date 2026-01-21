/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: assistant.reasoning_delta
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
    public static class AssistantReasoningDeltaData {

        @JsonProperty("reasoningId")
        private String reasoningId;

        @JsonProperty("deltaContent")
        private String deltaContent;

        public String getReasoningId() {
            return reasoningId;
        }

        public void setReasoningId(String reasoningId) {
            this.reasoningId = reasoningId;
        }

        public String getDeltaContent() {
            return deltaContent;
        }

        public void setDeltaContent(String deltaContent) {
            this.deltaContent = deltaContent;
        }
    }
}
