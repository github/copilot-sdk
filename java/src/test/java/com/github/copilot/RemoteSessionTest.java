/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.rpc.CreateSessionRequest;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.ResumeSessionRequest;
import com.github.copilot.rpc.SessionConfig;

/**
 * Tests for the {@code remoteSession} feature across all session config types.
 * <p>
 * Validates the complete lifecycle of the remote session mode:
 * <ul>
 * <li>Getter/setter and fluent chaining on {@link SessionConfig} and
 * {@link ResumeSessionConfig}</li>
 * <li>Propagation through {@link SessionRequestBuilder} into
 * {@link CreateSessionRequest} and {@link ResumeSessionRequest}</li>
 * <li>JSON wire-format serialization: correct key, correct value, omission when
 * unset</li>
 * <li>Defensive copy via {@code copy()} preserves the value</li>
 * <li>All three supported mode values ("off", "export", "on") are transmitted
 * correctly</li>
 * </ul>
 */
class RemoteSessionTest {

    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

    // =========================================================================
    // SessionConfig getter/setter/copy
    // =========================================================================

    @Test
    void sessionConfig_remoteSessionDefaultsToNull() {
        var cfg = new SessionConfig();
        assertNull(cfg.getRemoteSession(), "remoteSession should be null when not set");
    }

    @ParameterizedTest
    @ValueSource(strings = {"off", "export", "on"})
    void sessionConfig_setRemoteSessionReturnsSelf(String mode) {
        var cfg = new SessionConfig();
        SessionConfig result = cfg.setRemoteSession(mode);
        assertSame(cfg, result, "setRemoteSession should return the same instance for chaining");
        assertEquals(mode, cfg.getRemoteSession());
    }

    @Test
    void sessionConfig_copyPreservesRemoteSession() {
        var original = new SessionConfig().setRemoteSession("export");
        var copy = original.clone();
        assertEquals("export", copy.getRemoteSession());
    }

    @Test
    void sessionConfig_copyPreservesNullRemoteSession() {
        var original = new SessionConfig();
        var copy = original.clone();
        assertNull(copy.getRemoteSession());
    }

    @Test
    void sessionConfig_setRemoteSessionToNullClearsValue() {
        var cfg = new SessionConfig().setRemoteSession("on");
        cfg.setRemoteSession(null);
        assertNull(cfg.getRemoteSession());
    }

    // =========================================================================
    // ResumeSessionConfig getter/setter/copy
    // =========================================================================

    @Test
    void resumeSessionConfig_remoteSessionDefaultsToNull() {
        var cfg = new ResumeSessionConfig();
        assertNull(cfg.getRemoteSession(), "remoteSession should be null when not set");
    }

    @ParameterizedTest
    @ValueSource(strings = {"off", "export", "on"})
    void resumeSessionConfig_setRemoteSessionReturnsSelf(String mode) {
        var cfg = new ResumeSessionConfig();
        ResumeSessionConfig result = cfg.setRemoteSession(mode);
        assertSame(cfg, result, "setRemoteSession should return the same instance for chaining");
        assertEquals(mode, cfg.getRemoteSession());
    }

    @Test
    void resumeSessionConfig_copyPreservesRemoteSession() {
        var original = new ResumeSessionConfig().setRemoteSession("on");
        var copy = original.clone();
        assertEquals("on", copy.getRemoteSession());
    }

