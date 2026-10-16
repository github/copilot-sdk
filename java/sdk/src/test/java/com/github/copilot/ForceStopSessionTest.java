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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.SessionEvent;
import com.github.copilot.generated.SessionIdleEvent;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SessionHooks;

class ForceStopSessionTest {

    @CopilotResponse
    public record Answer(int value) {
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
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    void retirementPreservesTerminalOutcomeWhileSchedulerHandoffIsBlocked(boolean terminalError, boolean force)
            throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var server = new Server(); var client = server.createClient()) {
            var session = createSession(client);
            var occupying = session.sendAndWait(new MessageOptions().setPrompt("occupy scheduler"), 0);
            var continuation = occupying.whenComplete((value, error) -> {
                entered.countDown();
                awaitRelease(release);
            });
            try (var gate = (AutoCloseable) release::countDown) {
                client.ping("first send acknowledgment").get(5, TimeUnit.SECONDS);
                server.emitIdle(session.getSessionId());
                assertTrue(entered.await(5, TimeUnit.SECONDS), "A continuation must occupy the session scheduler");

                var completed = session.sendAndWait(new MessageOptions().setPrompt("completed terminal"), 0);
                client.ping("second send acknowledgment").get(5, TimeUnit.SECONDS);
                if (terminalError) {
                    server.emitEvent(session.getSessionId(), "session.error",
                            Map.of("errorType", "query", "message", "original terminal failure"));
                } else {
                    server.emitEvent(session.getSessionId(), "assistant.message",
                            Map.of("messageId", "terminal-message", "content", "completed before retirement"));
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

    private static CopilotSession createSession(CopilotClient client) throws Exception {
        return client.createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get(5,
                TimeUnit.SECONDS);
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

    private static final class Server implements AutoCloseable {

        private static final ObjectMapper MAPPER = new ObjectMapper();
        private final ServerSocket listener;
        private final Thread thread;
        private final AtomicInteger detaches = new AtomicInteger();
        private final CountDownLatch detachEntered = new CountDownLatch(1);
        private volatile Socket socket;
        private volatile boolean closed;
        private volatile boolean emitDetachCallbacks;
        private volatile boolean holdDetachResponse;
        private volatile Exception failure;

        Server() throws IOException {
            listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
            thread = new Thread(this::serve, "force-stop-session-server");
            thread.setDaemon(true);
            thread.start();
        }

        CopilotClient createClient() {
            return new CopilotClient(new CopilotClientOptions().setCliUrl("127.0.0.1:" + listener.getLocalPort()));
        }

        void emitIdle(String sessionId) throws IOException {
            emitEvent(sessionId, "session.idle", Map.of());
        }

        void emitEvent(String sessionId, String type, Map<String, Object> data) throws IOException {
            assertNotNull(socket, "Session creation must establish the connection before events");
            writeMessage(Map.of("jsonrpc", "2.0", "method", "session.event", "params",
                    Map.of("sessionId", sessionId, "event",
                            Map.of("type", type, "id", "00000000-0000-0000-0000-000000000001", "timestamp",
                                    "2026-10-05T00:00:00Z", "data", data))));
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
                    JsonNode params = request.path("params");
                    if (holdDetachResponse && request.path("method").asText().equals("session.detach")) {
                        detaches.incrementAndGet();
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
                        case "session.create" -> Map.of("sessionId", params.path("sessionId").asText());
                        case "session.send" -> Map.of("messageId", params.path("prompt").asText());
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
