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
 * API methods for the {@code autopilotObjective} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionAutopilotObjectiveApi {

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionAutopilotObjectiveApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionAutopilotObjectiveGetStateResult> getState() {
        return caller.invoke("session.autopilotObjective.getState", java.util.Map.of("sessionId", this.sessionId), SessionAutopilotObjectiveGetStateResult.class);
    }

}
