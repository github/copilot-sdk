/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.AgentInfo;
import com.github.copilot.rpc.CustomAgentConfig;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.NamedProviderConfig;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ProviderModelConfig;
import com.github.copilot.rpc.SessionConfig;

/**
 * End-to-end coverage for the experimental multi-provider BYOK registry
 * ({@code SessionConfig.providers} / {@code SessionConfig.models}). Validates
 * that several named providers, several models per provider, and custom agents
 * bound to those provider-qualified models can coexist in one session, be
 * launched, and route inference to the configured provider with the configured
 * wire model and headers.
 */
public class MultiProviderRegistryE2ETest {

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

    /**
     * Builds a heterogeneous registry: two providers of different types, with
     * multiple models each. Provider-qualified selection ids are
     * {@code alpha/sonnet}, {@code alpha/haiku}, {@code beta/opus},
     * {@code beta/haiku}.
     */
    private static List<NamedProviderConfig> registryProviders() {
        return List.of(
                new NamedProviderConfig().setName("alpha").setType("openai").setWireApi("completions")
                        .setBaseUrl("https://alpha.example.test/v1").setApiKey("alpha-secret")
                        .setHeaders(Map.of("X-Provider", "alpha")),
                new NamedProviderConfig().setName("beta").setType("anthropic").setBaseUrl("https://beta.example.test")
                        .setBearerToken("beta-bearer").setHeaders(Map.of("X-Provider", "beta")));
    }

    private static List<ProviderModelConfig> registryModels() {
        return List.of(
                new ProviderModelConfig().setId("sonnet").setProvider("alpha").setWireModel("byok-gpt-4o")
                        .setMaxPromptTokens(111111),
                new ProviderModelConfig().setId("haiku").setProvider("alpha").setWireModel("byok-gpt-4o-mini"),
                new ProviderModelConfig().setId("opus").setProvider("beta").setWireModel("byok-claude-3-opus"),
                new ProviderModelConfig().setId("haiku").setProvider("beta").setWireModel("byok-claude-3-haiku"));
    }

    private static List<CustomAgentConfig> registryAgents() {
        return List.of(
                new CustomAgentConfig().setName("orchestrator").setDisplayName("Orchestrator")
                        .setDescription("Top-level planner.").setPrompt("Plan and delegate.").setModel("alpha/sonnet"),
                new CustomAgentConfig().setName("researcher").setDisplayName("Researcher")
                        .setDescription("Deep research subagent.").setPrompt("Research thoroughly.")
                        .setModel("beta/opus"),
                new CustomAgentConfig().setName("fast-helper").setDisplayName("Fast Helper")
                        .setDescription("Quick subagent.").setPrompt("Answer quickly.").setModel("alpha/haiku"),
                new CustomAgentConfig().setName("summarizer").setDisplayName("Summarizer")
                        .setDescription("Summarizing subagent.").setPrompt("Summarize.").setModel("beta/haiku"));
    }

