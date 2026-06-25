/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.io.IOException;

/**
 * Forwards upstream WebSocket messages back to the owning
 * {@link LlmInferenceExchange}. The {@code 101} upgrade head is emitted eagerly
 * via {@link #start()} (the runtime gates the WebSocket connect on it);
 * thereafter writes are serialised so the head always precedes any body or
 * terminal frame.
 */
final class LlmWebSocketResponseBridge {

    private final LlmInferenceExchange exchange;
    private final Object lock = new Object();
    private boolean started;
    private boolean completed;

    LlmWebSocketResponseBridge(LlmInferenceExchange exchange) {
        this.exchange = exchange;
    }

    /**
     * Emits the {@code 101} upgrade head now, acknowledging the WebSocket connect.
     */
    void start() throws IOException {
        run(false, () -> {
        });
    }

    void write(CopilotWebSocketMessage message) throws IOException {
        run(false, () -> {
            if (message.binary()) {
                exchange.writeResponseBinary(message.data());
            } else {
                exchange.writeResponseText(message.text());
            }
        });
    }

    void end() throws IOException {
        run(true, exchange::endResponse);
    }

    void error(String message, String code) throws IOException {
        run(true, () -> exchange.errorResponse(message, code));
    }

    private void run(boolean terminal, IoAction action) throws IOException {
        synchronized (lock) {
            if (completed) {
                return;
            }
            if (!started) {
                started = true;
                exchange.startResponse(101, null, null);
            }
            if (terminal) {
                completed = true;
            }
            action.run();
        }
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }
}
