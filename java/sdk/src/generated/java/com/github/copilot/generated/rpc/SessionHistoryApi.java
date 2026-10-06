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
 * API methods for the {@code history} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionHistoryApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionHistoryApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Compacts the session history to reduce context usage.
     * <p>
     * Invokes the method with no params, applying the runtime defaults.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionHistoryCompactResult> compact() {
        return compact(null);
    }

    /**
     * Compacts the session history to reduce context usage.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionHistoryCompactResult> compact(SessionHistoryCompactParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = params == null ? MAPPER.createObjectNode() : MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.history.compact", _p, SessionHistoryCompactResult.class);
    }

    /**
     * Truncates persisted session history to a specific event.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionHistoryTruncateResult> truncate(SessionHistoryTruncateParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.history.truncate", _p, SessionHistoryTruncateResult.class);
    }

    /**
     * Lists the user turns that the session can rewind to. Never rejects for a busy session: rewind reads need the session's file-change captures to be settled, so a session that still holds active work answers with `unavailableReason: "session-busy"` and no points, which the caller can retry.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionHistoryListRewindPointsResult> listRewindPoints() {
        return caller.invoke("session.history.listRewindPoints", java.util.Map.of("sessionId", this.sessionId), SessionHistoryListRewindPointsResult.class);
    }

    /**
     * Previews the files that a conversation-and-files rewind would restore.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionHistoryPreviewRewindResult> previewRewind(SessionHistoryPreviewRewindParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.history.previewRewind", _p, SessionHistoryPreviewRewindResult.class);
    }

    /**
     * Rewinds the session conversation, optionally restoring files changed by the discarded turns. Not crash-atomic: file restore and conversation truncation are separate stores, applied in that order, so a process crash between them can leave the workspace rewound while the conversation still contains the discarded turns. There is no recovery journal; re-running the same rewind is the recovery path for a crash before truncation lands, since file restore is idempotent (already-restored files are reported as skipped) and truncation is re-derived from the still-retained boundary event. After truncation lands that boundary no longer exists, so the same request is rejected; the only stage that can still be outstanding is snapshot pruning, whose failure leaves orphan snapshots the capture store tolerates. The reverse inconsistency cannot occur, because truncation is never applied before file restore succeeds.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionHistoryRewindResult> rewind(SessionHistoryRewindParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.history.rewind", _p, SessionHistoryRewindResult.class);
    }

    /**
     * Cancels any in-progress background compaction on a local session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionHistoryCancelBackgroundCompactionResult> cancelBackgroundCompaction() {
        return caller.invoke("session.history.cancelBackgroundCompaction", java.util.Map.of("sessionId", this.sessionId), SessionHistoryCancelBackgroundCompactionResult.class);
    }

    /**
     * Aborts any in-progress manual compaction on a local session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionHistoryAbortManualCompactionResult> abortManualCompaction() {
        return caller.invoke("session.history.abortManualCompaction", java.util.Map.of("sessionId", this.sessionId), SessionHistoryAbortManualCompactionResult.class);
    }

    /**
     * Produces a markdown summary of the session's conversation context for hand-off scenarios.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionHistorySummarizeForHandoffResult> summarizeForHandoff() {
        return caller.invoke("session.history.summarizeForHandoff", java.util.Map.of("sessionId", this.sessionId), SessionHistorySummarizeForHandoffResult.class);
    }

    /**
     * Clears the session's conversation history, keeping only system and developer messages, and seeds the fresh context window with a first user message. Must be called from inside a tool handler: the clear has to drop the results of the tool calls its wipe orphans, and it rejects when no tool call is in flight.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionHistoryClearContextResult> clearContext(SessionHistoryClearContextParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.history.clearContext", _p, SessionHistoryClearContextResult.class);
    }

}
