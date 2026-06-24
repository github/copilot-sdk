/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.rpc.SessionAuthGetStatusResult;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

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
        Map<String, String> env = new HashMap<>(ctx.getEnvironment());
        env.put("COPILOT_DEBUG_GITHUB_API_URL", ctx.getProxyUrl());
        // Strip global auth tokens so there is no global identity to fall back to,
        // mirroring the Go/Node per-session-auth "without token" tests. Otherwise the
        // process-level fake token resolves to the default e2e user registered on the
        // proxy and the session reports a login.
        env.put("GH_TOKEN", "");
        env.put("GITHUB_TOKEN", "");
        env.put("COPILOT_SDK_AUTH_TOKEN", "");

        // Build the client directly (not via ctx.createClient) so the context's
        // default GitHub token is not auto-injected and useLoggedInUser is disabled.
        CopilotClientOptions options = new CopilotClientOptions().setCliPath(ctx.getCliPath())
                .setCwd(ctx.getWorkDir().toString()).setEnvironment(env).setUseLoggedInUser(false);

        try (CopilotClient client = new CopilotClient(options)) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            try {
                SessionAuthGetStatusResult authStatus = session.getRpc().auth.getStatus().get();

                // With no global or per-session token, there is no identity at all.
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
