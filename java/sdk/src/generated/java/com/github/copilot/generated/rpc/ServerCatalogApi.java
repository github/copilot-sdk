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
     * A bounded catalog search. Both the query length and the result count are capped by the schema so a caller cannot request an unbounded scan.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<CatalogSearchResult> search(CatalogSearchParams params) {
        return caller.invoke("catalog.search", params, CatalogSearchResult.class);
    }

}
