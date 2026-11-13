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
     * Reads the server's saved provider settings and credential availability without returning secrets or activating models.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ProvidersConfigReadResult> read() {
        return caller.invoke("providers.config.read", java.util.Map.of(), ProvidersConfigReadResult.class);
    }

    /**
     * Updates shared connection settings for a saved provider while preserving its credentials and other sensitive fields. This durable operation does not change any live session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ProvidersConfigUpdateProviderResult> updateProvider(ProvidersConfigUpdateProviderParams params) {
        return caller.invoke("providers.config.updateProvider", params, ProvidersConfigUpdateProviderResult.class);
    }

    /**
     * Updates one saved provider model without changing its sibling models. This durable operation does not change any live session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ProvidersConfigUpdateModelResult> updateModel(ProvidersConfigUpdateModelParams params) {
        return caller.invoke("providers.config.updateModel", params, ProvidersConfigUpdateModelResult.class);
    }

    /**
     * Removes one model from the server's saved provider configuration and removes its provider only when no saved models remain. This durable operation does not change any live session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ProvidersConfigRemoveModelResult> removeModel(ProvidersConfigRemoveModelParams params) {
        return caller.invoke("providers.config.removeModel", params, ProvidersConfigRemoveModelResult.class);
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
