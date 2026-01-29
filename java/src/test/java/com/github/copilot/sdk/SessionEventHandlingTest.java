/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.events.AbstractSessionEvent;
import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.events.SessionIdleEvent;
import com.github.copilot.sdk.events.SessionStartEvent;

/**
 * Unit tests for session event handling API.
 * <p>
 * These are pure unit tests that don't require the Copilot CLI. They test the
 * event dispatch mechanism directly.
 */
public class SessionEventHandlingTest {

    private CopilotSession session;

    @BeforeEach
    void setup() throws Exception {
        // Create a minimal session for testing event handling
        // We use reflection to create a session without a real RPC connection
        session = createTestSession();
    }

    private CopilotSession createTestSession() throws Exception {
        // Use the package-private constructor via reflection for testing
        var constructor = CopilotSession.class.getDeclaredConstructor(String.class, JsonRpcClient.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance("test-session-id", null, null);
    }

    @Test
    void testGenericEventHandler() {
        List<AbstractSessionEvent> receivedEvents = new ArrayList<>();

        session.on(event -> receivedEvents.add(event));

        // Dispatch some events
        dispatchEvent(createSessionStartEvent());
        dispatchEvent(createAssistantMessageEvent("Hello"));
        dispatchEvent(createSessionIdleEvent());

        assertEquals(3, receivedEvents.size());
        assertInstanceOf(SessionStartEvent.class, receivedEvents.get(0));
        assertInstanceOf(AssistantMessageEvent.class, receivedEvents.get(1));
        assertInstanceOf(SessionIdleEvent.class, receivedEvents.get(2));
    }

    @Test
    void testTypedEventHandler() {
        List<AssistantMessageEvent> receivedMessages = new ArrayList<>();

        session.on(AssistantMessageEvent.class, msg -> receivedMessages.add(msg));

        // Dispatch various events - only AssistantMessageEvent should be captured
        dispatchEvent(createSessionStartEvent());
        dispatchEvent(createAssistantMessageEvent("First message"));
        dispatchEvent(createSessionIdleEvent());
        dispatchEvent(createAssistantMessageEvent("Second message"));

        // Should only have the two assistant messages
        assertEquals(2, receivedMessages.size());
        assertEquals("First message", receivedMessages.get(0).getData().getContent());
        assertEquals("Second message", receivedMessages.get(1).getData().getContent());
    }

    @Test
    void testMultipleTypedHandlers() {
        List<AssistantMessageEvent> messages = new ArrayList<>();
        List<SessionIdleEvent> idles = new ArrayList<>();
        List<SessionStartEvent> starts = new ArrayList<>();

        session.on(AssistantMessageEvent.class, messages::add);
        session.on(SessionIdleEvent.class, idles::add);
        session.on(SessionStartEvent.class, starts::add);

        dispatchEvent(createSessionStartEvent());
        dispatchEvent(createAssistantMessageEvent("Hello"));
        dispatchEvent(createSessionIdleEvent());
        dispatchEvent(createAssistantMessageEvent("World"));

        assertEquals(1, starts.size());
        assertEquals(2, messages.size());
        assertEquals(1, idles.size());
    }

    @Test
    void testUnsubscribe() {
        AtomicInteger count = new AtomicInteger(0);

        Closeable subscription = session.on(AssistantMessageEvent.class, msg -> count.incrementAndGet());

        dispatchEvent(createAssistantMessageEvent("First"));
        assertEquals(1, count.get());

        // Unsubscribe
        try {
            subscription.close();
        } catch (Exception e) {
            fail("Unsubscribe should not throw: " + e.getMessage());
        }

        // Should no longer receive events
        dispatchEvent(createAssistantMessageEvent("Second"));
        assertEquals(1, count.get()); // Still 1, not 2
    }

    @Test
    void testUnsubscribeGenericHandler() {
        AtomicInteger count = new AtomicInteger(0);

        Closeable subscription = session.on(event -> count.incrementAndGet());

        dispatchEvent(createSessionStartEvent());
        assertEquals(1, count.get());

        try {
            subscription.close();
        } catch (Exception e) {
            fail("Unsubscribe should not throw: " + e.getMessage());
        }

        dispatchEvent(createSessionIdleEvent());
        assertEquals(1, count.get()); // Still 1
    }

    @Test
    void testMixedHandlers() {
        List<String> allEvents = new ArrayList<>();
        List<String> messageEvents = new ArrayList<>();

        // Generic handler captures everything
        session.on(event -> allEvents.add(event.getType()));

        // Typed handler captures only messages
        session.on(AssistantMessageEvent.class, msg -> messageEvents.add(msg.getData().getContent()));

        dispatchEvent(createSessionStartEvent());
        dispatchEvent(createAssistantMessageEvent("Hello"));
        dispatchEvent(createSessionIdleEvent());

        assertEquals(3, allEvents.size());
        assertEquals(1, messageEvents.size());
        assertEquals("Hello", messageEvents.get(0));
    }

    @Test
    void testHandlerReceivesCorrectEventData() {
        AtomicReference<String> capturedContent = new AtomicReference<>();
        AtomicReference<String> capturedSessionId = new AtomicReference<>();

        session.on(AssistantMessageEvent.class, msg -> {
            capturedContent.set(msg.getData().getContent());
        });

        session.on(SessionStartEvent.class, start -> {
            capturedSessionId.set(start.getData().getSessionId());
        });

        SessionStartEvent startEvent = createSessionStartEvent();
        startEvent.getData().setSessionId("my-session-123");
        dispatchEvent(startEvent);

        AssistantMessageEvent msgEvent = createAssistantMessageEvent("Test content");
        dispatchEvent(msgEvent);

        assertEquals("my-session-123", capturedSessionId.get());
        assertEquals("Test content", capturedContent.get());
    }

    @Test
    void testHandlerExceptionDoesNotBreakOtherHandlers() {
        List<String> handler2Events = new ArrayList<>();

        // Suppress logging for this test to avoid confusing stack traces in build
        // output
        Logger sessionLogger = Logger.getLogger(CopilotSession.class.getName());
        Level originalLevel = sessionLogger.getLevel();
        sessionLogger.setLevel(Level.OFF);

        try {
            // First handler throws an exception
            session.on(AssistantMessageEvent.class, msg -> {
                throw new RuntimeException("Handler 1 error");
            });

            // Second handler should still receive events
            session.on(AssistantMessageEvent.class, msg -> {
                handler2Events.add(msg.getData().getContent());
            });

            // This should not throw - exceptions are caught
            assertDoesNotThrow(() -> dispatchEvent(createAssistantMessageEvent("Test")));

            // Second handler should have received the event
            assertEquals(1, handler2Events.size());
            assertEquals("Test", handler2Events.get(0));
        } finally {
            sessionLogger.setLevel(originalLevel);
        }
    }

    @Test
    void testNoHandlersDoesNotThrow() {
        // Dispatching events with no handlers should not throw
        assertDoesNotThrow(() -> {
            dispatchEvent(createSessionStartEvent());
            dispatchEvent(createAssistantMessageEvent("Test"));
            dispatchEvent(createSessionIdleEvent());
        });
    }

    // Helper methods to dispatch events using reflection
    private void dispatchEvent(AbstractSessionEvent event) {
        try {
            Method dispatchMethod = CopilotSession.class.getDeclaredMethod("dispatchEvent", AbstractSessionEvent.class);
            dispatchMethod.setAccessible(true);
            dispatchMethod.invoke(session, event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to dispatch event", e);
        }
    }

    // Factory methods for creating test events
    private SessionStartEvent createSessionStartEvent() {
        SessionStartEvent event = new SessionStartEvent();
        SessionStartEvent.SessionStartData data = new SessionStartEvent.SessionStartData();
        data.setSessionId("test-session");
        event.setData(data);
        return event;
    }

    private AssistantMessageEvent createAssistantMessageEvent(String content) {
        AssistantMessageEvent event = new AssistantMessageEvent();
        AssistantMessageEvent.AssistantMessageData data = new AssistantMessageEvent.AssistantMessageData();
        data.setContent(content);
        event.setData(data);
        return event;
    }

    private SessionIdleEvent createSessionIdleEvent() {
        return new SessionIdleEvent();
    }
}
