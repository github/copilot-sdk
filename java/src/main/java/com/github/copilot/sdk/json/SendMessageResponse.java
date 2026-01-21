/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Internal response object from sending a message.
 * <p>
 * This is a low-level class for JSON-RPC communication containing the message
 * ID assigned by the server.
 *
 * @see SendMessageRequest
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SendMessageResponse {

    @JsonProperty("messageId")
    private String messageId;

    /**
     * Gets the message ID assigned by the server.
     *
     * @return the message ID
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Sets the message ID.
     *
     * @param messageId
     *            the message ID
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
