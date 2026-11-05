/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.AllowCopilotExperimental;
import com.github.copilot.CopilotClient;
import com.github.copilot.CopilotSession;
import com.github.copilot.E2ETestContext;
import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.generated.SessionIdleEvent;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.PingResponse;
import com.github.copilot.rpc.RuntimeConnection;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.ToolDefinition;

/**
 * Failsafe integration test for the in-process (FFI) transport.
 *
 * <p>
 * Loads the real {@code runtime.node} native library into this test process via
 * {@link com.github.copilot.ffi.FfiRuntimeHost}. Coverage includes both a
 * purely local {@code ping} round-trip and a replay-backed session turn that
 * exercises authentication, tool initialization, model traffic, and
 * asynchronous session event delivery over the FFI transport.
 *
 * <p>
 * Each fixture supplies typed network, token and home options. Separate hosts
 * use their own replay proxy and home without mutating the JVM's process
 * environment or working directory.
 *
 * <p>
 * Run with {@code mvn verify -Pinprocess} from the {@code java} reactor root,
 * which builds the {@code copilot-sdk-java-runtime} artifact and sets the
 * classifier JAR containing {@code runtime.node}, and keeps {@code forkCount=1}
 * while independent integration-test classes run concurrently.
 *
 * <p>
 * {@link RequireInProcess} disables this test unless the {@code -Pinprocess}
 * profile is active: without it, the {@code copilot-sdk-java-runtime}
 * classifier JAR providing {@code runtime.node} is not on the classpath, so the
 * test would fail with a {@code FileNotFoundException} rather than being
 * skipped.
 */
@AllowCopilotExperimental
@RequireInProcess
class InProcessTransportIT {

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
    void shouldStartPingAndStopOverInProcessFfi() throws Exception {
        CopilotClientOptions options = new CopilotClientOptions().setConnection(RuntimeConnection.forInProcess());
        try (CopilotClient client = ctx.createClient(options)) {
            client.start().get();

            PingResponse pong = client.ping("ffi message").get();
            assertEquals("pong: ffi message", pong.message());
            assertNotNull(pong.timestamp());

            client.stop().get();
        }
    }

    @Test
    void shouldCreateSessionAndCompleteTurnOverInProcessFfi() throws Exception {
        ctx.configureForTest("session", "should_receive_session_events");

        try (CopilotClient client = ctx.createClient();
                CopilotSession session = client
                        .createSession(ctx.newSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                        .get(30, TimeUnit.SECONDS)) {
            var idleReceived = new CompletableFuture<SessionIdleEvent>();
            session.on(SessionIdleEvent.class, idleReceived::complete);

            AssistantMessageEvent response = session.sendAndWait(new MessageOptions().setPrompt("What is 100+200?"))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            assertEquals("100 + 200 = 300", response.getData().content());
            assertNotNull(idleReceived.get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void shouldKeepIndependentJavaCallbacksAliveAfterAnotherHostStops() throws Exception {
        var firstReached = new CompletableFuture<Void>();
        var secondReached = new CompletableFuture<Void>();
        var firstResult = new CompletableFuture<Object>();
        var secondResult = new CompletableFuture<Object>();
        try (var firstContext = E2ETestContext.create(); var secondContext = E2ETestContext.create()) {
            configureIndependentHost(firstContext, "java-first");
            configureIndependentHost(secondContext, "java-second");
            try (var first = firstContext.createClient(new CopilotClientOptions()
                    .setConnection(RuntimeConnection.forInProcess()).setGitHubToken("java-first"));
                    var second = secondContext.createClient(new CopilotClientOptions()
                            .setConnection(RuntimeConnection.forInProcess()).setGitHubToken("java-second"))) {
                CompletableFuture.allOf(first.start(), second.start()).get(30, TimeUnit.SECONDS);
                try (var firstSession = first.createSession(gatedSession(firstContext, firstReached, firstResult))
                        .get(30, TimeUnit.SECONDS);
                        var secondSession = second
                                .createSession(gatedSession(secondContext, secondReached, secondResult))
                                .get(30, TimeUnit.SECONDS)) {
                    assertEquals("java-first",
                            firstSession.getRpc().gitHubAuth.getStatus().get(30, TimeUnit.SECONDS).login());
                    assertEquals("java-second",
                            secondSession.getRpc().gitHubAuth.getStatus().get(30, TimeUnit.SECONDS).login());
                    var prompt = new MessageOptions().setPrompt("Use encrypt_string to encrypt this string: Hello");
                    var firstTurn = firstSession.sendAndWait(prompt);
                    var secondTurn = secondSession.sendAndWait(prompt);
                    try {
                        CompletableFuture.allOf(firstReached, secondReached).get(30, TimeUnit.SECONDS);
                        assertFalse(firstTurn.isDone(), "The first callback must still be held");
                        assertFalse(secondTurn.isDone(), "The second callback must still be held");
                        firstResult.complete("HELLO");
                        assertTrue(firstTurn.get(60, TimeUnit.SECONDS).getData().content().contains("HELLO"));
                        firstSession.close();
                        first.stop().get(30, TimeUnit.SECONDS);

                        assertFalse(secondTurn.isDone(), "Stopping the first host must not finish the second callback");
                        secondResult.complete("HELLO");
                        assertTrue(secondTurn.get(60, TimeUnit.SECONDS).getData().content().contains("HELLO"));
                        assertEquals("pong: survivor", second.ping("survivor").get(30, TimeUnit.SECONDS).message());
                    } finally {
                        firstResult.complete("HELLO");
                        secondResult.complete("HELLO");
                    }
                }
            }
        }
    }

    private static void configureIndependentHost(E2ETestContext context, String token) throws Exception {
        context.configureForTest("tools", "invokes_custom_tool");
        context.setCopilotUserByToken(token,
                Map.of("login", token, "id", 12345, "copilot_plan", "individual_pro", "endpoints",
                        Map.of("api", context.getProxyUrl(), "telemetry", context.getProxyUrl()),
                        "analytics_tracking_id", token));
    }

    private static SessionConfig gatedSession(E2ETestContext context, CompletableFuture<Void> reached,
            CompletableFuture<Object> result) {
        var tool = ToolDefinition.create("encrypt_string", "Encrypts a string",
                Map.of("type", "object", "properties",
                        Map.of("input", Map.of("type", "string", "description", "String to encrypt")), "required",
                        List.of("input")),
                invocation -> {
                    assertEquals("Hello", invocation.getArguments().get("input"));
                    reached.complete(null);
                    return result;
                });
        return context.newSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setTools(List.of(tool));
    }
}
