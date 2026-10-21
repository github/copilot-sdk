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
     * Returns a snapshot of the session's identifying metadata, mode, agent, and remote info.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMetadataSnapshotResult> snapshot() {
        return caller.invoke("session.metadata.snapshot", java.util.Map.of("sessionId", this.sessionId), SessionMetadataSnapshotResult.class);
    }

    /**
     * Returns the client-owned string metadata persisted with this local session. The metadata is not included in model context, events, telemetry, snapshots, or remote exports.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ClientMetadata> getClientMetadata() {
        return caller.invoke("session.metadata.getClientMetadata", java.util.Map.of("sessionId", this.sessionId), ClientMetadata.class);
    }

    /**
     * Atomically patches the client-owned string metadata persisted with this local session and returns the committed bag.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ClientMetadata> updateClientMetadata(SessionMetadataUpdateClientMetadataParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.metadata.updateClientMetadata", _p, ClientMetadata.class);
    }

    /**
     * Reports whether the local session is currently processing user/agent messages.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMetadataIsProcessingResult> isProcessing() {
        return caller.invoke("session.metadata.isProcessing", java.util.Map.of("sessionId", this.sessionId), SessionMetadataIsProcessingResult.class);
    }

    /**
     * Returns a snapshot of activity flags for the session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMetadataActivityResult> activity() {
        return caller.invoke("session.metadata.activity", java.util.Map.of("sessionId", this.sessionId), SessionMetadataActivityResult.class);
    }

    /**
     * Returns the token breakdown for the session's current context window for a given model.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMetadataContextInfoResult> contextInfo(SessionMetadataContextInfoParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.metadata.contextInfo", _p, SessionMetadataContextInfoResult.class);
    }

    /**
     * Returns the experimental per-source attribution breakdown of the session's current context window as a flat list of entries (skills, subagents, MCP servers, built-in tools, plugin rollups, system/tool-definition costs, with nesting via parentId), plus the successful compaction count. The heaviest individual messages are available separately via `metadata.getContextHeaviestMessages`. Returns null until the session has initialized its system prompt and tool metadata.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMetadataGetContextAttributionResult> getContextAttribution() {
        return caller.invoke("session.metadata.getContextAttribution", java.util.Map.of("sessionId", this.sessionId), SessionMetadataGetContextAttributionResult.class);
    }

    /**
     * Returns the largest individual messages currently in the session's context window, most-expensive first. Companion to `metadata.getContextAttribution`. Returns an empty list until the session has initialized.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMetadataGetContextHeaviestMessagesResult> getContextHeaviestMessages(SessionMetadataGetContextHeaviestMessagesParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.metadata.getContextHeaviestMessages", _p, SessionMetadataGetContextHeaviestMessagesResult.class);
    }

    /**
     * Records a working-directory/git context change and emits a `session.context_changed` event. For a local session, a report whose `cwd` diverges from the session's current working directory is ignored (the call still succeeds but records nothing and emits no event): a local session's working directory is authoritative and is moved via `metadata.setWorkingDirectory` (or an SDK `session.resume` that supplies a `workingDirectory`), not by this method.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> recordContextChange(SessionMetadataRecordContextChangeParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.metadata.recordContextChange", _p, Void.class);
    }

    /**
     * Updates the session's working directory. For local sessions the target is validated first (an absolute path that exists on disk) and the permission primary directory is re-based; a rejected validation fails the call before any session state changes.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMetadataSetWorkingDirectoryResult> setWorkingDirectory(SessionMetadataSetWorkingDirectoryParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.metadata.setWorkingDirectory", _p, SessionMetadataSetWorkingDirectoryResult.class);
    }

    /**
     * Re-tokenizes the session's existing messages against a model and returns aggregate token totals.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMetadataRecomputeContextTokensResult> recomputeContextTokens(SessionMetadataRecomputeContextTokensParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.metadata.recomputeContextTokens", _p, SessionMetadataRecomputeContextTokensResult.class);
    }

}
