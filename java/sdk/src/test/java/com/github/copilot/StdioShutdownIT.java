/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import com.github.copilot.rpc.CopilotClientOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises owned-process shutdown through the public client API.
 */
@Timeout(value = 90, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class StdioShutdownIT {

    @Test
    void stopWaitsForCleanupAfterStdinEof() throws Exception {
        try (var fixture = new Fixture("stop")) {
            fixture.client.start().get(30, TimeUnit.SECONDS);
            fixture.client.stop().get(60, TimeUnit.SECONDS);
            fixture.assertCleanExit();
        }
    }

    @Test
    void closeWaitsForCleanupAfterStdinEof() throws Exception {
        try (var fixture = new Fixture("dispose")) {
            fixture.client.start().get(30, TimeUnit.SECONDS);
            fixture.client.close();
            fixture.assertCleanExit();
        }
    }

    @Test
    void forceStopDoesNotWaitForGracefulCleanup() throws Exception {
        try (var fixture = new Fixture("force")) {
            fixture.client.start().get(30, TimeUnit.SECONDS);
            fixture.client.forceStop().get(5, TimeUnit.SECONDS);
            fixture.assertExited();
            assertFalse(Files.exists(fixture.marker));
        }
    }

    @Test
    void stopTerminatesChildThatDoesNotExitAfterEof() throws Exception {
        try (var fixture = new Fixture("fallback")) {
            fixture.client.start().get(30, TimeUnit.SECONDS);
            long started = System.nanoTime();
            fixture.client.stop().get(60, TimeUnit.SECONDS);
            assertTrue(System.nanoTime() - started >= TimeUnit.SECONDS.toNanos(10),
                    "Stop must allow the full graceful exit timeout before terminating");
            fixture.assertCleanExit();
        }
    }

    @Test
    void closeTerminatesChildThatDoesNotExitAfterEof() throws Exception {
        try (var fixture = new Fixture("fallback")) {
            fixture.client.start().get(30, TimeUnit.SECONDS);
            long started = System.nanoTime();
            fixture.client.close();
            assertTrue(System.nanoTime() - started >= TimeUnit.SECONDS.toNanos(10),
                    "Close must allow the full graceful exit timeout before terminating");
            fixture.assertCleanExit();
        }
    }

    @Test
    void failedStartupTerminatesChild() throws Exception {
        try (var fixture = new Fixture("start-failure")) {
            assertThrows(ExecutionException.class, () -> fixture.client.start().get(30, TimeUnit.SECONDS));
            fixture.assertExited();
            assertFalse(Files.exists(fixture.marker));
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final Path directory;
        private final Path marker;
        private final Path pid;
        private final CopilotClient client;

        private Fixture(String mode) throws Exception {
            directory = Files.createDirectories(Path.of("target", "shutdown-" + UUID.randomUUID()).toAbsolutePath());
            marker = directory.resolve("cleanup.jsonl");
            pid = directory.resolve("pid");
            Path script = Path.of("..", "..", "test", "harness", "stdio-shutdown-runtime.cjs").toAbsolutePath();
            assertTrue(Files.isRegularFile(script), "Shared shutdown fixture must exist");
            client = new CopilotClient(new CopilotClientOptions().setAutoStart(false).setCliPath("node")
                    .setCliArgs(new String[]{script.toString(), marker.toString(), mode, pid.toString()}));
        }

        private void assertCleanExit() throws Exception {
            assertEquals("{\"type\":\"span\"}\n", Files.readString(marker));
            assertExited();
        }

        private void assertExited() throws Exception {
            assertTrue(Files.isRegularFile(pid), "Child must have started");
            assertFalse(
                    ProcessHandle.of(Long.parseLong(Files.readString(pid))).map(ProcessHandle::isAlive).orElse(false),
                    "Child must be reaped before shutdown returns");
        }

        @Override
        public void close() throws Exception {
            if (Files.isRegularFile(pid)) {
                var process = ProcessHandle.of(Long.parseLong(Files.readString(pid)));
                if (process.isPresent() && process.get().isAlive()) {
                    process.get().destroyForcibly();
                    process.get().onExit().get(10, TimeUnit.SECONDS);
                }
            }
            client.close();
            try (var paths = Files.walk(directory)) {
                for (var path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }
}
