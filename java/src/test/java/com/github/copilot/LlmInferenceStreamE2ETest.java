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
import com.github.copilot.rpc.SessionConfig;

/**
 * Verifies that the callback can synthesize a streaming inference response that
 * the runtime reduces into the final assistant message.
 */
public class LlmInferenceStreamE2ETest {

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

    private static final class StreamingHandler implements LlmInferenceProvider {

        private final List<String> urls = new ArrayList<>();

        @Override
        public void onLlmRequest(LlmInferenceRequest req) throws Exception {
            synchronized (urls) {
                urls.add(req.getUrl());
            }
            if (isInferenceUrl(req.getUrl())) {
                handleInference(req, SYNTHETIC_TEXT);
            } else {
                handleNonInferenceModelTraffic(req, null);
            }
        }

        synchronized int inferenceCount() {
            synchronized (urls) {
                int n = 0;
                for (String url : urls) {
                    if (isInferenceUrl(url)) {
                        n++;
                    }
                }
                return n;
            }
        }
    }

    @Test
    void streamsSyntheticInference() throws Exception {
        setupCapiAuth(ctx);
        StreamingHandler handler = new StreamingHandler();

        try (CopilotClient client = newLlmClient(ctx, handler)) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            AssistantMessageEvent result = session.sendAndWait(new MessageOptions().setPrompt("Say OK.")).get(60,
                    TimeUnit.SECONDS);
            session.close();

            assertTrue(handler.inferenceCount() > 0, "Expected at least one inference request via the callback");

            // Validate the final assistant response arrived (guards against truncated
            // captures)
            assertTrue(assistantText(result).contains("OK from the synthetic"),
                    "Expected synthetic content in assistant reply, got " + assistantText(result));
        }
    }
}
