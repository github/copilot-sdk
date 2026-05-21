/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.generated.AssistantMessageEvent;
import com.github.copilot.sdk.json.MessageOptions;

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
     * Creates a {@link JsonRpcClient} whose {@code invoke()} returns futures that
     * never complete. The reader thread blocks forever on the input stream, and
     * writes go to a no-op output stream.
     */
    private JsonRpcClient createHangingRpcClient() throws Exception {
        InputStream blockingInput = new InputStream() {
            @Override
            public int read() throws IOException {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return -1;
                }
                return -1;
            }
        };
        ByteArrayOutputStream sinkOutput = new ByteArrayOutputStream();

        var ctor = JsonRpcClient.class.getDeclaredConstructor(InputStream.class, java.io.OutputStream.class,
                Socket.class, Process.class);
        ctor.setAccessible(true);
        return (JsonRpcClient) ctor.newInstance(blockingInput, sinkOutput, null, null);
    }

    /**
     * After {@code close()}, the future returned by {@code sendAndWait} must NOT be
     * completed by a stale timeout.
     * <p>
     * Contract: {@code close()} shuts down the timeout scheduler before the
     * blocking {@code session.destroy} RPC call, so any pending timeout task is
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

                // close() blocks up to 5s on session.destroy RPC. The 2s timeout
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
