/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static com.github.copilot.LlmInferenceTestSupport.assistantText;
import static com.github.copilot.LlmInferenceTestSupport.emptyHeaders;
import static com.github.copilot.LlmInferenceTestSupport.handleNonInferenceModelTraffic;
import static com.github.copilot.LlmInferenceTestSupport.headers;
import static com.github.copilot.LlmInferenceTestSupport.isInferenceUrl;
import static com.github.copilot.LlmInferenceTestSupport.json;
import static com.github.copilot.LlmInferenceTestSupport.newLlmClient;
import static com.github.copilot.LlmInferenceTestSupport.responsesEvents;
import static com.github.copilot.LlmInferenceTestSupport.setupCapiAuth;
import static com.github.copilot.LlmInferenceTestSupport.sse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

/**
 * Verifies that the runtime can drive the WebSocket {@code /responses}
 * transport through the callback, with one inbound request-body frame per WS
 * message.
 */
public class LlmInferenceWebSocketE2ETest {

    private static final String WS_TEXT = "OK from the synthetic ws.";
    private static final List<String> WS_SUPPORTED_ENDPOINTS = List.of("/responses", "ws:/responses");

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

    private static final class WebSocketHandler implements LlmInferenceProvider {

        private final List<String> transports = new ArrayList<>();
        private final AtomicInteger wsRequestCount = new AtomicInteger();

        @Override
        public void onLlmRequest(LlmInferenceRequest req) throws Exception {
            synchronized (transports) {
                transports.add(req.getTransport());
            }
            if (LlmInferenceRequest.TRANSPORT_WEBSOCKET.equals(req.getTransport())) {
                handleWebSocket(req);
            } else if (isInferenceUrl(req.getUrl())) {
                handleHttpInference(req);
            } else {
                handleNonInferenceModelTraffic(req, WS_SUPPORTED_ENDPOINTS);
            }
        }

        // Answers single-shot HTTP inference requests (e.g. title generation) that
        // don't pick the WebSocket transport.
        private void handleHttpInference(LlmInferenceRequest req) throws Exception {
            req.getRequestBody().readAllBytes();
            LlmInferenceResponseSink sink = req.getResponseBody();
            sink.start(new LlmInferenceResponseInit(200).setHeaders(headers("content-type", "text/event-stream")));
            for (Map<String, Object> event : responsesEvents(WS_TEXT, "resp_stub_ws_1")) {
                sink.write(sse((String) event.get("type"), event).getBytes(StandardCharsets.UTF_8));
            }
            sink.end();
        }

        private void handleWebSocket(LlmInferenceRequest req) throws Exception {
            LlmInferenceResponseSink sink = req.getResponseBody();
            // Ack the upgrade (status 101-equivalent) before any message flows.
            sink.start(new LlmInferenceResponseInit(101).setHeaders(emptyHeaders()));
            // One inbound chunk == one WS message (a response.create request).
            while (req.getRequestBody().read() != null) {
                wsRequestCount.incrementAndGet();
                for (Map<String, Object> event : responsesEvents(WS_TEXT, "resp_stub_ws_1")) {
                    sink.write(json(event).getBytes(StandardCharsets.UTF_8));
                }
            }
            sink.end();
        }

        int wsRequests() {
            synchronized (transports) {
                int n = 0;
                for (String transport : transports) {
                    if (LlmInferenceRequest.TRANSPORT_WEBSOCKET.equals(transport)) {
                        n++;
                    }
                }
                return n;
            }
        }
    }

    @Test
    void drivesWebSocketTransport() throws Exception {
        setupCapiAuth(ctx);
        WebSocketHandler handler = new WebSocketHandler();

        try (CopilotClient client = newLlmClient(ctx, handler, "COPILOT_EXP_COPILOT_CLI_WEBSOCKET_RESPONSES=true")) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            AssistantMessageEvent result = session.sendAndWait(new MessageOptions().setPrompt("Say OK.")).get(60,
                    TimeUnit.SECONDS);
            session.close();

            // The main agent turn (tools present, not single-shot) selected the
            // WebSocket transport and drove it through the callback.
            assertTrue(handler.wsRequests() > 0, "Expected at least one websocket request via the callback");
            assertTrue(handler.wsRequestCount.get() > 0, "Expected the runtime to send at least one ws message");

            // Validate the final assistant response arrived (guards against truncated
            // captures)
            assertTrue(assistantText(result).contains("OK from the synthetic ws"),
                    "Expected synthetic ws content in assistant reply, got " + assistantText(result));
        }
    }
}
