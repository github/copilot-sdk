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
import org.junit.jupiter.params.provider.CsvSource;
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
        emit(type, data, "");
    }

    private void emit(String type, Map<String, Object> data, String agentId) throws Exception {
        var event = JsonRpcClient.getObjectMapper().convertValue(Map.of("id", java.util.UUID.randomUUID().toString(),
                "timestamp", "2026-01-01T00:00:00Z", "type", type, "data", data, "agentId", agentId),
                SessionEvent.class);
        var dispatch = CopilotSession.class.getDeclaredMethod("dispatchEvent", SessionEvent.class);
        dispatch.setAccessible(true);
        dispatch.invoke(session, event);
    }

    private void assistant(String origin, String content) throws Exception {
        emit("assistant.message", Map.of("messageId", "assistant", "originatingMessageId", origin, "content", content));
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void correlatesErrorsIndependentlyOfMessageEvents(boolean structured, boolean beforeAck) throws Exception {
        for (String source : List.of("user", "assistant", "autopilot", "system", "session-memory-rollover", "marked",
                "pre-message", "steering")) {
            var admission = new CompletableFuture<SendMessageResponse>();
            when(rpc.invoke(eq("session.send"), any(), eq(SendMessageResponse.class))).thenReturn(admission);
            var options = new MessageOptions().setPrompt("waited");
            if (structured)
                options.setResponseSchema(Map.of("type", "object"));
            var result = session.sendAndWait(options, 1_000);
            if (!beforeAck)
                admission.complete(new SendMessageResponse("user-1"));
            assistant(source.equals("pre-message") ? "other" : "user-1", "{\"ok\":true}");
            if (source.equals("assistant"))
                assistant("other", "other");
            else
                emit("user.message",
                        Map.of("messageId", "other", "content", "other", "source",
                                source.equals("steering") || source.equals("pre-message") ? "sdk" : source,
                                "isAutopilotContinuation", source.equals("marked")));
            emit("session.error", Map.of("errorType", "query", "message", "provider failed", "originatingMessageId",
                    source.equals("user") || source.equals("assistant") ? "other" : "user-1"));
            emit("session.idle", Map.of());
            if (beforeAck)
                admission.complete(new SendMessageResponse("user-1"));
            if (source.equals("user") || source.equals("assistant"))
                assertEquals("{\"ok\":true}", result.get(1, TimeUnit.SECONDS).getData().content());
            else
                assertTrue(assertThrows(ExecutionException.class, () -> result.get(1, TimeUnit.SECONDS)).getCause()
                        .getMessage().contains("provider failed"));
        }
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
    @ValueSource(strings = {"session.idle", "session.autopilot_stopped", "session.error"})
    void plainWaitPreservesPreAckOutcomeThroughRetirement(String terminal) throws Exception {
        var admission = new CompletableFuture<SendMessageResponse>();
        when(rpc.invoke(eq("session.send"), any(), eq(SendMessageResponse.class))).thenReturn(admission);
        var result = session.sendAndWait(new MessageOptions().setPrompt("finish"), 0);
        assistant("user-1", "finished");
        emit(terminal, Map.of("reason", "task_complete", "requestId", "user-1", "errorType", "test", "message",
                "original failure"));
        session.forceStopLocally();
        admission.complete(new SendMessageResponse("user-1"));
        if (terminal.equals("session.error")) {
            assertEquals("Session error: original failure",
                    assertThrows(ExecutionException.class, () -> result.get(1, TimeUnit.SECONDS)).getCause()
                            .getMessage());
        } else {
            assertEquals("finished", result.get(1, TimeUnit.SECONDS).getData().content());
        }
    }

    @Test
    void plainWaitIgnoresStaleAbortedIdle() throws Exception {
        var result = session.sendAndWait(new MessageOptions().setPrompt("queued"), 1_000);
        emit("session.idle", Map.of("aborted", true));
        emit("session.idle", Map.of());
        assertFalse(result.isDone());
        emit("user.message", Map.of("messageId", "user-1", "content", "queued"));
        emit("session.idle", Map.of());
        assertNull(result.get(1, TimeUnit.SECONDS));
    }

    @Test
    void plainWaitCompletesPolicyBlockedPromptOnCorrelatedWarning() throws Exception {
        var result = session.sendAndWait(new MessageOptions().setPrompt("blocked"), 1_000);
        emit("session.warning",
                Map.of("warningType", "policy_blocked", "message", "blocked", "originatingMessageId", "other"));
        emit("session.idle", Map.of());
        assertFalse(result.isDone());
        emit("session.warning",
                Map.of("warningType", "policy_blocked", "message", "blocked", "originatingMessageId", "user-1"));
        emit("session.idle", Map.of());
        assertNull(result.get(1, TimeUnit.SECONDS));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void plainWaitIgnoresEarlierSuccessfulIdle(boolean beforeAck) throws Exception {
        var admission = new CompletableFuture<SendMessageResponse>();
        when(rpc.invoke(eq("session.send"), any(), eq(SendMessageResponse.class))).thenReturn(admission);
        var result = session.sendAndWait(new MessageOptions().setPrompt("queued"), 1_000);
        if (!beforeAck)
            admission.complete(new SendMessageResponse("user-1"));
        assistant("other", "previous");
        emit("session.idle", Map.of());
        if (beforeAck)
            admission.complete(new SendMessageResponse("user-1"));
        assistant("user-1", "current");
        emit("session.idle", Map.of());
        assertEquals("current", result.get(1, TimeUnit.SECONDS).getData().content());
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void plainWaitCorrelatesPolicyBlockedPrompts(boolean beforeAck, boolean aborted) throws Exception {
        var admission = new CompletableFuture<SendMessageResponse>();
        when(rpc.invoke(eq("session.send"), any(), eq(SendMessageResponse.class))).thenReturn(admission);
        var result = session.sendAndWait(new MessageOptions().setPrompt("blocked"), 1_000);
        if (!beforeAck)
            admission.complete(new SendMessageResponse("user-1"));
        emit("session.warning",
                Map.of("warningType", "policy_blocked", "message", "Blocked", "originatingMessageId", "other"));
        emit("session.warning",
                Map.of("warningType", "policy_blocked", "message", "Blocked", "originatingMessageId", "user-1"),
                "child");
        emit("session.idle", Map.of());
        emit("session.warning",
                Map.of("warningType", "policy_blocked", "message", "Blocked", "originatingMessageId", "user-1"));
        emit("session.idle", Map.of("aborted", aborted));
        if (beforeAck)
            admission.complete(new SendMessageResponse("user-1"));
        if (aborted) {
            var error = assertThrows(ExecutionException.class, () -> result.get(1, TimeUnit.SECONDS));
            assertTrue(error.getCause().getMessage().toLowerCase().contains("aborted"), error.toString());
        } else {
            assertNull(result.get(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void plainWaitRejectsAbortedIdleAfterMatchingUserMessage() throws Exception {
        var result = session.sendAndWait(new MessageOptions().setPrompt("cancel"), 1_000);
        emit("user.message", Map.of("messageId", "user-1", "content", "cancel"));
        emit("session.idle", Map.of("aborted", true));
        var error = assertThrows(ExecutionException.class, () -> result.get(1, TimeUnit.SECONDS));
        assertTrue(error.getCause().getMessage().toLowerCase().contains("aborted"), error.toString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void autopilotTerminalBuffersPreAckAndIgnoresUnrelatedRequests(boolean structured) throws Exception {
        when(rpc.invoke(eq("session.send"), any(), eq(SendMessageResponse.class))).thenAnswer(invocation -> {
            assistant("user-1", "{\"count\":42,\"color\":\"red\"}");
            emit("user.message", Map.of("content", "Continue", "messageId", "other", "source", "autopilot",
                    "isAutopilotContinuation", true));
            emit("session.autopilot_stopped", Map.of("reason", "task_complete", "requestId", "other"));
            emit("session.autopilot_stopped", Map.of("reason", "task_complete"));
            emit("session.autopilot_stopped", Map.of("reason", "task_complete", "requestId", "user-1"), "child");
            emit("session.idle", Map.of("mode", "autopilot"));
            assistant("user-1", "{\"count\":7,\"color\":\"blue\"}");
            emit("session.autopilot_stopped", Map.of("reason", "task_complete", "requestId", "user-1"));
            return CompletableFuture.completedFuture(new SendMessageResponse("user-1"));
        });
        var options = new MessageOptions().setPrompt("inventory");
        if (structured)
            options.setResponseSchema(Map.of("type", "object"));
        assertEquals("{\"count\":7,\"color\":\"blue\"}",
                session.sendAndWait(options).get(1, TimeUnit.SECONDS).getData().content());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void autopilotErrorTerminalDoesNotRequireSessionError(boolean structured) throws Exception {
        var options = new MessageOptions().setPrompt("fail");
        if (structured)
            options.setResponseSchema(Map.of("type", "object"));
        var result = session.sendAndWait(options);
        emit("session.autopilot_stopped", Map.of("reason", "error", "requestId", "user-1"));
        assertEquals("Autopilot request failed",
                assertThrows(ExecutionException.class, () -> result.get(1, TimeUnit.SECONDS)).getCause().getMessage());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void autopilotAbortedTerminalDoesNotSucceed(boolean structured) throws Exception {
        var options = new MessageOptions().setPrompt("cancel");
        if (structured)
            options.setResponseSchema(Map.of("type", "object"));
        var result = session.sendAndWait(options);
        assistant("user-1", "{\"count\":42,\"color\":\"red\"}");
        emit("session.autopilot_stopped", Map.of("reason", "aborted", "requestId", "user-1"));
        var error = assertThrows(ExecutionException.class, () -> result.get(1, TimeUnit.SECONDS));
        assertTrue(error.getCause().getMessage().toLowerCase().contains("aborted"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void autopilotTerminalsIsolateConcurrentWaits(boolean structured) throws Exception {
        when(rpc.invoke(eq("session.send"), any(), eq(SendMessageResponse.class))).thenReturn(
                CompletableFuture.completedFuture(new SendMessageResponse("first")),
                CompletableFuture.completedFuture(new SendMessageResponse("second")));
        var options = new MessageOptions().setPrompt("inventory");
        if (structured)
            options.setResponseSchema(Map.of("type", "object"));
        var first = session.sendAndWait(options);
        var second = session.sendAndWait(options);
        assistant("first", "{\"count\":42,\"color\":\"red\"}");
        emit("session.autopilot_stopped", Map.of("reason", "task_complete", "requestId", "first"));
        assistant("second", "{\"count\":7,\"color\":\"blue\"}");
        emit("session.autopilot_stopped", Map.of("reason", "task_complete", "requestId", "second"));
        assertEquals("{\"count\":42,\"color\":\"red\"}", first.get(1, TimeUnit.SECONDS).getData().content());
        assertEquals("{\"count\":7,\"color\":\"blue\"}", second.get(1, TimeUnit.SECONDS).getData().content());
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
