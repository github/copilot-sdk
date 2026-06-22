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
import java.util.concurrent.CompletableFuture;

/**
 * The idiomatic base for consumers that observe or replace LLM inference
 * requests. It implements {@link LlmInferenceProvider} by translating each
 * request into Java's canonical {@code java.net.http} types.
 * <p>
 * HTTP requests are forwarded through {@link #sendHttp}; override it to mutate
 * the request, post-process the response, or replace the call entirely.
 * WebSocket requests are serviced by {@link #openWebSocket}; override it to
 * mutate the handshake or return a fully custom
 * {@link CopilotWebSocketHandler}.
 *
 * @since 1.0.0
 */
public class LlmRequestHandler implements LlmInferenceProvider {

    private static final Set<String> FORBIDDEN_REQUEST_HEADERS = Set.of("host", "connection", "content-length",
            "transfer-encoding", "keep-alive", "upgrade", "proxy-connection", "te", "trailer");

    private static final HttpClient SHARED_HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER).build();

    private static final int RESPONSE_CHUNK_SIZE = 32 * 1024;

    static boolean isForbiddenRequestHeader(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return FORBIDDEN_REQUEST_HEADERS.contains(lower) || lower.startsWith("sec-websocket-");
    }

    @Override
    public final void onLlmRequest(LlmInferenceRequest request) throws Exception {
        LlmRequestContext ctx = new LlmRequestContext(request.getRequestId(), request.getSessionId(),
                request.getTransport(), request.getUrl(), request.getHeaders(), request.getCancellation());
        if (LlmInferenceRequest.TRANSPORT_WEBSOCKET.equals(request.getTransport())) {
            handleWebSocket(request, ctx);
        } else {
            handleHttp(request, ctx);
        }
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
    protected HttpResponse<InputStream> sendHttp(HttpRequest request, LlmRequestContext ctx) throws Exception {
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
     */
    protected CopilotWebSocketHandler openWebSocket(LlmRequestContext ctx) {
        return new ForwardingWebSocketHandler(ctx.url(), ctx.headers());
    }

    private void handleHttp(LlmInferenceRequest request, LlmRequestContext ctx) throws Exception {
        HttpRequest httpRequest = buildHttpRequest(request);
        HttpResponse<InputStream> response = sendHttp(httpRequest, ctx);
        streamResponseToSink(response, request);
    }

    private static HttpRequest buildHttpRequest(LlmInferenceRequest request) throws InterruptedException {
        String method = request.getMethod() == null ? "GET" : request.getMethod().toUpperCase(Locale.ROOT);
        boolean bodyless = method.equals("GET") || method.equals("HEAD");
        byte[] body = bodyless ? new byte[0] : request.getRequestBody().readAllBytes();
        HttpRequest.BodyPublisher publisher = body.length > 0
                ? HttpRequest.BodyPublishers.ofByteArray(body)
                : HttpRequest.BodyPublishers.noBody();

        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(request.getUrl())).method(method,
                publisher);
        Map<String, List<String>> headers = request.getHeaders();
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

    private static void streamResponseToSink(HttpResponse<InputStream> response, LlmInferenceRequest request)
            throws IOException {
        LlmInferenceResponseSink sink = request.getResponseBody();
        sink.start(new LlmInferenceResponseInit(response.statusCode()).setHeaders(response.headers().map()));
        try (InputStream body = response.body()) {
            byte[] buffer = new byte[RESPONSE_CHUNK_SIZE];
            int n;
            while ((n = body.read(buffer)) != -1) {
                if (n > 0) {
                    byte[] frame = new byte[n];
                    System.arraycopy(buffer, 0, frame, 0, n);
                    sink.writeBinary(frame);
                }
            }
        } catch (IOException e) {
            sink.error(e.getMessage(), null);
            return;
        }
        sink.end();
    }

    private void handleWebSocket(LlmInferenceRequest request, LlmRequestContext ctx) throws Exception {
        CopilotWebSocketHandler handler = openWebSocket(ctx);
        LlmInferenceResponseSink sink = request.getResponseBody();
        sink.start(new LlmInferenceResponseInit(101));

        WebSocketResponseWriter writer = new WebSocketResponseWriter() {
            @Override
            public void sendText(byte[] data) throws IOException {
                sink.write(data);
            }

            @Override
            public void sendBinary(byte[] data) throws IOException {
                sink.writeBinary(data);
            }
        };

        try {
            handler.open(writer);
        } catch (Exception e) {
            sink.error(rootMessage(e), null);
            handler.close();
            return;
        }

        Thread pump = new Thread(() -> {
            try {
                LlmRequestBody.Frame frame;
                while ((frame = request.getRequestBody().read()) != null) {
                    if (request.isCancelled()) {
                        return;
                    }
                    handler.sendRequestMessage(frame.data(), frame.binary());
                }
            } catch (Exception ignored) {
                // Pump stops; teardown happens via completion/cancellation below.
            }
        }, "llm-ws-request-pump");
        pump.setDaemon(true);
        pump.start();

        CompletableFuture<Void> pumpDone = new CompletableFuture<>();
        Thread joiner = new Thread(() -> {
            try {
                pump.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            pumpDone.complete(null);
        }, "llm-ws-pump-joiner");
        joiner.setDaemon(true);
        joiner.start();

        try {
            CompletableFuture.anyOf(handler.completion(), ctx.cancellation(), pumpDone).join();
        } catch (Exception ignored) {
            // Terminal state resolved below.
        }

        if (request.isCancelled()) {
            handler.close();
            sink.error("Request cancelled by runtime", "cancelled");
            return;
        }

        if (pumpDone.isDone() && !handler.completion().isDone()) {
            handler.close();
        }

        try {
            handler.completion().join();
            sink.end();
        } catch (Exception e) {
            sink.error(rootMessage(e), null);
        } finally {
            handler.close();
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message != null ? message : cause.getClass().getSimpleName();
    }
}
