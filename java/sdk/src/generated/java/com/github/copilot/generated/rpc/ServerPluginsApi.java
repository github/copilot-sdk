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

    /** API methods for the {@code plugins.builtin} sub-namespace. */
    public final ServerPluginsBuiltinApi builtin;
    /** API methods for the {@code plugins.marketplaces} sub-namespace. */
    public final ServerPluginsMarketplacesApi marketplaces;

    /** @param caller the RPC transport function */
    ServerPluginsApi(RpcCaller caller) {
        this.caller = caller;
        this.builtin = new ServerPluginsBuiltinApi(caller);
        this.marketplaces = new ServerPluginsMarketplacesApi(caller);
    }

    /**
     * Lists plugins installed in user/global state.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<PluginsListResult> list() {
        return caller.invoke("plugins.list", java.util.Map.of(), PluginsListResult.class);
    }

    /**
     * Installs a plugin from a marketplace, GitHub repo, URL, or local path.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<PluginsInstallResult> install(PluginsInstallParams params) {
        return caller.invoke("plugins.install", params, PluginsInstallResult.class);
    }

    /**
     * Uninstalls an installed plugin.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> uninstall(PluginsUninstallParams params) {
        return caller.invoke("plugins.uninstall", params, Void.class);
    }

    /**
     * Updates an installed plugin to its latest published version.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<PluginsUpdateResult> update(PluginsUpdateParams params) {
        return caller.invoke("plugins.update", params, PluginsUpdateResult.class);
    }

    /**
     * Updates every installed plugin to its latest published version.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<PluginsUpdateAllResult> updateAll() {
        return caller.invoke("plugins.updateAll", java.util.Map.of(), PluginsUpdateAllResult.class);
    }

    /**
     * Enables installed plugins for new sessions.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> enable(PluginsEnableParams params) {
        return caller.invoke("plugins.enable", params, Void.class);
    }

    /**
     * Disables installed plugins for new sessions.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> disable(PluginsDisableParams params) {
        return caller.invoke("plugins.disable", params, Void.class);
    }

}
