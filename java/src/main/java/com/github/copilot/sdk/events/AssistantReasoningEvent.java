/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: assistant.reasoning
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
    public static class AssistantReasoningData {

        @JsonProperty("reasoningId")
        private String reasoningId;

        @JsonProperty("content")
        private String content;

        public String getReasoningId() {
            return reasoningId;
        }

        public void setReasoningId(String reasoningId) {
            this.reasoningId = reasoningId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
