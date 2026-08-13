/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.copilot.rpc.CopilotClientOptions;

class BuiltinPluginDirectoriesTest {

    @Test
    void defaultAndEmptyDoNotCallRpc() throws Exception {
        assertDoesNotCallRpc(new CopilotClientOptions());
        assertDoesNotCallRpc(new CopilotClientOptions().setBuiltinPluginDirectories(List.of()));
    }

    @Test
    void configuredDirectoriesCallRpcOnceBeforeStartCompletes() throws Exception {
        var paths = List.of(Path.of("").toAbsolutePath().resolve("plugins/core"),
                Path.of("").toAbsolutePath().resolve("plugins/github"));

        try (var server = new FakeRuntimeServer();
                var client = new CopilotClient(
                        new CopilotClientOptions().setCliUrl(server.url()).setBuiltinPluginDirectories(paths))) {
            client.start().get(15, TimeUnit.SECONDS);

            assertEquals(1, server.builtinSetCount());
            JsonNode params = server.awaitBuiltinParams();
            assertEquals(paths.get(0).toString(), params.path("paths").get(0).asText());
            assertEquals(paths.get(1).toString(), params.path("paths").get(1).asText());
        }
    }

    @Test
    void relativeDirectoryIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CopilotClientOptions().setBuiltinPluginDirectories(List.of(Path.of("plugins/core"))));
    }

    private static void assertDoesNotCallRpc(CopilotClientOptions options) throws Exception {
        try (var server = new FakeRuntimeServer(); var client = new CopilotClient(options.setCliUrl(server.url()))) {
            client.start().get(15, TimeUnit.SECONDS);
            assertEquals(0, server.builtinSetCount());
        }
    }

    private static final class FakeRuntimeServer implements AutoCloseable {

        private final ServerSocket serverSocket;
        private final Thread acceptThread;
        private final CompletableFuture<JsonRpcClient> ready = new CompletableFuture<>();
        private final CompletableFuture<JsonNode> builtinParams = new CompletableFuture<>();
        private final AtomicInteger builtinSetCount = new AtomicInteger();

        FakeRuntimeServer() throws IOException {
            serverSocket = new ServerSocket(0);
            acceptThread = new Thread(this::acceptLoop, "builtin-plugin-runtime");
            acceptThread.setDaemon(true);
            acceptThread.start();
        }

        String url() {
            return "127.0.0.1:" + serverSocket.getLocalPort();
        }

        int builtinSetCount() {
            return builtinSetCount.get();
        }

        JsonNode awaitBuiltinParams() throws Exception {
            return builtinParams.get(15, TimeUnit.SECONDS);
        }

        private void acceptLoop() {
            try {
                Socket socket = serverSocket.accept();
                JsonRpcClient server = JsonRpcClient.fromSocket(socket);
                server.registerMethodHandler("connect", (id, params) -> respond(server, id,
                        Map.of("ok", true, "protocolVersion", 3, "version", "test")));
                server.registerMethodHandler("plugins.builtin.set", (id, params) -> {
                    builtinSetCount.incrementAndGet();
                    builtinParams.complete(params);
                    respond(server, id, Map.of());
                });
                ready.complete(server);
            } catch (IOException e) {
                ready.completeExceptionally(e);
                builtinParams.completeExceptionally(e);
            }
        }

        private static void respond(JsonRpcClient server, String id, Object result) {
            if (id == null) {
                return;
            }
            try {
                server.sendResponse(id, result);
            } catch (IOException e) {
                // Connection teardown can race the response during test cleanup.
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
