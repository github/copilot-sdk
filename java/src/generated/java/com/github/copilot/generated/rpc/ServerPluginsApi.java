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
 * API methods for the {@code plugins} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerPluginsApi {

    private final RpcCaller caller;

    /** API methods for the {@code plugins.marketplaces} sub-namespace. */
    public final ServerPluginsMarketplacesApi marketplaces;

    /** @param caller the RPC transport function */
    ServerPluginsApi(RpcCaller caller) {
        this.caller = caller;
        this.marketplaces = new ServerPluginsMarketplacesApi(caller);
    }

    /**
     * Plugins installed in user/global state.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<PluginsListResult> list() {
        return caller.invoke("plugins.list", java.util.Map.of(), PluginsListResult.class);
    }

    /**
     * Plugin source and optional working directory for relative-path resolution.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<PluginsInstallResult> install(PluginsInstallParams params) {
        return caller.invoke("plugins.install", params, PluginsInstallResult.class);
    }

    /**
     * Name (or spec) of the plugin to uninstall.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> uninstall(PluginsUninstallParams params) {
        return caller.invoke("plugins.uninstall", params, Void.class);
    }

    /**
     * Name (or spec) of the plugin to update.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<PluginsUpdateResult> update(PluginsUpdateParams params) {
        return caller.invoke("plugins.update", params, PluginsUpdateResult.class);
    }

    /**
     * Result of updating all installed plugins.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<PluginsUpdateAllResult> updateAll() {
        return caller.invoke("plugins.updateAll", java.util.Map.of(), PluginsUpdateAllResult.class);
    }

    /**
     * Plugin names (or specs) to enable.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> enable(PluginsEnableParams params) {
        return caller.invoke("plugins.enable", params, Void.class);
    }

    /**
     * Plugin names (or specs) to disable.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> disable(PluginsDisableParams params) {
        return caller.invoke("plugins.disable", params, Void.class);
    }

}
