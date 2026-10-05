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
 * API methods for the {@code sandbox} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerSandboxApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerSandboxApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Whether the host running this runtime can run the command sandbox. The runtime checks `supported` once per process. A capability answer can change while the process runs, for example after the user installs a missing package.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SandboxGetHostSupportResult> getHostSupport() {
        return caller.invoke("sandbox.getHostSupport", java.util.Map.of(), SandboxGetHostSupportResult.class);
    }

}
