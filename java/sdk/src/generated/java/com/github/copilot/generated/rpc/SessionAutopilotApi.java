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
 * API methods for the {@code autopilot} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class SessionAutopilotApi {

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionAutopilotApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Applies the runtime-owned Autopilot continuation policy at the session-idle decision point and routes any admitted continuation through the normal session send lifecycle. Only the SDK connection that owns the session can invoke this method.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<AutopilotEvaluateIdleContinuationResult> evaluateIdleContinuation() {
        return caller.invoke("session.autopilot.evaluateIdleContinuation", java.util.Map.of("sessionId", this.sessionId), AutopilotEvaluateIdleContinuationResult.class);
    }

}
