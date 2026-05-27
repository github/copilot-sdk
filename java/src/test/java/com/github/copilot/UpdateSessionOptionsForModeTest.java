/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.rpc.CopilotClientMode;
import com.github.copilot.rpc.CopilotClientOptions;

/**
 * Tests for {@link CopilotClient#updateSessionOptionsForMode}.
 */
class UpdateSessionOptionsForModeTest {

    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

    /**
     * A connected socket pair where the "server" side auto-replies to every
     * JSON-RPC request with {@code {"success": true}}.
     */
    private static final class AutoReplyPair implements AutoCloseable {

        final Socket clientSocket;
        final Socket serverSocket;
        final JsonRpcClient rpcClient;
        private volatile boolean running = true;
        private final Thread replyThread;
        /** The last JSON-RPC params node received by the stub server. */
        volatile JsonNode lastParams;
        /** The last JSON-RPC method received by the stub server. */
        volatile String lastMethod;

        AutoReplyPair() throws Exception {
            try (var ss = new ServerSocket(0)) {
                clientSocket = new Socket("localhost", ss.getLocalPort());
                serverSocket = ss.accept();
            }
            serverSocket.setSoTimeout(5000);
            rpcClient = JsonRpcClient.fromSocket(clientSocket);

            // Background thread that reads requests and sends back success responses
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

                        lastMethod = msg.get("method").asText();
                        lastParams = msg.get("params");
                        long id = msg.get("id").asLong();

                        // Send back a success response
                        String response = MAPPER.writeValueAsString(MAPPER.createObjectNode().put("jsonrpc", "2.0")
                                .put("id", id).set("result", MAPPER.createObjectNode().put("success", true)));
                        sendRpcMessage(out, response);
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

    // ── COPILOT_CLI mode tests ────────────────────────────────────────────────

    @Test
    void copilotCliMode_noFieldsSet_noPatchSent() throws Exception {
        try (var pair = new AutoReplyPair()) {
            var session = new CopilotSession("sess-1", pair.rpcClient);
            var client = new CopilotClient(new CopilotClientOptions().setAutoStart(false));

            client.updateSessionOptionsForMode(session, null, null, null, null).get();

            assertNull(pair.lastMethod, "No RPC call should be made when no fields are set in COPILOT_CLI mode");
            client.close();
        }
    }

    @Test
    void copilotCliMode_skipCustomInstructionsSet_patchContainsOnlyThatField() throws Exception {
        try (var pair = new AutoReplyPair()) {
            var session = new CopilotSession("sess-1", pair.rpcClient);
            var client = new CopilotClient(new CopilotClientOptions().setAutoStart(false));

            client.updateSessionOptionsForMode(session, true, null, null, null).get();

            assertEquals("session.options.update", pair.lastMethod);
            assertTrue(pair.lastParams.get("skipCustomInstructions").asBoolean());
            assertTrue(pair.lastParams.path("customAgentsLocalOnly").isMissingNode(),
                    "customAgentsLocalOnly should be absent");
            assertTrue(pair.lastParams.path("coauthorEnabled").isMissingNode(), "coauthorEnabled should be absent");
            assertTrue(pair.lastParams.path("manageScheduleEnabled").isMissingNode(),
                    "manageScheduleEnabled should be absent");
            assertTrue(pair.lastParams.path("installedPlugins").isMissingNode(), "installedPlugins should be absent");
            client.close();
        }
    }

    @Test
    void copilotCliMode_allFieldsSet_allPropagated() throws Exception {
        try (var pair = new AutoReplyPair()) {
            var session = new CopilotSession("sess-1", pair.rpcClient);
            var client = new CopilotClient(new CopilotClientOptions().setAutoStart(false));

            client.updateSessionOptionsForMode(session, false, true, true, false).get();

            assertEquals("session.options.update", pair.lastMethod);
            assertFalse(pair.lastParams.get("skipCustomInstructions").asBoolean());
            assertTrue(pair.lastParams.get("customAgentsLocalOnly").asBoolean());
            assertTrue(pair.lastParams.get("coauthorEnabled").asBoolean());
            assertFalse(pair.lastParams.get("manageScheduleEnabled").asBoolean());
            client.close();
        }
    }

    @Test
    void copilotCliMode_onlyCoauthorEnabled_patchSent() throws Exception {
        try (var pair = new AutoReplyPair()) {
            var session = new CopilotSession("sess-1", pair.rpcClient);
            var client = new CopilotClient(new CopilotClientOptions().setAutoStart(false));

            client.updateSessionOptionsForMode(session, null, null, true, null).get();

            assertEquals("session.options.update", pair.lastMethod);
            assertTrue(pair.lastParams.get("coauthorEnabled").asBoolean());
            client.close();
        }
    }

    // ── EMPTY mode tests ──────────────────────────────────────────────────────

    @Test
    void emptyMode_noFieldsSet_safeDefaultsSent() throws Exception {
        try (var pair = new AutoReplyPair()) {
            var session = new CopilotSession("sess-1", pair.rpcClient);
            var client = new CopilotClient(new CopilotClientOptions().setMode(CopilotClientMode.EMPTY)
                    .setCopilotHome("/tmp/copilot-home").setAutoStart(false));

            client.updateSessionOptionsForMode(session, null, null, null, null).get();

            assertEquals("session.options.update", pair.lastMethod);
            assertTrue(pair.lastParams.get("skipCustomInstructions").asBoolean(), "default: skip custom instructions");
            assertTrue(pair.lastParams.get("customAgentsLocalOnly").asBoolean(), "default: local agents only");
            assertFalse(pair.lastParams.get("coauthorEnabled").asBoolean(), "default: coauthor disabled");
            assertFalse(pair.lastParams.get("manageScheduleEnabled").asBoolean(), "default: schedule disabled");
            assertTrue(pair.lastParams.get("installedPlugins").isArray(), "installedPlugins should be empty array");
            assertEquals(0, pair.lastParams.get("installedPlugins").size());
            client.close();
        }
    }

    @Test
    void emptyMode_callerOverridesWin() throws Exception {
        try (var pair = new AutoReplyPair()) {
            var session = new CopilotSession("sess-1", pair.rpcClient);
            var client = new CopilotClient(new CopilotClientOptions().setMode(CopilotClientMode.EMPTY)
                    .setCopilotHome("/tmp/copilot-home").setAutoStart(false));

            client.updateSessionOptionsForMode(session, false, false, true, true).get();

            assertEquals("session.options.update", pair.lastMethod);
            assertFalse(pair.lastParams.get("skipCustomInstructions").asBoolean(), "caller override: don't skip");
            assertFalse(pair.lastParams.get("customAgentsLocalOnly").asBoolean(), "caller override: not local only");
            assertTrue(pair.lastParams.get("coauthorEnabled").asBoolean(), "caller override: coauthor enabled");
            assertTrue(pair.lastParams.get("manageScheduleEnabled").asBoolean(), "caller override: schedule enabled");
            assertTrue(pair.lastParams.get("installedPlugins").isArray(),
                    "installedPlugins always empty in EMPTY mode");
            assertEquals(0, pair.lastParams.get("installedPlugins").size());
            client.close();
        }
    }

    @Test
    void emptyMode_partialOverrides_restGetDefaults() throws Exception {
        try (var pair = new AutoReplyPair()) {
            var session = new CopilotSession("sess-1", pair.rpcClient);
            var client = new CopilotClient(new CopilotClientOptions().setMode(CopilotClientMode.EMPTY)
                    .setCopilotHome("/tmp/copilot-home").setAutoStart(false));

            // Only override coauthorEnabled, rest should use safe defaults
            client.updateSessionOptionsForMode(session, null, null, true, null).get();

            assertEquals("session.options.update", pair.lastMethod);
            assertTrue(pair.lastParams.get("skipCustomInstructions").asBoolean(), "default: skip");
            assertTrue(pair.lastParams.get("customAgentsLocalOnly").asBoolean(), "default: local only");
            assertTrue(pair.lastParams.get("coauthorEnabled").asBoolean(), "override: coauthor enabled");
            assertFalse(pair.lastParams.get("manageScheduleEnabled").asBoolean(), "default: schedule disabled");
            client.close();
        }
    }

    // ── SessionId injection ───────────────────────────────────────────────────

    @Test
    void sessionIdInjectedBySessionOptionsApi() throws Exception {
        try (var pair = new AutoReplyPair()) {
            var session = new CopilotSession("my-session-id", pair.rpcClient);
            var client = new CopilotClient(new CopilotClientOptions().setAutoStart(false));

            client.updateSessionOptionsForMode(session, true, null, null, null).get();

            assertEquals("session.options.update", pair.lastMethod);
            assertEquals("my-session-id", pair.lastParams.get("sessionId").asText(),
                    "SessionOptionsApi should inject sessionId");
            client.close();
        }
    }
}
