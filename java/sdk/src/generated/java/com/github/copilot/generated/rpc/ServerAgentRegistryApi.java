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
 * API methods for the {@code agentRegistry} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerAgentRegistryApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerAgentRegistryApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Spawns a managed-server child with the supplied configuration and returns a discriminated-union result. The caller (typically the CLI controller) is responsible for attaching to the spawned child and sending any follow-up prompt. When the controller-local spawn gate is closed the server returns JSON-RPC MethodNotFound.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<AgentRegistrySpawnResult> spawn(AgentRegistrySpawnParams params) {
        return caller.invoke("agentRegistry.spawn", params, AgentRegistrySpawnResult.class);
    }

    /**
     * Creates the local agent registry with mode 0700. Requires a trusted SDK host or runtime-direct caller.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<AgentRegistryEnsureDirResult> ensureDir(AgentRegistryEnsureDirParams params) {
        return caller.invoke("agentRegistry.ensureDir", params, AgentRegistryEnsureDirResult.class);
    }

    /**
     * Removes a local agent entry. An absent entry succeeds. Requires a trusted SDK host or runtime-direct caller.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<AgentRegistryRemoveEntryResult> removeEntry(AgentRegistryRemoveEntryParams params) {
        return caller.invoke("agentRegistry.removeEntry", params, AgentRegistryRemoveEntryResult.class);
    }

    /**
     * Lists live local agents without connection credentials. Only stale entries are removed; dead-pid entries remain on disk. Requires a trusted SDK host or runtime-direct caller.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<AgentRegistryListEntriesResult> listEntries(AgentRegistryListEntriesParams params) {
        return caller.invoke("agentRegistry.listEntries", params, AgentRegistryListEntriesResult.class);
    }

}
