/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * The base class for SDK consumers who want to observe or replace the LLM
 * inference requests the runtime issues (for both CAPI and BYOK providers).
 * <p>
 * When set as the {@code requestHandler} on
 * {@link com.github.copilot.rpc.CopilotClientOptions}, the runtime routes its
 * model-layer HTTP and WebSocket traffic through this handler instead of
 * issuing the calls itself. Subclass and override {@link #sendRequest} to
 * mutate or replace HTTP calls, or {@link #openWebSocket} to mutate the
 * handshake or return a fully custom {@link CopilotWebSocketHandler}.
 *
 * @since 1.0.0
 */
public class CopilotRequestHandler {

    private static final Set<String> FORBIDDEN_REQUEST_HEADERS = Set.of("host", "connection", "content-length",
            "transfer-encoding", "keep-alive", "upgrade", "proxy-connection", "te", "trailer");

    private static final HttpClient SHARED_HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER).build();

    private static final int RESPONSE_CHUNK_SIZE = 32 * 1024;

    static boolean isForbiddenRequestHeader(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return FORBIDDEN_REQUEST_HEADERS.contains(lower) || lower.startsWith("sec-websocket-");
    }

    /**
     * The {@link HttpClient} used to forward HTTP requests. Override to supply a
     * custom client (proxy, TLS, timeouts). The default never follows redirects, so
     * 3xx responses are forwarded verbatim.
     *
     * @return the HTTP client
     */
    protected HttpClient httpClient() {
        return SHARED_HTTP_CLIENT;
    }

    /**
     * Forwards an HTTP request and returns the upstream response. The default sends
     * {@code request} through {@link #httpClient()} and cancels the in-flight call
     * when the runtime cancels the request. Override to mutate the request before
     * sending, post-process the response, or replace the call entirely.
     *
     * @param request
     *            the request built from the runtime's inference request
     * @param ctx
     *            the per-request context
     * @return the upstream response, with the body as an {@link InputStream}
     * @throws Exception
     *             if the request could not be completed
     */
    protected HttpResponse<InputStream> sendRequest(HttpRequest request, CopilotRequestContext ctx) throws Exception {
        CompletableFuture<HttpResponse<InputStream>> future = httpClient().sendAsync(request,
                HttpResponse.BodyHandlers.ofInputStream());
        ctx.cancellation().whenComplete((v, t) -> future.cancel(true));
        return future.get();
    }

    /**
     * Returns a per-connection WebSocket handler for a WebSocket request. The
     * default opens a transparent forwarding connection to the request URL.
     * Override to mutate the handshake (via {@code ctx}) or return a fully custom
     * handler.
     *
     * @param ctx
     *            the per-request context
     * @return the WebSocket handler
     * @throws Exception
     *             if the handler could not be created
     */
    protected CopilotWebSocketHandler openWebSocket(CopilotRequestContext ctx) throws Exception {
        return new ForwardingCopilotWebSocketHandler(ctx);
    }

    /**
     * Entry point invoked by the adapter once per intercepted request. Routes to
     * the HTTP or WebSocket flow and drives the consumer's overridable hooks.
     */
    void handle(LlmInferenceExchange exchange) throws Exception {
        if (exchange.context().transport() == CopilotRequestTransport.WEBSOCKET) {
            handleWebSocket(exchange);
        } else {
            handleHttp(exchange);
        }
    }

    private void handleHttp(LlmInferenceExchange exchange) throws Exception {
        HttpRequest httpRequest = buildHttpRequest(exchange);
        HttpResponse<InputStream> response = sendRequest(httpRequest, exchange.context());
        streamResponse(response, exchange);
    }

    private static HttpRequest buildHttpRequest(LlmInferenceExchange exchange) throws InterruptedException {
        CopilotRequestContext ctx = exchange.context();
        String method = exchange.method() == null ? "GET" : exchange.method().toUpperCase(Locale.ROOT);
        boolean bodyless = method.equals("GET") || method.equals("HEAD");
        byte[] body = bodyless ? new byte[0] : exchange.drainBody();
        HttpRequest.BodyPublisher publisher = body.length > 0
                ? HttpRequest.BodyPublishers.ofByteArray(body)
                : HttpRequest.BodyPublishers.noBody();

        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(ctx.url())).method(method, publisher);
        Map<String, List<String>> headers = ctx.headers();
        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (isForbiddenRequestHeader(entry.getKey()) || entry.getValue() == null) {
                    continue;
                }
                for (String value : entry.getValue()) {
                    builder.header(entry.getKey(), value);
                }
            }
        }
        return builder.build();
    }

    private static void streamResponse(HttpResponse<InputStream> response, LlmInferenceExchange exchange)
            throws IOException {
        exchange.startResponse(response.statusCode(), null, response.headers().map());
        try (InputStream body = response.body()) {
            byte[] buffer = new byte[RESPONSE_CHUNK_SIZE];
            int n;
            while ((n = body.read(buffer)) != -1) {
                if (n > 0) {
                    byte[] frame = new byte[n];
                    System.arraycopy(buffer, 0, frame, 0, n);
                    exchange.writeResponseBinary(frame);
                }
            }
        } catch (IOException e) {
            exchange.errorResponse(e.getMessage(), null);
            return;
        }
        exchange.endResponse();
    }

    private void handleWebSocket(LlmInferenceExchange exchange) throws Exception {
        CopilotRequestContext ctx = exchange.context();
        LlmWebSocketResponseBridge bridge = new LlmWebSocketResponseBridge(exchange);
        ctx.setWebSocketResponse(bridge);

        CopilotWebSocketHandler handler = openWebSocket(ctx);
        try {
            handler.open();

            // The runtime blocks the WebSocket connect until it receives the 101
            // response head (the upgrade acknowledgement) and only then begins
            // forwarding inbound messages as request-body chunks. Emit it eagerly
            // here — waiting for the first upstream message would deadlock, since the
            // upstream stays silent until it receives a request message the runtime
            // won't send before the upgrade completes.
            bridge.start();

            CompletableFuture<Void> pumpDone = new CompletableFuture<>();
            Thread pump = new Thread(() -> {
                try {
                    LlmInferenceExchange.BodyFrame frame;
                    while ((frame = exchange.readFrame()) != null) {
                        handler.sendRequestMessage(new CopilotWebSocketMessage(frame.data(), frame.binary()));
                    }
                    pumpDone.complete(null);
                } catch (Exception e) {
                    pumpDone.completeExceptionally(e);
                }
            }, "llm-ws-request-pump");
            pump.setDaemon(true);
            pump.start();

            CompletableFuture.anyOf(pumpDone, handler.completion()).handle((v, t) -> null).join();

            if (pumpDone.isDone() && !handler.completion().isDone()) {
                if (isPumpFault(pumpDone)) {
                    handler.suppressCloseOnDispose();
                    awaitPump(pumpDone);
                    return;
                }
                handler.close(CopilotWebSocketCloseStatus.NORMAL_CLOSURE);
                handler.completion().join();
                return;
            }

            CopilotWebSocketCloseStatus status = handler.completion().join();
            if (status.error() != null) {
                throw asException(status.error());
            }
        } finally {
            handler.close();
        }
    }

    private static boolean isPumpFault(CompletableFuture<Void> pumpDone) {
        return pumpDone.isCompletedExceptionally();
    }

    private static void awaitPump(CompletableFuture<Void> pumpDone) throws Exception {
        try {
            pumpDone.join();
        } catch (CancellationException e) {
            throw e;
        } catch (Exception e) {
            throw asException(e.getCause() != null ? e.getCause() : e);
        }
    }

    private static Exception asException(Throwable t) {
        return t instanceof Exception e ? e : new RuntimeException(t);
    }
}
