/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.sdk.json.CopilotClientOptions;
import com.github.copilot.sdk.json.CustomAgentConfig;
import com.github.copilot.sdk.json.InfiniteSessionConfig;
import com.github.copilot.sdk.json.InputOptions;
import com.github.copilot.sdk.json.ModelCapabilitiesOverride;
import com.github.copilot.sdk.json.ProviderConfig;
import com.github.copilot.sdk.json.ResumeSessionConfig;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SessionUiCapabilities;
import com.github.copilot.sdk.json.TelemetryConfig;
import com.github.copilot.sdk.json.UserInputRequest;
import org.junit.jupiter.api.Test;

/**
 * Validates that every {@code clearXxx()} method resets its field to absent,
 * that Optional-returning getters report the correct state, and that Jackson
 * omits cleared fields from serialized output.
 */
class OptionalApiAndJacksonTest {

    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

    // ── CopilotClientOptions ──────────────────────────────────────────

    @Test
    void copilotClientOptions_clearSessionIdleTimeoutSeconds() {
        var opts = new CopilotClientOptions();
        opts.setSessionIdleTimeoutSeconds(120);
        assertFalse(opts.getSessionIdleTimeoutSeconds().isEmpty());

        opts.clearSessionIdleTimeoutSeconds();
        assertTrue(opts.getSessionIdleTimeoutSeconds().isEmpty());
    }

    @Test
    void copilotClientOptions_clearUseLoggedInUser() {
        var opts = new CopilotClientOptions();
        opts.setUseLoggedInUser(true);
        assertTrue(opts.getUseLoggedInUser().isPresent());

        opts.clearUseLoggedInUser();
        assertTrue(opts.getUseLoggedInUser().isEmpty());
    }

    // ── SessionConfig ─────────────────────────────────────────────────

    @Test
    void sessionConfig_clearEnableSessionTelemetry() {
        var cfg = new SessionConfig();
        cfg.setEnableSessionTelemetry(true);
        assertTrue(cfg.getEnableSessionTelemetry().isPresent());

        cfg.clearEnableSessionTelemetry();
        assertTrue(cfg.getEnableSessionTelemetry().isEmpty());
    }

    @Test
    void sessionConfig_clearEnableConfigDiscovery() {
        var cfg = new SessionConfig();
        cfg.setEnableConfigDiscovery(false);
        assertTrue(cfg.getEnableConfigDiscovery().isPresent());

        cfg.clearEnableConfigDiscovery();
        assertTrue(cfg.getEnableConfigDiscovery().isEmpty());
    }

    @Test
    void sessionConfig_clearIncludeSubAgentStreamingEvents() {
        var cfg = new SessionConfig();
        cfg.setIncludeSubAgentStreamingEvents(true);
        assertTrue(cfg.getIncludeSubAgentStreamingEvents().isPresent());

        cfg.clearIncludeSubAgentStreamingEvents();
        assertTrue(cfg.getIncludeSubAgentStreamingEvents().isEmpty());
    }

    // ── ResumeSessionConfig ───────────────────────────────────────────

    @Test
    void resumeSessionConfig_clearEnableSessionTelemetry() {
        var cfg = new ResumeSessionConfig();
        cfg.setEnableSessionTelemetry(false);
        assertTrue(cfg.getEnableSessionTelemetry().isPresent());

        cfg.clearEnableSessionTelemetry();
        assertTrue(cfg.getEnableSessionTelemetry().isEmpty());
    }

    @Test
    void resumeSessionConfig_clearEnableConfigDiscovery() {
        var cfg = new ResumeSessionConfig();
        cfg.setEnableConfigDiscovery(true);
        assertTrue(cfg.getEnableConfigDiscovery().isPresent());

        cfg.clearEnableConfigDiscovery();
        assertTrue(cfg.getEnableConfigDiscovery().isEmpty());
    }

    @Test
    void resumeSessionConfig_clearIncludeSubAgentStreamingEvents() {
        var cfg = new ResumeSessionConfig();
        cfg.setIncludeSubAgentStreamingEvents(false);
        assertTrue(cfg.getIncludeSubAgentStreamingEvents().isPresent());

        cfg.clearIncludeSubAgentStreamingEvents();
        assertTrue(cfg.getIncludeSubAgentStreamingEvents().isEmpty());
    }

    // ── InfiniteSessionConfig ─────────────────────────────────────────

