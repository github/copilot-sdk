/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents a tool invocation request from the AI assistant.
 * <p>
 * When the assistant invokes a tool, this object contains the context including
 * the session ID, tool call ID, tool name, and arguments parsed from the
 * assistant's request.
 *
 * @see ToolHandler
 * @see ToolDefinition
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ToolInvocation {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private String sessionId;
    private String toolCallId;
    private String toolName;
    private JsonNode argumentsNode;

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
     * Gets the arguments passed to the tool as a Map.
     * <p>
     * The arguments are provided as a {@code Map<String, Object>} matching the
     * parameter schema defined in the tool's {@link ToolDefinition}. Values can be
     * accessed using standard Map operations.
     * <p>
     * For type-safe access, use {@link #getArgumentsAs(Class)} to deserialize
     * arguments into a record or POJO.
     *
     * @return the arguments as a Map, or null if no arguments
     * @see #getArgumentsAs(Class)
     */
    public Map<String, Object> getArguments() {
        if (argumentsNode == null) {
            return null;
        }
        return MAPPER.convertValue(argumentsNode, MAP_TYPE);
    }

    /**
     * Deserializes the tool arguments into the specified type.
     * <p>
     * This method provides type-safe access to tool arguments by converting the
     * JSON arguments into a record, POJO, or other compatible type.
     *
     * <pre>{@code
     * // Define a record for your tool's arguments
     * record WeatherArgs(String city) {
     * }
     *
     * // In your tool handler
     * WeatherArgs args = invocation.getArgumentsAs(WeatherArgs.class);
     * String city = args.city();
     * }</pre>
     *
     * @param <T>
     *            the type to deserialize to
     * @param type
     *            the class of the target type
     * @return the arguments deserialized as the specified type
     * @throws IllegalArgumentException
     *             if deserialization fails
     * @since 1.0.0
     */
    public <T> T getArgumentsAs(Class<T> type) {
        try {
            return MAPPER.treeToValue(argumentsNode, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize arguments to " + type.getName(), e);
        }
    }

    /**
     * Sets the tool arguments.
     * <p>
     * <strong>Note:</strong> This method is intended for internal SDK use and JSON
     * deserialization. Users typically do not need to call this method directly.
     *
     * @param arguments
     *            the arguments as a JsonNode
     * @return this invocation for method chaining
     */
    @JsonSetter("arguments")
    public ToolInvocation setArguments(JsonNode arguments) {
        this.argumentsNode = arguments;
        return this;
    }
}
