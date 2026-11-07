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
 * API methods for the {@code decisions} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerDecisionsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerDecisionsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Asks a decision model yes/no, pick-one, or rating questions about shared context and returns normalized probabilities. Sessionless, so a host can classify or route a request before any session exists. Requires the `DECISIONS_API` feature flag. A failed request is a JSON-RPC error whose `data` is a `DecisionErrorData`.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<DecisionsAskResult> ask(DecisionsAskParams params) {
        return caller.invoke("decisions.ask", params, DecisionsAskResult.class);
    }

}
