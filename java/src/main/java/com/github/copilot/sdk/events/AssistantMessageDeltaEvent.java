/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: assistant.message_delta
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
    public static class AssistantMessageDeltaData {

        @JsonProperty("messageId")
        private String messageId;

        @JsonProperty("deltaContent")
        private String deltaContent;

        @JsonProperty("totalResponseSizeBytes")
        private Double totalResponseSizeBytes;

        @JsonProperty("parentToolCallId")
        private String parentToolCallId;

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        public String getDeltaContent() {
            return deltaContent;
        }

        public void setDeltaContent(String deltaContent) {
            this.deltaContent = deltaContent;
        }

        public Double getTotalResponseSizeBytes() {
            return totalResponseSizeBytes;
        }

        public void setTotalResponseSizeBytes(Double totalResponseSizeBytes) {
            this.totalResponseSizeBytes = totalResponseSizeBytes;
        }

        public String getParentToolCallId() {
            return parentToolCallId;
        }

        public void setParentToolCallId(String parentToolCallId) {
            this.parentToolCallId = parentToolCallId;
        }
    }
}
