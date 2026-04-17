/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code sessionFs} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerSessionFsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerSessionFsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Invokes {@code sessionFs.setProvider}.
     * @since 1.0.0
     */
    public CompletableFuture<SessionFsSetProviderResult> setProvider(SessionFsSetProviderParams params) {
        return caller.invoke("sessionFs.setProvider", params, SessionFsSetProviderResult.class);
    }

}
