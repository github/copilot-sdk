/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.rpc.MessageOptions;

/**
 * Regression tests for timeout edge cases in
 * {@link CopilotSession#sendAndWait}.
 * <p>
 * These tests assert two behavioral contracts of the shared
 * {@code ScheduledExecutorService} approach:
 * <ol>
 * <li>A pending timeout must NOT fire after {@code close()} and must NOT
 * complete the returned future with a {@code TimeoutException}.</li>
 * <li>Multiple {@code sendAndWait} calls must reuse a single shared scheduler
 * thread rather than spawning a new OS thread per call.</li>
 * </ol>
 */
public class TimeoutEdgeCaseTest {

    /**
     * Creates a {@link JsonRpcClient} whose prompt requests never complete but
     * whose cleanup detach request succeeds.
     */
    private JsonRpcClient createHangingRpcClient() throws Exception {
        PipedInputStream input = new PipedInputStream();
        PipedOutputStream responseWriter = new PipedOutputStream(input);
        OutputStream sinkOutput = new ByteArrayOutputStream() {
            @Override
            public synchronized void flush() throws IOException {
                super.flush();
                // Each JsonRpcClient.sendMessage() call writes exactly one
                // complete message before calling flush(), so the buffer must
                // be cleared after every flush; otherwise a later request's
                // "id" lookup can match a stale, already-processed message
                // still sitting in the buffer.
                try {
                    String request = toString(StandardCharsets.UTF_8);
                    if (!request.contains("\"method\":\"session.detach\"")) {
                        return;
                    }

                    int idIndex = request.indexOf("\"id\":");
                    int idEnd = request.indexOf(",", idIndex);
                    String id = request.substring(idIndex + "\"id\":".length(), idEnd);
                    String response = "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":{\"success\":true}}";
                    byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                    responseWriter.write(
                            ("Content-Length: " + responseBytes.length + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                    responseWriter.write(responseBytes);
                    responseWriter.flush();
                } finally {
                    reset();
                }
            }
        };

        var ctor = JsonRpcClient.class.getDeclaredConstructor(InputStream.class, java.io.OutputStream.class,
                Socket.class, Process.class);
        ctor.setAccessible(true);
        return (JsonRpcClient) ctor.newInstance(input, sinkOutput, null, null);
    }

    /**
     * After {@code close()}, the future returned by {@code sendAndWait} must NOT be
     * completed by a stale timeout.
     * <p>
     * Closing fails the pending response with {@code IllegalStateException}.
     */
    @Test
    void testTimeoutDoesNotFireAfterSessionClose() throws Exception {
        JsonRpcClient rpc = createHangingRpcClient();
        try {
            try (CopilotSession session = new CopilotSession("test-timeout-id", rpc)) {

                CompletableFuture<AssistantMessageEvent> result = session
                        // Keep a real scheduled timeout pending until the owner closes the session.
                        .sendAndWait(new MessageOptions().setPrompt("hello"), Long.MAX_VALUE);

                assertFalse(result.isDone(), "Future should be pending before timeout fires");

                session.close();

                ExecutionException error = assertThrows(ExecutionException.class,
                        () -> result.get(5, TimeUnit.SECONDS));
                IllegalStateException failure = assertInstanceOf(IllegalStateException.class, error.getCause());
                assertEquals("Session closed before response completed", failure.getMessage());
            }
        } finally {
            rpc.close();
        }
    }

    /**
     * A shared scheduler must reuse a single thread across multiple
     * {@code sendAndWait} calls, rather than spawning a new OS thread per call.
     * <p>
     * The owner thread group isolates this session's scheduler from concurrent
     * tests.
     */
    @Test
    void testSendAndWaitReusesTimeoutThread() throws Exception {
        var group = new ThreadGroup("owned-sendAndWait-scheduler");
        var executor = Executors.newSingleThreadExecutor(runnable -> new Thread(group, runnable));
        try {
            executor.submit(() -> {
                try (var rpc = createHangingRpcClient();
                        var session = new CopilotSession("test-thread-count-id", rpc)) {
                    var result1 = session.sendAndWait(new MessageOptions().setPrompt("hello1"), Long.MAX_VALUE);
                    assertEquals(1, countTimeoutThreads(group), "First call must start this session's scheduler");
                    var result2 = session.sendAndWait(new MessageOptions().setPrompt("hello2"), Long.MAX_VALUE);
                    assertEquals(1, countTimeoutThreads(group), "Second call must reuse the owned scheduler thread");
                    result1.cancel(true);
                    result2.cancel(true);
                }
                return null;
            }).get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Counts scheduler threads owned by this test, after synchronous worker
     * creation.
     */
    private long countTimeoutThreads(ThreadGroup group) {
        Thread[] threads = new Thread[8];
        int count;
        while ((count = group.enumerate(threads, false)) == threads.length) {
            threads = new Thread[threads.length * 2];
        }
        long schedulers = 0;
        for (int i = 0; i < count; i++) {
            if (threads[i].getName().equals("sendAndWait-timeout") && threads[i].isAlive()) {
                schedulers++;
            }
        }
        return schedulers;
    }
}
