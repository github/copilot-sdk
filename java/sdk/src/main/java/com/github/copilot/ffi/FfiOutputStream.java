/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

final class FfiOutputStream extends OutputStream {

    private final NativeBinding nativeBinding;
    private final AtomicInteger connectionId;
    private final AtomicBoolean closing;
    private final ReentrantLock operationLock;

    FfiOutputStream(NativeBinding nativeBinding, AtomicInteger connectionId, AtomicBoolean closing,
            ReentrantLock operationLock) {
        this.nativeBinding = Objects.requireNonNull(nativeBinding, "nativeBinding must not be null");
        this.connectionId = Objects.requireNonNull(connectionId, "connectionId must not be null");
        this.closing = Objects.requireNonNull(closing, "closing must not be null");
        this.operationLock = Objects.requireNonNull(operationLock, "operationLock must not be null");
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte) b}, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        Objects.requireNonNull(b, "buffer must not be null");
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException("Invalid off/len for buffer of length " + b.length);
        }
        if (len == 0) {
            return;
        }

        operationLock.lock();
        try {
            if (closing.get()) {
                throw new IOException("The in-process runtime connection is closed.");
            }
            int id = connectionId.get();
            if (id == 0) {
                throw new IOException("The in-process runtime connection is closed.");
            }

            byte[] payload = (off == 0 && len == b.length) ? b : Arrays.copyOfRange(b, off, off + len);
            if (!nativeBinding.connectionWrite(id, payload, payload.length)) {
                throw new IOException("Failed to write a frame to the in-process runtime connection.");
            }
        } finally {
            operationLock.unlock();
        }
    }
}
