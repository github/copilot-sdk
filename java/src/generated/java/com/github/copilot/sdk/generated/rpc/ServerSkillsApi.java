/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code skills} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerSkillsApi {

    private final RpcCaller caller;

    /** API methods for the {@code skills.config} sub-namespace. */
    public final ServerSkillsConfigApi config;

    /** @param caller the RPC transport function */
    ServerSkillsApi(RpcCaller caller) {
        this.caller = caller;
        this.config = new ServerSkillsConfigApi(caller);
    }

    /**
     * Invokes {@code skills.discover}.
     * @since 1.0.0
     */
    public CompletableFuture<SkillsDiscoverResult> discover(SkillsDiscoverParams params) {
        return caller.invoke("skills.discover", params, SkillsDiscoverResult.class);
    }

}
