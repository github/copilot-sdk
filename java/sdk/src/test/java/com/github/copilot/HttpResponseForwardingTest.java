/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLSession;

import org.junit.jupiter.api.Test;

import com.github.copilot.generated.rpc.LlmInferenceHttpResponseChunkParams;
import com.github.copilot.generated.rpc.RpcCaller;
import com.github.copilot.generated.rpc.ServerRpc;

class HttpResponseForwardingTest {

    private static final Duration DEADLINE = Duration.ofSeconds(5);

    @Test
    void readsAheadAndCoalescesUnderOneWithheldAck() throws Exception {
        try (TestFlow flow = new TestFlow()) {
            flow.source.feed("first");
            flow.ack(flow.next("llmInference.httpResponseStart"));

            PendingCall first = flow.nextData();
            assertArrayEquals(bytes("first"), data(first));

            byte[] expected = new byte[HttpResponseReader.CHUNK_SIZE];
            for (int i = 0; i < 32; i++) {
                byte[] fragment = new byte[1024];
                Arrays.fill(fragment, (byte) i);
                System.arraycopy(fragment, 0, expected, i * fragment.length, fragment.length);
                flow.source.feed(fragment);
            }
            flow.source.awaitBytesRead(5 + HttpResponseReader.CHUNK_SIZE);
            flow.source.feed("last");

            assertNull(flow.poll(), "No second data RPC may overtake the outstanding write");
            assertEquals(1, flow.caller.outstandingData.get());
            assertEquals(1, flow.caller.maximumOutstandingData.get());

            flow.ack(first);
            PendingCall combined = flow.nextData();
            assertArrayEquals(expected, data(combined));
            assertEquals(1, flow.caller.outstandingData.get());

            flow.source.finish();
            flow.ack(combined);
            PendingCall last = flow.nextData();
            assertArrayEquals(bytes("last"), data(last));
            flow.ack(last);

            PendingCall end = flow.next("llmInference.httpResponseChunk");
            assertTrue(chunk(end).end());
            assertNull(chunk(end).error());
            flow.ack(end);
            flow.awaitComplete();
            assertEquals(1, flow.caller.maximumOutstandingData.get());
        }
    }

    @Test
    void flushesPartialBytesWithoutWaitingForFutureInput() throws Exception {
        try (TestFlow flow = new TestFlow()) {
            flow.ack(flow.next("llmInference.httpResponseStart"));
            flow.source.feed("partial");

            PendingCall partial = flow.nextData();
            assertArrayEquals(bytes("partial"), data(partial));

            flow.source.finish();
            flow.ack(partial);
            PendingCall end = flow.next("llmInference.httpResponseChunk");
            assertTrue(chunk(end).end());
            flow.ack(end);
            flow.awaitComplete();
        }
    }

    @Test
    void upstreamErrorFollowsBufferedBytesAndOutstandingAck() throws Exception {
        try (TestFlow flow = new TestFlow()) {
            flow.source.feed("first");
            flow.ack(flow.next("llmInference.httpResponseStart"));
            PendingCall first = flow.nextData();

            flow.source.feed("partial");
            flow.source.fail(new IOException("upstream failed"));
            flow.source.awaitFailureRead();
            assertNull(flow.poll(), "Buffered bytes must not overtake the outstanding write");

            flow.ack(first);
            PendingCall partial = flow.nextData();
            assertArrayEquals(bytes("partial"), data(partial));
            assertNull(flow.poll(), "The upstream error must follow the buffered bytes");

            flow.ack(partial);
            PendingCall error = flow.next("llmInference.httpResponseChunk");
            assertTrue(chunk(error).end());
            assertEquals("upstream failed", chunk(error).error().message());
            flow.ack(error);
            flow.awaitComplete();
            assertTrue(flow.source.closed.get());
        }
    }

    @Test
    void uncheckedUpstreamErrorFollowsBufferedBytesAndOutstandingAck() throws Exception {
        try (TestFlow flow = new TestFlow()) {
            flow.source.feed("first");
            flow.ack(flow.next("llmInference.httpResponseStart"));
            PendingCall first = flow.nextData();

            flow.source.feed("partial");
            flow.source.fail(new UncheckedIOException(new IOException("unchecked upstream failed")));
            flow.source.awaitFailureRead();
            assertNull(flow.poll(), "Buffered bytes must not overtake the outstanding write");

            flow.ack(first);
            PendingCall partial = flow.nextData();
            assertArrayEquals(bytes("partial"), data(partial));
            assertNull(flow.poll(), "The upstream error must follow the buffered bytes");

            flow.ack(partial);
            PendingCall error = flow.next("llmInference.httpResponseChunk");
            assertTrue(chunk(error).end());
            assertEquals("unchecked upstream failed", chunk(error).error().message());
            flow.ack(error);
            flow.awaitComplete();
            assertTrue(flow.source.closed.get());
        }
    }

