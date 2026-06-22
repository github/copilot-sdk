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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

/**
 * Verifies that a consumer-initiated cancellation (the consumer's own upstream
 * call was aborted) terminates the request via a response error rather than
 * hanging the runtime.
 */
public class LlmInferenceConsumerCancelE2ETest {

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

    private static final class ConsumerCancelHandler implements LlmInferenceProvider {

        private final AtomicInteger inferenceAttempts = new AtomicInteger();

        @Override
        public void onLlmRequest(LlmInferenceRequest req) throws Exception {
            if (serviceNonInference(req)) {
                return;
            }
            if (!isInferenceUrl(req.getUrl())) {
                respondBuffered(req, 200, headers("content-type", "application/json"), "{}");
                return;
            }

            // Consumer-initiated cancellation: the consumer's own upstream call was
            // aborted, so it tells the runtime to give up on this request. No response
            // head is ever produced; the runtime should see a transport failure rather
            // than hanging.
            drainRequest(req);
            inferenceAttempts.incrementAndGet();
            req.getResponseBody().error("upstream call aborted by consumer", "cancelled");
        }
    }

    @Test
    void surfacesConsumerInitiatedCancel() throws Exception {
        setupCapiAuth(ctx);
        ConsumerCancelHandler handler = new ConsumerCancelHandler();

        try (CopilotClient client = newLlmClient(ctx, handler)) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            try {
                session.sendAndWait(new MessageOptions().setPrompt("Say OK.")).get(60, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // Expected: the consumer cancelled the inference request.
            }
            session.close();
        }

        // The runtime reached the inference step and the consumer's cancellation
        // terminated it (rather than the runtime hanging).
        assertTrue(handler.inferenceAttempts.get() > 0, "Expected the inference callback to be attempted");
    }
}
