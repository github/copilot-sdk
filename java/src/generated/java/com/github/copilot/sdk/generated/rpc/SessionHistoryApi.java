/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

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
     * Optional compaction parameters.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionHistoryCompactResult> compact() {
        return caller.invoke("session.history.compact", java.util.Map.of("sessionId", this.sessionId), SessionHistoryCompactResult.class);
    }

    /**
     * Identifier of the event to truncate to; this event and all later events are removed.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionHistoryTruncateResult> truncate(SessionHistoryTruncateParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.history.truncate", _p, SessionHistoryTruncateResult.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionHistoryCancelBackgroundCompactionResult> cancelBackgroundCompaction() {
        return caller.invoke("session.history.cancelBackgroundCompaction", java.util.Map.of("sessionId", this.sessionId), SessionHistoryCancelBackgroundCompactionResult.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionHistoryAbortManualCompactionResult> abortManualCompaction() {
        return caller.invoke("session.history.abortManualCompaction", java.util.Map.of("sessionId", this.sessionId), SessionHistoryAbortManualCompactionResult.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionHistorySummarizeForHandoffResult> summarizeForHandoff() {
        return caller.invoke("session.history.summarizeForHandoff", java.util.Map.of("sessionId", this.sessionId), SessionHistorySummarizeForHandoffResult.class);
    }

}
