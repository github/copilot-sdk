/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Output for a post-tool-use-failure hook.
 * <p>
 * Only {@link #getAdditionalContext()} is consumed by the host CLI — it is
 * appended as hidden guidance to the model alongside the failed tool result.
 *
 * @since 1.3.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostToolUseFailureHookOutput {

    @JsonProperty("additionalContext")
    private String additionalContext;

    /**
     * Gets the additional context to inject into the conversation.
     *
     * @return the additional context, or {@code null}
     */
    public String getAdditionalContext() {
        return additionalContext;
    }

    /**
     * Sets the additional context to inject into the conversation for the language
     * model.
     *
     * @param additionalContext
     *            the additional context
     * @return this instance for method chaining
     */
    public PostToolUseFailureHookOutput setAdditionalContext(String additionalContext) {
        this.additionalContext = additionalContext;
        return this;
    }
}
