/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.github.copilot.sdk.events.AssistantMessageEvent;
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

    @Test
    void testPermissionHandlerForWriteOperations(TestInfo testInfo) throws Exception {
        ctx.configureForTest("permissions", "permission_handler_for_write_operations");

        List<PermissionRequest> permissionRequests = new ArrayList<>();

        final String[] sessionIdHolder = new String[1];

        SessionConfig config = new SessionConfig().setOnPermissionRequest((request, invocation) -> {
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

    @Test
    void testDenyPermission(TestInfo testInfo) throws Exception {
        ctx.configureForTest("permissions", "deny_permission");

        SessionConfig config = new SessionConfig().setOnPermissionRequest((request, invocation) -> {
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

    @Test
    void testWithoutPermissionHandler(TestInfo testInfo) throws Exception {
        ctx.configureForTest("permissions", "without_permission_handler");

        try (CopilotClient client = ctx.createClient()) {
            // Create session without onPermissionRequest handler
            CopilotSession session = client.createSession().get();

            AssistantMessageEvent response = session.sendAndWait(new MessageOptions().setPrompt("What is 2+2?")).get(60,
                    TimeUnit.SECONDS);

            assertNotNull(response);
            assertTrue(response.getData().getContent().contains("4"),
                    "Response should contain 4: " + response.getData().getContent());

            session.close();
        }
    }

    @Test
    void testAsyncPermissionHandler(TestInfo testInfo) throws Exception {
        ctx.configureForTest("permissions", "async_permission_handler");

        List<PermissionRequest> permissionRequests = new ArrayList<>();

        SessionConfig config = new SessionConfig().setOnPermissionRequest((request, invocation) -> {
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

    @Test
    void testResumeSessionWithPermissionHandler(TestInfo testInfo) throws Exception {
        ctx.configureForTest("permissions", "resume_session_with_permission_handler");

        List<PermissionRequest> permissionRequests = new ArrayList<>();

        try (CopilotClient client = ctx.createClient()) {
            // Create session without permission handler
            CopilotSession session1 = client.createSession().get();
            String sessionId = session1.getSessionId();
            session1.sendAndWait(new MessageOptions().setPrompt("What is 1+1?")).get(60, TimeUnit.SECONDS);

            // Resume with permission handler
            ResumeSessionConfig resumeConfig = new ResumeSessionConfig()
                    .setOnPermissionRequest((request, invocation) -> {
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

    @Test
    void testToolCallIdInPermissionRequests(TestInfo testInfo) throws Exception {
        ctx.configureForTest("permissions", "tool_call_id_in_permission_requests");

        final boolean[] receivedToolCallId = {false};

        SessionConfig config = new SessionConfig().setOnPermissionRequest((request, invocation) -> {
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
     * Note: This test verifies error handling in permission handlers. When the
     * handler throws an exception, the SDK should deny the permission and the
     * assistant should indicate it couldn't complete the task.
     *
     * Currently disabled because the test is flaky and requires proper error
     * handling infrastructure that returns a response promptly when permission is
     * denied due to errors.
     */
    @Disabled("Requires improved error handling for permission handler exceptions")
    @Test
    void testPermissionHandlerErrorsGracefully(TestInfo testInfo) throws Exception {
        ctx.configureForTest("permissions", "permission_handler_errors_gracefully");

        SessionConfig config = new SessionConfig().setOnPermissionRequest((request, invocation) -> {
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
            String content = response.getData().getContent().toLowerCase();
            assertTrue(content.contains("fail") || content.contains("cannot") || content.contains("unable")
                    || content.contains("permission"), "Response should indicate failure: " + content);

            session.close();
        }
    }
}
