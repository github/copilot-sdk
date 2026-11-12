/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.generated.McpOauthRequiredEvent;
import com.github.copilot.rpc.CloudSessionOptions;
import com.github.copilot.rpc.CloudSessionRepository;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.McpAuthHandler;
import com.github.copilot.rpc.McpAuthInvocation;
import com.github.copilot.rpc.McpAuthRequest;
import com.github.copilot.rpc.McpAuthResponse;
import com.github.copilot.rpc.McpAuthResult;
import com.github.copilot.rpc.McpAuthToken;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;

class McpAuthInterestRegistrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void legacyRecordsKeepOriginalComponentLists() {
        assertArrayEquals(new String[]{"isCancelled", "token"}, Arrays.stream(McpAuthResult.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName).toArray(String[]::new));
        assertArrayEquals(
                new String[]{"requestId", "serverName", "serverUrl", "reason", "wwwAuthenticateParams",
                        "resourceMetadata", "staticClientConfig"},
                Arrays.stream(McpAuthRequest.class.getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName).toArray(String[]::new));
    }

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
        assertNull(withoutMetadata.runtimeRefreshSupported());
        assertEquals(withoutMetadata.reason(), new McpAuthRequest("oauth-request", "oauth-server",
                "https://example.com/mcp", withoutMetadata.reason(), null, null, null).reason());
    }

    @Test
    void runtimeRefreshResultIsDistinctFromTokenAndCancellation() {
        var handoff = McpAuthResponse.runtimeRefresh();
        assertTrue(handoff.isRuntimeRefresh());
        assertNull(handoff.legacyResult());
        assertFalse(McpAuthResponse.legacy(new McpAuthResult(true, null)).isRuntimeRefresh());
    }

    @Test
    void runtimeRefreshCallbackPreservesTheAdvertisedRequestOnWire() throws Exception {
        var observedRequest = new CompletableFuture<McpAuthRequest>();
        try (var server = new RecordingRuntime(true, true);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(server.url()));
                var session = client
                        .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                .setOnMcpAuthRequest(runtimeRefreshHandler((request, supported) -> {
                                    assertTrue(supported);
                                    observedRequest.complete(request);
                                    return McpAuthResponse.runtimeRefresh();
                                })))
                        .get(5, TimeUnit.SECONDS)) {
            var request = observedRequest.get(5, TimeUnit.SECONDS);
            assertEquals("refresh", request.reason().getValue());
            var response = server.mcpAuthResponse.get(5, TimeUnit.SECONDS);
            assertEquals(session.getSessionId(), response.path("sessionId").asText());
            assertEquals("oauth-request", response.path("requestId").asText());
            assertEquals("runtimeRefresh", response.path("result").path("kind").asText());
            assertEquals(1, response.path("result").size());
        }
    }

    @Test
    void runtimeRefreshWithoutAdvertisedCapabilitySendsCancelled() throws Exception {
        for (Boolean supported : new Boolean[]{null, false}) {
            try (var server = new RecordingRuntime(true, supported);
                    var client = new CopilotClient(new CopilotClientOptions().setCliUrl(server.url()));
                    var session = client
                            .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                    .setOnMcpAuthRequest(runtimeRefreshHandler((request, capability) -> {
                                        assertFalse(capability);
                                        return McpAuthResponse.runtimeRefresh();
                                    })))
                            .get(5, TimeUnit.SECONDS)) {
                var response = server.mcpAuthResponse.get(5, TimeUnit.SECONDS);
                assertEquals("oauth-request", response.path("requestId").asText());
                assertEquals("cancelled", response.path("result").path("kind").asText(),
                        "runtimeRefreshSupported=" + supported);
                assertEquals(1, response.path("result").size());
            }

        }
    }

    @Test
    void tokenAndCancellationHaveTheSamePrecedenceForEveryCapability() throws Exception {
        for (Boolean supported : new Boolean[]{null, false, true}) {
            for (String responseKind : new String[]{"token", "cancelled", "cancelledWithToken", "emptyToken",
                    "nullToken"}) {
                var handlerResult = switch (responseKind) {
                    case "token" -> new McpAuthResult(false, new McpAuthToken("host-token", null, null));
                    case "cancelledWithToken" -> new McpAuthResult(true, new McpAuthToken("host-token", null, null));
                    case "emptyToken" -> new McpAuthResult(false, new McpAuthToken("", null, null));
                    case "nullToken" -> new McpAuthResult(false, new McpAuthToken(null, null, null));
                    default -> McpAuthResult.cancelled();
                };
                String expected = responseKind.equals("token")
                        ? "token"
                        : (responseKind.equals("emptyToken") || responseKind.equals("nullToken"))
                                && Boolean.TRUE.equals(supported) ? "runtimeRefresh" : "cancelled";
                var server = new RecordingRuntime(true, supported);
                try (server;
                        var client = new CopilotClient(new CopilotClientOptions().setCliUrl(server.url()));
                        var session = client
                                .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                        .setOnMcpAuthRequest(runtimeRefreshHandler((request, capability) -> {
                                            assertEquals(Boolean.TRUE.equals(supported), capability);
                                            return new McpAuthResponse(handlerResult, true);
                                        })))
                                .get(5, TimeUnit.SECONDS)) {
                    var response = server.mcpAuthResponse.get(5, TimeUnit.SECONDS);
                    assertEquals(session.getSessionId(), response.path("sessionId").asText());
                    assertEquals("oauth-request", response.path("requestId").asText());
                    var result = response.path("result");
                    assertEquals(expected, result.path("kind").asText(),
                            "runtimeRefreshSupported=" + supported + ", response=" + responseKind);
                    if (expected.equals("token")) {
                        assertEquals("host-token", result.path("accessToken").asText());
                    } else {
                        assertEquals(1, result.size());
                    }
                } catch (java.util.concurrent.TimeoutException error) {
                    throw new AssertionError("runtimeRefreshSupported=" + supported + ", response=" + responseKind
                            + ", requests=" + server.requests(), error);
                }
            }
        }
    }

    private static McpAuthHandler runtimeRefreshHandler(BiFunction<McpAuthRequest, Boolean, McpAuthResponse> callback) {
        return new McpAuthHandler() {
            @Override
            public CompletableFuture<McpAuthResult> handle(McpAuthRequest request, McpAuthInvocation invocation) {
                fail("Opt-in dispatch must run before the legacy handler");
                return CompletableFuture.completedFuture(McpAuthResult.cancelled());
            }

            @Override
            public CompletableFuture<McpAuthResponse> handleWithRuntimeRefresh(McpAuthRequest request,
                    McpAuthInvocation invocation, boolean runtimeRefreshSupported) {
                return CompletableFuture.completedFuture(callback.apply(request, runtimeRefreshSupported));
            }
        };
    }

    @Test
    void legacyHandlerPreservesTokenOnAdvertisedRefresh() throws Exception {
        try (var server = new RecordingRuntime(true, true);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(server.url()));
                var session = client
                        .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                .setOnMcpAuthRequest((request, invocation) -> CompletableFuture.completedFuture(
                                        new McpAuthResult(false, new McpAuthToken("host-token", null, null)))))
                        .get(5, TimeUnit.SECONDS)) {
            var response = server.mcpAuthResponse.get(5, TimeUnit.SECONDS);
            assertEquals(session.getSessionId(), response.path("sessionId").asText());
            assertEquals("oauth-request", response.path("requestId").asText());
            assertEquals("token", response.path("result").path("kind").asText());
            assertEquals("host-token", response.path("result").path("accessToken").asText());
        }
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
        private final boolean emitMcpAuthRequest;
        private final Boolean runtimeRefreshSupported;
        private final CompletableFuture<JsonNode> mcpAuthResponse = new CompletableFuture<>();
        private volatile boolean running = true;

        RecordingRuntime() throws Exception {
            this(false, null);
        }

        RecordingRuntime(boolean emitMcpAuthRequest, Boolean runtimeRefreshSupported) throws Exception {
            this.emitMcpAuthRequest = emitMcpAuthRequest;
            this.runtimeRefreshSupported = runtimeRefreshSupported;
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
                    if ("session.mcp.oauth.handlePendingRequest".equals(method)) {
                        mcpAuthResponse.complete(message.path("params").deepCopy());
                    }
                    if (emitMcpAuthRequest && "session.eventLog.registerInterest".equals(method)
                            && "mcp.oauth_required".equals(message.path("params").path("eventType").asText())) {
                        ObjectNode notification = MAPPER.createObjectNode();
                        notification.put("jsonrpc", "2.0");
                        notification.put("method", "session.event");
                        var params = notification.putObject("params");
                        params.put("sessionId", message.path("params").path("sessionId").asText());
                        var event = params.putObject("event");
                        event.put("type", "mcp.oauth_required");
                        event.put("id", java.util.UUID.randomUUID().toString());
                        event.put("timestamp", "2026-01-01T00:00:00Z");
                        event.putNull("parentId");
                        event.put("ephemeral", true);
                        var data = event.putObject("data");
                        data.put("requestId", "oauth-request");
                        data.put("serverName", "oauth-server");
                        data.put("serverUrl", "https://example.com/mcp");
                        data.put("reason", "refresh");
                        if (runtimeRefreshSupported != null) {
                            data.put("runtimeRefreshSupported", runtimeRefreshSupported);
                        }
                        sendMessage(out, notification);
                    }
                }
            } catch (Exception ex) {
                if (running) {
                    mcpAuthResponse.completeExceptionally(ex);
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
                case "session.mcp.oauth.handlePendingRequest" -> result.put("success", true);
                case "session.skills.reload" -> {
                }
                case "session.detach" -> {
                    result.put("success", true);
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
            sendMessage(out, response);
        }

        private static void sendMessage(OutputStream out, JsonNode message) throws Exception {
            byte[] body = MAPPER.writeValueAsBytes(message);
            out.write(("Content-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(body);
            out.flush();
        }
    }
}
