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
     * Replaces selected permission policy fields (rules, paths, URLs, exclusions, allow-all flags) on the session.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionPermissionsConfigureResult> configure(SessionPermissionsConfigureParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.permissions.configure", _p, SessionPermissionsConfigureResult.class);
    }

    /**
     * Provides a decision for a pending tool permission request.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionPermissionsHandlePendingPermissionRequestResult> handlePendingPermissionRequest(SessionPermissionsHandlePendingPermissionRequestParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.permissions.handlePendingPermissionRequest", _p, SessionPermissionsHandlePendingPermissionRequestResult.class);
    }

    /**
     * Reconstructs the set of pending tool permission requests from the session's event history.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionPermissionsPendingRequestsResult> pendingRequests() {
        return caller.invoke("session.permissions.pendingRequests", java.util.Map.of("sessionId", this.sessionId), SessionPermissionsPendingRequestsResult.class);
    }

    /**
     * Enables or disables automatic approval of tool permission requests for the session.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionPermissionsSetApproveAllResult> setApproveAll(SessionPermissionsSetApproveAllParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.permissions.setApproveAll", _p, SessionPermissionsSetApproveAllResult.class);
    }

    /**
     * Sets the permission mode for the session. `manual` follows the normal approval flow, `assisted` attaches LLM safety recommendations, and `allow-all` automatically approves permission requests. The result returns the authoritative post-mutation mode so callers can update local state without racing the `session.permissions_changed` notification.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionPermissionsSetModeResult> setMode(SessionPermissionsSetModeParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.permissions.setMode", _p, SessionPermissionsSetModeResult.class);
    }

    /**
     * Returns the current permission mode for the session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionPermissionsGetModeResult> getMode() {
        return caller.invoke("session.permissions.getMode", java.util.Map.of("sessionId", this.sessionId), SessionPermissionsGetModeResult.class);
    }

    /**
     * Adds or removes session-scoped or location-scoped permission rules.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionPermissionsModifyRulesResult> modifyRules(SessionPermissionsModifyRulesParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.permissions.modifyRules", _p, SessionPermissionsModifyRulesResult.class);
    }

    /**
     * Sets whether the client wants permission prompts bridged into session events.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionPermissionsSetRequiredResult> setRequired(SessionPermissionsSetRequiredParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.permissions.setRequired", _p, SessionPermissionsSetRequiredResult.class);
    }

    /**
     * Clears session-scoped tool approvals and, for full resets, exact session-approved paths.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionPermissionsResetSessionApprovalsResult> resetSessionApprovals(SessionPermissionsResetSessionApprovalsParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.permissions.resetSessionApprovals", _p, SessionPermissionsResetSessionApprovalsResult.class);
    }

    /**
     * Notifies the runtime that a permission prompt UI has been shown to the user.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionPermissionsNotifyPromptShownResult> notifyPromptShown(SessionPermissionsNotifyPromptShownParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.permissions.notifyPromptShown", _p, SessionPermissionsNotifyPromptShownResult.class);
    }

}
