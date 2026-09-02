/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.github.copilot.generated.McpHeadersRefreshRequiredReason;

/**
 * Request to refresh dynamic headers for a host-managed MCP server.
 *
 * @param serverName
 *            display name of the managed MCP server
 * @param serverUrl
 *            URL of the managed MCP server
 * @param reason
 *            reason the headers must be refreshed
 * @since 1.0.0
 */
public record McpHeadersRefreshRequest(String serverName, String serverUrl, McpHeadersRefreshRequiredReason reason) {
}
