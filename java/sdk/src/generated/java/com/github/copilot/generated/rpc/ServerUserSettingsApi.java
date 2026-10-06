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
 * API methods for the {@code user.settings} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerUserSettingsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerUserSettingsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Invokes {@code user.settings.reload}.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> reload() {
        return caller.invoke("user.settings.reload", java.util.Map.of(), Void.class);
    }

    /**
     * Per-key metadata for every known user setting (settings.json overlaid with the legacy config.json, config.json wins), including settings left at their default. Excludes repository- and enterprise-managed overrides.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<UserSettingsGetResult> get() {
        return caller.invoke("user.settings.get", java.util.Map.of(), UserSettingsGetResult.class);
    }

    /**
     * Partial user settings to write to settings.json. Each top-level key is written individually, replacing the existing value; a key whose value is null is removed.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<UserSettingsSetResult> set(UserSettingsSetParams params) {
        return caller.invoke("user.settings.set", params, UserSettingsSetResult.class);
    }

}
