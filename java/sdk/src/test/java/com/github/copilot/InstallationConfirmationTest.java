/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.generated.rpc.InstallationConfirmationRequest;
import com.github.copilot.generated.rpc.InstallationDecision;
import com.github.copilot.generated.rpc.InstallationsConfirmResult;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.InstallationConfirmationContext;
import com.github.copilot.rpc.InstallationConfirmationHandler;
import com.github.copilot.rpc.RuntimeConnection;

@AllowCopilotExperimental
class InstallationConfirmationTest {

    private static final int TIMEOUT_SECONDS = 15;
    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

    @Test
    void typedReviewReceivedWithOriginalOperationAndSessionPreserved() throws Exception {
        var observedRequest = new CompletableFuture<InstallationConfirmationRequest>();
        var observedContext = new CompletableFuture<InstallationConfirmationContext>();
        InstallationConfirmationHandler handler = (request, context) -> {
            observedRequest.complete(request);
            observedContext.complete(context);
            return CompletableFuture.completedFuture(InstallationDecision.CONFIRM);
        };

        try (var peer = new TestPeer(handler)) {
            var result = peer.confirm(request("a")).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var request = observedRequest.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var context = observedContext.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertEquals("operation-a", request.operationId());
            assertEquals("original-session", request.policySessionId());
            assertEquals("challenge-a", request.confirmationId());
            assertEquals("fingerprint-a", request.reviewFingerprint());
            assertFalse(context.getCancelled().isDone());
            assertEquals(InstallationDecision.CONFIRM, result.decision());
            assertEquals(0, peer.extensionRegistrationCount());
        }
    }

    @Test
    void challengeAndFingerprintAreEchoed() throws Exception {
        try (var peer = new TestPeer(
                (request, context) -> CompletableFuture.completedFuture(InstallationDecision.DECLINE))) {
            var result = peer.confirm(request("echo")).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertEquals("challenge-echo", result.confirmationId());
            assertEquals("fingerprint-echo", result.reviewFingerprint());
            assertEquals(InstallationDecision.DECLINE, result.decision());
        }
    }

    @Test
    void cancelDecisionIsEchoed() throws Exception {
        try (var peer = new TestPeer(
                (request, context) -> CompletableFuture.completedFuture(InstallationDecision.CANCEL))) {
            var result = peer.confirm(request("cancel")).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertEquals("challenge-cancel", result.confirmationId());
            assertEquals("fingerprint-cancel", result.reviewFingerprint());
            assertEquals(InstallationDecision.CANCEL, result.decision());
        }
    }

    @Test
    void concurrentOutOfOrderDecisionsAndAnotherRpcWhilePending() throws Exception {
        var aDecision = new CompletableFuture<InstallationDecision>();
        var bDecision = new CompletableFuture<InstallationDecision>();
        var sawA = new CompletableFuture<Void>();
        var sawB = new CompletableFuture<Void>();
        InstallationConfirmationHandler handler = (request, context) -> {
            if ("operation-a".equals(request.operationId())) {
                sawA.complete(null);
                return aDecision;
            }
            sawB.complete(null);
            return bDecision;
        };

        try (var peer = new TestPeer(handler)) {
            var first = peer.confirm(request("a"));
            var second = peer.confirm(request("b"));
            sawA.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            sawB.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            var tokenError = assertRpcError(peer.runtime.invoke("gitHubToken.getToken",
                    Map.of("registrationId", "unknown", "host", "github.com", "reason", "test"), Object.class));
            assertEquals(-32603, tokenError.getCode());

            bDecision.complete(InstallationDecision.DECLINE);
            var secondResult = second.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertEquals("challenge-b", secondResult.confirmationId());
            assertEquals(InstallationDecision.DECLINE, secondResult.decision());

            aDecision.complete(InstallationDecision.CONFIRM);
            var firstResult = first.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertEquals("challenge-a", firstResult.confirmationId());
            assertEquals(InstallationDecision.CONFIRM, firstResult.decision());
        }
    }

