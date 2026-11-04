/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.consumer;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

    @Test
    void readsExceptionSerializedBeforePublicDataApi() throws Exception {
        // Serialized with the pre-data, package-private class and an empty stack trace.
        var serialized = Base64.getDecoder().decode(
                "rO0ABXNyACNjb20uZ2l0aHViLmNvcGlsb3QuSnNvblJwY0V4Y2VwdGlvbuJ4OuGze8omAgABSQAEY29kZXhyABpqYXZhLmxhbmcuUnVudGltZUV4Y2VwdGlvbp5fBkcKNIPlAgAAeHIAE2phdmEubGFuZy5FeGNlcHRpb27Q/R8+GjscxAIAAHhyABNqYXZhLmxhbmcuVGhyb3dhYmxl1cY1Jzl3uMsDAARMAAVjYXVzZXQAFUxqYXZhL2xhbmcvVGhyb3dhYmxlO0wADWRldGFpbE1lc3NhZ2V0ABJMamF2YS9sYW5nL1N0cmluZztbAApzdGFja1RyYWNldAAeW0xqYXZhL2xhbmcvU3RhY2tUcmFjZUVsZW1lbnQ7TAAUc3VwcHJlc3NlZEV4Y2VwdGlvbnN0ABBMamF2YS91dGlsL0xpc3Q7eHBxAH4ACHQADlJlcXVlc3QgZmFpbGVkdXIAHltMamF2YS5sYW5nLlN0YWNrVHJhY2VFbGVtZW50OwJGKjw8/SI5AgAAeHAAAAAAc3IAH2phdmEudXRpbC5Db2xsZWN0aW9ucyRFbXB0eUxpc3R6uBe0PKee3gIAAHhweP//gv8=");
        try (var input = new ObjectInputStream(new ByteArrayInputStream(serialized))) {
            var error = assertInstanceOf(JsonRpcException.class, input.readObject());
            assertEquals(ERROR_CODE, error.getCode());
            assertEquals(ERROR_MESSAGE, error.getMessage());
            assertNull(error.getData());
        }
    }

    @Test
    void serializationPreservesErrorData() throws Exception {
        var data = MAPPER.readTree("{\"detail\":[false,null]}");
        var bytes = new ByteArrayOutputStream();
        try (var output = new ObjectOutputStream(bytes)) {
            output.writeObject(new JsonRpcException(ERROR_CODE, ERROR_MESSAGE, data));
        }
        try (var input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            var error = assertInstanceOf(JsonRpcException.class, input.readObject());
            assertEquals(ERROR_CODE, error.getCode());
            assertEquals(ERROR_MESSAGE, error.getMessage());
            assertEquals(data, error.getData());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-number", "2147483648"})
    void framedPeerReportsMalformedLength(String length) {
        // Fixture diagnostics, not coverage of the SDK's independent frame parser.
        var frame = ("Content-Length: " + length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII);
        var error = assertThrows(IOException.class, () -> FramedServer.readFrame(new ByteArrayInputStream(frame)));
        assertEquals("Invalid Content-Length: " + length, error.getMessage());
        assertInstanceOf(NumberFormatException.class, error.getCause());
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
                    return;
                }
            } catch (RuntimeException | Error ex) {
                finished.completeExceptionally(ex);
                return;
            }
            finished.complete(null);
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
                    var value = line.substring("Content-Length:".length()).trim();
                    try {
                        length = Integer.parseInt(value);
                    } catch (NumberFormatException ex) {
                        throw new IOException("Invalid Content-Length: " + value, ex);
                    }
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
