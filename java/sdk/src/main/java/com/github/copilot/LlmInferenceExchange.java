/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

import com.github.copilot.generated.rpc.LlmInferenceHttpResponseChunkError;
import com.github.copilot.generated.rpc.LlmInferenceHttpResponseChunkParams;
import com.github.copilot.generated.rpc.LlmInferenceHttpResponseChunkResult;
import com.github.copilot.generated.rpc.LlmInferenceHttpResponseStartResult;
import com.github.copilot.generated.rpc.LlmInferenceHttpResponseStartParams;
import com.github.copilot.generated.rpc.RpcCaller;
import com.github.copilot.generated.rpc.ServerLlmInferenceApi;

import org.jspecify.annotations.Nullable;

/**
 * One intercepted request in flight. Carries the request context plus the body
 * byte stream the runtime feeds in via {@code httpRequestChunk} frames, and
 * emits the consumer's response straight back to the runtime through the
 * generated {@code llmInference} server API.
 * <p>
 * This is the single object the {@link LlmInferenceAdapter} owns and the
 * {@link CopilotRequestHandler} writes to, replacing the former
 * provider/sink/request-body/response-channel indirection. The response state
 * machine is strict: {@link #startResponse} once, then zero or more
 * {@code writeResponse*} calls, finishing with exactly one of
 * {@link #endResponse} or {@link #errorResponse}.
 */
final class LlmInferenceExchange {

    /**
     * A single request body frame.
     *
     * @param data
     *            the frame bytes
     * @param binary
     *            {@code true} when delivered as binary, {@code false} for UTF-8
     *            text
     * @param agentInvocationId
     *            the runtime-provided invocation identity, or {@code null}
     */
    record BodyFrame(byte[] data, boolean binary, @Nullable String agentInvocationId) {
    }

    private enum ItemKind {
        CHUNK, END, CANCEL
    }

    private record BodyItem(ItemKind kind, byte[] data, boolean binary, @Nullable String agentInvocationId) {
    }

    private final String requestId;
    private String method;
    private final Supplier<ServerLlmInferenceApi> rpcSupplier;
    private final RpcCaller rpcCaller;

    private final BlockingQueue<BodyItem> body = new LinkedBlockingQueue<>();
    private final CompletableFuture<Void> cancellation = new CompletableFuture<>();

    private final Object lock = new Object();
    private boolean started;
    private boolean finished;
    private boolean cancelled;

    private CopilotRequestContext context;

    LlmInferenceExchange(String requestId, Supplier<ServerLlmInferenceApi> rpcSupplier, RpcCaller rpcCaller) {
        this.requestId = requestId;
        this.rpcSupplier = rpcSupplier;
        this.rpcCaller = rpcCaller;
    }

    String requestId() {
        return requestId;
    }

    String method() {
        return method;
    }

    void setMethod(String method) {
        this.method = method;
    }

    CompletableFuture<Void> cancellation() {
        return cancellation;
    }

    CopilotRequestContext context() {
        return context;
    }

    void setContext(CopilotRequestContext context) {
        this.context = context;
    }

    boolean started() {
        synchronized (lock) {
            return started;
        }
    }

    boolean finished() {
        synchronized (lock) {
            return finished;
        }
    }

    boolean cancelled() {
        synchronized (lock) {
            return cancelled;
        }
    }

    // --- Request body feed (driven by the adapter as chunk frames arrive) ---

    void pushChunk(byte[] data, boolean binary, @Nullable String agentInvocationId) {
        body.add(new BodyItem(ItemKind.CHUNK, data, binary, agentInvocationId));
    }

    void pushEnd() {
        body.add(new BodyItem(ItemKind.END, null, false, null));
    }

    void pushCancel() {
        synchronized (lock) {
            cancelled = true;
        }
        if (!cancellation.isDone()) {
            cancellation.complete(null);
        }
        body.add(new BodyItem(ItemKind.CANCEL, null, false, null));
    }

    /**
     * Reads the next request body frame, blocking until one is available.
     *
     * @return the next frame, or {@code null} when the body has ended
     * @throws InterruptedException
     *             if interrupted while waiting
     * @throws CancellationException
     *             if the runtime cancelled the request
     */
    BodyFrame readFrame() throws InterruptedException {
        BodyItem item = body.take();
        switch (item.kind()) {
            case CANCEL -> {
                // Re-arm the sentinel so subsequent reads keep failing fast.
                body.add(item);
                throw new CancellationException("Request cancelled by runtime");
            }
            case END -> {
                body.add(item);
                return null;
            }
            default -> {
                return new BodyFrame(item.data(), item.binary(), item.agentInvocationId());
            }
        }
    }

