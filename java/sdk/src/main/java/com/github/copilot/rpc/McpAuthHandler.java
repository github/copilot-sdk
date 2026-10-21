/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.concurrent.CompletableFuture;

/**
 * Handles MCP OAuth requests from the runtime.
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface McpAuthHandler {
    /**
     * Handles an MCP OAuth request.
     *
     * @param request
     *            the MCP OAuth request details
     * @param invocation
     *            the invocation context with session information
     * @return a future resolving to token data or cancellation
     */
    CompletableFuture<McpAuthResult> handle(McpAuthRequest request, McpAuthInvocation invocation);

    /**
     * Handles an OAuth request with an optional runtime-owned refresh capability.
     * The default preserves the legacy handler. An adopting host must choose an
     * eligible advertised Refresh before calling {@link #handle}; do not first
     * schedule legacy detached recovery and then convert its cancellation.
     *
     * @param request
     *            the unchanged legacy request
     * @param invocation
     *            the invocation context
     * @param runtimeRefreshSupported
     *            whether this request advertises refresh handoff; omission is false
     * @return a future resolving to a legacy response or runtime refresh
     */
    default CompletableFuture<McpAuthResponse> handleWithRuntimeRefresh(McpAuthRequest request,
            McpAuthInvocation invocation, boolean runtimeRefreshSupported) {
        return handle(request, invocation).thenApply(McpAuthResponse::legacy);
    }
}
