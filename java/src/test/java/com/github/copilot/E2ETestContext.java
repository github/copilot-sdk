/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

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

import com.github.copilot.rpc.CopilotClientOptions;

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
 * 		CopilotSession session = client
 * 				.createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();
 * 		// ... run test ...
 * 	}
 * }
 * }
 * </pre>
 */
public class E2ETestContext implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(E2ETestContext.class.getName());

    /**
     * The default GitHub token used by the CLI in e2e tests. The proxy resolves
     * this token to the default Copilot user registered at context creation.
     */
    private static final String DEFAULT_GITHUB_TOKEN = "fake-token-for-e2e-tests";
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

        // Register a default Copilot user for the CLI's default token so the proxy's
        // /copilot_internal/user endpoint returns a valid user (HTTP 200) instead of
        // 401 "Bad credentials". CLI 1.0.64-1 gates MCP enablement on this user:
        // `is_mcp_enabled` (added by the proxy) is the global gate, and snake_case
        // `copilot_plan` makes the third-party MCP policy resolver early-return
        // allow-all for non-org plans (anything other than business/enterprise),
        // avoiding a /copilot/mcp_registry network call the proxy does not serve.
        // Without this, MCP servers never reach CONNECTED. This mirrors the Go,
        // Node, Python, and .NET harnesses, which all register the same default
        // individual_pro user at context creation.
        Map<String, Object> defaultUser = new HashMap<>();
        defaultUser.put("login", "e2e-test-user");
        defaultUser.put("copilot_plan", "individual_pro");
        defaultUser.put("endpoints", Map.of("api", proxyUrl, "telemetry", "https://localhost:1/telemetry"));
        defaultUser.put("analytics_tracking_id", "e2e-test-tracking-id");
        proxy.setCopilotUserByToken(DEFAULT_GITHUB_TOKEN, defaultUser);

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
     * Gets the repository root for locating shared test assets.
     */
    public Path getRepoRoot() {
        return repoRoot;
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
        // Route GitHub API calls (e.g. the MCP registry policy check) to the
        // replay proxy so MCP enablement stays hermetic. Without this the CLI
        // reaches the real api.github.com, which is slow/unreachable on macOS
        // CI runners and makes MCP servers time out before reaching connected.
        env.put("COPILOT_DEBUG_GITHUB_API_URL", proxyUrl);
        env.put("COPILOT_HOME", homeDir.toString());
        env.put("GH_CONFIG_DIR", homeDir.toString());
        env.put("XDG_CONFIG_HOME", homeDir.toString());
        env.put("XDG_STATE_HOME", homeDir.toString());

        // Configure CONNECT proxy for HTTPS interception if available
        String connectUrl = proxy.getConnectProxyUrl();
        String caFile = proxy.getCaFilePath();
        if (connectUrl != null && !connectUrl.isEmpty() && caFile != null && !caFile.isEmpty()) {
            String noProxy = "127.0.0.1,localhost,::1";
            env.put("HTTP_PROXY", connectUrl);
            env.put("HTTPS_PROXY", connectUrl);
            env.put("http_proxy", connectUrl);
            env.put("https_proxy", connectUrl);
            env.put("NO_PROXY", noProxy);
            env.put("no_proxy", noProxy);
            env.put("NODE_EXTRA_CA_CERTS", caFile);
            env.put("SSL_CERT_FILE", caFile);
            env.put("REQUESTS_CA_BUNDLE", caFile);
            env.put("CURL_CA_BUNDLE", caFile);
            env.put("GIT_SSL_CAINFO", caFile);
            env.put("GH_TOKEN", DEFAULT_GITHUB_TOKEN);
            env.put("GITHUB_TOKEN", DEFAULT_GITHUB_TOKEN);
            env.put("GH_ENTERPRISE_TOKEN", "");
            env.put("GITHUB_ENTERPRISE_TOKEN", "");
        }

        return env;
    }

    /**
     * Creates a CopilotClient configured for this test context.
     *
     * @return a new CopilotClient
     */
    public CopilotClient createClient() {
        CopilotClientOptions options = new CopilotClientOptions().setCliPath(cliPath).setCwd(workDir.toString())
                .setEnvironment(getEnvironment()).setGitHubToken(DEFAULT_GITHUB_TOKEN);

        return new CopilotClient(options);
    }

    /**
     * Creates a CopilotClient with the given options, applied on top of the default
     * options for this test context.
     *
     * @param options
     *            options to apply; environment and cliPath will be set from the
     *            context if not already set
     * @return a new CopilotClient
     */
    public CopilotClient createClient(CopilotClientOptions options) {
        if (options.getCliPath() == null) {
            options.setCliPath(cliPath);
        }
        if (options.getCwd() == null) {
            options.setCwd(workDir.toString());
        }
        if (options.getEnvironment() == null || options.getEnvironment().isEmpty()) {
            options.setEnvironment(getEnvironment());
        }
        if (options.getGitHubToken() == null) {
            options.setGitHubToken(DEFAULT_GITHUB_TOKEN);
        }

        return new CopilotClient(options);
    }

    /**
     * Configures the proxy to return a specific Copilot user response for a given
     * token. Used for per-session authentication tests.
     *
     * @param token
     *            the GitHub token
     * @param login
     *            the user login
     * @param copilotPlan
     *            the Copilot plan
     * @param apiUrl
     *            the API URL for the user endpoints
     * @param telemetryUrl
     *            the telemetry URL
     * @param analyticsTrackingId
     *            the analytics tracking ID
     * @throws IOException
     *             if the request fails
     * @throws InterruptedException
     *             if the request is interrupted
     */
    public void setCopilotUserByToken(String token, String login, String copilotPlan, String apiUrl,
            String telemetryUrl, String analyticsTrackingId) throws IOException, InterruptedException {
        ensureProxyAlive();
        proxy.setCopilotUserByToken(token, login, copilotPlan, apiUrl, telemetryUrl, analyticsTrackingId);
    }

    /**
     * Initializes the proxy state without loading a snapshot.
     * <p>
     * Use this for tests that need the proxy to be active (e.g., for per-session
     * auth token resolution via {@code /copilot_internal/user}) but do not make AI
     * completion requests and therefore have no snapshot to load.
     * </p>
     * <p>
     * The proxy requires its internal {@code state} to be initialized before it can
     * handle most endpoints. Without this call the proxy throws an error and
     * returns HTTP 500 for any request that arrives before a {@code /config} POST
     * has been made.
     * </p>
     *
     * @throws IOException
     *             if the proxy configuration request fails
     * @throws InterruptedException
     *             if the request is interrupted
     */
    public void initializeProxy() throws IOException, InterruptedException {
        ensureProxyAlive();
        // Pass a non-existent snapshot path. The proxy initializes its state even when
        // the file is absent (storedData simply remains undefined), which is fine for
        // tests that never make AI chat-completion requests.
        proxy.configure(workDir.resolve("no-snapshot.yaml").toString(), workDir.toString());
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

        // Try nodejs installation. As of CLI 1.0.64-1 the @github/copilot package
        // is a thin loader; the runnable index.js ships in the installed
        // platform-specific package (e.g. @github/copilot-linux-x64). Exactly one
        // is installed. Running index.js under Node.js is the documented preferred
        // entry point and matches the Go, Python, Rust, and .NET test harnesses.
        Path githubModules = repoRoot.resolve("nodejs/node_modules/@github");
        if (Files.isDirectory(githubModules)) {
            try (var modules = Files.newDirectoryStream(githubModules, "copilot-*")) {
                for (Path module : modules) {
                    Path indexJs = module.resolve("index.js");
                    if (Files.exists(indexJs)) {
                        return indexJs.toString();
                    }
                }
            }
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
