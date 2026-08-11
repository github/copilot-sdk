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
 * API methods for the {@code workspaces} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionWorkspacesApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionWorkspacesApi(RpcCaller caller, String sessionId) {
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
    public CompletableFuture<SessionWorkspacesGetWorkspaceResult> getWorkspace() {
        return caller.invoke("session.workspaces.getWorkspace", java.util.Map.of("sessionId", this.sessionId), SessionWorkspacesGetWorkspaceResult.class);
    }

    /**
     * Workspace metadata fields to update.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionWorkspacesUpdateMetadataResult> updateMetadata(SessionWorkspacesUpdateMetadataParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.workspaces.updateMetadata", _p, SessionWorkspacesUpdateMetadataResult.class);
    }

    /**
     * Optional session context used when creating a local workspace.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionWorkspacesEnsureResult> ensure(SessionWorkspacesEnsureParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.workspaces.ensure", _p, SessionWorkspacesEnsureResult.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionWorkspacesListFilesResult> listFiles() {
        return caller.invoke("session.workspaces.listFiles", java.util.Map.of("sessionId", this.sessionId), SessionWorkspacesListFilesResult.class);
    }

    /**
     * Relative path of the workspace file to read.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionWorkspacesReadFileResult> readFile(SessionWorkspacesReadFileParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.workspaces.readFile", _p, SessionWorkspacesReadFileResult.class);
    }

    /**
     * Relative path and UTF-8 content for the workspace file to create or overwrite.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> createFile(SessionWorkspacesCreateFileParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.workspaces.createFile", _p, Void.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionWorkspacesListCheckpointsResult> listCheckpoints() {
        return caller.invoke("session.workspaces.listCheckpoints", java.util.Map.of("sessionId", this.sessionId), SessionWorkspacesListCheckpointsResult.class);
    }

    /**
     * Checkpoint number to read.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionWorkspacesReadCheckpointResult> readCheckpoint(SessionWorkspacesReadCheckpointParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.workspaces.readCheckpoint", _p, SessionWorkspacesReadCheckpointResult.class);
    }

    /**
     * Compaction summary checkpoint to persist.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionWorkspacesAddSummaryResult> addSummary(SessionWorkspacesAddSummaryParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.workspaces.addSummary", _p, SessionWorkspacesAddSummaryResult.class);
    }

    /**
     * Rollback point for local workspace summaries.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionWorkspacesTruncateSummariesResult> truncateSummaries(SessionWorkspacesTruncateSummariesParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.workspaces.truncateSummaries", _p, SessionWorkspacesTruncateSummariesResult.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionWorkspacesReadAutopilotObjectiveResult> readAutopilotObjective() {
        return caller.invoke("session.workspaces.readAutopilotObjective", java.util.Map.of("sessionId", this.sessionId), SessionWorkspacesReadAutopilotObjectiveResult.class);
    }

    /**
     * Autopilot objective file content to persist.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionWorkspacesWriteAutopilotObjectiveResult> writeAutopilotObjective(SessionWorkspacesWriteAutopilotObjectiveParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.workspaces.writeAutopilotObjective", _p, SessionWorkspacesWriteAutopilotObjectiveResult.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionWorkspacesDeleteAutopilotObjectiveResult> deleteAutopilotObjective() {
        return caller.invoke("session.workspaces.deleteAutopilotObjective", java.util.Map.of("sessionId", this.sessionId), SessionWorkspacesDeleteAutopilotObjectiveResult.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionWorkspacesAutopilotObjectiveExistsResult> autopilotObjectiveExists() {
        return caller.invoke("session.workspaces.autopilotObjectiveExists", java.util.Map.of("sessionId", this.sessionId), SessionWorkspacesAutopilotObjectiveExistsResult.class);
    }

    /**
     * Pasted content to save as a UTF-8 file in the session workspace.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionWorkspacesSaveLargePasteResult> saveLargePaste(SessionWorkspacesSaveLargePasteParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.workspaces.saveLargePaste", _p, SessionWorkspacesSaveLargePasteResult.class);
    }

    /**
     * Parameters for computing a workspace diff.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionWorkspacesDiffResult> diff(SessionWorkspacesDiffParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.workspaces.diff", _p, SessionWorkspacesDiffResult.class);
    }

}
