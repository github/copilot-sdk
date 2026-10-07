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
 * API methods for the {@code environments} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerEnvironmentsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerEnvironmentsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Optional discovery filters supported by GitHub Mission Control.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<EnvironmentsListResult> list(EnvironmentsListParams params) {
        return caller.invoke("environments.list", params, EnvironmentsListResult.class);
    }

    /**
     * Identify a Mission Control environment to retrieve.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<EnvironmentsGetResult> get(EnvironmentsGetParams params) {
        return caller.invoke("environments.get", params, EnvironmentsGetResult.class);
    }

    /**
     * Identify a user-managed Mission Control environment to delete.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> delete(EnvironmentsDeleteParams params) {
        return caller.invoke("environments.delete", params, Void.class);
    }

}
