/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code mcp} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerMcpApi {

    private final RpcCaller caller;

    /** API methods for the {@code mcp.config} sub-namespace. */
    public final ServerMcpConfigApi config;

    /** @param caller the RPC transport function */
    ServerMcpApi(RpcCaller caller) {
        this.caller = caller;
        this.config = new ServerMcpConfigApi(caller);
    }

    /**
     * Invokes {@code mcp.discover}.
     * @since 1.0.0
     */
    public CompletableFuture<McpDiscoverResult> discover(McpDiscoverParams params) {
        return caller.invoke("mcp.discover", params, McpDiscoverResult.class);
    }

}
