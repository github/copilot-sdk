/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defines a tool that can be invoked by the AI assistant.
 * <p>
 * Tools extend the assistant's capabilities by allowing it to call back into
 * your application to perform actions or retrieve information. Each tool has a
 * name, description, parameter schema, and a handler function that executes
 * when the tool is invoked.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var tool = ToolDefinition.create("get_weather", "Get the current weather for a location",
 * 		Map.of("type", "object", "properties",
 * 				Map.of("location", Map.of("type", "string", "description", "City name")), "required",
 * 				List.of("location")),
 * 		invocation -> {
 * 			String location = ((Map<String, Object>) invocation.getArguments()).get("location").toString();
 * 			return CompletableFuture.completedFuture(getWeatherData(location));
 * 		});
 * }</pre>
 *
 * @see SessionConfig#setTools(java.util.List)
 * @see ToolHandler
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolDefinition {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("parameters")
    private Object parameters;

    private transient ToolHandler handler;

    /**
     * Creates an empty tool definition.
     * <p>
     * Use the setter methods to configure the tool.
     */
    public ToolDefinition() {
    }

    /**
     * Creates a tool definition with all properties.
     *
     * @param name
     *            the unique name of the tool
     * @param description
     *            a description of what the tool does
     * @param parameters
     *            the JSON Schema defining the tool's parameters
     * @param handler
     *            the handler function to execute when invoked
     */
    public ToolDefinition(String name, String description, Object parameters, ToolHandler handler) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
        this.handler = handler;
    }

    /**
     * Gets the tool name.
     *
     * @return the unique name of the tool
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the tool name.
     * <p>
     * The name should be unique within a session and follow naming conventions
     * similar to function names (e.g., "get_user", "search_files").
     *
     * @param name
     *            the unique name of the tool
     * @return this tool definition for method chaining
     */
    public ToolDefinition setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Gets the tool description.
     *
     * @return the description of what the tool does
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the tool description.
     * <p>
     * The description helps the AI understand when and how to use the tool. Be
     * clear and specific about the tool's purpose and any constraints.
     *
     * @param description
     *            the description of what the tool does
     * @return this tool definition for method chaining
     */
    public ToolDefinition setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Gets the parameter schema.
     *
     * @return the JSON Schema for the tool's parameters
     */
    public Object getParameters() {
        return parameters;
    }

    /**
     * Sets the parameter schema.
     * <p>
     * The schema should follow JSON Schema format and define the structure of
     * arguments the tool accepts. This is typically a {@code Map} with "type",
     * "properties", and "required" fields.
     *
     * @param parameters
     *            the JSON Schema for the tool's parameters
     * @return this tool definition for method chaining
     */
    public ToolDefinition setParameters(Object parameters) {
        this.parameters = parameters;
        return this;
    }

    /**
     * Gets the tool handler.
     *
     * @return the handler function that executes when the tool is invoked
     */
    public ToolHandler getHandler() {
        return handler;
    }

    /**
     * Sets the tool handler.
     * <p>
     * The handler is called when the assistant invokes this tool. It receives a
     * {@link ToolInvocation} with the arguments and should return a
     * {@code CompletableFuture} with the result.
     *
     * @param handler
     *            the handler function
     * @return this tool definition for method chaining
     * @see ToolHandler
     */
    public ToolDefinition setHandler(ToolHandler handler) {
        this.handler = handler;
        return this;
    }

    /**
     * Creates a tool definition with a JSON schema for parameters.
     * <p>
     * This is a convenience factory method for creating tools with a
     * {@code Map}-based parameter schema.
     *
     * @param name
     *            the unique name of the tool
     * @param description
     *            a description of what the tool does
     * @param schema
     *            the JSON Schema as a {@code Map}
     * @param handler
     *            the handler function to execute when invoked
     * @return a new tool definition
     */
    public static ToolDefinition create(String name, String description, Map<String, Object> schema,
            ToolHandler handler) {
        return new ToolDefinition(name, description, schema, handler);
    }
}
