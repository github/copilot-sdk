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
     * Returns feature availability and bounded polling limits for the EXPERIMENTAL session connector API. This method never performs a Connector service request.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionConnectorsGetCapabilitiesResult> getCapabilities() {
        return caller.invoke("session.connectors.getCapabilities", java.util.Map.of("sessionId", this.sessionId), SessionConnectorsGetCapabilitiesResult.class);
    }

    /**
     * Returns the session account selection, or null.
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
     * Returns authoritative session Connector state from current availability, pinned account selection, cached catalog, and live MCP projection without performing a Connector service request.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionConnectorsGetStatusResult> getStatus() {
        return caller.invoke("session.connectors.getStatus", java.util.Map.of("sessionId", this.sessionId), SessionConnectorsGetStatusResult.class);
    }

    /**
     * Returns the cached Connector catalog for the pinned opaque account selection, fetching it only when this session has no cached catalog.
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
     * Refreshes and validates the Connector catalog for the pinned opaque account selection.
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
     * Initiates an idempotent Connector connection request without opening a browser. Returns connected when the service is immediately authoritative, consent_required with a validated URL, or pending with an opaque continuation ID.
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
     * Re-initiates an idempotent Connector connection request without browser or UI effects, with the same typed outcomes as connect.
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
     * Continues a pending Connector connection with caller-supplied attempt, interval, and deadline bounds. The runtime never opens the returned consent URL.
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
     * Disconnects one Connector for the pinned opaque account selection, refreshes the authoritative catalog, and removes its session-owned MCP projection.
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
     * Reconciles the authoritative cached or freshly requested Connector catalog into the session Connector MCP projection and returns live status.
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
     * Reconciles the authoritative cached or freshly requested Connector catalog into the session Connector MCP projection and returns live status.
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
     * Reconciles the authoritative Connector catalog into the session MCP projection during startup with a bounded deadline and fail-closed cleanup.
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
     * Removes the runtime-owned Connector MCP projection without changing service-side connections.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<ConnectorStatus> withdrawProjection() {
        return caller.invoke("session.connectors.withdrawProjection", java.util.Map.of("sessionId", this.sessionId), ConnectorStatus.class);
    }

}
