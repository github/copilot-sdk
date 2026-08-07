package com.github.copilot.spike;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Spike — validate the InProcess callback flow against the real runtime.node.
 *
 * <p>This program isolates and instruments the exact code path that hangs in the
 * SDK's E2E tests:
 * <ol>
 *   <li>Load {@code runtime.node} via JNA</li>
 *   <li>Call {@code copilot_runtime_host_start} with a bounded timeout</li>
 *   <li>If successful, call {@code copilot_runtime_connection_open} with a
 *       diagnostic callback</li>
 *   <li>Clean up: {@code connection_close} → {@code host_shutdown}</li>
 * </ol>
 *
 * <p>Usage:
 * <pre>
 * # Build:
 * mvn clean package -q
 *
 * # Run (runtime.node path from the native-staging directory):
 * java -jar target/real-runtime-callback-spike-0.1.0.jar \
 *     target/native-staging/linux-x64/native/linux-x64/runtime.node
 *
 * # Or supply the path to any runtime.node on disk:
 * java -jar target/real-runtime-callback-spike-0.1.0.jar /path/to/runtime.node
 * </pre>
 */
public class RealRuntimeSpikeMain {

    private static final Logger LOG = Logger.getLogger(RealRuntimeSpikeMain.class.getName());

    /** Timeout for host_start (the Rust side has a 30 s READY_TIMEOUT internally). */
    private static final int HOST_START_TIMEOUT_SECONDS = 60;

