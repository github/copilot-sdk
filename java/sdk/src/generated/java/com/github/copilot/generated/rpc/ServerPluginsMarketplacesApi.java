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
 * API methods for the {@code plugins.marketplaces} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerPluginsMarketplacesApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerPluginsMarketplacesApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Lists all registered marketplaces (defaults + user-added).
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<PluginsMarketplacesListResult> list() {
        return caller.invoke("plugins.marketplaces.list", java.util.Map.of(), PluginsMarketplacesListResult.class);
    }

    /**
     * Registers a new marketplace from a source (owner/repo, URL, or local path).
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<PluginsMarketplacesAddResult> add(PluginsMarketplacesAddParams params) {
        return caller.invoke("plugins.marketplaces.add", params, PluginsMarketplacesAddResult.class);
    }

    /**
     * Removes a previously-registered marketplace. When the marketplace has dependent plugins and `force` is not set, the marketplace is left intact and the result lists the dependents so the caller can decide whether to retry with `force=true`.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<PluginsMarketplacesRemoveResult> remove(PluginsMarketplacesRemoveParams params) {
        return caller.invoke("plugins.marketplaces.remove", params, PluginsMarketplacesRemoveResult.class);
    }

    /**
     * Lists plugins advertised by a registered marketplace.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<PluginsMarketplacesBrowseResult> browse(PluginsMarketplacesBrowseParams params) {
        return caller.invoke("plugins.marketplaces.browse", params, PluginsMarketplacesBrowseResult.class);
    }

    /**
     * Re-fetches one or all registered marketplace catalogs.
     * <p>
     * Invokes the method with no params, applying the runtime defaults.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<PluginsMarketplacesRefreshResult> refresh() {
        return refresh(null);
    }

    /**
     * Re-fetches one or all registered marketplace catalogs.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<PluginsMarketplacesRefreshResult> refresh(PluginsMarketplacesRefreshParams params) {
        return caller.invoke("plugins.marketplaces.refresh", params == null ? java.util.Map.of() : params, PluginsMarketplacesRefreshResult.class);
    }

}
