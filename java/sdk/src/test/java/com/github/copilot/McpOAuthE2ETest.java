/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.McpOauthRequestReason;
import com.github.copilot.generated.SessionEvent;
import com.github.copilot.generated.SessionMcpServerStatusChangedEvent;
import com.github.copilot.generated.rpc.DiagnosticLogLevel;
import com.github.copilot.generated.rpc.DiagnosticSource;
import com.github.copilot.generated.rpc.DiagnosticSourcesConfiguration;
import com.github.copilot.generated.rpc.DiagnosticsConfiguration;
import com.github.copilot.generated.rpc.McpDiagnosticSourceConfiguration;
import com.github.copilot.generated.rpc.SessionDiagnosticsReadParams;
import com.github.copilot.generated.rpc.SessionMcpAppsCallToolParams;
import com.github.copilot.generated.rpc.McpServerStatus;
import com.github.copilot.generated.rpc.SessionMcpListToolsParams;
import com.github.copilot.generated.rpc.SessionMcpListResult;
import com.github.copilot.generated.rpc.SessionMcpOauthHandlePendingRequestParams;
import com.github.copilot.generated.rpc.SessionMcpOauthLoginParams;
import com.github.copilot.rpc.McpAuthInvocation;
import com.github.copilot.rpc.McpAuthHandler;
import com.github.copilot.rpc.McpAuthRequest;
import com.github.copilot.rpc.McpAuthResponse;
import com.github.copilot.rpc.McpAuthResult;
import com.github.copilot.rpc.McpAuthToken;
import com.github.copilot.rpc.McpHttpServerConfig;
import com.github.copilot.rpc.PermissionHandler;

@Tag("e2e")
public class McpOAuthE2ETest {
    private static final String EXPECTED_TOKEN = "sdk-host-token";
    private static final String REFRESH_TOKEN = EXPECTED_TOKEN + "-refresh";
    private static final String UPSCOPE_TOKEN = EXPECTED_TOKEN + "-upscope";
    private static final String REAUTH_TOKEN = EXPECTED_TOKEN + "-reauth";
    private static final String CIMD_URL = "https://github.com/copilot/cli/client-metadata.json";
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
    @Timeout(value = 900, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SAME_THREAD)
    void testUsesCimdUrlInsteadOfDynamicRegistration() throws Exception {
        var statusObserver = new McpStatusObserver("oauth-cimd-mcp");
        try (var oauthServer = OAuthMcpServer.start(ctx.getRepoRoot(), false, true);
                var client = ctx.createClient();
                var session = client.createSession(ctx.newSessionConfig().setOnEvent(statusObserver::onEvent)
                        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setAuthClientIdMetadataUrl(CIMD_URL)
                        .setMcpServers(Map.of("oauth-cimd-mcp",
                                new McpHttpServerConfig().setUrl(oauthServer.url() + "/mcp").setTools(List.of("*")))))
                        .get()) {
            waitForMcpServerStatus(session, "oauth-cimd-mcp", McpServerStatus.NEEDS_AUTH, new AtomicReference<>(),
                    statusObserver);
            var result = E2ETestContext.awaitReply(
                    session.getRpc().mcp.oauth.login(SessionMcpOauthLoginParams.builder()
                            .sessionId(session.getSessionId()).serverName("oauth-cimd-mcp").build()),
                    30, TimeUnit.SECONDS);
            assertNotNull(result.authorizationUrl());
            var clientId = List.of(URI.create(result.authorizationUrl()).getQuery().split("&")).stream()
                    .filter(part -> part.startsWith("client_id=")).findFirst()
                    .map(part -> URLDecoder.decode(part.substring("client_id=".length()), StandardCharsets.UTF_8))
                    .orElseThrow();
            assertEquals(CIMD_URL, clientId);
            assertTrue(oauthServer.requests().stream().noneMatch(request -> "/register".equals(request.path())));
        }
    }

