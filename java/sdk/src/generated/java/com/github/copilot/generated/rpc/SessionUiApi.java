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
     * Runs a transient no-tools model query against the current conversation context.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionUiEphemeralQueryResult> ephemeralQuery(SessionUiEphemeralQueryParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.ui.ephemeralQuery", _p, SessionUiEphemeralQueryResult.class);
    }

    /**
     * Requests structured input from a UI-capable client.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionUiElicitationResult> elicitation(SessionUiElicitationParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.ui.elicitation", _p, SessionUiElicitationResult.class);
    }

    /**
     * Provides the user response for a pending elicitation request.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionUiHandlePendingElicitationResult> handlePendingElicitation(SessionUiHandlePendingElicitationParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.ui.handlePendingElicitation", _p, SessionUiHandlePendingElicitationResult.class);
    }

    /**
     * Resolves a pending elicitation request after direct interaction in the trusted in-process client. Only an accepted response to the built-in ask_user tool can become trusted human evidence.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UIElicitationResult> handleHumanAskUser(SessionUiHandleHumanAskUserParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.ui.handleHumanAskUser", _p, UIElicitationResult.class);
    }

    /**
     * Resolves a pending `user_input.requested` event with the user's response.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionUiHandlePendingUserInputResult> handlePendingUserInput(SessionUiHandlePendingUserInputParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.ui.handlePendingUserInput", _p, SessionUiHandlePendingUserInputResult.class);
    }

    /**
     * Resolves a pending `user_input.requested` event after direct interaction in the trusted in-process client.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UIHandlePendingResult> handleHumanUserInput(SessionUiHandleHumanUserInputParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.ui.handleHumanUserInput", _p, UIHandlePendingResult.class);
    }

    /**
     * Resolves a pending `sampling.requested` event with a sampling result, or rejects it.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionUiHandlePendingSamplingResult> handlePendingSampling(SessionUiHandlePendingSamplingParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.ui.handlePendingSampling", _p, SessionUiHandlePendingSamplingResult.class);
    }

    /**
     * Resolves a pending `auto_mode_switch.requested` event with the user's accept/decline decision.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionUiHandlePendingAutoModeSwitchResult> handlePendingAutoModeSwitch(SessionUiHandlePendingAutoModeSwitchParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.ui.handlePendingAutoModeSwitch", _p, SessionUiHandlePendingAutoModeSwitchResult.class);
    }

    /**
     * Resolves a pending `session_limits_exhausted.requested` event with the user's selected limit action.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionUiHandlePendingSessionLimitsExhaustedResult> handlePendingSessionLimitsExhausted(SessionUiHandlePendingSessionLimitsExhaustedParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.ui.handlePendingSessionLimitsExhausted", _p, SessionUiHandlePendingSessionLimitsExhaustedResult.class);
    }

    /**
     * Resolves a pending `exit_plan_mode.requested` event with the user's response.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionUiHandlePendingExitPlanModeResult> handlePendingExitPlanMode(SessionUiHandlePendingExitPlanModeParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.ui.handlePendingExitPlanMode", _p, SessionUiHandlePendingExitPlanModeResult.class);
    }

    /**
     * Resolves a pending `exit_plan_mode.requested` event after direct interaction in the trusted in-process client.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UIHandlePendingResult> handleHumanExitPlanMode(SessionUiHandleHumanExitPlanModeParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.ui.handleHumanExitPlanMode", _p, UIHandlePendingResult.class);
    }

    /**
     * Registers an in-process handler for auto-mode-switch requests so the server bridge skips dispatch.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionUiRegisterDirectAutoModeSwitchHandlerResult> registerDirectAutoModeSwitchHandler() {
        return caller.invoke("session.ui.registerDirectAutoModeSwitchHandler", java.util.Map.of("sessionId", this.sessionId), SessionUiRegisterDirectAutoModeSwitchHandlerResult.class);
    }

    /**
     * Unregisters a previously-registered in-process auto-mode-switch handler by its opaque handle.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionUiUnregisterDirectAutoModeSwitchHandlerResult> unregisterDirectAutoModeSwitchHandler(SessionUiUnregisterDirectAutoModeSwitchHandlerParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.ui.unregisterDirectAutoModeSwitchHandler", _p, SessionUiUnregisterDirectAutoModeSwitchHandlerResult.class);
    }

}
