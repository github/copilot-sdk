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
     * Optional GitHub token used to look up quota for a specific user instead of the global auth context.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<AccountGetQuotaResult> getQuota() {
        return caller.invoke("account.getQuota", java.util.Map.of(), AccountGetQuotaResult.class);
    }

    /**
     * Current authentication state
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<AccountGetCurrentAuthResult> getCurrentAuth() {
        return caller.invoke("account.getCurrentAuth", java.util.Map.of(), AccountGetCurrentAuthResult.class);
    }

    /**
     * List of all authenticated users
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<List<AccountAllUsers>> getAllUsers() {
        return caller.invoke("account.getAllUsers", java.util.Map.of(), RpcMapper.INSTANCE.getTypeFactory().constructCollectionType(List.class, AccountAllUsers.class));
    }

    /**
     * Credentials to store after successful authentication
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<AccountLoginResult> login(AccountLoginParams params) {
        return caller.invoke("account.login", params, AccountLoginResult.class);
    }

    /**
     * User to log out
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<AccountLogoutResult> logout(AccountLogoutParams params) {
        return caller.invoke("account.logout", params, AccountLogoutResult.class);
    }

}
