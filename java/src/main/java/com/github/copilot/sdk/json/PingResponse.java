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
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PingResponse {

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("protocolVersion")
    private Integer protocolVersion;

    /**
     * Gets the echo message from the server.
     *
     * @return the message echoed back by the server
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message.
     *
     * @param message
     *            the message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the server timestamp.
     *
     * @return the timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp.
     *
     * @param timestamp
     *            the timestamp
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the SDK protocol version supported by the server.
     * <p>
     * The SDK validates that this version matches the expected version to ensure
     * compatibility.
     *
     * @return the protocol version, or {@code null} if not reported
     */
    public Integer getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * Sets the protocol version.
     *
     * @param protocolVersion
     *            the protocol version
     */
    public void setProtocolVersion(Integer protocolVersion) {
        this.protocolVersion = protocolVersion;
    }
}
