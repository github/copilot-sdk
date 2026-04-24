/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code auth} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionAuthApi {

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionAuthApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Invokes {@code session.auth.getStatus}.
     * @since 1.0.0
     */
    public CompletableFuture<SessionAuthGetStatusResult> getStatus() {
        return caller.invoke("session.auth.getStatus", java.util.Map.of("sessionId", this.sessionId), SessionAuthGetStatusResult.class);
    }

}
