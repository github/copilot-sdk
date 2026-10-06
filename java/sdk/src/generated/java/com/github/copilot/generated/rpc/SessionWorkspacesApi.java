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
     * Gets current workspace metadata for the session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionWorkspacesGetWorkspaceResult> getWorkspace() {
        return caller.invoke("session.workspaces.getWorkspace", java.util.Map.of("sessionId", this.sessionId), SessionWorkspacesGetWorkspaceResult.class);
    }

    /**
     * Updates workspace metadata for a local session and returns the refreshed workspace.
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
     * Ensures a local session workspace exists and returns it.
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
     * Lists files stored in the session workspace files directory.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionWorkspacesListFilesResult> listFiles() {
        return caller.invoke("session.workspaces.listFiles", java.util.Map.of("sessionId", this.sessionId), SessionWorkspacesListFilesResult.class);
    }

    /**
     * Reads a file from the session workspace files directory.
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
     * Creates or overwrites a file in the session workspace files directory.
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
     * Returns metadata for a file or directory in the session workspace files directory.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionWorkspacesStatFileResult> statFile(SessionWorkspacesStatFileParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.workspaces.statFile", _p, SessionWorkspacesStatFileResult.class);
    }

    /**
     * Creates a directory in the session workspace files directory.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> createDirectory(SessionWorkspacesCreateDirectoryParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.workspaces.createDirectory", _p, Void.class);
    }

    /**
     * Removes a file or directory from the session workspace files directory.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> removePath(SessionWorkspacesRemovePathParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.workspaces.removePath", _p, Void.class);
    }

    /**
     * Renames a file or directory within the session workspace files directory.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> renamePath(SessionWorkspacesRenamePathParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.workspaces.renamePath", _p, Void.class);
    }

    /**
     * Lists workspace checkpoints in chronological order.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionWorkspacesListCheckpointsResult> listCheckpoints() {
        return caller.invoke("session.workspaces.listCheckpoints", java.util.Map.of("sessionId", this.sessionId), SessionWorkspacesListCheckpointsResult.class);
    }

    /**
     * Reads the content of a workspace checkpoint by number.
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
     * Adds a compaction summary checkpoint to the local session workspace.
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
     * Truncates local workspace compaction summaries after a rollback.
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
     * Reads the autopilot objective state file from the local session workspace.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionWorkspacesReadAutopilotObjectiveResult> readAutopilotObjective() {
        return caller.invoke("session.workspaces.readAutopilotObjective", java.util.Map.of("sessionId", this.sessionId), SessionWorkspacesReadAutopilotObjectiveResult.class);
    }

    /**
     * Writes the autopilot objective state file in the local session workspace.
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
     * Deletes the autopilot objective state file from the local session workspace.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionWorkspacesDeleteAutopilotObjectiveResult> deleteAutopilotObjective() {
        return caller.invoke("session.workspaces.deleteAutopilotObjective", java.util.Map.of("sessionId", this.sessionId), SessionWorkspacesDeleteAutopilotObjectiveResult.class);
    }

    /**
     * Checks whether the local session workspace has an autopilot objective state file.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionWorkspacesAutopilotObjectiveExistsResult> autopilotObjectiveExists() {
        return caller.invoke("session.workspaces.autopilotObjectiveExists", java.util.Map.of("sessionId", this.sessionId), SessionWorkspacesAutopilotObjectiveExistsResult.class);
    }

    /**
     * Saves pasted content as a UTF-8 file in the session workspace.
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
     * Computes a diff for the session workspace. Never rejects for a busy session: a `session`-mode diff that cannot read the session's file-change captures falls back to an unstaged git diff with `isFallback: true` and reports why in `unavailableReason`.
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
