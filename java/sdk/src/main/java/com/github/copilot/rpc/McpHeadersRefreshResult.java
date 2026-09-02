/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.Map;

/**
 * Dynamic headers returned for a host-managed MCP server.
 *
 * @param headers
 *            headers to use, or {@code null} for no result
 * @param ttlMs
 *            optional cache lifetime in milliseconds
 * @since 1.0.0
 */
public record McpHeadersRefreshResult(Map<String, String> headers, Long ttlMs) {

    /**
     * Creates a defensive copy of the supplied headers.
     */
    public McpHeadersRefreshResult {
        headers = headers == null ? null : Map.copyOf(headers);
    }

    /**
     * Creates a result with headers and no handler-specific cache lifetime.
     *
     * @param headers
     *            headers to use
     * @return a headers result
     */
    public static McpHeadersRefreshResult withHeaders(Map<String, String> headers) {
        return new McpHeadersRefreshResult(headers, null);
    }

    /**
     * Creates a result with headers and a cache lifetime.
     *
     * @param headers
     *            headers to use
     * @param ttlMs
     *            cache lifetime in milliseconds
     * @return a headers result
     */
    public static McpHeadersRefreshResult withHeaders(Map<String, String> headers, long ttlMs) {
        return new McpHeadersRefreshResult(headers, ttlMs);
    }

    /**
     * Creates an explicit no-result response.
     *
     * @return a no-result response
     */
    public static McpHeadersRefreshResult none() {
        return new McpHeadersRefreshResult(null, null);
    }
}
