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
 * API methods for the {@code commands} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerCommandsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerCommandsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Slash commands available in the session, after applying any include/exclude filters.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<CommandsListResult> list() {
        return caller.invoke("commands.list", java.util.Map.of(), CommandsListResult.class);
    }

}
