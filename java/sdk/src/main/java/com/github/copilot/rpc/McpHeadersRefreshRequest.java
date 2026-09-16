/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.github.copilot.generated.McpHeadersRefreshRequiredReason;

/**
 * Request to refresh short-lived client-to-Copilot-Connectors service
 * authorization headers for a connected Connector MCP endpoint.
 *
 * @param serverKey
 *            stable server key used in the Connector MCP server map
 * @param serverUrl
 *            exact service-advertised Connector MCP endpoint URL
 * @param reason
 *            reason the headers must be refreshed
 * @since 1.0.0
 */
public record McpHeadersRefreshRequest(String serverKey, String serverUrl, McpHeadersRefreshRequiredReason reason) {
}
