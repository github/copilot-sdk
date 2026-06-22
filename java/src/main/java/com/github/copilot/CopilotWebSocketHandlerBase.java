/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A per-connection WebSocket handler returned by
 * {@link CopilotRequestHandler#openWebSocket}.
 * <p>
 * The default implementation is {@link CopilotWebSocketHandler}, which dials
 * the real upstream and transparently relays messages in both directions. A
 * full transport replacement subclasses this type directly and brings its own
 * transport and receive loop, forwarding upstream-to-runtime messages by
 * calling {@link #sendResponseMessage} and finishing with
 * {@link #close(CopilotWebSocketCloseStatus)}.
 *
 * @since 1.0.0
 */
public abstract class CopilotWebSocketHandlerBase implements AutoCloseable {

    private final LlmWebSocketResponseBridge response;
    private final CompletableFuture<CopilotWebSocketCloseStatus> completion = new CompletableFuture<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile boolean suppressCloseOnDispose;

    /** The request context for this WebSocket connection. */
    protected final CopilotRequestContext context;

    /**
     * Initializes a per-connection handler for the supplied request context.
     *
     * @param context
     *            the per-request context
     */
    protected CopilotWebSocketHandlerBase(CopilotRequestContext context) {
        this.context = context;
        this.response = Objects.requireNonNull(context.webSocketResponse(),
                "WebSocket response bridge is not attached");
    }

    /**
     * Sends a message from the runtime to the upstream connection.
     *
     * @param message
     *            the message to forward upstream
     * @throws Exception
     *             if the message could not be forwarded
     */
    public abstract void sendRequestMessage(CopilotWebSocketMessage message) throws Exception;

    /**
     * Sends a message from the upstream connection back to the runtime. Override to
     * mutate or duplicate messages; call {@code super} to emit.
     *
     * @param message
     *            the upstream-to-runtime message
     * @throws Exception
     *             if the message could not be delivered
     */
    public void sendResponseMessage(CopilotWebSocketMessage message) throws Exception {
        response.write(message);
    }

    /**
     * Closes the connection and finalises the runtime-facing response. Idempotent.
     *
     * @param status
     *            the terminal status; a non-null
     *            {@link CopilotWebSocketCloseStatus#error()} surfaces a transport
     *            failure, otherwise a clean end-of-stream
     * @throws Exception
     *             if the terminal frame could not be delivered
     */
    public void close(CopilotWebSocketCloseStatus status) throws Exception {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (status.error() != null) {
            response.error(status.description() != null ? status.description() : status.error().getMessage(),
                    status.errorCode());
        } else {
            response.end();
        }
        completion.complete(status);
    }

    /**
     * Tears down the connection, finalising with a normal closure unless the
     * connection has already been closed or close-on-dispose was suppressed.
     */
    @Override
    public void close() {
        if (!suppressCloseOnDispose && !closed.get()) {
            try {
                close(CopilotWebSocketCloseStatus.NORMAL_CLOSURE);
            } catch (Exception ignored) {
                // Best-effort teardown; the connection may already be gone.
            }
        }
    }

    CompletableFuture<CopilotWebSocketCloseStatus> completion() {
        return completion;
    }

    void suppressCloseOnDispose() {
        suppressCloseOnDispose = true;
    }

    void open() throws Exception {
        // Default: nothing to establish. CopilotWebSocketHandler dials
        // the upstream here.
    }
}
