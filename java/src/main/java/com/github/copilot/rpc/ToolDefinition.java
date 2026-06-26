/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.copilot.CopilotExperimental;

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
 * @param overridesBuiltInTool
 *            when {@code true}, indicates that this tool intentionally
 *            overrides a built-in CLI tool with the same name; {@code null} or
 *            {@code false} means the tool is purely custom
 * @param skipPermission
 *            when {@code true}, the CLI skips the permission request for this
 *            tool invocation; {@code null} or {@code false} uses normal
 *            permission handling
 * @param defer
 *            controls whether the tool may be deferred (loaded lazily via tool
 *            search) rather than always pre-loaded; {@code null} lets the
 *            runtime decide
 * @see SessionConfig#setTools(java.util.List)
 * @see ToolHandler
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ToolDefinition(@JsonProperty("name") String name, @JsonProperty("description") String description,
        @JsonProperty("parameters") Object parameters, @JsonIgnore ToolHandler handler,
        @JsonProperty("overridesBuiltInTool") Boolean overridesBuiltInTool,
        @JsonProperty("skipPermission") Boolean skipPermission, @JsonProperty("defer") ToolDefer defer) {

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
        return new ToolDefinition(name, description, schema, handler, null, null, null);
    }

    /**
     * Creates a tool definition that overrides a built-in CLI tool.
     * <p>
     * Use this factory method when you want your custom tool to replace a built-in
     * tool (e.g., {@code grep}, {@code read_file}) with the same name. Setting
     * {@code overridesBuiltInTool} to {@code true} signals to the CLI that this is
     * intentional.
     *
     * @param name
     *            the name of the built-in tool to override
     * @param description
     *            a description of what the tool does
     * @param schema
     *            the JSON Schema as a {@code Map}
     * @param handler
     *            the handler function to execute when invoked
     * @return a new tool definition with the override flag set
     * @since 1.0.11
     */
    public static ToolDefinition createOverride(String name, String description, Map<String, Object> schema,
            ToolHandler handler) {
        return new ToolDefinition(name, description, schema, handler, true, null, null);
    }

    /**
     * Creates a tool definition that skips the permission request.
     * <p>
     * Use this factory method when the tool is safe to invoke without user
     * permission confirmation. Setting {@code skipPermission} to {@code true}
     * signals to the CLI that no permission check is needed.
     *
     * @param name
     *            the unique name of the tool
     * @param description
     *            a description of what the tool does
     * @param schema
     *            the JSON Schema as a {@code Map}
     * @param handler
     *            the handler function to execute when invoked
     * @return a new tool definition with permission skipping enabled
     * @since 1.0.0
     */
    public static ToolDefinition createSkipPermission(String name, String description, Map<String, Object> schema,
            ToolHandler handler) {
        return new ToolDefinition(name, description, schema, handler, null, true, null);
    }

    /**
     * Creates a tool definition with an explicit deferral mode.
     * <p>
     * Use this factory method to control whether the tool may be deferred (loaded
     * lazily via tool search) rather than always pre-loaded. Pass
     * {@link ToolDefer#AUTO} to allow deferral and {@link ToolDefer#NEVER} to force
     * the tool to always be pre-loaded.
     *
     * @param name
     *            the unique name of the tool
     * @param description
     *            a description of what the tool does
     * @param schema
     *            the JSON Schema as a {@code Map}
     * @param handler
     *            the handler function to execute when invoked
     * @param defer
     *            the deferral mode for the tool
     * @return a new tool definition with the deferral mode set
     * @since 1.0.0
     */
    public static ToolDefinition createWithDefer(String name, String description, Map<String, Object> schema,
            ToolHandler handler, ToolDefer defer) {
        return new ToolDefinition(name, description, schema, handler, null, null, defer);
    }

    /**
     * Discovers tool definitions from an object whose methods are annotated with
     * {@code @CopilotTool}. Requires that the {@code CopilotToolProcessor}
     * annotation processor ran at compile time (generating the
     * {@code $$CopilotToolMeta} companion class).
     *
     * @param instance
     *            the object containing {@code @CopilotTool}-annotated methods
     * @return list of tool definitions with working invocation handlers
     * @throws IllegalStateException
     *             if the generated {@code $$CopilotToolMeta} class is not found
     *             (annotation processor did not run)
     * @since 1.0.2
     */
    @CopilotExperimental
    public static List<ToolDefinition> fromObject(Object instance) {
        if (instance == null) {
            throw new IllegalArgumentException("instance must not be null");
        }
        Class<?> clazz = instance.getClass();
        return loadDefinitions(clazz, instance);
    }

    /**
     * Discovers tool definitions from a class with static
     * {@code @CopilotTool}-annotated methods. Requires that the
     * {@code CopilotToolProcessor} annotation processor ran at compile time
     * (generating the {@code $$CopilotToolMeta} companion class).
     *
     * @param clazz
     *            the class containing static {@code @CopilotTool}-annotated methods
     * @return list of tool definitions with working invocation handlers
     * @throws IllegalStateException
     *             if the generated {@code $$CopilotToolMeta} class is not found
     *             (annotation processor did not run)
     * @since 1.0.2
     */
    @CopilotExperimental
    public static List<ToolDefinition> fromClass(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }
        List<String> instanceMethods = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(com.github.copilot.tool.CopilotTool.class))
                .filter(m -> !Modifier.isStatic(m.getModifiers())).map(Method::getName).collect(Collectors.toList());
        if (!instanceMethods.isEmpty()) {
            throw new IllegalArgumentException(
                    "fromClass() requires all @CopilotTool methods to be static, but found instance methods: "
                            + instanceMethods + ". Use fromObject(new " + clazz.getSimpleName() + "()) instead.");
        }
        return loadDefinitions(clazz, null);
    }

    @SuppressWarnings("unchecked")
    private static List<ToolDefinition> loadDefinitions(Class<?> clazz, Object instance) {
        String metaClassName = clazz.getName() + "$$CopilotToolMeta";
        try {
            Class<?> metaClass = Class.forName(metaClassName, true, clazz.getClassLoader());
            var provider = (com.github.copilot.tool.CopilotToolMetadataProvider<Object>) metaClass
                    .getDeclaredConstructor().newInstance();
            return provider.definitions(instance, getConfiguredMapper());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Generated class " + metaClassName + " not found. "
                    + "Ensure the CopilotToolProcessor annotation processor ran during compilation. "
                    + "Add the copilot-sdk-java dependency to your annotation processor path.", e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke " + metaClassName + ".definitions()", e);
        }
    }

    /**
     * Returns the SDK-configured ObjectMapper for tool argument/result
     * serialization. Configuration mirrors
     * {@code JsonRpcClient.createObjectMapper()}.
     */
    private static ObjectMapper getConfiguredMapper() {
        return ConfiguredMapperHolder.INSTANCE;
    }

    /**
     * Lazy holder for the configured ObjectMapper (thread-safe, initialized on
     * first access).
     */
    private static final class ConfiguredMapperHolder {
        static final ObjectMapper INSTANCE = createMapper();

        private static ObjectMapper createMapper() {
            // Configuration must match JsonRpcClient.createObjectMapper()
            var mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
            return mapper;
        }
    }
}
