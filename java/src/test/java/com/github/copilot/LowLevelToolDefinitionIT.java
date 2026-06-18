/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.ToolDefinition;
import com.github.copilot.rpc.ToolSet;

/**
 * Failsafe integration test for explicit (non-ergonomic) tool definition APIs.
 *
 * @see Snapshot: tools/low_level_tool_definition
 */
class LowLevelToolDefinitionIT {

    private static E2ETestContext ctx;
    private String currentPhase;

    record PhaseArgs(String phase) {
    }

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
    void lowLevelToolDefinition() throws Exception {
        ctx.configureForTest("tools", "low_level_tool_definition");

        Map<String, Object> setPhaseSchema = Map.of("type", "object", "properties",
                Map.of("phase", Map.of("type", "string", "enum", List.of("searching", "analyzing", "done"))),
                "required", List.of("phase"));

        ToolDefinition setPhaseTool = ToolDefinition.create("set_current_phase", "Sets the current phase of the agent",
                setPhaseSchema, invocation -> {
                    PhaseArgs args = invocation.getArgumentsAs(PhaseArgs.class);
                    currentPhase = args.phase();
                    return CompletableFuture.completedFuture("Phase set to " + currentPhase);
                });

        Map<String, Object> searchSchema = Map.of("type", "object", "properties",
                Map.of("keyword", Map.of("type", "string")), "required", List.of("keyword"));

        ToolDefinition searchTool = ToolDefinition.create("search_items", "Search for items by keyword", searchSchema,
                invocation -> {
                    Map<String, Object> args = invocation.getArguments();
                    args.get("keyword");
                    return CompletableFuture.completedFuture("Found: item_alpha, item_beta");
                });

        Map<String, Object> grepSchema = Map.of("type", "object", "properties",
                Map.of("query", Map.of("type", "string")), "required", List.of("query"));

        ToolDefinition grepOverrideTool = ToolDefinition.createOverride("grep", "Custom grep override", grepSchema,
                invocation -> {
                    Map<String, Object> args = invocation.getArguments();
                    String query = (String) args.get("query");
                    return CompletableFuture.completedFuture("CUSTOM_GREP: " + query);
                });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setAvailableTools(new ToolSet().addCustom("*").addBuiltIn("web_fetch"))
                            .setTools(List.of(setPhaseTool, searchTool, grepOverrideTool)))
                    .get(30, TimeUnit.SECONDS);

            try {
                AssistantMessageEvent response = session.sendAndWait(new MessageOptions().setPrompt(
                        "First, set the current phase to 'analyzing'. Then search for items with keyword 'copilot'. Report the phase and search results."),
                        60_000).get(90, TimeUnit.SECONDS);

                assertNotNull(response, "Expected a response from the assistant");
                String content = response.getData().content().toLowerCase();
                assertTrue(content.contains("analyzing"),
                        "Response should contain the updated phase: " + response.getData().content());
                assertTrue(content.contains("item_alpha") || content.contains("item_beta"),
                        "Response should contain search results: " + response.getData().content());
                assertTrue("analyzing".equals(currentPhase),
                        "Expected currentPhase to be analyzing but was: " + currentPhase);
            } finally {
                session.close();
            }
        }
    }
}
