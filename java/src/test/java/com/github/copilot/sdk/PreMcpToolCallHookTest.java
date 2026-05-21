/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.sdk.json.McpServerConfig;
import com.github.copilot.sdk.json.McpStdioServerConfig;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.PreMcpToolCallHookInput;
import com.github.copilot.sdk.json.PreMcpToolCallHookOutput;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SessionHooks;

/**
 * E2E tests for the preMcpToolCall hook, verifying meta manipulation scenarios:
 * setting meta, replacing meta, and removing meta.
 *
 * <p>
 * These tests use the shared CapiProxy infrastructure for deterministic API
 * response replay. Snapshots are stored in
 * test/snapshots/pre_mcp_tool_call_hook/.
 * </p>
 */
public class PreMcpToolCallHookTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
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

    private static Path findTestHarnessDir() {
        Path dir = Paths.get(System.getProperty("user.dir"));
        while (dir != null) {
            Path candidate = dir.resolve("test").resolve("harness").resolve("test-mcp-meta-echo-server.mjs");
            if (Files.exists(candidate)) {
                return candidate.getParent();
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("Could not find test/harness/test-mcp-meta-echo-server.mjs");
    }

    private static HashMap<String, McpServerConfig> createMetaEchoMcpConfig(Path testHarnessDir) {
        var servers = new HashMap<String, McpServerConfig>();
        servers.put("meta-echo",
                new McpStdioServerConfig().setCommand("node")
                        .setArgs(List.of(testHarnessDir.resolve("test-mcp-meta-echo-server.mjs").toString()))
                        .setWorkingDirectory(testHarnessDir.toString()).setTools(List.of("*")));
        return servers;
    }

    /**
     * Verifies that the preMcpToolCall hook can set meta on a tool call.
     *
     * @see Snapshot: pre_mcp_tool_call_hook/should_set_meta_via_premcptoolcall_hook
     */
    @Test
    void testShouldSetMetaViaPreMcpToolCallHook() throws Exception {
        ctx.configureForTest("pre_mcp_tool_call_hook", "should_set_meta_via_premcptoolcall_hook");

        Path testHarnessDir = findTestHarnessDir();
        var hookInputs = new ArrayList<PreMcpToolCallHookInput>();

        ObjectNode metaToSet = MAPPER.createObjectNode();
        metaToSet.put("injected", "by-hook");
        metaToSet.put("source", "test");

        var config = new SessionConfig().setMcpServers(createMetaEchoMcpConfig(testHarnessDir))
                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                .setHooks(new SessionHooks().setOnPreMcpToolCall((input, invocation) -> {
                    hookInputs.add(input);
                    return CompletableFuture.completedFuture(new PreMcpToolCallHookOutput(metaToSet));
                }));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();

            var response = session.sendAndWait(new MessageOptions().setPrompt(
                    "Use the meta-echo/echo_meta tool with value 'test-set'. Reply with just the raw tool result."))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            String content = response.getData().content();
            assertNotNull(content);
            assertTrue(content.contains("injected"), "Response should contain injected meta key");
            assertTrue(content.contains("by-hook"), "Response should contain injected meta value");

            assertFalse(hookInputs.isEmpty(), "Should have received preMcpToolCall hook calls");
            assertEquals("meta-echo", hookInputs.get(0).getServerName());
            assertEquals("echo_meta", hookInputs.get(0).getToolName());
            assertNotNull(hookInputs.get(0).getWorkingDirectory());
            assertFalse(hookInputs.get(0).getWorkingDirectory().isEmpty());
            assertTrue(hookInputs.get(0).getTimestamp() > 0);
        }
    }

    /**
     * Verifies that the preMcpToolCall hook can replace meta on a tool call.
     *
     * @see Snapshot:
     *      pre_mcp_tool_call_hook/should_replace_meta_via_premcptoolcall_hook
     */
    @Test
    void testShouldReplaceMetaViaPreMcpToolCallHook() throws Exception {
        ctx.configureForTest("pre_mcp_tool_call_hook", "should_replace_meta_via_premcptoolcall_hook");

        Path testHarnessDir = findTestHarnessDir();
        var hookInputs = new ArrayList<PreMcpToolCallHookInput>();

        ObjectNode replacementMeta = MAPPER.createObjectNode();
        replacementMeta.put("completely", "replaced");

        var config = new SessionConfig().setMcpServers(createMetaEchoMcpConfig(testHarnessDir))
                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                .setHooks(new SessionHooks().setOnPreMcpToolCall((input, invocation) -> {
                    hookInputs.add(input);
                    return CompletableFuture.completedFuture(new PreMcpToolCallHookOutput(replacementMeta));
                }));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();

            var response = session.sendAndWait(new MessageOptions().setPrompt(
                    "Use the meta-echo/echo_meta tool with value 'test-replace'. Reply with just the raw tool result."))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            String content = response.getData().content();
            assertNotNull(content);
            assertTrue(content.contains("completely"), "Response should contain replaced meta key");
            assertTrue(content.contains("replaced"), "Response should contain replaced meta value");

            assertFalse(hookInputs.isEmpty(), "Should have received preMcpToolCall hook calls");
            assertEquals("meta-echo", hookInputs.get(0).getServerName());
            assertEquals("echo_meta", hookInputs.get(0).getToolName());
        }
    }

    /**
     * Verifies that the preMcpToolCall hook can remove meta from a tool call.
     *
     * @see Snapshot:
     *      pre_mcp_tool_call_hook/should_remove_meta_via_premcptoolcall_hook
     */
    @Test
    void testShouldRemoveMetaViaPreMcpToolCallHook() throws Exception {
        ctx.configureForTest("pre_mcp_tool_call_hook", "should_remove_meta_via_premcptoolcall_hook");

        Path testHarnessDir = findTestHarnessDir();
        var hookInputs = new ArrayList<PreMcpToolCallHookInput>();

        var config = new SessionConfig().setMcpServers(createMetaEchoMcpConfig(testHarnessDir))
                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                .setHooks(new SessionHooks().setOnPreMcpToolCall((input, invocation) -> {
                    hookInputs.add(input);
                    // Return output with null metaToUse to signal removal
                    return CompletableFuture.completedFuture(new PreMcpToolCallHookOutput(null));
                }));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();

            var response = session.sendAndWait(new MessageOptions().setPrompt(
                    "Use the meta-echo/echo_meta tool with value 'test-remove'. Reply with just the raw tool result."))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            String content = response.getData().content();
            assertNotNull(content);
            assertTrue(content.contains("\"meta\":null") || content.contains("\"meta\": null"),
                    "Response should contain null meta");
            assertTrue(content.contains("test-remove"), "Response should contain the test value");

            assertFalse(hookInputs.isEmpty(), "Should have received preMcpToolCall hook calls");
            assertEquals("meta-echo", hookInputs.get(0).getServerName());
            assertEquals("echo_meta", hookInputs.get(0).getToolName());
        }
    }
}
