/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

/**
 * The transport the runtime would otherwise use to issue an intercepted
 * model-layer request.
 *
 * @since 1.0.0
 */
public enum CopilotRequestTransport {

    /**
     * Plain HTTP or a streamed SSE response. Each request/response body chunk is an
     * opaque byte range.
     */
    HTTP,

    /**
     * Full-duplex WebSocket channel. Each request-body chunk is one inbound
     * WebSocket message and each response-body write is one outbound message.
     */
    WEBSOCKET;

    /** The wire value for the plain HTTP and SSE transport. */
    static final String WIRE_HTTP = "http";

    /** The wire value for the full-duplex WebSocket transport. */
    static final String WIRE_WEBSOCKET = "websocket";

    /**
     * Maps a wire transport string onto the enum, defaulting to {@link #HTTP} for
     * {@code null} or any unrecognised value.
     *
     * @param wire
     *            the wire transport value
     * @return the transport
     */
    static CopilotRequestTransport fromWire(String wire) {
        return WIRE_WEBSOCKET.equals(wire) ? WEBSOCKET : HTTP;
    }
}
