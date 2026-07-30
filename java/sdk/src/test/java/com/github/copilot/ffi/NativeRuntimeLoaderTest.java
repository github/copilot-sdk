/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NativeRuntimeLoaderTest {

    private static final String TEST_CLASSIFIER = "linux-x64";
    private static final String TEST_VERSION = "1.2.3-test";
    private static final byte[] FAKE_BINARY_CONTENT = "fake runtime.node binary content".getBytes();

    // -------------------------------------------------------------------------
    // Version properties resource reading
    // -------------------------------------------------------------------------

    @Test
    void readVersionReturnsVersionFromPropertiesResource(@TempDir Path tempDir) throws Exception {
        ClassLoader loader = classLoaderWithVersionResource(tempDir, "1.0.5-preview");
        assertEquals("1.0.5-preview", NativeRuntimeLoader.readVersion(loader));
    }

    @Test
    void readVersionThrowsWhenResourceMissing() {
        ClassLoader emptyLoader = new URLClassLoader(new URL[0], null);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> NativeRuntimeLoader.readVersion(emptyLoader));
        assertTrue(ex.getMessage().contains(NativeRuntimeLoader.VERSION_RESOURCE));
    }

    @Test
    void readVersionThrowsWhenVersionPropertyIsBlank(@TempDir Path tempDir) throws Exception {
        ClassLoader loader = classLoaderWithVersionResource(tempDir, "  ");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> NativeRuntimeLoader.readVersion(loader));
        assertTrue(ex.getMessage().contains("version"));
    }

    // -------------------------------------------------------------------------
    // Source 1: COPILOT_CLI_PATH as explicit runtime override
    // -------------------------------------------------------------------------

    @Test
    void resolveFromExplicitPathReturnsPathWhenFileIsValid(@TempDir Path tempDir) throws Exception {
        Path runtimeNode = tempDir.resolve(NativeRuntimeLoader.RUNTIME_FILENAME);
        Files.write(runtimeNode, FAKE_BINARY_CONTENT);

        Path result = NativeRuntimeLoader.resolveFromExplicitPath(runtimeNode.toString());

        assertEquals(runtimeNode, result);
    }

    @Test
    void resolveFromExplicitPathThrowsWhenFileDoesNotExist(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("nonexistent.node");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> NativeRuntimeLoader.resolveFromExplicitPath(missing.toString()));
        assertTrue(ex.getMessage().contains(NativeRuntimeLoader.COPILOT_CLI_PATH_ENV),
                "Error must mention the env variable name: " + ex.getMessage());
    }

    @Test
    void resolveFromExplicitPathThrowsWhenFileIsEmpty(@TempDir Path tempDir) throws Exception {
        Path empty = tempDir.resolve(NativeRuntimeLoader.RUNTIME_FILENAME);
        Files.createFile(empty); // zero bytes

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> NativeRuntimeLoader.resolveFromExplicitPath(empty.toString()));
        assertTrue(ex.getMessage().contains(NativeRuntimeLoader.COPILOT_CLI_PATH_ENV),
                "Error must mention the env variable name: " + ex.getMessage());
    }

    @Test
    void explicitOverrideTakesPriorityOverClasspathExtraction(@TempDir Path tempDir) throws Exception {
        // Source 1: runtime.node directly specified via COPILOT_CLI_PATH
        Path runtimeNode = tempDir.resolve(NativeRuntimeLoader.RUNTIME_FILENAME);
        Files.write(runtimeNode, FAKE_BINARY_CONTENT);

        // Source 2 is also available (should be ignored)
        Path cacheBase = tempDir.resolve("cache");
        ClassLoader loader = classLoaderWithRuntimeResource(tempDir, TEST_CLASSIFIER);

        Path result = NativeRuntimeLoader.resolve(runtimeNode.toString(), cacheBase, loader, TEST_CLASSIFIER,
                TEST_VERSION);

        assertEquals(runtimeNode, result, "Source 1 (COPILOT_CLI_PATH) must take priority over classpath extraction");
    }

    @Test
    void explicitOverrideThrowsImmediatelyWhenPathIsInvalid(@TempDir Path tempDir) throws Exception {
        // Source 2 is available, but source 1 is invalid — must throw, not silently
        // fall through
        Path missing = tempDir.resolve("not-a-runtime.node");
        Path cacheBase = tempDir.resolve("cache");
        ClassLoader loader = classLoaderWithRuntimeResource(tempDir, TEST_CLASSIFIER);

        assertThrows(IllegalStateException.class, () -> NativeRuntimeLoader.resolve(missing.toString(), cacheBase,
                loader, TEST_CLASSIFIER, TEST_VERSION));
    }

    // -------------------------------------------------------------------------
    // Source 2: classpath extraction to cache
    // -------------------------------------------------------------------------

    @Test
    void extractToCacheCopiesResourceToVersionedCacheDirectory(@TempDir Path tempDir) throws Exception {
        Path cacheBase = tempDir.resolve("cache");
        ClassLoader loader = classLoaderWithRuntimeResource(tempDir, TEST_CLASSIFIER);

        Path result = NativeRuntimeLoader.extractToCache(cacheBase, loader, TEST_CLASSIFIER, TEST_VERSION);

        Path expected = cacheBase.resolve(TEST_VERSION).resolve(TEST_CLASSIFIER)
                .resolve(NativeRuntimeLoader.RUNTIME_FILENAME);
        assertEquals(expected, result);
        assertTrue(Files.isRegularFile(result));
        assertTrue(Files.size(result) > 0);
    }

    @Test
    void extractToCacheReturnsCachedFileOnSecondCall(@TempDir Path tempDir) throws Exception {
        Path cacheBase = tempDir.resolve("cache");
        ClassLoader loader = classLoaderWithRuntimeResource(tempDir, TEST_CLASSIFIER);

        Path first = NativeRuntimeLoader.extractToCache(cacheBase, loader, TEST_CLASSIFIER, TEST_VERSION);
        long modifiedAfterFirstExtraction = Files.getLastModifiedTime(first).toMillis();

        // Small delay so modification time would differ if the file were rewritten
        Thread.sleep(50);

        Path second = NativeRuntimeLoader.extractToCache(cacheBase, loader, TEST_CLASSIFIER, TEST_VERSION);
        long modifiedAfterSecondCall = Files.getLastModifiedTime(second).toMillis();

        assertEquals(first, second);
        assertEquals(modifiedAfterFirstExtraction, modifiedAfterSecondCall,
                "Cached file must not be overwritten on cache hit");
    }

    @Test
    void extractToCacheThrowsWhenClasspathResourceMissing(@TempDir Path tempDir) {
        Path cacheBase = tempDir.resolve("cache");
        ClassLoader emptyLoader = new URLClassLoader(new URL[0], null);

        assertThrows(IOException.class,
                () -> NativeRuntimeLoader.extractToCache(cacheBase, emptyLoader, TEST_CLASSIFIER, TEST_VERSION));
    }

    @Test
    void extractedBinaryContentsMatchClasspathResource(@TempDir Path tempDir) throws Exception {
        Path cacheBase = tempDir.resolve("cache");
        ClassLoader loader = classLoaderWithRuntimeResource(tempDir, TEST_CLASSIFIER);

        Path result = NativeRuntimeLoader.extractToCache(cacheBase, loader, TEST_CLASSIFIER, TEST_VERSION);

        byte[] extracted = Files.readAllBytes(result);
        assertBytesEqual(FAKE_BINARY_CONTENT, extracted);
    }

    @Test
    void extractToCacheFiltersClasspathByClassifier(@TempDir Path tempDir) throws Exception {
        Path cacheBase = tempDir.resolve("cache");
        ClassLoader loader = classLoaderWithRuntimeResource(tempDir, TEST_CLASSIFIER);

        Path result = NativeRuntimeLoader.extractToCache(cacheBase, loader, TEST_CLASSIFIER, TEST_VERSION);

        assertTrue(result.toString().contains(TEST_CLASSIFIER), "Cache path must include the classifier: " + result);
    }

    // -------------------------------------------------------------------------
    // Source 3: bundled-CLI sibling
    // -------------------------------------------------------------------------

    @Test
    void bundledCliSiblingIsUsedWhenClasspathResourceAbsent(@TempDir Path tempDir) throws Exception {
        Path bundledCliDir = tempDir.resolve("bundled-cli");
        Files.createDirectories(bundledCliDir);
        Path runtimeNode = bundledCliDir.resolve(NativeRuntimeLoader.RUNTIME_FILENAME);
        Files.write(runtimeNode, FAKE_BINARY_CONTENT);

        Path cacheBase = tempDir.resolve("cache");
        ClassLoader emptyLoader = new URLClassLoader(new URL[0], null); // no classpath resource

        Path result = NativeRuntimeLoader.resolve(null, cacheBase, emptyLoader, TEST_CLASSIFIER, TEST_VERSION,
                bundledCliDir);

        assertEquals(runtimeNode, result,
                "Source 3 (bundled-CLI sibling) must be used when classpath resource is absent");
    }

    @Test
    void classpathResourceWinsOverBundledCliSibling(@TempDir Path tempDir) throws Exception {
        // Source 3: bundled CLI dir with runtime.node (should NOT win)
        Path bundledCliDir = tempDir.resolve("bundled-cli");
        Files.createDirectories(bundledCliDir);
        Files.write(bundledCliDir.resolve(NativeRuntimeLoader.RUNTIME_FILENAME), "bundled".getBytes());

        // Source 2: classpath resource (should win)
        Path cacheBase = tempDir.resolve("cache");
        ClassLoader loader = classLoaderWithRuntimeResource(tempDir, TEST_CLASSIFIER);

        Path result = NativeRuntimeLoader.resolve(null, cacheBase, loader, TEST_CLASSIFIER, TEST_VERSION,
                bundledCliDir);

        Path expectedFromClasspath = cacheBase.resolve(TEST_VERSION).resolve(TEST_CLASSIFIER)
                .resolve(NativeRuntimeLoader.RUNTIME_FILENAME);
        assertEquals(expectedFromClasspath, result,
                "Source 2 (classpath) must win over source 3 (bundled-CLI sibling)");
        assertNotEquals(bundledCliDir.resolve(NativeRuntimeLoader.RUNTIME_FILENAME), result);
    }

    @Test
    void bundledCliSiblingIsIgnoredWhenRuntimeNodeMissing(@TempDir Path tempDir) {
        Path bundledCliDir = tempDir.resolve("bundled-cli-no-runtime");
        // bundledCliDir doesn't even exist — no runtime.node present

        Path cacheBase = tempDir.resolve("cache");
        ClassLoader emptyLoader = new URLClassLoader(new URL[0], null);

        // Both source 2 and source 3 absent: must throw (the classpath error)
        IOException ex = assertThrows(IOException.class, () -> NativeRuntimeLoader.resolve(null, cacheBase, emptyLoader,
                TEST_CLASSIFIER, TEST_VERSION, bundledCliDir));
        assertTrue(ex.getMessage().contains("classpath"), "Error should mention classpath: " + ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // Atomic publication test seam
    // -------------------------------------------------------------------------

    @Test
    void defaultPublisherMovesSourceToTarget(@TempDir Path tempDir) throws Exception {
        Path temp = Files.createTempFile(tempDir, "runtime-tmp-", ".node");
        Files.write(temp, FAKE_BINARY_CONTENT);
        Path target = tempDir.resolve(NativeRuntimeLoader.RUNTIME_FILENAME);

        NativeRuntimeLoader.DEFAULT_PUBLISHER.publish(temp, target);

        assertTrue(Files.isRegularFile(target), "Target must exist after publication");
        assertTrue(Files.size(target) > 0, "Target must be non-empty");
        assertFalse(Files.exists(temp), "Source temp file must be absent after atomic move");
    }

    @Test
    void extractionCleansUpTempFileWhenPublicationFails(@TempDir Path tempDir) throws Exception {
        Path cacheBase = tempDir.resolve("cache");
        ClassLoader loader = classLoaderWithRuntimeResource(tempDir, TEST_CLASSIFIER);

        // Capture the temp path so we can verify it was deleted
        Path[] capturedTemp = {null};
        NativeRuntimeLoader.AtomicPublisher failingPublisher = (temp, cached) -> {
            capturedTemp[0] = temp;
            throw new AtomicMoveNotSupportedException(temp.toString(), cached.toString(),
                    "filesystem does not support atomic moves — test");
        };

        assertThrows(AtomicMoveNotSupportedException.class, () -> NativeRuntimeLoader.extractToCache(cacheBase, loader,
                TEST_CLASSIFIER, TEST_VERSION, failingPublisher));

        assertNotNull(capturedTemp[0], "Publisher must have been invoked");
        assertFalse(Files.exists(capturedTemp[0]), "Temp file must be deleted after failed publication");
    }

    @Test
    void extractionCleansUpTempFileWhenPublisherThrowsIllegalStateException(@TempDir Path tempDir) throws Exception {
        Path cacheBase = tempDir.resolve("cache");
        ClassLoader loader = classLoaderWithRuntimeResource(tempDir, TEST_CLASSIFIER);

        Path[] capturedTemp = {null};
        NativeRuntimeLoader.AtomicPublisher unsupportedPublisher = (temp, cached) -> {
            capturedTemp[0] = temp;
            // Simulate the wrapping that DEFAULT_PUBLISHER performs for
            // AtomicMoveNotSupportedException
            throw new IllegalStateException("Filesystem does not support atomic moves; cannot safely publish "
                    + NativeRuntimeLoader.RUNTIME_FILENAME + " to " + cached);
        };

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> NativeRuntimeLoader
                .extractToCache(cacheBase, loader, TEST_CLASSIFIER, TEST_VERSION, unsupportedPublisher));

        assertTrue(ex.getMessage().contains("atomic moves"),
                "Error message should describe the atomic-move failure: " + ex.getMessage());
        assertNotNull(capturedTemp[0], "Publisher must have been invoked");
        assertFalse(Files.exists(capturedTemp[0]), "Temp file must be deleted after failed atomic publication");
    }

    // -------------------------------------------------------------------------
    // Concurrent extraction safety
    // -------------------------------------------------------------------------

    @Test
    void concurrentExtractionByMultipleThreadsBothSucceed(@TempDir Path tempDir) throws Exception {
        Path cacheBase = tempDir.resolve("cache");
        ClassLoader loader = classLoaderWithRuntimeResource(tempDir, TEST_CLASSIFIER);
        int threadCount = 8;
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        List<Future<Path>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(pool.submit(() -> {
                startGate.await();
                return NativeRuntimeLoader.extractToCache(cacheBase, loader, TEST_CLASSIFIER, TEST_VERSION);
            }));
        }

        startGate.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        Path expected = cacheBase.resolve(TEST_VERSION).resolve(TEST_CLASSIFIER)
                .resolve(NativeRuntimeLoader.RUNTIME_FILENAME);
        for (Future<Path> future : futures) {
            Path result = future.get();
            assertEquals(expected, result);
            assertTrue(Files.isRegularFile(result));
            assertTrue(Files.size(result) > 0);
        }
    }

    // -------------------------------------------------------------------------
    // resolve() -- full three-source resolution chain
    // -------------------------------------------------------------------------

    @Test
    void resolveWithNullCliEnvExtractsFromClasspath(@TempDir Path tempDir) throws Exception {
        Path cacheBase = tempDir.resolve("cache");
        ClassLoader loader = classLoaderWithRuntimeResource(tempDir, TEST_CLASSIFIER);

        Path result = NativeRuntimeLoader.resolve(null, cacheBase, loader, TEST_CLASSIFIER, TEST_VERSION);

        assertNotNull(result);
        assertTrue(Files.isRegularFile(result));
        assertTrue(Files.size(result) > 0);
    }

    @Test
    void resolveThrowsWhenNoSourceIsAvailable(@TempDir Path tempDir) {
        Path cacheBase = tempDir.resolve("cache");
        ClassLoader emptyLoader = new URLClassLoader(new URL[0], null);

        // No CLI env, no classpath resource, no bundled-CLI dir → throw
        assertThrows(IOException.class,
                () -> NativeRuntimeLoader.resolve(null, cacheBase, emptyLoader, TEST_CLASSIFIER, TEST_VERSION));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static ClassLoader classLoaderWithVersionResource(Path tempDir, String version) throws IOException {
        Path propsFile = tempDir.resolve(NativeRuntimeLoader.VERSION_RESOURCE);
        Files.writeString(propsFile, "version=" + version + "\n");
        return new URLClassLoader(new URL[]{tempDir.toUri().toURL()}, null);
    }

    private static ClassLoader classLoaderWithRuntimeResource(Path tempDir, String classifier) throws IOException {
        Path resourceDir = tempDir.resolve("native").resolve(classifier);
        Files.createDirectories(resourceDir);
        Files.write(resourceDir.resolve(NativeRuntimeLoader.RUNTIME_FILENAME), FAKE_BINARY_CONTENT);
        return new URLClassLoader(new URL[]{tempDir.toUri().toURL()}, null);
    }

    private static void assertBytesEqual(byte[] expected, byte[] actual) {
        assertEquals(expected.length, actual.length, "Array lengths differ");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], "Byte differs at index " + i);
        }
    }
}
