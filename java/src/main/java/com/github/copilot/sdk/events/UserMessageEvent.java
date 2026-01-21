/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Event: user.message
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class UserMessageEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private UserMessageData data;

    @Override
    public String getType() {
        return "user.message";
    }

    public UserMessageData getData() {
        return data;
    }

    public void setData(UserMessageData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserMessageData {

        @JsonProperty("content")
        private String content;

        @JsonProperty("transformedContent")
        private String transformedContent;

        @JsonProperty("attachments")
        private List<Attachment> attachments;

        @JsonProperty("source")
        private String source;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getTransformedContent() {
            return transformedContent;
        }

        public void setTransformedContent(String transformedContent) {
            this.transformedContent = transformedContent;
        }

        public List<Attachment> getAttachments() {
            return attachments;
        }

        public void setAttachments(List<Attachment> attachments) {
            this.attachments = attachments;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Attachment {

            @JsonProperty("type")
            private String type;

            @JsonProperty("path")
            private String path;

            @JsonProperty("displayName")
            private String displayName;

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getPath() {
                return path;
            }

            public void setPath(String path) {
                this.path = path;
            }

            public String getDisplayName() {
                return displayName;
            }

            public void setDisplayName(String displayName) {
                this.displayName = displayName;
            }
        }
    }
}
