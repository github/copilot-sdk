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
 * API methods for the {@code plugins.marketplaces} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionPluginsMarketplacesApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionPluginsMarketplacesApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Lists registered and enterprise-managed desired marketplaces using the live session's retained policy.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionPluginsMarketplacesListResult> list() {
        return caller.invoke("session.plugins.marketplaces.list", java.util.Map.of("sessionId", this.sessionId), SessionPluginsMarketplacesListResult.class);
    }

    /**
     * Adds a marketplace when permitted by the live session's retained managed policy.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionPluginsMarketplacesAddResult> add(SessionPluginsMarketplacesAddParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.plugins.marketplaces.add", _p, SessionPluginsMarketplacesAddResult.class);
    }

    /**
     * Removes a marketplace when permitted by the live session's retained managed policy.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionPluginsMarketplacesRemoveResult> remove(SessionPluginsMarketplacesRemoveParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.plugins.marketplaces.remove", _p, SessionPluginsMarketplacesRemoveResult.class);
    }

    /**
     * Browses a marketplace resolved through the live session's working directory and retained managed policy.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionPluginsMarketplacesBrowseResult> browse(SessionPluginsMarketplacesBrowseParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.plugins.marketplaces.browse", _p, SessionPluginsMarketplacesBrowseResult.class);
    }

    /**
     * Refreshes marketplaces resolved through the live session's working directory and retained managed policy.
     * <p>
     * Invokes the method with no params, applying the runtime defaults.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionPluginsMarketplacesRefreshResult> refresh() {
        return refresh(null);
    }

    /**
     * Refreshes marketplaces resolved through the live session's working directory and retained managed policy.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionPluginsMarketplacesRefreshResult> refresh(SessionPluginsMarketplacesRefreshParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = params == null ? MAPPER.createObjectNode() : MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.plugins.marketplaces.refresh", _p, SessionPluginsMarketplacesRefreshResult.class);
    }

}
