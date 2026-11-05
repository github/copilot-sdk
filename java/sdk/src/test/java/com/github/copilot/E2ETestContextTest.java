/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.RuntimeConnection;

class E2ETestContextTest {

    @Test
    void pathlessChildConnectionsUseThePreparedExecutable() throws Exception {
        try (E2ETestContext ctx = E2ETestContext.create()) {
            var stdio = RuntimeConnection.forStdio();
            var tcp = RuntimeConnection.forTcp();
            for (RuntimeConnection connection : List.of(stdio, tcp)) {
                try (var client = ctx.createClient(new CopilotClientOptions().setConnection(connection))) {
                    assertEquals(ctx.getCliPath(), connection == stdio ? stdio.getPath() : tcp.getPath());
                }
            }
        }
    }

    @Test
    void replyWaitRetainsTheResultPublishedAtItsTimeoutBoundary() throws Exception {
        for (var result : new String[]{"ready", null}) {
            var reply = new TimeoutBoundaryReply<String>(future -> future.complete(result));
            assertSame(result, E2ETestContext.awaitReply(reply, 0, TimeUnit.NANOSECONDS));
            assertEquals(0, reply.timeout);
            assertSame(TimeUnit.NANOSECONDS, reply.unit);
            assertEquals(1, reply.waits);
            assertEquals(1, reply.snapshots);
        }
    }

    @Test
    void replyWaitStillFailsWithTheSameTimeoutWhileItsReplyIsPending() {
        var reply = new TimeoutBoundaryReply<String>(future -> {
        });
        var failure = assertThrows(TimeoutException.class,
                () -> E2ETestContext.awaitReply(reply, 0, TimeUnit.NANOSECONDS));
        assertSame(reply.expired, failure);
        assertEquals(1, reply.waits);
        assertEquals(0, reply.snapshots);
        assertFalse(reply.isDone());
    }

    @Test
    void replyWaitPreservesTheFailurePublishedAtItsTimeoutBoundary() {
        for (var fault : new Throwable[]{new IOException("the issued RPC failed"), new CompletionException(null)}) {
            var reply = new TimeoutBoundaryReply<String>(future -> future.completeExceptionally(fault));
            var failure = assertThrows(ExecutionException.class,
                    () -> E2ETestContext.awaitReply(reply, 0, TimeUnit.NANOSECONDS));
            assertSame(fault, failure.getCause());
            assertEquals(1, failure.getSuppressed().length);
            assertSame(reply.expired, failure.getSuppressed()[0]);
            assertEquals(1, reply.snapshots);
        }
    }

    @Test
    void replyWaitPreservesCancellationAtItsTimeoutBoundary() {
        var reply = new TimeoutBoundaryReply<String>(future -> future.cancel(true));
        var failure = assertThrows(CancellationException.class,
                () -> E2ETestContext.awaitReply(reply, 0, TimeUnit.NANOSECONDS));
        assertEquals(1, failure.getSuppressed().length);
        assertSame(reply.expired, failure.getSuppressed()[0]);
        assertEquals(1, reply.snapshots);
        assertTrue(reply.isCancelled());
    }

    @Test
    void replyWaitDoesNotInspectASnapshotAfterOrdinarySuccess() throws Exception {
        var reply = new CompletableFuture<String>() {
            @Override
            public String getNow(String absent) {
                throw new AssertionError("ordinary success must not use the timeout fallback");
            }
        };
        reply.complete("ready");
        assertEquals("ready", E2ETestContext.awaitReply(reply, 0, TimeUnit.NANOSECONDS));
    }

    @Test
    void replyWaitDoesNotRecoverAfterItsOwnerIsInterrupted() {
        var interrupted = new InterruptedException("the owner stopped");
        var reply = new CompletableFuture<String>() {
            @Override
            public String get(long timeout, TimeUnit unit) throws InterruptedException {
                throw interrupted;
            }

            @Override
            public String getNow(String absent) {
                throw new AssertionError("owner interruption must not inspect a reply");
            }
        };
        reply.complete("ready");
        assertSame(interrupted, assertThrows(InterruptedException.class,
                () -> E2ETestContext.awaitReply(reply, 0, TimeUnit.NANOSECONDS)));
    }

    private static final class TimeoutBoundaryReply<T> extends CompletableFuture<T> {
        private final java.util.function.Consumer<CompletableFuture<T>> atTimeout;
        private TimeoutException expired;
        private long timeout;
        private TimeUnit unit;
        private int waits;
        private int snapshots;

