package com.github.copilot.spike;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * An {@link InputStream} backed by a {@link BlockingQueue} of byte arrays.
 *
 * <p>This bridges the gap between JNA callbacks (which deliver data as {@code byte[]})
 * and the existing {@code JsonRpcClient} which reads from an {@code InputStream}.
 *
 * <p>Unlike {@link java.io.PipedInputStream}, this has <b>no thread-affinity checks</b>,
 * making it safe for use with JNA callbacks that arrive on short-lived native-attached threads.
 *
 * <p>A sentinel (zero-length byte array) signals end-of-stream.
 */
public class QueueInputStream extends InputStream {

    private static final byte[] EOF_SENTINEL = new byte[0];

    private final BlockingQueue<byte[]> queue;

    /** Current chunk being consumed. */
    private byte[] current;
    /** Read position within {@link #current}. */
    private int pos;
    /** True after EOF sentinel received. */
    private boolean eof;

    public QueueInputStream() {
        this(new LinkedBlockingQueue<>());
    }

    public QueueInputStream(BlockingQueue<byte[]> queue) {
        this.queue = queue;
    }

    /**
     * Enqueue a chunk of data. Called from any thread (including JNA callback threads).
     * The byte array is NOT copied — caller must not mutate it after enqueuing.
     */
    public void enqueue(byte[] data) {
        queue.add(data);
    }

    /**
     * Signal end-of-stream. After this, reads will return -1 once all queued data is consumed.
     */
    public void signalEof() {
        queue.add(EOF_SENTINEL);
    }

    @Override
    public int read() throws IOException {
        if (eof) {
            return -1;
        }
        while (current == null || pos >= current.length) {
            try {
                current = queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for data", e);
            }
            if (current == EOF_SENTINEL) {
                eof = true;
                return -1;
            }
            pos = 0;
        }
        return current[pos++] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (eof) {
            return -1;
        }
        if (len == 0) {
            return 0;
        }

        // Ensure we have a current chunk
        while (current == null || pos >= current.length) {
            try {
                current = queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for data", e);
            }
            if (current == EOF_SENTINEL) {
                eof = true;
                return -1;
            }
            pos = 0;
        }

        // Copy as much as possible from the current chunk
        int available = current.length - pos;
        int toCopy = Math.min(available, len);
        System.arraycopy(current, pos, b, off, toCopy);
        pos += toCopy;
        return toCopy;
    }

    @Override
    public int available() {
        if (current != null && pos < current.length) {
            return current.length - pos;
        }
        return 0;
    }
}
