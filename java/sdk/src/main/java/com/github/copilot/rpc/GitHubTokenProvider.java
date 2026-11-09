/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.concurrent.CompletableFuture;

/**
 * Acquires rotating GitHub tokens for one session.
 * <p>
 * Implementations return either a token with its remaining lifetime in seconds
 * at callback completion or an explicit cancellation. More than 3600 seconds
 * must still remain after token validation; validation time counts against the
 * lifetime. Production GitHub tokens typically last eight hours. Initial
 * cancellation, callback errors, and invalid token responses reject session
 * creation or resume instead of falling back to ambient authentication.
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
