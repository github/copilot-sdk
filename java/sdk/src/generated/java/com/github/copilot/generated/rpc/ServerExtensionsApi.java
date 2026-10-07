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
 * API methods for the {@code extensions} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerExtensionsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerExtensionsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Extensions discovered from persisted Copilot home state and their effective loading mode. Launch-scoped additional plugins are not included.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ExtensionsDiscoverResult> discover() {
        return caller.invoke("extensions.discover", java.util.Map.of(), ExtensionsDiscoverResult.class);
    }

    /**
     * Source-qualified extension identifiers to persistently enable for future sessions.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> enable(ExtensionsEnableParams params) {
        return caller.invoke("extensions.enable", params, Void.class);
    }

    /**
     * Source-qualified extension identifiers to persistently disable for future sessions.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> disable(ExtensionsDisableParams params) {
        return caller.invoke("extensions.disable", params, Void.class);
    }

}
