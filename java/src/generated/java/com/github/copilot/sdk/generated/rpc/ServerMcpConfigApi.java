/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code mcp.config} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerMcpConfigApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerMcpConfigApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Invokes {@code mcp.config.list}.
     * @since 1.0.0
     */
    public CompletableFuture<McpConfigListResult> list() {
        return caller.invoke("mcp.config.list", java.util.Map.of(), McpConfigListResult.class);
    }

    /**
     * Invokes {@code mcp.config.add}.
     * @since 1.0.0
     */
    public CompletableFuture<Void> add(McpConfigAddParams params) {
        return caller.invoke("mcp.config.add", params, Void.class);
    }

    /**
     * Invokes {@code mcp.config.update}.
     * @since 1.0.0
     */
    public CompletableFuture<Void> update(McpConfigUpdateParams params) {
        return caller.invoke("mcp.config.update", params, Void.class);
    }

    /**
     * Invokes {@code mcp.config.remove}.
     * @since 1.0.0
     */
    public CompletableFuture<Void> remove(McpConfigRemoveParams params) {
        return caller.invoke("mcp.config.remove", params, Void.class);
    }

    /**
     * Invokes {@code mcp.config.enable}.
     * @since 1.0.0
     */
    public CompletableFuture<Void> enable(McpConfigEnableParams params) {
        return caller.invoke("mcp.config.enable", params, Void.class);
    }

    /**
     * Invokes {@code mcp.config.disable}.
     * @since 1.0.0
     */
    public CompletableFuture<Void> disable(McpConfigDisableParams params) {
        return caller.invoke("mcp.config.disable", params, Void.class);
    }

}
