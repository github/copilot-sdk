/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.McpOauthRequestReason;
import com.github.copilot.generated.rpc.SessionMcpAppsCallToolParams;
import com.github.copilot.generated.rpc.McpServerStatus;
import com.github.copilot.generated.rpc.SessionMcpListToolsParams;
import com.github.copilot.generated.rpc.SessionMcpOauthHandlePendingRequestParams;
import com.github.copilot.rpc.McpAuthInvocation;
import com.github.copilot.rpc.McpAuthRequest;
import com.github.copilot.rpc.McpAuthResult;
import com.github.copilot.rpc.McpAuthToken;
import com.github.copilot.rpc.McpHttpServerConfig;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

public class McpOAuthE2ETest {
    private static final String EXPECTED_TOKEN = "sdk-host-token";
    private static final String REFRESH_TOKEN = EXPECTED_TOKEN + "-refresh";
    private static final String UPSCOPE_TOKEN = EXPECTED_TOKEN + "-upscope";
    private static final String REAUTH_TOKEN = EXPECTED_TOKEN + "-reauth";
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
    void testShouldSatisfyMcpOauthUsingHostProvidedToken() throws Exception {
        try (var oauthServer = OAuthMcpServer.start(ctx.getRepoRoot())) {
            var serverName = "oauth-protected-mcp";
            var observedRequest = new java.util.concurrent.atomic.AtomicReference<com.github.copilot.rpc.McpAuthRequest>();
            var observedInvocation = new java.util.concurrent.atomic.AtomicReference<McpAuthInvocation>();

            try (var client = ctx.createClient();
                    var session = client
                            .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                    .setOnMcpAuthRequest((request, invocation) -> {
                                        observedRequest.set(request);
                                        observedInvocation.set(invocation);
                                        return java.util.concurrent.CompletableFuture.completedFuture(
                                                McpAuthResult.token(new McpAuthToken(EXPECTED_TOKEN, "Bearer", 3600L)));
                                    }).setMcpServers(Map.of(serverName, new McpHttpServerConfig()
                                            .setUrl(oauthServer.url() + "/mcp").setTools(List.of("*")))))
                            .get()) {
                waitForMcpServerStatus(session, serverName, McpServerStatus.CONNECTED, observedRequest);
                assertNotNull(observedInvocation.get(), "MCP auth invocation should be provided");
                assertEquals(session.getSessionId(), observedInvocation.get().getSessionId());
                var tools = session.getRpc().mcp.listTools(new SessionMcpListToolsParams(null, serverName)).get(30,
                        TimeUnit.SECONDS);
                assertTrue(tools.tools().stream().anyMatch(tool -> "whoami".equals(tool.name())));
            }

            var request = observedRequest.get();
            assertNotNull(request, "MCP auth handler should be invoked");
            assertEquals(serverName, request.serverName());
            assertEquals(oauthServer.url() + "/mcp", request.serverUrl());
            assertEquals(McpOauthRequestReason.INITIAL, request.reason());
            assertNotNull(request.wwwAuthenticateParams());
            assertEquals(oauthServer.url() + "/.well-known/oauth-protected-resource",
                    request.wwwAuthenticateParams().resourceMetadataUrl());
            assertEquals("mcp.read", request.wwwAuthenticateParams().scope());
            assertEquals("invalid_token", request.wwwAuthenticateParams().error());
            assertEquals(oauthServer.url() + "/mcp",
                    MAPPER.readTree(request.resourceMetadata()).path("resource").asText());

            var requests = oauthServer.requests();
            assertTrue(requests.stream().anyMatch(record -> record.authorization() == null));
            assertTrue(
                    requests.stream().anyMatch(record -> ("Bearer " + EXPECTED_TOKEN).equals(record.authorization())));
        }
    }

    @Test
    void testShouldRequestReplacementTokensAcrossMcpOauthLifecycle() throws Exception {
        try (var oauthServer = OAuthMcpServer.start(ctx.getRepoRoot())) {
            var serverName = "oauth-lifecycle-mcp";
            var observedReasons = new CopyOnWriteArrayList<McpOauthRequestReason>();
            var refreshCount = new java.util.concurrent.atomic.AtomicInteger();

            try (var client = ctx.createClient();
                    var session = client.createSession(new SessionConfig().setEnableMcpApps(true)
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setOnMcpAuthRequest((request, invocation) -> {
                                assertNotNull(invocation);
                                observedReasons.add(request.reason());
                                var result = switch (request.reason()) {
                                    case REFRESH -> {
                                        assertNotNull(request.wwwAuthenticateParams());
                                        assertNull(request.wwwAuthenticateParams().resourceMetadataUrl());
                                        assertEquals("invalid_token", request.wwwAuthenticateParams().error());
                                        if (refreshCount.incrementAndGet() > 1) {
                                            yield McpAuthResult.cancelled();
                                        }
                                        yield McpAuthResult.token(new McpAuthToken(REFRESH_TOKEN, null, null));
                                    }
                                    case UPSCOPE -> {
                                        assertNotNull(request.wwwAuthenticateParams());
                                        assertEquals(oauthServer.url() + "/.well-known/oauth-protected-resource",
                                                request.wwwAuthenticateParams().resourceMetadataUrl());
                                        assertEquals("mcp.write", request.wwwAuthenticateParams().scope());
                                        assertEquals("insufficient_scope", request.wwwAuthenticateParams().error());
                                        yield McpAuthResult.token(new McpAuthToken(UPSCOPE_TOKEN, null, null));
                                    }
                                    case REAUTH -> McpAuthResult.token(new McpAuthToken(REAUTH_TOKEN, null, null));
                                    default -> McpAuthResult.token(new McpAuthToken(EXPECTED_TOKEN, null, null));
                                };
                                return java.util.concurrent.CompletableFuture.completedFuture(result);
                            }).setMcpServers(Map.of(serverName, new McpHttpServerConfig()
                                    .setUrl(oauthServer.url() + "/mcp").setTools(List.of("*")))))
                            .get()) {
                waitForMcpServerStatus(session, serverName, McpServerStatus.CONNECTED,
                        new java.util.concurrent.atomic.AtomicReference<>());
                callWhoami(session, serverName, "refresh");
                callWhoami(session, serverName, "upscope");
                callWhoami(session, serverName, "reauth");
            }

            assertEquals(List.of(McpOauthRequestReason.INITIAL, McpOauthRequestReason.REFRESH,
                    McpOauthRequestReason.UPSCOPE, McpOauthRequestReason.REFRESH, McpOauthRequestReason.REAUTH),
                    observedReasons);

            var requests = oauthServer.requests();
            assertTrue(
                    requests.stream().anyMatch(record -> ("Bearer " + REFRESH_TOKEN).equals(record.authorization())));
            assertTrue(
                    requests.stream().anyMatch(record -> ("Bearer " + UPSCOPE_TOKEN).equals(record.authorization())));
            assertTrue(requests.stream().anyMatch(record -> ("Bearer " + REAUTH_TOKEN).equals(record.authorization())));
        }
    }

    @Test
    void testShouldCancelPendingMcpOauthRequest() throws Exception {
        try (var oauthServer = OAuthMcpServer.start(ctx.getRepoRoot())) {
            var serverName = "oauth-cancelled-mcp";
            var observedRequest = new java.util.concurrent.atomic.AtomicReference<com.github.copilot.rpc.McpAuthRequest>();

            try (var client = ctx.createClient();
                    var session = client
                            .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                    .setOnMcpAuthRequest((request, invocation) -> {
                                        assertNotNull(invocation);
                                        observedRequest.set(request);
                                        return java.util.concurrent.CompletableFuture
                                                .completedFuture(McpAuthResult.cancelled());
                                    }).setMcpServers(Map.of(serverName, new McpHttpServerConfig()
                                            .setUrl(oauthServer.url() + "/mcp").setTools(List.of("*")))))
                            .get()) {
                waitForMcpServerStatus(session, serverName, McpServerStatus.NEEDS_AUTH, observedRequest);

                // Race: session.create kicks off the MCP connection, but the SDK
                // registers its `mcp.oauth_required` interest only after create
                // returns. If the initial 401 wins, the runtime records
                // `needs-auth` without invoking the host callback. A later auth
                // retry (interest now registered) fires the callback with the same
                // INITIAL reason. Wait for the callback instead of sampling it the
                // instant `needs-auth` appears, which is what made this test flaky.
                var request = waitForAuthRequest(observedRequest);
                assertEquals(serverName, request.serverName());
                assertEquals(McpOauthRequestReason.INITIAL, request.reason());
            }
        }
    }

    @Test
    void testShouldResolvePendingMcpOauthRequestThroughRpc() throws Exception {
        try (var oauthServer = OAuthMcpServer.start(ctx.getRepoRoot())) {
            var serverName = "oauth-direct-rpc-mcp";
            var observedRequest = new AtomicReference<McpAuthRequest>();
            var pendingHandlerResult = new CompletableFuture<McpAuthResult>();

            try (var client = ctx.createClient();
                    var session = client.createSession(new SessionConfig().setEnableMcpApps(true)
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setOnMcpAuthRequest((request, invocation) -> {
                                assertNotNull(invocation);
                                observedRequest.set(request);
                                return pendingHandlerResult;
                            }).setMcpServers(Map.of(serverName, new McpHttpServerConfig()
                                    .setUrl(oauthServer.url() + "/mcp").setTools(List.of("*")))))
                            .get()) {
                var connected = CompletableFuture.runAsync(() -> {
                    try {
                        waitForMcpServerStatus(session, serverName, McpServerStatus.CONNECTED, observedRequest);
                    } catch (Exception ex) {
                        throw new CompletionException(ex);
                    }
                });

                var request = waitForAuthRequest(observedRequest);
                assertEquals(serverName, request.serverName());
                assertEquals(oauthServer.url() + "/mcp", request.serverUrl());
                assertEquals(McpOauthRequestReason.INITIAL, request.reason());
                assertNotNull(request.wwwAuthenticateParams());
                assertEquals(oauthServer.url() + "/.well-known/oauth-protected-resource",
                        request.wwwAuthenticateParams().resourceMetadataUrl());
                assertEquals("mcp.read", request.wwwAuthenticateParams().scope());
                assertEquals("invalid_token", request.wwwAuthenticateParams().error());

                var handled = session.getRpc().mcp.oauth.handlePendingRequest(
                        new SessionMcpOauthHandlePendingRequestParams(null, request.requestId(), Map.of("kind", "token",
                                "accessToken", EXPECTED_TOKEN, "tokenType", "Bearer", "expiresIn", 3600L)))
                        .get(30, TimeUnit.SECONDS);
                assertTrue(handled.success());

                pendingHandlerResult.complete(McpAuthResult.cancelled());
                connected.get(60, TimeUnit.SECONDS);
                var tools = session.getRpc().mcp.listTools(new SessionMcpListToolsParams(null, serverName)).get(30,
                        TimeUnit.SECONDS);
                assertTrue(tools.tools().stream().anyMatch(tool -> "whoami".equals(tool.name())));
            } finally {
                pendingHandlerResult.complete(McpAuthResult.cancelled());
            }

            var requests = oauthServer.requests();
            assertTrue(
                    requests.stream().anyMatch(record -> ("Bearer " + EXPECTED_TOKEN).equals(record.authorization())));
        }
    }

    private static void callWhoami(CopilotSession session, String serverName, String scenario) throws Exception {
        var result = session.getRpc().mcp.apps.callTool(
                new SessionMcpAppsCallToolParams(null, serverName, "whoami", Map.of("scenario", scenario), serverName))
                .get(30, TimeUnit.SECONDS);
        var content = result.path("content");
        assertEquals(1, content.size());
        assertEquals("oauth-test-user", content.get(0).path("text").asText());
    }

    private static void waitForMcpServerStatus(CopilotSession session, String serverName, McpServerStatus status,
            java.util.concurrent.atomic.AtomicReference<com.github.copilot.rpc.McpAuthRequest> observedRequest)
            throws Exception {
        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
        var lastStatus = "<not listed>";
        while (System.nanoTime() < deadline) {
            var result = session.getRpc().mcp.list().get(5, TimeUnit.SECONDS);
            var server = result.servers().stream().filter(candidate -> serverName.equals(candidate.name())).findFirst();
            if (server.isPresent()) {
                lastStatus = String.valueOf(server.get().status());
            }
            if (server.isPresent() && status.equals(server.get().status())) {
                return;
            }
            Thread.sleep(200);
        }
        fail(serverName + " did not reach " + status + "; last status was " + lastStatus + "; auth handler invoked="
                + (observedRequest.get() != null));
    }

    private static McpAuthRequest waitForAuthRequest(AtomicReference<McpAuthRequest> observedRequest) throws Exception {
        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (System.nanoTime() < deadline) {
            var request = observedRequest.get();
            if (request != null) {
                return request;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Timed out waiting for MCP OAuth request");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OAuthMcpRequest(String authorization) {
    }

    private record OAuthMcpServer(Process process, String url) implements AutoCloseable {
        static OAuthMcpServer start(Path repoRoot) throws Exception {
            var script = repoRoot.resolve("test").resolve("harness").resolve("test-mcp-oauth-server.mjs");
            var processBuilder = new ProcessBuilder(resolveExecutable("node"), script.toString());
            processBuilder.environment().put("EXPECTED_TOKEN", EXPECTED_TOKEN);
            var process = processBuilder.start();
            var stderr = new StringBuilder();
            Thread stderrThread = new Thread(() -> {
                try (var reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    reader.lines().forEach(stderr::append);
                } catch (IOException ex) {
                    stderr.append(ex.getMessage());
                }
            });
            stderrThread.setDaemon(true);
            stderrThread.start();
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                while (System.nanoTime() < deadline) {
                    if (reader.ready()) {
                        var line = reader.readLine();
                        if (line != null && line.startsWith("Listening: ")) {
                            return new OAuthMcpServer(process, line.substring("Listening: ".length()));
                        }
                    }
                    Thread.sleep(50);
                }
            }
            process.destroyForcibly();
            throw new AssertionError("Timed out waiting for OAuth MCP server: " + stderr);
        }

        List<OAuthMcpRequest> requests() throws Exception {
            var client = HttpClient.newHttpClient();
            var response = client.send(HttpRequest.newBuilder(URI.create(url + "/__requests"))
                    .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            return MAPPER.readValue(response.body(), new TypeReference<List<OAuthMcpRequest>>() {
            });
        }

        private static String resolveExecutable(String executable) {
            var path = System.getenv("PATH");
            if (path == null || path.isBlank()) {
                throw new IllegalStateException("PATH is not configured; cannot find " + executable);
            }

            var extensions = isWindows()
                    ? System.getenv().getOrDefault("PATHEXT", ".COM;.EXE;.BAT;.CMD").split(";")
                    : new String[]{""};
            for (var directory : path.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
                if (directory.isBlank()) {
                    continue;
                }
                for (var extension : extensions) {
                    var candidate = Path.of(directory).resolve(executable + extension).toAbsolutePath().normalize();
                    if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                        return candidate.toString();
                    }
                }
            }
            throw new IllegalStateException("Could not find " + executable + " on PATH.");
        }

        private static boolean isWindows() {
            return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
        }

        @Override
        public void close() {
            process.destroyForcibly();
        }
    }
}
