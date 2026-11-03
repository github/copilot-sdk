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
 * API methods for the {@code cafeMetrics} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class SessionCafeMetricsApi {

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionCafeMetricsApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Records live model-call events, including subagents, and root compaction events. Replaces any earlier recorder. Requires the private trusted CLI host capability.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> attach() {
        return caller.invoke("session.cafeMetrics.attach", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

    /**
     * Stops recording this session. Does nothing if it has no recorder. Requires the private trusted CLI host capability.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> detach() {
        return caller.invoke("session.cafeMetrics.detach", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

}
