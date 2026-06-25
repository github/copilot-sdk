/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.concurrent.CompletableFuture;

import com.github.copilot.CopilotExperimental;

/**
 * Functional interface for supplying per-provider bearer tokens for BYOK
 * provider requests.
 * <p>
 * The callback returns the raw token without a {@code Bearer } prefix. The SDK
 * keeps this callback client-side and the runtime requests a token via the
 * session-scoped {@code providerToken.getToken} RPC before each outbound model
 * request.
 * <p>
 * <strong>Experimental.</strong> This managed-identity surface may change or be
 * removed in future SDK or CLI releases.
 *
 * @see ProviderConfig#setBearerTokenProvider(BearerTokenProvider)
 * @see NamedProviderConfig#setBearerTokenProvider(BearerTokenProvider)
 * @since 1.0.0
 */
@CopilotExperimental
@FunctionalInterface
public interface BearerTokenProvider {

    /**
     * Gets a bearer token for the provider identified by {@code args}.
     *
     * @param args
     *            the provider token request arguments
     * @return a future that completes with the raw token, without a {@code Bearer }
     *         prefix
     */
    CompletableFuture<String> getToken(ProviderTokenArgs args);
}
