/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code tasks} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionTasksApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionTasksApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Agent type, prompt, name, and optional description and model override for the new task.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionTasksStartAgentResult> startAgent(SessionTasksStartAgentParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.tasks.startAgent", _p, SessionTasksStartAgentResult.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionTasksListResult> list() {
        return caller.invoke("session.tasks.list", java.util.Map.of("sessionId", this.sessionId), SessionTasksListResult.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<Void> refresh() {
        return caller.invoke("session.tasks.refresh", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<Void> waitForPending() {
        return caller.invoke("session.tasks.waitForPending", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

    /**
     * Identifier of the background task to fetch progress for.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionTasksGetProgressResult> getProgress(SessionTasksGetProgressParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.tasks.getProgress", _p, SessionTasksGetProgressResult.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionTasksGetCurrentPromotableResult> getCurrentPromotable() {
        return caller.invoke("session.tasks.getCurrentPromotable", java.util.Map.of("sessionId", this.sessionId), SessionTasksGetCurrentPromotableResult.class);
    }

    /**
     * Identifier of the task to promote to background mode.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionTasksPromoteToBackgroundResult> promoteToBackground(SessionTasksPromoteToBackgroundParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.tasks.promoteToBackground", _p, SessionTasksPromoteToBackgroundResult.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionTasksPromoteCurrentToBackgroundResult> promoteCurrentToBackground() {
        return caller.invoke("session.tasks.promoteCurrentToBackground", java.util.Map.of("sessionId", this.sessionId), SessionTasksPromoteCurrentToBackgroundResult.class);
    }

    /**
     * Identifier of the background task to cancel.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionTasksCancelResult> cancel(SessionTasksCancelParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.tasks.cancel", _p, SessionTasksCancelResult.class);
    }

    /**
     * Identifier of the completed or cancelled task to remove from tracking.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionTasksRemoveResult> remove(SessionTasksRemoveParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.tasks.remove", _p, SessionTasksRemoveResult.class);
    }

    /**
     * Identifier of the target agent task, message content, and optional sender agent ID.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionTasksSendMessageResult> sendMessage(SessionTasksSendMessageParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.tasks.sendMessage", _p, SessionTasksSendMessageResult.class);
    }

}
