/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.copilot.generated.rpc.ProtocolImageGenerationOptions;
import com.github.copilot.rpc.ImageGenerationConfig;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;

class ImageGenerationConfigTest {
    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {true, false})
    void protocolOptionsPreserveOneArgumentConstructor(Boolean enabled) {
        var options = new ProtocolImageGenerationOptions(enabled);
        assertEquals(enabled, options.enabled());
        assertNull(options.watermark());
        JsonNode json = JsonRpcClient.getObjectMapper().valueToTree(options);
        assertFalse(json.has("watermark"));
        if (enabled == null) {
            assertFalse(json.has("enabled"));
        } else {
            assertEquals(enabled.booleanValue(), json.get("enabled").booleanValue());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void protocolOptionsForwardExplicitWatermark(boolean watermark) {
        var options = new ProtocolImageGenerationOptions(null, watermark);
        assertEquals(watermark, options.watermark());
        JsonNode json = JsonRpcClient.getObjectMapper().valueToTree(options);
        assertEquals(watermark, json.get("watermark").booleanValue());
        assertFalse(json.has("enabled"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createAndResumeForwardExplicitOptIn(boolean enabled) {
        var imageGeneration = new ImageGenerationConfig().setEnabled(enabled);
        var createConfig = new SessionConfig().setImageGeneration(imageGeneration).clone();
        var resumeConfig = new ResumeSessionConfig().setImageGeneration(imageGeneration).clone();
        assertSame(imageGeneration, createConfig.getImageGeneration());
        assertSame(imageGeneration, resumeConfig.getImageGeneration());
        var create = SessionRequestBuilder.buildCreateRequest(createConfig, "image-test");
        var resume = SessionRequestBuilder.buildResumeRequest("image-test", resumeConfig);
        for (Object request : new Object[]{create, resume}) {
            JsonNode json = JsonRpcClient.getObjectMapper().valueToTree(request);
            assertEquals(enabled, json.get("imageGeneration").get("enabled").booleanValue());
            assertEquals(1, json.get("imageGeneration").size());
        }
    }

    @Test
    void createAndResumeOmitUnsetOptIn() {
        var create = SessionRequestBuilder.buildCreateRequest(new SessionConfig(), "image-test");
        var resume = SessionRequestBuilder.buildResumeRequest("image-test", new ResumeSessionConfig());
        for (Object request : new Object[]{create, resume}) {
            JsonNode json = JsonRpcClient.getObjectMapper().valueToTree(request);
            assertFalse(json.has("imageGeneration"));
        }
        JsonNode empty = JsonRpcClient.getObjectMapper().valueToTree(new ImageGenerationConfig());
        assertEquals(0, empty.size());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createAndResumeForwardWatermarkWithoutEnablingImages(boolean watermark) {
        var imageGeneration = new ImageGenerationConfig().setWatermark(watermark);
        assertEquals(watermark, imageGeneration.getWatermark());
        var create = SessionRequestBuilder
                .buildCreateRequest(new SessionConfig().setImageGeneration(imageGeneration).clone(), "image-test");
        var resume = SessionRequestBuilder.buildResumeRequest("image-test",
                new ResumeSessionConfig().setImageGeneration(imageGeneration).clone());
        for (Object request : new Object[]{create, resume}) {
            JsonNode json = JsonRpcClient.getObjectMapper().valueToTree(request);
            assertEquals(watermark, json.get("imageGeneration").get("watermark").booleanValue());
            assertEquals(1, json.get("imageGeneration").size());
        }
    }
}
