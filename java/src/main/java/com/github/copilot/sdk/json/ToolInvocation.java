/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a tool invocation request from the AI assistant.
 * <p>
 * When the assistant invokes a tool, this object contains the context including
 * the session ID, tool call ID, tool name, and arguments parsed from the
 * assistant's request.
 *
 * @see ToolHandler
 * @see ToolDefinition
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ToolInvocation {

    private String sessionId;
    private String toolCallId;
    private String toolName;
    private Object arguments;

    /**
     * Gets the session ID where the tool was invoked.
     *
     * @return the session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the session ID.
     *
     * @param sessionId
     *            the session ID
     * @return this invocation for method chaining
     */
    public ToolInvocation setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    /**
     * Gets the unique identifier for this tool call.
     * <p>
     * This ID correlates the tool invocation with its response.
     *
     * @return the tool call ID
     */
    public String getToolCallId() {
        return toolCallId;
    }

    /**
     * Sets the tool call ID.
     *
     * @param toolCallId
     *            the tool call ID
     * @return this invocation for method chaining
     */
    public ToolInvocation setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
        return this;
    }

    /**
     * Gets the name of the tool being invoked.
     *
     * @return the tool name
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * Sets the tool name.
     *
     * @param toolName
     *            the tool name
     * @return this invocation for method chaining
     */
    public ToolInvocation setToolName(String toolName) {
        this.toolName = toolName;
        return this;
    }

    /**
     * Gets the arguments passed to the tool.
     * <p>
     * This is typically a {@code Map<String, Object>} matching the parameter schema
     * defined in the tool's {@link ToolDefinition}.
     *
     * @return the arguments object
     */
    public Object getArguments() {
        return arguments;
    }

    /**
     * Sets the tool arguments.
     *
     * @param arguments
     *            the arguments object
     * @return this invocation for method chaining
     */
    public ToolInvocation setArguments(Object arguments) {
        this.arguments = arguments;
        return this;
    }
}
