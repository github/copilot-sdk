/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.json.MessageOptions;

/**
 * Regression coverage for the race between {@code sendAndWait()} and
 * {@code close()}.
 * <p>
 * If {@code close()} shuts down the timeout scheduler after
 * {@code ensureNotTerminated()} passes but before
 * {@code timeoutScheduler.schedule()} executes, the schedule call throws
 * {@link RejectedExecutionException}. This test asserts that
 * {@code sendAndWait()} handles this race by returning a future that completes
 * exceptionally (rather than propagating the exception to the caller or leaving
 * the returned future incomplete).
 */
public class SchedulerShutdownRaceTest {

    @SuppressWarnings("unchecked")
    @Test
    void sendAndWaitShouldReturnFailedFutureWhenSchedulerIsShutDown() throws Exception {
        // Build a session via reflection (package-private constructor)
        var ctor = CopilotSession.class.getDeclaredConstructor(String.class, JsonRpcClient.class, String.class);
        ctor.setAccessible(true);

        // Mock JsonRpcClient so send() returns a pending future instead of NPE
        var mockRpc = mock(JsonRpcClient.class);
        when(mockRpc.invoke(any(), any(), any())).thenReturn(new CompletableFuture<>());

        var session = ctor.newInstance("race-test", mockRpc, null);

        // Shut down the scheduler without setting isTerminated,
        // simulating the race window between ensureNotTerminated() and schedule()
        var schedulerField = CopilotSession.class.getDeclaredField("timeoutScheduler");
        schedulerField.setAccessible(true);
        var scheduler = (ScheduledExecutorService) schedulerField.get(session);
        scheduler.shutdownNow();

        // sendAndWait must return a failed future rather than throwing directly.
        CompletableFuture<?> result = session.sendAndWait(new MessageOptions().setPrompt("test"), 5000);

        assertNotNull(result, "sendAndWait should return a future, not throw");
        var ex = assertThrows(ExecutionException.class, () -> result.get(1, TimeUnit.SECONDS));
        assertInstanceOf(RejectedExecutionException.class, ex.getCause());
    }
}
