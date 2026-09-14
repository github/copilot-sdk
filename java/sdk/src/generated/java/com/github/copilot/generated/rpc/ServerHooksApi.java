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
 * API methods for the {@code hooks} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerHooksApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerHooksApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Optional project paths and host-exclusion behavior for server-scoped hook discovery.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<HooksDiscoverResult> discover(HooksDiscoverParams params) {
        return caller.invoke("hooks.discover", params, HooksDiscoverResult.class);
    }

}
