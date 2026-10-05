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
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionModelGetCurrentResult> getCurrent() {
        return caller.invoke("session.model.getCurrent", java.util.Map.of("sessionId", this.sessionId), SessionModelGetCurrentResult.class);
    }

    /**
     * Target model identifier and optional reasoning effort, summary, capability overrides, and context tier.
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
     * An Auto preference request for the session. This updates Auto configuration only; it does not change the selected model to `auto`.
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
     * Managed, repository, and CLI model overrides to overlay onto the session at startup.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionModelApplyStartupOverlayResult> applyStartupOverlay(SessionModelApplyStartupOverlayParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.model.applyStartupOverlay", _p, SessionModelApplyStartupOverlayResult.class);
    }

    /**
     * Host-supplied exact model selection IDs to allow for this running session. CAPI IDs are intersected with repository `.github/allowed_models.txt` policy; provider-qualified IDs remain exempt from repository-only policy but are restricted by this host list. Omit or pass null to clear the host restriction; an explicit empty or disjoint list is rejected. Validation and pre-selection fallback failures preserve the previous restriction. Failures after a fallback selection commits retain the new restriction and selected model; callers should inspect current session state after such an error.
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
     * Reasoning effort level to apply to the currently selected model.
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
     * Optional listing options.
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
     * Optional listing options.
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
