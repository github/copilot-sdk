/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Event: system.message
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SystemMessageEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SystemMessageData data;

    @Override
    public String getType() {
        return "system.message";
    }

    public SystemMessageData getData() {
        return data;
    }

    public void setData(SystemMessageData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SystemMessageData {

        @JsonProperty("content")
        private String content;

        @JsonProperty("type")
        private String type;

        @JsonProperty("metadata")
        private Map<String, Object> metadata;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
}
