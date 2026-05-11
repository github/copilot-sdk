/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.generated.ExitPlanModeCompletedEvent;
import com.github.copilot.sdk.generated.ExitPlanModeRequestedEvent;
import com.github.copilot.sdk.json.AutoModeSwitchRequest;
import com.github.copilot.sdk.json.AutoModeSwitchResponse;
import com.github.copilot.sdk.json.CopilotClientOptions;
import com.github.copilot.sdk.json.ExitPlanModeRequest;
import com.github.copilot.sdk.json.ExitPlanModeResult;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.SessionConfig;

/**
 * E2E tests for exit-plan-mode and auto-mode-switch handler APIs.
 *
 * <p>
 * Ported from {@code ModeHandlersE2ETests.cs} in the reference implementation
 * dotnet SDK.
 * </p>
 */
public class ModeHandlersTest {

    private static final String TOKEN = "mode-handler-token";

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

    private CopilotClient createAuthenticatedClient() {
        Map<String, String> env = new HashMap<>(ctx.getEnvironment());
        env.put("COPILOT_DEBUG_GITHUB_API_URL", ctx.getProxyUrl());

        return ctx.createClient(new CopilotClientOptions().setEnvironment(env));
    }

    private void configureAuthenticatedUser() throws Exception {
        ctx.setCopilotUserByToken(TOKEN, "mode-handler-user", "individual_pro", ctx.getProxyUrl(),
                "https://localhost:1/telemetry", "mode-handler-tracking-id");
    }

    @Test
    void shouldInvokeExitPlanModeHandlerWhenModelUsesTool() throws Exception {
        final String summary = "Greeting file implementation plan";
        configureAuthenticatedUser();

        var handlerCalled = new CompletableFuture<ExitPlanModeRequest>();

        try (var client = createAuthenticatedClient()) {
            var session = client.createSession(new SessionConfig().setGitHubToken(TOKEN)
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setOnExitPlanMode((request, invocation) -> {
                        handlerCalled.complete(request);
                        return CompletableFuture.completedFuture(new ExitPlanModeResult().setApproved(true)
                                .setSelectedAction("interactive").setFeedback("Approved by the Java E2E test"));
                    })).get(30, TimeUnit.SECONDS);

            var requestedEvent = new CompletableFuture<ExitPlanModeRequestedEvent>();
            var completedEvent = new CompletableFuture<ExitPlanModeCompletedEvent>();

            session.on(event -> {
                if (event instanceof ExitPlanModeRequestedEvent requested
                        && summary.equals(requested.getData().summary())) {
                    requestedEvent.complete(requested);
                } else if (event instanceof ExitPlanModeCompletedEvent completed
                        && Boolean.TRUE.equals(completed.getData().approved())
                        && "interactive".equals(completed.getData().selectedAction())) {
                    completedEvent.complete(completed);
                }
            });

            var response = session.sendAndWait(new MessageOptions().setPrompt(
                    "Create a brief implementation plan for adding a greeting.txt file, then request approval with exit_plan_mode.")
                    .setMode("plan")).get(120, TimeUnit.SECONDS);

            var request = handlerCalled.get(10, TimeUnit.SECONDS);
            assertEquals(summary, request.getSummary());
            assertNotNull(request.getActions());
            assertTrue(request.getActions().contains("interactive"));
            assertNotNull(request.getPlanContent());

            var reqEvent = requestedEvent.get(10, TimeUnit.SECONDS);
            assertEquals(request.getSummary(), reqEvent.getData().summary());

            var compEvent = completedEvent.get(10, TimeUnit.SECONDS);
            assertTrue(compEvent.getData().approved());
            assertEquals("interactive", compEvent.getData().selectedAction());

            assertNotNull(response);

            session.close();
        }
    }

    @Test
    void shouldInvokeAutoModeSwitchHandlerWhenRateLimited() throws Exception {
        configureAuthenticatedUser();

        var handlerCalled = new CompletableFuture<AutoModeSwitchRequest>();

        try (var client = createAuthenticatedClient()) {
            var session = client.createSession(
                    new SessionConfig().setGitHubToken(TOKEN).setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setOnAutoModeSwitch((request, invocation) -> {
                                handlerCalled.complete(request);
                                return CompletableFuture.completedFuture(AutoModeSwitchResponse.YES);
                            }))
                    .get(30, TimeUnit.SECONDS);

            session.sendAndWait(new MessageOptions()
                    .setPrompt("Explain that auto mode recovered from a rate limit in one short sentence."))
                    .get(30, TimeUnit.SECONDS);

            var request = handlerCalled.get(30, TimeUnit.SECONDS);
            assertEquals("user_weekly_rate_limited", request.getErrorCode());
            assertEquals(1.0, request.getRetryAfterSeconds());

            session.close();
        }
    }
}