        private TimeoutBoundaryReply(java.util.function.Consumer<CompletableFuture<T>> atTimeout) {
            this.atTimeout = atTimeout;
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            this.timeout = timeout;
            this.unit = unit;
            waits++;
            try {
                return super.get(timeout, unit);
            } catch (TimeoutException failure) {
                expired = failure;
                atTimeout.accept(this);
                throw failure;
            }
        }

        @Override
        public T get() {
            throw new AssertionError("timeout recovery must not wait again");
        }

        @Override
        public T getNow(T absent) {
            snapshots++;
            return super.getNow(absent);
        }
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void readinessUsesTheCallingThreadAndRequiresTheExactResult() throws Exception {
        var owner = Thread.currentThread();
        var calls = new AtomicInteger();
        var predicates = new AtomicInteger();
        var snapshot = E2ETestContext.awaitReadiness(() -> {
            assertSame(owner, Thread.currentThread());
            return CompletableFuture.completedFuture(switch (calls.incrementAndGet()) {
                case 1 -> null;
                case 2 -> "another-session";
                case 3 -> "ready";
                default -> throw new AssertionError("readiness must stop at the matching reply");
            });
        }, value -> {
            assertSame(owner, Thread.currentThread());
            predicates.incrementAndGet();
            return "ready".equals(value);
        }, 0, "the exact reply was not observed");
        assertEquals("ready", snapshot);
        assertEquals(3, calls.get());
        assertEquals(3, predicates.get());
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void readinessConsumesAnAlreadyIssuedReplyBeforeDispatching() throws Exception {
        var first = CompletableFuture.completedFuture("ready");
        var snapshot = E2ETestContext.awaitReadiness(() -> {
            throw new AssertionError("the issued query must be consumed first");
        }, first, "ready"::equals, 0, "readiness was not observed");
        assertEquals("ready", snapshot);
        assertFalse(first.isCancelled());
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void readinessContinuesAfterAnAlreadyIssuedNonmatchingReply() throws Exception {
        var calls = new AtomicInteger();
        var first = CompletableFuture.completedFuture("pending");
        var snapshot = E2ETestContext.awaitReadiness(() -> {
            assertTrue(first.isDone(), "queries must not overlap");
            calls.incrementAndGet();
            return CompletableFuture.completedFuture("ready");
        }, first, "ready"::equals, 0, "readiness was not observed");
        assertEquals("ready", snapshot);
        assertEquals(1, calls.get());
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void readinessPreservesExceptionalRepliesWithoutRetrying() {
        for (var fault : new Throwable[]{new IOException("the getter failed"), new CompletionException(null)}) {
            var calls = new AtomicInteger();
            var predicates = new AtomicInteger();
            var failure = assertThrows(ExecutionException.class, () -> E2ETestContext.awaitReadiness(() -> {
                calls.incrementAndGet();
                return CompletableFuture.failedFuture(fault);
            }, value -> {
                predicates.incrementAndGet();
                return true;
            }, 0, "readiness was not observed"));
            assertSame(fault, failure.getCause());
            assertEquals(1, calls.get());
            assertEquals(0, predicates.get());
        }
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void readinessPreservesCancellationWithoutRetrying() {
        var query = new CompletableFuture<String>();
        query.cancel(true);
        var calls = new AtomicInteger();
        var predicates = new AtomicInteger();
        assertThrows(CancellationException.class, () -> E2ETestContext.awaitReadiness(() -> {
            calls.incrementAndGet();
            return query;
        }, value -> {
            predicates.incrementAndGet();
            return true;
        }, 0, "readiness was not observed"));
        assertEquals(1, calls.get());
        assertEquals(0, predicates.get());
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void readinessPreservesPredicateFailuresWithoutRetrying() {
        for (var fault : new Throwable[]{new IllegalStateException("the predicate failed"),
                new AssertionError("bad")}) {
            var calls = new AtomicInteger();
            var failure = assertThrows(fault.getClass(), () -> E2ETestContext.awaitReadiness(() -> {
                calls.incrementAndGet();
                return CompletableFuture.completedFuture("ready");
            }, value -> {
                if (fault instanceof RuntimeException exception) {
                    throw exception;
                }
                throw (AssertionError) fault;
            }, 0, "readiness was not observed"));
            assertSame(fault, failure);
            assertEquals(1, calls.get());
        }
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void readinessPreservesDispatchFailuresWithoutRetrying() {
        var fault = new IllegalStateException("the getter could not be dispatched");
        var calls = new AtomicInteger();
        var failure = assertThrows(IllegalStateException.class, () -> E2ETestContext.awaitReadiness(() -> {
            calls.incrementAndGet();
            throw fault;
        }, value -> true, 0, "readiness was not observed"));
        assertSame(fault, failure);
        assertEquals(1, calls.get());
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void readinessCancelsItsOutstandingQueryAndReportsTheLastNonmatchingReply() {
        for (var last : new String[]{null, "another-session"}) {
            var calls = new AtomicInteger();
            var outstanding = new CompletableFuture<String>() {
                @Override
                public String get() throws InterruptedException, ExecutionException {
                    Thread.currentThread().interrupt();
                    return super.get();
                }
            };
            try {
                var failure = assertThrows(InterruptedException.class, () -> E2ETestContext.awaitReadiness(() -> {
                    if (calls.incrementAndGet() == 1) {
                        return CompletableFuture.completedFuture(last);
                    }
                    return outstanding;
                }, "ready"::equals, 0, "expected the matching session"));
                assertTrue(outstanding.isCancelled());
                assertEquals(2, calls.get());
                assertFalse(Thread.currentThread().isInterrupted(), "cleanup must be allowed to block");
                assertEquals(1, failure.getSuppressed().length);
                assertEquals("expected the matching session; last observation=" + last,
                        failure.getSuppressed()[0].getMessage());
            } finally {
                Thread.interrupted();
                outstanding.cancel(true);
            }
        }
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void readinessDoesNotDispatchWhenAlreadyInterrupted() {
        var first = new CompletableFuture<String>();
        try {
            Thread.currentThread().interrupt();
            var failure = assertThrows(InterruptedException.class, () -> E2ETestContext.awaitReadiness(() -> {
                throw new AssertionError("interruption must prevent new work");
            }, first, "ready"::equals, 0, "readiness interrupted"));
            assertTrue(first.isCancelled());
            assertFalse(Thread.currentThread().isInterrupted());
            assertEquals("readiness interrupted; last observation=<no completed reply>",
                    failure.getSuppressed()[0].getMessage());
        } finally {
            Thread.interrupted();
            first.cancel(true);
        }
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void conditionReadinessRetainsSuccessObservedDuringCancellation() throws Exception {
        var entered = new CountDownLatch(1);
        var held = new CountDownLatch(1);
        var interrupted = new AtomicBoolean();
        var executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()) {
            @Override
            public void execute(Runnable command) {
                super.execute(command);
                try {
                    entered.await();
                } catch (InterruptedException interruption) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("readiness probe admission was interrupted", interruption);
                }
            }
        };
        try {
            E2ETestContext.awaitCondition(() -> {
                entered.countDown();
                try {
                    held.await();
                    return false;
                } catch (InterruptedException cancellation) {
                    interrupted.set(true);
                    return true;
                }
            }, 0, 50, "the successful probe was lost", executor);
            assertTrue(interrupted.get());
            assertTrue(executor.isTerminated());
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void conditionReadinessDoesNotStartAnotherProbeAfterStoppingItsObserver() throws Exception {
        var entered = new CountDownLatch(1);
        var held = new CountDownLatch(1);
        var ready = new AtomicBoolean();
        var calls = new AtomicInteger();
        var executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()) {
            @Override
            public void execute(Runnable command) {
                super.execute(command);
                try {
                    entered.await();
                } catch (InterruptedException interruption) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("readiness probe admission was interrupted", interruption);
                }
            }

            @Override
            protected void afterExecute(Runnable command, Throwable failure) {
                ready.set(true);
                super.afterExecute(command, failure);
            }
        };
        try {
            var failure = assertThrows(AssertionError.class, () -> E2ETestContext.awaitCondition(() -> {
                calls.incrementAndGet();
                if (ready.get()) {
                    return true;
                }
                entered.countDown();
                try {
                    held.await();
                    return false;
                } catch (InterruptedException cancellation) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }, 0, 50, "readiness was not observed", executor));
            assertTrue(failure.getCause() instanceof TimeoutException);
            assertEquals(1, calls.get());
            assertTrue(ready.get());
            assertTrue(executor.isTerminated());
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void bufferedReadinessRetainsTheCanceledWorkersConsumedSuccess() throws Exception {
        var owner = Thread.currentThread();
        var entered = new CountDownLatch(1);
        var held = new CountDownLatch(1);
        var ready = new AtomicBoolean(true);
        var interrupted = new AtomicBoolean();
        var calls = new AtomicInteger();
        var worker = new AtomicReference<Thread>();
        var executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()) {
            @Override
            public void execute(Runnable command) {
                super.execute(command);
                try {
                    entered.await();
                } catch (InterruptedException interruption) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("readiness probe admission was interrupted", interruption);
                }
            }
        };
        E2ETestContext.awaitBufferedCondition(() -> {
            if (calls.incrementAndGet() != 1) {
                return ready.get();
            }
            worker.set(Thread.currentThread());
            entered.countDown();
            try {
                held.await();
                return false;
            } catch (InterruptedException cancellation) {
                interrupted.set(true);
                return ready.getAndSet(false);
            }
        }, 0, 50, "consumed readiness was lost", executor);
        assertEquals(1, calls.get());
        assertFalse(worker.get() == owner, "the admitted worker must consume readiness, not the final caller probe");
        assertTrue(interrupted.get());
        assertFalse(ready.get());
        assertTrue(executor.isTerminated());
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void bufferedReadinessRetainsAnEventPublishedWhileStoppingItsObserver() throws Exception {
        var entered = new CountDownLatch(1);
        var ready = new AtomicBoolean();
        var executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()) {
            @Override
            public void execute(Runnable command) {
                super.execute(command);
                try {
                    entered.await();
                } catch (InterruptedException interruption) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("readiness probe admission was interrupted", interruption);
                }
            }

            @Override
            protected void afterExecute(Runnable command, Throwable failure) {
                ready.set(true);
                super.afterExecute(command, failure);
            }
        };
        try {
            E2ETestContext.awaitBufferedCondition(() -> {
                entered.countDown();
                return ready.get();
            }, 0, 50, "the final buffered event was lost", executor);
            assertTrue(ready.get());
            assertTrue(executor.isTerminated());
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
    }

    @Test
    void bufferedReadinessChecksTheFinalStateOnlyAfterStoppingItsWorker() throws Exception {
        var owner = Thread.currentThread();
        var workerActive = new AtomicBoolean();
        var finalProbe = new AtomicBoolean();
        var held = new CountDownLatch(1);
        E2ETestContext.awaitBufferedCondition(() -> {
            if (Thread.currentThread() == owner) {
                assertFalse(workerActive.get(), "final probe must not overlap the canceled worker");
                finalProbe.set(true);
                return true;
            }
            workerActive.set(true);
            try {
                held.await();
                return false;
            } finally {
                workerActive.set(false);
            }
        }, 0, 50, "buffered readiness was not observed");
        assertTrue(finalProbe.get());
        assertFalse(workerActive.get());
    }

    @Test
    void bufferedReadinessStillFailsWhenTheFinalStateIsPending() {
        var failure = assertThrows(AssertionError.class,
                () -> E2ETestContext.awaitBufferedCondition(() -> false, 0, 50, "still pending"));
        assertEquals("still pending", failure.getMessage());
        assertTrue(failure.getCause() instanceof TimeoutException);
    }

    @Test
    void bufferedReadinessDoesNotHideAFailedFinalProbe() {
        var owner = Thread.currentThread();
        var held = new CountDownLatch(1);
        var failure = assertThrows(IOException.class, () -> E2ETestContext.awaitBufferedCondition(() -> {
            if (Thread.currentThread() == owner) {
                throw new IOException("buffered probe failed");
            }
            held.await();
            return false;
        }, 0, 50, "readiness timed out"));
        assertEquals("buffered probe failed", failure.getMessage());
    }

    @Test
    void defaultTelemetryEndpointUsesTheOwnedProxy() throws Exception {
        try (E2ETestContext ctx = E2ETestContext.create()) {
            var client = HttpClient.newHttpClient();
            var userRequest = HttpRequest.newBuilder(URI.create(ctx.getProxyUrl() + "/copilot_internal/user"))
                    .header("Authorization", "Bearer " + ctx.getEnvironment().get("GH_TOKEN")).build();
            var userResponse = client.send(userRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, userResponse.statusCode());
            var telemetryEndpoint = new ObjectMapper().readTree(userResponse.body()).path("endpoints").path("telemetry")
                    .asText();
            assertEquals(ctx.getProxyUrl(), telemetryEndpoint);
            var telemetryRequest = HttpRequest.newBuilder(URI.create(telemetryEndpoint + "/telemetry"))
                    .POST(HttpRequest.BodyPublishers.ofString("{}\n")).build();
            var telemetryResponse = client.send(telemetryRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(204, telemetryResponse.statusCode());
            assertEquals("", telemetryResponse.body());
        }
    }

    @Test
    void expectedUserPromptsParseFoldedBlockScalars() {
        String snapshot = """
                conversations:
                  - messages:
                      - role: user
                        content: First prompt
                          continued here.
                      - role: assistant
                        content: response
                      - role: user
                        content: >-
                          <system_notification>

                          Agent completed successfully.

                          </system_notification>
                """;

        assertEquals(
                List.of("First prompt continued here.",
                        "<system_notification>\nAgent completed successfully.\n</system_notification>"),
                E2ETestContext.parseExpectedUserPrompts(snapshot));
    }
}
