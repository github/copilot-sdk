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
import com.github.copilot.generated.rpc.ServerLlmInferenceApi;

/**
 * Adapts the generated {@code llmInference.*} reverse-RPC entry points onto a
 * consumer's {@link CopilotRequestHandler}. Each {@code httpRequestStart}
 * allocates an {@link LlmInferenceExchange} and runs the handler in the
 * background; subsequent {@code httpRequestChunk} frames feed its request body
 * stream.
 */
final class LlmInferenceAdapter {

    private static final Logger LOG = Logger.getLogger(LlmInferenceAdapter.class.getName());

    private final CopilotRequestHandler handler;
    private final Supplier<ServerLlmInferenceApi> rpcSupplier;
    private final Executor executor;

    private final Map<String, LlmInferenceExchange> pending = new ConcurrentHashMap<>();

    LlmInferenceAdapter(CopilotRequestHandler handler, Supplier<ServerLlmInferenceApi> rpcSupplier, Executor executor) {
        this.handler = handler;
        this.rpcSupplier = rpcSupplier;
        this.executor = executor;
    }

    void registerHandlers(JsonRpcClient rpc) {
        rpc.registerMethodHandler("llmInference.httpRequestStart",
                (rpcId, params) -> handleRequestStart(rpc, rpcId, params));
        rpc.registerMethodHandler("llmInference.httpRequestChunk",
                (rpcId, params) -> handleRequestChunk(rpc, rpcId, params));
    }

    private LlmInferenceExchange getOrCreateExchange(String requestId) {
        // The runtime dispatches httpRequestStart and httpRequestChunk frames
        // independently. Even though the current reader dispatches them in
        // order, get-or-create keeps the adapter correct regardless: a body
        // chunk (including the terminal end frame) that races ahead of its
        // start frame is buffered into the same exchange rather than dropped,
        // which would otherwise hang the body drain forever.
        return pending.computeIfAbsent(requestId, id -> new LlmInferenceExchange(id, rpcSupplier));
    }

    private void handleRequestStart(JsonRpcClient rpc, String rpcId, JsonNode params) {
        String requestId = params.get("requestId").asText();
        String sessionId = textOrNull(params, "sessionId");
        String method = textOrNull(params, "method");
        String url = textOrNull(params, "url");
        CopilotRequestTransport transport = CopilotRequestTransport.fromWire(textOrNull(params, "transport"));
        Map<String, List<String>> headers = parseHeaders(params.get("headers"));

        // Adopt any exchange a racing chunk already created — with its buffered
        // body — rather than dropping those frames.
        LlmInferenceExchange exchange = getOrCreateExchange(requestId);
        exchange.setMethod(method);
        exchange.setContext(
                new CopilotRequestContext(requestId, sessionId, transport, url, headers, exchange.cancellation()));

        // Return from httpRequestStart immediately (after registering state) so the
        // runtime's RPC reply is not gated on the consumer's I/O. The actual handler
        // work runs asynchronously.
        runAsync(() -> runHandler(exchange));

        ack(rpc, rpcId);
    }

    private void handleRequestChunk(JsonRpcClient rpc, String rpcId, JsonNode params) {
        String requestId = params.get("requestId").asText();
        // May arrive before the matching start frame; get-or-create so the body
        // is buffered, never lost.
        LlmInferenceExchange exchange = getOrCreateExchange(requestId);
        routeChunk(exchange, params);
        ack(rpc, rpcId);
    }

    private static void routeChunk(LlmInferenceExchange exchange, JsonNode params) {
        if (boolOr(params, "cancel")) {
            exchange.pushCancel();
            return;
        }
        String data = textOr(params, "data", "");
        boolean binary = boolOr(params, "binary");
        if (!data.isEmpty()) {
            byte[] bytes = binary ? Base64.getDecoder().decode(data) : data.getBytes(StandardCharsets.UTF_8);
            exchange.pushChunk(bytes, binary);
        }
        if (boolOr(params, "end")) {
            exchange.pushEnd();
        }
    }

    private void runHandler(LlmInferenceExchange exchange) {
        try {
            handler.handle(exchange);
            if (!exchange.finished()) {
                finalizeError(exchange, 502, "LLM inference handler returned without finalising the response "
                        + "(call endResponse() or errorResponse())", null);
            }
        } catch (Exception e) {
            if (exchange.cancelled() || exchange.cancellation().isDone()) {
                // The runtime already cancelled this request; the handler's throw is
                // just the abort propagating out of its upstream call.
                finalizeError(exchange, 499, "Request cancelled by runtime", "cancelled");
            } else {
                String message = e.getMessage() != null ? e.getMessage() : e.toString();
                finalizeError(exchange, 502, message, null);
            }
        } finally {
            pending.remove(exchange.requestId());
        }
    }

    private static void finalizeError(LlmInferenceExchange exchange, int status, String message, String code) {
        if (exchange.finished()) {
            return;
        }
        try {
            if (!exchange.started()) {
                exchange.startResponse(status, null, null);
            }
            exchange.errorResponse(message, code);
        } catch (IOException e) {
            LOG.log(Level.FINE, "Failed to deliver LLM inference failure", e);
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
}
