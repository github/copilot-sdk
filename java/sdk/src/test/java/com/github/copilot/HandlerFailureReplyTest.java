/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.generated.CommandExecuteEvent;
import com.github.copilot.generated.ExternalToolRequestedEvent;
import com.github.copilot.generated.SessionEvent;
import com.github.copilot.rpc.CommandDefinition;
import com.github.copilot.rpc.CommandHandler;
import com.github.copilot.rpc.ToolDefinition;
import com.github.copilot.rpc.ToolHandler;
import com.github.copilot.rpc.ToolResultObject;

/**
 * Verifies the error text sent back to the runtime when a slash-command or tool
 * handler fails: it is the handler's own exception message, whether the handler
 * throws before returning its future or the future fails later.
 */
class HandlerFailureReplyTest {

    private static final String SESSION_ID = "handler-failure-session";

    private final BlockingQueue<ObjectNode> commandReplies = new LinkedBlockingQueue<>();
    private final BlockingQueue<ObjectNode> toolReplies = new LinkedBlockingQueue<>();
    private CopilotSession session;

    @BeforeEach
    void setup() {
        var rpc = mock(JsonRpcClient.class);
        when(rpc.invoke(eq("session.commands.handlePendingCommand"), any(), any())).thenAnswer(invocation -> {
            commandReplies.add(invocation.getArgument(1));
            return CompletableFuture.completedFuture(null);
        });
        when(rpc.invoke(eq("session.tools.handlePendingToolCall"), any(), any())).thenAnswer(invocation -> {
            toolReplies.add(invocation.getArgument(1));
            return CompletableFuture.completedFuture(null);
        });
        session = new CopilotSession(SESSION_ID, rpc);
    }

    @Test
    void failedCommandHandlerRepliesWithItsOwnMessage() throws Exception {
        CommandHandler throwing = context -> {
            throw new IllegalStateException("boom");
        };
        CommandHandler failed = context -> CompletableFuture.failedFuture(new IllegalStateException("boom"));
        CommandHandler async = context -> CompletableFuture.runAsync(() -> {
            throw new IllegalStateException("boom");
        });
        CommandHandler asyncWithoutMessage = context -> CompletableFuture.runAsync(() -> {
            throw new IllegalStateException();
        });
        CommandHandler throwingEmptyMessage = context -> {
            throw new IllegalStateException("");
        };
        CommandHandler failedEmptyMessage = context -> CompletableFuture.failedFuture(new IllegalStateException(""));
        CommandHandler asyncEmptyMessage = context -> CompletableFuture.runAsync(() -> {
            throw new IllegalStateException("");
        });
        session.registerCommands(List.of(command("throws", throwing), command("failed", failed),
                command("async", async), command("async-no-message", asyncWithoutMessage),
                command("throws-empty-message", throwingEmptyMessage),
                command("failed-empty-message", failedEmptyMessage),
                command("async-empty-message", asyncEmptyMessage)));

        assertEquals("boom", commandError("throws"));
        assertEquals("boom", commandError("failed"));
        assertEquals("boom", commandError("async"));
        assertEquals("java.lang.IllegalStateException", commandError("async-no-message"));
        assertEquals("java.lang.IllegalStateException: ", commandError("failed-empty-message"));
        assertEquals("java.lang.IllegalStateException: ", commandError("async-empty-message"));
        assertEquals("java.lang.IllegalStateException: ", commandError("throws-empty-message"));
    }

