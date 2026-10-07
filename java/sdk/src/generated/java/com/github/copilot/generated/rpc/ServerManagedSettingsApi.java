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

    /**
     * Optional opaque account selection or GitHub token whose managed settings are resolved.
     * <p>
     * Invokes the method with no params, applying the runtime defaults.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ManagedSettingsResolveResult> resolve() {
        return resolve(null);
    }

    /**
     * Optional opaque account selection or GitHub token whose managed settings are resolved.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ManagedSettingsResolveResult> resolve(ManagedSettingsResolveParams params) {
        return caller.invoke("managedSettings.resolve", params == null ? java.util.Map.of() : params, ManagedSettingsResolveResult.class);
    }

    /**
     * The authoring JSON schema for managed settings recognized by this runtime.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ManagedSettingsSchemaResult> schema() {
        return caller.invoke("managedSettings.schema", java.util.Map.of(), ManagedSettingsSchemaResult.class);
    }

    /**
     * A candidate managed-settings document to validate without applying it.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ManagedSettingsValidateResult> validate(ManagedSettingsValidateParams params) {
        return caller.invoke("managedSettings.validate", params, ManagedSettingsValidateResult.class);
    }

    /**
     * Candidate managed-settings documents to merge without applying them.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ManagedSettingsComposeResult> compose(ManagedSettingsComposeParams params) {
        return caller.invoke("managedSettings.compose", params, ManagedSettingsComposeResult.class);
    }

}
