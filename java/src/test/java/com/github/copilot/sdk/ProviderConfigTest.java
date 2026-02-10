/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.github.copilot.sdk.json.AzureOptions;
import com.github.copilot.sdk.json.ProviderConfig;
import com.github.copilot.sdk.json.ResumeSessionConfig;
import com.github.copilot.sdk.json.SessionConfig;

/**
 * Tests for {@link ProviderConfig} and {@link AzureOptions} BYOK (Bring Your
 * Own Key) configuration.
 *
 * <p>
 * Covers fluent setters, JSON serialization, null-field omission, and
 * integration with {@link SessionConfig} and {@link ResumeSessionConfig}.
 * </p>
 */
public class ProviderConfigTest {

    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

    // =========================================================================
    // Fluent setters and getters
    // =========================================================================

    @Test
    void testDefaultsAreNull() {
        var provider = new ProviderConfig();

        assertNull(provider.getType());
        assertNull(provider.getWireApi());
        assertNull(provider.getBaseUrl());
        assertNull(provider.getApiKey());
        assertNull(provider.getBearerToken());
        assertNull(provider.getAzure());
    }

    @Test
    void testFluentSettersReturnSameInstance() {
        var provider = new ProviderConfig();

        ProviderConfig result = provider.setType("openai").setWireApi("completions")
                .setBaseUrl("https://api.openai.com/v1").setApiKey("sk-test-key").setBearerToken("bearer-token")
                .setAzure(new AzureOptions());

        // All chained calls should return the same instance
        assertEquals(provider, result);
    }

    @Test
    void testGettersReturnSetValues() {
        var azure = new AzureOptions().setApiVersion("2024-02-01");
        var provider = new ProviderConfig().setType("azure-openai").setWireApi("chat")
                .setBaseUrl("https://my-resource.openai.azure.com").setApiKey("my-key").setBearerToken("my-token")
                .setAzure(azure);

        assertEquals("azure-openai", provider.getType());
        assertEquals("chat", provider.getWireApi());
        assertEquals("https://my-resource.openai.azure.com", provider.getBaseUrl());
        assertEquals("my-key", provider.getApiKey());
        assertEquals("my-token", provider.getBearerToken());
        assertNotNull(provider.getAzure());
        assertEquals("2024-02-01", provider.getAzure().getApiVersion());
    }

    // =========================================================================
    // AzureOptions
    // =========================================================================

    @Test
    void testAzureOptionsDefaultsAreNull() {
        var azure = new AzureOptions();
        assertNull(azure.getApiVersion());
    }

    @Test
    void testAzureOptionsFluentSetter() {
        var azure = new AzureOptions();
        AzureOptions result = azure.setApiVersion("2023-12-01-preview");

        assertEquals(azure, result);
        assertEquals("2023-12-01-preview", azure.getApiVersion());
    }

    // =========================================================================
    // JSON serialization — OpenAI BYOK
    // =========================================================================

    @Test
    void testSerializeOpenAiProvider() throws Exception {
        var provider = new ProviderConfig().setType("openai").setBaseUrl("https://api.openai.com/v1")
                .setApiKey("sk-test-key");

        JsonNode json = MAPPER.valueToTree(provider);

        assertEquals("openai", json.get("type").asText());
        assertEquals("https://api.openai.com/v1", json.get("baseUrl").asText());
        assertEquals("sk-test-key", json.get("apiKey").asText());
        // Null fields must be omitted (NON_NULL)
        assertTrue(json.path("wireApi").isMissingNode());
        assertTrue(json.path("bearerToken").isMissingNode());
        assertTrue(json.path("azure").isMissingNode());
    }

    @Test
    void testDeserializeOpenAiProvider() throws Exception {
        String json = """
                {
                    "type": "openai",
                    "baseUrl": "https://api.openai.com/v1",
                    "apiKey": "sk-test-key"
                }
                """;

        ProviderConfig provider = MAPPER.readValue(json, ProviderConfig.class);

        assertEquals("openai", provider.getType());
        assertEquals("https://api.openai.com/v1", provider.getBaseUrl());
        assertEquals("sk-test-key", provider.getApiKey());
        assertNull(provider.getWireApi());
        assertNull(provider.getBearerToken());
        assertNull(provider.getAzure());
    }

    // =========================================================================
    // JSON serialization — Azure OpenAI BYOK
    // =========================================================================

    @Test
    void testSerializeAzureOpenAiProvider() throws Exception {
        var provider = new ProviderConfig().setType("azure-openai").setBaseUrl("https://my-resource.openai.azure.com")
                .setApiKey("azure-api-key").setAzure(new AzureOptions().setApiVersion("2024-02-01"));

        JsonNode json = MAPPER.valueToTree(provider);

        assertEquals("azure-openai", json.get("type").asText());
        assertEquals("https://my-resource.openai.azure.com", json.get("baseUrl").asText());
        assertEquals("azure-api-key", json.get("apiKey").asText());
        assertNotNull(json.get("azure"));
        assertEquals("2024-02-01", json.get("azure").get("apiVersion").asText());
    }

