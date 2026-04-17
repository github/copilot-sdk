/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code model} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionModelApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionModelApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Invokes {@code session.model.getCurrent}.
     * @since 1.0.0
     */
    public CompletableFuture<SessionModelGetCurrentResult> getCurrent() {
        return caller.invoke("session.model.getCurrent", java.util.Map.of("sessionId", this.sessionId), SessionModelGetCurrentResult.class);
    }

    /**
     * Invokes {@code session.model.switchTo}.
     * @since 1.0.0
     */
    public CompletableFuture<SessionModelSwitchToResult> switchTo(SessionModelSwitchToParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.model.switchTo", _p, SessionModelSwitchToResult.class);
    }

}
