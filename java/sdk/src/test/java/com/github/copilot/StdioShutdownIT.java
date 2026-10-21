/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import com.github.copilot.rpc.CopilotClientOptions;

import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    void failedShutdownStillWaitsForCleanupAfterStdinEof() throws Exception {
        try (var fixture = new Fixture("shutdown-error"); var watcher = FileSystems.getDefault().newWatchService()) {
            fixture.client.start().get(30, TimeUnit.SECONDS);
            fixture.directory.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
            Path eof = fixture.marker.resolveSibling(fixture.marker.getFileName() + ".eof");
            Path release = fixture.marker.resolveSibling(fixture.marker.getFileName() + ".release");
            var stopping = fixture.client.stop();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (!Files.isRegularFile(eof)) {
                var key = watcher.poll(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                assertNotNull(key, "Failed shutdown must still deliver stdin EOF before terminating the runtime");
                key.pollEvents();
                assertTrue(key.reset(), "The owned cleanup directory must remain available");
            }
            assertFalse(stopping.isDone(), "Shutdown must not return before EOF-driven cleanup completes");
            Files.writeString(release, "");
            stopping.get(60, TimeUnit.SECONDS);
            fixture.assertCleanExit();
        }
    }

    @Test
    void stopPreservesDetachedServiceAfterLauncherAndRuntimeExit() throws Exception {
        try (var fixture = new Fixture("stop", true, true)) {
            fixture.client.start().get(30, TimeUnit.SECONDS);
            fixture.assertServiceResponsive();
            fixture.client.stop().get(60, TimeUnit.SECONDS);
            fixture.assertCleanExit();
            fixture.assertServiceResponsive();
        }
    }

    @Test
    void closePreservesDetachedServiceAfterLauncherAndRuntimeExit() throws Exception {
        try (var fixture = new Fixture("dispose", true, true)) {
            fixture.client.start().get(30, TimeUnit.SECONDS);
            fixture.assertServiceResponsive();
            fixture.client.close();
            fixture.assertCleanExit();
            fixture.assertServiceResponsive();
        }
    }

    @Test
    void stopPreservesDetachedTcpServiceWithoutPeerEof() throws Exception {
        try (var fixture = new Fixture("stop", true, true, false)) {
            fixture.client.start().get(30, TimeUnit.SECONDS);
            fixture.assertServiceResponsive();
            fixture.client.stop().get(60, TimeUnit.SECONDS);
            fixture.assertCleanExit();
            fixture.assertServiceResponsive();
        }
    }

    @Test
    void closePreservesDetachedTcpServiceWithoutPeerEof() throws Exception {
        try (var fixture = new Fixture("dispose", true, true, false)) {
            fixture.client.start().get(30, TimeUnit.SECONDS);
            fixture.assertServiceResponsive();
            fixture.client.close();
            fixture.assertCleanExit();
            fixture.assertServiceResponsive();
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
    void stopTerminatesLauncherAndItsChildAfterEof() throws Exception {
        try (var fixture = new Fixture("fallback", true)) {
            fixture.client.start().get(30, TimeUnit.SECONDS);
            fixture.client.stop().get(60, TimeUnit.SECONDS);
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
        private final Path launcherPid;
        private final Path servicePid;
        private final Path servicePort;
        private final CopilotClient client;

        private Fixture(String mode) throws Exception {
            this(mode, false);
        }

        private Fixture(String mode, boolean useLauncher) throws Exception {
            this(mode, useLauncher, false);
        }

        private Fixture(String mode, boolean useLauncher, boolean detachedService) throws Exception {
            this(mode, useLauncher, detachedService, true);
        }

        private Fixture(String mode, boolean useLauncher, boolean detachedService, boolean useStdio) throws Exception {
            directory = Files.createDirectories(Path.of("target", "shutdown-" + UUID.randomUUID()).toAbsolutePath());
            marker = directory.resolve("cleanup.jsonl");
            pid = directory.resolve("pid");
            launcherPid = useLauncher ? directory.resolve("launcher-pid") : null;
            servicePid = detachedService ? directory.resolve("service-pid") : null;
            servicePort = detachedService ? directory.resolve("service-port") : null;
            Path script = Path.of("..", "..", "test", "harness", "stdio-shutdown-runtime.cjs").toAbsolutePath();
            assertTrue(Files.isRegularFile(script), "Shared shutdown fixture must exist");
            String nodePath = TestUtil.findExecutableInPath("node");
            assertNotNull(nodePath, "Node.js was not found in PATH");
            var args = new ArrayList<String>();
            if (useLauncher) {
                Path launcher = directory.resolve("launcher.cjs");
                Files.writeString(launcher, """
                        const { spawn } = require("node:child_process");
                        require("node:fs").writeFileSync(process.argv[2], String(process.pid));
                        const args = process.argv.slice(3);
                        const fail = error => { console.error(error); process.exit(1); };
                        const launch = () => {
                            if (args[0] === "--in-process-runtime") {
                                args.shift();
                                const script = args.shift();
                                process.argv = [process.execPath, script, ...args];
                                require(script);
                                return;
                            }
                            const child = spawn(process.execPath, args, { stdio: "inherit" });
                            child.on("error", fail);
                            child.on("exit", code => process.exit(code ?? 1));
                        };
                        if (args[0] === "--detached-service") {
                            args.shift();
                            const service = spawn(process.execPath, ["-e", `
                                const fs = require("node:fs");
                                const server = require("node:net").createServer(socket => {
                                    socket.write(process.argv[3] + ":");
                                    socket.pipe(socket);
                                });
                                server.listen(0, "127.0.0.1", () => {
                                    fs.writeFileSync(process.argv[1], String(process.pid));
                                    fs.writeFileSync(process.argv[2], String(server.address().port));
                                    process.send("ready");
                                    process.disconnect();
                                });
                            `, args.shift(), args.shift(), args.shift()],
                                { detached: true, stdio: ["ignore", "ignore", "ignore", "ipc"] });
                            service.on("error", fail);
                            service.once("message", message => {
                                if (message !== "ready") return fail(new Error("Unexpected service handshake"));
                                service.unref();
                                launch();
                            });
                        } else {
                            launch();
                        }
                        """);
                args.add(launcher.toString());
                args.add(launcherPid.toString());
                if (detachedService) {
                    args.addAll(List.of("--detached-service", servicePid.toString(), servicePort.toString(),
                            directory.getFileName().toString()));
                }
            }
            if (useLauncher && !useStdio) {
                args.add("--in-process-runtime");
            }
            args.addAll(List.of(script.toString(), marker.toString(), mode, pid.toString()));
            if (!useStdio) {
                args.add("--fixture-tcp");
            }
            client = new CopilotClient(new CopilotClientOptions().setAutoStart(false).setCliPath(nodePath)
                    .setCliArgs(args.toArray(String[]::new)).setUseStdio(useStdio));
        }

        private void assertCleanExit() throws Exception {
            assertEquals("{\"type\":\"span\"}\n", Files.readString(marker));
            assertExited();
        }

        private void assertServiceResponsive() throws Exception {
            assertTrue(Files.isRegularFile(servicePort), "The detached service must acknowledge readiness");
            try (var socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", Integer.parseInt(Files.readString(servicePort))),
                        5000);
                socket.setSoTimeout(5000);
                String nonce = UUID.randomUUID().toString();
                byte[] expected = (directory.getFileName() + ":" + nonce).getBytes(StandardCharsets.UTF_8);
                socket.getOutputStream().write(nonce.getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().flush();
                assertArrayEquals(expected, socket.getInputStream().readNBytes(expected.length),
                        "The detached service must still serve requests, not merely retain a PID");
            }
        }

        private void assertExited() throws Exception {
            assertExited(pid);
            if (launcherPid != null) {
                assertExited(launcherPid);
            }
        }

        private static void assertExited(Path pid) throws Exception {
            assertTrue(Files.isRegularFile(pid), "Child must have started");
            assertFalse(
                    ProcessHandle.of(Long.parseLong(Files.readString(pid))).map(ProcessHandle::isAlive).orElse(false),
                    "Child must be reaped before shutdown returns");
        }

        @Override
        public void close() throws Exception {
            var ownedPids = new ArrayList<Path>();
            if (servicePid != null) {
                ownedPids.add(servicePid);
            }
            ownedPids.add(pid);
            if (launcherPid != null) {
                ownedPids.add(launcherPid);
            }
            for (var ownedPid : ownedPids) {
                if (Files.isRegularFile(ownedPid)) {
                    var process = ProcessHandle.of(Long.parseLong(Files.readString(ownedPid)));
                    if (process.isPresent() && process.get().isAlive()) {
                        process.get().destroyForcibly();
                        process.get().onExit().get(10, TimeUnit.SECONDS);
                    }
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
