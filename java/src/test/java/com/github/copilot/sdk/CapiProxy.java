/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Manages a replaying proxy server for E2E tests.
 *
 * <p>
 * This spawns the shared test harness server from test/harness/server.ts which
 * acts as a replaying proxy to AI endpoints. It captures and stores
 * request/response pairs in YAML snapshot files and replays stored responses on
 * subsequent runs for deterministic testing.
 * </p>
 *
 * <p>
 * Usage example:
 * </p>
 *
 * <pre>
 * {@code
 * CapiProxy proxy = new CapiProxy();
 * String proxyUrl = proxy.start();
 *
 * // Configure for a specific test
 * proxy.configure("test/snapshots/tools/my_test.yaml", workDir);
 *
 * // ... run tests with proxyUrl ...
 *
 * // Get captured exchanges
 * List<Map<String, Object>> exchanges = proxy.getExchanges();
 *
 * proxy.stop();
 * }
 * </pre>
 */
public class CapiProxy implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern LISTENING_PATTERN = Pattern.compile("Listening: (http://[^\\s]+)");

    private Process process;
    private String proxyUrl;
    private final HttpClient httpClient;
    private BufferedReader stdoutReader;

    public CapiProxy() {
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Starts the proxy server and returns its URL.
     *
     * @return the proxy URL (e.g., "http://localhost:12345")
     * @throws IOException
     *             if the server fails to start
     * @throws InterruptedException
     *             if the startup is interrupted
     */
    public String start() throws IOException, InterruptedException {
        if (proxyUrl != null) {
            return proxyUrl;
        }

        // Find the repo root by looking for the test/harness directory
        Path harnessDir = findHarnessDirectory();
        if (harnessDir == null) {
            throw new IOException("Could not find test/harness directory. "
                    + "Make sure you are running from within the copilot-sdk repository.");
        }

        // Start the harness server using npx tsx
        ProcessBuilder pb = new ProcessBuilder("npx", "tsx", "server.ts");
        pb.directory(harnessDir.toFile());
        pb.redirectErrorStream(false);

        process = pb.start();

        // Read stdout to get the listening URL
        // Note: We keep the reader open to avoid closing the process input stream
        stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        // Also consume stderr in a background thread to prevent blocking
        Thread stderrThread = new Thread(() -> {
            try (BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String errLine;
                while ((errLine = errReader.readLine()) != null) {
                    System.err.println("[CapiProxy stderr] " + errLine);
                }
            } catch (IOException e) {
                // Ignore
            }
        });
        stderrThread.setDaemon(true);
        stderrThread.start();

        String line = stdoutReader.readLine();
        if (line == null) {
            // Try to get error info
            StringBuilder errInfo = new StringBuilder();
            try (BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String errLine;
                while ((errLine = errReader.readLine()) != null) {
                    errInfo.append(errLine).append("\n");
                }
            }
            process.destroyForcibly();
            throw new IOException("Failed to read proxy URL - server may have crashed. Stderr: " + errInfo);
        }

        Matcher matcher = LISTENING_PATTERN.matcher(line);
        if (!matcher.find()) {
            process.destroyForcibly();
            throw new IOException("Unexpected proxy output: " + line);
        }

        proxyUrl = matcher.group(1);
        return proxyUrl;
    }

    /**
     * Configures the proxy for a specific test file.
     *
     * @param filePath
     *            the path to the YAML snapshot file (relative to repo root)
     * @param workDir
     *            the working directory for path normalization
     * @throws IOException
     *             if the configuration fails
     * @throws InterruptedException
     *             if the request is interrupted
     */
    public void configure(String filePath, String workDir) throws IOException, InterruptedException {
        configure(filePath, workDir, null);
    }

    /**
     * Configures the proxy for a specific test file.
     *
     * @param filePath
     *            the path to the YAML snapshot file (relative to repo root)
     * @param workDir
     *            the working directory for path normalization
     * @param testInfo
     *            optional test information (file and line number)
     * @throws IOException
     *             if the configuration fails
     * @throws InterruptedException
     *             if the request is interrupted
     */
    public void configure(String filePath, String workDir, TestInfo testInfo) throws IOException, InterruptedException {
        if (proxyUrl == null) {
            throw new IllegalStateException("Proxy not started");
        }

        Map<String, Object> config = new java.util.HashMap<>();
        config.put("filePath", filePath);
        config.put("workDir", workDir);
        if (testInfo != null) {
            config.put("testInfo", Map.of("file", testInfo.file(), "line", testInfo.line()));
        }

        String body = MAPPER.writeValueAsString(config);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(proxyUrl + "/config"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Proxy config failed with status " + response.statusCode() + ": " + response.body());
        }
    }

    /**
     * Gets the captured HTTP exchanges from the proxy.
     *
     * @return list of exchange maps containing request/response data
     * @throws IOException
     *             if the request fails
     * @throws InterruptedException
     *             if the request is interrupted
     */
    public List<Map<String, Object>> getExchanges() throws IOException, InterruptedException {
        if (proxyUrl == null) {
            throw new IllegalStateException("Proxy not started");
        }

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(proxyUrl + "/exchanges")).GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to get exchanges: " + response.statusCode());
        }

        return MAPPER.readValue(response.body(), new TypeReference<List<Map<String, Object>>>() {
        });
    }

    /**
     * Stops the proxy server gracefully.
     *
     * @throws IOException
     *             if the stop request fails
     * @throws InterruptedException
     *             if the request is interrupted
     */
    public void stop() throws IOException, InterruptedException {
        stop(false);
    }

    /**
     * Stops the proxy server.
     *
     * @param skipWritingCache
     *            if true, won't write captured exchanges to disk
     * @throws IOException
     *             if the stop request fails
     * @throws InterruptedException
     *             if the request is interrupted
     */
    public void stop(boolean skipWritingCache) throws IOException, InterruptedException {
        if (process == null) {
            return;
        }

        // Send stop request to the server
        if (proxyUrl != null) {
            try {
                String stopUrl = proxyUrl + "/stop";
                if (skipWritingCache) {
                    stopUrl += "?skipWritingCache=true";
                }

                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(stopUrl))
                        .POST(HttpRequest.BodyPublishers.noBody()).build();

                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                // Best effort - ignore errors
            }
        }

        // Wait for the process to exit
        process.waitFor(5, TimeUnit.SECONDS);
        if (process.isAlive()) {
            process.destroyForcibly();
        }

        // Close the stdout reader
        if (stdoutReader != null) {
            try {
                stdoutReader.close();
            } catch (IOException e) {
                // Ignore
            }
            stdoutReader = null;
        }

        process = null;
        proxyUrl = null;
    }

    /**
     * Gets the proxy URL.
     *
     * @return the proxy URL, or null if not started
     */
    public String getProxyUrl() {
        return proxyUrl;
    }

    /**
     * Checks if the proxy process is still alive.
     *
     * @return true if the proxy is running, false otherwise
     */
    public boolean isAlive() {
        return process != null && process.isAlive();
    }

    /**
     * Restarts the proxy server. This stops the current instance (if any) and
     * starts a new one.
     *
     * @return the new proxy URL
     * @throws IOException
     *             if the server fails to start
     * @throws InterruptedException
     *             if the startup is interrupted
     */
    public String restart() throws IOException, InterruptedException {
        try {
            stop(true); // Skip writing cache on restart
        } catch (Exception e) {
            // Best effort - force cleanup
            if (process != null) {
                process.destroyForcibly();
                process = null;
            }
            proxyUrl = null;
        }
        return start();
    }

    @Override
    public void close() throws Exception {
        stop();
    }

    /**
     * Finds the test/harness directory by walking up from the current directory.
     */
    private Path findHarnessDirectory() {
        // First, check for copilot.sdk.dir system property (set by Maven during tests)
        String sdkDir = System.getProperty("copilot.sdk.dir");
        if (sdkDir != null && !sdkDir.isEmpty()) {
            Path harnessDir = Paths.get(sdkDir).resolve("test").resolve("harness");
            if (harnessDir.toFile().exists() && harnessDir.resolve("server.ts").toFile().exists()) {
                return harnessDir;
            }
        }

        // Fallback: walk up the directory tree looking for test/harness
        Path current = Paths.get(System.getProperty("user.dir"));
        while (current != null) {
            Path harnessDir = current.resolve("test").resolve("harness");
            if (harnessDir.toFile().exists() && harnessDir.resolve("server.ts").toFile().exists()) {
                return harnessDir;
            }
            current = current.getParent();
        }

        return null;
    }

    /**
     * Test information record for configuring the proxy.
     */
    public record TestInfo(String file, int line) {
    }
}
