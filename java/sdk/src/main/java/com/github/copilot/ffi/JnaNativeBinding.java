/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * JNA-backed implementation of {@link NativeBinding}.
 *
 * <p>
 * Loads the {@code runtime.node} native library by absolute path and delegates
 * each {@link NativeBinding} method to the corresponding
 * {@code copilot_runtime_*} C ABI export.
 *
 * <h2>Library-never-unloads pattern</h2>
 * <p>
 * The loaded JNA library handle is held in a {@code static} field and is never
 * released. Native worker threads spawned by the runtime outlive any individual
 * {@code FfiRuntimeHost} instance; unloading the library while those threads
 * are active would cause a crash. This mirrors the Rust runtime's own
 * {@code OnceLock<Mutex<HashMap<PathBuf, &'static Library>>>} pattern.
 *
 * <h2>Duplicate-load guard</h2>
 * <p>
 * Loading a library from a <em>different</em> absolute path in the same JVM
 * process is rejected with {@link IllegalStateException}. Loading from the
 * <em>same</em> path more than once is silently accepted.
 *
 * <h2>Active-callback tracking</h2>
 * <p>
 * The {@link #activeCallbacks} counter is incremented when the native runtime
 * enters the outbound callback and decremented when the callback returns.
 * Callers (e.g. {@code FfiRuntimeHost}) must drain this counter to zero before
 * calling {@link #connectionClose} or {@link #hostShutdown}.
 *
 * <h2>GraalVM Native Image</h2>
 * <p>
 * JNA callback upcalls are not supported under GraalVM Native Image. InProcess
 * transport is not available in native-image executables; use subprocess
 * transport instead.
 */
final class JnaNativeBinding implements NativeBinding {

    private static final Logger LOG = Logger.getLogger(JnaNativeBinding.class.getName());

    /**
     * JNA inner interface mapping the five {@code copilot_runtime_*} C ABI exports.
     */
    interface CopilotRuntimeLibrary extends Library {
        /** Corresponds to {@code copilot_runtime_host_start}. */
        int copilot_runtime_host_start(byte[] argvJson, int argvJsonLen, byte[] envJson, int envJsonLen);

        /** Corresponds to {@code copilot_runtime_host_shutdown}. */
        boolean copilot_runtime_host_shutdown(int serverId);

        /** Corresponds to {@code copilot_runtime_connection_open}. */
        int copilot_runtime_connection_open(int serverId, OutboundCallback callback, Pointer userData, byte[] extSource,
                int extSourceLen, byte[] extName, int extNameLen, byte[] connToken, int connTokenLen);

        /** Corresponds to {@code copilot_runtime_connection_write}. */
        boolean copilot_runtime_connection_write(int connectionId, byte[] data, int dataLen);

        /** Corresponds to {@code copilot_runtime_connection_close}. */
        boolean copilot_runtime_connection_close(int connectionId);
    }

    // -------------------------------------------------------------------------
    // Process-wide singleton — never unloaded
    // -------------------------------------------------------------------------

    private static final Object LOAD_LOCK = new Object();

    /** Absolute path of the library that was first loaded into this JVM process. */
    private static volatile Path loadedPath;

    /** The loaded JNA library interface. Never released after first set. */
    private static volatile CopilotRuntimeLibrary loadedLib;

    // -------------------------------------------------------------------------
    // Instance state
    // -------------------------------------------------------------------------

    /**
     * The library interface used by this instance for all delegated calls.
     *
     * <p>
     * For the production path ({@link #JnaNativeBinding(Path)}), this is always the
     * same object as {@link #loadedLib} (the static singleton). For the test path
     * ({@link #JnaNativeBinding(CopilotRuntimeLibrary)}), this may be a stub or
     * mock without modifying the static singleton.
     */
    private final CopilotRuntimeLibrary lib;

    /**
     * Count of callbacks currently executing on native threads. Must reach zero
     * before {@link #connectionClose} or {@link #hostShutdown} is called.
     */
    final AtomicInteger activeCallbacks = new AtomicInteger(0);

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Loads (or re-uses) the native library at the given absolute path.
     *
     * @param libraryPath
     *            absolute path to the {@code runtime.node} native library
     * @throws IllegalStateException
     *             if a <em>different</em> library path has already been loaded in
     *             this JVM process
     */
    JnaNativeBinding(Path libraryPath) {
        Path absPath = libraryPath.toAbsolutePath().normalize();
        synchronized (LOAD_LOCK) {
            if (loadedLib == null) {
                LOG.fine(() -> "Loading native library from: " + absPath);
                loadedLib = Native.load(absPath.toString(), CopilotRuntimeLibrary.class);
                loadedPath = absPath;
                LOG.fine(() -> "Native library loaded: " + absPath);
            } else if (!absPath.equals(loadedPath)) {
                throw new IllegalStateException("An in-process FFI runtime library is already loaded from '"
                        + loadedPath + "'; loading a different library from '" + absPath
                        + "' in the same process is not supported.");
            }
        }
        this.lib = loadedLib;
    }

    /**
     * Testing constructor — accepts a pre-built {@link CopilotRuntimeLibrary}
     * directly, bypassing disk I/O and the static singleton guard.
     *
     * <p>
     * This constructor is package-private and intended solely for unit tests.
     *
     * @param library
     *            a {@link CopilotRuntimeLibrary} stub or mock for testing
     */
    JnaNativeBinding(CopilotRuntimeLibrary library) {
        // Testing seam — skip the static singleton guard.
        this.lib = library;
    }

    // -------------------------------------------------------------------------
    // NativeBinding delegation
    // -------------------------------------------------------------------------

    @Override
    public int hostStart(byte[] argvJson, int argvJsonLen, byte[] envJson, int envJsonLen) {
        return lib.copilot_runtime_host_start(argvJson, argvJsonLen, envJson, envJsonLen);
    }

    @Override
    public boolean hostShutdown(int serverId) {
        return lib.copilot_runtime_host_shutdown(serverId);
    }

    @Override
    public int connectionOpen(int serverId, OutboundCallback callback, Pointer userData, byte[] extSource,
            int extSourceLen, byte[] extName, int extNameLen, byte[] connToken, int connTokenLen) {
        // Wrap the caller's callback to maintain active-callback tracking.
        OutboundCallback tracked = (ud, data, len) -> {
            activeCallbacks.incrementAndGet();
            try {
                callback.invoke(ud, data, len);
            } finally {
                activeCallbacks.decrementAndGet();
            }
        };
        return lib.copilot_runtime_connection_open(serverId, tracked, userData, extSource, extSourceLen, extName,
                extNameLen, connToken, connTokenLen);
    }

    @Override
    public boolean connectionWrite(int connectionId, byte[] data, int dataLen) {
        return lib.copilot_runtime_connection_write(connectionId, data, dataLen);
    }

    @Override
    public boolean connectionClose(int connectionId) {
        return lib.copilot_runtime_connection_close(connectionId);
    }

    // -------------------------------------------------------------------------
    // Testing support
    // -------------------------------------------------------------------------

    /**
     * Resets the process-wide static state for unit tests.
     *
     * <p>
     * <strong>Must only be called from test code.</strong> Resets
     * {@link #loadedPath} and {@link #loadedLib} so that a subsequent
     * {@link #JnaNativeBinding(Path)} call can load a different library. In
     * production, the library is never unloaded.
     */
    static void resetForTesting() {
        synchronized (LOAD_LOCK) {
            loadedPath = null;
            loadedLib = null;
        }
    }
}
