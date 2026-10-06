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

}
