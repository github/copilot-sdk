/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.copilot.generated.rpc.LlmInferenceHttpResponseChunkError;
import com.github.copilot.generated.rpc.LlmInferenceHttpResponseChunkParams;
import com.github.copilot.generated.rpc.LlmInferenceHttpResponseChunkResult;
import com.github.copilot.generated.rpc.LlmInferenceHttpResponseStartParams;
import com.github.copilot.generated.rpc.LlmInferenceHttpResponseStartResult;
import com.github.copilot.generated.rpc.ServerLlmInferenceApi;

/**
 * Bridges the {@code llmInference.*} reverse-RPC protocol onto an
 * {@link LlmInferenceProvider}. Inbound {@code httpRequestStart} /
 * {@code httpRequestChunk} calls are translated into provider invocations and a
 * per-{@code requestId} {@link LlmInferenceResponseSink} that emits outbound
 * {@code httpResponseStart} / {@code httpResponseChunk} frames.
 */
final class LlmInferenceAdapter {

    private static final Logger LOG = Logger.getLogger(LlmInferenceAdapter.class.getName());

    private final LlmInferenceProvider handler;
    private final Supplier<ServerLlmInferenceApi> rpcSupplier;
    private final Executor executor;

    private final Map<String, PendingState> pending = new ConcurrentHashMap<>();
    private final Map<String, List<ChunkFrame>> staged = new ConcurrentHashMap<>();

    LlmInferenceAdapter(LlmInferenceProvider handler, Supplier<ServerLlmInferenceApi> rpcSupplier, Executor executor) {
        this.handler = handler;
        this.rpcSupplier = rpcSupplier;
        this.executor = executor;
    }

    void registerHandlers(JsonRpcClient rpc) {
        rpc.registerMethodHandler("llmInference.httpRequestStart",
                (requestId, params) -> handleRequestStart(rpc, requestId, params));
        rpc.registerMethodHandler("llmInference.httpRequestChunk",
                (requestId, params) -> handleRequestChunk(rpc, requestId, params));
    }

    private void handleRequestStart(JsonRpcClient rpc, String rpcId, JsonNode params) {
        String requestId = params.get("requestId").asText();
        String sessionId = textOrNull(params, "sessionId");
        String method = textOrNull(params, "method");
        String url = textOrNull(params, "url");
        String transport = params.has("transport") && !params.get("transport").isNull()
                ? params.get("transport").asText()
                : LlmInferenceRequest.TRANSPORT_HTTP;
        Map<String, List<String>> headers = parseHeaders(params.get("headers"));

        PendingState state = new PendingState();
        ResponseSink sink = new ResponseSink(requestId, state);

        pending.put(requestId, state);
        List<ChunkFrame> stagedFrames = staged.remove(requestId);
        if (stagedFrames != null) {
            for (ChunkFrame frame : stagedFrames) {
                routeChunk(state, frame);
            }
        }

        LlmInferenceRequest request = new LlmInferenceRequest(requestId, sessionId, method, url, headers, transport,
                state.body, sink, state.cancellation);
        runAsync(() -> runHandler(request, sink, state));

        ack(rpc, rpcId);
    }

    private void handleRequestChunk(JsonRpcClient rpc, String rpcId, JsonNode params) {
        String requestId = params.get("requestId").asText();
        ChunkFrame frame = new ChunkFrame(textOr(params, "data", ""), boolOr(params, "binary"), boolOr(params, "end"),
                boolOr(params, "cancel"));

        PendingState state = pending.get(requestId);
        if (state == null) {
            staged.computeIfAbsent(requestId, k -> new ArrayList<>()).add(frame);
            ack(rpc, rpcId);
            return;
        }
        routeChunk(state, frame);
        ack(rpc, rpcId);
    }

