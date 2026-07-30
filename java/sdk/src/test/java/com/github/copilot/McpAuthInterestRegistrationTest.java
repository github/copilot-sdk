/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.generated.McpOauthRequiredEvent;
import com.github.copilot.rpc.CloudSessionOptions;
import com.github.copilot.rpc.CloudSessionRepository;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.McpAuthResult;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;

class McpAuthInterestRegistrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void mcpOauthRequiredEventExposesOptionalResourceMetadata() throws Exception {
        var data = MAPPER.readValue("""
                {
                  "requestId": "oauth-request",
                  "reason": "initial",
                  "serverName": "oauth-server",
                  "serverUrl": "https://example.com/mcp",
                  "wwwAuthenticateParams": {
                    "resourceMetadataUrl": "https://example.com/.well-known/oauth-protected-resource"
                  },
                  "resourceMetadata": "{\\"resource\\":\\"https://example.com/mcp\\"}",
                  "staticClientConfig": {
                    "clientId": "static-client",
                    "clientSecret": "static-secret",
                    "grantType": "client_credentials",
                    "publicClient": false
                  }
                }
                """, McpOauthRequiredEvent.McpOauthRequiredEventData.class);

        assertEquals("{\"resource\":\"https://example.com/mcp\"}", data.resourceMetadata());
        assertNotNull(data.wwwAuthenticateParams());
        assertNotNull(data.staticClientConfig());
        assertEquals("static-secret", data.staticClientConfig().clientSecret());

        var withoutMetadata = MAPPER.readValue("""
                {
                  "requestId": "oauth-request",
                  "reason": "initial",
                  "serverName": "oauth-server",
                  "serverUrl": "https://example.com/mcp"
                }
                """, McpOauthRequiredEvent.McpOauthRequiredEventData.class);

