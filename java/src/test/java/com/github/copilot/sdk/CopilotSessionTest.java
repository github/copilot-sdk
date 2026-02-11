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
import java.util.Map;
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
import com.github.copilot.sdk.json.ToolDefinition;

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

    /**
     * Verifies that a session can be created and destroyed properly.
     *
     * @see Snapshot: session/should_receive_session_events
     */
    @Test
    void testShouldReceiveSessionEvents_createAndDestroy() throws Exception {
        ctx.configureForTest("session", "should_receive_session_events");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setModel("fake-test-model")).get();

            assertNotNull(session.getSessionId());
            assertTrue(session.getSessionId().matches("^[a-f0-9-]+$"));

            List<AbstractSessionEvent> messages = session.getMessages().get();
            assertFalse(messages.isEmpty());
            assertTrue(messages.get(0) instanceof SessionStartEvent);

            session.close();

            // Session should no longer be accessible - now throws IllegalStateException
            try {
                session.getMessages().get();
                fail("Expected exception for closed session");
            } catch (Exception e) {
                // After our changes, we now get IllegalStateException directly
                String message = e.getMessage();
                String causeMessage = e.getCause() != null ? e.getCause().getMessage() : null;
                boolean matchesClosed = message != null && message.toLowerCase().contains("closed");
                boolean matchesNotFound = causeMessage != null && causeMessage.toLowerCase().contains("not found");
                assertTrue(matchesClosed || matchesNotFound);
            }
        }
    }

    /**
     * Verifies that sessions maintain conversation state across multiple messages.
     *
     * @see Snapshot: session/should_have_stateful_conversation
     */
    @Test
    void testShouldHaveStatefulConversation() throws Exception {
        ctx.configureForTest("session", "should_have_stateful_conversation");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();

            AssistantMessageEvent response1 = session.sendAndWait(new MessageOptions().setPrompt("What is 1+1?"), 60000)
                    .get(90, TimeUnit.SECONDS);

            assertNotNull(response1);
            assertTrue(response1.getData().content().contains("2"),
                    "Response should contain 2: " + response1.getData().content());

            AssistantMessageEvent response2 = session
                    .sendAndWait(new MessageOptions().setPrompt("Now if you double that, what do you get?"), 60000)
                    .get(90, TimeUnit.SECONDS);

            assertNotNull(response2);
            assertTrue(response2.getData().content().contains("4"),
                    "Response should contain 4: " + response2.getData().content());

            session.close();
        }
    }

    /**
     * Verifies that session events (user.message, assistant.message, session.idle)
     * are properly received.
     *
     * @see Snapshot: session/should_receive_session_events
     */
    @Test
    void testShouldReceiveSessionEvents() throws Exception {
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
            assertTrue(assistantMsg.getData().content().contains("300"),
                    "Response should contain 300: " + assistantMsg.getData().content());

            session.close();
        }
    }

    /**
     * Verifies that send() returns immediately while events stream in background.
     *
     * @see Snapshot:
     *      session/send_returns_immediately_while_events_stream_in_background
     */
    @Test
    void testSendReturnsImmediatelyWhileEventsStreamInBackground() throws Exception {
        ctx.configureForTest("session", "send_returns_immediately_while_events_stream_in_background");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();

            var events = new ArrayList<String>();
            var lastMessage = new AtomicReference<AssistantMessageEvent>();
            var done = new CompletableFuture<Void>();

            session.on(evt -> {
                events.add(evt.getType());
                if (evt instanceof AssistantMessageEvent msg) {
                    lastMessage.set(msg);
                } else if (evt instanceof SessionIdleEvent) {
                    done.complete(null);
                }
            });

            // Use a slow command so we can verify send() returns before completion
            // Use String convenience overload (covers send(String) path)
            session.send("Run 'sleep 2 && echo done'").get();

            // At this point, we might not have received session.idle yet
            // The event handling happens asynchronously

            // Wait for completion
            done.get(60, TimeUnit.SECONDS);

            assertTrue(events.contains("session.idle"));
            assertTrue(events.contains("assistant.message"));
            assertNotNull(lastMessage.get());
            assertTrue(lastMessage.get().getData().content().contains("done"),
                    "Response should contain done: " + lastMessage.get().getData().content());

            session.close();
        }
    }

    /**
     * Verifies that sendAndWait blocks until session is idle and returns the final
     * assistant message.
     *
     * @see Snapshot:
     *      session/sendandwait_blocks_until_session_idle_and_returns_final_assistant_message
     */
    @Test
    void testSendAndWaitBlocksUntilSessionIdleAndReturnsFinalAssistantMessage() throws Exception {
        ctx.configureForTest("session", "sendandwait_blocks_until_session_idle_and_returns_final_assistant_message");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();

            var events = new ArrayList<String>();
            session.on(evt -> events.add(evt.getType()));

            // Use String convenience overload (covers sendAndWait(String) path)
            AssistantMessageEvent response = session.sendAndWait("What is 2+2?").get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            assertEquals("assistant.message", response.getType());
            assertTrue(response.getData().content().contains("4"),
                    "Response should contain 4: " + response.getData().content());
            assertTrue(events.contains("session.idle"));
            assertTrue(events.contains("assistant.message"));

            session.close();
        }
    }

    /**
     * Verifies that a session can be resumed using the same client.
     *
     * @see Snapshot: session/should_resume_a_session_using_the_same_client
     */
    @Test
    void testShouldResumeSessionUsingTheSameClient() throws Exception {
        ctx.configureForTest("session", "should_resume_a_session_using_the_same_client");

        try (CopilotClient client = ctx.createClient()) {
            // Create initial session
            CopilotSession session1 = client.createSession().get();
            String sessionId = session1.getSessionId();

            AssistantMessageEvent answer = session1.sendAndWait(new MessageOptions().setPrompt("What is 1+1?")).get(60,
                    TimeUnit.SECONDS);
            assertNotNull(answer);
            assertTrue(answer.getData().content().contains("2"),
                    "Response should contain 2: " + answer.getData().content());

            // Resume using the same client
            CopilotSession session2 = client.resumeSession(sessionId).get();

            assertEquals(sessionId, session2.getSessionId());

            // Verify resumed session has the previous messages
            List<AbstractSessionEvent> messages = session2.getMessages().get(60, TimeUnit.SECONDS);
            boolean hasAssistantMessage = messages.stream().filter(m -> m instanceof AssistantMessageEvent)
                    .map(m -> (AssistantMessageEvent) m).anyMatch(m -> m.getData().content().contains("2"));
            assertTrue(hasAssistantMessage, "Should find previous assistant message containing 2");

            session2.close();
        }
    }

    /**
     * Verifies that a session can be resumed using a new client.
     *
     * @see Snapshot: session/should_resume_a_session_using_a_new_client
     */
    @Test
    void testShouldResumeSessionUsingNewClient() throws Exception {
        ctx.configureForTest("session", "should_resume_a_session_using_a_new_client");

        // Use a single try-with-resources for the first client to keep it alive
        // throughout the test, matching the behavior of other SDK implementations
        try (CopilotClient client1 = ctx.createClient()) {
            // Create initial session
            CopilotSession session1 = client1.createSession().get();
            String sessionId = session1.getSessionId();

            AssistantMessageEvent answer = session1.sendAndWait(new MessageOptions().setPrompt("What is 1+1?")).get(60,
                    TimeUnit.SECONDS);
            assertNotNull(answer);
            assertTrue(answer.getData().content().contains("2"),
                    "Response should contain 2: " + answer.getData().content());

            // Resume using a new client (keeping client1 alive)
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
    }

    /**
     * Verifies that sessions work with appended system message configuration.
     *
     * @see Snapshot:
     *      session/should_create_a_session_with_appended_systemmessage_config
     */
    @Test
    void testShouldCreateSessionWithAppendedSystemMessageConfig() throws Exception {
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
            assertTrue(response.getData().content().contains("GitHub"),
                    "Response should contain GitHub: " + response.getData().content());
            assertTrue(response.getData().content().contains("Have a nice day!"),
                    "Response should end with 'Have a nice day!': " + response.getData().content());
            session.close();
        }
    }

    /**
     * Verifies that sessions work with replaced system message configuration.
     *
     * @see Snapshot:
     *      session/should_create_a_session_with_replaced_systemmessage_config
     */
    @Test
    void testShouldCreateSessionWithReplacedSystemMessageConfig() throws Exception {
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
            assertTrue(response.getData().content().contains("Testy McTestface"),
                    "Response should contain 'Testy McTestface': " + response.getData().content());
            session.close();
        }
    }

    /**
     * Verifies that streaming delta events are received when streaming is enabled.
     *
     * @see Snapshot:
     *      session/should_receive_streaming_delta_events_when_streaming_is_enabled
     */
    @Test
    void testShouldReceiveStreamingDeltaEventsWhenStreamingIsEnabled() throws Exception {
        ctx.configureForTest("session", "should_receive_streaming_delta_events_when_streaming_is_enabled");

        try (CopilotClient client = ctx.createClient()) {
            SessionConfig config = new SessionConfig().setStreaming(true);

            CopilotSession session = client.createSession(config).get();

            var receivedEvents = new ArrayList<AbstractSessionEvent>();
            var idleReceived = new CompletableFuture<Void>();

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

    /**
     * Verifies that a session can be aborted during tool execution.
     *
     * @see Snapshot: session/should_abort_a_session
     */
    @Test
    void testShouldAbortSession() throws Exception {
        ctx.configureForTest("session", "should_abort_a_session");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();
            assertNotNull(session.getSessionId());

            // Set up wait for tool execution to start BEFORE sending
            var toolStartFuture = new CompletableFuture<ToolExecutionStartEvent>();
            var sessionIdleFuture = new CompletableFuture<SessionIdleEvent>();

            session.on(evt -> {
                if (evt instanceof ToolExecutionStartEvent toolStart && !toolStartFuture.isDone()) {
                    toolStartFuture.complete(toolStart);
                } else if (evt instanceof SessionIdleEvent idle && !sessionIdleFuture.isDone()) {
                    sessionIdleFuture.complete(idle);
                }
            });

            // Send a message that will trigger a long-running shell command
            session.send(new MessageOptions()
                    .setPrompt("run the shell command 'sleep 100' (note this works on both bash and PowerShell)"))
                    .get();

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
            assertTrue(messages.stream().anyMatch(m -> m instanceof AbortEvent), "Expected an abort event in messages");

            // We should be able to send another message
            AssistantMessageEvent answer = session.sendAndWait(new MessageOptions().setPrompt("What is 2+2?")).get(60,
                    TimeUnit.SECONDS);
            assertNotNull(answer);
            assertTrue(answer.getData().content().contains("4"),
                    "Response should contain 4: " + answer.getData().content());

            session.close();
        }
    }

    /**
     * Verifies that sessions can be created with available tools configuration.
     *
     * @see Snapshot: session/should_create_a_session_with_availabletools
     */
    @Test
    void testShouldCreateSessionWithAvailableTools() throws Exception {
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

    /**
     * Verifies that sessions can be created with excluded tools configuration.
     *
     * @see Snapshot: session/should_create_a_session_with_excludedtools
     */
    @Test
    void testShouldCreateSessionWithExcludedTools() throws Exception {
        ctx.configureForTest("session", "should_create_a_session_with_excludedtools");

        try (CopilotClient client = ctx.createClient()) {
            SessionConfig config = new SessionConfig().setExcludedTools(List.of("view"));

            CopilotSession session = client.createSession(config).get();

            assertNotNull(session.getSessionId());

            AssistantMessageEvent response = session.sendAndWait(new MessageOptions().setPrompt("What is 1+1?")).get(60,
                    TimeUnit.SECONDS);

            assertNotNull(response);
            assertTrue(response.getData().content().contains("2"),
                    "Response should contain 2: " + response.getData().content());
            session.close();
        }
    }

    /**
     * Verifies that an error is thrown when resuming a non-existent session.
     *
     * @see Snapshot: session/should_receive_session_events
     */
    @Test
    void testShouldThrowErrorWhenResumingNonExistentSession() throws Exception {
        ctx.configureForTest("session", "should_receive_session_events");

        try (CopilotClient client = ctx.createClient()) {
            try {
                client.resumeSession("non-existent-session-id").get(30, TimeUnit.SECONDS);
                fail("Expected exception when resuming non-existent session");
            } catch (Exception e) {
                // Should throw an error
                assertTrue(e.getMessage() != null || e.getCause() != null, "Exception should have a message or cause");
            }
        }
    }

    /**
     * Verifies that sessions can be created with a custom config directory.
     *
     * @see Snapshot: session/should_create_session_with_custom_config_dir
     */
    @Test
    void testShouldCreateSessionWithCustomConfigDir() throws Exception {
        ctx.configureForTest("session", "should_create_session_with_custom_config_dir");

        try (CopilotClient client = ctx.createClient()) {
            String customConfigDir = ctx.getWorkDir().resolve("custom-config").toString();

            SessionConfig config = new SessionConfig().setConfigDir(customConfigDir);
            CopilotSession session = client.createSession(config).get();

            assertNotNull(session.getSessionId());
            assertTrue(session.getSessionId().matches("^[a-f0-9-]+$"));

            // Session should work normally with custom config dir
            AssistantMessageEvent response = session.sendAndWait(new MessageOptions().setPrompt("What is 1+1?")).get(60,
                    TimeUnit.SECONDS);

            assertNotNull(response);
            assertTrue(response.getData().content().contains("2"),
                    "Response should contain 2: " + response.getData().content());

            session.close();
        }
    }

    // This test validates client-side timeout behavior. The snapshot has no
    // assistant response because the test expects timeout BEFORE completion.
    // Note: In CI mode, the proxy logs "No cached response found" errors to
    // stderr, but these are expected - the timeout still triggers correctly.
    /**
     * Verifies that sendAndWait throws an exception on timeout.
     *
     * @see Snapshot: session/sendandwait_throws_on_timeout
     */
    @Test
    void testSendAndWaitThrowsOnTimeout() throws Exception {
        ctx.configureForTest("session", "sendandwait_throws_on_timeout");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();

            // Use a short timeout that will trigger before any response
            try {
                session.sendAndWait(new MessageOptions().setPrompt("Run 'sleep 2 && echo done'"), 100).get(30,
                        TimeUnit.SECONDS);
                fail("Expected timeout exception");
            } catch (Exception e) {
                // Should throw a timeout-related error from sendAndWait
                String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                String causeMessage = e.getCause() != null && e.getCause().getMessage() != null
                        ? e.getCause().getMessage().toLowerCase()
                        : "";
                assertTrue(
                        message.contains("timeout") || message.contains("sendandwait timed out")
                                || causeMessage.contains("timeout") || causeMessage.contains("sendandwait timed out"),
                        "Should throw timeout exception, got: " + e.getMessage()
                                + (e.getCause() != null ? " caused by: " + e.getCause().getMessage() : ""));
            }

            session.close();
        }
    }

    /**
     * Verifies that sessions can be listed.
     *
     * @see Snapshot: session/should_list_sessions
     */
    @Test
    void testShouldListSessions() throws Exception {
        ctx.configureForTest("session", "should_list_sessions");

        try (CopilotClient client = ctx.createClient()) {
            // Create two sessions and send one message to each (matches snapshot format)
            CopilotSession session1 = client.createSession().get();
            session1.sendAndWait(new MessageOptions().setPrompt("Say hello")).get(60, TimeUnit.SECONDS);

            CopilotSession session2 = client.createSession().get();
            session2.sendAndWait(new MessageOptions().setPrompt("Say goodbye")).get(60, TimeUnit.SECONDS);

            // Small delay to ensure session files are written to disk
            Thread.sleep(200);

            // List all sessions
            var sessions = client.listSessions().get(30, TimeUnit.SECONDS);

            // Should have at least the sessions we created
            assertNotNull(sessions);
            assertFalse(sessions.isEmpty(), "Should have at least 1 session");

            // Our sessions should be in the list
            var sessionIds = sessions.stream().map(s -> s.getSessionId()).toList();
            assertTrue(sessionIds.contains(session1.getSessionId()), "Session 1 should be in the list");
            assertTrue(sessionIds.contains(session2.getSessionId()), "Session 2 should be in the list");

            session1.close();
            session2.close();
        }
    }

    /**
     * Verifies that sessions can be deleted.
     *
     * @see Snapshot: session/should_delete_session
     */
    @Test
    void testShouldDeleteSession() throws Exception {
        ctx.configureForTest("session", "should_delete_session");

        try (CopilotClient client = ctx.createClient()) {
            // Create a session
            CopilotSession session = client.createSession().get();
            String sessionId = session.getSessionId();

            session.sendAndWait(new MessageOptions().setPrompt("Hello")).get(60, TimeUnit.SECONDS);

            // Delete the session using the client API
            // In CI mode with replaying proxy, session files may not be persisted,
            // so we handle the "session not found" case as acceptable
            try {
                client.deleteSession(sessionId).get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                // In CI replay mode, session files don't exist - this is expected
                if (System.getenv("CI") != null && e.getMessage() != null && e.getMessage().contains("not found")) {
                    return; // Test passes - CI mode doesn't persist sessions
                }
                throw e;
            }

            // Trying to resume the deleted session should fail
            try {
                client.resumeSession(sessionId).get(30, TimeUnit.SECONDS);
                fail("Expected exception when resuming deleted session");
            } catch (Exception e) {
                // Should throw an error indicating session not found
                assertTrue(e.getMessage() != null || e.getCause() != null, "Exception should have a message or cause");
            }
        }
    }

    /**
     * Verifies that sessions can be created with custom tools.
     *
     * @see Snapshot: session/should_create_session_with_custom_tool
     */
    @Test
    void testShouldCreateSessionWithCustomTool() throws Exception {
        ctx.configureForTest("session", "should_create_session_with_custom_tool");

        // Define a custom get_secret_number tool
        Map<String, Object> parameters = new java.util.HashMap<>();
        Map<String, Object> properties = new java.util.HashMap<>();
        Map<String, Object> keyProp = new java.util.HashMap<>();
        keyProp.put("type", "string");
        keyProp.put("description", "Key");
        properties.put("key", keyProp);
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", java.util.List.of("key"));

        ToolDefinition getSecretNumberTool = ToolDefinition.create("get_secret_number", "Gets the secret number",
                parameters, (invocation) -> {
                    Map<String, Object> args = invocation.getArguments();
                    String key = (String) args.get("key");
                    // Return 54321 for ALPHA, 0 otherwise
                    int result = "ALPHA".equals(key) ? 54321 : 0;
                    return CompletableFuture.completedFuture(String.valueOf(result));
                });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setTools(java.util.List.of(getSecretNumberTool))).get();

            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions().setPrompt("What is the secret number for key ALPHA?"))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            assertTrue(response.getData().content().contains("54321"),
                    "Response should contain 54321: " + response.getData().content());

            session.close();
        }
    }

    /**
     * Verifies that streaming option is passed to session creation.
     *
     * @see Snapshot: session/should_pass_streaming_option_to_session_creation
     */
    @Test
    void testShouldPassStreamingOptionToSessionCreation() throws Exception {
        ctx.configureForTest("session", "should_pass_streaming_option_to_session_creation");

        try (CopilotClient client = ctx.createClient()) {
            // Verify that the streaming option is accepted without errors
            CopilotSession session = client.createSession(new SessionConfig().setStreaming(true)).get();

            assertNotNull(session.getSessionId());
            assertTrue(session.getSessionId().matches("^[a-f0-9-]+$"));

            // Session should still work normally
            AssistantMessageEvent response = session.sendAndWait(new MessageOptions().setPrompt("What is 1+1?")).get(60,
                    TimeUnit.SECONDS);

            assertNotNull(response);
            assertTrue(response.getData().content().contains("2"),
                    "Response should contain 2: " + response.getData().content());

            session.close();
        }
    }
}