    private void routeChunk(PendingState state, ChunkFrame frame) {
        if (frame.cancel()) {
            synchronized (state.lock) {
                state.cancelled = true;
            }
            if (!state.cancellation.isDone()) {
                state.cancellation.complete(null);
            }
            state.body.close();
            return;
        }
        if (!frame.data().isEmpty()) {
            byte[] bytes = frame.binary()
                    ? Base64.getDecoder().decode(frame.data())
                    : frame.data().getBytes(StandardCharsets.UTF_8);
            state.body.push(bytes, frame.binary());
        }
        if (frame.end()) {
            state.body.close();
        }
    }

    private void runHandler(LlmInferenceRequest request, ResponseSink sink, PendingState state) {
        try {
            handler.onLlmRequest(request);
            boolean finished;
            synchronized (state.lock) {
                finished = state.finished;
            }
            if (!finished) {
                failViaSink(sink, state, "LLM inference provider returned without finalising the response "
                        + "(call ResponseBody.end() or .error())");
            }
        } catch (Exception e) {
            boolean cancelled;
            synchronized (state.lock) {
                cancelled = state.cancelled;
            }
            if (cancelled || state.cancellation.isDone()) {
                finishCancelled(sink, state);
            } else {
                String message = e.getMessage() != null ? e.getMessage() : e.toString();
                failViaSink(sink, state, message);
            }
        }
    }

    private void failViaSink(ResponseSink sink, PendingState state, String message) {
        boolean finished;
        boolean started;
        synchronized (state.lock) {
            finished = state.finished;
            started = state.started;
        }
        if (finished) {
            return;
        }
        try {
            if (!started) {
                sink.start(new LlmInferenceResponseInit(502));
            }
            sink.error(message, null);
        } catch (IOException e) {
            LOG.log(Level.FINE, "Failed to deliver LLM inference failure", e);
        }
    }

    private void finishCancelled(ResponseSink sink, PendingState state) {
        boolean finished;
        boolean started;
        synchronized (state.lock) {
            finished = state.finished;
            started = state.started;
        }
        if (finished) {
            return;
        }
        try {
            if (!started) {
                sink.start(new LlmInferenceResponseInit(499));
            }
            sink.error("Request cancelled by runtime", "cancelled");
        } catch (IOException e) {
            LOG.log(Level.FINE, "Failed to deliver LLM inference cancellation", e);
        }
    }

    private void ack(JsonRpcClient rpc, String rpcId) {
        long id;
        try {
            id = Long.parseLong(rpcId);
        } catch (NumberFormatException e) {
            return;
        }
        try {
            rpc.sendResponse(id, Map.of());
        } catch (IOException e) {
            LOG.log(Level.FINE, "Failed to acknowledge LLM inference frame", e);
        }
    }

    private ServerLlmInferenceApi requireApi() throws IOException {
        ServerLlmInferenceApi api = rpcSupplier.get();
        if (api == null) {
            throw new IOException("LLM inference response sink used after RPC connection closed");
        }
        return api;
    }

    private void runAsync(Runnable task) {
        try {
            if (executor != null) {
                CompletableFuture.runAsync(task, executor);
            } else {
                CompletableFuture.runAsync(task);
            }
        } catch (RejectedExecutionException e) {
            LOG.log(Level.WARNING, "Executor rejected LLM inference task; running inline", e);
            task.run();
        }
    }

    private static String textOrNull(JsonNode params, String field) {
        return params.has(field) && !params.get(field).isNull() ? params.get(field).asText() : null;
    }

    private static String textOr(JsonNode params, String field, String fallback) {
        return params.has(field) && !params.get(field).isNull() ? params.get(field).asText() : fallback;
    }

    private static boolean boolOr(JsonNode params, String field) {
        return params.has(field) && !params.get(field).isNull() && params.get(field).asBoolean();
    }

