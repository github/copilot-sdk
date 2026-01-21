/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.copilot.sdk.json.JsonRpcError;
import com.github.copilot.sdk.json.JsonRpcRequest;
import com.github.copilot.sdk.json.JsonRpcResponse;

/**
 * JSON-RPC 2.0 client implementation for communicating with the Copilot CLI.
 */
class JsonRpcClient implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(JsonRpcClient.class.getName());
    private static final ObjectMapper MAPPER = createObjectMapper();

    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final Socket socket;
    private final Process process;
    private final AtomicLong requestIdCounter = new AtomicLong(0);
    private final Map<Long, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();
    private final Map<String, BiConsumer<String, JsonNode>> notificationHandlers = new ConcurrentHashMap<>();
    private final ExecutorService readerExecutor;
    private volatile boolean running = true;

    private JsonRpcClient(InputStream inputStream, OutputStream outputStream, Socket socket, Process process) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.socket = socket;
        this.process = process;
        this.readerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "jsonrpc-reader");
            t.setDaemon(true);
            return t;
        });
        startReader();
    }

    static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    public static ObjectMapper getObjectMapper() {
        return MAPPER;
    }

    /**
     * Creates a JSON-RPC client using stdio with a process.
     */
    public static JsonRpcClient fromProcess(Process process) {
        return new JsonRpcClient(process.getInputStream(), process.getOutputStream(), null, process);
    }

    /**
     * Creates a JSON-RPC client using TCP socket.
     */
    public static JsonRpcClient fromSocket(Socket socket) throws IOException {
        return new JsonRpcClient(socket.getInputStream(), socket.getOutputStream(), socket, null);
    }

    /**
     * Registers a handler for JSON-RPC method calls (requests/notifications from
     * server).
     */
    public void registerMethodHandler(String method, BiConsumer<String, JsonNode> handler) {
        notificationHandlers.put(method, handler);
    }

    /**
     * Sends a JSON-RPC request and waits for the response.
     */
    public <T> CompletableFuture<T> invoke(String method, Object params, Class<T> responseType) {
        long id = requestIdCounter.incrementAndGet();
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pendingRequests.put(id, future);

        JsonRpcRequest request = new JsonRpcRequest();
        request.setJsonrpc("2.0");
        request.setId(id);
        request.setMethod(method);
        request.setParams(params);

        try {
            sendMessage(request);
        } catch (IOException e) {
            pendingRequests.remove(id);
            future.completeExceptionally(e);
        }

        return future.thenApply(result -> {
            try {
                if (responseType == Void.class || responseType == void.class) {
                    return null;
                }
                return MAPPER.treeToValue(result, responseType);
            } catch (JsonProcessingException e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Sends a JSON-RPC notification (no response expected).
     */
    public void notify(String method, Object params) throws IOException {
        JsonRpcRequest notification = new JsonRpcRequest();
        notification.setJsonrpc("2.0");
        notification.setMethod(method);
        notification.setParams(params);
        sendMessage(notification);
    }

    /**
     * Sends a JSON-RPC response to a server request.
     */
    public void sendResponse(Object id, Object result) throws IOException {
        JsonRpcResponse response = new JsonRpcResponse();
        response.setJsonrpc("2.0");
        response.setId(id);
        response.setResult(result);
        sendMessage(response);
    }

    /**
     * Sends a JSON-RPC error response to a server request.
     */
    public void sendErrorResponse(Object id, int code, String message) throws IOException {
        JsonRpcResponse response = new JsonRpcResponse();
        response.setJsonrpc("2.0");
        response.setId(id);
        JsonRpcError error = new JsonRpcError();
        error.setCode(code);
        error.setMessage(message);
        response.setError(error);
        sendMessage(response);
    }

    private synchronized void sendMessage(Object message) throws IOException {
        String json = MAPPER.writeValueAsString(message);
        byte[] content = json.getBytes(StandardCharsets.UTF_8);
        String header = "Content-Length: " + content.length + "\r\n\r\n";

        outputStream.write(header.getBytes(StandardCharsets.UTF_8));
        outputStream.write(content);
        outputStream.flush();

        LOG.fine("Sent: " + json);
    }

    private void startReader() {
        readerExecutor.submit(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

                while (running) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }

                    // Parse headers
                    int contentLength = -1;
                    while (!line.isEmpty()) {
                        if (line.toLowerCase().startsWith("content-length:")) {
                            contentLength = Integer.parseInt(line.substring(15).trim());
                        }
                        line = reader.readLine();
                        if (line == null) {
                            return;
                        }
                    }

                    if (contentLength <= 0) {
                        continue;
                    }

                    // Read content
                    char[] buffer = new char[contentLength];
                    int read = 0;
                    while (read < contentLength) {
                        int result = reader.read(buffer, read, contentLength - read);
                        if (result == -1) {
                            return;
                        }
                        read += result;
                    }

                    String content = new String(buffer);
                    LOG.fine("Received: " + content);

                    handleMessage(content);
                }
            } catch (Exception e) {
                if (running) {
                    LOG.log(Level.SEVERE, "Error in JSON-RPC reader", e);
                }
            }
        });
    }

    private void handleMessage(String content) {
        try {
            JsonNode node = MAPPER.readTree(content);

            // Check if this is a response to our request
            if (node.has("id") && !node.get("id").isNull() && (node.has("result") || node.has("error"))) {
                long id = node.get("id").asLong();
                CompletableFuture<JsonNode> future = pendingRequests.remove(id);
                if (future != null) {
                    if (node.has("error")) {
                        JsonNode errorNode = node.get("error");
                        String errorMessage = errorNode.has("message")
                                ? errorNode.get("message").asText()
                                : "Unknown error";
                        int errorCode = errorNode.has("code") ? errorNode.get("code").asInt() : -1;
                        future.completeExceptionally(new JsonRpcException(errorCode, errorMessage));
                    } else {
                        future.complete(node.get("result"));
                    }
                }
            }
            // Check if this is a request from server (has method and id)
            else if (node.has("method")) {
                String method = node.get("method").asText();
                JsonNode params = node.get("params");
                Object id = node.has("id") && !node.get("id").isNull() ? node.get("id") : null;

                BiConsumer<String, JsonNode> handler = notificationHandlers.get(method);
                if (handler != null) {
                    try {
                        // Create a context that includes the request ID for responses
                        handler.accept(id != null ? id.toString() : null, params);
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Error handling method " + method, e);
                        if (id != null) {
                            try {
                                sendErrorResponse(id, -32603, e.getMessage());
                            } catch (IOException ioe) {
                                LOG.log(Level.SEVERE, "Failed to send error response", ioe);
                            }
                        }
                    }
                } else {
                    LOG.fine("No handler for method: " + method);
                    if (id != null) {
                        try {
                            sendErrorResponse(id, -32601, "Method not found: " + method);
                        } catch (IOException ioe) {
                            LOG.log(Level.SEVERE, "Failed to send error response", ioe);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error parsing JSON-RPC message", e);
        }
    }

    @Override
    public void close() {
        running = false;
        readerExecutor.shutdownNow();

        // Cancel all pending requests
        pendingRequests.forEach((id, future) -> future.completeExceptionally(new IOException("Client closed")));
        pendingRequests.clear();

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            LOG.log(Level.FINE, "Error closing socket", e);
        }

        if (process != null) {
            process.destroy();
        }
    }

    public boolean isConnected() {
        if (socket != null) {
            return socket.isConnected() && !socket.isClosed();
        }
        if (process != null) {
            return process.isAlive();
        }
        return false;
    }

    public Process getProcess() {
        return process;
    }
}
