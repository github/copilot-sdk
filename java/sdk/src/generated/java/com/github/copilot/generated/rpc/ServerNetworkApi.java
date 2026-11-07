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
 * API methods for the {@code network} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerNetworkApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerNetworkApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Registers the calling SDK client's network callbacks, replacing any previous registration. With `resolveProxy`, the client decides the proxies for every request the runtime would otherwise route by ambient proxy settings (proxy environment variables, `NO_PROXY` and operating-system settings), or leaves a request to that routing; requests with an explicitly configured proxy and loopback targets never consult it. With `authenticateNegotiate`, the client supplies the Kerberos tokens for Negotiate proxy challenges instead of native credential lookup. The callbacks serve requests made for the client's sessions and, with `runtime` scope, the runtime's shared traffic, which includes background requests and remote MCP server connections; only one connection may hold `runtime` scope. The registration ends when the connection closes.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> setProvider(NetworkSetProviderParams params) {
        return caller.invoke("network.setProvider", params, Void.class);
    }

}