    // =========================================================================
    // SessionRequestBuilder – CreateSessionRequest wiring
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {"off", "export", "on"})
    void buildCreateRequest_propagatesRemoteSession(String mode) {
        var config = new SessionConfig().setRemoteSession(mode);
        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);
        assertEquals(mode, request.getRemoteSession());
    }

    @Test
    void buildCreateRequest_nullConfig_remoteSessionIsNull() {
        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(null);
        assertNull(request.getRemoteSession());
    }

    @Test
    void buildCreateRequest_unsetRemoteSession_isNull() {
        var config = new SessionConfig();
        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);
        assertNull(request.getRemoteSession());
    }

    // =========================================================================
    // SessionRequestBuilder – ResumeSessionRequest wiring
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {"off", "export", "on"})
    void buildResumeRequest_propagatesRemoteSession(String mode) {
        var config = new ResumeSessionConfig().setRemoteSession(mode);
        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-1", config);
        assertEquals(mode, request.getRemoteSession());
    }

    @Test
    void buildResumeRequest_nullConfig_remoteSessionIsNull() {
        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-1", null);
        assertNull(request.getRemoteSession());
    }

    @Test
    void buildResumeRequest_unsetRemoteSession_isNull() {
        var config = new ResumeSessionConfig();
        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-1", config);
        assertNull(request.getRemoteSession());
    }

    // =========================================================================
    // JSON wire-format: CreateSessionRequest
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {"off", "export", "on"})
    void createRequest_serializesRemoteSessionCorrectly(String mode) throws Exception {
        var config = new SessionConfig().setRemoteSession(mode);
        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);

        String json = MAPPER.writeValueAsString(request);
        JsonNode tree = MAPPER.readTree(json);

        assertTrue(tree.has("remoteSession"), "Serialized JSON should contain 'remoteSession' field for mode: " + mode);
        assertEquals(mode, tree.get("remoteSession").asText());
    }

    @Test
    void createRequest_omitsRemoteSessionWhenNull() throws Exception {
        var config = new SessionConfig();
        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);

        String json = MAPPER.writeValueAsString(request);
        JsonNode tree = MAPPER.readTree(json);

        assertFalse(tree.has("remoteSession"), "Serialized JSON should omit 'remoteSession' when not set");
    }

    // =========================================================================
    // JSON wire-format: ResumeSessionRequest
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {"off", "export", "on"})
    void resumeRequest_serializesRemoteSessionCorrectly(String mode) throws Exception {
        var config = new ResumeSessionConfig().setRemoteSession(mode);
        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-1", config);

        String json = MAPPER.writeValueAsString(request);
        JsonNode tree = MAPPER.readTree(json);

        assertTrue(tree.has("remoteSession"), "Serialized JSON should contain 'remoteSession' field for mode: " + mode);
        assertEquals(mode, tree.get("remoteSession").asText());
    }

    @Test
    void resumeRequest_omitsRemoteSessionWhenNull() throws Exception {
        var config = new ResumeSessionConfig();
        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-1", config);

        String json = MAPPER.writeValueAsString(request);
        JsonNode tree = MAPPER.readTree(json);

        assertFalse(tree.has("remoteSession"), "Serialized JSON should omit 'remoteSession' when not set");
    }

    // =========================================================================
    // JSON round-trip: CreateSessionRequest
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {"off", "export", "on"})
    void createRequest_roundTripsRemoteSession(String mode) throws Exception {
        var config = new SessionConfig().setRemoteSession(mode);
        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);

        String json = MAPPER.writeValueAsString(request);
        CreateSessionRequest deserialized = MAPPER.readValue(json, CreateSessionRequest.class);
        assertEquals(mode, deserialized.getRemoteSession());
    }

    @Test
    void createRequest_roundTripsNullRemoteSession() throws Exception {
        var config = new SessionConfig();
        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);

        String json = MAPPER.writeValueAsString(request);
        CreateSessionRequest deserialized = MAPPER.readValue(json, CreateSessionRequest.class);
        assertNull(deserialized.getRemoteSession());
    }

    // =========================================================================
    // JSON round-trip: ResumeSessionRequest
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {"off", "export", "on"})
    void resumeRequest_roundTripsRemoteSession(String mode) throws Exception {
        var config = new ResumeSessionConfig().setRemoteSession(mode);
        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-1", config);

        String json = MAPPER.writeValueAsString(request);
        ResumeSessionRequest deserialized = MAPPER.readValue(json, ResumeSessionRequest.class);
        assertEquals(mode, deserialized.getRemoteSession());
    }

    // =========================================================================
    // Fluent chaining: remoteSession composes with other config options
    // =========================================================================

    @Test
    void sessionConfig_remoteSessionComposesWithOtherFields() {
        var config = new SessionConfig().setModel("gpt-4o").setRemoteSession("export").setReasoningEffort("high");

        assertEquals("gpt-4o", config.getModel());
        assertEquals("export", config.getRemoteSession());
        assertEquals("high", config.getReasoningEffort());
    }

    @Test
    void resumeSessionConfig_remoteSessionComposesWithOtherFields() {
        var config = new ResumeSessionConfig().setModel("gpt-4o").setRemoteSession("on").setReasoningEffort("medium");

        assertEquals("gpt-4o", config.getModel());
        assertEquals("on", config.getRemoteSession());
        assertEquals("medium", config.getReasoningEffort());
    }

    @Test
    void createRequest_remoteSessionDoesNotAffectOtherFields() throws Exception {
        var config = new SessionConfig().setModel("gpt-4o").setRemoteSession("export").setReasoningEffort("high")
                .setGitHubToken("ghp_test");

        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);
        String json = MAPPER.writeValueAsString(request);
        JsonNode tree = MAPPER.readTree(json);

        assertEquals("export", tree.get("remoteSession").asText());
        assertEquals("gpt-4o", tree.get("model").asText());
        assertEquals("high", tree.get("reasoningEffort").asText());
        assertEquals("ghp_test", tree.get("gitHubToken").asText());
    }

    @Test
    void resumeRequest_remoteSessionDoesNotAffectOtherFields() throws Exception {
        var config = new ResumeSessionConfig().setModel("gpt-4o").setRemoteSession("on").setReasoningEffort("medium")
                .setGitHubToken("ghp_test");

        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-1", config);
        String json = MAPPER.writeValueAsString(request);
        JsonNode tree = MAPPER.readTree(json);

        assertEquals("on", tree.get("remoteSession").asText());
        assertEquals("gpt-4o", tree.get("model").asText());
        assertEquals("medium", tree.get("reasoningEffort").asText());
        assertEquals("ghp_test", tree.get("gitHubToken").asText());
    }

    // =========================================================================
    // Deserialization from raw JSON (simulates CLI response ingestion)
    // =========================================================================

    @Test
    void createRequest_deserializesRemoteSessionFromRawJson() throws Exception {
        String json = """
                {
                    "sessionId": "test-session",
                    "remoteSession": "export",
                    "model": "gpt-4o"
                }
                """;
        CreateSessionRequest request = MAPPER.readValue(json, CreateSessionRequest.class);
        assertEquals("export", request.getRemoteSession());
        assertEquals("test-session", request.getSessionId());
    }

    @Test
    void resumeRequest_deserializesRemoteSessionFromRawJson() throws Exception {
        String json = """
                {
                    "sessionId": "resume-session",
                    "remoteSession": "on",
                    "model": "gpt-4o"
                }
                """;
        ResumeSessionRequest request = MAPPER.readValue(json, ResumeSessionRequest.class);
        assertEquals("on", request.getRemoteSession());
        assertEquals("resume-session", request.getSessionId());
    }

    @Test
    void createRequest_deserializesWithMissingRemoteSession() throws Exception {
        String json = """
                {
                    "sessionId": "test-session",
                    "model": "gpt-4o"
                }
                """;
        CreateSessionRequest request = MAPPER.readValue(json, CreateSessionRequest.class);
        assertNull(request.getRemoteSession());
    }

    // =========================================================================
    // Handoff event with remoteSessionId (remote session lifecycle)
    // =========================================================================

    @Test
    void handoffEvent_withRemoteSourceType_containsRemoteSessionId() throws Exception {
        String json = """
                {
                    "type": "session.handoff",
                    "data": {
                        "handoffTime": "2025-06-01T12:00:00Z",
                        "sourceType": "remote",
                        "remoteSessionId": "remote-sess-42",
                        "summary": "Session exported for remote execution",
                        "repository": {
                            "owner": "test-org",
                            "name": "test-repo",
                            "branch": "feature-branch"
                        }
                    }
                }
                """;

        var event = (com.github.copilot.generated.SessionHandoffEvent) MAPPER.readValue(json,
                com.github.copilot.generated.SessionEvent.class);
        assertNotNull(event);
        var data = event.getData();
        assertEquals("remote-sess-42", data.remoteSessionId());
        assertEquals(com.github.copilot.generated.HandoffSourceType.REMOTE, data.sourceType());
        assertEquals("Session exported for remote execution", data.summary());
        assertEquals("test-org", data.repository().owner());
        assertEquals("test-repo", data.repository().name());
        assertEquals("feature-branch", data.repository().branch());
    }

    @Test
    void handoffEvent_withoutRemoteSessionId_fieldIsNull() throws Exception {
        String json = """
                {
                    "type": "session.handoff",
                    "data": {
                        "targetAgent": "local-agent"
                    }
                }
                """;

        var event = (com.github.copilot.generated.SessionHandoffEvent) MAPPER.readValue(json,
                com.github.copilot.generated.SessionEvent.class);
        assertNotNull(event);
        assertNull(event.getData().remoteSessionId());
    }
}
