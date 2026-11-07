/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Bridges a client-global reverse RPC request to an SDK host callback that the
 * runtime can cancel.
 * <p>
 * Requests are tracked by request id before they are scheduled, so a
 * {@code $/cancelRequest} or connection close retires queued work before it
 * reaches the callback, cancels pending callback futures, and suppresses late
 * responses. A cancelled request is answered with the standard
 * request-cancelled error; after a connection close no response can be
 * delivered.
 *
 * @param <T>
 *            the generated request type
 * @param <V>
 *            the value the callback supplies
 */
final class CancellableCallbackAdapter<T, V> {

    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();
    private static final int INTERNAL_ERROR = -32603;
    private static final int REQUEST_CANCELLED = -32800;

    private final Logger log = Logger.getLogger(getClass().getName());
    private final String method;
    private final String label;
    private final Class<T> requestType;
    private final Function<T, CompletableFuture<V>> callback;
    private final Function<V, Object> toResult;
    private final Executor executor;
    // Keyed by the JSON rendering of the request id so numeric and string ids stay
    // distinct.
    private final Map<String, PendingRequest> pending = new ConcurrentHashMap<>();
    private volatile boolean closed;

    /**
     * Creates an adapter for one reverse RPC method.
     *
     * @param method
     *            the reverse RPC method this adapter answers
     * @param label
     *            the capitalized feature name used in error messages, such as
     *            {@code "Proxy resolution"}
     * @param requestType
     *            the generated request type the parameters are read as
     * @param callback
     *            the SDK host callback
     * @param toResult
     *            wraps the callback's value in the method's result
     * @param executor
     *            the executor the callback runs on
     */
    CancellableCallbackAdapter(String method, String label, Class<T> requestType,
            Function<T, CompletableFuture<V>> callback, Function<V, Object> toResult, Executor executor) {
        this.method = method;
        this.label = label;
        this.requestType = requestType;
        this.callback = callback;
        this.toResult = toResult;
        this.executor = executor;
    }

    void registerHandlers(JsonRpcClient rpc) {
        rpc.registerMethodHandler(method, (rpcId, params) -> handleRequest(rpc, rpcId, params));
    }

    /**
     * Cancels the request named by a {@code $/cancelRequest} notification.
     */
    void handleCancel(JsonRpcClient rpc, JsonNode params) {
        JsonNode id = params != null ? params.get("id") : null;
        if (id == null || id.isNull()) {
            return;
        }
        PendingRequest request = pending.get(id.toString());
        if (request == null || !request.retire()) {
            return;
        }
        request.cancelCallback();
        sendError(rpc, request.responseId, REQUEST_CANCELLED, label + " request cancelled");
    }

    /**
     * Retires every request when the connection closes; no response can be
     * delivered.
     */
    void closePending() {
        closed = true;
        pending.values().forEach(request -> {
            if (request.retire()) {
                request.cancelCallback();
            }
        });
    }

    private void handleRequest(JsonRpcClient rpc, String rpcId, JsonNode params) {
        if (rpcId == null) {
            return;
        }
        var request = new PendingRequest(rpcId, parseRpcId(rpcId));
        if (pending.putIfAbsent(rpcId, request) != null) {
            sendError(rpc, request.responseId, INTERNAL_ERROR, "Duplicate " + label + " request id");
            return;
        }
        if (closed) {
            request.retire();
            return;
        }
        try {
            // The callback may block on native APIs; keep the reader free.
            CompletableFuture.runAsync(() -> runCallback(rpc, request, params), executor);
        } catch (RejectedExecutionException error) {
            // Never fall back to the reader: a blocking callback would stall every response
            // and the cancellation that could release it.
            log.log(Level.WARNING, "Executor rejected " + label + " task", error);
            completeError(rpc, request, label + " callback could not be scheduled: " + message(error));
        }
    }

    private void runCallback(JsonRpcClient rpc, PendingRequest request, JsonNode params) {
        if (request.retired()) {
            return;
        }
        CompletableFuture<V> value;
        try {
            value = callback.apply(MAPPER.treeToValue(params, requestType));
        } catch (Throwable error) {
            // Also answer Errors, e.g. an UnsatisfiedLinkError from a native lookup, so
            // the runtime doesn't wait for its timeout; fatal ones are rethrown after.
            completeError(rpc, request, message(error));
            CopilotRequestHandler.rethrowFatalError(error);
            return;
        }
        if (value == null) {
            completeError(rpc, request, label + " callback returned a null future");
            return;
        }
        request.attach(value);
        value.whenComplete((result, error) -> {
            if (error != null) {
                completeError(rpc, request, message(
                        error instanceof CompletionException && error.getCause() != null ? error.getCause() : error));
                return;
            }
            completeResult(rpc, request, result);
        });
    }

    private void completeResult(JsonRpcClient rpc, PendingRequest request, V value) {
        if (!request.retire()) {
            return;
        }
        try {
            rpc.sendResponse(request.responseId, toResult.apply(value));
        } catch (IOException error) {
            log.log(Level.FINE, "Failed to send " + label + " response", error);
        }
    }

    private void completeError(JsonRpcClient rpc, PendingRequest request, String message) {
        if (!request.retire()) {
            return;
        }
        sendError(rpc, request.responseId, INTERNAL_ERROR, message);
    }

    private void sendError(JsonRpcClient rpc, Object responseId, int code, String message) {
        try {
            rpc.sendErrorResponse(responseId, code, message);
        } catch (IOException error) {
            log.log(Level.FINE, "Failed to send " + label + " error", error);
        }
    }

    private static String message(Throwable error) {
        return error.getMessage() != null ? error.getMessage() : error.toString();
    }

    private static Object parseRpcId(String rpcId) {
        try {
            return Long.valueOf(rpcId);
        } catch (NumberFormatException ignored) {
            return rpcId;
        }
    }

    private final class PendingRequest {

        private final String key;
        private final Object responseId;
        private final AtomicBoolean retired = new AtomicBoolean();
        private volatile CompletableFuture<V> callbackFuture;

        PendingRequest(String key, Object responseId) {
            this.key = key;
            this.responseId = responseId;
        }

        void attach(CompletableFuture<V> future) {
            callbackFuture = future;
            // Retirement may have raced the callback; it cancels whichever side observes
            // the other.
            if (retired()) {
                future.cancel(false);
            }
        }

        void cancelCallback() {
            CompletableFuture<V> future = callbackFuture;
            if (future != null) {
                future.cancel(false);
            }
        }

        boolean retire() {
            if (!retired.compareAndSet(false, true)) {
                return false;
            }
            pending.remove(key, this);
            return true;
        }

        boolean retired() {
            return retired.get();
        }
    }
}
