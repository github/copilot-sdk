/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Locates, extracts, and caches the {@code runtime.node} native binary.
 *
 * <p>
 * Resolution order:
 * <ol>
 * <li>The {@code COPILOT_RUNTIME_PATH} environment variable (if set, treated as
 * the resolved {@code runtime.node} path and returned directly).</li>
 * <li>Classpath resource {@code native/<classifier>/runtime.node} extracted to
 * {@code ~/.copilot/runtime-cache/<version>/<classifier>/runtime.node}.</li>
 * <li>A {@code runtime.node} file alongside the bundled CLI binary.</li>
 * </ol>
 *
 * <p>
 * The version is read from the {@code copilot-runtime.properties} resource that
 * is written by Maven resource filtering at build time. A missing or blank
 * version is a configuration error and causes {@link #resolve()} to throw.
 *
 * <p>
 * Extraction is atomic: the binary is written to a unique sibling temp file and
 * renamed into place with {@link StandardCopyOption#ATOMIC_MOVE}. If another
 * process wins the race, the winner's file is accepted after a
 * regular/non-empty sanity check. No file locking is used. The execute
 * permission bit is NOT set on the extracted file; JNA's {@code dlopen} does
 * not require it.
 */
public final class NativeRuntimeLoader {

    private static final Logger LOG = Logger.getLogger(NativeRuntimeLoader.class.getName());
    private static final String PROPERTIES_RESOURCE = "copilot-runtime.properties";
    private static final String BINARY_NAME = "runtime.node";
    private static final String RUNTIME_PATH_ENV = "COPILOT_RUNTIME_PATH";
    private static final String CLI_PATH_ENV = "COPILOT_CLI_PATH";

    private NativeRuntimeLoader() {
    }

    /**
     * Resolves the filesystem path to the {@code runtime.node} native binary,
     * extracting and caching it from the classpath if necessary.
     *
     * @return the absolute path to an existing, non-empty {@code runtime.node} file
     * @throws NativeRuntimeLoaderException
     *             if the binary cannot be resolved, extracted, or cached
     */
    public static Path resolve() throws NativeRuntimeLoaderException {
        return resolve(System.getenv(RUNTIME_PATH_ENV), System.getenv(CLI_PATH_ENV),
                NativeRuntimeLoader.class.getClassLoader(), Paths.get(System.getProperty("user.home")),
                PlatformDetector.detectClassifier());
    }

    static Path resolve(String runtimePathOverride, String bundledCliPath, ClassLoader classLoader, Path userHome,
            String classifier) throws NativeRuntimeLoaderException {
        // 1. Explicit runtime.node override
        if (runtimePathOverride != null && !runtimePathOverride.isBlank()) {
            return Paths.get(runtimePathOverride);
        }

        // 2. Extract from classpath resource
        String resourcePath = "native/" + classifier + "/" + BINARY_NAME;

        URL resourceUrl = classLoader.getResource(resourcePath);
        if (resourceUrl != null) {
            String version = loadVersion(classLoader);
            return extractToCache(resourceUrl, version, classifier, userHome);
        }

        // 3. Alongside bundled CLI (fall-through when no classpath resource)
        if (bundledCliPath != null && !bundledCliPath.isBlank()) {
            Path sibling = Paths.get(bundledCliPath).getParent();
            if (sibling != null) {
                Path candidate = sibling.resolve(BINARY_NAME);
                if (isValidCacheEntry(candidate)) {
                    return candidate;
                }
            }
        }

        throw new NativeRuntimeLoaderException("Could not locate native/" + classifier
                + "/runtime.node on the classpath. " + "Ensure a platform-specific native JAR is on the classpath.");
    }

    /**
     * Loads the artifact version from the {@code copilot-runtime.properties}
     * resource on the classpath.
     *
     * @return the non-blank version string
     * @throws NativeRuntimeLoaderException
     *             if the resource is missing or the version value is blank
     */
    static String loadVersion() throws NativeRuntimeLoaderException {
        return loadVersion(NativeRuntimeLoader.class.getClassLoader());
    }

    static String loadVersion(ClassLoader classLoader) throws NativeRuntimeLoaderException {
        InputStream in = classLoader.getResourceAsStream(PROPERTIES_RESOURCE);
        if (in == null) {
            throw new NativeRuntimeLoaderException("Missing classpath resource: " + PROPERTIES_RESOURCE
                    + ". Ensure the SDK JAR was built with Maven resource filtering enabled.");
        }
        Properties props = new Properties();
        try (in) {
            props.load(in);
        } catch (IOException e) {
            throw new NativeRuntimeLoaderException("Failed to read " + PROPERTIES_RESOURCE + ": " + e.getMessage(), e);
        }
        String version = props.getProperty("version");
        if (version == null || version.isBlank() || version.startsWith("${")) {
            throw new NativeRuntimeLoaderException(
                    "Version property in " + PROPERTIES_RESOURCE + " is missing or was not filtered by Maven. "
                            + "Rebuild the project with Maven to apply resource filtering.");
        }
        return version.trim();
    }

    static Path extractToCache(URL resourceUrl, String version, String classifier, Path userHome)
            throws NativeRuntimeLoaderException {
        Path cacheDir = userHome.resolve(Paths.get(".copilot", "runtime-cache", version, classifier));
        Path cached = cacheDir.resolve(BINARY_NAME);

        // 1. Cache hit: regular, non-empty file
        if (isValidCacheEntry(cached)) {
            LOG.fine("Native binary cache hit: " + cached);
            return cached;
        }

        // 2. Create cache directory
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            throw new NativeRuntimeLoaderException("Failed to create native binary cache directory: " + cacheDir, e);
        }

        // 3. Create unique temp file in same directory (ATOMIC_MOVE requires same
        // filesystem)
        Path temp = cacheDir.resolve(BINARY_NAME + ".tmp-" + UUID.randomUUID());
        try {
            extractToTemp(resourceUrl, temp);
            atomicPublish(temp, cached);
        } finally {
            // 6. Delete caller's temp file in finally block (no-op if already moved or
            // missing)
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // best-effort cleanup
            }
        }

        return cached;
    }

    private static void extractToTemp(URL resourceUrl, Path temp) throws NativeRuntimeLoaderException {
        try (InputStream in = resourceUrl.openStream();
                FileChannel fc = FileChannel.open(temp, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            // Copy via InputStream → temp path (piping through a buffer)
            byte[] buf = new byte[65536];
            long total = 0;
            int n;
            while ((n = in.read(buf)) >= 0) {
                int written = 0;
                while (written < n) {
                    written += fc.write(java.nio.ByteBuffer.wrap(buf, written, n - written));
                }
                total += n;
            }
            if (total == 0) {
                throw new NativeRuntimeLoaderException(
                        "Classpath resource native/…/runtime.node is empty; the native JAR may be corrupt.");
            }
            // 4. Flush and force to disk before atomic rename
            fc.force(true);
        } catch (IOException e) {
            throw new NativeRuntimeLoaderException("Failed to write native binary to temp file: " + temp, e);
        }
    }

    private static void atomicPublish(Path temp, Path cached) throws NativeRuntimeLoaderException {
        // 5. Atomic rename
        try {
            Files.move(temp, cached, StandardCopyOption.ATOMIC_MOVE);
            LOG.fine("Native binary extracted to cache: " + cached);
        } catch (AtomicMoveNotSupportedException e) {
            throw new NativeRuntimeLoaderException(
                    "Filesystem does not support atomic moves; cannot safely publish native binary to " + cached
                            + ". Use a local filesystem for the home directory.",
                    e);
        } catch (IOException e) {
            // Another process may have published first — accept if valid
            if (isValidCacheEntry(cached)) {
                LOG.fine("Native binary race: another process published first, accepting winner: " + cached);
                return;
            }
            throw new NativeRuntimeLoaderException("Failed to atomically publish native binary to " + cached, e);
        }
    }

    /**
     * Returns {@code true} if {@code path} is a regular, non-empty file.
     *
     * @param path
     *            the path to check
     * @return {@code true} if the cache entry is valid
     */
    static boolean isValidCacheEntry(Path path) {
        try {
            return Files.isRegularFile(path) && Files.size(path) > 0;
        } catch (IOException e) {
            return false;
        }
    }
}
