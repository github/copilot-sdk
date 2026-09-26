/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.github.copilot.generated.rpc.ExtensionLaunchProfile;
import com.github.copilot.generated.rpc.ExtensionLaunchProviderResolveRequest;
import com.github.copilot.generated.rpc.ExtensionLaunchProviderResolveResult;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.ExtensionLaunchProvider;

@AllowCopilotExperimental
class ExtensionLaunchProviderTest {

    @Test
    void configuredProviderRegistersAndHandlesResolveDuringStartup() throws Exception {
        var observed = new CompletableFuture<ExtensionLaunchProviderResolveRequest>();
        ExtensionLaunchProvider provider = request -> {
            observed.complete(request);
            return CompletableFuture.completedFuture(new ExtensionLaunchProviderResolveResult(
                    new ExtensionLaunchProfile("java", List.of("extension-host"), Map.of("EXTENSION_SOURCE", "java"))));
        };

        try (var server = new FakeRuntimeServer();
                var client = new CopilotClient(
                        new CopilotClientOptions().setCliUrl(server.url()).setExtensionLaunchProvider(provider))) {
            client.start().get(15, TimeUnit.SECONDS);

            var request = observed.get(15, TimeUnit.SECONDS);
            assertEquals("project:java-e2e", request.id());
            assertEquals("java-e2e", request.name());
            assertEquals("/extensions/java-e2e.jar", request.modulePath());
            assertEquals("project", request.source().getValue());

            var result = server.resolveResult().get(15, TimeUnit.SECONDS);
            assertEquals("java", result.launch().executable());
            assertEquals(List.of("extension-host"), result.launch().args());
            assertEquals(Map.of("EXTENSION_SOURCE", "java"), result.launch().env());
            assertEquals(1, server.registrationCount());
        }
    }

    @Test
    void clonePreservesProvider() {
        ExtensionLaunchProvider provider = request -> CompletableFuture
                .completedFuture(new ExtensionLaunchProviderResolveResult(null));
        var clone = new CopilotClientOptions().setExtensionLaunchProvider(provider).clone();

        assertSame(provider, clone.getExtensionLaunchProvider());
    }

    private static final class FakeRuntimeServer implements AutoCloseable {

        private final ServerSocket serverSocket;
        private final Thread acceptThread;
        private final CompletableFuture<JsonRpcClient> ready = new CompletableFuture<>();
        private final CompletableFuture<ExtensionLaunchProviderResolveResult> resolveResult = new CompletableFuture<>();
        private final AtomicInteger registrationCount = new AtomicInteger();

        FakeRuntimeServer() throws IOException {
            serverSocket = new ServerSocket(0);
            acceptThread = new Thread(this::acceptLoop, "extension-launch-provider-runtime");
            acceptThread.setDaemon(true);
            acceptThread.start();
        }

        String url() {
            return "127.0.0.1:" + serverSocket.getLocalPort();
        }

        int registrationCount() {
            return registrationCount.get();
        }

        CompletableFuture<ExtensionLaunchProviderResolveResult> resolveResult() {
            return resolveResult;
        }

        private void acceptLoop() {
            try {
                Socket socket = serverSocket.accept();
                JsonRpcClient server = JsonRpcClient.fromSocket(socket, rpc -> {
                    rpc.registerMethodHandler("connect", (id, params) -> respond(rpc, id,
                            Map.of("ok", true, "protocolVersion", 3, "version", "test")));
                    rpc.registerMethodHandler("registerExtensionLaunchProvider", (id, params) -> {
                        registrationCount.incrementAndGet();
                        rpc.invoke("extensionLaunchProvider.resolve",
                                Map.of("id", "project:java-e2e", "name", "java-e2e", "modulePath",
                                        "/extensions/java-e2e.jar", "source", "project"),
                                ExtensionLaunchProviderResolveResult.class).whenComplete((result, error) -> {
                                    if (error != null) {
                                        resolveResult.completeExceptionally(error);
                                        sendError(rpc, id, error);
                                        return;
                                    }
                                    resolveResult.complete(result);
                                    respond(rpc, id, Map.of());
                                });
                    });
                });
                ready.complete(server);
            } catch (IOException e) {
                ready.completeExceptionally(e);
                resolveResult.completeExceptionally(e);
            }
        }

        private static void respond(JsonRpcClient server, String id, Object result) {
            if (id == null) {
                return;
            }
            try {
                server.sendResponse(parseRpcId(id), result);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to send fake runtime response", e);
            }
        }

        private static void sendError(JsonRpcClient server, String id, Throwable error) {
            if (id == null) {
                return;
            }
            try {
                server.sendErrorResponse(parseRpcId(id), -32603, error.getMessage());
            } catch (IOException e) {
                resolveResultFailure(error, e);
            }
        }

        private static Object parseRpcId(String id) {
            try {
                return Long.valueOf(id);
            } catch (NumberFormatException ignored) {
                return id;
            }
        }

        private static void resolveResultFailure(Throwable original, IOException responseFailure) {
            original.addSuppressed(responseFailure);
        }

        @Override
        public void close() throws Exception {
            JsonRpcClient server = ready.getNow(null);
            if (server != null) {
                server.close();
            }
            serverSocket.close();
            acceptThread.join(TimeUnit.SECONDS.toMillis(5));
        }
    }
}