    @Test
    void messageLessUncheckedUpstreamErrorHasTerminalMessage() throws Exception {
        try (TestFlow flow = new TestFlow()) {
            flow.ack(flow.next("llmInference.httpResponseStart"));
            flow.source.fail(new IllegalStateException());
            flow.source.awaitFailureRead();

            PendingCall error = flow.next("llmInference.httpResponseChunk");
            assertTrue(chunk(error).end());
            assertTrue(chunk(error).error().message().contains(IllegalStateException.class.getName()));
            flow.ack(error);
            flow.awaitComplete();
            assertTrue(flow.source.closed.get());
        }
    }

    @Test
    void cancellationClosesSourceWithoutWaitingForDataAck() throws Exception {
        try (TestFlow flow = new TestFlow()) {
            flow.source.feed("first");
            flow.ack(flow.next("llmInference.httpResponseStart"));
            PendingCall first = flow.nextData();

            flow.exchange.pushCancel();
            flow.source.awaitClosed();

            PendingCall cancelled = flow.next("llmInference.httpResponseChunk");
            assertTrue(chunk(cancelled).end());
            assertEquals("cancelled", chunk(cancelled).error().code());
            flow.ack(cancelled);
            flow.awaitComplete();
            flow.ack(first);
        }
    }

    @Test
    void cancellationClosesSourceWithoutWaitingForHeadAck() throws Exception {
        try (TestFlow flow = new TestFlow()) {
            PendingCall head = flow.next("llmInference.httpResponseStart");

            flow.exchange.pushCancel();
            flow.source.awaitClosed();

            PendingCall cancelled = flow.next("llmInference.httpResponseChunk");
            assertTrue(chunk(cancelled).end());
            assertEquals("cancelled", chunk(cancelled).error().code());
            flow.ack(cancelled);
            flow.awaitComplete();
            flow.ack(head);
        }
    }

    @Test
    void rpcRejectionClosesSourceAndReportsError() throws Exception {
        assertRpcFailureClosesSource(new JsonRpcException(-32603, "write rejected"), "write rejected");
    }

    @Test
    void connectionLossClosesSourceAndReportsError() throws Exception {
        assertRpcFailureClosesSource(new IOException("Client closed"), "Client closed");
    }

    private static void assertRpcFailureClosesSource(Exception failure, String expectedMessage) throws Exception {
        try (TestFlow flow = new TestFlow()) {
            flow.source.feed("first");
            flow.ack(flow.next("llmInference.httpResponseStart"));
            PendingCall first = flow.nextData();

            flow.reject(first, failure);
            flow.source.awaitClosed();

            PendingCall error = flow.next("llmInference.httpResponseChunk");
            assertTrue(chunk(error).end());
            assertTrue(chunk(error).error().message().contains(expectedMessage));
            flow.ack(error);
            flow.awaitComplete();
        }
    }

    private static byte[] data(PendingCall call) {
        LlmInferenceHttpResponseChunkParams chunk = chunk(call);
        assertFalse(chunk.end());
        assertEquals(Boolean.TRUE, chunk.binary());
        return Base64.getDecoder().decode(chunk.data());
    }

