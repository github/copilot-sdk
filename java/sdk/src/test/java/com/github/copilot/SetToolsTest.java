/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.generated.ExternalToolRequestedEvent;
import com.github.copilot.rpc.ToolDefinition;
import com.github.copilot.rpc.ToolDefer;

@AllowCopilotExperimental
class SetToolsTest {

    private JsonRpcClient rpc;
    private CopilotSession session;

    @BeforeEach
    void setup() {
        rpc = mock(JsonRpcClient.class);
        session = new CopilotSession("set-tools-session", rpc);
    }

    @Test
    void sendsCompleteReplacementPayloadAndInstallsOnlyHandlersAfterAcceptance() throws Exception {
        var accepted = new CompletableFuture<Void>();
        List<ObjectNode> requests = captureSetToolsRequests(accepted);
        var originalCalls = new CopyOnWriteArrayList<String>();
        var replacementCalls = new CopyOnWriteArrayList<String>();
        session.registerTools(List.of(tool("original_tool", "Original", "old", originalCalls)));

        ToolDefinition declarationOnly = new ToolDefinition("declaration_only", null, Map.of("type", "object"), null,
                true, true, ToolDefer.NEVER, Map.of("owner", "test"), true);
        var pending = session
                .setTools(List.of(tool("replacement_tool", null, "new", replacementCalls), declarationOnly));

        assertEquals(1, requests.size());
        JsonNode payload = requests.get(0);
        assertEquals("set-tools-session", payload.path("sessionId").asText());
        assertEquals("replacement_tool", payload.at("/tools/0/name").asText());
        assertEquals("", payload.at("/tools/0/description").asText());
        assertEquals("object", payload.at("/tools/0/parameters/type").asText());
        assertFalse(payload.at("/tools/0").has("handler"));
        assertEquals("declaration_only", payload.at("/tools/1/name").asText());
        assertEquals("", payload.at("/tools/1/description").asText());
        assertTrue(payload.at("/tools/1/overridesBuiltInTool").asBoolean());
        assertTrue(payload.at("/tools/1/skipPermission").asBoolean());
        assertEquals("never", payload.at("/tools/1/defer").asText());
        assertTrue(payload.at("/tools/1/isTerminal").asBoolean());
        assertEquals("test", payload.at("/tools/1/metadata/owner").asText());

        dispatchToolRequest("request-old", "original_tool");
        waitForCalls(originalCalls, List.of("original_tool"));
        assertNull(session.getTool("replacement_tool"));

        accepted.complete(null);
        pending.get(1, TimeUnit.SECONDS);

        assertNull(session.getTool("original_tool"));
        dispatchToolRequest("request-new", "replacement_tool");
        waitForCalls(replacementCalls, List.of("replacement_tool"));
    }

    @Test
    void rejectionLeavesExistingHandlersUnchanged() throws Exception {
        var rejected = new CompletableFuture<Void>();
        captureSetToolsRequests(rejected);
        var originalCalls = new CopyOnWriteArrayList<String>();
        var replacementCalls = new CopyOnWriteArrayList<String>();
        session.registerTools(List.of(tool("lookup", "Original", "old", originalCalls)));

        var pending = session.setTools(List.of(tool("lookup", "Replacement", "new", replacementCalls)));
        rejected.completeExceptionally(new IllegalArgumentException("invalid params"));

        var error = assertThrows(ExecutionException.class, () -> pending.get(1, TimeUnit.SECONDS));
        assertInstanceOf(IllegalArgumentException.class, error.getCause());
        dispatchToolRequest("request-after-reject", "lookup");
        waitForCalls(originalCalls, List.of("lookup"));
        assertTrue(replacementCalls.isEmpty());
    }

    @Test
    void emptyListRemovesHandlersAfterAcceptance() throws Exception {
        var accepted = new CompletableFuture<Void>();
        List<ObjectNode> requests = captureSetToolsRequests(accepted);
        session.registerTools(List.of(tool("lookup", "Original", "old", new CopyOnWriteArrayList<>())));

        var pending = session.setTools(List.of());
        assertTrue(requests.get(0).path("tools").isArray());
        assertEquals(0, requests.get(0).path("tools").size());

        accepted.complete(null);
        pending.get(1, TimeUnit.SECONDS);
        assertNull(session.getTool("lookup"));
    }

