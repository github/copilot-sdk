package com.github.copilot.spike.mrjar;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Loads the native library via JNA and registers a JNA {@link Callback} for the
 * outbound (Rust → Java) data path.
 *
 * <p>Used on all JDK versions. The multi-release JAR swap point is
 * {@link ReaderThreadFactory} (platform thread on JDK 17, virtual thread on JDK 25),
 * not the native binding itself. FFM is deferred per ADR-007.
 */
final class NativeBindingProvider {

    private static final Logger LOG = Logger.getLogger(NativeBindingProvider.class.getName());

    /** JNA interface mapping the Rust test DLL. */
    public interface CallbackTestLib extends Library {
        int host_start();
        boolean host_shutdown(int serverHandle);
        int connection_open(int serverHandle, OutboundCallback callback, Pointer userData, int burstCount);
        boolean connection_write(int connectionHandle, byte[] data, int len);
        boolean connection_close(int connectionHandle);
    }

    /** JNA callback — invoked by native code on a JNA-managed thread. */
    public interface OutboundCallback extends Callback {
        void invoke(Pointer userData, Pointer data, int len);
    }

    private CallbackTestLib lib;

    /** Strong reference to prevent GC of the callback while native code holds the function pointer. */
    private OutboundCallback callbackRef;

    NativeBindingProvider() {
        LOG.info("[JNA] NativeBindingProvider created");
    }

    void loadLibrary(String libName) {
        LOG.info("[JNA] Loading native library '" + libName + "' via JNA...");
        lib = Native.load(libName, CallbackTestLib.class);
        LOG.info("[JNA] Library loaded.");
    }

    int hostStart() {
        LOG.info("[JNA] Calling host_start()...");
        int handle = lib.host_start();
        LOG.info("[JNA] host_start() returned " + handle);
        return handle;
    }

    boolean hostShutdown(int serverHandle) {
        LOG.info("[JNA] Calling host_shutdown(" + serverHandle + ")...");
        boolean ok = lib.host_shutdown(serverHandle);
        LOG.info("[JNA] host_shutdown() returned " + ok);
        return ok;
    }

    int connectionOpen(int serverHandle, QueueInputStream queueIn,
                       AtomicInteger activeCallbacks, int burstCount) {
        LOG.info("[JNA] Creating JNA Callback (new Java thread per invocation)...");

        callbackRef = (Pointer userData, Pointer data, int len) -> {
            int active = activeCallbacks.incrementAndGet();
            String threadName = Thread.currentThread().getName();
            long threadId = Thread.currentThread().getId();
            LOG.info("[JNA callback] ENTERED on thread '" + threadName
                    + "' (id=" + threadId + "), active=" + active + ", len=" + len);

            try {
                byte[] bytes = data.getByteArray(0, len);
                String content = new String(bytes, StandardCharsets.UTF_8);
                LOG.info("[JNA callback] Received: " + content);

                byte[] frame = new byte[4 + len];
                frame[0] = (byte) (len >> 24);
                frame[1] = (byte) (len >> 16);
                frame[2] = (byte) (len >> 8);
                frame[3] = (byte) len;
                System.arraycopy(bytes, 0, frame, 4, len);
                queueIn.enqueue(frame);
                LOG.info("[JNA callback] Enqueued into QueueInputStream.");
            } finally {
                int remaining = activeCallbacks.decrementAndGet();
                LOG.info("[JNA callback] EXITING on thread '" + threadName
                        + "' (id=" + threadId + "), active=" + remaining);
            }
        };

        LOG.info("[JNA] Calling connection_open(serverHandle=" + serverHandle
                + ", burstCount=" + burstCount + ")...");
        int connHandle = lib.connection_open(serverHandle, callbackRef, Pointer.NULL, burstCount);
        LOG.info("[JNA] connection_open() returned connHandle=" + connHandle);
        return connHandle;
    }

    boolean connectionWrite(int connHandle, byte[] data) {
        LOG.info("[JNA] Calling connection_write(connHandle=" + connHandle
                + ", len=" + data.length + ")...");
        boolean ok = lib.connection_write(connHandle, data, data.length);
        LOG.info("[JNA] connection_write() returned " + ok);
        return ok;
    }

    boolean connectionClose(int connHandle) {
        LOG.info("[JNA] Calling connection_close(connHandle=" + connHandle + ")...");
        boolean ok = lib.connection_close(connHandle);
        LOG.info("[JNA] connection_close() returned " + ok);
        return ok;
    }
}
