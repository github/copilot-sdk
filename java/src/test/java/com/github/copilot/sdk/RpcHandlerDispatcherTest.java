/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.sdk.json.PermissionRequestResult;
import com.github.copilot.sdk.json.PreToolUseHookOutput;
import com.github.copilot.sdk.json.SessionHooks;
import com.github.copilot.sdk.json.SessionLifecycleEvent;
import com.github.copilot.sdk.json.ToolDefinition;
import com.github.copilot.sdk.json.ToolResultObject;
import com.github.copilot.sdk.json.UserInputResponse;

/**
 * Unit tests for {@link RpcHandlerDispatcher} focusing on coverage gaps
 * identified by JaCoCo: unknown sessions, missing fields, error paths, and edge
 * cases for each handler method.
 */
class RpcHandlerDispatcherTest {

    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();
    private static final int SOCKET_TIMEOUT_MS = 5000;

    private Socket clientSideSocket;
    private Socket serverSideSocket;
    private JsonRpcClient rpc;
    private Map<String, CopilotSession> sessions;
    private CopyOnWriteArrayList<SessionLifecycleEvent> lifecycleEvents;
    private RpcHandlerDispatcher dispatcher;
    private InputStream responseStream;
    private Map<String, BiConsumer<String, JsonNode>> handlers;

