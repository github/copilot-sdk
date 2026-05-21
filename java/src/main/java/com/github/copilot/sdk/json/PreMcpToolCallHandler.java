/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.concurrent.CompletableFuture;

/**
 * Handler for pre-MCP-tool-call hooks.
 * <p>
 * This hook is called before an MCP tool call is dispatched, allowing you to
 * modify or remove the {@code _meta} field sent with the tool call.
 *
 * @since 1.0.8
 */
@FunctionalInterface
public interface PreMcpToolCallHandler {

    /**
     * Handles a pre-MCP-tool-call hook invocation.
     *
     * @param input
     *            the hook input containing server name, tool name, arguments, and
     *            meta
     * @param invocation
     *            context information about the invocation
     * @return a future that resolves with the hook output, or {@code null} to
     *         preserve the existing {@code _meta}
     */
    CompletableFuture<PreMcpToolCallHookOutput> handle(PreMcpToolCallHookInput input, HookInvocation invocation);
}
