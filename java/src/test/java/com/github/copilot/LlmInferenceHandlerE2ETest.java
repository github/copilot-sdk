/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static com.github.copilot.LlmInferenceTestSupport.assistantText;
import static com.github.copilot.LlmInferenceTestSupport.newLlmClient;
import static com.github.copilot.LlmInferenceTestSupport.setupCapiAuth;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
 * Verifies that the runtime's model-layer traffic can be forwarded through the
 * idiomatic {@link LlmRequestHandler} seams to a real upstream: an HTTP send
 * override that mutates the request/response and a forwarding
 * {@link CopilotWebSocketHandler} that observes messages in both directions.
 */
public class LlmInferenceHandlerE2ETest {

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

    private static String rewriteHost(String base, URI original) {
        String path = original.getRawPath() == null ? "" : original.getRawPath();
        String query = original.getRawQuery();
        return base + path + (query != null ? "?" + query : "");
    }

    @Test
    void forwardsThroughIdiomaticHandler() throws Exception {
        setupCapiAuth(ctx);

        AtomicInteger httpRequests = new AtomicInteger();
        AtomicInteger httpResponses = new AtomicInteger();
        AtomicInteger wsRequestMessages = new AtomicInteger();
        AtomicInteger wsResponseMessages = new AtomicInteger();

        try (FakeUpstreamServer upstream = new FakeUpstreamServer("OK from synthetic HTTP upstream.",
                "OK from synthetic WS upstream.")) {

            String httpBase = upstream.httpUrl();
            String wsBase = upstream.wsUrl();

            LlmRequestHandler handler = new LlmRequestHandler() {
                @Override
                protected HttpResponse<InputStream> sendHttp(HttpRequest request, LlmRequestContext rctx)
                        throws Exception {
                    httpRequests.incrementAndGet();
                    URI rewritten = URI.create(rewriteHost(httpBase, request.uri()));
                    HttpRequest.Builder builder = HttpRequest.newBuilder().uri(rewritten);
                    request.bodyPublisher().ifPresentOrElse(bp -> builder.method(request.method(), bp),
                            () -> builder.method(request.method(), HttpRequest.BodyPublishers.noBody()));
                    request.headers().map().forEach((name, values) -> {
                        for (String value : values) {
                            try {
                                builder.header(name, value);
                            } catch (IllegalArgumentException ignored) {
                                // Restricted header rejected by java.net.http; skip it.
                            }
                        }
                    });
                    builder.header("x-test-mutated", "1");
                    HttpResponse<InputStream> response = httpClient()
                            .sendAsync(builder.build(), HttpResponse.BodyHandlers.ofInputStream()).get();
                    httpResponses.incrementAndGet();
                    return response;
                }

                @Override
                protected CopilotWebSocketHandler openWebSocket(LlmRequestContext rctx) {
                    String rewritten = rewriteHost(wsBase, URI.create(rctx.url()));
                    return new ForwardingWebSocketHandler(rewritten, rctx.headers()) {
                        @Override
                        protected byte[] onSendRequestMessage(byte[] data, boolean binary) {
                            wsRequestMessages.incrementAndGet();
                            return data;
                        }

                        @Override
                        protected byte[] onSendResponseMessage(byte[] data, boolean binary) {
                            wsResponseMessages.incrementAndGet();
                            return data;
                        }
                    };
                }
            };

            try (CopilotClient client = newLlmClient(ctx, handler,
                    "COPILOT_EXP_COPILOT_CLI_WEBSOCKET_RESPONSES=true")) {
                CopilotSession session = client
                        .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

                AssistantMessageEvent result = session.sendAndWait(new MessageOptions().setPrompt("Say OK.")).get(60,
                        TimeUnit.SECONDS);
                session.close();

                // The HTTP seam fired — the runtime issued model-layer GETs (catalog,
                // policy) and possibly a single-shot inference through the send override.
                assertTrue(httpRequests.get() > 0, "Expected the HTTP send override to fire");
                assertTrue(httpResponses.get() > 0, "Expected the HTTP response mutation to fire");

                // The WebSocket seam fired — the main agent turn went over the WS path and
                // we observed messages in both directions.
                assertTrue(wsRequestMessages.get() > 0, "Expected runtime -> upstream ws messages");
                assertTrue(wsResponseMessages.get() > 0, "Expected upstream -> runtime ws messages");
                assertTrue(upstream.upstreamWsRequests() > 0, "Expected the upstream WS to receive request messages");

                // Validate the final assistant response arrived (guards against truncated
                // captures)
                String text = assistantText(result);
                assertTrue(text.contains("OK from synthetic") && text.contains("upstream"),
                        "Expected synthetic upstream content in assistant reply, got " + text);
            }
        }
    }
}
