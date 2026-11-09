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
 * API methods for the {@code providers.config} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerProvidersConfigApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerProvidersConfigApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Adds a model to the server's saved provider configuration. This durable operation does not change any live session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ProvidersConfigAddModelResult> addModel(ProvidersConfigAddModelParams params) {
        return caller.invoke("providers.config.addModel", params, ProvidersConfigAddModelResult.class);
    }

}