    @BeforeEach
    void setup() throws Exception {
        // Create a socket pair for the JsonRpcClient
        try (ServerSocket ss = new ServerSocket(0)) {
            clientSideSocket = new Socket("localhost", ss.getLocalPort());
            serverSideSocket = ss.accept();
        }
        serverSideSocket.setSoTimeout(SOCKET_TIMEOUT_MS);

        rpc = JsonRpcClient.fromSocket(clientSideSocket);
        responseStream = serverSideSocket.getInputStream();

        sessions = new ConcurrentHashMap<>();
        lifecycleEvents = new CopyOnWriteArrayList<>();
        dispatcher = new RpcHandlerDispatcher(sessions, lifecycleEvents::add);
        dispatcher.registerHandlers(rpc);

        // Extract the registered handlers via reflection so we can invoke them directly
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

    /** Invoke a registered RPC handler directly. */
    private void invokeHandler(String method, String requestId, JsonNode params) {
        handlers.get(method).accept(requestId, params);
    }

    /** Read a single JSON-RPC response message from the server-side socket. */
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

    /** Create and register a CopilotSession in the sessions map. */
    private CopilotSession createSession(String sessionId) {
        CopilotSession session = new CopilotSession(sessionId, rpc);
        sessions.put(sessionId, session);
        return session;
    }

    // ===== session.event tests =====

    @Test
    void sessionEventWithNullEventNode() throws Exception {
        CopilotSession session = createSession("s1");
        var dispatched = new CopyOnWriteArrayList<>();
        session.on(dispatched::add);

        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", "s1");
        // "event" field is absent → eventNode is null

        invokeHandler("session.event", null, params);

        // Give a moment for async processing (though this handler is synchronous)
        Thread.sleep(50);
        assertTrue(dispatched.isEmpty(), "No events should be dispatched when eventNode is null");
    }

    @Test
    void sessionEventWithUnknownSession() {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", "unknown");
        ObjectNode event = params.putObject("event");
        event.put("type", "assistantMessage");
        event.putObject("data").put("content", "hello");

        // Should not throw — silently skips when session is not found
        assertDoesNotThrow(() -> invokeHandler("session.event", null, params));
    }

    // ===== session.lifecycle tests =====

    @Test
    void lifecycleEventWithMissingTypeAndSessionId() {
        ObjectNode params = MAPPER.createObjectNode();
        // No "type" or "sessionId" fields — defaults to ""

        invokeHandler("session.lifecycle", null, params);

        assertEquals(1, lifecycleEvents.size());
        assertEquals("", lifecycleEvents.get(0).getType());
        assertEquals("", lifecycleEvents.get(0).getSessionId());
    }

    @Test
    void lifecycleEventWithoutMetadata() {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("type", "started");
        params.put("sessionId", "s1");
        // No "metadata" field at all

        invokeHandler("session.lifecycle", null, params);

        assertEquals(1, lifecycleEvents.size());
        assertEquals("started", lifecycleEvents.get(0).getType());
        assertNull(lifecycleEvents.get(0).getMetadata());
    }

    @Test
    void lifecycleEventWithNullMetadata() {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("type", "ended");
        params.put("sessionId", "s2");
        params.putNull("metadata");

        invokeHandler("session.lifecycle", null, params);

        assertEquals(1, lifecycleEvents.size());
        assertEquals("ended", lifecycleEvents.get(0).getType());
        assertNull(lifecycleEvents.get(0).getMetadata());
    }

    // ===== tool.call tests =====

    @Test
    void toolCallWithUnknownSession() throws Exception {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", "nonexistent");
        params.put("toolCallId", "tc1");
        params.put("toolName", "my_tool");
        params.putObject("arguments");

        invokeHandler("tool.call", "1", params);

        JsonNode response = readResponse();
        assertNotNull(response.get("error"));
        assertEquals(-32602, response.get("error").get("code").asInt());
        assertTrue(response.get("error").get("message").asText().contains("nonexistent"));
    }

    @Test
    void toolCallWithUnknownTool() throws Exception {
        createSession("s1");
        // Don't register any tools

        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", "s1");
        params.put("toolCallId", "tc1");
        params.put("toolName", "nonexistent_tool");
        params.putObject("arguments");

        invokeHandler("tool.call", "2", params);

        JsonNode response = readResponse();
        JsonNode result = response.get("result").get("result");
        assertEquals("failure", result.get("resultType").asText());
        assertTrue(result.get("error").asText().contains("nonexistent_tool"));
    }

    @Test
    void toolCallReturnsToolResultObjectDirectly() throws Exception {
        CopilotSession session = createSession("s1");
        var tool = ToolDefinition.create("my_tool", "A test tool", Map.of("type", "object"),
                invocation -> CompletableFuture.completedFuture(
                        new ToolResultObject().setResultType("success").setTextResultForLlm("direct result")));
        session.registerTools(List.of(tool));

        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", "s1");
        params.put("toolCallId", "tc1");
        params.put("toolName", "my_tool");
        params.putObject("arguments");

        invokeHandler("tool.call", "3", params);

        JsonNode response = readResponse();
        JsonNode result = response.get("result").get("result");
        assertEquals("success", result.get("resultType").asText());
        assertEquals("direct result", result.get("textResultForLlm").asText());
    }

    @Test
    void toolCallWithNonStringResult() throws Exception {
        CopilotSession session = createSession("s1");
        var tool = ToolDefinition.create("map_tool", "Returns a map", Map.of("type", "object"),
                invocation -> CompletableFuture.completedFuture(Map.of("key", "value")));
        session.registerTools(List.of(tool));

        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", "s1");
        params.put("toolCallId", "tc1");
        params.put("toolName", "map_tool");
        params.putObject("arguments");

        invokeHandler("tool.call", "4", params);

        JsonNode response = readResponse();
        JsonNode result = response.get("result").get("result");
        assertEquals("success", result.get("resultType").asText());
        // The map should be serialized to JSON string
        assertNotNull(result.get("textResultForLlm").asText());
    }

    @Test
    void toolCallHandlerFails() throws Exception {
        CopilotSession session = createSession("s1");
        var tool = ToolDefinition.create("fail_tool", "Fails", Map.of("type", "object"),
                invocation -> CompletableFuture.failedFuture(new RuntimeException("tool error")));
        session.registerTools(List.of(tool));

        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", "s1");
        params.put("toolCallId", "tc1");
        params.put("toolName", "fail_tool");
        params.putObject("arguments");

        invokeHandler("tool.call", "5", params);

        JsonNode response = readResponse();
        JsonNode result = response.get("result").get("result");
        assertEquals("failure", result.get("resultType").asText());
    }

    // ===== permission.request tests =====

    @Test
    void permissionRequestWithUnknownSession() throws Exception {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", "nonexistent");
        params.putObject("permissionRequest");

        invokeHandler("permission.request", "10", params);

        JsonNode response = readResponse();
        JsonNode result = response.get("result").get("result");
        assertEquals("denied-no-approval-rule-and-could-not-request-from-user", result.get("kind").asText());
    }

    @Test
    void permissionRequestWithHandler() throws Exception {
        CopilotSession session = createSession("s1");
        session.registerPermissionHandler((request, invocation) -> CompletableFuture
                .completedFuture(new PermissionRequestResult().setKind("allow")));

        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", "s1");
        params.putObject("permissionRequest");

        invokeHandler("permission.request", "11", params);

        JsonNode response = readResponse();
        JsonNode result = response.get("result").get("result");
        assertEquals("allow", result.get("kind").asText());
    }

    @Test
    void permissionRequestHandlerFails() throws Exception {
        CopilotSession session = createSession("s1");
        session.registerPermissionHandler(
                (request, invocation) -> CompletableFuture.failedFuture(new RuntimeException("permission error")));

        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", "s1");
        params.putObject("permissionRequest");

        invokeHandler("permission.request", "12", params);

        JsonNode response = readResponse();
        // CopilotSession catches the exception and returns a denied result
        JsonNode result = response.get("result").get("result");
        assertEquals("denied-no-approval-rule-and-could-not-request-from-user", result.get("kind").asText());
    }

    // ===== userInput.request tests =====

    @Test
    void userInputRequestWithUnknownSession() throws Exception {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", "nonexistent");
        params.put("question", "What?");

        invokeHandler("userInput.request", "20", params);

        JsonNode response = readResponse();
        assertNotNull(response.get("error"));
        assertEquals(-32602, response.get("error").get("code").asInt());
    }

    @Test
    void userInputRequestWithNullChoicesAndFreeform() throws Exception {
        CopilotSession session = createSession("s1");
        session.registerUserInputHandler((request, invocation) -> CompletableFuture
                .completedFuture(new UserInputResponse().setAnswer("my answer").setWasFreeform(true)));

        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", "s1");
        params.put("question", "What is your name?");
        // No "choices" or "allowFreeform" fields

        invokeHandler("userInput.request", "21", params);

        JsonNode response = readResponse();
        JsonNode result = response.get("result");
        assertEquals("my answer", result.get("answer").asText());
        assertTrue(result.get("wasFreeform").asBoolean());
    }

    @Test
    void userInputRequestWithNullAnswer() throws Exception {
        CopilotSession session = createSession("s1");
        session.registerUserInputHandler((request, invocation) -> CompletableFuture
                .completedFuture(new UserInputResponse().setAnswer(null).setWasFreeform(false)));

        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", "s1");
        params.put("question", "Choose something");

        invokeHandler("userInput.request", "22", params);

        JsonNode response = readResponse();
        JsonNode result = response.get("result");
        // Null answer should be replaced with empty string
        assertEquals("", result.get("answer").asText());
        assertFalse(result.get("wasFreeform").asBoolean());
    }

    @Test
    void userInputRequestWithNoHandler() throws Exception {
        // Session exists but no user input handler registered
        createSession("s1");

        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", "s1");
        params.put("question", "What?");

        invokeHandler("userInput.request", "23", params);

        JsonNode response = readResponse();
        // No handler → CopilotSession returns failedFuture → dispatcher's
        // .exceptionally() fires
        assertNotNull(response.get("error"));
        assertEquals(-32603, response.get("error").get("code").asInt());
        assertTrue(response.get("error").get("message").asText().contains("User input handler error"));
    }

    @Test
    void userInputRequestHandlerFails() throws Exception {
        CopilotSession session = createSession("s1");
        session.registerUserInputHandler(
                (request, invocation) -> CompletableFuture.failedFuture(new RuntimeException("handler failed")));

        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", "s1");
        params.put("question", "What?");

        invokeHandler("userInput.request", "24", params);

        JsonNode response = readResponse();
        assertNotNull(response.get("error"));
        assertEquals(-32603, response.get("error").get("code").asInt());
    }

    // ===== hooks.invoke tests =====

    @Test
    void hooksInvokeWithUnknownSession() throws Exception {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", "nonexistent");
        params.put("hookType", "preToolUse");
        params.putObject("input");

        invokeHandler("hooks.invoke", "30", params);

        JsonNode response = readResponse();
        assertNotNull(response.get("error"));
        assertEquals(-32602, response.get("error").get("code").asInt());
    }

    @Test
    void hooksInvokeWithNullOutput() throws Exception {
        CopilotSession session = createSession("s1");
        // Register empty hooks — no specific handler for preToolUse → returns null
        session.registerHooks(new SessionHooks());

        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", "s1");
        params.put("hookType", "preToolUse");
        params.putObject("input");

        invokeHandler("hooks.invoke", "31", params);

        JsonNode response = readResponse();
        // Null output triggers NPE in Map.of() → falls to .exceptionally() → error
        // response
        assertNotNull(response.get("error"));
        assertEquals(-32603, response.get("error").get("code").asInt());
    }

    @Test
    void hooksInvokeWithNonNullOutput() throws Exception {
        CopilotSession session = createSession("s1");
        session.registerHooks(new SessionHooks().setOnPreToolUse((input, invocation) -> CompletableFuture
                .completedFuture(new PreToolUseHookOutput().setPermissionDecision("allow"))));

        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", "s1");
        params.put("hookType", "preToolUse");
        ObjectNode input = params.putObject("input");
        input.put("toolName", "some_tool");
        input.put("toolCallId", "tc1");

        invokeHandler("hooks.invoke", "32", params);

        JsonNode response = readResponse();
        JsonNode output = response.get("result").get("output");
        assertNotNull(output);
        assertEquals("allow", output.get("permissionDecision").asText());
    }

    @Test
    void hooksInvokeHandlerFails() throws Exception {
        CopilotSession session = createSession("s1");
        session.registerHooks(new SessionHooks().setOnPreToolUse(
                (input, invocation) -> CompletableFuture.failedFuture(new RuntimeException("hook error"))));

        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", "s1");
        params.put("hookType", "preToolUse");
        ObjectNode input = params.putObject("input");
        input.put("toolName", "some_tool");
        input.put("toolCallId", "tc1");

        invokeHandler("hooks.invoke", "33", params);

        JsonNode response = readResponse();
        assertNotNull(response.get("error"));
        assertEquals(-32603, response.get("error").get("code").asInt());
        assertTrue(response.get("error").get("message").asText().contains("Hooks handler error"));
    }

    @Test
    void hooksInvokeWithNoHooksRegistered() throws Exception {
        // Session exists but no hooks registered at all → returns null output
        createSession("s1");

        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", "s1");
        params.put("hookType", "preToolUse");
        params.putObject("input");

        invokeHandler("hooks.invoke", "34", params);

        JsonNode response = readResponse();
        // Null output triggers NPE in Map.of() → falls to .exceptionally() → error
        // response
        assertNotNull(response.get("error"));
        assertEquals(-32603, response.get("error").get("code").asInt());
    }
}
