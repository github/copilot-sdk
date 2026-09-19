/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.consumer;

import static org.junit.jupiter.api.Assertions.*;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.CopilotClient;
import com.github.copilot.JsonRpcException;
import com.github.copilot.rpc.CopilotClientOptions;

@Timeout(15)
class JsonRpcErrorDataTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int ERROR_CODE = -32001;
    private static final String ERROR_MESSAGE = "Request failed";

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"{\"reason\":\"details-not-in-message\",\"nested\":{\"items\":[1,false,null]}}",
            "[{\"nested\":[\"value\"]},2,false,null]", "\"detail — unicode\"", "42", "9007199254740993", "1.25", "0",
            "true", "false", "{}", "[]", "\"\"", "null"})
    void preservesRawDataThroughPublicClient(String dataJson) throws Exception {
        try (var server = new FramedServer(Reply.ERROR, dataJson);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(server.url()))) {
            client.start().get(5, TimeUnit.SECONDS);
            var future = client.ping("error");

            var getFailure = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
            var error = assertInstanceOf(JsonRpcException.class, getFailure.getCause());
            assertEquals(JsonRpcException.class, error.getClass());
            assertInstanceOf(RuntimeException.class, error);
            assertEquals(ERROR_CODE, error.getCode());
            assertEquals(ERROR_MESSAGE, error.getMessage());
            assertEquals("com.github.copilot.JsonRpcException: Request failed", error.toString());
            if (dataJson == null) {
                assertNull(error.getData());
            } else {
                assertEquals(MAPPER.readTree(dataJson), error.getData());
            }

            var joinFailure = assertThrows(CompletionException.class, future::join);
            assertSame(error, joinFailure.getCause());
        }
    }

    @Test
    void preservesSuccessfulResponses() throws Exception {
        try (var server = new FramedServer(Reply.SUCCESS, null);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(server.url()))) {
            client.start().get(5, TimeUnit.SECONDS);
            assertEquals("hello", client.ping("hello").get(5, TimeUnit.SECONDS).message());
        }
    }

    @Test
    void localFailuresRemainDistinctFromRemoteErrors() {
        try (var client = new CopilotClient(new CopilotClientOptions().setAutoStart(false))) {
            RuntimeException failure = assertThrows(IllegalStateException.class, () -> client.ping("not connected"));
            assertEquals("Client not connected. Call start() first.", failure.getMessage());
            assertFalse(failure instanceof JsonRpcException);
        }
    }

    @Test
    void preservesConstructorCompatibility() throws Exception {
        var withoutData = new JsonRpcException(ERROR_CODE, ERROR_MESSAGE);
        assertNull(withoutData.getData());
        assertEquals(ERROR_CODE, withoutData.getCode());
        assertEquals(ERROR_MESSAGE, withoutData.getMessage());
        var data = MAPPER.readTree("{\"detail\":false}");
        var withData = new JsonRpcException(ERROR_CODE, ERROR_MESSAGE, data);
        assertSame(data, withData.getData());
        assertEquals(withoutData.toString(), withData.toString());
    }

    private enum Reply {
        ERROR, SUCCESS
    }

    /**
     * A framed loopback peer that uses no SDK internals or CLI runtime.
     */
    private static final class FramedServer implements AutoCloseable {

        private final ServerSocket listener;
        private final Thread worker;
        private final CompletableFuture<Void> finished = new CompletableFuture<>();
        private volatile Socket socket;
        private volatile boolean closing;

        FramedServer(Reply reply, String dataJson) throws IOException {
            listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
            listener.setSoTimeout(5000);
            worker = new Thread(() -> serve(reply, dataJson), "json-rpc-error-data-peer");
            worker.setDaemon(true);
            worker.start();
        }

        String url() {
            return "127.0.0.1:" + listener.getLocalPort();
        }

        private void serve(Reply reply, String dataJson) {
            try (var accepted = listener.accept()) {
                socket = accepted;
                accepted.setSoTimeout(5000);
                while (!closing) {
                    JsonNode request = readFrame(accepted.getInputStream());
                    if (request == null) {
                        break;
                    }
                    if (!request.has("id")) {
                        continue;
                    }
                    var response = MAPPER.createObjectNode().put("jsonrpc", "2.0");
                    response.set("id", request.get("id"));
                    switch (request.path("method").asText()) {
                        case "connect" -> response.putObject("result").put("protocolVersion", 2);
                        case "ping" -> {
                            if (reply == Reply.SUCCESS) {
                                response.putObject("result")
                                        .put("message", request.path("params").path("message").asText())
                                        .put("protocolVersion", 2);
                            } else {
                                var error = response.putObject("error").put("code", ERROR_CODE).put("message",
                                        ERROR_MESSAGE);
                                if (dataJson != null) {
                                    error.set("data", MAPPER.readTree(dataJson));
                                }
                            }
                        }
                        case "runtime.shutdown" -> response.putObject("result");
                        default -> throw new IOException("Unexpected method: " + request.path("method"));
                    }
                    writeFrame(accepted.getOutputStream(), response);
                }
            } catch (IOException ex) {
                if (!closing) {
                    finished.completeExceptionally(ex);
                }
            } finally {
                finished.complete(null);
            }
        }

        private static JsonNode readFrame(InputStream input) throws IOException {
            var header = new StringBuilder();
            while (!header.toString().endsWith("\r\n\r\n")) {
                int value = input.read();
                if (value < 0) {
                    if (header.isEmpty()) {
                        return null;
                    }
                    throw new EOFException("Incomplete frame header");
                }
                header.append((char) value);
                if (header.length() > 1024) {
                    throw new IOException("Frame header too large");
                }
            }
            int length = -1;
            for (String line : header.toString().split("\r\n")) {
                if (line.startsWith("Content-Length:")) {
                    length = Integer.parseInt(line.substring("Content-Length:".length()).trim());
                }
            }
            if (length < 0 || length > 65536) {
                throw new IOException("Invalid frame length: " + length);
            }
            byte[] body = input.readNBytes(length);
            if (body.length != length) {
                throw new EOFException("Incomplete frame body");
            }
            return MAPPER.readTree(body);
        }

        private static void writeFrame(OutputStream output, JsonNode response) throws IOException {
            byte[] body = MAPPER.writeValueAsBytes(response);
            output.write(("Content-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            output.write(body);
            output.flush();
        }

        @Override
        public void close() throws Exception {
            closing = true;
            listener.close();
            Socket accepted = socket;
            if (accepted != null) {
                accepted.close();
            }
            worker.join(5000);
            assertFalse(worker.isAlive(), "Framed peer must terminate");
            finished.get(5, TimeUnit.SECONDS);
        }
    }
}
