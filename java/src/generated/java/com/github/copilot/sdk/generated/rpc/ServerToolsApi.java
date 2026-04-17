/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code tools} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerToolsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerToolsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Invokes {@code tools.list}.
     * @since 1.0.0
     */
    public CompletableFuture<ToolsListResult> list(ToolsListParams params) {
        return caller.invoke("tools.list", params, ToolsListResult.class);
    }

}
