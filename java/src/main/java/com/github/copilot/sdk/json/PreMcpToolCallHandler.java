/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.concurrent.CompletableFuture;

/**
 * Handler for pre-MCP-tool-call hooks.
 * <p>
 * This hook is called before an MCP tool call is dispatched to an MCP server,
 * allowing you to:
 * <ul>
 * <li>Inspect the tool call arguments and server name</li>
 * <li>Set, replace, or remove MCP request metadata ({@code _meta})</li>
 * </ul>
 *
 * @since 1.0.8
 */
@FunctionalInterface
public interface PreMcpToolCallHandler {

    /**
     * Handles a pre-MCP-tool-call hook invocation.
     *
     * @param input
     *            the hook input containing server name, tool name, and arguments
     * @param invocation
     *            context information about the invocation
     * @return a future that resolves with the hook output, or {@code null} to
     *         preserve existing metadata (no-op)
     */
    CompletableFuture<PreMcpToolCallHookOutput> handle(PreMcpToolCallHookInput input, HookInvocation invocation);
}
