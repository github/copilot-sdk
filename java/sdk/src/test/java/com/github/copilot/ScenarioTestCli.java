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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class ScenarioTestCli implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Function<JsonNode, JsonNode> handler;
    private final List<JsonNode> requests = new CopyOnWriteArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicReference<Throwable> responderFailure = new AtomicReference<>();
    private final BytePipe toClient = new BytePipe();
    private final BytePipe toRuntime = new BytePipe();
    private final InputStream runtimeInput = toRuntime.inputStream();
    private final OutputStream runtimeOutput = toClient.outputStream();
    private final Thread responder;

    ScenarioTestCli(Function<JsonNode, JsonNode> handler) throws IOException {
        this.handler = handler;
        this.responder = new Thread(this::respondToRequests, "scenario-fake-runtime");
        this.responder.setDaemon(true);
        this.responder.start();
    }

    CopilotClient.InProcessTransport open() {
        return new CopilotClient.InProcessTransport(toClient.inputStream(), toRuntime.outputStream(), this::close);
    }

    JsonNode request(String method) {
        return requests(method).stream().findFirst().orElseGet(() -> {
            fail("Expected request for " + method + "; captured methods were "
                    + requests.stream().map(request -> request.path("method").asText()).toList());
            return NullNode.getInstance();
        });
    }

    List<JsonNode> requests(String method) {
        return requests.stream().filter(request -> method.equals(request.path("method").asText()))
                .map(request -> (JsonNode) request.deepCopy()).toList();
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
                } catch (Throwable failure) {
                    responderFailure.compareAndSet(null, failure);
                    ObjectNode errorNode = response.putObject("error");
                    errorNode.put("code", -32603);
                    errorNode.put("message", "Scenario fake runtime handler failed: " + failure.getMessage());
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
            throw new AssertionError("Scenario fake runtime failed", failure);
        }
    }

    private static JsonNode readMessage(InputStream in) throws IOException {
        int contentLength = -1;
        var line = new ByteArrayOutputStream();
        while (true) {
            int value = in.read();
            if (value == -1) {
                return null;
            }
            if (value == '\n') {
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
            } else if (value != '\r') {
                line.write(value);
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
