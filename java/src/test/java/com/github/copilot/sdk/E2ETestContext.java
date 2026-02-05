/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.copilot.sdk.json.CopilotClientOptions;

/**
 * E2E test context that manages the test environment including the CapiProxy,
 * working directories, and CLI path.
 *
 * <p>
 * This provides a complete test environment similar to the Node.js, .NET, Go,
 * and Python SDK test harnesses. It manages:
 * </p>
 * <ul>
 * <li>A replaying CapiProxy for deterministic API responses</li>
 * <li>Temporary home and work directories for test isolation</li>
 * <li>Environment variables for the Copilot CLI</li>
 * </ul>
 *
 * <p>
 * Usage example:
 * </p>
 *
 * <pre>
 * {@code
 * try (E2ETestContext ctx = E2ETestContext.create()) {
 * 	ctx.configureForTest("tools", "my_test_name");
 *
 * 	try (CopilotClient client = ctx.createClient()) {
 * 		CopilotSession session = client.createSession().get();
 * 		// ... run test ...
 * 	}
 * }
 * }
 * </pre>
 */
public class E2ETestContext implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(E2ETestContext.class.getName());
    private static final Pattern SNAKE_CASE = Pattern.compile("[^a-zA-Z0-9]");
    private static final Pattern USER_CONTENT_PATTERN = Pattern
            .compile("^\\s+-\\s+role:\\s+user\\s*$\\s+content:\\s*(.+?)$", Pattern.MULTILINE);

    private final String cliPath;
    private final Path homeDir;
    private final Path workDir;
    private String proxyUrl;
    private final CapiProxy proxy;
    private final Path repoRoot;
    private Path currentSnapshotFile;

    private E2ETestContext(String cliPath, Path homeDir, Path workDir, String proxyUrl, CapiProxy proxy,
            Path repoRoot) {
        this.cliPath = cliPath;
        this.homeDir = homeDir;
        this.workDir = workDir;
        this.proxyUrl = proxyUrl;
        this.proxy = proxy;
        this.repoRoot = repoRoot;
    }

    /**
     * Creates a new E2E test context.
     *
     * @return the test context
     * @throws IOException
     *             if setup fails
     * @throws InterruptedException
     *             if setup is interrupted
     */
    public static E2ETestContext create() throws IOException, InterruptedException {
        Path repoRoot = findRepoRoot();
        String cliPath = getCliPath(repoRoot);

        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path homeDir = Files.createTempDirectory(tempDir, "copilot-test-config-");
        Path workDir = Files.createTempDirectory(tempDir, "copilot-test-work-");

        CapiProxy proxy = new CapiProxy();
        String proxyUrl = proxy.start();

        return new E2ETestContext(cliPath, homeDir, workDir, proxyUrl, proxy, repoRoot);
    }

    /**
     * Gets the Copilot CLI path.
     */
    public String getCliPath() {
        return cliPath;
    }

    /**
     * Gets the temporary home directory for test isolation.
     */
    public Path getHomeDir() {
        return homeDir;
    }

    /**
     * Gets the temporary working directory for tests.
     */
    public Path getWorkDir() {
        return workDir;
    }

    /**
     * Gets the proxy URL.
     */
    public String getProxyUrl() {
        return proxyUrl;
    }

    /**
     * Configures the proxy for a specific test.
     *
     * @param testFile
     *            the test category folder (e.g., "tools", "session", "permissions")
     * @param testName
     *            the test method name (will be converted to snake_case)
     * @throws IOException
     *             if configuration fails
     * @throws InterruptedException
     *             if configuration is interrupted
     */
    public void configureForTest(String testFile, String testName) throws IOException, InterruptedException {
        // Restart the proxy if it has crashed
        ensureProxyAlive();

        // Convert test method names to lowercase snake_case for snapshot filenames
        // to avoid case collisions on case-insensitive filesystems (macOS/Windows)
        String sanitizedName = SNAKE_CASE.matcher(testName).replaceAll("_").toLowerCase();
        Path snapshotFile = repoRoot.resolve("test").resolve("snapshots").resolve(testFile)
                .resolve(sanitizedName + ".yaml");

        // Validate snapshot exists - fail fast with a clear message
        if (!Files.exists(snapshotFile)) {
            Path snapshotsDir = repoRoot.resolve("test").resolve("snapshots").resolve(testFile);
            String availableSnapshots = "";
            if (Files.exists(snapshotsDir)) {
                try (var files = Files.list(snapshotsDir)) {
                    availableSnapshots = files.filter(p -> p.toString().endsWith(".yaml"))
                            .map(p -> p.getFileName().toString().replace(".yaml", "")).sorted()
                            .reduce((a, b) -> a + ", " + b).orElse("<none>");
                }
            }
            throw new IOException(String.format(
                    "Snapshot file not found: %s%n" + "Category: %s, Test: %s (sanitized: %s)%n"
                            + "Available snapshots in '%s/': %s%n"
                            + "Ensure the snapshot exists and the test name matches exactly.",
                    snapshotFile, testFile, testName, sanitizedName, testFile, availableSnapshots));
        }

        this.currentSnapshotFile = snapshotFile;
        proxy.configure(snapshotFile.toString(), workDir.toString());

        // Log expected prompts to help debug prompt mismatch issues
        List<String> expectedPrompts = getExpectedUserPrompts();
        if (!expectedPrompts.isEmpty()) {
            LOG.info(() -> String.format("Configured snapshot '%s/%s' expects prompts: %s", testFile, sanitizedName,
                    expectedPrompts));
        }
    }

    /**
     * Gets the expected user prompts from the current snapshot file.
     * <p>
     * This is useful for debugging when tests fail with "No cached response found"
     * errors from CapiProxy. The prompts in your test must match these exactly.
     * </p>
     *
     * @return list of expected user prompt strings, or empty list if none found
     */
    public List<String> getExpectedUserPrompts() {
        if (currentSnapshotFile == null || !Files.exists(currentSnapshotFile)) {
            return List.of();
        }
        try {
            String content = Files.readString(currentSnapshotFile);
            List<String> prompts = new ArrayList<>();
            Matcher matcher = USER_CONTENT_PATTERN.matcher(content);
            while (matcher.find()) {
                String prompt = matcher.group(1).trim();
                // Remove quotes if present
                if ((prompt.startsWith("\"") && prompt.endsWith("\""))
                        || (prompt.startsWith("'") && prompt.endsWith("'"))) {
                    prompt = prompt.substring(1, prompt.length() - 1);
                }
                if (!prompts.contains(prompt)) {
                    prompts.add(prompt);
                }
            }
            return prompts;
        } catch (IOException e) {
            LOG.warning("Failed to read snapshot file: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Ensures the proxy is alive, restarting it if necessary.
     *
     * @throws IOException
     *             if the proxy cannot be restarted
     * @throws InterruptedException
     *             if interrupted during restart
     */
    public void ensureProxyAlive() throws IOException, InterruptedException {
        if (!proxy.isAlive()) {
            proxyUrl = proxy.restart();
        }
    }

    /**
     * Gets the captured HTTP exchanges from the proxy.
     *
     * @return list of exchange maps
     * @throws IOException
     *             if the request fails
     * @throws InterruptedException
     *             if the request is interrupted
     */
    public List<Map<String, Object>> getExchanges() throws IOException, InterruptedException {
        return proxy.getExchanges();
    }

    /**
     * Gets the environment variables needed for the Copilot CLI.
     *
     * @return map of environment variables
     */
    public Map<String, String> getEnvironment() {
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("COPILOT_API_URL", proxyUrl);
        env.put("XDG_CONFIG_HOME", homeDir.toString());
        env.put("XDG_STATE_HOME", homeDir.toString());
        return env;
    }

    /**
     * Creates a CopilotClient configured for this test context.
     *
     * @return a new CopilotClient
     */
    public CopilotClient createClient() {
        CopilotClientOptions options = new CopilotClientOptions().setCliPath(cliPath).setCwd(workDir.toString())
                .setEnvironment(getEnvironment());

        // In CI, use a fake token to avoid auth issues
        String ci = System.getenv("CI");
        if (ci != null && !ci.isEmpty()) {
            options.setGithubToken("fake-token-for-e2e-tests");
        }

        return new CopilotClient(options);
    }

    @Override
    public void close() throws Exception {
        proxy.stop();

        // Clean up temp directories (best effort)
        deleteRecursively(homeDir);
        deleteRecursively(workDir);
    }

    private static Path findRepoRoot() throws IOException {
        // First, check for copilot.sdk.dir system property (set by Maven during tests)
        String sdkDir = System.getProperty("copilot.sdk.dir");
        if (sdkDir != null && !sdkDir.isEmpty()) {
            Path sdkPath = Paths.get(sdkDir);
            if (Files.exists(sdkPath)) {
                return sdkPath;
            }
        }

        // Fallback: search up from current directory
        Path dir = Paths.get(System.getProperty("user.dir"));
        while (dir != null) {
            if (Files.exists(dir.resolve("nodejs")) && Files.exists(dir.resolve("test").resolve("harness"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IOException("Could not find repository root. Either set copilot.sdk.dir system property "
                + "or run from within the copilot-sdk repository.");
    }

    private static String getCliPath(Path repoRoot) throws IOException {
        // Try environment variable first (explicit override)
        String envPath = System.getenv("COPILOT_CLI_PATH");
        if (envPath != null && !envPath.isEmpty()) {
            return envPath;
        }

        // Try test harness platform-specific binary (preferred as it has correct
        // version)
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        String platform = os.contains("mac") ? "darwin" : os.contains("win") ? "win32" : "linux";
        String cpuArch = arch.contains("aarch64") || arch.contains("arm64") ? "arm64" : "x64";
        Path platformBinary = repoRoot
                .resolve("test/harness/node_modules/@github/copilot-" + platform + "-" + cpuArch + "/copilot");
        if (os.contains("win")) {
            platformBinary = repoRoot
                    .resolve("test/harness/node_modules/@github/copilot-" + platform + "-" + cpuArch + "/copilot.exe");
        }
        if (Files.exists(platformBinary)) {
            return platformBinary.toString();
        }

        // Try test harness npm-loader.js
        Path harnessCliPath = repoRoot.resolve("test/harness/node_modules/@github/copilot/npm-loader.js");
        if (Files.exists(harnessCliPath)) {
            return harnessCliPath.toString();
        }

        // Try nodejs installation
        Path cliPath = repoRoot.resolve("nodejs/node_modules/@github/copilot/index.js");
        if (Files.exists(cliPath)) {
            return cliPath.toString();
        }

        // Fallback: try to find 'copilot' in PATH
        String copilotInPath = findCopilotInPath();
        if (copilotInPath != null) {
            return copilotInPath;
        }

        throw new IOException("CLI not found. Either install 'copilot' globally, set COPILOT_CLI_PATH, "
                + "or run 'npm install' in the nodejs directory or test/harness directory.");
    }

    private static String findCopilotInPath() {
        try {
            String command = System.getProperty("os.name").toLowerCase().contains("win") ? "where" : "which";
            ProcessBuilder pb = new ProcessBuilder(command, "copilot");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                int exitCode = process.waitFor();
                if (exitCode == 0 && line != null && !line.isEmpty()) {
                    return line.trim();
                }
            }
        } catch (Exception e) {
            // Ignore - copilot not found in PATH
        }
        return null;
    }

    private static void deleteRecursively(Path path) {
        try {
            if (Files.exists(path)) {
                Files.walk(path).sorted((a, b) -> b.compareTo(a)) // Reverse order to delete children first
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                // Best effort
                            }
                        });
            }
        } catch (IOException e) {
            // Best effort
        }
    }
}
