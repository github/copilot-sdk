/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class AhpTestClientTest {
    @Test
    @Timeout(120)
    void readsAndReapsFramesWhileCommonPoolWorkersAreOccupied(@TempDir Path temporary) throws Exception {
        Path driver = temporary.resolve("echo.mjs");
        Files.writeString(driver, """
                import { writeFileSync } from "node:fs";
                import { createInterface } from "node:readline";
                process.stdout.write(JSON.stringify({ ready: true }) + "\\n");
                for await (const line of createInterface({ input: process.stdin })) {
                    const command = JSON.parse(line);
                    if (command.op === "eof") break;
                    if (command.op === "hold") {
                        writeFileSync(command.marker, "admitted");
                        await new Promise(resolve => process.stdin.once("end", resolve));
                        break;
                    }
                    process.stdout.write(JSON.stringify(command.op === "error"
                        ? { error: "controlled driver failure" }
                        : { result: command }) + "\\n");
                }
                process.stdin.destroy();
                """);
        Path log = temporary.resolve("probe.log");
        Path java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java");
        String sdkRoot = System.getProperty("copilot.sdk.dir");
        assertNotNull(sdkRoot, "Set copilot.sdk.dir to the SDK root when running outside Maven");
        // Pool saturation belongs to this child JVM, never to parallel JUnit tests.
        Process probe = new ProcessBuilder(java.toString(), "-Djava.util.concurrent.ForkJoinPool.common.parallelism=2",
                "-cp", probeClasspath(), Probe.class.getName(), sdkRoot, driver.toString()).redirectErrorStream(true)
                .redirectOutput(log.toFile()).start();
        try {
            int exit = probe.waitFor();
            String output = Files.readString(log);
            assertEquals(0, exit, output);
            assertTrue(output.contains("AHP_FRAMES_READ_WITH_POOL_OCCUPIED"), output);
        } catch (InterruptedException error) {
            try {
                error.addSuppressed(new IOException("AHP probe output:\n" + Files.readString(log)));
            } catch (IOException diagnostics) {
                error.addSuppressed(diagnostics);
            }
            throw error;
        } finally {
            if (probe.isAlive()) {
                probe.destroyForcibly();
                probe.waitFor();
            }
        }
    }

    private static String probeClasspath() throws Exception {
        var entries = new LinkedHashSet<String>();
        for (String property : List.of("java.class.path", "surefire.test.class.path", "jdk.module.path")) {
            for (String entry : System.getProperty(property, "").split(Pattern.quote(File.pathSeparator))) {
                if (!entry.isEmpty()) {
                    entries.add(entry);
                }
            }
        }
        // Surefire moves SDK dependencies off the classpath when it patches the SDK
        // module.
        for (Class<?> type : List.of(Probe.class, JsonRpcClient.class,
                com.fasterxml.jackson.databind.ObjectMapper.class, com.fasterxml.jackson.core.JsonFactory.class,
                com.fasterxml.jackson.annotation.JsonProperty.class,
                com.fasterxml.jackson.datatype.jsr310.JavaTimeModule.class)) {
            var source = type.getProtectionDomain().getCodeSource();
            assertNotNull(source, "Missing probe class location: " + type.getName());
            entries.add(Path.of(source.getLocation().toURI()).toString());
        }
        return String.join(File.pathSeparator, entries);
    }

    public static final class Probe {
        public static void main(String[] args) throws Exception {
            var occupied = new CountDownLatch(2);
            var release = new CountDownLatch(1);
            for (int i = 0; i < 2; i++) {
                ForkJoinPool.commonPool().execute(() -> {
                    occupied.countDown();
                    try {
                        release.await();
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(error);
                    }
                });
            }
            occupied.await();
            try {
                try (var client = new AhpTestClient(Path.of(args[0]), args[1])) {
                    assertEquals("first", client.request(Map.of("value", "first")).path("value").asText());
                    assertEquals("second", client.request(Map.of("value", "second")).path("value").asText());
                    IOException error = assertThrows(IOException.class, () -> client.request(Map.of("op", "error")));
                    assertEquals("controlled driver failure", error.getMessage());
                    ExecutionException eof = assertThrows(ExecutionException.class,
                            () -> client.request(Map.of("op", "eof")));
                    assertInstanceOf(UncheckedIOException.class, eof.getCause());
                }
                assertTrue(Thread.getAllStackTraces().keySet().stream()
                        .noneMatch(thread -> thread.isAlive() && thread.getName().equals("ahp-test-client-output")));
                Path marker = Path.of(args[1]).getParent().resolve("read-admitted");
                Thread requester = null;
                try {
                    try (var changes = marker.getFileSystem().newWatchService();
                            var client = new AhpTestClient(Path.of(args[0]), args[1])) {
                        marker.getParent().register(changes, StandardWatchEventKinds.ENTRY_CREATE);
                        var pending = new FutureTask<>(
                                () -> client.request(Map.of("op", "hold", "marker", marker.toString())));
                        requester = new Thread(pending, "ahp-interrupted-request");
                        requester.start();
                        while (!Files.exists(marker)) {
                            changes.take().reset();
                        }
                        requester.interrupt();
                        ExecutionException interrupted = assertThrows(ExecutionException.class, pending::get);
                        assertInstanceOf(InterruptedException.class, interrupted.getCause());
                    }
                } finally {
                    if (requester != null) {
                        requester.interrupt();
                        requester.join();
                    }
                }
                assertTrue(Thread.getAllStackTraces().keySet().stream()
                        .noneMatch(thread -> thread.isAlive() && thread.getName().equals("ahp-test-client-output")));
                System.out.println("AHP_FRAMES_READ_WITH_POOL_OCCUPIED");
            } finally {
                release.countDown();
            }
        }
    }
}
