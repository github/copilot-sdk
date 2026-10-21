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
     * Plans installation of an Agent Plugin search candidate from its verified Agent Finder materialisation without writing files. Only candidates that carry a handle can be planned. The review shows only verified data, and installing leaves the plugin disabled until it is enabled through plugins.enable.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<PluginInstallationManagementResult> planInstall(PluginsPlanInstallParams params) {
        return caller.invoke("plugins.planInstall", params, PluginInstallationManagementResult.class);
    }

    /**
     * Consumes one verified Agent Plugin installation plan, requests explicit human consent through installations.confirm on the original connection, then reacquires and verifies every file and registers the plugin, disabled, exactly as a direct plugin install. The installed plugin is listed, enabled and uninstalled through the existing plugins methods.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<PluginInstallationResult> applyInstall(PluginsApplyInstallParams params) {
        return caller.invoke("plugins.applyInstall", params, PluginInstallationResult.class);
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
