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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.InstallationConfirmationRequest;
import com.github.copilot.generated.rpc.InstallationDecision;
import com.github.copilot.generated.rpc.InstallationsConfirmResult;
import com.github.copilot.rpc.InstallationConfirmationContext;
import com.github.copilot.rpc.InstallationConfirmationHandler;

/**
 * Bridges {@code installations.confirm} reverse RPC calls to the configured
 * installation confirmation handler.
 */
final class InstallationConfirmationAdapter {

    private static final Logger LOG = Logger.getLogger(InstallationConfirmationAdapter.class.getName());
    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();
    private static final int INVALID_PARAMS = -32602;
    private static final int INTERNAL_ERROR = -32603;
    private static final int REQUEST_CANCELLED = -32800;

    private final InstallationConfirmationHandler handler;
    private final Executor executor;
    private final CompletableFuture<Void> connectionClosed = new CompletableFuture<>();
    private final Map<Long, PendingConfirmation> pending = new ConcurrentHashMap<>();

    InstallationConfirmationAdapter(InstallationConfirmationHandler handler, Executor executor) {
        this.handler = handler;
        this.executor = executor;
    }

    void registerHandlers(JsonRpcClient rpc) {
        rpc.registerMethodHandler("installations.confirm", (rpcId, params) -> handleConfirm(rpc, rpcId, params));
        rpc.registerMethodHandler("$/cancelRequest", (rpcId, params) -> handleCancel(rpc, params));
    }

    void closePending() {
        connectionClosed.complete(null);
        pending.values().forEach(PendingConfirmation::closeConnection);
        pending.clear();
    }

    private void handleConfirm(JsonRpcClient rpc, String rpcId, JsonNode params) {
        if (rpcId == null) {
            return;
        }

        Object responseId = parseRpcId(rpcId);
        try {
            InstallationConfirmationRequest request = MAPPER.treeToValue(params, InstallationConfirmationRequest.class);
            if (!isValidRequest(request)) {
                sendError(rpc, responseId, INVALID_PARAMS, "Invalid installation confirmation review");
                return;
            }
            Long numericId = parseNumericRequestId(rpcId);
            var pendingConfirmation = new PendingConfirmation(numericId, responseId, request);
            if (numericId != null && pending.putIfAbsent(numericId, pendingConfirmation) != null) {
                sendError(rpc, responseId, INTERNAL_ERROR, "Duplicate installation confirmation request id");
                return;
            }
            runAsync(() -> runHandler(rpc, pendingConfirmation));
        } catch (Exception error) {
            sendError(rpc, responseId, INVALID_PARAMS, "Invalid installation confirmation review");
        }
    }

    private void handleCancel(JsonRpcClient rpc, JsonNode params) {
        if (params == null || !params.has("id") || !params.get("id").canConvertToLong()
                || !params.get("id").isIntegralNumber()) {
            return;
        }

        PendingConfirmation confirmation = pending.get(params.get("id").asLong());
        if (confirmation == null || !confirmation.cancel()) {
            return;
        }

        sendError(rpc, confirmation.responseId, REQUEST_CANCELLED, "Installation confirmation request cancelled");
    }

    private void runHandler(JsonRpcClient rpc, PendingConfirmation confirmation) {
        if (confirmation.retired() || connectionClosed.isDone()) {
            return;
        }

        CompletableFuture<InstallationDecision> decisionFuture;
        try {
            decisionFuture = handler.confirm(confirmation.request, confirmation.context);
        } catch (Exception error) {
            completeError(rpc, confirmation, "Installation confirmation handler failed: " + message(error));
            return;
        }
        if (decisionFuture == null) {
            completeError(rpc, confirmation, "Installation confirmation handler returned a null future");
            return;
        }

        decisionFuture.whenComplete((decision, error) -> {
            if (confirmation.retired() || connectionClosed.isDone()) {
                return;
            }
            if (error != null) {
                Throwable cause = error instanceof CompletionException && error.getCause() != null
                        ? error.getCause()
                        : error;
                completeError(rpc, confirmation, "Installation confirmation handler failed: " + message(cause));
                return;
            }
            if (decision == null) {
                completeError(rpc, confirmation, "Invalid installation confirmation decision");
                return;
            }
            completeResult(rpc, confirmation, decision);
        });
    }

    private void completeResult(JsonRpcClient rpc, PendingConfirmation confirmation, InstallationDecision decision) {
        var result = new InstallationsConfirmResult(confirmation.request.confirmationId(),
                confirmation.request.reviewFingerprint(), decision);
        if (!confirmation.retire()) {
            return;
        }
        try {
            rpc.sendResponse(confirmation.responseId, result);
        } catch (IOException error) {
            LOG.log(Level.FINE, "Failed to send installation confirmation response", error);
        }
    }

    private void completeError(JsonRpcClient rpc, PendingConfirmation confirmation, String message) {
        if (!confirmation.retire()) {
            return;
        }
        sendError(rpc, confirmation.responseId, INTERNAL_ERROR, message);
    }

    private void sendError(JsonRpcClient rpc, Object responseId, int code, String message) {
        try {
            rpc.sendErrorResponse(responseId, code, message);
        } catch (IOException error) {
            LOG.log(Level.FINE, "Failed to send installation confirmation error", error);
        }
    }

    private void runAsync(Runnable task) {
        try {
            if (executor != null) {
                CompletableFuture.runAsync(task, executor);
            } else {
                CompletableFuture.runAsync(task);
            }
        } catch (RejectedExecutionException error) {
            LOG.log(Level.WARNING, "Executor rejected installation confirmation task; running inline", error);
            task.run();
        }
    }

    private static Object parseRpcId(String rpcId) {
        try {
            return Long.valueOf(rpcId);
        } catch (NumberFormatException ignored) {
            return rpcId;
        }
    }

    private static Long parseNumericRequestId(String rpcId) {
        try {
            return Long.valueOf(rpcId);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String message(Throwable error) {
        return error.getMessage() != null ? error.getMessage() : error.toString();
    }

    private static boolean isValidRequest(InstallationConfirmationRequest request) {
        return request != null && request.confirmationId() != null && request.operationId() != null
                && request.expiresAt() != null && request.reviewFingerprint() != null && request.review() != null;
    }

    private final class PendingConfirmation {

        private final Long numericId;
        private final Object responseId;
        private final InstallationConfirmationRequest request;
        private final CompletableFuture<Void> cancelled = new CompletableFuture<>();
        private final InstallationConfirmationContext context = new InstallationConfirmationContext(cancelled);
        private final AtomicBoolean retired = new AtomicBoolean();

        PendingConfirmation(Long numericId, Object responseId, InstallationConfirmationRequest request) {
            this.numericId = numericId;
            this.responseId = responseId;
            this.request = request;
        }

        boolean cancel() {
            if (!retire()) {
                return false;
            }
            cancelled.complete(null);
            return true;
        }

        boolean closeConnection() {
            if (!retire()) {
                return false;
            }
            cancelled.complete(null);
            return true;
        }

        boolean retire() {
            if (!retired.compareAndSet(false, true)) {
                return false;
            }
            if (numericId != null) {
                pending.remove(numericId, this);
            }
            return true;
        }

        boolean retired() {
            return retired.get();
        }
    }
}
