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
 * API methods for the {@code gitHubRelease} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class ServerGitHubReleaseApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerGitHubReleaseApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Allocates a connection-owned request id before a release lookup, snapshot, or asset download starts. Use the id to cancel that operation. A snapshot retains its allocation until disposal or connection close.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<GitHubReleaseRequestIdResult> allocateRequestId() {
        return caller.invoke("gitHubRelease.allocateRequestId", java.util.Map.of(), GitHubReleaseRequestIdResult.class);
    }

    /**
     * Retires an unused allocation that this caller will never dispatch. Only an unclaimed id on this connection with no active guard is eligible. Active, foreign, unknown and previously retired ids return abandoned: false. Ordinary cancellation still preserves the first canceled claim. This cleanup remains available during shutdown and is refused for extensions.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<GitHubReleaseAbandonRequestResult> abandonRequest(GitHubReleaseAbandonRequestParams params) {
        return caller.invoke("gitHubRelease.abandonRequest", params, GitHubReleaseAbandonRequestResult.class);
    }

    /**
     * Looks up a published release or release list. A bound host connection uses its original auth source. Other SDK callers keep the existing authInfo path. The runtime resolves credentials for each request and redirect. Public releases also permit anonymous access. The kind field distinguishes a result, a missing tag, no release, a rate limit, and failure. This raw lookup does not return a shared snapshot.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<GitHubReleaseLookupResult> lookup(GitHubReleaseLookupParams params) {
        return caller.invoke("gitHubRelease.lookup", params, GitHubReleaseLookupResult.class);
    }

    /**
     * Loads one immutable release snapshot for an update client or SDK host. The runtime retains the original credential source and resolves it for each request. Calls with the same allocated request id and options share one lookup and result. A dropped waiter does not cancel that lookup. Use gitHubRelease.cancel to dispose the operation. The runtime admits one failure report before it publishes a failed result. Extensions cannot use this method.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<GitHubReleaseSnapshotResult> getSnapshot(GitHubReleaseGetSnapshotParams params) {
        return caller.invoke("gitHubRelease.getSnapshot", params, GitHubReleaseSnapshotResult.class);
    }

    /**
     * Cancels an allocated operation on this connection. For a snapshot, disposal also waits for its owned lookup and retires its credential authority. It does not delete completed caller-owned downloads. An unknown or foreign id returns canceled: false. A completed download still returns its owned file, which the caller must dispose.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<GitHubReleaseCancelResult> cancel(GitHubReleaseCancelParams params) {
        return caller.invoke("gitHubRelease.cancel", params, GitHubReleaseCancelResult.class);
    }

    /**
     * Streams a published Copilot release asset to a private file and computes its SHA-256. The URL must name an HTTPS release download in a supported repository. A snapshotRequestId retains that snapshot's original credential source. Otherwise, a bound host uses its original owner, and other SDK callers keep the authInfo path. Each request and redirect resolves credentials again. Revocation fails without an auth fallback. Ordinary credential rejection and transport fallback keep their existing behavior. A digest mismatch removes the file. The result contains file metadata, not archive bytes. The caller must dispose the completed file after use. The download's own requestId controls progress and cancellation. Extensions cannot use this method.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<GitHubReleaseDownloadAssetResult> downloadAsset(GitHubReleaseDownloadAssetParams params) {
        return caller.invoke("gitHubRelease.downloadAsset", params, GitHubReleaseDownloadAssetResult.class);
    }

    /**
     * Reports how many bytes a running `gitHubRelease.downloadAsset` has written, for a progress display. The runtime sends no progress notification for a download, so a host polls this while the download runs. An id with no running download reports zero bytes and no total, which is the normal answer before the body starts and after the download ends.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<GitHubReleaseDownloadProgressResult> getDownloadProgress(GitHubReleaseGetDownloadProgressParams params) {
        return caller.invoke("gitHubRelease.getDownloadProgress", params, GitHubReleaseDownloadProgressResult.class);
    }

}
