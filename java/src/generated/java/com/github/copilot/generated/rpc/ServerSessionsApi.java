/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code sessions} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerSessionsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerSessionsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Open a session by creating, resuming, attaching, connecting to a remote, or handing off.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsOpenResult> open() {
        return caller.invoke("sessions.open", java.util.Map.of(), SessionsOpenResult.class);
    }

    /**
     * Source session identifier to fork from, optional event-ID boundary, and optional friendly name for the new session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsForkResult> fork(SessionsForkParams params) {
        return caller.invoke("sessions.fork", params, SessionsForkResult.class);
    }

    /**
     * Remote session connection parameters.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsConnectResult> connect() {
        return caller.invoke("sessions.connect", java.util.Map.of(), SessionsConnectResult.class);
    }

    /**
     * Optional source filter, metadata-load limit, and context filter applied to the returned sessions.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsListResult> list() {
        return caller.invoke("sessions.list", java.util.Map.of(), SessionsListResult.class);
    }

    /**
     * GitHub task ID to look up.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsFindByTaskIdResult> findByTaskId(SessionsFindByTaskIdParams params) {
        return caller.invoke("sessions.findByTaskId", params, SessionsFindByTaskIdResult.class);
    }

    /**
     * UUID prefix to resolve to a unique session ID.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsFindByPrefixResult> findByPrefix(SessionsFindByPrefixParams params) {
        return caller.invoke("sessions.findByPrefix", params, SessionsFindByPrefixResult.class);
    }

    /**
     * Optional working-directory context used to score session relevance.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsGetLastForContextResult> getLastForContext(SessionsGetLastForContextParams params) {
        return caller.invoke("sessions.getLastForContext", params, SessionsGetLastForContextResult.class);
    }

    /**
     * Session ID whose event-log file path to compute.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsGetEventFilePathResult> getEventFilePath() {
        return caller.invoke("sessions.getEventFilePath", java.util.Map.of(), SessionsGetEventFilePathResult.class);
    }

    /**
     * Map of sessionId -> on-disk size in bytes for each session's workspace directory.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsGetSizesResult> getSizes() {
        return caller.invoke("sessions.getSizes", java.util.Map.of(), SessionsGetSizesResult.class);
    }

    /**
     * Session IDs to test for live in-use locks.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsCheckInUseResult> checkInUse(SessionsCheckInUseParams params) {
        return caller.invoke("sessions.checkInUse", params, SessionsCheckInUseResult.class);
    }

    /**
     * Session ID to look up the persisted remote-steerable flag for.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsGetPersistedRemoteSteerableResult> getPersistedRemoteSteerable() {
        return caller.invoke("sessions.getPersistedRemoteSteerable", java.util.Map.of(), SessionsGetPersistedRemoteSteerableResult.class);
    }

    /**
     * Session ID to close.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<Void> close() {
        return caller.invoke("sessions.close", java.util.Map.of(), Void.class);
    }

    /**
     * Session IDs to close, deactivate, and delete from disk.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsBulkDeleteResult> bulkDelete(SessionsBulkDeleteParams params) {
        return caller.invoke("sessions.bulkDelete", params, SessionsBulkDeleteResult.class);
    }

    /**
     * Age threshold and optional flags controlling which old sessions are pruned (or simulated when dryRun is true).
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsPruneOldResult> pruneOld(SessionsPruneOldParams params) {
        return caller.invoke("sessions.pruneOld", params, SessionsPruneOldResult.class);
    }

    /**
     * Session ID whose pending events should be flushed to disk.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<Void> save() {
        return caller.invoke("sessions.save", java.util.Map.of(), Void.class);
    }

    /**
     * Session ID whose in-use lock should be released.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<Void> releaseLock() {
        return caller.invoke("sessions.releaseLock", java.util.Map.of(), Void.class);
    }

    /**
     * Session metadata records to enrich with summary and context information.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsEnrichMetadataResult> enrichMetadata(SessionsEnrichMetadataParams params) {
        return caller.invoke("sessions.enrichMetadata", params, SessionsEnrichMetadataResult.class);
    }

    /**
     * Active session ID and an optional flag for deferring repo-level hooks until folder trust.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<Void> reloadPluginHooks(SessionsReloadPluginHooksParams params) {
        return caller.invoke("sessions.reloadPluginHooks", params, Void.class);
    }

    /**
     * Active session ID whose deferred repo-level hooks should be loaded.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsLoadDeferredRepoHooksResult> loadDeferredRepoHooks() {
        return caller.invoke("sessions.loadDeferredRepoHooks", java.util.Map.of(), SessionsLoadDeferredRepoHooksResult.class);
    }

    /**
     * Manager-wide additional plugins to register; replaces any previously-configured set.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<Void> setAdditionalPlugins(SessionsSetAdditionalPluginsParams params) {
        return caller.invoke("sessions.setAdditionalPlugins", params, Void.class);
    }

    /**
     * Session ID whose board entry count should be returned.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsGetBoardEntryCountResult> getBoardEntryCount() {
        return caller.invoke("sessions.getBoardEntryCount", java.util.Map.of(), SessionsGetBoardEntryCountResult.class);
    }

    /**
     * Parameters for attaching the remote-control singleton to a session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsStartRemoteControlResult> startRemoteControl(SessionsStartRemoteControlParams params) {
        return caller.invoke("sessions.startRemoteControl", params, SessionsStartRemoteControlResult.class);
    }

    /**
     * Parameters for atomically rebinding the remote-control singleton.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsTransferRemoteControlResult> transferRemoteControl(SessionsTransferRemoteControlParams params) {
        return caller.invoke("sessions.transferRemoteControl", params, SessionsTransferRemoteControlResult.class);
    }

    /**
     * Patch for the singleton's steering state.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsSetRemoteControlSteeringResult> setRemoteControlSteering(SessionsSetRemoteControlSteeringParams params) {
        return caller.invoke("sessions.setRemoteControlSteering", params, SessionsSetRemoteControlSteeringResult.class);
    }

    /**
     * Parameters for stopping the remote-control singleton.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsStopRemoteControlResult> stopRemoteControl() {
        return caller.invoke("sessions.stopRemoteControl", java.util.Map.of(), SessionsStopRemoteControlResult.class);
    }

    /**
     * Wrapper for the singleton's current status.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsGetRemoteControlStatusResult> getRemoteControlStatus() {
        return caller.invoke("sessions.getRemoteControlStatus", java.util.Map.of(), SessionsGetRemoteControlStatusResult.class);
    }

    /**
     * Cursor and optional long-poll wait for polling runtime-spawned sessions.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsPollSpawnedSessionsResult> pollSpawnedSessions() {
        return caller.invoke("sessions.pollSpawnedSessions", java.util.Map.of(), SessionsPollSpawnedSessionsResult.class);
    }

    /**
     * Params to attach an extension loader's tools to a session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionsRegisterExtensionToolsOnSessionResult> registerExtensionToolsOnSession(SessionsRegisterExtensionToolsOnSessionParams params) {
        return caller.invoke("sessions.registerExtensionToolsOnSession", params, SessionsRegisterExtensionToolsOnSessionResult.class);
    }

    /**
     * Params to attach or detach an in-process ExtensionController delegate.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<Void> configureSessionExtensions(SessionsConfigureSessionExtensionsParams params) {
        return caller.invoke("sessions.configureSessionExtensions", params, Void.class);
    }

}