    @Test
    void failedToolHandlerRepliesWithItsOwnMessage() throws Exception {
        ToolHandler throwing = invocation -> {
            throw new IllegalStateException("boom");
        };
        ToolHandler failed = invocation -> CompletableFuture.failedFuture(new IllegalStateException("boom"));
        ToolHandler async = invocation -> CompletableFuture.supplyAsync(() -> {
            throw new IllegalStateException("boom");
        });
        ToolHandler asyncWithoutMessage = invocation -> CompletableFuture.supplyAsync(() -> {
            throw new IllegalStateException();
        });
        ToolHandler throwingEmptyMessage = invocation -> {
            throw new IllegalStateException("");
        };
        ToolHandler failedEmptyMessage = invocation -> CompletableFuture.failedFuture(new IllegalStateException(""));
        ToolHandler asyncEmptyMessage = invocation -> CompletableFuture.supplyAsync(() -> {
            throw new IllegalStateException("");
        });
        session.registerTools(List.of(tool("throws", throwing), tool("failed", failed), tool("async", async),
                tool("async-no-message", asyncWithoutMessage), tool("throws-empty-message", throwingEmptyMessage),
                tool("failed-empty-message", failedEmptyMessage), tool("async-empty-message", asyncEmptyMessage)));

        assertEquals("boom", toolError("throws"));
        assertEquals("boom", toolError("failed"));
        assertEquals("boom", toolError("async"));
        assertEquals("java.lang.IllegalStateException", toolError("async-no-message"));
        assertEquals("java.lang.IllegalStateException: ", toolError("failed-empty-message"));
        assertEquals("java.lang.IllegalStateException: ", toolError("async-empty-message"));
        assertEquals("java.lang.IllegalStateException: ", toolError("throws-empty-message"));
    }

    @Test
    void toolResultThatCannotBeEncodedRepliesWithError() throws Exception {
        ToolHandler encodableTelemetry = invocation -> CompletableFuture
                .completedFuture(new ToolResultObject("success", "ok", null, null, null, Map.of("label", "note")));
        ToolHandler unencodableTelemetry = invocation -> CompletableFuture.completedFuture(
                new ToolResultObject("success", "ok", null, null, null, Map.of("handle", new Object())));
        ToolHandler unencodableResult = invocation -> CompletableFuture.completedFuture(Map.of("handle", new Object()));
        session.registerTools(List.of(tool("encodable-telemetry", encodableTelemetry),
                tool("unencodable-telemetry", unencodableTelemetry), tool("unencodable-result", unencodableResult)));

        assertNull(toolError("encodable-telemetry"));
        String telemetryError = toolError("unencodable-telemetry");
        assertTrue(
                telemetryError.startsWith(
                        "failed to serialize tool telemetry: No serializer found for class java.lang.Object"),
                telemetryError);
        String resultError = toolError("unencodable-result");
        assertTrue(resultError.startsWith("No serializer found for class java.lang.Object"), resultError);
    }

    private String commandError(String name) throws Exception {
        var event = new CommandExecuteEvent();
        event.setData(new CommandExecuteEvent.CommandExecuteEventData("request-" + name, "/" + name, name, ""));
        dispatch(event);
        return replyError(commandReplies, name);
    }

    private String toolError(String name) throws Exception {
        var event = new ExternalToolRequestedEvent();
        event.setData(new ExternalToolRequestedEvent.ExternalToolRequestedEventData("request-" + name, SESSION_ID,
                "tool-call-" + name, name, null, Map.of(), null, null, null));
        dispatch(event);
        return replyError(toolReplies, name);
    }

    private static String replyError(BlockingQueue<ObjectNode> replies, String name) throws Exception {
        ObjectNode reply = replies.poll(5, TimeUnit.SECONDS);
        assertNotNull(reply, "No reply for " + name);
        assertEquals("request-" + name, reply.path("requestId").asText());
        return reply.path("error").asText(null);
    }

    private void dispatch(SessionEvent event) throws Exception {
        Method dispatchMethod = CopilotSession.class.getDeclaredMethod("dispatchEvent", SessionEvent.class);
        dispatchMethod.setAccessible(true);
        dispatchMethod.invoke(session, event);
    }

    private static CommandDefinition command(String name, CommandHandler handler) {
        return new CommandDefinition().setName(name).setHandler(handler);
    }

    private static ToolDefinition tool(String name, ToolHandler handler) {
        return ToolDefinition.create(name, "Fails on purpose", Map.of("type", "object"), handler);
    }
}
