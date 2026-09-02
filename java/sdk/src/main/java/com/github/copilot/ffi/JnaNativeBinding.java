/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
 * <h2>Callback lifetime</h2>
 * <p>
 * The native runtime can invoke an outbound callback after connection close and
 * host shutdown return. Each JNA callback wrapper is therefore retained for the
 * lifetime of the JVM. After host shutdown, its Java delegate is detached so a
 * late native invocation safely becomes a no-op without retaining the complete
 * host object graph.
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
        int copilot_runtime_host_start(byte[] argvJson, SizeT argvJsonLen, byte[] envJson, SizeT envJsonLen);

        /**
         * Corresponds to {@code copilot_runtime_host_shutdown}.
         *
         * <p>
         * Returns {@code byte} (not Java {@code boolean}) because the Rust ABI exports
         * a one-byte {@code bool}. JNA maps Java {@code boolean} as a 32-bit C
         * {@code int}, which would read three extra bytes.
         */
        byte copilot_runtime_host_shutdown(int serverId);

        /** Corresponds to {@code copilot_runtime_connection_open}. */
        int copilot_runtime_connection_open(int serverId, OutboundCallback callback, Pointer userData, byte[] extSource,
                SizeT extSourceLen, byte[] extName, SizeT extNameLen, byte[] connToken, SizeT connTokenLen);

        /**
         * Corresponds to {@code copilot_runtime_connection_write}.
         *
         * @see #copilot_runtime_host_shutdown for why this returns {@code byte}
         */
        byte copilot_runtime_connection_write(int connectionId, byte[] data, SizeT dataLen);

        /**
         * Corresponds to {@code copilot_runtime_connection_close}.
         *
         * @see #copilot_runtime_host_shutdown for why this returns {@code byte}
         */
        byte copilot_runtime_connection_close(int connectionId);
    }

    // -------------------------------------------------------------------------
    // Process-wide singleton — never unloaded
    // -------------------------------------------------------------------------

    private static final Object LOAD_LOCK = new Object();

    /** Absolute path of the library that was first loaded into this JVM process. */
    private static volatile Path loadedPath;

    /** The loaded JNA library interface. Never released after first set. */
    private static volatile CopilotRuntimeLibrary loadedLib;

    /**
     * Process-lifetime roots for JNA callback trampolines. Native code can invoke a
     * callback after connection and host teardown return, so entries are never
     * removed in production.
     */
    private static final Set<OutboundCallback> RETAINED_CALLBACKS = ConcurrentHashMap.newKeySet();

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

    /**
     * Callback registrations keyed by connection handle.
     * <p>
     * Registrations remain here through connection close because native callbacks
     * can still arrive. Successful host shutdown detaches their Java delegates; the
     * wrappers themselves remain rooted by {@link #RETAINED_CALLBACKS}.
     */
    private final Map<Integer, CallbackRegistration> callbackRegistrations = new ConcurrentHashMap<>();

    private static final class CallbackRegistration {
        private final int serverId;
        private final AtomicReference<OutboundCallback> delegate;
        private final AtomicInteger activeCallbacks;
        private final OutboundCallback wrapper;

        private CallbackRegistration(int serverId, OutboundCallback delegate, AtomicInteger activeCallbacks) {
            this.serverId = serverId;
            this.delegate = new AtomicReference<>(delegate);
            this.activeCallbacks = activeCallbacks;
            this.wrapper = this::invoke;
        }

        private void invoke(Pointer userData, Pointer data, SizeT len) {
            activeCallbacks.incrementAndGet();
            try {
                OutboundCallback callback = delegate.get();
                if (callback != null) {
                    callback.invoke(userData, data, len);
                }
            } finally {
                activeCallbacks.decrementAndGet();
            }
        }

        private void detach() {
            delegate.set(null);
        }
    }

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
                try {
                    loadedLib = Native.load(absPath.toString(), CopilotRuntimeLibrary.class);
                } catch (UnsatisfiedLinkError e) {
                    throw new IllegalStateException("Failed to load native library from '" + absPath + "'", e);
                }
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
        return lib.copilot_runtime_host_start(argvJson, new SizeT(argvJsonLen), envJson, new SizeT(envJsonLen));
    }

    @Override
    public boolean hostShutdown(int serverId) {
        boolean shutdown = lib.copilot_runtime_host_shutdown(serverId) != 0;
        if (shutdown) {
            callbackRegistrations.forEach((connectionId, registration) -> {
                if (registration.serverId == serverId && callbackRegistrations.remove(connectionId, registration)) {
                    registration.detach();
                }
            });
        }
        return shutdown;
    }

    @Override
    public int connectionOpen(int serverId, OutboundCallback callback, Pointer userData, byte[] extSource,
            int extSourceLen, byte[] extName, int extNameLen, byte[] connToken, int connTokenLen) {
        CallbackRegistration registration = new CallbackRegistration(serverId, callback, activeCallbacks);
        int connectionId = lib.copilot_runtime_connection_open(serverId, registration.wrapper, userData, extSource,
                new SizeT(extSourceLen), extName, new SizeT(extNameLen), connToken, new SizeT(connTokenLen));
        if (connectionId != 0) {
            RETAINED_CALLBACKS.add(registration.wrapper);
            callbackRegistrations.put(connectionId, registration);
        }
        return connectionId;
    }

    @Override
    public boolean connectionWrite(int connectionId, byte[] data, int dataLen) {
        return lib.copilot_runtime_connection_write(connectionId, data, new SizeT(dataLen)) != 0;
    }

    @Override
    public boolean connectionClose(int connectionId) {
        return lib.copilot_runtime_connection_close(connectionId) != 0;
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
            RETAINED_CALLBACKS.clear();
        }
    }
}
