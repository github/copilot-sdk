/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.events.AbstractSessionEvent;
import com.github.copilot.sdk.events.AbortEvent;
import com.github.copilot.sdk.events.AssistantMessageDeltaEvent;
import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.events.SessionIdleEvent;
import com.github.copilot.sdk.events.SessionStartEvent;
import com.github.copilot.sdk.events.ToolExecutionStartEvent;
import com.github.copilot.sdk.events.UserMessageEvent;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SystemMessageConfig;

/**
 * Tests for CopilotSession.
 *
 * <p>
 * These tests use the shared CapiProxy infrastructure for deterministic API
 * response replay. Snapshots are stored in test/snapshots/session/.
 * </p>
 */
public class CopilotSessionTest {

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
    void testCreateAndDestroySession() throws Exception {
        ctx.configureForTest("session", "should_receive_session_events");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setModel("fake-test-model")).get();

            assertNotNull(session.getSessionId());
            assertTrue(session.getSessionId().matches("^[a-f0-9-]+$"));

            List<AbstractSessionEvent> messages = session.getMessages().get();
            assertFalse(messages.isEmpty());
            assertTrue(messages.get(0) instanceof SessionStartEvent);

            session.close();

            // Session should no longer be accessible
            try {
                session.getMessages().get();
                fail("Expected exception for closed session");
            } catch (Exception e) {
                assertTrue(e.getMessage().toLowerCase().contains("not found")
                        || e.getCause().getMessage().toLowerCase().contains("not found"));
            }
        }
    }

    @Test
    void testStatefulConversation() throws Exception {
        ctx.configureForTest("session", "should_have_stateful_conversation");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();

            AssistantMessageEvent response1 = session.sendAndWait(new MessageOptions().setPrompt("What is 1+1?"), 60000)
                    .get(90, TimeUnit.SECONDS);

            assertNotNull(response1);
            assertTrue(response1.getData().getContent().contains("2"),
                    "Response should contain 2: " + response1.getData().getContent());

            AssistantMessageEvent response2 = session
                    .sendAndWait(new MessageOptions().setPrompt("Now if you double that, what do you get?"), 60000)
                    .get(90, TimeUnit.SECONDS);

            assertNotNull(response2);
            assertTrue(response2.getData().getContent().contains("4"),
                    "Response should contain 4: " + response2.getData().getContent());

            session.close();
        }
    }

    @Test
    void testReceiveSessionEvents() throws Exception {
        ctx.configureForTest("session", "should_receive_session_events");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();

            List<AbstractSessionEvent> receivedEvents = new ArrayList<>();
            CompletableFuture<Void> idleReceived = new CompletableFuture<>();

            session.on(evt -> {
                receivedEvents.add(evt);
                if (evt instanceof SessionIdleEvent) {
                    idleReceived.complete(null);
                }
            });

            session.send(new MessageOptions().setPrompt("What is 100+200?")).get();

            idleReceived.get(60, TimeUnit.SECONDS);

            assertFalse(receivedEvents.isEmpty());
            assertTrue(receivedEvents.stream().anyMatch(e -> e instanceof UserMessageEvent));
            assertTrue(receivedEvents.stream().anyMatch(e -> e instanceof AssistantMessageEvent));
            assertTrue(receivedEvents.stream().anyMatch(e -> e instanceof SessionIdleEvent));

            // Find the assistant message
            AssistantMessageEvent assistantMsg = receivedEvents.stream().filter(e -> e instanceof AssistantMessageEvent)
                    .map(e -> (AssistantMessageEvent) e).findFirst().orElse(null);

            assertNotNull(assistantMsg);
            assertTrue(assistantMsg.getData().getContent().contains("300"),
                    "Response should contain 300: " + assistantMsg.getData().getContent());

            session.close();
        }
    }

    @Test
    void testSendReturnsImmediately() throws Exception {
        ctx.configureForTest("session", "send_returns_immediately_while_events_stream_in_background");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();

            List<String> events = new ArrayList<>();
            AtomicReference<AssistantMessageEvent> lastMessage = new AtomicReference<>();
            CompletableFuture<Void> done = new CompletableFuture<>();

            session.on(evt -> {
                events.add(evt.getType());
                if (evt instanceof AssistantMessageEvent msg) {
                    lastMessage.set(msg);
                } else if (evt instanceof SessionIdleEvent) {
                    done.complete(null);
                }
            });

            // Use a slow command so we can verify send() returns before completion
            session.send(new MessageOptions().setPrompt("Run 'sleep 2 && echo done'")).get();

            // At this point, we might not have received session.idle yet
            // The event handling happens asynchronously

            // Wait for completion
            done.get(60, TimeUnit.SECONDS);

            assertTrue(events.contains("session.idle"));
            assertTrue(events.contains("assistant.message"));
            assertNotNull(lastMessage.get());
            assertTrue(lastMessage.get().getData().getContent().contains("done"),
                    "Response should contain done: " + lastMessage.get().getData().getContent());

            session.close();
        }
    }

    @Test
    void testSendAndWaitBlocksUntilIdle() throws Exception {
        ctx.configureForTest("session", "sendandwait_blocks_until_session_idle_and_returns_final_assistant_message");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();

            List<String> events = new ArrayList<>();
            session.on(evt -> events.add(evt.getType()));

            AssistantMessageEvent response = session.sendAndWait(new MessageOptions().setPrompt("What is 2+2?")).get(60,
                    TimeUnit.SECONDS);

            assertNotNull(response);
            assertEquals("assistant.message", response.getType());
            assertTrue(response.getData().getContent().contains("4"),
                    "Response should contain 4: " + response.getData().getContent());
            assertTrue(events.contains("session.idle"));
            assertTrue(events.contains("assistant.message"));

            session.close();
        }
    }

    @Test
    void testResumeSessionWithSameClient() throws Exception {
        ctx.configureForTest("session", "should_resume_a_session_using_the_same_client");

        try (CopilotClient client = ctx.createClient()) {
            // Create initial session
            CopilotSession session1 = client.createSession().get();
            String sessionId = session1.getSessionId();

            AssistantMessageEvent answer = session1.sendAndWait(new MessageOptions().setPrompt("What is 1+1?")).get(60,
                    TimeUnit.SECONDS);
            assertNotNull(answer);
            assertTrue(answer.getData().getContent().contains("2"),
                    "Response should contain 2: " + answer.getData().getContent());

            // Resume using the same client
            CopilotSession session2 = client.resumeSession(sessionId).get();

            assertEquals(sessionId, session2.getSessionId());

            // Verify resumed session has the previous messages
            List<AbstractSessionEvent> messages = session2.getMessages().get(60, TimeUnit.SECONDS);
            boolean hasAssistantMessage = messages.stream().filter(m -> m instanceof AssistantMessageEvent)
                    .map(m -> (AssistantMessageEvent) m).anyMatch(m -> m.getData().getContent().contains("2"));
            assertTrue(hasAssistantMessage, "Should find previous assistant message containing 2");

            session2.close();
        }
    }

    @Test
    void testResumeSessionWithNewClient() throws Exception {
        ctx.configureForTest("session", "should_resume_a_session_using_a_new_client");

        String sessionId;

        // First client - create session
        try (CopilotClient client1 = ctx.createClient()) {
            CopilotSession session1 = client1.createSession().get();
            sessionId = session1.getSessionId();

            AssistantMessageEvent answer = session1.sendAndWait(new MessageOptions().setPrompt("What is 1+1?")).get(60,
                    TimeUnit.SECONDS);
            assertNotNull(answer);
            assertTrue(answer.getData().getContent().contains("2"),
                    "Response should contain 2: " + answer.getData().getContent());
        }

        // Second client - resume session
        try (CopilotClient client2 = ctx.createClient()) {
            CopilotSession session2 = client2.resumeSession(sessionId).get();

            assertEquals(sessionId, session2.getSessionId());

            // When resuming with a new client, validate messages contain expected types
            List<AbstractSessionEvent> messages = session2.getMessages().get(60, TimeUnit.SECONDS);
            assertTrue(messages.stream().anyMatch(m -> m instanceof UserMessageEvent),
                    "Should contain user.message event");
            assertTrue(messages.stream().anyMatch(m -> "session.resume".equals(m.getType())),
                    "Should contain session.resume event");

            session2.close();
        }
    }

    @Test
    void testSessionWithAppendedSystemMessage() throws Exception {
        ctx.configureForTest("session", "should_create_a_session_with_appended_systemmessage_config");

        try (CopilotClient client = ctx.createClient()) {
            String systemMessageSuffix = "End each response with the phrase 'Have a nice day!'";
            SessionConfig config = new SessionConfig().setSystemMessage(
                    new SystemMessageConfig().setContent(systemMessageSuffix).setMode(SystemMessageMode.APPEND));

            CopilotSession session = client.createSession(config).get();

            assertNotNull(session.getSessionId());

            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions().setPrompt("What is your full name?")).get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            assertTrue(response.getData().getContent().contains("GitHub"),
                    "Response should contain GitHub: " + response.getData().getContent());
            assertTrue(response.getData().getContent().contains("Have a nice day!"),
                    "Response should end with 'Have a nice day!': " + response.getData().getContent());
            session.close();
        }
    }

    @Test
    void testSessionWithReplacedSystemMessage() throws Exception {
        ctx.configureForTest("session", "should_create_a_session_with_replaced_systemmessage_config");

        try (CopilotClient client = ctx.createClient()) {
            String testSystemMessage = "You are an assistant called Testy McTestface. Reply succinctly.";
            SessionConfig config = new SessionConfig().setSystemMessage(
                    new SystemMessageConfig().setContent(testSystemMessage).setMode(SystemMessageMode.REPLACE));

            CopilotSession session = client.createSession(config).get();

            assertNotNull(session.getSessionId());

            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions().setPrompt("What is your full name?")).get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            assertTrue(response.getData().getContent().contains("Testy McTestface"),
                    "Response should contain 'Testy McTestface': " + response.getData().getContent());
            session.close();
        }
    }

    @Test
    void testSessionWithStreamingEnabled() throws Exception {
        ctx.configureForTest("session", "should_receive_streaming_delta_events_when_streaming_is_enabled");

        try (CopilotClient client = ctx.createClient()) {
            SessionConfig config = new SessionConfig().setStreaming(true);

            CopilotSession session = client.createSession(config).get();

            List<AbstractSessionEvent> receivedEvents = new ArrayList<>();
            CompletableFuture<Void> idleReceived = new CompletableFuture<>();

            session.on(evt -> {
                receivedEvents.add(evt);
                if (evt instanceof SessionIdleEvent) {
                    idleReceived.complete(null);
                }
            });

            session.send(new MessageOptions().setPrompt("What is 2+2?")).get();

            idleReceived.get(60, TimeUnit.SECONDS);

            // Should have received delta events when streaming is enabled
            boolean hasDeltaEvents = receivedEvents.stream().anyMatch(e -> e instanceof AssistantMessageDeltaEvent);
            assertTrue(hasDeltaEvents, "Should receive streaming delta events when streaming is enabled");

            session.close();
        }
    }

    @Test
    void testAbortSession() throws Exception {
        ctx.configureForTest("session", "should_abort_a_session");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();
            assertNotNull(session.getSessionId());

            // Set up wait for tool execution to start BEFORE sending
            CompletableFuture<ToolExecutionStartEvent> toolStartFuture = new CompletableFuture<>();
            CompletableFuture<SessionIdleEvent> sessionIdleFuture = new CompletableFuture<>();

            session.on(evt -> {
                if (evt instanceof ToolExecutionStartEvent toolStart && !toolStartFuture.isDone()) {
                    toolStartFuture.complete(toolStart);
                } else if (evt instanceof SessionIdleEvent idle && !sessionIdleFuture.isDone()) {
                    sessionIdleFuture.complete(idle);
                }
            });

            // Send a message that will trigger a long-running shell command
            session.send(new MessageOptions()
                    .setPrompt("run the shell command 'sleep 100' (note this works on both bash and PowerShell)")).get();

            // Wait for the tool to start executing
            toolStartFuture.get(60, TimeUnit.SECONDS);

            // Abort the session while the tool is running
            session.abort();

            // Wait for session to become idle after abort
            sessionIdleFuture.get(30, TimeUnit.SECONDS);

            // The session should still be alive and usable after abort
            List<AbstractSessionEvent> messages = session.getMessages().get(60, TimeUnit.SECONDS);
            assertFalse(messages.isEmpty());

            // Verify an abort event exists in messages
            assertTrue(messages.stream().anyMatch(m -> m instanceof AbortEvent),
                    "Expected an abort event in messages");

            // We should be able to send another message
            AssistantMessageEvent answer = session.sendAndWait(new MessageOptions().setPrompt("What is 2+2?")).get(60,
                    TimeUnit.SECONDS);
            assertNotNull(answer);
            assertTrue(answer.getData().getContent().contains("4"),
                    "Response should contain 4: " + answer.getData().getContent());

            session.close();
        }
    }

    @Test
    void testSessionWithAvailableTools() throws Exception {
        ctx.configureForTest("session", "should_create_a_session_with_availabletools");

        try (CopilotClient client = ctx.createClient()) {
            SessionConfig config = new SessionConfig().setAvailableTools(List.of("view", "edit"));

            CopilotSession session = client.createSession(config).get();

            assertNotNull(session.getSessionId());

            AssistantMessageEvent response = session.sendAndWait(new MessageOptions().setPrompt("What is 1+1?")).get(60,
                    TimeUnit.SECONDS);

            assertNotNull(response);
            session.close();
        }
    }

    @Test
    void testSessionWithExcludedTools() throws Exception {
        ctx.configureForTest("session", "should_create_a_session_with_excludedtools");

        try (CopilotClient client = ctx.createClient()) {
            SessionConfig config = new SessionConfig().setExcludedTools(List.of("view"));

            CopilotSession session = client.createSession(config).get();

            assertNotNull(session.getSessionId());

            AssistantMessageEvent response = session.sendAndWait(new MessageOptions().setPrompt("What is 1+1?")).get(60,
                    TimeUnit.SECONDS);

            assertNotNull(response);
            assertTrue(response.getData().getContent().contains("2"),
                    "Response should contain 2: " + response.getData().getContent());
            session.close();
        }
    }
}
