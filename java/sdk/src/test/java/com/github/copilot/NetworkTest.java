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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.copilot.generated.rpc.NetworkScope;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.NetworkConfig;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

class NetworkTest {

    private static final int TIMEOUT_SECONDS = 15;
    private static final int METHOD_NOT_FOUND = -32601;
    private static final String PROXY = "PROXY proxy.example:8080; HTTPS proxy.example:443; DIRECT";

    /** The network callbacks, with the request parameter each one receives. */
    private enum Callback {
        RESOLVE_PROXY("network.resolveProxy", "url", "Proxy resolution",
                "https://api.example.com/"), AUTHENTICATE_NEGOTIATE("network.authenticateNegotiate", "spn",
                        "Negotiate authentication", "HTTP/proxy.example");

        final String method;
        final String parameter;
        final String label;
        final String sample;

        Callback(String method, String parameter, String label, String sample) {
            this.method = method;
            this.parameter = parameter;
            this.label = label;
            this.sample = sample;
        }

        /**
         * Sets this callback on the configuration, passing it the request parameter.
         */
        NetworkConfig configure(NetworkConfig config, Function<String, CompletableFuture<String>> callback) {
            return switch (this) {
                case RESOLVE_PROXY -> config.setResolveProxy(request -> callback.apply(request.url()));
                case AUTHENTICATE_NEGOTIATE ->
                    config.setAuthenticateNegotiate(request -> callback.apply(request.spn()));
            };
        }
    }

    @Test
    void registersBothCallbacksAndAnswersConcurrentLookups() throws Exception {
        var calls = new ConcurrentLinkedQueue<String>();
        var allReceived = new CompletableFuture<Void>();
        Function<String, CompletableFuture<String>> record = value -> {
            calls.add(value);
            if (calls.size() == 3) {
                allReceived.complete(null);
            }
            // Complete only once every lookup is pending, proving they are not serialized.
            return allReceived.thenApply(ignored -> value);
        };
        var config = new NetworkConfig().setResolveProxy(
                request -> record.apply(request.url()).thenApply(url -> url.startsWith("https:") ? PROXY : null))
                .setAuthenticateNegotiate(request -> record.apply(request.spn()).thenApply(NetworkTest::encode));

        try (var server = new FakeRuntimeServer(null, false); var client = server.createClient(config)) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var responses = List.of(server.request(Callback.RESOLVE_PROXY, "https://api.example.com/"),
                    server.request(Callback.RESOLVE_PROXY, "http://example.com/path"),
                    server.request(Callback.AUTHENTICATE_NEGOTIATE, "HTTP/proxy.example"));
            var results = new ArrayList<String>();
            for (var response : responses) {
                results.add(response.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).toString());
            }

