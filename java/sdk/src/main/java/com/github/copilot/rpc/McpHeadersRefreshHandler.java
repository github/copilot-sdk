/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.concurrent.CompletableFuture;

/**
 * Supplies short-lived client-to-Copilot-Connectors service authorization
 * headers for a connected Connector MCP endpoint.
 * <p>
 * The authorization is normally the selected account's GitHub bearer token for
 * the exact service-advertised endpoint. This handler does not supply
 * downstream provider credentials; the Connector service owns those provider
 * tokens.
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface McpHeadersRefreshHandler {
    /**
     * Handles a service-authorization header refresh request.
     *
     * @param request
     *            the connected Copilot Connector MCP endpoint details
     * @param invocation
     *            the invocation context with session information
     * @return a future resolving to headers or an explicit no-result response
     */
    CompletableFuture<McpHeadersRefreshResult> handle(McpHeadersRefreshRequest request,
            McpHeadersRefreshInvocation invocation);
}
