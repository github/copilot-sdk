/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.ModelSwitchAutoTierStatus;
import com.github.copilot.generated.rpc.SessionModelSwitchAutoTierResult;
import com.github.copilot.rpc.AutoTier;
import com.github.copilot.rpc.ElicitationParams;
import com.github.copilot.rpc.ElicitationSchema;
import com.github.copilot.rpc.SessionCapabilities;
import com.github.copilot.rpc.SessionUiCapabilities;
import com.github.copilot.rpc.SetModelOptions;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Verifies the wire payloads produced by Auto routing preference switches.
 * <p>
 * The runtime treats an explicit {@code null} {@code autoTier} (return to
 * provider-default routing) differently from an absent one (leave the
 * preference unchanged), so these tests assert on the raw JSON.
 */
@AllowCopilotExperimental
class SessionAutoTierSwitchTest {

    @Test
    void nullRequiredValuesPreserveOmittedWireFields() throws Exception {
        try (var sockets = new SocketPair()) {
            var session = new CopilotSession("sess-validation", sockets.client());
            session.setCapabilities(new SessionCapabilities().setUi(new SessionUiCapabilities().setElicitation(true)));
            var stub = sockets.stubServer();

            assertDoesNotThrow(() -> session.getUi().elicitation(
                    new ElicitationParams().setRequestedSchema(new ElicitationSchema().setProperties(Map.of()))));
            assertFalse(stub.readOneMessage().path("params").has("message"));

            assertDoesNotThrow(() -> session.getUi().elicitation(
                    new ElicitationParams().setMessage("Choose").setRequestedSchema(new ElicitationSchema())));
            assertFalse(stub.readOneMessage().path("params").path("requestedSchema").has("properties"));

            assertDoesNotThrow(() -> session.getUi().confirm(null));
            assertFalse(stub.readOneMessage().path("params").has("message"));

            assertDoesNotThrow(() -> session.getUi().select(null, new String[]{"one"}));
            assertFalse(stub.readOneMessage().path("params").has("message"));

            assertDoesNotThrow(() -> session.getUi().input(null, null));
            assertFalse(stub.readOneMessage().path("params").has("message"));

            assertDoesNotThrow(() -> session.log(null, null, null, null));
            assertFalse(stub.readOneMessage().path("params").has("message"));
        }
    }

    @Test
    void customTierRoundTripsAcrossConvenienceAndGeneratedApis() throws Exception {
        ObjectMapper mapper = JsonRpcClient.getObjectMapper();
        AutoTier tier = AutoTier.fromValue("premium-v2");
        assertEquals("\"premium-v2\"", mapper.writeValueAsString(tier));
        assertEquals(tier, mapper.readValue("\"premium-v2\"", AutoTier.class));
        var generated = mapper.readValue("\"premium-v2\"", com.github.copilot.generated.rpc.AutoTier.class);
        assertEquals("premium-v2", generated.getValue());
        assertEquals("\"premium-v2\"", mapper.writeValueAsString(generated));
        assertEquals(AutoTier.BALANCE, AutoTier.fromValue("balance"));
    }

    @Test
    void legacy_model_switch_preserves_reasoning_and_omits_provider() throws Exception {
        try (var sockets = new SocketPair()) {
            var session = new CopilotSession("sess-legacy", sockets.client());
            session.setModel("gpt-5", "high");
            var params = sockets.stubServer().readOneMessage().get("params");
            assertEquals("gpt-5", params.get("modelId").asText());
            assertEquals("high", params.get("reasoningEffort").asText());
            assertFalse(params.has("providerId"));
            assertFalse(params.has("autoTier"));
        }
    }

    @Test
    void model_switch_with_summary_preserves_its_wire_fields() throws Exception {
        try (var sockets = new SocketPair()) {
            var session = new CopilotSession("sess-summary", sockets.client());
            session.setModel("gpt-5", "high", "concise", null);
            var params = sockets.stubServer().readOneMessage().get("params");
            assertEquals("gpt-5", params.get("modelId").asText());
            assertEquals("high", params.get("reasoningEffort").asText());
            assertEquals("concise", params.get("reasoningSummary").asText());
            assertFalse(params.has("providerId"));
            assertFalse(params.has("autoTier"));
        }
    }

