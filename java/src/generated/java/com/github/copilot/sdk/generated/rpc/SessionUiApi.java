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
     * Prompt message and JSON schema describing the form fields to elicit from the user.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionUiElicitationResult> elicitation(SessionUiElicitationParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.ui.elicitation", _p, SessionUiElicitationResult.class);
    }

    /**
     * Pending elicitation request ID and the user's response (accept/decline/cancel + form values).
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionUiHandlePendingElicitationResult> handlePendingElicitation(SessionUiHandlePendingElicitationParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.ui.handlePendingElicitation", _p, SessionUiHandlePendingElicitationResult.class);
    }

    /**
     * Request ID of a pending `user_input.requested` event and the user's response.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionUiHandlePendingUserInputResult> handlePendingUserInput(SessionUiHandlePendingUserInputParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.ui.handlePendingUserInput", _p, SessionUiHandlePendingUserInputResult.class);
    }

    /**
     * Request ID of a pending `sampling.requested` event and an optional sampling result payload (omit to reject).
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionUiHandlePendingSamplingResult> handlePendingSampling(SessionUiHandlePendingSamplingParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.ui.handlePendingSampling", _p, SessionUiHandlePendingSamplingResult.class);
    }

    /**
     * Request ID of a pending `auto_mode_switch.requested` event and the user's response.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionUiHandlePendingAutoModeSwitchResult> handlePendingAutoModeSwitch(SessionUiHandlePendingAutoModeSwitchParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.ui.handlePendingAutoModeSwitch", _p, SessionUiHandlePendingAutoModeSwitchResult.class);
    }

    /**
     * Request ID of a pending `exit_plan_mode.requested` event and the user's response.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionUiHandlePendingExitPlanModeResult> handlePendingExitPlanMode(SessionUiHandlePendingExitPlanModeParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.ui.handlePendingExitPlanMode", _p, SessionUiHandlePendingExitPlanModeResult.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionUiRegisterDirectAutoModeSwitchHandlerResult> registerDirectAutoModeSwitchHandler() {
        return caller.invoke("session.ui.registerDirectAutoModeSwitchHandler", java.util.Map.of("sessionId", this.sessionId), SessionUiRegisterDirectAutoModeSwitchHandlerResult.class);
    }

    /**
     * Opaque handle previously returned by `registerDirectAutoModeSwitchHandler` to release.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionUiUnregisterDirectAutoModeSwitchHandlerResult> unregisterDirectAutoModeSwitchHandler(SessionUiUnregisterDirectAutoModeSwitchHandlerParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.ui.unregisterDirectAutoModeSwitchHandler", _p, SessionUiUnregisterDirectAutoModeSwitchHandlerResult.class);
    }

}
