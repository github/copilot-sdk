/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code permissions} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionPermissionsApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionPermissionsApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Pending permission request ID and the decision to apply (approve/reject and scope).
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     * @since 1.0.0
     */
    public CompletableFuture<SessionPermissionsHandlePendingPermissionRequestResult> handlePendingPermissionRequest(SessionPermissionsHandlePendingPermissionRequestParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.permissions.handlePendingPermissionRequest", _p, SessionPermissionsHandlePendingPermissionRequestResult.class);
    }

    /**
     * Whether to auto-approve all tool permission requests for the rest of the session.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     * @since 1.0.0
     */
    public CompletableFuture<SessionPermissionsSetApproveAllResult> setApproveAll(SessionPermissionsSetApproveAllParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.permissions.setApproveAll", _p, SessionPermissionsSetApproveAllResult.class);
    }

    /**
     * No parameters; clears all session-scoped tool permission approvals.
     * @since 1.0.0
     */
    public CompletableFuture<SessionPermissionsResetSessionApprovalsResult> resetSessionApprovals() {
        return caller.invoke("session.permissions.resetSessionApprovals", java.util.Map.of("sessionId", this.sessionId), SessionPermissionsResetSessionApprovalsResult.class);
    }

}