    @Test
    void testDeserializeAzureOpenAiProvider() throws Exception {
        String json = """
                {
                    "type": "azure-openai",
                    "baseUrl": "https://my-resource.openai.azure.com",
                    "apiKey": "azure-key",
                    "azure": {
                        "apiVersion": "2024-02-01"
                    }
                }
                """;

        ProviderConfig provider = MAPPER.readValue(json, ProviderConfig.class);

        assertEquals("azure-openai", provider.getType());
        assertEquals("https://my-resource.openai.azure.com", provider.getBaseUrl());
        assertEquals("azure-key", provider.getApiKey());
        assertNotNull(provider.getAzure());
        assertEquals("2024-02-01", provider.getAzure().getApiVersion());
    }

    // =========================================================================
    // JSON serialization — Bearer token authentication
    // =========================================================================

    @Test
    void testSerializeBearerTokenProvider() throws Exception {
        var provider = new ProviderConfig().setType("openai").setBaseUrl("https://custom-provider.example.com/v1")
                .setBearerToken("eyJhbGciOiJSUzI1NiIs...");

        JsonNode json = MAPPER.valueToTree(provider);

        assertEquals("openai", json.get("type").asText());
        assertEquals("https://custom-provider.example.com/v1", json.get("baseUrl").asText());
        assertEquals("eyJhbGciOiJSUzI1NiIs...", json.get("bearerToken").asText());
        assertTrue(json.path("apiKey").isMissingNode());
    }

    @Test
    void testDeserializeBearerTokenProvider() throws Exception {
        String json = """
                {
                    "type": "openai",
                    "baseUrl": "https://custom-provider.example.com/v1",
                    "bearerToken": "my-bearer-token"
                }
                """;

        ProviderConfig provider = MAPPER.readValue(json, ProviderConfig.class);

        assertEquals("openai", provider.getType());
        assertEquals("https://custom-provider.example.com/v1", provider.getBaseUrl());
        assertEquals("my-bearer-token", provider.getBearerToken());
        assertNull(provider.getApiKey());
    }

    // =========================================================================
    // JSON serialization — custom wire API
    // =========================================================================

    @Test
    void testSerializeCustomWireApi() throws Exception {
        var provider = new ProviderConfig().setType("openai").setBaseUrl("https://custom.example.com").setApiKey("key")
                .setWireApi("responses");

        JsonNode json = MAPPER.valueToTree(provider);

        assertEquals("responses", json.get("wireApi").asText());
    }

    // =========================================================================
    // JSON serialization — all fields populated
    // =========================================================================

    @Test
    void testSerializeAllFields() throws Exception {
        var provider = new ProviderConfig().setType("azure-openai").setWireApi("completions")
                .setBaseUrl("https://my-resource.openai.azure.com").setApiKey("my-api-key")
                .setBearerToken("my-bearer-token").setAzure(new AzureOptions().setApiVersion("2024-02-01"));

        JsonNode json = MAPPER.valueToTree(provider);

        assertEquals("azure-openai", json.get("type").asText());
        assertEquals("completions", json.get("wireApi").asText());
        assertEquals("https://my-resource.openai.azure.com", json.get("baseUrl").asText());
        assertEquals("my-api-key", json.get("apiKey").asText());
        assertEquals("my-bearer-token", json.get("bearerToken").asText());
        assertEquals("2024-02-01", json.get("azure").get("apiVersion").asText());
        assertEquals(6, json.size(), "Expected exactly 6 JSON fields");
    }

    @Test
    void testSerializeEmptyProviderOmitsAllFields() throws Exception {
        var provider = new ProviderConfig();

        JsonNode json = MAPPER.valueToTree(provider);

        assertEquals(0, json.size(), "Empty ProviderConfig should serialize to {}");
    }

    @Test
    void testSerializeEmptyAzureOptionsOmitsAllFields() throws Exception {
        var azure = new AzureOptions();

        JsonNode json = MAPPER.valueToTree(azure);

        assertEquals(0, json.size(), "Empty AzureOptions should serialize to {}");
    }

    // =========================================================================
    // JSON round-trip
    // =========================================================================

