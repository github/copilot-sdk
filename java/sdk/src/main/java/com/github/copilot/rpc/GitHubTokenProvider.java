/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.concurrent.CompletableFuture;

/**
 * Acquires rotating GitHub tokens for one session.
 * <p>
 * Implementations return either a token with a positive remaining lifetime or
 * an explicit cancellation. Production GitHub tokens typically last eight
 * hours.
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface GitHubTokenProvider {

    /**
     * Acquires a GitHub token for the supplied host and session context.
     *
     * @param args
     *            callback context
     * @return a future containing a token or cancellation result
     */
    CompletableFuture<GitHubTokenProviderResult> getToken(GitHubTokenProviderArgs args);
}
