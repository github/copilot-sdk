/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Input for a user-prompt-submitted hook.
 * <p>
 * This hook is invoked when the user submits a prompt, allowing you to
 * intercept and modify the prompt before it is processed.
 *
 * @param sessionId
 *            the runtime session ID of the session that triggered the hook
 * @param timestamp
 *            the timestamp in milliseconds since epoch when the prompt was
 *            submitted
 * @param cwd
 *            the current working directory
 * @param prompt
 *            the user's prompt text
 * @since 1.0.7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserPromptSubmittedHookInput(@JsonProperty("sessionId") String sessionId,
        @JsonProperty("timestamp") long timestamp, @JsonProperty("cwd") String cwd,
        @JsonProperty("prompt") String prompt) {
}
