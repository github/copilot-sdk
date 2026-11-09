/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code providers} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerProvidersApi {

    private final RpcCaller caller;

    /** API methods for the {@code providers.config} sub-namespace. */
    public final ServerProvidersConfigApi config;

    /** @param caller the RPC transport function */
    ServerProvidersApi(RpcCaller caller) {
        this.caller = caller;
        this.config = new ServerProvidersConfigApi(caller);
    }

}
