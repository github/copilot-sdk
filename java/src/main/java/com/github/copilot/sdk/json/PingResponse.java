/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from a ping request to the Copilot CLI server.
 * <p>
 * The ping response confirms connectivity and provides information about the
 * server, including the protocol version.
 *
 * @see com.github.copilot.sdk.CopilotClient#ping(String)
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PingResponse(
        /** The echo message from the server. */
        @JsonProperty("message") String message,
        /** The server timestamp in milliseconds since epoch. */
        @JsonProperty("timestamp") long timestamp,
        /**
         * The SDK protocol version supported by the server. The SDK validates that this
         * version matches the expected version to ensure compatibility.
         */
        @JsonProperty("protocolVersion") Integer protocolVersion) {
}
