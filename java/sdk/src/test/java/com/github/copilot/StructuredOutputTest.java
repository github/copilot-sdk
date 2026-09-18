/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.copilot.generated.SessionEvent;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.SendMessageRequest;
import com.github.copilot.rpc.SendMessageResponse;

class StructuredOutputTest {
    @CopilotResponse
    public record Inventory(int count, String color) {
    }

    @CopilotResponse
    public record Nested(List<Inventory> items) {
    }

    private JsonRpcClient rpc;
    private CopilotSession session;

    @BeforeEach
    void setup() {
        rpc = mock(JsonRpcClient.class);
        session = new CopilotSession("session-1", rpc, null);
        when(rpc.invoke(eq("session.send"), any(), eq(SendMessageResponse.class)))
                .thenReturn(CompletableFuture.completedFuture(new SendMessageResponse("user-1")));
        when(rpc.invoke(eq("session.detach"), any(), eq(CopilotSession.SessionDetachResponse.class)))
                .thenReturn(CompletableFuture.completedFuture(new CopilotSession.SessionDetachResponse(true, null)));
    }

    @AfterEach
    void close() {
        session.close();
    }

    private void emit(String type, Map<String, Object> data) throws Exception {
        var event = JsonRpcClient.getObjectMapper().convertValue(Map.of("id", java.util.UUID.randomUUID().toString(),
                "timestamp", "2026-01-01T00:00:00Z", "type", type, "data", data), SessionEvent.class);
        var dispatch = CopilotSession.class.getDeclaredMethod("dispatchEvent", SessionEvent.class);
        dispatch.setAccessible(true);
        dispatch.invoke(session, event);
    }

    private void assistant(String origin, String content) throws Exception {
        emit("assistant.message", Map.of("messageId", "assistant", "originatingMessageId", origin, "content", content));
    }

    @Test
    void infersClosedNestedSchemasUsingToolProcessor() {
        var schema = JsonRpcClient.getObjectMapper().valueToTree(ResponseSchemas.forType(Nested.class));
        assertFalse(schema.path("additionalProperties").asBoolean(true));
        var inventory = schema.path("properties").path("items").path("items");
        assertEquals("integer", inventory.path("properties").path("count").path("type").asText());
        assertFalse(inventory.path("additionalProperties").asBoolean(true));
        assertEquals(2, inventory.path("required").size());
    }

    @Test
    void buffersPreAckEventsAndDoesNotMutateOptions() throws Exception {
        when(rpc.invoke(eq("session.send"), any(), eq(SendMessageResponse.class))).thenAnswer(invocation -> {
            SendMessageRequest request = invocation.getArgument(1);
            assertEquals(ResponseSchemas.forType(Inventory.class),
                    ((Map<?, ?>) request.getResponseFormat().get("jsonSchema")).get("schema"));
            assistant("user-1", "{\"count\":42,\"color\":\"red\"}");
            emit("session.idle", Map.of());
            return CompletableFuture.completedFuture(new SendMessageResponse("user-1"));
        });
        var options = new MessageOptions().setPrompt("inventory");
        assertEquals(new Inventory(42, "red"), session.sendAndWait(options, Inventory.class).get(1, TimeUnit.SECONDS));
        assertNull(options.getResponseSchema());
    }

    @Test
    void ignoresUnrelatedMessagesAndWaitsForCorrectedFinalIdle() throws Exception {
        var result = session.sendAndWait("inventory", Inventory.class);
        emit("session.idle", Map.of());
        assistant("user-1", "{\"count\":42,\"color\":\"red\"}");
        emit("session.idle", Map.of("mode", "autopilot"));
        assertFalse(result.isDone());
        assistant("user-1", "{\"count\":99,\"color\":\"blue\"}");
        assistant("other", "{\"count\":123,\"color\":\"wrong\"}");
        emit("session.idle", Map.of());
        assertEquals(new Inventory(99, "blue"), result.get(1, TimeUnit.SECONDS));
    }

