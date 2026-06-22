/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

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
     * Optional GitHub token used to look up quota for a specific user instead of the global auth context.
     * @since 1.0.0
     */
    public CompletableFuture<AccountGetQuotaResult> getQuota() {
        return caller.invoke("account.getQuota", java.util.Map.of(), AccountGetQuotaResult.class);
    }

    /**
     * Current authentication state
     * @since 1.0.0
     */
    public CompletableFuture<AccountGetCurrentAuthResult> getCurrentAuth() {
        return caller.invoke("account.getCurrentAuth", java.util.Map.of(), AccountGetCurrentAuthResult.class);
    }

    /**
     * List of all authenticated users
     * @since 1.0.0
     */
    public CompletableFuture<Void> getAllUsers() {
        return caller.invoke("account.getAllUsers", java.util.Map.of(), Void.class);
    }

    /**
     * Credentials to store after successful authentication
     * @since 1.0.0
     */
    public CompletableFuture<AccountLoginResult> login(AccountLoginParams params) {
        return caller.invoke("account.login", params, AccountLoginResult.class);
    }

    /**
     * User to log out
     * @since 1.0.0
     */
    public CompletableFuture<AccountLogoutResult> logout(AccountLogoutParams params) {
        return caller.invoke("account.logout", params, AccountLogoutResult.class);
    }

}
