/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static com.github.copilot.LlmInferenceTestSupport.SYNTHETIC_TEXT;
import static com.github.copilot.LlmInferenceTestSupport.assistantText;
import static com.github.copilot.LlmInferenceTestSupport.handleInference;
import static com.github.copilot.LlmInferenceTestSupport.handleNonInferenceModelTraffic;
import static com.github.copilot.LlmInferenceTestSupport.isInferenceUrl;
import static com.github.copilot.LlmInferenceTestSupport.newLlmClient;
import static com.github.copilot.LlmInferenceTestSupport.setupCapiAuth;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ProviderConfig;
import com.github.copilot.rpc.SessionConfig;

/**
 * Verifies that the triggering session id is threaded into every inference
 * callback, for both CAPI and BYOK sessions, and that per-session ids differ.
 */
public class LlmInferenceSessionIdE2ETest {

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

    private record InterceptedRequest(String url, String sessionId) {
    }

    private static final class SessionIdHandler implements LlmInferenceProvider {

        private final List<InterceptedRequest> records = new ArrayList<>();

        @Override
        public void onLlmRequest(LlmInferenceRequest req) throws Exception {
            synchronized (records) {
                records.add(new InterceptedRequest(req.getUrl(), req.getSessionId()));
            }
            if (isInferenceUrl(req.getUrl())) {
                handleInference(req, SYNTHETIC_TEXT);
            } else {
                handleNonInferenceModelTraffic(req, null);
            }
        }

        List<InterceptedRequest> inferenceRecords() {
            synchronized (records) {
                List<InterceptedRequest> out = new ArrayList<>();
                for (InterceptedRequest r : records) {
                    if (isInferenceUrl(r.url())) {
                        out.add(r);
                    }
                }
                return out;
            }
        }
    }

    @Test
    void threadsSessionIdForCapiAndByok() throws Exception {
        setupCapiAuth(ctx);
        SessionIdHandler handler = new SessionIdHandler();

        try (CopilotClient client = newLlmClient(ctx, handler)) {
            // CAPI session.
            CopilotSession capiSession = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();
            String capiSessionId = capiSession.getSessionId();

            AssistantMessageEvent capiResult = capiSession.sendAndWait(new MessageOptions().setPrompt("Say OK."))
                    .get(60, TimeUnit.SECONDS);
            capiSession.close();

            List<InterceptedRequest> capiInference = handler.inferenceRecords();
            assertFalse(capiInference.isEmpty(), "Expected at least one intercepted inference request");
            for (InterceptedRequest r : capiInference) {
                assertEquals(capiSessionId, r.sessionId(), "CAPI inference request must carry the session id");
            }
            assertTrue(assistantText(capiResult).contains("OK from the synthetic"),
                    "Expected synthetic content in CAPI assistant reply, got " + assistantText(capiResult));

            // BYOK session.
            int before = handler.inferenceRecords().size();
            ProviderConfig provider = new ProviderConfig().setType("openai").setWireApi("responses")
                    .setBaseUrl("https://byok.invalid/v1").setApiKey("byok-secret").setModelId("claude-sonnet-4.5")
                    .setWireModel("claude-sonnet-4.5");
            CopilotSession byokSession = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setModel("claude-sonnet-4.5").setProvider(provider))
                    .get();
            String byokSessionId = byokSession.getSessionId();

            AssistantMessageEvent byokResult = byokSession.sendAndWait(new MessageOptions().setPrompt("Say OK."))
                    .get(60, TimeUnit.SECONDS);
            byokSession.close();

            List<InterceptedRequest> byokInference = handler.inferenceRecords();
            assertTrue(byokInference.size() > before, "Expected at least one intercepted BYOK inference request");
            for (InterceptedRequest r : byokInference.subList(before, byokInference.size())) {
                assertEquals(byokSessionId, r.sessionId(), "BYOK inference request must carry the session id");
            }
            assertNotEquals(capiSessionId, byokSessionId, "Expected per-session ids to differ between turns");
            assertTrue(assistantText(byokResult).contains("OK from the synthetic"),
                    "Expected synthetic content in BYOK assistant reply, got " + assistantText(byokResult));
        }
    }
}
