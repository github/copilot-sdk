/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.rpc.CreateSessionRequest;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.ResumeSessionRequest;
import com.github.copilot.rpc.ShellAuthenticationHandler;
import com.github.copilot.rpc.ShellAuthenticationRequest;
import com.github.copilot.rpc.ShellAuthenticationResponse;
import com.github.copilot.rpc.SessionConfig;

/**
 * Unit tests for shell password prompts: {@code shellAuthentication.request}
 * and {@code shellAuthentication.cancel}. Mirrors the Node.js client shell
 * authentication tests.
 */
@AllowCopilotExperimental
class ShellAuthenticationTest {

    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();
    private static final int SOCKET_TIMEOUT_MS = 5000;
    private static final Logger SESSION_LOG = Logger.getLogger(CopilotSession.class.getName());

    private Socket clientSideSocket;
    private Socket serverSideSocket;
    private JsonRpcClient rpc;
    private Map<String, CopilotSession> sessions;
    private RpcHandlerDispatcher dispatcher;
    private InputStream responseStream;
    private Map<String, BiConsumer<String, JsonNode>> handlers;
    private final List<LogRecord> loggedFailures = new CopyOnWriteArrayList<>();
    private final Handler failureLog = new Handler() {
        @Override
        public void publish(LogRecord record) {
            String message = record.getMessage();
            if (message != null && message.startsWith("Shell authentication handler failed")) {
                loggedFailures.add(record);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    @BeforeEach
    void setup() throws Exception {
        SESSION_LOG.addHandler(failureLog);
        var loopback = InetAddress.getLoopbackAddress();
        try (ServerSocket ss = new ServerSocket(0, 1, loopback)) {
            clientSideSocket = new Socket(loopback, ss.getLocalPort());
            serverSideSocket = ss.accept();
        }
        serverSideSocket.setSoTimeout(SOCKET_TIMEOUT_MS);
        rpc = JsonRpcClient.fromSocket(clientSideSocket);
        responseStream = serverSideSocket.getInputStream();
        sessions = new ConcurrentHashMap<>();
        dispatcher = new RpcHandlerDispatcher(sessions, event -> {
        }, null, new GitHubTokenProviderRegistry());
        dispatcher.registerHandlers(rpc);

        Field f = JsonRpcClient.class.getDeclaredField("notificationHandlers");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, BiConsumer<String, JsonNode>> h = (Map<String, BiConsumer<String, JsonNode>>) f.get(rpc);
        handlers = h;
    }

    @AfterEach
    void teardown() throws Exception {
        SESSION_LOG.removeHandler(failureLog);
        if (rpc != null) {
            rpc.close();
        }
        if (serverSideSocket != null) {
            serverSideSocket.close();
        }
        if (clientSideSocket != null) {
            clientSideSocket.close();
        }
    }

    private void invokeHandler(String method, String requestId, JsonNode params) {
        handlers.get(method).accept(requestId, params);
    }

    private JsonNode readResponse() throws Exception {
        StringBuilder header = new StringBuilder();
        while (!header.toString().endsWith("\r\n\r\n")) {
            int b = responseStream.read();
            if (b == -1) {
                throw new java.io.IOException("Unexpected end of stream");
            }
            header.append((char) b);
        }
        String headerStr = header.toString().trim();
        int idx = headerStr.indexOf(':');
        int contentLength = Integer.parseInt(headerStr.substring(idx + 1).trim());
        return MAPPER.readTree(responseStream.readNBytes(contentLength));
    }

    private CopilotSession createSession(String sessionId, ShellAuthenticationHandler handler) {
        CopilotSession session = new CopilotSession(sessionId, rpc);
        session.registerShellAuthenticationHandler(handler);
        sessions.put(sessionId, session);
        return session;
    }

    private static ObjectNode secretRequest(String sessionId, String requestId) {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", sessionId);
        params.put("requestId", requestId);
        params.put("program", "ssh");
        params.put("prompt", "Enter passphrase for key '/k': ");
        params.put("inputKind", "secret");
        params.put("command", "ssh host");
        params.put("cwd", "/work");
        return params;
    }

    private static ObjectNode cancelRequest(String sessionId, String requestId) {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("sessionId", sessionId);
        params.put("requestId", requestId);
        return params;
    }

    @Test
    void returnsASecretOnlyForAWellFormedAnswer() throws Exception {
        List<Supplier<CompletableFuture<ShellAuthenticationResponse>>> answers = new ArrayList<>(List.of(
                () -> CompletableFuture.completedFuture(ShellAuthenticationResponse.accept("HUNTER2-FAKE-0001")),
                () -> {
                    throw new IllegalStateException("rejected HUNTER2-FAKE-0003");
                }, () -> null,
                () -> CompletableFuture.failedFuture(new IllegalArgumentException("rejected HUNTER2-FAKE-0004")),
                () -> CompletableFuture
                        .<ShellAuthenticationResponse>failedFuture(
                                new UnsupportedOperationException("rejected HUNTER2-FAKE-0005"))
                        .thenApply(response -> response),
                () -> CompletableFuture
                        .completedFuture(new ShellAuthenticationResponse().setAction("maybe").setValue("unused")),
                () -> CompletableFuture.completedFuture(ShellAuthenticationResponse.cancel())));
        int count = answers.size();
        createSession("s1", (request, invocation) -> answers.remove(0).get());

        List<JsonNode> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            invokeHandler("shellAuthentication.request", String.valueOf(i + 1), secretRequest("s1", "secret-" + i));
            results.add(readResponse().get("result"));
        }

        assertEquals(MAPPER.readTree("{\"action\":\"accept\",\"value\":\"HUNTER2-FAKE-0001\"}"), results.get(0));
        for (JsonNode result : results.subList(1, results.size())) {
            assertEquals(MAPPER.readTree("{\"action\":\"cancel\"}"), result);
        }
        assertEquals(List.of(
                "Shell authentication handler failed for requestId=secret-1 with java.lang.IllegalStateException",
                "Shell authentication handler failed for requestId=secret-2 with java.lang.IllegalStateException",
                "Shell authentication handler failed for requestId=secret-3 with java.lang.IllegalArgumentException",
                "Shell authentication handler failed for requestId=secret-4 with java.lang.UnsupportedOperationException"),
                loggedFailures.stream().map(LogRecord::getMessage).toList());
        // Only the type is logged, never the exception, whose message could contain the
        // answer.
        assertTrue(loggedFailures.stream()
                .allMatch(record -> record.getLevel() == Level.SEVERE && record.getThrown() == null));
    }

    @Test
    void aHandlerThatThrowsAnErrorStillCancelsThePrompt() throws Exception {
        createSession("s1", (request, invocation) -> {
            throw new AssertionError("rejected HUNTER2-FAKE-0006");
        });

        invokeHandler("shellAuthentication.request", "1", secretRequest("s1", "secret-error"));

        assertEquals(MAPPER.readTree("{\"action\":\"cancel\"}"), readResponse().get("result"));
        assertEquals(
                List.of("Shell authentication handler failed for requestId=secret-error with java.lang.AssertionError"),
                loggedFailures.stream().map(LogRecord::getMessage).toList());
        assertNull(loggedFailures.get(0).getThrown());
    }

    @Test
    void passesTheRuntimeRequestFields() throws Exception {
        List<ShellAuthenticationRequest> requests = new ArrayList<>();
        List<String> sessionIds = new ArrayList<>();
        createSession("s1", (request, invocation) -> {
            requests.add(request);
            sessionIds.add(invocation.getSessionId());
            return CompletableFuture.completedFuture(ShellAuthenticationResponse.cancel());
        });

        invokeHandler("shellAuthentication.request", "1", secretRequest("s1", "secret-1"));
        readResponse();
        ObjectNode userName = secretRequest("s1", "secret-2");
        userName.put("inputKind", "text");
        invokeHandler("shellAuthentication.request", "2", userName);
        readResponse();

        assertEquals(List.of("s1", "s1"), sessionIds);
        ShellAuthenticationRequest secret = requests.get(0);
        assertEquals(List.of("secret-1", "ssh", "Enter passphrase for key '/k': ", "secret", "ssh host", "/work"),
                List.of(secret.getRequestId(), secret.getProgram(), secret.getPrompt(), secret.getInputKind(),
                        secret.getCommand(), secret.getCwd()));
        assertEquals("text", requests.get(1).getInputKind());
    }

    @Test
    void cancelsAMalformedRequestWithoutAsking() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        createSession("s1", (request, invocation) -> {
            calls.incrementAndGet();
            return CompletableFuture.completedFuture(ShellAuthenticationResponse.accept("unused"));
        });
        ObjectNode unknownKind = secretRequest("s1", "secret-1");
        unknownKind.put("inputKind", "password");
        ObjectNode noRequestId = secretRequest("s1", "secret-2");
        noRequestId.remove("requestId");
        ObjectNode unknownSession = secretRequest("unknown", "secret-3");

        int id = 0;
        for (ObjectNode params : List.of(unknownKind, noRequestId, unknownSession)) {
            invokeHandler("shellAuthentication.request", String.valueOf(++id), params);
            assertEquals("cancel", readResponse().get("result").get("action").asText());
        }
        assertEquals(0, calls.get());
    }

    @Test
    void runtimeCancelClosesAPendingPrompt() throws Exception {
        CompletableFuture<CompletionStage<Void>> started = new CompletableFuture<>();
        CompletableFuture<ShellAuthenticationResponse> neverAnswered = new CompletableFuture<>();
        createSession("s1", (request, invocation) -> {
            started.complete(invocation.getCancellation());
            return neverAnswered;
        });

        invokeHandler("shellAuthentication.request", "1", secretRequest("s1", "secret-1"));
        CompletionStage<Void> cancellation = started.get(5, TimeUnit.SECONDS);
        invokeHandler("shellAuthentication.cancel", "2", cancelRequest("s1", "secret-1"));

        JsonNode first = readResponse();
        JsonNode second = readResponse();
        JsonNode secretResponse = first.get("id").asInt() == 1 ? first : second;
        assertEquals("cancel", secretResponse.get("result").get("action").asText());
        // Cancellation completes on its own thread, which can answer the request
        // before it completes the stage the handler got.
        cancellation.toCompletableFuture().get(5, TimeUnit.SECONDS);
        assertTrue(neverAnswered.isCancelled());
        assertEquals(List.of(), loggedFailures);
    }

    @Test
    void aDuplicateRequestIsCancelledWithoutClosingTheOpenPrompt() throws Exception {
        CompletableFuture<CompletionStage<Void>> started = new CompletableFuture<>();
        AtomicInteger calls = new AtomicInteger();
        createSession("s1", (request, invocation) -> {
            calls.incrementAndGet();
            started.complete(invocation.getCancellation());
            return new CompletableFuture<>();
        });

        invokeHandler("shellAuthentication.request", "1", secretRequest("s1", "secret-1"));
        CompletionStage<Void> cancellation = started.get(5, TimeUnit.SECONDS);
        invokeHandler("shellAuthentication.request", "2", secretRequest("s1", "secret-1"));

        JsonNode duplicate = readResponse();
        assertEquals(2, duplicate.get("id").asInt());
        assertEquals("cancel", duplicate.get("result").get("action").asText());
        assertFalse(cancellation.toCompletableFuture().isDone());
        assertEquals(1, calls.get());

        invokeHandler("shellAuthentication.cancel", "3", cancelRequest("s1", "secret-1"));
        JsonNode first = readResponse();
        JsonNode second = readResponse();
        JsonNode open = first.get("id").asInt() == 1 ? first : second;
        assertEquals("cancel", open.get("result").get("action").asText());
    }

    @Test
    void cancelReachesTheSessionThatTookTheRequestAfterAResume() throws Exception {
        CompletableFuture<Void> started = new CompletableFuture<>();
        CompletableFuture<ShellAuthenticationResponse> neverAnswered = new CompletableFuture<>();
        createSession("s1", (request, invocation) -> {
            started.complete(null);
            return neverAnswered;
        });

        invokeHandler("shellAuthentication.request", "1", secretRequest("s1", "secret-1"));
        started.get(5, TimeUnit.SECONDS);
        // A resume registers a new session object while the prompt is open.
        sessions.put("s1", new CopilotSession("s1", rpc));
        invokeHandler("shellAuthentication.cancel", "2", cancelRequest("s1", "secret-1"));

        JsonNode first = readResponse();
        JsonNode second = readResponse();
        JsonNode secretResponse = first.get("id").asInt() == 1 ? first : second;
        assertEquals("cancel", secretResponse.get("result").get("action").asText());
    }

    @Test
    void connectionCloseAndSessionCloseCancelOpenPrompts() throws Exception {
        CompletableFuture<ShellAuthenticationResponse> connectionAnswer = new CompletableFuture<>();
        CompletableFuture<Void> connectionStarted = new CompletableFuture<>();
        createSession("s1", (request, invocation) -> {
            connectionStarted.complete(null);
            return connectionAnswer;
        });
        invokeHandler("shellAuthentication.request", "1", secretRequest("s1", "secret-1"));
        connectionStarted.get(5, TimeUnit.SECONDS);
        dispatcher.cancelShellAuthenticationRequests();
        assertEquals("cancel", readResponse().get("result").get("action").asText());
        assertTrue(connectionAnswer.isCancelled());

        CompletableFuture<ShellAuthenticationResponse> closedAnswer = new CompletableFuture<>();
        CompletableFuture<Void> closedStarted = new CompletableFuture<>();
        CopilotSession closed = createSession("s2", (request, invocation) -> {
            closedStarted.complete(null);
            return closedAnswer;
        });
        invokeHandler("shellAuthentication.request", "2", secretRequest("s2", "secret-2"));
        closedStarted.get(5, TimeUnit.SECONDS);
        closed.cancelPendingShellAuthenticationRequests();
        assertEquals("cancel", readResponse().get("result").get("action").asText());
        assertTrue(closedAnswer.isCancelled());
    }

    @Test
    void runtimeCancelDoesNotRunAppContinuationsOnTheReaderThread() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Void> handled = new CompletableFuture<>();
        CompletableFuture<Void> continuationRan = new CompletableFuture<>();
        createSession("s1", (request, invocation) -> {
            invocation.getCancellation().thenRun(() -> {
                continuationRan.complete(null);
                // An app continuation that waits, for example on a call to the runtime,
                // which only the reader thread can answer.
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            handled.complete(null);
            return new CompletableFuture<>();
        });
        invokeHandler("shellAuthentication.request", "1", secretRequest("s1", "secret-1"));
        handled.get(5, TimeUnit.SECONDS);

        // invokeHandler runs the cancel on the calling thread, as the reader thread
        // does.
        CompletableFuture
                .runAsync(() -> invokeHandler("shellAuthentication.cancel", "2", cancelRequest("s1", "secret-1")))
                .get(5, TimeUnit.SECONDS);
        continuationRan.get(5, TimeUnit.SECONDS);
        release.countDown();

        JsonNode first = readResponse();
        JsonNode second = readResponse();
        JsonNode secretResponse = first.get("id").asInt() == 1 ? first : second;
        assertEquals("cancel", secretResponse.get("result").get("action").asText());
    }

    @Test
    void forceStopDoesNotRunAppContinuationsOnTheStoppingThread() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Void> handled = new CompletableFuture<>();
        CompletableFuture<Void> continuationRan = new CompletableFuture<>();
        CopilotSession session = createSession("s1", (request, invocation) -> {
            invocation.getCancellation().thenRun(() -> {
                continuationRan.complete(null);
                // An app continuation that waits, for example for the app to finish
                // stopping.
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            handled.complete(null);
            return new CompletableFuture<>();
        });
        invokeHandler("shellAuthentication.request", "1", secretRequest("s1", "secret-1"));
        handled.get(5, TimeUnit.SECONDS);

        try {
            CompletableFuture.runAsync(session::forceStopLocally).get(5, TimeUnit.SECONDS);
            continuationRan.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
        }
        assertEquals("cancel", readResponse().get("result").get("action").asText());
    }

    @Test
    void transportLoggingNeverShowsAnAcceptedAnswer() throws Exception {
        Logger transportLog = Logger.getLogger(JsonRpcClient.class.getName());
        Level previousLevel = transportLog.getLevel();
        List<String> logged = new CopyOnWriteArrayList<>();
        Handler capture = new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        capture.setLevel(Level.FINE);
        transportLog.addHandler(capture);
        transportLog.setLevel(Level.FINE);
        try {
            createSession("s1", (request, invocation) -> CompletableFuture
                    .completedFuture(ShellAuthenticationResponse.accept("HUNTER2-FAKE-0001")));
            invokeHandler("shellAuthentication.request", "1", secretRequest("s1", "secret-1"));
            assertEquals("HUNTER2-FAKE-0001", readResponse().get("result").get("value").asText());
            // The response is logged right after it is written.
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (logged.stream().noneMatch(message -> message.startsWith("Sent: ")) && System.nanoTime() < deadline) {
                Thread.sleep(1);
            }
        } finally {
            transportLog.removeHandler(capture);
            transportLog.setLevel(previousLevel);
        }

        List<String> sent = logged.stream().filter(message -> message.startsWith("Sent: ")).toList();
        assertEquals(1, sent.size(), sent::toString);
        assertTrue(sent.get(0).contains("\"value\":\"<redacted>\""), sent.get(0));
        assertTrue(logged.stream().noneMatch(message -> message.contains("HUNTER2")), logged::toString);
    }

    @Test
    void aPromptCannotOpenWhileItsSessionIsClosing() throws Exception {
        AtomicInteger asked = new AtomicInteger();
        CopilotSession session = createSession("s1", (request, invocation) -> {
            asked.incrementAndGet();
            return new CompletableFuture<>();
        });
        var request = new ShellAuthenticationRequest().setRequestId("secret-1").setProgram("ssh")
                .setPrompt("Enter passphrase for key '/k': ").setInputKind("secret");
        var admitted = new CompletableFuture<CompletableFuture<ShellAuthenticationResponse>>();
        Thread admitting = new Thread(
                () -> admitted.complete(session.handleShellAuthenticationRequest(request, new CompletableFuture<>())));
        // Close the session while the prompt is being admitted: admission waits for the
        // lock that retirement holds, then sees the session closed.
        synchronized (session) {
            admitting.start();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (admitting.getState() != Thread.State.BLOCKED && System.nanoTime() < deadline) {
                Thread.sleep(1);
            }
            assertEquals(Thread.State.BLOCKED, admitting.getState(), "admission did not wait for the session lock");
            session.forceStopLocally();
        }

        assertEquals(ShellAuthenticationResponse.ACTION_CANCEL,
                admitted.get(5, TimeUnit.SECONDS).get(5, TimeUnit.SECONDS).getAction());
        assertEquals(0, asked.get());
    }

    @Test
    void anAnswerAfterTheSessionStopsIsRefusedBeforeItsCancellationRuns() throws Exception {
        CompletableFuture<ShellAuthenticationResponse> answer = new CompletableFuture<>();
        CopilotSession session = createSession("s1", (request, invocation) -> answer);
        var request = new ShellAuthenticationRequest().setRequestId("secret-1").setProgram("ssh")
                .setPrompt("Enter passphrase for key '/k': ").setInputKind("secret");
        // Stopping completes the prompt's cancellation on a cleanup thread. This one
        // ignores that completion to hold the moment before the thread runs.
        var cancellationNotYetRun = new CompletableFuture<Void>() {
            @Override
            public boolean complete(Void value) {
                return false;
            }
        };
        CompletableFuture<ShellAuthenticationResponse> response = session.handleShellAuthenticationRequest(request,
                cancellationNotYetRun);

        session.forceStopLocally();
        answer.complete(ShellAuthenticationResponse.accept("HUNTER2-FAKE-0001"));

        ShellAuthenticationResponse sent = response.get(5, TimeUnit.SECONDS);
        assertEquals(ShellAuthenticationResponse.ACTION_CANCEL, sent.getAction());
        assertNull(sent.getValue());
    }

    @Test
    void requestsShellAuthenticationOnlyWithAHandler() {
        ShellAuthenticationHandler handler = (request, invocation) -> CompletableFuture
                .completedFuture(ShellAuthenticationResponse.cancel());

        CreateSessionRequest create = SessionRequestBuilder
                .buildCreateRequest(new SessionConfig().setOnShellAuthenticationRequest(handler), "create-session");
        ResumeSessionRequest resume = SessionRequestBuilder.buildResumeRequest("resume-session",
                new ResumeSessionConfig().setOnShellAuthenticationRequest(handler));
        CreateSessionRequest plainCreate = SessionRequestBuilder.buildCreateRequest(new SessionConfig(),
                "plain-create");
        ResumeSessionRequest plainResume = SessionRequestBuilder.buildResumeRequest("plain-resume",
                new ResumeSessionConfig());

        assertTrue(create.getRequestShellAuthentication());
        assertTrue(resume.getRequestShellAuthentication());
        assertNull(plainCreate.getRequestShellAuthentication());
        assertNull(plainResume.getRequestShellAuthentication());
        assertTrue(MAPPER.valueToTree(create).get("requestShellAuthentication").asBoolean());
        assertFalse(MAPPER.valueToTree(plainCreate).has("requestShellAuthentication"));
    }
}
