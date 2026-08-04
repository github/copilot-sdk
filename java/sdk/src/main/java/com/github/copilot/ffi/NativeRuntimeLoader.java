/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

/**
 * Locates the {@code runtime.node} native binary, extracts it to a versioned
 * cache directory, and returns the filesystem path for JNA to load.
 *
 * <p>
 * Resolution order:
 * <ol>
 * <li><strong>{@code COPILOT_CLI_PATH}</strong> — checks for
 * {@code runtime.node} alongside the configured CLI before any classpath or
 * platform work.</li>
 * <li><strong>Classpath resource</strong>
 * {@code native/<classifier>/runtime.node} — extracted atomically to
 * {@code ~/.copilot/runtime-cache/<version>/<classifier>/runtime.node}.</li>
 * <li>{@code runtime.node} alongside the bundled {@code copilot}
 * executable.</li>
 * </ol>
 */
public final class NativeRuntimeLoader {

    static final String RUNTIME_FILENAME = "runtime.node";
    /** Environment variable that overrides where the runtime is loaded from. */
    public static final String COPILOT_CLI_PATH_ENV = "COPILOT_CLI_PATH";
    static final String VERSION_RESOURCE = "copilot-runtime.properties";

    /**
     * Abstraction for the atomic publish step, enabling deterministic failure
     * injection in tests while preserving {@link StandardCopyOption#ATOMIC_MOVE} in
     * production.
     */
    @FunctionalInterface
    interface AtomicPublisher {
        /**
         * Atomically publishes {@code temp} to {@code cached}.
         *
         * @param temp
         *            fully-written temporary file in the same directory as
         *            {@code cached}
         * @param cached
         *            intended final location
         * @throws IOException
         *             if the move fails
         */
        void publish(Path temp, Path cached) throws IOException;
    }

