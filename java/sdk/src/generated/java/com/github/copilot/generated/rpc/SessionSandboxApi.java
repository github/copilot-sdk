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
 * API methods for the {@code sandbox} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionSandboxApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionSandboxApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Returns whether managed policy requires sandbox enforcement and whether an enforcement failure has permanently blocked the session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionSandboxGetEnforcementStatusResult> getEnforcementStatus() {
        return caller.invoke("session.sandbox.getEnforcementStatus", java.util.Map.of("sessionId", this.sessionId), SessionSandboxGetEnforcementStatusResult.class);
    }

    /**
     * Disables sandboxing for the remainder of the current session and approves the referenced pending sandbox-bypass permission request. The request is rejected unless the exact request is still pending and the effective sandbox policy permits bypass.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionSandboxDisableForSessionResult> disableForSession(SessionSandboxDisableForSessionParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.sandbox.disableForSession", _p, SessionSandboxDisableForSessionResult.class);
    }

    /**
     * Adds the path offered by a pending sandbox escalation permission request's sandboxPathGrant to the session's sandbox policy and approves the request, so the blocked operation re-runs inside the sandbox rather than outside it. The request is rejected unless the exact request is still pending, carries a sandboxPathGrant, and the grant still takes effect under the current managed policy. Does not persist the path; hosts that store sandbox settings save it themselves.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionSandboxGrantPathForRequestResult> grantPathForRequest(SessionSandboxGrantPathForRequestParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.sandbox.grantPathForRequest", _p, SessionSandboxGrantPathForRequestResult.class);
    }

}
