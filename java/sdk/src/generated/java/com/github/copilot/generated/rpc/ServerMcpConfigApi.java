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
     * Lists MCP servers from user configuration.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpConfigListResult> list() {
        return caller.invoke("mcp.config.list", java.util.Map.of(), McpConfigListResult.class);
    }

    /**
     * Adds an MCP server to user configuration.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> add(McpConfigAddParams params) {
        return caller.invoke("mcp.config.add", params, Void.class);
    }

    /**
     * Updates an MCP server in user configuration.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> update(McpConfigUpdateParams params) {
        return caller.invoke("mcp.config.update", params, Void.class);
    }

    /**
     * Removes an MCP server from user configuration.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> remove(McpConfigRemoveParams params) {
        return caller.invoke("mcp.config.remove", params, Void.class);
    }

    /**
     * Enables MCP servers in user configuration for new sessions.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> enable(McpConfigEnableParams params) {
        return caller.invoke("mcp.config.enable", params, Void.class);
    }

    /**
     * Disables MCP servers in user configuration for new sessions.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> disable(McpConfigDisableParams params) {
        return caller.invoke("mcp.config.disable", params, Void.class);
    }

    /**
     * Drops this runtime process's in-memory MCP server-definition cache so the next MCP config read observes disk.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> reload() {
        return caller.invoke("mcp.config.reload", java.util.Map.of(), Void.class);
    }

}
