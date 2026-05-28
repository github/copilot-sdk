/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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

class InternalExecutorProviderTest {

    @Test
    void baseProviderUsesCommonPoolWithoutOwnership() {
        Executor executor = new InternalExecutorProvider(null).get();

        assertSame(ForkJoinPool.commonPool(), executor);
        assertFalse(new InternalExecutorProvider(executor).canBeShutdown());
        assertFalse(Modifier.isPublic(InternalExecutorProvider.class.getModifiers()));
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
        Path baseClass = classes.resolve("com/github/copilot/InternalExecutorProvider.class");
        Path java25Class = classes.resolve("META-INF/versions/25/com/github/copilot/InternalExecutorProvider.class");
        assertTrue(Files.exists(baseClass), "Base InternalExecutorProvider class must be compiled");
        assertTrue(Files.exists(java25Class), "JDK 25 build must compile the multi-release executor provider");

        Path jar = Files.createTempFile("copilot-sdk-internal-executor", ".jar");
        try {
            createProviderJar(jar, baseClass, java25Class);

            try (var loader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, null)) {
                Class<?> provider = Class.forName("com.github.copilot.InternalExecutorProvider", true, loader);
                var constructor = provider.getDeclaredConstructor(Executor.class);
                Method get = provider.getDeclaredMethod("get");
                Method canBeShutdown = provider.getDeclaredMethod("canBeShutdown");
                constructor.setAccessible(true);
                get.setAccessible(true);
                canBeShutdown.setAccessible(true);

                Object providerInstance = constructor.newInstance((Executor) null);
                Executor executor = (Executor) get.invoke(providerInstance);
                try {
                    assertTrue((Boolean) canBeShutdown.invoke(providerInstance));
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

    @Test
    void clientCloseShutsDownOwnedDefaultExecutorOnJdk25() throws Exception {
        if (Runtime.version().feature() < 25) {
            return;
        }

        Path classes = Path.of("target", "classes");
        Path jar = Files.createTempFile("copilot-sdk-client-internal-executor", ".jar");
        try {
            createClassesJar(jar, classes);

            try (var loader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, null)) {
                Class<?> clientClass = Class.forName("com.github.copilot.CopilotClient", true, loader);
                AutoCloseable client = (AutoCloseable) clientClass.getConstructor().newInstance();
                Field executorField = clientClass.getDeclaredField("executor");
                Field executorCanBeShutdownField = clientClass.getDeclaredField("executorCanBeShutdown");
                executorField.setAccessible(true);
                executorCanBeShutdownField.setAccessible(true);
                ExecutorService ownedExecutor = (ExecutorService) executorField.get(client);

                assertNotNull(ownedExecutor);
                assertTrue((Boolean) executorCanBeShutdownField.get(client));
                assertFalse(ownedExecutor.isShutdown());

                client.close();

                assertTrue(ownedExecutor.isShutdown());
                assertTrue(ownedExecutor.awaitTermination(5, TimeUnit.SECONDS));
                assertTrue(ownedExecutor.isTerminated());
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
            addClass(output, "com/github/copilot/InternalExecutorProvider.class", baseClass);
            addClass(output, "META-INF/versions/25/com/github/copilot/InternalExecutorProvider.class", java25Class);
        }
    }

    private static void createClassesJar(Path jar, Path classes) throws IOException {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.putValue("Multi-Release", "true");

        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar), manifest);
                var files = Files.walk(classes)) {
            var iterator = files.iterator();
            while (iterator.hasNext()) {
                Path file = iterator.next();
                if (!Files.isRegularFile(file)) {
                    continue;
                }

                String entryName = classes.relativize(file).toString().replace('\\', '/');
                if ("META-INF/MANIFEST.MF".equals(entryName)) {
                    continue;
                }

                addClass(output, entryName, file);
            }
        }
    }

    private static void addClass(JarOutputStream output, String entryName, Path classFile) throws IOException {
        output.putNextEntry(new JarEntry(entryName));
        Files.copy(classFile, output);
        output.closeEntry();
    }
}
