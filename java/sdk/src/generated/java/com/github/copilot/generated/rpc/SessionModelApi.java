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
     * Gets the session's authoritative model snapshot, including the committed Auto preference and any newer unclaimed Auto preference waiting for a future user turn.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionModelGetCurrentResult> getCurrent() {
        return caller.invoke("session.model.getCurrent", java.util.Map.of("sessionId", this.sessionId), SessionModelGetCurrentResult.class);
    }

    /**
     * Switches the session to a model and optional reasoning configuration.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionModelSwitchToResult> switchTo(SessionModelSwitchToParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.model.switchTo", _p, SessionModelSwitchToResult.class);
    }

    /**
     * Requests an Auto preference change without changing the session's selected model. The latest unclaimed request wins; the runtime commits it only after a later prompt using the `auto` model mints a usable model and token pair. A `pending` response confirms that the request was accepted, not that it committed. Observe eventual success through `session.model_change`, failure through the ephemeral `session.auto_tier_switch_failed` event, or current unclaimed state through `session.model.getCurrent`.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionModelSwitchAutoTierResult> switchAutoTier(SessionModelSwitchAutoTierParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.model.switchAutoTier", _p, SessionModelSwitchAutoTierResult.class);
    }

    /**
     * Resolves and applies organization-managed and repository model overlays.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<ModelSwitchToResult> applyStartupOverlay(SessionModelApplyStartupOverlayParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.model.applyStartupOverlay", _p, ModelSwitchToResult.class);
    }

    /**
     * Replaces or clears the host-supplied model allowlist for a running session.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionModelSetAllowedModelsResult> setAllowedModels(SessionModelSetAllowedModelsParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.model.setAllowedModels", _p, SessionModelSetAllowedModelsResult.class);
    }

    /**
     * Updates the session's reasoning effort without changing the selected model.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionModelSetReasoningEffortResult> setReasoningEffort(SessionModelSetReasoningEffortParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.model.setReasoningEffort", _p, SessionModelSetReasoningEffortResult.class);
    }

    /**
     * Lists models available to this session using its own auth and integration context. Connected hosts (CLI TUI, GitHub App) should call this through the session client so remote sessions return the remote CLI's available models rather than the caller's.
     * <p>
     * Invokes the method with no params, applying the runtime defaults.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionModelListResult> list() {
        return list(null);
    }

    /**
     * Lists models available to this session using its own auth and integration context. Connected hosts (CLI TUI, GitHub App) should call this through the session client so remote sessions return the remote CLI's available models rather than the caller's.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionModelListResult> list(SessionModelListParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = params == null ? MAPPER.createObjectNode() : MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.model.list", _p, SessionModelListResult.class);
    }

}