    byte[] drainBody() throws InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BodyFrame frame;
        while ((frame = readFrame()) != null) {
            out.writeBytes(frame.data());
        }
        return out.toByteArray();
    }

    // --- Response emit (driven by the handler) ---

    void startResponse(int status, String statusText, Map<String, List<String>> headers) throws IOException {
        join(startResponseAsync(status, statusText, headers));
    }

    CompletableFuture<Void> startResponseAsync(int status, String statusText, Map<String, List<String>> headers)
            throws IOException {
        synchronized (lock) {
            if (started) {
                throw new IOException("LLM inference response startResponse() called twice");
            }
            if (finished) {
                throw new IOException("LLM inference response already finished");
            }
            started = true;
        }
        if (headers == null) {
            // The generated builder requires headers, but the established wire payload
            // omits null headers. Keep this explicit compatibility path behind the same
            // closed-connection check as the generated API.
            api();
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("requestId", requestId);
            params.put("status", (long) status);
            if (statusText != null) {
                params.put("statusText", statusText);
            }
            return rpcCaller.invoke("llmInference.httpResponseStart", params, LlmInferenceHttpResponseStartResult.class)
                    .thenApply(ignored -> null);
        }
        var params = LlmInferenceHttpResponseStartParams.builder().requestId(requestId).status((long) status)
                .statusText(statusText).headers(headers).build();
        return api().httpResponseStart(params).thenApply(ignored -> null);
    }

    void writeResponseText(String text) throws IOException {
        writeChunk(text, false);
    }

    void writeResponseBinary(byte[] data) throws IOException {
        writeChunk(Base64.getEncoder().encodeToString(data), true);
    }

    void writeResponseBinary(byte[] data, int offset, int length) throws IOException {
        ByteBuffer encoded = Base64.getEncoder().encode(ByteBuffer.wrap(data, offset, length));
        writeChunk(new String(encoded.array(), 0, encoded.limit(), StandardCharsets.ISO_8859_1), true);
    }

    CompletableFuture<Void> writeResponseBinaryAsync(byte[] data) throws IOException {
        return writeChunkAsync(Base64.getEncoder().encodeToString(data), true);
    }

    void endResponse() throws IOException {
        synchronized (lock) {
            if (finished) {
                return;
            }
            finished = true;
        }
        var params = LlmInferenceHttpResponseChunkParams.builder().requestId(requestId).data("").end(Boolean.TRUE)
                .build();
        join(api().httpResponseChunk(params));
    }

    void errorResponse(String message, String code) throws IOException {
        synchronized (lock) {
            if (finished) {
                return;
            }
            finished = true;
        }
        if (message == null) {
            // The generated builder requires a message, but the established wire payload
            // permits a terminal error object with no message.
            api();
            Map<String, Object> error = new LinkedHashMap<>();
            if (code != null) {
                error.put("code", code);
            }
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("requestId", requestId);
            params.put("data", "");
            params.put("end", Boolean.TRUE);
            params.put("error", error);
            join(rpcCaller.invoke("llmInference.httpResponseChunk", params, LlmInferenceHttpResponseChunkResult.class));
            return;
        }
        var error = LlmInferenceHttpResponseChunkError.builder().message(message).code(code).build();
        var params = LlmInferenceHttpResponseChunkParams.builder().requestId(requestId).data("").end(Boolean.TRUE)
                .error(error).build();
        join(api().httpResponseChunk(params));
    }

    private void writeChunk(String data, boolean binary) throws IOException {
        join(writeChunkAsync(data, binary));
    }

    private CompletableFuture<Void> writeChunkAsync(String data, boolean binary) throws IOException {
        synchronized (lock) {
            if (cancelled) {
                throw new IOException("LLM inference request was cancelled by the runtime");
            }
            if (!started) {
                throw new IOException("LLM inference response writeResponse() called before startResponse()");
            }
            if (finished) {
                throw new IOException(
                        "LLM inference response writeResponse() called after endResponse()/errorResponse()");
            }
        }
        var params = LlmInferenceHttpResponseChunkParams.builder().requestId(requestId).data(data)
                .binary(binary ? Boolean.TRUE : null).end(Boolean.FALSE).build();
        return api().httpResponseChunk(params).thenApply(ignored -> null);
    }

    private ServerLlmInferenceApi api() throws IOException {
        ServerLlmInferenceApi api = rpcSupplier.get();
        if (api == null) {
            throw new IOException("LLM inference response used after RPC connection closed");
        }
        return api;
    }

    private static <T> T join(CompletableFuture<T> future) throws IOException {
        try {
            return future.join();
        } catch (CompletionException | CancellationException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new IOException(cause.getMessage(), cause);
        }
    }
}
