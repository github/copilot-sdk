/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Event representing a complete message from the assistant.
 * <p>
 * This event is fired when the assistant has finished generating a response.
 * For streaming responses, use {@link AssistantMessageDeltaEvent} instead.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * session.on(event -> {
 * 	if (event instanceof AssistantMessageEvent msg) {
 * 		String content = msg.getData().getContent();
 * 		System.out.println("Assistant: " + content);
 * 	}
 * });
 * }</pre>
 *
 * @see AssistantMessageDeltaEvent
 * @see AbstractSessionEvent
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AssistantMessageEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private AssistantMessageData data;

    /**
     * {@inheritDoc}
     *
     * @return "assistant.message"
     */
    @Override
    public String getType() {
        return "assistant.message";
    }

    /**
     * Gets the message data.
     *
     * @return the message data containing content and tool requests
     */
    public AssistantMessageData getData() {
        return data;
    }

    /**
     * Sets the message data.
     *
     * @param data
     *            the message data
     */
    public void setData(AssistantMessageData data) {
        this.data = data;
    }

    /**
     * Contains the assistant message content and metadata.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AssistantMessageData {

        @JsonProperty("messageId")
        private String messageId;

        @JsonProperty("content")
        private String content;

        @JsonProperty("toolRequests")
        private List<ToolRequest> toolRequests;

        @JsonProperty("parentToolCallId")
        private String parentToolCallId;

        @JsonProperty("reasoningOpaque")
        private String reasoningOpaque;

        @JsonProperty("reasoningText")
        private String reasoningText;

        @JsonProperty("encryptedContent")
        private String encryptedContent;

        /**
         * Gets the unique message identifier.
         *
         * @return the message ID
         */
        public String getMessageId() {
            return messageId;
        }

        /**
         * Sets the message identifier.
         *
         * @param messageId
         *            the message ID
         */
        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        /**
         * Gets the text content of the assistant's message.
         *
         * @return the message content
         */
        public String getContent() {
            return content;
        }

        /**
         * Sets the message content.
         *
         * @param content
         *            the message content
         */
        public void setContent(String content) {
            this.content = content;
        }

        /**
         * Gets the list of tool requests made by the assistant.
         *
         * @return the tool requests, or {@code null} if none
         */
        public List<ToolRequest> getToolRequests() {
            return toolRequests == null ? null : Collections.unmodifiableList(toolRequests);
        }

        /**
         * Sets the tool requests.
         *
         * @param toolRequests
         *            the tool requests
         */
        public void setToolRequests(List<ToolRequest> toolRequests) {
            this.toolRequests = toolRequests;
        }

        /**
         * Gets the parent tool call ID if this message is in response to a tool.
         *
         * @return the parent tool call ID, or {@code null}
         */
        public String getParentToolCallId() {
            return parentToolCallId;
        }

        /**
         * Sets the parent tool call ID.
         *
         * @param parentToolCallId
         *            the parent tool call ID
         */
        public void setParentToolCallId(String parentToolCallId) {
            this.parentToolCallId = parentToolCallId;
        }

        /**
         * Gets the opaque reasoning content (encrypted/encoded).
         *
         * @return the opaque reasoning content, or {@code null}
         */
        public String getReasoningOpaque() {
            return reasoningOpaque;
        }

        /**
         * Sets the opaque reasoning content.
         *
         * @param reasoningOpaque
         *            the opaque reasoning content
         */
        public void setReasoningOpaque(String reasoningOpaque) {
            this.reasoningOpaque = reasoningOpaque;
        }

        /**
         * Gets the human-readable reasoning text.
         *
         * @return the reasoning text, or {@code null}
         */
        public String getReasoningText() {
            return reasoningText;
        }

        /**
         * Sets the reasoning text.
         *
         * @param reasoningText
         *            the reasoning text
         */
        public void setReasoningText(String reasoningText) {
            this.reasoningText = reasoningText;
        }

        /**
         * Gets the encrypted content.
         *
         * @return the encrypted content, or {@code null}
         */
        public String getEncryptedContent() {
            return encryptedContent;
        }

        /**
         * Sets the encrypted content.
         *
         * @param encryptedContent
         *            the encrypted content
         */
        public void setEncryptedContent(String encryptedContent) {
            this.encryptedContent = encryptedContent;
        }

        /**
         * Represents a request from the assistant to invoke a tool.
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ToolRequest {

            @JsonProperty("toolCallId")
            private String toolCallId;

            @JsonProperty("name")
            private String name;

            @JsonProperty("arguments")
            private Object arguments;

            /**
             * Gets the unique tool call identifier.
             *
             * @return the tool call ID
             */
            public String getToolCallId() {
                return toolCallId;
            }

            /**
             * Sets the tool call identifier.
             *
             * @param toolCallId
             *            the tool call ID
             */
            public void setToolCallId(String toolCallId) {
                this.toolCallId = toolCallId;
            }

            /**
             * Gets the name of the tool to invoke.
             *
             * @return the tool name
             */
            public String getName() {
                return name;
            }

            /**
             * Sets the tool name.
             *
             * @param name
             *            the tool name
             */
            public void setName(String name) {
                this.name = name;
            }

            /**
             * Gets the arguments to pass to the tool.
             *
             * @return the tool arguments (typically a Map or JsonNode)
             */
            public Object getArguments() {
                return arguments;
            }

            /**
             * Sets the tool arguments.
             *
             * @param arguments
             *            the tool arguments
             */
            public void setArguments(Object arguments) {
                this.arguments = arguments;
            }
        }
    }
}
