package com.github.copilot.spike.mrjar;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * An {@link InputStream} backed by a {@link BlockingQueue} of byte arrays.
 *
 * <p>Bridges JNA/FFM callbacks (which deliver data as {@code byte[]}) into the
 * {@code InputStream} contract needed by {@code JsonRpcClient}. Unlike
 * {@link java.io.PipedInputStream}, this has <b>no thread-affinity checks</b>,
 * making it safe for use with both JNA callbacks (short-lived threads) and
 * FFM upcall stubs (native thread).
 */
public class QueueInputStream extends InputStream {

    private static final byte[] EOF_SENTINEL = new byte[0];

    private final BlockingQueue<byte[]> queue;
    private byte[] current;
    private int pos;
    private boolean eof;

    public QueueInputStream() {
        this(new LinkedBlockingQueue<>());
    }

    public QueueInputStream(BlockingQueue<byte[]> queue) {
        this.queue = queue;
    }

    public void enqueue(byte[] data) {
        queue.add(data);
    }

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
