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
 * API methods for the {@code shell} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerShellApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerShellApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Filters the supplied host environment with the runtime's mandatory interactive-shell credential and telemetry exclusions, adds Git hardening, and removes any extra names the host supplies. An IDE terminal host or an in-process CLI uses this before launching an inherited terminal. The result derives only from the caller's supplied environment, never the runtime process environment.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ShellBuildInteractiveEnvironmentResult> buildInteractiveEnvironment(ShellBuildInteractiveEnvironmentParams params) {
        return caller.invoke("shell.buildInteractiveEnvironment", params, ShellBuildInteractiveEnvironmentResult.class);
    }

}
