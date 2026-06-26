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
     *            the MCP OAuth request context
     * @return a future resolving to token data or cancellation
     */
    CompletableFuture<McpAuthResult> handle(McpAuthRequest request);
}
