/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.github.copilot.generated.rpc.AuthIdentityMetadata;
import com.github.copilot.generated.rpc.AuthInfoType;
import com.github.copilot.generated.rpc.ConnectorAvailability;
import com.github.copilot.generated.rpc.ConnectorSessionAccount;
import com.github.copilot.generated.rpc.McpConfigAddParams;
import com.github.copilot.generated.rpc.McpDiscoverParams;
import com.github.copilot.generated.rpc.RpcCaller;
import com.github.copilot.generated.rpc.ServerRpc;
import com.github.copilot.generated.rpc.SessionAgentSelectParams;
import com.github.copilot.generated.rpc.SessionConnectorsConnectParams;
import com.github.copilot.generated.rpc.SessionConnectorsContinueConnectionParams;
import com.github.copilot.generated.rpc.SessionConnectorsDisconnectParams;
import com.github.copilot.generated.rpc.SessionConnectorsDisconnectResult;
import com.github.copilot.generated.rpc.SessionConnectorsGetCapabilitiesResult;
import com.github.copilot.generated.rpc.SessionConnectorsGetStatusResult;
import com.github.copilot.generated.rpc.SessionConnectorsListParams;
import com.github.copilot.generated.rpc.SessionConnectorsListResult;
import com.github.copilot.generated.rpc.SessionConnectorsReconcileParams;
import com.github.copilot.generated.rpc.SessionConnectorsReconcileRequest;
import com.github.copilot.generated.rpc.SessionConnectorsReconcileResult;
import com.github.copilot.generated.rpc.SessionConnectorsReconnectParams;
import com.github.copilot.generated.rpc.SessionConnectorsRefreshParams;
import com.github.copilot.generated.rpc.SessionConnectorsRefreshResult;
import com.github.copilot.generated.rpc.SessionMcpListConfiguredResult;
import com.github.copilot.generated.rpc.SessionModelGetReasoningSummariesParams;
import com.github.copilot.generated.rpc.SessionModelGetReasoningSummariesResult;
import com.github.copilot.generated.rpc.SessionModelSwitchToParams;
import com.github.copilot.generated.rpc.SessionModelSwitchToRequest;
import com.github.copilot.generated.rpc.SessionOptionsUpdateParams;
import com.github.copilot.generated.rpc.SessionRpc;
import com.github.copilot.generated.rpc.SessionToolsUpdateSubagentSettingsParams;
import com.github.copilot.generated.rpc.SubagentSettingsConfig;
import com.github.copilot.rpc.AgentInfo;

/**
 * Unit tests for the generated RPC wrapper classes ({@link ServerRpc} and
 * {@link SessionRpc}). Uses a simple in-memory {@link RpcCaller} stub to verify
 * that:
 * <ul>
 * <li>The correct RPC method name is passed for each API call.</li>
 * <li>{@link SessionRpc} automatically injects {@code sessionId} into every
 * call.</li>
 * <li>Session methods with extra params merge those params with the session
 * ID.</li>
 * </ul>
 */
class RpcWrappersTest {

    /**
     * A simple stub {@link RpcCaller} that records every call made to it and
     * returns a pre-configured result (or null).
     */
    private static final class StubCaller implements RpcCaller {

        static record Call(String method, Object params, Class<?> resultType) {
        }

        final List<Call> calls = new ArrayList<>();
        Object nextResult = null;

        @Override
        @SuppressWarnings("unchecked")
        public <T> CompletableFuture<T> invoke(String method, Object params, Class<T> resultType) {
            calls.add(new Call(method, params, resultType));
            return CompletableFuture.completedFuture((T) nextResult);
        }
    }

    // ── ServerRpc tests ───────────────────────────────────────────────────────

    @Test
    void serverRpc_instantiates_with_all_namespace_fields() {
        var stub = new StubCaller();
        var server = new ServerRpc(stub);

        assertNotNull(server.models);
        assertNotNull(server.tools);
        assertNotNull(server.account);
        assertNotNull(server.mcp);
        assertNotNull(server.mcp.config); // nested sub-namespace
        assertNotNull(server.sessionFs);
        assertNotNull(server.sessions);
    }

    @Test
    void serverRpc_models_list_invokes_correct_rpc_method() {
        var stub = new StubCaller();
        stub.nextResult = null; // no result needed for method dispatch test

        var server = new ServerRpc(stub);
        server.models.list();

        assertEquals(1, stub.calls.size());
        assertEquals("models.list", stub.calls.get(0).method());
    }

    @Test
    void serverRpc_account_getAllUsers_returns_typed_list() throws Exception {
        var stub = new StubCaller();
        stub.nextResult = new ObjectMapper().readTree("""
                [
                  {
                    "authInfo": { "kind": "oauth" },
                    "token": "token-1"
                  }
                ]
                """);

        var server = new ServerRpc(stub);
        var users = server.account.getAllUsers().get();

        assertEquals(1, stub.calls.size());
        assertEquals("account.getAllUsers", stub.calls.get(0).method());
        assertEquals(1, users.size());
        assertInstanceOf(Map.class, users.get(0).authInfo());
        assertEquals("token-1", users.get(0).token());
    }

    @Test
    void serverRpc_ping_passes_params_directly() {
        var stub = new StubCaller();
        var server = new ServerRpc(stub);

        var params = com.github.copilot.generated.rpc.PingParams.builder().message(null).build();
        server.ping(params);

        assertEquals(1, stub.calls.size());
        assertEquals("ping", stub.calls.get(0).method());
        assertSame(params, stub.calls.get(0).params());
    }

    @Test
    void serverRpc_mcp_config_list_invokes_correct_rpc_method() {
        var stub = new StubCaller();
        var server = new ServerRpc(stub);

        server.mcp.config.list();

        assertEquals(1, stub.calls.size());
        assertEquals("mcp.config.list", stub.calls.get(0).method());
    }

    @Test
    void serverRpc_mcp_config_add_passes_params() {
        var stub = new StubCaller();
        var server = new ServerRpc(stub);

        var params = McpConfigAddParams.builder().name("myServer").config(null).build();
        server.mcp.config.add(params);

        assertEquals(1, stub.calls.size());
        assertEquals("mcp.config.add", stub.calls.get(0).method());
        assertSame(params, stub.calls.get(0).params());
    }

