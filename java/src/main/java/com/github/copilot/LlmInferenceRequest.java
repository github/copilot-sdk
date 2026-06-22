/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * An outbound model-layer request the runtime is asking the SDK consumer to
 * service on its behalf.
 * <p>
 * This is a low-level shape: URL / method / headers verbatim, the request body
 * delivered as a stream of frames via {@link #getRequestBody()}, and the
 * response written through {@link #getResponseBody()}. The runtime does not
 * classify the request (no provider type, endpoint kind, or wire API);
 * consumers that need that information derive it from the URL and headers. For
 * the idiomatic {@code java.net.http} view, subclass {@link LlmRequestHandler}
 * instead of implementing {@link LlmInferenceProvider} directly.
 *
 * @since 1.0.0
 */
public final class LlmInferenceRequest {

    /** The transport value for plain HTTP and SSE requests. */
    public static final String TRANSPORT_HTTP = "http";

    /** The transport value for full-duplex WebSocket requests. */
    public static final String TRANSPORT_WEBSOCKET = "websocket";

    private final String requestId;
    private final String sessionId;
    private final String method;
    private final String url;
    private final Map<String, List<String>> headers;
    private final String transport;
    private final LlmRequestBody requestBody;
    private final LlmInferenceResponseSink responseBody;
    private final CompletableFuture<Void> cancellation;

    LlmInferenceRequest(String requestId, String sessionId, String method, String url,
            Map<String, List<String>> headers, String transport, LlmRequestBody requestBody,
            LlmInferenceResponseSink responseBody, CompletableFuture<Void> cancellation) {
        this.requestId = requestId;
        this.sessionId = sessionId;
        this.method = method;
        this.url = url;
        this.headers = headers;
        this.transport = transport;
        this.requestBody = requestBody;
        this.responseBody = responseBody;
        this.cancellation = cancellation;
    }

    /**
     * Gets the opaque runtime-minted id, stable across the request lifecycle.
     *
     * @return the request id
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Gets the id of the runtime session that triggered this request, or
     * {@code null} when the request was issued outside any session (for example the
     * startup model catalog).
     *
     * @return the session id, or {@code null}
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Gets the HTTP method (GET, POST, ...).
     *
     * @return the method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Gets the absolute request URL.
     *
     * @return the URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets the request headers, multi-valued.
     *
     * @return the headers (never {@code null})
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * Gets the transport the runtime would otherwise use: {@value #TRANSPORT_HTTP}
     * (the default, covering plain HTTP and SSE) or {@value #TRANSPORT_WEBSOCKET}
     * (a full-duplex message channel where each request body frame is one inbound
     * message and each response write is one outbound message).
     *
     * @return the transport
     */
    public String getTransport() {
        return transport;
    }

    /**
     * Gets the request body, yielding frames as they arrive from the runtime.
     *
     * @return the request body
     */
    public LlmRequestBody getRequestBody() {
        return requestBody;
    }

    /**
     * Gets the sink the consumer writes the upstream response into.
     *
     * @return the response sink
     */
    public LlmInferenceResponseSink getResponseBody() {
        return responseBody;
    }

    /**
     * Whether the runtime has cancelled this in-flight request.
     *
     * @return {@code true} once the request has been cancelled
     */
    public boolean isCancelled() {
        return cancellation.isDone();
    }

    /**
     * A future that completes when the runtime cancels this in-flight request (for
     * example because the agent turn was aborted upstream). Use it to tear down the
     * outbound call.
     *
     * @return the cancellation future
     */
    public CompletableFuture<Void> getCancellation() {
        return cancellation;
    }
}
