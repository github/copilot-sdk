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
 * API methods for the {@code mcp.oauth} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionMcpOauthApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionMcpOauthApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Resolves a pending MCP OAuth request with a host-provided token, cancellation, or a noninteractive runtime refresh handoff when the request advertises runtimeRefreshSupported. Acceptance resolves the host response, not credential installation, reconnect, or the original tool invocation.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMcpOauthHandlePendingRequestResult> handlePendingRequest(SessionMcpOauthHandlePendingRequestParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.oauth.handlePendingRequest", _p, SessionMcpOauthHandlePendingRequestResult.class);
    }

    /**
     * Notifies the session that MCP OAuth authentication succeeded and updated credentials were persisted, so cached tool definitions can be refreshed.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> authenticationStateChanged(SessionMcpOauthAuthenticationStateChangedParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.oauth.authenticationStateChanged", _p, Void.class);
    }

    /**
     * Prepares an inert, expiring owned OAuth login bound to the original session requester and exact installation. Does not activate, connect, read credentials or open a browser.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMcpOauthPrepareLoginResult> prepareLogin(SessionMcpOauthPrepareLoginParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.oauth.prepareLogin", _p, SessionMcpOauthPrepareLoginResult.class);
    }

    /**
     * Reserves a host-driven OAuth flow identity before authentication or cancellation.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> prepareFlow(SessionMcpOauthPrepareFlowParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.oauth.prepareFlow", _p, Void.class);
    }

    /**
     * Cancels an exact host-driven OAuth flow on its original session requester.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> cancelFlow(SessionMcpOauthCancelFlowParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.oauth.cancelFlow", _p, Void.class);
    }

    /**
     * Authenticates an arbitrary MCP URL through the original session credential owner, with correlated host progress and authorization callbacks.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<McpOauthAuthenticateResult> authenticate(SessionMcpOauthAuthenticateParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.oauth.authenticate", _p, McpOauthAuthenticateResult.class);
    }

    /**
     * Checks whether the selected MCP credential owner has a valid access token.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Boolean> hasValidTokens(SessionMcpOauthHasValidTokensParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.oauth.hasValidTokens", _p, Boolean.class);
    }

    /**
     * Attempts silent MCP token refresh without opening a browser.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Boolean> tryRefreshTokens(SessionMcpOauthTryRefreshTokensParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.oauth.tryRefreshTokens", _p, Boolean.class);
    }

    /**
     * Stores a confidential MCP client's static secret in its selected credential owner.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> saveStaticClientSecret(SessionMcpOauthSaveStaticClientSecretParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.oauth.saveStaticClientSecret", _p, Void.class);
    }

    /**
     * Releases an explicit host-owned MCP OAuth store on its original session requester.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> disposeStore(SessionMcpOauthDisposeStoreParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.oauth.disposeStore", _p, Void.class);
    }

    /**
     * Completes a tokenless initialize handshake for an unsaved remote MCP URL, preserving session egress and the refusing authentication challenge.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<McpOauthTestConnectionResult> testConnection(SessionMcpOauthTestConnectionParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.oauth.testConnection", _p, McpOauthTestConnectionResult.class);
    }

    /**
     * Derives an MCP credential key through the selected session account and store owner.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<String> storeKey(SessionMcpOauthStoreKeyParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.oauth.storeKey", _p, String.class);
    }

    /**
     * Starts OAuth authentication for a remote MCP server. Owned servers require the original one-use prepareLogin handle and exact installation ID; manual servers retain the existing direct login behaviour.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMcpOauthLoginResult> login(SessionMcpOauthLoginParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.oauth.login", _p, SessionMcpOauthLoginResult.class);
    }

    /**
     * Starts OAuth authentication for a remote MCP server. Owned servers require the original one-use prepareLogin handle and exact installation ID; manual servers retain the existing direct login behaviour.
     * <p>
     * Accepts the extensible request, including inputs added after the params record.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMcpOauthLoginResult> login(SessionMcpOauthLoginRequest request) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(Objects.requireNonNull(request, "request"));
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.oauth.login", _p, SessionMcpOauthLoginResult.class);
    }

    /**
     * Completes a runtime-managed MCP OAuth login after the authorization server redirects to a host-managed callback URL.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> complete(SessionMcpOauthCompleteParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.oauth.complete", _p, Void.class);
    }

    /**
     * Passively probes a configured remote MCP server to classify whether OAuth is required or a cached/override token is accepted. Does not start OAuth, emit pending OAuth requests, or mutate MCP connection state.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpOauthProbeResult> probe(SessionMcpOauthProbeParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.oauth.probe", _p, McpOauthProbeResult.class);
    }

    /**
     * Passively probes a configured remote MCP server to classify whether OAuth is required or a cached/override token is accepted. Does not start OAuth, emit pending OAuth requests, or mutate MCP connection state.
     * <p>
     * Accepts the extensible request, including inputs added after the params record.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<McpOauthProbeResult> probe(SessionMcpOauthProbeRequest request) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(Objects.requireNonNull(request, "request"));
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.oauth.probe", _p, McpOauthProbeResult.class);
    }

    /**
     * Cancels the exact owned OAuth login issued to this original session requester, without clearing shared credentials.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMcpOauthCancelLoginResult> cancelLogin(SessionMcpOauthCancelLoginParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.oauth.cancelLogin", _p, SessionMcpOauthCancelLoginResult.class);
    }

    /**
     * Responds to a pending MCP OAuth authorization request by its request id.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMcpOauthRespondResult> respond(SessionMcpOauthRespondParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.oauth.respond", _p, SessionMcpOauthRespondResult.class);
    }

}
