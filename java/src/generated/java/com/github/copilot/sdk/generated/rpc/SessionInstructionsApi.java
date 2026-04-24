/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code instructions} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionInstructionsApi {

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionInstructionsApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Invokes {@code session.instructions.getSources}.
     * @since 1.0.0
     */
    public CompletableFuture<SessionInstructionsGetSourcesResult> getSources() {
        return caller.invoke("session.instructions.getSources", java.util.Map.of("sessionId", this.sessionId), SessionInstructionsGetSourcesResult.class);
    }

}
