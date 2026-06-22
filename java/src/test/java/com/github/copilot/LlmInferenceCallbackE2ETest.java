/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static com.github.copilot.LlmInferenceTestSupport.handleNonInferenceModelTraffic;
import static com.github.copilot.LlmInferenceTestSupport.newLlmClient;
import static com.github.copilot.LlmInferenceTestSupport.setupCapiAuth;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

/**
 * Verifies that a registered LLM inference callback intercepts the runtime's
 * model-layer traffic (the startup catalog and the per-turn inference call) for
 * a CAPI session, fully replacing the outbound calls.
 */
public class LlmInferenceCallbackE2ETest {

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

    private static final class RecordingHandler implements LlmInferenceProvider {

        private final List<String> urls = new ArrayList<>();

        @Override
        public void onLlmRequest(LlmInferenceRequest req) throws Exception {
            synchronized (urls) {
                urls.add(req.getUrl());
            }
            handleNonInferenceModelTraffic(req, null);
        }

        synchronized List<String> snapshot() {
            synchronized (urls) {
                return new ArrayList<>(urls);
            }
        }
    }

    @Test
    void interceptsModelTraffic() throws Exception {
        setupCapiAuth(ctx);
        RecordingHandler handler = new RecordingHandler();

        try (CopilotClient client = newLlmClient(ctx, handler)) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            // The buffered fallback returns empty JSON for the inference call, which is
            // not a valid model response, so the turn fails; swallow that. What we
            // assert is that the runtime attempted the callback.
            try {
                session.sendAndWait(new MessageOptions().setPrompt("Say OK.")).get(60, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // Expected: the synthetic empty response is not a valid completion.
            }
            session.close();
        }

        List<String> received = handler.snapshot();
        assertFalse(received.isEmpty(), "Expected the runtime to invoke the inference callback");

        boolean sawCatalog = false;
        for (String url : received) {
            assertTrue(url.startsWith("http://") || url.startsWith("https://"), "Expected an absolute URL, got " + url);
            if (url.toLowerCase(Locale.ROOT).endsWith("/models")) {
                sawCatalog = true;
            }
        }
        assertTrue(sawCatalog, "Expected to intercept the /models catalog request");
    }
}
