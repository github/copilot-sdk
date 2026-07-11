/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.github.copilot.rpc.AzureOptions;
import com.github.copilot.rpc.NamedProviderConfig;
import com.github.copilot.rpc.ProviderModelConfig;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;

/**
 * Tests for the additive multi-provider BYOK registry:
 * {@link NamedProviderConfig}, {@link ProviderModelConfig}, and their
 * integration with {@link SessionConfig} and {@link ResumeSessionConfig}.
 */
public class MultiProviderConfigTest {

    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

    @Test
    void testNamedProviderConfigDefaultsAreNull() {
        var provider = new NamedProviderConfig();

        assertNull(provider.getName());
        assertNull(provider.getType());
        assertNull(provider.getWireApi());
        assertNull(provider.getBaseUrl());
        assertNull(provider.getApiKey());
        assertNull(provider.getBearerToken());
        assertNull(provider.getAzure());
        assertNull(provider.getHeaders());
    }

    @Test
    void testNamedProviderConfigFluentSettersReturnSameInstance() {
        var provider = new NamedProviderConfig();

        NamedProviderConfig result = provider.setName("my-openai").setType("openai").setWireApi("responses")
                .setBaseUrl("https://api.openai.com/v1").setApiKey("sk-test").setBearerToken("bearer")
                .setAzure(new AzureOptions()).setHeaders(Map.of("X-Custom", "v"));

        assertEquals(provider, result);
    }

    @Test
    void testSerializeNamedProviderConfig() throws Exception {
        var provider = new NamedProviderConfig().setName("my-openai").setType("openai").setWireApi("responses")
                .setBaseUrl("https://api.openai.com/v1").setApiKey("sk-test");

        JsonNode json = MAPPER.valueToTree(provider);

        assertEquals("my-openai", json.get("name").asText());
        assertEquals("openai", json.get("type").asText());
        assertEquals("responses", json.get("wireApi").asText());
        assertEquals("https://api.openai.com/v1", json.get("baseUrl").asText());
        assertEquals("sk-test", json.get("apiKey").asText());
        // Null fields must be omitted (NON_NULL)
        assertTrue(json.path("bearerToken").isMissingNode());
        assertTrue(json.path("azure").isMissingNode());
        assertTrue(json.path("headers").isMissingNode());
    }

    @Test
    void testProviderModelConfigDefaultsAreNull() {
        var model = new ProviderModelConfig();

        assertNull(model.getId());
        assertNull(model.getProvider());
        assertNull(model.getWireModel());
        assertNull(model.getModelId());
        assertNull(model.getName());
        assertTrue(model.getMaxPromptTokens().isEmpty());
        assertTrue(model.getMaxContextWindowTokens().isEmpty());
        assertTrue(model.getMaxOutputTokens().isEmpty());
        assertNull(model.getCapabilities());
    }

    @Test
    void testSerializeProviderModelConfig() throws Exception {
        var model = new ProviderModelConfig().setId("gpt-x").setProvider("my-openai").setWireModel("gpt-x-2025")
                .setModelId("gpt-4o").setName("My GPT-X").setMaxPromptTokens(100_000).setMaxContextWindowTokens(128_000)
                .setMaxOutputTokens(4096);

        JsonNode json = MAPPER.valueToTree(model);

        assertEquals("gpt-x", json.get("id").asText());
        assertEquals("my-openai", json.get("provider").asText());
        assertEquals("gpt-x-2025", json.get("wireModel").asText());
        assertEquals("gpt-4o", json.get("modelId").asText());
        assertEquals("My GPT-X", json.get("name").asText());
        assertEquals(100_000, json.get("maxPromptTokens").asInt());
        assertEquals(128_000, json.get("maxContextWindowTokens").asInt());
        assertEquals(4096, json.get("maxOutputTokens").asInt());
        assertTrue(json.path("capabilities").isMissingNode());

        // Round-trip
        ProviderModelConfig deserialized = MAPPER.readValue(MAPPER.writeValueAsString(model),
                ProviderModelConfig.class);
        assertEquals("gpt-x", deserialized.getId());
        assertEquals("my-openai", deserialized.getProvider());
        assertEquals(100_000, deserialized.getMaxPromptTokens().getAsInt());
        assertEquals(128_000, deserialized.getMaxContextWindowTokens().getAsInt());
        assertEquals(4096, deserialized.getMaxOutputTokens().getAsInt());
    }

    @Test
    void testSessionConfigWithProvidersAndModels() throws Exception {
        var config = new SessionConfig().setModel("gpt-4")
                .setProviders(List.of(new NamedProviderConfig().setName("my-openai").setType("openai")
                        .setBaseUrl("https://api.openai.com/v1").setApiKey("sk-test")))
                .setModels(List.of(new ProviderModelConfig().setId("gpt-x").setProvider("my-openai")));

        JsonNode json = MAPPER.valueToTree(config);

        assertNotNull(json.get("providers"));
        assertEquals(1, json.get("providers").size());
        assertEquals("my-openai", json.get("providers").get(0).get("name").asText());
        assertNotNull(json.get("models"));
        assertEquals("gpt-x", json.get("models").get(0).get("id").asText());
        assertEquals("my-openai", json.get("models").get(0).get("provider").asText());
    }

    @Test
    void testSessionConfigWithoutProvidersOmitsFields() throws Exception {
        var config = new SessionConfig().setModel("gpt-4");

        JsonNode json = MAPPER.valueToTree(config);

        assertTrue(json.path("providers").isMissingNode());
        assertTrue(json.path("models").isMissingNode());
    }

    @Test
    void testSessionConfigCopyPreservesProvidersAndModels() {
        var config = new SessionConfig().setProviders(List.of(new NamedProviderConfig().setName("my-azure")))
                .setModels(List.of(new ProviderModelConfig().setId("deploy-1").setProvider("my-azure")));

        SessionConfig copy = config.clone();

        assertNotNull(copy.getProviders());
        assertEquals(1, copy.getProviders().size());
        assertEquals("my-azure", copy.getProviders().get(0).getName());
        assertNotNull(copy.getModels());
        assertEquals("deploy-1", copy.getModels().get(0).getId());
    }

    @Test
    void testResumeSessionConfigWithProvidersAndModels() throws Exception {
        var config = new ResumeSessionConfig()
                .setProviders(List.of(new NamedProviderConfig().setName("my-azure").setType("azure")
                        .setBaseUrl("https://example.openai.azure.com")
                        .setAzure(new AzureOptions().setApiVersion("2024-10-21"))))
                .setModels(List
                        .of(new ProviderModelConfig().setId("deploy-1").setProvider("my-azure").setModelId("gpt-4o")));

        JsonNode json = MAPPER.valueToTree(config);

        assertNotNull(json.get("providers"));
        assertEquals("my-azure", json.get("providers").get(0).get("name").asText());
        assertEquals("2024-10-21", json.get("providers").get(0).get("azure").get("apiVersion").asText());
        assertNotNull(json.get("models"));
        assertEquals("deploy-1", json.get("models").get(0).get("id").asText());
        assertEquals("gpt-4o", json.get("models").get(0).get("modelId").asText());
    }

    @Test
    void testResumeSessionConfigWithoutProvidersOmitsFields() throws Exception {
        var config = new ResumeSessionConfig().setStreaming(true);

        JsonNode json = MAPPER.valueToTree(config);

        assertTrue(json.path("providers").isMissingNode());
        assertTrue(json.path("models").isMissingNode());
    }
}
