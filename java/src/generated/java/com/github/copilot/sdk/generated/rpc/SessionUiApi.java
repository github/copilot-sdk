/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code ui} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionUiApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionUiApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Invokes {@code session.ui.elicitation}.
     * @since 1.0.0
     */
    public CompletableFuture<SessionUiElicitationResult> elicitation(SessionUiElicitationParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.ui.elicitation", _p, SessionUiElicitationResult.class);
    }

    /**
     * Invokes {@code session.ui.handlePendingElicitation}.
     * @since 1.0.0
     */
    public CompletableFuture<SessionUiHandlePendingElicitationResult> handlePendingElicitation(SessionUiHandlePendingElicitationParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.ui.handlePendingElicitation", _p, SessionUiHandlePendingElicitationResult.class);
    }

}
