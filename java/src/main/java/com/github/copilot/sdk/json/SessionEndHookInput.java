/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Input for a session-end hook.
 * <p>
 * This hook is invoked when a session ends, allowing you to perform cleanup or
 * logging.
 *
 * @since 1.0.7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionEndHookInput {

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("cwd")
    private String cwd;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("finalMessage")
    private String finalMessage;

    @JsonProperty("error")
    private String error;

    /**
     * Gets the timestamp when the session ended.
     *
     * @return the timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp when the session ended.
     *
     * @param timestamp
     *            the timestamp in milliseconds since epoch
     * @return this instance for method chaining
     */
    public SessionEndHookInput setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * Gets the current working directory.
     *
     * @return the current working directory
     */
    public String getCwd() {
        return cwd;
    }

    /**
     * Sets the current working directory.
     *
     * @param cwd
     *            the current working directory
     * @return this instance for method chaining
     */
    public SessionEndHookInput setCwd(String cwd) {
        this.cwd = cwd;
        return this;
    }

    /**
     * Gets the reason for session end.
     *
     * @return the reason: "complete", "error", "abort", "timeout", or
     *         "user_exit"
     */
    public String getReason() {
        return reason;
    }

    /**
     * Sets the reason for session end.
     *
     * @param reason
     *            the reason: "complete", "error", "abort", "timeout", or
     *            "user_exit"
     * @return this instance for method chaining
     */
    public SessionEndHookInput setReason(String reason) {
        this.reason = reason;
        return this;
    }

    /**
     * Gets the final message, if any.
     *
     * @return the final message, or {@code null}
     */
    public String getFinalMessage() {
        return finalMessage;
    }

    /**
     * Sets the final message.
     *
     * @param finalMessage
     *            the final message
     * @return this instance for method chaining
     */
    public SessionEndHookInput setFinalMessage(String finalMessage) {
        this.finalMessage = finalMessage;
        return this;
    }

    /**
     * Gets the error message, if the session ended due to an error.
     *
     * @return the error message, or {@code null}
     */
    public String getError() {
        return error;
    }

    /**
     * Sets the error message.
     *
     * @param error
     *            the error message
     * @return this instance for method chaining
     */
    public SessionEndHookInput setError(String error) {
        this.error = error;
        return this;
    }
}
