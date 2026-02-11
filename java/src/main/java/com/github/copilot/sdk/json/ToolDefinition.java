/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * // Define a record for your tool's arguments
 * record WeatherArgs(String location) {
 * }
 *
 * var tool = ToolDefinition.create("get_weather", "Get the current weather for a location",
 * 		Map.of("type", "object", "properties",
 * 				Map.of("location", Map.of("type", "string", "description", "City name")), "required",
 * 				List.of("location")),
 * 		invocation -> {
 * 			// Type-safe access with records (recommended)
 * 			WeatherArgs args = invocation.getArgumentsAs(WeatherArgs.class);
 * 			return CompletableFuture.completedFuture(getWeatherData(args.location()));
 *
 * 			// Or use Map-based access
 * 			// Map<String, Object> args = invocation.getArguments();
 * 			// String location = (String) args.get("location");
 * 		});
 * }</pre>
 *
 * @param name
 *            the unique name of the tool
 * @param description
 *            a description of what the tool does
 * @param parameters
 *            the JSON Schema defining the tool's parameters
 * @param handler
 *            the handler function to execute when invoked
 * @see SessionConfig#setTools(java.util.List)
 * @see ToolHandler
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ToolDefinition(@JsonProperty("name") String name, @JsonProperty("description") String description,
        @JsonProperty("parameters") Object parameters, @JsonIgnore ToolHandler handler) {

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
