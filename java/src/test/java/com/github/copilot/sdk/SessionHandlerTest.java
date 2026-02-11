/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.sdk.json.PermissionRequestResult;
import com.github.copilot.sdk.json.SessionEndHookOutput;
import com.github.copilot.sdk.json.SessionHooks;
import com.github.copilot.sdk.json.SessionStartHookOutput;
import com.github.copilot.sdk.json.ToolDefinition;
import com.github.copilot.sdk.json.UserInputRequest;
import com.github.copilot.sdk.json.UserInputResponse;
import com.github.copilot.sdk.json.UserPromptSubmittedHookOutput;

/**
 * Unit tests for CopilotSession internal handler methods.
 * <p>
 * Tests package-private handler and hook dispatch logic that doesn't require a
 * live CLI connection.
 */
public class SessionHandlerTest {

    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

    private CopilotSession session;

    @BeforeEach
    void setup() throws Exception {
        var constructor = CopilotSession.class.getDeclaredConstructor(String.class, JsonRpcClient.class, String.class);
        constructor.setAccessible(true);
        session = constructor.newInstance("handler-test-session", null, null);
    }

    // ===== setEventErrorPolicy =====

    @Test
    void testSetEventErrorPolicyNullThrowsNPE() {
        assertThrows(NullPointerException.class, () -> session.setEventErrorPolicy(null));
    }

    @Test
    void testSetEventErrorPolicySetsValue() {
        session.setEventErrorPolicy(EventErrorPolicy.SUPPRESS_AND_LOG_ERRORS);
        // No exception means success; the policy is stored internally
    }

    // ===== handlePermissionRequest: no handler registered =====

    @Test
    void testHandlePermissionRequestWithNoHandlerReturnsDenied() throws Exception {
        JsonNode data = MAPPER.valueToTree(Map.of("tool", "read_file", "resource", "/tmp/test"));

        PermissionRequestResult result = session.handlePermissionRequest(data).get();

        assertEquals("denied-no-approval-rule-and-could-not-request-from-user", result.getKind());
    }

    // ===== handlePermissionRequest: handler throws =====

    @Test
    void testHandlePermissionRequestHandlerExceptionReturnsDenied() throws Exception {
        session.registerPermissionHandler((request, invocation) -> {
            throw new RuntimeException("handler boom");
        });

        JsonNode data = MAPPER.valueToTree(Map.of("tool", "read_file"));

        PermissionRequestResult result = session.handlePermissionRequest(data).get();

        assertEquals("denied-no-approval-rule-and-could-not-request-from-user", result.getKind());
    }

    // ===== handlePermissionRequest: handler future fails =====

    @Test
    void testHandlePermissionRequestHandlerFutureFailsReturnsDenied() throws Exception {
        session.registerPermissionHandler(
                (request, invocation) -> CompletableFuture.failedFuture(new RuntimeException("async handler boom")));

        JsonNode data = MAPPER.valueToTree(Map.of("tool", "read_file"));

        PermissionRequestResult result = session.handlePermissionRequest(data).get();

        assertEquals("denied-no-approval-rule-and-could-not-request-from-user", result.getKind());
    }

    // ===== handlePermissionRequest: handler succeeds =====

    @Test
    void testHandlePermissionRequestHandlerSucceeds() throws Exception {
        session.registerPermissionHandler((request, invocation) -> {
            assertEquals("handler-test-session", invocation.getSessionId());
            var res = new PermissionRequestResult();
            res.setKind("allow");
            return CompletableFuture.completedFuture(res);
        });

        JsonNode data = MAPPER.valueToTree(Map.of("tool", "read_file"));

        PermissionRequestResult result = session.handlePermissionRequest(data).get();

        assertEquals("allow", result.getKind());
    }

    // ===== handleUserInputRequest: no handler registered =====

