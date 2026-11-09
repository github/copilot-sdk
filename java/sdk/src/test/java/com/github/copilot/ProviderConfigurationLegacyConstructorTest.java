/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.NamedProviderConfig;
import com.github.copilot.generated.rpc.ProviderModelConfig;

class ProviderConfigurationLegacyConstructorTest {

    @Test
    void providerKeepsItsReleasedConstructorAndOmitsUnspecifiedRuntimeOptions() {
        var provider = new NamedProviderConfig("saved", null, null, null, "https://example.test", "fixture-key",
                "fixture-bearer", null, Map.of("X-Fixture", "kept"), null, false);
        assertEquals("saved", provider.name());
        assertEquals("fixture-key", provider.apiKey());
        assertEquals("fixture-bearer", provider.bearerToken());
        assertEquals(Map.of("X-Fixture", "kept"), provider.headers());
        assertNull(provider.localModel());
        assertFalse(new ObjectMapper().valueToTree(provider).has("localModel"));
    }

    @Test
    void modelKeepsItsReleasedConstructorAndOmitsUnspecifiedAutoMetadata() {
        var model = new ProviderModelConfig("model", "saved", "wire", "base", "Display", 4096L, 8192L, 1024L, null,
                null, Map.of("kept", true));
        assertEquals("wire", model.wireModel());
        assertEquals(4096L, model.maxPromptTokens());
        assertEquals(Map.of("kept", true), model.metadata());
        assertNull(model.autoLocal());
        assertFalse(new ObjectMapper().valueToTree(model).has("autoLocal"));
    }

    @Test
    void canonicalRecordsPreservePublishedRuntimeOptionsAndAutoMetadata() throws Exception {
        var mapper = new ObjectMapper();
        var providerJson = mapper.readTree("""
                {"name":"managed","type":"winml","baseUrl":"model.gguf",
                 "localModel":{"ctxSize":8192,"args":["--jinja"],"futureOption":true}}
                """);
        var provider = mapper.treeToValue(providerJson, NamedProviderConfig.class);
        assertEquals(providerJson, mapper.valueToTree(provider));
        var modelJson = mapper.readTree("""
                {"id":"model","provider":"managed","autoLocal":{"capiId":"base","maxConcurrent":2,"future":true}}
                """);
        var model = mapper.treeToValue(modelJson, ProviderModelConfig.class);
        assertEquals(modelJson, mapper.valueToTree(model));
    }
}
