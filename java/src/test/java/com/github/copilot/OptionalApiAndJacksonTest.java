/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    // ── Value retrieval through Optional getters ────────────────────────

    @Test
    void copilotClientOptions_sessionIdleTimeoutSecondsValue() {
        var opts = new CopilotClientOptions();
        assertTrue(opts.getSessionIdleTimeoutSeconds().isEmpty());

        opts.setSessionIdleTimeoutSeconds(300);
        assertEquals(300, opts.getSessionIdleTimeoutSeconds().getAsInt());

        opts.setSessionIdleTimeoutSeconds(0);
        assertTrue(opts.getSessionIdleTimeoutSeconds().isPresent());
        assertEquals(0, opts.getSessionIdleTimeoutSeconds().getAsInt());
    }

    @Test
    void copilotClientOptions_useLoggedInUserValue() {
        var opts = new CopilotClientOptions();
        assertTrue(opts.getUseLoggedInUser().isEmpty());

        opts.setUseLoggedInUser(true);
        assertEquals(Boolean.TRUE, opts.getUseLoggedInUser().get());

        opts.setUseLoggedInUser(false);
        assertEquals(Boolean.FALSE, opts.getUseLoggedInUser().get());
    }

    @Test
    void sessionConfig_enableSessionTelemetryValue() {
        var cfg = new SessionConfig();
        assertFalse(cfg.getEnableSessionTelemetry().orElse(false));

        cfg.setEnableSessionTelemetry(true);
        assertTrue(cfg.getEnableSessionTelemetry().orElse(false));

        cfg.setEnableSessionTelemetry(false);
        assertFalse(cfg.getEnableSessionTelemetry().orElse(true));
    }

    @Test
    void sessionConfig_enableConfigDiscoveryValue() {
        var cfg = new SessionConfig();
        assertTrue(cfg.getEnableConfigDiscovery().isEmpty());

        cfg.setEnableConfigDiscovery(true);
        assertTrue(cfg.getEnableConfigDiscovery().get());

        cfg.setEnableConfigDiscovery(false);
        assertFalse(cfg.getEnableConfigDiscovery().get());
    }

    @Test
    void sessionConfig_includeSubAgentStreamingEventsValue() {
        var cfg = new SessionConfig();
        assertTrue(cfg.getIncludeSubAgentStreamingEvents().isEmpty());

        cfg.setIncludeSubAgentStreamingEvents(true);
        assertTrue(cfg.getIncludeSubAgentStreamingEvents().get());
    }

    @Test
    void sessionConfig_granularMultitenancyFieldsValue() {
        var cfg = new SessionConfig();
        assertTrue(cfg.getSkipEmbeddingRetrieval().isEmpty());
        assertNull(cfg.getOrganizationCustomInstructions());
        assertTrue(cfg.getEnableOnDemandInstructionDiscovery().isEmpty());
        assertNull(cfg.getEmbeddingCacheStorage());
        assertTrue(cfg.getEnableFileHooks().isEmpty());
        assertTrue(cfg.getEnableHostGitOperations().isEmpty());
        assertTrue(cfg.getEnableSessionStore().isEmpty());
        assertTrue(cfg.getEnableSkills().isEmpty());

        cfg.setSkipEmbeddingRetrieval(true);
        cfg.setOrganizationCustomInstructions("Org instructions");
        cfg.setEnableOnDemandInstructionDiscovery(false);
        cfg.setEmbeddingCacheStorage("persistent");
        cfg.setEnableFileHooks(true);
        cfg.setEnableHostGitOperations(false);
        cfg.setEnableSessionStore(true);
        cfg.setEnableSkills(false);

        assertTrue(cfg.getSkipEmbeddingRetrieval().get());
        assertEquals("Org instructions", cfg.getOrganizationCustomInstructions());
        assertFalse(cfg.getEnableOnDemandInstructionDiscovery().get());
        assertEquals("persistent", cfg.getEmbeddingCacheStorage());
        assertTrue(cfg.getEnableFileHooks().get());
        assertFalse(cfg.getEnableHostGitOperations().get());
        assertTrue(cfg.getEnableSessionStore().get());
        assertFalse(cfg.getEnableSkills().get());
    }

    @Test
    void resumeSessionConfig_enableSessionTelemetryValue() {
        var cfg = new ResumeSessionConfig();
        assertTrue(cfg.getEnableSessionTelemetry().isEmpty());

        cfg.setEnableSessionTelemetry(true);
        assertTrue(cfg.getEnableSessionTelemetry().get());

        cfg.setEnableSessionTelemetry(false);
        assertFalse(cfg.getEnableSessionTelemetry().get());
    }

    @Test
    void resumeSessionConfig_enableConfigDiscoveryValue() {
        var cfg = new ResumeSessionConfig();
        assertTrue(cfg.getEnableConfigDiscovery().isEmpty());

        cfg.setEnableConfigDiscovery(true);
        assertTrue(cfg.getEnableConfigDiscovery().get());
    }

    @Test
    void resumeSessionConfig_includeSubAgentStreamingEventsValue() {
        var cfg = new ResumeSessionConfig();
        assertTrue(cfg.getIncludeSubAgentStreamingEvents().isEmpty());

        cfg.setIncludeSubAgentStreamingEvents(false);
        assertFalse(cfg.getIncludeSubAgentStreamingEvents().get());
    }

    @Test
    void resumeSessionConfig_granularMultitenancyFieldsValue() {
        var cfg = new ResumeSessionConfig();
        assertTrue(cfg.getSkipEmbeddingRetrieval().isEmpty());
        assertNull(cfg.getOrganizationCustomInstructions());
        assertTrue(cfg.getEnableOnDemandInstructionDiscovery().isEmpty());
        assertNull(cfg.getEmbeddingCacheStorage());
        assertTrue(cfg.getEnableFileHooks().isEmpty());
        assertTrue(cfg.getEnableHostGitOperations().isEmpty());
        assertTrue(cfg.getEnableSessionStore().isEmpty());
        assertTrue(cfg.getEnableSkills().isEmpty());

        cfg.setSkipEmbeddingRetrieval(false);
        cfg.setOrganizationCustomInstructions("Resume org instructions");
        cfg.setEnableOnDemandInstructionDiscovery(true);
        cfg.setEmbeddingCacheStorage("persistent");
        cfg.setEnableFileHooks(false);
        cfg.setEnableHostGitOperations(true);
        cfg.setEnableSessionStore(false);
        cfg.setEnableSkills(true);

        assertFalse(cfg.getSkipEmbeddingRetrieval().get());
        assertEquals("Resume org instructions", cfg.getOrganizationCustomInstructions());
        assertTrue(cfg.getEnableOnDemandInstructionDiscovery().get());
        assertEquals("persistent", cfg.getEmbeddingCacheStorage());
        assertFalse(cfg.getEnableFileHooks().get());
        assertTrue(cfg.getEnableHostGitOperations().get());
        assertFalse(cfg.getEnableSessionStore().get());
        assertTrue(cfg.getEnableSkills().get());
    }

    @Test
    void infiniteSessionConfig_thresholdValues() {
        var cfg = new InfiniteSessionConfig();
        assertTrue(cfg.getBackgroundCompactionThreshold().isEmpty());
        assertTrue(cfg.getBufferExhaustionThreshold().isEmpty());

        cfg.setBackgroundCompactionThreshold(0.6);
        cfg.setBufferExhaustionThreshold(0.85);
        assertEquals(0.6, cfg.getBackgroundCompactionThreshold().getAsDouble(), 0.001);
        assertEquals(0.85, cfg.getBufferExhaustionThreshold().getAsDouble(), 0.001);
    }

    @Test
    void infiniteSessionConfig_enabledValue() {
        var cfg = new InfiniteSessionConfig();
        assertTrue(cfg.getEnabled().isEmpty());

        cfg.setEnabled(true);
        assertTrue(cfg.getEnabled().get());

        cfg.setEnabled(false);
        assertFalse(cfg.getEnabled().get());
    }

    @Test
    void inputOptions_minAndMaxLengthValues() {
        var opts = new InputOptions();
        assertTrue(opts.getMinLength().isEmpty());
        assertTrue(opts.getMaxLength().isEmpty());

        opts.setMinLength(1);
        opts.setMaxLength(255);
        assertEquals(1, opts.getMinLength().getAsInt());
        assertEquals(255, opts.getMaxLength().getAsInt());
    }

    @Test
    void supports_visionAndReasoningEffortValues() {
        var s = new ModelCapabilitiesOverride.Supports();
        assertTrue(s.getVision().isEmpty());
        assertTrue(s.getReasoningEffort().isEmpty());

        s.setVision(true);
        s.setReasoningEffort(false);
        assertTrue(s.getVision().get());
        assertFalse(s.getReasoningEffort().get());
    }

    @Test
    void limits_tokenValues() {
        var l = new ModelCapabilitiesOverride.Limits();
        assertTrue(l.getMaxPromptTokens().isEmpty());
        assertTrue(l.getMaxOutputTokens().isEmpty());
        assertTrue(l.getMaxContextWindowTokens().isEmpty());

        l.setMaxPromptTokens(4096);
        l.setMaxOutputTokens(1024);
        l.setMaxContextWindowTokens(16384);
        assertEquals(4096, l.getMaxPromptTokens().getAsInt());
        assertEquals(1024, l.getMaxOutputTokens().getAsInt());
        assertEquals(16384, l.getMaxContextWindowTokens().getAsInt());
    }

    @Test
    void providerConfig_tokenValues() {
        var cfg = new ProviderConfig();
        assertTrue(cfg.getMaxPromptTokens().isEmpty());
        assertTrue(cfg.getMaxOutputTokens().isEmpty());

        cfg.setMaxPromptTokens(8192);
        cfg.setMaxOutputTokens(2048);
        assertEquals(8192, cfg.getMaxPromptTokens().getAsInt());
        assertEquals(2048, cfg.getMaxOutputTokens().getAsInt());
    }

    @Test
    void telemetryConfig_captureContentValue() {
        var cfg = new TelemetryConfig();
        assertTrue(cfg.getCaptureContent().isEmpty());

        cfg.setCaptureContent(true);
        assertTrue(cfg.getCaptureContent().get());

        cfg.setCaptureContent(false);
        assertFalse(cfg.getCaptureContent().get());
    }

    @Test
    void sessionUiCapabilities_elicitationValue() {
        var caps = new SessionUiCapabilities();
        assertTrue(caps.getElicitation().isEmpty());
        assertFalse(caps.getElicitation().orElse(false));

        caps.setElicitation(true);
        assertTrue(caps.getElicitation().orElse(false));
    }

    @Test
    void customAgentConfig_inferValue() {
        var cfg = new CustomAgentConfig();
        assertTrue(cfg.getInfer().isEmpty());

        cfg.setInfer(true);
        assertTrue(cfg.getInfer().get());

        cfg.setInfer(false);
        assertFalse(cfg.getInfer().get());
    }

    @Test
    void userInputRequest_allowFreeformValue() {
        var req = new UserInputRequest();
        assertTrue(req.getAllowFreeform().isEmpty());

        req.setAllowFreeform(true);
        assertTrue(req.getAllowFreeform().get());

        req.setAllowFreeform(false);
        assertFalse(req.getAllowFreeform().get());
    }

    // ── JSON deserialization into Optional-returning classes ───────────

    @Test
    void jackson_deserializeSupportsWithFields() throws Exception {
        String json = "{\"vision\":true,\"reasoningEffort\":false}";
        var supports = MAPPER.readValue(json, ModelCapabilitiesOverride.Supports.class);
        assertTrue(supports.getVision().get());
        assertFalse(supports.getReasoningEffort().get());
    }

    @Test
    void jackson_deserializeSupportsEmpty() throws Exception {
        String json = "{}";
        var supports = MAPPER.readValue(json, ModelCapabilitiesOverride.Supports.class);
        assertTrue(supports.getVision().isEmpty());
        assertTrue(supports.getReasoningEffort().isEmpty());
    }

    @Test
    void jackson_deserializeLimitsWithFields() throws Exception {
        String json = "{\"max_prompt_tokens\":4096,\"max_output_tokens\":1024,\"max_context_window_tokens\":16384}";
        var limits = MAPPER.readValue(json, ModelCapabilitiesOverride.Limits.class);
        assertEquals(4096, limits.getMaxPromptTokens().getAsInt());
        assertEquals(1024, limits.getMaxOutputTokens().getAsInt());
        assertEquals(16384, limits.getMaxContextWindowTokens().getAsInt());
    }

    @Test
    void jackson_deserializeLimitsEmpty() throws Exception {
        String json = "{}";
        var limits = MAPPER.readValue(json, ModelCapabilitiesOverride.Limits.class);
        assertTrue(limits.getMaxPromptTokens().isEmpty());
        assertTrue(limits.getMaxOutputTokens().isEmpty());
        assertTrue(limits.getMaxContextWindowTokens().isEmpty());
    }

    @Test
    void jackson_deserializeInfiniteSessionConfigWithFields() throws Exception {
        String json = "{\"enabled\":true,\"backgroundCompactionThreshold\":0.7,\"bufferExhaustionThreshold\":0.9}";
        var cfg = MAPPER.readValue(json, InfiniteSessionConfig.class);
        assertTrue(cfg.getEnabled().get());
        assertEquals(0.7, cfg.getBackgroundCompactionThreshold().getAsDouble(), 0.001);
        assertEquals(0.9, cfg.getBufferExhaustionThreshold().getAsDouble(), 0.001);
    }

    @Test
    void jackson_deserializeInfiniteSessionConfigEmpty() throws Exception {
        String json = "{}";
        var cfg = MAPPER.readValue(json, InfiniteSessionConfig.class);
        assertTrue(cfg.getEnabled().isEmpty());
        assertTrue(cfg.getBackgroundCompactionThreshold().isEmpty());
        assertTrue(cfg.getBufferExhaustionThreshold().isEmpty());
    }

    // ── Jackson serialization roundtrip ───────────────────────────────
    //
    // Classes whose fields carry @JsonProperty (InfiniteSessionConfig,
    // ModelCapabilitiesOverride inner classes) are serialized via field
    // access: Jackson writes the field when set and omits it when cleared.
    //
    // Classes without @JsonProperty on fields (SessionConfig,
    // CopilotClientOptions, TelemetryConfig, ProviderConfig) are normally
    // copied to wire DTOs by SessionRequestBuilder. Their scalar getters can
    // still be serialized directly, while @JsonIgnore on Optional-returning
    // getters prevents Jackson from attempting to serialize Optional wrappers.

    @Test
    void jackson_sessionConfigEmbeddingCacheStorageSerialized() throws Exception {
        var cfg = new SessionConfig();
        cfg.setEmbeddingCacheStorage("persistent");

        String withField = MAPPER.writeValueAsString(cfg);
        assertTrue(withField.contains("\"embeddingCacheStorage\":\"persistent\""));

        cfg.clearEmbeddingCacheStorage();

        String cleared = MAPPER.writeValueAsString(cfg);
        assertFalse(cleared.contains("embeddingCacheStorage"));
    }

    @Test
    void jackson_resumeSessionConfigEmbeddingCacheStorageSerialized() throws Exception {
        var cfg = new ResumeSessionConfig();
        cfg.setEmbeddingCacheStorage("persistent");

        String withField = MAPPER.writeValueAsString(cfg);
        assertTrue(withField.contains("\"embeddingCacheStorage\":\"persistent\""));

        cfg.clearEmbeddingCacheStorage();

        String cleared = MAPPER.writeValueAsString(cfg);
        assertFalse(cleared.contains("embeddingCacheStorage"));
    }

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
