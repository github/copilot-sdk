/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Input for a pre-MCP-tool-call hook.
 * <p>
 * This hook is called before an MCP tool call is dispatched, allowing you to
 * modify the {@code _meta} field that is sent with the tool call.
 *
 * @since 1.0.8
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PreMcpToolCallHookInput {

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("cwd")
    private String workingDirectory;

    @JsonProperty("serverName")
    private String serverName;

    @JsonProperty("toolName")
    private String toolName;

    @JsonProperty("arguments")
    private JsonNode arguments;

    @JsonProperty("toolCallId")
    private String toolCallId;

    @JsonProperty("_meta")
    private JsonNode meta;

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
    public PreMcpToolCallHookInput setSessionId(String sessionId) {
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
    public PreMcpToolCallHookInput setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * Gets the current working directory.
     *
     * @return the working directory path
     */
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * Sets the current working directory.
     *
     * @param workingDirectory
     *            the working directory path
     * @return this instance for method chaining
     */
    public PreMcpToolCallHookInput setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
        return this;
    }

    /**
     * Gets the name of the MCP server.
     *
     * @return the server name
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Sets the name of the MCP server.
     *
     * @param serverName
     *            the server name
     * @return this instance for method chaining
     */
    public PreMcpToolCallHookInput setServerName(String serverName) {
        this.serverName = serverName;
        return this;
    }

    /**
     * Gets the name of the tool being called.
     *
     * @return the tool name
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * Sets the name of the tool being called.
     *
     * @param toolName
     *            the tool name
     * @return this instance for method chaining
     */
    public PreMcpToolCallHookInput setToolName(String toolName) {
        this.toolName = toolName;
        return this;
    }

    /**
     * Gets the arguments passed to the tool.
     *
     * @return the tool arguments as a JSON node
     */
    public JsonNode getArguments() {
        return arguments;
    }

    /**
     * Sets the arguments passed to the tool.
     *
     * @param arguments
     *            the tool arguments as a JSON node
     * @return this instance for method chaining
     */
    public PreMcpToolCallHookInput setArguments(JsonNode arguments) {
        this.arguments = arguments;
        return this;
    }

    /**
     * Gets the tool call ID.
     *
     * @return the tool call ID, or {@code null} if not set
     */
    public String getToolCallId() {
        return toolCallId;
    }

    /**
     * Sets the tool call ID.
     *
     * @param toolCallId
     *            the tool call ID
     * @return this instance for method chaining
     */
    public PreMcpToolCallHookInput setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
        return this;
    }

    /**
     * Gets the existing {@code _meta} object that would be sent with the tool call.
     *
     * @return the meta as a JSON node, or {@code null} if not present
     */
    public JsonNode getMeta() {
        return meta;
    }

    /**
     * Sets the existing {@code _meta} object.
     *
     * @param meta
     *            the meta as a JSON node
     * @return this instance for method chaining
     */
    public PreMcpToolCallHookInput setMeta(JsonNode meta) {
        this.meta = meta;
        return this;
    }
}