    @Test
    void positional_model_switch_overloads_accept_null_model_and_omit_it() throws Exception {
        try (var sockets = new SocketPair()) {
            var session = new CopilotSession("sess-null-model", sockets.client());
            var stub = sockets.stubServer();

            session.setModel((String) null);
            assertNullModelWasOmitted(stub.readOneMessage().get("params"));
            session.setModel((String) null, (String) null);
            assertNullModelWasOmitted(stub.readOneMessage().get("params"));
            session.setModel((String) null, (String) null, (com.github.copilot.rpc.ModelCapabilitiesOverride) null);
            assertNullModelWasOmitted(stub.readOneMessage().get("params"));
            session.setModel((String) null, (String) null, (String) null,
                    (com.github.copilot.rpc.ModelCapabilitiesOverride) null);
            assertNullModelWasOmitted(stub.readOneMessage().get("params"));
        }
    }

    @Test
    void setModel_preserves_open_auto_tiers_and_capability_values() throws Exception {
        try (var sockets = new SocketPair()) {
            var session = new CopilotSession("sess-capabilities", sockets.client());
            var stub = sockets.stubServer();
            var capabilities = new com.github.copilot.rpc.ModelCapabilitiesOverride()
                    .setSupports(new com.github.copilot.rpc.ModelCapabilitiesOverride.Supports().setVision(false)
                            .setReasoningEffort(true))
                    .setLimits(new com.github.copilot.rpc.ModelCapabilitiesOverride.Limits().setMaxPromptTokens(0));

            session.setModel(new SetModelOptions().setModel("auto")
                    .setAutoTier(AutoTier.fromValue("provider-future-tier")).setReasoningEffort("xhigh")
                    .setReasoningSummary("detailed").setModelCapabilities(capabilities));

            var params = stub.readOneMessage().get("params");
            assertEquals("sess-capabilities", params.path("sessionId").asText());
            assertEquals("provider-future-tier", params.path("autoTier").asText());
            assertEquals("xhigh", params.path("reasoningEffort").asText());
            assertEquals("detailed", params.path("reasoningSummary").asText());
            assertFalse(params.path("modelCapabilities").path("supports").path("vision").asBoolean());
            assertTrue(params.path("modelCapabilities").path("supports").path("reasoningEffort").asBoolean());
            assertEquals(0, params.path("modelCapabilities").path("limits").path("max_prompt_tokens").asInt());
        }
    }

    @Test
    void setModel_omits_autoTier_when_no_preference_is_requested() throws Exception {
        try (var sockets = new SocketPair()) {
            var session = new CopilotSession("sess-1", sockets.client());
            var stub = sockets.stubServer();

            session.setModel(new SetModelOptions().setModel("auto"));

            var params = stub.readOneMessage().get("params");
            assertEquals("auto", params.get("modelId").asText());
            assertFalse(params.has("autoTier"), "an unset preference must not appear on the wire");

            session.setModel(new SetModelOptions().setModel("auto").setAutoTier(null));
            assertFalse(stub.readOneMessage().path("params").has("autoTier"),
                    "a null convenience option must leave the preference unchanged");
        }
    }

    @Test
    void setModel_omits_reasoningEffort_when_unset() throws Exception {
        try (var sockets = new SocketPair()) {
            var session = new CopilotSession("sess-unset-effort", sockets.client());

            session.setModel(new SetModelOptions().setModel("auto").setAutoTier(AutoTier.FAST));

            var params = sockets.stubServer().readOneMessage().get("params");
            assertEquals("fast", params.path("autoTier").asText());
            assertFalse(params.has("reasoningEffort"), "an unset effort must not reset the current override");
        }
    }

