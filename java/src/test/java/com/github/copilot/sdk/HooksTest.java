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
import com.github.copilot.sdk.json.SessionHooks;

/**
 * Tests for hooks functionality (pre-tool-use and post-tool-use hooks).
 *
 * <p>
 * These tests use the shared CapiProxy infrastructure for deterministic API
 * response replay. Snapshots are stored in test/snapshots/hooks/.
 * </p>
 *
 * <p>
 * Note: Tests for userPromptSubmitted, sessionStart, and sessionEnd hooks are
 * not included as they are not tested in the upstream .NET or Node.js SDKs and
 * require test harness updates to properly invoke these hooks.
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
}
