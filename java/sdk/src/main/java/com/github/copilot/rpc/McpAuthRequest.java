/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.github.copilot.generated.McpOauthRequiredStaticClientConfig;
import com.github.copilot.generated.McpOauthRequestReason;
import com.github.copilot.generated.McpOauthWWWAuthenticateParams;

/**
 * MCP OAuth request that the SDK host can satisfy with a host-acquired token.
 *
 * @since 1.0.0
 */
public record McpAuthRequest(String requestId, String serverName, String serverUrl, McpOauthRequestReason reason,
        McpOauthWWWAuthenticateParams wwwAuthenticateParams, String resourceMetadata,
        McpOauthRequiredStaticClientConfig staticClientConfig) {
}
