/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.copilot.CopilotExperimental;
import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code sandbox.settings} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class ServerSandboxSettingsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerSandboxSettingsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Resolves a stored sandbox settings object into the effective one, filling every value the user left unset with its default. Read this to render or reason about the sandbox configuration a session would actually run under, rather than the sparse document the user wrote.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<JsonNode> applyDefaults(SandboxSettingsApplyDefaultsParams params) {
        return caller.invoke("sandbox.settings.applyDefaults", params, JsonNode.class);
    }

}
