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
 * API methods for the {@code mcp} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionMcpApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** API methods for the {@code mcp.oauth} sub-namespace. */
    public final SessionMcpOauthApi oauth;
    /** API methods for the {@code mcp.headers} sub-namespace. */
    public final SessionMcpHeadersApi headers;
    /** API methods for the {@code mcp.apps} sub-namespace. */
    public final SessionMcpAppsApi apps;
    /** API methods for the {@code mcp.resources} sub-namespace. */
    public final SessionMcpResourcesApi resources;
    /** API methods for the {@code mcp.prompts} sub-namespace. */
    public final SessionMcpPromptsApi prompts;

    /** @param caller the RPC transport function */
    SessionMcpApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
        this.oauth = new SessionMcpOauthApi(caller, sessionId);
        this.headers = new SessionMcpHeadersApi(caller, sessionId);
        this.apps = new SessionMcpAppsApi(caller, sessionId);
        this.resources = new SessionMcpResourcesApi(caller, sessionId);
        this.prompts = new SessionMcpPromptsApi(caller, sessionId);
    }

    /**
     * Records the IDE the host is connected to, so the agent's system prompt can name it and its workspace folder. Null or an omitted `ide` clears the recorded value, which is how a host reports that it is disconnected; there is no separate clear method. Both `ideName` and `workspaceFolder` are required together, because half a state cannot be attributed to a project.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> setConnectedIdeInfo(SessionMcpSetConnectedIdeInfoParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.setConnectedIdeInfo", _p, Void.class);
    }

    /**
     * Lists materialized MCP servers and their connection status. Cache misses may start and wait for MCP servers.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMcpListResult> list() {
        return caller.invoke("session.mcp.list", java.util.Map.of("sessionId", this.sessionId), SessionMcpListResult.class);
    }

    /**
     * Lists effective MCP configuration without starting, restarting, authenticating, or waiting for servers. An optional live observation is from an already materialized matching server; this is not a readiness guarantee.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMcpListConfiguredResult> listConfigured() {
        return caller.invoke("session.mcp.listConfigured", java.util.Map.of("sessionId", this.sessionId), SessionMcpListConfiguredResult.class);
    }

    /**
     * Lists the tools exposed by a connected MCP server on this session's host. This performs a live `tools/list` request. Tool UI metadata is returned independently of whether MCP Apps rendering is enabled for the session.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMcpListToolsResult> listTools(SessionMcpListToolsParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.listTools", _p, SessionMcpListToolsResult.class);
    }

    /**
     * Enables an MCP server for the session.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> enable(SessionMcpEnableParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.enable", _p, Void.class);
    }

    /**
     * Enables an MCP server for the session.
     * <p>
     * Accepts the extensible request, including inputs added after the params record.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> enable(SessionMcpEnableRequest request) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(Objects.requireNonNull(request, "request"));
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.enable", _p, Void.class);
    }

    /**
     * Disables an MCP server for the session.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> disable(SessionMcpDisableParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.disable", _p, Void.class);
    }

    /**
     * Disables an MCP server for the session.
     * <p>
     * Accepts the extensible request, including inputs added after the params record.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> disable(SessionMcpDisableRequest request) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(Objects.requireNonNull(request, "request"));
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.disable", _p, Void.class);
    }

    /**
     * Reloads MCP server connections for the session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> reload() {
        return caller.invoke("session.mcp.reload", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

    /**
     * Releases any turns waiting on an in-flight MCP load without cancelling the load, letting the agent proceed while MCP servers finish connecting in the background. No-op when no MCP load is in flight or waiting turns were already released.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMcpMoveLoadingToBackgroundResult> moveLoadingToBackground() {
        return caller.invoke("session.mcp.moveLoadingToBackground", java.util.Map.of("sessionId", this.sessionId), SessionMcpMoveLoadingToBackgroundResult.class);
    }

    /**
     * Reloads MCP server connections for the session with an explicit host-provided configuration.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<McpStartServersResult> reloadWithConfig(SessionMcpReloadWithConfigParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.reloadWithConfig", _p, McpStartServersResult.class);
    }

    /**
     * Runs an MCP sampling inference on behalf of an MCP server.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMcpExecuteSamplingResult> executeSampling(SessionMcpExecuteSamplingParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.executeSampling", _p, SessionMcpExecuteSamplingResult.class);
    }

    /**
     * Cancels an in-flight MCP sampling execution by request ID.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMcpCancelSamplingExecutionResult> cancelSamplingExecution(SessionMcpCancelSamplingExecutionParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.cancelSamplingExecution", _p, SessionMcpCancelSamplingExecutionResult.class);
    }

    /**
     * Sets how environment-variable values supplied to MCP servers are resolved (direct or indirect).
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMcpSetEnvValueModeResult> setEnvValueMode(SessionMcpSetEnvValueModeParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.setEnvValueMode", _p, SessionMcpSetEnvValueModeResult.class);
    }

    /**
     * Removes the auto-managed `github` MCP server when present.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMcpRemoveGitHubResult> removeGitHub() {
        return caller.invoke("session.mcp.removeGitHub", java.util.Map.of("sessionId", this.sessionId), SessionMcpRemoveGitHubResult.class);
    }

    /**
     * Configures the built-in GitHub MCP server for the session's current auth context.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<McpConfigureGitHubResult> configureGitHub(SessionMcpConfigureGitHubParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.configureGitHub", _p, McpConfigureGitHubResult.class);
    }

    /**
     * Starts an individual MCP server on the live session. Omit `config` for a config-free start-by-name of an already-configured server (reuses the server's already-registered configuration); supply `config` to start from a caller-supplied configuration. Session-scoped and ephemeral: the server is added to this session's running set only and is reaped when the session ends. Does NOT modify persistent user configuration (`mcp.config.*`), so it does not affect future sessions. The server surfaces through `session.mcp.list` and the `session.mcp_servers_loaded` / `session.mcp_server_status_changed` events like any other server.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> startServer(SessionMcpStartServerParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.startServer", _p, Void.class);
    }

    /**
     * Starts an individual MCP server on the live session. Omit `config` for a config-free start-by-name of an already-configured server (reuses the server's already-registered configuration); supply `config` to start from a caller-supplied configuration. Session-scoped and ephemeral: the server is added to this session's running set only and is reaped when the session ends. Does NOT modify persistent user configuration (`mcp.config.*`), so it does not affect future sessions. The server surfaces through `session.mcp.list` and the `session.mcp_servers_loaded` / `session.mcp_server_status_changed` events like any other server.
     * <p>
     * Accepts the extensible request, including inputs added after the params record.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> startServer(SessionMcpStartServerRequest request) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(Objects.requireNonNull(request, "request"));
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.startServer", _p, Void.class);
    }

    /**
     * Restarts an individual MCP server on the live session (stops then starts). Omit `config` for a config-free restart-by-name of an already-configured server; supply `config` to restart with a replacement configuration. Session-scoped and ephemeral: does NOT modify persistent user configuration (`mcp.config.*`).
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> restartServer(SessionMcpRestartServerParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.restartServer", _p, Void.class);
    }

    /**
     * Restarts an individual MCP server on the live session (stops then starts). Omit `config` for a config-free restart-by-name of an already-configured server; supply `config` to restart with a replacement configuration. Session-scoped and ephemeral: does NOT modify persistent user configuration (`mcp.config.*`).
     * <p>
     * Accepts the extensible request, including inputs added after the params record.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> restartServer(SessionMcpRestartServerRequest request) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(Objects.requireNonNull(request, "request"));
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.restartServer", _p, Void.class);
    }

    /**
     * Stops an individual MCP server on the session's host.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> stopServer(SessionMcpStopServerParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.stopServer", _p, Void.class);
    }

    /**
     * Stops an individual MCP server on the session's host.
     * <p>
     * Accepts the extensible request, including inputs added after the params record.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> stopServer(SessionMcpStopServerRequest request) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(Objects.requireNonNull(request, "request"));
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.stopServer", _p, Void.class);
    }

    /**
     * Registers a pre-connected external MCP client (e.g. IDE) on the session's host. The caller retains lifecycle ownership of the client and transport. Marked internal because the `client` and `transport` arguments are in-process MCP SDK instances that cannot be serialized across the JSON-RPC boundary; once the CLI moves on top of the SDK, external clients will be expressed as transport configs the runtime can construct itself.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> registerExternalClient(SessionMcpRegisterExternalClientParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.registerExternalClient", _p, Void.class);
    }

    /**
     * Unregisters a previously registered external MCP client by server name. Marked internal as the paired companion of `registerExternalClient`: only in-process callers that registered a client this way can meaningfully unregister it. Disappears alongside `registerExternalClient`: once external clients are described to the runtime as config rather than handed in as instances, lifecycle (including deregistration) is owned entirely by the runtime.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> unregisterExternalClient(SessionMcpUnregisterExternalClientParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.unregisterExternalClient", _p, Void.class);
    }

    /**
     * Checks whether a named MCP server is currently running on the session's host.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionMcpIsServerRunningResult> isServerRunning(SessionMcpIsServerRunningParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.mcp.isServerRunning", _p, SessionMcpIsServerRunningResult.class);
    }

}
