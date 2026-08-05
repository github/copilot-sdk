package com.github.copilot.spike.inprocess;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.WString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Sets process-level environment variables that native code loaded in-process can read,
 * and restores them when closed.
 *
 * <h2>Why this class is necessary</h2>
 *
 * <p>Java has no public API to modify the live process environment block.
 * {@code System.setProperty()} writes only the JVM property bag; native code loaded via
 * JNA (e.g., {@code runtime.node}) calls the OS env API ({@code GetEnvironmentVariableW}
 * on Windows, {@code getenv()} on Unix) which reads from the process-level environment
 * block, not the JVM property bag.
 *
 * <p>The in-process transport reads {@code COPILOT_API_URL} from the process environment
 * to determine which Copilot API endpoint to contact.  To redirect traffic to the replay
 * proxy, the Java E2E harness must write that URL into the native process environment
 * block before the runtime is loaded — something only achievable via JNA-backed OS calls.
 *
 * <h2>Analogues in other SDKs</h2>
 * <ul>
 *   <li>Rust: {@code InProcessEnvGuard} in {@code rust/tests/e2e/support.rs}
 *       uses {@code std::env::set_var} (which calls {@code SetEnvironmentVariableW} on
 *       Windows, {@code setenv()} on Unix).</li>
 *   <li>.NET: {@code InProcessEnvIsolation.Apply()} calls
 *       {@code Environment.SetEnvironmentVariable()} (Windows, which maps to
 *       {@code SetEnvironmentVariableW}) and additionally calls libc {@code setenv()}/
 *       {@code unsetenv()} via P/Invoke on non-Windows (because .NET's managed env cache
 *       and the C env block are separate on Unix).</li>
 * </ul>
 *
 * <h2>Thread safety</h2>
 * <p>The guard mutates process-global state.  The E2E harness must serialize tests
 * (concurrency = 1) while the guard is active.  Rust enforces this by setting
 * {@code RUST_E2E_CONCURRENCY=1} when in-process; Java must do the same.
 *
 * <h2>Windows implementation detail</h2>
 * <p>{@code SetEnvironmentVariableW} updates the Win32 process environment block.
 * Rust code in the loaded DLL reads env via {@code GetEnvironmentVariableW} (the Win32
 * API), so it sees the updated value.  Java's {@code System.getenv()} is a startup-time
 * snapshot and is NOT updated — that is fine for reading (to save previous values)
 * because we save before any guard is applied.
 *
 * <p>On Linux/macOS, the libc {@code setenv()} call updates the C library's
 * {@code environ} pointer, which native code in the same process reads via
 * {@code getenv()}.  Both the JVM and the loaded native library share the same
 * libc instance in a normal dynamic-linking scenario.
 */
