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

            @JsonProperty("filePath")
            private String filePath;

            @JsonProperty("displayName")
            private String displayName;

            @JsonProperty("text")
            private String text;

            @JsonProperty("selection")
            private Selection selection;

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

            /**
             * Gets the file path (used for selection attachments).
             *
             * @return the file path
             */
            public String getFilePath() {
                return filePath;
            }

            /**
             * Sets the file path (used for selection attachments).
             *
             * @param filePath
             *            the file path
             */
            public void setFilePath(String filePath) {
                this.filePath = filePath;
            }

            public String getDisplayName() {
                return displayName;
            }

            public void setDisplayName(String displayName) {
                this.displayName = displayName;
            }

            /**
             * Gets the text content (used for selection attachments).
             *
             * @return the selected text
             */
            public String getText() {
                return text;
            }

            /**
             * Sets the text content (used for selection attachments).
             *
             * @param text
             *            the selected text
             */
            public void setText(String text) {
                this.text = text;
            }

            /**
             * Gets the selection range (used for selection attachments).
             *
             * @return the selection range
             */
            public Selection getSelection() {
                return selection;
            }

            /**
             * Sets the selection range (used for selection attachments).
             *
             * @param selection
             *            the selection range
             */
            public void setSelection(Selection selection) {
                this.selection = selection;
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Selection {

                @JsonProperty("start")
                private Position start;

                @JsonProperty("end")
                private Position end;

                public Position getStart() {
                    return start;
                }

                public void setStart(Position start) {
                    this.start = start;
                }

                public Position getEnd() {
                    return end;
                }

                public void setEnd(Position end) {
                    this.end = end;
                }

                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class Position {

                    @JsonProperty("line")
                    private int line;

                    @JsonProperty("character")
                    private int character;

                    public int getLine() {
                        return line;
                    }

                    public void setLine(int line) {
                        this.line = line;
                    }

                    public int getCharacter() {
                        return character;
                    }

                    public void setCharacter(int character) {
                        this.character = character;
                    }
                }
            }
        }
    }
}
