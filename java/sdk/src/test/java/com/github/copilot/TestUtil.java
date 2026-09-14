/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

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
     * <li>Use the {@code COPILOT_CLI_PATH} environment variable when set.</li>
     * <li>Prepare the release pinned by {@code nodejs/package.json}.</li>
     * <li>Otherwise search the system PATH using {@code where.exe} (Windows) or
     * {@code which} (Linux/macOS).</li>
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
        String envPath = System.getenv("COPILOT_CLI_PATH");
        if (envPath != null && !envPath.isEmpty()) {
            return envPath;
        }

        Path current = Paths.get(System.getProperty("user.dir"));
        while (current != null) {
            if (current.resolve("nodejs/package.json").toFile().exists()) {
                try {
                    return preparePinnedCli(current);
                } catch (Exception preparationFailed) {
                    break;
                }
            }
            current = current.getParent();
        }

        return findCopilotInPath();
    }

    static String preparePinnedCli(Path repoRoot) throws Exception {
        var nodePath = findExecutableInPath("node");
        if (nodePath == null) {
            throw new IllegalStateException("Node.js was not found in PATH");
        }
        var process = new ProcessBuilder(nodePath, "node_modules/tsx/dist/cli.mjs", "scripts/prepare-runtime.ts",
                "--print-path").directory(repoRoot.resolve("nodejs").toFile()).redirectErrorStream(true).start();
        String output;
        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = reader.lines().reduce((first, second) -> second).orElse("").trim();
        }
        int exitCode = process.waitFor();
        if (exitCode != 0 || output.isEmpty()) {
            throw new IllegalStateException("Failed to prepare the pinned Copilot CLI: " + output);
        }
        return output;
    }

    private static String findExecutableInPath(String name) {
        var pathValue = System.getenv("PATH");
        if (pathValue == null || pathValue.isEmpty()) {
            return null;
        }

        var windows = System.getProperty("os.name").toLowerCase().contains("win");
        var fileName = windows ? name + ".exe" : name;
        for (var directory : pathValue.split(Pattern.quote(File.pathSeparator))) {
            var unquotedDirectory = directory.replaceAll("^\"|\"$", "");
            var candidate = Paths.get(unquotedDirectory, fileName).toAbsolutePath().normalize();
            if (Files.isRegularFile(candidate) && (windows || Files.isExecutable(candidate))) {
                return candidate.toString();
            }
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
