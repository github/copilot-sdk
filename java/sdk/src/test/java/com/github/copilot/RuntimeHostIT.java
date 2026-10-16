/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.copilot.generated.rpc.HostLocalServerOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SessionHooks;
import com.github.copilot.rpc.SystemMessageConfig;
import com.github.copilot.rpc.ToolDefinition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@AllowCopilotExperimental
@Timeout(120)
@EnabledIfEnvironmentVariable(named = "COPILOT_RUNTIME_HOST_E2E", matches = "1")
class RuntimeHostIT {
    private static final String TOOL_PROMPT = "Use the magic_number tool with seed 'hello' and tell me the result";
    private static final String COMPOSED_PROMPT = "Call magic_number with seed 'hello' and client_echo with text 'ping', then report both results";
    private static final String MARKER = "APPLICATION_OWNED_AHP_PROMPT";

    public record MagicInput(String seed) {
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(30, TimeUnit.SECONDS);
    }

    private static final class Application {
        final E2ETestContext context;
        final CopilotClient client;
        volatile CopilotSession session;
        volatile String createSessionId;
        volatile String resumeSessionId;
        final AtomicInteger creates = new AtomicInteger();
        final AtomicInteger resumes = new AtomicInteger();
        final AtomicInteger toolCalls = new AtomicInteger();
        final AtomicInteger exits = new AtomicInteger();
        final CopyOnWriteArrayList<String> hookSessions = new CopyOnWriteArrayList<>();
        final CopyOnWriteArrayList<CopilotSession> releases = new CopyOnWriteArrayList<>();
        final CompletableFuture<Void> released = new CompletableFuture<>();
        final CompletableFuture<Void> exited = new CompletableFuture<>();

        Application(E2ETestContext context, CopilotClient client) {
            this.context = context;
            this.client = client;
        }

        List<ToolDefinition> tools() {
            return List
                    .of(ToolDefinition.create(
                            "magic_number", "Returns a magic number", Map.of("type", "object", "properties",
                                    Map.of("seed", Map.of("type", "string")), "required", List.of("seed")),
                            invocation -> {
                                assertEquals("hello", invocation.getArgumentsAs(MagicInput.class).seed());
                                toolCalls.incrementAndGet();
                                return CompletableFuture.completedFuture("MAGIC_hello_42");
                            }));
        }

        SessionHooks hooks() {
            return new SessionHooks().setOnPreToolUse((input, invocation) -> {
                hookSessions.add(invocation.getSessionId());
                return CompletableFuture.completedFuture(null);
            });
        }

        SystemMessageConfig prompt() {
            return new SystemMessageConfig().setMode(SystemMessageMode.APPEND).setContent(MARKER);
        }

        SessionConfig configure(SessionConfig config) {
            return config.setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setTools(tools()).setHooks(hooks())
                    .setSystemMessage(prompt());
        }

        ResumeSessionConfig configure(ResumeSessionConfig config) {
            return config.setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setTools(tools()).setHooks(hooks())
                    .setSystemMessage(prompt());
        }

        AhpHostOptions options() {
            return new AhpHostOptions().setLocalServer(new HostLocalServerOptions(null, null, null, null))
                    .setCreateSession(request -> {
                        creates.incrementAndGet();
                        assertFalse(request.cancellation().toCompletableFuture().isDone());
                        assertEquals(context.getWorkDir().toString(), request.config().getWorkingDirectory());
                        createSessionId = request.config().getSessionId();
                        assertNotNull(createSessionId);
                        assertFalse(createSessionId.isEmpty());
                        return client.createSession(configure(request.config())).thenApply(value -> {
                            assertEquals(createSessionId, value.getSessionId());
                            return session = value;
                        });
                    }).setResumeSession(request -> {
                        resumes.incrementAndGet();
                        assertFalse(request.cancellation().toCompletableFuture().isDone());
                        assertEquals(false, request.config().getContinuePendingWork().orElseThrow());
                        assertEquals(context.getWorkDir().toString(), request.config().getWorkingDirectory());
                        resumeSessionId = request.sessionId();
                        return client.resumeSession(request.sessionId(), configure(request.config()))
                                .thenApply(value -> {
                                    assertEquals(resumeSessionId, value.getSessionId());
                                    return session = value;
                                });
                    }).setOnSessionReleased(value -> {
                        releases.add(value);
                        released.complete(null);
                        return CompletableFuture.completedFuture(null);
                    }).setOnExit(info -> {
                        exits.incrementAndGet();
                        exited.complete(null);
                        return CompletableFuture.completedFuture(null);
                    });
        }

        void assertCallbacks(String sessionId) throws Exception {
            assertEquals(1, toolCalls.get());
            assertTrue(hookSessions.contains(sessionId));
            JsonNode exchanges = JsonRpcClient.getObjectMapper().valueToTree(context.getExchanges());
            assertTrue(exchanges.toString().contains(MARKER));
            assertTrue(exchanges.toString().contains("\"name\":\"magic_number\""));
        }

        void assertReleased() throws Exception {
            await(released);
            assertEquals(1, releases.size());
            assertSame(session, releases.get(0));
        }
    }

    private static String connect(AhpTestClient ahp, AhpHost host, String clientId) throws Exception {
        assertNotNull(host.getUrl());
        var command = new HashMap<String, Object>(
                Map.of("op", "connect", "url", host.getUrl(), "githubToken", "fake-token-for-e2e-tests"));
        if (host.getToken() != null) {
            command.put("token", host.getToken());
        }
        if (clientId != null) {
            command.put("clientId", clientId);
        }
        return ahp.request(command).get("clientId").asText();
    }

