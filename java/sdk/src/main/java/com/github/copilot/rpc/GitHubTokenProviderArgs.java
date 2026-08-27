/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.github.copilot.generated.rpc.GitHubTokenAcquireReason;

/**
 * Context supplied when a session needs a GitHub token.
 *
 * @param host
 *            effective GitHub host for which a token is required
 * @param sessionId
 *            session receiving the token, or {@code null} before a cloud
 *            session has been assigned an ID
 * @param reason
 *            whether this is the initial acquisition or a refresh
 * @since 1.0.0
 */
public record GitHubTokenProviderArgs(String host, String sessionId, GitHubTokenAcquireReason reason) {
}
