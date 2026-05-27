/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.rpc.McpServerConfig;
import com.github.copilot.rpc.McpStdioServerConfig;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.PreMcpToolCallHookInput;
import com.github.copilot.rpc.PreMcpToolCallHookOutput;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SessionHooks;

/**
 * Tests for preMcpToolCall hook functionality.
 *
 * <p>
 * These tests use the shared CapiProxy infrastructure for deterministic API
 * response replay. Snapshots are stored in
 * test/snapshots/pre_mcp_tool_call_hook/.
 * </p>
 */
public class PreMcpToolCallHookTest {

    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();
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

    /**
     * Verifies that preMcpToolCall hook can set metadata on the MCP request.
     *
     * @see Snapshot: pre_mcp_tool_call_hook/should_set_meta_via_premcptoolcall_hook
     */
    @Disabled("Requires snapshot: pre_mcp_tool_call_hook/should_set_meta_via_premcptoolcall_hook")
    @Test
    void testShouldSetMetaViaPreMcpToolCallHook() throws Exception {
        ctx.configureForTest("pre_mcp_tool_call_hook", "should_set_meta_via_premcptoolcall_hook");

        var hookInputs = new java.util.ArrayList<PreMcpToolCallHookInput>();

        var mcpServers = new HashMap<String, McpServerConfig>();
        mcpServers.put("meta-echo", new McpStdioServerConfig().setCommand("npx").setArgs(List.of("-y", "mcp-meta-echo"))
                .setTools(List.of("*")).setWorkingDirectory(ctx.getWorkDir().toString()));

        var hooks = new SessionHooks().setOnPreMcpToolCall((input, invocation) -> {
            hookInputs.add(input);
            JsonNode metaNode = MAPPER.valueToTree(Map.of("injected", "by-hook", "source", "test"));
            return CompletableFuture.completedFuture(PreMcpToolCallHookOutput.withMeta(metaNode));
        });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setMcpServers(mcpServers).setHooks(hooks)
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            AssistantMessageEvent response = session.sendAndWait(new MessageOptions().setPrompt(
                    "Use the meta-echo/echo_meta tool with value 'test-set'. Reply with just the raw tool result."))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            assertFalse(hookInputs.isEmpty(), "Should have received preMcpToolCall hook calls");

            // Verify hook input fields
            PreMcpToolCallHookInput hookInput = hookInputs.get(0);
            assertEquals("meta-echo", hookInput.getServerName());
            assertNotNull(hookInput.getToolName());
            assertNotNull(hookInput.getCwd());
            assertTrue(hookInput.getTimestamp() > 0);

            // Verify the response contains the injected metadata
            String content = response.getData().content();
            assertTrue(content.contains("by-hook"), "Response should contain injected metadata: " + content);

            session.close();
        }
    }

    /**
     * Verifies that preMcpToolCall hook can replace existing metadata.
     *
     * @see Snapshot:
     *      pre_mcp_tool_call_hook/should_replace_meta_via_premcptoolcall_hook
     */
    @Disabled("Requires snapshot: pre_mcp_tool_call_hook/should_replace_meta_via_premcptoolcall_hook")
    @Test
    void testShouldReplaceMetaViaPreMcpToolCallHook() throws Exception {
        ctx.configureForTest("pre_mcp_tool_call_hook", "should_replace_meta_via_premcptoolcall_hook");

        var mcpServers = new HashMap<String, McpServerConfig>();
        mcpServers.put("meta-echo", new McpStdioServerConfig().setCommand("npx").setArgs(List.of("-y", "mcp-meta-echo"))
                .setTools(List.of("*")).setWorkingDirectory(ctx.getWorkDir().toString()));

        var hooks = new SessionHooks().setOnPreMcpToolCall((input, invocation) -> {
            JsonNode metaNode = MAPPER.valueToTree(Map.of("replaced", "true", "original", "gone"));
            return CompletableFuture.completedFuture(PreMcpToolCallHookOutput.withMeta(metaNode));
        });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setMcpServers(mcpServers).setHooks(hooks)
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            AssistantMessageEvent response = session.sendAndWait(new MessageOptions().setPrompt(
                    "Use the meta-echo/echo_meta tool with value 'test-replace'. Reply with just the raw tool result."))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);

            // Verify the response contains the replaced metadata
            String content = response.getData().content();
            assertTrue(content.contains("replaced"), "Response should contain replaced metadata: " + content);

            session.close();
        }
    }

    /**
     * Verifies that preMcpToolCall hook can remove metadata from the MCP request.
     *
     * @see Snapshot:
     *      pre_mcp_tool_call_hook/should_remove_meta_via_premcptoolcall_hook
     */
    @Disabled("Requires snapshot: pre_mcp_tool_call_hook/should_remove_meta_via_premcptoolcall_hook")
    @Test
    void testShouldRemoveMetaViaPreMcpToolCallHook() throws Exception {
        ctx.configureForTest("pre_mcp_tool_call_hook", "should_remove_meta_via_premcptoolcall_hook");

        var mcpServers = new HashMap<String, McpServerConfig>();
        mcpServers.put("meta-echo", new McpStdioServerConfig().setCommand("npx").setArgs(List.of("-y", "mcp-meta-echo"))
                .setTools(List.of("*")).setWorkingDirectory(ctx.getWorkDir().toString()));

        var hooks = new SessionHooks().setOnPreMcpToolCall((input, invocation) -> {
            // Return output with null metaToUse to remove metadata
            return CompletableFuture.completedFuture(PreMcpToolCallHookOutput.removeMeta());
        });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setMcpServers(mcpServers).setHooks(hooks)
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            AssistantMessageEvent response = session.sendAndWait(new MessageOptions().setPrompt(
                    "Use the meta-echo/echo_meta tool with value 'test-remove'. Reply with just the raw tool result."))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);

            session.close();
        }
    }
}
