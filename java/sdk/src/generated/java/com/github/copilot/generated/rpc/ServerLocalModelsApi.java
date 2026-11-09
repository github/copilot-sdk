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
 * API methods for the {@code localModels} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerLocalModelsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerLocalModelsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Configures this SDK connection's local model manager. Runtime admission, credentials, jobs, files, progress, and cancellation remain connection-owned. A source session supplies live registration and selection without extending its lifetime.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<LocalModelsSnapshot> configure(LocalModelsConfigureParams params) {
        return caller.invoke("localModels.configure", params, LocalModelsSnapshot.class);
    }

    /**
     * Reads this connection's local model state without scanning files or contacting a registry. Does not expose another connection's jobs.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<LocalModelsSnapshot> inspect() {
        return caller.invoke("localModels.inspect", java.util.Map.of(), LocalModelsSnapshot.class);
    }

    /**
     * Waits for a newer local model revision and returns current state plus unacknowledged host notifications. Only this SDK connection's manager is visible. Connection closure cancels the wait.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<LocalModelsSnapshot> watch(LocalModelsWatchParams params) {
        return caller.invoke("localModels.watch", params, LocalModelsSnapshot.class);
    }

    /**
     * Scans this connection's model files and schedules a catalog refresh under its current preferences and runtime admission.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> refresh(LocalModelsRefreshParams params) {
        return caller.invoke("localModels.refresh", params, Void.class);
    }

    /**
     * Loads the local model catalog under the background policy without repeating a completed successful fetch for unchanged access.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> prefetch() {
        return caller.invoke("localModels.prefetch", java.util.Map.of(), Void.class);
    }

    /**
     * Resolves current artifact access and resource requirements without changing the caller's descriptor or starting a download.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<LocalModelsPrepareResult> prepare(LocalModelsPrepareParams params) {
        return caller.invoke("localModels.prepare", params, LocalModelsPrepareResult.class);
    }

    /**
     * Downloads, updates, or repairs a model through runtime-owned acquisition, registration, activation, and compensation. The connection-owned job can finish its saved registration after its source session closes. The call completes with that job.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> setup(LocalModelsSetupParams params) {
        return caller.invoke("localModels.setup", params, Void.class);
    }

    /**
     * Removes a managed model's live and saved registration and local files under the connection-owned family lease. Preserves files if registration cleanup cannot safely finish.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> deleteModel(LocalModelsDeleteModelParams params) {
        return caller.invoke("localModels.deleteModel", params, Void.class);
    }

    /**
     * Requests cancellation of this connection's setup job for the given catalog identifier. An identifier never grants access to another connection's jobs.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> cancel(LocalModelsCancelParams params) {
        return caller.invoke("localModels.cancel", params, Void.class);
    }

    /**
     * Cancels this connection's active setup jobs and waits for their cleanup. Removal jobs finish instead of stopping halfway through registration cleanup.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> cancelAll() {
        return caller.invoke("localModels.cancelAll", java.util.Map.of(), Void.class);
    }

    /**
     * Dismisses one notice from this connection's local model state.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> dismissNotice(LocalModelsDismissNoticeParams params) {
        return caller.invoke("localModels.dismissNotice", params, Void.class);
    }

}