    @Test
    void setModel_sends_explicit_null_reasoningEffort_when_resetting() throws Exception {
        try (var sockets = new SocketPair()) {
            var session = new CopilotSession("sess-reset-effort", sockets.client());
            var options = new SetModelOptions().setModel("auto").setReasoningEffort(null);

            session.setModel(options);
            var reset = sockets.stubServer().readOneMessage().get("params");
            assertTrue(reset.has("reasoningEffort"), "an explicit reset must be present: " + reset);
            assertTrue(reset.get("reasoningEffort").isNull(), "an explicit reset must send JSON null: " + reset);

            options.setReasoningEffort("high");
            session.setModel(options);
            assertEquals("high", sockets.stubServer().readOneMessage().path("params").path("reasoningEffort").asText());

            options.setReasoningEffort(null);
            session.setModel(options);
            var reusedReset = sockets.stubServer().readOneMessage().get("params");
            assertTrue(reusedReset.has("reasoningEffort"),
                    "a reused option must preserve an explicit reset: " + reusedReset);
            assertTrue(reusedReset.get("reasoningEffort").isNull());
        }
    }

    @Test
    void setModel_sends_requested_autoTier() throws Exception {
        try (var sockets = new SocketPair()) {
            var session = new CopilotSession("sess-2", sockets.client());
            var stub = sockets.stubServer();

            session.setModel(
                    new SetModelOptions().setModel("auto").setAutoTier(AutoTier.FAST).setReasoningEffort("high"));

            var sent = stub.readOneMessage();
            assertEquals("session.model.switchTo", sent.get("method").asText());
            var params = sent.get("params");
            assertEquals("fast", params.get("autoTier").asText());
            assertEquals("high", params.get("reasoningEffort").asText());
            assertEquals("sess-2", params.get("sessionId").asText());
        }
    }

    @Test
    void setModel_sends_explicit_null_autoTier_when_clearing() throws Exception {
        try (var sockets = new SocketPair()) {
            var session = new CopilotSession("sess-3", sockets.client());
            var stub = sockets.stubServer();

            session.setModel(new SetModelOptions().setModel("auto").setResetAutoTier(true));

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

            var options = new SetModelOptions().setModel("auto").setAutoTier(AutoTier.BALANCE).setResetAutoTier(true);

            assertThrows(IllegalArgumentException.class, () -> session.setModel(options));
        }
    }

    @Test
    void setModel_requires_a_model() throws Exception {
        try (var sockets = new SocketPair()) {
            var session = new CopilotSession("sess-5", sockets.client());

            assertThrows(IllegalArgumentException.class, () -> session.setModel(new SetModelOptions()));
            assertThrows(IllegalArgumentException.class, () -> session.setModel((SetModelOptions) null));
            assertEquals(0, sockets.serverSocket.getInputStream().available(), "Invalid options must not send an RPC");
        }
    }

    private static void assertNullModelWasOmitted(JsonNode params) {
        assertEquals("sess-null-model", params.path("sessionId").asText());
        assertFalse(params.has("modelId"));
    }

    @Test
    void setAutoTier_sends_the_requested_tier() throws Exception {
        try (var sockets = new SocketPair()) {
            var session = new CopilotSession("sess-6", sockets.client());
            var stub = sockets.stubServer();

            session.setAutoTier(AutoTier.FAST);

            var sent = stub.readOneMessage();
            assertEquals("session.model.switchAutoTier", sent.get("method").asText());
            var params = sent.get("params");
            assertEquals("fast", params.get("autoTier").asText());
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
                  "pendingAutoTier": "fast",
                  "activatingAutoTier": null,
                  "supersededAutoTier": "efficiency"
                }
                """;

        var result = new ObjectMapper().readValue(json, SessionModelSwitchAutoTierResult.class);

        assertEquals(ModelSwitchAutoTierStatus.PENDING, result.status());
        assertEquals(com.github.copilot.generated.rpc.AutoTier.BALANCE, result.effectiveAutoTier());
        assertEquals(com.github.copilot.generated.rpc.AutoTier.FAST, result.pendingAutoTier());
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
            var loopback = InetAddress.getLoopbackAddress();
            try (var ss = new ServerSocket(0, 1, loopback)) {
                clientSocket = new Socket(loopback, ss.getLocalPort());
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
