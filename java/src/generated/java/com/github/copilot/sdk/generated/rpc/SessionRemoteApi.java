/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code remote} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionRemoteApi {

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionRemoteApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Invokes {@code session.remote.enable}.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionRemoteEnableResult> enable() {
        return caller.invoke("session.remote.enable", java.util.Map.of("sessionId", this.sessionId), SessionRemoteEnableResult.class);
    }

    /**
     * Invokes {@code session.remote.disable}.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<Void> disable() {
        return caller.invoke("session.remote.disable", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

}
