/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.generated.rpc.SessionAuthGetStatusResult;
import com.github.copilot.sdk.json.CopilotClientOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.SessionConfig;

/**
 * Tests for per-session GitHub authentication.
 *
 * <p>
 * These tests verify that a per-session GitHub token is resolved into a full
 * identity by the CLI runtime and that sessions with different tokens are
 * isolated from each other.
 * </p>
 */
public class PerSessionAuthTest {

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
     * Creates a CopilotClient with the GitHub API URL redirected to the proxy so
     * that per-session auth token resolution (fetchCopilotUser) is intercepted.
     */
    private CopilotClient createAuthTestClient() {
        Map<String, String> env = new HashMap<>(ctx.getEnvironment());
        env.put("COPILOT_DEBUG_GITHUB_API_URL", ctx.getProxyUrl());
        return ctx.createClient(new CopilotClientOptions().setEnvironment(env));
    }

    private void setupCopilotUsers() throws Exception {
        // Initialize proxy state before registering tokens — the proxy requires its
        // internal state to be initialized (via /config) before it can handle the
        // /copilot_internal/user endpoint used for per-session auth resolution.
        ctx.initializeProxy();
        ctx.setCopilotUserByToken("token-alice", "alice", "individual_pro", ctx.getProxyUrl(),
                "https://localhost:1/telemetry", "alice-tracking-id");
        ctx.setCopilotUserByToken("token-bob", "bob", "business", ctx.getProxyUrl(), "https://localhost:1/telemetry",
                "bob-tracking-id");
    }

    @Test
    void shouldAuthenticateWithGitHubToken() throws Exception {
        setupCopilotUsers();

        try (CopilotClient client = createAuthTestClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setGitHubToken("token-alice")
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            try {
                SessionAuthGetStatusResult authStatus = session.getRpc().auth.getStatus().get();

                assertTrue(authStatus.isAuthenticated(), "Expected session to be authenticated");
                assertEquals("alice", authStatus.login());
            } finally {
                session.close();
            }
        }
    }

    @Test
    void shouldIsolateAuthBetweenSessions() throws Exception {
        setupCopilotUsers();

        try (CopilotClient client = createAuthTestClient()) {
            CopilotSession sessionA = client.createSession(new SessionConfig().setGitHubToken("token-alice")
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();
            CopilotSession sessionB = client.createSession(new SessionConfig().setGitHubToken("token-bob")
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            try {
                SessionAuthGetStatusResult statusA = sessionA.getRpc().auth.getStatus().get();
                SessionAuthGetStatusResult statusB = sessionB.getRpc().auth.getStatus().get();

                assertTrue(statusA.isAuthenticated(), "Expected session A to be authenticated");
                assertEquals("alice", statusA.login());

                assertTrue(statusB.isAuthenticated(), "Expected session B to be authenticated");
                assertEquals("bob", statusB.login());
            } finally {
                sessionA.close();
                sessionB.close();
            }
        }
    }

    @Test
    void shouldBeUnauthenticatedWithoutToken() throws Exception {
        try (CopilotClient client = createAuthTestClient()) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            try {
                SessionAuthGetStatusResult authStatus = session.getRpc().auth.getStatus().get();

                // Without a per-session token, there is no per-session identity.
                // In CI the process-level fake token may still authenticate globally,
                // so we check login rather than isAuthenticated.
                assertNull(authStatus.login(), "Expected no login without per-session token");
            } finally {
                session.close();
            }
        }
    }

    @Test
    void shouldFailWithInvalidToken() throws Exception {
        setupCopilotUsers();

        try (CopilotClient client = createAuthTestClient()) {
            Exception ex = assertThrows(Exception.class, () -> {
                CopilotSession session = client.createSession(new SessionConfig().setGitHubToken("invalid-token")
                        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();
                session.close();
            });

            assertNotNull(ex);
        }
    }
}
