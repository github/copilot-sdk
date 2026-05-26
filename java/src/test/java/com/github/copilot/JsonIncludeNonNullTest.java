/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.CustomAgentConfig;
import com.github.copilot.rpc.InfiniteSessionConfig;
import com.github.copilot.rpc.InputOptions;
import com.github.copilot.rpc.ModelCapabilitiesOverride;
import com.github.copilot.rpc.ProviderConfig;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SessionUiCapabilities;
import com.github.copilot.rpc.TelemetryConfig;
import com.github.copilot.rpc.UserInputRequest;

/**
 * Verifies that public DTO classes in the {@code com.github.copilot.rpc}
 * package are annotated with {@code @JsonInclude(JsonInclude.Include.NON_NULL)}
 * so that null-valued fields are omitted during JSON serialization.
 */
class JsonIncludeNonNullTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // --- Annotation presence checks ---

    @Test
    void copilotClientOptionsHasNonNullAnnotation() {
        assertHasNonNullInclude(CopilotClientOptions.class);
    }

    @Test
    void sessionConfigHasNonNullAnnotation() {
        assertHasNonNullInclude(SessionConfig.class);
    }

    @Test
    void resumeSessionConfigHasNonNullAnnotation() {
        assertHasNonNullInclude(ResumeSessionConfig.class);
    }

    @Test
    void infiniteSessionConfigHasNonNullAnnotation() {
        assertHasNonNullInclude(InfiniteSessionConfig.class);
    }

    @Test
    void inputOptionsHasNonNullAnnotation() {
        assertHasNonNullInclude(InputOptions.class);
    }

    @Test
    void modelCapabilitiesOverrideHasNonNullAnnotation() {
        assertHasNonNullInclude(ModelCapabilitiesOverride.class);
    }

    @Test
    void providerConfigHasNonNullAnnotation() {
        assertHasNonNullInclude(ProviderConfig.class);
    }

    @Test
    void telemetryConfigHasNonNullAnnotation() {
        assertHasNonNullInclude(TelemetryConfig.class);
    }

    @Test
    void sessionUiCapabilitiesHasNonNullAnnotation() {
        assertHasNonNullInclude(SessionUiCapabilities.class);
    }

    @Test
    void customAgentConfigHasNonNullAnnotation() {
        assertHasNonNullInclude(CustomAgentConfig.class);
    }

    @Test
    void userInputRequestHasNonNullAnnotation() {
        assertHasNonNullInclude(UserInputRequest.class);
    }

    // --- Serialization tests: null fields are omitted ---

    @Test
    void inputOptionsOmitsNullFieldsInJson() throws JsonProcessingException {
        var opts = new InputOptions();
        String json = MAPPER.writeValueAsString(opts);
        assertEquals("{}", json, "All-null InputOptions should serialize to empty JSON");
    }

    @Test
    void telemetryConfigOmitsNullFieldsInJson() throws JsonProcessingException {
        var config = new TelemetryConfig();
        String json = MAPPER.writeValueAsString(config);
        assertEquals("{}", json, "All-null TelemetryConfig should serialize to empty JSON");
    }

    @Test
    void sessionUiCapabilitiesOmitsNullFieldsInJson() throws JsonProcessingException {
        var caps = new SessionUiCapabilities();
        String json = MAPPER.writeValueAsString(caps);
        assertEquals("{}", json, "All-null SessionUiCapabilities should serialize to empty JSON");
    }

    @Test
    void userInputRequestOmitsNullFieldsInJson() throws JsonProcessingException {
        var req = new UserInputRequest();
        String json = MAPPER.writeValueAsString(req);
        assertFalse(json.contains("null"), "UserInputRequest with no fields set should not contain 'null' values");
    }

    @Test
    void inputOptionsIncludesSetFieldsInJson() throws JsonProcessingException {
        var opts = new InputOptions();
        opts.setMinLength(5);
        opts.setMaxLength(100);
        String json = MAPPER.writeValueAsString(opts);
        assertTrue(json.contains("\"minLength\":5"), "Set minLength should appear in JSON");
        assertTrue(json.contains("\"maxLength\":100"), "Set maxLength should appear in JSON");
        assertFalse(json.contains("\"title\""), "Unset title should be omitted from JSON");
    }

    @Test
    void telemetryConfigIncludesSetFieldsInJson() throws JsonProcessingException {
        var config = new TelemetryConfig();
        config.setOtlpEndpoint("http://localhost:4318");
        String json = MAPPER.writeValueAsString(config);
        assertTrue(json.contains("\"otlpEndpoint\":\"http://localhost:4318\""),
                "Set otlpEndpoint should appear in JSON");
        assertFalse(json.contains("\"filePath\""), "Unset filePath should be omitted from JSON");
    }

    @Test
    void sessionUiCapabilitiesIncludesSetFieldsInJson() throws JsonProcessingException {
        var caps = new SessionUiCapabilities();
        caps.setElicitation(true);
        String json = MAPPER.writeValueAsString(caps);
        assertTrue(json.contains("\"elicitation\":true"), "Set elicitation should appear in JSON");
    }

    private void assertHasNonNullInclude(Class<?> clazz) {
        JsonInclude annotation = clazz.getAnnotation(JsonInclude.class);
        assertNotNull(annotation, clazz.getSimpleName() + " should be annotated with @JsonInclude");
        assertEquals(JsonInclude.Include.NON_NULL, annotation.value(),
                clazz.getSimpleName() + " @JsonInclude should use Include.NON_NULL");
    }

}
