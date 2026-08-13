/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link InputStream} backed by a {@link BlockingQueue} of byte-array chunks.
 *
 * <p>
 * Used by the in-process FFI transport to bridge native callback frames into
 * the JSON-RPC reader.
 */
public class QueueInputStream extends InputStream {

    private static final byte[] EOF_SENTINEL = new byte[0];

    private final BlockingQueue<byte[]> queue;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private byte[] currentChunk;
    private int currentOffset;
    private boolean eof;

    /**
     * Creates a queue-backed input stream with an unbounded queue.
     */
    public QueueInputStream() {
        this(new LinkedBlockingQueue<>());
    }

    /**
     * Testing constructor that injects a queue implementation.
     *
     * @param queue
     *            backing queue
     */
    QueueInputStream(BlockingQueue<byte[]> queue) {
        this.queue = Objects.requireNonNull(queue, "queue must not be null");
    }

    void enqueue(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || closed.get()) {
            return;
        }
        queue.offer(bytes);
    }

    @Override
    public int read() throws IOException {
        byte[] one = new byte[1];
        int read = read(one, 0, 1);
        if (read == -1) {
            return -1;
        }
        return one[0] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        Objects.requireNonNull(b, "buffer must not be null");
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException("Invalid off/len for buffer of length " + b.length);
        }
        if (len == 0) {
            return 0;
        }
        if (eof) {
            return -1;
        }

        while (currentChunk == null || currentOffset >= currentChunk.length) {
            byte[] next;
            try {
                next = queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for callback data", e);
            }
            if (next == EOF_SENTINEL) {
                eof = true;
                return -1;
            }
            if (next.length == 0) {
                continue;
            }
            currentChunk = next;
            currentOffset = 0;
        }

        int available = currentChunk.length - currentOffset;
        int toCopy = Math.min(available, len);
        System.arraycopy(currentChunk, currentOffset, b, off, toCopy);
        currentOffset += toCopy;
        return toCopy;
    }

    @Override
    public int available() {
        if (currentChunk == null || currentOffset >= currentChunk.length) {
            return 0;
        }
        return currentChunk.length - currentOffset;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            queue.offer(EOF_SENTINEL);
        }
    }
}
