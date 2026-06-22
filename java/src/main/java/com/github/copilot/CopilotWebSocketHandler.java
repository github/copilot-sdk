/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.util.concurrent.CompletableFuture;

/**
 * A per-connection WebSocket handler returned by
 * {@link LlmRequestHandler#openWebSocket}.
 * <p>
 * The default implementation is {@link ForwardingWebSocketHandler}, which dials
 * the real upstream and transparently forwards messages in both directions. A
 * full transport replacement implements this interface directly and brings its
 * own transport and receive loop.
 *
 * @since 1.0.0
 */
public interface CopilotWebSocketHandler extends AutoCloseable {

    /**
     * Establishes the connection and starts forwarding upstream-to-runtime messages
     * into {@code responseWriter}. Must not block until the connection completes;
     * it returns once the connection is established.
     *
     * @param responseWriter
     *            the sink for upstream-to-runtime messages
     * @throws Exception
     *             if the connection could not be established
     */
    void open(WebSocketResponseWriter responseWriter) throws Exception;

    /**
     * Forwards one runtime-to-upstream message.
     *
     * @param data
     *            the message bytes
     * @param binary
     *            {@code true} when the runtime delivered the message as binary
     * @throws Exception
     *             if the message could not be forwarded
     */
    void sendRequestMessage(byte[] data, boolean binary) throws Exception;

    /**
     * A future that completes when the upstream connection finishes. It completes
     * normally on a clean close and exceptionally on a transport error.
     *
     * @return the completion future
     */
    CompletableFuture<Void> completion();

    /**
     * Tears down the connection. Idempotent.
     */
    @Override
    void close();
}
