/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PostToolUseHookInput;
import com.github.copilot.sdk.json.PreToolUseHookInput;
import com.github.copilot.sdk.json.PreToolUseHookOutput;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SessionEndHookInput;
import com.github.copilot.sdk.json.SessionHooks;
import com.github.copilot.sdk.json.SessionStartHookInput;
import com.github.copilot.sdk.json.UserPromptSubmittedHookInput;

/**
 * Tests for hooks functionality (pre-tool-use, post-tool-use,
 * user-prompt-submitted, session-start, and session-end hooks).
 *
 * <p>
 * These tests use the shared CapiProxy infrastructure for deterministic API
 * response replay. Snapshots are stored in test/snapshots/hooks/.
 * </p>
 */
public class HooksTest {

    private static E2ETestContext ctx;

    @BeforeAll
    static void setup() throws Exception {
        ctx = E2ETestContext.create();
    }

    @AfterAll
    static void teardown() throws Exception {
        if (ctx != null) {
            ctx.close();
        }
    }

    @Test
    void testPreToolUseHookInvokedWhenModelRunsTool() throws Exception {
        ctx.configureForTest("hooks", "invoke_pre_tool_use_hook_when_model_runs_a_tool");

        List<PreToolUseHookInput> preToolUseInputs = new ArrayList<>();
        final String[] sessionIdHolder = new String[1];

        SessionConfig config = new SessionConfig().setHooks(new SessionHooks().setOnPreToolUse((input, invocation) -> {
            preToolUseInputs.add(input);
            assertEquals(sessionIdHolder[0], invocation.getSessionId());
            return CompletableFuture.completedFuture(new PreToolUseHookOutput().setPermissionDecision("allow"));
        }));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();
            sessionIdHolder[0] = session.getSessionId();

            // Create a file for the model to read
            Path testFile = ctx.getWorkDir().resolve("hello.txt");
            Files.writeString(testFile, "Hello from the test!");

            session.sendAndWait(
                    new MessageOptions().setPrompt("Read the contents of hello.txt and tell me what it says"))
                    .get(60, TimeUnit.SECONDS);

            // Should have received at least one preToolUse hook call
            assertFalse(preToolUseInputs.isEmpty(), "Should have received preToolUse hook calls");

            // Should have received the tool name
            assertTrue(preToolUseInputs.stream().anyMatch(i -> i.getToolName() != null && !i.getToolName().isEmpty()),
                    "Should have received tool name in preToolUse hook");
        }
    }

    @Test
    void testPostToolUseHookInvokedAfterModelRunsTool() throws Exception {
        ctx.configureForTest("hooks", "invoke_post_tool_use_hook_after_model_runs_a_tool");

        List<PostToolUseHookInput> postToolUseInputs = new ArrayList<>();
        final String[] sessionIdHolder = new String[1];

        SessionConfig config = new SessionConfig().setHooks(new SessionHooks().setOnPostToolUse((input, invocation) -> {
            postToolUseInputs.add(input);
            assertEquals(sessionIdHolder[0], invocation.getSessionId());
            return CompletableFuture.completedFuture(null);
        }));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();
            sessionIdHolder[0] = session.getSessionId();

            // Create a file for the model to read
            Path testFile = ctx.getWorkDir().resolve("world.txt");
            Files.writeString(testFile, "World from the test!");

            session.sendAndWait(
                    new MessageOptions().setPrompt("Read the contents of world.txt and tell me what it says"))
                    .get(60, TimeUnit.SECONDS);

            // Should have received at least one postToolUse hook call
            assertFalse(postToolUseInputs.isEmpty(), "Should have received postToolUse hook calls");

            // Should have received the tool name and result
            assertTrue(postToolUseInputs.stream().anyMatch(i -> i.getToolName() != null && !i.getToolName().isEmpty()),
                    "Should have received tool name in postToolUse hook");
            assertTrue(postToolUseInputs.stream().anyMatch(i -> i.getToolResult() != null),
                    "Should have received tool result in postToolUse hook");
        }
    }

    @Test
    void testBothHooksInvokedForSingleToolCall() throws Exception {
        ctx.configureForTest("hooks", "invoke_both_hooks_for_single_tool_call");

        List<PreToolUseHookInput> preToolUseInputs = new ArrayList<>();
        List<PostToolUseHookInput> postToolUseInputs = new ArrayList<>();

        SessionConfig config = new SessionConfig().setHooks(new SessionHooks().setOnPreToolUse((input, invocation) -> {
            preToolUseInputs.add(input);
            return CompletableFuture.completedFuture(new PreToolUseHookOutput().setPermissionDecision("allow"));
        }).setOnPostToolUse((input, invocation) -> {
            postToolUseInputs.add(input);
            return CompletableFuture.completedFuture(null);
        }));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();

            // Create a file for the model to read
            Path testFile = ctx.getWorkDir().resolve("both.txt");
            Files.writeString(testFile, "Testing both hooks!");

            session.sendAndWait(new MessageOptions().setPrompt("Read the contents of both.txt")).get(60,
                    TimeUnit.SECONDS);

            // Both hooks should have been called
            assertFalse(preToolUseInputs.isEmpty(), "Should have received preToolUse hook calls");
            assertFalse(postToolUseInputs.isEmpty(), "Should have received postToolUse hook calls");

            // The same tool should appear in both
            Set<String> preToolNames = preToolUseInputs.stream().map(PreToolUseHookInput::getToolName)
                    .filter(n -> n != null && !n.isEmpty()).collect(Collectors.toSet());
            Set<String> postToolNames = postToolUseInputs.stream().map(PostToolUseHookInput::getToolName)
                    .filter(n -> n != null && !n.isEmpty()).collect(Collectors.toSet());

            // Check if there's any overlap
            boolean hasOverlap = preToolNames.stream().anyMatch(postToolNames::contains);
            assertTrue(hasOverlap, "Expected the same tool to appear in both pre and post hooks");
        }
    }

    @Test
    void testDenyToolExecutionWhenPreToolUseReturnsDeny() throws Exception {
        ctx.configureForTest("hooks", "deny_tool_execution_when_pre_tool_use_returns_deny");

        List<PreToolUseHookInput> preToolUseInputs = new ArrayList<>();

        SessionConfig config = new SessionConfig().setHooks(new SessionHooks().setOnPreToolUse((input, invocation) -> {
            preToolUseInputs.add(input);
            // Deny all tool calls
            return CompletableFuture.completedFuture(new PreToolUseHookOutput().setPermissionDecision("deny"));
        }));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();

            // Create a file
            Path testFile = ctx.getWorkDir().resolve("protected.txt");
            String originalContent = "Original content that should not be modified";
            Files.writeString(testFile, originalContent);

            var response = session
                    .sendAndWait(
                            new MessageOptions().setPrompt("Edit protected.txt and replace 'Original' with 'Modified'"))
                    .get(60, TimeUnit.SECONDS);

            // The hook should have been called
            assertFalse(preToolUseInputs.isEmpty(), "Should have received preToolUse hook calls");

            // The response should be defined
            assertNotNull(response, "Response should not be null");
        }
    }

    @Test
    void testUserPromptSubmittedHookInvokedWhenUserSendsMessage() throws Exception {
        ctx.configureForTest("hooks", "invoke_user_prompt_submitted_hook");

        List<UserPromptSubmittedHookInput> promptInputs = new ArrayList<>();
        final String[] sessionIdHolder = new String[1];

        SessionConfig config = new SessionConfig()
                .setHooks(new SessionHooks().setOnUserPromptSubmitted((input, invocation) -> {
                    promptInputs.add(input);
                    assertEquals(sessionIdHolder[0], invocation.getSessionId());
                    return CompletableFuture.completedFuture(null);
                }));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();
            sessionIdHolder[0] = session.getSessionId();

            session.sendAndWait(new MessageOptions().setPrompt("Hello, Copilot!")).get(60, TimeUnit.SECONDS);

            // Should have received at least one userPromptSubmitted hook call
            assertFalse(promptInputs.isEmpty(), "Should have received userPromptSubmitted hook calls");

            // Should have received the prompt
            assertTrue(promptInputs.stream().anyMatch(i -> i.getPrompt() != null && !i.getPrompt().isEmpty()),
                    "Should have received prompt in userPromptSubmitted hook");
        }
    }

    @Test
    void testSessionStartHookInvokedWhenSessionCreated() throws Exception {
        ctx.configureForTest("hooks", "invoke_session_start_hook");

        List<SessionStartHookInput> startInputs = new ArrayList<>();

        SessionConfig config = new SessionConfig()
                .setHooks(new SessionHooks().setOnSessionStart((input, invocation) -> {
                    startInputs.add(input);
                    return CompletableFuture.completedFuture(null);
                }));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();

            // Send a message to trigger the session lifecycle
            session.sendAndWait(new MessageOptions().setPrompt("Hello")).get(60, TimeUnit.SECONDS);

            // Should have received at least one sessionStart hook call
            assertFalse(startInputs.isEmpty(), "Should have received sessionStart hook calls");

            // Should have received the source
            assertTrue(startInputs.stream().anyMatch(i -> i.getSource() != null),
                    "Should have received source in sessionStart hook");
        }
    }

    @Test
    void testSessionEndHookInvokedWhenSessionEnds() throws Exception {
        ctx.configureForTest("hooks", "invoke_session_end_hook");

        List<SessionEndHookInput> endInputs = new ArrayList<>();

        SessionConfig config = new SessionConfig().setHooks(new SessionHooks().setOnSessionEnd((input, invocation) -> {
            endInputs.add(input);
            return CompletableFuture.completedFuture(null);
        }));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();

            // Send a message and wait for completion
            session.sendAndWait(new MessageOptions().setPrompt("Say hello")).get(60, TimeUnit.SECONDS);

            // Should have received at least one sessionEnd hook call
            assertFalse(endInputs.isEmpty(), "Should have received sessionEnd hook calls");

            // Should have received the reason
            assertTrue(endInputs.stream().anyMatch(i -> i.getReason() != null),
                    "Should have received reason in sessionEnd hook");
        }
    }
}
