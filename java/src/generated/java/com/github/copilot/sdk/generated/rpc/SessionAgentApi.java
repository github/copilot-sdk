/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code agent} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionAgentApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionAgentApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Invokes {@code session.agent.list}.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionAgentListResult> list() {
        return caller.invoke("session.agent.list", java.util.Map.of("sessionId", this.sessionId), SessionAgentListResult.class);
    }

    /**
     * Invokes {@code session.agent.getCurrent}.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionAgentGetCurrentResult> getCurrent() {
        return caller.invoke("session.agent.getCurrent", java.util.Map.of("sessionId", this.sessionId), SessionAgentGetCurrentResult.class);
    }

    /**
     * Invokes {@code session.agent.select}.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionAgentSelectResult> select(SessionAgentSelectParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.agent.select", _p, SessionAgentSelectResult.class);
    }

    /**
     * Invokes {@code session.agent.deselect}.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<Void> deselect() {
        return caller.invoke("session.agent.deselect", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

    /**
     * Invokes {@code session.agent.reload}.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionAgentReloadResult> reload() {
        return caller.invoke("session.agent.reload", java.util.Map.of("sessionId", this.sessionId), SessionAgentReloadResult.class);
    }

}
