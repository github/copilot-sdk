/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

/**
 * The terminal status for a callback-owned WebSocket connection.
 *
 * @since 1.0.0
 */
public final class CopilotWebSocketCloseStatus {

    /** A shared normal-closure (clean end-of-stream) instance. */
    public static final CopilotWebSocketCloseStatus NORMAL_CLOSURE = new CopilotWebSocketCloseStatus(null, null, null);

    private final String description;
    private final String errorCode;
    private final Throwable error;

    /**
     * Creates a close status.
     *
     * @param description
     *            the close description, or {@code null}
     * @param errorCode
     *            an optional machine-readable error code surfaced to the runtime
     *            when the close is a failure, or {@code null}
     * @param error
     *            the error that terminated the connection, or {@code null} for a
     *            clean close
     */
    public CopilotWebSocketCloseStatus(String description, String errorCode, Throwable error) {
        this.description = description;
        this.errorCode = errorCode;
        this.error = error;
    }

    /**
     * Gets the close description, if any.
     *
     * @return the description, or {@code null}
     */
    public String description() {
        return description;
    }

    /**
     * Gets the optional error code surfaced to the runtime when the close is a
     * failure rather than a clean end-of-stream.
     *
     * @return the error code, or {@code null}
     */
    public String errorCode() {
        return errorCode;
    }

    /**
     * Gets the error that terminated the connection, if any.
     *
     * @return the error, or {@code null} for a clean close
     */
    public Throwable error() {
        return error;
    }
}
