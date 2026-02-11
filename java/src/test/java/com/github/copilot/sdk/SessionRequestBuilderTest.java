/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.json.CreateSessionRequest;
import com.github.copilot.sdk.json.ResumeSessionConfig;
import com.github.copilot.sdk.json.ResumeSessionRequest;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SessionHooks;
import com.github.copilot.sdk.json.ToolDefinition;
import com.github.copilot.sdk.json.UserInputResponse;

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

    // =========================================================================
    // buildResumeRequest
    // =========================================================================

    @Test
    void testBuildResumeRequestNullConfig() {
        ResumeSessionRequest request = SessionRequestBuilder.buildResumeRequest("sid-1", null);
        assertEquals("sid-1", request.getSessionId());
        assertNull(request.getModel());
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
        var response = session.handleUserInputRequest(new com.github.copilot.sdk.json.UserInputRequest()).get();
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
}
