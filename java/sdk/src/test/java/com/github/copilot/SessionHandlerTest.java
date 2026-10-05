/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.rpc.AgentStopHookOutput;
import com.github.copilot.rpc.PermissionRequestResult;
import com.github.copilot.rpc.PermissionRequestResultKind;
import com.github.copilot.rpc.SessionEndHookOutput;
import com.github.copilot.rpc.SessionHooks;
import com.github.copilot.rpc.SessionStartHookOutput;
import com.github.copilot.rpc.SubagentStartHookOutput;
import com.github.copilot.rpc.SubagentStopHookOutput;
import com.github.copilot.rpc.ToolDefinition;
import com.github.copilot.rpc.UserInputRequest;
import com.github.copilot.rpc.UserInputResponse;
import com.github.copilot.rpc.UserPromptSubmittedHookOutput;
import com.github.copilot.rpc.UserPromptTransformedHookOutput;

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

        assertEquals("user-not-available", result.getKind());
    }

    // ===== handlePermissionRequest: handler throws =====

    @Test
    void testHandlePermissionRequestHandlerExceptionReturnsDenied() throws Exception {
        session.registerPermissionHandler((request, invocation) -> {
            throw new RuntimeException("handler boom");
        });

        JsonNode data = MAPPER.valueToTree(Map.of("tool", "read_file"));

        PermissionRequestResult result = session.handlePermissionRequest(data).get();

        assertEquals("user-not-available", result.getKind());
    }

    // ===== handlePermissionRequest: handler future fails =====

    @Test
    void testHandlePermissionRequestHandlerFutureFailsReturnsDenied() throws Exception {
        session.registerPermissionHandler(
                (request, invocation) -> CompletableFuture.failedFuture(new RuntimeException("async handler boom")));

        JsonNode data = MAPPER.valueToTree(Map.of("tool", "read_file"));

        PermissionRequestResult result = session.handlePermissionRequest(data).get();

        assertEquals("user-not-available", result.getKind());
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

    // ===== handlePermissionRequest: handler returns NO_RESULT (v3 path) =====

    @Test
    void testHandlePermissionRequestNoResultPassesThrough() throws Exception {
        session.registerPermissionHandler((request, invocation) -> {
            var res = new PermissionRequestResult();
            res.setKind(PermissionRequestResultKind.NO_RESULT);
            return CompletableFuture.completedFuture(res);
        });

        JsonNode data = MAPPER.valueToTree(Map.of("tool", "read_file"));

        PermissionRequestResult result = session.handlePermissionRequest(data).get();

        // In v3, NO_RESULT is a valid response — the session just returns it
        // and the caller (CopilotSession.executePermissionAndRespondAsync) decides
        // to skip sending the RPC response.
        assertEquals("no-result", result.getKind());
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

    @Test
    void testHandleHooksInvokeUserPromptTransformed() throws Exception {
        var hooks = new SessionHooks().setOnUserPromptTransformed((hookInput, invocation) -> {
            assertEquals("handler-test-session", invocation.getSessionId());
            assertEquals("original prompt", hookInput.prompt());
            assertEquals("transformed prompt", hookInput.transformedPrompt());
            return CompletableFuture.completedFuture(new UserPromptTransformedHookOutput("replacement prompt"));
        });
        session.registerHooks(hooks);

        JsonNode input = MAPPER.valueToTree(Map.of("sessionId", "runtime-session", "timestamp", 1735689600L, "cwd",
                "/tmp", "prompt", "original prompt", "transformedPrompt", "transformed prompt"));

        Object result = session.handleHooksInvoke("userPromptTransformed", input).get();

        assertInstanceOf(UserPromptTransformedHookOutput.class, result);
        var output = (UserPromptTransformedHookOutput) result;
        assertEquals("replacement prompt", output.modifiedTransformedPrompt());
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

    // ===== handleHooksInvoke: agentStop =====

    @Test
    void testHandleHooksInvokeAgentStop() throws Exception {
        var hooks = new SessionHooks().setOnAgentStop((hookInput, invocation) -> {
            assertEquals("handler-test-session", invocation.getSessionId());
            assertEquals("runtime-session-123", hookInput.getSessionId());
            assertEquals("end_turn", hookInput.getStopReason());
            assertEquals("/tmp/transcript.jsonl", hookInput.getTranscriptPath());
            assertTrue(hookInput.getStopHookActive());
            return CompletableFuture.completedFuture(
                    new AgentStopHookOutput().setDecision("block").setReason("finish the remaining work"));
        });
        session.registerHooks(hooks);

        JsonNode input = MAPPER.valueToTree(Map.of("sessionId", "runtime-session-123", "timestamp", 1735689600L, "cwd",
                "/tmp", "stopReason", "end_turn", "transcriptPath", "/tmp/transcript.jsonl", "stop_hook_active", true));

        Object result = session.handleHooksInvoke("agentStop", input).get();

        assertInstanceOf(AgentStopHookOutput.class, result);
        var output = (AgentStopHookOutput) result;
        assertEquals("block", output.getDecision());
        assertEquals("finish the remaining work", output.getReason());
    }

    @Test
    void testHandleHooksInvokeSubagentStart() throws Exception {
        var hooks = new SessionHooks().setOnSubagentStart((hookInput, invocation) -> {
            assertEquals("handler-test-session", invocation.getSessionId());
            assertEquals("parent-session", hookInput.sessionId());
            assertEquals(1735689600000L, hookInput.timestamp());
            assertEquals("/tmp", hookInput.cwd());
            assertEquals("/tmp/transcript.jsonl", hookInput.transcriptPath());
            assertEquals("explore", hookInput.agentName());
            assertEquals("Explore Agent", hookInput.agentDisplayName());
            assertEquals("Read code", hookInput.agentDescription());
            return CompletableFuture.completedFuture(new SubagentStartHookOutput("Follow the file"));
        });
        assertTrue(hooks.hasHooks());
        session.registerHooks(hooks);

        JsonNode input = MAPPER.valueToTree(Map.of("sessionId", "parent-session", "timestamp", 1735689600000L, "cwd",
                "/tmp", "transcriptPath", "/tmp/transcript.jsonl", "agentName", "explore", "agentDisplayName",
                "Explore Agent", "agentDescription", "Read code"));

        var output = assertInstanceOf(SubagentStartHookOutput.class,
                session.handleHooksInvoke("subagentStart", input).get());
        assertEquals("Follow the file", MAPPER.valueToTree(output).get("additionalContext").asText());
    }

    @Test
    void testHandleHooksInvokeSubagentStop() throws Exception {
        var hooks = new SessionHooks().setOnSubagentStop((hookInput, invocation) -> {
            assertEquals("handler-test-session", invocation.getSessionId());
            assertEquals("parent-session", hookInput.sessionId());
            assertEquals(1735689600001L, hookInput.timestamp());
            assertEquals("/tmp", hookInput.cwd());
            assertEquals("/tmp/transcript.jsonl", hookInput.transcriptPath());
            assertEquals("explore", hookInput.agentName());
            assertEquals("explore", hookInput.agentType());
            assertEquals("read-file", hookInput.agentId());
            assertEquals("Explore Agent", hookInput.agentDisplayName());
            assertEquals("Read code", hookInput.agentDescription());
            assertEquals("end_turn", hookInput.stopReason());
            assertEquals("original answer", hookInput.response());
            return CompletableFuture.completedFuture(new SubagentStopHookOutput(null, null, "rewritten answer"));
        });
        assertTrue(hooks.hasHooks());
        session.registerHooks(hooks);

        JsonNode input = MAPPER.valueToTree(Map.ofEntries(Map.entry("sessionId", "parent-session"),
                Map.entry("timestamp", 1735689600001L), Map.entry("cwd", "/tmp"),
                Map.entry("transcriptPath", "/tmp/transcript.jsonl"), Map.entry("agentName", "explore"),
                Map.entry("agentType", "explore"), Map.entry("agentId", "read-file"),
                Map.entry("agentDisplayName", "Explore Agent"), Map.entry("agentDescription", "Read code"),
                Map.entry("stopReason", "end_turn"), Map.entry("response", "original answer")));

        var output = assertInstanceOf(SubagentStopHookOutput.class,
                session.handleHooksInvoke("subagentStop", input).get());
        JsonNode result = MAPPER.valueToTree(output);
        assertEquals("rewritten answer", result.get("modifiedResponse").asText());
        assertFalse(result.has("decision"));
        assertFalse(result.has("reason"));
    }

    @Test
    void testHandleHooksInvokeSubagentStopBlock() throws Exception {
        session.registerHooks(new SessionHooks().setOnSubagentStop((input, invocation) -> CompletableFuture
                .completedFuture(new SubagentStopHookOutput("block", "Keep researching", null))));

        JsonNode result = MAPPER
                .valueToTree(session
                        .handleHooksInvoke("subagentStop",
                                MAPPER.valueToTree(Map.of("sessionId", "parent-session", "agentName", "explore")))
                        .get());
        assertEquals("block", result.get("decision").asText());
        assertEquals("Keep researching", result.get("reason").asText());
        assertFalse(result.has("modifiedResponse"));
    }

    // ===== handleHooksInvoke: sessionId deserialization on hook inputs =====

    @Test
    void testHookInputSessionIdDeserializedForSessionStart() throws Exception {
        var hooks = new SessionHooks().setOnSessionStart((hookInput, invocation) -> {
            assertEquals("runtime-session-123", hookInput.sessionId());
            assertEquals(1735689600L, hookInput.timestamp());
            assertEquals("/tmp", hookInput.cwd());
            return CompletableFuture.completedFuture(new SessionStartHookOutput(null, null));
        });
        session.registerHooks(hooks);

        JsonNode input = MAPPER.valueToTree(
                Map.of("sessionId", "runtime-session-123", "timestamp", 1735689600L, "cwd", "/tmp", "source", "new"));

        session.handleHooksInvoke("sessionStart", input).get();
    }

    @Test
    void testHookInputSessionIdDeserializedForSessionEnd() throws Exception {
        var hooks = new SessionHooks().setOnSessionEnd((hookInput, invocation) -> {
            assertEquals("runtime-session-456", hookInput.sessionId());
            assertEquals("user_closed", hookInput.reason());
            return CompletableFuture.completedFuture(new SessionEndHookOutput(false, null, null));
        });
        session.registerHooks(hooks);

        JsonNode input = MAPPER.valueToTree(Map.of("sessionId", "runtime-session-456", "timestamp", 1735689600L, "cwd",
                "/tmp", "reason", "user_closed"));

        session.handleHooksInvoke("sessionEnd", input).get();
    }

    @Test
    void testHookInputSessionIdDeserializedForUserPromptSubmitted() throws Exception {
        var hooks = new SessionHooks().setOnUserPromptSubmitted((hookInput, invocation) -> {
            assertEquals("runtime-session-789", hookInput.sessionId());
            assertEquals("hello", hookInput.prompt());
            return CompletableFuture.completedFuture(new UserPromptSubmittedHookOutput(null, null, null));
        });
        session.registerHooks(hooks);

        JsonNode input = MAPPER.valueToTree(
                Map.of("sessionId", "runtime-session-789", "timestamp", 1735689600L, "cwd", "/tmp", "prompt", "hello"));

        session.handleHooksInvoke("userPromptSubmitted", input).get();
    }

    // ===== handleHooksInvoke: unhandled hook type =====

    @Test
    void testHandleHooksInvokeUnhandledHookType() throws Exception {
        session.registerHooks(new SessionHooks());

        JsonNode input = MAPPER.valueToTree(Map.of());

        Object result = session.handleHooksInvoke("unknownHookType", input).get();

        assertNull(result);
    }

    @Test
    void testHandleHooksInvokeSubagentLifecycleDoesNotLogAsUnhandled() throws Exception {
        session.registerHooks(new SessionHooks());
        Logger logger = Logger.getLogger(CopilotSession.class.getName());
        Level previousLevel = logger.getLevel();
        List<String> unhandled = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getMessage().startsWith("Unhandled hook type: ")) {
                    unhandled.add(record.getMessage());
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        handler.setLevel(Level.FINE);
        logger.addHandler(handler);
        logger.setLevel(Level.FINE);
        try {
            JsonNode input = MAPPER.valueToTree(Map.of("sessionId", "handler-test-session", "agentName", "task"));
            assertNull(session.handleHooksInvoke("subagentStart", input).get());
            assertNull(session.handleHooksInvoke("subagentStop", input).get());
            assertNull(session.handleHooksInvoke("unknownHookType", input).get());

            assertEquals(List.of("Unhandled hook type: unknownHookType"), unhandled);
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(previousLevel);
        }
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
