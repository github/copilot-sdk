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
 * API methods for the {@code managedSettings} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerManagedSettingsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerManagedSettingsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Validated device-managed settings discovered before a session exists.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ManagedSettingsReadResult> read() {
        return caller.invoke("managedSettings.read", java.util.Map.of(), ManagedSettingsReadResult.class);
    }

    /**
     * Invokes {@code managedSettings.clearCache}.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> clearCache() {
        return caller.invoke("managedSettings.clearCache", java.util.Map.of(), Void.class);
    }

}
