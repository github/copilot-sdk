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
     * New-work inventory or recovery request under an explicitly selected existing session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpInstallationManagementResult> list(McpInstallationsListParams params) {
        return caller.invoke("mcp.installations.list", params, McpInstallationManagementResult.class);
    }

    /**
     * New-work inventory or recovery request under an explicitly selected existing session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpInstallationManagementResult> recover(McpInstallationsRecoverParams params) {
        return caller.invoke("mcp.installations.recover", params, McpInstallationManagementResult.class);
    }

    /**
     * Existing-operation control. A new session selector is deliberately not accepted.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpInstallationManagementResult> status(McpInstallationsStatusParams params) {
        return caller.invoke("mcp.installations.status", params, McpInstallationManagementResult.class);
    }

    /**
     * Existing-operation control. A new session selector is deliberately not accepted.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpInstallationManagementResult> cancel(McpInstallationsCancelParams params) {
        return caller.invoke("mcp.installations.cancel", params, McpInstallationManagementResult.class);
    }

}
