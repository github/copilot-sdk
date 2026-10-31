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
 * API methods for the {@code settings} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class ServerSettingsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerSettingsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Removes one key from a settings file addressed by path, leaving every other key untouched. Takes the key exactly as written rather than a canonical setting path, because its purpose is to delete a key the loader refused: a misspelling, or a setting that no longer exists. Refuses rather than overwrites when the file does not parse, so a malformed file is never replaced by a rewrite that drops everything it could not read.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SettingsRemoveRawKeyResult> removeRawKey(SettingsRemoveRawKeyParams params) {
        return caller.invoke("settings.removeRawKey", params, SettingsRemoveRawKeyResult.class);
    }

    /**
     * Replaces the secret-bearing parts of a settings value with a redacted placeholder, so the value can be shown to a user or written to a log. `basePath` is where the value sits in the settings tree, which is what decides whether a field is secret.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SettingsRedactValueForDisplayResult> redactValueForDisplay(SettingsRedactValueForDisplayParams params) {
        return caller.invoke("settings.redactValueForDisplay", params, SettingsRedactValueForDisplayResult.class);
    }

}