    private static Map<String, List<String>> parseHeaders(JsonNode node) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (node != null && node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                List<String> values = new ArrayList<>();
                JsonNode value = entry.getValue();
                if (value.isArray()) {
                    value.forEach(item -> values.add(item.asText()));
                } else if (!value.isNull()) {
                    values.add(value.asText());
                }
                result.put(entry.getKey(), values);
            });
        }
        return result;
    }

    private record ChunkFrame(String data, boolean binary, boolean end, boolean cancel) {
    }

    private static final class PendingState {

        private final LlmRequestBody body = new LlmRequestBody();
        private final CompletableFuture<Void> cancellation = new CompletableFuture<>();
        private final Object lock = new Object();
        private boolean started;
        private boolean finished;
        private boolean cancelled;
    }

    private final class ResponseSink implements LlmInferenceResponseSink {

        private final String requestId;
        private final PendingState state;

        ResponseSink(String requestId, PendingState state) {
            this.requestId = requestId;
            this.state = state;
        }

        @Override
        public void start(LlmInferenceResponseInit init) throws IOException {
            synchronized (state.lock) {
                if (state.started) {
                    throw new IOException("LLM inference response sink start() called twice");
                }
                if (state.finished) {
                    throw new IOException("LLM inference response sink already finished");
                }
                state.started = true;
            }
            var params = new LlmInferenceHttpResponseStartParams(requestId, (long) init.getStatus(),
                    init.getStatusText(), init.getHeaders());
            LlmInferenceHttpResponseStartResult result = join(requireApi().httpResponseStart(params));
            if (result != null && Boolean.FALSE.equals(result.accepted())) {
                rejectedByRuntime();
            }
        }

        @Override
        public void write(byte[] data) throws IOException {
            sendChunk(new String(data, StandardCharsets.UTF_8), false);
        }

        @Override
        public void writeBinary(byte[] data) throws IOException {
            sendChunk(Base64.getEncoder().encodeToString(data), true);
        }

        private void sendChunk(String data, boolean binary) throws IOException {
            synchronized (state.lock) {
                if (state.cancelled) {
                    throw new IOException("LLM inference request was cancelled by the runtime");
                }
                if (!state.started) {
                    throw new IOException("LLM inference response sink write() called before start()");
                }
                if (state.finished) {
                    throw new IOException("LLM inference response sink write() called after end()/error()");
                }
            }
            var params = new LlmInferenceHttpResponseChunkParams(requestId, data, binary ? Boolean.TRUE : null,
                    Boolean.FALSE, null);
            LlmInferenceHttpResponseChunkResult result = join(requireApi().httpResponseChunk(params));
            if (result != null && Boolean.FALSE.equals(result.accepted())) {
                rejectedByRuntime();
            }
        }

        @Override
        public void end() throws IOException {
            synchronized (state.lock) {
                if (state.finished) {
                    return;
                }
                state.finished = true;
            }
            removePending();
            var params = new LlmInferenceHttpResponseChunkParams(requestId, "", null, Boolean.TRUE, null);
            join(requireApi().httpResponseChunk(params));
        }

        @Override
        public void error(String message, String code) throws IOException {
            synchronized (state.lock) {
                if (state.finished) {
                    return;
                }
                state.finished = true;
            }
            removePending();
            var error = new LlmInferenceHttpResponseChunkError(message, code);
            var params = new LlmInferenceHttpResponseChunkParams(requestId, "", null, Boolean.TRUE, error);
            join(requireApi().httpResponseChunk(params));
        }

        private void rejectedByRuntime() throws IOException {
            synchronized (state.lock) {
                if (!state.cancelled) {
                    state.cancelled = true;
                }
                state.finished = true;
            }
            if (!state.cancellation.isDone()) {
                state.cancellation.complete(null);
            }
            removePending();
            throw new IOException("LLM inference response was rejected by the runtime (request no longer active)");
        }

        private void removePending() {
            pending.remove(requestId);
        }

        private <T> T join(CompletableFuture<T> future) throws IOException {
            try {
                return future.join();
            } catch (RuntimeException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                throw new IOException(cause.getMessage(), cause);
            }
        }
    }
}
