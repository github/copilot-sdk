/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
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

/** Unit tests for {@link NativeRuntimeLoader}. */
class NativeRuntimeLoaderTest {

    @Test
    void isValidCacheEntryNonExistent(@TempDir Path tmp) {
        assertFalse(NativeRuntimeLoader.isValidCacheEntry(tmp.resolve("missing")));
    }

    @Test
    void isValidCacheEntryEmpty(@TempDir Path tmp) throws Exception {
        Path empty = tmp.resolve("empty");
        Files.createFile(empty);
        assertFalse(NativeRuntimeLoader.isValidCacheEntry(empty));
    }

    @Test
    void isValidCacheEntryNonEmpty(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("binary");
        Files.write(f, "content".getBytes(StandardCharsets.UTF_8));
        assertTrue(NativeRuntimeLoader.isValidCacheEntry(f));
    }

    @Test
    void isValidCacheEntryDirectory(@TempDir Path tmp) {
        assertFalse(NativeRuntimeLoader.isValidCacheEntry(tmp));
    }

    @Test
    void loadVersionMissingResourceThrows() throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new URL[0], null)) {
            NativeRuntimeLoaderException ex = assertThrows(NativeRuntimeLoaderException.class,
                    () -> NativeRuntimeLoader.loadVersion(loader));
            assertTrue(ex.getMessage().contains("Missing classpath resource"));
        }
    }

    @Test
    void loadVersionUnfilteredPlaceholderThrows(@TempDir Path tmp) throws Exception {
        URLClassLoader loader = writePropertiesAndCreateLoader(tmp, "version=${project.version}\n");
        try (loader) {
            NativeRuntimeLoaderException ex = assertThrows(NativeRuntimeLoaderException.class,
                    () -> NativeRuntimeLoader.loadVersion(loader));
            assertTrue(ex.getMessage().contains("not filtered"));
        }
    }

    @Test
    void loadVersionBlankValueThrows(@TempDir Path tmp) throws Exception {
        URLClassLoader loader = writePropertiesAndCreateLoader(tmp, "version=\n");
        try (loader) {
            NativeRuntimeLoaderException ex = assertThrows(NativeRuntimeLoaderException.class,
                    () -> NativeRuntimeLoader.loadVersion(loader));
            assertTrue(ex.getMessage().contains("missing"));
        }
    }

    @Test
    void loadVersionMissingKeyThrows(@TempDir Path tmp) throws Exception {
        URLClassLoader loader = writePropertiesAndCreateLoader(tmp, "other=value\n");
        try (loader) {
            NativeRuntimeLoaderException ex = assertThrows(NativeRuntimeLoaderException.class,
                    () -> NativeRuntimeLoader.loadVersion(loader));
            assertTrue(ex.getMessage().contains("missing"));
        }
    }

    @Test
    void loadVersionValidValueSucceeds(@TempDir Path tmp) throws Exception {
        URLClassLoader loader = writePropertiesAndCreateLoader(tmp, "version=1.2.3\n");
        try (loader) {
            assertEquals("1.2.3", NativeRuntimeLoader.loadVersion(loader));
        }
    }

    @Test
    void extractionCreatesFileInCacheDir(@TempDir Path tmpHome) throws Exception {
        String version = "1.2.3-test";
        String classifier = "linux-x64";
        byte[] content = "fake-runtime-node-content".getBytes(StandardCharsets.UTF_8);

        Path cached = NativeRuntimeLoader.extractToCache(buildInMemoryUrl(content), version, classifier, tmpHome);

        assertTrue(Files.isRegularFile(cached));
        assertArrayEquals(content, Files.readAllBytes(cached));
        assertEquals(tmpHome.resolve(".copilot/runtime-cache/" + version + "/" + classifier + "/runtime.node"), cached);
    }

    @Test
    void extractionCacheHitSkipsReExtraction(@TempDir Path tmpHome) throws Exception {
        String version = "1.2.3-test";
        String classifier = "linux-x64";
        byte[] content = "fake-runtime-node-content".getBytes(StandardCharsets.UTF_8);

        Path cached = NativeRuntimeLoader.extractToCache(buildInMemoryUrl(content), version, classifier, tmpHome);
        long modifiedFirst = Files.getLastModifiedTime(cached).toMillis();

        Thread.sleep(50);
        Path cached2 = NativeRuntimeLoader.extractToCache(buildInMemoryUrl(content), version, classifier, tmpHome);
        long modifiedSecond = Files.getLastModifiedTime(cached2).toMillis();

        assertEquals(cached, cached2);
        assertEquals(modifiedFirst, modifiedSecond, "File was re-written on cache hit");
    }

    @Test
    void extractionEmptyResourceThrows(@TempDir Path tmpHome) {
        NativeRuntimeLoaderException ex = assertThrows(NativeRuntimeLoaderException.class,
                () -> NativeRuntimeLoader.extractToCache(buildInMemoryUrl(new byte[0]), "1.0.0", "linux-x64", tmpHome));
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    void extractionNoTempFileLeftAfterSuccess(@TempDir Path tmpHome) throws Exception {
        String version = "1.2.3-test";
        String classifier = "linux-x64";
        byte[] content = "fake-runtime-node-content".getBytes(StandardCharsets.UTF_8);

        NativeRuntimeLoader.extractToCache(buildInMemoryUrl(content), version, classifier, tmpHome);

        Path cacheDir = tmpHome.resolve(".copilot/runtime-cache/" + version + "/" + classifier);
        try (var files = Files.list(cacheDir)) {
            long tmpCount = files.filter(p -> p.getFileName().toString().contains(".tmp-")).count();
            assertEquals(0, tmpCount, "Temp files left after extraction");
        }
    }

    @Test
    void concurrentExtractionBothSucceed(@TempDir Path tmpHome) throws Exception {
        String version = "1.2.3-concurrent";
        String classifier = "linux-x64";
        byte[] content = "fake-runtime-node-concurrent".getBytes(StandardCharsets.UTF_8);

        int threadCount = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        List<Future<Path>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return NativeRuntimeLoader.extractToCache(buildInMemoryUrl(content), version, classifier, tmpHome);
            }));
        }

        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        for (Future<Path> f : futures) {
            Path result = f.get();
            assertNotNull(result);
            assertTrue(Files.isRegularFile(result));
            assertArrayEquals(content, Files.readAllBytes(result), "Concurrent extraction produced corrupt content");
        }
    }

    @Test
    void runtimeOverrideWinsOverClasspathAndCliSibling(@TempDir Path tmpHome) throws Exception {
        Path explicitRuntime = tmpHome.resolve("explicit-runtime.node");
        Files.write(explicitRuntime, "runtime".getBytes(StandardCharsets.UTF_8));
        Path bundledCli = tmpHome.resolve("copilot");
        Files.write(bundledCli, "cli".getBytes(StandardCharsets.UTF_8));
        Path siblingRuntime = tmpHome.resolve("runtime.node");
        Files.write(siblingRuntime, "sibling".getBytes(StandardCharsets.UTF_8));

        try (URLClassLoader emptyLoader = new URLClassLoader(new URL[0], null)) {
            Path resolved = NativeRuntimeLoader.resolve(explicitRuntime.toString(), bundledCli.toString(), emptyLoader,
                    tmpHome, "linux-x64");
            assertEquals(explicitRuntime, resolved);
        }
    }

    @Test
    void resolveFallsBackToRuntimeNodeSibling(@TempDir Path tmpHome) throws Exception {
        Path bundledCli = tmpHome.resolve("copilot");
        Files.write(bundledCli, "cli".getBytes(StandardCharsets.UTF_8));
        Path siblingRuntime = tmpHome.resolve("runtime.node");
        Files.write(siblingRuntime, "sibling".getBytes(StandardCharsets.UTF_8));

        try (URLClassLoader emptyLoader = new URLClassLoader(new URL[0], null)) {
            Path resolved = NativeRuntimeLoader.resolve(null, bundledCli.toString(), emptyLoader, tmpHome, "linux-x64");
            assertEquals(siblingRuntime, resolved);
        }
    }

    @Test
    void missingClasspathResourceThrows(@TempDir Path tmpHome) throws Exception {
        Path bundledCli = tmpHome.resolve("copilot");
        Files.write(bundledCli, "cli".getBytes(StandardCharsets.UTF_8));

        try (URLClassLoader emptyLoader = new URLClassLoader(new URL[0], null)) {
            NativeRuntimeLoaderException ex = assertThrows(NativeRuntimeLoaderException.class,
                    () -> NativeRuntimeLoader.resolve(null, bundledCli.toString(), emptyLoader, tmpHome, "win32-x64"));
            assertTrue(ex.getMessage().contains("Could not locate native/win32-x64/runtime.node"));
        }
    }

    private static URLClassLoader writePropertiesAndCreateLoader(Path dir, String content) throws IOException {
        Files.writeString(dir.resolve("copilot-runtime.properties"), content, StandardCharsets.UTF_8);
        return new URLClassLoader(new URL[]{dir.toUri().toURL()}, null);
    }

    /** Builds a {@code URL} that serves {@code data} as its content. */
    private static URL buildInMemoryUrl(byte[] data) throws IOException {
        return new URL("mem", "", 0, "/runtime.node", new URLStreamHandler() {
            @Override
            protected java.net.URLConnection openConnection(URL u) {
                return new java.net.URLConnection(u) {
                    @Override
                    public void connect() {
                    }

                    @Override
                    public InputStream getInputStream() {
                        return new ByteArrayInputStream(data);
                    }
                };
            }
        });
    }
}