    @Test
    void cancelRequestRetiresOnlyCancelledReviewAndDropsLateDecision() throws Exception {
        var lateDecision = new CompletableFuture<InstallationDecision>();
        var contexts = new ConcurrentHashMap<String, CompletableFuture<InstallationConfirmationContext>>();
        InstallationConfirmationHandler handler = (request, context) -> {
            contextFuture(contexts, request.operationId()).complete(context);
            if ("operation-a".equals(request.operationId())) {
                return lateDecision;
            }
            return CompletableFuture.completedFuture(InstallationDecision.DECLINE);
        };

        try (var peer = new TestPeer(handler)) {
            var first = peer.confirm(request("a"));
            var context = contextFuture(contexts, "operation-a").get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            peer.cancel(1L);
            var error = assertRpcError(first);
            assertEquals(-32800, error.getCode());
            assertTrue(context.getCancelled().isDone());

            lateDecision.complete(InstallationDecision.CONFIRM);
            var second = peer.confirm(request("b")).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertEquals("challenge-b", second.confirmationId());
            assertEquals(InstallationDecision.DECLINE, second.decision());
        }
    }

    @Test
    void cancelRequestAndDecisionReadyTogetherRejectsDecision() throws Exception {
        var returnGate = new CompletableFuture<Void>();
        var observedContext = new CompletableFuture<InstallationConfirmationContext>();
        InstallationConfirmationHandler handler = (request, context) -> {
            observedContext.complete(context);
            returnGate.join();
            return CompletableFuture.completedFuture(InstallationDecision.CONFIRM);
        };

        try (var peer = new TestPeer(handler)) {
            var response = peer.confirm(request("a"));
            var context = observedContext.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            peer.cancel(1L);
            var error = assertRpcError(response);
            assertEquals(-32800, error.getCode());
            assertTrue(context.getCancelled().isDone());

            returnGate.complete(null);
        } finally {
            returnGate.complete(null);
        }
    }

    @Test
    void staleUnknownOrStringCancelIdDoesNotTouchSuccessor() throws Exception {
        var bDecision = new CompletableFuture<InstallationDecision>();
        var contexts = new ConcurrentHashMap<String, CompletableFuture<InstallationConfirmationContext>>();
        InstallationConfirmationHandler handler = (request, context) -> {
            contextFuture(contexts, request.operationId()).complete(context);
            if ("operation-b".equals(request.operationId())) {
                return bDecision;
            }
            return CompletableFuture.completedFuture(InstallationDecision.DECLINE);
        };

        try (var peer = new TestPeer(handler)) {
            var first = peer.confirm(request("a")).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertEquals(InstallationDecision.DECLINE, first.decision());
            peer.cancel(1L);

            var second = peer.confirm(request("b"));
            var context = contextFuture(contexts, "operation-b").get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            peer.cancel(999L);
            peer.cancel("2");
            assertFalse(context.getCancelled().isDone());

            bDecision.complete(InstallationDecision.CONFIRM);
            var secondResult = second.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertEquals("challenge-b", secondResult.confirmationId());
            assertEquals(InstallationDecision.CONFIRM, secondResult.decision());
        }
    }

    @Test
    void connectionCloseCancelsAllReviewsAndDoesNotAffectAnotherConnection() throws Exception {
        var decisions = new ConcurrentHashMap<String, CompletableFuture<InstallationDecision>>();
        decisions.put("operation-a", new CompletableFuture<>());
        decisions.put("operation-b", new CompletableFuture<>());
        var contexts = new ConcurrentHashMap<String, CompletableFuture<InstallationConfirmationContext>>();
        try (var first = new TestPeer((request, context) -> {
            contextFuture(contexts, request.operationId()).complete(context);
            return decisions.get(request.operationId());
        });
                var second = new TestPeer(
                        (request, context) -> CompletableFuture.completedFuture(InstallationDecision.DECLINE))) {
            var firstResponse = first.confirm(request("a"));
            var secondResponse = first.confirm(request("b"));
            var firstContext = contextFuture(contexts, "operation-a").get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var secondContext = contextFuture(contexts, "operation-b").get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            first.closeRuntimeConnection();
            firstContext.getCancelled().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            secondContext.getCancelled().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertFutureFails(firstResponse);
            assertFutureFails(secondResponse);

            decisions.get("operation-a").complete(InstallationDecision.CONFIRM);
            decisions.get("operation-b").complete(InstallationDecision.DECLINE);
            var secondResult = second.confirm(request("c")).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertEquals("challenge-c", secondResult.confirmationId());
            assertEquals(InstallationDecision.DECLINE, secondResult.decision());
        }
    }

