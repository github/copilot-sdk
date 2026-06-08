/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

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
     * All registered marketplaces, including built-in defaults.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<PluginsMarketplacesListResult> list() {
        return caller.invoke("plugins.marketplaces.list", java.util.Map.of(), PluginsMarketplacesListResult.class);
    }

    /**
     * Marketplace source to register.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<PluginsMarketplacesAddResult> add(PluginsMarketplacesAddParams params) {
        return caller.invoke("plugins.marketplaces.add", params, PluginsMarketplacesAddResult.class);
    }

    /**
     * Name of the marketplace to remove and an optional force flag.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<PluginsMarketplacesRemoveResult> remove(PluginsMarketplacesRemoveParams params) {
        return caller.invoke("plugins.marketplaces.remove", params, PluginsMarketplacesRemoveResult.class);
    }

    /**
     * Name of the marketplace whose plugin catalog to fetch.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<PluginsMarketplacesBrowseResult> browse(PluginsMarketplacesBrowseParams params) {
        return caller.invoke("plugins.marketplaces.browse", params, PluginsMarketplacesBrowseResult.class);
    }

    /**
     * Optional marketplace name; omit to refresh all.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<PluginsMarketplacesRefreshResult> refresh() {
        return caller.invoke("plugins.marketplaces.refresh", java.util.Map.of(), PluginsMarketplacesRefreshResult.class);
    }

}
