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
 * API methods for the {@code lsp} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerLspApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerLspApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Reads the process-wide language-server inventory for a working directory without starting servers. Uses the host configuration home and persisted workspace trust.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<LspListResult> list(LspListParams params) {
        return caller.invoke("lsp.list", params, LspListResult.class);
    }

}
