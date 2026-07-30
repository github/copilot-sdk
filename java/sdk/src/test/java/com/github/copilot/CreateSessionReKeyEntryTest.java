/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

/**
 * Tests that CopilotClient rejects session.create responses whose sessionId
 * differs from the client-supplied one. Re-keying the sessions map at runtime
 * is intentionally not supported — the server must honor the client-supplied
 * sessionId (or generate one when none is supplied).
 */
class CreateSessionReKeyEntryTest {

    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

    /**
     * A connected socket pair where the server replies to "session.create" with a
     * configurable sessionId and then replies to "session.options.update" with
     * success or failure.
     */
    private static final class ReKeyServer implements AutoCloseable {

        final Socket clientSocket;
        final Socket serverSocket;
        final JsonRpcClient rpcClient;
        private volatile boolean running = true;
        private final Thread replyThread;

        /** The sessionId to return in the session.create response. */
        private final String returnedSessionId;
        /** If true, the session.options.update call will fail. */
        private final boolean failOptionsUpdate;

        ReKeyServer(String returnedSessionId, boolean failOptionsUpdate) throws Exception {
            this.returnedSessionId = returnedSessionId;
            this.failOptionsUpdate = failOptionsUpdate;

            try (var ss = new ServerSocket(0)) {
                clientSocket = new Socket("localhost", ss.getLocalPort());
                serverSocket = ss.accept();
            }
            serverSocket.setSoTimeout(5000);
            rpcClient = JsonRpcClient.fromSocket(clientSocket);

            replyThread = new Thread(() -> {
                try {
                    var in = serverSocket.getInputStream();
                    var out = serverSocket.getOutputStream();
                    while (running) {
                        // Read Content-Length header
                        var header = new StringBuilder();
                        int b;
                        while ((b = in.read()) != -1) {
                            if (b == '\n' && header.toString().endsWith("\r")) {
                                break;
                            }
                            header.append((char) b);
                        }
                        if (b == -1)
                            break;
                        // Skip blank line
                        in.read(); // '\r'
                        in.read(); // '\n'

                        String hdr = header.toString().trim();
                        int colon = hdr.indexOf(':');
                        int len = Integer.parseInt(hdr.substring(colon + 1).trim());
                        byte[] body = in.readNBytes(len);
                        JsonNode msg = MAPPER.readTree(body);

                        String method = msg.get("method").asText();
                        long id = msg.get("id").asLong();

                        if ("session.create".equals(method)) {
                            // Return a response with the (possibly different) session ID
                            ObjectNode result = MAPPER.createObjectNode();
                            result.put("sessionId", returnedSessionId);
                            String response = MAPPER.writeValueAsString(MAPPER.createObjectNode().put("jsonrpc", "2.0")
                                    .put("id", id).set("result", result));
                            sendRpcMessage(out, response);
                        } else if ("session.options.update".equals(method)) {
                            if (failOptionsUpdate) {
                                // Send an error response
                                ObjectNode error = MAPPER.createObjectNode();
                                error.put("code", -32000);
                                error.put("message", "simulated options update failure");
                                String response = MAPPER.writeValueAsString(MAPPER.createObjectNode()
                                        .put("jsonrpc", "2.0").put("id", id).set("error", error));
                                sendRpcMessage(out, response);
                            } else {
                                // Send a success response
                                String response = MAPPER.writeValueAsString(
                                        MAPPER.createObjectNode().put("jsonrpc", "2.0").put("id", id).set("result",
                                                MAPPER.createObjectNode().put("success", true)));
                                sendRpcMessage(out, response);
                            }
                        } else {
                            // Generic success for anything else
                            String response = MAPPER.writeValueAsString(MAPPER.createObjectNode().put("jsonrpc", "2.0")
                                    .put("id", id).set("result", MAPPER.createObjectNode().put("success", true)));
                            sendRpcMessage(out, response);
                        }
                    }
                } catch (Exception e) {
                    if (running) {
                        // Ignore expected exceptions on shutdown
                    }
                }
            });
            replyThread.setDaemon(true);
            replyThread.start();
        }

        private static void sendRpcMessage(OutputStream out, String json) throws Exception {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            String header = "Content-Length: " + bytes.length + "\r\n\r\n";
            out.write(header.getBytes(StandardCharsets.UTF_8));
            out.write(bytes);
            out.flush();
        }

