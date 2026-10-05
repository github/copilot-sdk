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
 * API methods for the {@code sandbox.proxyCa} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerSandboxProxyCaApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerSandboxProxyCaApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Identifies the credential hosts that the persistent certificate authority of the sandbox credential proxy must cover. The runtime always adds the hosts from the saved user settings.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SandboxProxyCaGetStatusResult> getStatus(SandboxProxyCaGetStatusParams params) {
        return caller.invoke("sandbox.proxyCa.getStatus", params, SandboxProxyCaGetStatusResult.class);
    }

    /**
     * Identifies the credential hosts that the persistent certificate authority of the sandbox credential proxy must cover. The runtime always adds the hosts from the saved user settings.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SandboxProxyCaCreateResult> create(SandboxProxyCaCreateParams params) {
        return caller.invoke("sandbox.proxyCa.create", params, SandboxProxyCaCreateResult.class);
    }

    /**
     * Identifies the credential hosts that the persistent certificate authority of the sandbox credential proxy must cover. The runtime always adds the hosts from the saved user settings.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SandboxProxyCaRotateResult> rotate(SandboxProxyCaRotateParams params) {
        return caller.invoke("sandbox.proxyCa.rotate", params, SandboxProxyCaRotateResult.class);
    }

    /**
     * Identifies the credential hosts that the persistent certificate authority of the sandbox credential proxy must cover. The runtime always adds the hosts from the saved user settings.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SandboxProxyCaTrustResult> trust(SandboxProxyCaTrustParams params) {
        return caller.invoke("sandbox.proxyCa.trust", params, SandboxProxyCaTrustResult.class);
    }

    /**
     * Status of the persistent certificate authority of the sandbox credential proxy.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SandboxProxyCaRemoveResult> remove() {
        return caller.invoke("sandbox.proxyCa.remove", java.util.Map.of(), SandboxProxyCaRemoveResult.class);
    }

}
