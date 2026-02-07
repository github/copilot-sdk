/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.SessionConfig;

/**
 * Tests for closed-session guard functionality in CopilotSession.
 *
 * <p>
 * Verifies that all public methods that interact with session state throw
 * IllegalStateException when invoked after close(), and that close() itself is
 * idempotent.
 * </p>
 */
public class ClosedSessionGuardTest {

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
     * Verifies that send(String) throws IllegalStateException after session is
     * terminated.
     */
    @Test
    void testSendStringThrowsAfterTermination() throws Exception {
        ctx.configureForTest("session", "should_receive_session_events");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setModel("fake-test-model")).get();
            session.close();

            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
                session.send("test message");
            });
            assertTrue(thrown.getMessage().contains("closed"), "Exception message should mention session is closed");
        }
    }

    /**
     * Verifies that send(MessageOptions) throws IllegalStateException after session
     * is terminated.
     */
    @Test
    void testSendOptionsThrowsAfterTermination() throws Exception {
        ctx.configureForTest("session", "should_receive_session_events");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setModel("fake-test-model")).get();
            session.close();

            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
                session.send(new MessageOptions().setPrompt("test message"));
            });
            assertTrue(thrown.getMessage().contains("closed"), "Exception message should mention session is closed");
        }
    }

    /**
     * Verifies that sendAndWait(String) throws IllegalStateException after session
     * is terminated.
     */
    @Test
    void testSendAndWaitStringThrowsAfterTermination() throws Exception {
        ctx.configureForTest("session", "should_receive_session_events");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setModel("fake-test-model")).get();
            session.close();

            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
                session.sendAndWait("test message");
            });
            assertTrue(thrown.getMessage().contains("closed"), "Exception message should mention session is closed");
        }
    }

    /**
     * Verifies that sendAndWait(MessageOptions) throws IllegalStateException after
     * session is terminated.
     */
    @Test
    void testSendAndWaitOptionsThrowsAfterTermination() throws Exception {
        ctx.configureForTest("session", "should_receive_session_events");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setModel("fake-test-model")).get();
            session.close();

            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
                session.sendAndWait(new MessageOptions().setPrompt("test message"));
            });
            assertTrue(thrown.getMessage().contains("closed"), "Exception message should mention session is closed");
        }
    }

    /**
     * Verifies that sendAndWait(MessageOptions, long) throws IllegalStateException
     * after session is terminated.
     */
    @Test
    void testSendAndWaitWithTimeoutThrowsAfterTermination() throws Exception {
        ctx.configureForTest("session", "should_receive_session_events");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setModel("fake-test-model")).get();
            session.close();

            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
                session.sendAndWait(new MessageOptions().setPrompt("test message"), 5000);
            });
            assertTrue(thrown.getMessage().contains("closed"), "Exception message should mention session is closed");
        }
    }

    /**
     * Verifies that on(Consumer) throws IllegalStateException after session is
     * terminated.
     */
    @Test
    void testOnConsumerThrowsAfterTermination() throws Exception {
        ctx.configureForTest("session", "should_receive_session_events");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setModel("fake-test-model")).get();
            session.close();

            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
                session.on(evt -> {
                    // Handler should never be registered
                });
            });
            assertTrue(thrown.getMessage().contains("closed"), "Exception message should mention session is closed");
        }
    }

    /**
     * Verifies that on(Class, Consumer) throws IllegalStateException after session
     * is terminated.
     */
    @Test
    void testOnTypedConsumerThrowsAfterTermination() throws Exception {
        ctx.configureForTest("session", "should_receive_session_events");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setModel("fake-test-model")).get();
            session.close();

            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
                session.on(AssistantMessageEvent.class, msg -> {
                    // Handler should never be registered
                });
            });
            assertTrue(thrown.getMessage().contains("closed"), "Exception message should mention session is closed");
        }
    }

    /**
     * Verifies that getMessages() throws IllegalStateException after session is
     * terminated.
     */
    @Test
    void testGetMessagesThrowsAfterTermination() throws Exception {
        ctx.configureForTest("session", "should_receive_session_events");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setModel("fake-test-model")).get();
            session.close();

            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
                session.getMessages();
            });
            assertTrue(thrown.getMessage().contains("closed"), "Exception message should mention session is closed");
        }
    }

    /**
     * Verifies that abort() throws IllegalStateException after session is
     * terminated.
     */
    @Test
    void testAbortThrowsAfterTermination() throws Exception {
        ctx.configureForTest("session", "should_receive_session_events");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setModel("fake-test-model")).get();
            session.close();

            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
                session.abort();
            });
            assertTrue(thrown.getMessage().contains("closed"), "Exception message should mention session is closed");
        }
    }

    /**
     * Verifies that setEventErrorHandler() throws IllegalStateException after
     * session is terminated.
     */
    @Test
    void testSetEventErrorHandlerThrowsAfterTermination() throws Exception {
        ctx.configureForTest("session", "should_receive_session_events");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setModel("fake-test-model")).get();
            session.close();

            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
                session.setEventErrorHandler((event, ex) -> {
                    // Handler should never be set
                });
            });
            assertTrue(thrown.getMessage().contains("closed"), "Exception message should mention session is closed");
        }
    }

    /**
     * Verifies that setEventErrorPolicy() throws IllegalStateException after
     * session is terminated.
     */
    @Test
    void testSetEventErrorPolicyThrowsAfterTermination() throws Exception {
        ctx.configureForTest("session", "should_receive_session_events");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setModel("fake-test-model")).get();
            session.close();

            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
                session.setEventErrorPolicy(EventErrorPolicy.SUPPRESS_AND_LOG_ERRORS);
            });
            assertTrue(thrown.getMessage().contains("closed"), "Exception message should mention session is closed");
        }
    }

    /**
     * Verifies that getSessionId() still works after session is terminated (it's
     * just a field read).
     */
    @Test
    void testGetSessionIdWorksAfterTermination() throws Exception {
        ctx.configureForTest("session", "should_receive_session_events");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setModel("fake-test-model")).get();
            String sessionIdBeforeClose = session.getSessionId();
            session.close();

            String sessionIdAfterClose = session.getSessionId();
            assertEquals(sessionIdBeforeClose, sessionIdAfterClose, "Session ID should remain accessible after close");
        }
    }

    /**
     * Verifies that getWorkspacePath() still works after session is terminated
     * (it's just a field read).
     */
    @Test
    void testGetWorkspacePathWorksAfterTermination() throws Exception {
        ctx.configureForTest("session", "should_receive_session_events");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setModel("fake-test-model")).get();
            String pathBeforeClose = session.getWorkspacePath();
            session.close();

            String pathAfterClose = session.getWorkspacePath();
            assertEquals(pathBeforeClose, pathAfterClose, "Workspace path should remain accessible after close");
        }
    }

    /**
     * Verifies that close() is idempotent and can be called multiple times safely.
     */
    @Test
    void testCloseIsIdempotent() throws Exception {
        ctx.configureForTest("session", "should_receive_session_events");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setModel("fake-test-model")).get();

            // First close should succeed
            assertDoesNotThrow(() -> session.close());

            // Second close should also succeed (no-op)
            assertDoesNotThrow(() -> session.close());

            // Third close should also succeed (no-op)
            assertDoesNotThrow(() -> session.close());
        }
    }

    /**
     * Verifies that try-with-resources double-close scenario works correctly.
     */
    @Test
    void testTryWithResourcesDoubleClose() throws Exception {
        ctx.configureForTest("session", "should_receive_session_events");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setModel("fake-test-model")).get();

            try (session) {
                // Manual close within try-with-resources
                session.close();
                // Automatic close will happen at end of block
            } // Second close happens here

            // Should be able to verify it's closed
            assertThrows(IllegalStateException.class, () -> {
                session.send("test");
            });
        }
    }
}
