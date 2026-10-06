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
     * Resolves a pending MCP OAuth request with a host-provided token or cancellation. The pending request is emitted as mcp.oauth_required with the data necessary to authorize the request.
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
