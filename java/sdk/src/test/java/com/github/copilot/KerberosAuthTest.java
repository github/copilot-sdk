/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.copilot.generated.rpc.KerberosAuthAcquireRequest;
import com.github.copilot.generated.rpc.KerberosAuthMode;
import com.github.copilot.generated.rpc.KerberosAuthScope;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.KerberosAuthConfig;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

class KerberosAuthTest {

    private static final int TIMEOUT_SECONDS = 15;

    @ParameterizedTest
    @EnumSource(KerberosAuthMode.class)
    void registersModeAndRoutesConcurrentAcquireRequests(KerberosAuthMode mode) throws Exception {
        var spns = List.of("HTTP/proxy.example", "ldap/directory.example@EXAMPLE.COM");
        var calls = new ConcurrentLinkedQueue<String>();
        var allReceived = new CompletableFuture<Void>();
        var config = new KerberosAuthConfig(mode, request -> {
            calls.add(request.spn());
            if (calls.size() == spns.size()) {
                allReceived.complete(null);
            }
            // Complete only once every lookup is pending, proving they are not serialized.
            return allReceived.thenApply(ignored -> encode(request.spn()));
        });

        try (var server = new FakeRuntimeServer(null, false); var client = server.createClient(config)) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var responses = spns.stream().map(server::acquire).toList();
            var tokens = new ArrayList<String>();
            for (var response : responses) {
                tokens.add(response.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).get("token").asText());
            }

