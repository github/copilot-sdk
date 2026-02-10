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
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SendMessageResponse(
        /** The message ID assigned by the server. */
        @JsonProperty("messageId") String messageId) {
}
