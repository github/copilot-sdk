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
 * API methods for the {@code connectors} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerConnectorsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerConnectorsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Returns feature availability.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ConnectorsGetCapabilitiesResult> getCapabilities() {
        return caller.invoke("connectors.getCapabilities", java.util.Map.of(), ConnectorsGetCapabilitiesResult.class);
    }

    /**
     * Returns eligible accounts.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ConnectorsGetAccountsResult> getAccounts() {
        return caller.invoke("connectors.getAccounts", java.util.Map.of(), ConnectorsGetAccountsResult.class);
    }

    /**
     * Lists entries for the selected account.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ConnectorsListResult> list(ConnectorsListParams params) {
        return caller.invoke("connectors.list", params, ConnectorsListResult.class);
    }

    /**
     * Refreshes entries for the selected account.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ConnectorsRefreshResult> refresh(ConnectorsRefreshParams params) {
        return caller.invoke("connectors.refresh", params, ConnectorsRefreshResult.class);
    }

}
