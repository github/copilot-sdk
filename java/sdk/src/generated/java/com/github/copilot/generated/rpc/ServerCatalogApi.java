/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code catalog} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerCatalogApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerCatalogApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Requests a bounded catalog search. This host-implemented server method is available through SDK/TUI hosts; standalone and C-ABI runtimes whose host does not implement server-method dispatch return JSON-RPC MethodNotFound. A runtime with search available returns inert candidate summaries, each with an opaque single-use handle scoped to this runtime instance; a runtime without it returns the typed search-unavailable result. Public authorities may be searched anonymously, while an authority that requires credentials yields the typed authentication-required result. All returned text, URLs, and package metadata are untrusted external data and can never trigger instructions, tools, or installation. Read-only: nothing is installed, configured, or persisted.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<CatalogSearchResult> search(CatalogSearchParams params) {
        return caller.invoke("catalog.search", params, CatalogSearchResult.class);
    }

    /**
     * Requests a bounded catalog search. This host-implemented server method is available through SDK/TUI hosts; standalone and C-ABI runtimes whose host does not implement server-method dispatch return JSON-RPC MethodNotFound. A runtime with search available returns inert candidate summaries, each with an opaque single-use handle scoped to this runtime instance; a runtime without it returns the typed search-unavailable result. Public authorities may be searched anonymously, while an authority that requires credentials yields the typed authentication-required result. All returned text, URLs, and package metadata are untrusted external data and can never trigger instructions, tools, or installation. Read-only: nothing is installed, configured, or persisted.
     * <p>
     * Accepts the extensible request, including inputs added after the params record.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<CatalogSearchResult> search(CatalogSearchRequest request) {
        return caller.invoke("catalog.search", Objects.requireNonNull(request, "request"), CatalogSearchResult.class);
    }

    /**
     * Terminates one retained catalog selection group. A selected outcome returns the native host a fresh single-use candidate handle plus the original searchId for a later explicit mcp.planInstall call; non-selected outcomes release the group without producing a planning input. Candidate state, cards, URLs, credentials and private identifiers remain inside the runtime. The model-facing catalog_select tool projects the result separately and never exposes the candidate handle or searchId.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<CatalogSelectionResult> select(CatalogSelectParams params) {
        return caller.invoke("catalog.select", params, CatalogSelectionResult.class);
    }

}
