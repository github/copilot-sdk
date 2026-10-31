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
import com.github.copilot.generated.rpc.KerberosAuthAcquireRequest;
import com.github.copilot.generated.rpc.KerberosAuthAcquireResult;

/**
 * Bridges {@code kerberosAuth.acquire} reverse RPC calls to the configured
 * Kerberos authentication callback.
 * <p>
 * Acquisitions are tracked by request id before they are scheduled, so a
 * {@code $/cancelRequest} or connection close retires queued work before it
 * reaches the callback, cancels pending callback futures, and suppresses late
 * responses.
 */
final class KerberosAuthAdapter {

    private static final Logger LOG = Logger.getLogger(KerberosAuthAdapter.class.getName());
    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();
    private static final int INTERNAL_ERROR = -32603;
    private static final int REQUEST_CANCELLED = -32800;

    private final Function<KerberosAuthAcquireRequest, CompletableFuture<String>> callback;
    private final Executor executor;
    // Keyed by the JSON rendering of the request id so numeric and string ids stay
    // distinct.
    private final Map<String, PendingAcquisition> pending = new ConcurrentHashMap<>();
    private volatile boolean closed;

    KerberosAuthAdapter(Function<KerberosAuthAcquireRequest, CompletableFuture<String>> callback, Executor executor) {
        this.callback = callback;
        this.executor = executor;
    }

    void registerHandlers(JsonRpcClient rpc) {
        rpc.registerMethodHandler("kerberosAuth.acquire", (rpcId, params) -> handleAcquire(rpc, rpcId, params));
    }

    /**
     * Cancels the acquisition named by a {@code $/cancelRequest} notification.
     */
    void handleCancel(JsonRpcClient rpc, JsonNode params) {
        JsonNode id = params != null ? params.get("id") : null;
        if (id == null || id.isNull()) {
            return;
        }
        PendingAcquisition acquisition = pending.get(id.toString());
        if (acquisition == null || !acquisition.retire()) {
            return;
        }
        acquisition.cancelCallback();
        sendError(rpc, acquisition.responseId, REQUEST_CANCELLED, "Kerberos auth request cancelled");
    }

    /**
     * Retires every acquisition when the connection closes; no response can be
     * delivered.
     */
    void closePending() {
        closed = true;
        pending.values().forEach(acquisition -> {
            if (acquisition.retire()) {
                acquisition.cancelCallback();
            }
        });
    }

    private void handleAcquire(JsonRpcClient rpc, String rpcId, JsonNode params) {
        if (rpcId == null) {
            return;
        }
        var acquisition = new PendingAcquisition(rpcId, parseRpcId(rpcId));
        if (pending.putIfAbsent(rpcId, acquisition) != null) {
            sendError(rpc, acquisition.responseId, INTERNAL_ERROR, "Duplicate Kerberos auth request id");
            return;
        }
        if (closed) {
            acquisition.retire();
            return;
        }
        try {
            // The callback may block on native credential APIs; keep the reader free.
            CompletableFuture.runAsync(() -> runCallback(rpc, acquisition, params), executor);
        } catch (RejectedExecutionException error) {
            // Never fall back to the reader: a blocking lookup would stall every response
            // and the cancellation that could release it.
            LOG.log(Level.WARNING, "Executor rejected Kerberos auth task", error);
            completeError(rpc, acquisition, "Kerberos auth callback could not be scheduled: " + message(error));
        }
    }

    private void runCallback(JsonRpcClient rpc, PendingAcquisition acquisition, JsonNode params) {
        if (acquisition.retired()) {
            return;
        }
        CompletableFuture<String> token;
        try {
            token = callback.apply(MAPPER.treeToValue(params, KerberosAuthAcquireRequest.class));
        } catch (Exception error) {
            completeError(rpc, acquisition, message(error));
            return;
        }
        if (token == null) {
            completeError(rpc, acquisition, "Kerberos auth callback returned a null future");
            return;
        }
        acquisition.attach(token);
        token.whenComplete((value, error) -> {
            if (error != null) {
                completeError(rpc, acquisition, message(
                        error instanceof CompletionException && error.getCause() != null ? error.getCause() : error));
                return;
            }
            completeResult(rpc, acquisition, value);
        });
    }

    private void completeResult(JsonRpcClient rpc, PendingAcquisition acquisition, String token) {
        if (!acquisition.retire()) {
            return;
        }
        try {
            rpc.sendResponse(acquisition.responseId, new KerberosAuthAcquireResult(token));
        } catch (IOException error) {
            LOG.log(Level.FINE, "Failed to send Kerberos auth response", error);
        }
    }

    private void completeError(JsonRpcClient rpc, PendingAcquisition acquisition, String message) {
        if (!acquisition.retire()) {
            return;
        }
        sendError(rpc, acquisition.responseId, INTERNAL_ERROR, message);
    }

    private static void sendError(JsonRpcClient rpc, Object responseId, int code, String message) {
        try {
            rpc.sendErrorResponse(responseId, code, message);
        } catch (IOException error) {
            LOG.log(Level.FINE, "Failed to send Kerberos auth error", error);
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

    private final class PendingAcquisition {

        private final String key;
        private final Object responseId;
        private final AtomicBoolean retired = new AtomicBoolean();
        private volatile CompletableFuture<String> callbackFuture;

        PendingAcquisition(String key, Object responseId) {
            this.key = key;
            this.responseId = responseId;
        }

        void attach(CompletableFuture<String> future) {
            callbackFuture = future;
            // Retirement may have raced the callback; it cancels whichever side observes
            // the other.
            if (retired()) {
                future.cancel(false);
            }
        }

        void cancelCallback() {
            CompletableFuture<String> future = callbackFuture;
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