    @Test
    void infiniteSessionConfig_clearEnabled() {
        var cfg = new InfiniteSessionConfig();
        cfg.setEnabled(true);
        assertTrue(cfg.getEnabled().isPresent());

        cfg.clearEnabled();
        assertTrue(cfg.getEnabled().isEmpty());
    }

    @Test
    void infiniteSessionConfig_clearBackgroundCompactionThreshold() {
        var cfg = new InfiniteSessionConfig();
        cfg.setBackgroundCompactionThreshold(0.75);
        assertFalse(cfg.getBackgroundCompactionThreshold().isEmpty());

        cfg.clearBackgroundCompactionThreshold();
        assertTrue(cfg.getBackgroundCompactionThreshold().isEmpty());
    }

    @Test
    void infiniteSessionConfig_clearBufferExhaustionThreshold() {
        var cfg = new InfiniteSessionConfig();
        cfg.setBufferExhaustionThreshold(0.9);
        assertFalse(cfg.getBufferExhaustionThreshold().isEmpty());

        cfg.clearBufferExhaustionThreshold();
        assertTrue(cfg.getBufferExhaustionThreshold().isEmpty());
    }

    // ── InputOptions ──────────────────────────────────────────────────

    @Test
    void inputOptions_clearMinLength() {
        var opts = new InputOptions();
        opts.setMinLength(5);
        assertFalse(opts.getMinLength().isEmpty());

        opts.clearMinLength();
        assertTrue(opts.getMinLength().isEmpty());
    }

    @Test
    void inputOptions_clearMaxLength() {
        var opts = new InputOptions();
        opts.setMaxLength(100);
        assertFalse(opts.getMaxLength().isEmpty());

        opts.clearMaxLength();
        assertTrue(opts.getMaxLength().isEmpty());
    }

    // ── ModelCapabilitiesOverride.Supports ─────────────────────────────

    @Test
    void supports_clearVision() {
        var s = new ModelCapabilitiesOverride.Supports();
        s.setVision(true);
        assertTrue(s.getVision().isPresent());

        s.clearVision();
        assertTrue(s.getVision().isEmpty());
    }

    @Test
    void supports_clearReasoningEffort() {
        var s = new ModelCapabilitiesOverride.Supports();
        s.setReasoningEffort(false);
        assertTrue(s.getReasoningEffort().isPresent());

        s.clearReasoningEffort();
        assertTrue(s.getReasoningEffort().isEmpty());
    }

    // ── ModelCapabilitiesOverride.Limits ───────────────────────────────

    @Test
    void limits_clearMaxPromptTokens() {
        var l = new ModelCapabilitiesOverride.Limits();
        l.setMaxPromptTokens(4096);
        assertFalse(l.getMaxPromptTokens().isEmpty());

        l.clearMaxPromptTokens();
        assertTrue(l.getMaxPromptTokens().isEmpty());
    }

    @Test
    void limits_clearMaxOutputTokens() {
        var l = new ModelCapabilitiesOverride.Limits();
        l.setMaxOutputTokens(1024);
        assertFalse(l.getMaxOutputTokens().isEmpty());

        l.clearMaxOutputTokens();
        assertTrue(l.getMaxOutputTokens().isEmpty());
    }

    @Test
    void limits_clearMaxContextWindowTokens() {
        var l = new ModelCapabilitiesOverride.Limits();
        l.setMaxContextWindowTokens(16384);
        assertFalse(l.getMaxContextWindowTokens().isEmpty());

        l.clearMaxContextWindowTokens();
        assertTrue(l.getMaxContextWindowTokens().isEmpty());
    }

    // ── ProviderConfig ────────────────────────────────────────────────

    @Test
    void providerConfig_clearMaxPromptTokens() {
        var cfg = new ProviderConfig();
        cfg.setMaxPromptTokens(2048);
        assertFalse(cfg.getMaxPromptTokens().isEmpty());

        cfg.clearMaxPromptTokens();
        assertTrue(cfg.getMaxPromptTokens().isEmpty());
    }

    @Test
    void providerConfig_clearMaxOutputTokens() {
        var cfg = new ProviderConfig();
        cfg.setMaxOutputTokens(512);
        assertFalse(cfg.getMaxOutputTokens().isEmpty());

        cfg.clearMaxOutputTokens();
        assertTrue(cfg.getMaxOutputTokens().isEmpty());
    }

    // ── TelemetryConfig ───────────────────────────────────────────────

    @Test
    void telemetryConfig_clearCaptureContent() {
        var cfg = new TelemetryConfig();
        cfg.setCaptureContent(true);
        assertTrue(cfg.getCaptureContent().isPresent());

        cfg.clearCaptureContent();
        assertTrue(cfg.getCaptureContent().isEmpty());
    }

