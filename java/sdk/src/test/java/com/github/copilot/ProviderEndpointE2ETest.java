/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.rpc.ProviderEndpointType;
import com.github.copilot.generated.rpc.ProviderEndpointWireApi;
import com.github.copilot.generated.rpc.ProviderSessionToken;
import com.github.copilot.generated.rpc.SessionProviderGetEndpointResult;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ProviderConfig;
import com.github.copilot.rpc.SessionConfig;

/**
 * Tests for the {@code session.provider.getEndpoint} RPC, which surfaces the
 * resolved provider endpoint and credentials for either a BYOK or CAPI session.
 */
public class ProviderEndpointE2ETest {

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

    // session.provider.getEndpoint is gated behind
    // COPILOT_ALLOW_GET_PROVIDER_ENDPOINT;
    // the harness env passed to the CLI subprocess opts in for these tests.
    private CopilotClient createProviderEndpointClient() {
        Map<String, String> env = new HashMap<>(ctx.getEnvironment());
        env.put("COPILOT_ALLOW_GET_PROVIDER_ENDPOINT", "true");
        return ctx.createClient(new CopilotClientOptions().setEnvironment(env));
    }

    @Test
    void shouldReturnByokProviderEndpointWhenCustomProviderConfigured() throws Exception {
        try (CopilotClient client = createProviderEndpointClient()) {
            Map<String, String> customHeaders = new HashMap<>();
            customHeaders.put("X-Custom-Header", "byok-yes");

            ProviderConfig provider = new ProviderConfig().setType("openai").setWireApi("completions")
                    .setBaseUrl("https://api.example.test/v1").setApiKey("byok-secret").setHeaders(customHeaders);

            CopilotSession session = client.createSession(
                    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setProvider(provider))
                    .get();

            try {
                SessionProviderGetEndpointResult endpoint = session.getRpc().provider.getEndpoint().get();

                assertEquals(ProviderEndpointType.OPENAI, endpoint.type());
                assertEquals(ProviderEndpointWireApi.COMPLETIONS, endpoint.wireApi());
                assertEquals("https://api.example.test/v1", endpoint.baseUrl());
                assertEquals("byok-secret", endpoint.apiKey());
                assertEquals("byok-yes", endpoint.headers().get("X-Custom-Header"));
                // BYOK sessions never issue a CAPI session token.
                assertNull(endpoint.sessionToken(), "BYOK session should not have a session token");
            } finally {
                try {
                    session.close();
                } catch (Exception ignored) {
                    // disconnect may fail since the BYOK provider URL is fake
                }
            }
        }
    }

    @Test
    void shouldReturnCapiProviderEndpointForOAuthAuthenticatedSession() throws Exception {
        ctx.initializeProxy();
        ctx.setCopilotUserByToken("fake-token-for-e2e-tests", "e2e-user", "individual_pro", ctx.getProxyUrl(),
                "https://localhost:1/telemetry", "e2e-tracking-id");

        try (CopilotClient client = createProviderEndpointClient()) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            try {
                SessionProviderGetEndpointResult endpoint = session.getRpc().provider.getEndpoint().get();

                assertNotNull(endpoint.type(), "CAPI endpoint should have a provider type");
                assertTrue(
                        endpoint.type() == ProviderEndpointType.OPENAI || endpoint.type() == ProviderEndpointType.AZURE
                                || endpoint.type() == ProviderEndpointType.ANTHROPIC,
                        "expected type in {openai, azure, anthropic}, got " + endpoint.type());
                // wireApi is omitted for anthropic; otherwise one of the OpenAI shapes.
                if (endpoint.type() != ProviderEndpointType.ANTHROPIC) {
                    assertTrue(
                            endpoint.wireApi() == ProviderEndpointWireApi.COMPLETIONS
                                    || endpoint.wireApi() == ProviderEndpointWireApi.RESPONSES,
                            "expected wireApi in {completions, responses}, got " + endpoint.wireApi());
                }

                // CAPI baseUrl is the (proxy) Copilot API URL injected by the harness.
                assertTrue(endpoint.baseUrl().startsWith("http://") || endpoint.baseUrl().startsWith("https://"),
                        "expected http(s) baseUrl, got " + endpoint.baseUrl());

                // For CAPI OAuth sessions the apiKey is the resolved GitHub bearer.
                assertNotNull(endpoint.apiKey(), "CAPI OAuth session must surface apiKey");
                assertFalse(endpoint.apiKey().isEmpty(), "apiKey must be non-empty");

                Map<String, String> headers = endpoint.headers();
                String integrationId = headers.get("Copilot-Integration-Id");
                assertNotNull(integrationId, "Copilot-Integration-Id header must be present");
                assertFalse(integrationId.isEmpty(), "Copilot-Integration-Id must be non-empty");

                String userAgent = headers.get("User-Agent");
                assertNotNull(userAgent, "User-Agent header must be present");
                assertTrue(userAgent.toLowerCase().contains("copilot"),
                        "expected User-Agent to mention Copilot, got " + userAgent);

                String apiVersion = headers.get("X-GitHub-Api-Version");
                assertNotNull(apiVersion, "X-GitHub-Api-Version header must be present");
                assertFalse(apiVersion.isEmpty(), "X-GitHub-Api-Version must be non-empty");

                String interactionId = headers.get("X-Interaction-Id");
                assertNotNull(interactionId, "X-Interaction-Id header must be present");
                assertTrue(interactionId.matches(".*[0-9a-f-]{8,}.*"),
                        "expected X-Interaction-Id to look like a hex/uuid value, got " + interactionId);

                String authorization = headers.get("Authorization");
                assertEquals("Bearer " + endpoint.apiKey(), authorization);

                ProviderSessionToken sessionToken = endpoint.sessionToken();
                if (sessionToken != null) {
                    assertEquals("Copilot-Session-Token", sessionToken.header());
                    assertFalse(sessionToken.token().isEmpty(), "session token must be non-empty");
                    // expiresAt is optional; when present it parses as OffsetDateTime so no
                    // additional validation is needed.
                }
            } finally {
                session.close();
            }
        }
    }
}
