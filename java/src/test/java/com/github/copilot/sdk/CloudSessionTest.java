/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.sdk.json.CloudSessionOptions;
import com.github.copilot.sdk.json.CloudSessionRepository;
import com.github.copilot.sdk.json.CreateSessionRequest;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.ProviderConfig;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SessionLifecycleEvent;
import com.github.copilot.sdk.json.ToolDefinition;
import com.github.copilot.sdk.json.ToolResultObject;

/**
 * Tests for {@link CopilotClient#createCloudSession} and
 * {@link CopilotClient#createSession} cloud-config rejection.
 *
 * <p>
 * Covers:
 * <ol>
 * <li>{@code createSession} rejects cloud config</li>
 * <li>Wire payload omits {@code sessionId} and includes {@code cloud}</li>
 * <li>{@code createCloudSession} rejects caller-provided {@code sessionId}</li>
 * <li>{@code createCloudSession} rejects caller-provided {@code provider}</li>
 * <li>{@code createCloudSession} requires {@code cloud} to be set</li>
 * <li>Early {@code session.event} notifications are buffered and replayed</li>
 * <li>Inbound RPC requests are parked until the session is registered</li>
 * </ol>
 */
class CloudSessionTest {

    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();
    private static final int SOCKET_TIMEOUT_MS = 5000;

    // Socket-pair fields used by routing-related tests
    private Socket clientSideSocket;
    private Socket serverSideSocket;
    private JsonRpcClient rpc;
    private Map<String, CopilotSession> sessions;
    private CopyOnWriteArrayList<SessionLifecycleEvent> lifecycleEvents;
    private PendingRoutingState pendingState;
    private RpcHandlerDispatcher dispatcher;
    private InputStream responseStream;
    private Map<String, BiConsumer<String, JsonNode>> handlers;

    @BeforeEach
    void setup() throws Exception {
        try (ServerSocket ss = new ServerSocket(0)) {
            clientSideSocket = new Socket("localhost", ss.getLocalPort());
            serverSideSocket = ss.accept();
        }
        serverSideSocket.setSoTimeout(SOCKET_TIMEOUT_MS);

        rpc = JsonRpcClient.fromSocket(clientSideSocket);
        responseStream = serverSideSocket.getInputStream();

        sessions = new ConcurrentHashMap<>();
        lifecycleEvents = new CopyOnWriteArrayList<>();
        pendingState = new PendingRoutingState();

        dispatcher = new RpcHandlerDispatcher(sessions, lifecycleEvents::add, null, pendingState);
        dispatcher.registerHandlers(rpc);

        // Extract registered handlers via reflection (same pattern as
        // RpcHandlerDispatcherTest)
        Field f = JsonRpcClient.class.getDeclaredField("notificationHandlers");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, BiConsumer<String, JsonNode>> h = (Map<String, BiConsumer<String, JsonNode>>) f.get(rpc);
        handlers = h;
    }

    @AfterEach
    void teardown() throws Exception {
        if (rpc != null) {
            rpc.close();
        }
        if (serverSideSocket != null) {
            serverSideSocket.close();
        }
        if (clientSideSocket != null) {
            clientSideSocket.close();
        }
    }

    private void invokeHandler(String method, String requestId, JsonNode params) {
        handlers.get(method).accept(requestId, params);
    }

    private JsonNode readResponse() throws Exception {
        StringBuilder header = new StringBuilder();
        while (!header.toString().endsWith("\r\n\r\n")) {
            int b = responseStream.read();
            if (b == -1) {
                throw new java.io.IOException("Unexpected end of stream");
            }
            header.append((char) b);
        }
        String headerStr = header.toString().trim();
        int idx = headerStr.indexOf(':');
        int contentLength = Integer.parseInt(headerStr.substring(idx + 1).trim());
        byte[] body = responseStream.readNBytes(contentLength);
        return MAPPER.readTree(body);
    }

    // =========================================================================
    // Test 1: createSession rejects cloud config
    // =========================================================================

    @Test
    void createSession_rejectsCloudConfig() {
        var client = new CopilotClient();
        var config = new SessionConfig().setCloud(new CloudSessionOptions())
                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL);

        var future = client.createSession(config);

        var ex = assertThrows(ExecutionException.class, future::get, "createSession should fail with cloud config set");
        assertInstanceOf(IllegalArgumentException.class, ex.getCause(), "Cause should be IllegalArgumentException");
        assertTrue(ex.getCause().getMessage().contains("cloud"), "Error message should mention 'cloud'");