    @Test
    void testShouldRegisterMultipleProvidersWithCustomAgentsBoundToTheirModels() throws Exception {
        ctx.configureForTest("multi_provider_registry",
                "should_register_multiple_providers_with_custom_agents_bound_to_their_models");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setProviders(registryProviders()).setModels(registryModels())
                            .setCustomAgents(registryAgents()).setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get();

            List<AgentInfo> agents = session.listAgents().get(30, TimeUnit.SECONDS);

            // All four custom agents coexist in a single session.
            assertEquals(4, agents.size(), "Expected all four custom agents to coexist");

            // Each agent is bound to its configured provider-qualified BYOK model.
            assertAgentModel(agents, "orchestrator", "alpha/sonnet", "Orchestrator", "Top-level planner.");
            assertAgentModel(agents, "researcher", "beta/opus", "Researcher", "Deep research subagent.");
            assertAgentModel(agents, "fast-helper", "alpha/haiku", "Fast Helper", "Quick subagent.");
            assertAgentModel(agents, "summarizer", "beta/haiku", "Summarizer", "Summarizing subagent.");

            // Models from BOTH providers are represented, proving the two
            // providers and their models coexist within the same session.
            Set<String> boundModels = new HashSet<>();
            for (AgentInfo agent : agents) {
                boundModels.add(agent.getModel());
            }
            assertTrue(boundModels.stream().anyMatch(m -> m != null && m.startsWith("alpha/")),
                    "Expected a model from provider 'alpha' to be represented");
            assertTrue(boundModels.stream().anyMatch(m -> m != null && m.startsWith("beta/")),
                    "Expected a model from provider 'beta' to be represented");
        }
    }

    @Test
    void testShouldRouteAlphaSonnetTurnToItsProviderAndWireModel() throws Exception {
        assertRouting("should_route_alpha_sonnet_turn_to_its_provider_and_wire_model", "alpha/sonnet", "byok-gpt-4o",
                "alpha");
    }

    @Test
    void testShouldRouteAlphaHaikuTurnToItsProviderAndWireModel() throws Exception {
        assertRouting("should_route_alpha_haiku_turn_to_its_provider_and_wire_model", "alpha/haiku", "byok-gpt-4o-mini",
                "alpha");
    }

    @Test
    void testShouldRouteDeltaTurboTurnToItsProviderAndWireModel() throws Exception {
        assertRouting("should_route_delta_turbo_turn_to_its_provider_and_wire_model", "delta/turbo", "byok-gpt-4-turbo",
                "delta");
    }

    /**
     * Selects {@code selectionId} in a session whose registry holds two
     * OpenAI-compatible providers (each pointed at the replay proxy), runs a turn,
     * and asserts the captured request used the model's configured wire model and
     * carried the owning provider's header and credential.
     */
    private void assertRouting(String snapshot, String selectionId, String expectedWireModel,
            String expectedProviderHeader) throws Exception {
        ctx.configureForTest("multi_provider_registry", snapshot);

        try (CopilotClient client = ctx.createClient()) {
            // Two OpenAI-compatible providers, both pointed at the replay proxy
            // so their /chat/completions traffic is captured. They are
            // distinguished on the wire by their per-provider X-Provider header.
            // "alpha" carries two models (multiple models per provider);
            // "delta" carries one.
            List<NamedProviderConfig> providers = List.of(
                    new NamedProviderConfig().setName("alpha").setType("openai").setWireApi("completions")
                            .setBaseUrl(ctx.getProxyUrl()).setApiKey("alpha-secret")
                            .setHeaders(Map.of("X-Provider", "alpha")),
                    new NamedProviderConfig().setName("delta").setType("openai").setWireApi("completions")
                            .setBaseUrl(ctx.getProxyUrl()).setApiKey("delta-secret")
                            .setHeaders(Map.of("X-Provider", "delta")));
            List<ProviderModelConfig> models = List.of(
                    new ProviderModelConfig().setId("sonnet").setProvider("alpha").setWireModel("byok-gpt-4o"),
                    new ProviderModelConfig().setId("haiku").setProvider("alpha").setWireModel("byok-gpt-4o-mini"),
                    new ProviderModelConfig().setId("turbo").setProvider("delta").setWireModel("byok-gpt-4-turbo"));

            CopilotSession session = client.createSession(new SessionConfig().setModel(selectionId)
                    .setProviders(providers).setModels(models).setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get();

            session.sendAndWait(new MessageOptions().setPrompt("What is 5+5?")).get(30, TimeUnit.SECONDS);

            List<Map<String, Object>> exchanges = ctx.getExchanges();
            assertEquals(1, exchanges.size(), "Expected exactly one captured /chat/completions exchange");
            Map<String, Object> exchange = exchanges.get(0);

            @SuppressWarnings("unchecked")
            Map<String, Object> request = (Map<String, Object>) exchange.get("request");

            // The wire model sent to the provider is the selected model's wire
            // model, not its provider-qualified selection id.
            assertEquals(expectedWireModel, request.get("model"));

            // The request carried the owning provider's custom header, proving
            // the turn was dispatched against the correct provider connection.
            assertEquals(expectedProviderHeader, getHeaderValue(exchange, "X-Provider"));

            // The provider's API key was applied as an Authorization header.
            String authorization = getHeaderValue(exchange, "Authorization");
            assertNotNull(authorization, "Expected an Authorization header on the dispatched request");
            assertFalse(authorization.isEmpty(), "Expected a non-empty Authorization header");
        }
    }

    private static void assertAgentModel(List<AgentInfo> agents, String name, String expectedModel,
            String expectedDisplayName, String expectedDescription) {
        AgentInfo agent = agents.stream().filter(a -> name.equals(a.getName())).findFirst()
                .orElseThrow(() -> new AssertionError("Expected an agent named '" + name + "'"));
        assertEquals(expectedModel, agent.getModel(), "Unexpected model binding for agent '" + name + "'");
        assertEquals(expectedDisplayName, agent.getDisplayName(), "Unexpected display name for agent '" + name + "'");
        assertEquals(expectedDescription, agent.getDescription(), "Unexpected description for agent '" + name + "'");
    }

    @SuppressWarnings("unchecked")
    private static String getHeaderValue(Map<String, Object> exchange, String name) {
        Object headersObj = exchange.get("requestHeaders");
        if (!(headersObj instanceof Map<?, ?> headers)) {
            return null;
        }
        for (Map.Entry<?, ?> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().toString().equalsIgnoreCase(name)) {
                Object value = entry.getValue();
                if (value instanceof List<?> list) {
                    return list.isEmpty() ? null : String.valueOf(list.get(0));
                }
                return value != null ? value.toString() : null;
            }
        }
        return null;
    }
}
