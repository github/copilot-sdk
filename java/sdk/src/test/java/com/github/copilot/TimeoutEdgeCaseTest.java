/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

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
     * Contract: {@code close()} shuts down the timeout scheduler before the
     * blocking {@code session.detach} RPC call, so any pending timeout task is
     * cancelled and the future remains incomplete (not exceptionally completed with
     * {@code TimeoutException}).
     */
    @Test
    void testTimeoutDoesNotFireAfterSessionClose() throws Exception {
        JsonRpcClient rpc = createHangingRpcClient();
        try {
            try (CopilotSession session = new CopilotSession("test-timeout-id", rpc)) {

                CompletableFuture<AssistantMessageEvent> result = session
                        .sendAndWait(new MessageOptions().setPrompt("hello"), 2000);

                assertFalse(result.isDone(), "Future should be pending before timeout fires");

                // close() blocks up to 5s on session.detach RPC. The 2s timeout
                // fires during that window with the current per-call scheduler.
                session.close();

                assertFalse(result.isDone(), "Future should not be completed by a timeout after session is closed. "
                        + "The per-call ScheduledExecutorService leaked a TimeoutException.");
            }
        } finally {
            rpc.close();
        }
    }

    /**
     * A shared scheduler must reuse a single thread across multiple
     * {@code sendAndWait} calls, rather than spawning a new OS thread per call.
     * <p>
     * Contract: after two consecutive {@code sendAndWait} calls the number of live
     * {@code sendAndWait-timeout} threads must not increase after the second call.
     */
    @Test
    void testSendAndWaitReusesTimeoutThread() throws Exception {
        JsonRpcClient rpc = createHangingRpcClient();
        try {
            try (CopilotSession session = new CopilotSession("test-thread-count-id", rpc)) {

                long baselineCount = countTimeoutThreads();

                CompletableFuture<AssistantMessageEvent> result1 = session
                        .sendAndWait(new MessageOptions().setPrompt("hello1"), 30000);

                Thread.sleep(100);
                long afterFirst = countTimeoutThreads();
                assertTrue(afterFirst >= baselineCount + 1,
                        "Expected at least one new sendAndWait-timeout thread after first call. " + "Baseline: "
                                + baselineCount + ", after: " + afterFirst);

                CompletableFuture<AssistantMessageEvent> result2 = session
                        .sendAndWait(new MessageOptions().setPrompt("hello2"), 30000);

                Thread.sleep(100);
                long afterSecond = countTimeoutThreads();
                assertTrue(afterSecond == afterFirst,
                        "Shared scheduler should reuse the same thread — no new threads after second call. "
                                + "After first: " + afterFirst + ", after second: " + afterSecond);

                result1.cancel(true);
                result2.cancel(true);
            }
        } finally {
            rpc.close();
        }
    }

    /**
     * Counts the number of live threads whose name contains "sendAndWait-timeout".
     */
    private long countTimeoutThreads() {
        return Thread.getAllStackTraces().keySet().stream().filter(t -> t.getName().contains("sendAndWait-timeout"))
                .filter(Thread::isAlive).count();
    }
}
