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
 * API methods for the {@code plugins.builtin} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerPluginsBuiltinApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerPluginsBuiltinApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Trusted built-in plugin directories to use for this runtime process.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> set(PluginsBuiltinSetParams params) {
        return caller.invoke("plugins.builtin.set", params, Void.class);
    }

}
