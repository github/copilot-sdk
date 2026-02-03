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
 * @since 1.0.7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserPromptSubmittedHookInput {

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("cwd")
    private String cwd;

    @JsonProperty("prompt")
    private String prompt;

    /**
     * Gets the timestamp when the prompt was submitted.
     *
     * @return the timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp when the prompt was submitted.
     *
     * @param timestamp
     *            the timestamp in milliseconds since epoch
     * @return this instance for method chaining
     */
    public UserPromptSubmittedHookInput setTimestamp(long timestamp) {
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
    public UserPromptSubmittedHookInput setCwd(String cwd) {
        this.cwd = cwd;
        return this;
    }

    /**
     * Gets the user's prompt.
     *
     * @return the prompt text
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Sets the user's prompt.
     *
     * @param prompt
     *            the prompt text
     * @return this instance for method chaining
     */
    public UserPromptSubmittedHookInput setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }
}
