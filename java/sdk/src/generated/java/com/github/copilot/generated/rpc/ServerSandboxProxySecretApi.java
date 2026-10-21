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
 * API methods for the {@code sandbox.proxySecret} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class ServerSandboxProxySecretApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerSandboxProxySecretApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Reports where this runtime keeps the sandbox proxy password: the credential store directory and the entry identifier within it. Both are derived from the configuration directory this runtime was started with, so a host does not resolve them itself. Read this to recognize a `${secret:…}` value in stored sandbox settings as this runtime's own proxy password rather than a reference the user wrote by hand. Marked internal because it exists to carry a CLI call site off the napi boundary onto the SDK contract.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SandboxProxySecretLocation> getLocation() {
        return caller.invoke("sandbox.proxySecret.getLocation", java.util.Map.of(), SandboxProxySecretLocation.class);
    }

    /**
     * Reads the stored sandbox proxy password from the credential store `sandbox.proxySecret.getLocation` names, preferring the OS keychain and falling back to the on-disk store. Reports an absent value rather than failing when no entry exists. Marked internal because it exists to carry a CLI call site off the napi boundary onto the SDK contract.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SandboxProxySecretGetResult> get() {
        return caller.invoke("sandbox.proxySecret.get", java.util.Map.of(), SandboxProxySecretGetResult.class);
    }

    /**
     * Stores the sandbox proxy password in the credential store `sandbox.proxySecret.getLocation` names, replacing any existing entry. Reports whether the OS keychain accepted it or it fell back to the on-disk store. Marked internal because it exists to carry a CLI call site off the napi boundary onto the SDK contract.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SandboxProxySecretSaveResult> save(SandboxProxySecretSaveParams params) {
        return caller.invoke("sandbox.proxySecret.save", params, SandboxProxySecretSaveResult.class);
    }

    /**
     * Removes the stored sandbox proxy password from the credential store `sandbox.proxySecret.getLocation` names. Succeeds when no entry exists, so it is safe to call while clearing a proxy password whose origin is unknown. Marked internal because it exists to carry a CLI call site off the napi boundary onto the SDK contract.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> delete() {
        return caller.invoke("sandbox.proxySecret.delete", java.util.Map.of(), Void.class);
    }

}
