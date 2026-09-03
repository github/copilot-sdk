/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.ModelSwitchAutoTierStatus;
import com.github.copilot.generated.rpc.SessionModelSwitchAutoTierResult;
import com.github.copilot.rpc.AutoTier;
import com.github.copilot.rpc.SetModelOptions;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import org.junit.jupiter.api.Test;

/**
 * Verifies the wire payloads produced by Auto routing preference switches.
 * <p>
 * The runtime treats an explicit {@code null} {@code autoTier} (return to
 * provider-default routing) differently from an absent one (leave the
 * preference unchanged), so these tests assert on the raw JSON rather than on
 * the generated params records, which drop null properties.
 */
@AllowCopilotExperimental
class SessionAutoTierSwitchTest {

    @Test
    void setModel_omits_autoTier_when_no_preference_is_requested() throws Exception {
        try (var sockets = new SocketPair()) {
            var session = new CopilotSession("sess-1", sockets.client());
            var stub = sockets.stubServer();

            session.setModel(new SetModelOptions().setModel("auto"));

            var params = stub.readOneMessage().get("params");
            assertEquals("auto", params.get("modelId").asText());
            assertFalse(params.has("autoTier"), "an unset preference must not appear on the wire");
        }
    }

    @Test
    void setModel_sends_requested_autoTier() throws Exception {
        try (var sockets = new SocketPair()) {
            var session = new CopilotSession("sess-2", sockets.client());
            var stub = sockets.stubServer();

            session.setModel(new SetModelOptions().setModel("auto").setAutoTier(AutoTier.INTELLIGENCE)
                    .setReasoningEffort("high"));

            var sent = stub.readOneMessage();
            assertEquals("session.model.switchTo", sent.get("method").asText());
            var params = sent.get("params");
            assertEquals("intelligence", params.get("autoTier").asText());
            assertEquals("high", params.get("reasoningEffort").asText());
            assertEquals("sess-2", params.get("sessionId").asText());
        }
    }

    @Test
    void setModel_sends_explicit_null_autoTier_when_clearing() throws Exception {
        try (var sockets = new SocketPair()) {
            var session = new CopilotSession("sess-3", sockets.client());
            var stub = sockets.stubServer();

            session.setModel(new SetModelOptions().setModel("auto").setClearAutoTier(true));

            var sent = stub.readOneMessage();
            assertEquals("session.model.switchTo", sent.get("method").asText());
            var params = sent.get("params");
            assertTrue(params.has("autoTier"), "clearing must send the property");
            assertTrue(params.get("autoTier").isNull(), "clearing must send an explicit null");
            assertEquals("sess-3", params.get("sessionId").asText());
        }
    }

    @Test
    void setModel_rejects_a_tier_combined_with_clearing() throws Exception {
        try (var sockets = new SocketPair()) {
            var session = new CopilotSession("sess-4", sockets.client());

            var options = new SetModelOptions().setModel("auto").setAutoTier(AutoTier.BALANCE).setClearAutoTier(true);

            assertThrows(IllegalArgumentException.class, () -> session.setModel(options));
        }
    }

    @Test
    void setModel_requires_a_model() throws Exception {
        try (var sockets = new SocketPair()) {
            var session = new CopilotSession("sess-5", sockets.client());

            assertThrows(IllegalArgumentException.class, () -> session.setModel(new SetModelOptions()));
            assertThrows(IllegalArgumentException.class, () -> session.setModel((SetModelOptions) null));
        }
    }

    @Test
    void setAutoTier_sends_the_requested_tier() throws Exception {
        try (var sockets = new SocketPair()) {
            var session = new CopilotSession("sess-6", sockets.client());
            var stub = sockets.stubServer();

            session.setAutoTier(AutoTier.EFFICIENCY);

            var sent = stub.readOneMessage();
            assertEquals("session.model.switchAutoTier", sent.get("method").asText());
            var params = sent.get("params");
            assertEquals("efficiency", params.get("autoTier").asText());
            assertEquals("sess-6", params.get("sessionId").asText());
        }
    }

    @Test
    void setAutoTier_sends_explicit_null_for_provider_default_routing() throws Exception {
        try (var sockets = new SocketPair()) {
            var session = new CopilotSession("sess-7", sockets.client());
            var stub = sockets.stubServer();

            session.setAutoTier(null);

            var sent = stub.readOneMessage();
            assertEquals("session.model.switchAutoTier", sent.get("method").asText());
            var params = sent.get("params");
            assertTrue(params.has("autoTier"), "returning to provider-default routing must send the property");
            assertTrue(params.get("autoTier").isNull(), "returning to provider-default routing must send null");
            assertEquals("sess-7", params.get("sessionId").asText());
        }
    }

    @Test
    void switchAutoTier_result_deserializes_every_field() throws Exception {
        var json = """
                {
                  "status": "pending",
                  "effectiveAutoTier": "balance",
                  "pendingAutoTier": "intelligence",
                  "activatingAutoTier": null,
                  "supersededAutoTier": "efficiency"
                }
                """;

        var result = new ObjectMapper().readValue(json, SessionModelSwitchAutoTierResult.class);

        assertEquals(ModelSwitchAutoTierStatus.PENDING, result.status());
        assertEquals(com.github.copilot.generated.rpc.AutoTier.BALANCE, result.effectiveAutoTier());
        assertEquals(com.github.copilot.generated.rpc.AutoTier.INTELLIGENCE, result.pendingAutoTier());
        assertNull(result.activatingAutoTier());
        assertEquals(com.github.copilot.generated.rpc.AutoTier.EFFICIENCY, result.supersededAutoTier());
    }

    /**
     * Loopback socket pair; the client side backs a real {@link JsonRpcClient} and
     * the server side exposes the raw outbound messages.
     */
    private static final class SocketPair implements AutoCloseable {

        private final Socket clientSocket;
        private final Socket serverSocket;
        private final JsonRpcClient rpcClient;

        SocketPair() throws Exception {
            try (var ss = new ServerSocket(0)) {
                clientSocket = new Socket("localhost", ss.getLocalPort());
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

    /** Reads Content-Length framed JSON-RPC messages from the server socket. */
    private static final class StubServer {

        private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

        private final InputStream in;

        StubServer(Socket socket) {
            try {
                this.in = socket.getInputStream();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        JsonNode readOneMessage() throws Exception {
            var header = new StringBuilder();
            int b;
            while ((b = in.read()) != -1) {
                if (b == '\n' && header.toString().endsWith("\r")) {
                    break;
                }
                header.append((char) b);
            }
            in.read();
            in.read();

            String hdr = header.toString().trim();
            int colon = hdr.indexOf(':');
            int len = Integer.parseInt(hdr.substring(colon + 1).trim());
            byte[] body = in.readNBytes(len);
            return MAPPER.readTree(body);
        }
    }
}
