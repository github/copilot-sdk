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
 * @param timestamp
 *            the timestamp in milliseconds since epoch when the session ended
 * @param cwd
 *            the current working directory
 * @param reason
 *            the reason: "complete", "error", "abort", "timeout", or
 *            "user_exit"
 * @param finalMessage
 *            the final message, or {@code null}
 * @param error
 *            the error message, or {@code null}
 * @since 1.0.7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionEndHookInput(@JsonProperty("timestamp") long timestamp, @JsonProperty("cwd") String cwd,
        @JsonProperty("reason") String reason, @JsonProperty("finalMessage") String finalMessage,
        @JsonProperty("error") String error) {
}
