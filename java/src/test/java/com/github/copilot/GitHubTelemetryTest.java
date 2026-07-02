/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.copilot.generated.rpc.GitHubTelemetryNotification;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;

/**
 * Exercises the hand-written GitHub telemetry forwarding surface: the
 * {@code gitHubTelemetry.event} notification adapter, the
 * {@code enableGitHubTelemetryForwarding} capability flag on the create/resume
 * requests, and the {@code onGitHubTelemetry} client option.
 */
@AllowCopilotExperimental
class GitHubTelemetryTest {

    private record SocketPair(JsonRpcClient client, Socket serverSide,
            ServerSocket serverSocket) implements AutoCloseable {

        @Override
        public void close() throws Exception {
            client.close();
            serverSide.close();
            serverSocket.close();
        }
    }

    private SocketPair createSocketPair() throws Exception {
        var serverSocket = new ServerSocket(0);
        var clientSocket = new Socket("localhost", serverSocket.getLocalPort());
        var serverSide = serverSocket.accept();
        var client = JsonRpcClient.fromSocket(clientSocket);
        return new SocketPair(client, serverSide, serverSocket);
    }

    private void writeRpcMessage(OutputStream out, String json) throws IOException {
        byte[] content = json.getBytes(StandardCharsets.UTF_8);
        String header = "Content-Length: " + content.length + "\r\n\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(content);
        out.flush();
    }

    @Test
    void adapterDispatchesNotificationToHandlerWithTypedPayload() throws Exception {
        try (var pair = createSocketPair()) {
            var received = new CompletableFuture<GitHubTelemetryNotification>();
            Function<GitHubTelemetryNotification, CompletableFuture<Void>> handler = notification -> {
                received.complete(notification);
                return CompletableFuture.completedFuture(null);
            };
            new GitHubTelemetryAdapter(handler).registerHandlers(pair.client());

            String notification = """
                    {
                      "jsonrpc": "2.0",
                      "method": "gitHubTelemetry.event",
                      "params": {
                        "sessionId": "sess-123",
                        "restricted": true,
                        "event": {
                          "kind": "tool_call_executed",
                          "created_at": "2024-01-01T00:00:00Z",
                          "model_call_id": "call-9",
                          "properties": { "tool": "shell" },
                          "metrics": { "duration_ms": 42.5 },
                          "exp_assignment_context": "ctx",
                          "features": { "flag_a": "on" },
                          "session_id": "sess-123",
                          "copilot_tracking_id": "track-1",
                          "client": {
                            "cli_version": "1.2.3",
                            "os_platform": "win32",
                            "os_version": "10",
                            "os_arch": "x64",
                            "node_version": "20.0.0",
                            "is_staff": false
                          }
                        }
                      }
                    }
                    """;
            writeRpcMessage(pair.serverSide().getOutputStream(), notification);

            GitHubTelemetryNotification result = received.get(5, TimeUnit.SECONDS);
            assertEquals("sess-123", result.sessionId());
            assertTrue(result.restricted());

            var event = result.event();
            assertNotNull(event);
            assertEquals("tool_call_executed", event.kind());
            assertEquals("2024-01-01T00:00:00Z", event.createdAt());
            assertEquals("call-9", event.modelCallId());
            assertEquals("shell", event.properties().get("tool"));
            assertEquals(42.5, event.metrics().get("duration_ms"));
            assertEquals("ctx", event.expAssignmentContext());
            assertEquals("on", event.features().get("flag_a"));
            assertEquals("sess-123", event.sessionId());
            assertEquals("track-1", event.copilotTrackingId());

            var client = event.client();
            assertNotNull(client);
            assertEquals("1.2.3", client.cliVersion());
            assertEquals("win32", client.osPlatform());
            assertEquals("x64", client.osArch());
            assertEquals("20.0.0", client.nodeVersion());
            assertEquals(Boolean.FALSE, client.isStaff());
        }
    }