    @Test
    void concurrentWaitsAreCorrelated() throws Exception {
        when(rpc.invoke(eq("session.send"), any(), eq(SendMessageResponse.class))).thenReturn(
                CompletableFuture.completedFuture(new SendMessageResponse("first")),
                CompletableFuture.completedFuture(new SendMessageResponse("second")));
        var first = session.sendAndWait("one", Inventory.class);
        var second = session.sendAndWait("two", Inventory.class);
        assistant("first", "{\"count\":42,\"color\":\"red\"}");
        assistant("second", "{\"count\":7,\"color\":\"blue\"}");
        emit("session.idle", Map.of());
        assertEquals(42, first.get(1, TimeUnit.SECONDS).count());
        assertEquals(7, second.get(1, TimeUnit.SECONDS).count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "not JSON", "{\"count\":\"bad\"}", "{\"count\":42,\"color\":\"red\"} trailing"})
    void rejectsNullAndInvalidJson(String content) throws Exception {
        var result = session.sendAndWait("inventory", Inventory.class);
        assistant("user-1", content);
        emit("session.idle", Map.of());
        assertThrows(ExecutionException.class, () -> result.get(1, TimeUnit.SECONDS));
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "aborted", "error", "tool"})
    void rejectsFailedOrIncompleteRuns(String kind) throws Exception {
        var result = session.sendAndWait("inventory", Inventory.class);
        emit("user.message", Map.of("messageId", "user-1", "content", "inventory"));
        if (kind.equals("tool")) {
            assistant("user-1", "{\"count\":42,\"color\":\"red\"}");
            emit("assistant.message", Map.of("messageId", "commentary", "originatingMessageId", "user-1", "content",
                    "working", "toolRequests", List.of(Map.of("toolCallId", "t", "name", "tool"))));
        }
        if (kind.equals("error")) {
            emit("session.error", Map.of("errorType", "test", "message", "provider failed"));
        } else {
            emit("session.idle", kind.equals("aborted") ? Map.of("aborted", true) : Map.of());
        }
        assertThrows(ExecutionException.class, () -> result.get(1, TimeUnit.SECONDS));
    }

    @Test
    void timeoutCancellationAndCloseCompleteWaits() throws Exception {
        var timed = session.sendAndWait(new MessageOptions().setPrompt("inventory"), Inventory.class, 10);
        assertThrows(ExecutionException.class, () -> timed.get(1, TimeUnit.SECONDS));
        var cancelled = session.sendAndWait("inventory", Inventory.class);
        assertTrue(cancelled.cancel(true));
        var closing = session.sendAndWait(new MessageOptions().setPrompt("inventory"), Inventory.class, 0);
        session.close();
        assertThrows(ExecutionException.class, () -> closing.get(1, TimeUnit.SECONDS));
    }

    @Test
    void admissionFailureIsNotHiddenByBufferedEvents() throws Exception {
        when(rpc.invoke(eq("session.send"), any(), eq(SendMessageResponse.class))).thenAnswer(invocation -> {
            assistant("user-1", "{\"count\":42,\"color\":\"red\"}");
            emit("session.idle", Map.of());
            return CompletableFuture.failedFuture(new IllegalArgumentException("invalid schema"));
        });
        var result = session.sendAndWait("inventory", Inventory.class);
        assertEquals("invalid schema",
                assertThrows(ExecutionException.class, () -> result.get(1, TimeUnit.SECONDS)).getCause().getMessage());
    }

    @Test
    void explicitSchemaIsForwardedUnchangedAndConflictingTypedOptionsAreRejected() throws Exception {
        var schema = Map.<String, Object>of("type", "object", "description", "unchanged");
        var options = new MessageOptions().setPrompt("inventory").setResponseSchema(schema);
        assertThrows(IllegalArgumentException.class, () -> session.sendAndWait(options, Inventory.class));
        assertThrows(IllegalArgumentException.class, () -> session
                .sendAndWait(new MessageOptions().setPrompt("inventory").setMode("immediate"), Inventory.class));
        verifyNoInteractions(rpc);
        var result = session.sendAndWait(options);
        assistant("user-1", "{}");
        emit("session.idle", Map.of());
        assertEquals("{}", result.get(1, TimeUnit.SECONDS).getData().content());
        verify(rpc).invoke(eq("session.send"),
                argThat(request -> request instanceof SendMessageRequest send
                        && schema.equals(((Map<?, ?>) send.getResponseFormat().get("jsonSchema")).get("schema"))),
                eq(SendMessageResponse.class));
    }
}
