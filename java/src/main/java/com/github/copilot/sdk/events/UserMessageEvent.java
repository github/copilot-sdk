/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Event: user.message
 *
 * @since 1.0.0
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
    public record UserMessageData(@JsonProperty("content") String content,
            @JsonProperty("transformedContent") String transformedContent,
            @JsonProperty("attachments") List<Attachment> attachments, @JsonProperty("source") String source) {

        /** Returns a defensive copy of the attachments list. */
        @Override
        public List<Attachment> attachments() {
            return attachments == null ? null : Collections.unmodifiableList(attachments);
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Attachment(@JsonProperty("type") String type, @JsonProperty("path") String path,
                @JsonProperty("filePath") String filePath, @JsonProperty("displayName") String displayName,
                @JsonProperty("text") String text, @JsonProperty("selection") Selection selection) {

            @JsonIgnoreProperties(ignoreUnknown = true)
            public record Selection(@JsonProperty("start") Position start, @JsonProperty("end") Position end) {

                @JsonIgnoreProperties(ignoreUnknown = true)
                public record Position(@JsonProperty("line") int line, @JsonProperty("character") int character) {
                }
            }
        }
    }
}
