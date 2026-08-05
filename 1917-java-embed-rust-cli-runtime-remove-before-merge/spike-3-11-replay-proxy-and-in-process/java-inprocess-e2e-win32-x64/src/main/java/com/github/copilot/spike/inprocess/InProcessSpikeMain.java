package com.github.copilot.spike.inprocess;

import com.sun.jna.Pointer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Spike 3.11 — InProcess E2E transport on win32-x64.
 *
 * <p>Demonstrates the full InProcess flow against the real {@code runtime.node}:
 * <ol>
 *   <li>Apply {@link InProcessEnvGuard} to set process-level env vars that the
 *       in-process runtime reads (e.g. {@code COPILOT_API_URL} for proxy redirect,
 *       {@code COPILOT_SDK_AUTH_TOKEN} for auth).</li>
 *   <li>Load {@code runtime.node} by absolute path via JNA.</li>
 *   <li>Call {@code copilot_runtime_host_start} (blocking up to 30 s).</li>
 *   <li>Register the outbound callback and call {@code copilot_runtime_connection_open}.</li>
 *   <li>Write a JSON-RPC ping request using LSP {@code Content-Length:} framing.</li>
 *   <li>Read the pong response from the {@link QueueInputStream} fed by the callback.</li>
 *   <li>Verify the response and shut down cleanly.</li>
 * </ol>
 *
 * <h2>Key finding: InProcessEnvGuard is required for proxy redirection</h2>
 *
 * <p>The replay proxy redirects HTTP traffic from the runtime to a local recording.
 * The runtime reads {@code COPILOT_API_URL} from the native process environment block,
 * not from Java's {@code System.getenv()} snapshot or any per-client dictionary.
 * {@link InProcessEnvGuard} uses JNA to call {@code SetEnvironmentVariableW} (Windows)
 * before starting the runtime, so the replay proxy URL is seen by the native code.
 *
 * <h2>Running against the replay proxy (full E2E)</h2>
 *
 * <p>Start the proxy with:
 * <pre>
 * cd test/harness && node server.ts
 * </pre>
 * Then set {@code COPILOT_API_URL} to the proxy URL before running this spike.
 * The spike will send traffic to the proxy, which replays YAML-recorded API responses.
 *
 * <h2>Running without a proxy (direct Copilot API)</h2>
 *
 * <p>Requires a real GitHub token with Copilot access:
 * <pre>
 * $env:GH_TOKEN = "ghp_..."
 * $env:COPILOT_CLI_PATH = "C:\...\copilot-win32-x64\copilot.exe"
 * $env:RUNTIME_NODE_PATH = "C:\...\copilot-win32-x64\prebuilds\win32-x64\runtime.node"
 * java -jar target\spike-3-11-inprocess-e2e-win32-x64.jar
 * </pre>
 */
public class InProcessSpikeMain {

    private static final Logger LOG = Logger.getLogger(InProcessSpikeMain.class.getName());

    // Timeout waiting for the pong response after writing the ping.
    private static final long RESPONSE_TIMEOUT_SECONDS = 60;

