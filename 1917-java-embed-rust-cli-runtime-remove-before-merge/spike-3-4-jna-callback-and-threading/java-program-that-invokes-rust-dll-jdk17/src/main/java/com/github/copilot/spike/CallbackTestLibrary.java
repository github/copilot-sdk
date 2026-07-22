package com.github.copilot.spike;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * JNA interface mapping the Rust test DLL's exported C ABI functions.
 *
 * <p>Mirrors the real {@code runtime.node} entry points in simplified form.
 */
public interface CallbackTestLibrary extends Library {

    /**
     * Simulates {@code copilot_runtime_host_start}.
     *
     * @return server handle (0 = failure)
     */
    int host_start();

    /**
     * Simulates {@code copilot_runtime_host_shutdown}.
     *
     * @param serverHandle handle from {@link #host_start()}
     * @return true on success
     */
    boolean host_shutdown(int serverHandle);

    /**
     * Simulates {@code copilot_runtime_connection_open}.
     *
     * <p>Spawns a native thread that invokes {@code callback} {@code burstCount} times
     * with JSON-RPC-like payloads.
     *
     * @param serverHandle handle from {@link #host_start()}
     * @param callback     the outbound callback (Rust → Java)
     * @param userData     opaque pointer passed back to callback
     * @param burstCount   number of messages the native thread will send
     * @return connection handle (0 = failure)
     */
    int connection_open(int serverHandle, OutboundCallback callback, Pointer userData, int burstCount);

    /**
     * Simulates {@code copilot_runtime_connection_write} (Java → Rust).
     *
     * @param connectionHandle handle from {@link #connection_open}
     * @param data             byte array to send
     * @param len              length of data
     * @return true on success
     */
    boolean connection_write(int connectionHandle, byte[] data, int len);

    /**
     * Simulates {@code copilot_runtime_connection_close}.
     *
     * @param connectionHandle handle from {@link #connection_open}
     * @return true on success
     */
    boolean connection_close(int connectionHandle);

    /**
     * Callback interface for native → Java data delivery.
     *
     * <p>Invoked by the Rust DLL on a <em>native thread</em>, not the Java caller's thread.
     * JNA automatically attaches the native thread to the JVM.
     */
    interface OutboundCallback extends Callback {
        /**
         * Called by native code to deliver data to Java.
         *
         * @param userData opaque pointer (same as passed to {@code connection_open})
         * @param data     pointer to the byte buffer (valid only for the duration of this call)
         * @param len      number of bytes
         */
        void invoke(Pointer userData, Pointer data, int len);
    }
}
