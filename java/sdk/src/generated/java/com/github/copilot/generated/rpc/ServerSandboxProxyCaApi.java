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
     * Reports whether the persistent certificate authority of the sandbox credential proxy exists, whether OS trust includes it, and whether it must be rotated. Changes nothing.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SandboxProxyCaGetStatusResult> getStatus(SandboxProxyCaGetStatusParams params) {
        return caller.invoke("sandbox.proxyCa.getStatus", params, SandboxProxyCaGetStatusResult.class);
    }

    /**
     * Creates the persistent certificate authority of the sandbox credential proxy if none is stored, without changing OS trust, and returns the path of its public certificate. Keeps an existing certificate authority, even one that must be rotated. Fails where OS trust is unsupported. Trust it with sandbox.proxyCa.trust: the CLI trusts only the hosts in the saved user settings, so it refuses a certificate authority that also covers hosts from sandboxConfig.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SandboxProxyCaCreateResult> create(SandboxProxyCaCreateParams params) {
        return caller.invoke("sandbox.proxyCa.create", params, SandboxProxyCaCreateResult.class);
    }

    /**
     * Replaces the persistent certificate authority of the sandbox credential proxy with a new one for the current credential hosts. If OS trust included the old one, removes it and trusts the new one, which can show an OS authentication prompt. Running sandboxed tools keep the old certificate authority until they restart.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SandboxProxyCaRotateResult> rotate(SandboxProxyCaRotateParams params) {
        return caller.invoke("sandbox.proxyCa.rotate", params, SandboxProxyCaRotateResult.class);
    }

    /**
     * Adds the persistent certificate authority of the sandbox credential proxy to OS trust, so sandboxed clients that read only OS trust accept the proxy. Call create first. Refuses a certificate authority that is not constrained to the current credential hosts. Can show an OS authentication prompt.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SandboxProxyCaTrustResult> trust(SandboxProxyCaTrustParams params) {
        return caller.invoke("sandbox.proxyCa.trust", params, SandboxProxyCaTrustResult.class);
    }

    /**
     * Removes the persistent certificate authority of the sandbox credential proxy from OS trust. Keeps the stored certificate authority. Can show an OS authentication prompt. Sandboxed clients that read only OS trust then reject the proxy; clients that read the per-process certificate bundle continue to work.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SandboxProxyCaRemoveResult> remove() {
        return caller.invoke("sandbox.proxyCa.remove", java.util.Map.of(), SandboxProxyCaRemoveResult.class);
    }

}
