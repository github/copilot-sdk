/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * Typed client for session-scoped RPC methods.
 * <p>
 * Provides strongly-typed access to all session-level API namespaces.
 * The {@code sessionId} is injected automatically into every call.
 * <p>
 * Obtain an instance by calling {@code new SessionRpc(caller, sessionId)}.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionRpc {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** API methods for the {@code model} namespace. */
    public final SessionModelApi model;
    /** API methods for the {@code mode} namespace. */
    public final SessionModeApi mode;
    /** API methods for the {@code plan} namespace. */
    public final SessionPlanApi plan;
    /** API methods for the {@code workspace} namespace. */
    public final SessionWorkspaceApi workspace;
    /** API methods for the {@code fleet} namespace. */
    public final SessionFleetApi fleet;
    /** API methods for the {@code agent} namespace. */
    public final SessionAgentApi agent;
    /** API methods for the {@code skills} namespace. */
    public final SessionSkillsApi skills;
    /** API methods for the {@code mcp} namespace. */
    public final SessionMcpApi mcp;
    /** API methods for the {@code plugins} namespace. */
    public final SessionPluginsApi plugins;
    /** API methods for the {@code extensions} namespace. */
    public final SessionExtensionsApi extensions;
    /** API methods for the {@code tools} namespace. */
    public final SessionToolsApi tools;
    /** API methods for the {@code commands} namespace. */
    public final SessionCommandsApi commands;
    /** API methods for the {@code ui} namespace. */
    public final SessionUiApi ui;
    /** API methods for the {@code permissions} namespace. */
    public final SessionPermissionsApi permissions;
    /** API methods for the {@code shell} namespace. */
    public final SessionShellApi shell;
    /** API methods for the {@code history} namespace. */
    public final SessionHistoryApi history;
    /** API methods for the {@code usage} namespace. */
    public final SessionUsageApi usage;

    /**
     * Creates a new session RPC client.
     *
     * @param caller    the RPC transport function (e.g., {@code jsonRpcClient::invoke})
     * @param sessionId the session ID to inject into every request
     */
    public SessionRpc(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
        this.model = new SessionModelApi(caller, sessionId);
        this.mode = new SessionModeApi(caller, sessionId);
        this.plan = new SessionPlanApi(caller, sessionId);
        this.workspace = new SessionWorkspaceApi(caller, sessionId);
        this.fleet = new SessionFleetApi(caller, sessionId);
        this.agent = new SessionAgentApi(caller, sessionId);
        this.skills = new SessionSkillsApi(caller, sessionId);
        this.mcp = new SessionMcpApi(caller, sessionId);
        this.plugins = new SessionPluginsApi(caller, sessionId);
        this.extensions = new SessionExtensionsApi(caller, sessionId);
        this.tools = new SessionToolsApi(caller, sessionId);
        this.commands = new SessionCommandsApi(caller, sessionId);
        this.ui = new SessionUiApi(caller, sessionId);
        this.permissions = new SessionPermissionsApi(caller, sessionId);
        this.shell = new SessionShellApi(caller, sessionId);
        this.history = new SessionHistoryApi(caller, sessionId);
        this.usage = new SessionUsageApi(caller, sessionId);
    }

    /**
     * Invokes {@code session.log}.
     * @since 1.0.0
     */
    public CompletableFuture<SessionLogResult> log(SessionLogParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.log", _p, SessionLogResult.class);
    }

}
