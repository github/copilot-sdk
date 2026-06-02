/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

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
     * @since 1.0.0
     */
    public CompletableFuture<Void> reload() {
        return caller.invoke("user.settings.reload", java.util.Map.of(), Void.class);
    }

}
