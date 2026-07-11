/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Input for a pre-MCP-tool-call hook.
 * <p>
 * This hook fires before an MCP tool call is dispatched to an MCP server,
 * allowing you to inspect or modify the request metadata.
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
    private String cwd;

    @JsonProperty("serverName")
    private String serverName;

    @JsonProperty("toolName")
    private String toolName;

    @JsonProperty("arguments")
    private JsonNode arguments;

    @JsonProperty("toolCallId")
    private String toolCallId;

    @JsonProperty("_meta")
    private Map<String, JsonNode> meta;

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
    public PreMcpToolCallHookInput setCwd(String cwd) {
        this.cwd = cwd;
        return this;
    }

    /**
     * Gets the name of the MCP server being called.
     *
     * @return the server name
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Sets the name of the MCP server being called.
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
     * Gets the name of the MCP tool being called.
     *
     * @return the tool name
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * Sets the name of the MCP tool being called.
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
     * Gets the arguments for the MCP tool call.
     *
     * @return the arguments as a JSON node, or {@code null}
     */
    public JsonNode getArguments() {
        return arguments;
    }

    /**
     * Sets the arguments for the MCP tool call.
     *
     * @param arguments
     *            the arguments as a JSON node
     * @return this instance for method chaining
     */
    public PreMcpToolCallHookInput setArguments(JsonNode arguments) {
        this.arguments = arguments;
        return this;
    }

    /**
     * Gets the tool call ID, if available.
     *
     * @return the tool call ID, or {@code null}
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
     * Gets the MCP request metadata, if present.
     *
     * @return the metadata map, or {@code null}
     */
    public Map<String, JsonNode> getMeta() {
        return meta;
    }

    /**
     * Sets the MCP request metadata.
     *
     * @param meta
     *            the metadata map
     * @return this instance for method chaining
     */
    public PreMcpToolCallHookInput setMeta(Map<String, JsonNode> meta) {
        this.meta = meta;
        return this;
    }
}
