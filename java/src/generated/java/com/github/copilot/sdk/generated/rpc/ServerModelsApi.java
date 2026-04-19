/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code models} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerModelsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerModelsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Invokes {@code models.list}.
     * @since 1.0.0
     */
    public CompletableFuture<ModelsListResult> list() {
        return caller.invoke("models.list", java.util.Map.of(), ModelsListResult.class);
    }

}
