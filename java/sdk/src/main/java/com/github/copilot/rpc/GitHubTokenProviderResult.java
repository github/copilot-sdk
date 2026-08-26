/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.Objects;

/**
 * Result of acquiring a session-scoped GitHub token.
 * <p>
 * Token values are redacted from {@link #toString()}.
 *
 * @since 1.0.0
 */
public final class GitHubTokenProviderResult {

    private final String accessToken;
    private final long expiresIn;
    private final String tokenType;
    private final boolean cancelled;

    private GitHubTokenProviderResult(String accessToken, long expiresIn, String tokenType, boolean cancelled) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.tokenType = tokenType;
        this.cancelled = cancelled;
    }

    /**
     * Creates a token result.
     *
     * @param accessToken
     *            GitHub access token
     * @param expiresIn
     *            positive remaining lifetime in seconds when the callback completes
     * @return the token result
     */
    public static GitHubTokenProviderResult token(String accessToken, long expiresIn) {
        return token(accessToken, expiresIn, null);
    }

    /**
     * Creates a token result with an explicit OAuth token type.
     *
     * @param accessToken
     *            GitHub access token
     * @param expiresIn
     *            positive remaining lifetime in seconds when the callback completes
     * @param tokenType
     *            OAuth token type, or {@code null} to use the runtime's bearer
     *            default
     * @return the token result
     */
    public static GitHubTokenProviderResult token(String accessToken, long expiresIn, String tokenType) {
        Objects.requireNonNull(accessToken, "accessToken must not be null");
        return new GitHubTokenProviderResult(accessToken, expiresIn, tokenType, false);
    }

    /**
     * Creates an explicit cancellation result.
     *
     * @return the cancellation result
     */
    public static GitHubTokenProviderResult cancelled() {
        return new GitHubTokenProviderResult(null, 0, null, true);
    }

    /**
     * Gets whether acquisition was cancelled.
     *
     * @return {@code true} for a cancellation result
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Gets the access token.
     *
     * @return the token, or {@code null} for cancellation
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Gets the remaining token lifetime.
     *
     * @return remaining lifetime in seconds
     */
    public long getExpiresIn() {
        return expiresIn;
    }

    /**
     * Gets the OAuth token type.
     *
     * @return token type, or {@code null} for the runtime default
     */
    public String getTokenType() {
        return tokenType;
    }

    @Override
    public String toString() {
        if (cancelled) {
            return "GitHubTokenProviderResult{cancelled}";
        }
        return "GitHubTokenProviderResult{accessToken=<redacted>, expiresIn=" + expiresIn + ", tokenType=" + tokenType
                + "}";
    }
}