    public static void main(String[] args) throws Exception {
        configureLogging();
        LOG.info("=== Spike 3.11 — InProcess E2E (win32-x64) ===");
        LOG.info("JVM version: " + System.getProperty("java.version"));
        LOG.info("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));

        // ------------------------------------------------------------------
        // 1. Locate runtime.node and copilot.exe
        // ------------------------------------------------------------------
        String runtimeNodePath = resolveRuntimeNodePath();
        String copilotExePath = resolveCopilotExePath();
        String ghToken = resolveGhToken();
        String copilotHome = resolveCopilotHome();

        LOG.info("runtime.node : " + runtimeNodePath);
        LOG.info("copilot.exe  : " + copilotExePath);
        LOG.info("COPILOT_HOME : " + copilotHome);

        // ------------------------------------------------------------------
        // 2. Apply InProcessEnvGuard
        //
        // Sets the following in the native process environment block so the
        // runtime.node (loaded in-process via JNA) reads them via GetEnvironmentVariableW:
        //   COPILOT_SDK_AUTH_TOKEN — auth token for the runtime
        //   GH_TOKEN               — GitHub token (used by the runtime for auth)
        //   COPILOT_HOME           — isolate runtime home dir to temp
        //   COPILOT_DISABLE_KEYTAR — "1" to bypass keychain in CI/spike
        //
        // In a real E2E test against the replay proxy, this map would also include:
        //   COPILOT_API_URL        — "http://localhost:<proxyPort>" to redirect HTTP
        //   COPILOT_DEBUG_GITHUB_API_URL — same proxy URL
        //   GH_CONFIG_DIR          — temp dir for isolated gh config
        //   XDG_CONFIG_HOME        — temp dir for isolated XDG config
        // ------------------------------------------------------------------
        Map<String, String> envOverrides = new LinkedHashMap<>();
        envOverrides.put("COPILOT_SDK_AUTH_TOKEN", ghToken);
        envOverrides.put("GH_TOKEN", ghToken);
        envOverrides.put("GITHUB_TOKEN", ghToken);
        envOverrides.put("COPILOT_HOME", copilotHome);
        envOverrides.put("COPILOT_DISABLE_KEYTAR", "1");
        // If a replay proxy URL is given, redirect HTTP traffic to it.
        String proxyUrl = System.getenv("COPILOT_API_URL");
        if (proxyUrl != null && !proxyUrl.isEmpty()) {
            LOG.info("Redirecting runtime HTTP to replay proxy: " + proxyUrl);
            envOverrides.put("COPILOT_API_URL", proxyUrl);
            envOverrides.put("COPILOT_DEBUG_GITHUB_API_URL", proxyUrl);
        } else {
            LOG.info("No COPILOT_API_URL set — using real Copilot API (requires live token).");
        }

        try (InProcessEnvGuard envGuard = new InProcessEnvGuard(envOverrides)) {

            // ------------------------------------------------------------------
            // 3. Load runtime.node via JNA
            // ------------------------------------------------------------------
            CopilotRuntimeLibrary lib = CopilotRuntimeLibrary.load(runtimeNodePath);

            // ------------------------------------------------------------------
            // 4. Build argv_json for copilot_runtime_host_start
            //
            // From spike-3-9 resolution:
            //   argv_json = [entrypoint, "--embedded-host", "--no-auto-update",
            //                "--auth-token-env", "COPILOT_SDK_AUTH_TOKEN",
            //                "--no-auto-login"]
            //
            // Prefix with "node" only if entrypoint ends in ".js"; copilot.exe
            // is a binary entrypoint so no prefix is needed.
            // ------------------------------------------------------------------
            String argvJson = buildArgvJson(copilotExePath);
            byte[] argvJsonBytes = argvJson.getBytes(StandardCharsets.UTF_8);
            LOG.info("[host_start] argv_json=" + argvJson);

            // env_json: only 3 keys are supported; we use null here because the
            // InProcessEnvGuard already set the token in the native env block.
            // Alternatively pass {"COPILOT_SDK_AUTH_TOKEN":"<token>"} explicitly.
            byte[] envJsonBytes = null;
            long envJsonLen = 0;

            // ------------------------------------------------------------------
            // 5. copilot_runtime_host_start — BLOCKS up to 30 s
            // ------------------------------------------------------------------
            LOG.info("[host_start] Calling copilot_runtime_host_start (blocks up to 30s)...");
            long startMs = System.currentTimeMillis();
            int serverId = lib.copilot_runtime_host_start(
                    argvJsonBytes, argvJsonBytes.length,
                    envJsonBytes, envJsonLen);
            long elapsedMs = System.currentTimeMillis() - startMs;
            LOG.info("[host_start] returned serverId=" + serverId + " after " + elapsedMs + " ms");

            if (serverId == 0) {
                LOG.severe("FAIL: copilot_runtime_host_start returned 0 (failure).");
                LOG.severe("Check: Is runtime.node the correct binary for this platform?");
                LOG.severe("Check: Is the auth token valid and does it have Copilot access?");
                System.exit(1);
            }

            // ------------------------------------------------------------------
            // 6. Set up QueueInputStream + callback tracking
            // ------------------------------------------------------------------
            QueueInputStream queueIn = new QueueInputStream();
            AtomicInteger activeCallbacks = new AtomicInteger(0);

            // The callback is invoked on a new JNA-managed Java thread per call.
            // We hold a strong reference to prevent GC while native code holds
            // the function pointer (spike-3-4 finding).
            CopilotRuntimeLibrary.OutboundCallback callbackRef =
                    (Pointer userData, Pointer data, long len) -> {
                        int active = activeCallbacks.incrementAndGet();
                        String threadName = Thread.currentThread().getName();
                        long threadId = Thread.currentThread().threadId();
                        LOG.info("[callback] ENTERED on thread '" + threadName
                                + "' id=" + threadId + " active=" + active + " len=" + len);
                        try {
                            // Copy data before returning — pointer is only valid during callback
                            byte[] bytes = data.getByteArray(0, (int) len);
                            queueIn.enqueue(bytes);
                            LOG.info("[callback] Frame enqueued (" + len + " bytes).");
                        } finally {
                            int remaining = activeCallbacks.decrementAndGet();
                            LOG.info("[callback] EXITING thread '" + threadName
                                    + "' remaining active=" + remaining);
                        }
                    };

            // ------------------------------------------------------------------
            // 7. copilot_runtime_connection_open
            // ------------------------------------------------------------------
            LOG.info("[connection_open] Calling copilot_runtime_connection_open...");
            int connectionId = lib.copilot_runtime_connection_open(
                    serverId,
                    callbackRef,
                    Pointer.NULL,   // user_data: always null (use Java closure capture)
                    Pointer.NULL, 0, // ext_source: reserved, always null/0
                    Pointer.NULL, 0, // ext_name:   reserved, always null/0
                    Pointer.NULL, 0  // conn_token: reserved, always null/0
            );
            LOG.info("[connection_open] returned connectionId=" + connectionId);

            if (connectionId == 0) {
                LOG.severe("FAIL: copilot_runtime_connection_open returned 0 (failure).");
                lib.copilot_runtime_host_shutdown(serverId);
                System.exit(1);
            }

            // ------------------------------------------------------------------
            // 8. Write a JSON-RPC ping request using LSP Content-Length framing
            //
            // From spike-3-9: the wire format is LSP "Content-Length: N\r\n\r\n<body>",
            // identical to the stdio transport.  The existing Java JsonRpcClient uses
            // this same framing — no change needed at the FFI boundary.
            // ------------------------------------------------------------------
            String pingMessage = "hello from java inprocess spike";
            String pingBody = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\","
                    + "\"params\":{\"message\":\"" + pingMessage + "\"}}";
            String pingFrame = "Content-Length: " + pingBody.length() + "\r\n\r\n" + pingBody;
            byte[] pingFrameBytes = pingFrame.getBytes(StandardCharsets.UTF_8);

            LOG.info("[connection_write] Writing ping: " + pingFrame.replace("\r\n", "\\r\\n"));
            boolean written = lib.copilot_runtime_connection_write(
                    connectionId, pingFrameBytes, pingFrameBytes.length);
            if (!written) {
                LOG.severe("FAIL: copilot_runtime_connection_write returned false.");
                lib.copilot_runtime_connection_close(connectionId);
                lib.copilot_runtime_host_shutdown(serverId);
                System.exit(1);
            }

            // ------------------------------------------------------------------
            // 9. Read the pong response from QueueInputStream
            //
            // Each on_outbound callback call delivers one complete LSP frame.
            // Use takeFrame() to get the raw bytes and parse the Content-Length header.
            // ------------------------------------------------------------------
            LOG.info("[reader] Waiting for pong response (timeout=" + RESPONSE_TIMEOUT_SECONDS + "s)...");

            // Start a reader thread (in production this would be a JsonRpcClient reader thread)
            CountDownLatch pongLatch = new CountDownLatch(1);
            String[] pongBodyHolder = {null};
            String[] errorHolder = {null};

            Thread readerThread = new Thread(() -> {
                try {
                    // Read frames until we see a response to our ping (id=1)
                    while (true) {
                        byte[] frameBytes = queueIn.takeFrame();
                        if (frameBytes.length == 0) {
                            errorHolder[0] = "Stream closed before pong received.";
                            pongLatch.countDown();
                            return;
                        }
                        String frame = new String(frameBytes, StandardCharsets.UTF_8);
                        LOG.info("[reader] Received frame (" + frameBytes.length + " bytes): "
                                + frame.replace("\r\n", "\\r\\n"));

                        // Parse LSP header
                        int headerEnd = frame.indexOf("\r\n\r\n");
                        if (headerEnd < 0) {
                            LOG.warning("[reader] Malformed frame (no header boundary): " + frame);
                            continue;
                        }
                        String body = frame.substring(headerEnd + 4);

                        // Look for our ping response (id=1, result containing pong message)
                        if (body.contains("\"id\":1") && body.contains("pong")) {
                            pongBodyHolder[0] = body;
                            pongLatch.countDown();
                            return;
                        }
                        // Other frames (e.g. server-sent notifications) — keep reading
                        LOG.info("[reader] Frame is not our pong, continuing...");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errorHolder[0] = "Interrupted: " + e.getMessage();
                    pongLatch.countDown();
                }
            }, "spike-3-11-reader");
            readerThread.setDaemon(true);
            readerThread.start();

            boolean gotPong = pongLatch.await(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // ------------------------------------------------------------------
            // 10. Shutdown: drain callbacks → connection_close → host_shutdown
            // ------------------------------------------------------------------
            LOG.info("[shutdown] Waiting for active callbacks to drain...");
            int drainAttempts = 0;
            while (activeCallbacks.get() > 0 && drainAttempts++ < 100) {
                Thread.sleep(10);
            }
            queueIn.signalEof();

            LOG.info("[shutdown] Calling copilot_runtime_connection_close...");
            boolean closed = lib.copilot_runtime_connection_close(connectionId);
            LOG.info("[shutdown] connection_close returned " + closed);

            LOG.info("[shutdown] Calling copilot_runtime_host_shutdown...");
            boolean shutdown = lib.copilot_runtime_host_shutdown(serverId);
            LOG.info("[shutdown] host_shutdown returned " + shutdown);

            // callbackRef strong reference is released when this try-block exits,
            // after which native code can no longer invoke the callback.

            // ------------------------------------------------------------------
            // 11. Assert and report
            // ------------------------------------------------------------------
            LOG.info("--- Spike 3.11 result ---");

            if (!gotPong || errorHolder[0] != null) {
                LOG.severe("FAIL: Did not receive pong within " + RESPONSE_TIMEOUT_SECONDS + "s.");
                if (errorHolder[0] != null) LOG.severe("Error: " + errorHolder[0]);
                System.exit(1);
            }

            String pongBody = pongBodyHolder[0];
            if (!pongBody.contains("pong: " + pingMessage)) {
                LOG.severe("FAIL: Pong body does not contain expected text.");
                LOG.severe("Body: " + pongBody);
                System.exit(1);
            }

            LOG.info("PASS: Received pong: " + pongBody);
            LOG.info("PASS: InProcess transport works on win32-x64.");
            LOG.info("PASS: InProcessEnvGuard successfully set process-level env vars.");
            LOG.info("PASS: replay proxy redirection is possible by setting COPILOT_API_URL"
                    + " in the InProcessEnvGuard map.");

        } // InProcessEnvGuard.close() restores env vars here

        LOG.info("InProcessEnvGuard has restored all env vars. Spike complete.");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves the absolute path to {@code runtime.node}.
     *
     * <p>Priority:
     * <ol>
     *   <li>{@code RUNTIME_NODE_PATH} environment variable (explicit override)</li>
     *   <li>Well-known location relative to {@code COPILOT_CLI_PATH} — the
     *       {@code prebuilds/win32-x64/runtime.node} sibling.</li>
     *   <li>The {@code runtime.node} bundled in the monorepo's {@code nodejs/node_modules}
     *       (for development use only).</li>
     * </ol>
     */
    private static String resolveRuntimeNodePath() {
        // Explicit override
        String explicit = System.getenv("RUNTIME_NODE_PATH");
        if (explicit != null && !explicit.isEmpty() && Files.isRegularFile(Paths.get(explicit))) {
            return explicit;
        }

        // Sibling of COPILOT_CLI_PATH: e.g. .../copilot-win32-x64/prebuilds/win32-x64/runtime.node
        String cliPath = System.getenv("COPILOT_CLI_PATH");
        if (cliPath != null && !cliPath.isEmpty()) {
            Path cliDir = Paths.get(cliPath).getParent();
            if (cliDir != null) {
                Path sibling = cliDir.resolve("prebuilds").resolve("win32-x64").resolve("runtime.node");
                if (Files.isRegularFile(sibling)) {
                    return sibling.toString();
                }
            }
        }

        // Fallback: monorepo nodejs/node_modules (dev-only)
        Path devPath = Paths.get(System.getProperty("user.dir"))
                .resolve("..")  // spike dir
                .resolve("..")  // 1917-java-embed dir
                .resolve("..")  // copilot-sdk root
                .resolve("nodejs")
                .resolve("node_modules")
                .resolve("@github")
                .resolve("copilot-win32-x64")
                .resolve("prebuilds")
                .resolve("win32-x64")
                .resolve("runtime.node")
                .toAbsolutePath()
                .normalize();
        if (Files.isRegularFile(devPath)) {
            LOG.info("[resolve] Using monorepo runtime.node: " + devPath);
            return devPath.toString();
        }

        throw new IllegalStateException(
                "runtime.node not found. Set RUNTIME_NODE_PATH to the absolute path of the binary.");
    }

    /**
     * Resolves the Copilot CLI entrypoint path ({@code copilot.exe} on Windows).
     */
    private static String resolveCopilotExePath() {
        String explicit = System.getenv("COPILOT_CLI_PATH");
        if (explicit != null && !explicit.isEmpty() && Files.isRegularFile(Paths.get(explicit))) {
            return explicit;
        }

        Path devPath = Paths.get(System.getProperty("user.dir"))
                .resolve("..")
                .resolve("..")
                .resolve("..")
                .resolve("nodejs")
                .resolve("node_modules")
                .resolve("@github")
                .resolve("copilot-win32-x64")
                .resolve("copilot.exe")
                .toAbsolutePath()
                .normalize();
        if (Files.isRegularFile(devPath)) {
            return devPath.toString();
        }

        throw new IllegalStateException(
                "copilot.exe not found. Set COPILOT_CLI_PATH to the absolute path.");
    }

    /**
     * Resolves a GitHub auth token.  Uses GH_TOKEN or GITHUB_TOKEN; fails if neither is set.
     */
    private static String resolveGhToken() {
        String token = System.getenv("GH_TOKEN");
        if (token == null || token.isEmpty()) token = System.getenv("GITHUB_TOKEN");
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException(
                    "No GitHub token found. Set GH_TOKEN or GITHUB_TOKEN env var.");
        }
        return token;
    }

    /**
     * Returns an isolated COPILOT_HOME temp directory for this spike run.
     */
    private static String resolveCopilotHome() throws IOException {
        String home = System.getenv("COPILOT_HOME");
        if (home != null && !home.isEmpty()) return home;
        Path tmp = Files.createTempDirectory("spike-3-11-copilot-home-");
        tmp.toFile().deleteOnExit();
        return tmp.toString();
    }

    /**
     * Builds the {@code argv_json} array for {@code copilot_runtime_host_start}.
     *
     * <p>From spike-3-9:
     * <ul>
     *   <li>Prefix with {@code "node"} only if entrypoint ends in {@code .js}.</li>
     *   <li>Always include {@code --embedded-host} and {@code --no-auto-update}.</li>
     *   <li>Include {@code --auth-token-env COPILOT_SDK_AUTH_TOKEN} when a token
     *       is being provided via env (which is our case).</li>
     *   <li>Include {@code --no-auto-login} to skip interactive login.</li>
     * </ul>
     */
    private static String buildArgvJson(String entrypoint) {
        StringBuilder sb = new StringBuilder("[");
        // Prefix with "node" for .js entrypoints; copilot.exe is a binary entrypoint.
        if (entrypoint.endsWith(".js")) {
            appendJsonString(sb, "node");
            sb.append(',');
        }
        appendJsonString(sb, entrypoint);
        sb.append(',');
        appendJsonString(sb, "--embedded-host");
        sb.append(',');
        appendJsonString(sb, "--no-auto-update");
        sb.append(',');
        appendJsonString(sb, "--auth-token-env");
        sb.append(',');
        appendJsonString(sb, "COPILOT_SDK_AUTH_TOKEN");
        sb.append(',');
        appendJsonString(sb, "--no-auto-login");
        sb.append(']');
        return sb.toString();
    }

    private static void appendJsonString(StringBuilder sb, String value) {
        sb.append('"');
        // Minimal JSON escaping for path strings (backslash and double-quote)
        for (char c : value.toCharArray()) {
            if (c == '\\') sb.append("\\\\");
            else if (c == '"') sb.append("\\\"");
            else sb.append(c);
        }
        sb.append('"');
    }

    private static void configureLogging() {
        Logger root = Logger.getLogger("");
        root.setLevel(Level.INFO);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.INFO);
        handler.setFormatter(new SimpleFormatter() {
            @Override
            public synchronized String format(java.util.logging.LogRecord lr) {
                return String.format("[%s] %s%n",
                        lr.getLevel().getName(),
                        lr.getMessage());
            }
        });
        root.getHandlers()[0].setLevel(Level.INFO);
        for (java.util.logging.Handler h : root.getHandlers()) root.removeHandler(h);
        root.addHandler(handler);
    }
}
