/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
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
import com.github.copilot.generated.rpc.LlmInferenceHttpResponseStartParams;
import com.github.copilot.generated.rpc.ServerLlmInferenceApi;

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
     */
    record BodyFrame(byte[] data, boolean binary) {
    }

    private enum ItemKind {
        CHUNK, END, CANCEL
    }

    private record BodyItem(ItemKind kind, byte[] data, boolean binary) {
    }

    private final String requestId;
    private String method;
    private final Supplier<ServerLlmInferenceApi> rpcSupplier;

    private final BlockingQueue<BodyItem> body = new LinkedBlockingQueue<>();
    private final CompletableFuture<Void> cancellation = new CompletableFuture<>();

    private final Object lock = new Object();
    private boolean started;
    private boolean finished;
    private boolean cancelled;

    private CopilotRequestContext context;

    LlmInferenceExchange(String requestId, Supplier<ServerLlmInferenceApi> rpcSupplier) {
        this.requestId = requestId;
        this.rpcSupplier = rpcSupplier;
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

    void pushChunk(byte[] data, boolean binary) {
        body.add(new BodyItem(ItemKind.CHUNK, data, binary));
    }

    void pushEnd() {
        body.add(new BodyItem(ItemKind.END, null, false));
    }

    void pushCancel() {
        synchronized (lock) {
            cancelled = true;
        }
        if (!cancellation.isDone()) {
            cancellation.complete(null);
        }
        body.add(new BodyItem(ItemKind.CANCEL, null, false));
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
                return new BodyFrame(item.data(), item.binary());
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
        synchronized (lock) {
            if (started) {
                throw new IOException("LLM inference response startResponse() called twice");
            }
            if (finished) {
                throw new IOException("LLM inference response already finished");
            }
            started = true;
        }
        var params = new LlmInferenceHttpResponseStartParams(requestId, (long) status, statusText, headers);
        join(api().httpResponseStart(params));
    }

    void writeResponseText(String text) throws IOException {
        writeChunk(text, false);
    }

    void writeResponseBinary(byte[] data) throws IOException {
        writeChunk(Base64.getEncoder().encodeToString(data), true);
    }

    void endResponse() throws IOException {
        synchronized (lock) {
            if (finished) {
                return;
            }
            finished = true;
        }
        var params = new LlmInferenceHttpResponseChunkParams(requestId, "", null, Boolean.TRUE, null);
        join(api().httpResponseChunk(params));
    }

    void errorResponse(String message, String code) throws IOException {
        synchronized (lock) {
            if (finished) {
                return;
            }
            finished = true;
        }
        var error = new LlmInferenceHttpResponseChunkError(message, code);
        var params = new LlmInferenceHttpResponseChunkParams(requestId, "", null, Boolean.TRUE, error);
        join(api().httpResponseChunk(params));
    }

    private void writeChunk(String data, boolean binary) throws IOException {
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
        var params = new LlmInferenceHttpResponseChunkParams(requestId, data, binary ? Boolean.TRUE : null,
                Boolean.FALSE, null);
        join(api().httpResponseChunk(params));
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