    @Test
    void stoppingClientRetiresPendingReviews() throws Exception {
        var pending = new CompletableFuture<InstallationDecision>();
        var observedContext = new CompletableFuture<InstallationConfirmationContext>();
        try (var peer = new TestPeer((request, context) -> {
            observedContext.complete(context);
            return pending;
        })) {
            var response = peer.confirm(request("a"));
            var context = observedContext.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            peer.client.forceStop().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            context.getCancelled().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertTrue(context.getCancelled().isDone());
            assertFutureFails(response);

            pending.complete(InstallationDecision.CONFIRM);
        }
    }

    @Test
    void missingHandlerInvalidReviewHandlerErrorsAndUnknownDecisionsNeverApprove() throws Exception {
        try (var missing = new TestPeer(null)) {
            var error = assertRpcError(missing.confirm(request("a")));
            assertEquals(-32601, error.getCode());
        }

        var handlerCalled = new AtomicBoolean();
        try (var invalid = new TestPeer((request, context) -> {
            handlerCalled.set(true);
            return CompletableFuture.completedFuture(InstallationDecision.CONFIRM);
        })) {
            var malformed = MAPPER.createObjectNode();
            malformed.put("confirmationId", "challenge-invalid");
            malformed.put("reviewFingerprint", "fingerprint-invalid");
            malformed.put("review", "not-an-object");
            var error = assertRpcError(
                    invalid.runtime.invoke("installations.confirm", malformed, InstallationsConfirmResult.class));
            assertEquals(-32602, error.getCode());
            assertFalse(handlerCalled.get());
        }

        try (var failing = new TestPeer((request, context) -> {
            throw new IllegalStateException("refused");
        })) {
            var error = assertRpcError(failing.confirm(request("a")));
            assertEquals(-32603, error.getCode());
        }

        try (var invalidDecision = new TestPeer(
                (request, context) -> CompletableFuture.completedFuture(InstallationDecision.fromValue("unknown")))) {
            var error = assertRpcError(invalidDecision.confirm(request("a")));
            assertEquals(-32603, error.getCode());
        }
    }

