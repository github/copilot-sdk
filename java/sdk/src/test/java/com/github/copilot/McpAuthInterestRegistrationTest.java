/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
import com.github.copilot.rpc.PermissionRequestResult;
import com.github.copilot.rpc.PermissionRequestResultKind;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;

class McpAuthInterestRegistrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void recordingRuntimeReservesItsClientAddress() throws Exception {
        try (var server = new RecordingRuntime(); var competing = new ServerSocket()) {
            competing.setReuseAddress(true);
            assertThrows(BindException.class,
                    () -> competing.bind(new InetSocketAddress("127.0.0.1", server.listener.getLocalPort())));
        }
    }

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
    void failedInterestRegistrationDoesNotReplaceResidentSession() throws Exception {
        try (var server = new RecordingRuntime(false, null, "resident-session", true);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(server.url()))) {
            var residentRequest = new CompletableFuture<McpAuthRequest>();
            var resumeRequest = new CompletableFuture<McpAuthRequest>();
            client.createSession(new SessionConfig().setSessionId("resident-session")
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                    .setOnMcpAuthRequest(runtimeRefreshHandler((request, supported) -> {
                        residentRequest.complete(request);
                        return McpAuthResponse.legacy(McpAuthResult.cancelled());
                    }))).get(5, TimeUnit.SECONDS);

            var resumeConfig = new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                    .setOnMcpAuthRequest(runtimeRefreshHandler((request, supported) -> {
                        resumeRequest.complete(request);
                        return McpAuthResponse.legacy(McpAuthResult.cancelled());
                    }));
            var failure = assertThrows(ExecutionException.class,
                    () -> client.resumeSession("requested-session", resumeConfig).get(5, TimeUnit.SECONDS));
            assertTrue(failure.getCause().getMessage().contains("interest registration failed"));
            server.failedInterestProcessed.get(5, TimeUnit.SECONDS);
            assertEquals(0, server.mcpAuthEvents.get(),
                    "The fixture must wait for resume rollback before publishing an OAuth request");
            server.sendMcpAuthEvent(server.output, "resident-session");

            assertEquals("oauth-request", residentRequest.get(5, TimeUnit.SECONDS).requestId());
            assertFalse(resumeRequest.isDone(), "failed resume must not take over the resident session");
            var response = server.mcpAuthResponse.get(5, TimeUnit.SECONDS);
            assertEquals("resident-session", response.path("sessionId").asText());
            assertEquals("cancelled", response.path("result").path("kind").asText());
        }
    }

    @Test
    void resumeRoutesCanonicalMcpAuthBeforeInterestAcknowledgement() throws Exception {
        String canonicalId = "abcdef01-2345-4567-89ab-cdef01234567";
        for (boolean hasResident : List.of(false, true)) {
            var residentCalls = new AtomicInteger();
            var resumedCalls = new AtomicInteger();
            try (var server = new RecordingRuntime(true, null, canonicalId, false, true);
                    var client = new CopilotClient(new CopilotClientOptions().setCliUrl(server.url()));
                    var resident = hasResident
                            ? client.createSession(new SessionConfig().setSessionId(canonicalId)
                                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                    .setOnMcpAuthRequest((request, invocation) -> {
                                        residentCalls.incrementAndGet();
                                        return CompletableFuture.completedFuture(new McpAuthResult(false,
                                                new McpAuthToken("resident-token", null, null)));
                                    })).get(5, TimeUnit.SECONDS)
                            : null) {
                var resuming = client.resumeSession("abcdef01",
                        new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                .setOnMcpAuthRequest((request, invocation) -> {
                                    resumedCalls.incrementAndGet();
                                    return CompletableFuture.completedFuture(
                                            new McpAuthResult(false, new McpAuthToken("resumed-token", null, null)));
                                }));
                CopilotSession resumed;
                try {
                    resumed = resuming.get(5, TimeUnit.SECONDS);
                } catch (java.util.concurrent.TimeoutException error) {
                    throw new AssertionError(
                            "Resume did not answer OAuth before interest acknowledgement: " + server.requests(), error);
                }
                try (resumed) {
                    assertEquals(canonicalId, resumed.getSessionId());
                    var response = server.mcpAuthResponse.get(5, TimeUnit.SECONDS);
                    assertEquals(canonicalId, response.path("sessionId").asText());
                    assertEquals("resumed-token", response.path("result").path("accessToken").asText());
                    assertEquals(1, resumedCalls.get());
                    assertEquals(0, residentCalls.get());
                }
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"false, false, false", "false, false, true", "false, true, false", "false, true, true",
            "true, false, false", "true, false, true", "true, true, false", "true, true, true"})
    void failedConcurrentResumesNeverRestoreFailedCallbacks(boolean hasResident, boolean firstFailsFirst,
            boolean usePrefixes) throws Exception {
        String canonicalId = "abcdef01-2345-4567-89ab-cdef01234567";
        var failedHandlerCalls = new AtomicInteger();
        var residentHandlerCalls = new AtomicInteger();
        PermissionHandler failedHandler = (request, invocation) -> {
            failedHandlerCalls.incrementAndGet();
            return CompletableFuture.completedFuture(PermissionRequestResult.approveOnce());
        };
        try (var server = new RecordingRuntime(false, null, canonicalId, false);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(server.url()));
                var resident = hasResident
                        ? client.createSession(new SessionConfig().setSessionId(canonicalId)
                                .setOnPermissionRequest((request, invocation) -> {
                                    residentHandlerCalls.incrementAndGet();
                                    return CompletableFuture.completedFuture(new PermissionRequestResult()
                                            .setKind(PermissionRequestResultKind.REJECTED));
                                })).get(5, TimeUnit.SECONDS)
                        : null) {
            server.holdResumeInterests = true;
            var config = new ResumeSessionConfig().setOnPermissionRequest(failedHandler).setOnMcpAuthRequest(
                    (request, invocation) -> CompletableFuture.completedFuture(McpAuthResult.cancelled()));
            var first = client.resumeSession(usePrefixes ? "abcdef01" : canonicalId, config);
            var firstInterest = server.awaitResumeInterest();
            var second = client.resumeSession(usePrefixes ? "abcdef01-2345" : canonicalId, config);
            var secondInterest = server.awaitResumeInterest();
            assertFalse(first.isDone());
            assertFalse(second.isDone());

            server.failInterest(firstFailsFirst ? firstInterest : secondInterest);
            var earlierFailure = assertThrows(ExecutionException.class,
                    () -> (firstFailsFirst ? first : second).get(5, TimeUnit.SECONDS));
            assertTrue(earlierFailure.getCause().getMessage().contains("interest registration failed"));
            server.failInterest(firstFailsFirst ? secondInterest : firstInterest);
            var laterFailure = assertThrows(ExecutionException.class,
                    () -> (firstFailsFirst ? second : first).get(5, TimeUnit.SECONDS));
            assertTrue(laterFailure.getCause().getMessage().contains("interest registration failed"));

            var permission = server.requestPermission(canonicalId).get(5, TimeUnit.SECONDS);
            assertEquals(
                    hasResident
                            ? PermissionRequestResultKind.REJECTED.getValue()
                            : PermissionRequestResultKind.DENIED_COULD_NOT_REQUEST_FROM_USER.getValue(),
                    permission.at("/result/result/kind").asText());
            assertEquals(0, failedHandlerCalls.get(), "a failed resume must not answer permissions");
            assertEquals(hasResident ? 1 : 0, residentHandlerCalls.get());
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
        private final String returnedResumeId;
        private final boolean failInterestRegistration;
        private final boolean emitBeforeInterestResponse;
        private final AtomicInteger interestRegistrationCount = new AtomicInteger();
        private final AtomicInteger mcpAuthEvents = new AtomicInteger();
        private final CompletableFuture<Void> failedInterestProcessed = new CompletableFuture<>();
        private final CompletableFuture<JsonNode> mcpAuthResponse = new CompletableFuture<>();
        private final LinkedBlockingQueue<JsonNode> heldInterests = new LinkedBlockingQueue<>();
        private final ConcurrentHashMap<Long, CompletableFuture<JsonNode>> permissionResponses = new ConcurrentHashMap<>();
        private final AtomicInteger nextCallbackId = new AtomicInteger(10000);
        private volatile boolean holdResumeInterests;
        private volatile OutputStream output;
        private volatile boolean running = true;

        RecordingRuntime() throws Exception {
            this(false, null, null, false);
        }

        RecordingRuntime(boolean emitMcpAuthRequest, Boolean runtimeRefreshSupported) throws Exception {
            this(emitMcpAuthRequest, runtimeRefreshSupported, null, false);
        }

        RecordingRuntime(boolean emitMcpAuthRequest, Boolean runtimeRefreshSupported, String returnedResumeId,
                boolean failInterestRegistration) throws Exception {
            this(emitMcpAuthRequest, runtimeRefreshSupported, returnedResumeId, failInterestRegistration, false);
        }

        RecordingRuntime(boolean emitMcpAuthRequest, Boolean runtimeRefreshSupported, String returnedResumeId,
                boolean failInterestRegistration, boolean emitBeforeInterestResponse) throws Exception {
            this.emitMcpAuthRequest = emitMcpAuthRequest;
            this.runtimeRefreshSupported = runtimeRefreshSupported;
            this.returnedResumeId = returnedResumeId;
            this.failInterestRegistration = failInterestRegistration;
            this.emitBeforeInterestResponse = emitBeforeInterestResponse;
            listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
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

        JsonNode awaitResumeInterest() throws Exception {
            var request = heldInterests.poll(5, TimeUnit.SECONDS);
            assertNotNull(request, "resume did not register OAuth interest: " + requests());
            return request;
        }

        void failInterest(JsonNode request) throws Exception {
            sendError(output, request.path("id").asLong());
        }

        CompletableFuture<JsonNode> requestPermission(String sessionId) throws Exception {
            long id = nextCallbackId.incrementAndGet();
            var response = new CompletableFuture<JsonNode>();
            permissionResponses.put(id, response);
            var request = MAPPER.createObjectNode().put("jsonrpc", "2.0").put("id", id).put("method",
                    "permission.request");
            var params = request.putObject("params");
            params.put("sessionId", sessionId);
            params.putObject("permissionRequest").put("kind", "shell").put("fullCommandText", "echo checked");
            sendMessage(output, request);
            return response;
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
                output = out;
                boolean resumed = false;
                JsonNode pendingInterest = null;
                while (running) {
                    JsonNode message = readMessage(in);
                    if (message == null) {
                        return;
                    }
                    if (!message.has("method")) {
                        var response = permissionResponses.remove(message.path("id").asLong());
                        if (response == null) {
                            throw new IllegalStateException("Unexpected callback response: " + message);
                        }
                        response.complete(message);
                        continue;
                    }
                    String method = message.path("method").asText();
                    requests.add(new RpcRequest(method, message.path("params").deepCopy()));
                    if ("session.resume".equals(method)) {
                        resumed = true;
                    }
                    boolean mcpAuthInterest = "session.eventLog.registerInterest".equals(method)
                            && "mcp.oauth_required".equals(message.path("params").path("eventType").asText());
                    // Runtime events use canonical IDs even when interest uses a prefix.
                    String eventSessionId = resumed && returnedResumeId != null
                            ? returnedResumeId
                            : message.path("params").path("sessionId").asText();
                    if (holdResumeInterests && resumed && mcpAuthInterest) {
                        heldInterests.add(message);
                        continue;
                    }
                    if (emitBeforeInterestResponse && resumed && mcpAuthInterest) {
                        if (pendingInterest != null) {
                            throw new IllegalStateException("Only one interest registration may wait for OAuth");
                        }
                        pendingInterest = message;
                        sendMcpAuthEvent(out, eventSessionId);
                        continue;
                    }
                    boolean failedInterestRegistration = failInterestRegistration
                            && "session.eventLog.registerInterest".equals(method)
                            && interestRegistrationCount.incrementAndGet() > 1;
                    if (failedInterestRegistration) {
                        sendError(out, message.path("id").asLong());
                    } else {
                        sendResponse(out, message.path("id").asLong(), resultFor(method, message.path("params")));
                    }
                    if ("session.mcp.oauth.handlePendingRequest".equals(method)) {
                        mcpAuthResponse.complete(message.path("params").deepCopy());
                        if (pendingInterest != null) {
                            sendResponse(out, pendingInterest.path("id").asLong(),
                                    resultFor("session.eventLog.registerInterest", pendingInterest.path("params")));
                            pendingInterest = null;
                        }
                    }
                    if (emitMcpAuthRequest && mcpAuthInterest && !emitBeforeInterestResponse) {
                        sendMcpAuthEvent(out, eventSessionId);
                    }
                    if (failedInterestRegistration) {
                        failedInterestProcessed.complete(null);
                    }
                }
            } catch (Exception ex) {
                if (running) {
                    mcpAuthResponse.completeExceptionally(ex);
                    permissionResponses.values().forEach(response -> response.completeExceptionally(ex));
                }
            }
        }

        private void sendMcpAuthEvent(OutputStream out, String sessionId) throws Exception {
            mcpAuthEvents.incrementAndGet();
            ObjectNode notification = MAPPER.createObjectNode();
            notification.put("jsonrpc", "2.0");
            notification.put("method", "session.event");
            var params = notification.putObject("params");
            params.put("sessionId", sessionId);
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

        private JsonNode resultFor(String method, JsonNode params) {
            ObjectNode result = MAPPER.createObjectNode();
            switch (method) {
                case "connect" -> {
                    result.put("ok", true);
                    result.put("protocolVersion", 3);
                    result.put("version", "test");
                }
                case "sessions.findByPrefix" -> {
                    if (returnedResumeId != null && returnedResumeId.toLowerCase(java.util.Locale.ROOT)
                            .startsWith(params.path("prefix").asText().toLowerCase(java.util.Locale.ROOT))) {
                        result.put("sessionId", returnedResumeId);
                    }
                }
                case "session.create", "session.resume" -> {
                    String sessionId = params.path("sessionId").asText("server-assigned-session");
                    if (sessionId.isEmpty()) {
                        sessionId = "server-assigned-session";
                    }
                    if ("session.resume".equals(method) && returnedResumeId != null) {
                        sessionId = returnedResumeId;
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

        private static void sendError(OutputStream out, long id) throws Exception {
            ObjectNode response = MAPPER.createObjectNode();
            response.put("jsonrpc", "2.0");
            response.put("id", id);
            response.putObject("error").put("code", -32603).put("message", "interest registration failed");
            sendMessage(out, response);
        }

        private static void sendMessage(OutputStream out, JsonNode message) throws Exception {
            byte[] body = MAPPER.writeValueAsBytes(message);
            synchronized (out) {
                out.write(("Content-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                out.write(body);
                out.flush();
            }
        }
    }
}