    @Test
    void serverRpc_mcp_discover_passes_params() {
        var stub = new StubCaller();
        var server = new ServerRpc(stub);

        var params = McpDiscoverParams.builder().workingDirectory("/workspace").includeEffectiveSource(null).build();
        server.mcp.discover(params);

        assertEquals(1, stub.calls.size());
        assertEquals("mcp.discover", stub.calls.get(0).method());
        assertSame(params, stub.calls.get(0).params());
    }

    // ── SessionRpc tests ──────────────────────────────────────────────────────

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"empty", "configured"})
    @AllowCopilotExperimental
    void sessionRpc_subagent_settings_preserve_explicit_null(String mode) {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "subagent-session");
        var settings = mode == null
                ? null
                : SubagentSettingsConfig.builder().maxDepth("configured".equals(mode) ? 4L : null).build();

        session.tools
                .updateSubagentSettings(SessionToolsUpdateSubagentSettingsParams.builder().subagents(settings).build())
                .join();

        assertEquals(1, stub.calls.size());
        var call = stub.calls.get(0);
        assertEquals("session.tools.updateSubagentSettings", call.method());
        var payload = assertInstanceOf(JsonNode.class, call.params());
        var expected = new ObjectMapper().createObjectNode().put("sessionId", "subagent-session");
        if (mode == null) {
            expected.putNull("subagents");
        } else {
            var subagents = expected.putObject("subagents");
            if ("configured".equals(mode)) {
                subagents.put("maxDepth", 4L);
            }
        }
        assertEquals(expected, payload);
    }

    @ParameterizedTest
    @CsvSource({"true,false", "false,true", "null,null"})
    void sessionRpc_reasoning_summaries_accepts_only_host_state(String persisted, String visible) {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "reasoning-session");
        Boolean persistedShowReasoning = "null".equals(persisted) ? null : Boolean.valueOf(persisted);
        Boolean summariesVisible = "null".equals(visible) ? null : Boolean.valueOf(visible);
        var params = SessionModelGetReasoningSummariesParams.builder().persistedShowReasoning(persistedShowReasoning)
                .summariesVisible(summariesVisible).build();

        assertNull(params.sessionId());
        session.model.getReasoningSummaries(params).join();

        assertEquals(1, stub.calls.size());
        var call = stub.calls.get(0);
        assertEquals("session.model.getReasoningSummaries", call.method());
        assertEquals(SessionModelGetReasoningSummariesResult.class, call.resultType());
        var payload = assertInstanceOf(JsonNode.class, call.params());
        var expected = new ObjectMapper().createObjectNode().put("sessionId", "reasoning-session");
        if (persistedShowReasoning != null) {
            expected.put("persistedShowReasoning", persistedShowReasoning);
        }
        if (summariesVisible != null) {
            expected.put("summariesVisible", summariesVisible);
        }
        assertEquals(expected, payload);
    }

    @Test
    @AllowCopilotExperimental
    void sessionRpc_mcp_listConfigured_preserves_runtime_lifecycle() throws Exception {
        var stub = new StubCaller();
        stub.nextResult = new ObjectMapper().readValue("""
                {
                  "servers": [
                    {"name":"cold","enabled":true,"live":{"status":"not_configured"}},
                    {"name":"old-connection","enabled":false,"live":{"status":"connected"}},
                    {"name":"auth","enabled":true,"live":{"status":"needs-auth"}},
                    {"name":"stopped","enabled":true,"live":{"status":"stopped"}},
                    {"name":"failed","enabled":true,"live":{"status":"failed","error":"connection failed"}}
                  ]
                }
                """, SessionMcpListConfiguredResult.class);
        var session = new SessionRpc(stub, "session-mcp");

        var inventory = session.mcp.listConfigured().get();

        assertEquals(List.of("not_configured", "connected", "needs-auth", "stopped", "failed"),
                inventory.servers().stream().map(server -> server.live().status().getValue()).toList());
        assertFalse(inventory.servers().get(1).enabled());
        assertEquals("connection failed", inventory.servers().get(4).live().error());
        assertEquals(List.of(new StubCaller.Call("session.mcp.listConfigured", Map.of("sessionId", "session-mcp"),
                SessionMcpListConfiguredResult.class)), stub.calls);
    }

    @Test
    void sessionRpc_instantiates_with_all_namespace_fields() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-001");

        assertNotNull(session.model);
        assertNotNull(session.mode);
        assertNotNull(session.plan);
        assertNotNull(session.workspaces);
        assertNotNull(session.fleet);
        assertNotNull(session.agent);
        assertNotNull(session.skills);
        assertNotNull(session.mcp);
        assertNotNull(session.plugins);
        assertNotNull(session.extensions);
        assertNotNull(session.tools);
        assertNotNull(session.commands);
        assertNotNull(session.ui);
        assertNotNull(session.permissions);
        assertNotNull(session.shell);
        assertNotNull(session.history);
        assertNotNull(session.usage);
        assertNotNull(session.connectors);
    }

    @Test
    @AllowCopilotExperimental
    void sessionRpc_connectors_exposes_host_lifecycle_methods() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-connectors");

        session.connectors.getCapabilities();
        session.connectors.getStatus();
        session.connectors.list(
                SessionConnectorsListParams.builder().sessionId("ignored-session").accountId("account-1").build());
        session.connectors.refresh(
                SessionConnectorsRefreshParams.builder().sessionId("ignored-session").accountId("account-1").build());
        session.connectors.connect(SessionConnectorsConnectParams.builder().sessionId("ignored-session")
                .accountId("account-1").connectorName("outlook").build());
        session.connectors.reconnect(SessionConnectorsReconnectParams.builder().sessionId("ignored-session")
                .accountId("account-1").connectorName("outlook").build());
        session.connectors.continueConnection(SessionConnectorsContinueConnectionParams.builder()
                .sessionId("ignored-session").continuationId("continuation-1").maxAttempts(3L).pollIntervalMs(1_000L)
                .deadlineMs(30_000L).build());
        session.connectors.disconnect(SessionConnectorsDisconnectParams.builder().sessionId("ignored-session")
                .accountId("account-1").connectorName("outlook").build());
        session.connectors.reconcile(SessionConnectorsReconcileParams.builder().sessionId("ignored-session")
                .accountId("account-1").refreshCatalog(true).build());

        assertEquals(9, stub.calls.size());
        assertConnectorCall(stub.calls.get(0), "session.connectors.getCapabilities",
                SessionConnectorsGetCapabilitiesResult.class);
        assertConnectorCall(stub.calls.get(1), "session.connectors.getStatus", SessionConnectorsGetStatusResult.class);

        var listParams = assertConnectorCall(stub.calls.get(2), "session.connectors.list",
                SessionConnectorsListResult.class);
        assertEquals("account-1", listParams.get("accountId").asText());

        var refreshParams = assertConnectorCall(stub.calls.get(3), "session.connectors.refresh",
                SessionConnectorsRefreshResult.class);
        assertEquals("account-1", refreshParams.get("accountId").asText());

        var connectParams = assertConnectorCall(stub.calls.get(4), "session.connectors.connect",
                com.github.copilot.generated.rpc.ConnectorConnectResult.class);
        assertEquals("outlook", connectParams.get("connectorName").asText());

        var reconnectParams = assertConnectorCall(stub.calls.get(5), "session.connectors.reconnect",
                com.github.copilot.generated.rpc.ConnectorConnectResult.class);
        assertEquals("outlook", reconnectParams.get("connectorName").asText());

        var continueParams = assertConnectorCall(stub.calls.get(6), "session.connectors.continueConnection",
                com.github.copilot.generated.rpc.ConnectorConnectResult.class);
        assertEquals("continuation-1", continueParams.get("continuationId").asText());
        assertEquals(3L, continueParams.get("maxAttempts").asLong());
        assertEquals(1_000L, continueParams.get("pollIntervalMs").asLong());
        assertEquals(30_000L, continueParams.get("deadlineMs").asLong());

        var disconnectParams = assertConnectorCall(stub.calls.get(7), "session.connectors.disconnect",
                SessionConnectorsDisconnectResult.class);
        assertEquals("outlook", disconnectParams.get("connectorName").asText());

        var reconcileParams = assertConnectorCall(stub.calls.get(8), "session.connectors.reconcile",
                SessionConnectorsReconcileResult.class);
        assertTrue(reconcileParams.get("refreshCatalog").asBoolean());
        assertFalse(reconcileParams.has("forceConnectorName"));
        assertEquals(3, reconcileParams.size());
    }

    @ParameterizedTest
    @CsvSource(value = {"enabled, missing, missing, false, false", "enabled, false, false, false, false",
            "enabled, true, missing, true, false", "enabled, missing, true, false, true",
            "enabled, true, false, true, false", "enabled, false, true, false, true", "enabled, true, true, true, true",
            "disabled, true, true, false, false", "unavailable, true, true, false, false"}, nullValues = "missing")
    @AllowCopilotExperimental
    void sessionRpc_connectors_capability_gates_preserve_legacy_requests(String availability,
            Boolean sessionAccountSelection, Boolean targetedReconcile, boolean expectAccount, boolean expectTarget)
            throws Exception {
        var mapper = new ObjectMapper();
        var capabilitiesJson = mapper.createObjectNode().put("apiVersion", 1).put("availability", availability)
                .put("consentContinuation", true).put("opaqueAccountSelection", true).put("maxPollAttempts", 5)
                .put("maxPollIntervalMs", 2_000).put("maxDeadlineMs", 30_000);
        if (sessionAccountSelection != null) {
            capabilitiesJson.put("sessionAccountSelection", sessionAccountSelection);
        }
        if (targetedReconcile != null) {
            capabilitiesJson.put("targetedReconcile", targetedReconcile);
        }
        var stub = new StubCaller();
        stub.nextResult = mapper.treeToValue(capabilitiesJson, SessionConnectorsGetCapabilitiesResult.class);
        var session = new SessionRpc(stub, "sess-connectors");

        var capabilities = session.connectors.getCapabilities().get();
        assertEquals(sessionAccountSelection, capabilities.sessionAccountSelection());
        assertEquals(targetedReconcile, capabilities.targetedReconcile());
        stub.nextResult = null;

        if (capabilities.availability() == ConnectorAvailability.ENABLED
                && Boolean.TRUE.equals(capabilities.sessionAccountSelection())) {
            session.connectors.getAccount();
        }
        if (capabilities.availability() == ConnectorAvailability.ENABLED) {
            session.connectors.reconcile(new SessionConnectorsReconcileRequest("account-1").setRefreshCatalog(false)
                    .setForceConnectorName(Boolean.TRUE.equals(capabilities.targetedReconcile()) ? "outlook" : null));
        }

        boolean enabled = "enabled".equals(availability);
        assertEquals(1 + (expectAccount ? 1 : 0) + (enabled ? 1 : 0), stub.calls.size());
        var capabilityParams = assertConnectorCall(stub.calls.get(0), "session.connectors.getCapabilities",
                SessionConnectorsGetCapabilitiesResult.class);
        assertEquals(1, capabilityParams.size());
        int nextCall = 1;
        if (expectAccount) {
            var params = assertConnectorCall(stub.calls.get(nextCall++), "session.connectors.getAccount",
                    ConnectorSessionAccount.class);
            assertEquals(mapper.valueToTree(Map.of("sessionId", "sess-connectors")), params);
        }
        if (enabled) {
            var params = assertConnectorCall(stub.calls.get(nextCall), "session.connectors.reconcile",
                    SessionConnectorsReconcileResult.class);
            assertEquals("account-1", params.get("accountId").asText());
            assertFalse(params.get("refreshCatalog").asBoolean());
            assertEquals(expectTarget, params.has("forceConnectorName"));
            assertEquals(expectTarget ? 4 : 3, params.size());
            if (expectTarget) {
                assertEquals("outlook", params.get("forceConnectorName").asText());
            }
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"token", "token-provider"})
    @AllowCopilotExperimental
    void sessionRpc_connectors_getAccount_preserves_present_and_null_results(String authType) throws Exception {
        var mapper = new ObjectMapper();
        try (var sockets = new SocketPair()) {
            var stub = sockets.stubServer();
            var session = new SessionRpc(sockets.client()::invoke, "sess-connectors");
            var capabilitiesFuture = session.connectors.getCapabilities();
            var capabilitiesRequest = stub.readOneMessage();
            assertEquals("session.connectors.getCapabilities", capabilitiesRequest.get("method").asText());
            stub.respond(capabilitiesRequest, mapper.readTree("""
                    {
                      "apiVersion": 1,
                      "availability": "enabled",
                      "consentContinuation": true,
                      "opaqueAccountSelection": true,
                      "sessionAccountSelection": true,
                      "maxPollAttempts": 5,
                      "maxPollIntervalMs": 2000,
                      "maxDeadlineMs": 30000
                    }
                    """));
            var capabilities = capabilitiesFuture.get(3, TimeUnit.SECONDS);
            assertEquals(ConnectorAvailability.ENABLED, capabilities.availability());
            assertTrue(Boolean.TRUE.equals(capabilities.sessionAccountSelection()));

            var future = session.connectors.getAccount();
            var request = stub.readOneMessage();
            assertEquals("session.connectors.getAccount", request.get("method").asText());
            assertEquals(mapper.valueToTree(Map.of("sessionId", "sess-connectors")), request.get("params"));
            var expected = authType == null ? mapper.getNodeFactory().nullNode() : mapper.readTree("""
                    {
                      "accountId": "account-1",
                      "authInfo": {"type":"%s","host":"github.com","login":"octocat"}
                    }
                    """.formatted(authType));
            stub.respond(request, expected);
            ConnectorSessionAccount account = future.get(3, TimeUnit.SECONDS);

            if (authType == null) {
                assertNull(account);
            } else {
                assertNotNull(account);
                assertEquals("account-1", account.accountId());
                AuthIdentityMetadata authInfo = account.authInfo();
                assertEquals(AuthInfoType.fromValue(authType), authInfo.type());
                assertEquals("github.com", authInfo.host());
                assertEquals("octocat", authInfo.login());
                assertEquals(expected, mapper.valueToTree(account));
                assertEquals(List.of("accountId", "authInfo"),
                        Arrays.stream(ConnectorSessionAccount.class.getRecordComponents()).map(RecordComponent::getName)
                                .sorted().toList());
                assertEquals(List.of("host", "login", "type"),
                        Arrays.stream(AuthIdentityMetadata.class.getRecordComponents()).map(RecordComponent::getName)
                                .sorted().toList());
            }
        }
    }

    @Test
    @AllowCopilotExperimental
    void sessionRpc_connectors_unknown_continuation_outcome_is_a_decode_error() throws Exception {
        try (var sockets = new SocketPair()) {
            var stub = sockets.stubServer();
            var session = new SessionRpc(sockets.client()::invoke, "sess-connectors");
            var future = session.connectors.continueConnection(SessionConnectorsContinueConnectionParams.builder()
                    .sessionId("ignored-session").continuationId("continuation-1").maxAttempts(3L)
                    .pollIntervalMs(1_000L).deadlineMs(10_000L).build());
            var request = stub.readOneMessage();
            assertEquals("session.connectors.continueConnection", request.get("method").asText());
            assertEquals(new ObjectMapper().readTree("""
                    {
                      "sessionId":"sess-connectors",
                      "continuationId":"continuation-1",
                      "maxAttempts":3,
                      "pollIntervalMs":1000,
                      "deadlineMs":10000
                    }
                    """), request.get("params"));
            stub.respond(request, new ObjectMapper().readTree("""
                    {"kind":"verification_required","continuationId":"continuation-1"}
                    """));

            var failure = assertThrows(ExecutionException.class, () -> future.get(3, TimeUnit.SECONDS));
            var decodeError = assertInstanceOf(InvalidTypeIdException.class, failure.getCause());
            assertEquals("verification_required", decodeError.getTypeId());
        }
    }

    @Test
    void sessionRpc_model_getCurrent_injects_sessionId_automatically() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-abc");

        session.model.getCurrent();

        assertEquals(1, stub.calls.size());
        assertEquals("session.model.getCurrent", stub.calls.get(0).method());

        // Params should be a Map containing sessionId
        var params = stub.calls.get(0).params();
        assertInstanceOf(Map.class, params);
        assertEquals("sess-abc", ((Map<?, ?>) params).get("sessionId"));
    }

    @Test
    void sessionRpc_model_switchTo_merges_sessionId_with_extra_params() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-xyz");

        // switchTo takes extra params beyond sessionId
        var switchParams = SessionModelSwitchToParams.builder().sessionId(null).modelId("gpt-5").autoTier(null)
                .reasoningEffort(null).reasoningSummary(null).verbosity(null).modelCapabilities(null).contextTier(null)
                .source(null).deferIfModelChangeQueued(null).compactionDecision(null).runCompactionPreflight(null)
                .repoScope(null).modelChangeScope(null).requireAvailable(null).pickerPersistence(null).build();
        session.model.switchTo(switchParams);
        session.model.switchTo(new SessionModelSwitchToRequest("gpt-5").setProviderId("catalog-provider"));

        assertEquals(2, stub.calls.size());
        assertEquals("session.model.switchTo", stub.calls.get(0).method());

        // Params should be a JsonNode containing both sessionId and modelId
        var params = stub.calls.get(0).params();
        assertInstanceOf(com.fasterxml.jackson.databind.node.ObjectNode.class, params);
        var node = (com.fasterxml.jackson.databind.node.ObjectNode) params;
        assertEquals("sess-xyz", node.get("sessionId").asText());
        assertFalse(node.has("providerId"));
        assertEquals("gpt-5", node.get("modelId").asText());
        var providerRequest = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(1).params();
        assertEquals("sess-xyz", providerRequest.get("sessionId").asText());
        assertEquals("catalog-provider", providerRequest.get("providerId").asText());
        assertEquals("gpt-5", providerRequest.get("modelId").asText());
    }

    @Test
    void modelSwitchBuilderPreservesOmittedNullAndValueOnTheWire() throws Exception {
        var builder = SessionModelSwitchToParams.builder().modelId("gpt-5");
        var requests = List.of(builder.build(), builder.reasoningEffort(null).build(),
                builder.reasoningEffort("high").build());
        try (var sockets = new SocketPair()) {
            var peer = sockets.stubServer();
            var session = new CopilotSession("presence-session", sockets.client());
            try {
                for (int index = 0; index < requests.size(); index++) {
                    var response = session.getRpc().model.switchTo(requests.get(index));
                    var message = peer.readOneMessage();
                    var params = message.path("params");
                    assertEquals("session.model.switchTo", message.path("method").asText());
                    assertEquals("presence-session", params.path("sessionId").asText());
                    assertEquals("gpt-5", params.path("modelId").asText());
                    assertFalse(params.has("autoTier"), "Untouched nullable fields must stay omitted: " + params);
                    var mapper = JsonRpcClient.getObjectMapper();
                    assertEquals(params,
                            mapper.valueToTree(mapper.treeToValue(params, SessionModelSwitchToParams.class)));
                    if (index == 0) {
                        assertFalse(params.has("reasoningEffort"), "Untouched effort must stay omitted: " + params);
                    } else {
                        assertTrue(params.has("reasoningEffort"), "Explicit effort must be present: " + params);
                        if (index == 1) {
                            assertTrue(params.get("reasoningEffort").isNull(), "Reset must send JSON null: " + params);
                        } else {
                            assertEquals("high", params.get("reasoningEffort").asText());
                        }
                    }
                    peer.respond(message, JsonRpcClient.getObjectMapper().createObjectNode().put("modelId", "gpt-5"));
                    response.get(3, TimeUnit.SECONDS);
                }
            } finally {
                session.forceStopLocally();
            }
        }
    }

    @Test
    void optionsUpdateBuilderPreservesExplicitClearsOnTheWire() throws Exception {
        var limits = com.github.copilot.generated.rpc.SessionLimitsConfig.builder().maxAiCredits(5.0).build();
        var requests = List.of(SessionOptionsUpdateParams.builder().build(),
                SessionOptionsUpdateParams.builder().sessionLimits(null).includedBuiltinAgents(null)
                        .includedBuiltinSkills(null).build(),
                SessionOptionsUpdateParams.builder().sessionLimits(limits).includedBuiltinAgents(List.of())
                        .includedBuiltinSkills(List.of()).build());
        try (var sockets = new SocketPair()) {
            var peer = sockets.stubServer();
            var session = new CopilotSession("limits-session", sockets.client());
            try {
                for (int index = 0; index < requests.size(); index++) {
                    var response = session.getRpc().options.update(requests.get(index));
                    var message = peer.readOneMessage();
                    var params = message.path("params");
                    assertEquals("session.options.update", message.path("method").asText());
                    assertEquals("limits-session", params.path("sessionId").asText());
                    assertFalse(params.has("model"), "Untouched options must stay omitted: " + params);
                    var mapper = JsonRpcClient.getObjectMapper();
                    assertEquals(params,
                            mapper.valueToTree(mapper.treeToValue(params, SessionOptionsUpdateParams.class)));
                    for (String field : List.of("sessionLimits", "includedBuiltinAgents", "includedBuiltinSkills")) {
                        if (index == 0) {
                            assertFalse(params.has(field), "Untouched " + field + " must stay omitted: " + params);
                        } else {
                            assertTrue(params.has(field), "Explicit " + field + " must be present: " + params);
                            if (index == 1) {
                                assertTrue(params.get(field).isNull(), "Clear must send JSON null: " + params);
                            }
                        }
                    }
                    if (index == 2) {
                        assertEquals(5.0, params.path("sessionLimits").path("maxAiCredits").asDouble());
                        assertTrue(params.get("includedBuiltinAgents").isArray());
                        assertTrue(params.get("includedBuiltinAgents").isEmpty());
                        assertTrue(params.get("includedBuiltinSkills").isArray());
                        assertTrue(params.get("includedBuiltinSkills").isEmpty());
                    }
                    peer.respond(message, JsonRpcClient.getObjectMapper().createObjectNode());
                    response.get(3, TimeUnit.SECONDS);
                }
            } finally {
                session.forceStopLocally();
            }
        }
    }

    @Test
    void extensibleModelSwitchRequestPreservesExplicitNull() {
        var caller = new StubCaller();
        var rpc = new SessionRpc(caller, "request-session");
        rpc.model.switchTo(new SessionModelSwitchToRequest("gpt-5"));
        rpc.model.switchTo(new SessionModelSwitchToRequest("gpt-5").setReasoningEffort(null));
        rpc.model.switchTo(new SessionModelSwitchToRequest("gpt-5").setReasoningEffort("high"));

        var omitted = JsonRpcClient.getObjectMapper().valueToTree(caller.calls.get(0).params());
        var cleared = JsonRpcClient.getObjectMapper().valueToTree(caller.calls.get(1).params());
        var provided = JsonRpcClient.getObjectMapper().valueToTree(caller.calls.get(2).params());
        assertFalse(omitted.has("reasoningEffort"));
        assertTrue(cleared.has("reasoningEffort"), "Explicit null must be present: " + cleared);
        assertTrue(cleared.get("reasoningEffort").isNull());
        assertEquals("high", provided.path("reasoningEffort").asText());
        assertFalse(cleared.has("autoTier"), "Untouched nullable fields must stay omitted: " + cleared);
    }

    @Test
    void nullablePresenceSurvivesJacksonWithoutChangingLegacyConstructorOmission() throws Exception {
        var mapper = JsonRpcClient.getObjectMapper();
        var input = mapper.readTree("""
                {"sessionId":"round-trip-session","modelId":"gpt-5","reasoningEffort":null}
                """);
        var decoded = mapper.treeToValue(input, SessionModelSwitchToParams.class);
        assertEquals(input, mapper.valueToTree(decoded));

        var types = Arrays.stream(SessionModelSwitchToParams.class.getRecordComponents())
                .filter(component -> !component.getAccessor()
                        .isAnnotationPresent(com.fasterxml.jackson.annotation.JsonIgnore.class))
                .map(RecordComponent::getType).toArray(Class<?>[]::new);
        var arguments = new Object[types.length];
        arguments[0] = "legacy-session";
        arguments[1] = "gpt-5";
        var legacy = SessionModelSwitchToParams.class.getConstructor(types).newInstance(arguments);
        var wire = mapper.valueToTree(legacy);
        assertEquals("legacy-session", wire.path("sessionId").asText());
        assertEquals("gpt-5", wire.path("modelId").asText());
        assertFalse(wire.has("reasoningEffort"), "Legacy null arguments must retain omission: " + wire);
        assertEquals(legacy, mapper.treeToValue(wire, SessionModelSwitchToParams.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"modelId\":\"gpt-5\"}", "{\"modelId\":\"gpt-5\",\"reasoningEffort\":null}",
            "{\"modelId\":\"gpt-5\",\"reasoningEffort\":\"high\"}"})
    void modelSwitchPresenceSurvivesCodecLessStreamingParsers(String json) throws Exception {
        var mapper = new ObjectMapper();
        try (var parser = new JsonFactory().createParser(json)) {
            assertNull(parser.getCodec());
            var request = mapper.readValue(parser, SessionModelSwitchToParams.class);
            assertEquals("gpt-5", request.modelId());
            assertEquals(mapper.readTree(json), mapper.valueToTree(request));
            assertNull(parser.getCodec());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"sessionId\":\"streaming-session\"}",
            "{\"sessionId\":\"streaming-session\",\"includedBuiltinSkills\":null,\"sessionLimits\":null}",
            "{\"sessionId\":\"streaming-session\",\"includedBuiltinSkills\":[\"code-review\"],"
                    + "\"sessionLimits\":{\"maxAiCredits\":5.0}}"})
    void optionPresenceSurvivesCodecLessStreamingParsers(String json) throws Exception {
        var mapper = new ObjectMapper();
        try (var parser = new JsonFactory().createParser(json)) {
            assertNull(parser.getCodec());
            var request = mapper.readValue(parser, SessionOptionsUpdateParams.class);
            assertEquals("streaming-session", request.sessionId());
            assertEquals(mapper.readTree(json), mapper.valueToTree(request));
            assertNull(parser.getCodec());
        }
    }

    @Test
    void nullableRecordPropertiesUseReaderConfiguration() throws Exception {
        var mapper = new ObjectMapper();
        var reader = mapper.readerFor(SessionOptionsUpdateParams.class)
                .with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        try (var parser = mapper.getFactory().createParser("{\"includedBuiltinSkills\":\"code-review\"}")) {
            SessionOptionsUpdateParams request = reader.readValue(parser);
            assertEquals(List.of("code-review"), request.includedBuiltinSkills());
        }
    }

    @Test
    void sessionRpc_agent_list_injects_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-999");

        session.agent.list();

        assertEquals(1, stub.calls.size());
        assertEquals("session.agent.list", stub.calls.get(0).method());

        var params = stub.calls.get(0).params();
        assertInstanceOf(com.fasterxml.jackson.databind.node.ObjectNode.class, params);
        assertEquals("sess-999", ((com.fasterxml.jackson.databind.node.ObjectNode) params).get("sessionId").asText());
    }

    @Test
    void sessionRpc_agent_select_merges_sessionId_with_extra_params() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-select");

        var selectParams = SessionAgentSelectParams.builder().sessionId(null).name("my-agent").build();
        session.agent.select(selectParams);

        assertEquals(1, stub.calls.size());
        assertEquals("session.agent.select", stub.calls.get(0).method());

        var params = stub.calls.get(0).params();
        assertInstanceOf(com.fasterxml.jackson.databind.node.ObjectNode.class, params);
        var node = (com.fasterxml.jackson.databind.node.ObjectNode) params;
        assertEquals("sess-select", node.get("sessionId").asText());
        assertEquals("my-agent", node.get("name").asText());
    }

    @Test
    void sessionRpc_different_sessions_have_different_sessionIds() {
        var stub = new StubCaller();
        var session1 = new SessionRpc(stub, "sess-1");
        var session2 = new SessionRpc(stub, "sess-2");

        session1.model.getCurrent();
        session2.model.getCurrent();

        assertEquals(2, stub.calls.size());
        var params1 = (Map<?, ?>) stub.calls.get(0).params();
        var params2 = (Map<?, ?>) stub.calls.get(1).params();
        assertEquals("sess-1", params1.get("sessionId"));
        assertEquals("sess-2", params2.get("sessionId"));
    }

    @Test
    void rpcCaller_is_implementable_as_anonymous_class_or_method_reference() {
        // Verify RpcCaller can be used as an anonymous class
        AtomicReference<String> capturedMethod = new AtomicReference<>();
        RpcCaller caller = new RpcCaller() {
            @Override
            public <T> CompletableFuture<T> invoke(String method, Object params, Class<T> resultType) {
                capturedMethod.set(method);
                return CompletableFuture.completedFuture(null);
            }
        };

        var server = new ServerRpc(caller);
        server.models.list();

        assertEquals("models.list", capturedMethod.get());
    }

    @Test
    void serverRpc_account_getQuota_invokes_correct_method() {
        var stub = new StubCaller();
        var server = new ServerRpc(stub);

        server.account.getQuota();

        assertEquals(1, stub.calls.size());
        assertEquals("account.getQuota", stub.calls.get(0).method());
    }

    // ── CopilotSession.getRpc() wiring tests ──────────────────────────────────
    // These tests use a socket-pair backed JsonRpcClient (same pattern as
    // RpcHandlerDispatcherTest) to construct a real CopilotSession and verify
    // that getRpc() returns a correctly wired SessionRpc.

    @ParameterizedTest
    @ValueSource(strings = {"list", "getCurrent", "select"})
    void copilotSession_agent_methods_deserialize_scoped_hooks_and_plugin_ownership(String method) throws Exception {
        var mapper = JsonRpcClient.getObjectMapper();
        var agentJson = mapper.readTree("""
                {
                  "name": "hook-agent",
                  "displayName": "Hook Agent",
                  "description": "Agent with scoped hooks",
                  "pluginId": "mp:catalog::tools",
                  "hooks": {
                    "preToolUse": [{"type":"command","bash":"echo scoped-hook","timeoutSec":10}],
                    "sessionStart": [{"type":"command","bash":"echo started"}]
                  }
                }
                """);
        try (var sockets = new SocketPair()) {
            var stub = sockets.stubServer();
            var session = new CopilotSession("sess-agent-hooks", sockets.client());
            try {
                CompletableFuture<List<AgentInfo>> future = switch (method) {
                    case "list" -> session.listAgents();
                    case "getCurrent" -> session.getCurrentAgent().thenApply(List::of);
                    case "select" -> session.selectAgent("hook-agent").thenApply(List::of);
                    default -> throw new IllegalArgumentException(method);
                };
                var request = stub.readOneMessage();
                assertEquals("session.agent." + method, request.get("method").asText());
                assertEquals("sess-agent-hooks", request.get("params").get("sessionId").asText());
                if ("select".equals(method)) {
                    assertEquals("hook-agent", request.get("params").get("name").asText());
                }
                var result = mapper.createObjectNode();
                if ("list".equals(method)) {
                    result.putArray("agents").add(agentJson);
                } else {
                    result.set("agent", agentJson);
                }
                stub.respond(request, result);

                var agents = future.get(3, TimeUnit.SECONDS);
                assertEquals(1, agents.size());
                assertEquals("hook-agent", agents.get(0).getName());
                assertEquals("mp:catalog::tools", agents.get(0).getPluginId());
                assertNotNull(agents.get(0).getHooks());
                assertEquals(agentJson.get("hooks"), mapper.valueToTree(agents.get(0).getHooks()));
            } finally {
                session.forceStopLocally();
            }
        }
    }

    @Test
    void copilotSession_compact_invokes_session_history_compact() throws Exception {
        var mapper = JsonRpcClient.getObjectMapper();
        try (var sockets = new SocketPair()) {
            var stub = sockets.stubServer();
            var session = new CopilotSession("sess-compact", sockets.client());
            try {
                CompletableFuture<Void> future = session.compact();
                var request = stub.readOneMessage();
                assertEquals("session.history.compact", request.get("method").asText());
                assertEquals("sess-compact", request.get("params").get("sessionId").asText());

                var result = mapper.createObjectNode();
                result.put("success", true);
                result.put("tokensRemoved", 1200);
                result.put("messagesRemoved", 4);
                result.put("summaryContent", "Earlier turns summarized.");
                stub.respond(request, result);

                assertNull(future.get(3, TimeUnit.SECONDS));
            } finally {
                session.forceStopLocally();
            }
        }
    }

    @Test
    void copilotSession_getRpc_returns_non_null_session_rpc() throws Exception {
        try (var sockets = new SocketPair()) {
            var rpc = sockets.client();
            var session = new CopilotSession("sess-unit", rpc);

            assertNotNull(session.getRpc());
        }
    }

    @Test
    void copilotSession_getRpc_sessionId_matches_session() throws Exception {
        try (var sockets = new SocketPair()) {
            var rpc = sockets.client();
            var stub = sockets.stubServer();
            var session = new CopilotSession("sess-test-id", rpc);

            // Call any no-arg session method via getRpc() to verify sessionId injection
            session.getRpc().agent.list();

            // Drain the sent message from the stub server
            var sent = stub.readOneMessage();
            assertEquals("session.agent.list", sent.get("method").asText());
            assertEquals("sess-test-id", sent.get("params").get("sessionId").asText());
        }
    }

    @Test
    void copilotSession_getRpc_updates_when_sessionId_changes() throws Exception {
        try (var sockets = new SocketPair()) {
            var rpc = sockets.client();
            var stub = sockets.stubServer();
            var session = new CopilotSession("old-id", rpc);

            // Simulate server returning a different sessionId (v2 CLI behaviour)
            session.setActiveSessionId("new-id");

            session.getRpc().agent.list();

            var sent = stub.readOneMessage();
            assertEquals("new-id", sent.get("params").get("sessionId").asText(),
                    "getRpc() should reflect the updated sessionId");
        }
    }

    @Test
    void copilotSession_getRpc_all_namespace_fields_present() throws Exception {
        try (var sockets = new SocketPair()) {
            var rpc = sockets.client();
            var session = new CopilotSession("sess-ns", rpc);

            var sessionRpc = session.getRpc();
            assertNotNull(sessionRpc.model);
            assertNotNull(sessionRpc.agent);
            assertNotNull(sessionRpc.skills);
            assertNotNull(sessionRpc.tools);
            assertNotNull(sessionRpc.permissions);
            assertNotNull(sessionRpc.commands);
            assertNotNull(sessionRpc.ui);
            assertNotNull(sessionRpc.connectors);
        }
    }

    @Test
    void copilotSession_getRpc_is_lazy_and_cached() throws Exception {
        // Verify lazy init: getRpc() returns the same instance on repeated calls
        // (caches rather than allocating a new SessionRpc per call).
        try (var sockets = new SocketPair()) {
            var rpc = sockets.client();
            var session = new CopilotSession("sess-cache", rpc);

            var first = session.getRpc();
            var second = session.getRpc();
            assertNotNull(first);
            assertSame(first, second, "getRpc() must return the cached instance when sessionId has not changed");
        }
    }

    @Test
    void copilotSession_getRpc_returns_new_instance_after_sessionId_change() throws Exception {
        // Verify that after setActiveSessionId() the old cached instance is discarded
        // and the next getRpc() call produces a fresh SessionRpc with the new ID.
        try (var sockets = new SocketPair()) {
            var rpc = sockets.client();
            var stub = sockets.stubServer();
            var session = new CopilotSession("old-id", rpc);

            var before = session.getRpc();
            session.setActiveSessionId("new-id");
            var after = session.getRpc();

            assertNotNull(before);
            assertNotNull(after);
            assertNotSame(before, after, "getRpc() must return a new instance after sessionId changes");

            // Confirm the new instance uses the new sessionId
            after.agent.list();
            var sent = stub.readOneMessage();
            assertEquals("new-id", sent.get("params").get("sessionId").asText());
        }
    }

    @Test
    void copilotClient_getRpc_throws_before_start() {
        // CopilotClient.getRpc() should throw before start() is called.
        var client = new CopilotClient();
        assertThrows(IllegalStateException.class, client::getRpc,
                "getRpc() must throw IllegalStateException if called before start()");
    }

    // ── session.mcp.apps.callTool tests ───────────────────────────────────────

    @Test
    void sessionRpc_mcp_apps_callTool_invokes_correct_rpc_method() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-mcp");

        var params = com.github.copilot.generated.rpc.SessionMcpAppsCallToolParams.builder().sessionId(null)
                .serverName("my-server").toolName("my-tool").arguments(null).originServerName("my-origin").build();
        session.mcp.apps.callTool(params);

        assertEquals(1, stub.calls.size());
        assertEquals("session.mcp.apps.callTool", stub.calls.get(0).method());
    }

    @Test
    void sessionRpc_mcp_apps_callTool_injects_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-ct-inject");

        var params = com.github.copilot.generated.rpc.SessionMcpAppsCallToolParams.builder().sessionId(null)
                .serverName("server1").toolName("tool1").arguments(null).originServerName("server-origin").build();
        session.mcp.apps.callTool(params);

        var sentParams = stub.calls.get(0).params();
        assertInstanceOf(com.fasterxml.jackson.databind.node.ObjectNode.class, sentParams);
        var node = (com.fasterxml.jackson.databind.node.ObjectNode) sentParams;
        assertEquals("sess-ct-inject", node.get("sessionId").asText());
    }

    @Test
    void sessionRpc_mcp_apps_callTool_request_preserves_metadata() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-meta");
        var params = new com.github.copilot.generated.rpc.SessionMcpAppsCallToolRequest("server", "tool", "server")
                .setArguments(Map.of("value", 42)).setMeta(Map.of("com.example/context", Map.of("enabled", false)));

        session.mcp.apps.callTool(params);

        assertEquals(1, stub.calls.size());
        var call = stub.calls.get(0);
        assertEquals("session.mcp.apps.callTool", call.method());
        var node = new ObjectMapper().valueToTree(call.params());
        assertEquals("sess-meta", node.get("sessionId").asText());
        assertEquals(42, node.get("arguments").get("value").asInt());
        assertEquals(false, node.get("_meta").get("com.example/context").get("enabled").asBoolean());
    }

    @Test
    void sessionRpc_mcp_apps_callTool_keeps_null_map_entries() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-null-map");
        var nested = new LinkedHashMap<String, Object>();
        nested.put("inner", null);
        nested.put("x", 2);
        var arguments = new LinkedHashMap<String, Object>();
        arguments.put("keep", 1);
        arguments.put("explicitNull", null);
        arguments.put("nested", nested);
        var meta = new LinkedHashMap<String, Object>();
        meta.put("k", null);

        session.mcp.apps
                .callTool(new com.github.copilot.generated.rpc.SessionMcpAppsCallToolRequest("server", "tool", "server")
                        .setArguments(arguments).setMeta(meta));
        // Exercise legacy wire omission independently of the builder's required-field
        // checks.
        session.mcp.apps.callTool(new ObjectMapper().convertValue(
                Map.of("serverName", "server", "toolName", "tool", "arguments", arguments),
                com.github.copilot.generated.rpc.SessionMcpAppsCallToolParams.class));

        assertEquals(2, stub.calls.size());
        for (var call : stub.calls) {
            var node = (com.fasterxml.jackson.databind.node.ObjectNode) call.params();
            var args = node.get("arguments");
            assertEquals(1, args.get("keep").asInt());
            assertTrue(args.has("explicitNull"), "a null map entry must be sent, got: " + node);
            assertTrue(args.get("explicitNull").isNull());
            assertTrue(args.get("nested").has("inner"), "a nested null map entry must be sent, got: " + node);
            assertTrue(args.get("nested").get("inner").isNull());
        }
        var requestNode = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(0).params();
        assertTrue(requestNode.get("_meta").has("k"), "a null _meta entry must be sent, got: " + requestNode);
        assertTrue(requestNode.get("_meta").get("k").isNull());
        // Unset properties are still omitted.
        var recordNode = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(1).params();
        assertFalse(recordNode.has("originServerName"), "an unset property must be omitted, got: " + recordNode);
    }

    @Test
    void sessionRpc_mcp_apps_callTool_returns_jsonNode_payload() throws Exception {
        var stub = new StubCaller();
        var mapper = new ObjectMapper();
        var expectedResult = mapper.createObjectNode();
        expectedResult.put("content", "hello world");
        expectedResult.put("isError", false);
        stub.nextResult = expectedResult;

        var session = new SessionRpc(stub, "sess-payload");
        var params = com.github.copilot.generated.rpc.SessionMcpAppsCallToolParams.builder().sessionId(null)
                .serverName("echo-server").toolName("echo").arguments(null).originServerName("echo-origin").build();
        var future = session.mcp.apps.callTool(params);

        var result = future.get();
        assertInstanceOf(com.fasterxml.jackson.databind.JsonNode.class, result);
        assertEquals("hello world", result.get("content").asText());
        assertEquals(false, result.get("isError").asBoolean());
    }

    private static com.fasterxml.jackson.databind.JsonNode assertConnectorCall(StubCaller.Call call,
            String expectedMethod, Class<?> expectedResultType) {
        assertEquals(expectedMethod, call.method());
        assertEquals(expectedResultType, call.resultType());
        var params = new ObjectMapper().valueToTree(call.params());
        assertEquals("sess-connectors", params.get("sessionId").asText());
        return params;
    }

    /**
     * Helper that creates a loopback socket pair. The client side is used by
     * {@link JsonRpcClient}; the server side can be read to inspect outbound
     * messages.
     */
    private static final class SocketPair implements AutoCloseable {

        private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

        private final java.net.Socket clientSocket;
        private final java.net.Socket serverSocket;
        private final JsonRpcClient rpcClient;

        SocketPair() throws Exception {
            var loopback = java.net.InetAddress.getLoopbackAddress();
            try (var ss = new java.net.ServerSocket(0, 1, loopback)) {
                clientSocket = new java.net.Socket(loopback, ss.getLocalPort());
                serverSocket = ss.accept();
            }
            serverSocket.setSoTimeout(3000);
            rpcClient = JsonRpcClient.fromSocket(clientSocket);
        }

        JsonRpcClient client() {
            return rpcClient;
        }

        StubServer stubServer() {
            return new StubServer(serverSocket);
        }

        @Override
        public void close() throws Exception {
            rpcClient.close();
            clientSocket.close();
            serverSocket.close();
        }
    }

    /**
     * Exchanges raw JSON-RPC messages on the server side of the socket.
     */
    private static final class StubServer {

        private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

        private final java.io.InputStream in;
        private final java.io.OutputStream out;

        StubServer(java.net.Socket socket) {
            try {
                this.in = socket.getInputStream();
                this.out = socket.getOutputStream();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        void respond(JsonNode request, JsonNode result) throws Exception {
            var response = MAPPER.createObjectNode();
            response.put("jsonrpc", "2.0");
            response.set("id", request.get("id"));
            response.set("result", result);
            byte[] body = MAPPER.writeValueAsBytes(response);
            out.write(("Content-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            out.write(body);
            out.flush();
        }

        /**
         * Reads one JSON-RPC message (Content-Length framed) from the stream.
         */
        com.fasterxml.jackson.databind.JsonNode readOneMessage() throws Exception {
            // Read Content-Length header
            var header = new StringBuilder();
            int b;
            while ((b = in.read()) != -1) {
                if (b == '\n' && header.toString().endsWith("\r")) {
                    break;
                }
                header.append((char) b);
            }
            // Skip blank line
            in.read(); // '\r'
            in.read(); // '\n'

            String hdr = header.toString().trim();
            int colon = hdr.indexOf(':');
            int len = Integer.parseInt(hdr.substring(colon + 1).trim());
            byte[] body = in.readNBytes(len);
            return MAPPER.readTree(body);
        }
    }
}
