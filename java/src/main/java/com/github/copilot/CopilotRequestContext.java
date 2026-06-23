/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * The per-request context handed to every {@link CopilotRequestHandler} hook.
 * It exposes the routing and cancellation details of a single intercepted
 * request so overrides can observe or rewrite it.
 *
 * @since 1.0.0
 */
public final class CopilotRequestContext {

    private final String requestId;
    @Nullable
    private final String sessionId;
    private final CopilotRequestTransport transport;
    private final String url;
    private final Map<String, List<String>> headers;
    private final CompletableFuture<Void> cancellation;

    private LlmWebSocketResponseBridge webSocketResponse;

    CopilotRequestContext(String requestId, @Nullable String sessionId, CopilotRequestTransport transport, String url,
            Map<String, List<String>> headers, CompletableFuture<Void> cancellation) {
        this.requestId = requestId;
        this.sessionId = sessionId;
        this.transport = transport;
        this.url = url;
        this.headers = headers;
        this.cancellation = cancellation;
    }

    /**
     * Gets the opaque runtime-minted request id, stable across the request
     * lifecycle.
     *
     * @return the request id
     */
    public String requestId() {
        return requestId;
    }

    /**
     * Gets the id of the runtime session that triggered this request, or
     * {@code null} when the request was issued outside any session (for example the
     * startup model catalog).
     *
     * @return the session id, or {@code null}
     */
    @Nullable
    public String sessionId() {
        return sessionId;
    }

    /**
     * Gets the transport the runtime would otherwise use.
     *
     * @return the transport
     */
    public CopilotRequestTransport transport() {
        return transport;
    }

    /**
     * Gets the absolute request URL.
     *
     * @return the URL
     */
    public String url() {
        return url;
    }

    /**
     * Gets the request headers, multi-valued.
     *
     * @return the headers (never {@code null})
     */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /**
     * A future that completes when the runtime cancels this in-flight request (for
     * example because the agent turn was aborted upstream). Subclasses that issue
     * their own I/O should pass it through so the upstream call is torn down too.
     *
     * @return the cancellation future
     */
    public CompletableFuture<Void> cancellation() {
        return cancellation;
    }

    /**
     * Whether the runtime has cancelled this in-flight request.
     *
     * @return {@code true} once the request has been cancelled
     */
    public boolean isCancelled() {
        return cancellation.isDone();
    }

    LlmWebSocketResponseBridge webSocketResponse() {
        return webSocketResponse;
    }

    void setWebSocketResponse(LlmWebSocketResponseBridge webSocketResponse) {
        this.webSocketResponse = webSocketResponse;
    }
}