        @Override
        public void close() throws Exception {
            running = false;
            rpcClient.close();
            clientSocket.close();
            serverSocket.close();
            replyThread.join(3000);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, CopilotSession> getSessionsMap(CopilotClient client) throws Exception {
        Field f = CopilotClient.class.getDeclaredField("sessions");
        f.setAccessible(true);
        return (Map<String, CopilotSession>) f.get(client);
    }

    private static void injectConnection(CopilotClient client, JsonRpcClient rpc) throws Exception {
        // Build a Connection record via the private record constructor
        Class<?> connClass = null;
        for (Class<?> c : CopilotClient.class.getDeclaredClasses()) {
            if (c.getSimpleName().equals("Connection")) {
                connClass = c;
                break;
            }
        }
        assertNotNull(connClass, "Could not find Connection inner class");

        var ctor = connClass.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        // Connection(JsonRpcClient rpc, Process process, ServerRpc serverRpc)
        Object connection = ctor.newInstance(rpc, null, null);

        Field f = CopilotClient.class.getDeclaredField("connectionFuture");
        f.setAccessible(true);
        f.set(client, CompletableFuture.completedFuture(connection));
    }

    @Test
    void createSession_serverReturnsDifferentSessionId_throwsAndRemovesPreRegisteredEntry() throws Exception {
        String clientSessionId = "client-supplied-id";
        String serverSessionId = "server-returned-id";

        try (var server = new ReKeyServer(serverSessionId, false)) {
            var client = new CopilotClient(new CopilotClientOptions().setAutoStart(false));
            injectConnection(client, server.rpcClient);

            var config = new SessionConfig().setSessionId(clientSessionId)
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL);

            ExecutionException ex = assertThrows(ExecutionException.class, () -> client.createSession(config).get());
            assertNotNull(ex.getCause());
            assertTrue(ex.getCause().getMessage().contains(serverSessionId),
                    "Error message should mention the server-returned sessionId");
            assertTrue(ex.getCause().getMessage().contains(clientSessionId),
                    "Error message should mention the client-requested sessionId");

            Map<String, CopilotSession> sessions = getSessionsMap(client);
            assertNull(sessions.get(clientSessionId),
                    "Pre-registered client-supplied sessionId should be removed after rejection");
            assertNull(sessions.get(serverSessionId),
                    "Server-returned sessionId should never be registered after rejection");
            assertTrue(sessions.isEmpty(), "Sessions map should be empty after rejected create");

            client.close();
        }
    }

    @Test
    void createSession_serverReturnsDifferentSessionIdWithSkipCustomInstructions_throwsAndCleansUp() throws Exception {
        String clientSessionId = "client-supplied-id";
        String serverSessionId = "server-returned-id";

        try (var server = new ReKeyServer(serverSessionId, true)) {
            var client = new CopilotClient(new CopilotClientOptions().setAutoStart(false));
            injectConnection(client, server.rpcClient);

            // Even when skipCustomInstructions would trigger session.options.update,
            // the sessionId-mismatch error fires first and short-circuits the flow.
            var config = new SessionConfig().setSessionId(clientSessionId)
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setSkipCustomInstructions(true);

            ExecutionException ex = assertThrows(ExecutionException.class, () -> client.createSession(config).get());
            assertNotNull(ex.getCause());

            Map<String, CopilotSession> sessions = getSessionsMap(client);
            assertNull(sessions.get(clientSessionId),
                    "Pre-registered client-supplied sessionId should be removed on failure");
            assertNull(sessions.get(serverSessionId),
                    "Server-returned sessionId should never be registered on failure");
            assertTrue(sessions.isEmpty(), "Sessions map should be empty after failed create");

            client.close();
        }
    }

    @Test
    void createSession_serverReturnsSameSessionId_sessionKeptUnderClientId() throws Exception {
        String sessionId = "same-id-for-both";

        try (var server = new ReKeyServer(sessionId, false)) {
            var client = new CopilotClient(new CopilotClientOptions().setAutoStart(false));
            injectConnection(client, server.rpcClient);

            var config = new SessionConfig().setSessionId(sessionId)
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL);

            CopilotSession session = client.createSession(config).get();

            Map<String, CopilotSession> sessions = getSessionsMap(client);

            // When IDs match, the session stays under the original key
            assertSame(session, sessions.get(sessionId),
                    "Session should remain under original key when server returns same ID");
            assertEquals(1, sessions.size(), "Should have exactly one entry in sessions map");

            client.close();
        }
    }
}