    @Test
    @Timeout(value = 900, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SAME_THREAD)
    void testShouldSatisfyMcpOauthUsingHostProvidedToken() throws Exception {
        try (var oauthServer = OAuthMcpServer.start(ctx.getRepoRoot())) {
            var serverName = "oauth-protected-mcp";
            var authHandler = new InitialMcpAuthHandler();
            McpAuthObservation initialAuth;
            var statusObserver = new McpStatusObserver(serverName);

            try (var client = ctx.createClient();
                    var session = client.createSession(ctx.newSessionConfig()
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setOnEvent(statusObserver::onEvent)
                            .setDiagnostics(DiagnosticsConfiguration.builder()
                                    .sources(DiagnosticSourcesConfiguration.builder()
                                            .mcp(McpDiagnosticSourceConfiguration.builder()
                                                    .level(DiagnosticLogLevel.DEBUG).build())
                                            .build())
                                    .build())
                            .setOnMcpAuthRequest(authHandler).setMcpServers(Map.of(serverName, new McpHttpServerConfig()
                                    .setUrl(oauthServer.url() + "/mcp").setTools(List.of("*")))))
                            .get()) {
                try {
                    waitForMcpServerStatus(session, serverName, McpServerStatus.CONNECTED, authHandler.observedRequest,
                            statusObserver);
                } catch (AssertionError failure) {
                    try {
                        var requests = oauthServer.requests().stream().map(record -> "path=" + record.path()
                                + "; hostToken=" + ("Bearer " + EXPECTED_TOKEN).equals(record.authorization()))
                                .toList();
                        System.err.println("[SDK initial OAuth connection failure] " + requests);
                        var diagnostics = session.getRpc().diagnostics.read(SessionDiagnosticsReadParams.builder()
                                .sources(List.of(DiagnosticSource.MCP)).max(500L).waitMs(0L).build())
                                .get(30, TimeUnit.SECONDS);
                        System.err.println("[SDK initial OAuth MCP connections] "
                                + diagnostics.entries().stream()
                                        .map(entry -> "timestamp=" + entry.timestamp() + "; connection="
                                                + entry.details().connectionId() + "; server="
                                                + entry.details().serverName() + "; kind=" + entry.details().kind()
                                                + "; direction=" + entry.details().direction())
                                        .toList()
                                + "; cursorStatus=" + diagnostics.cursorStatus() + "; hasMore=" + diagnostics.hasMore()
                                + "; droppedCount=" + diagnostics.droppedCount());
                    } catch (Exception | AssertionError diagnosticError) {
                        failure.addSuppressed(diagnosticError);
                    }
                    throw failure;
                }
                initialAuth = authHandler.initialAuth.get();
                assertNotNull(initialAuth, "MCP auth handler should be invoked for INITIAL");
                assertNotNull(initialAuth.invocation(), "MCP auth invocation should be provided");
                assertEquals(session.getSessionId(), initialAuth.invocation().getSessionId());
                assertFalse(initialAuth.runtimeRefreshSupported(), "Initial authentication cannot advertise refresh");
                var tools = E2ETestContext.awaitReply(
                        session.getRpc().mcp
                                .listTools(SessionMcpListToolsParams.builder().serverName(serverName).build()),
                        30, TimeUnit.SECONDS);
                assertTrue(tools.tools().stream().anyMatch(tool -> "whoami".equals(tool.name())));
            }

            var request = initialAuth.request();
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

    record McpAuthObservation(McpAuthRequest request, McpAuthInvocation invocation, boolean runtimeRefreshSupported) {
    }

    static final class InitialMcpAuthHandler implements McpAuthHandler {
        final AtomicReference<McpAuthRequest> observedRequest = new AtomicReference<>();
        final AtomicReference<McpAuthObservation> initialAuth = new AtomicReference<>();

        @Override
        public CompletableFuture<McpAuthResult> handle(McpAuthRequest request, McpAuthInvocation invocation) {
            observedRequest.set(request);
            return CompletableFuture
                    .completedFuture(McpAuthResult.token(new McpAuthToken(EXPECTED_TOKEN, "Bearer", 3600L)));
        }

        @Override
        public CompletableFuture<McpAuthResponse> handleWithRuntimeRefresh(McpAuthRequest request,
                McpAuthInvocation invocation, boolean supported) {
            if (request.reason() == McpOauthRequestReason.INITIAL) {
                initialAuth.compareAndSet(null, new McpAuthObservation(request, invocation, supported));
            }
            return McpAuthHandler.super.handleWithRuntimeRefresh(request, invocation, supported);
        }
    }

    @Test
    @Timeout(value = 900, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SAME_THREAD)
    void testShouldRequestReplacementTokensAcrossMcpOauthLifecycle() throws Exception {
        try (var oauthServer = OAuthMcpServer.start(ctx.getRepoRoot(), true)) {
            var serverName = "oauth-lifecycle-mcp";
            var observedReasons = new CopyOnWriteArrayList<McpOauthRequestReason>();
            var observedRequest = new AtomicReference<McpAuthRequest>();
            var refreshCount = new java.util.concurrent.atomic.AtomicInteger();
            var statusObserver = new McpStatusObserver(serverName);

            try (var client = ctx.createClient();
                    var session = client.createSession(ctx.newSessionConfig().setEnableMcpApps(true)
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setOnMcpAuthRequest((request, invocation) -> {
                                assertNotNull(invocation);
                                observedRequest.set(request);
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
                                        assertEquals("mcp.read mcp.write", request.wwwAuthenticateParams().scope());
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
                try (var statusSubscription = session.on(statusObserver::onEvent)) {
                    oauthServer.releaseInitialChallenge();
                    waitForMcpServerStatus(session, serverName, McpServerStatus.CONNECTED, observedRequest,
                            statusObserver);
                }
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
    @Timeout(value = 900, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SAME_THREAD)
    void testShouldCancelPendingMcpOauthRequest() throws Exception {
        try (var oauthServer = OAuthMcpServer.start(ctx.getRepoRoot())) {
            var serverName = "oauth-cancelled-mcp";
            var observedRequest = new java.util.concurrent.atomic.AtomicReference<com.github.copilot.rpc.McpAuthRequest>();
            var requestSeen = new CompletableFuture<McpAuthRequest>();
            var statusObserver = new McpStatusObserver(serverName);

            try (var client = ctx.createClient();
                    var session = client
                            .createSession(ctx.newSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                    .setOnEvent(statusObserver::onEvent).setOnMcpAuthRequest((request, invocation) -> {
                                        assertNotNull(invocation);
                                        observedRequest.set(request);
                                        requestSeen.complete(request);
                                        return java.util.concurrent.CompletableFuture
                                                .completedFuture(McpAuthResult.cancelled());
                                    }).setMcpServers(Map.of(serverName, new McpHttpServerConfig()
                                            .setUrl(oauthServer.url() + "/mcp").setTools(List.of("*")))))
                            .get()) {
                waitForMcpServerStatus(session, serverName, McpServerStatus.NEEDS_AUTH, observedRequest,
                        statusObserver);

                // Race: session.create kicks off the MCP connection, but the SDK
                // registers its `mcp.oauth_required` interest only after create
                // returns. If the initial 401 wins, the runtime records
                // `needs-auth` without invoking the host callback. A later auth
                // retry (interest now registered) fires the callback with the same
                // INITIAL reason. Wait for the callback instead of sampling it the
                // instant `needs-auth` appears, which is what made this test flaky.
                var request = E2ETestContext.awaitReply(requestSeen, 30, TimeUnit.SECONDS);
                assertEquals(serverName, request.serverName());
                assertEquals(McpOauthRequestReason.INITIAL, request.reason());
            }
        }
    }

    @Test
    @Timeout(value = 900, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SAME_THREAD)
    void testShouldResolvePendingMcpOauthRequestThroughRpc() throws Exception {
        try (var oauthServer = OAuthMcpServer.start(ctx.getRepoRoot())) {
            var serverName = "oauth-direct-rpc-mcp";
            var observedRequest = new AtomicReference<McpAuthRequest>();
            var observedRequestIds = new CopyOnWriteArrayList<String>();
            var requestSeen = new CompletableFuture<McpAuthRequest>();
            var pendingHandlerResult = new CompletableFuture<McpAuthResult>();
            var statusObserver = new McpStatusObserver(serverName);

            try (var client = ctx.createClient();
                    var session = client.createSession(ctx.newSessionConfig().setEnableMcpApps(true)
                            .setOnEvent(statusObserver::onEvent).setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setOnMcpAuthRequest((request, invocation) -> {
                                assertNotNull(invocation);
                                observedRequest.set(request);
                                observedRequestIds.add(request.requestId());
                                requestSeen.complete(request);
                                return pendingHandlerResult;
                            }).setMcpServers(Map.of(serverName, new McpHttpServerConfig()
                                    .setUrl(oauthServer.url() + "/mcp").setTools(List.of("*")))))
                            .get()) {
                CompletableFuture<SessionMcpListResult> current = null;
                var lastReply = new AtomicReference<Object>("<no completed reply>");
                try {
                    current = awaitAuthRequest(() -> session.getRpc().mcp.list(), requestSeen, lastReply::set);
                    var request = requestSeen.get();
                    assertEquals(serverName, request.serverName());
                    assertEquals(oauthServer.url() + "/mcp", request.serverUrl());
                    assertEquals(McpOauthRequestReason.INITIAL, request.reason());
                    assertNotNull(request.wwwAuthenticateParams());
                    assertEquals(oauthServer.url() + "/.well-known/oauth-protected-resource",
                            request.wwwAuthenticateParams().resourceMetadataUrl());
                    assertEquals("mcp.read", request.wwwAuthenticateParams().scope());
                    assertEquals("invalid_token", request.wwwAuthenticateParams().error());

                    var handled = E2ETestContext
                            .awaitReply(
                                    session.getRpc().mcp.oauth
                                            .handlePendingRequest(SessionMcpOauthHandlePendingRequestParams.builder()
                                                    .requestId(request.requestId())
                                                    .result(Map.of("kind", "token", "accessToken", EXPECTED_TOKEN,
                                                            "tokenType", "Bearer", "expiresIn", 3600L))
                                                    .build()),
                                    30, TimeUnit.SECONDS);
                    assertTrue(handled.success());

                    pendingHandlerResult.complete(McpAuthResult.cancelled());
                    waitForMcpServerStatus(session, serverName, McpServerStatus.CONNECTED, observedRequest,
                            statusObserver, current);
                    var tools = E2ETestContext.awaitReply(
                            session.getRpc().mcp
                                    .listTools(SessionMcpListToolsParams.builder().serverName(serverName).build()),
                            30, TimeUnit.SECONDS);
                    assertTrue(tools.tools().stream().anyMatch(tool -> "whoami".equals(tool.name())));
                } catch (Exception | AssertionError failure) {
                    addReplyFailure(failure, current);
                    failure.addSuppressed(new IllegalStateException("MCP OAuth request IDs: " + observedRequestIds
                            + "; last reply=" + lastReply.get() + "; last auth request=" + observedRequest.get()
                            + "; last cached status=" + statusObserver.latestStatus.get()));
                    throw failure;
                } finally {
                    if (current != null) {
                        current.cancel(true);
                    }
                    pendingHandlerResult.complete(McpAuthResult.cancelled());
                }
            } finally {
                pendingHandlerResult.complete(McpAuthResult.cancelled());
            }

            var requests = oauthServer.requests();
            assertTrue(
                    requests.stream().anyMatch(record -> ("Bearer " + EXPECTED_TOKEN).equals(record.authorization())));
        }
    }

    private static void callWhoami(CopilotSession session, String serverName, String scenario) throws Exception {
        var result = E2ETestContext.awaitReply(
                session.getRpc().mcp.apps
                        .callTool(SessionMcpAppsCallToolParams.builder().serverName(serverName).toolName("whoami")
                                .arguments(Map.of("scenario", scenario)).originServerName(serverName).build()),
                30, TimeUnit.SECONDS);
        var content = result.path("content");
        assertEquals(1, content.size());
        assertEquals("oauth-test-user", content.get(0).path("text").asText());
    }

    private static void waitForMcpServerStatus(CopilotSession session, String serverName, McpServerStatus status,
            java.util.concurrent.atomic.AtomicReference<com.github.copilot.rpc.McpAuthRequest> observedRequest,
            McpStatusObserver statusObserver) throws Exception {
        waitForMcpServerStatus(session, serverName, status, observedRequest, statusObserver, null);
    }

    private static void waitForMcpServerStatus(CopilotSession session, String serverName, McpServerStatus status,
            AtomicReference<McpAuthRequest> observedRequest, McpStatusObserver statusObserver,
            CompletableFuture<SessionMcpListResult> firstQuery) throws Exception {
        var lastServer = new java.util.concurrent.atomic.AtomicReference<>("<not listed>");
        try {
            E2ETestContext.awaitReadiness(() -> session.getRpc().mcp.list(), firstQuery, result -> {
                var server = result.servers().stream().filter(candidate -> serverName.equals(candidate.name()))
                        .findFirst();
                lastServer.set(server.map(Object::toString).orElse("<not listed>"));
                return server.isPresent() && status.equals(server.get().status());
            }, 200, serverName + " did not reach " + status);
        } catch (Exception | AssertionError failure) {
            failure.addSuppressed(new IllegalStateException("last server=" + lastServer.get() + "; last auth request="
                    + observedRequest.get() + "; last cached status=" + statusObserver.latestStatus.get()));
            throw failure;
        }
    }

    private static <T> CompletableFuture<T> awaitAuthRequest(java.util.function.Supplier<CompletableFuture<T>> query,
            CompletableFuture<?> requestSeen, java.util.function.Consumer<T> observe) throws Exception {
        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        var current = query.get();
        boolean transferred = false;
        try {
            while (!requestSeen.isDone()) {
                E2ETestContext.awaitReply(CompletableFuture.anyOf(requestSeen, current),
                        Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                if (current.isDone()) {
                    observe.accept(current.get());
                }
                if (requestSeen.isDone()) {
                    break;
                }
                Thread.sleep(200);
                if (!requestSeen.isDone()) {
                    if (System.nanoTime() >= deadline) {
                        E2ETestContext.awaitReply(requestSeen, 0, TimeUnit.NANOSECONDS);
                        break;
                    }
                    current = query.get();
                }
            }
            requestSeen.get();
            if (current.isDone()) {
                observe.accept(current.get());
            }
            transferred = true;
            return current;
        } catch (Exception | AssertionError failure) {
            addReplyFailure(failure, current);
            throw failure;
        } finally {
            if (!transferred) {
                current.cancel(true);
            }
        }
    }

    private static void addReplyFailure(Throwable failure, CompletableFuture<?> reply) {
        if (reply != null && reply.isCompletedExceptionally()) {
            try {
                reply.join();
            } catch (CompletionException | CancellationException replyFailure) {
                if (failure != replyFailure && !(replyFailure instanceof CompletionException
                        && failure.getCause() == replyFailure.getCause())) {
                    failure.addSuppressed(replyFailure);
                }
            }
        }
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES, threadMode = Timeout.ThreadMode.SAME_THREAD)
    void authRequestIsServicedWhileItsReadinessQueryWaitsForAuthentication() throws Exception {
        var current = new CompletableFuture<String>();
        var requestSeen = new CompletableFuture<String>();
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var publisher = new Thread(() -> {
            while (current.getNumberOfDependents() == 0) {
                if (Thread.interrupted()) {
                    return;
                }
                java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
            }
            requestSeen.complete("authenticate");
        });
        publisher.start();
        try {
            var retained = awaitAuthRequest(() -> {
                calls.incrementAndGet();
                return current;
            }, requestSeen, value -> fail("the readiness reply must stay held until authentication"));
            assertEquals("authenticate", requestSeen.get());
            assertSame(current, retained);
            assertEquals(1, calls.get());
            assertFalse(current.isDone(), "auth must be serviced before waiting for CONNECTED");
            current.complete("connected");
            assertEquals("connected", E2ETestContext.awaitReadiness(() -> {
                throw new AssertionError("the retained query must be consumed");
            }, retained, "connected"::equals, 200, "not connected"));
        } finally {
            publisher.interrupt();
            publisher.join();
            current.cancel(true);
            requestSeen.cancel(true);
        }
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES, threadMode = Timeout.ThreadMode.SAME_THREAD)
    void authRequestCanBeDrivenByASecondReadinessQuery() throws Exception {
        var pending = CompletableFuture.completedFuture("needs-auth");
        var current = new CompletableFuture<String>();
        var requestSeen = new CompletableFuture<String>();
        var last = new AtomicReference<String>();
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        try {
            var retained = awaitAuthRequest(() -> {
                return switch (calls.incrementAndGet()) {
                    case 1 -> pending;
                    case 2 -> {
                        assertTrue(pending.isDone(), "queries must not overlap");
                        requestSeen.complete("authenticate");
                        yield current;
                    }
                    default -> throw new AssertionError("must service auth before dispatching more queries");
                };
            }, requestSeen, last::set);
            assertEquals("authenticate", requestSeen.get());
            assertEquals("needs-auth", last.get());
            assertSame(current, retained);
            assertEquals(2, calls.get());
            assertFalse(current.isDone());
        } finally {
            current.cancel(true);
            requestSeen.cancel(true);
        }
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES, threadMode = Timeout.ThreadMode.SAME_THREAD)
    void authReadinessPreservesQueryFailureWithoutRetrying() {
        var fault = new IOException("the status RPC failed");
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var failure = assertThrows(java.util.concurrent.ExecutionException.class, () -> awaitAuthRequest(() -> {
            calls.incrementAndGet();
            return CompletableFuture.failedFuture(fault);
        }, new CompletableFuture<>(), value -> fail("a failed reply cannot be an observation")));
        assertSame(fault, failure.getCause());
        assertEquals(1, calls.get());
        assertEquals(0, failure.getSuppressed().length, "the same query fault need not be reported twice");
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES, threadMode = Timeout.ThreadMode.SAME_THREAD)
    void authReadinessCancelsTheOutstandingQueryWhenItsAuthRequestFails() {
        var current = new CompletableFuture<String>();
        var fault = new IOException("auth request delivery failed");
        var failure = assertThrows(java.util.concurrent.ExecutionException.class, () -> awaitAuthRequest(() -> current,
                CompletableFuture.failedFuture(fault), value -> fail("the readiness query must stay pending")));
        assertSame(fault, failure.getCause());
        assertTrue(current.isCancelled());
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES, threadMode = Timeout.ThreadMode.SAME_THREAD)
    void authReadinessCancelsItsQueryOnInterruptionWithoutBlockingCleanup() {
        var current = new CompletableFuture<String>();
        try {
            assertThrows(InterruptedException.class, () -> awaitAuthRequest(() -> {
                Thread.currentThread().interrupt();
                return current;
            }, new CompletableFuture<>(), value -> fail("the readiness query must stay pending")));
            assertTrue(current.isCancelled());
            assertFalse(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
            current.cancel(true);
        }
    }

    @Test
    void authFailureRetainsAnIndependentExceptionalReadinessReply() {
        var bodyFailure = new AssertionError("auth assertion failed");
        var queryFailure = new IOException("the status RPC failed independently");
        addReplyFailure(bodyFailure, CompletableFuture.failedFuture(queryFailure));
        assertEquals(1, bodyFailure.getSuppressed().length);
        assertSame(queryFailure, bodyFailure.getSuppressed()[0].getCause());
    }

    @Test
    void statusObserverRetainsOnlyTheExpectedRootServersLatestStatus() {
        var serverName = "matching-server";
        var observer = new McpStatusObserver(serverName);
        var event = new SessionMcpServerStatusChangedEvent();
        observer.onEvent(null);
        observer.onEvent(event);
        assertNull(observer.latestStatus.get());

        var connected = SessionMcpServerStatusChangedEvent.SessionMcpServerStatusChangedEventData.builder()
                .serverName(serverName).status(com.github.copilot.generated.McpServerStatus.CONNECTED).build();
        event.setData(connected);
        event.setAgentId("child-agent");
        observer.onEvent(event);
        assertNull(observer.latestStatus.get());

        event.setAgentId(null);
        event.setData(SessionMcpServerStatusChangedEvent.SessionMcpServerStatusChangedEventData.builder()
                .serverName("other-server").status(com.github.copilot.generated.McpServerStatus.CONNECTED).build());
        observer.onEvent(event);
        assertNull(observer.latestStatus.get());

        event.setData(connected);
        observer.onEvent(event);
        assertEquals(com.github.copilot.generated.McpServerStatus.CONNECTED, observer.latestStatus.get());
        event.setData(SessionMcpServerStatusChangedEvent.SessionMcpServerStatusChangedEventData.builder()
                .serverName(serverName).status(com.github.copilot.generated.McpServerStatus.FAILED)
                .error("later failure").build());
        observer.onEvent(event);
        assertEquals(com.github.copilot.generated.McpServerStatus.FAILED, observer.latestStatus.get());

        event.setData(JsonRpcClient.getObjectMapper().convertValue(Map.of("serverName", serverName),
                SessionMcpServerStatusChangedEvent.SessionMcpServerStatusChangedEventData.class));
        observer.onEvent(event);
        assertNull(observer.latestStatus.get());
    }

    private static final class McpStatusObserver {
        private final String serverName;
        private final AtomicReference<com.github.copilot.generated.McpServerStatus> latestStatus = new AtomicReference<>();

        private McpStatusObserver(String serverName) {
            this.serverName = serverName;
        }

        private void onEvent(SessionEvent event) {
            if (event instanceof SessionMcpServerStatusChangedEvent statusEvent && event.getAgentId() == null) {
                var data = statusEvent.getData();
                if (data != null && serverName.equals(data.serverName())) {
                    latestStatus.set(data.status());
                }
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OAuthMcpRequest(String authorization, String path) {
    }

    private record OAuthMcpServer(Process process, String url) implements AutoCloseable {
        static OAuthMcpServer start(Path repoRoot) throws Exception {
            return start(repoRoot, false);
        }

        static OAuthMcpServer start(Path repoRoot, boolean deferInitialChallenge) throws Exception {
            return start(repoRoot, deferInitialChallenge, false);
        }

        static OAuthMcpServer start(Path repoRoot, boolean deferInitialChallenge, boolean cimdSupported)
                throws Exception {
            var script = repoRoot.resolve("test").resolve("harness").resolve("test-mcp-oauth-server.mjs");
            var processBuilder = new ProcessBuilder(resolveExecutable("node"), script.toString());
            processBuilder.environment().put("EXPECTED_TOKEN", EXPECTED_TOKEN);
            if (deferInitialChallenge) {
                processBuilder.environment().put("DEFER_INITIAL_CHALLENGE", "true");
            }
            if (cimdSupported) {
                processBuilder.environment().put("CIMD_SUPPORTED", "true");
            }
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
                var announcedUrl = new java.util.concurrent.atomic.AtomicReference<String>();
                var line = new StringBuilder();
                E2ETestContext.awaitBufferedCondition(() -> {
                    while (reader.ready()) {
                        int next = reader.read();
                        if (next == -1) {
                            throw new IOException("OAuth MCP server exited: " + stderr);
                        }
                        if (next == '\n') {
                            String completeLine = line.toString().stripTrailing();
                            line.setLength(0);
                            if (completeLine.startsWith("Listening: ")) {
                                announcedUrl.set(completeLine.substring("Listening: ".length()));
                                return true;
                            }
                        } else {
                            line.append((char) next);
                        }
                    }
                    return false;
                }, 10_000, 50, "Timed out waiting for OAuth MCP server: " + stderr);
                return new OAuthMcpServer(process, announcedUrl.get());
            } catch (Exception | AssertionError failure) {
                process.destroyForcibly();
                throw failure;
            }
        }

        void releaseInitialChallenge() throws Exception {
            var client = HttpClient.newHttpClient();
            var response = client.send(
                    HttpRequest.newBuilder(URI.create(url + "/__release-initial-challenge"))
                            .timeout(Duration.ofSeconds(10)).POST(HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.discarding());
            assertEquals(204, response.statusCode());
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
