/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import java.util.Objects;
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
    /** API methods for the {@code mcp.installations} sub-namespace. */
    public final ServerMcpInstallationsApi installations;

    /** @param caller the RPC transport function */
    ServerMcpApi(RpcCaller caller) {
        this.caller = caller;
        this.config = new ServerMcpConfigApi(caller);
        this.installations = new ServerMcpInstallationsApi(caller);
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

    /**
     * A side-effect-free request for an MCP install plan. Computing a plan never writes configuration, stores a secret, or reloads MCP servers.
     * <p>
     * Accepts the extensible request, including inputs added after the params record.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpPlanInstallResult> planInstall(McpPlanInstallRequest request) {
        return caller.invoke("mcp.planInstall", Objects.requireNonNull(request, "request"), McpPlanInstallResult.class);
    }

    /**
     * Side-effect-free preparation of one original bound, input-free remote MCP choice.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpInstallationManagementResult> prepareInstall(McpPrepareInstallParams params) {
        return caller.invoke("mcp.prepareInstall", params, McpInstallationManagementResult.class);
    }

    /**
     * Applies exactly one previously prepared operation on its original connection.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpInstallationResult> applyInstall(McpApplyInstallParams params) {
        return caller.invoke("mcp.applyInstall", params, McpInstallationResult.class);
    }

    /**
     * Read-only preparation of one owned removal under fresh selected-session authority.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpInstallationManagementResult> planUninstall(McpPlanUninstallParams params) {
        return caller.invoke("mcp.planUninstall", params, McpInstallationManagementResult.class);
    }

    /**
     * One-use application of the exact retained removal plan.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpInstallationResult> applyUninstall(McpApplyUninstallParams params) {
        return caller.invoke("mcp.applyUninstall", params, McpInstallationResult.class);
    }

}
