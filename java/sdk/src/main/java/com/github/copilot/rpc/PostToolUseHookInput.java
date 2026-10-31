/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

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

    @JsonProperty("toolCallId")
    private String toolCallId;

    /**
     * Gets the original tool-call ID, unchanged by argument modifications.
     * Correlate using this ID and {@link #getSessionId()}, not the callback owner's
     * session ID.
     *
     * @return the tool-call ID, or {@code null} on older runtimes
     */
    public String getToolCallId() {
        return toolCallId;
    }

    /**
     * Sets the original tool-call ID.
     *
     * @param toolCallId
     *            the tool-call ID, or {@code null}
     * @return this instance for method chaining
     */
    public PostToolUseHookInput setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
        return this;
    }

    @JsonProperty("traceparent")
    private String traceparent;

    @JsonProperty("tracestate")
    private String tracestate;

    /**
     * Gets the existing W3C trace context associated with this hook.
     *
     * @return the trace context, or {@code null} when unavailable
     */
    public String getTraceparent() {
        return traceparent;
    }

    /**
     * Sets the existing W3C trace context.
     *
     * @param traceparent
     *            the trace context, or {@code null}
     * @return this instance for method chaining
     */
    public PostToolUseHookInput setTraceparent(String traceparent) {
        this.traceparent = traceparent;
        return this;
    }

    /**
     * Gets the existing W3C vendor trace state associated with this hook.
     *
     * @return the trace state, or {@code null} when unavailable
     */
    public String getTracestate() {
        return tracestate;
    }

    /**
     * Sets the existing W3C vendor trace state.
     *
     * @param tracestate
     *            the trace state, or {@code null}
     * @return this instance for method chaining
     */
    public PostToolUseHookInput setTracestate(String tracestate) {
        this.tracestate = tracestate;
        return this;
    }

    @JsonProperty("agentId")
    private String agentId;

    public String getAgentId() {
        return agentId;
    }

    public PostToolUseHookInput setAgentId(String agentId) {
        this.agentId = agentId;
        return this;
    }

    @JsonProperty("agentType")
    private String agentType;

    public String getAgentType() {
        return agentType;
    }

    public PostToolUseHookInput setAgentType(String agentType) {
        this.agentType = agentType;
        return this;
    }

    @JsonProperty("parentAgentId")
    private String parentAgentId;

    public String getParentAgentId() {
        return parentAgentId;
    }

    public PostToolUseHookInput setParentAgentId(String parentAgentId) {
        this.parentAgentId = parentAgentId;
        return this;
    }

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

    @JsonProperty("toolResult")
    private JsonNode toolResult;

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
    public PostToolUseHookInput setSessionId(String sessionId) {
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
