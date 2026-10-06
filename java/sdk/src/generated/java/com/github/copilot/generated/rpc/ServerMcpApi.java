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
     * Discovers MCP servers from user, workspace, plugin, and builtin sources.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpDiscoverResult> discover(McpDiscoverParams params) {
        return caller.invoke("mcp.discover", params, McpDiscoverResult.class);
    }

    /**
     * Requests a side-effect-free MCP install plan from a catalog candidate handle or a caller-supplied card. This host-implemented server method is available through SDK/TUI hosts; standalone and C-ABI runtimes whose host does not implement server-method dispatch return JSON-RPC MethodNotFound. A runtime with planning available returns a normalised plan and opaque single-use plan handle; a runtime without it returns the typed planning-unavailable result. A completed plan reports resource identity, provenance, eligible transport choices, the user-scope target, required typed values and secret placeholders, the policy result, the configuration changes installing would make, and whether a reload would be needed. Planning never writes configuration, stores a secret, or reloads MCP servers, so abandoning a plan needs no call and leaves nothing behind.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpPlanInstallResult> planInstall(McpPlanInstallParams params) {
        return caller.invoke("mcp.planInstall", params, McpPlanInstallResult.class);
    }

    /**
     * Requests a side-effect-free MCP install plan from a catalog candidate handle or a caller-supplied card. This host-implemented server method is available through SDK/TUI hosts; standalone and C-ABI runtimes whose host does not implement server-method dispatch return JSON-RPC MethodNotFound. A runtime with planning available returns a normalised plan and opaque single-use plan handle; a runtime without it returns the typed planning-unavailable result. A completed plan reports resource identity, provenance, eligible transport choices, the user-scope target, required typed values and secret placeholders, the policy result, the configuration changes installing would make, and whether a reload would be needed. Planning never writes configuration, stores a secret, or reloads MCP servers, so abandoning a plan needs no call and leaves nothing behind.
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
     * Consumes a bound catalogue plan and retains one exact fully resolved personal remote MCP operation requiring no supplied values or configured secrets. Returns its runtime operation ID and original expiry before any confirmation, activation, writer initialisation or installation effect. Register the original connection, operation and selected-session binding before calling applyInstall. Missing lower owned admission is unavailable, never a raw-config fallback.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpInstallationManagementResult> prepareInstall(McpPrepareInstallParams params) {
        return caller.invoke("mcp.prepareInstall", params, McpInstallationManagementResult.class);
    }

    /**
     * Consumes a retained prepared MCP operation once, revalidates its original authority, requests explicit human consent through installations.confirm on the original connection, then revalidates source and applies the sealed transaction. An uncertain result requires original-operation inspection or recovery, never replay.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpInstallationResult> applyInstall(McpApplyInstallParams params) {
        return caller.invoke("mcp.applyInstall", params, McpInstallationResult.class);
    }

    /**
     * Prepares a read-only removal plan for an exact owned receipt under the selected existing session. Returns the original operation ID before confirmation; neither planning nor abandonment changes configuration or shared OAuth credentials.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpInstallationManagementResult> planUninstall(McpPlanUninstallParams params) {
        return caller.invoke("mcp.planUninstall", params, McpInstallationManagementResult.class);
    }

    /**
     * Consumes the original owned-removal plan once and requests fresh exact human confirmation on its original connection. Drift is refused; unrelated manual configuration and shared OAuth credentials are preserved.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpInstallationResult> applyUninstall(McpApplyUninstallParams params) {
        return caller.invoke("mcp.applyUninstall", params, McpInstallationResult.class);
    }

}
