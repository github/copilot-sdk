/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.io.IOException;

/**
 * The sink a consumer writes an upstream response into.
 * <p>
 * The state machine is strict: call {@link #start} exactly once, then zero or
 * more {@link #write}/{@link #writeBinary} calls, and finish with exactly one
 * of {@link #end} or {@link #error}. Calling out of order throws.
 *
 * @since 1.0.0
 */
public interface LlmInferenceResponseSink {

    /**
     * Sends the response head (status + headers) back to the runtime.
     *
     * @param init
     *            the response head
     * @throws IOException
     *             if the frame could not be delivered or the sink is in the wrong
     *             state
     */
    void start(LlmInferenceResponseInit init) throws IOException;

    /**
     * Sends a body frame as UTF-8 text (the common case for JSON / SSE).
     *
     * @param data
     *            the body bytes, interpreted as UTF-8 text on the wire
     * @throws IOException
     *             if the frame could not be delivered or the sink is in the wrong
     *             state
     */
    void write(byte[] data) throws IOException;

    /**
     * Sends a body frame as binary (base64-encoded on the wire).
     *
     * @param data
     *            the body bytes
     * @throws IOException
     *             if the frame could not be delivered or the sink is in the wrong
     *             state
     */
    void writeBinary(byte[] data) throws IOException;

    /**
     * Marks end-of-stream cleanly.
     *
     * @throws IOException
     *             if the terminal frame could not be delivered
     */
    void end() throws IOException;

    /**
     * Marks end-of-stream with a transport-level failure.
     *
     * @param message
     *            a human-readable failure description
     * @param code
     *            an optional machine-readable error code, or {@code null}
     * @throws IOException
     *             if the terminal frame could not be delivered
     */
    void error(String message, String code) throws IOException;
}
