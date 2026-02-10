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
 * 		String content = msg.getData().content();
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
    public record AssistantMessageData(@JsonProperty("messageId") String messageId,
            @JsonProperty("content") String content, @JsonProperty("toolRequests") List<ToolRequest> toolRequests,
            @JsonProperty("parentToolCallId") String parentToolCallId,
            @JsonProperty("reasoningOpaque") String reasoningOpaque,
            @JsonProperty("reasoningText") String reasoningText,
            @JsonProperty("encryptedContent") String encryptedContent) {

        /** Returns a defensive copy of the tool requests list. */
        @Override
        public List<ToolRequest> toolRequests() {
            return toolRequests == null ? null : Collections.unmodifiableList(toolRequests);
        }

        /**
         * Represents a request from the assistant to invoke a tool.
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record ToolRequest(@JsonProperty("toolCallId") String toolCallId, @JsonProperty("name") String name,
                @JsonProperty("arguments") Object arguments) {
        }
    }
}
