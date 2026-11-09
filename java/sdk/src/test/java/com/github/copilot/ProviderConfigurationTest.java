/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.generated.rpc.NamedProviderConfig;

class ProviderConfigurationTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"openai", "azure", "anthropic", "winml"})
    void preservesSettingsWithoutCredentials(String kind) throws Exception {
        ObjectNode settings = (ObjectNode) mapper.readTree("""
                {
                    "name": "discovered", "baseUrl": "http://127.0.0.1:1234",
                    "wireApi": "responses", "transport": "http",
                    "azure": { "apiVersion": "2025-01-01" },
                    "localModel": { "device": "gpu", "future": [1, true] },
                    "modelProvider": "azure_openai"
                }
                """);
        if (kind != null) {
            settings.put("type", kind);
        }
        ObjectNode input = settings.deepCopy();
        input.put("apiKey", "fixture-key");
        input.put("bearerToken", "fixture-token");
        input.putObject("headers").put("Authorization", "fixture-header");
        input.put("hasBearerTokenProvider", true);
        NamedProviderConfig provider = mapper.treeToValue(input, NamedProviderConfig.class);
        ObjectNode original = mapper.valueToTree(provider);

        var saved = ProviderConfiguration.toSavedProviderConfiguration(provider);

        assertEquals(settings, mapper.valueToTree(saved));
        assertEquals(original, mapper.valueToTree(provider));
    }

    @Test
    void omitsAbsentOptionalSettings() throws Exception {
        var settings = mapper.readTree("""
                {"name":"minimal","baseUrl":"http://127.0.0.1:1234"}
                """);
        var provider = mapper.treeToValue(settings, NamedProviderConfig.class);

        assertEquals(settings, mapper.valueToTree(ProviderConfiguration.toSavedProviderConfiguration(provider)));
    }
}
