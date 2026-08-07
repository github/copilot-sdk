package com.github.copilot.spike;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Pointer;

/**
 * JNA interface mapping the real {@code runtime.node} C ABI exports.
 *
 * <p>Function signatures match {@code cabi.rs} in copilot-agent-runtime.
 */
public interface CopilotRuntimeLibrary extends Library {

    /**
     * {@code copilot_runtime_host_start} — spawns the embedded Node child and
     * blocks until it reports readiness (up to ~30 s on the Rust side).
     *
     * @return server handle ({@code 0} on failure or timeout)
     */
    int copilot_runtime_host_start(byte[] argvJson, int argvJsonLen,
                                   byte[] envJson, int envJsonLen);

    /**
     * {@code copilot_runtime_host_shutdown} — tears down the embedded host.
     * Returns {@code byte} (not boolean) because the Rust ABI exports a
     * one-byte bool.
     */
    byte copilot_runtime_host_shutdown(int serverId);

    /**
     * {@code copilot_runtime_connection_open} — opens a bidirectional
     * connection and registers the outbound callback.
     */
    int copilot_runtime_connection_open(int serverId, OutboundCallback callback,
                                        Pointer userData, byte[] extSource,
                                        int extSourceLen, byte[] extName,
                                        int extNameLen, byte[] connToken,
                                        int connTokenLen);

    /**
     * {@code copilot_runtime_connection_write} — writes a JSON-RPC frame.
     */
    byte copilot_runtime_connection_write(int connectionId, byte[] data,
                                          int dataLen);

    /**
     * {@code copilot_runtime_connection_close} — closes a connection.
     */
    byte copilot_runtime_connection_close(int connectionId);

    /**
     * Outbound callback: Rust → Java data delivery on a native thread.
     */
    interface OutboundCallback extends Callback {
        void invoke(Pointer userData, Pointer data, int len);
    }
}
