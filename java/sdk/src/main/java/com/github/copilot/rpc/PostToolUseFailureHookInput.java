/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Input for a post-tool-use-failure hook.
 * <p>
 * Fires after a tool execution whose result was "failure". The CLI extracts the
 * failure message from the tool result and passes it as the {@link #getError()}
 * field (rather than passing the full result object).
 *
 * @since 1.3.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostToolUseFailureHookInput {

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("cwd")
    private String cwd;

    @JsonProperty("toolName")
    private String toolName;

    @JsonProperty("toolArgs")
    private JsonNode toolArgs;

    @JsonProperty("error")
    private String error;

    /**
     * Gets the runtime session ID of the session that triggered the hook.
     *
     * @return the session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the runtime session ID of the session that triggered the hook.
     *
     * @param sessionId
     *            the session ID
     * @return this instance for method chaining
     */
    public PostToolUseFailureHookInput setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

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
    public PostToolUseFailureHookInput setTimestamp(long timestamp) {
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
    public PostToolUseFailureHookInput setCwd(String cwd) {
        this.cwd = cwd;
        return this;
    }

    /**
     * Gets the name of the tool that failed.
     *
     * @return the tool name
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * Sets the name of the tool that failed.
     *
     * @param toolName
     *            the tool name
     * @return this instance for method chaining
     */
    public PostToolUseFailureHookInput setToolName(String toolName) {
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
    public PostToolUseFailureHookInput setToolArgs(JsonNode toolArgs) {
        this.toolArgs = toolArgs;
        return this;
    }

    /**
     * Gets the failure message extracted from the tool's result.
     *
     * @return the error message
     */
    public String getError() {
        return error;
    }

    /**
     * Sets the failure message extracted from the tool's result.
     *
     * @param error
     *            the error message
     * @return this instance for method chaining
     */
    public PostToolUseFailureHookInput setError(String error) {
        this.error = error;
        return this;
    }
}
