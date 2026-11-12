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
 * API methods for the {@code kerberosAuth} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerKerberosAuthApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerKerberosAuthApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Registers the calling SDK client's Kerberos callback. The callback authenticates requests made for the client's sessions and, with `runtime` scope, the runtime's shared background traffic; only one connection may hold `runtime` scope. The registration ends when the connection closes.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> setProvider(KerberosAuthSetProviderParams params) {
        return caller.invoke("kerberosAuth.setProvider", params, Void.class);
    }

}
