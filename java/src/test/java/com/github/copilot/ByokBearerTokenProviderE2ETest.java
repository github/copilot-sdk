/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static com.github.copilot.CopilotRequestTestSupport.buildNonInferenceResponse;
import static com.github.copilot.CopilotRequestTestSupport.newLlmClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLSession;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.BearerTokenProvider;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.NamedProviderConfig;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ProviderModelConfig;
import com.github.copilot.rpc.SessionConfig;

/**
 * End-to-end coverage for the experimental BYOK bearer-token-provider surface
 * ({@code BearerTokenProvider} on a provider config). The callback stays
 * entirely on the SDK/client side: the SDK keeps it off the wire, sends only
 * the {@code hasBearerTokenProvider} flag, and the runtime calls back over the
 * session-scoped {@code providerToken.getToken} RPC before each outbound model
 * request.
 */
public class ByokBearerTokenProviderE2ETest {

    private static final String PRIMARY_HOST = "byok-endpoint.invalid";
    private static final String PRIMARY_BASE_URL = "https://" + PRIMARY_HOST + "/v1";
    private static final String RED_HOST = "byok-red.invalid";
    private static final String RED_BASE_URL = "https://" + RED_HOST + "/v1";
    private static final String BLUE_HOST = "byok-blue.invalid";
    private static final String BLUE_BASE_URL = "https://" + BLUE_HOST + "/v1";

    private static E2ETestContext ctx;
    private CapturingRequestHandler handler;

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

    @BeforeEach
    void resetHandler() {
        handler = new CapturingRequestHandler();
    }

    @Test
    void appliesCallbackTokenAsAuthorizationHeader() throws Exception {
        String sentinel = "sentinel-bearer-token-abc123";
        AtomicInteger calls = new AtomicInteger();
        BearerTokenProvider tokenProvider = args -> {
            calls.incrementAndGet();
            return CompletableFuture.completedFuture(sentinel);
        };

        List<NamedProviderConfig> providers = List.of(new NamedProviderConfig().setName("mi").setType("openai")
                .setWireApi("completions").setBaseUrl(PRIMARY_BASE_URL).setBearerTokenProvider(tokenProvider));
        List<ProviderModelConfig> models = List
                .of(new ProviderModelConfig().setId("default").setProvider("mi").setWireModel("byok-gpt-4o"));

        runTurn(providers, models, "mi/default", "What is 5+5?");

        assertTrue(handler.authHeaders().contains("Bearer " + sentinel),
                "Expected captured Authorization headers to contain the callback token: " + handler.authHeaders());
        assertTrue(calls.get() >= 1, "Expected the callback to be invoked at least once");
    }

    @Test
    void reacquiresFreshTokenForEachRequest() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        BearerTokenProvider tokenProvider = args -> CompletableFuture
                .completedFuture("rotating-token-" + calls.incrementAndGet());

        List<NamedProviderConfig> providers = List.of(new NamedProviderConfig().setName("mi").setType("openai")
                .setWireApi("completions").setBaseUrl(PRIMARY_BASE_URL).setBearerTokenProvider(tokenProvider));
        List<ProviderModelConfig> models = List
                .of(new ProviderModelConfig().setId("default").setProvider("mi").setWireModel("byok-gpt-4o"));

        runTurn(providers, models, "mi/default", "What is 1+1?");
        runTurn(providers, models, "mi/default", "What is 2+2?");

