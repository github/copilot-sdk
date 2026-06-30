/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

/**
 * Result returned by an MCP auth request handler.
 *
 * @since 1.0.0
 */
public record McpAuthResult(boolean isCancelled, McpAuthToken token) {
    /**
     * Creates a token result.
     *
     * @param token
     *            the host-provided OAuth token data
     * @return token result
     */
    public static McpAuthResult token(McpAuthToken token) {
        return new McpAuthResult(false, token);
    }

    /**
     * Creates a cancellation result.
     *
     * @return cancellation result
     */
    public static McpAuthResult cancelled() {
        return new McpAuthResult(true, null);
    }
}
