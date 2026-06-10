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
 * API methods for the {@code instructions} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerInstructionsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerInstructionsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Optional project paths to include in instruction discovery.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<InstructionsDiscoverResult> discover(InstructionsDiscoverParams params) {
        return caller.invoke("instructions.discover", params, InstructionsDiscoverResult.class);
    }

}
