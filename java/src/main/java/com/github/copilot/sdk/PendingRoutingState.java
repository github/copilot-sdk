/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import com.github.copilot.sdk.generated.SessionEvent;

/**
 * Thread-safe state for pending-routing mode used by
 * {@link CopilotClient#createCloudSession}.
 *
 * <p>
 * While one or more cloud {@code session.create} calls are in flight (guard
 * count {@code > 0}), notifications and inbound RPC requests addressed to
 * session ids that are not yet registered are buffered here rather than
 * dropped. Once {@link CopilotClient#createCloudSession} receives the
 * runtime-assigned session id, it calls {@link #registerAndFlush} to atomically
 * insert the session into the sessions map, drain any buffered events into it,
 * and complete any parked request waiters.
 *
 * <p>
 * All mutation methods synchronize on {@code this}. The sessions-map put inside
 * {@link #registerAndFlush} is performed while holding the lock so that the
 * {@link #tryBufferNotification} / {@link #tryParkRequest} check-then-act is
 * free of TOCTOU races.
 */
final class PendingRoutingState {

    static final int BUFFER_LIMIT = 128;

    private static final Logger LOG = Logger.getLogger(PendingRoutingState.class.getName());

    private int guardCount = 0;
    /** Buffered session.event notifications keyed by session id. */
    private final Map<String, ArrayDeque<SessionEvent>> pendingEvents = new HashMap<>();
    /**
     * Parked CompletableFutures for inbound RPC requests waiting for a session to
     * be registered.
     */
    private final Map<String, List<CompletableFuture<CopilotSession>>> pendingWaiters = new HashMap<>();

    /** Increment the guard count. Must be matched by {@link #decrementGuard}. */
    synchronized void incrementGuard() {
        guardCount++;
    }

    /**
     * Decrement the guard count. If the count reaches zero, clears all buffered
     * events and completes all parked request waiters exceptionally with a
     * canonical message that is distinct from the overflow-eviction path.
     */
    synchronized void decrementGuard() {
        guardCount = Math.max(0, guardCount - 1);
        if (guardCount != 0) {
            return;
        }
        pendingEvents.clear();
        var stale = new ArrayList<CompletableFuture<CopilotSession>>();
        for (var list : pendingWaiters.values()) {
            stale.addAll(list);
        }
        pendingWaiters.clear();
        if (!stale.isEmpty()) {
            // Use a distinct phrasing from the overflow-eviction path so that
            // debugging can tell the two failure modes apart. Matches the Rust
            // SDK message (PR #1394 commit e0ff254f) and the TS SDK (commit
            // c167bc3e).
            LOG.warning("Pending session routing ended before session was registered; " + "completing " + stale.size()
                    + " parked request waiter(s) exceptionally");
            var ex = new RuntimeException("pending session routing ended before session was registered");
            for (var waiter : stale) {
                waiter.completeExceptionally(ex);
            }
        }
    }

    /**
     * Attempt to buffer a {@code session.event} notification for a pending session.
     *
     * <p>
     * The {@code sessions} map is checked inside this synchronized method so that
     * the "session not found → buffer" decision is atomic with
     * {@link #registerAndFlush}'s "put in map → flush buffer" operation.
     *
     * @param sessionId
     *            the session id from the notification
     * @param event
     *            the parsed event to buffer
     * @param sessions
     *            the live sessions map (checked under lock)
     * @return {@code true} if the event was buffered; {@code false} if the session
     *         is already registered (caller should dispatch directly) or pending
     *         routing is inactive (caller should drop)
     */
    synchronized boolean tryBufferNotification(String sessionId, SessionEvent event,
            Map<String, CopilotSession> sessions) {
        if (sessions.containsKey(sessionId)) {
            return false; // session found; caller dispatches directly
        }
        if (guardCount == 0) {
            return false; // no pending routing; drop
        }
        var queue = pendingEvents.computeIfAbsent(sessionId, k -> new ArrayDeque<>());
        if (queue.size() >= BUFFER_LIMIT) {
            queue.pollFirst();
            LOG.warning("Pending session notification buffer full for session " + sessionId + "; dropping oldest");
        }
        queue.addLast(event);
        return true;
    }

    /**
     * Attempt to park an inbound RPC request until the session is registered.
     *
     * <p>
     * Like {@link #tryBufferNotification}, the {@code sessions} map is checked
     * under the lock to avoid TOCTOU races with {@link #registerAndFlush}.
     *
     * @param sessionId
     *            the session id from the request params
     * @param sessions
     *            the live sessions map (checked under lock)
     * @return a future that will be resolved with the {@link CopilotSession} when
     *         registered (or completed exceptionally when the guard is dropped), or
     *         {@code null} if the session is already registered (callers should use
     *         it directly) or if pending routing is inactive (caller should send
     *         error)
     */
    synchronized CompletableFuture<CopilotSession> tryParkRequest(String sessionId,
            Map<String, CopilotSession> sessions) {
        CopilotSession existing = sessions.get(sessionId);
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }
        if (guardCount == 0) {
            return null; // no pending; caller sends error
        }
        var future = new CompletableFuture<CopilotSession>();
        var list = pendingWaiters.computeIfAbsent(sessionId, k -> new ArrayList<>());
        if (list.size() >= BUFFER_LIMIT) {
            // Cap parked waiters per session. When exceeded, evict the oldest
            // and complete it with a distinct overflow message so the runtime
            // gets an error response rather than hanging on a reply that will
            // never arrive. Matches Rust PR #1394 (commit 491b4427) and TS
            // (commit c167bc3e).
            var oldest = list.remove(0);
            LOG.warning("Pending session request waiter buffer full for session " + sessionId + " (limit="
                    + BUFFER_LIMIT + "); evicting oldest request");
            oldest.completeExceptionally(new RuntimeException("pending session buffer overflow"));
        }
        list.add(future);
        return future;
    }

    /**
     * Result of {@link #registerAndFlush}: buffered events to dispatch and parked
     * waiters to complete.
     */
    record FlushResult(List<SessionEvent> events, List<CompletableFuture<CopilotSession>> waiters) {
    }

    /**
     * Atomically register a session in the sessions map and drain any buffered
     * events and parked waiters for that session.
     *
     * <p>
     * The {@code sessions.put} is performed inside the lock so that concurrent
     * {@link #tryBufferNotification} / {@link #tryParkRequest} callers that haven't
     * yet acquired the lock will see the session as registered.
     *
     * @param sessionId
     *            the session id to register
     * @param session
     *            the session object
     * @param sessions
     *            the live sessions map; the put is performed under lock
     * @return buffered events and parked waiters to dispatch/complete outside the
     *         lock
     */
    synchronized FlushResult registerAndFlush(String sessionId, CopilotSession session,
            Map<String, CopilotSession> sessions) {
        sessions.put(sessionId, session);

        var queue = pendingEvents.remove(sessionId);
        var events = queue != null ? new ArrayList<>(queue) : Collections.<SessionEvent>emptyList();

        var waiters = pendingWaiters.remove(sessionId);
        var futures = waiters != null
                ? new ArrayList<>(waiters)
                : Collections.<CompletableFuture<CopilotSession>>emptyList();

        return new FlushResult(events, futures);
    }
}
