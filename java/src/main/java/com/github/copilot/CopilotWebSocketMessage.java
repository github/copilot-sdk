/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.nio.charset.StandardCharsets;

/**
 * A single WebSocket message exchanged through a
 * {@link CopilotWebSocketHandlerBase} hook.
 *
 * @param data
 *            the message payload bytes
 * @param binary
 *            {@code true} for a binary frame, {@code false} for a UTF-8 text
 *            frame
 * @since 1.0.0
 */
public record CopilotWebSocketMessage(byte[] data, boolean binary) {

    /**
     * Decodes the payload as UTF-8 text.
     *
     * @return the payload as text
     */
    public String text() {
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * Creates a text message from a UTF-8 string.
     *
     * @param text
     *            the text payload
     * @return a text message
     */
    public static CopilotWebSocketMessage text(String text) {
        return new CopilotWebSocketMessage(text.getBytes(StandardCharsets.UTF_8), false);
    }

    /**
     * Creates a binary message from raw bytes.
     *
     * @param data
     *            the binary payload
     * @return a binary message
     */
    public static CopilotWebSocketMessage binary(byte[] data) {
        return new CopilotWebSocketMessage(data, true);
    }
}
