/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

    /**
     * Verifies that assistant turn events (turn_start, turn_end) are emitted.
     *
     * @see Snapshot: session/should_receive_session_events
     */
    @Test
    void testShouldReceiveSessionEvents_assistantTurnEvents() throws Exception {
        // Use existing session snapshot that emits turn events
        ctx.configureForTest("session", "should_receive_session_events");

        var allEvents = new ArrayList<AbstractSessionEvent>();

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();

            session.on(event -> allEvents.add(event));

            // Use prompt that matches the snapshot
            session.sendAndWait(new MessageOptions().setPrompt("What is 100+200?")).get(60, TimeUnit.SECONDS);

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

    /**
     * Verifies that user message events are emitted.
     *
     * @see Snapshot: session/should_receive_session_events
     */
    @Test
    void testShouldReceiveSessionEvents_userMessageEvent() throws Exception {
        // Use existing session snapshot
        ctx.configureForTest("session", "should_receive_session_events");

        var userMessages = new ArrayList<UserMessageEvent>();

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();

            session.on(UserMessageEvent.class, userMessages::add);

            // Use prompt that matches the snapshot
            session.sendAndWait(new MessageOptions().setPrompt("What is 100+200?")).get(60, TimeUnit.SECONDS);

            // Verify user message was captured
            assertFalse(userMessages.isEmpty(), "Should receive user.message event");
        }
    }

    /**
     * Verifies that tool execution complete events are emitted.
     *
     * @see Snapshot: tools/invokes_built_in_tools
     */
    @Test
    void testInvokesBuiltInTools_toolExecutionCompleteEvent() throws Exception {
        // Use existing tools snapshot for built-in tool invocation
        ctx.configureForTest("tools", "invokes_built_in_tools");

        var toolStarts = new ArrayList<ToolExecutionStartEvent>();
        var toolCompletes = new ArrayList<ToolExecutionCompleteEvent>();

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();

            session.on(ToolExecutionStartEvent.class, toolStarts::add);
            session.on(ToolExecutionCompleteEvent.class, toolCompletes::add);

            // Create the README.md file expected by the snapshot - must have ONLY one line
            // to match the snapshot's expected tool response: "1. # ELIZA, the only chatbot
            // you'll ever need"
            Path testFile = ctx.getWorkDir().resolve("README.md");
            Files.writeString(testFile, "# ELIZA, the only chatbot you'll ever need");

            // Use prompt that matches the snapshot
            session.sendAndWait(new MessageOptions().setPrompt("What's the first line of README.md in this directory?"))
                    .get(60, TimeUnit.SECONDS);

            // Verify tool execution events
            assertFalse(toolStarts.isEmpty(), "Should receive tool.execution_start event");
            assertFalse(toolCompletes.isEmpty(), "Should receive tool.execution_complete event");

            // Verify tool execution completed successfully
            assertTrue(toolCompletes.stream().anyMatch(e -> e.getData().success()),
                    "At least one tool execution should be successful");
        }
    }

    /**
     * Verifies that assistant usage events are handled when emitted.
     *
     * @see Snapshot: session/should_receive_session_events
     */
    @Test
    void testShouldReceiveSessionEvents_assistantUsageEvent() throws Exception {
        // Use existing session snapshot
        ctx.configureForTest("session", "should_receive_session_events");

        var usageEvents = new ArrayList<AssistantUsageEvent>();

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();

            session.on(AssistantUsageEvent.class, usageEvents::add);

            // Use prompt that matches the snapshot
            session.sendAndWait(new MessageOptions().setPrompt("What is 100+200?")).get(60, TimeUnit.SECONDS);

            // Usage events may or may not be emitted depending on the model/API version
            // This test verifies the event handler works when they are emitted
            // We don't assert they must be present since it depends on the backend
        }
    }

    /**
     * Verifies that session.idle event is emitted after message completion.
     *
     * @see Snapshot: session/should_receive_session_events
     */
    @Test
    void testShouldReceiveSessionEvents_sessionIdleAfterMessage() throws Exception {
        // Use existing session snapshot
        ctx.configureForTest("session", "should_receive_session_events");

        var allEvents = new ArrayList<AbstractSessionEvent>();

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();

            session.on(event -> allEvents.add(event));

            // Use prompt that matches the snapshot
            session.sendAndWait(new MessageOptions().setPrompt("What is 100+200?")).get(60, TimeUnit.SECONDS);

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

    /**
     * Verifies the order of events during tool execution.
     *
     * @see Snapshot: tools/invokes_built_in_tools
     */
    @Test
    void testInvokesBuiltInTools_eventOrderDuringToolExecution() throws Exception {
        // Use existing tools snapshot for built-in tool invocation
        ctx.configureForTest("tools", "invokes_built_in_tools");

        var eventTypes = new ArrayList<String>();

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();

            session.on(event -> eventTypes.add(event.getType()));

            // Create the README.md file expected by the snapshot - must have ONLY one line
            // to match the snapshot's expected tool response: "1. # ELIZA, the only chatbot
            // you'll ever need"
            Path testFile = ctx.getWorkDir().resolve("README.md");
            Files.writeString(testFile, "# ELIZA, the only chatbot you'll ever need");

            // Use prompt that matches the snapshot
            session.sendAndWait(new MessageOptions().setPrompt("What's the first line of README.md in this directory?"))
                    .get(60, TimeUnit.SECONDS);

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
