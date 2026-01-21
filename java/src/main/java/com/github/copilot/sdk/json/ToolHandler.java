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
 * ToolHandler handler = invocation -> {
 * 	Map<String, Object> args = (Map<String, Object>) invocation.getArguments();
 * 	String query = args.get("query").toString();
 *
 * 	// Perform the tool's action
 * 	String result = performSearch(query);
 *
 * 	return CompletableFuture.completedFuture(result);
 * };
 * }</pre>
 *
 * @see ToolDefinition
 * @see ToolInvocation
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
