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
 * API methods for the {@code connectors} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionConnectorsApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionConnectorsApi(RpcCaller caller, String sessionId) {
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
    public CompletableFuture<SessionConnectorsGetCapabilitiesResult> getCapabilities() {
        return caller.invoke("session.connectors.getCapabilities", java.util.Map.of("sessionId", this.sessionId), SessionConnectorsGetCapabilitiesResult.class);
    }

    /**
     * Identifies the target session.
     *
     * @return a future that completes with the {@code ConnectorSessionAccount} value,
     *     or {@code null} when the result is absent. Callers must handle the
     *     {@code null} completion value.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ConnectorSessionAccount> getAccount() {
        return caller.invoke("session.connectors.getAccount", java.util.Map.of("sessionId", this.sessionId), ConnectorSessionAccount.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionConnectorsGetStatusResult> getStatus() {
        return caller.invoke("session.connectors.getStatus", java.util.Map.of("sessionId", this.sessionId), SessionConnectorsGetStatusResult.class);
    }

    /**
     * Pins a Connector operation to one host-owned GitHub account through its opaque selection ID. Provider tokens are never accepted.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionConnectorsListResult> list(SessionConnectorsListParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.connectors.list", _p, SessionConnectorsListResult.class);
    }

    /**
     * Pins a Connector operation to one host-owned GitHub account through its opaque selection ID. Provider tokens are never accepted.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionConnectorsRefreshResult> refresh(SessionConnectorsRefreshParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.connectors.refresh", _p, SessionConnectorsRefreshResult.class);
    }

    /**
     * Selects one Connector and the pinned host-owned account used for its service and MCP authorization.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ConnectorConnectResult> connect(SessionConnectorsConnectParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.connectors.connect", _p, ConnectorConnectResult.class);
    }

    /**
     * Selects one Connector and the pinned host-owned account used for its service and MCP authorization.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ConnectorConnectResult> reconnect(SessionConnectorsReconnectParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.connectors.reconnect", _p, ConnectorConnectResult.class);
    }

    /**
     * Explicitly bounded continuation of a pending Connector connection.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ConnectorConnectResult> continueConnection(SessionConnectorsContinueConnectionParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.connectors.continueConnection", _p, ConnectorConnectResult.class);
    }

    /**
     * Selects one Connector and the pinned host-owned account used for its service and MCP authorization.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionConnectorsDisconnectResult> disconnect(SessionConnectorsDisconnectParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.connectors.disconnect", _p, SessionConnectorsDisconnectResult.class);
    }

    /**
     * Requests authoritative Connector-to-MCP reconciliation for the pinned account.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionConnectorsReconcileResult> reconcile(SessionConnectorsReconcileParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.connectors.reconcile", _p, SessionConnectorsReconcileResult.class);
    }

    /**
     * Requests authoritative Connector-to-MCP reconciliation for the pinned account.
     * <p>
     * Accepts the extensible request, including inputs added after the params record.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionConnectorsReconcileResult> reconcile(SessionConnectorsReconcileRequest request) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(Objects.requireNonNull(request, "request"));
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.connectors.reconcile", _p, SessionConnectorsReconcileResult.class);
    }

    /**
     * Pins a Connector operation to one host-owned GitHub account through its opaque selection ID. Provider tokens are never accepted.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<ConnectorStatus> reconcileForStartup(SessionConnectorsReconcileForStartupParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.connectors.reconcileForStartup", _p, ConnectorStatus.class);
    }

    /**
     * Identifies the target session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<ConnectorStatus> withdrawProjection() {
        return caller.invoke("session.connectors.withdrawProjection", java.util.Map.of("sessionId", this.sessionId), ConnectorStatus.class);
    }

}
