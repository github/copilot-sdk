/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.AutoModeSwitchResponse;
import com.github.copilot.rpc.CloudSessionOptions;
import com.github.copilot.rpc.CloudSessionRepository;
import com.github.copilot.rpc.CreateSessionRequest;
import com.github.copilot.rpc.DefaultAgentConfig;
import com.github.copilot.rpc.ElicitationHandler;
import com.github.copilot.rpc.ElicitationResult;
import com.github.copilot.rpc.ElicitationResultAction;
import com.github.copilot.rpc.ExitPlanModeResult;
import com.github.copilot.rpc.LargeToolOutputConfig;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.ResumeSessionRequest;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SessionHooks;
import com.github.copilot.rpc.ToolDefinition;
import com.github.copilot.rpc.UserInputResponse;

/**
 * Unit tests for {@link SessionRequestBuilder} branch coverage.
 * <p>
 * Exercises branches in buildCreateRequest, buildResumeRequest, and
 * configureSession that are not reached by E2E tests.
 */
public class SessionRequestBuilderTest {

    // =========================================================================
    // buildCreateRequest
    // =========================================================================

    @Test
    void testBuildCreateRequestNullConfig() {
        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(null);
        assertNotNull(request);
        assertNull(request.getModel());
        assertTrue(request.getRequestPermission(), "requestPermission should be true even for null config");
        assertEquals("direct", request.getEnvValueMode(), "envValueMode should be 'direct' even for null config");
    }

    @Test
    void testBuildCreateRequestHooksNonNullButEmpty() {
        // Hooks object exists but hasHooks() returns false
        var config = new SessionConfig().setHooks(new SessionHooks());

        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);

