/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Reads a blocking HTTP response body on a daemon thread and keeps at most one
 * response chunk buffered ahead of the RPC writer.
 */
final class HttpResponseReader implements AutoCloseable {

    static final int CHUNK_SIZE = 32 * 1024;

    // Match the read-ahead bound so a source fragment as large as the bound is
    // taken in one read; a smaller scratch would split it into several reads
    // and several source round-trips for no benefit.
    private static final int READ_SIZE = CHUNK_SIZE;

    record Result(byte[] data, IOException error, boolean end) {
    }

    private final InputStream source;
    private final byte[] buffered = new byte[CHUNK_SIZE];
    private final Thread thread;

    private int bufferedCount;
    private boolean closed;
    private boolean end;
    private IOException error;

    HttpResponseReader(InputStream source) {
        this.source = source;
        thread = new Thread(this::readLoop, "llm-http-response-reader");
        thread.setDaemon(true);
        thread.start();
    }

    synchronized Result next() throws InterruptedException {
        while (bufferedCount == 0 && !end && error == null && !closed) {
            wait();
        }
        if (bufferedCount > 0) {
            byte[] data = Arrays.copyOf(buffered, bufferedCount);
            bufferedCount = 0;
            notifyAll();
            return new Result(data, null, false);
        }
        if (error != null) {
            IOException result = error;
            error = null;
            return new Result(null, result, false);
        }
        return new Result(null, null, true);
    }

    private void readLoop() {
        byte[] readBuffer = new byte[READ_SIZE];
        try {
            while (true) {
                int capacity;
                synchronized (this) {
                    while (bufferedCount == CHUNK_SIZE && !closed) {
                        wait();
                    }
                    if (closed) {
                        return;
                    }
                    capacity = Math.min(readBuffer.length, CHUNK_SIZE - bufferedCount);
                }

                int count = source.read(readBuffer, 0, capacity);
                if (count < 0) {
                    synchronized (this) {
                        end = true;
                        notifyAll();
                    }
                    return;
                }
                if (count == 0) {
                    Thread.yield();
                    continue;
                }

                synchronized (this) {
                    if (closed) {
                        return;
                    }
                    System.arraycopy(readBuffer, 0, buffered, bufferedCount, count);
                    bufferedCount += count;
                    notifyAll();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            synchronized (this) {
                if (!closed) {
                    error = e;
                    notifyAll();
                }
            }
        } finally {
            synchronized (this) {
                end = true;
                notifyAll();
            }
        }
    }

    void cancel() {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            notifyAll();
        }
        try {
            source.close();
        } catch (IOException ignored) {
            // A pending read is already being abandoned.
        }
        thread.interrupt();
    }

    @Override
    public void close() {
        cancel();
        try {
            thread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
