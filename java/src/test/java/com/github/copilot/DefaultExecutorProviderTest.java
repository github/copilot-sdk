/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.CopilotClientOptions;

class DefaultExecutorProviderTest {

    @Test
    void baseProviderUsesCommonPoolWithoutOwnership() {
        Executor executor = DefaultExecutorProvider.create();

        assertSame(ForkJoinPool.commonPool(), executor);
        assertFalse(DefaultExecutorProvider.isOwned(executor));
    }

    @Test
    void clientDoesNotShutDownUserProvidedExecutor() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            try (var client = new CopilotClient(new CopilotClientOptions().setAutoStart(false).setExecutor(executor))) {
                assertNotNull(client);
            }

            assertFalse(executor.isShutdown());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void multiReleaseJarUsesOwnedVirtualThreadExecutorOnJdk25() throws Exception {
        if (Runtime.version().feature() < 25) {
            return;
        }

        Path classes = Path.of("target", "classes");
        Path baseClass = classes.resolve("com/github/copilot/DefaultExecutorProvider.class");
        Path java25Class = classes.resolve("META-INF/versions/25/com/github/copilot/DefaultExecutorProvider.class");
        assertTrue(Files.exists(baseClass), "Base DefaultExecutorProvider class must be compiled");
        assertTrue(Files.exists(java25Class), "JDK 25 build must compile the multi-release executor provider");

        Path jar = Files.createTempFile("copilot-sdk-default-executor", ".jar");
        try {
            createProviderJar(jar, baseClass, java25Class);

            try (var loader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, null)) {
                Class<?> provider = Class.forName("com.github.copilot.DefaultExecutorProvider", true, loader);
                Method create = provider.getDeclaredMethod("create");
                Method isOwned = provider.getDeclaredMethod("isOwned", Executor.class);
                create.setAccessible(true);
                isOwned.setAccessible(true);

                Executor executor = (Executor) create.invoke(null);
                try {
                    assertTrue((Boolean) isOwned.invoke(null, executor));
                    CompletableFuture<Boolean> virtualThreadUsed = new CompletableFuture<>();
                    executor.execute(() -> virtualThreadUsed.complete(isCurrentThreadVirtual()));

                    assertTrue(virtualThreadUsed.get(5, TimeUnit.SECONDS));
                } finally {
                    if (executor instanceof ExecutorService executorService) {
                        executorService.shutdownNow();
                    }
                }
            }
        } finally {
            Files.deleteIfExists(jar);
        }
    }

    private static boolean isCurrentThreadVirtual() {
        try {
            Method isVirtual = Thread.class.getMethod("isVirtual");
            return (Boolean) isVirtual.invoke(Thread.currentThread());
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static void createProviderJar(Path jar, Path baseClass, Path java25Class) throws IOException {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.putValue("Multi-Release", "true");

        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar), manifest)) {
            addClass(output, "com/github/copilot/DefaultExecutorProvider.class", baseClass);
            addClass(output, "META-INF/versions/25/com/github/copilot/DefaultExecutorProvider.class", java25Class);
        }
    }

    private static void addClass(JarOutputStream output, String entryName, Path classFile) throws IOException {
        output.putNextEntry(new JarEntry(entryName));
        Files.copy(classFile, output);
        output.closeEntry();
    }
}
