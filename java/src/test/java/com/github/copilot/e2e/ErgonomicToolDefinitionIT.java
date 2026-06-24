/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.e2e;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.CopilotClient;
import com.github.copilot.CopilotSession;
import com.github.copilot.E2ETestContext;
import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.ToolDefinition;
import com.github.copilot.rpc.ToolSet;

/**
 * Failsafe integration test for the ergonomic {@code @CopilotTool} +
 * {@code ToolDefinition.fromObject()} API.
 *
 * <p>
 * This test proves that the ergonomic annotation-based API produces identical
 * wire behavior to the low-level {@code ToolDefinition.create()} API tested in
 * {@code LowLevelToolDefinitionIT}.
 *
 * @see Snapshot: tools/ergonomic_tool_definition
 */
class ErgonomicToolDefinitionIT {

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
    void ergonomicToolDefinition() throws Exception {
        ctx.configureForTest("tools", "ergonomic_tool_definition");

        ErgonomicTestTools tools = new ErgonomicTestTools();
        List<ToolDefinition> toolDefs = ToolDefinition.fromObject(tools);

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setAvailableTools(new ToolSet().addCustom("*").addBuiltIn("web_fetch")).setTools(toolDefs))
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
                assertTrue("analyzing".equals(tools.currentPhase),
                        "Expected currentPhase to be 'analyzing' but was: " + tools.currentPhase);
            } finally {
                session.close();
            }
        }
    }
}
