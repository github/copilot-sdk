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
 * API methods for the {@code secrets} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerSecretsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerSecretsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Registers secret values for redaction in session logs and exports. The SDK calls this to inject dynamically generated secret values (e.g., OIDC tokens).
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SecretsAddFilterValuesResult> addFilterValues(SecretsAddFilterValuesParams params) {
        return caller.invoke("secrets.addFilterValues", params, SecretsAddFilterValuesResult.class);
    }

}