    @Test
    void testHandleUserInputRequestNoHandler() {
        var request = new UserInputRequest();

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> session.handleUserInputRequest(request).get());
        assertInstanceOf(IllegalStateException.class, ex.getCause());
    }

    // ===== handleUserInputRequest: handler throws synchronously =====

    @Test
    void testHandleUserInputRequestHandlerThrowsSynchronously() {
        session.registerUserInputHandler((req, invocation) -> {
            throw new RuntimeException("sync user input boom");
        });

        var request = new UserInputRequest();

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> session.handleUserInputRequest(request).get());
        assertInstanceOf(RuntimeException.class, ex.getCause());
    }

    // ===== handleUserInputRequest: handler future fails =====

    @Test
    void testHandleUserInputRequestHandlerFutureFails() {
        session.registerUserInputHandler(
                (req, invocation) -> CompletableFuture.failedFuture(new RuntimeException("async user input boom")));

        var request = new UserInputRequest();

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> session.handleUserInputRequest(request).get());
        assertInstanceOf(RuntimeException.class, ex.getCause());
    }

    // ===== handleUserInputRequest: handler succeeds =====

    @Test
    void testHandleUserInputRequestHandlerSucceeds() throws Exception {
        session.registerUserInputHandler((req, invocation) -> {
            assertEquals("handler-test-session", invocation.getSessionId());
            return CompletableFuture.completedFuture(new UserInputResponse().setAnswer("user typed this"));
        });

        var request = new UserInputRequest();

        UserInputResponse response = session.handleUserInputRequest(request).get();

        assertEquals("user typed this", response.getAnswer());
    }

    // ===== handleHooksInvoke: no hooks registered =====

    @Test
    void testHandleHooksInvokeNoHooksReturnsNull() throws Exception {
        JsonNode input = MAPPER.valueToTree(Map.of());

        Object result = session.handleHooksInvoke("preToolUse", input).get();

        assertNull(result);
    }

    // ===== handleHooksInvoke: userPromptSubmitted =====

    @Test
    void testHandleHooksInvokeUserPromptSubmitted() throws Exception {
        var hooks = new SessionHooks().setOnUserPromptSubmitted((hookInput, invocation) -> {
            assertEquals("handler-test-session", invocation.getSessionId());
            return CompletableFuture
                    .completedFuture(new UserPromptSubmittedHookOutput("modified prompt", "extra context", false));
        });
        session.registerHooks(hooks);

        JsonNode input = MAPPER
                .valueToTree(Map.of("timestamp", 1735689600L, "cwd", "/tmp", "prompt", "original prompt"));

        Object result = session.handleHooksInvoke("userPromptSubmitted", input).get();

        assertInstanceOf(UserPromptSubmittedHookOutput.class, result);
        var output = (UserPromptSubmittedHookOutput) result;
        assertEquals("modified prompt", output.modifiedPrompt());
    }

    // ===== handleHooksInvoke: sessionStart =====

    @Test
    void testHandleHooksInvokeSessionStart() throws Exception {
        var hooks = new SessionHooks().setOnSessionStart((hookInput, invocation) -> {
            assertEquals("handler-test-session", invocation.getSessionId());
            return CompletableFuture.completedFuture(new SessionStartHookOutput("additional context", null));
        });
        session.registerHooks(hooks);

        JsonNode input = MAPPER.valueToTree(Map.of("timestamp", 1735689600L, "cwd", "/tmp", "source", "test"));

        Object result = session.handleHooksInvoke("sessionStart", input).get();

        assertInstanceOf(SessionStartHookOutput.class, result);
        var output = (SessionStartHookOutput) result;
        assertEquals("additional context", output.additionalContext());
    }

    // ===== handleHooksInvoke: sessionEnd =====

    @Test
    void testHandleHooksInvokeSessionEnd() throws Exception {
        var hooks = new SessionHooks().setOnSessionEnd((hookInput, invocation) -> {
            assertEquals("handler-test-session", invocation.getSessionId());
            return CompletableFuture.completedFuture(new SessionEndHookOutput(false, null, "summary"));
        });
        session.registerHooks(hooks);

        JsonNode input = MAPPER.valueToTree(Map.of("timestamp", 1735689600L, "cwd", "/tmp", "reason", "user_closed"));

        Object result = session.handleHooksInvoke("sessionEnd", input).get();

        assertInstanceOf(SessionEndHookOutput.class, result);
        var output = (SessionEndHookOutput) result;
        assertEquals("summary", output.sessionSummary());
    }

    // ===== handleHooksInvoke: unhandled hook type =====

    @Test
    void testHandleHooksInvokeUnhandledHookType() throws Exception {
        session.registerHooks(new SessionHooks());

        JsonNode input = MAPPER.valueToTree(Map.of());

        Object result = session.handleHooksInvoke("unknownHookType", input).get();

        assertNull(result);
    }

    // ===== handleHooksInvoke: handler throws =====

    @Test
    void testHandleHooksInvokeHandlerThrows() throws Exception {
        var hooks = new SessionHooks().setOnSessionStart((hookInput, invocation) -> {
            throw new RuntimeException("hook boom");
        });
        session.registerHooks(hooks);

        JsonNode input = MAPPER.valueToTree(Map.of("timestamp", 1735689600L, "cwd", "/tmp", "source", "test"));

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> session.handleHooksInvoke("sessionStart", input).get());
        assertInstanceOf(RuntimeException.class, ex.getCause());
    }

    // ===== handleHooksInvoke: invalid JSON for hook input =====

    @Test
    void testHandleHooksInvokeInvalidJsonFails() throws Exception {
        var hooks = new SessionHooks().setOnSessionStart(
                (hookInput, invocation) -> CompletableFuture.completedFuture(new SessionStartHookOutput(null, null)));
        session.registerHooks(hooks);

        // Pass an array node which can't be deserialized into SessionStartHookInput
        JsonNode input = MAPPER.valueToTree(List.of("not", "an", "object"));

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> session.handleHooksInvoke("sessionStart", input).get());
        assertInstanceOf(Exception.class, ex.getCause());
    }

    // ===== handleHooksInvoke: hook handler with null callback =====

    @Test
    void testHandleHooksInvokeNullCallbackReturnsNull() throws Exception {
        // SessionHooks with only userPromptSubmitted set, sessionStart is null
        var hooks = new SessionHooks().setOnUserPromptSubmitted((hookInput, invocation) -> CompletableFuture
                .completedFuture(new UserPromptSubmittedHookOutput(null, null, null)));
        session.registerHooks(hooks);

        // Invoke sessionStart hook - its handler is null
        JsonNode input = MAPPER.valueToTree(Map.of("timestamp", 1735689600L, "cwd", "/tmp", "source", "test"));

        Object result = session.handleHooksInvoke("sessionStart", input).get();

        assertNull(result);
    }

    // ===== registerTools =====

    @Test
    void testRegisterToolsNullIsSafe() {
        session.registerTools(null);
        assertNull(session.getTool("anything"));
    }

    @Test
    void testRegisterToolsEmptyListClearsTools() {
        session.registerTools(List.of(ToolDefinition.create("my_tool", "desc", Map.of(),
                invocation -> CompletableFuture.completedFuture("result"))));
        assertNotNull(session.getTool("my_tool"));

        session.registerTools(List.of());
        assertNull(session.getTool("my_tool"));
    }
}
