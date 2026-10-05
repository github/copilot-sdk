/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.HostCreateSessionParams;
import com.github.copilot.generated.rpc.HostGitHubEnvironmentOptions;
import com.github.copilot.generated.rpc.HostLocalServerOptions;
import com.github.copilot.generated.rpc.HostSessionCreateCallback;
import com.github.copilot.generated.rpc.ServerHostApi;
import com.github.copilot.generated.rpc.SessionLimitsConfig;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@AllowCopilotExperimental
class AhpHostTest {
    private static AhpHostOptions localOptions() {
        return new AhpHostOptions().setLocalServer(new HostLocalServerOptions(null, null, null, null));
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(10, TimeUnit.SECONDS);
    }

    @Test
    void requiresExplicitTransportBeforeConnecting() throws Exception {
        try (var server = new FakeRuntime(); var client = server.client()) {
            for (AhpHostOptions options : Arrays.asList(null, new AhpHostOptions())) {
                var error = assertThrows(Exception.class, () -> await(client.startAhpHost(options)));
                assertInstanceOf(IllegalArgumentException.class, error.getCause());
            }
            assertFalse(server.connected.isDone());
        }
    }

    @Test
    void forwardsEnvironmentFieldsForRuntimeValidation() throws Exception {
        for (String[] fields : new String[][]{{"", "compute"}, {" ", "compute"}, {"host", ""}, {"host", " "}}) {
            try (var server = new FakeRuntime();
                    var client = server.client();
                    var host = await(client.startAhpHost(new AhpHostOptions()
                            .setGithubEnvironment(new HostGitHubEnvironmentOptions(fields[0], fields[1]))))) {
                var environment = await(server.startRequest).path("githubEnvironment");
                assertEquals(fields[0], environment.path("name").asText());
                assertEquals(fields[1], environment.path("computeId").asText());
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"true,false", "false,true", "true,true"})
    void forwardsExplicitTransportsAndOptionalReadiness(boolean local, boolean github) throws Exception {
        try (var server = new FakeRuntime(); var client = server.client()) {
            var options = new AhpHostOptions();
            if (local) {
                options.setLocalServer(new HostLocalServerOptions("127.0.0.1", 0L, "test-token", true));
            }
            if (github) {
                options.setGithubEnvironment(new HostGitHubEnvironmentOptions("test host", "compute"));
            }
            try (var host = await(client.startAhpHost(options))) {
                var request = await(server.startRequest);
                assertEquals(local, request.hasNonNull("localServer"));
                assertEquals(github, request.hasNonNull("githubEnvironment"));
                for (String oldField : List.of("hostname", "port", "token", "requireConnectionToken")) {
                    assertFalse(request.has(oldField));
                }
                if (local) {
                    assertEquals("test-token", request.path("localServer").path("token").asText());
                    assertEquals(0, request.path("localServer").path("port").asInt());
                }
                if (github) {
                    assertEquals("test host", request.path("githubEnvironment").path("name").asText());
                    assertEquals("compute", request.path("githubEnvironment").path("computeId").asText());
                }
                assertEquals(local ? "ws://127.0.0.1:12345" : null, host.getUrl());
                assertEquals(local ? "test-token" : null, host.getToken());
                assertEquals(github ? "environment-123" : null, host.getEnvironmentId());
                assertNull(host.getPid());
            }
        }
    }

    @Test
    void snapshotsOptionsBeforeConnecting() throws Exception {
        try (var server = new FakeRuntime(); var client = server.client()) {
            server.finishConnect = new CompletableFuture<>();
            var options = localOptions().setGithubEnvironment(new HostGitHubEnvironmentOptions("original", "compute"));
            var pending = client.startAhpHost(options);
            await(server.connectEntered);
            try {
                options.setLocalServer(null).setGithubEnvironment(new HostGitHubEnvironmentOptions("", ""));
            } finally {
                server.finishConnect.complete(null);
            }
            try (var host = await(pending)) {
                var request = await(server.startRequest);
                assertTrue(request.hasNonNull("localServer"));
                assertEquals("original", request.path("githubEnvironment").path("name").asText());
                assertEquals("compute", request.path("githubEnvironment").path("computeId").asText());
            }
        }
    }

    @Test
    void publicHostApiExposesOnlyOwnerOperations() {
        var methods = Arrays.stream(ServerHostApi.class.getMethods())
                .filter(method -> method.getDeclaringClass() == ServerHostApi.class).map(method -> method.getName())
                .collect(Collectors.toSet());
        assertEquals(Set.of("start", "dispose", "publishSession"), methods);
    }

    @Test
    void generatedHandoffMapsRoundTripScalarAndStructuredSettings() throws Exception {
        var mapper = new ObjectMapper();
        Map<String, Object> config = new HashMap<>();
        config.put("sessionId", "requested");
        config.put("workingDirectory", "/workspace");
        config.put("streaming", true);
        config.put("threshold", 42);
        config.put("additionalDirectories", List.of("/other"));
        config.put("featureFlags", Map.of("flag", true));
        config.put("optional", null);
        var request = new HostCreateSessionParams("handoff", false, config);
        assertEquals(config,
                mapper.readValue(mapper.writeValueAsBytes(request), HostCreateSessionParams.class).config());
        var callback = mapper.readValue(
                mapper.writeValueAsBytes(
                        Map.of("hostId", "host", "handoffId", "handoff", "resume", false, "config", config)),
                HostSessionCreateCallback.class);
        assertEquals(config, callback.config());
    }

    @Test
    void cancelledStartupDisposesAfterStartCompletes() throws Exception {
        try (var server = new FakeRuntime(); var client = server.client()) {
            server.finishStart = new CompletableFuture<>();
            var pending = client.startAhpHost(localOptions());
            String hostId = await(server.startEntered);
            assertTrue(pending.cancel(false));
            assertEquals(0, server.disposals.get());
            server.finishStart.complete(null);
            assertEquals(hostId, await(server.disposedHost));
            assertTrue(pending.isCancelled());
            assertEquals(1, server.disposals.get());
        }
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void preservesFactoryConfigurationAndReleasesExactObject(boolean resume, boolean enabled) throws Exception {
        try (var server = new FakeRuntime(); var client = server.client()) {
            var created = new CompletableFuture<CopilotSession>();
            var released = new CompletableFuture<CopilotSession>();
            var options = localOptions().setCreateSession(request -> client
                    .createSession(request.config().setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .thenApply(session -> {
                        created.complete(session);
                        return session;
                    }))
                    .setResumeSession(request -> client
                            .resumeSession(request.sessionId(),
                                    request.config().setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                            .thenApply(session -> {
                                created.complete(session);
                                return session;
                            }))
                    .setOnSessionReleased(session -> {
                        released.complete(session);
                        return CompletableFuture.completedFuture(null);
                    });
            var host = await(client.startAhpHost(options));
            Map<String, Object> config = new HashMap<>(Map.of("sessionId", "requested", "workingDirectory",
                    "/workspace/project", "additionalDirectories", List.of(), "configDir", "/workspace/config",
                    "mcpOAuthTokenStorage", "memory", "gitHubToken", "unit-test-token", "featureFlags",
                    Map.of("custom_flag_name", true), "enableExperimentalMode", true, "enableMcpApps", enabled));
            config.put("streaming", enabled);
            if (resume) {
                config.put("suppressResumeEvent", enabled);
                config.put("continuePendingWork", false);
            }
            assertEquals("requested",
                    await(server.materialize(host, "handoff", resume, config)).get("sessionId").asText());
            server.release(host, "handoff");
            server.release(host, "handoff");
            assertSame(await(created), await(released));
            assertTrue(server.sessionRequests.get(0).get("additionalDirectories").isEmpty());
            await(host.dispose());
        }
    }

    @ParameterizedTest
    @CsvSource({"streaming,false", "streaming,true", "enableMcpApps,false", "enableMcpApps,true",
            "suppressResumeEvent,false", "suppressResumeEvent,true"})
    void rejectsChangedBooleanSettings(String setting, boolean expected) throws Exception {
        try (var server = new FakeRuntime(); var client = server.client()) {
            var host = await(client.startAhpHost(localOptions().setResumeSession(request -> {
                switch (setting) {
                    case "streaming" -> request.config().setStreaming(!expected);
                    case "enableMcpApps" -> request.config().setEnableMcpApps(!expected);
                    case "suppressResumeEvent" -> request.config().setDisableResume(!expected);
                    default -> throw new AssertionError(setting);
                }
                return client.resumeSession(request.sessionId(),
                        request.config().setOnPermissionRequest(PermissionHandler.APPROVE_ALL));
            })));
            var error = assertThrows(Exception.class, () -> await(
                    server.materialize(host, "changed", true, Map.of("sessionId", "changed", setting, expected))));
            assertTrue(error.getMessage().contains("config."));
            await(host.dispose());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void comparesNumericSettingsByValue(boolean resume) throws Exception {
        for (Number credits : List.<Number>of(10, 3000000000L, 10.5)) {
            for (boolean changed : List.of(false, true)) {
                try (var server = new FakeRuntime(); var client = server.client()) {
                    var host = await(client.startAhpHost(localOptions().setCreateSession(request -> {
                        if (changed) {
                            request.config().setSessionLimits(new SessionLimitsConfig(credits.doubleValue() + 1));
                        }
                        return client
                                .createSession(request.config().setOnPermissionRequest(PermissionHandler.APPROVE_ALL));
                    }).setResumeSession(request -> {
                        if (changed) {
                            request.config().setSessionLimits(new SessionLimitsConfig(credits.doubleValue() + 1));
                        }
                        return client.resumeSession(request.sessionId(),
                                request.config().setOnPermissionRequest(PermissionHandler.APPROVE_ALL));
                    })));
                    var pending = server.materialize(host, "numeric", resume,
                            Map.of("sessionId", "numeric", "sessionLimits", Map.of("maxAiCredits", credits)));
                    if (changed) {
                        var error = assertThrows(Exception.class, () -> await(pending));
                        assertTrue(error.getMessage().contains("config.sessionLimits"));
                    } else {
                        assertEquals("numeric", await(pending).get("sessionId").asText());
                    }
                    await(host.dispose());
                }
            }
        }
    }

    @Test
    void cancellationUnblocksRpcAndReleasesLateResultAfterOwnerStops() throws Exception {
        try (var server = new FakeRuntime(); var client = server.client()) {
            var late = new CompletableFuture<CopilotSession>();
            var entered = new CompletableFuture<CompletionStage<Void>>();
            var released = new CompletableFuture<CopilotSession>();
            var original = await(client.createSession(new SessionConfig().setSessionId("requested")
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)));
            var host = await(client.startAhpHost(localOptions().setCreateSession(request -> {
                entered.complete(request.cancellation());
                return late;
            }).setOnSessionReleased(session -> {
                released.complete(session);
                return CompletableFuture.completedFuture(null);
            })));
            var pending = server.materialize(host, "pending", false, Map.of("sessionId", "requested"));
            var cancellation = await(entered);
            assertThrows(Exception.class,
                    () -> await(server.materialize(host, "pending", false, Map.of("sessionId", "requested"))));
            assertFalse(pending.isDone());
            server.release(host, "pending");
            assertThrows(Exception.class, () -> await(pending));
            await(cancellation.toCompletableFuture());
            await(client.stop());
            late.complete(original);
            assertSame(original, await(released));
        }
    }

    @Test
    void retainedResumeDoesNotReconfigureOriginal() throws Exception {
        try (var server = new FakeRuntime(); var client = server.client()) {
            var original = await(client.createSession(new SessionConfig().setSessionId("retained")
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)));
            var host = await(client.startAhpHost(
                    localOptions().setResumeSession(request -> CompletableFuture.completedFuture(original))));
            assertEquals("retained",
                    await(server.materialize(host, "resume", true,
                            Map.of("sessionId", "retained", "workingDirectory", "/new/default"))).get("sessionId")
                            .asText());
            assertEquals(1, server.sessionRequests.size());
            await(host.dispose());
        }
    }

    @Test
    void rejectsForeignObjectsAndChangedHostSettings() throws Exception {
        try (var server = new FakeRuntime();
                var foreignServer = new FakeRuntime();
                var client = server.client();
                var foreign = foreignServer.client()) {
            var wrongOwner = await(foreign.createSession(new SessionConfig().setSessionId("requested")
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)));
            var released = new CompletableFuture<CopilotSession>();
            var host = await(client.startAhpHost(
                    localOptions().setCreateSession(request -> CompletableFuture.completedFuture(wrongOwner))
                            .setOnSessionReleased(session -> {
                                released.complete(session);
                                return CompletableFuture.completedFuture(null);
                            })));
            var error = assertThrows(Exception.class,
                    () -> await(server.materialize(host, "foreign", false, Map.of("sessionId", "requested"))));
            assertTrue(error.getMessage().contains("owning client"));
            assertSame(wrongOwner, await(released));
            await(host.dispose());
            var changed = await(client.startAhpHost(localOptions().setCreateSession(request -> client.createSession(
                    request.config().setModel("changed").setOnPermissionRequest(PermissionHandler.APPROVE_ALL)))));
            error = assertThrows(Exception.class, () -> await(server.materialize(changed, "changed", false,
                    Map.of("sessionId", "requested", "model", "original"))));
            assertTrue(error.getMessage().contains("config.model"));
            await(changed.dispose());
        }
    }

    @Test
    void publishesDisposesRepeatedlyAndRetainsOriginalTransport() throws Exception {
        try (var server = new FakeRuntime(); var client = server.client()) {
            var exits = new AtomicInteger();
            var exited = new CompletableFuture<Void>();
            var host = await(client.startAhpHost(localOptions().setOnExit(info -> {
                exits.incrementAndGet();
                exited.complete(null);
                return CompletableFuture.completedFuture(null);
            })));
            assertNull(host.getPid());
            assertEquals("ahp-session:/resident", await(host.publishSession("resident")).sessionUri());
            await(CompletableFuture.allOf(host.dispose(), host.dispose()));
            await(exited);
            assertEquals(2, server.disposals.get());
            assertEquals(1, exits.get());
            await(client.stop());
            assertThrows(Exception.class, () -> await(host.dispose()));
            assertEquals(2, server.disposals.get());
        }
    }

    private static final class FakeRuntime implements AutoCloseable {
        final ServerSocket listener = new ServerSocket(0);
        final CompletableFuture<JsonRpcClient> connected = new CompletableFuture<>();
        final CopyOnWriteArrayList<JsonNode> sessionRequests = new CopyOnWriteArrayList<>();
        final AtomicInteger disposals = new AtomicInteger();
        final CompletableFuture<String> startEntered = new CompletableFuture<>();
        final CompletableFuture<JsonNode> startRequest = new CompletableFuture<>();
        final CompletableFuture<String> disposedHost = new CompletableFuture<>();
        final CompletableFuture<Void> connectEntered = new CompletableFuture<>();
        volatile CompletableFuture<Void> finishConnect = CompletableFuture.completedFuture(null);
        volatile CompletableFuture<Void> finishStart = CompletableFuture.completedFuture(null);
        final Thread accept;

        FakeRuntime() throws IOException {
            accept = new Thread(() -> {
                try {
                    var rpc = JsonRpcClient.fromSocket(listener.accept(), transport -> {
                        transport.registerMethodHandler("connect", (id, params) -> {
                            connectEntered.complete(null);
                            finishConnect.thenRun(() -> respond(transport, id,
                                    Map.of("ok", true, "protocolVersion", 3, "version", "test")));
                        });
                        for (String method : List.of("session.create", "session.resume")) {
                            transport.registerMethodHandler(method, (id, params) -> {
                                sessionRequests.add(params);
                                respond(transport, id, Map.of("sessionId",
                                        params.path("sessionId").asText(UUID.randomUUID().toString())));
                            });
                        }
                        for (String method : List.of("session.disconnect", "session.options.update")) {
                            transport.registerMethodHandler(method, (id, params) -> respond(transport, id, Map.of()));
                        }
                        transport.registerMethodHandler("host.start", (id, params) -> {
                            startEntered.complete(params.get("hostId").asText());
                            startRequest.complete(params);
                            var result = new HashMap<String, Object>();
                            result.put("hostId", params.get("hostId").asText());
                            if (params.hasNonNull("localServer")) {
                                result.put("url", "ws://127.0.0.1:12345");
                                result.put("token", "test-token");
                            }
                            if (params.hasNonNull("githubEnvironment")) {
                                result.put("environmentId", "environment-123");
                            }
                            finishStart.thenRun(() -> respond(transport, id, result));
                        });
                        transport.registerMethodHandler("host.publishSession",
                                (id, params) -> respond(transport, id,
                                        Map.of("sessionId", params.get("sessionId").asText(), "sessionUri",
                                                "ahp-session:/" + params.get("sessionId").asText())));
                        transport.registerMethodHandler("host.dispose", (id, params) -> {
                            disposals.incrementAndGet();
                            try {
                                transport.notify("host.exited",
                                        Map.of("hostId", params.get("hostId").asText(), "reason", "disposed"));
                            } catch (IOException error) {
                                throw new IllegalStateException(error);
                            }
                            respond(transport, id, Map.of());
                            disposedHost.complete(params.get("hostId").asText());
                        });
                    });
                    connected.complete(rpc);
                } catch (IOException error) {
                    connected.completeExceptionally(error);
                }
            }, "ahp-test-runtime");
            accept.setDaemon(true);
            accept.start();
        }

        CopilotClient client() {
            return new CopilotClient(new CopilotClientOptions().setCliUrl("127.0.0.1:" + listener.getLocalPort()));
        }

        CompletableFuture<JsonNode> materialize(AhpHost host, String handoff, boolean resume,
                Map<String, Object> config) throws Exception {
            return await(connected).invoke("host.materializeSession",
                    Map.of("hostId", host.getHostId(), "handoffId", handoff, "resume", resume, "config", config),
                    JsonNode.class);
        }

        void release(AhpHost host, String handoff) throws Exception {
            await(connected).notify("host.sessionReleased", Map.of("hostId", host.getHostId(), "handoffId", handoff));
        }

        static void respond(JsonRpcClient rpc, String id, Object value) {
            try {
                rpc.sendResponse(Long.valueOf(id), value);
            } catch (IOException error) {
                throw new IllegalStateException(error);
            }
        }

        @Override
        public void close() throws Exception {
            if (connected.getNow(null) != null) {
                connected.join().close();
            }
            listener.close();
            accept.join(5000);
        }
    }
}
