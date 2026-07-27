package com.github.copilot.spike.inprocess;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * An {@link InputStream} backed by a {@link BlockingQueue} of byte arrays.
 *
 * <p>Bridges JNA outbound callbacks (which deliver data as {@code byte[]}) into the
 * {@code InputStream} contract that an LSP-framing reader can consume. Unlike
 * {@link java.io.PipedInputStream}, this implementation has <b>no thread-affinity
 * checks</b>, making it safe for use with JNA callbacks, which JNA invokes on a new
 * short-lived thread per call (observed as Thread-0, Thread-1, …).
 *
 * <p><b>Real runtime.node frame delivery:</b> Each invocation of the
 * {@link com.github.copilot.spike.inprocess.CopilotRuntimeLibrary.OutboundCallback}
 * delivers one <em>complete</em> LSP frame
 * ({@code Content-Length: N\r\n\r\n<body>}).  Callers may read the stream as a byte
 * stream (parsing {@code Content-Length:} headers) or may call
 * {@link #takeFrame()} to receive a complete frame as a byte array.
 *
 * <p>This class is identical in design to the one proven in spike-3-4
 * ({@code java-program-that-invokes-rust-dll-mr-jar-17-25}), with one change:
 * the spike-3-4 version prepended a 4-byte binary length header (a local convention
 * of the test DLL); this version does <em>not</em> prepend any header because the real
 * runtime delivers self-describing LSP frames.
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

    /**
     * Enqueues a frame delivered by the outbound callback. May be called from any thread.
     *
     * @param data the frame bytes; the array is not copied — callers must not reuse it.
     */
    public void enqueue(byte[] data) {
        queue.add(data);
    }

    /**
     * Signals end-of-stream. Subsequent reads will return -1.
     */
    public void signalEof() {
        queue.add(EOF_SENTINEL);
    }

    /**
     * Blocks until one complete frame is available, then returns it.
     *
     * <p>Useful when the caller knows that each enqueue corresponds to one complete
     * LSP frame and does not want to parse a streaming byte view.
     *
     * @return the next frame's bytes.
     * @throws InterruptedException if the calling thread is interrupted.
     */
    public byte[] takeFrame() throws InterruptedException {
        byte[] frame = queue.take();
        if (frame == EOF_SENTINEL) {
            eof = true;
            return new byte[0];
        }
        return frame;
    }

    @Override
    public int read() throws IOException {
        if (eof) return -1;
        while (current == null || pos >= current.length) {
            try {
                current = queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for callback data", e);
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
        if (eof) return -1;
        if (len == 0) return 0;
        while (current == null || pos >= current.length) {
            try {
                current = queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for callback data", e);
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
        if (current != null && pos < current.length) return current.length - pos;
        return 0;
    }
}
