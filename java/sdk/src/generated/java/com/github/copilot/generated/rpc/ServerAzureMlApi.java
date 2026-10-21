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
 * API methods for the {@code azureMl} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerAzureMlApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerAzureMlApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Lists eligible Azure ML local models and redacted warnings before a chat session exists.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<AzureMlCatalogResult> catalog(AzureMlCatalogParams params) {
        return caller.invoke("azureMl.catalog", params, AzureMlCatalogResult.class);
    }

    /**
     * Revalidates the newest eligible version of one Azure ML model family.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<AzureMlLatestResult> latest(AzureMlLatestParams params) {
        return caller.invoke("azureMl.latest", params, AzureMlLatestResult.class);
    }

    /**
     * Resolves immutable artifact facts without returning signed access URLs.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<AzureMlResolveResult> resolve(AzureMlResolveParams params) {
        return caller.invoke("azureMl.resolve", params, AzureMlResolveResult.class);
    }

    /**
     * Downloads and verifies a connection-owned model version, returning only after placement and manifest publication.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<AzureMlDownloadResult> download(AzureMlDownloadParams params) {
        return caller.invoke("azureMl.download", params, AzureMlDownloadResult.class);
    }

    /**
     * Acquires a connection-owned model-family mutation lease across transfer, activation, registration, and cleanup.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<AzureMlAcquireFamilyResult> acquireFamily(AzureMlAcquireFamilyParams params) {
        return caller.invoke("azureMl.acquireFamily", params, AzureMlAcquireFamilyResult.class);
    }

    /**
     * Releases a connection-owned family lease after its active download and cleanup operations complete.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> releaseFamily(AzureMlReleaseFamilyParams params) {
        return caller.invoke("azureMl.releaseFamily", params, Void.class);
    }

    /**
     * Returns progress for an active download owned by this connection, or null after completion.
     *
     * @return a future that completes with the {@code AzureMlDownloadStatus} value,
     *     or {@code null} when the result is absent. Callers must handle the
     *     {@code null} completion value.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<AzureMlDownloadStatus> status(AzureMlStatusParams params) {
        return caller.invoke("azureMl.status", params, AzureMlDownloadStatus.class);
    }

    /**
     * Cancels a connection-owned download; the download call reports terminal cleanup separately.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Boolean> cancel(AzureMlCancelParams params) {
        return caller.invoke("azureMl.cancel", params, Boolean.class);
    }

    /**
     * Discards an inactive model version under the server-owned Azure ML library.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> discard(AzureMlDiscardParams params) {
        return caller.invoke("azureMl.discard", params, Void.class);
    }

}
