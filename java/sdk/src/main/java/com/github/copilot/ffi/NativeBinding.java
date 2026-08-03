/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import com.sun.jna.Pointer;

/**
 * Internal abstraction over the Copilot runtime C ABI.
 *
 * <p>
 * Defines the five {@code extern "C"} entry points exposed by the native
 * {@code runtime.node} library. The JNA-backed implementation
 * ({@link JnaNativeBinding}) delegates to these through JNA. A future FFM
 * implementation may be substituted via the multi-release JAR mechanism without
 * changing callers.
 *
 * <p>
 * All classes in {@code com.github.copilot.ffi} are internal; consumers must
 * not reference them directly.
 *
 * <h2>C ABI entry points</h2>
 * <ul>
 * <li>{@code copilot_runtime_host_start} — start the runtime host</li>
 * <li>{@code copilot_runtime_host_shutdown} — shut down the runtime host</li>
 * <li>{@code copilot_runtime_connection_open} — open a bidirectional
 * connection</li>
 * <li>{@code copilot_runtime_connection_write} — write a JSON-RPC frame to the
 * runtime</li>
 * <li>{@code copilot_runtime_connection_close} — close a connection</li>
 * </ul>
 *
 * <h2>Wire format</h2>
 * <p>
 * All frames use LSP {@code Content-Length} header framing, identical to the
 * stdio transport. No special encoding or decoding is needed at the FFI
 * boundary.
 */
interface NativeBinding {

    /**
     * Starts the runtime host.
     *
     * <p>
     * Blocks for up to ~30 s while the worker boots and connects back. Must not be
     * called on an async/reactive executor thread.
     *
     * @param argvJson
     *            UTF-8 JSON array of strings: the entrypoint and required flags
     * @param argvJsonLen
     *            byte length of {@code argvJson}
     * @param envJson
     *            UTF-8 JSON object of environment overrides, or {@code null} when
     *            empty
     * @param envJsonLen
     *            byte length of {@code envJson}, or {@code 0} when {@code envJson}
     *            is null
     * @return server handle ({@code 0} on failure)
     */
    int hostStart(byte[] argvJson, int argvJsonLen, byte[] envJson, int envJsonLen);

    /**
     * Shuts down the runtime host.
     *
     * @param serverId
     *            non-zero server handle returned by {@link #hostStart}
     * @return {@code true} on success
     */
    boolean hostShutdown(int serverId);

    /**
     * Opens a bidirectional connection and registers the outbound data callback.
     *
     * <p>
     * The {@code extSource}, {@code extName}, and {@code connToken} parameters are
     * reserved extension points. All current SDK implementations pass
     * {@code null}/0 for all three.
     *
     * @param serverId
     *            non-zero server handle returned by {@link #hostStart}
     * @param callback
     *            JNA callback invoked by the runtime on native threads when
     *            outbound data is available; must be held as a strong reference by
     *            the caller
     * @param userData
     *            opaque cookie passed back to {@code callback} unchanged; pass
     *            {@link Pointer#NULL}
     * @param extSource
     *            reserved; pass {@code null}
     * @param extSourceLen
     *            byte length of {@code extSource}; pass {@code 0}
     * @param extName
     *            reserved; pass {@code null}
     * @param extNameLen
     *            byte length of {@code extName}; pass {@code 0}
     * @param connToken
     *            reserved; pass {@code null}
     * @param connTokenLen
     *            byte length of {@code connToken}; pass {@code 0}
     * @return connection handle ({@code 0} on failure)
     */
    int connectionOpen(int serverId, OutboundCallback callback, Pointer userData, byte[] extSource, int extSourceLen,
            byte[] extName, int extNameLen, byte[] connToken, int connTokenLen);

    /**
     * Writes a JSON-RPC frame to the runtime.
     *
     * <p>
     * The native side copies the buffer synchronously before returning; the byte
     * array does not need to survive past this call.
     *
     * @param connectionId
     *            non-zero connection handle returned by {@link #connectionOpen}
     * @param data
     *            frame bytes
     * @param dataLen
     *            byte length of {@code data}
     * @return {@code true} on success
     */
    boolean connectionWrite(int connectionId, byte[] data, int dataLen);

    /**
     * Closes a connection.
     *
     * @param connectionId
     *            non-zero connection handle returned by {@link #connectionOpen}
     * @return {@code true} on success
     */
    boolean connectionClose(int connectionId);
}
