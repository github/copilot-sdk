/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.rpc.McpAuthResult;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;

class McpOAuthResumeE2ETest {

    private static final String SNAPSHOT = "resumes_a_persisted_session_from_a_new_client_when_an_mcp_oauth_handler_is_configured";

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
    @Tag("isolated-resume")
    void resumesAPersistedSessionFromANewClientWhenAnMcpOauthHandlerIsConfigured() throws Exception {
        ctx.configureForTest("session", SNAPSHOT);

        String sessionId;
        try (var client = ctx.createClient();
                var session = client
                        .createSession(
                                new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                        .setOnMcpAuthRequest((request, invocation) -> CompletableFuture
                                                .completedFuture(McpAuthResult.cancelled())))
                        .get(30, TimeUnit.SECONDS)) {
            sessionId = session.getSessionId();

            AssistantMessageEvent response = session.sendAndWait(new MessageOptions().setPrompt("What is 1+1?"), 60_000)
                    .get(90, TimeUnit.SECONDS);
            assertNotNull(response);
            assertTrue(response.getData().content().contains("2"),
                    "Response should contain 2: " + response.getData().content());
        }

        try (var client = ctx.createClient();
                var session = client
                        .resumeSession(sessionId,
                                new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                        .setOnMcpAuthRequest((request, invocation) -> CompletableFuture
                                                .completedFuture(McpAuthResult.cancelled())))
                        .get(30, TimeUnit.SECONDS)) {
            assertEquals(sessionId, session.getSessionId());
        }
    }
}
