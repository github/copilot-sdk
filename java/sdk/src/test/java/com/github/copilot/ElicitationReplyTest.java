/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.generated.SessionEvent;
import com.github.copilot.rpc.ElicitationHandler;
import com.github.copilot.rpc.ElicitationResult;
import com.github.copilot.rpc.ElicitationResultAction;

/**
 * Verifies that an elicitation request routed to this client's handler is
 * always answered: with the handler's result when it can be sent, and with
 * {@code cancel} when it cannot.
 */
class ElicitationReplyTest {

    private static final String SESSION_ID = "elicitation-reply-session";

    private final BlockingQueue<ObjectNode> replies = new LinkedBlockingQueue<>();
    private JsonRpcClient rpc;
    private CopilotSession session;

    @BeforeEach
    void setup() {
        rpc = mock(JsonRpcClient.class);
        when(rpc.invoke(eq("session.ui.handlePendingElicitation"), any(), any())).thenAnswer(invocation -> {
            replies.add(invocation.getArgument(1));
            return CompletableFuture.completedFuture(null);
        });
        session = new CopilotSession(SESSION_ID, rpc);
    }

    @Test
    void acceptedResultIsSent() throws Exception {
        ObjectNode reply = reply("accepted", context -> CompletableFuture.completedFuture(
                new ElicitationResult().setAction(ElicitationResultAction.ACCEPT).setContent(Map.of("name", "Ada"))));

        assertEquals("accept", reply.path("result").path("action").asText());
        assertEquals("Ada", reply.path("result").path("content").path("name").asText());
    }

    @Test
    void nullResultIsCancelled() throws Exception {
        ObjectNode reply = reply("null-result", context -> CompletableFuture.completedFuture(null));

        assertEquals("cancel", reply.path("result").path("action").asText());
    }

    @Test
    void resultWhoseContentCannotBeSerializedIsCancelled() throws Exception {
        Map<String, Object> content = new HashMap<>();
        content.put("value", new UnserializableValue());

        ObjectNode reply = reply("unserializable-content", context -> CompletableFuture.completedFuture(
                new ElicitationResult().setAction(ElicitationResultAction.ACCEPT).setContent(content)));

        assertEquals("cancel", reply.path("result").path("action").asText());
        assertTrue(reply.path("result").path("content").isMissingNode(), "cancel reply must not carry content");
    }

    @Test
    void handlerThrowingAnErrorIsCancelled() throws Exception {
        ObjectNode reply = reply("throws-error", context -> {
            throw new AssertionError("handler failed");
        });

        assertEquals("cancel", reply.path("result").path("action").asText());
    }

    @Test
    void resultRpcThatCompletesExceptionallyIsCancelled() throws Exception {
        var resultResponse = new CompletableFuture<Object>();
        when(rpc.invoke(eq("session.ui.handlePendingElicitation"), any(), any())).thenAnswer(invocation -> {
            ObjectNode params = invocation.getArgument(1);
            replies.add(params);
            return "accept".equals(params.path("result").path("action").asText())
                    ? resultResponse
                    : CompletableFuture.completedFuture(null);
        });

        dispatch("failed-result-rpc", context -> CompletableFuture.completedFuture(
                new ElicitationResult().setAction(ElicitationResultAction.ACCEPT).setContent(Map.of("name", "Ada"))));
        ObjectNode result = awaitReply("failed-result-rpc");
        assertEquals("accept", result.path("result").path("action").asText());

        resultResponse.completeExceptionally(new IllegalStateException("result RPC failed"));

        ObjectNode cancel = awaitReply("failed-result-rpc");
        assertEquals("cancel", cancel.path("result").path("action").asText());
        assertTrue(cancel.path("result").path("content").isMissingNode(), "cancel reply must not carry content");
    }

    private ObjectNode reply(String requestId, ElicitationHandler handler) throws Exception {
        dispatch(requestId, handler);
        return awaitReply(requestId);
    }

    private void dispatch(String requestId, ElicitationHandler handler) throws Exception {
        session.registerElicitationHandler(handler);
        SessionEvent event = JsonRpcClient.getObjectMapper().convertValue(
                Map.of("type", "elicitation.requested", "data", Map.of("requestId", requestId, "message", "Name?")),
                SessionEvent.class);
        Method dispatchMethod = CopilotSession.class.getDeclaredMethod("dispatchEvent", SessionEvent.class);
        dispatchMethod.setAccessible(true);
        dispatchMethod.invoke(session, event);
    }

    private ObjectNode awaitReply(String requestId) throws Exception {
        ObjectNode reply = replies.poll(5, TimeUnit.SECONDS);
        assertNotNull(reply, "No reply for " + requestId);
        assertEquals(requestId, reply.path("requestId").asText());
        assertNull(replies.poll(200, TimeUnit.MILLISECONDS), "More than one reply for " + requestId);
        return reply;
    }

    /**
     * A value Jackson cannot serialize, because its only property fails to read.
     */
    public static class UnserializableValue {
        public String getValue() {
            throw new IllegalStateException("not serializable");
        }
    }
}