            assertEquals(List.of("{\"mode\":\"" + mode.getValue() + "\"}"), server.registrations());
            assertEquals(Set.copyOf(spns), Set.copyOf(calls));
            assertEquals(spns.stream().map(KerberosAuthTest::encode).toList(), tokens);
        }
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(value = KerberosAuthScope.class, names = "RUNTIME")
    void registersRuntimeScopeAndOmitsUnsetScope(KerberosAuthScope scope) throws Exception {
        Function<KerberosAuthAcquireRequest, CompletableFuture<String>> callback = request -> CompletableFuture
                .completedFuture(null);
        var config = scope == null
                ? new KerberosAuthConfig(KerberosAuthMode.FALLBACK, callback)
                : new KerberosAuthConfig(KerberosAuthMode.FALLBACK, scope, callback);

        try (var server = new FakeRuntimeServer(null, false); var client = server.createClient(config)) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            var expected = scope == null ? "{\"mode\":\"fallback\"}" : "{\"mode\":\"fallback\",\"scope\":\"runtime\"}";
            assertEquals(List.of(expected), server.registrations());
        }
    }

    @Test
    void doesNotRegisterOrHandleAcquireWithoutOption() throws Exception {
        try (var server = new FakeRuntimeServer(null, false); var client = server.createClient(null)) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var error = assertRpcError(server.acquire("HTTP/proxy"));

            assertEquals(List.of(), server.registrations());
            assertEquals(-32601, error.getCode());
        }
    }

    @Test
    void returnsNoTokenWhenUnavailableAndPropagatesCallbackFailures() throws Exception {
        var outcome = new AtomicReference<>("unavailable");
        var config = new KerberosAuthConfig(KerberosAuthMode.ALWAYS, request -> switch (outcome.get()) {
            case "failed" -> CompletableFuture.failedFuture(new IllegalStateException("lookup failed"));
            case "thrown" -> throw new IllegalStateException("lookup threw");
            default -> CompletableFuture.completedFuture(null);
        });

        try (var server = new FakeRuntimeServer(null, false); var client = server.createClient(config)) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var unavailable = server.acquire("HTTP/proxy").get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            outcome.set("failed");
            var failed = assertRpcError(server.acquire("HTTP/proxy"));
            outcome.set("thrown");
            var thrown = assertRpcError(server.acquire("HTTP/proxy"));

            assertEquals("{}", unavailable.toString());
            assertEquals(List.of(-32603, "lookup failed", -32603, "lookup threw"),
                    List.of(failed.getCode(), failed.getMessage(), thrown.getCode(), thrown.getMessage()));
        }
    }

    @Test
    void registrationFailureFailsStartup() throws Exception {
        var config = new KerberosAuthConfig(KerberosAuthMode.FALLBACK,
                request -> CompletableFuture.completedFuture(null));

        try (var server = new FakeRuntimeServer("provider already registered", false);
                var client = server.createClient(config)) {
            var error = assertThrows(ExecutionException.class,
                    () -> client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS));

            var cause = assertInstanceOf(JsonRpcException.class, unwrap(error));
            assertEquals("provider already registered", cause.getMessage());
        }
    }

    @Test
    void registersBeforeSessionsRequestedDuringStartup() throws Exception {
        var config = new KerberosAuthConfig(KerberosAuthMode.ALWAYS,
                request -> CompletableFuture.completedFuture(null));

        try (var server = new FakeRuntimeServer(null, true); var client = server.createClient(config)) {
            var starting = client.start();
            server.connectReceived().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL));
            starting.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            session.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertEquals(List.of("kerberosAuth.setProvider", "session.create"), server.order());
        }
    }

    @Test
    void clonePreservesKerberosAuth() {
        var config = new KerberosAuthConfig(KerberosAuthMode.FALLBACK,
                request -> CompletableFuture.completedFuture(null));
        var clone = new CopilotClientOptions().setKerberosAuth(config).clone();

        assertSame(config, clone.getKerberosAuth());
    }

    @Test
    void executorRejectionFailsAcquisitionWithoutRunningOnReader() throws Exception {
        var reject = new AtomicBoolean();
        var calls = new AtomicInteger();
        Executor executor = task -> {
            if (reject.get()) {
                throw new RejectedExecutionException("saturated");
            }
            ForkJoinPool.commonPool().execute(task);
        };
        var config = new KerberosAuthConfig(KerberosAuthMode.ALWAYS, request -> {
            calls.incrementAndGet();
            return CompletableFuture.completedFuture(encode(request.spn()));
        });

        try (var server = new FakeRuntimeServer(null, false); var client = server.createClient(config, executor)) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            reject.set(true);
            var error = assertRpcError(server.acquire("HTTP/proxy"));
            int callsAfterRejection = calls.get();
            reject.set(false);
            var token = server.acquire("HTTP/proxy").get(TIMEOUT_SECONDS, TimeUnit.SECONDS).get("token").asText();

            assertEquals(List.of(-32603, 0, encode("HTTP/proxy")),
                    List.of(error.getCode(), callsAfterRejection, token));
        }
    }

    @Test
    void cancelRequestSkipsQueuedAcquisitionAndCancelsPendingCallback() throws Exception {
        var worker = Executors.newSingleThreadExecutor();
        var gate = new CompletableFuture<Void>();
        var pendingToken = new CompletableFuture<String>();
        var pendingInvoked = new CompletableFuture<Void>();
        var calls = new ConcurrentLinkedQueue<String>();
        var config = new KerberosAuthConfig(KerberosAuthMode.ALWAYS, request -> {
            calls.add(request.spn());
            pendingInvoked.complete(null);
            return pendingToken;
        });

        try (var server = new FakeRuntimeServer(null, false); var client = server.createClient(config, worker)) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var pending = server.acquire("HTTP/pending");
            pendingInvoked.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            worker.execute(gate::join);
            // The reader handles messages in order, so this acquisition is queued
            // behind the gate before the cancellations arrive.
            var queued = server.acquire("HTTP/queued");
            server.cancel(1L);
            server.cancel(2L);
            var pendingError = assertRpcError(pending);
            var queuedError = assertRpcError(queued);
            assertThrows(CancellationException.class, () -> pendingToken.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            gate.complete(null);
            // FIFO: once this runs, the queued acquisition task has already run.
            worker.submit(() -> {
            }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertEquals(List.of(-32800, -32800, List.of("HTTP/pending")),
                    List.of(pendingError.getCode(), queuedError.getCode(), List.copyOf(calls)));
        } finally {
            gate.complete(null);
            worker.shutdownNow();
        }
    }

    @Test
    void forceStopSkipsQueuedAcquisitionAndCancelsPendingCallback() throws Exception {
        var worker = Executors.newSingleThreadExecutor();
        var gate = new CompletableFuture<Void>();
        var pendingToken = new CompletableFuture<String>();
        var pendingInvoked = new CompletableFuture<Void>();
        var calls = new ConcurrentLinkedQueue<String>();
        var config = new KerberosAuthConfig(KerberosAuthMode.ALWAYS, request -> {
            calls.add(request.spn());
            pendingInvoked.complete(null);
            return pendingToken;
        });

        try (var server = new FakeRuntimeServer(null, false); var client = server.createClient(config, worker)) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var pending = server.acquire("HTTP/pending");
            pendingInvoked.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            worker.execute(gate::join);
            var queued = server.acquire("HTTP/queued");
            // The client answers the unknown method on its reader after queuing the
            // acquisition.
            assertEquals(-32601, assertRpcError(server.barrier()).getCode());

            client.forceStop().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThrows(CancellationException.class, () -> pendingToken.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            gate.complete(null);
            // FIFO: once this runs, the queued acquisition task has already run.
            worker.submit(() -> {
            }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertThrows(ExecutionException.class, () -> pending.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertThrows(ExecutionException.class, () -> queued.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertEquals(List.of("HTTP/pending"), List.copyOf(calls));
        } finally {
            gate.complete(null);
            worker.shutdownNow();
        }
    }

    @Test
    void fineDiagnosticsRedactTokenWithoutChangingWireResponse() throws Exception {
        var secret = encode("HTTP/proxy-secret");
        var config = new KerberosAuthConfig(KerberosAuthMode.ALWAYS,
                request -> CompletableFuture.completedFuture(secret));
        Logger logger = Logger.getLogger(JsonRpcClient.class.getName());
        Level previousLevel = logger.getLevel();
        var sent = new CopyOnWriteArrayList<String>();
        var tokenResponse = new CompletableFuture<JsonNode>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                String message = record.getMessage();
                if (message == null || !message.startsWith("Sent: ")) {
                    return;
                }
                sent.add(message);
                try {
                    JsonNode node = JsonRpcClient.getObjectMapper().readTree(message.substring("Sent: ".length()));
                    if (node.path("result").has("token")) {
                        tokenResponse.complete(node);
                    }
                } catch (IOException e) {
                    tokenResponse.completeExceptionally(e);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
        logger.setLevel(Level.FINE);
        try (var server = new FakeRuntimeServer(null, false); var client = server.createClient(config)) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var wireToken = server.acquire("HTTP/proxy").get(TIMEOUT_SECONDS, TimeUnit.SECONDS).get("token").asText();
            var logged = tokenResponse.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).at("/result/token").asText();

            assertEquals(List.of(secret, "<redacted>", false),
                    List.of(wireToken, logged, sent.stream().anyMatch(message -> message.contains(secret))));
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(previousLevel);
        }
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static JsonRpcException assertRpcError(CompletableFuture<?> future) {
        var error = assertThrows(ExecutionException.class, () -> future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        return assertInstanceOf(JsonRpcException.class, unwrap(error));
    }

    private static Throwable unwrap(Throwable error) {
        while ((error instanceof ExecutionException || error instanceof CompletionException)
                && error.getCause() != null) {
            error = error.getCause();
        }
        return error;
    }

    private static final class FakeRuntimeServer implements AutoCloseable {

        private static final Map<String, Object> CONNECT_RESULT = Map.of("ok", true, "protocolVersion",
                SdkProtocolVersion.get(), "version", "kerberos-auth-test");

        private final String setProviderError;
        private final boolean holdConnect;
        private final ServerSocket serverSocket;
        private final Thread acceptThread;
        private final CompletableFuture<JsonRpcClient> ready = new CompletableFuture<>();
        private final CompletableFuture<Void> connectReceived = new CompletableFuture<>();
        private final CompletableFuture<Void> sessionCreated = new CompletableFuture<>();
        private final List<String> registrations = new CopyOnWriteArrayList<>();
        private final List<String> order = new CopyOnWriteArrayList<>();

        /**
         * @param setProviderError
         *            error message for {@code kerberosAuth.setProvider}, or
         *            {@code null} to accept registration
         * @param holdConnect
         *            hold the {@code connect} response until a session is created or a
         *            short timeout elapses
         */
        FakeRuntimeServer(String setProviderError, boolean holdConnect) throws IOException {
            this.setProviderError = setProviderError;
            this.holdConnect = holdConnect;
            serverSocket = new ServerSocket(0);
            acceptThread = new Thread(this::acceptLoop, "kerberos-auth-runtime");
            acceptThread.setDaemon(true);
            acceptThread.start();
        }

        CopilotClient createClient(KerberosAuthConfig kerberosAuth) {
            return createClient(kerberosAuth, null);
        }

        CopilotClient createClient(KerberosAuthConfig kerberosAuth, Executor executor) {
            var options = new CopilotClientOptions().setCliUrl("127.0.0.1:" + serverSocket.getLocalPort());
            if (kerberosAuth != null) {
                options.setKerberosAuth(kerberosAuth);
            }
            if (executor != null) {
                options.setExecutor(executor);
            }
            return new CopilotClient(options);
        }

        CompletableFuture<JsonNode> acquire(String spn) {
            return ready.join().invoke("kerberosAuth.acquire", Map.of("spn", spn), JsonNode.class);
        }

        void cancel(long id) throws IOException {
            ready.join().notify("$/cancelRequest", Map.of("id", id));
        }

        /** Round-trips a request the client answers on its reader thread. */
        CompletableFuture<JsonNode> barrier() {
            return ready.join().invoke("kerberosAuthTest.barrier", Map.of(), JsonNode.class);
        }

        CompletableFuture<Void> connectReceived() {
            return connectReceived;
        }

        List<String> registrations() {
            return List.copyOf(registrations);
        }

        List<String> order() {
            return List.copyOf(order);
        }

        private void acceptLoop() {
            try {
                Socket socket = serverSocket.accept();
                ready.complete(JsonRpcClient.fromSocket(socket, rpc -> {
                    rpc.registerMethodHandler("connect", (id, params) -> {
                        connectReceived.complete(null);
                        // A session request that does not wait for startup arrives while connect is
                        // held and releases it; one that waits cannot, so the timeout releases it.
                        var release = holdConnect
                                ? sessionCreated.copy().completeOnTimeout(null, 500, TimeUnit.MILLISECONDS)
                                : CompletableFuture.completedFuture(null);
                        release.thenRun(() -> respond(rpc, id, CONNECT_RESULT));
                    });
                    rpc.registerMethodHandler("kerberosAuth.setProvider", (id, params) -> {
                        order.add("kerberosAuth.setProvider");
                        registrations.add(params.toString());
                        if (setProviderError != null) {
                            sendError(rpc, id, setProviderError);
                        } else {
                            respond(rpc, id, null);
                        }
                    });
                    rpc.registerMethodHandler("session.create", (id, params) -> {
                        order.add("session.create");
                        sessionCreated.complete(null);
                        respond(rpc, id, Map.of("sessionId", params.get("sessionId").asText()));
                    });
                }));
            } catch (IOException e) {
                ready.completeExceptionally(e);
            }
        }

        private static void respond(JsonRpcClient rpc, String id, Object result) {
            try {
                rpc.sendResponse(parseRpcId(id), result);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to send fake runtime response", e);
            }
        }

        private static void sendError(JsonRpcClient rpc, String id, String message) {
            try {
                rpc.sendErrorResponse(parseRpcId(id), -32603, message);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to send fake runtime error", e);
            }
        }

        private static Object parseRpcId(String id) {
            try {
                return Long.valueOf(id);
            } catch (NumberFormatException ignored) {
                return id;
            }
        }

        @Override
        public void close() throws Exception {
            JsonRpcClient server = ready.getNow(null);
            if (server != null) {
                server.close();
            }
            serverSocket.close();
            acceptThread.join(TimeUnit.SECONDS.toMillis(5));
        }
    }
}