        List<String> auths = handler.authHeaders();
        assertTrue(auths.size() >= 2, "Expected at least two captured Authorization headers, got " + auths);
        assertTrue(auths.get(0).startsWith("Bearer rotating-token-"), "Expected rotating token, got " + auths);
        assertTrue(auths.get(1).startsWith("Bearer rotating-token-"), "Expected rotating token, got " + auths);
        assertNotEquals(auths.get(0), auths.get(1), "Expected distinct tokens per request");
        assertTrue(calls.get() >= 2, "Expected the callback to be invoked at least twice");
    }

    @Test
    void dispatchesTokenAcquisitionPerProvider() throws Exception {
        List<String> acquiredFor = new ArrayList<>();
        BearerTokenProvider redCallback = args -> {
            assertEquals("red", args.getProviderName(), "Expected providerName to be forwarded");
            assertTrue(args.getSessionId() != null && !args.getSessionId().isEmpty(),
                    "Expected a non-empty session id in token args");
            synchronized (acquiredFor) {
                acquiredFor.add("red");
            }
            return CompletableFuture.completedFuture("token-for-red");
        };
        BearerTokenProvider blueCallback = args -> {
            assertEquals("blue", args.getProviderName(), "Expected providerName to be forwarded");
            assertTrue(args.getSessionId() != null && !args.getSessionId().isEmpty(),
                    "Expected a non-empty session id in token args");
            synchronized (acquiredFor) {
                acquiredFor.add("blue");
            }
            return CompletableFuture.completedFuture("token-for-blue");
        };

        List<NamedProviderConfig> providers = List.of(
                new NamedProviderConfig().setName("red").setType("openai").setWireApi("completions")
                        .setBaseUrl(RED_BASE_URL).setBearerTokenProvider(redCallback),
                new NamedProviderConfig().setName("blue").setType("openai").setWireApi("completions")
                        .setBaseUrl(BLUE_BASE_URL).setBearerTokenProvider(blueCallback));
        List<ProviderModelConfig> models = List.of(
                new ProviderModelConfig().setId("default").setProvider("red").setWireModel("byok-gpt-4o"),
                new ProviderModelConfig().setId("default").setProvider("blue").setWireModel("byok-gpt-4o"));

        runTurn(providers, models, "red/default", "What is 3+3?");
        runTurn(providers, models, "blue/default", "What is 4+4?");

        assertEquals("Bearer token-for-red", handler.authHeaderForHost(RED_HOST));
        assertEquals("Bearer token-for-blue", handler.authHeaderForHost(BLUE_HOST));
        synchronized (acquiredFor) {
            assertTrue(acquiredFor.contains("red"), "Expected red provider to acquire a token");
            assertTrue(acquiredFor.contains("blue"), "Expected blue provider to acquire a token");
        }
    }

    private void runTurn(List<NamedProviderConfig> providers, List<ProviderModelConfig> models, String selectionId,
            String prompt) throws Exception {
        try (CopilotClient client = newLlmClient(ctx, handler)) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setModel(selectionId).setProviders(providers).setModels(models))
                    .get(60, TimeUnit.SECONDS);
            try {
                session.sendAndWait(new MessageOptions().setPrompt(prompt)).get(60, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // The fake BYOK endpoint returns 404 after capturing the token-bearing request.
            } finally {
                try {
                    session.close();
                } catch (Exception ignored) {
                    // Ignore disconnect errors for the fake BYOK endpoint.
                }
            }
        }
    }

    private static final class CapturingRequestHandler extends CopilotRequestHandler {

        private final ConcurrentLinkedQueue<CapturedRequest> captures = new ConcurrentLinkedQueue<>();

        @Override
        protected HttpResponse<InputStream> sendRequest(HttpRequest request, CopilotRequestContext rctx)
                throws Exception {
            String host = request.uri().getHost();
            if (host != null && host.endsWith(".invalid")) {
                captures.add(new CapturedRequest(request.uri().getHost(),
                        request.headers().firstValue("Authorization").orElse(null)));
                return new StubHttpResponse(404, "{\"error\":{\"message\":\"fake byok endpoint\"}}");
            }
            return buildNonInferenceResponse(request.uri().toString());
        }

        List<String> authHeaders() {
            List<String> auths = new ArrayList<>();
            for (CapturedRequest capture : captures) {
                if (capture.authorization() != null) {
                    auths.add(capture.authorization());
                }
            }
            return auths;
        }

        String authHeaderForHost(String host) {
            for (CapturedRequest capture : captures) {
                if (host.equals(capture.host())) {
                    return capture.authorization();
                }
            }
            return null;
        }
    }

    private static final class StubHttpResponse implements HttpResponse<InputStream> {

        private final int status;
        private final HttpHeaders headers;
        private final byte[] body;

        StubHttpResponse(int status, String body) {
            this.status = status;
            this.body = body.getBytes(StandardCharsets.UTF_8);
            this.headers = HttpHeaders.of(Map.of("content-type", List.of("application/json")), (k, v) -> true);
        }

        @Override
        public int statusCode() {
            return status;
        }

        @Override
        public HttpRequest request() {
            return null;
        }

        @Override
        public Optional<HttpResponse<InputStream>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return headers;
        }

        @Override
        public InputStream body() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return null;
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }

    private record CapturedRequest(String host, String authorization) {
    }
}
