/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class JsonRpcClientTest {

    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

    // ---- Helpers ----

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

    /** Write a raw JSON-RPC message (with Content-Length header) to a stream. */
    private void writeRpcMessage(OutputStream out, String json) throws IOException {
        byte[] content = json.getBytes(StandardCharsets.UTF_8);
        String header = "Content-Length: " + content.length + "\r\n\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(content);
        out.flush();
    }

    /** Read a single JSON-RPC message (Content-Length framed) from a stream. */
    private String readRpcMessage(InputStream in) throws IOException {
        var headerLine = new StringBuilder();
        int contentLength = -1;
        boolean lastWasCR = false;
        boolean inHeaders = true;

        while (inHeaders) {
            int b = in.read();
            if (b == -1)
                throw new IOException("EOF");
            if (b == '\r') {
                lastWasCR = true;
            } else if (b == '\n') {
                String line = headerLine.toString();
                headerLine.setLength(0);
                lastWasCR = false;
                if (line.isEmpty()) {
                    inHeaders = false;
                } else if (line.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.substring(15).trim());
                }
            } else {
                if (lastWasCR) {
                    headerLine.append('\r');
                    lastWasCR = false;
                }
                headerLine.append((char) b);
            }
        }

        byte[] buffer = new byte[contentLength];
        int read = 0;
        while (read < contentLength) {
            int result = in.read(buffer, read, contentLength - read);
            if (result == -1)
                throw new IOException("EOF");
            read += result;
        }
        return new String(buffer, StandardCharsets.UTF_8);
    }

    // ---- notify() ----

    @Test
    void testNotify() throws Exception {
        try (var pair = createSocketPair()) {
            pair.client.notify("test.method", Map.of("key", "value"));

            String msg = readRpcMessage(pair.serverSide.getInputStream());
            JsonNode node = MAPPER.readTree(msg);
            assertEquals("2.0", node.get("jsonrpc").asText());
            assertEquals("test.method", node.get("method").asText());
            assertNull(node.get("id"), "Notification should not have an id");
            assertEquals("value", node.get("params").get("key").asText());
        }
    }

    // ---- isConnected() ----

    @Test
    void testIsConnectedWithSocket() throws Exception {
        try (var pair = createSocketPair()) {
            assertTrue(pair.client.isConnected());
        }
    }

    @Test
    void testIsConnectedWithSocketClosed() throws Exception {
        var pair = createSocketPair();
        pair.client.close();
        assertFalse(pair.client.isConnected());
        pair.serverSide.close();
        pair.serverSocket.close();
    }

    @Test
    void testIsConnectedWithProcess() throws Exception {
        Process proc = new ProcessBuilder("cat").start();
        try (var client = JsonRpcClient.fromProcess(proc)) {
            assertTrue(client.isConnected());
        }
    }

    @Test
    void testIsConnectedWithProcessDead() throws Exception {
        Process proc = new ProcessBuilder("cat").start();
        var client = JsonRpcClient.fromProcess(proc);
        proc.destroy();
        proc.waitFor(5, TimeUnit.SECONDS);
        assertFalse(client.isConnected());
        client.close();
    }

    // ---- getProcess() ----

    @Test
    void testGetProcessReturnsProcess() throws Exception {
        Process proc = new ProcessBuilder("cat").start();
        try (var client = JsonRpcClient.fromProcess(proc)) {
            assertSame(proc, client.getProcess());
        }
    }

    @Test
    void testGetProcessNullForSocket() throws Exception {
        try (var pair = createSocketPair()) {
            assertNull(pair.client.getProcess());
        }
    }

    // ---- invoke() edge cases ----

    @Test
    void testInvokeWithVoidPrimitive() throws Exception {
        try (var pair = createSocketPair()) {
            CompletableFuture<Void> future = pair.client.invoke("test", Map.of(), void.class);

            String request = readRpcMessage(pair.serverSide.getInputStream());
            long id = MAPPER.readTree(request).get("id").asLong();

            writeRpcMessage(pair.serverSide.getOutputStream(),
                    "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":{\"any\":\"thing\"}}");

            assertNull(future.get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void testInvokeWithSendFailure() throws Exception {
        var serverSocket = new ServerSocket(0);
        var clientSocket = new Socket("localhost", serverSocket.getLocalPort());
        var serverSide = serverSocket.accept();
        var client = JsonRpcClient.fromSocket(clientSocket);

        // Close the client socket so write will fail
        clientSocket.close();
        Thread.sleep(100);

        CompletableFuture<?> future = client.invoke("test", Map.of(), JsonNode.class);

        var ex = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
        assertInstanceOf(IOException.class, ex.getCause());
        client.close();
        serverSide.close();
        serverSocket.close();
    }

    @Test
    void testInvokeWithDeserializationError() throws Exception {
        try (var pair = createSocketPair()) {
            // Integer cannot be deserialized from a JSON object
            CompletableFuture<Integer> future = pair.client.invoke("test", Map.of(), Integer.class);

            String request = readRpcMessage(pair.serverSide.getInputStream());
            long id = MAPPER.readTree(request).get("id").asLong();

            writeRpcMessage(pair.serverSide.getOutputStream(),
                    "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":{\"complex\":\"object\"}}");

            var ex = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
            // CompletableFuture unwraps CompletionException, so cause is the
            // underlying JsonProcessingException
            assertInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class, ex.getCause());
        }
    }

    // ---- handleMessage: response handling ----

    @Test
    void testResponseWithUnknownId() throws Exception {
        try (var pair = createSocketPair()) {
            // Response for an id that has no pending request - silently ignored
            writeRpcMessage(pair.serverSide.getOutputStream(),
                    "{\"jsonrpc\":\"2.0\",\"id\":99999,\"result\":{\"ok\":true}}");

            Thread.sleep(200);
            // No exception, just silently dropped
        }
    }

    @Test
    void testErrorResponseWithoutMessage() throws Exception {
        try (var pair = createSocketPair()) {
            CompletableFuture<?> future = pair.client.invoke("test", Map.of(), JsonNode.class);

            String request = readRpcMessage(pair.serverSide.getInputStream());
            long id = MAPPER.readTree(request).get("id").asLong();

            // Error with code but no message field
            writeRpcMessage(pair.serverSide.getOutputStream(),
                    "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"error\":{\"code\":-32600}}");

            var ex = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
            var rpcEx = assertInstanceOf(JsonRpcException.class, ex.getCause());
            assertEquals("Unknown error", rpcEx.getMessage());
            assertEquals(-32600, rpcEx.getCode());
        }
    }

    @Test
    void testErrorResponseWithoutCode() throws Exception {
        try (var pair = createSocketPair()) {
            CompletableFuture<?> future = pair.client.invoke("test", Map.of(), JsonNode.class);

            String request = readRpcMessage(pair.serverSide.getInputStream());
            long id = MAPPER.readTree(request).get("id").asLong();

            // Error with message but no code field
            writeRpcMessage(pair.serverSide.getOutputStream(),
                    "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"error\":{\"message\":\"bad request\"}}");

            var ex = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
            var rpcEx = assertInstanceOf(JsonRpcException.class, ex.getCause());
            assertEquals("bad request", rpcEx.getMessage());
            assertEquals(-1, rpcEx.getCode());
        }
    }

    // ---- handleMessage: server method calls ----

    @Test
    void testNoHandlerForNotification() throws Exception {
        try (var pair = createSocketPair()) {
            // Notification (no id) for unregistered method -- silently logged
            writeRpcMessage(pair.serverSide.getOutputStream(),
                    "{\"jsonrpc\":\"2.0\",\"method\":\"unknown.method\",\"params\":{}}");

            Thread.sleep(200);
        }
    }

    @Test
    void testNoHandlerForRequestSendsErrorResponse() throws Exception {
        try (var pair = createSocketPair()) {
            // Request (with id) for unregistered method -> -32601 Method not found
            writeRpcMessage(pair.serverSide.getOutputStream(),
                    "{\"jsonrpc\":\"2.0\",\"id\":42,\"method\":\"unknown.method\",\"params\":{}}");

            String response = readRpcMessage(pair.serverSide.getInputStream());
            JsonNode node = MAPPER.readTree(response);
            assertEquals("2.0", node.get("jsonrpc").asText());
            assertTrue(node.has("error"));
            assertEquals(-32601, node.get("error").get("code").asInt());
            assertTrue(node.get("error").get("message").asText().contains("Method not found"));
        }
    }

    @Test
    void testHandlerThrowsExceptionWithId() throws Exception {
        try (var pair = createSocketPair()) {
            pair.client.registerMethodHandler("fail.method", (id, params) -> {
                throw new RuntimeException("handler error");
            });

            // Request with id - handler throws -> -32603 Internal error
            writeRpcMessage(pair.serverSide.getOutputStream(),
                    "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"fail.method\",\"params\":{}}");

            String response = readRpcMessage(pair.serverSide.getInputStream());
            JsonNode node = MAPPER.readTree(response);
            assertTrue(node.has("error"));
            assertEquals(-32603, node.get("error").get("code").asInt());
            assertEquals("handler error", node.get("error").get("message").asText());
        }
    }

    @Test
    void testHandlerThrowsExceptionWithoutId() throws Exception {
        try (var pair = createSocketPair()) {
            pair.client.registerMethodHandler("fail.notify", (id, params) -> {
                throw new RuntimeException("notify error");
            });

            // Notification (no id) - handler throws -> just logged, no error response
            writeRpcMessage(pair.serverSide.getOutputStream(),
                    "{\"jsonrpc\":\"2.0\",\"method\":\"fail.notify\",\"params\":{}}");

            Thread.sleep(200);
            // Should not crash
        }
    }

    @Test
    void testMethodCallWithNullId() throws Exception {
        try (var pair = createSocketPair()) {
            var received = new AtomicReference<String>();
            pair.client.registerMethodHandler("test.null.id", (id, params) -> {
                received.set(id);
            });

            // Explicit null id - should be treated as notification
            writeRpcMessage(pair.serverSide.getOutputStream(),
                    "{\"jsonrpc\":\"2.0\",\"id\":null,\"method\":\"test.null.id\",\"params\":{}}");

            Thread.sleep(200);
            assertNull(received.get());
        }
    }

    // ---- handleMessage: edge cases ----

    @Test
    void testInvalidJson() throws Exception {
        try (var pair = createSocketPair()) {
            writeRpcMessage(pair.serverSide.getOutputStream(), "not valid json {{{");

            Thread.sleep(200);
            // Should not crash, error is logged
        }
    }

    @Test
    void testMessageWithNeitherResponseNorMethod() throws Exception {
        try (var pair = createSocketPair()) {
            // JSON object with id but no result/error/method - silently ignored
            writeRpcMessage(pair.serverSide.getOutputStream(), "{\"jsonrpc\":\"2.0\",\"id\":1}");

            Thread.sleep(200);
        }
    }

    // ---- reader: header parsing edge cases ----

    @Test
    void testReaderWithUnknownHeader() throws Exception {
        try (var pair = createSocketPair()) {
            var received = new CompletableFuture<JsonNode>();
            pair.client.registerMethodHandler("test.header", (id, params) -> {
                received.complete(params);
            });

            // Send a message with an extra header before Content-Length
            var out = pair.serverSide.getOutputStream();
            String json = "{\"jsonrpc\":\"2.0\",\"method\":\"test.header\",\"params\":{\"ok\":true}}";
            byte[] content = json.getBytes(StandardCharsets.UTF_8);
            String msg = "X-Custom-Header: value\r\nContent-Length: " + content.length + "\r\n\r\n";
            out.write(msg.getBytes(StandardCharsets.UTF_8));
            out.write(content);
            out.flush();

            JsonNode params = received.get(5, TimeUnit.SECONDS);
            assertTrue(params.get("ok").asBoolean());
        }
    }

    @Test
    void testReaderWithMissingContentLength() throws Exception {
        try (var pair = createSocketPair()) {
            var received = new CompletableFuture<JsonNode>();
            pair.client.registerMethodHandler("test.after", (id, params) -> {
                received.complete(params);
            });

            var out = pair.serverSide.getOutputStream();

            // First: send a message with no Content-Length header (just blank line) -
            // should skip
            out.write("X-Only-Header: no-length\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();

            // Then: send a proper message that should be received
            String json = "{\"jsonrpc\":\"2.0\",\"method\":\"test.after\",\"params\":{\"ok\":true}}";
            byte[] content = json.getBytes(StandardCharsets.UTF_8);
            String proper = "Content-Length: " + content.length + "\r\n\r\n";
            out.write(proper.getBytes(StandardCharsets.UTF_8));
            out.write(content);
            out.flush();

            JsonNode params = received.get(5, TimeUnit.SECONDS);
            assertTrue(params.get("ok").asBoolean());
        }
    }

    // ---- close() ----

    @Test
    void testCloseWithPendingRequests() throws Exception {
        var pair = createSocketPair();
        CompletableFuture<?> future = pair.client.invoke("test", Map.of(), JsonNode.class);
        // Read the outgoing request to avoid blocking
        readRpcMessage(pair.serverSide.getInputStream());

        // Close without responding - should cancel pending request
        pair.client.close();

        var ex = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
        assertInstanceOf(IOException.class, ex.getCause());

        pair.serverSide.close();
        pair.serverSocket.close();
    }
}