    // ── SessionUiCapabilities ─────────────────────────────────────────

    @Test
    void sessionUiCapabilities_clearElicitation() {
        var caps = new SessionUiCapabilities();
        caps.setElicitation(true);
        assertTrue(caps.getElicitation().isPresent());

        caps.clearElicitation();
        assertTrue(caps.getElicitation().isEmpty());
    }

    // ── CustomAgentConfig ─────────────────────────────────────────────

    @Test
    void customAgentConfig_clearInfer() {
        var cfg = new CustomAgentConfig();
        cfg.setInfer(true);
        assertTrue(cfg.getInfer().isPresent());

        cfg.clearInfer();
        assertTrue(cfg.getInfer().isEmpty());
    }

    // ── UserInputRequest ──────────────────────────────────────────────

    @Test
    void userInputRequest_clearAllowFreeform() {
        var req = new UserInputRequest();
        req.setAllowFreeform(false);
        assertTrue(req.getAllowFreeform().isPresent());

        req.clearAllowFreeform();
        assertTrue(req.getAllowFreeform().isEmpty());
    }

    // ── Jackson serialization roundtrip ───────────────────────────────

    // ── Jackson serialization roundtrip ───────────────────────────────
    //
    // Classes whose fields carry @JsonProperty (InfiniteSessionConfig,
    // ModelCapabilitiesOverride inner classes) are serialized via field
    // access: Jackson writes the field when set and omits it when cleared.
    //
    // Classes without @JsonProperty on fields (SessionConfig,
    // CopilotClientOptions, TelemetryConfig, ProviderConfig) are not
    // directly serialized — their values are copied to wire DTOs by
    // SessionRequestBuilder. The @JsonIgnore on their Optional-returning
    // getters prevents Jackson from attempting to serialize Optional
    // wrappers if the class is ever processed.

    @Test
    void jackson_infiniteSessionConfigClearedFieldsOmitted() throws Exception {
        var cfg = new InfiniteSessionConfig();
        cfg.setEnabled(true);
        cfg.setBackgroundCompactionThreshold(0.75);
        cfg.setBufferExhaustionThreshold(0.9);

        String withFields = MAPPER.writeValueAsString(cfg);
        assertTrue(withFields.contains("enabled"));
        assertTrue(withFields.contains("backgroundCompactionThreshold"));
        assertTrue(withFields.contains("bufferExhaustionThreshold"));

        cfg.clearEnabled();
        cfg.clearBackgroundCompactionThreshold();
        cfg.clearBufferExhaustionThreshold();

        String cleared = MAPPER.writeValueAsString(cfg);
        assertFalse(cleared.contains("enabled"));
        assertFalse(cleared.contains("backgroundCompactionThreshold"));
        assertFalse(cleared.contains("bufferExhaustionThreshold"));
    }

    @Test
    void jackson_modelCapabilitiesOverrideSupportsClearedFieldsOmitted() throws Exception {
        var supports = new ModelCapabilitiesOverride.Supports();
        supports.setVision(true);
        supports.setReasoningEffort(false);

        String withFields = MAPPER.writeValueAsString(supports);
        assertTrue(withFields.contains("vision"));
        assertTrue(withFields.contains("reasoningEffort"));

        supports.clearVision();
        supports.clearReasoningEffort();

        String cleared = MAPPER.writeValueAsString(supports);
        assertFalse(cleared.contains("vision"));
        assertFalse(cleared.contains("reasoningEffort"));
    }

    @Test
    void jackson_modelCapabilitiesOverrideLimitsClearedFieldsOmitted() throws Exception {
        var limits = new ModelCapabilitiesOverride.Limits();
        limits.setMaxPromptTokens(2048);
        limits.setMaxOutputTokens(512);
        limits.setMaxContextWindowTokens(16384);

        String withFields = MAPPER.writeValueAsString(limits);
        assertTrue(withFields.contains("max_prompt_tokens"));
        assertTrue(withFields.contains("max_output_tokens"));
        assertTrue(withFields.contains("max_context_window_tokens"));

        limits.clearMaxPromptTokens();
        limits.clearMaxOutputTokens();
        limits.clearMaxContextWindowTokens();

        String cleared = MAPPER.writeValueAsString(limits);
        assertFalse(cleared.contains("max_prompt_tokens"));
        assertFalse(cleared.contains("max_output_tokens"));
        assertFalse(cleared.contains("max_context_window_tokens"));
    }
}