        assertNull(withoutMetadata.resourceMetadata());
        assertNull(withoutMetadata.wwwAuthenticateParams());
    }

    @Test
    void createSessionRegistersMcpAuthInterestOnlyWhenHandlerConfigured() throws Exception {
        try (var server = new RecordingRuntime();
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(server.url()))) {
            try (var session = client.createSession(
                    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setOnEvent(event -> {
                    })).get()) {
                assertNotNull(session);
            }

            assertNoMcpAuthInterest(server.requests());
            assertTrue(server.requests().stream().anyMatch(request -> "session.create".equals(request.method())
                    && request.params().path("requestPermission").asBoolean()));

            server.clearRequests();

            try (var session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setOnMcpAuthRequest((request, invocation) -> {
                                assertNotNull(request);
                                assertNotNull(invocation);
                                return java.util.concurrent.CompletableFuture
                                        .completedFuture(McpAuthResult.cancelled());
                            }))
                    .get()) {
                assertNotNull(session);
            }

            List<RpcRequest> requests = server.requests();
            assertEquals("session.create", requests.get(0).method());
            assertEquals("session.eventLog.registerInterest", requests.get(1).method());
            assertEquals("mcp.oauth_required", requests.get(1).params().path("eventType").asText());
        }
    }

    @Test
    void cloudCreateSessionRegistersMcpAuthInterestAfterCreateOnlyWhenHandlerConfigured() throws Exception {
        try (var server = new RecordingRuntime();
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(server.url()))) {
            var cloud = new CloudSessionOptions().setRepository(
                    new CloudSessionRepository().setOwner("github").setName("copilot-sdk").setBranch("main"));

            try (var session = client
                    .createSession(
                            new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setCloud(cloud))
                    .get()) {
                assertNotNull(session);
            }

            assertNoMcpAuthInterest(server.requests());
            server.clearRequests();

            try (var session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setCloud(cloud).setOnMcpAuthRequest((request, invocation) -> {
                                assertNotNull(request);
                                assertNotNull(invocation);
                                return java.util.concurrent.CompletableFuture
                                        .completedFuture(McpAuthResult.cancelled());
                            }))
                    .get()) {
                assertNotNull(session);
            }

            List<RpcRequest> requests = server.requests();
            assertEquals("session.create", requests.get(0).method());
            assertEquals("session.eventLog.registerInterest", requests.get(1).method());
            assertEquals("mcp.oauth_required", requests.get(1).params().path("eventType").asText());
        }
    }

    @Test
    void resumeSessionRegistersMcpAuthInterestOnlyWhenHandlerConfigured() throws Exception {
        try (var server = new RecordingRuntime();
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(server.url()))) {
            try (var session = client.resumeSession("session-without-auth", new ResumeSessionConfig()
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setOnEvent(event -> {
                    })).get()) {
                assertNotNull(session);
            }

            assertNoMcpAuthInterest(server.requests());
            assertTrue(server.requests().stream().anyMatch(request -> "session.resume".equals(request.method())
                    && request.params().path("requestPermission").asBoolean()));

            server.clearRequests();

            try (var session = client.resumeSession("session-with-auth",
                    new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setOnMcpAuthRequest((request, invocation) -> {
                                assertNotNull(request);
                                assertNotNull(invocation);
                                return java.util.concurrent.CompletableFuture
                                        .completedFuture(McpAuthResult.cancelled());
                            }))
                    .get()) {
                assertNotNull(session);
            }

            List<RpcRequest> requests = server.requests();
            assertEquals("session.resume", requests.get(0).method());
            assertEquals("session.eventLog.registerInterest", requests.get(1).method());
            assertEquals("mcp.oauth_required", requests.get(1).params().path("eventType").asText());
        }
    }

    private static void assertNoMcpAuthInterest(List<RpcRequest> requests) {
        assertFalse(requests.stream().anyMatch(request -> "session.eventLog.registerInterest".equals(request.method())
                && "mcp.oauth_required".equals(request.params().path("eventType").asText())));
    }

    private record RpcRequest(String method, JsonNode params) {
    }

    private static final class RecordingRuntime implements AutoCloseable {
        private final ServerSocket listener;
        private final Thread thread;
        private final List<RpcRequest> requests = new CopyOnWriteArrayList<>();
        private volatile boolean running = true;

        RecordingRuntime() throws Exception {
            listener = new ServerSocket(0);
            thread = new Thread(this::run, "mcp-auth-interest-test-runtime");
            thread.setDaemon(true);
            thread.start();
        }

        String url() {
            return "127.0.0.1:" + listener.getLocalPort();
        }

        List<RpcRequest> requests() {
            return List.copyOf(requests);
        }

        void clearRequests() {
            requests.clear();
        }

        @Override
        public void close() throws Exception {
            running = false;
            listener.close();
            thread.join(2000);
        }

        private void run() {
            try (Socket socket = listener.accept()) {
                var in = socket.getInputStream();
                var out = socket.getOutputStream();
                while (running) {
                    JsonNode message = readMessage(in);
                    if (message == null) {
                        return;
                    }
                    String method = message.path("method").asText();
                    requests.add(new RpcRequest(method, message.path("params").deepCopy()));
                    sendResponse(out, message.path("id").asLong(), resultFor(method, message.path("params")));
                }
            } catch (Exception ex) {
                if (running) {
                    throw new RuntimeException(ex);
                }
            }
        }

        private static JsonNode resultFor(String method, JsonNode params) {
            ObjectNode result = MAPPER.createObjectNode();
            switch (method) {
                case "connect" -> {
                    result.put("ok", true);
                    result.put("protocolVersion", 3);
                    result.put("version", "test");
                }
                case "session.create", "session.resume" -> {
                    String sessionId = params.path("sessionId").asText("server-assigned-session");
                    if (sessionId.isEmpty()) {
                        sessionId = "server-assigned-session";
                    }
                    result.put("sessionId", sessionId);
                    result.putNull("workspacePath");
                    result.putNull("capabilities");
                }
                case "session.eventLog.registerInterest" -> result.put("id", "interest-1");
                case "session.options.update" -> result.put("success", true);
                case "session.skills.reload", "session.destroy" -> {
                }
                default -> throw new IllegalStateException("Unexpected RPC method " + method);
            }
            return result;
        }

        private static JsonNode readMessage(java.io.InputStream in) throws Exception {
            StringBuilder header = new StringBuilder();
            int b;
            while ((b = in.read()) != -1) {
                header.append((char) b);
                if (header.toString().endsWith("\r\n\r\n")) {
                    break;
                }
            }
            if (b == -1) {
                return null;
            }
            int contentLength = 0;
            for (String line : header.toString().split("\r\n")) {
                int colon = line.indexOf(':');
                if (colon > 0 && "Content-Length".equals(line.substring(0, colon))) {
                    contentLength = Integer.parseInt(line.substring(colon + 1).trim());
                }
            }
            byte[] body = in.readNBytes(contentLength);
            return MAPPER.readTree(body);
        }

        private static void sendResponse(OutputStream out, long id, JsonNode result) throws Exception {
            ObjectNode response = MAPPER.createObjectNode();
            response.put("jsonrpc", "2.0");
            response.put("id", id);
            response.set("result", result);
            byte[] body = MAPPER.writeValueAsBytes(response);
            out.write(("Content-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(body);
            out.flush();
        }
    }
}
