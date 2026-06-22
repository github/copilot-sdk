/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static com.github.copilot.LlmInferenceTestSupport.drainRequest;
import static com.github.copilot.LlmInferenceTestSupport.headers;
import static com.github.copilot.LlmInferenceTestSupport.isInferenceUrl;
import static com.github.copilot.LlmInferenceTestSupport.newLlmClient;
import static com.github.copilot.LlmInferenceTestSupport.respondBuffered;
import static com.github.copilot.LlmInferenceTestSupport.serviceNonInference;
import static com.github.copilot.LlmInferenceTestSupport.setupCapiAuth;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

/**
 * Verifies that the consumer observes a runtime-driven cancellation of an
 * in-flight inference request (the agent turn was aborted upstream).
 */
public class LlmInferenceCancelE2ETest {

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

    private static final class CancellingHandler implements LlmInferenceProvider {

        private final AtomicBoolean inferenceEntered = new AtomicBoolean();
        private final AtomicBoolean sawAbort = new AtomicBoolean();
        private final CountDownLatch abortSeen = new CountDownLatch(1);

        @Override
        public void onLlmRequest(LlmInferenceRequest req) throws Exception {
            if (serviceNonInference(req)) {
                return;
            }
            if (!isInferenceUrl(req.getUrl())) {
                respondBuffered(req, 200, headers("content-type", "application/json"), "{}");
                return;
            }

            // Inference: never produce a response. Wait for the runtime to cancel us,
            // recording the abort.
            drainRequest(req);
            inferenceEntered.set(true);
            req.getCancellation().join();
            sawAbort.set(true);
            abortSeen.countDown();
            // Runtime already dropped the request on cancel; the sink error is a no-op.
            try {
                req.getResponseBody().error("cancelled by upstream", "cancelled");
            } catch (Exception ignored) {
                // Best effort.
            }
        }
    }

    private static void waitFor(AtomicBoolean predicate, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (!predicate.get()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("waitFor timed out");
            }
            Thread.sleep(50);
        }
    }

    @Test
    void observesRuntimeDrivenCancel() throws Exception {
        setupCapiAuth(ctx);
        CancellingHandler handler = new CancellingHandler();

        try (CopilotClient client = newLlmClient(ctx, handler)) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            session.send(new MessageOptions().setPrompt("Say OK.")).get(60, TimeUnit.SECONDS);
            waitFor(handler.inferenceEntered, 60_000);
            session.abort().get(30, TimeUnit.SECONDS);

            assertTrue(handler.abortSeen.await(30, TimeUnit.SECONDS),
                    "Timed out waiting for the consumer to observe runtime cancellation");
            session.close();
        }

        assertTrue(handler.inferenceEntered.get(), "Expected the inference callback to be entered");
        assertTrue(handler.sawAbort.get(), "Expected the consumer to observe the runtime-driven cancellation");
    }
}
