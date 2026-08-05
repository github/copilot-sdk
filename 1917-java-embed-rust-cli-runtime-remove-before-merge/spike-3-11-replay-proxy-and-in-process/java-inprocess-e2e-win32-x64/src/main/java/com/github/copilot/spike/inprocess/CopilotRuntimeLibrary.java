package com.github.copilot.spike.inprocess;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * JNA interface for the five {@code extern "C"} entry points of the Copilot
 * runtime's C ABI front door, as exercised against the <em>real</em>
 * {@code runtime.node} on win32-x64.
 *
 * <p><b>size_t mapping:</b> On Windows x64, {@code size_t} is 64-bit.  JNA maps
 * Java {@code long} (always 64-bit) to 64-bit values on the wire, making it the
 * correct Java type for {@code size_t} parameters on win64.  (Do NOT use
 * {@code NativeLong}: on Windows x64 the C {@code long} type is 32-bit under
 * MSVC's LLP64 model, which would truncate the value.)
 *
 * <p><b>uint32_t handles:</b> Java {@code int} is 32-bit and is sign-extended on
 * the stack; for handle values that are always small and positive this is safe.
 *
 * <p><b>bool return:</b> JNA maps Java {@code boolean} to C {@code _Bool}/
 * {@code bool} (1 byte).  The Rust {@code #[repr(C)]} bool ABI is stable.
 *
 * <p>See spike-3-9 for the complete parameter specification that informed these
 * types.  See spike-3-4 for the callback threading proof.
 */
public interface CopilotRuntimeLibrary extends Library {

    /**
     * Loads {@code runtime.node} from the given absolute path.
     *
     * @param absolutePath the filesystem path to {@code runtime.node}
     * @return the bound library instance
     */
    static CopilotRuntimeLibrary load(String absolutePath) {
        Logger log = Logger.getLogger(CopilotRuntimeLibrary.class.getName());
        log.info("[JNA] Loading runtime.node from: " + absolutePath);
        Map<String, Object> options = new HashMap<>();
        // JNA default is to look up by short name; for an absolute path we tell JNA
        // to use the path as-is.  Native.load() accepts an absolute path directly.
        CopilotRuntimeLibrary lib = Native.load(absolutePath, CopilotRuntimeLibrary.class,
                Collections.emptyMap());
        log.info("[JNA] runtime.node loaded successfully.");
        return lib;
    }

    // -------------------------------------------------------------------------
    // Outbound callback — invoked by native code on a JNA-managed thread.
    //
    // IMPORTANT: The callback instance must be held as a strong Java reference
    // for the entire lifetime of the connection; if GC'd, the native function
    // pointer becomes dangling and the JVM will crash.  See spike-3-4 for proof.
    //
    // Callback is invoked on a new short-lived thread per call (JNA creates and
    // attaches a Java thread automatically).  The 'data' pointer is only valid
    // for the duration of this callback invocation — copy it immediately via
    // Pointer.getByteArray(0, len).
    //
    // The len parameter is size_t (64-bit on win64) → Java long.
    // -------------------------------------------------------------------------

    /** JNA callback type for the outbound (runtime → Java) data path. */
    interface OutboundCallback extends Callback {
        /**
         * Called by native code to deliver one complete LSP frame to Java.
         *
         * @param userData the opaque pointer passed to {@code connection_open};
         *                 always {@link Pointer#NULL} in this spike (closure capture
         *                 is used instead of the C void-pointer cookie).
         * @param data     pointer to the frame bytes; valid only during this call.
         * @param len      number of bytes; size_t on win64 → Java {@code long}.
         */
        void invoke(Pointer userData, Pointer data, long len);
    }

    // -------------------------------------------------------------------------
    // C ABI entry points
    //
    // Parameter specification from spike-3-9:
    //   argv_json   — UTF-8 JSON array: always non-null.
    //   env_json    — UTF-8 JSON object or null (pass null/0 if no overrides).
    //   server_id   — handle from host_start (0 = failure).
    //   connection_id — handle from connection_open (0 = failure).
    //   ext_source/ext_name/conn_token — always null/0 (reserved extension points).
    // -------------------------------------------------------------------------

    /**
     * Starts the runtime host.  Blocks up to ~30 s while the worker boots.
     * Must be called from a blocking (platform) thread.
     *
     * @param argvJson    UTF-8 JSON array of CLI arguments; never null.
     * @param argvJsonLen byte length of argvJson.
     * @param envJson     UTF-8 JSON object of env overrides; null with len=0 if none.
     * @param envJsonLen  byte length of envJson, or 0 if envJson is null.
     * @return server handle (0 = failure — no further error retrieval API exists).
     */
    int copilot_runtime_host_start(
            byte[] argvJson, long argvJsonLen,
            byte[] envJson, long envJsonLen);

    /**
     * Shuts down the runtime host.
     *
     * @param serverId server handle from {@link #copilot_runtime_host_start}.
     * @return true on success.
     */
    boolean copilot_runtime_host_shutdown(int serverId);

    /**
     * Opens a bidirectional JSON-RPC connection.
     *
     * @param serverId      server handle.
     * @param onOutbound    callback for runtime → Java frames; must be held alive.
     * @param userData      always {@link Pointer#NULL}; use Java closure capture instead.
     * @param extSource     reserved; always {@link Pointer#NULL}.
     * @param extSourceLen  always 0.
     * @param extName       reserved; always {@link Pointer#NULL}.
     * @param extNameLen    always 0.
     * @param connToken     reserved; always {@link Pointer#NULL}.
     * @param connTokenLen  always 0.
     * @return connection handle (0 = failure).
     */
    int copilot_runtime_connection_open(
            int serverId,
            OutboundCallback onOutbound,
            Pointer userData,
            Pointer extSource, long extSourceLen,
            Pointer extName,   long extNameLen,
            Pointer connToken, long connTokenLen);

    /**
     * Writes a JSON-RPC frame from Java into the runtime.
     * Native side copies the buffer synchronously before returning.
     *
     * @param connectionId connection handle.
     * @param data         frame bytes (LSP Content-Length framing).
     * @param len          byte count; size_t on win64 → Java {@code long}.
     * @return true on success.
     */
    boolean copilot_runtime_connection_write(
            int connectionId, byte[] data, long len);

    /**
     * Closes a connection.
     *
     * @param connectionId connection handle.
     * @return true on success.
     */
    boolean copilot_runtime_connection_close(int connectionId);
}
