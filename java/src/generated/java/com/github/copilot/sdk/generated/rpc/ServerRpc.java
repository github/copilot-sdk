/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * Typed client for server-level RPC methods.
 * <p>
 * Provides strongly-typed access to all server-level API namespaces.
 * <p>
 * Obtain an instance by calling {@code new ServerRpc(caller)}.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerRpc {

    private final RpcCaller caller;

    /** API methods for the {@code models} namespace. */
    public final ServerModelsApi models;
    /** API methods for the {@code tools} namespace. */
    public final ServerToolsApi tools;
    /** API methods for the {@code account} namespace. */
    public final ServerAccountApi account;
    /** API methods for the {@code mcp} namespace. */
    public final ServerMcpApi mcp;
    /** API methods for the {@code sessionFs} namespace. */
    public final ServerSessionFsApi sessionFs;
    /** API methods for the {@code sessions} namespace. */
    public final ServerSessionsApi sessions;

    /**
     * Creates a new server RPC client.
     *
     * @param caller the RPC transport function (e.g., {@code jsonRpcClient::invoke})
     */
    public ServerRpc(RpcCaller caller) {
        this.caller = caller;
        this.models = new ServerModelsApi(caller);
        this.tools = new ServerToolsApi(caller);
        this.account = new ServerAccountApi(caller);
        this.mcp = new ServerMcpApi(caller);
        this.sessionFs = new ServerSessionFsApi(caller);
        this.sessions = new ServerSessionsApi(caller);
    }

    /**
     * Invokes {@code ping}.
     * @since 1.0.0
     */
    public CompletableFuture<PingResult> ping(PingParams params) {
        return caller.invoke("ping", params, PingResult.class);
    }

}
