/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.generated.rpc.RpcCaller;

final class RpcSurfaceTestCli implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    record RpcError(int code, String message, JsonNode data) {
    }

    static final class RecordingCaller implements RpcCaller {

        record Call(String method, Object params, Object resultType) {
        }

        private final List<Call> calls = new CopyOnWriteArrayList<>();

        @Override
        public <T> CompletableFuture<T> invoke(String method, Object params, Class<T> resultType) {
            calls.add(new Call(method, params, resultType));
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public <T> CompletableFuture<T> invoke(String method, Object params, JavaType resultType) {
            calls.add(new Call(method, params, resultType));
            return CompletableFuture.completedFuture(null);
        }

        List<Call> calls() {
            return List.copyOf(calls);
        }

        void clear() {
            calls.clear();
        }
    }

    private final Function<JsonNode, JsonNode> handler;
    private final List<JsonNode> requests = new CopyOnWriteArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicReference<Throwable> responderFailure = new AtomicReference<>();
    private final BytePipe toClient;
    private final BytePipe toRuntime;
    private final InputStream runtimeInput;
    private final OutputStream runtimeOutput;
    private final Thread responder;

    RpcSurfaceTestCli(Function<JsonNode, JsonNode> handler) throws IOException {
        this.handler = handler;
        this.toClient = new BytePipe();
        this.toRuntime = new BytePipe();
        this.runtimeInput = toRuntime.inputStream();
        this.runtimeOutput = toClient.outputStream();
        this.responder = new Thread(this::respondToRequests, "fake-rpc-runtime");
        this.responder.setDaemon(true);
        this.responder.start();
    }

    CopilotClient.InProcessTransport open() {
        return new CopilotClient.InProcessTransport(toClient.inputStream(), toRuntime.outputStream(), this::close);
    }

    JsonNode request(String method) {
        return requests.stream().filter(request -> method.equals(request.path("method").asText())).findFirst()
                .orElseGet(() -> {
                    fail("Expected request for " + method + "; captured methods were "
                            + requests.stream().map(request -> request.path("method").asText()).toList());
                    return NullNode.getInstance();
                });
    }

    long requestCount(String method) {
        return requests.stream().filter(request -> method.equals(request.path("method").asText())).count();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            assertResponderHealthy();
            return;
        }
        toRuntime.close();
        toClient.close();
        responder.interrupt();
        assertResponderHealthy();
    }

    private void respondToRequests() {
        try {
            while (!closed.get()) {
                JsonNode request = readMessage(runtimeInput);
                if (request == null) {
                    return;
                }
                requests.add(request.deepCopy());
                if (!request.hasNonNull("id")) {
                    continue;
                }

                ObjectNode response = MAPPER.createObjectNode();
                response.put("jsonrpc", "2.0");
                response.set("id", request.get("id"));
                try {
                    JsonNode result = handler.apply(request);
                    response.set("result", result == null ? NullNode.getInstance() : result);
                } catch (RpcErrorException error) {
                    ObjectNode errorNode = response.putObject("error");
                    errorNode.put("code", error.error.code());
                    errorNode.put("message", error.error.message());
                    if (error.error.data() != null) {
                        errorNode.set("data", error.error.data());
                    }
                } catch (Throwable failure) {
                    responderFailure.compareAndSet(null, failure);
                    ObjectNode errorNode = response.putObject("error");
                    errorNode.put("code", -32603);
                    errorNode.put("message", "Fake runtime handler failed: " + failure.getMessage());
                }
                writeMessage(runtimeOutput, response);
            }
        } catch (IOException e) {
            if (!closed.get()) {
                responderFailure.compareAndSet(null, e);
            }
        }
    }

    private void assertResponderHealthy() {
        Throwable failure = responderFailure.get();
        if (failure != null) {
            throw new AssertionError("Fake runtime failed", failure);
        }
    }

    static RuntimeException error(int code, String message, JsonNode data) {
        return new RpcErrorException(new RpcError(code, message, data));
    }

    private static JsonNode readMessage(InputStream in) throws IOException {
        int contentLength = -1;
        var line = new ByteArrayOutputStream();
        while (true) {
            int b = in.read();
            if (b == -1) {
                return null;
            }
            if (b == '\n') {
                String header = line.toString(StandardCharsets.UTF_8).trim();
                line.reset();
                if (header.isEmpty()) {
                    break;
                }
                if (header.toLowerCase(Locale.ROOT).startsWith("content-length:")) {
                    try {
                        contentLength = Integer.parseInt(header.substring(header.indexOf(':') + 1).trim());
                    } catch (NumberFormatException e) {
                        throw new IOException("Invalid Content-Length header: " + header, e);
                    }
                }
            } else if (b != '\r') {
                line.write(b);
            }
        }
        if (contentLength < 0) {
            throw new IOException("Missing Content-Length header");
        }
        byte[] body = in.readNBytes(contentLength);
        return body.length == contentLength ? MAPPER.readTree(body) : null;
    }

    private static void writeMessage(OutputStream out, JsonNode message) throws IOException {
        byte[] body = MAPPER.writeValueAsBytes(message);
        out.write(("Content-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }

    private static final class RpcErrorException extends RuntimeException {

        private static final long serialVersionUID = 1L;
        private final RpcError error;

        RpcErrorException(RpcError error) {
            super(error.message());
            this.error = error;
        }
    }

    private static final class BytePipe {

        private final Pipe pipe;

        BytePipe() throws IOException {
            this.pipe = Pipe.open();
        }

        InputStream inputStream() {
            return Channels.newInputStream(pipe.source());
        }

        OutputStream outputStream() {
            return Channels.newOutputStream(pipe.sink());
        }

        void close() {
            closeQuietly(pipe.sink());
            closeQuietly(pipe.source());
        }

        private static void closeQuietly(Closeable closeable) {
            try {
                closeable.close();
            } catch (IOException e) {
                // Nothing useful to do while tearing down a test pipe.
            }
        }
    }
}
