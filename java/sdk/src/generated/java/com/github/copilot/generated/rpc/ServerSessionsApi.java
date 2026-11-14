/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import java.util.List;
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
     * Creates or resumes a local session and returns the opened session ID.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionsOpenResult> open(SessionsOpenParams params) {
        return caller.invoke("sessions.open", params, SessionsOpenResult.class);
    }

    /**
     * Creates a new session by forking persisted history from an existing session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionsForkResult> fork(SessionsForkParams params) {
        return caller.invoke("sessions.fork", params, SessionsForkResult.class);
    }

    /**
     * Connects to an existing remote session and exposes it as an SDK session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionsConnectResult> connect(SessionsConnectParams params) {
        return caller.invoke("sessions.connect", params, SessionsConnectResult.class);
    }

    /**
     * Lists sessions, optionally filtered by source and working-directory context. Returned entries are discriminated by `isRemote`: local entries carry only the lightweight `LocalSessionMetadataValue` shape; remote entries carry the full `RemoteSessionMetadataValue` shape (repository, PR number, taskType, etc.).
     * <p>
     * Invokes the method with no params, applying the runtime defaults.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionsListResult> list() {
        return list(null);
    }

    /**
     * Lists sessions, optionally filtered by source and working-directory context. Returned entries are discriminated by `isRemote`: local entries carry only the lightweight `LocalSessionMetadataValue` shape; remote entries carry the full `RemoteSessionMetadataValue` shape (repository, PR number, taskType, etc.).
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionsListResult> list(SessionsListParams params) {
        return caller.invoke("sessions.list", params == null ? java.util.Map.of() : params, SessionsListResult.class);
    }

    /**
     * Reads lightweight persisted metadata for one local session without opening it.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SessionsGetMetadataResult> getMetadata(SessionsGetMetadataParams params) {
        return caller.invoke("sessions.getMetadata", params, SessionsGetMetadataResult.class);
    }

    /**
     * Reads client-owned metadata for multiple persisted local sessions without opening them. Results preserve request order and report missing, corrupt, unsupported, or temporarily unavailable sessions independently.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<List<Object>> getClientMetadata(SessionsGetClientMetadataParams params) {
        return caller.invoke("sessions.getClientMetadata", params, RpcMapper.INSTANCE.getTypeFactory().constructCollectionType(List.class, Object.class));
    }

    /**
     * Reads a page of durable events directly from a local session's persisted journal without creating, resuming, or activating the session. The first read pins the currently opened journal generation and its byte-length boundary; opaque cursor continuations remain on that generation across runtime-owned compaction, truncation, and rewrite operations, which replace the live path atomically, and events appended after the boundary are excluded. For cold hydration, await the first successful page before activation and establish lossless live-event buffering before resume; merge subsequent live events by ID, preserving persisted order and letting live payloads win. Continuations are process-local, single-use capabilities bound to the originating session and storage context and must be paged sequentially; concurrent or repeated use of the same cursor expires that duplicate read rather than reading the generation twice. A complete snapshot has cursorStatus 'ok' and hasMore false. Snapshots expire after five idle minutes, with at most eight retained per process and idle-only eviction under pressure; completion and cancelled-worker exit release their handles. No transcript copy is created, but retained handles may keep replaced files' disk blocks alive until release. Pages have a soft 1 MiB serialized event-array budget including resolved binary assets; one oversized event is returned alone to guarantee progress. Working memory also includes a record/lookahead and asset resolution; resolving the first binary reference may scan the full pinned generation to build a bounded offset index. If the snapshot expires, is evicted, is cancelled before a continuation is established, or becomes unreadable after an observable unsupported in-place shortening, the continuation returns cursorStatus 'expired' with an empty terminal page and never falls back to a different generation. A missing or initially unreadable journal is an RPC error. Persisted history excludes ephemeral events and may omit payloads that are reconstructed only for an active session; use the active session event stream for post-resume live events.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionsReadPersistedEventsResult> readPersistedEvents(SessionsReadPersistedEventsParams params) {
        return caller.invoke("sessions.readPersistedEvents", params, SessionsReadPersistedEventsResult.class);
    }

    /**
     * Lists recent local session IDs that contain user-visible history, omitting housekeeping-only sessions.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SessionsListNonEmptySessionIdsResult> listNonEmptySessionIds(SessionsListNonEmptySessionIdsParams params) {
        return caller.invoke("sessions.listNonEmptySessionIds", params, SessionsListNonEmptySessionIdsResult.class);
    }

    /**
     * Finds the local session bound to a GitHub task ID, if any.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionsFindByTaskIdResult> findByTaskId(SessionsFindByTaskIdParams params) {
        return caller.invoke("sessions.findByTaskId", params, SessionsFindByTaskIdResult.class);
    }

    /**
     * Resolves a UUID prefix to a unique session ID, if exactly one session matches.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionsFindByPrefixResult> findByPrefix(SessionsFindByPrefixParams params) {
        return caller.invoke("sessions.findByPrefix", params, SessionsFindByPrefixResult.class);
    }

    /**
     * Returns the most-relevant prior session for a given working-directory context.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionsGetLastForContextResult> getLastForContext(SessionsGetLastForContextParams params) {
        return caller.invoke("sessions.getLastForContext", params, SessionsGetLastForContextResult.class);
    }

    /**
     * Computes the absolute path to a session's persisted events.jsonl file. Internal: filesystem paths are only meaningful in-process (CLI and runtime share a filesystem). Currently used by the CLI's contribution-graph feature to read historical events directly. Remote SDK consumers must not depend on this; a proper event-query API would replace it if the contribution graph ever needed to work over the wire.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SessionsGetEventFilePathResult> getEventFilePath(SessionsGetEventFilePathParams params) {
        return caller.invoke("sessions.getEventFilePath", params, SessionsGetEventFilePathResult.class);
    }

    /**
     * Returns the on-disk byte size of each session's workspace directory.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionsGetSizesResult> getSizes() {
        return caller.invoke("sessions.getSizes", java.util.Map.of(), SessionsGetSizesResult.class);
    }

    /**
     * Returns the subset of the supplied session IDs that are currently held by another running process.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionsCheckInUseResult> checkInUse(SessionsCheckInUseParams params) {
        return caller.invoke("sessions.checkInUse", params, SessionsCheckInUseResult.class);
    }

    /**
     * Returns a session's persisted remote-steerable flag, if any has been recorded. Internal: this is CLI-specific book-keeping used by `--continue` / `--resume` to inherit the prior session's remote-steerable preference. SDK consumers that want similar behavior should manage their own persistence around start/stop calls rather than relying on this runtime-side flag.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SessionsGetPersistedRemoteSteerableResult> getPersistedRemoteSteerable(SessionsGetPersistedRemoteSteerableParams params) {
        return caller.invoke("sessions.getPersistedRemoteSteerable", params, SessionsGetPersistedRemoteSteerableResult.class);
    }

    /**
     * Closes a session: emits shutdown, flushes pending events, releases the in-use lock, and disposes the active session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> close(SessionsCloseParams params) {
        return caller.invoke("sessions.close", params, Void.class);
    }

    /**
     * Closes, deactivates, and deletes a set of sessions, returning the bytes freed per session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionsBulkDeleteResult> bulkDelete(SessionsBulkDeleteParams params) {
        return caller.invoke("sessions.bulkDelete", params, SessionsBulkDeleteResult.class);
    }

    /**
     * Deletes one local session from disk after running the same lifecycle hooks as the session manager.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> delete(SessionsDeleteParams params) {
        return caller.invoke("sessions.delete", params, Void.class);
    }

    /**
     * Deletes sessions older than the given threshold, with optional dry-run and exclusion list.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionsPruneOldResult> pruneOld(SessionsPruneOldParams params) {
        return caller.invoke("sessions.pruneOld", params, SessionsPruneOldResult.class);
    }

    /**
     * Flushes a session's pending events to disk.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> save(SessionsSaveParams params) {
        return caller.invoke("sessions.save", params, Void.class);
    }

    /**
     * Releases the in-use lock held by this process for a session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> releaseLock(SessionsReleaseLockParams params) {
        return caller.invoke("sessions.releaseLock", params, Void.class);
    }

    /**
     * Backfills missing summary and context fields on the supplied session metadata records.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionsEnrichMetadataResult> enrichMetadata(SessionsEnrichMetadataParams params) {
        return caller.invoke("sessions.enrichMetadata", params, SessionsEnrichMetadataResult.class);
    }

    /**
     * Creates the workspace record for a session that has not been opened yet. A host that hands a session off to another application — writing the record and then launching that application against the session ID — needs the record on disk before any session exists to carry it, which the session-scoped workspace methods cannot do. Replaces any existing record and resets the checkpoint index. When writing to the local filesystem, a stored `fork_count` survives on disk. Returns the record it built, so a surviving stored `fork_count` can differ from the answer.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SessionsCreateWorkspaceResult> createWorkspace(SessionsCreateWorkspaceParams params) {
        return caller.invoke("sessions.createWorkspace", params, SessionsCreateWorkspaceResult.class);
    }

    /**
     * Reads a session's workspace record straight from disk, without opening the session. Resuming by session ID has to know where the session lives before it can connect, so the lookup cannot come from the session-scoped workspace methods, which resolve their location from a live session's context. Returns no record when the file is absent.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SessionsLoadWorkspaceResult> loadWorkspace(SessionsLoadWorkspaceParams params) {
        return caller.invoke("sessions.loadWorkspace", params, SessionsLoadWorkspaceResult.class);
    }

    /**
     * Merges fields into a session's workspace record on disk, creating the record when it is absent. The counterpart to `sessions.loadWorkspace`, for the same before-the-session-exists case. It preserves stored workspace-schema fields the request does not supply, does not preserve stored keys outside the workspace schema, and never replaces a stored `fork_count`.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> updateWorkspaceFields(SessionsUpdateWorkspaceFieldsParams params) {
        return caller.invoke("sessions.updateWorkspaceFields", params, Void.class);
    }

    /**
     * Reloads user, plugin, and (optionally) repo hooks on the active session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> reloadPluginHooks(SessionsReloadPluginHooksParams params) {
        return caller.invoke("sessions.reloadPluginHooks", params, Void.class);
    }

    /**
     * Loads previously-deferred repo-level hooks on the active session, returning queued startup prompts.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionsLoadDeferredRepoHooksResult> loadDeferredRepoHooks(SessionsLoadDeferredRepoHooksParams params) {
        return caller.invoke("sessions.loadDeferredRepoHooks", params, SessionsLoadDeferredRepoHooksResult.class);
    }

    /**
     * Replaces the manager-wide additional plugins registered with the session manager.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> setAdditionalPlugins(SessionsSetAdditionalPluginsParams params) {
        return caller.invoke("sessions.setAdditionalPlugins", params, Void.class);
    }

    /**
     * Gets the dynamic-context board entry count associated with a session, when available. Internal: this exists solely so CLI telemetry events (`rem_spawn_gate`, `rem_consolidation_complete`) can pair START / END board counts around the detached rem-agent spawn. "Dynamic context board" is a runtime-internal concept that is not part of the public SDK contract; the long-term plan is to relocate the telemetry emission into the runtime so this method can be deleted entirely.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SessionsGetBoardEntryCountResult> getBoardEntryCount(SessionsGetBoardEntryCountParams params) {
        return caller.invoke("sessions.getBoardEntryCount", params, SessionsGetBoardEntryCountResult.class);
    }

    /**
     * Attaches the runtime-managed remote-control singleton to a session, awaiting initial setup. If remote control is already attached to a different session, the singleton is transferred (preserving the underlying Mission Control connection). Returns the final status.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionsStartRemoteControlResult> startRemoteControl(SessionsStartRemoteControlParams params) {
        return caller.invoke("sessions.startRemoteControl", params, SessionsStartRemoteControlResult.class);
    }

    /**
     * Atomically rebinds the remote-control singleton to a different session, preserving the underlying Mission Control connection. When `expectedFromSessionId` is provided and does not match the singleton's current `attachedSessionId`, the transfer is rejected with `transferred: false` and the current status is returned unchanged.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionsTransferRemoteControlResult> transferRemoteControl(SessionsTransferRemoteControlParams params) {
        return caller.invoke("sessions.transferRemoteControl", params, SessionsTransferRemoteControlResult.class);
    }

    /**
     * Patches the steering state of the active remote-control singleton. When remote control is off, this is a no-op and the off status is returned. Today only `enabled: true` is actionable on the underlying exporter; passing `false` is reserved for future use.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionsSetRemoteControlSteeringResult> setRemoteControlSteering(SessionsSetRemoteControlSteeringParams params) {
        return caller.invoke("sessions.setRemoteControlSteering", params, SessionsSetRemoteControlSteeringResult.class);
    }

    /**
     * Stops the remote-control singleton. When `expectedSessionId` is provided and does not match the singleton's current `attachedSessionId`, the stop is rejected with `stopped: false` and the current status is returned unchanged (unless `force` is set, in which case the singleton is unconditionally torn down).
     * <p>
     * Invokes the method with no params, applying the runtime defaults.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionsStopRemoteControlResult> stopRemoteControl() {
        return stopRemoteControl(null);
    }

    /**
     * Stops the remote-control singleton. When `expectedSessionId` is provided and does not match the singleton's current `attachedSessionId`, the stop is rejected with `stopped: false` and the current status is returned unchanged (unless `force` is set, in which case the singleton is unconditionally torn down).
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionsStopRemoteControlResult> stopRemoteControl(SessionsStopRemoteControlParams params) {
        return caller.invoke("sessions.stopRemoteControl", params == null ? java.util.Map.of() : params, SessionsStopRemoteControlResult.class);
    }

    /**
     * Returns the current state of the remote-control singleton, including the attached session id and frontend URL when active.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionsGetRemoteControlStatusResult> getRemoteControlStatus() {
        return caller.invoke("sessions.getRemoteControlStatus", java.util.Map.of(), SessionsGetRemoteControlStatusResult.class);
    }

    /**
     * Attaches (or detaches) an in-process ExtensionController delegate for the given session in a local host adapter. Pass `controller: undefined` to detach. Internal because the controller cannot cross the JSON-RPC boundary; the runtime manages its own session extension service.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> configureSessionExtensions(SessionsConfigureSessionExtensionsParams params) {
        return caller.invoke("sessions.configureSessionExtensions", params, Void.class);
    }

}
