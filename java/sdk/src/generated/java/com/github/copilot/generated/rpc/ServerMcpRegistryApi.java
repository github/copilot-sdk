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
 * API methods for the {@code mcp.registry} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class ServerMcpRegistryApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerMcpRegistryApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Allocates an ID for one cancellable MCP registry search. The ID exists before the search starts, so callers can cancel before it starts. The networking stack supplies the cancellation namespace. The runtime retains at most 1,024 unused IDs. At capacity, another allocation can reclaim an unused ID. Active searches retain their IDs until they finish.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<McpRegistryRequestIdResult> allocateRequestId() {
        return caller.invoke("mcp.registry.allocateRequestId", java.util.Map.of(), McpRegistryRequestIdResult.class);
    }

    /**
     * Searches the MCP registry the supplied credential may read, resolving the registry endpoint from policy first. Hosts usually send credential-free `AuthIdentity`; a `token` identity can be resolved only when it embeds a token, while `env` and `gh-cli` identities can use a token embedded in the request first. The runtime follows registry pages, keeps the newest entry per server name, cuts the list to `limit`, and sorts an empty-query result by GitHub stars. Each server object is carried opaquely.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<McpRegistrySearchResult> search(McpRegistrySearchParams params) {
        return caller.invoke("mcp.registry.search", params, McpRegistrySearchResult.class);
    }

    /**
     * Abandons the registry search that uses the given request ID. It acts only on IDs from `mcp.registry.allocateRequestId`, so it never cancels another component's request. Answers `canceled: true` when it stops a running search. Answers `canceled: false` for unknown or reclaimed IDs, completed or canceled searches, and unused reservations. It releases an unused reservation, so a later search with that ID is refused.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<McpRegistryCancelResult> cancel(McpRegistryCancelParams params) {
        return caller.invoke("mcp.registry.cancel", params, McpRegistryCancelResult.class);
    }

}
