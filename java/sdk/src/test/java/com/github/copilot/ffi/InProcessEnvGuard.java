/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.WString;

/**
 * Mutates the live process environment block so that native code loaded
 * in-process (e.g. {@code runtime.node} via JNA) observes the given environment
 * variables, and restores the previous values on {@link #close()}.
 *
 * <p>
 * Java has no public API to modify the process-level environment block:
 * {@code System.setProperty()} only writes the JVM property bag, and
 * {@code System.getenv()} is an immutable startup-time snapshot. Native code
 * loaded via JNA reads the OS environment directly
 * ({@code GetEnvironmentVariableW} on Windows, {@code getenv()} on POSIX), so
 * the only way to make it see an overridden value is to call the OS API
 * directly through JNA.
 * </p>
 *
 * <p>
 * This mirrors the Rust {@code InProcessEnvGuard}
 * ({@code rust/tests/e2e/support.rs}) and the .NET
 * {@code InProcessEnvIsolation}
 * ({@code dotnet/test/Harness/InProcessEnvIsolation.cs}).
 * </p>
 *
 * <p>
 * <strong>Thread safety:</strong> this guard mutates process-global state.
 * Tests that use it must run with test concurrency 1 (see the
 * {@code -Pinprocess} Maven profile, which sets {@code failsafe.forkCount=1}
 * and disables parallel execution).
 * </p>
 */
public final class InProcessEnvGuard implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(InProcessEnvGuard.class.getName());

    /**
     * Environment variables suppressed because replay snapshots expect Bearer/OAuth
     * auth.
     */
    private static final List<String> SUPPRESSED_KEYS = List.of("COPILOT_HMAC_KEY", "CAPI_HMAC_KEY");

    /**
     * Windows kernel32: sets or deletes a variable in the process environment
     * block.
     */
    private interface Kernel32Env extends Library {
        boolean SetEnvironmentVariableW(WString lpName, WString lpValue);

        int GetEnvironmentVariableW(WString lpName, char[] lpBuffer, int nSize);
    }

    /** POSIX libc: sets or deletes a variable in the process environment block. */
    private interface LibcEnv extends Library {
        int setenv(String name, String value, int overwrite);

        int unsetenv(String name);

        /** Returns null if the variable is not set. */
        String getenv(String name);
    }

    /**
     * Sentinel indicating the variable was not set (distinct from empty string).
     */
    private static final String ABSENT_SENTINEL = new String("\0ABSENT\0");

    /**
     * name -> previous value ({@code null} means the variable was not set before).
     */
    private final List<Map.Entry<String, String>> saved = new ArrayList<>();
    private boolean closed;

    /**
     * Applies {@code applyEnv} to the native process environment block, saving the
     * previous values for restoration by {@link #close()}. Also suppresses
     * {@code COPILOT_HMAC_KEY} / {@code CAPI_HMAC_KEY} if present, since the replay
     * proxy expects Bearer/OAuth auth rather than HMAC.
     *
     * @param applyEnv
     *            environment variables to apply; values must not be {@code null}
     */
    public InProcessEnvGuard(Map<String, String> applyEnv) {
        for (Map.Entry<String, String> entry : applyEnv.entrySet()) {
            apply(entry.getKey(), entry.getValue());
        }
        for (String key : SUPPRESSED_KEYS) {
            String previous = nativeGetEnv(key);
            if (previous != null && !previous.isEmpty()) {
                apply(key, null);
            }
        }
    }

    private void apply(String name, String value) {
        String previous = nativeGetEnv(name);
        saved.add(Map.entry(name, previous == null ? ABSENT_SENTINEL : previous));
        nativeSetEnv(name, value);
    }

    /**
     * Restores every environment variable this guard touched to the value it had
     * before construction.
     */
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        List<Map.Entry<String, String>> reversed = new ArrayList<>(saved);
        Collections.reverse(reversed);
        for (Map.Entry<String, String> entry : reversed) {
            // ABSENT_SENTINEL uses a value ("\0ABSENT\0") impossible in real env vars.
            String restoreValue = ABSENT_SENTINEL.equals(entry.getValue()) ? null : entry.getValue();
            nativeSetEnv(entry.getKey(), restoreValue);
        }
    }

    private static String nativeGetEnv(String name) {
        if (isWindows()) {
            return nativeGetEnvWindows(name);
        } else {
            return nativeGetEnvUnix(name);
        }
    }

    private static String nativeGetEnvWindows(String name) {
        Kernel32Env kernel32 = Native.load("kernel32", Kernel32Env.class);
        char[] buffer = new char[32767];
        int len = kernel32.GetEnvironmentVariableW(new WString(name), buffer, buffer.length);
        if (len == 0) {
            // Variable not set (or error — treat as absent)
            return null;
        }
        return new String(buffer, 0, len);
    }

    private static String nativeGetEnvUnix(String name) {
        LibcEnv libc = Native.load("c", LibcEnv.class);
        return libc.getenv(name);
    }

    private static void nativeSetEnv(String name, String value) {
        if (isWindows()) {
            nativeSetEnvWindows(name, value);
        } else {
            nativeSetEnvUnix(name, value);
        }
    }

    private static void nativeSetEnvWindows(String name, String value) {
        Kernel32Env kernel32 = Native.load("kernel32", Kernel32Env.class);
        boolean ok = kernel32.SetEnvironmentVariableW(new WString(name), value != null ? new WString(value) : null);
        if (!ok) {
            LOG.warning("SetEnvironmentVariableW failed for key=" + name);
        }
    }

    private static void nativeSetEnvUnix(String name, String value) {
        LibcEnv libc = Native.load("c", LibcEnv.class);
        if (value != null) {
            int rc = libc.setenv(name, value, 1);
            if (rc != 0) {
                LOG.warning("setenv() failed for key=" + name + " rc=" + rc);
            }
        } else {
            int rc = libc.unsetenv(name);
            if (rc != 0) {
                LOG.warning("unsetenv() failed for key=" + name + " rc=" + rc);
            }
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
