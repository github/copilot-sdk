/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code metadata} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionMetadataApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionMetadataApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionMetadataSnapshotResult> snapshot() {
        return caller.invoke("session.metadata.snapshot", java.util.Map.of("sessionId", this.sessionId), SessionMetadataSnapshotResult.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionMetadataIsProcessingResult> isProcessing() {
        return caller.invoke("session.metadata.isProcessing", java.util.Map.of("sessionId", this.sessionId), SessionMetadataIsProcessingResult.class);
    }

    /**
     * Model identifier and token limits used to compute the context-info breakdown.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionMetadataContextInfoResult> contextInfo(SessionMetadataContextInfoParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.metadata.contextInfo", _p, SessionMetadataContextInfoResult.class);
    }

    /**
     * Updated working-directory/git context to record on the session.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<Void> recordContextChange(SessionMetadataRecordContextChangeParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.metadata.recordContextChange", _p, Void.class);
    }

    /**
     * Absolute path to set as the session's new working directory.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionMetadataSetWorkingDirectoryResult> setWorkingDirectory(SessionMetadataSetWorkingDirectoryParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.metadata.setWorkingDirectory", _p, SessionMetadataSetWorkingDirectoryResult.class);
    }

    /**
     * Model identifier to use when re-tokenizing the session's existing messages.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionMetadataRecomputeContextTokensResult> recomputeContextTokens(SessionMetadataRecomputeContextTokensParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.metadata.recomputeContextTokens", _p, SessionMetadataRecomputeContextTokensResult.class);
    }

}
