/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.copilot.generated.rpc.EntraTokenAcquireRequest;
import com.github.copilot.generated.rpc.EntraTokenGetTokenRequest;
import com.github.copilot.rpc.EntraTokenProvider;
import com.github.copilot.rpc.EntraTokenProviderContext;

final class EntraTokenAdapter {
    private static final Logger LOG = Logger.getLogger(EntraTokenAdapter.class.getName());
    private final EntraTokenProvider provider;
    private final Executor executor;
    private final Map<Long, CompletableFuture<Void>> pending = new ConcurrentHashMap<>();
    private volatile boolean closed;

    EntraTokenAdapter(EntraTokenProvider provider, Executor executor) {
        this.provider = provider;
        this.executor = executor;
    }

    void registerHandlers(JsonRpcClient rpc) {
        rpc.registerMethodHandler("entraToken.getToken", (id, params) -> {
            long requestId;
            EntraTokenAcquireRequest request;
            try {
                requestId = Long.parseLong(id);
                request = JsonRpcClient.getObjectMapper().treeToValue(params, EntraTokenGetTokenRequest.class)
                        .request();
            } catch (Exception error) {
                sendError(rpc, id, -32602, "Invalid Entra acquisition request");
                return;
            }
            var cancelled = new CompletableFuture<Void>();
            if (pending.putIfAbsent(requestId, cancelled) != null) {
                sendError(rpc, requestId, -32603, "Duplicate Entra acquisition request");
                return;
            }
            if (closed) {
                pending.remove(requestId, cancelled);
                cancelled.complete(null);
                return;
            }
            Runnable invoke = () -> {
                if (cancelled.isDone()) {
                    return;
                }
                try {
                    var result = provider.getToken(request, new EntraTokenProviderContext(cancelled));
                    if (result == null) {
                        throw new IllegalStateException("Entra token provider returned no future");
                    }
                    result.whenComplete((value, error) -> {
                        if (!pending.remove(requestId, cancelled) || closed) {
                            return;
                        }
                        if (error != null || value == null) {
                            sendError(rpc, requestId, -32603, "Entra token provider failed");
                        } else {
                            try {
                                rpc.sendResponse(requestId, value);
                            } catch (IOException sendError) {
                                LOG.log(Level.FINE, "Failed to send Entra acquisition response", sendError);
                            }
                        }
                    });
                } catch (Exception error) {
                    if (pending.remove(requestId, cancelled)) {
                        sendError(rpc, requestId, -32603, "Entra token provider failed");
                    }
                }
            };
            try {
                if (executor == null) {
                    CompletableFuture.runAsync(invoke);
                } else {
                    CompletableFuture.runAsync(invoke, executor);
                }
            } catch (RejectedExecutionException error) {
                pending.remove(requestId, cancelled);
                cancelled.complete(null);
                sendError(rpc, requestId, -32603, "Entra token provider executor rejected acquisition");
            }
        });
    }

    void handleCancel(JsonRpcClient rpc, JsonNode params) {
        if (params == null || !params.has("id") || !params.get("id").isIntegralNumber()
                || !params.get("id").canConvertToLong()) {
            return;
        }
        long id = params.get("id").asLong();
        var cancelled = pending.remove(id);
        if (cancelled != null) {
            cancelled.complete(null);
            sendError(rpc, id, -32800, "Entra acquisition cancelled");
        }
    }

    void closePending() {
        closed = true;
        pending.forEach((id, cancelled) -> cancelled.complete(null));
        pending.clear();
    }

    private static void sendError(JsonRpcClient rpc, Object id, int code, String message) {
        try {
            rpc.sendErrorResponse(id, code, message);
        } catch (IOException error) {
            LOG.log(Level.FINE, "Failed to send Entra acquisition error", error);
        }
    }
}
