/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

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
     * Secret values to add to the redaction filter.
     * @since 1.0.0
     */
    public CompletableFuture<SecretsAddFilterValuesResult> addFilterValues(SecretsAddFilterValuesParams params) {
        return caller.invoke("secrets.addFilterValues", params, SecretsAddFilterValuesResult.class);
    }

}
