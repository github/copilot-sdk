/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code gitHubAuth} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionGitHubAuthApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionGitHubAuthApi(RpcCaller caller, String sessionId) {
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
    public CompletableFuture<SessionGitHubAuthGetStatusResult> getStatus() {
        return caller.invoke("session.gitHubAuth.getStatus", java.util.Map.of("sessionId", this.sessionId), SessionGitHubAuthGetStatusResult.class);
    }

    /**
     * New auth credentials to install on the session. Omit to leave credentials unchanged.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionGitHubAuthSetCredentialsResult> setCredentials(SessionGitHubAuthSetCredentialsParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.gitHubAuth.setCredentials", _p, SessionGitHubAuthSetCredentialsResult.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> getCurrentAuthInfo() {
        return caller.invoke("session.gitHubAuth.getCurrentAuthInfo", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<List<SessionAuthStatus>> getAllAuthAvailable() {
        return caller.invoke("session.gitHubAuth.getAllAuthAvailable", java.util.Map.of("sessionId", this.sessionId), RpcMapper.INSTANCE.getTypeFactory().constructCollectionType(List.class, SessionAuthStatus.class));
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> refreshCopilotUser() {
        return caller.invoke("session.gitHubAuth.refreshCopilotUser", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

    /**
     * Internal GitHub login parameters.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<AuthInfo> login(SessionGitHubAuthLoginParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.gitHubAuth.login", _p, AuthInfo.class);
    }

    /**
     * Parameters for switching the session's active authentication.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> switchToAuth(SessionGitHubAuthSwitchToAuthParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.gitHubAuth.switchToAuth", _p, Void.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> logout() {
        return caller.invoke("session.gitHubAuth.logout", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

    /**
     * Parameters identifying a GitHub authentication to log out.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> logoutUser(SessionGitHubAuthLogoutUserParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.gitHubAuth.logoutUser", _p, Void.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<List<AuthValidationError>> lastAuthErrors() {
        return caller.invoke("session.gitHubAuth.lastAuthErrors", java.util.Map.of("sessionId", this.sessionId), RpcMapper.INSTANCE.getTypeFactory().constructCollectionType(List.class, AuthValidationError.class));
    }

}
