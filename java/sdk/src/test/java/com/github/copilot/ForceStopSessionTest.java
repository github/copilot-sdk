/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.SessionEvent;
import com.github.copilot.generated.SessionIdleEvent;
import com.github.copilot.rpc.AutoModeSwitchResponse;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.GitHubTokenProviderResult;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.RuntimeConnection;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SessionHooks;
import com.github.copilot.rpc.ToolDefinition;

class ForceStopSessionTest {

    @CopilotResponse
    public record Answer(int value) {
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shutdownReturnsItsFutureWhileInlineStartupWaitsForConnect(boolean force) throws Exception {
        var callers = Executors.newFixedThreadPool(2);
        try (var server = new Server(); var client = server.createClient(Runnable::run)) {
            server.holdConnect = true;
            var starting = callers.submit(client::start);
            var connect = server.heldConnects.poll(5, TimeUnit.SECONDS);
            assertNotNull(connect, "Inline startup must be waiting for the connect response");
            boolean connectReleased = false;
            try {
                var shutdown = callers.submit(() -> force ? client.forceStop() : client.stop());
                var stopping = shutdown.get(2, TimeUnit.SECONDS);
                if (!force) {
                    server.completeConnect(connect);
                    connectReleased = true;
                    starting.get(5, TimeUnit.SECONDS).get(5, TimeUnit.SECONDS);
                } else {
                    assertThrows(ExecutionException.class,
                            () -> starting.get(5, TimeUnit.SECONDS).get(5, TimeUnit.SECONDS));
                }
                stopping.get(5, TimeUnit.SECONDS);
            } finally {
                if (!connectReleased && !starting.isDone()) {
                    try {
                        server.completeConnect(connect);
                    } catch (IOException error) {
                        if (!server.peerClosed.await(5, TimeUnit.SECONDS)) {
                            throw error;
                        }
                    }
                }
            }
        } finally {
            callers.shutdownNow();
            assertTrue(callers.awaitTermination(5, TimeUnit.SECONDS), "The owned startup callers must stop");
        }
    }

    @Test
    void implicitInlineStartupReturnsItsFailureWhenForceStopped() throws Exception {
        var caller = Executors.newSingleThreadExecutor();
        try (var server = new Server(); var client = server.createClient(Runnable::run)) {
            server.holdConnect = true;
            var creating = caller.submit(() -> client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)));
            assertNotNull(server.heldConnects.poll(5, TimeUnit.SECONDS),
                    "Implicit startup must be waiting for the connect response");

            client.forceStop().get(5, TimeUnit.SECONDS);

