/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.copilot.CopilotExperimental;
import com.github.copilot.tool.Param;

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
     * @since 1.0.6
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
     * @since 1.0.6
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

    // ------------------------------------------------------------------
    // Fluent copy-style modifier methods for lambda-defined tools
    // ------------------------------------------------------------------

    /**
     * Returns a copy with the {@code overridesBuiltInTool} flag set.
     *
     * @param value
     *            {@code true} to indicate this tool intentionally overrides a
     *            built-in CLI tool with the same name
     * @return a new {@code ToolDefinition} with the flag applied
     * @since 1.0.6
     */
    @CopilotExperimental
    public ToolDefinition overridesBuiltInTool(boolean value) {
        return new ToolDefinition(name, description, parameters, handler, value, skipPermission, defer);
    }

    /**
     * Returns a copy with the {@code skipPermission} flag set.
     *
     * @param value
     *            {@code true} to skip the permission request for this tool
     *            invocation
     * @return a new {@code ToolDefinition} with the flag applied
     * @since 1.0.6
     */
    @CopilotExperimental
    public ToolDefinition skipPermission(boolean value) {
        return new ToolDefinition(name, description, parameters, handler, overridesBuiltInTool, value, defer);
    }

    /**
     * Returns a copy with the {@code defer} mode set.
     *
     * @param value
     *            the deferral mode; use {@link ToolDefer#AUTO} to allow deferral or
     *            {@link ToolDefer#NEVER} to force the tool to always be pre-loaded
     * @return a new {@code ToolDefinition} with the defer mode applied
     * @since 1.0.6
     */
    @CopilotExperimental
    public ToolDefinition defer(ToolDefer value) {
        return new ToolDefinition(name, description, parameters, handler, overridesBuiltInTool, skipPermission, value);
    }

    // ------------------------------------------------------------------
    // from(...) — sync, no ToolInvocation
    // ------------------------------------------------------------------

    /**
     * Creates a tool definition with a zero-argument synchronous handler.
     *
     * <p>
     * The handler is a {@link Supplier} that returns the tool result.
     *
     * <h2>Example</h2>
     *
     * <pre>{@code
     * ToolDefinition ping = ToolDefinition.from("ping", "Returns a simple pong response", () -> "pong");
     * }</pre>
     *
     * @param <R>
     *            the return type of the handler
     * @param name
     *            the unique name of the tool (must not be blank)
     * @param description
     *            a description of what the tool does (must not be blank)
     * @param handler
     *            the zero-argument sync handler
     * @return a new tool definition
     * @throws IllegalArgumentException
     *             if {@code name} or {@code description} is blank, or if
     *             {@code handler} is null
     * @since 1.0.6
     */
    @CopilotExperimental
    public static <R> ToolDefinition from(String name, String description, Supplier<R> handler) {
        requireNonBlankToolName(name);
        requireNonBlankDescription(description);
        requireNonNullHandler(handler, name);
        final ObjectMapper mapper = getConfiguredMapper();
        Map<String, Object> schema = ParamSchema.buildSchema(name, mapper);
        ToolHandler toolHandler = invocation -> {
            R result = handler.get();
            return CompletableFuture.completedFuture(formatResult(result, mapper));
        };
        return new ToolDefinition(name, description, schema, toolHandler, null, null, null);
    }

    /**
     * Creates a tool definition with a one-argument synchronous handler.
     *
     * <h2>Example</h2>
     *
     * <pre>{@code
     * ToolDefinition greet = ToolDefinition.from("greet", "Greets a user by name",
     * 		Param.of(String.class, "name", "The user's name"), name -> "Hello, " + name + "!");
     * }</pre>
     *
     * @param <T1>
     *            the type of the first parameter
     * @param <R>
     *            the return type of the handler
     * @param name
     *            the unique name of the tool (must not be blank)
     * @param description
     *            a description of what the tool does (must not be blank)
     * @param p1
     *            the first parameter descriptor
     * @param handler
     *            the one-argument sync handler
     * @return a new tool definition
     * @throws IllegalArgumentException
     *             if validation fails
     * @since 1.0.6
     */
    @CopilotExperimental
    public static <T1, R> ToolDefinition from(String name, String description, Param<T1> p1, Function<T1, R> handler) {
        requireNonBlankToolName(name);
        requireNonBlankDescription(description);
        requireNonNullHandler(handler, name);
        final ObjectMapper mapper = getConfiguredMapper();
        Map<String, Object> schema = ParamSchema.buildSchema(name, mapper, p1);
        ToolHandler toolHandler = invocation -> {
            T1 arg1 = ParamCoercion.coerce(invocation.getArguments(), p1, mapper);
            R result = handler.apply(arg1);
            return CompletableFuture.completedFuture(formatResult(result, mapper));
        };
        return new ToolDefinition(name, description, schema, toolHandler, null, null, null);
    }

    /**
     * Creates a tool definition with a two-argument synchronous handler.
     *
     * <h2>Example</h2>
     *
     * <pre>{@code
     * ToolDefinition add = ToolDefinition.from("add", "Adds two integers", Param.of(Integer.class, "a", "First number"),
     * 		Param.of(Integer.class, "b", "Second number"), (a, b) -> a + b);
     * }</pre>
     *
     * @param <T1>
     *            the type of the first parameter
     * @param <T2>
     *            the type of the second parameter
     * @param <R>
     *            the return type of the handler
     * @param name
     *            the unique name of the tool (must not be blank)
     * @param description
     *            a description of what the tool does (must not be blank)
     * @param p1
     *            the first parameter descriptor
     * @param p2
     *            the second parameter descriptor
     * @param handler
     *            the two-argument sync handler
     * @return a new tool definition
     * @throws IllegalArgumentException
     *             if validation fails
     * @since 1.0.6
     */
    @CopilotExperimental
    public static <T1, T2, R> ToolDefinition from(String name, String description, Param<T1> p1, Param<T2> p2,
            BiFunction<T1, T2, R> handler) {
        requireNonBlankToolName(name);
        requireNonBlankDescription(description);
        requireNonNullHandler(handler, name);
        final ObjectMapper mapper = getConfiguredMapper();
        Map<String, Object> schema = ParamSchema.buildSchema(name, mapper, p1, p2);
        ToolHandler toolHandler = invocation -> {
            T1 arg1 = ParamCoercion.coerce(invocation.getArguments(), p1, mapper);
            T2 arg2 = ParamCoercion.coerce(invocation.getArguments(), p2, mapper);
            R result = handler.apply(arg1, arg2);
            return CompletableFuture.completedFuture(formatResult(result, mapper));
        };
        return new ToolDefinition(name, description, schema, toolHandler, null, null, null);
    }

    // ------------------------------------------------------------------
    // fromAsync(...) — async, no ToolInvocation
    // ------------------------------------------------------------------

    /**
     * Creates a tool definition with a zero-argument asynchronous handler.
     *
     * <p>
     * The handler is a {@link Supplier} returning a {@link CompletableFuture}.
     *
     * <h2>Example</h2>
     *
     * <pre>{@code
     * ToolDefinition ping = ToolDefinition.fromAsync("ping", "Returns a pong response asynchronously",
     * 		() -> CompletableFuture.completedFuture("pong"));
     * }</pre>
     *
     * @param <R>
     *            the return type wrapped in {@link CompletableFuture}
     * @param name
     *            the unique name of the tool (must not be blank)
     * @param description
     *            a description of what the tool does (must not be blank)
     * @param handler
     *            the zero-argument async handler
     * @return a new tool definition
     * @throws IllegalArgumentException
     *             if validation fails
     * @since 1.0.6
     */
    @CopilotExperimental
    public static <R> ToolDefinition fromAsync(String name, String description,
            Supplier<CompletableFuture<R>> handler) {
        requireNonBlankToolName(name);
        requireNonBlankDescription(description);
        requireNonNullHandler(handler, name);
        final ObjectMapper mapper = getConfiguredMapper();
        Map<String, Object> schema = ParamSchema.buildSchema(name, mapper);
        ToolHandler toolHandler = invocation -> {
            CompletableFuture<R> future = handler.get();
            if (future == null) {
                return CompletableFuture.failedFuture(
                        new NullPointerException("Async handler for tool '" + name + "' returned a null future"));
            }
            return future.thenApply(result -> formatResult(result, mapper));
        };
        return new ToolDefinition(name, description, schema, toolHandler, null, null, null);
    }

    /**
     * Creates a tool definition with a one-argument asynchronous handler.
     *
     * <h2>Example</h2>
     *
     * <pre>{@code
     * ToolDefinition greet = ToolDefinition.fromAsync("greet_async", "Greets a user by name asynchronously",
     * 		Param.of(String.class, "name", "The user's name"),
     * 		name -> CompletableFuture.completedFuture("Hello, " + name + "!"));
     * }</pre>
     *
     * @param <T1>
     *            the type of the first parameter
     * @param <R>
     *            the return type wrapped in {@link CompletableFuture}
     * @param name
     *            the unique name of the tool (must not be blank)
     * @param description
     *            a description of what the tool does (must not be blank)
     * @param p1
     *            the first parameter descriptor
     * @param handler
     *            the one-argument async handler
     * @return a new tool definition
     * @throws IllegalArgumentException
     *             if validation fails
     * @since 1.0.6
     */
    @CopilotExperimental
    public static <T1, R> ToolDefinition fromAsync(String name, String description, Param<T1> p1,
            Function<T1, CompletableFuture<R>> handler) {
        requireNonBlankToolName(name);
        requireNonBlankDescription(description);
        requireNonNullHandler(handler, name);
        final ObjectMapper mapper = getConfiguredMapper();
        Map<String, Object> schema = ParamSchema.buildSchema(name, mapper, p1);
        ToolHandler toolHandler = invocation -> {
            T1 arg1 = ParamCoercion.coerce(invocation.getArguments(), p1, mapper);
            CompletableFuture<R> future = handler.apply(arg1);
            if (future == null) {
                return CompletableFuture.failedFuture(
                        new NullPointerException("Async handler for tool '" + name + "' returned a null future"));
            }
            return future.thenApply(result -> formatResult(result, mapper));
        };
        return new ToolDefinition(name, description, schema, toolHandler, null, null, null);
    }

    /**
     * Creates a tool definition with a two-argument asynchronous handler.
     *
     * @param <T1>
     *            the type of the first parameter
     * @param <T2>
     *            the type of the second parameter
     * @param <R>
     *            the return type wrapped in {@link CompletableFuture}
     * @param name
     *            the unique name of the tool (must not be blank)
     * @param description
     *            a description of what the tool does (must not be blank)
     * @param p1
     *            the first parameter descriptor
     * @param p2
     *            the second parameter descriptor
     * @param handler
     *            the two-argument async handler
     * @return a new tool definition
     * @throws IllegalArgumentException
     *             if validation fails
     * @since 1.0.6
     */
    @CopilotExperimental
    public static <T1, T2, R> ToolDefinition fromAsync(String name, String description, Param<T1> p1, Param<T2> p2,
            BiFunction<T1, T2, CompletableFuture<R>> handler) {
        requireNonBlankToolName(name);
        requireNonBlankDescription(description);
        requireNonNullHandler(handler, name);
        final ObjectMapper mapper = getConfiguredMapper();
        Map<String, Object> schema = ParamSchema.buildSchema(name, mapper, p1, p2);
        ToolHandler toolHandler = invocation -> {
            T1 arg1 = ParamCoercion.coerce(invocation.getArguments(), p1, mapper);
            T2 arg2 = ParamCoercion.coerce(invocation.getArguments(), p2, mapper);
            CompletableFuture<R> future = handler.apply(arg1, arg2);
            if (future == null) {
                return CompletableFuture.failedFuture(
                        new NullPointerException("Async handler for tool '" + name + "' returned a null future"));
            }
            return future.thenApply(result -> formatResult(result, mapper));
        };
        return new ToolDefinition(name, description, schema, toolHandler, null, null, null);
    }

    // ------------------------------------------------------------------
    // fromWithToolInvocation(...) — sync, with ToolInvocation context
    // ------------------------------------------------------------------

    /**
     * Creates a tool definition with a zero-argument synchronous handler that
     * receives the {@link ToolInvocation} context.
     *
     * <h2>Example</h2>
     *
     * <pre>{@code
     * ToolDefinition sessionInfo = ToolDefinition.fromWithToolInvocation("session_info", "Return the current session id",
     * 		invocation -> "sessionId=" + invocation.getSessionId());
     * }</pre>
     *
     * @param <R>
     *            the return type of the handler
     * @param name
     *            the unique name of the tool (must not be blank)
     * @param description
     *            a description of what the tool does (must not be blank)
     * @param handler
     *            a function accepting the {@link ToolInvocation} context
     * @return a new tool definition
     * @throws IllegalArgumentException
     *             if validation fails
     * @since 1.0.6
     */
    @CopilotExperimental
    public static <R> ToolDefinition fromWithToolInvocation(String name, String description,
            Function<ToolInvocation, R> handler) {
        requireNonBlankToolName(name);
        requireNonBlankDescription(description);
        requireNonNullHandler(handler, name);
        final ObjectMapper mapper = getConfiguredMapper();
        Map<String, Object> schema = ParamSchema.buildSchema(name, mapper);
        ToolHandler toolHandler = invocation -> {
            R result = handler.apply(invocation);
            return CompletableFuture.completedFuture(formatResult(result, mapper));
        };
        return new ToolDefinition(name, description, schema, toolHandler, null, null, null);
    }

    /**
     * Creates a tool definition with a one-argument synchronous handler that also
     * receives the {@link ToolInvocation} context.
     *
     * <h2>Example</h2>
     *
     * <pre>{@code
     * ToolDefinition reportPhase = ToolDefinition.fromWithToolInvocation("report_phase",
     * 		"Report the current phase along with invocation context", Param.of(String.class, "phase", "Current phase"),
     * 		(phase, invocation) -> "phase=" + phase + ", toolCallId=" + invocation.getToolCallId());
     * }</pre>
     *
     * @param <T1>
     *            the type of the first parameter
     * @param <R>
     *            the return type of the handler
     * @param name
     *            the unique name of the tool (must not be blank)
     * @param description
     *            a description of what the tool does (must not be blank)
     * @param p1
     *            the first parameter descriptor
     * @param handler
     *            a function accepting the typed argument and the
     *            {@link ToolInvocation} context
     * @return a new tool definition
     * @throws IllegalArgumentException
     *             if validation fails
     * @since 1.0.6
     */
    @CopilotExperimental
    public static <T1, R> ToolDefinition fromWithToolInvocation(String name, String description, Param<T1> p1,
            BiFunction<T1, ToolInvocation, R> handler) {
        requireNonBlankToolName(name);
        requireNonBlankDescription(description);
        requireNonNullHandler(handler, name);
        final ObjectMapper mapper = getConfiguredMapper();
        Map<String, Object> schema = ParamSchema.buildSchema(name, mapper, p1);
        ToolHandler toolHandler = invocation -> {
            T1 arg1 = ParamCoercion.coerce(invocation.getArguments(), p1, mapper);
            R result = handler.apply(arg1, invocation);
            return CompletableFuture.completedFuture(formatResult(result, mapper));
        };
        return new ToolDefinition(name, description, schema, toolHandler, null, null, null);
    }

    // ------------------------------------------------------------------
    // fromAsyncWithToolInvocation(...) — async, with ToolInvocation context
    // ------------------------------------------------------------------

    /**
     * Creates a tool definition with a zero-argument asynchronous handler that
     * receives the {@link ToolInvocation} context.
     *
     * <h2>Example</h2>
     *
     * <pre>{@code
     * ToolDefinition sessionInfo = ToolDefinition.fromAsyncWithToolInvocation("session_info_async",
     * 		"Return the current session id asynchronously",
     * 		invocation -> CompletableFuture.completedFuture("sessionId=" + invocation.getSessionId()));
     * }</pre>
     *
     * @param <R>
     *            the return type wrapped in {@link CompletableFuture}
     * @param name
     *            the unique name of the tool (must not be blank)
     * @param description
     *            a description of what the tool does (must not be blank)
     * @param handler
     *            a function accepting the {@link ToolInvocation} context, returning
     *            a {@link CompletableFuture}
     * @return a new tool definition
     * @throws IllegalArgumentException
     *             if validation fails
     * @since 1.0.6
     */
    @CopilotExperimental
    public static <R> ToolDefinition fromAsyncWithToolInvocation(String name, String description,
            Function<ToolInvocation, CompletableFuture<R>> handler) {
        requireNonBlankToolName(name);
        requireNonBlankDescription(description);
        requireNonNullHandler(handler, name);
        final ObjectMapper mapper = getConfiguredMapper();
        Map<String, Object> schema = ParamSchema.buildSchema(name, mapper);
        ToolHandler toolHandler = invocation -> {
            CompletableFuture<R> future = handler.apply(invocation);
            if (future == null) {
                return CompletableFuture.failedFuture(
                        new NullPointerException("Async handler for tool '" + name + "' returned a null future"));
            }
            return future.thenApply(result -> formatResult(result, mapper));
        };
        return new ToolDefinition(name, description, schema, toolHandler, null, null, null);
    }

    /**
     * Creates a tool definition with a one-argument asynchronous handler that also
     * receives the {@link ToolInvocation} context.
     *
     * <h2>Example</h2>
     *
     * <pre>{@code
     * ToolDefinition reportPhase = ToolDefinition.fromAsyncWithToolInvocation("report_phase_async",
     * 		"Report the current phase with invocation context asynchronously",
     * 		Param.of(String.class, "phase", "The current phase"), (phase, invocation) -> CompletableFuture
     * 				.completedFuture("phase=" + phase + ", toolCallId=" + invocation.getToolCallId()));
     * }</pre>
     *
     * @param <T1>
     *            the type of the first parameter
     * @param <R>
     *            the return type wrapped in {@link CompletableFuture}
     * @param name
     *            the unique name of the tool (must not be blank)
     * @param description
     *            a description of what the tool does (must not be blank)
     * @param p1
     *            the first parameter descriptor
     * @param handler
     *            a function accepting the typed argument and the
     *            {@link ToolInvocation} context, returning a
     *            {@link CompletableFuture}
     * @return a new tool definition
     * @throws IllegalArgumentException
     *             if validation fails
     * @since 1.0.6
     */
    @CopilotExperimental
    public static <T1, R> ToolDefinition fromAsyncWithToolInvocation(String name, String description, Param<T1> p1,
            BiFunction<T1, ToolInvocation, CompletableFuture<R>> handler) {
        requireNonBlankToolName(name);
        requireNonBlankDescription(description);
        requireNonNullHandler(handler, name);
        final ObjectMapper mapper = getConfiguredMapper();
        Map<String, Object> schema = ParamSchema.buildSchema(name, mapper, p1);
        ToolHandler toolHandler = invocation -> {
            T1 arg1 = ParamCoercion.coerce(invocation.getArguments(), p1, mapper);
            CompletableFuture<R> future = handler.apply(arg1, invocation);
            if (future == null) {
                return CompletableFuture.failedFuture(
                        new NullPointerException("Async handler for tool '" + name + "' returned a null future"));
            }
            return future.thenApply(result -> formatResult(result, mapper));
        };
        return new ToolDefinition(name, description, schema, toolHandler, null, null, null);
    }

    // ------------------------------------------------------------------
    // Internal helpers: result formatting, validation
    // ------------------------------------------------------------------

    /**
     * Formats a handler return value according to the tool result contract:
     * <ul>
     * <li>{@link String} — returned as-is</li>
     * <li>{@code null} — mapped to {@code "Success"} (covers handlers that return
     * null to indicate a successful no-value result)</li>
     * <li>any other value — JSON-serialized via {@link ObjectMapper}</li>
     * </ul>
     */
    private static Object formatResult(Object result, ObjectMapper mapper) {
        if (result == null) {
            return "Success";
        }
        if (result instanceof String) {
            return result;
        }
        if (result instanceof ToolResultObject) {
            return result;
        }
        try {
            return mapper.writeValueAsString(result);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize tool result to JSON", ex);
        }
    }

    // ------------------------------------------------------------------
    // Validation helpers
    // ------------------------------------------------------------------

    private static void requireNonBlankToolName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tool name must not be null or blank");
        }
    }

    private static void requireNonBlankDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Tool description must not be null or blank");
        }
    }

    private static void requireNonNullHandler(Object handler, String toolName) {
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null for tool '" + toolName + "'");
        }
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
