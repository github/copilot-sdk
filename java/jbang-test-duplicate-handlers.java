
//DEPS io.github.copilot-community-sdk:copilot-sdk:1.0.7
import com.github.copilot.sdk.*;
import com.github.copilot.sdk.events.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

/**
 * JBang test: verifies behavior when multiple handlers are registered for the same event type.
 *
 * Run with: jbang jbang-test-duplicate-handlers.java
 */
class DuplicateHandlersTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        var session = createTestSession();

        testBothTypedHandlersReceiveEvent(session);
        testBothGenericHandlersFire(createTestSession());
        testMixedGenericAndTypedBothFire(createTestSession());
        testUnsubscribeOneKeepsOther(createTestSession());
        testAllHandlersInvoked(createTestSession());
        testHandlerExceptionDoesNotBlockSecond(createTestSession());
        testHandlersRunOnDispatchThread(createTestSession());
        testHandlersRunOffMainThread(createTestSession());
        testConcurrentDispatchFromMultipleThreads(createTestSession());

        System.out.println();
        System.out.println("========================================");
        System.out.printf("Results: %d passed, %d failed%n", passed, failed);
        System.out.println("========================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    // --- Tests ---

    static void testBothTypedHandlersReceiveEvent(CopilotSession session) throws Exception {
        var count1 = new AtomicInteger();
        var count2 = new AtomicInteger();

        session.on(AssistantMessageEvent.class, msg -> count1.incrementAndGet());
        session.on(AssistantMessageEvent.class, msg -> count2.incrementAndGet());

        dispatchEvent(session, createAssistantMessageEvent("hello"));

        assertEq("Both typed handlers called", 1, count1.get());
        assertEq("Both typed handlers called (2nd)", 1, count2.get());
    }

    static void testBothGenericHandlersFire(CopilotSession session) throws Exception {
        var events1 = new ArrayList<String>();
        var events2 = new ArrayList<String>();

        session.on(event -> events1.add(event.getType()));
        session.on(event -> events2.add(event.getType()));

        dispatchEvent(session, createAssistantMessageEvent("test"));

        assertEq("Generic handler 1 received event", 1, events1.size());
        assertEq("Generic handler 2 received event", 1, events2.size());
    }

    static void testMixedGenericAndTypedBothFire(CopilotSession session) throws Exception {
        var genericCount = new AtomicInteger();
        var typedCount = new AtomicInteger();

        session.on(event -> genericCount.incrementAndGet());
        session.on(AssistantMessageEvent.class, msg -> typedCount.incrementAndGet());

        dispatchEvent(session, createAssistantMessageEvent("test"));

        assertEq("Generic handler fired", 1, genericCount.get());
        assertEq("Typed handler fired", 1, typedCount.get());
    }

    static void testUnsubscribeOneKeepsOther(CopilotSession session) throws Exception {
        var count1 = new AtomicInteger();
        var count2 = new AtomicInteger();

        var sub1 = session.on(AssistantMessageEvent.class, msg -> count1.incrementAndGet());
        session.on(AssistantMessageEvent.class, msg -> count2.incrementAndGet());

        dispatchEvent(session, createAssistantMessageEvent("before"));
        assertEq("Handler 1 before unsub", 1, count1.get());
        assertEq("Handler 2 before unsub", 1, count2.get());

        // Unsubscribe handler 1
        sub1.close();

        dispatchEvent(session, createAssistantMessageEvent("after"));
        assertEq("Handler 1 after unsub (unchanged)", 1, count1.get());
        assertEq("Handler 2 after unsub (incremented)", 2, count2.get());
    }

    static void testAllHandlersInvoked(CopilotSession session) throws Exception {
        var called = new ArrayList<String>();

        session.on(AssistantMessageEvent.class, msg -> called.add("first"));
        session.on(AssistantMessageEvent.class, msg -> called.add("second"));
        session.on(AssistantMessageEvent.class, msg -> called.add("third"));

        dispatchEvent(session, createAssistantMessageEvent("test"));

        assertEq("Three handlers called", 3, called.size());
        assertEq("All handlers invoked", true, called.containsAll(List.of("first", "second", "third")));
    }

    static void testHandlerExceptionDoesNotBlockSecond(CopilotSession session) throws Exception {
        var reached = new AtomicInteger();

        session.on(AssistantMessageEvent.class, msg -> {
            throw new RuntimeException("Boom!");
        });
        session.on(AssistantMessageEvent.class, msg -> reached.incrementAndGet());

        // Suppress CopilotSession logger to avoid noisy stack trace output
        var logger = java.util.logging.Logger.getLogger(CopilotSession.class.getName());
        var originalLevel = logger.getLevel();
        logger.setLevel(java.util.logging.Level.OFF);
        try {
            dispatchEvent(session, createAssistantMessageEvent("test"));
        } finally {
            logger.setLevel(originalLevel);
        }

        assertEq("Second handler still called after first threw", 1, reached.get());
    }

    /**
     * Verifies handlers execute on the thread that calls dispatchEvent,
     * simulating the real jsonrpc-reader thread.
     */
    static void testHandlersRunOnDispatchThread(CopilotSession session) throws Exception {
        var handlerThreadName = new AtomicReference<String>();

        session.on(AssistantMessageEvent.class, msg -> {
            handlerThreadName.set(Thread.currentThread().getName());
        });

        // Dispatch from a named thread to simulate the jsonrpc-reader
        var t = new Thread(() -> {
            try {
                dispatchEvent(session, createAssistantMessageEvent("async"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "jsonrpc-reader-mock");
        t.start();
        t.join(5000);

        assertEq("Handler ran on dispatch thread", "jsonrpc-reader-mock", handlerThreadName.get());
    }

    /**
     * Verifies that when dispatched from a background thread, handlers
     * do NOT run on the main thread — proving async delivery.
     */
    static void testHandlersRunOffMainThread(CopilotSession session) throws Exception {
        var mainThreadName = Thread.currentThread().getName();
        var handlerThreadName = new AtomicReference<String>();
        var latch = new java.util.concurrent.CountDownLatch(1);

        session.on(AssistantMessageEvent.class, msg -> {
            handlerThreadName.set(Thread.currentThread().getName());
            latch.countDown();
        });

        // Dispatch from a background thread (simulates jsonrpc-reader)
        new Thread(() -> {
            try {
                dispatchEvent(session, createAssistantMessageEvent("bg"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "background-dispatcher").start();

        var completed = latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        assertEq("Handler was invoked", true, completed);
        assertEq("Handler did NOT run on main thread", true,
                !mainThreadName.equals(handlerThreadName.get()));
        assertEq("Handler ran on background thread", "background-dispatcher",
                handlerThreadName.get());
    }

    /**
     * Verifies thread safety: concurrent dispatches from multiple threads
     * all reach registered handlers without lost events.
     */
    static void testConcurrentDispatchFromMultipleThreads(CopilotSession session) throws Exception {
        var totalEvents = 100;
        var receivedCount = new AtomicInteger();
        var threadNames = java.util.concurrent.ConcurrentHashMap.<String>newKeySet();
        var latch = new java.util.concurrent.CountDownLatch(totalEvents);

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
                    try {
                        dispatchEvent(session, createAssistantMessageEvent("msg-" + threadIdx + "-" + j));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }, "dispatcher-" + i);
            threads.add(t);
        }

        // Start all threads
        for (var t : threads) t.start();

        // Wait for all events to be delivered
        var completed = latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        for (var t : threads) t.join(5000);

        assertEq("All " + totalEvents + " events delivered", totalEvents, receivedCount.get());
        assertEq("Latch completed", true, completed);
        assertEq("Multiple threads dispatched", true, threadNames.size() > 1);
    }

    // --- Helpers ---

    static CopilotSession createTestSession() throws Exception {
        var rpcClass = Class.forName("com.github.copilot.sdk.JsonRpcClient");
        var ctor = CopilotSession.class.getDeclaredConstructor(String.class, rpcClass, String.class);
        ctor.setAccessible(true);
        return ctor.newInstance("test-session", null, null);
    }

    static void dispatchEvent(CopilotSession session, AbstractSessionEvent event) throws Exception {
        var method = CopilotSession.class.getDeclaredMethod("dispatchEvent", AbstractSessionEvent.class);
        method.setAccessible(true);
        method.invoke(session, event);
    }

    static AssistantMessageEvent createAssistantMessageEvent(String content) {
        var event = new AssistantMessageEvent();
        var data = new AssistantMessageEvent.AssistantMessageData();
        data.setContent(content);
        event.setData(data);
        return event;
    }

    static void assertEq(String testName, Object expected, Object actual) {
        if (Objects.equals(expected, actual)) {
            System.out.println("  ✓ " + testName);
            passed++;
        } else {
            System.out.println("  ✗ " + testName + " — expected: " + expected + ", got: " + actual);
            failed++;
        }
    }
}
