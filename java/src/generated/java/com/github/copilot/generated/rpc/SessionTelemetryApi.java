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
 * API methods for the {@code telemetry} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionTelemetryApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionTelemetryApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Feature override key/value pairs to attach to subsequent telemetry events from this session.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> setFeatureOverrides(SessionTelemetrySetFeatureOverridesParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.telemetry.setFeatureOverrides", _p, Void.class);
    }

}
