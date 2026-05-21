/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Shared test utilities for locating the Copilot CLI binary and other
 * cross-platform test helpers.
 */
public final class TestUtil {

    private TestUtil() {
    }

    /**
     * Returns a platform-independent path string for a file inside the system
     * temporary directory. Uses {@code java.io.tmpdir} so tests run correctly on
     * both POSIX and Windows.
     *
     * @param filename
     *            the file name (no directory separator required)
     * @return absolute path string in the system temp directory
     */
    public static String tempPath(String filename) {
        return Path.of(System.getProperty("java.io.tmpdir"), filename).toString();
    }

    /**
     * Locates a launchable Copilot CLI executable.
     * <p>
     * Resolution order:
     * <ol>
     * <li>Search the system PATH using {@code where.exe} (Windows) or {@code which}
     * (Linux/macOS).</li>
     * <li>Fall back to the {@code COPILOT_CLI_PATH} environment variable.</li>
     * <li>Walk parent directories looking for
     * {@code nodejs/node_modules/@github/copilot/index.js}.</li>
     * </ol>
     *
     * <p>
     * <b>Why iterate all PATH results?</b> On Windows, {@code where.exe copilot}
     * can return multiple candidates. The first hit is often a Linux ELF binary
     * bundled inside the VS Code Insiders extension directory — it exists on disk
     * but cannot be executed by {@link ProcessBuilder} (CreateProcess error 193).
     * This method tries each candidate with {@code --version} and returns the first
     * one that actually launches, skipping non-executable entries.
     *
     * @return the absolute path to a launchable {@code copilot} binary, or
     *         {@code null} if none was found
     */
    static String findCliPath() {
        String copilotInPath = findCopilotInPath();
        if (copilotInPath != null) {
            return copilotInPath;
        }

        String envPath = System.getenv("COPILOT_CLI_PATH");
        if (envPath != null && !envPath.isEmpty()) {
            return envPath;
        }

        Path current = Paths.get(System.getProperty("user.dir"));
        while (current != null) {
            Path cliPath = current.resolve("nodejs/node_modules/@github/copilot/index.js");
            if (cliPath.toFile().exists()) {
                return cliPath.toString();
            }
            current = current.getParent();
        }

        return null;
    }

    /**
     * Searches the system PATH for a launchable {@code copilot} executable.
     * <p>
     * Uses {@code where.exe} on Windows and {@code which} on Unix-like systems. On
     * Windows, {@code where.exe} may return multiple results (e.g. a Linux ELF
     * binary, a {@code .bat} wrapper, a {@code .cmd} wrapper). This method iterates
     * all results and returns the first one that {@link ProcessBuilder} can
     * actually start.
     */
    private static String findCopilotInPath() {
        try {
            String command = System.getProperty("os.name").toLowerCase().contains("win") ? "where" : "which";
            var pb = new ProcessBuilder(command, "copilot");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    return null;
                }
                var lines = reader.lines().map(String::trim).filter(l -> !l.isEmpty()).toList();
                for (String candidate : lines) {
                    try {
                        new ProcessBuilder(candidate, "--version").redirectErrorStream(true).start().destroyForcibly();
                        return candidate;
                    } catch (Exception launchFailed) {
                        // Not launchable on this platform — try next candidate
                    }
                }
            }
        } catch (Exception e) {
            // Ignore - copilot not found in PATH
        }
        return null;
    }
}
