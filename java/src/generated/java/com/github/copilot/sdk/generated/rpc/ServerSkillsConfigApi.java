/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code skills.config} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerSkillsConfigApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerSkillsConfigApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Invokes {@code skills.config.setDisabledSkills}.
     * @since 1.0.0
     */
    public CompletableFuture<Void> setDisabledSkills(SkillsConfigSetDisabledSkillsParams params) {
        return caller.invoke("skills.config.setDisabledSkills", params, Void.class);
    }

}