    /**
     * Production publisher: {@link Files#move} with
     * {@link StandardCopyOption#ATOMIC_MOVE}.
     */
    static final AtomicPublisher DEFAULT_PUBLISHER = (temp, cached) -> {
        try {
            Files.move(temp, cached, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            throw new IllegalStateException("Filesystem does not support atomic moves; cannot safely publish "
                    + RUNTIME_FILENAME + " to " + cached, ex);
        } catch (FileAlreadyExistsException ex) {
            // Another process won the race — accept the winner if it is a valid file.
            try {
                if (isValidCachedFile(cached)) {
                    return;
                }
            } catch (IOException ignored) {
                // fall through to the error below
            }
            throw new IllegalStateException(
                    "Concurrent extraction race: target already exists but is not a valid file: " + cached, ex);
        }
    };

    private NativeRuntimeLoader() {
    }

    /**
     * Resolves the filesystem path to the {@code runtime.node} binary.
     *
     * <p>
     * Follows the three-step resolution order documented on this class. The
     * returned path is guaranteed to refer to a regular, non-empty file at the time
     * of return.
     *
     * @return absolute path to the {@code runtime.node} binary
     * @throws IOException
     *             if the binary cannot be located or extracted
     * @throws IllegalStateException
     *             if required resources are missing or extraction fails
     */
    public static Path resolve() throws IOException {
        String cliPathEnv = System.getenv(COPILOT_CLI_PATH_ENV);
        Path cliOverride = resolveFromCliPath(cliPathEnv);
        if (cliOverride != null) {
            return cliOverride;
        }

        ClassLoader loader = NativeRuntimeLoader.class.getClassLoader();
        String classifier = PlatformDetector.detectClassifier();
        String version = readVersion(loader);
        Path cacheBase = defaultCacheBase();
        return resolve(null, findRuntimeOnPath(), cacheBase, loader, classifier, version);
    }

    /**
     * Reads the SDK version from the filtered {@code copilot-runtime.properties}
     * resource.
     *
     * @return the version string
     * @throws IOException
     *             if the resource cannot be read
     * @throws IllegalStateException
     *             if the resource is missing or the version property is blank
     */
    static String readVersion(ClassLoader loader) throws IOException {
        URL resource = loader.getResource(VERSION_RESOURCE);
        if (resource == null) {
            throw new IllegalStateException("Missing version resource: " + VERSION_RESOURCE
                    + " — ensure Maven resource filtering has run (mvn process-resources)");
        }
        Properties props = new Properties();
        try (InputStream in = resource.openStream()) {
            props.load(in);
        }
        String version = props.getProperty("version");
        if (version == null || version.isBlank()) {
            throw new IllegalStateException("Blank or missing 'version' property in " + VERSION_RESOURCE
                    + " — check Maven resource filtering configuration");
        }
        return version;
    }

    /**
     * Resolves the runtime binary path using the given parameters. Package-private
     * to allow injection of test doubles in unit tests.
     */
    static Path resolve(String cliPathEnv, Path cacheBase, ClassLoader loader, String classifier, String version)
            throws IOException {
        return resolve(cliPathEnv, cacheBase, loader, classifier, version, null, DEFAULT_PUBLISHER);
    }

    static Path resolve(String cliPathEnv, String bundledCliPath, Path cacheBase, ClassLoader loader, String classifier,
            String version) throws IOException {
        Path bundledCliDir = bundledCliPath == null ? null : Path.of(bundledCliPath).toAbsolutePath().getParent();
        return resolve(cliPathEnv, cacheBase, loader, classifier, version, bundledCliDir, DEFAULT_PUBLISHER);
    }

    static Path resolve(String cliPathEnv, Path cacheBase, ClassLoader loader, String classifier, String version,
            Path bundledCliDir) throws IOException {
        return resolve(cliPathEnv, cacheBase, loader, classifier, version, bundledCliDir, DEFAULT_PUBLISHER);
    }

    static Path resolve(String cliPathEnv, Path cacheBase, ClassLoader loader, String classifier, String version,
            Path bundledCliDir, AtomicPublisher publisher) throws IOException {
        Path cliOverride = resolveFromCliPath(cliPathEnv);
        if (cliOverride != null) {
            return cliOverride;
        }

        return resolveFromClasspathOrBundledCli(cacheBase, loader, classifier, version, bundledCliDir, publisher);
    }

    /**
     * Checks for {@code runtime.node} alongside the configured CLI.
     *
     * <p>
     * Checks, in order, the flat bundled layout ({@code runtime.node} directly next
     * to the CLI) and the npm package layout
     * ({@code prebuilds/<classifier>/runtime.node} next to the CLI), matching the
     * two layouts the {@code @github/copilot-<platform>} packages may ship.
     */
    static Path resolveFromCliPath(String cliPathStr) throws IOException {
        if (cliPathStr == null || cliPathStr.isBlank()) {
            return null;
        }
        Path cliPath = Path.of(cliPathStr).toAbsolutePath().normalize();
        Path parent = cliPath.getParent();

        Path flat = parent.resolve(RUNTIME_FILENAME);
        if (Files.isRegularFile(flat) && Files.size(flat) > 0) {
            return flat;
        }

        Path prebuilt = parent.resolve("prebuilds").resolve(PlatformDetector.detectClassifier())
                .resolve(RUNTIME_FILENAME);
        if (Files.isRegularFile(prebuilt) && Files.size(prebuilt) > 0) {
            return prebuilt;
        }

        return null;
    }

    /**
     * Extracts the classpath resource {@code native/<classifier>/runtime.node} to
     * the versioned cache directory, using an atomic publish sequence to prevent
     * readers from observing a partially-written file. Uses
     * {@link #DEFAULT_PUBLISHER}.
     *
     * @param cacheBase
     *            root cache directory (e.g. {@code ~/.copilot/runtime-cache})
     * @param loader
     *            class loader used to open the classpath resource
     * @param classifier
     *            platform classifier (e.g. {@code linux-x64})
     * @param version
     *            SDK version used as the cache key
     * @return path to the extracted {@code runtime.node} binary
     * @throws IOException
     *             if I/O or the atomic rename fails
     * @throws IllegalStateException
     *             if the classpath resource is missing or empty, or if the
     *             filesystem does not support atomic moves
     */
    static Path extractToCache(Path cacheBase, ClassLoader loader, String classifier, String version)
            throws IOException {
        return extractToCache(cacheBase, loader, classifier, version, DEFAULT_PUBLISHER);
    }

    /**
     * Extracts the classpath resource to the versioned cache directory with an
     * injectable publisher. Package-private for unit tests.
     *
     * @param cacheBase
     *            root cache directory
     * @param loader
     *            class loader used to open the classpath resource
     * @param classifier
     *            platform classifier
     * @param version
     *            SDK version used as the cache key
     * @param publisher
     *            atomic publish implementation
     * @return path to the extracted {@code runtime.node} binary
     * @throws IOException
     *             if I/O or the atomic rename fails
     * @throws IllegalStateException
     *             if the classpath resource is missing or empty
     */
    static Path extractToCache(Path cacheBase, ClassLoader loader, String classifier, String version,
            AtomicPublisher publisher) throws IOException {
        String resourcePath = "native/" + classifier + "/" + RUNTIME_FILENAME;
        Path cacheDir = cacheBase.resolve(version).resolve(classifier);
        Path cached = cacheDir.resolve(RUNTIME_FILENAME);

        // Step 1 — fast path: return an existing valid cache entry.
        if (isValidCachedFile(cached)) {
            return cached;
        }

        // Step 2 — locate the classpath resource before creating any files.
        URL resource = loader.getResource(resourcePath);
        if (resource == null) {
            throw new FileNotFoundException("Native runtime not found on classpath: " + resourcePath
                    + " — add the matching classifier JAR to the classpath");
        }

        // Step 3 — ensure the cache directory exists.
        Files.createDirectories(cacheDir);

        // Step 4 — write to a unique sibling temp file, then publish atomically.
        Path temp = Files.createTempFile(cacheDir, "runtime-tmp-", ".node");
        try {
            copyResourceToTemp(resource, resourcePath, temp);
            publisher.publish(temp, cached);
        } finally {
            tryDelete(temp);
        }

        return cached;
    }

    /**
     * Tries source 2 (classpath extraction) first and falls back to source 3
     * (bundled-CLI sibling) only when the classpath resource is absent.
     */
    private static Path resolveFromClasspathOrBundledCli(Path cacheBase, ClassLoader loader, String classifier,
            String version, Path bundledCliDir, AtomicPublisher publisher) throws IOException {
        // Source 2: classpath resource.
        try {
            return extractToCache(cacheBase, loader, classifier, version, publisher);
        } catch (FileNotFoundException ex) {
            // Source 3: runtime.node alongside the bundled CLI binary.
            if (bundledCliDir != null) {
                Path candidate = bundledCliDir.resolve(RUNTIME_FILENAME);
                try {
                    if (isValidCachedFile(candidate)) {
                        return candidate;
                    }
                } catch (IOException ignored) {
                    // fall through and rethrow the original classpath error
                }
            }
            throw ex;
        }
    }

    private static boolean isValidCachedFile(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        return Files.size(path) > 0;
    }

    private static void copyResourceToTemp(URL resource, String resourcePath, Path temp) throws IOException {
        try (InputStream in = resource.openStream()) {
            long bytesWritten = Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            if (bytesWritten == 0) {
                throw new IllegalStateException("Classpath resource is empty: " + resourcePath);
            }
        }
        // Flush OS buffers to durable storage before the atomic rename.
        try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }

    /**
     * Finds the runtime executable on the {@code PATH}.
     *
     * @return the absolute path, or {@code null} if none was found
     */
    public static String findRuntimeOnPath() {
        String pathValue = System.getenv("PATH");
        if (pathValue == null || pathValue.isBlank()) {
            return null;
        }

        String[] executableNames = isWindows()
                ? new String[]{"copilot.exe", "copilot.cmd", "copilot.bat", "copilot"}
                : new String[]{"copilot"};
        for (String directory : pathValue.split(java.io.File.pathSeparator)) {
            if (directory.isBlank()) {
                continue;
            }
            for (String executableName : executableNames) {
                Path candidate = Path.of(directory, executableName);
                if (Files.isRegularFile(candidate)) {
                    try {
                        return candidate.toRealPath().toString();
                    } catch (IOException ignored) {
                        return candidate.toAbsolutePath().normalize().toString();
                    }
                }
            }
        }
        return null;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
    }

    private static void tryDelete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup; an orphaned temp file in the cache directory is benign.
        }
    }

    private static Path defaultCacheBase() {
        return Path.of(System.getProperty("user.home"), ".copilot", "runtime-cache");
    }

}
