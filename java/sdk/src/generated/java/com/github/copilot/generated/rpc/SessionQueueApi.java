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
     * Returns the local session's pending user-facing queued items and steering messages.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionQueuePendingItemsResult> pendingItems() {
        return caller.invoke("session.queue.pendingItems", java.util.Map.of("sessionId", this.sessionId), SessionQueuePendingItemsResult.class);
    }

    /**
     * Returns the internal native queue snapshot for in-process session orchestration.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<QueueSnapshotResult> snapshot() {
        return caller.invoke("session.queue.snapshot", java.util.Map.of("sessionId", this.sessionId), QueueSnapshotResult.class);
    }

    /**
     * Moves an addressable queued item to a public visible position.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionQueueMoveItemResult> moveItem(SessionQueueMoveItemParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.queue.moveItem", _p, SessionQueueMoveItemResult.class);
    }

    /**
     * Inserts a new queued message at a public visible position.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionQueueInsertAtResult> insertAt(SessionQueueInsertAtParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.queue.insertAt", _p, SessionQueueInsertAtResult.class);
    }

    /**
     * Removes an addressable queued item by its stable id.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionQueueRemoveAtResult> removeAt(SessionQueueRemoveAtParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.queue.removeAt", _p, SessionQueueRemoveAtResult.class);
    }

    /**
     * Updates the text of an addressable single-message queue item.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionQueueUpdateTextResult> updateText(SessionQueueUpdateTextParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.queue.updateText", _p, SessionQueueUpdateTextResult.class);
    }

    /**
     * Atomically withdraws an unchanged user message of a local session: from the queued or steering lane while unconsumed, or from the running turn it started while the model has not answered it and nothing the user sent after it is pending. Withdrawing from the running turn interrupts that turn and removes its events from history. A client retaining the original draft may restore it only when removed is true.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionQueueWithdrawMessageResult> withdrawMessage(SessionQueueWithdrawMessageParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.queue.withdrawMessage", _p, SessionQueueWithdrawMessageResult.class);
    }

    /**
     * Atomically appends text and attachments to an unchanged, unconsumed local steering message. Returns updated=false if delivery or withdrawal already claimed the message.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionQueueAppendSteeringResult> appendSteering(SessionQueueAppendSteeringParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.queue.appendSteering", _p, SessionQueueAppendSteeringResult.class);
    }

    /**
     * Duplicates an addressable queued item immediately after its source.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionQueueDuplicateAtResult> duplicateAt(SessionQueueDuplicateAtParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.queue.duplicateAt", _p, SessionQueueDuplicateAtResult.class);
    }

    /**
     * Acquires or releases the queued-lane drain pause.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> setDrainPaused(SessionQueueSetDrainPausedParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.queue.setDrainPaused", _p, Void.class);
    }

    /**
     * Moves an addressable queued message into the live turn's steering lane.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionQueueSendNowResult> sendNow(SessionQueueSendNowParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.queue.sendNow", _p, SessionQueueSendNowResult.class);
    }

    /**
     * Reports whether the local session has native queued work pending.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<QueueHasPendingResult> hasPending() {
        return caller.invoke("session.queue.hasPending", java.util.Map.of("sessionId", this.sessionId), QueueHasPendingResult.class);
    }

    /**
     * Begins a native deferred-idle drain when background work has quiesced.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<QueueBeginDeferredIdleDrainResult> beginDeferredIdleDrain(SessionQueueBeginDeferredIdleDrainParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.queue.beginDeferredIdleDrain", _p, QueueBeginDeferredIdleDrainResult.class);
    }

    /**
     * Finishes a native deferred-idle drain and reports whether to drain queue work or emit idle.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<QueueFinishDeferredIdleDrainResult> finishDeferredIdleDrain(SessionQueueFinishDeferredIdleDrainParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.queue.finishDeferredIdleDrain", _p, QueueFinishDeferredIdleDrainResult.class);
    }

    /**
     * Marks session.idle as deferred by native background work state.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> deferSessionIdle(SessionQueueDeferSessionIdleParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.queue.deferSessionIdle", _p, Void.class);
    }

    /**
     * Removes the most recently queued user-facing item (LIFO).
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionQueueRemoveMostRecentResult> removeMostRecent() {
        return caller.invoke("session.queue.removeMostRecent", java.util.Map.of("sessionId", this.sessionId), SessionQueueRemoveMostRecentResult.class);
    }

    /**
     * Clears all pending queued items on the local session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> clear() {
        return caller.invoke("session.queue.clear", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

    /**
     * Consumes queued native system notifications matching an internal filter.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<QueueRemoveMostRecentResult> consumeSystemNotifications(SessionQueueConsumeSystemNotificationsParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.queue.consumeSystemNotifications", _p, QueueRemoveMostRecentResult.class);
    }

    /**
     * Enqueues the internal resume-pending wake item when orphan handling needs a follow-up turn.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<QueueEnqueueResumePendingResult> enqueueResumePending() {
        return caller.invoke("session.queue.enqueueResumePending", java.util.Map.of("sessionId", this.sessionId), QueueEnqueueResumePendingResult.class);
    }

    /**
     * Drains the native local-session work queue for in-process session orchestration.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> process() {
        return caller.invoke("session.queue.process", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

}
