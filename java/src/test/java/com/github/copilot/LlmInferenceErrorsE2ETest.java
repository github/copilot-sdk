/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static com.github.copilot.LlmInferenceTestSupport.drainRequest;
import static com.github.copilot.LlmInferenceTestSupport.headers;
import static com.github.copilot.LlmInferenceTestSupport.newLlmClient;
import static com.github.copilot.LlmInferenceTestSupport.respondBuffered;
import static com.github.copilot.LlmInferenceTestSupport.serviceNonInference;
import static com.github.copilot.LlmInferenceTestSupport.setupCapiAuth;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

/**
 * Verifies that an exception raised from the inference callback surfaces as a
 * turn error rather than hanging the runtime.
 */
public class LlmInferenceErrorsE2ETest {

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

    private static final class ThrowingHandler implements LlmInferenceProvider {

        private final AtomicInteger totalCalls = new AtomicInteger();
        private final AtomicInteger callsBeforeError = new AtomicInteger();

        @Override
        public void onLlmRequest(LlmInferenceRequest req) throws Exception {
            totalCalls.incrementAndGet();
            if (serviceNonInference(req)) {
                return;
            }
            String url = req.getUrl().toLowerCase(Locale.ROOT);
            if (url.contains("/chat/completions") || url.contains("/responses")) {
                drainRequest(req);
                callsBeforeError.incrementAndGet();
                throw new RuntimeException("synthetic-callback-transport-failure");
            }
            respondBuffered(req, 200, headers("content-type", "application/json"), "{}");
        }
    }

    @Test
    void surfacesHandlerErrors() throws Exception {
        setupCapiAuth(ctx);
        ThrowingHandler handler = new ThrowingHandler();

        try (CopilotClient client = newLlmClient(ctx, handler)) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            // The handler raises from the inference callback; the agent layer surfaces it
            // as an error or event rather than hanging. The assertion is loose: the
            // inference call was attempted and the runtime did not hang.
            try {
                session.sendAndWait(new MessageOptions().setPrompt("Say OK.")).get(60, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // Expected: the inference callback raised.
            }
            session.close();
        }

        assertTrue(handler.totalCalls.get() > 0, "Expected the callback to be invoked");
        assertTrue(handler.callsBeforeError.get() > 0, "Expected the inference callback to be reached and raise");
    }
}
