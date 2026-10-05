/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code accounts} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerAccountsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerAccountsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * OneAuth token request supplied by a trusted host application.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<EntraTokenAcquireResult> acquireEntraToken(AccountsAcquireEntraTokenParams params) {
        return caller.invoke("accounts.acquireEntraToken", params, EntraTokenAcquireResult.class);
    }

}
