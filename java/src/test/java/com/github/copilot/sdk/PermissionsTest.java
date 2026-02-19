/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.events.ToolExecutionCompleteEvent;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.PermissionRequest;
import com.github.copilot.sdk.json.PermissionRequestResult;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.ResumeSessionConfig;
import com.github.copilot.sdk.json.MessageOptions;

/**
 * Tests for permission callback functionality.
 *
 * <p>
 * These tests use the shared CapiProxy infrastructure for deterministic API
 * response replay. Snapshots are stored in test/snapshots/permissions/.
 * </p>
 */
public class PermissionsTest {

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
     * Verifies that permission handler is invoked for write operations.
     *
     * @see Snapshot: permissions/permission_handler_for_write_operations
     */
    @Test
    void testPermissionHandlerForWriteOperations(TestInfo testInfo) throws Exception {
        ctx.configureForTest("permissions", "permission_handler_for_write_operations");

        var permissionRequests = new ArrayList<PermissionRequest>();

        final String[] sessionIdHolder = new String[1];

        var config = new SessionConfig().setOnPermissionRequest((request, invocation) -> {
            permissionRequests.add(request);
            assertEquals(sessionIdHolder[0], invocation.getSessionId());
            // Approve the permission
            return CompletableFuture.completedFuture(new PermissionRequestResult().setKind("approved"));
        });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();
            sessionIdHolder[0] = session.getSessionId();

            // Write a test file
            Path testFile = ctx.getWorkDir().resolve("test.txt");
            Files.writeString(testFile, "original content");

            session.sendAndWait(new MessageOptions().setPrompt("Edit test.txt and replace 'original' with 'modified'"))
                    .get(60, TimeUnit.SECONDS);

            // Should have received at least one permission request
            assertFalse(permissionRequests.isEmpty(), "Should have received permission requests");

            // Should include write permission request
            boolean hasWriteRequest = permissionRequests.stream().anyMatch(req -> "write".equals(req.getKind()));
            assertTrue(hasWriteRequest, "Should have received a write permission request");

            session.close();
        }
    }

    /**
     * Verifies that permissions can be denied.
     *
     * @see Snapshot: permissions/deny_permission
     */
    @Test
    void testDenyPermission(TestInfo testInfo) throws Exception {
        ctx.configureForTest("permissions", "deny_permission");

        var config = new SessionConfig().setOnPermissionRequest((request, invocation) -> {
            // Deny all permissions
            return CompletableFuture
                    .completedFuture(new PermissionRequestResult().setKind("denied-interactively-by-user"));
        });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();

            String originalContent = "protected content";
            Path testFile = ctx.getWorkDir().resolve("protected.txt");
            Files.writeString(testFile, originalContent);

            session.sendAndWait(
                    new MessageOptions().setPrompt("Edit protected.txt and replace 'protected' with 'hacked'."))
                    .get(60, TimeUnit.SECONDS);

            // Verify the file was NOT modified
            String content = Files.readString(testFile);
            assertEquals(originalContent, content, "File should not have been modified");

            session.close();
        }
    }

    /**
     * Verifies that sessions work without a permission handler.
     *
     * @see Snapshot: permissions/without_permission_handler
     */
    @Test
    void testWithoutPermissionHandler(TestInfo testInfo) throws Exception {
        ctx.configureForTest("permissions", "without_permission_handler");

        try (CopilotClient client = ctx.createClient()) {
            // Create session without onPermissionRequest handler
            CopilotSession session = client.createSession().get();

            AssistantMessageEvent response = session.sendAndWait(new MessageOptions().setPrompt("What is 2+2?")).get(60,
                    TimeUnit.SECONDS);

            assertNotNull(response);
            assertTrue(response.getData().content().contains("4"),
                    "Response should contain 4: " + response.getData().content());

            session.close();
        }
    }

    /**
     * Verifies that async permission handlers work correctly.
     *
     * @see Snapshot: permissions/async_permission_handler
     */
    @Test
    void testAsyncPermissionHandler(TestInfo testInfo) throws Exception {
        ctx.configureForTest("permissions", "async_permission_handler");

        var permissionRequests = new ArrayList<PermissionRequest>();

        var config = new SessionConfig().setOnPermissionRequest((request, invocation) -> {
            permissionRequests.add(request);

            // Simulate async permission check with delay
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(10); // Small delay to simulate async check
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return new PermissionRequestResult().setKind("approved");
            });
        });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();

            session.sendAndWait(new MessageOptions().setPrompt("Run 'echo test' and tell me what happens")).get(60,
                    TimeUnit.SECONDS);

            // Should have received permission requests
            assertFalse(permissionRequests.isEmpty(), "Should have received permission requests");

            session.close();
        }
    }

    /**
     * Verifies that permission handlers work when resuming a session.
     *
     * @see Snapshot: permissions/resume_session_with_permission_handler
     */
    @Test
    void testResumeSessionWithPermissionHandler(TestInfo testInfo) throws Exception {
        ctx.configureForTest("permissions", "resume_session_with_permission_handler");

        var permissionRequests = new ArrayList<PermissionRequest>();

        try (CopilotClient client = ctx.createClient()) {
            // Create session without permission handler
            CopilotSession session1 = client.createSession().get();
            String sessionId = session1.getSessionId();
            session1.sendAndWait(new MessageOptions().setPrompt("What is 1+1?")).get(60, TimeUnit.SECONDS);

            // Resume with permission handler
            var resumeConfig = new ResumeSessionConfig().setOnPermissionRequest((request, invocation) -> {
                permissionRequests.add(request);
                return CompletableFuture.completedFuture(new PermissionRequestResult().setKind("approved"));
            });

            CopilotSession session2 = client.resumeSession(sessionId, resumeConfig).get();

            assertEquals(sessionId, session2.getSessionId());

            session2.sendAndWait(new MessageOptions().setPrompt("Run 'echo resumed' for me")).get(60, TimeUnit.SECONDS);

            // Should have permission requests from resumed session
            assertFalse(permissionRequests.isEmpty(), "Should have received permission requests from resumed session");

            session2.close();
        }
    }

    /**
     * Verifies that tool call IDs are included in permission requests.
     *
     * @see Snapshot: permissions/tool_call_id_in_permission_requests
     */
    @Test
    void testToolCallIdInPermissionRequests(TestInfo testInfo) throws Exception {
        ctx.configureForTest("permissions", "tool_call_id_in_permission_requests");

        final boolean[] receivedToolCallId = {false};

        var config = new SessionConfig().setOnPermissionRequest((request, invocation) -> {
            if (request.getToolCallId() != null) {
                receivedToolCallId[0] = true;
                assertFalse(request.getToolCallId().isEmpty(), "Tool call ID should not be empty");
            }
            return CompletableFuture.completedFuture(new PermissionRequestResult().setKind("approved"));
        });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();

            session.sendAndWait(new MessageOptions().setPrompt("Run 'echo test'")).get(60, TimeUnit.SECONDS);

            assertTrue(receivedToolCallId[0], "Should have received toolCallId in permission request");

            session.close();
        }
    }

    /**
     * Verifies that permission handler errors are handled gracefully.
     * <p>
     * When the handler throws an exception, the SDK should deny the permission and
     * the assistant should indicate it couldn't complete the task.
     * </p>
     *
     * @see Snapshot: permissions/should_handle_permission_handler_errors_gracefully
     */
    @Test
    void testShouldHandlePermissionHandlerErrorsGracefully(TestInfo testInfo) throws Exception {
        ctx.configureForTest("permissions", "should_handle_permission_handler_errors_gracefully");

        var config = new SessionConfig().setOnPermissionRequest((request, invocation) -> {
            // Throw an error in the handler
            throw new RuntimeException("Handler error");
        });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();

            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions().setPrompt("Run 'echo test'. If you can't, say 'failed'."))
                    .get(60, TimeUnit.SECONDS);

            // Should handle the error and deny permission
            assertNotNull(response);
            String content = response.getData().content().toLowerCase();
            assertTrue(content.contains("fail") || content.contains("cannot") || content.contains("unable")
                    || content.contains("permission"), "Response should indicate failure: " + content);

            session.close();
        }
    }

    /**
     * Verifies that tool operations are denied by default when no handler is
     * provided.
     *
     * @see Snapshot:
     *      permissions/should_deny_tool_operations_by_default_when_no_handler_is_provided
     */
    @Test
    void testShouldDenyToolOperationsByDefaultWhenNoHandlerIsProvided(TestInfo testInfo) throws Exception {
        ctx.configureForTest("permissions", "should_deny_tool_operations_by_default_when_no_handler_is_provided");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();

            final boolean[] permissionDenied = {false};
            session.on(ToolExecutionCompleteEvent.class, evt -> {
                if (!evt.getData().success() && evt.getData().error() != null && evt.getData().error().message() != null
                        && evt.getData().error().message().contains("Permission denied")) {
                    permissionDenied[0] = true;
                }
            });

            session.sendAndWait(new MessageOptions().setPrompt("Run 'node --version'")).get(60, TimeUnit.SECONDS);

            assertTrue(permissionDenied[0], "Expected a tool.execution_complete event with Permission denied result");

            session.close();
        }
    }

    /**
     * Verifies that tool operations are denied by default when no handler is
     * provided after resuming a session.
     *
     * @see Snapshot:
     *      permissions/should_deny_tool_operations_by_default_when_no_handler_is_provided_after_resume
     */
    @Test
    void testShouldDenyToolOperationsByDefaultWhenNoHandlerIsProvidedAfterResume(TestInfo testInfo) throws Exception {
        ctx.configureForTest("permissions",
                "should_deny_tool_operations_by_default_when_no_handler_is_provided_after_resume");

        try (CopilotClient client = ctx.createClient()) {
            var config = new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL);
            CopilotSession session1 = client.createSession(config).get();
            String sessionId = session1.getSessionId();
            session1.sendAndWait(new MessageOptions().setPrompt("What is 1+1?")).get(60, TimeUnit.SECONDS);

            CopilotSession session2 = client.resumeSession(sessionId).get();

            final boolean[] permissionDenied = {false};
            session2.on(ToolExecutionCompleteEvent.class, evt -> {
                if (!evt.getData().success() && evt.getData().error() != null && evt.getData().error().message() != null
                        && evt.getData().error().message().contains("Permission denied")) {
                    permissionDenied[0] = true;
                }
            });

            session2.sendAndWait(new MessageOptions().setPrompt("Run 'node --version'")).get(60, TimeUnit.SECONDS);

            assertTrue(permissionDenied[0], "Expected a tool.execution_complete event with Permission denied result");

            session2.close();
        }
    }
}