    @Test
    void clientOptsSessionsIntoForwardingAndReceivesEvents() throws Exception {
        var received = new CompletableFuture<GitHubTelemetryNotification>();
        Function<GitHubTelemetryNotification, CompletableFuture<Void>> handler = notification -> {
            received.complete(notification);
            return CompletableFuture.completedFuture(null);
        };

        try (var server = new FakeRuntimeServer();
                var client = new CopilotClient(
                        new CopilotClientOptions().setCliUrl(server.url()).setOnGitHubTelemetry(handler))) {

            client.start().get(15, TimeUnit.SECONDS);

            // Creating a session must opt it into telemetry forwarding.
            client.createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get(15,
                    TimeUnit.SECONDS);
            JsonNode createParams = server.awaitCreate();
            assertTrue(createParams.path("enableGitHubTelemetryForwarding").asBoolean(),
                    "create request should carry enableGitHubTelemetryForwarding=true");

            // The adapter registered on connect should forward server-pushed events.
            server.sendTelemetry(Map.of("sessionId", "sess-xyz", "restricted", false, "event",
                    Map.of("kind", "session_started", "session_id", "sess-xyz")));
            GitHubTelemetryNotification event = received.get(5, TimeUnit.SECONDS);
            assertEquals("sess-xyz", event.sessionId());
            assertFalse(event.restricted());
            assertEquals("session_started", event.event().kind());

            // Resuming a session must opt it in as well.
            client.resumeSession("resume-1",
                    new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get(15, TimeUnit.SECONDS);
            JsonNode resumeParams = server.awaitResume();
            assertTrue(resumeParams.path("enableGitHubTelemetryForwarding").asBoolean(),
                    "resume request should carry enableGitHubTelemetryForwarding=true");
        }
    }

    @Test
    void clientOmitsForwardingWhenNoHandler() throws Exception {
        try (var server = new FakeRuntimeServer();
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(server.url()))) {

            client.start().get(15, TimeUnit.SECONDS);

            client.createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get(15,
                    TimeUnit.SECONDS);
            JsonNode createParams = server.awaitCreate();
            assertFalse(createParams.has("enableGitHubTelemetryForwarding"),
                    "create request should omit the flag when no handler is registered");

            client.resumeSession("resume-1",
                    new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get(15, TimeUnit.SECONDS);
            JsonNode resumeParams = server.awaitResume();
            assertFalse(resumeParams.has("enableGitHubTelemetryForwarding"),
                    "resume request should omit the flag when no handler is registered");
        }
    }

    @Test
    void optionsRetainAndCloneTelemetryHandler() {
        Function<GitHubTelemetryNotification, CompletableFuture<Void>> handler = n -> CompletableFuture
                .completedFuture(null);
        var options = new CopilotClientOptions().setOnGitHubTelemetry(handler);
        assertSame(handler, options.getOnGitHubTelemetry());

        var copy = options.clone();
        assertSame(handler, copy.getOnGitHubTelemetry());
    }

    /**
     * A minimal in-process JSON-RPC runtime that answers the connect/create/resume
     * handshake so a real {@link CopilotClient} can be driven over a socket, and
     * can push {@code gitHubTelemetry.event} notifications back to the client.
     */
    private static final class FakeRuntimeServer implements AutoCloseable {

        private final ServerSocket serverSocket;
        private final Thread acceptThread;
        private final CompletableFuture<JsonRpcClient> ready = new CompletableFuture<>();
        private final CompletableFuture<JsonNode> createParams = new CompletableFuture<>();
        private final CompletableFuture<JsonNode> resumeParams = new CompletableFuture<>();

        FakeRuntimeServer() throws IOException {
            serverSocket = new ServerSocket(0);
            acceptThread = new Thread(this::acceptLoop, "fake-runtime-accept");
            acceptThread.setDaemon(true);
            acceptThread.start();
        }

        String url() {
            return "127.0.0.1:" + serverSocket.getLocalPort();
        }

        JsonNode awaitCreate() throws Exception {
            return createParams.get(15, TimeUnit.SECONDS);
        }

        JsonNode awaitResume() throws Exception {
            return resumeParams.get(15, TimeUnit.SECONDS);
        }

        void sendTelemetry(Object params) throws Exception {
            ready.get(15, TimeUnit.SECONDS).notify("gitHubTelemetry.event", params);
        }

        private void acceptLoop() {
            try {
                Socket socket = serverSocket.accept();
                JsonRpcClient server = JsonRpcClient.fromSocket(socket);
                server.registerMethodHandler("connect",
                        (id, params) -> respond(server, id, Map.of("protocolVersion", 2)));
                server.registerMethodHandler("session.create", (id, params) -> {
                    createParams.complete(params);
                    respond(server, id, Map.of("sessionId", params.path("sessionId").asText("created"), "workspacePath",
                            "/workspace"));
                });
                server.registerMethodHandler("session.resume", (id, params) -> {
                    resumeParams.complete(params);
                    respond(server, id, Map.of("sessionId", params.path("sessionId").asText("resume-1"),
                            "workspacePath", "/workspace"));
                });
                server.registerMethodHandler("session.destroy", (id, params) -> respond(server, id, Map.of()));
                server.registerMethodHandler("runtime.shutdown", (id, params) -> respond(server, id, Map.of()));
                ready.complete(server);
            } catch (IOException e) {
                ready.completeExceptionally(e);
                createParams.completeExceptionally(e);
                resumeParams.completeExceptionally(e);
            }
        }

        private static void respond(JsonRpcClient server, String id, Object result) {
            if (id == null) {
                return;
            }
            try {
                server.sendResponse(id, result);
            } catch (IOException e) {
                // Connection torn down (e.g. client closing); ignore.
            }
        }

        @Override
        public void close() throws Exception {
            JsonRpcClient server = ready.getNow(null);
            if (server != null) {
                server.close();
            }
            serverSocket.close();
        }
    }
}
