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
 * API methods for the {@code providersSource} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class ServerProvidersSourceApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerProvidersSourceApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Forwards a saved-model addition over the original authenticated local attachment to its owning server. Requires private CLI-host admission and fails if that attachment is no longer active; never saves to the controller as a fallback.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SavedProviderConfigurationAddModelResult> addModel(ProvidersSourceAddModelParams params) {
        return caller.invoke("providersSource.addModel", params, SavedProviderConfigurationAddModelResult.class);
    }

}
