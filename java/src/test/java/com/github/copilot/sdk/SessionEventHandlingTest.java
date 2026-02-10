/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
        var receivedEvents = new ArrayList<AbstractSessionEvent>();

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
        var receivedMessages = new ArrayList<AssistantMessageEvent>();

        session.on(AssistantMessageEvent.class, msg -> receivedMessages.add(msg));

        // Dispatch various events - only AssistantMessageEvent should be captured
        dispatchEvent(createSessionStartEvent());
        dispatchEvent(createAssistantMessageEvent("First message"));
        dispatchEvent(createSessionIdleEvent());
        dispatchEvent(createAssistantMessageEvent("Second message"));

        // Should only have the two assistant messages
        assertEquals(2, receivedMessages.size());
        assertEquals("First message", receivedMessages.get(0).getData().content());
        assertEquals("Second message", receivedMessages.get(1).getData().content());
    }

    @Test
    void testMultipleTypedHandlers() {
        var messages = new ArrayList<AssistantMessageEvent>();
        var idles = new ArrayList<SessionIdleEvent>();
        var starts = new ArrayList<SessionStartEvent>();

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
        var count = new AtomicInteger(0);

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
        var count = new AtomicInteger(0);

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
        var allEvents = new ArrayList<String>();
        var messageEvents = new ArrayList<String>();

        // Generic handler captures everything
        session.on(event -> allEvents.add(event.getType()));

        // Typed handler captures only messages
        session.on(AssistantMessageEvent.class, msg -> messageEvents.add(msg.getData().content()));

        dispatchEvent(createSessionStartEvent());
        dispatchEvent(createAssistantMessageEvent("Hello"));
        dispatchEvent(createSessionIdleEvent());

        assertEquals(3, allEvents.size());
        assertEquals(1, messageEvents.size());
        assertEquals("Hello", messageEvents.get(0));
    }

    @Test
    void testHandlerReceivesCorrectEventData() {
        var capturedContent = new AtomicReference<String>();
        var capturedSessionId = new AtomicReference<String>();

        session.on(AssistantMessageEvent.class, msg -> {
            capturedContent.set(msg.getData().content());
        });

        session.on(SessionStartEvent.class, start -> {
            capturedSessionId.set(start.getData().sessionId());
        });

        SessionStartEvent startEvent = createSessionStartEvent("my-session-123");
        dispatchEvent(startEvent);

        AssistantMessageEvent msgEvent = createAssistantMessageEvent("Test content");
        dispatchEvent(msgEvent);

        assertEquals("my-session-123", capturedSessionId.get());
        assertEquals("Test content", capturedContent.get());
    }

    @Test
    void testHandlerExceptionDoesNotBreakOtherHandlers() {
        var handler2Events = new ArrayList<String>();

        // Suppress logging for this test to avoid confusing stack traces in build
        // output
        Logger sessionLogger = Logger.getLogger(CopilotSession.class.getName());
        Level originalLevel = sessionLogger.getLevel();
        sessionLogger.setLevel(Level.OFF);

        try {
            // Use SUPPRESS policy so second handler still runs
            session.setEventErrorPolicy(EventErrorPolicy.SUPPRESS_AND_LOG_ERRORS);

            // First handler throws an exception
            session.on(AssistantMessageEvent.class, msg -> {
                throw new RuntimeException("Handler 1 error");
            });

            // Second handler should still receive events
            session.on(AssistantMessageEvent.class, msg -> {
                handler2Events.add(msg.getData().content());
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

    @Test
    void testDuplicateTypedHandlersBothReceiveEvent() {
        var count1 = new AtomicInteger();
        var count2 = new AtomicInteger();

        session.on(AssistantMessageEvent.class, msg -> count1.incrementAndGet());
        session.on(AssistantMessageEvent.class, msg -> count2.incrementAndGet());

        dispatchEvent(createAssistantMessageEvent("hello"));

        assertEquals(1, count1.get(), "First typed handler should be called");
        assertEquals(1, count2.get(), "Second typed handler should be called");
    }

    @Test
    void testDuplicateGenericHandlersBothFire() {
        var events1 = new ArrayList<String>();
        var events2 = new ArrayList<String>();

        session.on(event -> events1.add(event.getType()));
        session.on(event -> events2.add(event.getType()));

        dispatchEvent(createAssistantMessageEvent("test"));

        assertEquals(1, events1.size(), "First generic handler should receive event");
        assertEquals(1, events2.size(), "Second generic handler should receive event");
    }

    @Test
    void testUnsubscribeOneKeepsOther() {
        var count1 = new AtomicInteger();
        var count2 = new AtomicInteger();

        var sub1 = session.on(AssistantMessageEvent.class, msg -> count1.incrementAndGet());
        session.on(AssistantMessageEvent.class, msg -> count2.incrementAndGet());

        dispatchEvent(createAssistantMessageEvent("before"));
        assertEquals(1, count1.get());
        assertEquals(1, count2.get());

        // Unsubscribe first handler
        try {
            sub1.close();
        } catch (Exception e) {
            fail("Unsubscribe should not throw: " + e.getMessage());
        }

        dispatchEvent(createAssistantMessageEvent("after"));
        assertEquals(1, count1.get(), "Unsubscribed handler should not be called again");
        assertEquals(2, count2.get(), "Remaining handler should still be called");
    }

    @Test
    void testAllHandlersInvoked() {
        var called = new ArrayList<String>();

        session.on(AssistantMessageEvent.class, msg -> called.add("first"));
        session.on(AssistantMessageEvent.class, msg -> called.add("second"));
        session.on(AssistantMessageEvent.class, msg -> called.add("third"));

        dispatchEvent(createAssistantMessageEvent("test"));

        assertEquals(3, called.size(), "All three handlers should be invoked");
        assertTrue(called.containsAll(List.of("first", "second", "third")), "All handler labels should be present");
    }

    @Test
    void testHandlersRunOnDispatchThread() throws Exception {
        var handlerThreadName = new AtomicReference<String>();
        var latch = new CountDownLatch(1);

        session.on(AssistantMessageEvent.class, msg -> {
            handlerThreadName.set(Thread.currentThread().getName());
            latch.countDown();
        });

        // Dispatch from a named thread to simulate the jsonrpc-reader
        var t = new Thread(() -> dispatchEvent(createAssistantMessageEvent("async")), "jsonrpc-reader-mock");
        t.start();
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Handler should be invoked within timeout");
        t.join(5000);

        assertEquals("jsonrpc-reader-mock", handlerThreadName.get(),
                "Handler should run on the dispatch thread, not a different one");
    }

    @Test
    void testHandlersRunOffMainThread() throws Exception {
        var mainThreadName = Thread.currentThread().getName();
        var handlerThreadName = new AtomicReference<String>();
        var latch = new CountDownLatch(1);

        session.on(AssistantMessageEvent.class, msg -> {
            handlerThreadName.set(Thread.currentThread().getName());
            latch.countDown();
        });

        // Dispatch from a background thread (simulates jsonrpc-reader)
        new Thread(() -> dispatchEvent(createAssistantMessageEvent("bg")), "background-dispatcher").start();

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Handler should be invoked within timeout");
        assertNotEquals(mainThreadName, handlerThreadName.get(), "Handler should NOT run on the main/test thread");
        assertEquals("background-dispatcher", handlerThreadName.get(),
                "Handler should run on the background dispatch thread");
    }

    @Test
    void testConcurrentDispatchFromMultipleThreads() throws Exception {
        var totalEvents = 100;
        var receivedCount = new AtomicInteger();
        var threadNames = ConcurrentHashMap.<String>newKeySet();
        var latch = new CountDownLatch(totalEvents);

        session.on(AssistantMessageEvent.class, msg -> {
            receivedCount.incrementAndGet();
            threadNames.add(Thread.currentThread().getName());
            latch.countDown();
        });

        // Fire events from 10 concurrent threads, 10 events each
        var threads = new ArrayList<Thread>();
        for (int i = 0; i < 10; i++) {
            var threadIdx = i;
            var t = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    dispatchEvent(createAssistantMessageEvent("msg-" + threadIdx + "-" + j));
                }
            }, "dispatcher-" + i);
            threads.add(t);
        }

        for (var t : threads) {
            t.start();
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All events should be delivered within timeout");
        for (var t : threads) {
            t.join(5000);
        }

        assertEquals(totalEvents, receivedCount.get(), "All " + totalEvents + " events should be delivered");
        assertTrue(threadNames.size() > 1, "Events should have been dispatched from multiple threads");
    }

    // Helper methods to dispatch events using reflection
    // ====================================================================
    // EventErrorHandler tests
    // ====================================================================

    @Test
    void testDefaultPolicyPropagatesAndLogs() {
        // Default policy is PROPAGATE_AND_LOG_ERRORS — stops dispatch on first error
        var handler1Called = new AtomicInteger(0);
        var handler2Called = new AtomicInteger(0);

        Logger sessionLogger = Logger.getLogger(CopilotSession.class.getName());
        Level originalLevel = sessionLogger.getLevel();
        sessionLogger.setLevel(Level.OFF);

        try {
            // Both handlers throw — with PROPAGATE only one should execute
            session.on(AssistantMessageEvent.class, msg -> {
                handler1Called.incrementAndGet();
                throw new RuntimeException("boom 1");
            });

            session.on(AssistantMessageEvent.class, msg -> {
                handler2Called.incrementAndGet();
                throw new RuntimeException("boom 2");
            });

            assertDoesNotThrow(() -> dispatchEvent(createAssistantMessageEvent("Test")));

            // Only one handler should execute (default PROPAGATE_AND_LOG_ERRORS policy)
            int totalCalls = handler1Called.get() + handler2Called.get();
            assertEquals(1, totalCalls, "Only one handler should execute with default PROPAGATE_AND_LOG_ERRORS policy");
        } finally {
            sessionLogger.setLevel(originalLevel);
        }
    }

    @Test
    void testCustomEventErrorHandlerReceivesEventAndException() {
        var capturedEvents = new ArrayList<AbstractSessionEvent>();
        var capturedExceptions = new ArrayList<Exception>();

        Logger sessionLogger = Logger.getLogger(CopilotSession.class.getName());
        Level originalLevel = sessionLogger.getLevel();
        sessionLogger.setLevel(Level.OFF);

        try {
            session.setEventErrorHandler((event, exception) -> {
                capturedEvents.add(event);
                capturedExceptions.add(exception);
            });

            var thrownException = new RuntimeException("test error");
            session.on(AssistantMessageEvent.class, msg -> {
                throw thrownException;
            });

            var event = createAssistantMessageEvent("Hello");
            dispatchEvent(event);

            assertEquals(1, capturedEvents.size());
            assertSame(event, capturedEvents.get(0));
            assertEquals(1, capturedExceptions.size());
            assertSame(thrownException, capturedExceptions.get(0));
        } finally {
            sessionLogger.setLevel(originalLevel);
        }
    }

    @Test
    void testCustomErrorHandlerCalledForAllErrors() {
        var errorCount = new AtomicInteger(0);

        Logger sessionLogger = Logger.getLogger(CopilotSession.class.getName());
        Level originalLevel = sessionLogger.getLevel();
        sessionLogger.setLevel(Level.OFF);

        try {
            session.setEventErrorPolicy(EventErrorPolicy.SUPPRESS_AND_LOG_ERRORS);
            session.setEventErrorHandler((event, exception) -> {
                errorCount.incrementAndGet();
            });

            session.on(AssistantMessageEvent.class, msg -> {
                throw new RuntimeException("error 1");
            });
            session.on(AssistantMessageEvent.class, msg -> {
                throw new RuntimeException("error 2");
            });

            dispatchEvent(createAssistantMessageEvent("Test"));

            // Both handler errors should be reported to the custom error handler
            assertEquals(2, errorCount.get());
        } finally {
            sessionLogger.setLevel(originalLevel);
        }
    }

    @Test
    void testErrorHandlerItselfThrowingStopsDispatch() {
        var handler1Called = new AtomicInteger(0);
        var handler2Called = new AtomicInteger(0);

        Logger sessionLogger = Logger.getLogger(CopilotSession.class.getName());
        Level originalLevel = sessionLogger.getLevel();
        sessionLogger.setLevel(Level.OFF);

        try {
            session.setEventErrorHandler((event, exception) -> {
                throw new RuntimeException("error handler also broke");
            });

            // Two handlers that throw
            session.on(AssistantMessageEvent.class, msg -> {
                handler1Called.incrementAndGet();
                throw new RuntimeException("handler error");
            });

            session.on(AssistantMessageEvent.class, msg -> {
                handler2Called.incrementAndGet();
                throw new RuntimeException("handler error");
            });

            assertDoesNotThrow(() -> dispatchEvent(createAssistantMessageEvent("Test")));
            // Error handler threw — dispatch stops regardless of policy
            int totalCalls = handler1Called.get() + handler2Called.get();
            assertEquals(1, totalCalls,
                    "Only one handler should have been called (dispatch stopped when error handler threw)");
        } finally {
            sessionLogger.setLevel(originalLevel);
        }
    }

    @Test
    void testSetEventErrorHandlerToNullRestoresDefaultBehavior() {
        var errorCount = new AtomicInteger(0);

        Logger sessionLogger = Logger.getLogger(CopilotSession.class.getName());
        Level originalLevel = sessionLogger.getLevel();
        sessionLogger.setLevel(Level.OFF);

        try {
            // Set custom handler
            session.setEventErrorHandler((event, exception) -> {
                errorCount.incrementAndGet();
            });

            session.on(AssistantMessageEvent.class, msg -> {
                throw new RuntimeException("error");
            });

            dispatchEvent(createAssistantMessageEvent("Test1"));
            assertEquals(1, errorCount.get());

            // Reset to null (restore default logging-only behavior)
            session.setEventErrorHandler(null);

            dispatchEvent(createAssistantMessageEvent("Test2"));

            // Custom handler should NOT have been called again
            assertEquals(1, errorCount.get());
        } finally {
            sessionLogger.setLevel(originalLevel);
        }
    }

    @Test
    void testErrorHandlerReceivesCorrectEventType() {
        var capturedEvents = new ArrayList<AbstractSessionEvent>();

        Logger sessionLogger = Logger.getLogger(CopilotSession.class.getName());
        Level originalLevel = sessionLogger.getLevel();
        sessionLogger.setLevel(Level.OFF);

        try {
            session.setEventErrorPolicy(EventErrorPolicy.SUPPRESS_AND_LOG_ERRORS);
            session.setEventErrorHandler((event, exception) -> {
                capturedEvents.add(event);
            });

            session.on(event -> {
                throw new RuntimeException("always fails");
            });

            var msgEvent = createAssistantMessageEvent("msg");
            var idleEvent = createSessionIdleEvent();

            dispatchEvent(msgEvent);
            dispatchEvent(idleEvent);

            assertEquals(2, capturedEvents.size());
            assertInstanceOf(AssistantMessageEvent.class, capturedEvents.get(0));
            assertInstanceOf(SessionIdleEvent.class, capturedEvents.get(1));
        } finally {
            sessionLogger.setLevel(originalLevel);
        }
    }

    // ====================================================================
    // EventErrorPolicy tests
    // ====================================================================

    @Test
    void testDefaultPolicyPropagatesOnError() {
        var handler1Called = new AtomicInteger(0);
        var handler2Called = new AtomicInteger(0);

        Logger sessionLogger = Logger.getLogger(CopilotSession.class.getName());
        Level originalLevel = sessionLogger.getLevel();
        sessionLogger.setLevel(Level.OFF);

        try {
            session.setEventErrorHandler((event, exception) -> {
                // just consume
            });

            // Both handlers throw — with PROPAGATE only one should execute
            session.on(AssistantMessageEvent.class, msg -> {
                handler1Called.incrementAndGet();
                throw new RuntimeException("error 1");
            });

            session.on(AssistantMessageEvent.class, msg -> {
                handler2Called.incrementAndGet();
                throw new RuntimeException("error 2");
            });

            dispatchEvent(createAssistantMessageEvent("Test"));

            // Default is PROPAGATE_AND_LOG_ERRORS — only one handler runs
            int totalCalls = handler1Called.get() + handler2Called.get();
            assertEquals(1, totalCalls, "Only one handler should execute with default PROPAGATE_AND_LOG_ERRORS policy");
        } finally {
            sessionLogger.setLevel(originalLevel);
        }
    }

    @Test
    void testPropagatePolicyStopsOnFirstError() {
        var handler1Called = new AtomicInteger(0);
        var handler2Called = new AtomicInteger(0);
        var errorHandlerCalls = new AtomicInteger(0);

        Logger sessionLogger = Logger.getLogger(CopilotSession.class.getName());
        Level originalLevel = sessionLogger.getLevel();
        sessionLogger.setLevel(Level.OFF);

        try {
            session.setEventErrorPolicy(EventErrorPolicy.PROPAGATE_AND_LOG_ERRORS);
            session.setEventErrorHandler((event, exception) -> {
                errorHandlerCalls.incrementAndGet();
            });

            // Two handlers that throw
            session.on(AssistantMessageEvent.class, msg -> {
                handler1Called.incrementAndGet();
                throw new RuntimeException("error 1");
            });

            session.on(AssistantMessageEvent.class, msg -> {
                handler2Called.incrementAndGet();
                throw new RuntimeException("error 2");
            });

            dispatchEvent(createAssistantMessageEvent("Test"));

            // Only one handler should have been called (PROPAGATE_AND_LOG_ERRORS policy)
            assertEquals(1, errorHandlerCalls.get());
            int totalCalls = handler1Called.get() + handler2Called.get();
            assertEquals(1, totalCalls, "Only one handler should execute with PROPAGATE_AND_LOG_ERRORS policy");
        } finally {
            sessionLogger.setLevel(originalLevel);
        }
    }

    @Test
    void testPropagatePolicyErrorHandlerAlwaysInvoked() {
        var errorHandlerCalls = new AtomicInteger(0);

        Logger sessionLogger = Logger.getLogger(CopilotSession.class.getName());
        Level originalLevel = sessionLogger.getLevel();
        sessionLogger.setLevel(Level.OFF);

        try {
            session.setEventErrorPolicy(EventErrorPolicy.PROPAGATE_AND_LOG_ERRORS);
            session.setEventErrorHandler((event, exception) -> {
                errorHandlerCalls.incrementAndGet();
            });

            session.on(AssistantMessageEvent.class, msg -> {
                throw new RuntimeException("error");
            });

            dispatchEvent(createAssistantMessageEvent("Test"));

            // Error handler should be called even with PROPAGATE_AND_LOG_ERRORS policy
            assertEquals(1, errorHandlerCalls.get());
        } finally {
            sessionLogger.setLevel(originalLevel);
        }
    }

    @Test
    void testSuppressPolicyWithMultipleErrors() {
        var errorHandlerCalls = new AtomicInteger(0);
        var successfulHandlerCalls = new AtomicInteger(0);

        Logger sessionLogger = Logger.getLogger(CopilotSession.class.getName());
        Level originalLevel = sessionLogger.getLevel();
        sessionLogger.setLevel(Level.OFF);

        try {
            session.setEventErrorPolicy(EventErrorPolicy.SUPPRESS_AND_LOG_ERRORS);
            session.setEventErrorHandler((event, exception) -> {
                errorHandlerCalls.incrementAndGet();
            });

            session.on(AssistantMessageEvent.class, msg -> {
                throw new RuntimeException("error 1");
            });
            session.on(AssistantMessageEvent.class, msg -> {
                throw new RuntimeException("error 2");
            });
            session.on(AssistantMessageEvent.class, msg -> {
                successfulHandlerCalls.incrementAndGet();
            });
            session.on(AssistantMessageEvent.class, msg -> {
                throw new RuntimeException("error 3");
            });

            dispatchEvent(createAssistantMessageEvent("Test"));

            // All errors should be reported, successful handler should run
            assertEquals(3, errorHandlerCalls.get());
            assertEquals(1, successfulHandlerCalls.get());
        } finally {
            sessionLogger.setLevel(originalLevel);
        }
    }

    @Test
    void testSwitchPolicyDynamically() {
        var handler1Called = new AtomicInteger(0);
        var handler2Called = new AtomicInteger(0);

        Logger sessionLogger = Logger.getLogger(CopilotSession.class.getName());
        Level originalLevel = sessionLogger.getLevel();
        sessionLogger.setLevel(Level.OFF);

        try {
            session.setEventErrorHandler((event, exception) -> {
                // just consume
            });

            // Two handlers that throw
            session.on(AssistantMessageEvent.class, msg -> {
                handler1Called.incrementAndGet();
                throw new RuntimeException("error");
            });
            session.on(AssistantMessageEvent.class, msg -> {
                handler2Called.incrementAndGet();
                throw new RuntimeException("error");
            });

            // With SUPPRESS_AND_LOG_ERRORS, both should fire
            session.setEventErrorPolicy(EventErrorPolicy.SUPPRESS_AND_LOG_ERRORS);
            dispatchEvent(createAssistantMessageEvent("Test1"));
            assertEquals(1, handler1Called.get());
            assertEquals(1, handler2Called.get());

            handler1Called.set(0);
            handler2Called.set(0);

            // Switch to PROPAGATE_AND_LOG_ERRORS — only one should fire
            session.setEventErrorPolicy(EventErrorPolicy.PROPAGATE_AND_LOG_ERRORS);
            dispatchEvent(createAssistantMessageEvent("Test2"));
            int totalCalls = handler1Called.get() + handler2Called.get();
            assertEquals(1, totalCalls, "Only one handler should execute after switching to PROPAGATE_AND_LOG_ERRORS");
        } finally {
            sessionLogger.setLevel(originalLevel);
        }
    }

    @Test
    void testPropagatePolicyNoErrorHandlerStopsAndLogs() {
        var handler1Called = new AtomicInteger(0);
        var handler2Called = new AtomicInteger(0);

        Logger sessionLogger = Logger.getLogger(CopilotSession.class.getName());
        Level originalLevel = sessionLogger.getLevel();
        sessionLogger.setLevel(Level.OFF);

        try {
            // No error handler set, PROPAGATE_AND_LOG_ERRORS policy
            session.setEventErrorPolicy(EventErrorPolicy.PROPAGATE_AND_LOG_ERRORS);

            session.on(AssistantMessageEvent.class, msg -> {
                handler1Called.incrementAndGet();
                throw new RuntimeException("error");
            });

            session.on(AssistantMessageEvent.class, msg -> {
                handler2Called.incrementAndGet();
                throw new RuntimeException("error");
            });

            assertDoesNotThrow(() -> dispatchEvent(createAssistantMessageEvent("Test")));

            // PROPAGATE_AND_LOG_ERRORS policy should stop after first error
            int totalCalls = handler1Called.get() + handler2Called.get();
            assertEquals(1, totalCalls,
                    "Only one handler should execute with PROPAGATE_AND_LOG_ERRORS policy and no error handler");
        } finally {
            sessionLogger.setLevel(originalLevel);
        }
    }

    @Test
    void testErrorHandlerThrowingStopsRegardlessOfPolicy() {
        var handler1Called = new AtomicInteger(0);
        var handler2Called = new AtomicInteger(0);

        Logger sessionLogger = Logger.getLogger(CopilotSession.class.getName());
        Level originalLevel = sessionLogger.getLevel();
        sessionLogger.setLevel(Level.OFF);

        try {
            // SUPPRESS_AND_LOG_ERRORS policy, but error handler throws
            session.setEventErrorPolicy(EventErrorPolicy.SUPPRESS_AND_LOG_ERRORS);
            session.setEventErrorHandler((event, exception) -> {
                throw new RuntimeException("error handler broke");
            });

            session.on(AssistantMessageEvent.class, msg -> {
                handler1Called.incrementAndGet();
                throw new RuntimeException("error");
            });

            session.on(AssistantMessageEvent.class, msg -> {
                handler2Called.incrementAndGet();
                throw new RuntimeException("error");
            });

            assertDoesNotThrow(() -> dispatchEvent(createAssistantMessageEvent("Test")));

            // Error handler threw — should stop regardless of SUPPRESS_AND_LOG_ERRORS
            // policy
            int totalCalls = handler1Called.get() + handler2Called.get();
            assertEquals(1, totalCalls,
                    "Only one handler should execute when error handler throws, even with SUPPRESS_AND_LOG_ERRORS policy");
        } finally {
            sessionLogger.setLevel(originalLevel);
        }
    }

    // ====================================================================
    // Helper methods
    // ====================================================================

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
        return createSessionStartEvent("test-session");
    }

    private SessionStartEvent createSessionStartEvent(String sessionId) {
        var event = new SessionStartEvent();
        var data = new SessionStartEvent.SessionStartData(sessionId, 0, null, null, null, null);
        event.setData(data);
        return event;
    }

    private AssistantMessageEvent createAssistantMessageEvent(String content) {
        var event = new AssistantMessageEvent();
        var data = new AssistantMessageEvent.AssistantMessageData(null, content, null, null, null, null, null);
        event.setData(data);
        return event;
    }

    private SessionIdleEvent createSessionIdleEvent() {
        return new SessionIdleEvent();
    }
}
