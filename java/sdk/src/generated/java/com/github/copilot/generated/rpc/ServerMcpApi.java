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
     * Optional working directory used as context for MCP server discovery.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpDiscoverResult> discover(McpDiscoverParams params) {
        return caller.invoke("mcp.discover", params, McpDiscoverResult.class);
    }

    /**
     * A side-effect-free request for an MCP install plan. Computing a plan never writes configuration, stores a secret, or reloads MCP servers.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpPlanInstallResult> planInstall(McpPlanInstallParams params) {
        return caller.invoke("mcp.planInstall", params, McpPlanInstallResult.class);
    }

}
