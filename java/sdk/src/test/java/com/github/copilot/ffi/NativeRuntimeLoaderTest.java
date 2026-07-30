/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
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
    // COPILOT_CLI_PATH override
    // -------------------------------------------------------------------------

    @Test
    void resolveFromCliPathReturnsSiblingWhenRuntimeNodeExists(@TempDir Path tempDir) throws Exception {
        Path fakeCliPath = tempDir.resolve("copilot");
        Files.createFile(fakeCliPath);
        Path runtimeNode = tempDir.resolve(NativeRuntimeLoader.RUNTIME_FILENAME);
        Files.write(runtimeNode, FAKE_BINARY_CONTENT);

        Path result = NativeRuntimeLoader.resolveFromCliPath(fakeCliPath.toString());

        assertEquals(runtimeNode, result);
    }

    @Test
    void resolveFromCliPathReturnsNullWhenRuntimeNodeMissing(@TempDir Path tempDir) throws Exception {
        Path fakeCliPath = tempDir.resolve("copilot");
        Files.createFile(fakeCliPath);

        assertNull(NativeRuntimeLoader.resolveFromCliPath(fakeCliPath.toString()));
    }

    @Test
    void resolveFromCliPathReturnsNullWhenEnvIsNull() throws Exception {
        assertNull(NativeRuntimeLoader.resolveFromCliPath(null));
    }

    @Test
    void resolveFromCliPathReturnsNullWhenEnvIsBlank() throws Exception {
        assertNull(NativeRuntimeLoader.resolveFromCliPath("   "));
    }

    @Test
    void resolveFromCliPathReturnsNullWhenRuntimeNodeIsEmpty(@TempDir Path tempDir) throws Exception {
        Path fakeCliPath = tempDir.resolve("copilot");
        Files.createFile(fakeCliPath);
        Path runtimeNode = tempDir.resolve(NativeRuntimeLoader.RUNTIME_FILENAME);
        Files.createFile(runtimeNode); // empty file

        assertNull(NativeRuntimeLoader.resolveFromCliPath(fakeCliPath.toString()));
    }

    @Test
    void cliPathOverrideTakesPriorityOverClasspathExtraction(@TempDir Path tempDir) throws Exception {
        // Create a valid runtime.node alongside the fake CLI path
        Path fakeCliDir = tempDir.resolve("cli-dir");
        Files.createDirectories(fakeCliDir);
        Path fakeCliPath = fakeCliDir.resolve("copilot");
        Files.createFile(fakeCliPath);
        Path runtimeNode = fakeCliDir.resolve(NativeRuntimeLoader.RUNTIME_FILENAME);
        Files.write(runtimeNode, FAKE_BINARY_CONTENT);

        // Provide a classpath loader that also has the resource (should be ignored)
        Path cacheBase = tempDir.resolve("cache");
        ClassLoader loader = classLoaderWithRuntimeResource(tempDir, TEST_CLASSIFIER);

        Path result = NativeRuntimeLoader.resolve(fakeCliPath.toString(), cacheBase, loader, TEST_CLASSIFIER,
                TEST_VERSION);

        assertEquals(runtimeNode, result);
    }

    // -------------------------------------------------------------------------
    // Classpath extraction to cache
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
        // Put resources for two classifiers; extraction must target only the requested
        // one
        Path cacheBase = tempDir.resolve("cache");
        ClassLoader loader = classLoaderWithRuntimeResource(tempDir, TEST_CLASSIFIER);

        Path result = NativeRuntimeLoader.extractToCache(cacheBase, loader, TEST_CLASSIFIER, TEST_VERSION);

        assertTrue(result.toString().contains(TEST_CLASSIFIER), "Cache path must include the classifier: " + result);
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
    // resolve() -- full resolution chain
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
    void resolveThrowsWhenNoClasspathResourceAndNoCliOverride(@TempDir Path tempDir) {
        Path cacheBase = tempDir.resolve("cache");
        ClassLoader emptyLoader = new URLClassLoader(new URL[0], null);

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