    @Test
    void testRoundTripProviderConfig() throws Exception {
        var original = new ProviderConfig().setType("azure-openai").setWireApi("completions")
                .setBaseUrl("https://my-resource.openai.azure.com").setApiKey("my-key").setBearerToken("my-token")
                .setAzure(new AzureOptions().setApiVersion("2024-02-01"));

        String json = MAPPER.writeValueAsString(original);
        ProviderConfig deserialized = MAPPER.readValue(json, ProviderConfig.class);

        assertEquals(original.getType(), deserialized.getType());
        assertEquals(original.getWireApi(), deserialized.getWireApi());
        assertEquals(original.getBaseUrl(), deserialized.getBaseUrl());
        assertEquals(original.getApiKey(), deserialized.getApiKey());
        assertEquals(original.getBearerToken(), deserialized.getBearerToken());
        assertNotNull(deserialized.getAzure());
        assertEquals(original.getAzure().getApiVersion(), deserialized.getAzure().getApiVersion());
    }

    @Test
    void testForwardCompatibilityIgnoresUnknownFields() throws Exception {
        String json = """
                {
                    "type": "openai",
                    "baseUrl": "https://api.openai.com/v1",
                    "apiKey": "sk-key",
                    "unknownFutureField": "some-value",
                    "anotherNewField": 42
                }
                """;

        // Should not throw - ObjectMapper is configured with
        // FAIL_ON_UNKNOWN_PROPERTIES = false
        ProviderConfig provider = MAPPER.readValue(json, ProviderConfig.class);

        assertEquals("openai", provider.getType());
        assertEquals("https://api.openai.com/v1", provider.getBaseUrl());
        assertEquals("sk-key", provider.getApiKey());
    }

    // =========================================================================
    // Integration with SessionConfig
    // =========================================================================

    @Test
    void testSessionConfigWithOpenAiProvider() throws Exception {
        var config = new SessionConfig().setModel("gpt-4").setProvider(new ProviderConfig().setType("openai")
                .setBaseUrl("https://api.openai.com/v1").setApiKey("sk-test-key"));

        JsonNode json = MAPPER.valueToTree(config);

        assertNotNull(json.get("provider"));
        assertEquals("openai", json.get("provider").get("type").asText());
        assertEquals("https://api.openai.com/v1", json.get("provider").get("baseUrl").asText());
        assertEquals("sk-test-key", json.get("provider").get("apiKey").asText());
        assertEquals("gpt-4", json.get("model").asText());
    }

    @Test
    void testSessionConfigWithAzureProvider() throws Exception {
        var config = new SessionConfig().setModel("gpt-4").setProvider(
                new ProviderConfig().setType("azure-openai").setBaseUrl("https://my-resource.openai.azure.com")
                        .setApiKey("azure-key").setAzure(new AzureOptions().setApiVersion("2024-02-01")));

        JsonNode json = MAPPER.valueToTree(config);

        JsonNode providerNode = json.get("provider");
        assertNotNull(providerNode);
        assertEquals("azure-openai", providerNode.get("type").asText());
        assertEquals("2024-02-01", providerNode.get("azure").get("apiVersion").asText());
    }

    @Test
    void testSessionConfigWithoutProviderOmitsField() throws Exception {
        var config = new SessionConfig().setModel("gpt-4");

        JsonNode json = MAPPER.valueToTree(config);

        assertTrue(json.path("provider").isMissingNode(), "provider field should be omitted when null");
    }

    // =========================================================================
    // Integration with ResumeSessionConfig
    // =========================================================================

    @Test
    void testResumeSessionConfigWithProvider() throws Exception {
        var config = new ResumeSessionConfig().setStreaming(true).setProvider(new ProviderConfig().setType("openai")
                .setBaseUrl("https://api.openai.com/v1").setBearerToken("my-bearer-token"));

        assertNotNull(config.getProvider());
        assertEquals("openai", config.getProvider().getType());
        assertEquals("https://api.openai.com/v1", config.getProvider().getBaseUrl());
        assertEquals("my-bearer-token", config.getProvider().getBearerToken());
    }

    @Test
    void testResumeSessionConfigProviderSerialization() throws Exception {
        var config = new ResumeSessionConfig().setProvider(
                new ProviderConfig().setType("azure-openai").setBaseUrl("https://my-resource.openai.azure.com")
                        .setApiKey("key").setAzure(new AzureOptions().setApiVersion("2024-02-01")));

        JsonNode json = MAPPER.valueToTree(config);

        JsonNode providerNode = json.get("provider");
        assertNotNull(providerNode);
        assertEquals("azure-openai", providerNode.get("type").asText());
        assertEquals("https://my-resource.openai.azure.com", providerNode.get("baseUrl").asText());
        assertEquals("key", providerNode.get("apiKey").asText());
        assertEquals("2024-02-01", providerNode.get("azure").get("apiVersion").asText());
    }

    @Test
    void testResumeSessionConfigWithoutProviderOmitsField() throws Exception {
        var config = new ResumeSessionConfig().setStreaming(true);

        JsonNode json = MAPPER.valueToTree(config);

        assertTrue(json.path("provider").isMissingNode(), "provider field should be omitted when null");
    }
}