    @Test
    void concurrentCallsAreSentInOrderAndFailureDoesNotBlockLaterCall() throws Exception {
        var rpcResults = List.of(new CompletableFuture<Void>(), new CompletableFuture<Void>());
        var nextResult = new AtomicInteger();
        var requests = new ArrayList<ObjectNode>();
        when(rpc.invoke(eq("session.tools.set"), any(), eq(Void.class))).thenAnswer(invocation -> {
            requests.add(invocation.getArgument(1));
            return rpcResults.get(nextResult.getAndIncrement());
        });

        var first = session.setTools(List.of(tool("first", "First", "first", new CopyOnWriteArrayList<>())));
        var second = session.setTools(List.of(tool("second", "Second", "second", new CopyOnWriteArrayList<>())));

        assertEquals(1, requests.size(), "second replacement must wait for the first RPC to finish");
        assertEquals("first", requests.get(0).at("/tools/0/name").asText());

        rpcResults.get(0).completeExceptionally(new IllegalArgumentException("rejected"));
        var firstError = assertThrows(ExecutionException.class, () -> first.get(1, TimeUnit.SECONDS));
        assertInstanceOf(IllegalArgumentException.class, firstError.getCause());
        assertEquals(2, requests.size());
        assertEquals("second", requests.get(1).at("/tools/0/name").asText());

        rpcResults.get(1).complete(null);
        second.get(1, TimeUnit.SECONDS);
        assertNull(session.getTool("first"));
        assertNotNull(session.getTool("second"));
    }

    @Test
    void cancellingReturnedFutureDoesNotPreventAcceptedReplacementInstallingHandlers() throws Exception {
        var accepted = new CompletableFuture<Void>();
        captureSetToolsRequests(accepted);

        var pending = session
                .setTools(List.of(tool("replacement", "Replacement", "new", new CopyOnWriteArrayList<>())));
        assertTrue(pending.cancel(true));

        accepted.complete(null);

        assertTrue(pending.isCancelled());
        assertNotNull(session.getTool("replacement"));
    }

    @Test
    void cancellingWhileQueuedSendsNothing() throws Exception {
        var firstResult = new CompletableFuture<Void>();
        var requests = captureSetToolsRequests(firstResult);

        var first = session.setTools(List.of(tool("first", "First", "first", new CopyOnWriteArrayList<>())));
        var queued = session.setTools(List.of(tool("queued", "Queued", "queued", new CopyOnWriteArrayList<>())));
        assertTrue(queued.cancel(true));

        firstResult.complete(null);
        first.get(1, TimeUnit.SECONDS);

        assertEquals(1, requests.size());
        assertEquals("first", requests.get(0).at("/tools/0/name").asText());
        assertNotNull(session.getTool("first"));
        assertNull(session.getTool("queued"));
    }

    private List<ObjectNode> captureSetToolsRequests(CompletableFuture<Void> result) {
        var requests = new ArrayList<ObjectNode>();
        when(rpc.invoke(eq("session.tools.set"), any(), eq(Void.class))).thenAnswer(invocation -> {
            requests.add(invocation.getArgument(1));
            return result;
        });
        return requests;
    }

    private static ToolDefinition tool(String name, String description, String result, List<String> calls) {
        return ToolDefinition.create(name, description, Map.of("type", "object"), invocation -> {
            calls.add(name);
            return CompletableFuture.completedFuture(result);
        });
    }

    private void dispatchToolRequest(String requestId, String toolName) {
        var requested = new ExternalToolRequestedEvent();
        requested.setData(new ExternalToolRequestedEvent.ExternalToolRequestedEventData(requestId, "set-tools-session",
                "tool-call-" + requestId, toolName, null, Map.of(), null, null, null));
        try {
            Method dispatchMethod = CopilotSession.class.getDeclaredMethod("dispatchEvent",
                    com.github.copilot.generated.SessionEvent.class);
            dispatchMethod.setAccessible(true);
            dispatchMethod.invoke(session, requested);
        } catch (Exception e) {
            throw new RuntimeException("Failed to dispatch tool request", e);
        }
    }

    private static void waitForCalls(List<String> calls, List<String> expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (System.nanoTime() < deadline) {
            if (calls.equals(expected)) {
                return;
            }
            Thread.onSpinWait();
        }
        assertEquals(expected, calls);
    }
}
