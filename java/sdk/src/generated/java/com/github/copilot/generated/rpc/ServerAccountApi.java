/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code account} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerAccountApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerAccountApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Gets Copilot quota usage for the current or opaquely selected authenticated user.
     * <p>
     * Invokes the method with no params, applying the runtime defaults.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<AccountGetQuotaResult> getQuota() {
        return getQuota(null);
    }

    /**
     * Gets Copilot quota usage for the current or opaquely selected authenticated user.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<AccountGetQuotaResult> getQuota(AccountGetQuotaParams params) {
        return caller.invoke("account.getQuota", params == null ? java.util.Map.of() : params, AccountGetQuotaResult.class);
    }

    /**
     * Gets the currently active authentication credentials from the global auth manager.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<AccountGetCurrentAuthResult> getCurrentAuth() {
        return caller.invoke("account.getCurrentAuth", java.util.Map.of(), AccountGetCurrentAuthResult.class);
    }

    /**
     * Gets all authenticated users available for account switching.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<List<AccountAllUsers>> getAllUsers() {
        return caller.invoke("account.getAllUsers", java.util.Map.of(), RpcMapper.INSTANCE.getTypeFactory().constructCollectionType(List.class, AccountAllUsers.class));
    }

    /**
     * Validates and stores authentication credentials. When login is omitted, resolves the authenticated user from the token before persistence.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<AccountLoginResult> login(AccountLoginParams params) {
        return caller.invoke("account.login", params, AccountLoginResult.class);
    }

    /**
     * Removes user authentication from keychain and persisted state.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<AccountLogoutResult> logout(AccountLogoutParams params) {
        return caller.invoke("account.logout", params, AccountLogoutResult.class);
    }

    /**
     * Renders the sign-in banner and the one-line account description the runtime uses for an authentication identity. The runtime words these in several places for its own callers, so a host that announces a sign-in or an account switch should render them from here rather than composing its own, which would let the two spellings drift apart.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<AccountDescribeAuthInfoResult> describeAuthInfo(AccountDescribeAuthInfoParams params) {
        return caller.invoke("account.describeAuthInfo", params, AccountDescribeAuthInfoResult.class);
    }

    /**
     * Reports whether the supplied identity can create gists. Gists are unavailable on GitHub Enterprise Cloud with data residency, and to Enterprise Managed Users on github.com, so a host that offers gist actions should hide or disable them when this reports no support.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<AccountGetGistSupportResult> getGistSupport(AccountGetGistSupportParams params) {
        return caller.invoke("account.getGistSupport", params, AccountGetGistSupportResult.class);
    }

    /**
     * Resolves a path on the identity's GitHub host into an absolute URL, so a host can link to settings or documentation on the enterprise instance the user is signed in to rather than to github.com. Falls back to the public host when no identity is supplied.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<AccountGetWebUrlResult> getWebUrl(AccountGetWebUrlParams params) {
        return caller.invoke("account.getWebUrl", params, AccountGetWebUrlResult.class);
    }

    /**
     * Names the environment variable supplying a classic personal access token, if one is set. A classic token cannot be upgraded in place, so a host that finds one should tell the user which variable to unset rather than starting an interactive sign-in that the variable would override.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<AccountGetTokenEnvVarResult> getTokenEnvVar() {
        return caller.invoke("account.getTokenEnvVar", java.util.Map.of(), AccountGetTokenEnvVarResult.class);
    }

}