            var result = creating.get(5, TimeUnit.SECONDS);
            assertThrows(ExecutionException.class, () -> result.get(5, TimeUnit.SECONDS));
        } finally {
            caller.shutdownNow();
            assertTrue(caller.awaitTermination(5, TimeUnit.SECONDS), "The owned startup caller must stop");
        }
    }

    @Test
    void concurrentFailedResumesDuringGracefulStopRetireDisplacedWaits() throws Exception {
        try (var server = new Server(); var client = server.createClient()) {
            var resident = createSession(client);
            var waiting = resident.sendAndWait(new MessageOptions().setPrompt("pending response"), 0);
            assertNotNull(server.sends.poll(5, TimeUnit.SECONDS), "The legacy wait must be admitted");
            client.ping("send acknowledgement barrier").get(5, TimeUnit.SECONDS);
            server.holdResume = true;
            var config = new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL);
            var first = client.resumeSession(resident.getSessionId(), config);
            var firstRequest = server.heldResumes.poll(5, TimeUnit.SECONDS);
            assertNotNull(firstRequest, "The first resume must be admitted");
            var second = client.resumeSession(resident.getSessionId(), config);
            var secondRequest = server.heldResumes.poll(5, TimeUnit.SECONDS);
            assertNotNull(secondRequest, "The second resume must be admitted");
            server.holdDetachResponse = true;
            var stopping = client.stop();
            var detach = server.heldDetaches.poll(5, TimeUnit.SECONDS);
            assertNotNull(detach, "Graceful stop must be draining the second provisional session");
            try {
                for (var request : List.of(firstRequest, secondRequest)) {
                    server.writeMessage(Map.of("jsonrpc", "2.0", "id", request.get("id"), "error",
                            Map.of("code", -32603, "message", "controlled resume rejection")));
                    var resuming = request == firstRequest ? first : second;
                    assertThrows(ExecutionException.class, () -> resuming.get(5, TimeUnit.SECONDS));
                }
            } finally {
                server.completeDetach(detach, "success");
                stopping.get(CopilotSession.DETACH_TIMEOUT_SECONDS + 3, TimeUnit.SECONDS);
            }

            var error = assertThrows(ExecutionException.class, () -> waiting.get(5, TimeUnit.SECONDS));
            assertEquals("Session closed before response completed", error.getCause().getMessage());
            assertThrows(IllegalStateException.class, () -> resident.on(event -> {
            }));
        }
    }

    @Test
    void failedResumeDuringGracefulStopPreservesResidentCallbacksUntilDetachCompletes() throws Exception {
        var events = new AtomicInteger();
        var hooks = new AtomicInteger();
        try (var server = new Server(); var client = server.createClient()) {
            var resident = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setHooks(new SessionHooks().setOnSessionEnd((input, invocation) -> {
                                hooks.incrementAndGet();
                                return CompletableFuture.completedFuture(null);
                            })))
                    .get(5, TimeUnit.SECONDS);
            try (var subscription = resident.on(event -> events.incrementAndGet())) {
                server.holdResume = true;
                var resuming = client.resumeSession(resident.getSessionId(),
                        new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL));
                var resume = server.heldResumes.poll(5, TimeUnit.SECONDS);
                assertNotNull(resume, "Warm resume must be pending before stop");
                server.holdDetachResponse = true;
                var stopping = client.stop();
                var detach = server.heldDetaches.poll(5, TimeUnit.SECONDS);
                assertNotNull(detach, "Graceful stop must be draining the provisional session");
                try {
                    server.writeMessage(Map.of("jsonrpc", "2.0", "id", resume.get("id"), "error",
                            Map.of("code", -32603, "message", "controlled resume rejection")));
                    assertThrows(ExecutionException.class, () -> resuming.get(5, TimeUnit.SECONDS));
                    server.emitIdle(resident.getSessionId());
                    client.ping("failed resume callback barrier").get(5, TimeUnit.SECONDS);
                    assertEquals(1, events.get(), "The resident must receive terminal events while detach drains");
                    server.invokeSessionEnd(resident.getSessionId());
                    assertEquals(1, hooks.get(), "The resident's session-end hook must remain routed");
                } finally {
                    server.completeDetach(detach, "success");
                    stopping.get(CopilotSession.DETACH_TIMEOUT_SECONDS + 3, TimeUnit.SECONDS);
                }
                assertThrows(IllegalStateException.class, () -> resident.send("after stop"));
            }
        }
    }

    @Test
    void gracefulClosePreservesEventsAndSessionEndHooksUntilDetachCompletes() throws Exception {
        var events = new AtomicInteger();
        var hooks = new AtomicInteger();
        try (var server = new Server(); var client = server.createClient()) {
            server.emitDetachCallbacks = true;
            var session = client.createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                    .setHooks(new SessionHooks().setOnSessionEnd((input, invocation) -> {
                        assertEquals("user_exit", input.reason());
                        hooks.incrementAndGet();
                        return CompletableFuture.completedFuture(null);
                    }))).get(5, TimeUnit.SECONDS);
            try (var subscription = session.on(event -> events.incrementAndGet())) {
                session.close();
                assertEquals(1, server.detaches.get());
                assertEquals(1, hooks.get(), "Ordinary detach must still invoke the session-end hook");
                assertEquals(1, events.get(), "Ordinary detach must deliver events before callback retirement");
                server.emitIdle(session.getSessionId());
                client.ping("retired detach callbacks barrier").get(5, TimeUnit.SECONDS);
                assertEquals(1, events.get(), "Detach completion must retire subsequent callbacks");
                assertThrows(IllegalStateException.class, () -> session.on(event -> fail("Retired handler")));
                session.close();
                assertEquals(1, server.detaches.get(), "Repeated close must not detach again");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void resumeSettlementPreservesTheRuntimeCallbackOwnerWhileResidentCloses(boolean unsupportedRecovery)
            throws Exception {
        var events = new AtomicInteger();
        var hooks = new AtomicInteger();
        var resumedEvents = new AtomicInteger();
        var resumedHooks = new AtomicInteger();
        try (var server = new Server(); var client = server.createClient()) {
            var resident = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setHooks(new SessionHooks().setOnSessionEnd((input, invocation) -> {
                                assertEquals("user_exit", input.reason());
                                hooks.incrementAndGet();
                                return CompletableFuture.completedFuture(null);
                            })))
                    .get(5, TimeUnit.SECONDS);
            String sessionId = resident.getSessionId();
            try (var subscription = resident.on(event -> events.incrementAndGet())) {
                server.holdResumeResponse = true;
                server.holdDetachResponse = true;
                var resuming = client.resumeSession(sessionId,
                        new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                .setAutoModeRecovery(unsupportedRecovery)
                                .setOnEvent(event -> resumedEvents.incrementAndGet())
                                .setHooks(new SessionHooks().setOnSessionEnd((input, invocation) -> {
                                    resumedHooks.incrementAndGet();
                                    return CompletableFuture.completedFuture(null);
                                })).setOnAutoModeSwitch((request, invocation) -> CompletableFuture
                                        .completedFuture(AutoModeSwitchResponse.NO)));
                var resumeRequest = server.resumes.poll(5, TimeUnit.SECONDS);
                assertNotNull(resumeRequest, "Resume must be pending before the resident closes");
                var closing = CompletableFuture.runAsync(resident::close);
                try (var finishDetach = (AutoCloseable) () -> {
                    var detachRequest = server.heldDetaches.poll(5, TimeUnit.SECONDS);
                    assertNotNull(detachRequest, "Graceful close must send detach");
                    server.completeDetach(detachRequest, "success");
                    closing.get(5, TimeUnit.SECONDS);
                }) {
                    assertTrue(server.detachEntered.await(5, TimeUnit.SECONDS), "Graceful close must await detach");
                    assertFalse(closing.isDone());
                    if (unsupportedRecovery) {
                        server.writeMessage(Map.of("jsonrpc", "2.0", "id", resumeRequest.get("id"), "result",
                                Map.of("sessionId", sessionId)));
                    } else {
                        server.writeMessage(Map.of("jsonrpc", "2.0", "id", resumeRequest.get("id"), "error",
                                Map.of("code", -32000, "message", "resume failed")));
                    }
                    var error = assertThrows(ExecutionException.class, () -> resuming.get(5, TimeUnit.SECONDS));
                    if (unsupportedRecovery) {
                        assertInstanceOf(UnsupportedOperationException.class, error.getCause());
                    } else {
                        assertTrue(error.getCause().getMessage().contains("resume failed"));
                    }

                    server.emitIdle(sessionId);
                    server.invokeSessionEnd(sessionId);
                    assertEquals(unsupportedRecovery ? 0 : 1, hooks.get());
                    assertEquals(unsupportedRecovery ? 0 : 1, events.get());
                    assertEquals(unsupportedRecovery ? 1 : 0, resumedHooks.get());
                    assertEquals(unsupportedRecovery ? 1 : 0, resumedEvents.get(),
                            "An accepted resume must keep its callbacks even without recovery support");
                    assertFalse(closing.isDone(), "Callbacks must complete before detach is acknowledged");
                }
                server.emitIdle(sessionId);
                client.ping("restored resident callback retirement barrier").get(5, TimeUnit.SECONDS);
                assertEquals(unsupportedRecovery ? 0 : 1, events.get(),
                        "Detach acknowledgement must still retire the resident callbacks");
                assertEquals(unsupportedRecovery ? 2 : 0, resumedEvents.get(),
                        "The retired resident must not remove an accepted resume's callback owner");
                assertEquals(1, server.detaches.get(), "Rejected resume must not detach the resident a second time");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"success", "rejected", "rpc-error"})
    void timedOutClosePreservesEventsUntilDetachAcknowledgement(String outcome) throws Exception {
        var events = new AtomicInteger();
        var hooks = new AtomicInteger();
        try (var server = new Server(); var client = server.createClient()) {
            server.holdDetachResponse = true;
            var session = client.createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                    .setHooks(new SessionHooks().setOnSessionEnd((input, invocation) -> {
                        assertEquals("user_exit", input.reason());
                        hooks.incrementAndGet();
                        return CompletableFuture.completedFuture(null);
                    }))).get(5, TimeUnit.SECONDS);
            try (var subscription = session.on(event -> events.incrementAndGet())) {
                var closing = CompletableFuture.runAsync(session::close);
                var detach = server.heldDetaches.poll(5, TimeUnit.SECONDS);
                assertNotNull(detach, "The peer must receive the actual detach request");
                try (var acknowledgement = (AutoCloseable) () -> {
                    server.completeDetach(detach, outcome);
                    client.ping("detach acknowledgement barrier").get(5, TimeUnit.SECONDS);
                }) {
                    server.emitIdle(session.getSessionId());
                    client.ping("before close timeout barrier").get(5, TimeUnit.SECONDS);
                    assertEquals(1, events.get());
                    var error = assertThrows(ExecutionException.class, () -> closing.get(10, TimeUnit.SECONDS));
                    var closeError = assertInstanceOf(IllegalStateException.class, error.getCause());
                    assertEquals("Failed to detach session " + session.getSessionId(), closeError.getMessage());
                    assertInstanceOf(TimeoutException.class, closeError.getCause());

                    server.emitIdle(session.getSessionId());
                    client.ping("after close timeout barrier").get(5, TimeUnit.SECONDS);
                    assertEquals(2, events.get(), "A caller timeout must not retire callbacks before the detach ACK");
                    server.invokeSessionEnd(session.getSessionId());
                    assertEquals(1, hooks.get(), "The session-end hook must survive the caller's timeout too");
                    session.close();
                    assertEquals(1, server.detaches.get(), "Timed-out close must not issue another detach");
                }
                server.emitIdle(session.getSessionId());
                client.ping("after detach acknowledgement barrier").get(5, TimeUnit.SECONDS);
                assertEquals(2, events.get(), "The actual detach ACK must retire callbacks");
            }
        }
    }

    @Test
    void interruptedCloseKeepsCallbacksAndLateAcknowledgementDoesNotRetireReplacement() throws Exception {
        var events = new AtomicInteger();
        var replacementEvents = new AtomicInteger();
        var closeFailure = new AtomicReference<RuntimeException>();
        var interrupted = new AtomicBoolean();
        try (var server = new Server(); var client = server.createClient()) {
            server.holdDetachResponse = true;
            var session = createSession(client);
            var closing = new Thread(() -> {
                try {
                    session.close();
                } catch (RuntimeException error) {
                    closeFailure.set(error);
                    interrupted.set(Thread.currentThread().isInterrupted());
                }
            }, "interrupted-session-close");
            try (var subscription = session.on(event -> events.incrementAndGet())) {
                var detach = new AtomicReference<JsonNode>();
                try (var join = (AutoCloseable) () -> {
                    closing.join(10000);
                    assertFalse(closing.isAlive(), "The owned close thread must stop");
                }; var acknowledgement = (AutoCloseable) () -> {
                    if (detach.get() != null) {
                        server.completeDetach(detach.get(), "success");
                    }
                }) {
                    closing.start();
                    detach.set(server.heldDetaches.poll(5, TimeUnit.SECONDS));
                    assertNotNull(detach.get(), "The peer must admit detach before interruption");
                    closing.interrupt();
                    closing.join(5000);
                    assertFalse(closing.isAlive(), "Interrupted close must finish without waiting for the ACK");
                    var error = assertInstanceOf(IllegalStateException.class, closeFailure.get());
                    assertEquals("Failed to detach session " + session.getSessionId(), error.getMessage());
                    assertInstanceOf(InterruptedException.class, error.getCause());
                    assertTrue(interrupted.get(), "Close must restore the caller's interrupt flag");
                    server.emitIdle(session.getSessionId());
                    client.ping("interrupted close callback barrier").get(5, TimeUnit.SECONDS);
                    assertEquals(1, events.get(), "Interrupting the caller must not retire pending detach callbacks");

                    var replacement = client
                            .resumeSession(session.getSessionId(),
                                    new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                            .get(5, TimeUnit.SECONDS);
                    try (var replacementSubscription = replacement.on(event -> replacementEvents.incrementAndGet())) {
                        server.emitIdle(replacement.getSessionId());
                        client.ping("replacement callback barrier").get(5, TimeUnit.SECONDS);
                        assertEquals(1, replacementEvents.get());
                        server.completeDetach(detach.get(), "success");
                        detach.set(null);
                        server.emitIdle(replacement.getSessionId());
                        client.ping("old detach settled barrier").get(5, TimeUnit.SECONDS);
                        assertEquals(2, replacementEvents.get(),
                                "An old detach completion must not retire the replacement's callbacks");
                        assertEquals(1, events.get(), "New-generation events must not reach old subscriptions");
                        client.forceStop().get(5, TimeUnit.SECONDS);
                    }
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void transportFailureSettlesPendingDetach(boolean timeoutFirst) throws Exception {
        try (var server = new Server(); var client = server.createClient()) {
            server.holdDetachResponse = true;
            var session = createSession(client);
            var closing = CompletableFuture.runAsync(session::close);
            assertNotNull(server.heldDetaches.poll(5, TimeUnit.SECONDS), "Detach must be pending on the real peer");
            if (timeoutFirst) {
                var error = assertThrows(ExecutionException.class, () -> closing.get(10, TimeUnit.SECONDS));
                assertInstanceOf(TimeoutException.class, error.getCause().getCause());
            }
            server.socket.shutdownOutput();
            if (!timeoutFirst) {
                var error = assertThrows(ExecutionException.class, () -> closing.get(5, TimeUnit.SECONDS));
                var closeError = assertInstanceOf(IllegalStateException.class, error.getCause());
                var rpcError = assertInstanceOf(ExecutionException.class, closeError.getCause());
                assertInstanceOf(IOException.class, rpcError.getCause());
                assertEquals("Client closed", rpcError.getCause().getMessage());
            }
            var pingError = assertThrows(ExecutionException.class,
                    () -> client.ping("closed transport barrier").get(5, TimeUnit.SECONDS));
            assertInstanceOf(IOException.class, pingError.getCause());
            assertThrows(IllegalStateException.class, () -> session.send("after transport failure"));
            session.close();
            assertEquals(1, server.detaches.get(), "A settled failed detach must not be sent again");
            client.forceStop().get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void forceStopAfterCloseTimeoutDropsUnadmittedCallbacks() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var dispatchThread = new AtomicReference<Thread>();
        var laterCalls = new AtomicInteger();
        var errorCalls = new AtomicInteger();
        try (var server = new Server(); var client = server.createClient()) {
            server.holdDetachResponse = true;
            var session = createSession(client);
            session.setEventErrorHandler((event, error) -> errorCalls.incrementAndGet());
            try (var first = session.on(event -> {
                dispatchThread.set(Thread.currentThread());
                entered.countDown();
                awaitRelease(release);
                throw new IllegalStateException("Admitted callback failed after forced retirement");
            }); var second = session.on(event -> laterCalls.incrementAndGet())) {
                var closing = CompletableFuture.runAsync(session::close);
                assertNotNull(server.heldDetaches.poll(5, TimeUnit.SECONDS));
                var error = assertThrows(ExecutionException.class, () -> closing.get(10, TimeUnit.SECONDS));
                assertInstanceOf(TimeoutException.class, error.getCause().getCause());
                try (var gate = (AutoCloseable) () -> {
                    release.countDown();
                    var thread = dispatchThread.get();
                    if (thread != null) {
                        thread.join(5000);
                        assertFalse(thread.isAlive(), "The owned reader must exit after callback release");
                    }
                }) {
                    server.emitIdle(session.getSessionId());
                    assertTrue(entered.await(5, TimeUnit.SECONDS), "Post-timeout callback must still be admitted");
                    client.forceStop().get(5, TimeUnit.SECONDS);
                    assertEquals(1, release.getCount(), "Force stop must not join the admitted callback");
                }
                assertEquals(0, laterCalls.get(), "Force stop must drop the remaining dispatch snapshot");
                assertEquals(0, errorCalls.get(), "Force stop must also retire the error callback");
                session.close();
                assertEquals(1, server.detaches.get(), "Force stop must not resend the pending detach");
            }
        }
    }

    @Test
    void forceStopTerminatesRetainedSessionsWithoutDetachRpc() throws Exception {
        try (var server = new Server(); var client = server.createClient()) {
            var session = createSession(client);
            client.forceStop().get(5, TimeUnit.SECONDS);

            assertThrows(IllegalStateException.class, () -> session.on(event -> fail("Retired handler")));
            assertThrows(IllegalStateException.class,
                    () -> session.on(SessionIdleEvent.class, event -> fail("Retired typed handler")));
            assertThrows(IllegalStateException.class, () -> session.send("after force stop"));
            session.close();
            assertEquals(0, server.detaches.get(), "Local retirement must not detach over RPC");
        }
    }

    @Test
    void forceStopDropsRemainingSnapshotHandlersWithoutJoiningAnAdmittedHandler() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var dispatchThread = new AtomicReference<Thread>();
        var laterCalls = new AtomicInteger();
        try (var server = new Server(); var client = server.createClient()) {
            var session = createSession(client);
            try (var first = session.on(event -> {
                dispatchThread.set(Thread.currentThread());
                entered.countDown();
                awaitRelease(release);
            }); var second = session.on(event -> laterCalls.incrementAndGet())) {
                try {
                    server.emitIdle(session.getSessionId());
                    assertTrue(entered.await(5, TimeUnit.SECONDS), "The first handler must be admitted");
                    client.forceStop().get(5, TimeUnit.SECONDS);
                    assertEquals(1, release.getCount(), "Force stop must not require releasing the user handler");
                } finally {
                    release.countDown();
                    Thread thread = dispatchThread.get();
                    if (thread != null) {
                        thread.join(5000);
                        assertFalse(thread.isAlive(), "The stopped event reader must finish after its handler");
                    }
                }
                assertEquals(0, laterCalls.get(), "Retirement must discard the retained dispatch snapshot");
            }
        }
    }

    @Test
    void forceStopRetiresErrorCallbacksWhileGracefulCloseAwaitsDetach() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var dispatchThread = new AtomicReference<Thread>();
        var errorCalls = new AtomicInteger();
        try (var server = new Server(); var client = server.createClient()) {
            server.holdDetachResponse = true;
            var session = createSession(client);
            session.setEventErrorHandler((event, error) -> errorCalls.incrementAndGet());
            try (var first = session.on(event -> {
                dispatchThread.set(Thread.currentThread());
                entered.countDown();
                awaitRelease(release);
                throw new IllegalStateException("Admitted handler failed after forced retirement");
            })) {
                try {
                    server.emitIdle(session.getSessionId());
                    assertTrue(entered.await(5, TimeUnit.SECONDS), "The first snapshot handler must be admitted");
                    var closing = CompletableFuture.runAsync(session::close);
                    assertTrue(server.detachEntered.await(5, TimeUnit.SECONDS), "Graceful close must await detach");
                    // Prevent graceful close's final retirement from masking force escalation.
                    synchronized (session) {
                        client.forceStop().get(5, TimeUnit.SECONDS);
                        assertFalse(closing.isDone(), "Graceful close must still await its final retirement");
                        release.countDown();
                        Thread thread = dispatchThread.get();
                        thread.join(5000);
                        assertFalse(thread.isAlive(), "The stopped reader must finish its admitted handler");
                        assertEquals(0, errorCalls.get(), "Force stop must discard the unadmitted error callback");
                    }
                    var detachError = assertThrows(ExecutionException.class, () -> closing.get(5, TimeUnit.SECONDS));
                    assertInstanceOf(IllegalStateException.class, detachError.getCause());
                } finally {
                    release.countDown();
                    Thread thread = dispatchThread.get();
                    if (thread != null) {
                        thread.join(5000);
                        assertFalse(thread.isAlive(), "The event reader must finish after forced cleanup");
                    }
                }
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"false, false, false", "false, true, false", "true, false, false", "true, true, false",
            "false, false, true", "false, true, true", "true, false, true", "true, true, true"})
    void retirementPreservesTerminalOutcomeWhileSchedulerHandoffIsBlocked(boolean terminalError, boolean force,
            boolean firstFallback) throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var server = new Server(); var client = server.createClient()) {
            var session = createSession(client);
            server.completionSupported = firstFallback;
            var occupying = session.sendAndWait(new MessageOptions().setPrompt("occupy scheduler"), 0);
            var continuation = occupying.whenComplete((value, error) -> {
                entered.countDown();
                awaitRelease(release);
            });
            try (var gate = (AutoCloseable) release::countDown) {
                client.ping("first send acknowledgment").get(5, TimeUnit.SECONDS);
                if (firstFallback) {
                    var request = server.completions.poll(5, TimeUnit.SECONDS);
                    assertNotNull(request, "The occupying call must use the supported completion RPC");
                    server.completionSupported = false;
                    server.writeMessage(Map.of("jsonrpc", "2.0", "id", request.get("id"), "result",
                            Map.of("messageId", "occupying-request", "aborted", false)));
                } else {
                    server.emitEvent(session.getSessionId(), "user.message",
                            Map.of("messageId", "occupy scheduler", "content", "occupy scheduler"));
                    server.emitIdle(session.getSessionId());
                }
                assertTrue(entered.await(5, TimeUnit.SECONDS), "A continuation must occupy the session scheduler");

                var completed = session.sendAndWait(new MessageOptions().setPrompt("completed terminal"), 0);
                if (firstFallback) {
                    assertEquals("completed terminal", server.sends.poll(5, TimeUnit.SECONDS),
                            "The first fallback must admit its legacy wait before the terminal event");
                }
                client.ping("second send acknowledgment").get(5, TimeUnit.SECONDS);
                if (terminalError) {
                    server.emitEvent(session.getSessionId(), "session.error",
                            Map.of("errorType", "query", "message", "original terminal failure"));
                } else {
                    server.emitEvent(session.getSessionId(), "assistant.message",
                            Map.of("messageId", "terminal-message", "content", "completed before retirement",
                                    "originatingMessageId", "completed terminal"));
                    server.emitIdle(session.getSessionId());
                }
                client.ping("terminal dispatch acknowledgment").get(5, TimeUnit.SECONDS);
                assertFalse(completed.isDone(), "The received terminal outcome must still await scheduler handoff");

                if (force) {
                    client.forceStop().get(5, TimeUnit.SECONDS);
                } else {
                    CompletableFuture.runAsync(session::close).get(5, TimeUnit.SECONDS);
                }
                if (terminalError) {
                    var error = assertThrows(ExecutionException.class, () -> completed.get(5, TimeUnit.SECONDS));
                    assertEquals("Session error: original terminal failure", error.getCause().getMessage());
                } else {
                    assertEquals("completed before retirement", completed.get(5, TimeUnit.SECONDS).getData().content());
                }
                assertEquals(1, release.getCount(), "Retirement must not join the admitted continuation");
            }
            continuation.get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void forceStopFailsAcknowledgedPlainAndStructuredWaitsWithNoTimeout() throws Exception {
        var entered = new CountDownLatch(2);
        var release = new CountDownLatch(1);
        try (var server = new Server(); var client = server.createClient()) {
            var session = createSession(client);
            var plain = session.sendAndWait(new MessageOptions().setPrompt("plain"), 0);
            var structured = session.sendAndWait(new MessageOptions().setPrompt("structured"), Answer.class, 0);
            assertNotNull(server.sends.poll(5, TimeUnit.SECONDS), "First legacy send must be admitted");
            assertNotNull(server.sends.poll(5, TimeUnit.SECONDS), "Second legacy send must be admitted");
            client.ping("send acknowledgment barrier").get(5, TimeUnit.SECONDS);
            assertFalse(plain.isDone());
            assertFalse(structured.isDone());

            var plainContinuation = plain.whenComplete((value, error) -> {
                entered.countDown();
                awaitRelease(release);
            });
            var structuredContinuation = structured.whenComplete((value, error) -> {
                entered.countDown();
                awaitRelease(release);
            });
            try (var gate = (AutoCloseable) release::countDown) {
                client.forceStop().get(5, TimeUnit.SECONDS);
                assertTrue(entered.await(5, TimeUnit.SECONDS),
                        "Both waits must retire even while their application continuations are blocked");
                var plainError = assertThrows(ExecutionException.class, () -> plain.get(5, TimeUnit.SECONDS));
                var structuredError = assertThrows(ExecutionException.class, () -> structured.get(5, TimeUnit.SECONDS));
                assertInstanceOf(IllegalStateException.class, plainError.getCause());
                assertInstanceOf(IllegalStateException.class, structuredError.getCause());
                assertTrue(plainError.getCause().getMessage().contains("closed"));
                assertTrue(structuredError.getCause().getMessage().contains("closed"));
            }
            assertThrows(ExecutionException.class, () -> plainContinuation.get(5, TimeUnit.SECONDS));
            assertThrows(ExecutionException.class, () -> structuredContinuation.get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void gracefulCloseFailsAcknowledgedWaitsWithoutJoiningTheirContinuations() throws Exception {
        var entered = new CountDownLatch(2);
        var release = new CountDownLatch(1);
        try (var server = new Server(); var client = server.createClient()) {
            var session = createSession(client);
            var plain = session.sendAndWait(new MessageOptions().setPrompt("plain"), 0);
            var structured = session.sendAndWait(new MessageOptions().setPrompt("structured"), Answer.class, 0);
            assertNotNull(server.sends.poll(5, TimeUnit.SECONDS), "First legacy send must be admitted");
            assertNotNull(server.sends.poll(5, TimeUnit.SECONDS), "Second legacy send must be admitted");
            client.ping("send acknowledgment barrier").get(5, TimeUnit.SECONDS);
            assertFalse(plain.isDone());
            assertFalse(structured.isDone());

            var plainContinuation = plain.whenComplete((value, error) -> {
                entered.countDown();
                awaitRelease(release);
            });
            var structuredContinuation = structured.whenComplete((value, error) -> {
                entered.countDown();
                awaitRelease(release);
            });
            try (var gate = (AutoCloseable) release::countDown) {
                CompletableFuture.runAsync(session::close).get(5, TimeUnit.SECONDS);
                assertTrue(entered.await(5, TimeUnit.SECONDS), "Retirement must admit both blocked continuations");
                var plainError = assertThrows(ExecutionException.class, () -> plain.get(5, TimeUnit.SECONDS));
                var structuredError = assertThrows(ExecutionException.class, () -> structured.get(5, TimeUnit.SECONDS));
                assertInstanceOf(IllegalStateException.class, plainError.getCause());
                assertInstanceOf(IllegalStateException.class, structuredError.getCause());
                assertEquals("Session closed before response completed", plainError.getCause().getMessage());
                assertEquals("Session closed before response completed", structuredError.getCause().getMessage());
                assertEquals(1, server.detaches.get(), "Graceful close must still acknowledge one detach");
                session.close();
                assertEquals(1, server.detaches.get(), "Repeated close must not detach again");
            }
            assertThrows(ExecutionException.class, () -> plainContinuation.get(5, TimeUnit.SECONDS));
            assertThrows(ExecutionException.class, () -> structuredContinuation.get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void duplicateUntypedSubscriptionsStillDispatchOnceAndShareUnsubscription() throws Exception {
        var calls = new AtomicInteger();
        try (var server = new Server(); var client = server.createClient()) {
            var session = createSession(client);
            Consumer<SessionEvent> handler = event -> calls.incrementAndGet();
            try (var first = session.on(handler); var second = session.on(handler)) {
                server.emitIdle(session.getSessionId());
                client.ping("first dispatch barrier").get(5, TimeUnit.SECONDS);
                assertEquals(1, calls.get());

                first.close();
                server.emitIdle(session.getSessionId());
                client.ping("unsubscription barrier").get(5, TimeUnit.SECONDS);
                assertEquals(1, calls.get());
                client.forceStop().get(5, TimeUnit.SECONDS);
            }
        }
    }

    @Test
    void stopRetiresSessionsEvenWhenTheCallersExecutorNeverStartsDetach() throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var server = new Server(); var client = server.createClient(executor)) {
            var session = createSession(client);
            var waiting = session.sendAndWait(new MessageOptions().setPrompt("pending legacy response"), 0);
            client.ping("legacy send acknowledged").get(5, TimeUnit.SECONDS);
            assertFalse(waiting.isDone());
            executor.execute(() -> {
                entered.countDown();
                awaitRelease(release);
            });
            assertTrue(entered.await(5, TimeUnit.SECONDS), "The caller's executor must be occupied");

            client.stop().get(8, TimeUnit.SECONDS);
            executor.shutdownNow();
            assertThrows(IllegalStateException.class, () -> session.send("after stop"));
            var error = assertThrows(ExecutionException.class, () -> waiting.get(5, TimeUnit.SECONDS));
            assertEquals("Session closed before response completed", error.getCause().getMessage());
            assertEquals(0, server.detaches.get(), "The blocked executor must not have sent detach");
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "The owned executor must stop");
        }
    }

    @Test
    void stopDoesNotJoinToolCancellationContinuations() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var tool = new CompletableFuture<Object>();
        var continuation = tool.whenComplete((value, error) -> {
            entered.countDown();
            awaitRelease(release);
        });
        try (var server = new Server(); var client = server.createClient(Runnable::run)) {
            var session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setTools(
                            List.of(ToolDefinition.create("blocked_tool", "Blocks", Map.of(), invocation -> tool))))
                    .get(5, TimeUnit.SECONDS);
            var otherSession = createSession(client);
            server.emitEvent(session.getSessionId(), "external_tool.requested",
                    Map.of("requestId", "pending-tool", "sessionId", session.getSessionId(), "toolCallId", "tool-call",
                            "toolName", "blocked_tool", "arguments", Map.of()));
            client.ping("tool attached barrier").get(5, TimeUnit.SECONDS);

            var stopCall = CompletableFuture.supplyAsync(client::stop);
            try {
                var stopping = stopCall.get(2, TimeUnit.SECONDS);
                assertTrue(entered.await(5, TimeUnit.SECONDS), "Retirement must cancel the application-owned future");
                // Sessions retire on separate cleanup threads; stop completing is the barrier.
                stopping.get(8, TimeUnit.SECONDS);
                assertEquals(1, release.getCount(), "Shutdown must not join an application cancellation continuation");
                assertThrows(IllegalStateException.class, () -> session.send("after stop"));
                assertThrows(IllegalStateException.class, () -> otherSession.send("after stop"));
            } finally {
                release.countDown();
                stopCall.get(5, TimeUnit.SECONDS).get(8, TimeUnit.SECONDS);
                assertThrows(ExecutionException.class, () -> continuation.get(5, TimeUnit.SECONDS));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @AllowCopilotExperimental
    void stopReturnsBeforeAnInlineDetachWriteUnblocks(boolean rejected) throws Exception {
        var reject = new AtomicBoolean();
        Executor executor = task -> {
            if (reject.get()) {
                throw new java.util.concurrent.RejectedExecutionException("Controlled executor rejection");
            }
            task.run();
        };
        var block = new AtomicBoolean();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var server = new Server();
                var client = new CopilotClient(new CopilotClientOptions()
                        .setConnection(RuntimeConnection.forInProcess()).setExecutor(executor))) {
            client.setInProcessTransportFactory(options -> {
                var socket = new Socket("127.0.0.1", server.listener.getLocalPort());
                var output = new FilterOutputStream(socket.getOutputStream()) {
                    @Override
                    public void write(byte[] bytes, int offset, int length) throws IOException {
                        if (block.get()) {
                            entered.countDown();
                            awaitRelease(release);
                        }
                        out.write(bytes, offset, length);
                    }
                };
                return new CopilotClient.InProcessTransport(socket.getInputStream(), output, null);
            });
            var session = createSession(client);
            reject.set(rejected);
            block.set(true);
            var stopCall = CompletableFuture.supplyAsync(client::stop);
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS), "The detach send must reach the blocked transport");
                var stopping = stopCall.get(2, TimeUnit.SECONDS);
                stopping.get(8, TimeUnit.SECONDS);
                assertEquals(1, release.getCount(), "The detach deadline must not require unblocking the write");
                assertThrows(IllegalStateException.class, () -> session.send("after stop"));
            } finally {
                release.countDown();
                stopCall.get(5, TimeUnit.SECONDS).get(8, TimeUnit.SECONDS);
            }
        }
    }

    @Test
    void oldStopDoesNotClearSessionsOrTokenProvidersFromANewConnection() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var events = new AtomicInteger();
        var toolEntered = new CountDownLatch(1);
        try (var server = new Server(); var client = server.createClient()) {
            var oldSession = createSession(client);
            server.holdPing = true;
            var ping = client.ping("hold old cleanup");
            assertNotNull(server.heldPings.poll(5, TimeUnit.SECONDS), "The old request must remain pending at cleanup");
            var continuation = ping.whenComplete((value, error) -> {
                entered.countDown();
                awaitRelease(release);
            });
            var stopping = client.stop();
            try (var nextServer = new Server(server.listener)) {
                try {
                    assertTrue(entered.await(5, TimeUnit.SECONDS), "The old transport cleanup must be paused");
                    var replacement = client.createSession(new SessionConfig().setSessionId(oldSession.getSessionId())
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setTools(List
                                    .of(ToolDefinition.create("replacement_tool", "Blocks", Map.of(), invocation -> {
                                        toolEntered.countDown();
                                        return new CompletableFuture<>();
                                    })))
                            .setGitHubTokenProvider(args -> CompletableFuture
                                    .completedFuture(GitHubTokenProviderResult.token("replacement-token", 3600))))
                            .get(5, TimeUnit.SECONDS);
                    var request = nextServer.creates.poll(5, TimeUnit.SECONDS);
                    assertNotNull(request);
                    try (var subscription = replacement.on(event -> events.incrementAndGet())) {
                        release.countDown();
                        stopping.get(5, TimeUnit.SECONDS);
                        assertThrows(ExecutionException.class, () -> continuation.get(5, TimeUnit.SECONDS));
                        nextServer.emitIdle(replacement.getSessionId());
                        client.ping("replacement routing barrier").get(5, TimeUnit.SECONDS);
                        assertEquals(1, events.get(), "An old stop must preserve the new session's routing");
                        var tokenReply = nextServer.invokeToken(
                                request.path("params").path("gitHubTokenProviderRegistrationId").asText(),
                                replacement.getSessionId());
                        assertFalse(tokenReply.has("error"),
                                "An old stop must preserve new token providers: " + tokenReply);
                        assertEquals("replacement-token", tokenReply.path("result").path("accessToken").asText());
                        nextServer.emitEvent(replacement.getSessionId(), "external_tool.requested",
                                Map.of("requestId", "replacement-request", "sessionId", replacement.getSessionId(),
                                        "toolCallId", "replacement-call", "toolName", "replacement_tool", "arguments",
                                        Map.of()));
                        assertTrue(toolEntered.await(5, TimeUnit.SECONDS),
                                "An old transport close must not retire the replacement's tools");
                    }
                    client.stop().get(5, TimeUnit.SECONDS);
                } finally {
                    release.countDown();
                    stopping.get(5, TimeUnit.SECONDS);
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void callerCompletionOfStopCannotSuppressRegistryCleanup(boolean timeout) throws Exception {
        var queueTasks = new AtomicBoolean();
        var queued = new CountDownLatch(1);
        var tasks = new java.util.concurrent.ConcurrentLinkedQueue<Runnable>();
        Executor executor = task -> {
            if (queueTasks.get()) {
                tasks.add(task);
                queued.countDown();
            } else {
                task.run();
            }
        };
        try (var server = new Server(); var client = server.createClient(executor)) {
            var session = createSession(client);
            try (var nextServer = new Server(server.listener)) {
                queueTasks.set(true);
                var stopping = client.stop();
                assertTrue(queued.await(5, TimeUnit.SECONDS), "The caller-owned executor must accept queued detach");
                if (timeout) {
                    var error = assertThrows(ExecutionException.class,
                            () -> stopping.orTimeout(1, TimeUnit.MILLISECONDS).get(5, TimeUnit.SECONDS));
                    assertInstanceOf(TimeoutException.class, error.getCause());
                } else {
                    assertTrue(stopping.cancel(false));
                }
                assertTrue(server.peerClosed.await(8, TimeUnit.SECONDS), "Private shutdown must still close transport");
                tasks.clear();
                queueTasks.set(false);
                client.start().get(5, TimeUnit.SECONDS);
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                JsonNode reply = null;
                do {
                    var observed = nextServer.sessionEndReply(session.getSessionId(), deadline - System.nanoTime());
                    if (observed == null) {
                        break;
                    }
                    reply = observed;
                    if (reply.has("error")) {
                        break;
                    }
                    TimeUnit.MILLISECONDS.sleep(10);
                } while (System.nanoTime() < deadline);
                assertNotNull(reply, "The new peer must receive a response about the retired session");
                assertEquals("Unknown session " + session.getSessionId(), reply.path("error").path("message").asText(),
                        "Caller completion must not retain old session registrations: " + reply);
                client.stop().get(5, TimeUnit.SECONDS);
            } finally {
                queueTasks.set(false);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void stopRoutesCallbacksUntilDetachSettlesWithInlineExecutors(boolean rejected) throws Exception {
        var reject = new AtomicBoolean();
        Executor executor = task -> {
            if (reject.get()) {
                throw new java.util.concurrent.RejectedExecutionException("Controlled executor rejection");
            }
            task.run();
        };
        var events = new AtomicInteger();
        var hooks = new AtomicInteger();
        try (var server = new Server(); var client = server.createClient(executor)) {
            server.holdDetachResponse = true;
            var session = client.createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                    .setHooks(new SessionHooks().setOnSessionEnd((input, invocation) -> {
                        hooks.incrementAndGet();
                        return CompletableFuture.completedFuture(null);
                    }))).get(5, TimeUnit.SECONDS);
            try (var subscription = session.on(event -> events.incrementAndGet())) {
                reject.set(rejected);
                var stopping = client.stop();
                var detach = server.heldDetaches.poll(5, TimeUnit.SECONDS);
                assertNotNull(detach, "The peer must receive detach before emitting its final callbacks");
                server.emitIdle(session.getSessionId());
                client.ping("stop callback routing barrier").get(5, TimeUnit.SECONDS);
                assertEquals(1, events.get(), "Stop must retain event routing until detach settles");
                server.invokeSessionEnd(session.getSessionId());
                assertEquals(1, hooks.get(), "Stop must retain hook routing until detach settles");
                server.completeDetach(detach, "success");
                stopping.get(5, TimeUnit.SECONDS);
                assertThrows(IllegalStateException.class, () -> session.on(event -> fail("Retired handler")));
            }
        }
    }

    @Test
    void stopSharesOneDetachDeadlineWithoutHoldingTheCallersExecutor() throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        try (var server = new Server(); var client = server.createClient(executor)) {
            server.holdDetachResponse = true;
            for (int i = 0; i < 3; i++) {
                createSession(client);
            }
            long started = System.nanoTime();
            var stopping = client.stop();
            for (int i = 0; i < 3; i++) {
                assertNotNull(server.heldDetaches.poll(2, TimeUnit.SECONDS),
                        "Every detach must be sent without waiting for an earlier one");
            }
            CompletableFuture.runAsync(() -> {
            }, executor).get(2, TimeUnit.SECONDS);
            stopping.get(CopilotSession.DETACH_TIMEOUT_SECONDS + 3, TimeUnit.SECONDS);
            assertTrue(
                    System.nanoTime() - started < TimeUnit.SECONDS.toNanos(2 * CopilotSession.DETACH_TIMEOUT_SECONDS),
                    "Unanswered detaches must share one deadline");
            assertEquals(3, server.detaches.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void stopReturnsWhileASendHoldsTheSessionMonitor() throws Exception {
        var held = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var server = new Server(); var client = server.createClient()) {
            var session = createSession(client);
            // A legacy sendAndWait fallback holds this monitor while it writes
            // session.send.
            var holder = new Thread(() -> {
                synchronized (session) {
                    held.countDown();
                    awaitRelease(release);
                }
            }, "session-monitor-holder");
            holder.setDaemon(true);
            holder.start();
            try {
                assertTrue(held.await(5, TimeUnit.SECONDS), "The session monitor must be held");
                var stopping = CompletableFuture.supplyAsync(client::stop).get(2, TimeUnit.SECONDS);
                assertFalse(stopping.isDone(), "Retirement must still be waiting for the monitor");
                release.countDown();
                stopping.get(CopilotSession.DETACH_TIMEOUT_SECONDS + 3, TimeUnit.SECONDS);
                assertThrows(IllegalStateException.class, () -> session.send("after stop"));
                assertEquals(1, server.detaches.get());
            } finally {
                release.countDown();
                holder.join(5000);
            }
        }
    }

    @Test
    void stopRetiresTheSessionAnInterruptedResumeRestores() throws Exception {
        try (var server = new Server(); var client = server.createClient()) {
            var original = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setGitHubTokenProvider(args -> CompletableFuture
                                    .completedFuture(GitHubTokenProviderResult.token("original-token", 3600))))
                    .get(5, TimeUnit.SECONDS);
            var create = server.creates.poll(5, TimeUnit.SECONDS);
            assertNotNull(create);
            String registrationId = create.path("params").path("gitHubTokenProviderRegistrationId").asText();
            assertFalse(registrationId.isEmpty(), "The original session must register its token provider");

            server.holdResume = true;
            var resuming = client.resumeSession(original.getSessionId(),
                    new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL));
            assertNotNull(server.heldResumes.poll(5, TimeUnit.SECONDS), "The resume must be pending at stop");

            client.stop().get(CopilotSession.DETACH_TIMEOUT_SECONDS + 3, TimeUnit.SECONDS);
            assertThrows(ExecutionException.class, () -> resuming.get(5, TimeUnit.SECONDS));
            assertThrows(IllegalStateException.class, () -> original.send("after stop"),
                    "The session the failed resume restored must be retired");

            try (var nextServer = new Server(server.listener)) {
                createSession(client);
                var reply = nextServer.invokeToken(registrationId, original.getSessionId());
                assertTrue(reply.has("error"), "A stopped session's token provider must not survive: " + reply);
                client.stop().get(CopilotSession.DETACH_TIMEOUT_SECONDS + 3, TimeUnit.SECONDS);
            }
        }
    }

    private static CopilotSession createSession(CopilotClient client) throws Exception {
        return client.createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get(5,
                TimeUnit.SECONDS);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sendAndWaitUsesOnlyTheRuntimeOwnedResponse(boolean typed) throws Exception {
        try (var server = new Server(); var client = server.createClient()) {
            server.completionSupported = true;
            try (var session = createSession(client)) {
                var idles = new AtomicInteger();
                try (var subscription = session.on(SessionIdleEvent.class, event -> idles.incrementAndGet())) {
                    var options = new MessageOptions().setPrompt("question");
                    var waiting = typed
                            ? session.sendAndWait(options, Answer.class, 0)
                            : session.sendAndWait(options, 0);
                    var request = server.completions.poll(5, TimeUnit.SECONDS);
                    assertNotNull(request, "The existing helper must use the private completion RPC");
                    assertEquals("question", request.path("params").path("prompt").asText());
                    server.emitEvent(session.getSessionId(), "session.error",
                            Map.of("errorType", "diagnostic", "message", "unrelated error"));
                    server.emitIdle(session.getSessionId());
                    var response = server.emitEvent(session.getSessionId(), "assistant.message",
                            Map.of("messageId", "owned-response", "content", "{\"value\":42}"));
                    var idle = server.emitEvent(session.getSessionId(), "session.autopilot_stopped",
                            Map.of("reason", "task_complete", "requestId", "owned-request"));
                    server.writeMessage(Map.of("jsonrpc", "2.0", "id", request.get("id"), "result",
                            Map.of("messageId", "owned-request", "response", response, "aborted", false,
                                    "completionEventId", idle.get("id"))));
                    Object result = waiting.get(5, TimeUnit.SECONDS);
                    if (typed) {
                        assertEquals(new Answer(42), result);
                    } else {
                        var assistant = assertInstanceOf(com.github.copilot.generated.AssistantMessageEvent.class,
                                result);
                        assertEquals(response.get("id"), assistant.getId().toString());
                        assertEquals("{\"value\":42}", assistant.getData().content());
                    }
                    assertEquals(1, idles.get());
                }
            }
        }
    }

    @Test
    void cancellingSendAndWaitCancelsOnlyItsPrivateRequest() throws Exception {
        try (var server = new Server(); var client = server.createClient()) {
            server.completionSupported = true;
            try (var session = createSession(client)) {
                var waiting = session.sendAndWait(new MessageOptions().setPrompt("question"), 0);
                var request = server.completions.poll(5, TimeUnit.SECONDS);
                assertNotNull(request);
                assertTrue(waiting.cancel(true));
                assertEquals(request.get("id"), server.cancellations.poll(5, TimeUnit.SECONDS));
                assertEquals(0, server.detaches.get(), "Cancelling a wait must not close the session");
                assertNotNull(client.ping("still connected").get(5, TimeUnit.SECONDS));
            }
        }
    }

    private static void awaitRelease(CountDownLatch release) {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    release.await();
                    return;
                } catch (InterruptedException error) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    void toolResultFailureIsAnsweredWhileAnotherSessionToolOccupiesExecutor() throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        var toolEntered = new CountDownLatch(1);
        var releaseTool = new CountDownLatch(1);
        try (var server = new Server(); var client = server.createClient(executor)) {
            server.holdToolReplies = true;
            var first = client
                    .createSession(
                            new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                    .setTools(List.of(ToolDefinition.create("delivers", "Returns a result", Map.of(),
                                            invocation -> CompletableFuture.completedFuture("ok")))))
                    .get(5, TimeUnit.SECONDS);
            var second = client.createSession(
                    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setTools(List
                            .of(ToolDefinition.create("blocks", "Waits for the first session", Map.of(), invocation -> {
                                toolEntered.countDown();
                                awaitRelease(releaseTool);
                                return new CompletableFuture<>();
                            }))))
                    .get(5, TimeUnit.SECONDS);
            try {
                server.emitEvent(first.getSessionId(), "external_tool.requested",
                        Map.of("requestId", "request-delivers", "sessionId", first.getSessionId(), "toolCallId",
                                "call-delivers", "toolName", "delivers", "arguments", Map.of()));
                var delivery = server.toolReplies.poll(5, TimeUnit.SECONDS);
                assertNotNull(delivery, "The first tool result must reach the transport");
                assertEquals("ok", delivery.path("params").path("result").path("textResultForLlm").asText());

                server.emitEvent(second.getSessionId(), "external_tool.requested",
                        Map.of("requestId", "request-blocks", "sessionId", second.getSessionId(), "toolCallId",
                                "call-blocks", "toolName", "blocks", "arguments", Map.of()));
                assertTrue(toolEntered.await(5, TimeUnit.SECONDS), "The second tool must occupy the sole worker");
                server.writeMessage(Map.of("jsonrpc", "2.0", "id", delivery.get("id"), "error",
                        Map.of("code", -32603, "message", "tool result rejected")));

                var answer = server.toolReplies.poll(5, TimeUnit.SECONDS);
                assertNotNull(answer, "The failed result must be answered without waiting for the blocked handler");
                var params = answer.path("params");
                assertEquals(first.getSessionId(), params.path("sessionId").asText());
                assertEquals("request-delivers", params.path("requestId").asText());
                assertEquals("tool result rejected", params.path("error").asText());
                assertTrue(params.path("result").isMissingNode() || params.path("result").isNull(), params.toString());
                server.writeMessage(Map.of("jsonrpc", "2.0", "id", answer.get("id"), "result", Map.of()));
            } finally {
                releaseTool.countDown();
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "The owned handler executor must stop");
        }
    }

    private static final class Server implements AutoCloseable {

        private static final ObjectMapper MAPPER = new ObjectMapper();
        private final ServerSocket listener;
        private final Thread thread;
        private final AtomicInteger detaches = new AtomicInteger();
        private final CountDownLatch detachEntered = new CountDownLatch(1);
        private final CountDownLatch peerClosed = new CountDownLatch(1);
        private final java.util.concurrent.BlockingQueue<JsonNode> heldDetaches = new java.util.concurrent.LinkedBlockingQueue<>();
        private final java.util.concurrent.BlockingQueue<JsonNode> hookReplies = new java.util.concurrent.LinkedBlockingQueue<>();
        private final java.util.concurrent.BlockingQueue<JsonNode> creates = new java.util.concurrent.LinkedBlockingQueue<>();
        private final java.util.concurrent.BlockingQueue<JsonNode> heldPings = new java.util.concurrent.LinkedBlockingQueue<>();
        private final java.util.concurrent.BlockingQueue<JsonNode> heldResumes = new java.util.concurrent.LinkedBlockingQueue<>();
        private final java.util.concurrent.BlockingQueue<JsonNode> heldConnects = new java.util.concurrent.LinkedBlockingQueue<>();
        private final java.util.concurrent.BlockingQueue<JsonNode> toolReplies = new java.util.concurrent.LinkedBlockingQueue<>();
        private volatile Socket socket;
        private volatile boolean closed;
        private volatile boolean emitDetachCallbacks;
        private volatile boolean holdDetachResponse;
        private volatile boolean holdResumeResponse;
        private volatile boolean completionSupported;
        private volatile boolean holdPing;
        private volatile boolean holdResume;
        private volatile boolean holdConnect;
        private volatile boolean holdToolReplies;
        private final java.util.concurrent.BlockingQueue<JsonNode> completions = new java.util.concurrent.LinkedBlockingQueue<>();
        private final java.util.concurrent.BlockingQueue<JsonNode> cancellations = new java.util.concurrent.LinkedBlockingQueue<>();
        private final java.util.concurrent.BlockingQueue<JsonNode> resumes = new java.util.concurrent.LinkedBlockingQueue<>();
        private final java.util.concurrent.BlockingQueue<String> sends = new java.util.concurrent.LinkedBlockingQueue<>();
        private volatile Exception failure;

        Server() throws IOException {
            this(new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")));
        }

        Server(ServerSocket listener) {
            this.listener = listener;
            thread = new Thread(this::serve, "force-stop-session-server");
            thread.setDaemon(true);
            thread.start();
        }

        CopilotClient createClient() {
            return createClient(null);
        }

        CopilotClient createClient(Executor executor) {
            return new CopilotClient(
                    new CopilotClientOptions().setCliUrl("127.0.0.1:" + listener.getLocalPort()).setExecutor(executor));
        }

        void emitIdle(String sessionId) throws IOException {
            emitEvent(sessionId, "session.idle", Map.of());
        }

        void completeConnect(JsonNode request) throws IOException {
            writeMessage(Map.of("jsonrpc", "2.0", "id", request.get("id"), "result",
                    Map.of("protocolVersion", SdkProtocolVersion.get())));
        }

        void completeDetach(JsonNode request, String outcome) throws IOException {
            var response = MAPPER.createObjectNode().put("jsonrpc", "2.0");
            response.set("id", request.get("id"));
            switch (outcome) {
                case "success" -> response.set("result", MAPPER.valueToTree(Map.of("success", true)));
                case "rejected" -> response.set("result",
                        MAPPER.valueToTree(Map.of("success", false, "error", "controlled detach rejection")));
                case "rpc-error" -> response.set("error",
                        MAPPER.valueToTree(Map.of("code", -32603, "message", "controlled detach RPC error")));
                default -> throw new IllegalArgumentException("Unexpected detach outcome: " + outcome);
            }
            writeMessage(response);
        }

        void invokeSessionEnd(String sessionId) throws Exception {
            var reply = sessionEndReply(sessionId, TimeUnit.SECONDS.toNanos(5));
            assertNotNull(reply, "The SDK must acknowledge the session-end hook");
            assertFalse(reply.has("error"), "Session-end hook failed: " + reply);
        }

        JsonNode sessionEndReply(String sessionId, long timeoutNanos) throws Exception {
            writeMessage(Map.of("jsonrpc", "2.0", "id", 9_000_001L, "method", "hooks.invoke", "params",
                    Map.of("sessionId", sessionId, "hookType", "sessionEnd", "input",
                            Map.of("sessionId", sessionId, "timestamp", 0, "cwd", ".", "reason", "user_exit"))));
            var reply = hookReplies.poll(Math.max(0, timeoutNanos), TimeUnit.NANOSECONDS);
            return reply;
        }

        JsonNode invokeToken(String registrationId, String sessionId) throws Exception {
            writeMessage(Map.of("jsonrpc", "2.0", "id", 9_000_001L, "method", "gitHubToken.getToken", "params",
                    Map.of("registrationId", registrationId, "sessionId", sessionId, "host", "github.com", "reason",
                            "refresh")));
            var reply = hookReplies.poll(5, TimeUnit.SECONDS);
            assertNotNull(reply, "The SDK must respond to the token request");
            return reply;
        }

        Map<String, Object> emitEvent(String sessionId, String type, Map<String, Object> data) throws IOException {
            assertNotNull(socket, "Session creation must establish the connection before events");
            var event = Map.<String, Object>of("type", type, "id", java.util.UUID.randomUUID().toString(), "timestamp",
                    "2026-10-05T00:00:00Z", "data", data);
            writeMessage(Map.of("jsonrpc", "2.0", "method", "session.event", "params",
                    Map.of("sessionId", sessionId, "event", event)));
            return event;
        }

        private void serve() {
            try (Socket accepted = listener.accept()) {
                socket = accepted;
                JsonNode pendingDetachId = null;
                while (!closed) {
                    JsonNode request = readMessage(accepted.getInputStream());
                    if (request == null) {
                        return;
                    }
                    if (accepted.isOutputShutdown()) {
                        continue;
                    }
                    JsonNode params = request.path("params");
                    if (!request.has("method") && request.path("id").asLong() == 9_000_001L) {
                        hookReplies.add(request);
                        continue;
                    }
                    if (request.path("method").asText().equals("session.sendAndWait")) {
                        if (completionSupported) {
                            completions.add(request);
                        } else {
                            writeMessage(Map.of("jsonrpc", "2.0", "id", request.get("id"), "error",
                                    Map.of("code", -32601, "message", "Method not found")));
                        }
                        continue;
                    }
                    if (request.path("method").asText().equals("$/cancelRequest")) {
                        cancellations.add(params.get("id"));
                        continue;
                    }
                    if (holdConnect && request.path("method").asText().equals("connect")) {
                        heldConnects.add(request);
                        continue;
                    }
                    if (holdResumeResponse && request.path("method").asText().equals("session.resume")) {
                        resumes.add(request);
                        continue;
                    }
                    if (holdResume && request.path("method").asText().equals("session.resume")) {
                        heldResumes.add(request);
                        continue;
                    }
                    if (holdPing && request.path("method").asText().equals("ping")) {
                        heldPings.add(request);
                        continue;
                    }
                    if (holdToolReplies
                            && request.path("method").asText().equals("session.tools.handlePendingToolCall")) {
                        toolReplies.add(request);
                        continue;
                    }
                    if (holdDetachResponse && request.path("method").asText().equals("session.detach")) {
                        detaches.incrementAndGet();
                        heldDetaches.add(request);
                        detachEntered.countDown();
                        continue;
                    }
                    if (!request.has("method") && request.path("id").asLong() == 9_000_000L) {
                        if (pendingDetachId == null || request.has("error")) {
                            throw new IOException("Unexpected detach hook response: " + request);
                        }
                        var response = MAPPER.createObjectNode().put("jsonrpc", "2.0");
                        response.set("id", pendingDetachId);
                        response.set("result", MAPPER.valueToTree(Map.of("success", true)));
                        pendingDetachId = null;
                        writeMessage(response);
                        continue;
                    }
                    if (emitDetachCallbacks && request.path("method").asText().equals("session.detach")) {
                        detaches.incrementAndGet();
                        pendingDetachId = request.get("id");
                        String sessionId = params.path("sessionId").asText();
                        emitIdle(sessionId);
                        var hookInput = Map.of("sessionId", sessionId, "timestamp", 0, "cwd", ".", "reason",
                                "user_exit");
                        writeMessage(Map.of("jsonrpc", "2.0", "id", 9_000_000L, "method", "hooks.invoke", "params",
                                Map.of("sessionId", sessionId, "hookType", "sessionEnd", "input", hookInput)));
                        continue;
                    }
                    Object result = switch (request.path("method").asText()) {
                        case "connect" -> Map.of("protocolVersion", SdkProtocolVersion.get());
                        case "session.create", "session.resume" -> {
                            creates.add(request);
                            yield Map.of("sessionId", params.path("sessionId").asText());
                        }
                        case "session.send" -> {
                            sends.add(params.path("prompt").asText());
                            yield Map.of("messageId", params.path("prompt").asText());
                        }
                        case "session.detach" -> {
                            detaches.incrementAndGet();
                            yield Map.of("success", true);
                        }
                        case "ping" -> Map.of("message", params.path("message").asText(), "protocolVersion",
                                SdkProtocolVersion.get());
                        default -> throw new IOException("Unexpected RPC: " + request.path("method"));
                    };
                    var response = MAPPER.createObjectNode().put("jsonrpc", "2.0");
                    response.set("id", request.get("id"));
                    response.set("result", MAPPER.valueToTree(result));
                    writeMessage(response);
                }
            } catch (Exception error) {
                if (!closed) {
                    failure = error;
                }
            } finally {
                peerClosed.countDown();
            }
        }

        private static JsonNode readMessage(InputStream input) throws IOException {
            var header = new StringBuilder();
            while (!header.toString().endsWith("\r\n\r\n")) {
                int value = input.read();
                if (value < 0) {
                    if (header.isEmpty()) {
                        return null;
                    }
                    throw new IOException("Truncated RPC header");
                }
                header.append((char) value);
                if (header.length() > 8192) {
                    throw new IOException("Oversized RPC header");
                }
            }
            int length;
            try {
                length = Integer.parseInt(header.substring(header.indexOf(":") + 1).trim());
            } catch (NumberFormatException error) {
                throw new IOException("Invalid Content-Length", error);
            }
            if (length < 0 || length > 1_048_576) {
                throw new IOException("Invalid Content-Length: " + length);
            }
            byte[] body = input.readNBytes(length);
            if (body.length != length) {
                throw new IOException("Truncated RPC body");
            }
            return MAPPER.readTree(body);
        }

        private synchronized void writeMessage(Object message) throws IOException {
            byte[] body = MAPPER.writeValueAsBytes(message);
            var output = socket.getOutputStream();
            output.write(("Content-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            output.write(body);
            output.flush();
        }

        @Override
        public void close() throws Exception {
            closed = true;
            listener.close();
            if (socket != null) {
                socket.close();
            }
            thread.join(5000);
            assertFalse(thread.isAlive(), "The RPC fixture must stop");
            assertNull(failure, "RPC fixture failure: " + failure);
        }
    }
}
