/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The request body of an {@link LlmInferenceRequest}, delivered as a stream of
 * frames as they arrive from the runtime.
 * <p>
 * For plain HTTP the frames concatenate into the request entity; use
 * {@link #asInputStream()} or {@link #readAllBytes()}. For a WebSocket each
 * frame is one inbound message and the {@link Frame#binary()} flag
 * distinguishes text from binary; iterate with {@link #read()}.
 *
 * @since 1.0.0
 */
public final class LlmRequestBody {

    /**
     * A single request body frame.
     *
     * @param data
     *            the frame bytes
     * @param binary
     *            {@code true} when the frame was delivered as binary, {@code false}
     *            when it was UTF-8 text
     */
    public record Frame(byte[] data, boolean binary) {
    }

    private static final Frame END = new Frame(new byte[0], false);

    private final BlockingQueue<Frame> queue = new LinkedBlockingQueue<>();

    LlmRequestBody() {
    }

    void push(byte[] data, boolean binary) {
        queue.add(new Frame(data, binary));
    }

    void close() {
        queue.add(END);
    }

    /**
     * Reads the next request body frame, blocking until one is available.
     *
     * @return the next frame, or {@code null} when the body has ended
     * @throws InterruptedException
     *             if the calling thread is interrupted while waiting
     */
    public Frame read() throws InterruptedException {
        Frame frame = queue.take();
        if (frame == END) {
            // Re-arm the sentinel so repeated reads after end keep returning null.
            queue.add(END);
            return null;
        }
        return frame;
    }

    /**
     * Drains the entire request body into a single byte array, concatenating all
     * frames regardless of their {@link Frame#binary()} flag.
     *
     * @return the full request body bytes
     * @throws InterruptedException
     *             if the calling thread is interrupted while waiting
     */
    public byte[] readAllBytes() throws InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Frame frame;
        while ((frame = read()) != null) {
            out.writeBytes(frame.data());
        }
        return out.toByteArray();
    }

    /**
     * Adapts this body into a blocking {@link InputStream} over the concatenated
     * frame bytes. Thread interruption surfaces as an {@link IOException}.
     *
     * @return an input stream view of the request body
     */
    public InputStream asInputStream() {
        return new InputStream() {
            private byte[] current = new byte[0];
            private int pos;
            private boolean ended;

            @Override
            public int read() throws IOException {
                if (!fill()) {
                    return -1;
                }
                return current[pos++] & 0xFF;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (len == 0) {
                    return 0;
                }
                if (!fill()) {
                    return -1;
                }
                int n = Math.min(len, current.length - pos);
                System.arraycopy(current, pos, b, off, n);
                pos += n;
                return n;
            }

            private boolean fill() throws IOException {
                while (pos >= current.length) {
                    if (ended) {
                        return false;
                    }
                    try {
                        Frame frame = LlmRequestBody.this.read();
                        if (frame == null) {
                            ended = true;
                            return false;
                        }
                        current = frame.data();
                        pos = 0;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted while reading request body", e);
                    }
                }
                return true;
            }
        };
    }
}
