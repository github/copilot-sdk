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
     * Starts a background agent task in the session.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionTasksStartAgentResult> startAgent(SessionTasksStartAgentParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.tasks.startAgent", _p, SessionTasksStartAgentResult.class);
    }

    /**
     * Lists background tasks tracked by the session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionTasksListResult> list() {
        return caller.invoke("session.tasks.list", java.util.Map.of("sessionId", this.sessionId), SessionTasksListResult.class);
    }

    /**
     * Registers a client-owned task, or reclaims an orphaned task belonging to the same extension principal.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionTasksRegisterResult> register(SessionTasksRegisterParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.tasks.register", _p, SessionTasksRegisterResult.class);
    }

    /**
     * Publishes generic progress or a terminal outcome for a client-owned task.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionTasksUpdateResult> update(SessionTasksUpdateParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.tasks.update", _p, SessionTasksUpdateResult.class);
    }

    /**
     * Refreshes metadata for any detached background shells the runtime knows about.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> refresh() {
        return caller.invoke("session.tasks.refresh", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

    /**
     * Waits for all in-flight background tasks and any follow-up turns to settle.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> waitForPending() {
        return caller.invoke("session.tasks.waitForPending", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

    /**
     * Returns progress information for a background task by ID.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionTasksGetProgressResult> getProgress(SessionTasksGetProgressParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.tasks.getProgress", _p, SessionTasksGetProgressResult.class);
    }

    /**
     * Returns the first sync-waiting task that can currently be promoted to background mode.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionTasksGetCurrentPromotableResult> getCurrentPromotable() {
        return caller.invoke("session.tasks.getCurrentPromotable", java.util.Map.of("sessionId", this.sessionId), SessionTasksGetCurrentPromotableResult.class);
    }

    /**
     * Promotes an eligible synchronously-waited task so it continues running in the background.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionTasksPromoteToBackgroundResult> promoteToBackground(SessionTasksPromoteToBackgroundParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.tasks.promoteToBackground", _p, SessionTasksPromoteToBackgroundResult.class);
    }

    /**
     * Atomically promotes the first promotable sync-waiting task to background mode and returns it.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionTasksPromoteCurrentToBackgroundResult> promoteCurrentToBackground() {
        return caller.invoke("session.tasks.promoteCurrentToBackground", java.util.Map.of("sessionId", this.sessionId), SessionTasksPromoteCurrentToBackgroundResult.class);
    }

    /**
     * Cancels a background task.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionTasksCancelResult> cancel(SessionTasksCancelParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.tasks.cancel", _p, SessionTasksCancelResult.class);
    }

    /**
     * Removes a completed or cancelled background task from tracking.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionTasksRemoveResult> remove(SessionTasksRemoveParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.tasks.remove", _p, SessionTasksRemoveResult.class);
    }

    /**
     * Sends a message to a background agent task.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionTasksSendMessageResult> sendMessage(SessionTasksSendMessageParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.tasks.sendMessage", _p, SessionTasksSendMessageResult.class);
    }

}
