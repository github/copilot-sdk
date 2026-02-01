/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Output for a post-tool-use hook.
 *
 * @since 1.0.6
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostToolUseHookOutput {

    @JsonProperty("modifiedResult")
    private JsonNode modifiedResult;

    @JsonProperty("additionalContext")
    private String additionalContext;

    @JsonProperty("suppressOutput")
    private Boolean suppressOutput;

    /**
     * Gets the modified tool result.
     *
     * @return the modified result, or {@code null} to use original
     */
    public JsonNode getModifiedResult() {
        return modifiedResult;
    }

    /**
     * Sets the modified tool result.
     *
     * @param modifiedResult
     *            the modified result
     * @return this instance for method chaining
     */
    public PostToolUseHookOutput setModifiedResult(JsonNode modifiedResult) {
        this.modifiedResult = modifiedResult;
        return this;
    }

    /**
     * Gets additional context to provide to the model.
     *
     * @return the additional context
     */
    public String getAdditionalContext() {
        return additionalContext;
    }

    /**
     * Sets additional context to provide to the model.
     *
     * @param additionalContext
     *            the additional context
     * @return this instance for method chaining
     */
    public PostToolUseHookOutput setAdditionalContext(String additionalContext) {
        this.additionalContext = additionalContext;
        return this;
    }

    /**
     * Returns whether to suppress output.
     *
     * @return {@code true} to suppress output
     */
    public Boolean getSuppressOutput() {
        return suppressOutput;
    }

    /**
     * Sets whether to suppress output.
     *
     * @param suppressOutput
     *            {@code true} to suppress output
     * @return this instance for method chaining
     */
    public PostToolUseHookOutput setSuppressOutput(Boolean suppressOutput) {
        this.suppressOutput = suppressOutput;
        return this;
    }
}
