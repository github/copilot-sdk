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
     * Lists GitHub Mission Control environments visible to the authenticated identity. Does not require a running host and excludes host relay credentials.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<EnvironmentsListResult> list(EnvironmentsListParams params) {
        return caller.invoke("environments.list", params, EnvironmentsListResult.class);
    }

    /**
     * Gets safe discovery information for a GitHub Mission Control environment without requiring a running host.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<EnvironmentsGetResult> get(EnvironmentsGetParams params) {
        return caller.invoke("environments.get", params, EnvironmentsGetResult.class);
    }

    /**
     * Deletes a user-managed GitHub Mission Control environment. GitHub-managed environments cannot be deleted. Does not stop a running host, which may register again.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> delete(EnvironmentsDeleteParams params) {
        return caller.invoke("environments.delete", params, Void.class);
    }

}
