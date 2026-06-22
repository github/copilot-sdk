/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.io.IOException;

/**
 * Forwards upstream-to-runtime WebSocket messages back into the runtime
 * response. A {@link CopilotWebSocketHandler} receives one in
 * {@link CopilotWebSocketHandler#open}.
 *
 * @since 1.0.0
 */
public interface WebSocketResponseWriter {

    /**
     * Forwards an upstream text message to the runtime.
     *
     * @param data
     *            the message bytes, interpreted as UTF-8 text on the wire
     * @throws IOException
     *             if the message could not be delivered
     */
    void sendText(byte[] data) throws IOException;

    /**
     * Forwards an upstream binary message to the runtime.
     *
     * @param data
     *            the message bytes
     * @throws IOException
     *             if the message could not be delivered
     */
    void sendBinary(byte[] data) throws IOException;
}
