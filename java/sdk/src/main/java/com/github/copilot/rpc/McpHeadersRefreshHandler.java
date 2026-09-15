/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.concurrent.CompletableFuture;

/**
 * Supplies dynamic headers for a host-managed MCP server.
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface McpHeadersRefreshHandler {
    /**
     * Handles a dynamic-header refresh request.
     *
     * @param request
     *            the managed MCP server details
     * @param invocation
     *            the invocation context with session information
     * @return a future resolving to headers or an explicit no-result response
     */
    CompletableFuture<McpHeadersRefreshResult> handle(McpHeadersRefreshRequest request,
            McpHeadersRefreshInvocation invocation);
}
