/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static com.github.copilot.CopilotRequestTestSupport.buildNonInferenceResponse;
import static com.github.copilot.CopilotRequestTestSupport.isInferenceUrl;
import static com.github.copilot.CopilotRequestTestSupport.newLlmClient;
import static com.github.copilot.CopilotRequestTestSupport.setupCapiAuth;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

/**
 * Cancellation and error coverage for {@link CopilotRequestHandler}. These two
 * scenarios exercise the handler's terminal paths the happy-path session-id and
 * forwarding tests never reach:
 * <ul>
 * <li><b>Error</b> — the handler throws from
 * {@link CopilotRequestHandler#sendHttp} for an inference request. The base
 * adapter reports a transport error back to the runtime rather than
 * hanging.</li>
 * <li><b>Runtime cancel</b> — the handler blocks an inference request
 * indefinitely; when the consumer aborts the turn the runtime cancels the
 * in-flight request, firing {@link CopilotRequestContext#cancellation()}. The
 * handler observes the abort instead of leaking a stuck request.</li>
 * </ul>
 */
public class CopilotRequestCancelErrorE2ETest {

    private static E2ETestContext ctx;

    @BeforeAll
    static void setup() throws Exception {
        ctx = E2ETestContext.create();
    }

    @AfterAll
    static void teardown() throws Exception {
        if (ctx != null) {
            ctx.close();
        }
    }

    /** Throws from every inference request to exercise the error-reporting path. */
    private static final class ThrowingRequestHandler extends CopilotRequestHandler {

        private final AtomicInteger inferenceAttempts = new AtomicInteger();

        @Override
        protected HttpResponse<InputStream> sendHttp(HttpRequest request, CopilotRequestContext rctx) {
            String url = request.uri().toString();
            if (!isInferenceUrl(url)) {
                return buildNonInferenceResponse(url);
            }
            inferenceAttempts.incrementAndGet();
            throw new IllegalStateException("synthetic-callback-transport-failure");
        }
    }

    /** Blocks every inference request until the runtime cancels it. */
    private static final class CancellingRequestHandler extends CopilotRequestHandler {

        private volatile boolean inferenceEntered;
        private volatile boolean sawAbort;

        @Override
        protected HttpResponse<InputStream> sendHttp(HttpRequest request, CopilotRequestContext rctx) {
            String url = request.uri().toString();
            if (!isInferenceUrl(url)) {
                return buildNonInferenceResponse(url);
            }
            inferenceEntered = true;
            try {
                // Never produce a response; wait for the runtime to cancel us.
                rctx.cancellation().join();
            } catch (CancellationException | java.util.concurrent.CompletionException e) {
                // The cancellation future completes normally on cancel; this guards
                // against any exceptional completion too.
            }
            sawAbort = true;
            throw new CancellationException("Request cancelled by runtime");
        }
    }

    @Test
    void reportsThrownHandlerErrorInsteadOfHanging() throws Exception {
        setupCapiAuth(ctx);
        ThrowingRequestHandler handler = new ThrowingRequestHandler();

        try (CopilotClient client = newLlmClient(ctx, handler)) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            // The handler throws on inference; the turn surfaces an error (or completes
            // without an assistant message) rather than hanging.
            try {
                session.sendAndWait(new MessageOptions().setPrompt("Say OK.")).get(60, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // Expected: the inference callback raised.
            }
            session.close();
        }

        assertTrue(handler.inferenceAttempts.get() > 0, "Expected the inference callback to be reached and raise");
    }

    @Test
    void observesRuntimeCancellationOfInFlightInference() throws Exception {
        setupCapiAuth(ctx);
        CancellingRequestHandler handler = new CancellingRequestHandler();

        try (CopilotClient client = newLlmClient(ctx, handler)) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            session.send(new MessageOptions().setPrompt("Say OK.")).get(60, TimeUnit.SECONDS);
            waitFor(() -> handler.inferenceEntered, 60_000);
            session.abort().get(30, TimeUnit.SECONDS);
            waitFor(() -> handler.sawAbort, 30_000);
            session.close();
        }

        assertTrue(handler.inferenceEntered, "Expected the inference callback to be entered");
        assertTrue(handler.sawAbort, "Expected the callback to observe runtime cancellation");
    }

    private static void waitFor(java.util.function.BooleanSupplier predicate, long timeoutMillis)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (!predicate.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("waitFor timed out");
            }
            Thread.sleep(50);
        }
    }
}
