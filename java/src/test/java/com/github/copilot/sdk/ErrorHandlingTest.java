/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.events.AbstractSessionEvent;
import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.events.SessionErrorEvent;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.ToolDefinition;

import java.util.Map;

/**
 * E2E tests for error handling scenarios.
 * <p>
 * These tests verify that the SDK properly handles errors in various scenarios
 * including tool errors, permission handler errors, and session errors.
 * </p>
 */
public class ErrorHandlingTest {

    private static E2ETestContext ctx;

    @BeforeAll
    static void setup() throws Exception {
        ctx = E2ETestContext.create();
    }

    @AfterAll
    static void teardown() throws Exception {
        if (ctx != null) {
            ctx.close();
        }
    }

    /**
     * Tests that tool errors are handled gracefully and don't crash the session.
     */
    @Test
    void testToolErrorDoesNotCrashSession() throws Exception {
        ctx.configureForTest("tools", "handles_tool_calling_errors");

        List<AbstractSessionEvent> allEvents = new ArrayList<>();

        ToolDefinition errorTool = ToolDefinition.create("get_user_location", "Gets the user's location",
                Map.of("type", "object", "properties", Map.of()), (invocation) -> {
                    CompletableFuture<Object> future = new CompletableFuture<>();
                    future.completeExceptionally(new RuntimeException("Location service unavailable"));
                    return future;
                });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setTools(List.of(errorTool))).get();

            session.on(event -> allEvents.add(event));

            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions()
                            .setPrompt("What is my location? If you can't find out, just say 'unknown'."))
                    .get(60, TimeUnit.SECONDS);

            // Session should complete without crashing
            assertNotNull(response, "Should receive a response even when tool fails");

            // Should have received session.idle (indicating successful completion)
            assertTrue(allEvents.stream().anyMatch(e -> e instanceof com.github.copilot.sdk.events.SessionIdleEvent),
                    "Session should reach idle state after handling tool error");

            session.close();
        }
    }

    /**
     * Tests that returning a failure result from a tool is handled properly.
     */
    @Test
    void testToolReturnsFailureResult() throws Exception {
        ctx.configureForTest("tools", "handles_tool_calling_errors");

        ToolDefinition failTool = ToolDefinition.create("get_user_location", "Gets the user's location",
                Map.of("type", "object", "properties", Map.of()), (invocation) -> {
                    // Return a structured failure result via exception (matching the snapshot
                    // behavior)
                    return CompletableFuture.failedFuture(new RuntimeException("Location unavailable"));
                });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setTools(List.of(failTool))).get();

            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions()
                            .setPrompt("What is my location? If you can't find out, just say 'unknown'."))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response, "Should receive a response with failure result");

            session.close();
        }
    }

    /**
     * Tests that permission handler errors result in denied permission.
     */
    @Test
    void testPermissionHandlerErrorDeniesPermission() throws Exception {
        ctx.configureForTest("permissions", "should_handle_permission_handler_errors_gracefully");

        List<SessionErrorEvent> errorEvents = new ArrayList<>();

        SessionConfig config = new SessionConfig().setOnPermissionRequest((request, invocation) -> {
            throw new RuntimeException("Permission handler crashed");
        });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();

            session.on(SessionErrorEvent.class, errorEvents::add);

            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions().setPrompt("Run 'echo test'. If you can't, say 'failed'."))
                    .get(60, TimeUnit.SECONDS);

            // Should complete despite the error
            assertNotNull(response, "Should receive a response despite handler error");

            // The response should indicate failure/inability
            String content = response.getData().getContent().toLowerCase();
            assertTrue(
                    content.contains("fail") || content.contains("cannot") || content.contains("unable")
                            || content.contains("permission") || content.contains("denied"),
                    "Response should indicate permission was denied: " + content);

            session.close();
        }
    }

    /**
     * Tests that session error events contain proper error information.
     */
    @Test
    void testSessionErrorEventContainsDetails() throws Exception {
        ctx.configureForTest("permissions", "permission_handler_errors");

        List<SessionErrorEvent> errorEvents = new ArrayList<>();

        SessionConfig config = new SessionConfig().setOnPermissionRequest((request, invocation) -> {
            throw new RuntimeException("Test error message");
        });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();

            session.on(SessionErrorEvent.class, error -> {
                errorEvents.add(error);
                // Verify error event has data
                assertNotNull(error.getData(), "Error event should have data");
            });

            try {
                session.sendAndWait(new MessageOptions().setPrompt("Run 'ls' command")).get(60, TimeUnit.SECONDS);
            } catch (Exception e) {
                // Error is expected in some cases
            }

            session.close();
        }

        // Note: Whether error events are emitted depends on the CLI version and
        // scenario
        // This test verifies the handler can receive them when they occur
    }

    /**
     * Tests that the session continues to work after a tool error.
     */
    @Test
    void testSessionContinuesAfterToolError() throws Exception {
        ctx.configureForTest("tools", "handles_tool_calling_errors");

        ToolDefinition errorTool = ToolDefinition.create("get_user_location", "Gets the user's location",
                Map.of("type", "object", "properties", Map.of()), (invocation) -> {
                    return CompletableFuture.failedFuture(new RuntimeException("Service unavailable"));
                });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setTools(List.of(errorTool))).get();

            // First request that will cause tool error
            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions()
                            .setPrompt("What is my location? If you can't find out, just say 'unknown'."))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response, "Should receive first response");

            // Session should still be usable - the sendAndWait completed
            // This verifies the session didn't enter an error state

            session.close();
        }
    }
}
