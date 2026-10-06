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
 * API methods for the {@code provider} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionProviderApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionProviderApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Returns the provider endpoint and credentials the session is currently configured to talk to, so the caller can make inference calls directly against the same backend the session uses.
     * <p>
     * Invokes the method with no params, applying the runtime defaults.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionProviderGetEndpointResult> getEndpoint() {
        return getEndpoint(null);
    }

    /**
     * Returns the provider endpoint and credentials the session is currently configured to talk to, so the caller can make inference calls directly against the same backend the session uses.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionProviderGetEndpointResult> getEndpoint(SessionProviderGetEndpointParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = params == null ? MAPPER.createObjectNode() : MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.provider.getEndpoint", _p, SessionProviderGetEndpointResult.class);
    }

    /**
     * Adds BYOK providers and/or models to the session's registry at runtime, extending the additive registry built from the session's `providers`/`models` options. Both fields are optional, so a call may add providers only, models only, or both. Within a single call providers are registered before models, so a model may reference a provider added in the same call; across calls a model may reference any provider already registered (from session creation or a prior add). A model whose referenced provider is not registered by the end of the call is rejected. Newly added models become selectable via `model.list` / `model.switchTo` and are inherited by sub-agents spawned afterwards.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionProviderAddResult> add(SessionProviderAddParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.provider.add", _p, SessionProviderAddResult.class);
    }

    /**
     * Atomically updates the session's BYOK provider and model registry by applying the supplied snapshot, replacing existing entries, updating models, or removing entries absent from the snapshot.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionProviderSyncResult> sync(SessionProviderSyncParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.provider.sync", _p, SessionProviderSyncResult.class);
    }

    /**
     * Withdraws named host-managed models from the session's BYOK registry, leaving every other entry untouched. The scoped counterpart to `provider.sync`: a snapshot can only describe entries the caller knows about, so using it to remove one model silently withdraws rows registered by another source, such as a plugin calling `provider.add` at runtime. Naming what to remove leaves unrelated entries alone. Selection ids that are not registered are ignored, so withdrawal is idempotent. A provider is removed only when one of the withdrawn models was the last entry referencing it; a provider that simply has no models, which is the normal state while its rows are supplied by catalog discovery, is left in place.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionProviderWithdrawResult> withdraw(SessionProviderWithdrawParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.provider.withdraw", _p, SessionProviderWithdrawResult.class);
    }

}
