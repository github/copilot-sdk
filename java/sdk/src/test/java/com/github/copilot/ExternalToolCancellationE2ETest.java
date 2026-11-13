/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.ToolDefinition;

public class ExternalToolCancellationE2ETest {

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
    void shouldCancelToolHandlerWhenSessionDisconnects() throws Exception {
        ctx.configureForTest("external_tool_cancellation", "should_cancel_tool_handler_when_session_disconnects");

        var pendingTool = new CompletableFuture<CompletableFuture<Object>>();
        ToolDefinition slowTool = ToolDefinition.create("slow_analysis",
                "A slow analysis tool that blocks until released", slowAnalysisSchema(), invocation -> {
                    CompletableFuture<Object> pending = new CompletableFuture<>();
                    pendingTool.complete(pending);
                    return pending;
                });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig()
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setTools(List.of(slowTool)))
                    .get(60, TimeUnit.SECONDS);
            try {
                session.send(new MessageOptions()
                        .setPrompt("Use slow_analysis with value 'test_abort'. Wait for the result."))
                        .get(60, TimeUnit.SECONDS);

                var pending = pendingTool.get(60, TimeUnit.SECONDS);
                session.close();
                assertThrows(CancellationException.class, () -> pending.get(60, TimeUnit.SECONDS));
                assertTrue(pending.isCancelled(), "Tool handler future should be cancelled");
            } finally {
                if (session != null) {
                    session.close();
                }
            }
        }
    }

    private static Map<String, Object> slowAnalysisSchema() {
        Map<String, Object> props = new HashMap<>();
        props.put("value", Map.of("type", "string", "description", "Value to analyze"));
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", List.of("value"));
        return schema;
    }
}
