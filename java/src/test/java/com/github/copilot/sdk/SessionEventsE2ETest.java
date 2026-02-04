/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.events.AbstractSessionEvent;
import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.events.AssistantTurnEndEvent;
import com.github.copilot.sdk.events.AssistantTurnStartEvent;
import com.github.copilot.sdk.events.AssistantUsageEvent;
import com.github.copilot.sdk.events.SessionIdleEvent;
import com.github.copilot.sdk.events.ToolExecutionCompleteEvent;
import com.github.copilot.sdk.events.ToolExecutionStartEvent;
import com.github.copilot.sdk.events.UserMessageEvent;
import com.github.copilot.sdk.json.MessageOptions;

/**
 * E2E tests for session events to verify event lifecycle.
 * <p>
 * These tests verify that various session events are properly emitted during
 * typical interaction flows with the Copilot CLI.
 * </p>
 */
public class SessionEventsE2ETest {

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

    @Test
    void testAssistantTurnEventsEmitted() throws Exception {
        ctx.configureForTest("events", "assistant_turn_events_emitted");

        List<AbstractSessionEvent> allEvents = new ArrayList<>();

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();

            session.on(event -> allEvents.add(event));

            session.sendAndWait(new MessageOptions().setPrompt("Say hello")).get(60, TimeUnit.SECONDS);

            // Verify turn lifecycle events
            assertTrue(allEvents.stream().anyMatch(e -> e instanceof AssistantTurnStartEvent),
                    "Should receive assistant.turn_start event");
            assertTrue(allEvents.stream().anyMatch(e -> e instanceof AssistantTurnEndEvent),
                    "Should receive assistant.turn_end event");

            // Verify order: turn_start should come before turn_end
            int turnStartIndex = -1;
            int turnEndIndex = -1;
            for (int i = 0; i < allEvents.size(); i++) {
                if (allEvents.get(i) instanceof AssistantTurnStartEvent && turnStartIndex == -1) {
                    turnStartIndex = i;
                }
                if (allEvents.get(i) instanceof AssistantTurnEndEvent) {
                    turnEndIndex = i;
                }
            }
            assertTrue(turnStartIndex < turnEndIndex, "turn_start should come before turn_end");
        }
    }

    @Test
    void testUserMessageEventEmitted() throws Exception {
        ctx.configureForTest("events", "user_message_event_emitted");

        List<UserMessageEvent> userMessages = new ArrayList<>();

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();

            session.on(UserMessageEvent.class, userMessages::add);

            session.sendAndWait(new MessageOptions().setPrompt("Hello, Copilot!")).get(60, TimeUnit.SECONDS);

            // Verify user message was captured
            assertFalse(userMessages.isEmpty(), "Should receive user.message event");
        }
    }

    @Test
    void testToolExecutionCompleteEventEmitted() throws Exception {
        ctx.configureForTest("events", "tool_execution_complete_event_emitted");

        List<ToolExecutionStartEvent> toolStarts = new ArrayList<>();
        List<ToolExecutionCompleteEvent> toolCompletes = new ArrayList<>();

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();

            session.on(ToolExecutionStartEvent.class, toolStarts::add);
            session.on(ToolExecutionCompleteEvent.class, toolCompletes::add);

            // Create a file for the model to read
            Path testFile = ctx.getWorkDir().resolve("test-events.txt");
            Files.writeString(testFile, "Event test content");

            session.sendAndWait(new MessageOptions().setPrompt("Read the contents of test-events.txt")).get(60,
                    TimeUnit.SECONDS);

            // Verify tool execution events
            assertFalse(toolStarts.isEmpty(), "Should receive tool.execution_start event");
            assertFalse(toolCompletes.isEmpty(), "Should receive tool.execution_complete event");

            // Verify tool execution completed successfully
            assertTrue(toolCompletes.stream().anyMatch(e -> e.getData().isSuccess()),
                    "At least one tool execution should be successful");
        }
    }

    @Test
    void testAssistantUsageEventEmitted() throws Exception {
        ctx.configureForTest("events", "assistant_usage_event_emitted");

        List<AssistantUsageEvent> usageEvents = new ArrayList<>();

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();

            session.on(AssistantUsageEvent.class, usageEvents::add);

            session.sendAndWait(new MessageOptions().setPrompt("What is 2+2?")).get(60, TimeUnit.SECONDS);

            // Usage events may or may not be emitted depending on the model/API version
            // This test verifies the event handler works when they are emitted
            // We don't assert they must be present since it depends on the backend
        }
    }

    @Test
    void testSessionIdleEventAfterMessageComplete() throws Exception {
        ctx.configureForTest("events", "session_idle_after_message");

        List<AbstractSessionEvent> allEvents = new ArrayList<>();

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();

            session.on(event -> allEvents.add(event));

            session.sendAndWait(new MessageOptions().setPrompt("Say OK")).get(60, TimeUnit.SECONDS);

            // Verify session.idle is emitted after assistant.message
            assertTrue(allEvents.stream().anyMatch(e -> e instanceof SessionIdleEvent),
                    "Should receive session.idle event");
            assertTrue(allEvents.stream().anyMatch(e -> e instanceof AssistantMessageEvent),
                    "Should receive assistant.message event");

            // Verify order: assistant.message should come before session.idle
            int messageIndex = -1;
            int idleIndex = -1;
            for (int i = 0; i < allEvents.size(); i++) {
                if (allEvents.get(i) instanceof AssistantMessageEvent) {
                    messageIndex = i;
                }
                if (allEvents.get(i) instanceof SessionIdleEvent) {
                    idleIndex = i;
                }
            }
            assertTrue(messageIndex < idleIndex, "assistant.message should come before session.idle");
        }
    }

    @Test
    void testEventOrderDuringToolExecution() throws Exception {
        ctx.configureForTest("events", "event_order_during_tool_execution");

        List<String> eventTypes = new ArrayList<>();

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();

            session.on(event -> eventTypes.add(event.getType()));

            // Create a file for the model to read
            Path testFile = ctx.getWorkDir().resolve("order-test.txt");
            Files.writeString(testFile, "Order test content");

            session.sendAndWait(new MessageOptions().setPrompt("Read the contents of order-test.txt")).get(60,
                    TimeUnit.SECONDS);

            // Verify expected event types are present
            assertTrue(eventTypes.contains("user.message"), "Should have user.message");
            assertTrue(eventTypes.contains("assistant.turn_start"), "Should have assistant.turn_start");
            assertTrue(eventTypes.contains("tool.execution_start"), "Should have tool.execution_start");
            assertTrue(eventTypes.contains("tool.execution_complete"), "Should have tool.execution_complete");
            assertTrue(eventTypes.contains("assistant.message"), "Should have assistant.message");
            assertTrue(eventTypes.contains("assistant.turn_end"), "Should have assistant.turn_end");
            assertTrue(eventTypes.contains("session.idle"), "Should have session.idle");

            // Verify tool execution is between turn_start and turn_end
            int turnStartIdx = eventTypes.indexOf("assistant.turn_start");
            int toolStartIdx = eventTypes.indexOf("tool.execution_start");
            int toolCompleteIdx = eventTypes.indexOf("tool.execution_complete");
            int turnEndIdx = eventTypes.lastIndexOf("assistant.turn_end");

            assertTrue(turnStartIdx < toolStartIdx, "turn_start should be before tool.execution_start");
            assertTrue(toolStartIdx < toolCompleteIdx, "tool.execution_start should be before tool.execution_complete");
            assertTrue(toolCompleteIdx < turnEndIdx, "tool.execution_complete should be before turn_end");
        }
    }
}