public class InProcessEnvGuard implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(InProcessEnvGuard.class.getName());

    // -------------------------------------------------------------------------
    // OS-specific native env-mutation interfaces (JNA).
    // -------------------------------------------------------------------------

    /** Windows kernel32: set or delete an env var in the process env block. */
    private interface Kernel32Env extends Library {
        /**
         * Sets or deletes an environment variable in the process environment block.
         *
         * @param lpName  variable name; must not be null.
         * @param lpValue new value, or {@code null} to delete the variable.
         * @return non-zero on success.
         */
        boolean SetEnvironmentVariableW(WString lpName, WString lpValue);
    }

    /** Unix libc: set or delete an env var. */
    private interface LibcEnv extends Library {
        /**
         * Sets or updates an environment variable.
         *
         * @param name    variable name (narrow string).
         * @param value   new value (narrow string).
         * @param overwrite non-zero to overwrite existing value.
         * @return 0 on success, -1 on error.
         */
        int setenv(String name, String value, int overwrite);

        /**
         * Removes an environment variable.
         *
         * @param name variable name (narrow string).
         * @return 0 on success, -1 on error.
         */
        int unsetenv(String name);
    }

    // -------------------------------------------------------------------------
    // Saved env state — null value means "variable was not set before the guard".
    // -------------------------------------------------------------------------

    private final List<Map.Entry<String, String>> saved = new ArrayList<>();

    /**
     * Applies {@code applyEnv} to the native process environment block and saves the
     * previous values for restoration on {@link #close()}.
     *
     * <p>Also suppresses {@code COPILOT_HMAC_KEY} and {@code CAPI_HMAC_KEY} if they
     * exist, since the replay proxy expects Bearer/OAuth auth rather than HMAC.
     * This mirrors the Rust and .NET in-process guards.
     *
     * @param applyEnv env vars to apply (name → value); null values are not allowed.
     */
    public InProcessEnvGuard(Map<String, String> applyEnv) {
        LOG.info("[InProcessEnvGuard] Applying " + applyEnv.size()
                + " env overrides to the native process environment block"
                + (isWindows() ? " via SetEnvironmentVariableW" : " via setenv()"));

        for (Map.Entry<String, String> entry : applyEnv.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            String previous = System.getenv(name);  // JVM startup snapshot — fine for saving
            saved.add(Map.entry(name, previous == null ? "" : previous));
            // empty string as a sentinel for "was not set"
            if (previous == null) {
                saved.add(Map.entry("\0WAS_ABSENT\0" + name, ""));  // marker for absent entry
            }
            nativeSetEnv(name, value);
            LOG.info("[Env] " + name + "=" + (name.contains("TOKEN") ? "<redacted>" : value)
                    + " (saved previous: " + (previous == null ? "null" : "<present>") + ")");
        }

        // Suppress HMAC keys (replay proxy expects standard Bearer auth)
        for (String key : List.of("COPILOT_HMAC_KEY", "CAPI_HMAC_KEY")) {
            String previous = System.getenv(key);
            if (previous != null && !previous.isEmpty()) {
                saved.add(Map.entry(key, previous));
                nativeSetEnv(key, null);  // delete
                LOG.info("[Env] Suppressed " + key);
            }
        }

        LOG.info("[InProcessEnvGuard] Env guard active."
                + " Native code in this process will now see these values.");
    }

    /**
     * Restores all env vars to the values they had before this guard was created.
     */
    @Override
    public void close() {
        LOG.info("[InProcessEnvGuard] Restoring " + saved.size()
                + " env vars to pre-guard values"
                + (isWindows() ? " via SetEnvironmentVariableW" : " via setenv()/unsetenv()"));

        // Walk in reverse order (LIFO) to undo any sequence dependencies
        List<Map.Entry<String, String>> reversed = new ArrayList<>(saved);
        Collections.reverse(reversed);

        for (Map.Entry<String, String> entry : reversed) {
            String name = entry.getKey();
            if (name.startsWith("\0WAS_ABSENT\0")) {
                // Companion marker: nothing to restore (the real save/delete was already
                // applied in the loop above when we saw the real name first)
                continue;
            }
            // Check if the original was absent by looking for the marker
            boolean wasAbsent = saved.stream()
                    .anyMatch(e -> e.getKey().equals("\0WAS_ABSENT\0" + name));
            if (wasAbsent) {
                nativeSetEnv(name, null);  // delete (variable didn't exist before the guard)
            } else {
                nativeSetEnv(name, entry.getValue());  // restore previous value
            }
        }

        LOG.info("[InProcessEnvGuard] Restore complete.");
    }

    // -------------------------------------------------------------------------
    // Native env mutation helpers
    // -------------------------------------------------------------------------

    /**
     * Sets or deletes an environment variable in the native process environment block.
     *
     * @param name  variable name; must not be null.
     * @param value new value, or {@code null} to delete the variable.
     */
    private static void nativeSetEnv(String name, String value) {
        if (isWindows()) {
            nativeSetEnvWindows(name, value);
        } else {
            nativeSetEnvUnix(name, value);
        }
    }

    private static void nativeSetEnvWindows(String name, String value) {
        Kernel32Env kernel32 = Native.load("kernel32", Kernel32Env.class);
        // SetEnvironmentVariableW with null lpValue deletes the variable.
        boolean ok = kernel32.SetEnvironmentVariableW(
                new WString(name),
                value != null ? new WString(value) : null);
        if (!ok) {
            LOG.warning("[InProcessEnvGuard] SetEnvironmentVariableW failed for key=" + name);
        }
    }

    private static void nativeSetEnvUnix(String name, String value) {
        LibcEnv libc = Native.load("c", LibcEnv.class);
        if (value != null) {
            int rc = libc.setenv(name, value, 1 /* overwrite */);
            if (rc != 0) {
                LOG.warning("[InProcessEnvGuard] setenv() failed for key=" + name + " rc=" + rc);
            }
        } else {
            int rc = libc.unsetenv(name);
            if (rc != 0) {
                LOG.warning("[InProcessEnvGuard] unsetenv() failed for key=" + name + " rc=" + rc);
            }
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
