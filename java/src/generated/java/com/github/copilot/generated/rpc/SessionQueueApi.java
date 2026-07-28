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
 * API methods for the {@code queue} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionQueueApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionQueueApi(RpcCaller caller, String sessionId) {
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
    public CompletableFuture<SessionQueuePendingItemsResult> pendingItems() {
        return caller.invoke("session.queue.pendingItems", java.util.Map.of("sessionId", this.sessionId), SessionQueuePendingItemsResult.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionQueueSnapshotResult> snapshot() {
        return caller.invoke("session.queue.snapshot", java.util.Map.of("sessionId", this.sessionId), SessionQueueSnapshotResult.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionQueueHasPendingResult> hasPending() {
        return caller.invoke("session.queue.hasPending", java.util.Map.of("sessionId", this.sessionId), SessionQueueHasPendingResult.class);
    }

    /**
     * Inputs for starting a deferred-idle drain.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionQueueBeginDeferredIdleDrainResult> beginDeferredIdleDrain(SessionQueueBeginDeferredIdleDrainParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.queue.beginDeferredIdleDrain", _p, SessionQueueBeginDeferredIdleDrainResult.class);
    }

    /**
     * Inputs for completing a deferred-idle drain.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionQueueFinishDeferredIdleDrainResult> finishDeferredIdleDrain(SessionQueueFinishDeferredIdleDrainParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.queue.finishDeferredIdleDrain", _p, SessionQueueFinishDeferredIdleDrainResult.class);
    }

    /**
     * Inputs for marking session.idle deferred in native state.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> deferSessionIdle(SessionQueueDeferSessionIdleParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.queue.deferSessionIdle", _p, Void.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionQueueRemoveMostRecentResult> removeMostRecent() {
        return caller.invoke("session.queue.removeMostRecent", java.util.Map.of("sessionId", this.sessionId), SessionQueueRemoveMostRecentResult.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> clear() {
        return caller.invoke("session.queue.clear", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

    /**
     * Internal filter for consuming queued system notifications.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionQueueConsumeSystemNotificationsResult> consumeSystemNotifications(SessionQueueConsumeSystemNotificationsParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.queue.consumeSystemNotifications", _p, SessionQueueConsumeSystemNotificationsResult.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionQueueEnqueueResumePendingResult> enqueueResumePending() {
        return caller.invoke("session.queue.enqueueResumePending", java.util.Map.of("sessionId", this.sessionId), SessionQueueEnqueueResumePendingResult.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> process() {
        return caller.invoke("session.queue.process", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

}