        try {
            client.forceStop().get(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    // =========================================================================
    // Test 2: wire payload omits sessionId and includes cloud
    // =========================================================================

    @Test
    void buildCloudCreateRequest_omitsSessionIdAndIncludesCloud() throws Exception {
        var cloud = new CloudSessionOptions()
                .setRepository(new CloudSessionRepository().setOwner("github").setName("copilot-sdk"));
        var config = new SessionConfig().setCloud(cloud).setModel("gpt-5")
                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL);

        CreateSessionRequest request = SessionRequestBuilder.buildCloudCreateRequest(config);

        // Java-level assertions
        assertNull(request.getSessionId(), "sessionId must be null on the cloud create request");
        assertNotNull(request.getCloud(), "cloud must be set on the cloud create request");
        assertEquals("gpt-5", request.getModel());

        // Serialize to JSON and verify wire shape
        String json = MAPPER.writeValueAsString(request);
        JsonNode tree = MAPPER.readTree(json);

        assertFalse(tree.has("sessionId"), "sessionId must be absent from serialized JSON (NON_NULL omits it)");
        assertTrue(tree.has("cloud"), "cloud must be present in serialized JSON");
        assertTrue(tree.get("cloud").has("repository"), "cloud.repository must be present");
        assertEquals("github", tree.get("cloud").get("repository").get("owner").asText());
    }

    @Test
    void buildCloudCreateRequest_sessionIdOmittedEvenWhenModelIsNull() throws Exception {
        // Minimal config: only cloud set, no model
        var config = new SessionConfig().setCloud(new CloudSessionOptions());

        CreateSessionRequest request = SessionRequestBuilder.buildCloudCreateRequest(config);

        assertNull(request.getSessionId());
        String json = MAPPER.writeValueAsString(request);
        JsonNode tree = MAPPER.readTree(json);
        assertFalse(tree.has("sessionId"), "sessionId must never appear in cloud create wire payload");
        assertTrue(tree.has("cloud"));
    }

    // =========================================================================
    // Test 3: createCloudSession rejects caller-provided sessionId
    // =========================================================================

    @Test
    void createCloudSession_rejectsCallerSessionId() {
        var client = new CopilotClient();
        var config = new SessionConfig().setCloud(new CloudSessionOptions()).setSessionId("my-caller-session")
                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL);

        var future = client.createCloudSession(config);

        var ex = assertThrows(ExecutionException.class, future::get,
                "createCloudSession should fail when sessionId is set");
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("sessionId"), "Error message should mention 'sessionId'");