    private static void stopped(AhpTestClient ahp, AhpHost host, String clientId) throws Exception {
        ahp.request(Map.of("op", "stopped", "clientId", clientId, "url", host.getUrl()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void createsOrPublishesExactApplicationSession(boolean publish) throws Exception {
        try (var context = E2ETestContext.create()) {
            context.configureForTest("multi_client", "both_clients_see_tool_request_and_completion_events");
            try (var owner = context.createClient(); var ahp = new AhpTestClient(context.getRepoRoot())) {
                var app = new Application(context, owner);
                if (publish) {
                    app.session = await(owner.createSession(
                            app.configure(new SessionConfig().setWorkingDirectory(context.getWorkDir().toString()))));
                }
                try (var host = await(owner.startAhpHost(app.options()))) {
                    assertNull(host.getPid());
                    String clientId = connect(ahp, host, null);
                    String sessionId;
                    if (publish) {
                        sessionId = app.session.getSessionId();
                        var publication = await(host.publishSession(sessionId));
                        assertEquals(sessionId, publication.sessionId());
                        assertEquals("ahp-session:/" + sessionId, publication.sessionUri());
                        ahp.request(Map.of("op", "attach", "clientId", clientId, "sessionId", sessionId));
                    } else {
                        sessionId = ahp.request(Map.of("op", "create", "clientId", clientId, "workDir",
                                context.getWorkDir().toString())).get("sessionId").asText();
                        assertEquals(app.createSessionId, app.session.getSessionId());
                        assertNotEquals(sessionId, app.session.getSessionId());
                        assertEquals(1, app.creates.get());
                    }
                    var response = ahp.request(
                            Map.of("op", "turn", "clientId", clientId, "sessionId", sessionId, "prompt", TOOL_PROMPT));
                    assertTrue(response.get("text").asText().contains("MAGIC_hello_42"));
                    app.assertCallbacks(app.session.getSessionId());
                    await(CompletableFuture.allOf(host.dispose(), host.dispose()));
                    stopped(ahp, host, clientId);
                    if (publish) {
                        assertEquals(0, app.creates.get());
                        assertEquals(0, app.resumes.get());
                        assertTrue(app.releases.isEmpty());
                    } else {
                        app.assertReleased();
                    }
                    await(app.exited);
                    assertEquals(1, app.exits.get());
                    assertFalse(await(app.session.getMessages()).isEmpty());
                    assertEquals("pong: still alive", await(owner.ping("still alive")).message());
                }
            }
        }
    }

    @Test
    void resumesAfterRuntimeRestartAndComposesTools() throws Exception {
        try (var context = E2ETestContext.create(); var ahp = new AhpTestClient(context.getRepoRoot())) {
            context.configureForTest("runtime_host", "app_resume_callback_composes_tools_after_history");
            String clientId;
            String sessionId;
            CopilotSession original;
            try (var firstOwner = context.createClient()) {
                var app = new Application(context, firstOwner);
                try (var host = await(firstOwner.startAhpHost(app.options()))) {
                    clientId = connect(ahp, host, null);
                    sessionId = ahp.request(Map.of("op", "create", "clientId", clientId, "workDir",
                            context.getWorkDir().toString(), "clientTools", true)).get("sessionId").asText();
                    original = app.session;
                    assertEquals(1, app.creates.get());
                    assertEquals(app.createSessionId, original.getSessionId());
                    assertNotEquals(sessionId, original.getSessionId());
                    assertTrue(ahp.request(Map.of("op", "turn", "clientId", clientId, "sessionId", sessionId, "prompt",
                            "What is 2+2?")).get("text").asText().contains("4"));
                    await(host.dispose());
                    stopped(ahp, host, clientId);
                    app.assertReleased();
                }
                ahp.request(Map.of("op", "close", "clientId", clientId));
            }
            try (var resumedOwner = context.createClient()) {
                var app = new Application(context, resumedOwner);
                try (var host = await(resumedOwner.startAhpHost(app.options()))) {
                    assertNull(host.getPid());
                    connect(ahp, host, clientId);
                    var attached = ahp.request(
                            Map.of("op", "attach", "clientId", clientId, "sessionId", sessionId, "clientTools", true));
                    assertEquals(1, attached.get("history").size());
                    assertEquals("What is 2+2?", attached.get("history").get(0).get("message").get("text").asText());
                    assertEquals(0, app.creates.get());
                    assertEquals(1, app.resumes.get());
                    assertEquals(original.getSessionId(), app.resumeSessionId);
                    assertEquals(original.getSessionId(), app.session.getSessionId());
                    assertNotSame(original, app.session);
                    var response = ahp.request(Map.of("op", "turn", "clientId", clientId, "sessionId", sessionId,
                            "prompt", COMPOSED_PROMPT, "clientTools", true));
                    assertTrue(response.get("text").asText().contains("MAGIC_hello_42"));
                    assertTrue(response.get("text").asText().contains("CLIENT_ECHO_ping"));
                    assertEquals(1, response.get("clientToolCalls").asInt());
                    app.assertCallbacks(original.getSessionId());
                    await(host.dispose());
                    stopped(ahp, host, clientId);
                    app.assertReleased();
                    assertFalse(await(app.session.getMessages()).isEmpty());
                }
            }
        }
    }
}
