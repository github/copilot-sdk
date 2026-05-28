/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

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

    /** API methods for the {@code auth} namespace. */
    public final SessionAuthApi auth;
    /** API methods for the {@code canvas} namespace. */
    public final SessionCanvasApi canvas;
    /** API methods for the {@code model} namespace. */
    public final SessionModelApi model;
    /** API methods for the {@code mode} namespace. */
    public final SessionModeApi mode;
    /** API methods for the {@code name} namespace. */
    public final SessionNameApi name;
    /** API methods for the {@code plan} namespace. */
    public final SessionPlanApi plan;
    /** API methods for the {@code workspaces} namespace. */
    public final SessionWorkspacesApi workspaces;
    /** API methods for the {@code instructions} namespace. */
    public final SessionInstructionsApi instructions;
    /** API methods for the {@code fleet} namespace. */
    public final SessionFleetApi fleet;
    /** API methods for the {@code agent} namespace. */
    public final SessionAgentApi agent;
    /** API methods for the {@code tasks} namespace. */
    public final SessionTasksApi tasks;
    /** API methods for the {@code skills} namespace. */
    public final SessionSkillsApi skills;
    /** API methods for the {@code mcp} namespace. */
    public final SessionMcpApi mcp;
    /** API methods for the {@code plugins} namespace. */
    public final SessionPluginsApi plugins;
    /** API methods for the {@code options} namespace. */
    public final SessionOptionsApi options;
    /** API methods for the {@code lsp} namespace. */
    public final SessionLspApi lsp;
    /** API methods for the {@code extensions} namespace. */
    public final SessionExtensionsApi extensions;
    /** API methods for the {@code tools} namespace. */
    public final SessionToolsApi tools;
    /** API methods for the {@code commands} namespace. */
    public final SessionCommandsApi commands;
    /** API methods for the {@code telemetry} namespace. */
    public final SessionTelemetryApi telemetry;
    /** API methods for the {@code ui} namespace. */
    public final SessionUiApi ui;
    /** API methods for the {@code permissions} namespace. */
    public final SessionPermissionsApi permissions;
    /** API methods for the {@code metadata} namespace. */
    public final SessionMetadataApi metadata;
    /** API methods for the {@code shell} namespace. */
    public final SessionShellApi shell;
    /** API methods for the {@code history} namespace. */
    public final SessionHistoryApi history;
    /** API methods for the {@code queue} namespace. */
    public final SessionQueueApi queue;
    /** API methods for the {@code eventLog} namespace. */
    public final SessionEventLogApi eventLog;
    /** API methods for the {@code usage} namespace. */
    public final SessionUsageApi usage;
    /** API methods for the {@code remote} namespace. */
    public final SessionRemoteApi remote;
    /** API methods for the {@code schedule} namespace. */
    public final SessionScheduleApi schedule;

    /**
     * Creates a new session RPC client.
     *
     * @param caller    the RPC transport function (e.g., {@code jsonRpcClient::invoke})
     * @param sessionId the session ID to inject into every request
     */
    public SessionRpc(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
        this.auth = new SessionAuthApi(caller, sessionId);
        this.canvas = new SessionCanvasApi(caller, sessionId);
        this.model = new SessionModelApi(caller, sessionId);
        this.mode = new SessionModeApi(caller, sessionId);
        this.name = new SessionNameApi(caller, sessionId);
        this.plan = new SessionPlanApi(caller, sessionId);
        this.workspaces = new SessionWorkspacesApi(caller, sessionId);
        this.instructions = new SessionInstructionsApi(caller, sessionId);
        this.fleet = new SessionFleetApi(caller, sessionId);
        this.agent = new SessionAgentApi(caller, sessionId);
        this.tasks = new SessionTasksApi(caller, sessionId);
        this.skills = new SessionSkillsApi(caller, sessionId);
        this.mcp = new SessionMcpApi(caller, sessionId);
        this.plugins = new SessionPluginsApi(caller, sessionId);
        this.options = new SessionOptionsApi(caller, sessionId);
        this.lsp = new SessionLspApi(caller, sessionId);
        this.extensions = new SessionExtensionsApi(caller, sessionId);
        this.tools = new SessionToolsApi(caller, sessionId);
        this.commands = new SessionCommandsApi(caller, sessionId);
        this.telemetry = new SessionTelemetryApi(caller, sessionId);
        this.ui = new SessionUiApi(caller, sessionId);
        this.permissions = new SessionPermissionsApi(caller, sessionId);
        this.metadata = new SessionMetadataApi(caller, sessionId);
        this.shell = new SessionShellApi(caller, sessionId);
        this.history = new SessionHistoryApi(caller, sessionId);
        this.queue = new SessionQueueApi(caller, sessionId);
        this.eventLog = new SessionEventLogApi(caller, sessionId);
        this.usage = new SessionUsageApi(caller, sessionId);
        this.remote = new SessionRemoteApi(caller, sessionId);
        this.schedule = new SessionScheduleApi(caller, sessionId);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<Void> suspend() {
        return caller.invoke("session.suspend", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

    /**
     * Parameters for sending a user message to the session
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionSendResult> send(SessionSendParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.send", _p, SessionSendResult.class);
    }

    /**
     * Parameters for aborting the current turn
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionAbortResult> abort(SessionAbortParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.abort", _p, SessionAbortResult.class);
    }

    /**
     * Parameters for shutting down the session
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<Void> shutdown(SessionShutdownParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.shutdown", _p, Void.class);
    }

    /**
     * Message text, optional severity level, persistence flag, optional follow-up URL, and optional tip.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionLogResult> log(SessionLogParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.log", _p, SessionLogResult.class);
    }

}