    @Test
    void optionalLegacySessionIsNotInferred() throws Exception {
        var observedSession = new CompletableFuture<String>();
        try (var peer = new TestPeer((request, context) -> {
            observedSession.complete(request.policySessionId());
            return CompletableFuture.completedFuture(InstallationDecision.DECLINE);
        })) {
            var incoming = requestNode("legacy");
            incoming.remove("policySessionId");
            var result = peer.runtime.invoke("installations.confirm", incoming, InstallationsConfirmResult.class)
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertNull(observedSession.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertEquals(InstallationDecision.DECLINE, result.decision());
        }
    }

    @Test
    void clonePreservesInstallationConfirmationHandler() {
        InstallationConfirmationHandler handler = (request, context) -> CompletableFuture
                .completedFuture(InstallationDecision.CONFIRM);
        var clone = new CopilotClientOptions().setInstallationConfirmationHandler(handler).clone();

        assertSame(handler, clone.getInstallationConfirmationHandler());
    }

    private static CompletableFuture<InstallationConfirmationContext> contextFuture(
            ConcurrentHashMap<String, CompletableFuture<InstallationConfirmationContext>> contexts,
            String operationId) {
        return contexts.computeIfAbsent(operationId, ignored -> new CompletableFuture<>());
    }

    private static InstallationConfirmationRequest request(String operation) throws IOException {
        return MAPPER.treeToValue(requestNode(operation), InstallationConfirmationRequest.class);
    }

    private static ObjectNode requestNode(String operation) throws IOException {
        ObjectNode node = (ObjectNode) MAPPER.readTree(Files.readString(fixturePath()));
        node.put("operationId", "operation-" + operation);
        node.put("confirmationId", "challenge-" + operation);
        node.put("reviewFingerprint", "fingerprint-" + operation);
        return node;
    }

    private static Path fixturePath() {
        Path cwd = Path.of("").toAbsolutePath();
        for (Path path = cwd; path != null; path = path.getParent()) {
            Path sdkCandidate = path.resolve("rust/tests/fixtures/installation_confirmation.json");
            if (Files.exists(sdkCandidate)) {
                return sdkCandidate;
            }
            Path repoCandidate = path.resolve("src/sdk/rust/tests/fixtures/installation_confirmation.json");
            if (Files.exists(repoCandidate)) {
                return repoCandidate;
            }
        }
        throw new IllegalStateException("Could not locate installation confirmation fixture from " + cwd);
    }

    private static JsonRpcException assertRpcError(CompletableFuture<?> future) {
        var error = assertThrows(ExecutionException.class, () -> future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        var cause = unwrap(error.getCause());
        assertInstanceOf(JsonRpcException.class, cause);
        return (JsonRpcException) cause;
    }

    private static void assertFutureFails(CompletableFuture<?> future) {
        assertThrows(ExecutionException.class, () -> future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    private static Throwable unwrap(Throwable error) {
        if (error instanceof CompletionException && error.getCause() != null) {
            return error.getCause();
        }
        return error;
    }

    private static final class TestPeer implements AutoCloseable {

        private final JsonRpcClient runtime;
        private final CopilotClient client;
        private final InputStream clientInput;
        private final OutputStream runtimeOutput;
        private final InputStream runtimeInput;
        private final OutputStream clientOutput;
        private final AtomicInteger extensionRegistrations = new AtomicInteger();

        TestPeer(InstallationConfirmationHandler handler) throws Exception {
            InputStream openedClientInput = null;
            OutputStream openedRuntimeOutput = null;
            InputStream openedRuntimeInput = null;
            OutputStream openedClientOutput = null;
            try {
                var runtimeToClient = new LoopbackPipe();
                openedClientInput = runtimeToClient.input();
                openedRuntimeOutput = runtimeToClient.output();
                var clientToRuntime = new LoopbackPipe();
                openedRuntimeInput = clientToRuntime.input();
                openedClientOutput = clientToRuntime.output();

                clientInput = openedClientInput;
                runtimeOutput = openedRuntimeOutput;
                runtimeInput = openedRuntimeInput;
                clientOutput = openedClientOutput;

                runtime = JsonRpcClient.fromStreams(runtimeInput, runtimeOutput);
                runtime.registerMethodHandler("connect", (id, params) -> respond(runtime, id,
                        Map.of("ok", true, "protocolVersion", 3, "version", "installation-confirmation-test")));
                runtime.registerMethodHandler("runtime.shutdown", (id, params) -> respond(runtime, id, Map.of()));
                runtime.registerMethodHandler("registerExtensionLaunchProvider", (id, params) -> {
                    extensionRegistrations.incrementAndGet();
                    respond(runtime, id, Map.of());
                });

                var options = new CopilotClientOptions().setConnection(RuntimeConnection.forInProcess());
                if (handler != null) {
                    options.setInstallationConfirmationHandler(handler);
                }
                client = new CopilotClient(options);
                client.setInProcessTransportFactory(
                        ignored -> new CopilotClient.InProcessTransport(clientInput, clientOutput, () -> {
                        }));
                client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (Exception | Error error) {
                closeQuietly(openedClientOutput);
                closeQuietly(openedRuntimeInput);
                closeQuietly(openedRuntimeOutput);
                closeQuietly(openedClientInput);
                throw error;
            }
        }

        CompletableFuture<InstallationsConfirmResult> confirm(InstallationConfirmationRequest request) {
            return runtime.invoke("installations.confirm", request, InstallationsConfirmResult.class);
        }

        void cancel(Object id) throws IOException {
            runtime.notify("$/cancelRequest", Map.of("id", id));
        }

        int extensionRegistrationCount() {
            return extensionRegistrations.get();
        }

        void closeRuntimeConnection() {
            runtime.close();
        }

        private static void respond(JsonRpcClient rpc, String id, Object result) {
            if (id == null) {
                return;
            }
            try {
                rpc.sendResponse(parseRpcId(id), result);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to send fake runtime response", e);
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
        public void close() {
            try {
                client.forceStop().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
            runtime.close();
            closeQuietly(clientOutput);
            closeQuietly(runtimeInput);
            closeQuietly(runtimeOutput);
            closeQuietly(clientInput);
        }

        private static void closeQuietly(Closeable stream) {
            if (stream == null) {
                return;
            }
            try {
                stream.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * PipedInputStream treats a short-lived writer thread as a broken pipe, which
     * makes these cross-thread JSON-RPC tests flaky with virtual-thread executors.
     */
    private static final class LoopbackPipe {
        private static final int EOF = -1;

        private final LinkedBlockingQueue<Integer> bytes = new LinkedBlockingQueue<>();
        private final AtomicBoolean inputClosed = new AtomicBoolean();
        private final AtomicBoolean outputClosed = new AtomicBoolean();
        private final InputStream input = new Input();
        private final OutputStream output = new Output();

        InputStream input() {
            return input;
        }

        OutputStream output() {
            return output;
        }

        private final class Input extends InputStream {
            @Override
            public int read() throws IOException {
                if (inputClosed.get()) {
                    return EOF;
                }
                try {
                    int value = bytes.take();
                    if (value == EOF) {
                        requeueEof();
                    }
                    return value;
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while reading loopback pipe", error);
                }
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (b == null) {
                    throw new NullPointerException("b");
                }
                if (off < 0 || len < 0 || len > b.length - off) {
                    throw new IndexOutOfBoundsException();
                }
                if (len == 0) {
                    return 0;
                }

                int first = read();
                if (first == EOF) {
                    return EOF;
                }
                b[off] = (byte) first;
                int count = 1;
                while (count < len) {
                    Integer value = bytes.poll();
                    if (value == null) {
                        break;
                    }
                    if (value == EOF) {
                        requeueEof();
                        break;
                    }
                    b[off + count] = (byte) (value & 0xff);
                    count++;
                }
                return count;
            }

            @Override
            public void close() {
                if (inputClosed.compareAndSet(false, true)) {
                    signalEof();
                }
            }

            /** Keeps end of stream visible to later reads. */
            private void requeueEof() throws IOException {
                if (!bytes.offer(EOF)) {
                    throw new IOException("Could not preserve end of loopback pipe");
                }
            }
        }

        /** Enqueues end of stream, failing loudly rather than losing it. */
        private void signalEof() {
            try {
                bytes.put(EOF);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while closing loopback pipe", error);
            }
        }

        private final class Output extends OutputStream {
            @Override
            public void write(int b) throws IOException {
                if (outputClosed.get()) {
                    throw new IOException("Pipe closed");
                }
                if (inputClosed.get()) {
                    throw new IOException("Pipe input closed");
                }
                try {
                    bytes.put(b & 0xff);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while writing loopback pipe", error);
                }
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                if (b == null) {
                    throw new NullPointerException("b");
                }
                if (off < 0 || len < 0 || len > b.length - off) {
                    throw new IndexOutOfBoundsException();
                }
                for (int i = 0; i < len; i++) {
                    write(b[off + i]);
                }
            }

            @Override
            public void close() {
                if (outputClosed.compareAndSet(false, true)) {
                    signalEof();
                }
            }
        }
    }
}