        try {
            client.forceStop().get(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    // =========================================================================
    // Test 4: createCloudSession rejects caller-provided provider
    // =========================================================================

    @Test
    void createCloudSession_rejectsCallerProvider() {
        var client = new CopilotClient();
        var config = new SessionConfig().setCloud(new CloudSessionOptions())
                .setProvider(new ProviderConfig().setType("openai"))
                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL);

        var future = client.createCloudSession(config);

        var ex = assertThrows(ExecutionException.class, future::get,
                "createCloudSession should fail when provider is set");
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("provider"), "Error message should mention 'provider'");

        try {
            client.forceStop().get(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    // =========================================================================
    // Test 5: createCloudSession requires cloud to be set
    // =========================================================================

    @Test
    void createCloudSession_requiresCloud() {
        var client = new CopilotClient();
        var config = new SessionConfig().setModel("gpt-5").setOnPermissionRequest(PermissionHandler.APPROVE_ALL);
        // cloud is NOT set

        var future = client.createCloudSession(config);

        var ex = assertThrows(ExecutionException.class, future::get,
                "createCloudSession should fail when cloud is not set");
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("cloud"), "Error message should mention 'cloud'");

        try {
            client.forceStop().get(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    // =========================================================================
    // Test 6: early session.event notifications are buffered and replayed
    // =========================================================================

    @Test
    void bufferEarlySessionEventNotifications() throws Exception {
        // Enter pending routing mode (simulates createCloudSession in-flight)
        pendingState.incrementGuard();

        String pendingSessionId = "cloud-session-abc";

        // Dispatch a session.event while no session is registered yet.
        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", pendingSessionId);
        ObjectNode event = params.putObject("event");
        event.put("type", "sessionStart");
        ObjectNode data = event.putObject("data");
        data.put("sessionId", pendingSessionId);

        invokeHandler("session.event", null, params);

        // Give the (synchronous) handler a moment — no session registered yet, so the
        // event should be buffered, not dispatched.
        Thread.sleep(50);

        // Create the session object and register it via registerAndFlush, which
        // atomically inserts the session into the map and drains the buffer.
        var session = new CopilotSession(pendingSessionId, rpc);
        var dispatched = new CopyOnWriteArrayList<>();
        session.on(dispatched::add);

        var flush = pendingState.registerAndFlush(pendingSessionId, session, sessions);

        // Replay buffered events into the session (simulates what createCloudSession
        // does)
        for (var buffered : flush.events()) {
            session.dispatchEvent(buffered);
        }

        // Complete parked waiters (none in this test)
        for (var waiter : flush.waiters()) {
            waiter.complete(session);
        }

        // Release the guard
        pendingState.decrementGuard();

        // The buffered session.event should now have been replayed to the session
        Thread.sleep(50);
        assertEquals(1, dispatched.size(), "Buffered notification should have been replayed to the session");
    }

    @Test
    void bufferRespectsSizeLimit() throws Exception {
        pendingState.incrementGuard();
        String sid = "cloud-overflow-test";

        // Send more than BUFFER_LIMIT events
        int overLimit = PendingRoutingState.BUFFER_LIMIT + 10;
        for (int i = 0; i < overLimit; i++) {
            ObjectNode params = MAPPER.createObjectNode();
            params.put("sessionId", sid);
            ObjectNode event = params.putObject("event");
            event.put("type", "assistantMessage");
            event.putObject("data").put("content", "msg-" + i);
            invokeHandler("session.event", null, params);
        }

        var session = new CopilotSession(sid, rpc);
        var flush = pendingState.registerAndFlush(sid, session, sessions);

        // Should have been capped at BUFFER_LIMIT; oldest entries were dropped
        assertEquals(PendingRoutingState.BUFFER_LIMIT, flush.events().size(),
                "Buffer should be capped at BUFFER_LIMIT");

        pendingState.decrementGuard();
    }

    // =========================================================================
    // Test 7: inbound RPC requests are parked until the session is registered
    // =========================================================================

    @Test
    void parksInboundRequestsUntilRegistration() throws Exception {
        String pendingSessionId = "cloud-session-xyz";

        // Register a tool on the (not-yet-created) session by pre-creating it without
        // registering in the sessions map yet. We'll use the pending state directly.
        pendingState.incrementGuard();

        // In a background thread, send a tool.call request for the pending session.
        // The handler should park until the session is registered.
        var toolCallFuture = CompletableFuture.runAsync(() -> {
            ObjectNode params = MAPPER.createObjectNode();
            params.put("sessionId", pendingSessionId);
            params.put("toolCallId", "tc-1");
            params.put("toolName", "say_hello");
            params.set("arguments", MAPPER.createObjectNode());
            invokeHandler("tool.call", "42", params);
        });

        // Brief pause to allow the handler thread to start and park
        Thread.sleep(100);

        // Create and register the session with the requested tool
        var session = new CopilotSession(pendingSessionId, rpc);
        session.registerTools(java.util.List.of(
                ToolDefinition.create("say_hello", "Greets the user", Map.of("type", "object", "properties", Map.of()),
                        inv -> CompletableFuture.completedFuture(ToolResultObject.success("hello!")))));

        var flush = pendingState.registerAndFlush(pendingSessionId, session, sessions);

        // No buffered notifications in this test
        assertTrue(flush.events().isEmpty(), "No buffered events expected");

        // Complete any parked request waiters
        for (var waiter : flush.waiters()) {
            waiter.complete(session);
        }

        pendingState.decrementGuard();

        // Wait for the handler to finish (it was parked on the waiter)
        toolCallFuture.get(5, TimeUnit.SECONDS);

        // The tool.call handler should have executed and sent a response back on the
        // wire
        JsonNode response = readResponse();
        assertNotNull(response, "Should have received a tool response");
        assertEquals(42, response.get("id").asInt(), "Response id should match request id");
        assertNotNull(response.get("result"), "Tool call should produce a result");
    }

    @Test
    void parkedRequestFailsExceptionallyWhenGuardDroppedWithoutRegistration() throws Exception {
        String pendingSessionId = "cloud-session-dropped";

        pendingState.incrementGuard();

        // Park a request in the background
        var toolCallFuture = CompletableFuture.runAsync(() -> {
            ObjectNode params = MAPPER.createObjectNode();
            params.put("sessionId", pendingSessionId);
            params.put("toolCallId", "tc-2");
            params.put("toolName", "any_tool");
            params.set("arguments", MAPPER.createObjectNode());
            invokeHandler("tool.call", "99", params);
        });

        Thread.sleep(100);

        // Drop the guard WITHOUT registering the session. decrementGuard now
        // completes parked waiters internally with the canonical message.
        pendingState.decrementGuard();

        // The handler should receive the exceptional completion and send an
        // error response
        toolCallFuture.get(5, TimeUnit.SECONDS);

        JsonNode response = readResponse();
        assertNotNull(response, "Should have received an error response");
        assertEquals(99, response.get("id").asInt());
        assertNotNull(response.get("error"), "Response should be an error (not a result)");
        String errorMessage = response.get("error").get("message").asText();
        assertTrue(errorMessage.contains("routing ended before session was registered"),
                "Error message should contain the canonical guard-drop phrase; got: " + errorMessage);
    }

    // =========================================================================
    // Test 8: overflow path — oldest parked waiter gets the overflow message
    // =========================================================================

    @Test
    void parkedRequestWaiterBuffer_overflow_evictsOldestWithOverflowMessage() throws Exception {
        pendingState.incrementGuard();
        String sid = "cloud-overflow-requests";

        // Park BUFFER_LIMIT + 1 waiters via tryParkRequest. The 129th call must
        // evict the very first waiter and complete it with the overflow message.
        var waiters = new java.util.ArrayList<CompletableFuture<CopilotSession>>();
        for (int i = 0; i < PendingRoutingState.BUFFER_LIMIT + 1; i++) {
            waiters.add(pendingState.tryParkRequest(sid, sessions));
        }

        // The first waiter (oldest) must have been evicted with the overflow message.
        CompletableFuture<CopilotSession> oldest = waiters.get(0);
        assertTrue(oldest.isCompletedExceptionally(), "Oldest waiter should be completed exceptionally on overflow");
        ExecutionException ex = assertThrows(ExecutionException.class, oldest::get);
        assertEquals("pending session buffer overflow", ex.getCause().getMessage());

        // The remaining BUFFER_LIMIT waiters should still be pending.
        for (int i = 1; i <= PendingRoutingState.BUFFER_LIMIT; i++) {
            assertFalse(waiters.get(i).isDone(), "Waiter " + i + " should still be pending after overflow eviction");
        }

        // Registering the session resolves the remaining 128 waiters normally.
        var session = new CopilotSession(sid, rpc);
        var flush = pendingState.registerAndFlush(sid, session, sessions);
        assertEquals(PendingRoutingState.BUFFER_LIMIT, flush.waiters().size(),
                "registerAndFlush should return all non-evicted waiters");
        for (var waiter : flush.waiters()) {
            waiter.complete(session);
        }
        for (int i = 1; i <= PendingRoutingState.BUFFER_LIMIT; i++) {
            assertFalse(waiters.get(i).isCompletedExceptionally(),
                    "Waiter " + i + " should complete normally, not exceptionally");
            assertEquals(session, waiters.get(i).get(1, TimeUnit.SECONDS));
        }

        pendingState.decrementGuard();
    }

    // =========================================================================
    // Test 9: guard-drop message is distinct from overflow message
    // =========================================================================

    @Test
    void parkedRequestWaiter_guardDropMessage_isDistinctFromOverflowMessage() throws Exception {
        String pendingSessionId = "cloud-session-distinct-msg";

        pendingState.incrementGuard();

        // Park a request in the background via the full handler path so the
        // response travels over the socket — this mirrors the real runtime flow.
        var toolCallFuture = CompletableFuture.runAsync(() -> {
            ObjectNode params = MAPPER.createObjectNode();
            params.put("sessionId", pendingSessionId);
            params.put("toolCallId", "tc-distinct");
            params.put("toolName", "noop");
            params.set("arguments", MAPPER.createObjectNode());
            invokeHandler("tool.call", "77", params);
        });

        Thread.sleep(100);

        // Drop the guard without registration. decrementGuard completes waiters
        // internally with the canonical guard-drop message.
        pendingState.decrementGuard();

        toolCallFuture.get(5, TimeUnit.SECONDS);

        JsonNode response = readResponse();
        assertEquals(77, response.get("id").asInt());
        assertNotNull(response.get("error"), "Should be an error response");
        String msg = response.get("error").get("message").asText();

        // Must contain the guard-drop phrase — NOT the overflow phrase.
        assertTrue(msg.contains("routing ended before session was registered"),
                "Guard-drop error must use the routing-ended phrase; got: " + msg);
        assertFalse(msg.contains("buffer overflow"), "Guard-drop error must NOT use the overflow phrase; got: " + msg);
    }
}
