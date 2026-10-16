/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.copilot.rpc.ImageGenerationConfig;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;

class ImageGenerationConfigTest {
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
}
