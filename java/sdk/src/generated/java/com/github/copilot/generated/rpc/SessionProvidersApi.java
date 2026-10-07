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
 * API methods for the {@code providers} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionProvidersApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** API methods for the {@code providers.models} sub-namespace. */
    public final SessionProvidersModelsApi models;

    /** @param caller the RPC transport function */
    SessionProvidersApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
        this.models = new SessionProvidersModelsApi(caller, sessionId);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionProvidersGetCatalogResult> getCatalog() {
        return caller.invoke("session.providers.getCatalog", java.util.Map.of("sessionId", this.sessionId), SessionProvidersGetCatalogResult.class);
    }

    /**
     * Provider discovery parameters.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionProvidersDiscoverResult> discover(SessionProvidersDiscoverParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.providers.discover", _p, SessionProvidersDiscoverResult.class);
    }

    /**
     * Provider status request parameters.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionProvidersGetStatusResult> getStatus(SessionProvidersGetStatusParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.providers.getStatus", _p, SessionProvidersGetStatusResult.class);
    }

}
