/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Output for a user-prompt-submitted hook.
 * <p>
 * Allows modifying the user's prompt before processing.
 *
 * @since 1.0.7
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserPromptSubmittedHookOutput {

    @JsonProperty("modifiedPrompt")
    private String modifiedPrompt;

    @JsonProperty("additionalContext")
    private String additionalContext;

    @JsonProperty("suppressOutput")
    private Boolean suppressOutput;

    /**
     * Gets the modified prompt.
     *
     * @return the modified prompt, or {@code null} to use the original
     */
    public String getModifiedPrompt() {
        return modifiedPrompt;
    }

    /**
     * Sets a modified version of the user's prompt.
     *
     * @param modifiedPrompt
     *            the modified prompt to use instead of the original
     * @return this instance for method chaining
     */
    public UserPromptSubmittedHookOutput setModifiedPrompt(String modifiedPrompt) {
        this.modifiedPrompt = modifiedPrompt;
        return this;
    }

    /**
     * Gets the additional context to add.
     *
     * @return the additional context, or {@code null}
     */
    public String getAdditionalContext() {
        return additionalContext;
    }

    /**
     * Sets additional context to be added to the prompt.
     *
     * @param additionalContext
     *            the additional context
     * @return this instance for method chaining
     */
    public UserPromptSubmittedHookOutput setAdditionalContext(String additionalContext) {
        this.additionalContext = additionalContext;
        return this;
    }

    /**
     * Gets whether output should be suppressed.
     *
     * @return {@code true} to suppress output, or {@code null}
     */
    public Boolean getSuppressOutput() {
        return suppressOutput;
    }

    /**
     * Sets whether to suppress the output.
     *
     * @param suppressOutput
     *            {@code true} to suppress output
     * @return this instance for method chaining
     */
    public UserPromptSubmittedHookOutput setSuppressOutput(Boolean suppressOutput) {
        this.suppressOutput = suppressOutput;
        return this;
    }
}
