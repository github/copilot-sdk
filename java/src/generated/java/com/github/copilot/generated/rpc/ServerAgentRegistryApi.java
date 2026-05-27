/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code agentRegistry} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerAgentRegistryApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerAgentRegistryApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Inputs to spawn a managed-server child via the controller's spawn delegate.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<Void> spawn(AgentRegistrySpawnParams params) {
        return caller.invoke("agentRegistry.spawn", params, Void.class);
    }

}