        assertNull(request.getHooks(), "Should be null when hooks are empty");
    }

    @Test
    void testBuildCreateRequestHooksWithHandler() {
        var hooks = new SessionHooks().setOnPreToolUse((input, inv) -> CompletableFuture.completedFuture(null));
        var config = new SessionConfig().setHooks(hooks);

        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);

        assertTrue(request.getHooks(), "Should be true when hooks have handlers");
    }

    @Test
    void testBuildCreateRequestSetsEnvValueModeToDirect() {
        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(new SessionConfig());
        assertEquals("direct", request.getEnvValueMode());
    }

    @Test
    void testBuildCreateRequestAlwaysSetsRequestPermissionTrue() {
        // No permission handler set - requestPermission should still be true
        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(new SessionConfig());
        assertTrue(request.getRequestPermission(),
                "requestPermission should always be true to enable deny-by-default behavior");
    }

    @Test
    void testBuildCreateRequestSetsClientName() {
        var config = new SessionConfig().setClientName("my-app");
        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);
        assertEquals("my-app", request.getClientName());
    }

    @Test
    void testBuildCreateRequestSetsReasoningSummary() {
        var config = new SessionConfig().setReasoningSummary("concise");
        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);
        assertEquals("concise", request.getReasoningSummary());
    }

    @Test
    void testBuildCreateRequestSetsPluginDirectoriesAndLargeOutput() {
        var largeOutput = new LargeToolOutputConfig().setEnabled(true).setMaxSizeBytes(1024L)
                .setOutputDirectory("/tmp/out");
        var config = new SessionConfig().setPluginDirectories(List.of("/plugins/a")).setLargeOutput(largeOutput);
        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);
        assertEquals(List.of("/plugins/a"), request.getPluginDirectories());
        assertEquals(largeOutput, request.getLargeOutput());
    }

    @Test
    void testBuildCreateRequestForwardsEnableSessionTelemetryWhenFalse() {
        var config = new SessionConfig().setEnableSessionTelemetry(false);
        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);
        assertFalse(request.getEnableSessionTelemetry());
    }

    @Test
    void testBuildCreateRequestOmitsEnableSessionTelemetryWhenNotSet() {
        var config = new SessionConfig();
        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);
        assertNull(request.getEnableSessionTelemetry());
    }

    // =========================================================================
    // buildResumeRequest
    // =========================================================================

    @Test
    void testBuildResumeRequestNullConfig() {
        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-1", null);
        assertEquals("sid-1", request.getSessionId());
        assertNull(request.getModel());
        assertTrue(request.getRequestPermission(), "requestPermission should be true even for null config");
        assertEquals("direct", request.getEnvValueMode(), "envValueMode should be 'direct' even for null config");
    }

    @Test
    void testBuildResumeRequestForwardsEnableSessionTelemetryWhenFalse() {
        var config = new ResumeSessionConfig().setEnableSessionTelemetry(false);
        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-1", config);
        assertFalse(request.getEnableSessionTelemetry());
    }

    @Test
    void testBuildResumeRequestOmitsEnableSessionTelemetryWhenNotSet() {
        var config = new ResumeSessionConfig();
        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-1", config);
        assertNull(request.getEnableSessionTelemetry());
    }

    @Test
    void testBuildResumeRequestWithTools() {
        var tool = ToolDefinition.create("my_tool", "A tool", Map.of("type", "object"),
                inv -> CompletableFuture.completedFuture("result"));
        var config = new ResumeSessionConfig().setTools(List.of(tool));

        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-2", config);

        assertNotNull(request.getTools());
        assertEquals(1, request.getTools().size());
        assertEquals("my_tool", request.getTools().get(0).name());
    }

    @Test
    void testBuildResumeRequestWithUserInputHandler() {
        var config = new ResumeSessionConfig()
                .setOnUserInputRequest((req, inv) -> CompletableFuture.completedFuture(new UserInputResponse()));

        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-3", config);

        assertTrue(request.getRequestUserInput());
    }

    @Test
    void testBuildResumeRequestHooksNonNullButEmpty() {
        var config = new ResumeSessionConfig().setHooks(new SessionHooks());

        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-4", config);

        assertNull(request.getHooks(), "Should be null when hooks are empty");
    }

    @Test
    void testBuildResumeRequestHooksWithHandler() {
        var hooks = new SessionHooks().setOnSessionEnd((input, inv) -> CompletableFuture.completedFuture(null));
        var config = new ResumeSessionConfig().setHooks(hooks);

        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-5", config);

        assertTrue(request.getHooks(), "Should be true when hooks have handlers");
    }

    @Test
    void testBuildResumeRequestDisableResume() {
        var config = new ResumeSessionConfig().setDisableResume(true);

        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-6", config);

        assertTrue(request.getDisableResume());
    }

    @Test
    void testBuildResumeRequestStreaming() {
        var config = new ResumeSessionConfig().setStreaming(true);

        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-7", config);

        assertTrue(request.getStreaming());
    }

    @Test
    void testBuildResumeRequestSetsEnvValueModeToDirect() {
        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-8", new ResumeSessionConfig());
        assertEquals("direct", request.getEnvValueMode());
    }

    @Test
    void testBuildResumeRequestAlwaysSetsRequestPermissionTrue() {
        // No permission handler set - requestPermission should still be true
        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-9", new ResumeSessionConfig());
        assertTrue(request.getRequestPermission(),
                "requestPermission should always be true to enable deny-by-default behavior");
    }

    @Test
    void testBuildResumeRequestSetsClientName() {
        var config = new ResumeSessionConfig().setClientName("my-app");
        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-10", config);
        assertEquals("my-app", request.getClientName());
    }

    @Test
    void testBuildResumeRequestSetsReasoningSummary() {
        var config = new ResumeSessionConfig().setReasoningSummary("none");
        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-11", config);
        assertEquals("none", request.getReasoningSummary());
    }

    @Test
    void testBuildResumeRequestSetsPluginDirectoriesAndLargeOutput() {
        var largeOutput = new LargeToolOutputConfig().setEnabled(false).setMaxSizeBytes(2048L)
                .setOutputDirectory("/tmp/resume");
        var config = new ResumeSessionConfig().setPluginDirectories(List.of("/plugins/r")).setLargeOutput(largeOutput);
        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-12", config);
        assertEquals(List.of("/plugins/r"), request.getPluginDirectories());
        assertEquals(largeOutput, request.getLargeOutput());
    }

    // =========================================================================
    // configureSession (ResumeSessionConfig overload)
    // =========================================================================

    @Test
    void testConfigureResumeSessionNullConfig() throws Exception {
        var session = createTestSession();
        // Should not throw
        SessionRequestBuilder.configureSession(session, (ResumeSessionConfig) null);
    }

    @Test
    void testConfigureResumeSessionWithTools() throws Exception {
        var session = createTestSession();
        var tool = ToolDefinition.create("resume_tool", "desc", Map.of(),
                inv -> CompletableFuture.completedFuture("ok"));
        var config = new ResumeSessionConfig().setTools(List.of(tool));

        SessionRequestBuilder.configureSession(session, config);

        assertNotNull(session.getTool("resume_tool"));
    }

    @Test
    void testConfigureResumeSessionWithUserInputHandler() throws Exception {
        var session = createTestSession();
        var config = new ResumeSessionConfig()
                .setOnUserInputRequest((req, inv) -> CompletableFuture.completedFuture(new UserInputResponse()));

        SessionRequestBuilder.configureSession(session, config);

        // Handler was registered — verify by calling handleUserInputRequest
        // (package-private)
        var response = session.handleUserInputRequest(new com.github.copilot.rpc.UserInputRequest()).get();
        assertNotNull(response);
    }

    @Test
    void testConfigureResumeSessionWithHooks() throws Exception {
        var session = createTestSession();
        var hooks = new SessionHooks().setOnPreToolUse((input, inv) -> CompletableFuture.completedFuture(null));
        var config = new ResumeSessionConfig().setHooks(hooks);

        SessionRequestBuilder.configureSession(session, config);

        // Hooks registered — handleHooksInvoke should dispatch preToolUse
        var mapper = JsonRpcClient.getObjectMapper();
        var input = mapper.valueToTree(Map.of("toolName", "test_tool"));
        var result = session.handleHooksInvoke("preToolUse", input).get();
        assertNull(result); // handler returns null
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private CopilotSession createTestSession() throws Exception {
        var constructor = CopilotSession.class.getDeclaredConstructor(String.class, JsonRpcClient.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance("builder-test-session", null, null);
    }

    @Test
    void testBuildCreateRequestWithAgent() {
        var config = new SessionConfig().setAgent("my-agent");
        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config, "test-session-id");
        assertEquals("my-agent", request.getAgent());
    }

    @Test
    void testBuildResumeRequestWithAgent() {
        var config = new ResumeSessionConfig().setAgent("my-agent");
        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("session-id", config);
        assertEquals("my-agent", request.getAgent());
    }

    // =========================================================================
    // extractTransformCallbacks
    // =========================================================================

    @Test
    void extractTransformCallbacks_nullSystemMessage_returnsNull() {
        ExtractedTransforms result = SessionRequestBuilder.extractTransformCallbacks(null);
        assertNull(result.wireSystemMessage());
        assertNull(result.transformCallbacks());
    }

    @Test
    void extractTransformCallbacks_appendMode_returnsOriginalConfig() {
        var config = new com.github.copilot.rpc.SystemMessageConfig()
                .setMode(com.github.copilot.SystemMessageMode.APPEND).setContent("extra content");
        ExtractedTransforms result = SessionRequestBuilder.extractTransformCallbacks(config);
        assertSame(config, result.wireSystemMessage());
        assertNull(result.transformCallbacks());
    }

    @Test
    void extractTransformCallbacks_customizeModeNoTransforms_returnsOriginalConfig() {
        var sections = Map.of("tone", new com.github.copilot.rpc.SectionOverride()
                .setAction(com.github.copilot.rpc.SectionOverrideAction.REMOVE));
        var config = new com.github.copilot.rpc.SystemMessageConfig()
                .setMode(com.github.copilot.SystemMessageMode.CUSTOMIZE).setSections(sections);
        ExtractedTransforms result = SessionRequestBuilder.extractTransformCallbacks(config);
        assertSame(config, result.wireSystemMessage());
        assertNull(result.transformCallbacks());
    }

    @Test
    void extractTransformCallbacks_customizeModeWithTransform_extractsCallbacks() {
        var transformFn = (java.util.function.Function<String, CompletableFuture<String>>) content -> CompletableFuture
                .completedFuture(content + " modified");
        var sections = Map.of("identity", new com.github.copilot.rpc.SectionOverride().setTransform(transformFn));
        var config = new com.github.copilot.rpc.SystemMessageConfig()
                .setMode(com.github.copilot.SystemMessageMode.CUSTOMIZE).setSections(sections);

        ExtractedTransforms result = SessionRequestBuilder.extractTransformCallbacks(config);

        // Wire config should be different from original
        assertNotSame(config, result.wireSystemMessage());
        // Callbacks should be extracted
        assertNotNull(result.transformCallbacks());
        assertTrue(result.transformCallbacks().containsKey("identity"));
        // Wire config should have transform action instead of callback
        assertNotNull(result.wireSystemMessage().getSections());
        var wireSection = result.wireSystemMessage().getSections().get("identity");
        assertNotNull(wireSection);
        assertEquals(com.github.copilot.rpc.SectionOverrideAction.TRANSFORM, wireSection.getAction());
        assertNull(wireSection.getTransform());
    }

    @Test
    @SuppressWarnings("deprecation")
    void buildCreateRequestWithSessionId_usesProvidedSessionId() {
        var config = new SessionConfig();
        config.setSessionId("my-session-id");

        // The deprecated single-arg overload uses the sessionId from config when set
        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);

        assertEquals("my-session-id", request.getSessionId());
    }

    @Test
    void configureSessionWithNullConfig_returnsEarly() {
        // configureSession with null config should return without error
        CopilotSession session = new CopilotSession("session-1", null);
        // Covers the null config early-return branch (L219-220)
        assertDoesNotThrow(() -> SessionRequestBuilder.configureSession(session, (SessionConfig) null));
    }

    @Test
    void configureSessionWithCommands_registersCommands() {
        CopilotSession session = new CopilotSession("session-1", null);

        var cmd = new com.github.copilot.rpc.CommandDefinition().setName("deploy")
                .setHandler(ctx -> CompletableFuture.completedFuture(null));
        var config = new SessionConfig().setCommands(List.of(cmd));

        // Covers config.getCommands() != null branch (L235-236)
        SessionRequestBuilder.configureSession(session, config);
        // If no exception thrown, the branch was covered
    }

    @Test
    void configureSessionWithElicitationHandler_registersHandler() {
        CopilotSession session = new CopilotSession("session-1", null);

        ElicitationHandler handler = (context) -> CompletableFuture
                .completedFuture(new ElicitationResult().setAction(ElicitationResultAction.CANCEL));
        var config = new SessionConfig().setOnElicitationRequest(handler);

        // Covers config.getOnElicitationRequest() != null branch (L238-239)
        SessionRequestBuilder.configureSession(session, config);
    }

    @Test
    void configureSessionWithOnEvent_registersEventHandler() {
        CopilotSession session = new CopilotSession("session-1", null);

        var config = new SessionConfig().setOnEvent(event -> {
        });

        // Covers config.getOnEvent() != null branch (L241-242)
        SessionRequestBuilder.configureSession(session, config);
    }

    @Test
    void configureResumedSessionWithCommands_registersCommands() {
        CopilotSession session = new CopilotSession("session-1", null);

        var cmd = new com.github.copilot.rpc.CommandDefinition().setName("rollback")
                .setHandler(ctx -> CompletableFuture.completedFuture(null));
        var config = new ResumeSessionConfig().setCommands(List.of(cmd));

        // Covers ResumeSessionConfig.getCommands() != null branch (L271-272)
        SessionRequestBuilder.configureSession(session, config);
    }

    @Test
    void configureResumedSessionWithElicitationHandler_registersHandler() {
        CopilotSession session = new CopilotSession("session-1", null);

        ElicitationHandler handler = (context) -> CompletableFuture
                .completedFuture(new ElicitationResult().setAction(ElicitationResultAction.CANCEL));
        var config = new ResumeSessionConfig().setOnElicitationRequest(handler);

        // Covers ResumeSessionConfig.getOnElicitationRequest() != null branch
        // (L274-275)
        SessionRequestBuilder.configureSession(session, config);
    }

    @Test
    void configureResumedSessionWithOnEvent_registersEventHandler() {
        CopilotSession session = new CopilotSession("session-1", null);

        var config = new ResumeSessionConfig().setOnEvent(event -> {
        });

        // Covers ResumeSessionConfig.getOnEvent() != null branch (L277-278)
        SessionRequestBuilder.configureSession(session, config);
    }

    @Test
    void testBuildCreateRequestWithDefaultAgent() {
        var defaultAgent = new DefaultAgentConfig().setExcludedTools(List.of("secret_tool"));
        var config = new SessionConfig().setDefaultAgent(defaultAgent);

        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);

        assertNotNull(request.getDefaultAgent());
        assertEquals(List.of("secret_tool"), request.getDefaultAgent().getExcludedTools());
    }

    @Test
    void testBuildCreateRequestWithGitHubToken() {
        var config = new SessionConfig().setGitHubToken("ghp_per_session_token");

        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);

        assertEquals("ghp_per_session_token", request.getGitHubToken());
    }

    @Test
    void testBuildResumeRequestWithDefaultAgent() {
        var defaultAgent = new DefaultAgentConfig().setExcludedTools(List.of("secret_tool"));
        var config = new ResumeSessionConfig().setDefaultAgent(defaultAgent);

        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("test-session", config);

        assertNotNull(request.getDefaultAgent());
        assertEquals(List.of("secret_tool"), request.getDefaultAgent().getExcludedTools());
    }

    @Test
    void testBuildResumeRequestWithGitHubToken() {
        var config = new ResumeSessionConfig().setGitHubToken("ghp_per_session_token");

        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("test-session", config);

        assertEquals("ghp_per_session_token", request.getGitHubToken());
    }

    // =========================================================================
    // instructionDirectories propagation
    // =========================================================================

    @Test
    void testBuildCreateRequestPropagatesInstructionDirectories() {
        var dirs = List.of("/path/to/instructions", "/another/path");
        var config = new SessionConfig().setInstructionDirectories(dirs);

        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);

        assertEquals(dirs, request.getInstructionDirectories());
    }

    @Test
    void testBuildResumeRequestPropagatesInstructionDirectories() {
        var dirs = List.of("/resume/instructions", "/other/dir");
        var config = new ResumeSessionConfig().setInstructionDirectories(dirs);

        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-inst", config);

        assertEquals(dirs, request.getInstructionDirectories());
    }

    // =========================================================================
    // enableSessionTelemetry serialization
    // =========================================================================

    @Test
    void testCreateRequestSerializesEnableSessionTelemetryWhenFalse() throws Exception {
        var config = new SessionConfig().setEnableSessionTelemetry(false);
        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);
        var mapper = JsonRpcClient.getObjectMapper();
        var json = mapper.writeValueAsString(request);
        assertTrue(json.contains("\"enableSessionTelemetry\":false"),
                "enableSessionTelemetry should be serialized when set to false");
    }

    @Test
    void testCreateRequestOmitsEnableSessionTelemetryWhenNull() throws Exception {
        var config = new SessionConfig();
        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);
        var mapper = JsonRpcClient.getObjectMapper();
        var json = mapper.writeValueAsString(request);
        assertFalse(json.contains("enableSessionTelemetry"), "enableSessionTelemetry should be omitted when null");
    }

    @Test
    void testResumeRequestSerializesEnableSessionTelemetryWhenFalse() throws Exception {
        var config = new ResumeSessionConfig().setEnableSessionTelemetry(false);
        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-tel", config);
        var mapper = JsonRpcClient.getObjectMapper();
        var json = mapper.writeValueAsString(request);
        assertTrue(json.contains("\"enableSessionTelemetry\":false"),
                "enableSessionTelemetry should be serialized when set to false");
    }

    @Test
    void testResumeRequestOmitsEnableSessionTelemetryWhenNull() throws Exception {
        var config = new ResumeSessionConfig();
        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-tel", config);
        var mapper = JsonRpcClient.getObjectMapper();
        var json = mapper.writeValueAsString(request);
        assertFalse(json.contains("enableSessionTelemetry"), "enableSessionTelemetry should be omitted when null");
    }

    // =========================================================================
    // Mode handler request flags
    // =========================================================================

    @Test
    void testBuildCreateRequestWithExitPlanModeHandler() {
        var config = new SessionConfig().setOnExitPlanMode(
                (request, invocation) -> CompletableFuture.completedFuture(new ExitPlanModeResult()));

        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);

        assertTrue(request.getRequestExitPlanMode());
    }

    @Test
    void testBuildCreateRequestWithAutoModeSwitchHandler() {
        var config = new SessionConfig().setOnAutoModeSwitch(
                (request, invocation) -> CompletableFuture.completedFuture(AutoModeSwitchResponse.NO));

        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);

        assertTrue(request.getRequestAutoModeSwitch());
    }

    @Test
    void testBuildCreateRequestWithoutModeHandlers() {
        var config = new SessionConfig();

        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);

        assertNull(request.getRequestExitPlanMode());
        assertNull(request.getRequestAutoModeSwitch());
    }

    @Test
    void testBuildResumeRequestWithExitPlanModeHandler() {
        var config = new ResumeSessionConfig().setOnExitPlanMode(
                (request, invocation) -> CompletableFuture.completedFuture(new ExitPlanModeResult()));

        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("session-1", config);

        assertTrue(request.getRequestExitPlanMode());
    }

    @Test
    void testBuildResumeRequestWithAutoModeSwitchHandler() {
        var config = new ResumeSessionConfig().setOnAutoModeSwitch(
                (request, invocation) -> CompletableFuture.completedFuture(AutoModeSwitchResponse.NO));

        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("session-1", config);

        assertTrue(request.getRequestAutoModeSwitch());
    }

    @Test
    void configureSessionWithExitPlanModeHandler_registersHandler() {
        CopilotSession session = new CopilotSession("session-1", null);

        var config = new SessionConfig().setOnExitPlanMode(
                (request, invocation) -> CompletableFuture.completedFuture(new ExitPlanModeResult()));

        SessionRequestBuilder.configureSession(session, config);
    }

    @Test
    void configureSessionWithAutoModeSwitchHandler_registersHandler() {
        CopilotSession session = new CopilotSession("session-1", null);

        var config = new SessionConfig().setOnAutoModeSwitch(
                (request, invocation) -> CompletableFuture.completedFuture(AutoModeSwitchResponse.NO));

        SessionRequestBuilder.configureSession(session, config);
    }

    @Test
    void configureResumedSessionWithExitPlanModeHandler_registersHandler() {
        CopilotSession session = new CopilotSession("session-1", null);

        var config = new ResumeSessionConfig().setOnExitPlanMode(
                (request, invocation) -> CompletableFuture.completedFuture(new ExitPlanModeResult()));

        SessionRequestBuilder.configureSession(session, config);
    }

    @Test
    void configureResumedSessionWithAutoModeSwitchHandler_registersHandler() {
        CopilotSession session = new CopilotSession("session-1", null);

        var config = new ResumeSessionConfig().setOnAutoModeSwitch(
                (request, invocation) -> CompletableFuture.completedFuture(AutoModeSwitchResponse.NO));

        SessionRequestBuilder.configureSession(session, config);
    }

    @Test
    void testCreateRequestSerializesModeFlags() throws Exception {
        var config = new SessionConfig()
                .setOnExitPlanMode((r, i) -> CompletableFuture.completedFuture(new ExitPlanModeResult()))
                .setOnAutoModeSwitch((r, i) -> CompletableFuture.completedFuture(AutoModeSwitchResponse.NO));

        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);
        var mapper = JsonRpcClient.getObjectMapper();
        var json = mapper.writeValueAsString(request);

        assertTrue(json.contains("\"requestExitPlanMode\":true"));
        assertTrue(json.contains("\"requestAutoModeSwitch\":true"));
    }

    @Test
    void testResumeRequestSerializesModeFlags() throws Exception {
        var config = new ResumeSessionConfig()
                .setOnExitPlanMode((r, i) -> CompletableFuture.completedFuture(new ExitPlanModeResult()))
                .setOnAutoModeSwitch((r, i) -> CompletableFuture.completedFuture(AutoModeSwitchResponse.NO));

        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("session-1", config);
        var mapper = JsonRpcClient.getObjectMapper();
        var json = mapper.writeValueAsString(request);

        assertTrue(json.contains("\"requestExitPlanMode\":true"));
        assertTrue(json.contains("\"requestAutoModeSwitch\":true"));
    }

    // =========================================================================
    // Cloud session options wiring
    // =========================================================================

    @Test
    void testBuildCreateRequestPropagatesCloudSessionOptions() throws Exception {
        var cloud = new CloudSessionOptions()
                .setRepository(new CloudSessionRepository().setOwner("my-org").setName("my-repo").setBranch("main"));
        var config = new SessionConfig().setCloud(cloud);

        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);

        assertNotNull(request.getCloud());
        assertEquals("my-org", request.getCloud().getRepository().getOwner());
        assertEquals("my-repo", request.getCloud().getRepository().getName());
        assertEquals("main", request.getCloud().getRepository().getBranch());
    }

    @Test
    void testBuildCreateRequestOmitsCloudWhenNull() throws Exception {
        var config = new SessionConfig();

        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);
        var mapper = JsonRpcClient.getObjectMapper();
        var json = mapper.writeValueAsString(request);

        assertNull(request.getCloud());
        assertFalse(json.contains("\"cloud\""), "cloud should be omitted when null");
    }

    @Test
    void testCloudSessionOptionsSerializesCorrectly() throws Exception {
        var cloud = new CloudSessionOptions()
                .setRepository(new CloudSessionRepository().setOwner("acme").setName("widgets").setBranch("feature-1"));
        var config = new SessionConfig().setCloud(cloud);

        CreateSessionRequest request = SessionRequestBuilder.buildCreateRequest(config);
        var mapper = JsonRpcClient.getObjectMapper();
        var json = mapper.writeValueAsString(request);

        assertTrue(json.contains("\"cloud\""));
        assertTrue(json.contains("\"owner\":\"acme\""));
        assertTrue(json.contains("\"name\":\"widgets\""));
        assertTrue(json.contains("\"branch\":\"feature-1\""));
    }
}
