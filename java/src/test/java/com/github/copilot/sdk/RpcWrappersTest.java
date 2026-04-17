/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.sdk.generated.rpc.McpConfigAddParams;
import com.github.copilot.sdk.generated.rpc.McpDiscoverParams;
import com.github.copilot.sdk.generated.rpc.RpcCaller;
import com.github.copilot.sdk.generated.rpc.ServerRpc;
import com.github.copilot.sdk.generated.rpc.SessionAgentSelectParams;
import com.github.copilot.sdk.generated.rpc.SessionModelSwitchToParams;
import com.github.copilot.sdk.generated.rpc.SessionRpc;

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

        static record Call(String method, Object params) {
        }

        final List<Call> calls = new ArrayList<>();
        Object nextResult = null;

        @Override
        @SuppressWarnings("unchecked")
        public <T> CompletableFuture<T> invoke(String method, Object params, Class<T> resultType) {
            calls.add(new Call(method, params));
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
    void serverRpc_ping_passes_params_directly() {
        var stub = new StubCaller();
        var server = new ServerRpc(stub);

        var params = new com.github.copilot.sdk.generated.rpc.PingParams(null);
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

        var params = new McpConfigAddParams("myServer", null);
        server.mcp.config.add(params);

        assertEquals(1, stub.calls.size());
        assertEquals("mcp.config.add", stub.calls.get(0).method());
        assertSame(params, stub.calls.get(0).params());
    }

    @Test
    void serverRpc_mcp_discover_passes_params() {
        var stub = new StubCaller();
        var server = new ServerRpc(stub);

        var params = new McpDiscoverParams("/workspace");
        server.mcp.discover(params);

        assertEquals(1, stub.calls.size());
        assertEquals("mcp.discover", stub.calls.get(0).method());
        assertSame(params, stub.calls.get(0).params());
    }

    // ── SessionRpc tests ──────────────────────────────────────────────────────

    @Test
    void sessionRpc_instantiates_with_all_namespace_fields() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-001");

        assertNotNull(session.model);
        assertNotNull(session.mode);
        assertNotNull(session.plan);
        assertNotNull(session.workspace);
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
        var switchParams = new SessionModelSwitchToParams(null, "gpt-5", null, null);
        session.model.switchTo(switchParams);

        assertEquals(1, stub.calls.size());
        assertEquals("session.model.switchTo", stub.calls.get(0).method());

        // Params should be a JsonNode containing both sessionId and modelId
        var params = stub.calls.get(0).params();
        assertInstanceOf(com.fasterxml.jackson.databind.node.ObjectNode.class, params);
        var node = (com.fasterxml.jackson.databind.node.ObjectNode) params;
        assertEquals("sess-xyz", node.get("sessionId").asText());
        assertEquals("gpt-5", node.get("modelId").asText());
    }

    @Test
    void sessionRpc_agent_list_injects_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-999");

        session.agent.list();

        assertEquals(1, stub.calls.size());
        assertEquals("session.agent.list", stub.calls.get(0).method());

        var params = stub.calls.get(0).params();
        assertInstanceOf(Map.class, params);
        assertEquals("sess-999", ((Map<?, ?>) params).get("sessionId"));
    }

    @Test
    void sessionRpc_agent_select_merges_sessionId_with_extra_params() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-select");

        var selectParams = new SessionAgentSelectParams(null, "my-agent");
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
            try (var ss = new java.net.ServerSocket(0)) {
                clientSocket = new java.net.Socket("localhost", ss.getLocalPort());
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
     * Reads raw JSON-RPC messages written to the server side of the socket.
     */
    private static final class StubServer {

        private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

        private final java.io.InputStream in;

        StubServer(java.net.Socket socket) {
            try {
                this.in = socket.getInputStream();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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
