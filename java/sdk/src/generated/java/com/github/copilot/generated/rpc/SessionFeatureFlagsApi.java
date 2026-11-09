/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code featureFlags} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionFeatureFlagsApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionFeatureFlagsApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Configures the bound session's original feature service without changing its owner or assignment account. Explicit assignments disable online fetching. Configuration does not write user settings. Extensions and unrelated external peers cannot access this service.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionFeatureFlagsConfigureResult> configure(SessionFeatureFlagsConfigureParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.featureFlags.configure", _p, SessionFeatureFlagsConfigureResult.class);
    }

    /**
     * Reads the bound session's feature service and requested runtime evaluations. Omission of readiness options returns the current snapshot without waiting for online assignments. A revision barrier uses the original service notification and ends when this attachment closes. The opt-in displayOnly mode returns a redacted UI projection without assignment data or requested evaluations. Only the trusted runtime host can read that generation-bound projection.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionFeatureFlagsReadResult> read(SessionFeatureFlagsReadParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.featureFlags.read", _p, SessionFeatureFlagsReadResult.class);
    }

    /**
     * Reads the bound session's feature service and requested runtime evaluations. Omission of readiness options returns the current snapshot without waiting for online assignments. A revision barrier uses the original service notification and ends when this attachment closes. The opt-in displayOnly mode returns a redacted UI projection without assignment data or requested evaluations. Only the trusted runtime host can read that generation-bound projection.
     * <p>
     * Accepts the extensible request, including inputs added after the params record.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionFeatureFlagsReadResult> read(SessionFeatureFlagsReadRequest request) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(Objects.requireNonNull(request, "request"));
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.featureFlags.read", _p, SessionFeatureFlagsReadResult.class);
    }

    /**
     * Updates the original session feature service through its existing config merge, explicit unset, assignment replacement, and context operations. Changes apply in request field order and do not write settings to disk.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionFeatureFlagsUpdateResult> update(SessionFeatureFlagsUpdateParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.featureFlags.update", _p, SessionFeatureFlagsUpdateResult.class);
    }

}
