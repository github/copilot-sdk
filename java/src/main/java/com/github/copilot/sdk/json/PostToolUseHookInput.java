/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Input for a post-tool-use hook.
 *
 * @since 1.0.6
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostToolUseHookInput {

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("cwd")
    private String cwd;

    @JsonProperty("toolName")
    private String toolName;

    @JsonProperty("toolArgs")
    private JsonNode toolArgs;

    @JsonProperty("toolResult")
    private JsonNode toolResult;

    /**
     * Gets the timestamp of the hook invocation.
     *
     * @return the timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp of the hook invocation.
     *
     * @param timestamp
     *            the timestamp in milliseconds
     * @return this instance for method chaining
     */
    public PostToolUseHookInput setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * Gets the current working directory.
     *
     * @return the working directory path
     */
    public String getCwd() {
        return cwd;
    }

    /**
     * Sets the current working directory.
     *
     * @param cwd
     *            the working directory path
     * @return this instance for method chaining
     */
    public PostToolUseHookInput setCwd(String cwd) {
        this.cwd = cwd;
        return this;
    }

    /**
     * Gets the name of the tool that was invoked.
     *
     * @return the tool name
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * Sets the name of the tool that was invoked.
     *
     * @param toolName
     *            the tool name
     * @return this instance for method chaining
     */
    public PostToolUseHookInput setToolName(String toolName) {
        this.toolName = toolName;
        return this;
    }

    /**
     * Gets the arguments passed to the tool.
     *
     * @return the tool arguments as a JSON node
     */
    public JsonNode getToolArgs() {
        return toolArgs;
    }

    /**
     * Sets the arguments passed to the tool.
     *
     * @param toolArgs
     *            the tool arguments as a JSON node
     * @return this instance for method chaining
     */
    public PostToolUseHookInput setToolArgs(JsonNode toolArgs) {
        this.toolArgs = toolArgs;
        return this;
    }

    /**
     * Gets the result returned by the tool.
     *
     * @return the tool result as a JSON node
     */
    public JsonNode getToolResult() {
        return toolResult;
    }

    /**
     * Sets the result returned by the tool.
     *
     * @param toolResult
     *            the tool result as a JSON node
     * @return this instance for method chaining
     */
    public PostToolUseHookInput setToolResult(JsonNode toolResult) {
        this.toolResult = toolResult;
        return this;
    }
}
