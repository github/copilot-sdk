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
 * <li>{@code COPILOT_CLI_PATH} environment variable — if set, checks for
 * {@code runtime.node} in the same directory as the specified CLI binary.</li>
 * <li>Classpath resource {@code native/<classifier>/runtime.node} — extracted
 * atomically to
 * {@code ~/.copilot/runtime-cache/<version>/<classifier>/runtime.node}.</li>
 * </ol>
 */
public final class NativeRuntimeLoader {

    static final String RUNTIME_FILENAME = "runtime.node";
    static final String COPILOT_CLI_PATH_ENV = "COPILOT_CLI_PATH";
    static final String VERSION_RESOURCE = "copilot-runtime.properties";

    private NativeRuntimeLoader() {
    }

    /**
     * Resolves the filesystem path to the {@code runtime.node} binary.
     *
     * <p>
     * Follows the resolution order documented on this class. The returned path is
     * guaranteed to refer to a regular, non-empty file at the time of return.
     *
     * @return absolute path to the {@code runtime.node} binary
     * @throws IOException
     *             if the binary cannot be located or extracted
     * @throws IllegalStateException
     *             if required resources are missing or extraction fails
     */
    public static Path resolve() throws IOException {
        ClassLoader loader = NativeRuntimeLoader.class.getClassLoader();
        String classifier = PlatformDetector.detectClassifier();
        String version = readVersion(loader);
        Path cacheBase = defaultCacheBase();
        return resolve(System.getenv(COPILOT_CLI_PATH_ENV), cacheBase, loader, classifier, version);
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
     *
     * @param cliPathEnv
     *            value of the {@code COPILOT_CLI_PATH} environment variable, or
     *            {@code null}
     * @param cacheBase
     *            base directory for the extraction cache
     * @param loader
     *            class loader used to locate classpath resources
     * @param classifier
     *            platform classifier (e.g. {@code linux-x64})
     * @param version
     *            SDK version used as the cache key
     * @return path to the resolved {@code runtime.node} binary
     * @throws IOException
     *             if extraction or file I/O fails
     * @throws IllegalStateException
     *             if required resources are missing or extraction fails
     */
    static Path resolve(String cliPathEnv, Path cacheBase, ClassLoader loader, String classifier, String version)
            throws IOException {
        Path cliOverride = resolveFromCliPath(cliPathEnv);
        if (cliOverride != null) {
            return cliOverride;
        }
        return extractToCache(cacheBase, loader, classifier, version);
    }

    /**
     * Checks whether a {@code runtime.node} file exists alongside the binary
     * referred to by {@code cliPathStr}.
     *
     * @param cliPathStr
     *            value of the {@code COPILOT_CLI_PATH} environment variable
     * @return path to the sibling {@code runtime.node} if it is a regular non-empty
     *         file, or {@code null} if the override does not apply
     * @throws IOException
     *             if file-size probing fails
     */
    static Path resolveFromCliPath(String cliPathStr) throws IOException {
        if (cliPathStr == null || cliPathStr.isBlank()) {
            return null;
        }
        Path cliPath = Path.of(cliPathStr);
        Path parent = cliPath.getParent();
        Path candidate = parent != null ? parent.resolve(RUNTIME_FILENAME) : Path.of(RUNTIME_FILENAME);
        if (Files.isRegularFile(candidate) && Files.size(candidate) > 0) {
            return candidate;
        }
        return null;
    }

    /**
     * Extracts the classpath resource {@code native/<classifier>/runtime.node} to
     * the versioned cache directory, using an atomic publish sequence to prevent
     * readers from observing a partially-written file.
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
            publishAtomically(temp, cached);
            temp = null; // transfer ownership; do not delete in finally
        } finally {
            if (temp != null) {
                tryDelete(temp);
            }
        }

        return cached;
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

    private static void publishAtomically(Path temp, Path cached) throws IOException {
        try {
            Files.move(temp, cached, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            throw new IllegalStateException(
                    "Filesystem does not support atomic moves; cannot safely publish runtime.node to " + cached, ex);
        } catch (java.nio.file.FileAlreadyExistsException ex) {
            // Another process won the race — accept the winner if it is a valid file.
            if (isValidCachedFile(cached)) {
                return;
            }
            throw new IllegalStateException(
                    "Concurrent extraction race: target already exists but is not a valid file: " + cached, ex);
        }
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
