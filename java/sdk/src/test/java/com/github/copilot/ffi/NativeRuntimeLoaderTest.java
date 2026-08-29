/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
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

    private static final String TEST_CLASSIFIER = PlatformDetector.detectClassifier();
    private static final String OTHER_CLASSIFIER = TEST_CLASSIFIER.equals("darwin-arm64")
            ? "linux-x64"
            : "darwin-arm64";
    private static final String TEST_CLI_FILENAME = TEST_CLASSIFIER.startsWith("win32")
            ? NativeRuntimeLoader.CLI_FILENAME_WINDOWS
            : NativeRuntimeLoader.CLI_FILENAME;
    private static final String TEST_VERSION = "1.2.3-test";
    private static final String TEST_NATIVE_VERSION = "0.0.1-test";
    private static final byte[] FAKE_BINARY_CONTENT = "fake runtime.node binary content".getBytes();
    private static final byte[] FAKE_CLI_CONTENT = "fake copilot CLI content".getBytes();
    private static final byte[] OTHER_BINARY_CONTENT = "other runtime.node binary content".getBytes();
    private static final byte[] OTHER_CLI_CONTENT = "other copilot CLI content".getBytes();

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
    void resolveFromCliPathReturnsPrebuildsPathWhenFlatRuntimeNodeIsMissing(@TempDir Path tempDir) throws Exception {
        Path fakeCliPath = tempDir.resolve("copilot");
        Files.createFile(fakeCliPath);
        Path prebuiltDir = tempDir.resolve("prebuilds").resolve(PlatformDetector.detectClassifier());
        Files.createDirectories(prebuiltDir);
        Path runtimeNode = prebuiltDir.resolve(NativeRuntimeLoader.RUNTIME_FILENAME);
        Files.write(runtimeNode, FAKE_BINARY_CONTENT);

        Path result = NativeRuntimeLoader.resolveFromCliPath(fakeCliPath.toString());

        assertEquals(runtimeNode, result);
    }

    @Test
    void resolveFromCliPathPrefersFlatRuntimeNodeOverPrebuildsPath(@TempDir Path tempDir) throws Exception {
        Path fakeCliPath = tempDir.resolve("copilot");
        Files.createFile(fakeCliPath);
        Path flatRuntimeNode = tempDir.resolve(NativeRuntimeLoader.RUNTIME_FILENAME);
        Files.write(flatRuntimeNode, FAKE_BINARY_CONTENT);
        Path prebuiltDir = tempDir.resolve("prebuilds").resolve(PlatformDetector.detectClassifier());
        Files.createDirectories(prebuiltDir);
        Files.write(prebuiltDir.resolve(NativeRuntimeLoader.RUNTIME_FILENAME), OTHER_BINARY_CONTENT);

        Path result = NativeRuntimeLoader.resolveFromCliPath(fakeCliPath.toString());

        assertEquals(flatRuntimeNode, result);
    }

    @Test
    void resolveEntrypointUsesConfiguredCliWhenRuntimeIsInPrebuilds(@TempDir Path tempDir) throws Exception {
        Path cli = Files.writeString(tempDir.resolve("copilot"), "fake cli");
        Path runtimeDir = tempDir.resolve("prebuilds").resolve(PlatformDetector.detectClassifier());
        Files.createDirectories(runtimeDir);
        Path runtime = Files.write(runtimeDir.resolve(NativeRuntimeLoader.RUNTIME_FILENAME), FAKE_BINARY_CONTENT);

        assertEquals(cli, NativeRuntimeLoader.resolveEntrypoint(cli.toString(), runtime));
    }

    @Test
    void resolveFromCliPathReturnsAbsolutePathForRelativeCliPath() throws Exception {
        Path workingDirectory = Path.of("").toAbsolutePath();
        Path fakeCliDir = Files.createTempDirectory(Path.of("target").toAbsolutePath(), "relative-cli-test-");
        Path fakeCliPath = fakeCliDir.resolve("copilot");
        Files.createFile(fakeCliPath);
        Path runtimeNode = fakeCliDir.resolve(NativeRuntimeLoader.RUNTIME_FILENAME);
        Files.write(runtimeNode, FAKE_BINARY_CONTENT);

        Path relativeCliPath = workingDirectory.relativize(fakeCliPath);

        assertEquals(runtimeNode, NativeRuntimeLoader.resolveFromCliPath(relativeCliPath.toString()));
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

        // Source 2 is also available (should be ignored)
        Path cacheBase = tempDir.resolve("cache");
        ClassLoader loader = classLoaderWithRuntimeResource(tempDir, TEST_CLASSIFIER);

        Path result = NativeRuntimeLoader.resolve(fakeCliPath.toString(), cacheBase, loader, TEST_CLASSIFIER,
                TEST_VERSION);

        assertEquals(runtimeNode, result, "Source 1 (COPILOT_CLI_PATH) must take priority over classpath extraction");
    }

    // -------------------------------------------------------------------------
    // Source 2: classpath extraction to cache
    // -------------------------------------------------------------------------

    @Test
    void extractToCacheCopiesResourceToVersionedCacheDirectory(@TempDir Path tempDir) throws Exception {
        Path cacheBase = tempDir.resolve("cache");
        ClassLoader loader = classLoaderWithRuntimeResource(tempDir, TEST_CLASSIFIER);

        Path result = NativeRuntimeLoader.extractToCache(cacheBase, loader, TEST_CLASSIFIER, TEST_VERSION);

        Path expected = cacheBase.resolve(TEST_VERSION).resolve(TEST_NATIVE_VERSION).resolve(TEST_CLASSIFIER)
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
    void changedNativeVersionDoesNotReuseCachedArtifactsForSameSdkVersion(@TempDir Path tempDir) throws Exception {
        Path cacheBase = tempDir.resolve("cache");
        ClassLoader firstLoader = classLoaderWithNativeArtifacts(tempDir.resolve("native-v1"), TEST_CLASSIFIER, "1.0.0",
                FAKE_BINARY_CONTENT, FAKE_CLI_CONTENT);
        ClassLoader secondLoader = classLoaderWithNativeArtifacts(tempDir.resolve("native-v2"), TEST_CLASSIFIER,
                "1.1.0", OTHER_BINARY_CONTENT, OTHER_CLI_CONTENT);

        Path firstRuntime = NativeRuntimeLoader.extractToCache(cacheBase, firstLoader, TEST_CLASSIFIER, TEST_VERSION);
        Path secondRuntime = NativeRuntimeLoader.extractToCache(cacheBase, secondLoader, TEST_CLASSIFIER, TEST_VERSION);

        assertNotEquals(firstRuntime, secondRuntime, "Different native versions must use different cache entries");
        assertBytesEqual(FAKE_BINARY_CONTENT, Files.readAllBytes(firstRuntime));
        assertBytesEqual(FAKE_CLI_CONTENT, Files.readAllBytes(firstRuntime.getParent().resolve(TEST_CLI_FILENAME)));
        assertBytesEqual(OTHER_BINARY_CONTENT, Files.readAllBytes(secondRuntime));
        assertBytesEqual(OTHER_CLI_CONTENT, Files.readAllBytes(secondRuntime.getParent().resolve(TEST_CLI_FILENAME)));
    }

    @Test
    void extractToCacheThrowsWhenClasspathResourceMissing(@TempDir Path tempDir) {
        Path cacheBase = tempDir.resolve("cache");
        ClassLoader emptyLoader = new URLClassLoader(new URL[0], null);

        assertThrows(IOException.class,
                () -> NativeRuntimeLoader.extractToCache(cacheBase, emptyLoader, TEST_CLASSIFIER, TEST_VERSION));
    }

    @Test
    void extractToCacheThrowsWhenNativeMetadataMissing(@TempDir Path tempDir) throws Exception {
        Path resourceDir = tempDir.resolve("native").resolve(TEST_CLASSIFIER);
        Files.createDirectories(resourceDir);
        Files.write(resourceDir.resolve(NativeRuntimeLoader.RUNTIME_FILENAME), FAKE_BINARY_CONTENT);
        ClassLoader loader = new URLClassLoader(new URL[]{tempDir.toUri().toURL()}, null);

        IOException ex = assertThrows(IOException.class, () -> NativeRuntimeLoader
                .extractToCache(tempDir.resolve("cache"), loader, TEST_CLASSIFIER, TEST_VERSION));

        assertTrue(ex.getMessage().contains("platform.properties"));
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
        writeRuntimeResource(tempDir, TEST_CLASSIFIER, FAKE_BINARY_CONTENT);
        writeRuntimeResource(tempDir, OTHER_CLASSIFIER, OTHER_BINARY_CONTENT);
        ClassLoader loader = new URLClassLoader(new URL[]{tempDir.toUri().toURL()}, null);

        Path result = NativeRuntimeLoader.extractToCache(cacheBase, loader, TEST_CLASSIFIER, TEST_VERSION);

        assertTrue(result.toString().contains(TEST_CLASSIFIER), "Cache path must include the classifier: " + result);
        assertBytesEqual(FAKE_BINARY_CONTENT, Files.readAllBytes(result));
    }

    @Test
    void extractToCacheRepairsInvalidCacheEntry(@TempDir Path tempDir) throws Exception {
        Path cacheBase = tempDir.resolve("cache");
        Path cached = cacheBase.resolve(TEST_VERSION).resolve(TEST_NATIVE_VERSION).resolve(TEST_CLASSIFIER)
                .resolve(NativeRuntimeLoader.RUNTIME_FILENAME);
        Files.createDirectories(cached.getParent());
        Files.createFile(cached);
        ClassLoader loader = classLoaderWithRuntimeResource(tempDir, TEST_CLASSIFIER);

        Path result = NativeRuntimeLoader.extractToCache(cacheBase, loader, TEST_CLASSIFIER, TEST_VERSION);

        assertEquals(cached, result);
        assertBytesEqual(FAKE_BINARY_CONTENT, Files.readAllBytes(result));
    }

    @Test
    void nonExecutableCachedCliIsNotAcceptedAsValid(@TempDir Path tempDir) throws Exception {
        assumeTrue(!TEST_CLASSIFIER.startsWith("win32"));
        assumeTrue(Files.getFileStore(tempDir).supportsFileAttributeView("posix"));
        Path cacheBase = tempDir.resolve("cache");
        Path cacheDir = cacheBase.resolve(TEST_VERSION).resolve(TEST_NATIVE_VERSION).resolve(TEST_CLASSIFIER);
        Files.createDirectories(cacheDir);
        Files.write(cacheDir.resolve(NativeRuntimeLoader.RUNTIME_FILENAME), FAKE_BINARY_CONTENT);
        Path cachedCli = Files.write(cacheDir.resolve(TEST_CLI_FILENAME), FAKE_CLI_CONTENT);
        Files.setPosixFilePermissions(cachedCli, PosixFilePermissions.fromString("rw-------"));
        ClassLoader loader = classLoaderWithRuntimeAndCliResources(tempDir, TEST_CLASSIFIER);

        NativeRuntimeLoader.extractToCache(cacheBase, loader, TEST_CLASSIFIER, TEST_VERSION);

        assertTrue(Files.isExecutable(cachedCli), "A non-executable cached CLI must be repaired or replaced");
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

        Path expectedFromClasspath = cacheBase.resolve(TEST_VERSION).resolve(TEST_NATIVE_VERSION)
                .resolve(TEST_CLASSIFIER).resolve(NativeRuntimeLoader.RUNTIME_FILENAME);
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
    void cliIsExecutableBeforeAtomicPublication(@TempDir Path tempDir) throws Exception {
        assumeTrue(!TEST_CLASSIFIER.startsWith("win32"));
        assumeTrue(Files.getFileStore(tempDir).supportsFileAttributeView("posix"));
        Path cacheBase = tempDir.resolve("cache");
        ClassLoader loader = classLoaderWithRuntimeAndCliResources(tempDir, TEST_CLASSIFIER);
        NativeRuntimeLoader.AtomicPublisher publisher = (temp, cached) -> {
            if (cached.getFileName().toString().equals(TEST_CLI_FILENAME)) {
                assertTrue(Files.isExecutable(temp), "CLI temp file must be executable before atomic publication");
            }
            Files.move(temp, cached, StandardCopyOption.REPLACE_EXISTING);
        };

        NativeRuntimeLoader.extractToCache(cacheBase, loader, TEST_CLASSIFIER, TEST_VERSION, publisher);
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

        Path expected = cacheBase.resolve(TEST_VERSION).resolve(TEST_NATIVE_VERSION).resolve(TEST_CLASSIFIER)
                .resolve(NativeRuntimeLoader.RUNTIME_FILENAME);
        for (Future<Path> future : futures) {
            Path result = future.get();
            assertEquals(expected, result);
            assertTrue(Files.isRegularFile(result));
            assertTrue(Files.size(result) > 0);
        }
        try (var files = Files.list(expected.getParent())) {
            assertEquals(List.of(expected), files.toList(), "Concurrent extraction must clean up temporary files");
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

    @Test
    void resolveFallsBackToRuntimeAlongsideBundledCli(@TempDir Path tempDir) throws Exception {
        Path cacheBase = tempDir.resolve("cache");
        ClassLoader emptyLoader = new URLClassLoader(new URL[0], null);
        Path bundledCli = tempDir.resolve("copilot");
        Files.createFile(bundledCli);
        Path runtimeNode = tempDir.resolve(NativeRuntimeLoader.RUNTIME_FILENAME);
        Files.write(runtimeNode, FAKE_BINARY_CONTENT);

        Path result = NativeRuntimeLoader.resolve(null, bundledCli.toString(), cacheBase, emptyLoader, TEST_CLASSIFIER,
                TEST_VERSION);

        assertEquals(runtimeNode, result);
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
        writeRuntimeResource(tempDir, classifier, FAKE_BINARY_CONTENT);
        return new URLClassLoader(new URL[]{tempDir.toUri().toURL()}, null);
    }

    private static ClassLoader classLoaderWithRuntimeAndCliResources(Path tempDir, String classifier)
            throws IOException {
        writeRuntimeResource(tempDir, classifier, FAKE_BINARY_CONTENT);
        Path resourceDir = tempDir.resolve("native").resolve(classifier);
        Files.write(resourceDir.resolve(TEST_CLI_FILENAME), FAKE_CLI_CONTENT);
        return new URLClassLoader(new URL[]{tempDir.toUri().toURL()}, null);
    }

    private static ClassLoader classLoaderWithNativeArtifacts(Path tempDir, String classifier, String nativeVersion,
            byte[] runtimeContent, byte[] cliContent) throws IOException {
        writeRuntimeResource(tempDir, classifier, runtimeContent);
        Path resourceDir = tempDir.resolve("native").resolve(classifier);
        Files.write(resourceDir.resolve(TEST_CLI_FILENAME), cliContent);
        Files.writeString(resourceDir.resolve("platform.properties"),
                "classifier=" + classifier + "\nversion=" + nativeVersion + "\n");
        return new URLClassLoader(new URL[]{tempDir.toUri().toURL()}, null);
    }

    private static void writeRuntimeResource(Path tempDir, String classifier, byte[] content) throws IOException {
        Path resourceDir = tempDir.resolve("native").resolve(classifier);
        Files.createDirectories(resourceDir);
        Files.write(resourceDir.resolve(NativeRuntimeLoader.RUNTIME_FILENAME), content);
        Files.writeString(resourceDir.resolve("platform.properties"),
                "classifier=" + classifier + "\nversion=" + TEST_NATIVE_VERSION + "\n");
    }

    private static void assertBytesEqual(byte[] expected, byte[] actual) {
        assertEquals(expected.length, actual.length, "Array lengths differ");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], "Byte differs at index " + i);
        }
    }
}
