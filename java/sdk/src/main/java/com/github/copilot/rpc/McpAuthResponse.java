/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

/**
 * Opt-in MCP auth response that preserves legacy request and result record
 * patterns. A usable legacy token takes precedence over handoff; legacy
 * cancellation remains cancellation even if handoff is also requested.
 *
 * @param legacyResult
 *            optional legacy token or cancellation result
 * @param isRuntimeRefresh
 *            whether to hand an advertised Refresh back to its runtime
 *            operation
 * @since 1.0.0
 */
public record McpAuthResponse(McpAuthResult legacyResult, boolean isRuntimeRefresh) {
    /**
     * Wraps an existing token or cancellation result.
     *
     * @param result
     *            the legacy result
     * @return legacy response
     */
    public static McpAuthResponse legacy(McpAuthResult result) {
        return new McpAuthResponse(result, false);
    }

    /**
     * Hands an advertised Refresh back to the original runtime operation.
     * Acceptance is not token installation or reconnect completion.
     *
     * @return noninteractive runtime refresh response
     */
    public static McpAuthResponse runtimeRefresh() {
        return new McpAuthResponse(null, true);
    }
}
