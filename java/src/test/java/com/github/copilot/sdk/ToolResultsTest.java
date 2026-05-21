/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.generated.SessionEvent;
import com.github.copilot.sdk.generated.ToolExecutionCompleteEvent;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.ToolDefinition;
import com.github.copilot.sdk.json.ToolResultObject;

/**
 * E2E tests for tool result types — verifying that rejected and denied result
 * types are handled correctly by the runtime.
 *
 * <p>
 * Snapshots are stored in {@code test/snapshots/tool_results/}.
 * </p>
 */
public class ToolResultsTest {

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
     * Verifies that a tool returning a "rejected" resultType is reported as a
     * failed tool execution with the correct error code.
     *
     * @see Snapshot:
     *      tool_results/should_handle_tool_result_with_rejected_resulttype
     */
    @Test
    void testShouldHandleToolResultWithRejectedResultType() throws Exception {
        ctx.configureForTest("tool_results", "should_handle_tool_result_with_rejected_resulttype");

        var toolHandlerCalled = new boolean[]{false};

        Map<String, Object> params = Map.of("type", "object", "properties", Map.of(), "required", List.of());

        ToolDefinition deployTool = ToolDefinition.create("deploy_service", "Deploys a service", params,
                (invocation) -> {
                    toolHandlerCalled[0] = true;
                    return CompletableFuture.completedFuture(new ToolResultObject("rejected",
                            "Deployment rejected: policy violation - production deployments require approval", null,
                            null, null, null));
                });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setTools(List.of(deployTool))
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            List<SessionEvent> events = new ArrayList<>();
            session.on(events::add);

            session.sendAndWait(new MessageOptions().setPrompt(
                    "Deploy the service using deploy_service. If it's rejected, tell me it was 'rejected by policy'."))
                    .get(60, TimeUnit.SECONDS);

            assertTrue(toolHandlerCalled[0], "Tool handler should have been called");

            List<ToolExecutionCompleteEvent> toolEvents = events.stream()
                    .filter(e -> e instanceof ToolExecutionCompleteEvent).map(e -> (ToolExecutionCompleteEvent) e)
                    .toList();
            assertFalse(toolEvents.isEmpty(), "Should have a tool.execution_complete event");

            ToolExecutionCompleteEvent toolEvt = toolEvents.get(0);
            assertFalse(toolEvt.getData().success(), "Tool execution should not be marked as successful");
            assertNotNull(toolEvt.getData().error(), "Should have error details");
            assertEquals("rejected", toolEvt.getData().error().code(), "Error code should be 'rejected'");

            session.close();
        }
    }

    /**
     * Verifies that a tool returning a "denied" resultType is reported as a failed
     * tool execution with the correct error code.
     *
     * @see Snapshot: tool_results/should_handle_tool_result_with_denied_resulttype
     */
    @Test
    void testShouldHandleToolResultWithDeniedResultType() throws Exception {
        ctx.configureForTest("tool_results", "should_handle_tool_result_with_denied_resulttype");

        var toolHandlerCalled = new boolean[]{false};

        Map<String, Object> params = Map.of("type", "object", "properties", Map.of(), "required", List.of());

        ToolDefinition accessTool = ToolDefinition.create("access_secret", "Accesses a secret", params,
                (invocation) -> {
                    toolHandlerCalled[0] = true;
                    return CompletableFuture.completedFuture(new ToolResultObject("denied",
                            "Access denied: insufficient permissions to read secrets", null, null, null, null));
                });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setTools(List.of(accessTool))
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            List<SessionEvent> events = new ArrayList<>();
            session.on(events::add);

            session.sendAndWait(new MessageOptions().setPrompt(
                    "Use access_secret to get the API key. If access is denied, tell me it was 'access denied'."))
                    .get(60, TimeUnit.SECONDS);

            assertTrue(toolHandlerCalled[0], "Tool handler should have been called");

            List<ToolExecutionCompleteEvent> toolEvents = events.stream()
                    .filter(e -> e instanceof ToolExecutionCompleteEvent).map(e -> (ToolExecutionCompleteEvent) e)
                    .toList();
            assertFalse(toolEvents.isEmpty(), "Should have a tool.execution_complete event");

            ToolExecutionCompleteEvent toolEvt = toolEvents.get(0);
            assertFalse(toolEvt.getData().success(), "Tool execution should not be marked as successful");
            assertNotNull(toolEvt.getData().error(), "Should have error details");
            assertEquals("denied", toolEvt.getData().error().code(), "Error code should be 'denied'");

            session.close();
        }
    }
}
