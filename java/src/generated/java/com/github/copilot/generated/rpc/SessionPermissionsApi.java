/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

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

    /** API methods for the {@code permissions.paths} sub-namespace. */
    public final SessionPermissionsPathsApi paths;
    /** API methods for the {@code permissions.locations} sub-namespace. */
    public final SessionPermissionsLocationsApi locations;
    /** API methods for the {@code permissions.folderTrust} sub-namespace. */
    public final SessionPermissionsFolderTrustApi folderTrust;
    /** API methods for the {@code permissions.urls} sub-namespace. */
    public final SessionPermissionsUrlsApi urls;

    /** @param caller the RPC transport function */
    SessionPermissionsApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
        this.paths = new SessionPermissionsPathsApi(caller, sessionId);
        this.locations = new SessionPermissionsLocationsApi(caller, sessionId);
        this.folderTrust = new SessionPermissionsFolderTrustApi(caller, sessionId);
        this.urls = new SessionPermissionsUrlsApi(caller, sessionId);
    }

    /**
     * Patch of permission policy fields to apply (omit a field to leave it unchanged).
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionPermissionsConfigureResult> configure(SessionPermissionsConfigureParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.permissions.configure", _p, SessionPermissionsConfigureResult.class);
    }

    /**
     * Pending permission request ID and the decision to apply (approve/reject and scope).
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionPermissionsHandlePendingPermissionRequestResult> handlePendingPermissionRequest(SessionPermissionsHandlePendingPermissionRequestParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.permissions.handlePendingPermissionRequest", _p, SessionPermissionsHandlePendingPermissionRequestResult.class);
    }

    /**
     * No parameters; returns currently-pending permission requests for the session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionPermissionsPendingRequestsResult> pendingRequests() {
        return caller.invoke("session.permissions.pendingRequests", java.util.Map.of("sessionId", this.sessionId), SessionPermissionsPendingRequestsResult.class);
    }

    /**
     * Allow-all toggle for tool permission requests, with an optional telemetry source.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionPermissionsSetApproveAllResult> setApproveAll(SessionPermissionsSetApproveAllParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.permissions.setApproveAll", _p, SessionPermissionsSetApproveAllResult.class);
    }

    /**
     * Scope and add/remove instructions for modifying session- or location-scoped permission rules.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionPermissionsModifyRulesResult> modifyRules(SessionPermissionsModifyRulesParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.permissions.modifyRules", _p, SessionPermissionsModifyRulesResult.class);
    }

    /**
     * Toggles whether permission prompts should be bridged into session events for this client.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionPermissionsSetRequiredResult> setRequired(SessionPermissionsSetRequiredParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.permissions.setRequired", _p, SessionPermissionsSetRequiredResult.class);
    }

    /**
     * No parameters; clears all session-scoped tool permission approvals.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionPermissionsResetSessionApprovalsResult> resetSessionApprovals() {
        return caller.invoke("session.permissions.resetSessionApprovals", java.util.Map.of("sessionId", this.sessionId), SessionPermissionsResetSessionApprovalsResult.class);
    }

    /**
     * Notification payload describing the permission prompt that the client just rendered.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionPermissionsNotifyPromptShownResult> notifyPromptShown(SessionPermissionsNotifyPromptShownParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.permissions.notifyPromptShown", _p, SessionPermissionsNotifyPromptShownResult.class);
    }

}
