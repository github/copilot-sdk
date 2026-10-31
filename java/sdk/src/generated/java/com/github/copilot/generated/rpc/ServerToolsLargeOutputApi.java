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
 * API methods for the {@code tools.largeOutput} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class ServerToolsLargeOutputApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerToolsLargeOutputApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Removes expired tool output files using the runtime's retention policy. Refuses extension callers.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> cleanup() {
        return caller.invoke("tools.largeOutput.cleanup", java.util.Map.of(), Void.class);
    }

}
