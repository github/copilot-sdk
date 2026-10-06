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
 * API methods for the {@code models} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerModelsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerModelsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Lists Copilot models available to the authenticated user.
     * <p>
     * Invokes the method with no params, applying the runtime defaults.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ModelsListResult> list() {
        return list(null);
    }

    /**
     * Lists Copilot models available to the authenticated user.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ModelsListResult> list(ModelsListParams params) {
        return caller.invoke("models.list", params == null ? java.util.Map.of() : params, ModelsListResult.class);
    }

    /**
     * Returns the running runtime's complete catalog of well-known built-in model IDs without authentication or network access.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ModelsGetBuiltInCatalogResult> getBuiltInCatalog() {
        return caller.invoke("models.getBuiltInCatalog", java.util.Map.of(), ModelsGetBuiltInCatalogResult.class);
    }

}
