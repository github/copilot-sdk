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
 * API methods for the {@code mcp.installations} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerMcpInstallationsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerMcpInstallationsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Reads receipt-owned MCP inventory for the selected account and host without activating servers or reconstructing missing ownership. Configuration ownership does not prove session-specific usability.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpInstallationManagementResult> list(McpInstallationsListParams params) {
        return caller.invoke("mcp.installations.list", params, McpInstallationManagementResult.class);
    }

    /**
     * Reconciles already-confirmed durable MCP transactions, then inspects owned inventory. Does not replay apply or reconstruct deleted ownership metadata; unresolved or unsafe evidence remains an explicit refusal.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpInstallationManagementResult> recover(McpInstallationsRecoverParams params) {
        return caller.invoke("mcp.installations.recover", params, McpInstallationManagementResult.class);
    }

    /**
     * Inspects a known operation only on its original connection. Remains available after account or selected-session loss; does not acquire new authority or rebind an operation.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpInstallationManagementResult> status(McpInstallationsStatusParams params) {
        return caller.invoke("mcp.installations.status", params, McpInstallationManagementResult.class);
    }

    /**
     * Requests cancellation of a known operation on its original connection, including before apply or confirmation. Already-started effects retain their transaction lease and report an honest terminal or recovery outcome.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpInstallationManagementResult> cancel(McpInstallationsCancelParams params) {
        return caller.invoke("mcp.installations.cancel", params, McpInstallationManagementResult.class);
    }

}
