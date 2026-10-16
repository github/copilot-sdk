/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code repo} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class ServerRepoApi {

    private final RpcCaller caller;

    /** API methods for the {@code repo.settings} sub-namespace. */
    final ServerRepoSettingsApi settings;

    /** @param caller the RPC transport function */
    ServerRepoApi(RpcCaller caller) {
        this.caller = caller;
        this.settings = new ServerRepoSettingsApi(caller);
    }

}
