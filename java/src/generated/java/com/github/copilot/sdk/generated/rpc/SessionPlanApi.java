/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code plan} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionPlanApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionPlanApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Invokes {@code session.plan.read}.
     * @since 1.0.0
     */
    public CompletableFuture<SessionPlanReadResult> read() {
        return caller.invoke("session.plan.read", java.util.Map.of("sessionId", this.sessionId), SessionPlanReadResult.class);
    }

    /**
     * Invokes {@code session.plan.update}.
     * @since 1.0.0
     */
    public CompletableFuture<Void> update(SessionPlanUpdateParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.plan.update", _p, Void.class);
    }

    /**
     * Invokes {@code session.plan.delete}.
     * @since 1.0.0
     */
    public CompletableFuture<Void> delete() {
        return caller.invoke("session.plan.delete", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

}