            assertEquals(
                    List.of(List.of("{\"resolveProxy\":{},\"authenticateNegotiate\":{}}"),
                            Set.of("https://api.example.com/", "http://example.com/path", "HTTP/proxy.example"),
                            List.of("{\"proxy\":\"" + PROXY + "\"}", "{}",
                                    "{\"token\":\"" + encode("HTTP/proxy.example") + "\"}")),
                    List.of(server.registrations(), Set.copyOf(calls), results));
        }
    }

    static Stream<Arguments> configurations() {
        Function<String, CompletableFuture<String>> none = value -> CompletableFuture.completedFuture(null);
        return Stream.of(Arguments.of("unset", null, List.of(), List.of(METHOD_NOT_FOUND, METHOD_NOT_FOUND)),
                Arguments.of("empty", new NetworkConfig(), List.of(), List.of(METHOD_NOT_FOUND, METHOD_NOT_FOUND)),
                Arguments.of("scope only", new NetworkConfig().setScope(NetworkScope.RUNTIME), List.of(),
                        List.of(METHOD_NOT_FOUND, METHOD_NOT_FOUND)),
                Arguments.of("resolveProxy", Callback.RESOLVE_PROXY.configure(new NetworkConfig(), none),
                        List.of("{\"resolveProxy\":{}}"), List.of("{}", METHOD_NOT_FOUND)),
                Arguments.of("runtime authenticateNegotiate",
                        Callback.AUTHENTICATE_NEGOTIATE.configure(new NetworkConfig().setScope(NetworkScope.RUNTIME),
                                none),
                        List.of("{\"scope\":\"runtime\",\"authenticateNegotiate\":{}}"),
                        List.of(METHOD_NOT_FOUND, "{}")),
                Arguments.of("connection resolveProxy",
                        Callback.RESOLVE_PROXY.configure(new NetworkConfig().setScope(NetworkScope.CONNECTION), none),
                        List.of("{\"scope\":\"connection\",\"resolveProxy\":{}}"), List.of("{}", METHOD_NOT_FOUND)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("configurations")
    void registersAndHandlesOnlyConfiguredCallbacksAndScope(String name, NetworkConfig config,
            List<String> registrations, List<Object> outcomes) throws Exception {
        try (var server = new FakeRuntimeServer(null, false); var client = server.createClient(config)) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var actual = new ArrayList<Object>();
            for (var callback : Callback.values()) {
                var response = server.request(callback, callback.sample);
                try {
                    actual.add(response.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).toString());
                } catch (ExecutionException error) {
                    actual.add(assertInstanceOf(JsonRpcException.class, unwrap(error)).getCode());
                }
            }

            assertEquals(List.of(registrations, outcomes), List.of(server.registrations(), actual));
        }
    }

    @Test
    void registrationFailureFailsStartup() throws Exception {
        var config = new NetworkConfig().setResolveProxy(request -> CompletableFuture.completedFuture(null));

        try (var server = new FakeRuntimeServer("provider already registered", false);
                var client = server.createClient(config)) {
            var error = assertThrows(ExecutionException.class,
                    () -> client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS));

            var cause = assertInstanceOf(JsonRpcException.class, unwrap(error));
            assertEquals("provider already registered", cause.getMessage());
        }
    }

    @Test
    void forceStopEndsAStartupWhoseRegistrationIsNeverAnswered() throws Exception {
        var config = new NetworkConfig().setResolveProxy(request -> CompletableFuture.completedFuture(null));

        try (var server = new FakeRuntimeServer(FakeRuntimeServer.UNANSWERED, false);
                var client = server.createClient(config)) {
            var starting = client.start();
            server.setProviderReceived().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            client.forceStop().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertThrows(ExecutionException.class, () -> starting.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }
    }

    @Test
    void forceStopEndsAStartupRunningOnTheCallingThread() throws Exception {
        var config = new NetworkConfig().setResolveProxy(request -> CompletableFuture.completedFuture(null));
        // A direct executor runs the startup inside start(), which returns only once it
        // ends.
        Executor direct = Runnable::run;

        try (var server = new FakeRuntimeServer(FakeRuntimeServer.UNANSWERED, false);
                var client = server.createClient(options -> options.setNetwork(config).setExecutor(direct))) {
            var starting = CompletableFuture.supplyAsync(client::start).thenCompose(Function.identity());
            server.setProviderReceived().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            client.forceStop().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertThrows(ExecutionException.class, () -> starting.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }
    }

    @Test
    void startAfterForceStopRegistersAgain() throws Exception {
        var config = new NetworkConfig().setResolveProxy(request -> CompletableFuture.completedFuture(null));
        // Unlike the client's own, a caller's executor remains usable after
        // forceStop().
        var worker = Executors.newSingleThreadExecutor();

        try (var server = new FakeRuntimeServer(null, false);
                var client = server.createClient(options -> options.setNetwork(config).setExecutor(worker))) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            client.forceStop().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertEquals(List.of("network.setProvider", "network.setProvider"), server.order());
        } finally {
            worker.shutdownNow();
        }
    }

    @Test
    void registersBeforeSessionsRequestedDuringStartup() throws Exception {
        var config = new NetworkConfig().setAuthenticateNegotiate(request -> CompletableFuture.completedFuture(null));

        try (var server = new FakeRuntimeServer(null, true); var client = server.createClient(config)) {
            var starting = client.start();
            server.connectReceived().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL));
            starting.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            session.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertEquals(List.of("network.setProvider", "session.create"), server.order());
        }
    }

    @Test
    void clonePreservesNetwork() {
        var config = new NetworkConfig().setResolveProxy(request -> CompletableFuture.completedFuture(null));
        var clone = new CopilotClientOptions().setNetwork(config).clone();

        assertSame(config, clone.getNetwork());
    }

    @ParameterizedTest
    @EnumSource(Callback.class)
    void answersMissingValuesAndPropagatesCallbackFailures(Callback callback) throws Exception {
        var outcome = new AtomicReference<>("unavailable");
        var config = callback.configure(new NetworkConfig(), value -> switch (outcome.get()) {
            case "failed" -> CompletableFuture.failedFuture(new IllegalStateException("lookup failed"));
            case "thrown" -> throw new IllegalStateException("lookup threw");
            case "error" -> throw new UnsatisfiedLinkError("native lookup unavailable");
            case "null future" -> null;
            default -> CompletableFuture.completedFuture(null);
        });

        try (var server = new FakeRuntimeServer(null, false); var client = server.createClient(config)) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var unavailable = server.request(callback, callback.sample).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var errors = new ArrayList<Object>();
            for (var failure : List.of("failed", "thrown", "error", "null future")) {
                outcome.set(failure);
                var error = assertRpcError(server.request(callback, callback.sample));
                errors.add(List.of(error.getCode(), error.getMessage()));
            }

            assertEquals(
                    List.of("{}",
                            List.of(List.of(-32603, "lookup failed"), List.of(-32603, "lookup threw"),
                                    List.of(-32603, "native lookup unavailable"),
                                    List.of(-32603, callback.label + " callback returned a null future"))),
                    List.of(unavailable.toString(), errors));
        }
    }

    @ParameterizedTest
    @EnumSource(Callback.class)
    void executorRejectionFailsLookupWithoutRunningOnReader(Callback callback) throws Exception {
        var reject = new AtomicBoolean();
        var calls = new AtomicInteger();
        Executor executor = task -> {
            if (reject.get()) {
                throw new RejectedExecutionException("saturated");
            }
            ForkJoinPool.commonPool().execute(task);
        };
        var config = callback.configure(new NetworkConfig(), value -> {
            calls.incrementAndGet();
            return CompletableFuture.completedFuture("answer");
        });

        try (var server = new FakeRuntimeServer(null, false);
                var client = server.createClient(options -> options.setNetwork(config).setExecutor(executor))) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            reject.set(true);
            var error = assertRpcError(server.request(callback, callback.sample));
            int callsAfterRejection = calls.get();
            reject.set(false);
            var answer = server.request(callback, callback.sample).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertEquals(
                    List.of(-32603, callback.label + " callback could not be scheduled: saturated", 0,
                            "{\"" + (callback == Callback.RESOLVE_PROXY ? "proxy" : "token") + "\":\"answer\"}"),
                    List.of(error.getCode(), error.getMessage(), callsAfterRejection, answer.toString()));
        }
    }

    @ParameterizedTest
    @EnumSource(Callback.class)
    void cancelRequestSkipsQueuedLookupAndCancelsPendingCallback(Callback callback) throws Exception {
        var worker = Executors.newSingleThreadExecutor();
        var gate = new CompletableFuture<Void>();
        var pendingAnswer = new CompletableFuture<String>();
        var pendingInvoked = new CompletableFuture<Void>();
        var calls = new ConcurrentLinkedQueue<String>();
        var config = callback.configure(new NetworkConfig(), value -> {
            calls.add(value);
            pendingInvoked.complete(null);
            return pendingAnswer;
        });

        try (var server = new FakeRuntimeServer(null, false);
                var client = server.createClient(options -> options.setNetwork(config).setExecutor(worker))) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var pending = server.request(callback, "pending");
            pendingInvoked.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            worker.execute(gate::join);
            // The reader handles messages in order, so this lookup is queued behind the
            // gate before the cancellations arrive.
            var queued = server.request(callback, "queued");
            server.cancel(1L);
            server.cancel(2L);
            var pendingError = assertRpcError(pending);
            var queuedError = assertRpcError(queued);
            assertThrows(CancellationException.class, () -> pendingAnswer.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            gate.complete(null);
            // FIFO: once this runs, the queued lookup task has already run.
            worker.submit(() -> {
            }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            var cancelled = List.of(-32800, callback.label + " request cancelled");
            assertEquals(List.of(cancelled, cancelled, List.of("pending")),
                    List.of(List.of(pendingError.getCode(), pendingError.getMessage()),
                            List.of(queuedError.getCode(), queuedError.getMessage()), List.copyOf(calls)));
        } finally {
            gate.complete(null);
            worker.shutdownNow();
        }
    }

    @Test
    void cancelRequestRetiresOnlyTheNamedLookup() throws Exception {
        var proxy = new CompletableFuture<String>();
        var token = new CompletableFuture<String>();
        var invoked = new CountDown(2);
        var config = new NetworkConfig().setResolveProxy(request -> invoked.then(proxy))
                .setAuthenticateNegotiate(request -> invoked.then(token));

        try (var server = new FakeRuntimeServer(null, false); var client = server.createClient(config)) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var proxyResponse = server.request(Callback.RESOLVE_PROXY, Callback.RESOLVE_PROXY.sample);
            var tokenResponse = server.request(Callback.AUTHENTICATE_NEGOTIATE, Callback.AUTHENTICATE_NEGOTIATE.sample);
            invoked.done().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            server.cancel(2L);
            var tokenError = assertRpcError(tokenResponse);
            proxy.complete(PROXY);
            // The answer can arrive before the callback's future is attached and cancelled.
            assertThrows(CancellationException.class, () -> token.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));

            assertEquals(List.of(-32800, "Negotiate authentication request cancelled", "{\"proxy\":\"" + PROXY + "\"}"),
                    List.of(tokenError.getCode(), tokenError.getMessage(),
                            proxyResponse.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).toString()));
        }
    }

    @ParameterizedTest
    @EnumSource(Callback.class)
    void forceStopSkipsQueuedLookupAndCancelsPendingCallback(Callback callback) throws Exception {
        var worker = Executors.newSingleThreadExecutor();
        var gate = new CompletableFuture<Void>();
        var pendingAnswer = new CompletableFuture<String>();
        var pendingInvoked = new CompletableFuture<Void>();
        var calls = new ConcurrentLinkedQueue<String>();
        var config = callback.configure(new NetworkConfig(), value -> {
            calls.add(value);
            pendingInvoked.complete(null);
            return pendingAnswer;
        });

        try (var server = new FakeRuntimeServer(null, false);
                var client = server.createClient(options -> options.setNetwork(config).setExecutor(worker))) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var pending = server.request(callback, "pending");
            pendingInvoked.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            worker.execute(gate::join);
            var queued = server.request(callback, "queued");
            // The client answers the unknown method on its reader after queuing the lookup.
            assertEquals(METHOD_NOT_FOUND, assertRpcError(server.barrier()).getCode());

            client.forceStop().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThrows(CancellationException.class, () -> pendingAnswer.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            gate.complete(null);
            // FIFO: once this runs, the queued lookup task has already run.
            worker.submit(() -> {
            }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertThrows(ExecutionException.class, () -> pending.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertThrows(ExecutionException.class, () -> queued.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertEquals(List.of("pending"), List.copyOf(calls));
        } finally {
            gate.complete(null);
            worker.shutdownNow();
        }
    }

    @Test
    void fineDiagnosticsRedactTokenWithoutChangingWireResponse() throws Exception {
        var secret = encode("HTTP/proxy-secret");
        var config = new NetworkConfig().setAuthenticateNegotiate(request -> CompletableFuture.completedFuture(secret));
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
            var wireToken = server.request(Callback.AUTHENTICATE_NEGOTIATE, "HTTP/proxy")
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS).get("token").asText();
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

    /** Completes once a fixed number of callbacks have been invoked. */
    private static final class CountDown {

        private final AtomicInteger remaining;
        private final CompletableFuture<Void> done = new CompletableFuture<>();

        CountDown(int count) {
            remaining = new AtomicInteger(count);
        }

        <T> CompletableFuture<T> then(CompletableFuture<T> result) {
            if (remaining.decrementAndGet() == 0) {
                done.complete(null);
            }
            return result;
        }

        CompletableFuture<Void> done() {
            return done;
        }
    }

    private static final class FakeRuntimeServer implements AutoCloseable {

        private static final Map<String, Object> CONNECT_RESULT = Map.of("ok", true, "protocolVersion",
                SdkProtocolVersion.get(), "version", "network-test");
        /** Leaves {@code network.setProvider} unanswered. */
        static final String UNANSWERED = "<unanswered>";

        private final String setProviderError;
        private final boolean holdConnect;
        private final ServerSocket serverSocket;
        private final Thread acceptThread;
        private final CompletableFuture<JsonRpcClient> ready = new CompletableFuture<>();
        private final CompletableFuture<Void> connectReceived = new CompletableFuture<>();
        private final CompletableFuture<Void> setProviderReceived = new CompletableFuture<>();
        private final CompletableFuture<Void> sessionCreated = new CompletableFuture<>();
        private final List<String> registrations = new CopyOnWriteArrayList<>();
        private final List<String> order = new CopyOnWriteArrayList<>();
        private final List<JsonRpcClient> connections = new CopyOnWriteArrayList<>();

        /**
         * @param setProviderError
         *            error message for {@code network.setProvider}, {@link #UNANSWERED}
         *            to never answer it, or {@code null} to accept registration
         * @param holdConnect
         *            hold the {@code connect} response until a session is created or a
         *            short timeout elapses
         */
        FakeRuntimeServer(String setProviderError, boolean holdConnect) throws IOException {
            this.setProviderError = setProviderError;
            this.holdConnect = holdConnect;
            serverSocket = new ServerSocket(0);
            acceptThread = new Thread(this::acceptLoop, "network-runtime");
            acceptThread.setDaemon(true);
            acceptThread.start();
        }

        CopilotClient createClient(NetworkConfig network) {
            return createClient(options -> {
                if (network != null) {
                    options.setNetwork(network);
                }
            });
        }

        CopilotClient createClient(Consumer<CopilotClientOptions> configure) {
            var options = new CopilotClientOptions().setCliUrl("127.0.0.1:" + serverSocket.getLocalPort());
            configure.accept(options);
            return new CopilotClient(options);
        }

        CompletableFuture<JsonNode> request(Callback callback, String value) {
            return ready.join().invoke(callback.method, Map.of(callback.parameter, value), JsonNode.class);
        }

        void cancel(long id) throws IOException {
            ready.join().notify("$/cancelRequest", Map.of("id", id));
        }

        /** Round-trips a request the client answers on its reader thread. */
        CompletableFuture<JsonNode> barrier() {
            return ready.join().invoke("networkTest.barrier", Map.of(), JsonNode.class);
        }

        CompletableFuture<Void> connectReceived() {
            return connectReceived;
        }

        CompletableFuture<Void> setProviderReceived() {
            return setProviderReceived;
        }

        List<String> registrations() {
            return List.copyOf(registrations);
        }

        List<String> order() {
            return List.copyOf(order);
        }

        /**
         * Accepts each connection, so a client can start again; requests go to the
         * first.
         */
        private void acceptLoop() {
            try {
                while (true) {
                    Socket socket = serverSocket.accept();
                    var connection = JsonRpcClient.fromSocket(socket, rpc -> {
                        rpc.registerMethodHandler("connect", (id, params) -> {
                            connectReceived.complete(null);
                            // A session request that does not wait for startup arrives while connect is
                            // held and releases it; one that waits cannot, so the timeout releases it.
                            var release = holdConnect
                                    ? sessionCreated.copy().completeOnTimeout(null, 500, TimeUnit.MILLISECONDS)
                                    : CompletableFuture.completedFuture(null);
                            release.thenRun(() -> respond(rpc, id, CONNECT_RESULT));
                        });
                        rpc.registerMethodHandler("network.setProvider", (id, params) -> {
                            order.add("network.setProvider");
                            registrations.add(params.toString());
                            setProviderReceived.complete(null);
                            if (UNANSWERED.equals(setProviderError)) {
                                return;
                            }
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
                    });
                    connections.add(connection);
                    ready.complete(connection);
                }
            } catch (IOException e) {
                // Closing the server socket ends the loop.
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
            for (var connection : connections) {
                connection.close();
            }
            serverSocket.close();
            acceptThread.join(TimeUnit.SECONDS.toMillis(5));
        }
    }
}
