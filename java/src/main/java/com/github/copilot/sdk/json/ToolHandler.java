/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.concurrent.CompletableFuture;

/**
 * Functional interface for handling tool invocations from the AI assistant.
 * <p>
 * When the assistant decides to use a tool, it invokes this handler with the
 * tool's arguments. The handler should perform the requested action and return
 * the result.
 *
 * <h2>Example Implementation</h2>
 *
 * <pre>{@code
 * // Option 1: Type-safe access with records (recommended)
 * record SearchArgs(String query) {
 * }
 *
 * ToolHandler handler = invocation -> {
 * 	SearchArgs args = invocation.getArgumentsAs(SearchArgs.class);
 * 	String result = performSearch(args.query());
 * 	return CompletableFuture.completedFuture(result);
 * };
 *
 * // Option 2: Map-based access
 * ToolHandler handler = invocation -> {
 * 	Map<String, Object> args = invocation.getArguments();
 * 	String query = (String) args.get("query");
 * 	String result = performSearch(query);
 * 	return CompletableFuture.completedFuture(result);
 * };
 * }</pre>
 *
 * @see ToolDefinition
 * @see ToolInvocation
 * @since 1.0.0
 */
@FunctionalInterface
public interface ToolHandler {

    /**
     * Invokes the tool with the given invocation context.
     * <p>
     * The returned object will be serialized to JSON and sent back to the assistant
     * as the tool's result. This can be a {@code String}, {@code Map}, or any
     * JSON-serializable object.
     *
     * @param invocation
     *            the invocation context containing arguments
     * @return a future that completes with the tool's result
     */
    CompletableFuture<Object> invoke(ToolInvocation invocation);
}
