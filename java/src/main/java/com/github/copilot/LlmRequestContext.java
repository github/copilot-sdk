/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The per-request context handed to every {@link LlmRequestHandler} seam.
 *
 * @param requestId
 *            the opaque runtime-minted request id
 * @param sessionId
 *            the triggering session id, or {@code null} when issued outside any
 *            session
 * @param transport
 *            {@link LlmInferenceRequest#TRANSPORT_HTTP} or
 *            {@link LlmInferenceRequest#TRANSPORT_WEBSOCKET}
 * @param url
 *            the absolute request URL
 * @param headers
 *            the request headers, multi-valued
 * @param cancellation
 *            a future that completes when the runtime cancels the request
 * @since 1.0.0
 */
public record LlmRequestContext(String requestId, String sessionId, String transport, String url,
        Map<String, List<String>> headers, CompletableFuture<Void> cancellation) {
}
