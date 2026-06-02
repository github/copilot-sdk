/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code runtime} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerRuntimeApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerRuntimeApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Invokes {@code runtime.shutdown}.
     * @since 1.0.0
     */
    public CompletableFuture<Void> shutdown() {
        return caller.invoke("runtime.shutdown", java.util.Map.of(), Void.class);
    }

}
