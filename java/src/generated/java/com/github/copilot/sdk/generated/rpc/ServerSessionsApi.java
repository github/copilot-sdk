/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code sessions} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerSessionsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerSessionsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Source session identifier to fork from, optional event-ID boundary, and optional friendly name for the new session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsForkResult> fork(SessionsForkParams params) {
        return caller.invoke("sessions.fork", params, SessionsForkResult.class);
    }

    /**
     * Remote session connection parameters.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsConnectResult> connect() {
        return caller.invoke("sessions.connect", java.util.Map.of(), SessionsConnectResult.class);
    }

}