    public static void main(String[] args) throws Exception {
        configureLogging();

        // --- Resolve runtime.node path (the shared library loaded via JNA) ---
        String runtimePath;
        if (args.length > 0) {
            runtimePath = args[0];
        } else {
            // Default: look in the native-staging directory populated by the POM
            runtimePath = "target/native-staging/linux-x64/native/linux-x64/runtime.node";
        }

        // --- Resolve copilot CLI path (the executable spawned as a child by host_start) ---
        // runtime.node is a shared library (cdylib), NOT an executable.
        // host_start's argv must point to the copilot CLI binary, which gets
        // --embedded-host and connects back via napi-oop.
        String copilotCliPath;
        if (args.length > 1) {
            copilotCliPath = args[1];
        } else {
            // Default: look for the copilot binary next to runtime.node or in
            // the npm package location
            Path runtimeDir = Path.of(runtimePath).toAbsolutePath().normalize().getParent();
            Path candidate = runtimeDir.resolve("copilot");
            if (!Files.exists(candidate)) {
                // Try the npm package layout: nodejs/node_modules/@github/copilot-linux-x64/copilot
                // relative to the monorepo root
                Path repoRoot = Path.of("../..").toAbsolutePath().normalize();
                candidate = repoRoot.resolve("nodejs/node_modules/@github/copilot-linux-x64/copilot");
            }
            copilotCliPath = candidate.toString();
        }

        Path runtimeFile = Path.of(runtimePath).toAbsolutePath().normalize();
        Path copilotCliFile = Path.of(copilotCliPath).toAbsolutePath().normalize();
        LOG.info("=== Spike: Validate Callback Flow with Real runtime.node ===");
        LOG.info("runtime.node path (JNA library): " + runtimeFile);
        LOG.info("  File exists: " + Files.exists(runtimeFile));
        if (Files.exists(runtimeFile)) {
            LOG.info("  File size: " + Files.size(runtimeFile) + " bytes");
        }
        LOG.info("copilot CLI path (host_start argv[0]): " + copilotCliFile);
        LOG.info("  File exists: " + Files.exists(copilotCliFile));
        if (Files.exists(copilotCliFile)) {
            LOG.info("  File size: " + Files.size(copilotCliFile) + " bytes");
            LOG.info("  Executable: " + Files.isExecutable(copilotCliFile));
        }
        LOG.info("Main thread: " + Thread.currentThread().getName()
                + " (id=" + Thread.currentThread().threadId() + ")");
        LOG.info("java.version: " + System.getProperty("java.version"));
        LOG.info("os.name: " + System.getProperty("os.name"));
        LOG.info("os.arch: " + System.getProperty("os.arch"));

        if (!Files.exists(runtimeFile)) {
            LOG.severe("runtime.node not found at " + runtimeFile);
            LOG.severe("Run 'mvn generate-resources' first, or pass the path as an argument.");
            System.exit(1);
        }
        if (!Files.exists(copilotCliFile)) {
            LOG.severe("copilot CLI not found at " + copilotCliFile);
            LOG.severe("Ensure 'npm ci' has been run in the nodejs/ directory, or pass the path as the second argument.");
            System.exit(1);
        }

        // --- Load the real runtime.node via JNA ---
        LOG.info("[STEP 1] Loading runtime.node via JNA...");
        long loadStart = System.nanoTime();
        CopilotRuntimeLibrary lib;
        try {
            lib = Native.load(runtimeFile.toString(), CopilotRuntimeLibrary.class);
        } catch (UnsatisfiedLinkError e) {
            LOG.severe("[STEP 1] FAILED to load runtime.node: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
            return;
        }
        long loadElapsed = (System.nanoTime() - loadStart) / 1_000_000;
        LOG.info("[STEP 1] runtime.node loaded successfully in " + loadElapsed + " ms");

        // --- Build argv_json (same as FfiRuntimeHost.buildArgvJson) ---
        // argv[0] is the copilot CLI executable, NOT runtime.node.
        // The Rust embedded_host::start() does Command::new(argv[0]) to spawn
        // the child that connects back via napi-oop.
        String entrypoint = copilotCliFile.toString();
        String argvJson = "[\"" + escapeJson(entrypoint) + "\","
                + "\"--embedded-host\","
                + "\"--no-auto-update\","
                + "\"--log-level\",\"info\","
                + "\"--no-auto-login\"]";
        byte[] argvBytes = argvJson.getBytes(StandardCharsets.UTF_8);
        LOG.info("[STEP 2] argv_json (" + argvBytes.length + " bytes): " + argvJson);

        // --- Build env_json (minimal: just disable keytar) ---
        String envJson = "{\"COPILOT_DISABLE_KEYTAR\":\"1\"}";
        byte[] envBytes = envJson.getBytes(StandardCharsets.UTF_8);
        LOG.info("[STEP 2] env_json (" + envBytes.length + " bytes): " + envJson);

        // --- Call host_start on a separate thread with timeout ---
        LOG.info("[STEP 3] Calling copilot_runtime_host_start on background thread...");
        LOG.info("[STEP 3] Timeout: " + HOST_START_TIMEOUT_SECONDS + " s"
                + " (Rust READY_TIMEOUT is 30 s internally)");
        long hostStartTime = System.nanoTime();

        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "spike-host-start");
            t.setDaemon(true);
            return t;
        });

        Future<Integer> hostStartFuture = executor.submit(() -> {
            LOG.info("[host-start-thread] Thread started: " + Thread.currentThread().getName()
                    + " (id=" + Thread.currentThread().threadId() + ")");
            LOG.info("[host-start-thread] Calling copilot_runtime_host_start NOW...");
            long callStart = System.nanoTime();
            int result = lib.copilot_runtime_host_start(argvBytes, argvBytes.length,
                    envBytes, envBytes.length);
            long callElapsed = (System.nanoTime() - callStart) / 1_000_000;
            LOG.info("[host-start-thread] copilot_runtime_host_start returned: "
                    + result + " (elapsed: " + callElapsed + " ms)");
            return result;
        });

        int serverHandle;
        try {
            serverHandle = hostStartFuture.get(HOST_START_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            long elapsed = (System.nanoTime() - hostStartTime) / 1_000_000;
            LOG.severe("[STEP 3] TIMEOUT after " + elapsed + " ms waiting for host_start!");
            LOG.severe("[STEP 3] The Rust side has a 30 s READY_TIMEOUT. Possible causes:");
            LOG.severe("  - spawn_and_serve_background failed to spawn the child");
            LOG.severe("  - Child spawned but never called notify_ready");
            LOG.severe("  - Child spawned but crashed before connecting back");
            LOG.severe("  - The napi-oop socket handshake is hanging");
            LOG.severe("[STEP 3] Cancelling future and dumping threads...");
            hostStartFuture.cancel(true);
            dumpRelevantThreads();
            executor.shutdownNow();
            System.exit(2);
            return;
        }

        long hostStartElapsed = (System.nanoTime() - hostStartTime) / 1_000_000;
        LOG.info("[STEP 3] host_start completed in " + hostStartElapsed + " ms, serverHandle=" + serverHandle);

        if (serverHandle == 0) {
            LOG.severe("[STEP 3] host_start returned 0 (failure). Possible causes:");
            LOG.severe("  - argv_json parsing failed on the Rust side");
            LOG.severe("  - Child process spawn failed");
            LOG.severe("  - Child timed out during readiness handshake (30 s Rust READY_TIMEOUT)");
            dumpRelevantThreads();
            executor.shutdownNow();
            System.exit(3);
            return;
        }

        // --- connection_open with diagnostic callback ---
        LOG.info("[STEP 4] Calling copilot_runtime_connection_open...");
        AtomicInteger callbackCount = new AtomicInteger(0);
        AtomicInteger activeCallbacks = new AtomicInteger(0);

        // CRITICAL: hold as strong reference to prevent GC
        CopilotRuntimeLibrary.OutboundCallback callback = (Pointer userData, Pointer data, int len) -> {
            int active = activeCallbacks.incrementAndGet();
            int count = callbackCount.incrementAndGet();
            String threadName = Thread.currentThread().getName();
            long threadId = Thread.currentThread().threadId();
            try {
                byte[] bytes = data.getByteArray(0, Math.min(len, 4096));
                String preview = new String(bytes, StandardCharsets.UTF_8);
                if (preview.length() > 200) {
                    preview = preview.substring(0, 200) + "...";
                }
                LOG.info("[callback #" + count + "] thread='" + threadName + "' (id=" + threadId
                        + "), active=" + active + ", len=" + len
                        + ", preview: " + preview);
            } catch (Exception e) {
                LOG.warning("[callback #" + count + "] Error reading data: " + e.getMessage());
            } finally {
                activeCallbacks.decrementAndGet();
            }
        };

        long connStart = System.nanoTime();
        int connHandle = lib.copilot_runtime_connection_open(
                serverHandle, callback, Pointer.NULL,
                null, 0, null, 0, null, 0);
        long connElapsed = (System.nanoTime() - connStart) / 1_000_000;
        LOG.info("[STEP 4] connection_open returned connHandle=" + connHandle
                + " (elapsed: " + connElapsed + " ms)");

        if (connHandle == 0) {
            LOG.severe("[STEP 4] connection_open returned 0 (failure).");
            LOG.info("[STEP 5] Shutting down host...");
            lib.copilot_runtime_host_shutdown(serverHandle);
            executor.shutdownNow();
            System.exit(4);
            return;
        }

        // --- Wait briefly for any initial callbacks ---
        LOG.info("[STEP 4.1] Waiting 5 s for any initial outbound callbacks...");
        Thread.sleep(5000);
        LOG.info("[STEP 4.1] Callbacks received so far: " + callbackCount.get());

        // --- Send a minimal JSON-RPC initialize request ---
        String initRequest = "Content-Length: 80\r\n\r\n"
                + "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\","
                + "\"params\":{\"processId\":1}}";
        byte[] initBytes = initRequest.getBytes(StandardCharsets.UTF_8);
        LOG.info("[STEP 5] Sending initialize request (" + initBytes.length + " bytes)...");
        byte writeResult = lib.copilot_runtime_connection_write(connHandle, initBytes, initBytes.length);
        LOG.info("[STEP 5] connection_write returned: " + writeResult);

        // Wait for response callbacks
        LOG.info("[STEP 5.1] Waiting 5 s for response callbacks...");
        Thread.sleep(5000);
        LOG.info("[STEP 5.1] Total callbacks received: " + callbackCount.get());

        // --- Cleanup ---
        LOG.info("[STEP 6] Cleaning up...");

        LOG.info("[STEP 6.1] connection_close(connHandle=" + connHandle + ")...");
        byte closeResult = lib.copilot_runtime_connection_close(connHandle);
        LOG.info("[STEP 6.1] connection_close returned: " + closeResult);

        LOG.info("[STEP 6.2] host_shutdown(serverHandle=" + serverHandle + ")...");
        byte shutdownResult = lib.copilot_runtime_host_shutdown(serverHandle);
        LOG.info("[STEP 6.2] host_shutdown returned: " + shutdownResult);

        executor.shutdownNow();

        LOG.info("=== Spike complete ===");
        LOG.info("Summary:");
        LOG.info("  runtime.node loaded:    YES (" + loadElapsed + " ms)");
        LOG.info("  host_start result:      " + serverHandle + " (" + hostStartElapsed + " ms)");
        LOG.info("  connection_open result: " + connHandle + " (" + connElapsed + " ms)");
        LOG.info("  Total callbacks:        " + callbackCount.get());
        LOG.info("  write result:           " + writeResult);
        LOG.info("  close result:           " + closeResult);
        LOG.info("  shutdown result:        " + shutdownResult);
    }

    private static void dumpRelevantThreads() {
        LOG.info("--- Thread dump (relevant threads) ---");
        Thread.getAllStackTraces().forEach((thread, stack) -> {
            String name = thread.getName();
            if (name.contains("spike") || name.contains("copilot") || name.contains("ffi")
                    || name.contains("napi") || name.contains("host") || name.contains("main")) {
                StringBuilder sb = new StringBuilder();
                sb.append("  Thread '").append(name).append("' (id=").append(thread.threadId())
                        .append(", state=").append(thread.getState()).append(")\n");
                for (StackTraceElement ste : stack) {
                    sb.append("    at ").append(ste).append("\n");
                }
                LOG.info(sb.toString());
            }
        });
        LOG.info("--- End thread dump ---");
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void configureLogging() {
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        for (var handler : root.getHandlers()) {
            root.removeHandler(handler);
        }
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.ALL);
        ch.setFormatter(new SimpleFormatter());
        root.addHandler(ch);
    }
}