    private static LlmInferenceHttpResponseChunkParams chunk(PendingCall call) {
        return (LlmInferenceHttpResponseChunkParams) call.params();
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private record PendingCall(String method, Object params, CompletableFuture<Object> result) {
    }

    private static final class RecordingCaller implements RpcCaller {

        private final BlockingQueue<PendingCall> calls = new LinkedBlockingQueue<>();
        private final AtomicInteger outstandingData = new AtomicInteger();
        private final AtomicInteger maximumOutstandingData = new AtomicInteger();

        @Override
        public <T> CompletableFuture<T> invoke(String method, Object params, Class<T> resultType) {
            CompletableFuture<Object> result = new CompletableFuture<>();
            if (params instanceof LlmInferenceHttpResponseChunkParams chunk && !chunk.end()) {
                int outstanding = outstandingData.incrementAndGet();
                maximumOutstandingData.accumulateAndGet(outstanding, Math::max);
                result.whenComplete((ignored, error) -> outstandingData.decrementAndGet());
            }
            calls.add(new PendingCall(method, params, result));
            @SuppressWarnings("unchecked")
            CompletableFuture<T> typed = (CompletableFuture<T>) (CompletableFuture<?>) result;
            return typed;
        }
    }

    private static final class TestFlow implements AutoCloseable {

        private final QueueInputStream source = new QueueInputStream();
        private final RecordingCaller caller = new RecordingCaller();
        private final LlmInferenceExchange exchange;
        private final CompletableFuture<Void> task;

        TestFlow() {
            ServerRpc rpc = new ServerRpc(caller);
            exchange = new LlmInferenceExchange("test", () -> rpc.llmInference);
            exchange.setMethod("GET");
            exchange.setContext(new CopilotRequestContext("test", null, null, null, null, CopilotRequestTransport.HTTP,
                    "http://unused.test", Map.of(), exchange.cancellation()));
            exchange.pushEnd();

            CopilotRequestHandler handler = new CopilotRequestHandler() {
                @Override
                protected HttpResponse<InputStream> sendRequest(HttpRequest request, CopilotRequestContext context) {
                    return new StubResponse(source, request);
                }
            };
            task = CompletableFuture.runAsync(() -> {
                try {
                    handler.handle(exchange);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        PendingCall next(String method) throws Exception {
            PendingCall call = caller.calls.poll(DEADLINE.toMillis(), TimeUnit.MILLISECONDS);
            assertNotNull(call, "Timed out waiting for " + method);
            assertEquals(method, call.method());
            return call;
        }

        PendingCall nextData() throws Exception {
            PendingCall call = next("llmInference.httpResponseChunk");
            assertFalse(chunk(call).end());
            return call;
        }

        PendingCall poll() throws InterruptedException {
            return caller.calls.poll(100, TimeUnit.MILLISECONDS);
        }

        void ack(PendingCall call) {
            call.result().complete(null);
        }

        void reject(PendingCall call, Exception error) {
            call.result().completeExceptionally(error);
        }

        void awaitComplete() throws Exception {
            task.get(DEADLINE.toMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public void close() throws Exception {
            source.close();
            if (!task.isDone()) {
                exchange.pushCancel();
                PendingCall call;
                while ((call = caller.calls.poll()) != null) {
                    call.result().complete(null);
                }
            }
            task.get(DEADLINE.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private static final class QueueInputStream extends InputStream {

        private static final Object END = new Object();

        private final BlockingQueue<Object> items = new LinkedBlockingQueue<>();
        private final AtomicLong bytesRead = new AtomicLong();
        private final AtomicBoolean closed = new AtomicBoolean();
        private final CompletableFuture<Void> failureRead = new CompletableFuture<>();
        private final CompletableFuture<Void> closeObserved = new CompletableFuture<>();
        private final AtomicReference<byte[]> current = new AtomicReference<>();
        private int currentOffset;

        void feed(String value) {
            feed(bytes(value));
        }

        void feed(byte[] value) {
            items.add(value);
        }

        void finish() {
            items.add(END);
        }

        void fail(IOException error) {
            items.add(error);
        }

        void fail(RuntimeException error) {
            items.add(error);
        }

        void awaitBytesRead(long expected) throws Exception {
            long deadline = System.nanoTime() + DEADLINE.toNanos();
            while (bytesRead.get() < expected && System.nanoTime() < deadline) {
                Thread.sleep(5);
            }
            assertEquals(expected, bytesRead.get());
        }

        void awaitFailureRead() throws Exception {
            failureRead.get(DEADLINE.toMillis(), TimeUnit.MILLISECONDS);
        }

        void awaitClosed() throws Exception {
            closeObserved.get(DEADLINE.toMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int read() throws IOException {
            byte[] single = new byte[1];
            int count = read(single, 0, 1);
            return count < 0 ? -1 : Byte.toUnsignedInt(single[0]);
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            while (true) {
                if (closed.get()) {
                    throw new IOException("stream closed");
                }
                byte[] data = current.get();
                if (data != null) {
                    int count = Math.min(length, data.length - currentOffset);
                    System.arraycopy(data, currentOffset, buffer, offset, count);
                    currentOffset += count;
                    if (currentOffset == data.length) {
                        current.set(null);
                        currentOffset = 0;
                    }
                    bytesRead.addAndGet(count);
                    return count;
                }

                Object item;
                try {
                    item = items.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("interrupted", e);
                }
                if (item == END) {
                    return -1;
                }
                if (item instanceof IOException e) {
                    failureRead.complete(null);
                    throw e;
                }
                if (item instanceof RuntimeException e) {
                    failureRead.complete(null);
                    throw e;
                }
                current.set((byte[]) item);
            }
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                items.add(END);
                closeObserved.complete(null);
            }
        }
    }

    private record StubResponse(InputStream body, HttpRequest request) implements HttpResponse<InputStream> {

        @Override
        public int statusCode() {
            return 200;
        }

        @Override
        public Optional<HttpResponse<InputStream>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of("content-type", List.of("application/octet-stream")), (name, value) -> true);
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
