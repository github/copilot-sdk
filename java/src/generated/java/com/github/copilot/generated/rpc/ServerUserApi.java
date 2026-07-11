/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code user} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerUserApi {

    private final RpcCaller caller;

    /** API methods for the {@code user.settings} sub-namespace. */
    public final ServerUserSettingsApi settings;

    /** @param caller the RPC transport function */
    ServerUserApi(RpcCaller caller) {
        this.caller = caller;
        this.settings = new ServerUserSettingsApi(caller);
    }

}
